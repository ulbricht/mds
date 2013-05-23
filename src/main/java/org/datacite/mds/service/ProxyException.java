package org.datacite.mds.service;

/**
 * Wrapper for exceptions from Proxy Service
 */
public class ProxyException extends Exception {

    private static final long serialVersionUID = 1L;

    public ProxyException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProxyException(String message) {
        super(message);
    }
}
