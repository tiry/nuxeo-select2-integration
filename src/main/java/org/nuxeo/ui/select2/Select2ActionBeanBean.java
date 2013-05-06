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
import java.util.Map.Entry;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonGenerator;
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

@Name("select2ActionBean")
@Scope(ScopeType.EVENT)
public class Select2ActionBeanBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(Select2ActionBeanBean.class);

    @In(create = true, required = false)
    protected transient CoreSession documentManager;


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

}
