package org.datacite.mds.service.impl;

import java.util.Date;

import javax.validation.ValidationException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.datacite.mds.domain.Datacentre;
import org.datacite.mds.domain.Dataset;
import org.datacite.mds.service.DoiService;
import org.datacite.mds.service.HandleException;
import org.datacite.mds.service.HandleService;
import org.datacite.mds.service.SecurityException;
import org.datacite.mds.util.SecurityUtils;
import org.datacite.mds.validation.ValidationHelper;
import org.datacite.mds.web.api.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DoiServiceImpl implements DoiService {

    static final Logger log4j = Logger.getLogger(DoiServiceImpl.class);
    
    @Autowired
    HandleService handleService;
    
    @Autowired
    ValidationHelper validationHelper;

    @Override
    public Dataset createOrUpdate(String doi, String url, boolean testMode) throws HandleException, SecurityException, ValidationException {
        Datacentre datacentre = SecurityUtils.getCurrentDatacentre();
        SecurityUtils.checkQuota(datacentre);

        Dataset dataset = findOrNewDataset(doi);

	if (StringUtils.isNotEmpty(url))
		dataset.setUrl(url); 

        validationHelper.validate(dataset);

        log4j.debug("trying handle registration: " + doi);
        if (!testMode && StringUtils.isNotEmpty(url)) {
            try {
                handleService.create(doi, url);
                log4j.info(datacentre.getSymbol() + " successfuly minted " + doi);
                if (dataset.getMinted() == null)
                    dataset.setMinted(new Date());
            } catch (HandleException e) {
                handleService.update(doi, url);
                log4j.info(datacentre.getSymbol() + " successfuly updated " + doi);
            }
        } else
            log4j.debug("TEST MODE or empty URL- minting skipped");

        datacentre.incQuotaUsed(Datacentre.ForceRefresh.YES);

        if (!testMode) {
            if (dataset.getId() == null) {
                dataset.persist();
            } else {
                dataset.merge();
            }
            log4j.debug("doi registration: " + dataset.getDoi() + " successful");
        } else {
            log4j.debug("TEST MODE - registration skipped");
        }

        return dataset;
    }

    @Override
    public Dataset resolve(String doi) throws SecurityException, HandleException, NotFoundException {
        String url = resolveDoiOrNull(doi);
        
        Dataset dataset = findOrNewDataset(doi);
        
        if (dataset.getId() == null)
            throw new NotFoundException("DOI not found");
        
        dataset.setUrl(url);
        
        return dataset;
    }
    
    private Dataset findOrNewDataset(String doi) throws SecurityException {
        Datacentre datacentre = SecurityUtils.getCurrentDatacentre();
        Dataset dataset = Dataset.findDatasetByDoi(doi);
        if (dataset == null) {
            dataset = new Dataset();
            dataset.setDatacentre(datacentre);
            dataset.setDoi(doi);
        } else {
            SecurityUtils.checkDatasetOwnership(dataset, datacentre);
        }
        return dataset;

    }

    private String resolveDoiOrNull(String doi) throws HandleException {
        try {
            return handleService.resolve(doi);
        } catch (NotFoundException e) {
            return null;
        }
    }
}
