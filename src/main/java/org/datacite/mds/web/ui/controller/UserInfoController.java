package org.datacite.mds.web.ui.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.datacite.mds.domain.Allocator;
import org.datacite.mds.domain.AllocatorOrDatacentre;
import org.datacite.mds.domain.Datacentre;
import org.datacite.mds.domain.Dataset;
import org.datacite.mds.domain.Prefix;
import org.datacite.mds.service.SecurityException;
import org.datacite.mds.util.SecurityUtils;
import org.datacite.mds.web.ui.UiController;
import org.datacite.mds.web.ui.model.ChangePasswordMailModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class UserInfoController implements UiController {
    Logger log = Logger.getLogger(UserInfoController.class);

    @Value("${handle.testPrefix}")
    String testPrefix;

    @RequestMapping(value = "/userinfo", method = RequestMethod.GET)
    public String userinfo(Model model) throws SecurityException {
        if (SecurityUtils.isLoggedInAsDatacentre())
            userinfoDatacentre(model);
        else if (SecurityUtils.isLoggedInAsAllocator())
            userinfoAllocator(model);

        return "userinfo";
    }

    private void userinfoDatacentre(Model model) throws SecurityException {
        Datacentre datacentre = SecurityUtils.getCurrentDatacentre();
        addDatacentreToModel(datacentre, model);
        Allocator allocator = datacentre.getAllocator();
        addAllocatorToModel(allocator, model);
        addPrefixesToModel(datacentre, model);
        addDatasetsCountsToModel(datacentre, model);
    }

    private void userinfoAllocator(Model model) throws SecurityException {
        Allocator allocator = SecurityUtils.getCurrentAllocator();
        addAllocatorToModel(allocator, model);
        addPrefixesToModel(allocator, model);
        addDatasetsCountsToModel(allocator, model);
    }

    private void addDatacentreToModel(Datacentre datacentre, Model model) {
        log.debug("userinfo for datacentre '" + datacentre.getSymbol() + "'");
        model.addAttribute("datacentre", datacentre);
    }

    private void addAllocatorToModel(Allocator allocator, Model model) {
        log.debug("userinfo for allocator '" + allocator.getSymbol() + "'");
        model.addAttribute("allocator", allocator);
    }

    private void addDatasetsCountsToModel(AllocatorOrDatacentre user, Model model) {
        long countDatasets = Dataset.countDatasetsByAllocatorOrDatacentre(user);
        long countTestDatasets = Dataset.countTestDatasetsByAllocatorOrDatacentre(user);
        long countNonTestDatasets = countDatasets - countTestDatasets;
        model.addAttribute("countDatasets", countDatasets);
        model.addAttribute("countTestDatasets", countTestDatasets);
        model.addAttribute("countNonTestDatasets", countNonTestDatasets);
    }

    private void addPrefixesToModel(Allocator allocator, Model model) {
        List<Prefix> prefixes = new ArrayList<Prefix>(allocator.getPrefixes());
        Collections.sort(prefixes);
        List<String> labels = new ArrayList<String>();
        labels.add(testPrefix + " (test prefix)");
        for (Prefix prefix : prefixes)
            if (!prefix.getPrefix().equals(testPrefix))
                labels.add(prefix.getLabelWithDatacentres());
        model.addAttribute("prefixes", labels);
    }

    private void addPrefixesToModel(Datacentre datacentre, Model model) {
        List<Prefix> prefixes = new ArrayList<Prefix>(datacentre.getPrefixes());
        Collections.sort(prefixes);
        List<String> labels = new ArrayList<String>();
        long countTest = Dataset.countTestDatasetsByAllocatorOrDatacentre(datacentre);
        labels.add(testPrefix + " (test prefix; " + countTest + " IGSNs)");
        for (Prefix prefix : prefixes)
            if (!prefix.getPrefix().equals(testPrefix)) {
                long count = Dataset.countDatasetsByAllocatorOrDatacentre(datacentre, prefix.getPrefix());
                labels.add(prefix.getPrefix() + " (" + count + " IGSNs)");
            }
        model.addAttribute("prefixes", labels);
    }

    @ModelAttribute("mail")
    private ChangePasswordMailModel populateChangePasswordMailModel() {
        ChangePasswordMailModel mailModel = new ChangePasswordMailModel();
        mailModel.setSymbol(SecurityUtils.getCurrentSymbolOrNull());
        return mailModel;
    }

}
