/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     qlamerand
 */
package org.nuxeo.ui.select2.automation;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.i18n.I18NUtils;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.QName;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;


@Operation(id = SuggestDirectoryEntries.ID, category = Constants.CAT_SERVICES, label = "Get directory entries", description = "Get the entries of a directory. This is returning a blob containing a serialized JSON array. The input document, if specified, is used as a context for a potential local configuration of the directory.")
public class SuggestDirectoryEntries {

    private static final Log log = LogFactory.getLog(SuggestDirectoryEntries.class);

    public static final String ID = "Directory.SuggestEntries";

    @Context
    protected OperationContext ctx;

    @Context
    protected DirectoryService directoryService;

    @Context
    protected SchemaManager schemaManager;

    @Param(name = "directoryName", required = true)
    protected String directoryName;

    @Param(name = "translateLabels", required = false)
    protected boolean translateLabels;

    @Param(name = "lang", required = false)
    protected String lang;

    @Param(name = "prefix", required = false)
    protected String prefix;

    @OperationMethod
    public Blob run() throws Exception {
        Directory directory = directoryService.getDirectory(directoryName);
        Session session = null;
        try {
            session = directory.getSession();
            DocumentModelList entries = null;
            boolean postFilter = true;

            if (!translateLabels) {
                postFilter = false;
                // do the filtering at directory level
                if (prefix==null || prefix.isEmpty()) {
                    entries = session.getEntries();
                } else {
                    Map<String, Serializable> filter = new HashMap<String, Serializable>();
                    filter.put(directory.getIdField(), prefix);
                    filter.put("label", prefix);
                    entries = session.query(filter, filter.keySet());
                }
            } else {
                // get all entries and post-filter
                entries = session.getEntries();
            }

            String schemaName = directory.getSchema();
            Schema schema = schemaManager.getSchema(schemaName);
            JSONArray rows = new JSONArray();
            for (DocumentModel entry : entries) {
                JSONObject obj = new JSONObject();
                for (Field field : schema.getFields()) {
                    QName fieldName = field.getName();
                    String key = fieldName.getLocalName();
                    Serializable value = entry.getPropertyValue(fieldName.getPrefixedName());
                    if (translateLabels && "label".equals(key)) {
                        value = translate((String) value);
                    }
                    obj.element(key, value);
                }

                if (prefix!=null && !prefix.isEmpty() && postFilter) {
                    if (obj.getString("label").toLowerCase().startsWith(prefix.toLowerCase())) {
                        rows.add(obj);
                    }
                } else {
                    rows.add(obj);
                }

            }
            return new StringBlob(rows.toString(), "application/json");
        } finally {
            try {
                if (session != null) {
                    session.close();
                }
            } catch (ClientException ce) {
                log.error("Could not close directory session", ce);
            }
        }
    }

    protected Locale getLocale() {
        if (lang == null) {
            lang = (String) ctx.get("lang");
        }
        if (lang == null) {
            lang = "en";
        }
        return new Locale(lang);
    }

    protected String translate(String key) {
        if (key == null) {
            return "";
        }
        return I18NUtils.getMessageString("messages", key, new Object[0],
                getLocale());
    }

}
