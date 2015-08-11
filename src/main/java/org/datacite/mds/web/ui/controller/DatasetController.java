package org.datacite.mds.web.ui.controller;



import org.datacite.mds.service.ProxyService;
import org.datacite.mds.service.ProxyException;
import org.datacite.mds.service.SchemaService;
import org.datacite.mds.service.SchemaConvertException;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.validation.Valid;
import javax.validation.ValidationException;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.datacite.mds.domain.Allocator;
import org.datacite.mds.domain.AllocatorOrDatacentre;
import org.datacite.mds.domain.Datacentre;
import org.datacite.mds.domain.Dataset;
import org.datacite.mds.domain.Media;
import org.datacite.mds.domain.Metadata;
import org.datacite.mds.service.HandleException;
import org.datacite.mds.service.HandleService;
import org.datacite.mds.service.SecurityException;
import org.datacite.mds.util.SecurityUtils;
import org.datacite.mds.util.Utils;
import org.datacite.mds.validation.ValidationHelper;
import org.datacite.mds.web.api.NotFoundException;
import org.datacite.mds.web.ui.UiController;
import org.datacite.mds.web.ui.model.CreateDatasetModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.roo.addon.web.mvc.controller.RooWebScaffold;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.support.ByteArrayMultipartFileEditor;

@RooWebScaffold(path = "datasets", formBackingObject = Dataset.class, delete = false, populateMethods = false)
@RequestMapping("/datasets")
@Controller
public class DatasetController implements UiController {

    private static Logger log = Logger.getLogger(DatasetController.class);

    @Value("${handle.metadataRequired}")
    boolean metadataRequired;

    @Autowired
    HandleService handleService;

    @Autowired
    ProxyService proxyService; 

    @Autowired
    ValidationHelper validationHelper;

    @Autowired
    SchemaService schemaService; 


    @InitBinder
    void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(byte[].class, new ByteArrayMultipartFileEditor());
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public String show(@PathVariable("id") Long id, Model model) throws SecurityException {
        Dataset dataset = Dataset.findDataset(id);
        SecurityUtils.checkDatasetOwnership(dataset);
        model.addAttribute("dataset", dataset);
        List<Media> medias = Media.findMediasByDataset(dataset).getResultList();
        model.addAttribute("medias", medias);
        List<Metadata> metadatas = Metadata.findMetadatasByDataset(dataset).getResultList();
        model.addAttribute("metadatas", metadatas);
        try {
            Metadata metadata = metadatas.get(0);
            model.addAttribute("metadata", metadata);
            byte[] xml = metadata.getXml();
            model.addAttribute("prettyxml", Utils.formatXML(xml));
	    byte[] dif= metadata.getDif();
            model.addAttribute("prettydif",Utils.formatXML(dif));
        } catch (Exception e) {
        }
        model.addAttribute("resolvedUrl", resolveDoi(dataset));
        model.addAttribute("itemId", id);
        return "datasets/show";
    }

    private String resolveDoi(Dataset dataset) {
        try {
            String url = handleService.resolve(dataset.getDoi());
            return url;
        } catch (NotFoundException e) {
            return "not resolveable";
        } catch (HandleException e) {
            return "handle error";
        }
    }

    @ModelAttribute("datacentres")
    public Collection<Datacentre> populateDatacentres() throws SecurityException {
        if (SecurityUtils.isLoggedInAsDatacentre()) {
            Datacentre datacentre = SecurityUtils.getCurrentDatacentre();
            return Arrays.asList(datacentre);
        } else {
            Allocator allocator = SecurityUtils.getCurrentAllocator();
            return Datacentre.findAllDatacentresByAllocator(allocator);
        }
    }

    @RequestMapping(method = RequestMethod.GET)
    public String list(@RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size, Model model) throws SecurityException {
        AllocatorOrDatacentre user = SecurityUtils.getCurrentAllocatorOrDatacentre();
        int sizeNo = size == null ? LIST_DEFAULT_SIZE : Math.min(size.intValue(), LIST_MAX_SIZE);
        model.addAttribute(
                "datasets",
                Dataset.findDatasetEntriesByAllocatorOrDatacentre(user, page == null ? 0 : (page.intValue() - 1)
                        * sizeNo, sizeNo));
        float nrOfPages = (float) Dataset.countDatasetsByAllocatorOrDatacentre(user) / sizeNo;
        model.addAttribute("maxPages", (int) ((nrOfPages > (int) nrOfPages || nrOfPages == 0.0) ? nrOfPages + 1
                : nrOfPages));
        model.addAttribute("size", sizeNo);
        return "datasets/list";
    }

