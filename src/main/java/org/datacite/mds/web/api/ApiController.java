package org.datacite.mds.web.api;

import org.springframework.web.bind.annotation.ExceptionHandler;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.ValidationException;

import org.datacite.mds.web.api.NotFoundException;
import org.datacite.mds.web.api.DeletedException;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.datacite.mds.service.HandleException;
import org.datacite.mds.service.SecurityException;
import org.datacite.mds.util.ValidationUtils;
import org.hibernate.exception.JDBCConnectionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.jpa.JpaOptimisticLockingFailureException;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.Set;

/**
 * Empty Interface to 'mark' all our API controllers
 */
public class ApiController {

    Logger logger = Logger.getLogger(ApiResponseWrapper.class);

    @ExceptionHandler({ ConstraintViolationException.class, ValidationException.class, SecurityException.class,
            NotFoundException.class, HandleException.class, DeletedException.class,
            JpaOptimisticLockingFailureException.class, Exception.class })
    public ResponseEntity handleExceptions(Throwable ex, HttpServletResponse response) throws IOException {
        // logger.debug("handling exception: ", ex);

        String errormessage = "OK";
        HttpStatus errorcode = HttpStatus.OK;
        if (ex instanceof ConstraintViolationException) {
            ConstraintViolationException constraintException = (ConstraintViolationException) ex;
            Set<ConstraintViolation<?>> violations = constraintException.getConstraintViolations();
            errormessage = ValidationUtils.collateViolationMessages(violations);
            errorcode = HttpStatus.BAD_REQUEST;
        } else if (ex instanceof ValidationException) {
            handleCause(ex, response, HttpServletResponse.SC_BAD_REQUEST);
        } else if (ex instanceof SecurityException) {
            errormessage = ex.getMessage();
            errorcode = HttpStatus.FORBIDDEN;
        } else if (ex instanceof NotFoundException) {
            errormessage = ex.getMessage();
            errorcode = HttpStatus.NOT_FOUND;
            // logger.debug("Exception::::::" + ex.getMessage());
        } else if (ex instanceof HandleException) {
            errormessage = ex.getMessage();
            errorcode = HttpStatus.INTERNAL_SERVER_ERROR;
        } else if (ex instanceof DeletedException) {
            errormessage = ex.getMessage();
            errorcode = HttpStatus.GONE;
        } else if (ExceptionUtils.indexOfType(ex, JDBCConnectionException.class) != -1) {
            errormessage = "database connection problem";
            errorcode = HttpStatus.INTERNAL_SERVER_ERROR;
        } else if (ex instanceof JpaOptimisticLockingFailureException) {
            errormessage = "Another user has changed this record. Please try again";
            errorcode = HttpStatus.INTERNAL_SERVER_ERROR;
        } else {
            logger.error("uncaught exception", ex);
            errormessage = "uncaught exception (" + ex.getMessage() + ")";
            errorcode = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        return new ResponseEntity<String>(errormessage, errorcode);

    }

    private void handleCause(Throwable ex, HttpServletResponse response, int defaultResponseCode) throws IOException {
        Throwable cause = ex.getCause();
        if (cause == null)
            response.sendError(defaultResponseCode, ex.getMessage());
        else
            handleExceptions(cause, response);
    }

}
