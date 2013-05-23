package org.datacite.mds.web.ui.model;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.datacite.mds.domain.Dataset;
import org.hibernate.validator.constraints.NotEmpty;

import org.datacite.mds.validation.constraints.ValidDIForNULL;
import org.datacite.mds.validation.constraints.ValidISOorNULL;
import org.datacite.mds.validation.constraints.ValidXMLorNULL;

public class CreateMetadataModel {
   
    @NotNull
    private Dataset dataset;

	 @ValidXMLorNULL
    private byte[] xml;

    @ValidISOorNULL
    private byte[] iso;
	 
	 @ValidDIForNULL
    private byte[] dif;

    public Dataset getDataset() {
        return dataset;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }

    public byte[] getXml() {
        return xml;
    }

    public void setXml(byte[] xml) {
        this.xml = xml;
    }
   
    public byte[] getIso() {
        return iso;
    }

    public void setIso(byte[] iso) {
        this.iso = iso;
    }
    public byte[] getDif() {
        return dif;
    }

    public void setDif(byte[] dif) {
        this.dif = dif;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    
}