    @RequestMapping(params = "form", method = RequestMethod.GET)
    public String createForm(Model uiModel) {
        uiModel.addAttribute("createDatasetModel", new CreateDatasetModel());
        return "datasets/create";
    }

    @RequestMapping(method = RequestMethod.POST)
    public String create(@Valid CreateDatasetModel createDatasetModel, BindingResult result, Model model) throws SecurityException {
        Dataset dataset = modelToDataset(createDatasetModel, result);
        SecurityUtils.checkDatasetOwnership(dataset);
        Metadata metadata = modelToMetadata(createDatasetModel, dataset, result);

        checkQuota(dataset, result);

			try{
				if (!ArrayUtils.isEmpty(metadata.getXml()))
					proxyService.metaUpdate(metadata.getXml());
		   }catch (ProxyException e){
		             result.addError(new FieldError("", "doi", e.toString()));
			}

        if (!result.hasErrors()) {
            try {
                handleService.create(dataset.getDoi(), dataset.getUrl());
                if (dataset.getMinted() == null)
                    dataset.setMinted(new Date());
                log.info(dataset.getDatacentre().getSymbol() + " successfuly minted (via UI) " + dataset.getDoi());
            } catch (HandleException e) {
                log.debug("minting DOI failed; try to update it");
                try {
                    handleService.update(dataset.getDoi(), dataset.getUrl());
                    log.info(dataset.getDatacentre().getSymbol() + " successfuly updated (via UI) " + dataset.getDoi());
                } catch (HandleException ee) {
                    String message = "HandleService: " + ee.getMessage();
                    FieldError error = new FieldError("", "doi", dataset.getDoi(), false, null, null, message);
                    result.addError(error);
                }
            }
        }

        if (result.hasErrors()) {
            model.addAttribute("createDatasetModel", createDatasetModel);
            return "datasets/create";
        }

        dataset.persist();
        dataset.getDatacentre().incQuotaUsed(Datacentre.ForceRefresh.YES);
        boolean hasMetadata = !ArrayUtils.isEmpty(metadata.getXml());
        if (hasMetadata)
            metadata.persist();
        model.asMap().clear();
        return "redirect:/datasets/" + dataset.getId().toString();
    }
    
    private Dataset modelToDataset(CreateDatasetModel createDatasetModel, BindingResult result) {
        Dataset dataset = new Dataset();
        dataset.setDoi(createDatasetModel.getDoi());
        dataset.setDatacentre(createDatasetModel.getDatacentre());
        dataset.setUrl(createDatasetModel.getUrl());
        validationHelper.validateTo(result, dataset);
        return dataset;
    }
    
    private Metadata modelToMetadata(CreateDatasetModel createDatasetModel, Dataset dataset, BindingResult result) {
        Metadata metadata = new Metadata();
        metadata.setDataset(dataset);

        handleUploadedXml(createDatasetModel);
        
        try {

                byte[] xml = createDatasetModel.getXml();
                byte[] dif = createDatasetModel.getDif();
                byte[] iso = createDatasetModel.getIso();

            boolean hasMetadata = !ArrayUtils.isEmpty(xml) || !ArrayUtils.isEmpty(dif) || !ArrayUtils.isEmpty(iso) ;
            if (hasMetadata) {				

					metadata.setDif(dif);
					metadata.setIso(iso);

					if (ArrayUtils.isEmpty(xml)){
						if (!ArrayUtils.isEmpty(iso) && schemaService.isIsoSchema(iso)){
							byte[] datacite=schemaService.convertDifToDatacite(iso, dataset.getDoi());
									metadata.setXml(datacite);
									metadata.setIsConvertedByMds(true);
						}else if (!ArrayUtils.isEmpty(dif) && schemaService.isDifSchema(dif)){
								  byte[] datacite=schemaService.convertDifToDatacite(dif, dataset.getDoi());
	 							   metadata.setXml(datacite);
									metadata.setIsConvertedByMds(true);
						}
					}else{
						metadata.setXml(xml);
					}
		

				   validationHelper.validateTo(result, metadata);
            } else if (metadataRequired)
                result.rejectValue("xml", "org.datacite.mds.ui.model.CreateDatasetModel.xml.required");
        } catch (ValidationException e) {
            result.rejectValue("xml", null, e.getMessage());
        }
        catch (SchemaConvertException e){
             String error="Can not convert Schema to DataCite "+e.getMessage();
             result.addError(new FieldError("", "xml", error));
        }
        return metadata;
    }
    
