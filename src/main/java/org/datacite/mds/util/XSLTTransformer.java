package org.datacite.mds.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.validation.ConstraintViolationException;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;


import org.apache.log4j.Logger;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


public class XSLTTransformer {
	

//	Logger log = Logger.getLogger(this.getClass());


	@Transactional(propagation = Propagation.NEVER)
	public static byte [] convert(String xsltPath, byte[] orig, String doi) throws IOException, TransformerException {



		Resource xslt = new ClassPathResource(xsltPath);
		Source xsltSource;
		xsltSource = new StreamSource(xslt.getInputStream());
		TransformerFactory tFactory = TransformerFactory.newInstance();

		XSLTErrorListener conversionmessages=new XSLTErrorListener();
		XSLTErrorListener factorymessages=new XSLTErrorListener();


		tFactory.setErrorListener(factorymessages);

		Transformer transformer = tFactory.newTransformer(xsltSource);

		((net.sf.saxon.Controller)transformer).setMessageEmitter(new XSLTMessageWarner());//read xsl:message - output

		transformer.setErrorListener(conversionmessages); 

		transformer.setParameter("doi", doi);

		ByteArrayInputStream input = new ByteArrayInputStream(orig);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try{
			transformer.transform(new StreamSource(input),new StreamResult(output));
		}catch (TransformerException e){
			//exception is thrown when it should not
		}

		if (factorymessages.isError())
			throw new TransformerException(factorymessages.toString());
		if (conversionmessages.isError())
			throw new TransformerException(conversionmessages.toString());
        
		return output.toByteArray(); 


	}

}
