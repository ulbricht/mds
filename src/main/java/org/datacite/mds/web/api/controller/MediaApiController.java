package org.datacite.mds.web.api.controller;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.datacite.mds.domain.AllocatorOrDatacentre;
import org.datacite.mds.domain.Datacentre;
import org.datacite.mds.domain.Dataset;
import org.datacite.mds.domain.Media;
import org.datacite.mds.service.SecurityException;
import org.datacite.mds.util.SecurityUtils;
import org.datacite.mds.util.Utils;
import org.datacite.mds.web.api.ApiController;
import org.datacite.mds.web.api.ApiUtils;
import org.datacite.mds.web.api.NotFoundException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.persistence.NoResultException;

@Controller
@RequestMapping("/media")
public class MediaApiController extends ApiController {

    private static Logger log4j = Logger.getLogger(MediaApiController.class);

    @RequestMapping(value = "")
    public ResponseEntity<String> rootHandler() throws NotFoundException {
        throw new NotFoundException("doi missing");
    }

    @RequestMapping(value = "**", method = { RequestMethod.GET, RequestMethod.HEAD })
    public ResponseEntity<String> get(HttpServletRequest httpRequest)
            throws IOException, SecurityException, NotFoundException {
        String doi = getDoiFromRequest(httpRequest);
        AllocatorOrDatacentre user = SecurityUtils.getCurrentAllocatorOrDatacentre();

        Dataset dataset = Dataset.findDatasetByDoi(doi);
        if (dataset == null)
            throw new NotFoundException("DOI is unknown to MDS");

        SecurityUtils.checkDatasetOwnership(dataset, user);

        List<Media> medias = Media.findMediasByDataset(dataset).getResultList();
        if (medias.isEmpty())
            throw new NotFoundException("no media for the DOI");

        StringBuffer responseBody = new StringBuffer();
        for (Media media : medias)
            responseBody.append(media.getMediaType() + "=" + media.getUrl() + "\n");

        return new ResponseEntity<String>(responseBody.toString(), HttpStatus.OK);
    }

    private String getDoiFromRequest(HttpServletRequest request) {
        String uri = request.getServletPath();
        String doi = uri.replaceFirst("/media/", "");
        doi = Utils.normalizeDoi(doi);
        return doi;
    }

    @RequestMapping(value = "**", method = { RequestMethod.POST })
    public ResponseEntity<String> post(@RequestBody String body, @RequestParam(required = false) Boolean testMode,
            HttpServletRequest httpRequest) throws SecurityException, IOException, NotFoundException {
        String doi = getDoiFromRequest(httpRequest);
        if (testMode == null)
            testMode = false;
        Datacentre datacentre = SecurityUtils.getCurrentDatacentre();
        Dataset dataset = Dataset.findDatasetByDoi(doi);
        if (dataset == null)
            throw new NotFoundException("DOI is unknown to MDS");

        SecurityUtils.checkDatasetOwnership(dataset, datacentre);

        Properties props = new Properties();
        props.load(new StringReader(body));

        for (String mediaType : props.stringPropertyNames()) {
            String url = props.getProperty(mediaType);

            try {
                Media media = Media.findMediaByDatasetAndMediaType(dataset, mediaType);
                media.setUrl(url);
                if (!testMode)
                    media.merge();
            } catch (EmptyResultDataAccessException ex) {
                mediaPersist(dataset, mediaType, url, testMode);
            } catch (NoResultException ex) {
                mediaPersist(dataset, mediaType, url, testMode);
            }
        }

        log4j.info(datacentre.getSymbol() + " added/updated media for " + doi);

        String message = ApiUtils.makeResponseMessage("OK", testMode);
        return new ResponseEntity<String>(message, HttpStatus.OK);
    }

    private void mediaPersist(Dataset dataset, String mediaType, String url, Boolean testMode) {
        Media media = new Media();
        media.setDataset(dataset);
        media.setMediaType(mediaType);
        media.setUrl(url);
        if (!testMode)
            media.persist();
    }

}
