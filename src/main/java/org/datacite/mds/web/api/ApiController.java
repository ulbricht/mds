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

        if (ex instanceof ConstraintViolationException) {
            ConstraintViolationException constraintException = (ConstraintViolationException) ex;
            Set<ConstraintViolation<?>> violations = constraintException.getConstraintViolations();
            return new ResponseEntity<String>(ValidationUtils.collateViolationMessages(violations),
                    HttpStatus.BAD_REQUEST);

        } else if (ex instanceof ValidationException) {
            Throwable cause = ex.getCause();
            if (cause == null) {
                return new ResponseEntity<String>(ex.getMessage(), HttpStatus.BAD_REQUEST);
            } else {
                return handleExceptions(cause, response);
            }
        } else if (ex instanceof SecurityException) {
            return new ResponseEntity<String>(ex.getMessage(), HttpStatus.FORBIDDEN);
        } else if (ex instanceof NotFoundException) {
            return new ResponseEntity<String>(ex.getMessage(), HttpStatus.NOT_FOUND);
        } else if (ex instanceof HandleException) {
            return new ResponseEntity<String>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } else if (ex instanceof DeletedException) {
            return new ResponseEntity<String>(ex.getMessage(), HttpStatus.GONE);
        } else if (ExceptionUtils.indexOfType(ex, JDBCConnectionException.class) != -1) {
            return new ResponseEntity<String>("database connection problem", HttpStatus.INTERNAL_SERVER_ERROR);
        } else if (ex instanceof JpaOptimisticLockingFailureException) {
            return new ResponseEntity<String>("Another user has changed this record. Please try again",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        logger.error("uncaught exception", ex);
        return new ResponseEntity<String>("uncaught exception (" + ex.getMessage() + ")",
                HttpStatus.INTERNAL_SERVER_ERROR);

    }

}
