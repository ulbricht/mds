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
import org.datacite.mds.web.ui.model.CreateDatasetModel;
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
public class DatasetControllerTest {
    
    Datacentre datacentre;
    
    DatasetController controller;

    @Autowired
    ValidationHelper validationHelper;

    @Autowired
    SchemaService schemaService;
    @Autowired
    ProxyService proxyService;
    
    private HandleService mockHandleService;
    
    CreateDatasetModel createDatasetModel;
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
        controller = new DatasetController();
        controller.validationHelper = validationHelper;
        controller.metadataRequired = false;
		  controller.schemaService = schemaService;
		  controller.proxyService = proxyService;
        
        mockHandleService = EasyMock.createMock(HandleService.class);
        controller.handleService = mockHandleService;
        expectNoHandleServiceCall();
        
        datacentre = TestUtils.createDefaultDatacentre("10.5072");
        TestUtils.login(datacentre);

        xml = TestUtils.setDoiOfMetadata(TestUtils.getTestMetadata30(), doi);
        xml2 = TestUtils.setDoiOfMetadata(TestUtils.getTestMetadata21(), doi);
		  dif=TestUtils.getTestMetadataDif();
		  iso=TestUtils.getTestMetadataIso();
        assertTrue(!ArrayUtils.isEquals(xml, xml2));

        createDatasetModel = new CreateDatasetModel();
        createDatasetModel.setDatacentre(datacentre);
        createDatasetModel.setDoi(doi);
        createDatasetModel.setUrl(url);
        createDatasetModel.setXml(xml);

