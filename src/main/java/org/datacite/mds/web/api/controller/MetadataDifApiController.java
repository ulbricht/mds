package org.datacite.mds.web.api.controller;


import org.springframework.beans.factory.annotation.Value;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ValidationException;

import java.io.UnsupportedEncodingException;

import org.datacite.mds.service.ProxyService;
import org.datacite.mds.service.ProxyException;

import org.apache.commons.lang.BooleanUtils;
import org.apache.log4j.Logger;
import org.datacite.mds.domain.AllocatorOrDatacentre;
import org.datacite.mds.domain.Datacentre;
import org.datacite.mds.domain.Dataset;
import org.datacite.mds.domain.Metadata;
import org.datacite.mds.service.DoiService;
import org.datacite.mds.service.HandleException;
import org.datacite.mds.service.SchemaService;
import org.datacite.mds.service.SecurityException;
import org.datacite.mds.util.SecurityUtils;
import org.datacite.mds.validation.ValidationHelper;
import org.datacite.mds.web.api.ApiController;
import org.datacite.mds.web.api.ApiUtils;
import org.datacite.mds.web.api.DeletedException;
import org.datacite.mds.web.api.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import org.apache.commons.lang.ArrayUtils;

@RequestMapping("/igsnmetadata")
@Controller
public class MetadataDifApiController implements ApiController {

    private static Logger log4j = Logger.getLogger(MetadataDifApiController.class);
    
    @Autowired
    DoiService doiService;

    @Autowired
    ValidationHelper validationHelper;
    
    @Autowired
    SchemaService schemaService;

    @Autowired
    ProxyService proxyService; 
          
    
    @RequestMapping(value = "", method = { RequestMethod.GET, RequestMethod.HEAD })
    public ResponseEntity getRoot() {
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = "**", method = { RequestMethod.GET, RequestMethod.HEAD })
    public ResponseEntity<? extends Object> get(HttpServletRequest request) throws SecurityException, NotFoundException, DeletedException {
        String doi = getDoiFromRequest(request);
        log4j.debug(doi);
        AllocatorOrDatacentre user = SecurityUtils.getCurrentAllocatorOrDatacentre();
        
        Dataset dataset = Dataset.findDatasetByDoi(doi);
        if (dataset == null)
            throw new NotFoundException("IGSN is unknown to MDS");

        SecurityUtils.checkDatasetOwnership(dataset, user);

        if (!dataset.getIsActive())
            throw new DeletedException("dataset inactive");

        Metadata metadata = Metadata.findLatestMetadatasByDataset(dataset);
        if (metadata == null)
            throw new NotFoundException("no metadata for the IGSN");
        if (metadata.getDif() == null)
            throw new DeletedException("no IGSN metadata for the IGSN");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        return new ResponseEntity<Object>(metadata.getDif(), headers, HttpStatus.OK);
    }
    
    private String getDoiFromRequest(HttpServletRequest request) {
        String uri = request.getServletPath();
        String doi = uri.replaceFirst("/igsnmetadata/", "");
        return doi;
    }

    @RequestMapping(value = "**", method = RequestMethod.POST)
    public ResponseEntity<String> post(@RequestBody byte[] xml,
                                             @RequestParam(required = false) Boolean testMode,
                                             HttpServletRequest httpRequest) throws NotFoundException, ValidationException, HandleException, SecurityException, UnsupportedEncodingException {
        String doi;

			if (schemaService.isDifSchema(xml) || schemaService.isIsoSchema(xml)){
				doi = getDoiFromRequest(httpRequest);
			}else{
				doi = schemaService.getDoi(xml);
			}

        return storeMetadata(doi, xml, testMode, httpRequest);
    }

       
    @RequestMapping(value = "", method = RequestMethod.PUT)
    public ResponseEntity putRoot() throws HttpRequestMethodNotSupportedException {
        throw new HttpRequestMethodNotSupportedException("PUT");
    }

