package org.datacite.mds.service.impl;

import java.nio.charset.Charset;


import java.net.URL;
import javax.net.ssl.HttpsURLConnection;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.UnknownServiceException;
import java.io.UnsupportedEncodingException;
import java.io.FileNotFoundException;

import org.datacite.mds.service.ProxyException;
import org.datacite.mds.service.ProxyService;
import org.apache.commons.codec.binary.Base64;
import java.net.ProtocolException;
import java.io.InputStream;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.datacite.mds.domain.Dataset;
import org.datacite.mds.web.api.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.security.GeneralSecurityException;

@Service
public class ProxyServiceImpl implements ProxyService {
    
    private static Logger log = Logger.getLogger(ProxyServiceImpl.class);
    
    @Value("${handle.id}") private String adminId;

    @Value("${handle.password}") private String adminPassword;

    @Value("${handle.dummyMode}") boolean dummyMode;    
 
    @Value("${handle.proxyServer}") String dataciteservice;
   
    private static final Charset DEFAULT_ENCODING = Charset.forName("UTF8");
    
    
//-------------------PROXY-------------------
    /**
	  * sends the body with http-method method to serviceurl. A HTTP-Code will be returned and the
     * Answer-Body from the server will be copied to retbody. In Case of errors (connection problems, bad url ...)
	  * a ProxyException is thrown.
     * @param serviceurl the url that should be talked to
     * @param body the body to send, the received body from server
     * @param method HTTP method "PUT", "POST", "GET" ...
     * @return HTTP returncode
     * @throws ProxyException
     */
    private int httpRequest(String serviceurl, byte[] body, StringBuffer retbody, String method) 
         throws ProxyException{

		URL url;
		HttpsURLConnection con;
		boolean geterrorstream=false;


//--START FIXME  Certificate--

        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(
                    java.security.cert.X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(
                    java.security.cert.X509Certificate[] certs, String authType) {
            }
        } };
//--END FIXME Certificate--


		try{
//--START FIXME  Certificate--
		   final SSLContext sc = SSLContext.getInstance( "SSL" );
		   sc.init( null, trustAllCerts, new java.security.SecureRandom() );
  		   final SSLSocketFactory sslSocketFactory = sc.getSocketFactory();
//--END FIXME Certificate--

			url=new URL (serviceurl);
			con =(HttpsURLConnection) url.openConnection();	

//--START FIXME  Certificate--
			((HttpsURLConnection)con).setSSLSocketFactory(sslSocketFactory);
//--END FIXME Certificate--

			con.setRequestMethod(method);
		}catch (MalformedURLException e){throw new ProxyException(e.getMessage());}
	 	 catch (ProtocolException e){throw new ProxyException(e.getMessage());}
		 catch (IOException e){throw new ProxyException(e.getMessage());}
		 catch (GeneralSecurityException e){throw new ProxyException(e.getMessage());}

		prepareTransmission(con, body); 

		try{
			con.connect();
		}catch (IOException e){
			geterrorstream=true;
		}

		try{
				int responsecode=con.getResponseCode();
				if (!geterrorstream && responsecode>=200 && responsecode<300){
					convertInputStream(con.getInputStream(),retbody);
				}else{
					if (con.getErrorStream()!=null)
						convertInputStream(con.getErrorStream(),retbody);					
				}
				return responsecode;
		}catch (IOException e){
				throw new ProxyException(e.getMessage());
		}
    }

	/**
	* sets the message header and copies the message body into an output stream
	* @param HttpsURLConnection con
	* @param byte[] body
	* @throws ProxyException
	*/
	private void prepareTransmission(HttpsURLConnection con,byte[] body) throws ProxyException{
		try{
			String dataciteuser = adminId + ":" + adminPassword;
			con.setRequestProperty ("Authorization", "Basic " + StringUtils.trim(Base64.encodeBase64String(dataciteuser.getBytes())));
			con.setRequestProperty ("Content-Type","text/plain;charset=UTF-8");

			if (body.length>0){
				con.setDoOutput(true);
				OutputStream out = con.getOutputStream();
				out.write(body);
				out.close();
			}
		}catch (IOException e){throw new ProxyException(e.getMessage());}

	}

	/**
	* reads the InputStream from the server into a StringBuffer
	* @param InputStream is
	* @param InputStream is, StringBuffer sb
	* @throws ProxyException
	*/
	private void convertInputStream(InputStream is, StringBuffer sb) throws ProxyException{
		try{
			BufferedReader reader=new BufferedReader(new InputStreamReader(is, DEFAULT_ENCODING));
			sb.delete(0,sb.length());
			for (String line=reader.readLine(); line !=null ; line=reader.readLine())
				sb.append(line);
			reader.close();
		}catch (IOException e) {throw new ProxyException(e.getMessage());}
	}

//----------------------------------------------------



	 public boolean isProxyMode(){
		return StringUtils.isNotBlank(dataciteservice);
	 }


    @Override
    public String doiResolve(String doi) throws NotFoundException {

         Dataset dataset=Dataset.findDatasetByDoi(doi);
         if (dataset==null)
             throw new NotFoundException("handle " + doi + " does not exist");
         else 
             return dataset.getUrl();
         
    }


    @Override
    public void doiUpdate(String doi, String newUrl) throws ProxyException, IllegalArgumentException {
        if (StringUtils.isEmpty(doi) || StringUtils.isEmpty(newUrl))
            throw new IllegalArgumentException("IGSN and URL cannot be empty");

        if (dummyMode) {
           log.debug("response code from Proxy request: none - dummyMode on");
	        return;
   	  }

		  if (!isProxyMode()){
			  return;
		  }

		  String error;
		  try {
		    log.debug("create/update Proxy: IGSN: " + doi + " URL: " + newUrl);
			  String body=new String("igsn="+doi+"\nurl="+newUrl);
			  StringBuffer retbody=new StringBuffer();
			  int returncode = httpRequest(dataciteservice+"/igsn",body.getBytes(DEFAULT_ENCODING),retbody, "POST");

			  log.debug(String.format("response code from Proxy request: %d",returncode)+" - "+retbody.toString());

		 	  switch (returncode){
				  case HttpsURLConnection.HTTP_CREATED:
					  break;
				  default:
					  throw new ProxyException(dataciteservice+": "+retbody.toString());
//					  throw new HandleException(retbody.toString());
			  }
			  return;
		  }
		  catch (ProxyException e){
					  throw new ProxyException(e.getMessage());
//		     throw new HandleException(e.getMessage());
		  }
    }


    @Override    
    public void metaUpdate(byte[] xml) throws ProxyException{
		if (!isProxyMode() || dummyMode){
			return;
		}
		String error=new String("");		    
		StringBuffer retbody=new StringBuffer();
		int returncode=httpRequest(dataciteservice+"/metadata",xml, retbody, "POST");
	 	switch (returncode){
			case HttpsURLConnection.HTTP_CREATED:
				break;
			default:
				throw new ProxyException(dataciteservice+": "+retbody.toString());
		}
		return;                    

    }


    @Override
    public void metaDelete(String doi) throws ProxyException, NotFoundException{
		if (!isProxyMode() || dummyMode){
			return;
		}
		StringBuffer retbody=new StringBuffer();
		int returncode=httpRequest(dataciteservice+"/metadata/"+doi,new byte[0], retbody, "DELETE");
	 	switch (returncode){
			case HttpsURLConnection.HTTP_OK:
				break;
			case HttpsURLConnection.HTTP_NOT_FOUND:
				throw new NotFoundException("handle " + doi + " does not exist");
			default:
				throw new ProxyException(dataciteservice+": "+retbody.toString());
		}
    }
 
}
