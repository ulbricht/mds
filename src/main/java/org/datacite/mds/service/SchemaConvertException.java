package org.datacite.mds.service;

/**
 * Wrapper for exceptions from Proxy Service
 */
public class SchemaConvertException extends Exception {

    private static final long serialVersionUID = 1L;

    public SchemaConvertException(String message, Throwable cause) {
        super(message, cause);
    }

    public SchemaConvertException(String message) {
        super(message);
    }
}
