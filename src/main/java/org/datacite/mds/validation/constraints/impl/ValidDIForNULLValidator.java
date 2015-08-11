package org.datacite.mds.validation.constraints.impl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Validator;

import org.apache.commons.lang.ArrayUtils;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.datacite.mds.service.SchemaService;
import org.datacite.mds.util.ValidationUtils;
import org.datacite.mds.validation.constraints.ValidDIForNULL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

@Configurable
public class ValidDIForNULLValidator implements ConstraintValidator<ValidDIForNULL, byte[]> {
    Logger log = Logger.getLogger(ValidDIForNULLValidator.class);

    boolean enabled;
    
    String difNamespace;
    
    @Autowired
    SchemaService schemaService;

    public void initialize(ValidDIForNULL constraintAnnotation) {
        log.trace("init: enabled=" + enabled);
        log.trace("init: difNamespace=" + difNamespace);
        log.trace("init: schemaService=" + schemaService);
    }

    public boolean isValid(byte[] xml, ConstraintValidatorContext context) {

		  if (ArrayUtils.isEmpty(xml))
			return true;

        try {
            if (enabled) {
                checkValidity(xml);
            } else {
                log.debug("validation skipped; checking only for well-formedness");
                checkWellFormedness(xml);
            }
        } catch (Exception e) {
            ValidationUtils.addConstraintViolation(context, "xml error: " + e.getMessage());
            return false;
        }
        return true;
    }

    private void checkWellFormedness(byte[] xml) throws Exception {
        XMLReader reader = XMLReaderFactory.createXMLReader();
        InputStream xmlStream = new ByteArrayInputStream(xml);
        reader.parse(new InputSource(xmlStream));
    }

    private void checkValidity(byte[] xml) throws Exception {
        String schemaLocation = schemaService.getSchemaLocation(xml);
        String namespace=schemaService.getNamespace(xml);
        log.debug("schemaLocation=" + schemaLocation);
        if (!StringUtils.equals(namespace, difNamespace))
            throw new Exception("schemaLocation does not start with '"+difNamespace+"'");
        Validator validator = schemaService.getSchemaValidator(schemaLocation);
        InputStream xmlStream = new ByteArrayInputStream(xml);
        Source xmlSource = new StreamSource(xmlStream);
        validator.validate(xmlSource);
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
    
    public String getdifNamespace() {
        return difNamespace;
    }

    public void setdifNamespace(String difNamespace) {
        this.difNamespace = difNamespace;
    }

}