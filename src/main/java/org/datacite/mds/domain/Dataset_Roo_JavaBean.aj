// WARNING: DO NOT EDIT THIS FILE. THIS FILE IS MANAGED BY SPRING ROO.
// You may push code into the target .java compilation unit if you wish to edit any member(s).

package org.datacite.mds.domain;

import java.lang.Boolean;
import java.lang.Integer;
import java.lang.String;
import java.util.Date;
import org.datacite.mds.domain.Datacentre;

privileged aspect Dataset_Roo_JavaBean {
    
    public String Dataset.getDoi() {
        return this.doi;
    }
    
    public Boolean Dataset.getIsActive() {
        return this.isActive;
    }
    
    public void Dataset.setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public Boolean Dataset.getIsRefQuality() {
        return this.isRefQuality;
    }
    
    public void Dataset.setIsRefQuality(Boolean isRefQuality) {
        this.isRefQuality = isRefQuality;
    }
    
    public Integer Dataset.getLastLandingPageStatus() {
        return this.lastLandingPageStatus;
    }
    
    public void Dataset.setLastLandingPageStatus(Integer lastLandingPageStatus) {
        this.lastLandingPageStatus = lastLandingPageStatus;
    }
    
    public Date Dataset.getLastLandingPageStatusCheck() {
        return this.lastLandingPageStatusCheck;
    }
    
    public void Dataset.setLastLandingPageStatusCheck(Date lastLandingPageStatusCheck) {
        this.lastLandingPageStatusCheck = lastLandingPageStatusCheck;
    }
    
    public String Dataset.getLastMetadataStatus() {
        return this.lastMetadataStatus;
    }
    
    public void Dataset.setLastMetadataStatus(String lastMetadataStatus) {
        this.lastMetadataStatus = lastMetadataStatus;
    }
    
    public Datacentre Dataset.getDatacentre() {
        return this.datacentre;
    }
    
    public void Dataset.setDatacentre(Datacentre datacentre) {
        this.datacentre = datacentre;
    }
    
    public String Dataset.getUrl() {
        return this.url;
    }
    
    public void Dataset.setUrl(String url) {
        this.url = url;
    }
    
    public Date Dataset.getCreated() {
        return this.created;
    }
    
    public void Dataset.setCreated(Date created) {
        this.created = created;
    }
    
    public Date Dataset.getUpdated() {
        return this.updated;
    }
    
    public void Dataset.setUpdated(Date updated) {
        this.updated = updated;
    }
    
    public Date Dataset.getMinted() {
        return this.minted;
    }
    
    public void Dataset.setMinted(Date minted) {
        this.minted = minted;
    }
    
}
