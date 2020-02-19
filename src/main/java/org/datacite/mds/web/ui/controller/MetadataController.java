package org.datacite.mds.web.ui.controller;

import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.lang.ArrayUtils;
import org.datacite.mds.service.ProxyService;
import org.datacite.mds.service.ProxyException;
import org.datacite.mds.web.ui.model.CreateMetadataModel;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.datacite.mds.domain.Dataset;
import org.datacite.mds.domain.Metadata;
import org.datacite.mds.service.SecurityException;
import org.datacite.mds.util.SecurityUtils;
import org.datacite.mds.util.Utils;
import org.datacite.mds.web.ui.UiController;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.roo.addon.web.mvc.controller.RooWebScaffold;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.support.ByteArrayMultipartFileEditor;

import org.datacite.mds.service.SchemaConvertException;

import org.springframework.validation.BindingResult;
import javax.validation.Valid;
import javax.servlet.http.HttpServletRequest;
import javax.validation.ValidationException;
import org.datacite.mds.service.HandleException;
import org.datacite.mds.service.SchemaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.FieldError;

import org.datacite.mds.validation.ValidationHelper;
import org.apache.log4j.Logger;

@RooWebScaffold(path = "metadatas", formBackingObject = Metadata.class, delete = false, update = false, populateMethods = false)
@RequestMapping("/metadatas")
@Controller
public class MetadataController implements UiController {

    private static Logger log = Logger.getLogger(MetadataController.class);

    @Autowired
    SchemaService schemaService;

    @Autowired
    ProxyService proxyService;

    @Autowired
    ValidationHelper validationHelper;

