package org.datacite.mds.service.impl;

import java.nio.charset.Charset;

import net.handle.hdllib.AbstractMessage;
import net.handle.hdllib.AbstractResponse;
import net.handle.hdllib.AdminRecord;
import net.handle.hdllib.AuthenticationInfo;
import net.handle.hdllib.CreateHandleRequest;
import net.handle.hdllib.Encoder;
import net.handle.hdllib.HandleResolver;
import net.handle.hdllib.HandleValue;
import net.handle.hdllib.ModifyValueRequest;
import net.handle.hdllib.SecretKeyAuthenticationInfo;

import org.springframework.beans.factory.annotation.Autowired;

import org.datacite.mds.service.ProxyService;
import org.datacite.mds.service.ProxyException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.datacite.mds.domain.Dataset;
import org.datacite.mds.service.HandleException;
import org.datacite.mds.service.HandleService;
import org.datacite.mds.web.api.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.PrivateKey;
import net.handle.hdllib.PublicKeyAuthenticationInfo;
import net.handle.hdllib.Util;
import org.springframework.core.io.ClassPathResource;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
//import java.net.InetAddress;
//import java.net.UnknownHostException;


@Service
public class HandleServiceImpl implements HandleService {
    
    
    @Autowired
    ProxyService proxyService; 
    
    @Value("${handle.index}") private int adminIndex;

    @Value("${handle.id}") private String adminId;

    @Value("${handle.password}") private String adminPassword;

    @Value("${handle.traceMessages}") private boolean traceMessages;

    @Value("${handle.dummyMode}") boolean dummyMode;    
    
    private static final int URL_RECORD_INDEX = 1;

    private static final int ADMIN_RECORD_INDEX = 100;

    private static final Charset DEFAULT_ENCODING = Charset.forName("UTF8");

    static final Logger log4j = Logger.getLogger(HandleServiceImpl.class);

    HandleResolver resolver = new HandleResolver();


    @Override
    public String resolve(String doi) throws HandleException, NotFoundException {


	//if (proxyService.isProxyMode()){
	    return proxyService.doiResolve(doi); //get from DB not from Handle-service      
	//}else{
	//    return handleResolve(doi);      
	//}

    }

    @Override
    public void create(String doi, String url) throws HandleException {
	try{

		if (proxyService.isProxyMode()){
		    proxyService.doiUpdate( doi, url);      
		}else{
		    handleCreate( doi, url);         
		}
        }catch (ProxyException e){
		throw new HandleException(e.getMessage());
	}
    }

    @Override
    public void update(String doi, String newUrl) throws HandleException {
	try{

		if (proxyService.isProxyMode()){
		    proxyService.doiUpdate( doi, newUrl);       
		}else{
		    handleUpdate( doi, newUrl);
		}
        }catch (ProxyException e){
		throw new HandleException(e.getMessage());
	}
    }
    
    

    
//-------------------HANDLE-------------------    

    private String handleResolve(String doi) throws HandleException, NotFoundException {
        if (dummyMode)
            return "dummyMode";
        
        try {
            String[] types = { "URL" };
            HandleValue[] values = resolver.resolveHandle(doi, types, null);
            if (values.length == 0)
                throw new NotFoundException("no url found for handle " + doi);
            return values[0].getDataAsString();
        } catch (net.handle.hdllib.HandleException e) {
            if (e.getCode() == net.handle.hdllib.HandleException.HANDLE_DOES_NOT_EXIST) {
                throw new NotFoundException("handle " + doi + " does not exist");
            } else {
                String message = "tried to resolve handle " + doi + " but failed: " + e.getMessage();
                log4j.warn(message);
                throw new HandleException(message, e);
            }
        }
    }