    @RequestMapping(value = "**", method = RequestMethod.PUT)
    public ResponseEntity<String> put(@RequestBody byte[] xml,
                                             @RequestParam(required = false) Boolean testMode,
                                             HttpServletRequest httpRequest) throws NotFoundException, ValidationException, HandleException, SecurityException, UnsupportedEncodingException {
        String doi = getDoiFromRequest(httpRequest);
        return storeMetadata(doi, xml, testMode, httpRequest);
    }


    private ResponseEntity<String> storeMetadata(String doi, byte[] xml, Boolean testMode, HttpServletRequest httpRequest) throws NotFoundException, ValidationException, HandleException, SecurityException, UnsupportedEncodingException {
        String method = httpRequest.getMethod();
        if (testMode == null)
            testMode = false;
        String logPrefix = "*****" + method + " igsnmetadata (igsn=" + doi + ", testMode=" + testMode + ") ";

        log4j.debug(logPrefix);
        
        if (xml.length == 0)
            throw new ValidationException("request body must not be empty");
        
        if (doi==null || doi.length()==0)
            throw new ValidationException("failed to retrieve IGSN");
        
        Dataset oldDataset = Dataset.findDatasetByDoi(doi);
		  if (oldDataset == null) 
			throw new NotFoundException("IGSN doesn't exist");

		  Metadata oldmetadata=Metadata.findLatestMetadatasByDataset(oldDataset);

		  if (oldmetadata == null) 
			throw new NotFoundException("IGSN doesn't exist");
       
		  Metadata metadata=new Metadata();//copy old values
		
		  byte[] iso = oldmetadata.getIso();
		  if (!ArrayUtils.isEmpty(iso))
		  		metadata.setIso(iso);

		  byte[] dif =oldmetadata.getDif();
		  if (!ArrayUtils.isEmpty(dif))
		  		metadata.setDif(dif);

		  metadata.setXml(oldmetadata.getXml());

		  if (!schemaService.isDifSchema(xml))
            throw new ValidationException("no IGSN XML provided");

        metadata.setDif(xml);
        metadata.setDataset(oldDataset);
      


        validationHelper.validate(metadata);
        
       
        //increases the DOI-quota and validates the dataset
        Dataset dataset = doiService.createOrUpdate(doi, null, testMode);
        
        log4j.debug(logPrefix + "dataset id = " + dataset.getId());
        metadata.setDataset(dataset);
        if (!testMode) {
            log4j.debug(logPrefix + "persisting XML");
            metadata.persist();
            if (BooleanUtils.isFalse(dataset.getIsActive())) {
                dataset.setIsActive(true);
                dataset.merge();
            }
        }
 
        HttpHeaders headers = new HttpHeaders();
        if (method.equals("POST")) {
            StringBuffer location = httpRequest.getRequestURL().append("/" + doi);
            headers.set("Location", location.toString());
        }
        String message = ApiUtils.makeResponseMessage("OK (" + doi + ")", testMode);
        return new ResponseEntity<String>(message, headers, HttpStatus.CREATED);
    }

    @RequestMapping(value = "**", method = RequestMethod.DELETE)
    public ResponseEntity<String> delete(HttpServletRequest request,
            @RequestParam(required = false) Boolean testMode) throws SecurityException, NotFoundException, HandleException {
        String doi = getDoiFromRequest(request);
        if (testMode == null)
            testMode = false;
        log4j.debug("*****DELETE metadata (testMode=" + testMode + ") " + doi);
        
        Datacentre datacentre = SecurityUtils.getCurrentDatacentre();

        Dataset dataset = Dataset.findDatasetByDoi(doi);
        if (dataset == null)
            throw new NotFoundException("IGSN doesn't exist");
        
        Metadata metadata = Metadata.findLatestMetadatasByDataset(dataset);
        if (metadata == null)
            throw new NotFoundException("Metadata doesn't exist");

        SecurityUtils.checkDatasetOwnership(dataset, datacentre);
                      
        if (!testMode) {
				metadata.setDif(null);
				metadata.persist();
            log4j.info(datacentre.getSymbol() + " successfuly deactivated " + doi);
        }

        HttpHeaders headers = new HttpHeaders();
        String message = ApiUtils.makeResponseMessage("OK", testMode);
        return new ResponseEntity<String>(message, headers, HttpStatus.OK);
    }
}
