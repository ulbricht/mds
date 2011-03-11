package org.datacite.mds.validation.constraints.impl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang.StringUtils;
import org.datacite.mds.domain.Metadata;
import org.datacite.mds.util.Utils;
import org.datacite.mds.util.ValidationUtils;
import org.datacite.mds.validation.constraints.MatchDoi;
import org.springframework.beans.factory.annotation.Value;
import org.xml.sax.InputSource;

public class MatchDoiValidator implements ConstraintValidator<MatchDoi, Metadata> {
    String defaultMessage;

    @Value("${xml.schema.default.doiXPath}")
    String xPath;

    XPathExpression xPathExpression;

    public void initialize(MatchDoi constraintAnnotation) {
        defaultMessage = constraintAnnotation.message();
        initXPath();
    }

    private void initXPath() {
        XPathFactory factory = XPathFactory.newInstance();
        XPath xPath = factory.newXPath();
        try {
            xPathExpression = xPath.compile(this.xPath);
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isValid(Metadata metadata, ConstraintValidatorContext context) {
        ValidationUtils.addConstraintViolation(context, defaultMessage, "xml");
        String doiFromDataset = metadata.getDataset().getDoi();
        String doiFromXml;
        try {
            doiFromXml = getDoiFromXml(metadata);
            doiFromXml = Utils.normalizeDoi(doiFromXml);
        } catch (XPathExpressionException e) {
            return false;
        }
        boolean isValid = StringUtils.equals(doiFromDataset, doiFromXml);
        return isValid;
    }

    private String getDoiFromXml(Metadata metadata) throws XPathExpressionException {
        byte[] xml = metadata.getXml();
        InputStream stream = new ByteArrayInputStream(xml);
        InputSource source = new InputSource(stream);
        String doi = xPathExpression.evaluate(source);
        return doi;
    }
}
