package org.datacite.mds.service;

import javax.validation.ValidationException;
import javax.xml.validation.Validator;

import org.xml.sax.SAXException;

import org.datacite.mds.service.SchemaConvertException;

public interface SchemaService {
    
    String getNamespace(byte[] xml) throws ValidationException;
    
    String getSchemaLocation(byte[] xml) throws ValidationException;
    
    Validator getSchemaValidator(String schemaLocation) throws SAXException;
    
    String getDoi(byte[] xml);

    boolean isDifSchema(byte[] xml);

    boolean isIsoSchema(byte[] xml);

    byte[]  convertDifToDatacite(byte[] dif, String doi) throws SchemaConvertException;

}
