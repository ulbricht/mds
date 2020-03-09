package org.datacite.mds.web.api.controller;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.datacite.mds.service.HandleException;
import org.datacite.mds.service.HandleService;
import org.datacite.mds.web.api.ApiController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class StatusController extends ApiController {

    @PersistenceContext
    EntityManager em;

    @Autowired
    HandleService handleService;

    @RequestMapping(value = "/status")
    public ResponseEntity<String> status() throws HandleException {
        checkDatabaseConnection();
        handleService.ping();
        return new ResponseEntity<String>("OK", null, HttpStatus.OK);
    }

    private void checkDatabaseConnection() {
        em.createNativeQuery("SELECT 1").getSingleResult();
    }

}