        result = new BeanPropertyBindingResult(createDatasetModel, "createDatasetModel");
        model = new ExtendedModelMap();
    }

    @After
    public void verify() {
        EasyMock.verify(mockHandleService);
    }

    private void expectNoHandleServiceCall() {
        EasyMock.reset(mockHandleService);
        EasyMock.replay(mockHandleService);
    }

    private void expectHandleServiceCreate(String doi, String url) throws HandleException {
        EasyMock.reset(mockHandleService);
        mockHandleService.create(doi, url);
        EasyMock.replay(mockHandleService);
    }
    
    private void expectHandleServiceCreateAfterUpdate(String doi, String url) throws HandleException {
        EasyMock.reset(mockHandleService);
        mockHandleService.update(doi, url);
        EasyMock.expectLastCall().andThrow(new HandleException("handle exists"));
        mockHandleService.create(doi, url);
        EasyMock.replay(mockHandleService);
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
        createDatasetModel.setXml(xml2);
        assertCreateSuccess();
        assertEquals(1, Metadata.countMetadatas());
        Metadata metadata = Metadata.findAllMetadatas().get(0);
        assertArrayEquals(xml2, metadata.getXml());
    }
    
    @Test
    public void createWithoutMetadata() throws Exception {
        createDatasetModel.setXml(null);
        assertCreateSuccess();
        assertEquals(0, Metadata.countMetadatas());
    }

    @Test
    public void createMetadataRequired() throws Exception {
        controller.metadataRequired = true;
        createDatasetModel.setXml(null);
        assertCreateFailure();
    }

    @Test
    public void createDoiMalformed() {
        createDatasetModel.setDoi("foobar");
        assertCreateFailure();
    }

    @Test
    public void createUrlMalformed() {
        createDatasetModel.setUrl("foobar");
        assertCreateFailure();
    }

    @Test
    public void createBadXml() {
        createDatasetModel.setXml("foo".getBytes());
        assertCreateFailure();
    }
    
    @Test
    public void createTestHandlingOfXmlUploaded() {
        createDatasetModel.setDoi(null); // force failure
        createDatasetModel.setXmlUpload(xml2);
        assertArrayEquals(xml, createDatasetModel.getXml());
        assertCreateFailure();
        assertArrayEquals(xml2, createDatasetModel.getXml());
    }

    public void assertCreateSuccess() throws HandleException {
        expectHandleServiceCreate(doi, url);
        String view = controller.create(createDatasetModel, result, model);
        assertTrue(view.startsWith("redirect"));
        assertEquals(1, Dataset.countDatasets());
    }

    @Test
    public void assertCreateDifSuccess() throws HandleException {
        expectHandleServiceCreate(doi, url);
		  createDatasetModel.setDif(dif);
        String view = controller.create(createDatasetModel, result, model);
        assertTrue(view.startsWith("redirect"));
        assertEquals(1, Dataset.countDatasets());
        Metadata metadata = Metadata.findAllMetadatas().get(0);
		  assertArrayEquals(dif, metadata.getDif());
    }
    @Test
    public void assertCreateIsoSuccess() throws HandleException {
        expectHandleServiceCreate(doi, url);
		  createDatasetModel.setIso(iso);
        String view = controller.create(createDatasetModel, result, model);
        assertTrue(view.startsWith("redirect"));
        assertEquals(1, Dataset.countDatasets());
        Metadata metadata = Metadata.findAllMetadatas().get(0);
		  assertArrayEquals(iso, metadata.getIso());
    }
    @Test
    public void assertCreateDataciteAndDifSuccess() throws HandleException {
        expectHandleServiceCreate(doi, url);
		  createDatasetModel.setXmlUpload(xml);
		  createDatasetModel.setDif(dif);
        String view = controller.create(createDatasetModel, result, model);
        assertTrue(view.startsWith("redirect"));
        assertEquals(1, Dataset.countDatasets());
        Metadata metadata = Metadata.findAllMetadatas().get(0);
        assertArrayEquals(xml, metadata.getXml());
		  assertArrayEquals(dif, metadata.getDif());
    }
    @Test
    public void assertCreateDataciteAndIsoSuccess() throws HandleException {
        expectHandleServiceCreate(doi, url);
		  createDatasetModel.setXmlUpload(xml);
		  createDatasetModel.setIso(iso);
        String view = controller.create(createDatasetModel, result, model);
        assertTrue(view.startsWith("redirect"));
        assertEquals(1, Dataset.countDatasets());
        Metadata metadata = Metadata.findAllMetadatas().get(0);
        assertArrayEquals(xml, metadata.getXml());
		  assertArrayEquals(iso, metadata.getIso());
    }
    @Test
    public void assertCreateDifAndIsoSuccess() throws HandleException {
        expectHandleServiceCreate(doi, url);
		  createDatasetModel.setDif(dif);
		  createDatasetModel.setIso(iso);
        String view = controller.create(createDatasetModel, result, model);
        assertTrue(view.startsWith("redirect"));
        assertEquals(1, Dataset.countDatasets());
        Metadata metadata = Metadata.findAllMetadatas().get(0);
        assertArrayEquals(dif, metadata.getDif());
		  assertArrayEquals(iso, metadata.getIso());
    }
    @Test
    public void assertCreateDataciteAndDifAndIsoSuccess() throws HandleException {
        expectHandleServiceCreate(doi, url);
		  createDatasetModel.setXmlUpload(xml);
		  createDatasetModel.setDif(dif);
		  createDatasetModel.setIso(iso);
        String view = controller.create(createDatasetModel, result, model);
        assertTrue(view.startsWith("redirect"));
        assertEquals(1, Dataset.countDatasets());
        Metadata metadata = Metadata.findAllMetadatas().get(0);
        assertArrayEquals(xml, metadata.getXml());
        assertArrayEquals(dif, metadata.getDif());
		  assertArrayEquals(iso, metadata.getIso());
    }

    public void assertCreateFailure() {
        String view = controller.create(createDatasetModel, result, model);
        assertEquals("datasets/create", view);
        assertEquals(0, Dataset.countDatasets());
        assertEquals(0, Metadata.countMetadatas());
    }   
    
    @Test
    public void updateKeepMintedTimestamp() throws HandleException {
        assertCreateSuccess();
        Dataset dataset = Dataset.findDatasetByDoi(doi);
        dataset.setUrl(url + "/foo");
        Date minted = dataset.getMinted();

        expectHandleServiceCreateAfterUpdate(doi, dataset.getUrl());
        controller.update(dataset, result, model);
        
        assertEquals(minted, dataset.getMinted());
    }
    
}