    @InitBinder
    void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(byte[].class, new ByteArrayMultipartFileEditor());
    }

    @ModelAttribute("datasets")
    public Collection<Dataset> populateDatasets(@RequestParam(value = "dataset", required = false) Long datasetId)
            throws SecurityException {
        Dataset dataset = Dataset.findDataset(datasetId);
        if (dataset != null)
            SecurityUtils.checkDatasetOwnership(dataset);
        return Arrays.asList(dataset);
    }
    /*
     * @RequestMapping(method = RequestMethod.POST) public String create(@Valid
     * Metadata metadata, BindingResult bindingResult, Model uiModel,
     * HttpServletRequest httpServletRequest) throws SecurityException {
     * SecurityUtils.checkDatasetOwnership(metadata.getDataset()); if
     * (bindingResult.hasErrors()) { uiModel.addAttribute("metadata", metadata);
     * return "metadatas/create"; } uiModel.asMap().clear(); metadata.persist();
     * return "redirect:/metadatas/" +
     * encodeUrlPathSegment(metadata.getId().toString(), httpServletRequest); }
     */

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public String show(@PathVariable("id") Long id, Model model) throws SecurityException {
        Metadata metadata = Metadata.findMetadata(id);
        SecurityUtils.checkDatasetOwnership(metadata.getDataset());
        model.addAttribute("metadata", metadata);
        String prettyXml;
        try {
            byte[] xml = metadata.getXml();
            prettyXml = Utils.formatXML(xml);
        } catch (Exception e) {
            prettyXml = "error formatting xml: " + e.getMessage();
        }

        String prettyDif;
        try {
            byte[] dif = metadata.getDif();
            prettyDif = Utils.formatXML(dif);
        } catch (Exception e) {
            prettyDif = "error formatting dif: " + e.getMessage();
        }

        String prettyIso;
        try {
            byte[] iso = metadata.getIso();
            prettyIso = Utils.formatXML(iso);
        } catch (Exception e) {
            prettyIso = "error formatting iso: " + e.getMessage();
        }
        model.addAttribute("prettyxml", prettyXml);
        model.addAttribute("prettydif", prettyDif);
        model.addAttribute("prettyiso", prettyIso);
        model.addAttribute("itemId", id);
        return "metadatas/show";
    }

    @RequestMapping(method = RequestMethod.POST)
    public String create(@Valid CreateMetadataModel createMetadataModel, BindingResult bindingResult, Model uiModel) {

        if (bindingResult.hasErrors()) {
            uiModel.addAttribute("createMetadataModel", createMetadataModel);
            return "metadatas/create";
        }

        String errorfield = "xml";

        byte[] xml = createMetadataModel.getXml();
        byte[] dif = createMetadataModel.getDif();
        byte[] iso = createMetadataModel.getIso();
        Dataset dataset = createMetadataModel.getDataset();

        Metadata metadata = new Metadata();
        metadata.setDataset(dataset);

        try {

            if (ArrayUtils.getLength(iso) > 10 && schemaService.isIsoSchema(iso))
                metadata.setIso(iso);

            if (ArrayUtils.getLength(dif) > 10 && schemaService.isDifSchema(dif))
                metadata.setDif(dif);

            if (ArrayUtils.getLength(xml) > 10) {
                metadata.setXml(xml);
            } else {

                byte[] datacite = null;
                if (ArrayUtils.getLength(iso) > 10 && schemaService.isIsoSchema(iso)) {
                    errorfield = "iso";
                    datacite = schemaService.convertDifToDatacite(iso, metadata.getDataset().getDoi());
                } else if (ArrayUtils.getLength(dif) > 10 && schemaService.isDifSchema(dif)) {
                    errorfield = "dif";
                    datacite = schemaService.convertDifToDatacite(dif, metadata.getDataset().getDoi());
                }

                if (datacite != null) {
                    metadata.setXml(datacite);
                    metadata.setIsConvertedByMds(true);
                }
            }

            validationHelper.validateTo(bindingResult, metadata);

            if (bindingResult.hasErrors()) {
                uiModel.addAttribute("createMetadataModel", createMetadataModel);
                return "metadatas/create";
            }

            proxyService.metaUpdate(metadata.getXml());

            uiModel.asMap().clear();
            metadata.persist();
            return "redirect:/metadatas/" + metadata.getId().toString();

        } catch (ProxyException e) {
            bindingResult.addError(new FieldError("", "xml", e.toString()));
        } catch (ValidationException e) {
            String error = "Unable to parse XML " + e.getMessage();
            bindingResult.addError(new FieldError("", errorfield, error));
        } catch (SchemaConvertException e) {
            String error = "Can not convert Schema to DataCite " + e.getMessage();
            bindingResult.addError(new FieldError("", errorfield, error));
        }

        uiModel.addAttribute("createMetadataModel", createMetadataModel);
        return "metadatas/create";

    }

    @RequestMapping(params = "form", method = RequestMethod.GET)
    public String createForm(Model uiModel) {
        uiModel.addAttribute("createMetadataModel", new CreateMetadataModel());
        List dependencies = new ArrayList();
        if (Dataset.countDatasets() == 0) {
            dependencies.add(new String[] { "dataset", "datasets" });
        }
        uiModel.addAttribute("dependencies", dependencies);
        return "metadatas/create";
    }

    @RequestMapping(value = "/{id}", params = "raw", method = RequestMethod.GET)
    public ResponseEntity<? extends Object> showRaw(@PathVariable("id") Long id) throws SecurityException {
        HttpHeaders headers = new HttpHeaders();
        Metadata metadata = Metadata.findMetadata(id);
        SecurityUtils.checkDatasetOwnership(metadata.getDataset());
        if (metadata == null) {
            return new ResponseEntity<Object>(HttpStatus.NOT_FOUND);
        }
        headers.setContentType(MediaType.APPLICATION_XML);
        return new ResponseEntity<Object>(metadata.getXml(), headers, HttpStatus.OK);
    }

    @RequestMapping(value = "/{id}", params = "rawdif", method = RequestMethod.GET)
    public ResponseEntity<? extends Object> showRawDif(@PathVariable("id") Long id) {
        HttpHeaders headers = new HttpHeaders();
        Metadata metadata = Metadata.findMetadata(id);
        if (metadata == null) {
            return new ResponseEntity<Object>(HttpStatus.NOT_FOUND);
        }
        headers.setContentType(MediaType.APPLICATION_XML);
        return new ResponseEntity<Object>(metadata.getDif(), headers, HttpStatus.OK);
    }

    @RequestMapping(value = "/{id}", params = "rawiso", method = RequestMethod.GET)
    public ResponseEntity<? extends Object> showRawIso(@PathVariable("id") Long id) {
        HttpHeaders headers = new HttpHeaders();
        Metadata metadata = Metadata.findMetadata(id);
        if (metadata == null) {
            return new ResponseEntity<Object>(HttpStatus.NOT_FOUND);
        }
        headers.setContentType(MediaType.APPLICATION_XML);
        return new ResponseEntity<Object>(metadata.getIso(), headers, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET)
    public String list() {
        return "index";
    }
}