    private void handleCreate(String doi, String url) throws HandleException {
        if (StringUtils.isEmpty(doi) || StringUtils.isEmpty(url))
            throw new IllegalArgumentException("IGSN and URL cannot be empty");

        resolver.traceMessages = traceMessages;
        int timestamp = (int) (System.currentTimeMillis() / 1000);
        try {
            log4j.debug("creating Handle: IGSN: " + doi + " URL: " + url);

            AdminRecord admin = new AdminRecord(adminId.getBytes(DEFAULT_ENCODING), adminIndex, true, true, true, true,
                    true, true, true, true, true, true, true, true);

            HandleValue[] val = {
                    new HandleValue(ADMIN_RECORD_INDEX, "HS_ADMIN".getBytes(DEFAULT_ENCODING),
                            Encoder.encodeAdminRecord(admin), HandleValue.TTL_TYPE_RELATIVE, 86400, timestamp, null,
                            true, true, true, false),

                    new HandleValue(URL_RECORD_INDEX, "URL".getBytes(DEFAULT_ENCODING), url.getBytes(DEFAULT_ENCODING),
                            HandleValue.TTL_TYPE_RELATIVE, 86400, timestamp, null, true, true, true, false) };

/*            AuthenticationInfo authInfo = new SecretKeyAuthenticationInfo(adminId.getBytes(DEFAULT_ENCODING),
                    adminIndex, adminPassword.getBytes(DEFAULT_ENCODING));
*/
				PrivateKey privKey=loadPrivateKeyFromFile();

				AuthenticationInfo authInfo= new PublicKeyAuthenticationInfo(adminId.getBytes(DEFAULT_ENCODING),
                    adminIndex,privKey);

            CreateHandleRequest req = new CreateHandleRequest(doi.getBytes(DEFAULT_ENCODING), val, authInfo);

            if (!dummyMode) {

                AbstractResponse response = resolver.processRequest(req);
	//				AbstractResponse response=resolver.sendHdlTcpRequest(req,InetAddress.getByName(serverName),port);

                String msg = AbstractMessage.getResponseCodeMessage(response.responseCode);
                log4j.debug("response code from Handle request: " + msg);

                if (response.responseCode != AbstractMessage.RC_SUCCESS) {
                    throw new HandleException(msg);
                }
            } else {
                log4j.debug("response code from Handle request: none - dummyMode on");
            }
        } catch (net.handle.hdllib.HandleException e) {
            String message = "tried to register handle " + doi + " but failed: [" + e.getCode() + "] " + e.getMessage();
            log4j.error(message);
            throw new HandleException(message, e);
        }//catch (UnknownHostException e){}
    }

    private void handleUpdate(String doi, String newUrl) throws HandleException {
        if (StringUtils.isEmpty(doi) || StringUtils.isEmpty(newUrl))
            throw new IllegalArgumentException("IGSN and URL cannot be empty");

        log4j.debug("update Handle: IGSN: " + doi + " URL: " + newUrl);

        resolver.traceMessages = traceMessages;
        int timestamp = (int) (System.currentTimeMillis() / 1000);
        
        try {
            HandleValue[] val = {
                    new HandleValue(URL_RECORD_INDEX, "URL".getBytes(DEFAULT_ENCODING), 
                            newUrl.getBytes(DEFAULT_ENCODING),
                            HandleValue.TTL_TYPE_RELATIVE, 86400, timestamp, null, true, true, true, false) };

/*            AuthenticationInfo authInfo = new SecretKeyAuthenticationInfo(adminId.getBytes(DEFAULT_ENCODING),
                    adminIndex, adminPassword.getBytes(DEFAULT_ENCODING));
*/
				PrivateKey privKey=loadPrivateKeyFromFile();

				AuthenticationInfo authInfo= new PublicKeyAuthenticationInfo(adminId.getBytes(DEFAULT_ENCODING),
                    adminIndex,privKey);

            ModifyValueRequest req = new ModifyValueRequest(doi.getBytes(DEFAULT_ENCODING), val, authInfo);

            if (!dummyMode) {
                AbstractResponse response = resolver.processRequest(req);
					//AbstractResponse response=resolver.sendHdlTcpRequest(req,InetAddress.getByName(serverName),port);
                String msg = AbstractMessage.getResponseCodeMessage(response.responseCode);

                log4j.debug("response code from Handle request: " + msg);

                if (response.responseCode != AbstractMessage.RC_SUCCESS) {
                    throw new HandleException(msg);
                }
            } else {
                log4j.debug("response code from Handle request: none - dummyMode on");
            }
        } catch (net.handle.hdllib.HandleException e) {
            String message = "tried to update handle " + doi + " but failed: [" + e.getCode() + "] " + e.getMessage();
            throw new HandleException(message, e);
        }//catch (UnknownHostException e){}
    }


	private PrivateKey loadPrivateKeyFromFile() {
	    
     try {

		ClassPathResource keyfile=new ClassPathResource("privkey/admpriv.bin");
	
	 	 InputStream fin = keyfile.getInputStream();  
       ByteArrayOutputStream bout = new ByteArrayOutputStream();

       byte buf[] = new byte[256];
       int r = 0;
       while((r=fin.read(buf))>=0){
         bout.write(buf, 0, r);
       }
       buf = bout.toByteArray();
       
       byte passphrase[] = null;
       
       buf = Util.decrypt(buf, passphrase);
       
       return Util.getPrivateKeyFromBytes(buf, 0);
     } catch (Exception e) {
       e.printStackTrace(System.err);
     }
 
   return null;
 }
    
}