    private void handleUploadedXml(CreateDatasetModel createDatasetModel) {
        byte[] xmlUpload = createDatasetModel.getXmlUpload();
        boolean isFileUploaded = !ArrayUtils.isEmpty(xmlUpload);
        if (isFileUploaded)
            createDatasetModel.setXml(xmlUpload);
    }
    
    private void checkQuota(Dataset dataset, BindingResult result) { 
        try {
            SecurityUtils.checkQuota(dataset.getDatacentre());
        } catch (SecurityException e) {
            ObjectError error = new ObjectError("", e.getMessage());
            result.addError(error);
        }
    }

    @RequestMapping(method = RequestMethod.PUT)
    public String update(@Valid Dataset dataset, BindingResult result, Model model) throws SecurityException {
        checkBeforeMerge(dataset);
        if (!dataset.getUrl().isEmpty() && !result.hasErrors()) {
            try {
                handleService.update(dataset.getDoi(), dataset.getUrl());
                if (dataset.getMinted() == null) //update via proxy - create() and update() are the same functions
							dataset.setMinted(new Date());
                log.info(dataset.getDatacentre().getSymbol() + " successfuly updated (via UI) " + dataset.getDoi());
            } catch (HandleException e) {
                log.debug("updating DOI failed; try to mint it");
                try {
                    handleService.create(dataset.getDoi(), dataset.getUrl());
                    if (dataset.getMinted() == null)
                        dataset.setMinted(new Date());
                    log.info(dataset.getDatacentre().getSymbol() + " successfuly minted (via UI) " + dataset.getDoi());
                } catch (HandleException e1) {
                    ObjectError error = new ObjectError("", "HandleService: " + e.getMessage());
                    result.addError(error);
                }
            }
        }

        if (!result.hasErrors()) {
		try{

			//if dataset exists (!=null) and the dataset gets activated - do it.

			Dataset olddataset=Dataset.findDatasetByDoi(dataset.getDoi()); 
			if (olddataset!=null && !olddataset.getIsActive().equals(Boolean.TRUE) && dataset.getIsActive().equals(Boolean.FALSE)){

				dataset.setIsActive(true);
				dataset.merge();				//Update of URL succeeded - save the current state
				dataset.setIsActive(false);
				proxyService.metaDelete(dataset.getDoi());
			}
		}catch (ProxyException e){
		    result.addError(new FieldError("", "url", e.toString()));
		}catch (NotFoundException e){
		    result.addError(new FieldError("", "url", e.toString()));
		}
	}


        if (result.hasErrors()) {
            model.addAttribute("dataset", dataset);
            return "datasets/update";
        }
        dataset.merge();
        model.asMap().clear();
        return "redirect:/datasets/" + dataset.getId().toString();
    }
    
    private void checkBeforeMerge(Dataset dataset) throws SecurityException {
        Dataset origDataset = Dataset.findDataset(dataset.getId());
        if (!StringUtils.equals(origDataset.getDoi(), dataset.getDoi()))
            throw new SecurityException("changing DOI is not allowed");
        SecurityUtils.checkDatasetOwnership(origDataset);
    }

    @RequestMapping(params = "find=ByDoiEquals", method = RequestMethod.GET)
    public String findDatasetsByDoiEquals(@RequestParam("doi") String doi, Model model) {
        Dataset dataset = Dataset.findDatasetByDoi(doi);
        model.asMap().clear();
        return (dataset == null) ? "datasets/show" : "redirect:/datasets/" + dataset.getId();
    }

    @RequestMapping(value = "/{id}", params = "form", method = RequestMethod.GET)
    public String updateForm(@PathVariable("id") Long id, Model model) throws SecurityException {
        Dataset dataset = Dataset.findDataset(id);
        SecurityUtils.checkDatasetOwnership(dataset);
        model.addAttribute("dataset", dataset);
        model.addAttribute("resolvedUrl", resolveDoi(dataset));
        return "datasets/update";
    }
}
