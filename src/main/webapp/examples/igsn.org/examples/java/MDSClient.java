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
import java.util.Scanner;
import java.io.File;
import javax.xml.bind.DatatypeConverter;
import java.net.ProtocolException;
import java.io.InputStream;
import java.io.FileReader;

public class MDSClient {
    
    
    private String user;

    private String password;

 
    private String identifierUrl="https://doidb.wdc-terra.org/igsn/igsn";

    private String metadataUrl="https://doidb.wdc-terra.org/igsn/metadata";   

   
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
     * @throws Exception
     */
    private int httpRequest(String serviceurl, byte[] body, StringBuffer retbody, String method) 
         throws Exception{

		URL url;
		HttpsURLConnection con;
		boolean geterrorstream=false;

		try{
			url=new URL (serviceurl);
			con =(HttpsURLConnection) url.openConnection();	
			con.setRequestMethod(method);
		}catch (MalformedURLException e){throw new Exception(e.getMessage());}
	 	 catch (ProtocolException e){throw new Exception(e.getMessage());}
		 catch (IOException e){throw new Exception(e.getMessage());}

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
				throw new Exception(e.getMessage());
		}
    }

	/**
	* sets the message header and copies the message body into an output stream
	* @param HttpsURLConnection con
	* @param byte[] body
	* @throws ProxyException
	*/
	private void prepareTransmission(HttpsURLConnection con,byte[] body) throws Exception{
		try{
			String dataciteuser = user + ":" + password;
			con.setRequestProperty ("Authorization", "Basic " + (DatatypeConverter.printBase64Binary(dataciteuser.getBytes())).trim());
			con.setRequestProperty ("Content-Type","text/plain;charset=UTF-8");

			if (body.length>0){
				con.setDoOutput(true);
				OutputStream out = con.getOutputStream();
				out.write(body);
				out.close();
			}
		}catch (IOException e){throw new Exception(e.getMessage());}

	}

	/**
	* reads the InputStream from the server into a StringBuffer
	* @param InputStream is
	* @param InputStream is, StringBuffer sb
	* @throws ProxyException
	*/
	private void convertInputStream(InputStream is, StringBuffer sb) throws Exception{
		try{
			BufferedReader reader=new BufferedReader(new InputStreamReader(is, DEFAULT_ENCODING));
			sb.delete(0,sb.length());
			for (String line=reader.readLine(); line !=null ; line=reader.readLine())
				sb.append(line+"\n");
			reader.close();
		}catch (IOException e) {throw new Exception(e.getMessage());}
	}

//----------------------------------------------------


   public static void main( String[] argv){




      try{

         MDSClient client=new MDSClient();
         String result=null;

         if (argv.length <4)
            client.usage();

         client.user=argv[1];
         client.password=argv[2];     


         if (argv[0].equals("resolve")){
            result=client.doiResolve(argv[3]);
         }else if (argv[0].equals("mint")){
            if (argv.length <5)
               client.usage();

            BufferedReader reader=new BufferedReader(new FileReader(argv[4]));

            String line;
            String doi=null,url=null;

            while ((line = reader.readLine()) != null){
               if (line.startsWith("url="))
                  url=line.replace("url=","");
               String identifierType=client.identifierUrl.substring(client.identifierUrl.lastIndexOf("/")+1);
               if (line.startsWith(identifierType+"="))
                  doi=line.replace(identifierType+"=","");
            } 
            
            if (!doi.equals(argv[3]))
               throw new Exception(identifiertype.toUpperCase()+" in file "+arg[4]+" must match "+identifiertype.toUpperCase()+" of the commandline.");

            client.doiUpdate(doi, url);

            result="Success";
         }else if (argv[0].equals("getmeta")){
             result=client.metaRetrieve(argv[3]);     
         }else if (argv[0].equals("updatemeta")){
            if (argv.length <5)
               client.usage();
            String content = new Scanner(new File(argv[4])).useDelimiter("\\Z").next();
            client.metaUpdate(content.getBytes());
            result="Success";
         }else if (argv[0].equals("deactivate")){
            client.metaDelete(argv[3]);
            result="Success";   
         }else {
            client.usage();
         }
         
         System.out.println(result);


      }catch (Exception e){
         System.out.println("Error: "+e.getMessage());
         e.printStackTrace();
      }

   }


    public void usage(){


      System.out.println("java MDSClient resolve user password handlevalue");
      System.out.println("java MDSClient mint user password handlevalue url.txt");
      System.out.println("java MDSClient getmeta user password handlevalue");
      System.out.println("java MDSClient updatemeta user password handlevalue file.xml");
      System.out.println("java MDSClient deactivate user password handlevalue");

      System.exit(0);
    }



    public String doiResolve(String doi) throws Exception {


		  String body=new String();
		  StringBuffer retbody=new StringBuffer();
		  int returncode = httpRequest(identifierUrl+"/"+doi,body.getBytes(DEFAULT_ENCODING),retbody, "GET");
	 	  switch (returncode){
			  case HttpsURLConnection.HTTP_OK:
				  break;
			  default:
				  throw new Exception(identifierUrl+": "+retbody.toString());

		  }
        return retbody.toString();
         
    }

    public String metaRetrieve(String doi) throws Exception {


		  String body=new String();
		  StringBuffer retbody=new StringBuffer();
		  int returncode = httpRequest(metadataUrl+"/"+doi,body.getBytes(DEFAULT_ENCODING),retbody, "GET");
	 	  switch (returncode){
			  case HttpsURLConnection.HTTP_OK:
				  break;
			  default:
				  throw new Exception(metadataUrl+": "+retbody.toString());

		  }
        return retbody.toString();
         
    }




    public void doiUpdate(String doi, String newUrl) throws Exception, IllegalArgumentException {
        if (doi==null || newUrl==null || doi.isEmpty() || newUrl.isEmpty())
            throw new IllegalArgumentException("IGSN and URL cannot be empty");

		  String body=new String("igsn="+doi+"\nurl="+newUrl);
		  StringBuffer retbody=new StringBuffer();
		  int returncode = httpRequest(identifierUrl,body.getBytes(DEFAULT_ENCODING),retbody, "POST");

	 	  switch (returncode){
			  case HttpsURLConnection.HTTP_CREATED:
				  break;
			  default:
				  throw new Exception(identifierUrl+": "+retbody.toString());

		  }

    }


   
    public void metaUpdate(byte[] xml) throws Exception{

		StringBuffer retbody=new StringBuffer();
		int returncode=httpRequest(metadataUrl,xml, retbody, "POST");
	 	switch (returncode){
			case HttpsURLConnection.HTTP_CREATED:
				break;
			default:
				throw new Exception(metadataUrl+": "+retbody.toString());
		}

    }



    public void metaDelete(String doi) throws Exception, Exception{

		StringBuffer retbody=new StringBuffer();
		int returncode=httpRequest(metadataUrl+"/"+doi,new byte[0], retbody, "DELETE");
	 	switch (returncode){
			case HttpsURLConnection.HTTP_OK:
				break;
			case HttpsURLConnection.HTTP_NOT_FOUND:
				throw new Exception("handle " + doi + " does not exist");
			default:
				throw new Exception(metadataUrl+": "+retbody.toString());
		}
    }
 
}
