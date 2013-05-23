package org.datacite.mds.web.ui.controller;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.datacite.mds.service.SchemaService;
import org.datacite.mds.service.ProxyService;

import org.apache.commons.lang.ArrayUtils;
import org.datacite.mds.domain.Datacentre;
import org.datacite.mds.domain.Dataset;
import org.datacite.mds.domain.Metadata;
import org.datacite.mds.service.HandleException;
import org.datacite.mds.service.HandleService;
import org.datacite.mds.test.TestUtils;
import org.datacite.mds.util.Utils;
import org.datacite.mds.validation.ValidationHelper;
import org.datacite.mds.web.ui.model.CreateMetadataModel;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/META-INF/spring/applicationContext.xml")
@Transactional
public class MetadataControllerTest {
    
	 Datacentre datacentre;

	 Dataset dataset;
    
    MetadataController controller;

    @Autowired
    ValidationHelper validationHelper;

    @Autowired
    SchemaService schemaService;
    @Autowired
    ProxyService proxyService;
    
    
    CreateMetadataModel createMetadataModel;
    BindingResult result;
    Model model;
    
    String doi = "10.5072/TEST";
    String url = "http://example.com";
    byte[] xml;
    byte[] xml2;
    byte[] dif;
    byte[] iso;
    
    @Before
    public void init() {
        controller = new MetadataController();
        controller.validationHelper = validationHelper;
		  controller.schemaService = schemaService;
		  controller.proxyService = proxyService;
        
        
        datacentre = TestUtils.createDefaultDatacentre("10.5072");
        TestUtils.login(datacentre);

        dataset = TestUtils.createDataset(doi, datacentre);
        dataset.setIsActive(true);
        dataset.persist();


        xml = TestUtils.setDoiOfMetadata(TestUtils.getTestMetadata20(), doi);
        xml2 = TestUtils.setDoiOfMetadata(TestUtils.getTestMetadata21(), doi);
		  dif=TestUtils.getTestMetadataDif();
		  iso=TestUtils.getTestMetadataIso();
        assertTrue(!ArrayUtils.isEquals(xml, xml2));

        createMetadataModel = new CreateMetadataModel();
        createMetadataModel.setDataset(dataset);
        createMetadataModel.setXml(xml);

        result = new BeanPropertyBindingResult(createMetadataModel, "createMetadataModel");
        model = new ExtendedModelMap();
    }

    
    @Test
    public void create() throws Exception {
        assertCreateSuccess();
        assertEquals(1, Metadata.countMetadatas());
        Metadata metadata = Metadata.findAllMetadatas().get(0);
        assertArrayEquals(xml, metadata.getXml());
    }
    
    @Test
    public void createUploadMetadata() throws Exception {
        createMetadataModel.setXml(xml2);
        assertCreateSuccess();
        assertEquals(1, Metadata.countMetadatas());
        Metadata metadata = Metadata.findAllMetadatas().get(0);
        assertArrayEquals(xml2, metadata.getXml());
    }
    
    @Test
    public void createWithoutMetadata() throws Exception {
        createMetadataModel.setXml(null);
        assertCreateFailure();
        assertEquals(0, Metadata.countMetadatas());
    }

    @Test
    public void createBadXml() {
        createMetadataModel.setXml("foo".getBytes());
        assertCreateFailure();
    }
    

    public void assertCreateSuccess() throws HandleException {
        String view = controller.create(createMetadataModel, result, model);
        assertTrue(view.startsWith("redirect"));
        assertEquals(1, Metadata.countMetadatas());
    }

    public void assertCreateFailure() {
        String view = controller.create(createMetadataModel, result, model);
        assertEquals("metadatas/create", view);
        assertEquals(1, Dataset.countDatasets());
        assertEquals(0, Metadata.countMetadatas());
    }   

    @Test
    public void assertCreateDifSuccess() throws HandleException {
		  createMetadataModel.setDif(dif);
        String view = controller.create(createMetadataModel, result, model);
        assertTrue(view.startsWith("redirect"));
        assertEquals(1, Metadata.countMetadatas());
        Metadata metadata = Metadata.findAllMetadatas().get(0);
		  assertArrayEquals(dif, metadata.getDif());
    }
    @Test
    public void assertCreateIsoSuccess() throws HandleException {
		  createMetadataModel.setIso(iso);
        String view = controller.create(createMetadataModel, result, model);
        assertTrue(view.startsWith("redirect"));
        assertEquals(1, Dataset.countDatasets());
        Metadata metadata = Metadata.findAllMetadatas().get(0);
		  assertArrayEquals(iso, metadata.getIso());
    }
    @Test
    public void assertCreateDataciteAndDifSuccess() throws HandleException {
		  createMetadataModel.setXml(xml);
		  createMetadataModel.setDif(dif);
        String view = controller.create(createMetadataModel, result, model);
        assertTrue(view.startsWith("redirect"));
        assertEquals(1, Dataset.countDatasets());
        Metadata metadata = Metadata.findAllMetadatas().get(0);
        assertArrayEquals(xml, metadata.getXml());
        assertArrayEquals(dif, metadata.getDif());
    }
    @Test
    public void assertCreateDataciteAndIsoSuccess() throws HandleException {
		  createMetadataModel.setXml(xml);
		  createMetadataModel.setIso(iso);
        String view = controller.create(createMetadataModel, result, model);
        assertTrue(view.startsWith("redirect"));
        assertEquals(1, Dataset.countDatasets());
        Metadata metadata = Metadata.findAllMetadatas().get(0);
        assertArrayEquals(xml, metadata.getXml());
		  assertArrayEquals(iso, metadata.getIso());
    }
    @Test
    public void assertCreateDifAndIsoSuccess() throws HandleException {
		  createMetadataModel.setDif(dif);
		  createMetadataModel.setIso(iso);
        String view = controller.create(createMetadataModel, result, model);
        assertTrue(view.startsWith("redirect"));
        assertEquals(1, Dataset.countDatasets());
        Metadata metadata = Metadata.findAllMetadatas().get(0);
        assertArrayEquals(dif, metadata.getDif());
		  assertArrayEquals(iso, metadata.getIso());
    }
    @Test
    public void assertCreateDataciteAndDifAndIsoSuccess() throws HandleException {
		  createMetadataModel.setXml(xml);
		  createMetadataModel.setDif(dif);
		  createMetadataModel.setIso(iso);
        String view = controller.create(createMetadataModel, result, model);
        assertTrue(view.startsWith("redirect"));
        assertEquals(1, Dataset.countDatasets());
        Metadata metadata = Metadata.findAllMetadatas().get(0);
        assertArrayEquals(xml, metadata.getXml());
        assertArrayEquals(dif, metadata.getDif());
		  assertArrayEquals(iso, metadata.getIso());
    }


    

}
