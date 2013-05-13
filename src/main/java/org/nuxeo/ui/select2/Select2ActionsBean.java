/*
 * (C) Copyright ${year} Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */

package org.nuxeo.ui.select2;

import java.io.BufferedOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlInputHidden;
import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonGenerator;
import org.jboss.el.ValueExpressionLiteral;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.server.jaxrs.io.JsonWriter;
import org.nuxeo.ecm.automation.server.jaxrs.io.writers.JsonDocumentWriter;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.platform.forms.layout.api.Widget;
import org.nuxeo.runtime.api.Framework;

@Name("select2Actions")
@Scope(ScopeType.EVENT)
public class Select2ActionsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(Select2ActionsBean.class);

    protected static final String SELECT2_RESOURCES_MARKER = "SELECT2_RESOURCES_MARKER";

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    public boolean isMultiSelection(Widget widget) {
        if (widget.getProperty("multiple")!=null && widget.getProperty("multiple").toString().equalsIgnoreCase("true")) {
            return true;
        }
        return false;
    }

    public boolean mustIncludeResources() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext!=null) {
            HttpServletRequest request = (HttpServletRequest) facesContext.getExternalContext().getRequest();

            if (request.getAttribute(SELECT2_RESOURCES_MARKER)!=null) {
                return false;
            } else {
                request.setAttribute(SELECT2_RESOURCES_MARKER, "done");
                return true;
            }
        }
        return false;
    }

    protected DocumentModel resolveReference(String storedReference, String operationName) throws Exception {

        if (storedReference==null || storedReference.isEmpty()) {
            return null;
        }
        DocumentModel doc = null;

        if (operationName==null || operationName.isEmpty()) {
            DocumentRef ref = null;
            if (storedReference.startsWith("/")) {
                ref = new PathRef(storedReference);
            } else {
                ref = new IdRef(storedReference);
            }
            if (documentManager.exists(ref)) {
                doc = documentManager.getDocument(ref);
            }
        } else {
            AutomationService as = Framework.getLocalService(AutomationService.class);
            OperationContext ctx = new OperationContext(documentManager);
            ctx.setInput(storedReference);
            doc = (DocumentModel) as.run(ctx, operationName, null);
        }
        return doc;
    }

    public String resolveSingleReference(String storedReference, String operationName, String schemaNames) throws Exception {

        DocumentModel doc = resolveReference(storedReference, operationName);
        if (doc==null) {
            return "";
        }
        String[] schemas = null;
        if (schemaNames!=null && ! schemaNames.isEmpty()) {
            schemas = schemaNames.split(",");
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedOutputStream out = new BufferedOutputStream(baos);
        JsonDocumentWriter.writeDocument(out, doc, schemas);
        out.flush();
        return new String(baos.toByteArray(), "UTF-8");
    }

    public String resolveMultipleReferences(Object value, String operationName, String schemaNames) throws Exception {

        if (value==null) {
            return "[]";
        }

        List<String> storedRefs = new ArrayList<>();
        if (value instanceof List) {
            for (Object v : (List) value) {
                storedRefs.add(v.toString());
            }
        } else if (value instanceof Object[]) {
            for (Object v : (Object[]) value) {
                storedRefs.add(v.toString());
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedOutputStream out = new BufferedOutputStream(baos);
        JsonGenerator jg = JsonWriter.createGenerator(out);
        String[] schemas = null;
        if (schemaNames!=null && ! schemaNames.isEmpty()) {
            schemas = schemaNames.split(",");
        }
        jg.writeStartArray();

        for (String ref : storedRefs) {
            DocumentModel doc = resolveReference(ref, operationName);
            if (doc==null) {
                return "";
            }
            JsonDocumentWriter.writeDocument(jg, doc, schemas);
        }

        jg.writeEndArray();
        out.flush();
        String json =  new String(baos.toByteArray(), "UTF-8");

        if (!json.endsWith("]")) { // XXX !!!
            json = json + "]";
        }

        return json;
    }

    public String resolveSingleReferenceLabel(String storedReference, String operationName, String label) throws Exception {

        DocumentModel doc = resolveReference(storedReference, operationName);
        if (doc==null) {
            return "";
        }

        if (label!=null && ! label.isEmpty()){
            Object val = doc.getPropertyValue(label);
            if (val==null) {
                return "";
            } else {
                return val.toString();
            }
        }
        return doc.getTitle();
    }


    public List<String> resolveMultipleReferenceLabels(Object value, String operationName, String label) throws Exception {

        List<String> result = new ArrayList<>();

        if (value==null) {
            return result;
        }

        List<String> storedRefs = new ArrayList<>();
        if (value instanceof List) {
            for (Object v : (List) value) {
                storedRefs.add(v.toString());
            }
        } else if (value instanceof Object[]) {
            for (Object v : (Object[]) value) {
                storedRefs.add(v.toString());
            }
        }

        for (String ref : storedRefs) {
            DocumentModel doc = resolveReference(ref, operationName);
            if (doc!=null) {
                if (label!=null && ! label.isEmpty()){
                    Object val = doc.getPropertyValue(label);
                    if (val==null) {
                        result.add("");
                    } else {
                        result.add(val.toString());
                    }
                } else {
                  result.add(doc.getTitle());
                }
            }
        }
        return result;
    }

    public String encodeParameters(Widget widget) throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedOutputStream out = new BufferedOutputStream(baos);

        JsonGenerator jg = JsonWriter.createGenerator(out);
        jg.writeStartObject();

        for (Entry<String, Serializable> entry : widget.getProperties().entrySet()) {

            jg.writeStringField(entry.getKey(), entry.getValue().toString());
        }
        jg.writeEndObject();
        jg.flush();
        out.flush();
        return new String(baos.toByteArray(), "UTF-8");
    }

    public void valueChanged(ValueChangeEvent evt) {
        Object value = evt.getNewValue();
        String sourceId = evt.getComponent().getId();
        UIComponent initComponent = evt.getComponent().getParent().findComponent(sourceId + "-init");
        if (initComponent!=null) {
            HtmlInputHidden input = (HtmlInputHidden) initComponent;
            input.setValueExpression("field_0", new ValueExpressionLiteral(value, String.class));
            input.resetValue();
        }
    }
}
