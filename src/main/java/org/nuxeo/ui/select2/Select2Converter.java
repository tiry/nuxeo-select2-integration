package org.nuxeo.ui.select2;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;

import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.intercept.BypassInterceptors;

@Name("select2Converter")
@BypassInterceptors
@org.jboss.seam.annotations.faces.Converter
public class Select2Converter implements Serializable, Converter {

    private static final long serialVersionUID = 1L;

    protected static final String SEP = ",";


    protected String getSeparator() {
        return SEP;
    }

    @Override
    public Object getAsObject(FacesContext context, UIComponent component,
            String value) {
        if (value==null) {
            return null;
        } else {
            String[] values = value.split(getSeparator());
            return Arrays.asList(values);
        }
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component,
            Object value) {
        if (value==null) {
            return null;
        } else {
            String stringValue="";
            if (value instanceof List) {
                for (Object v : (List) value) {
                    stringValue += v.toString() + getSeparator();
                }
            } else if (value instanceof Object[]) {
                for (Object v : (Object[]) value) {
                    stringValue += v.toString() + getSeparator();
                }
            }
            if (stringValue.endsWith(getSeparator())) {
                stringValue = stringValue.substring(0, stringValue.length() - getSeparator().length());
            }
            return stringValue;
        }
    }

}
