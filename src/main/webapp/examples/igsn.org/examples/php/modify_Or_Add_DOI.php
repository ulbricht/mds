<?php


function httprequest($requestdata){

 $ch = curl_init(); 
// curl_setopt($ch,CURLOPT_SSL_VERIFYPEER,false);
 curl_setopt($ch, CURLOPT_URL, $requestdata["url"]);
 if (isset($requestdata["header"]))
	 curl_setopt($ch, CURLOPT_HTTPHEADER, $requestdata["header"]);
 
 switch ($requestdata["method"]){
	case "GET":  
		curl_setopt($ch,CURLOPT_HTTPGET,true);
		break;
	case	"POST": 
		curl_setopt($ch,CURLOPT_POST,true);
		curl_setopt($ch,CURLOPT_POSTFIELDS,$requestdata["payload"]);
		break;
	case "DELETE":
		curl_setopt($ch, CURLOPT_CUSTOMREQUEST, "DELETE");
		break;
 }

 curl_setopt($ch, CURLOPT_RETURNTRANSFER, true); 

 $result["content"]= curl_exec($ch);
 $result["errcode"]= curl_errno($ch);
 $result["errmsg"] = curl_error($ch) ;
 $result["header"] = curl_getinfo($ch);
 $result['http_code'] = curl_getinfo($ch,CURLINFO_HTTP_CODE);
 curl_close($ch);
 return $result;
}




$baseurl="https://doidb.wdc-terra.org/igsn/";
$igsn="10273/TEST/TESTGFZ2";
$newdoi="10.5072/TEST3";
$username="USERNAME";
$password="SECRET";


//echo "\n\n***GET IGSN Metadata***\n";

//GET METADATA
$getmetadata["url"]=$baseurl."metadata/".$igsn;
$getmetadata["method"]="GET";
$getmetadata["header"]=array("Accept: application/xml","Authorization: Basic " . base64_encode($username.":".$password));

$result=httprequest($getmetadata);

if ($result["http_code"] !=200)
	die ("Error: Server ".$baseurl." returned \"".$result["http_code"]." ".$result["content"]."\". No XML to modify was returned!\n");


//modify XML
$doc=DOMDocument::loadXML($result["content"]);
$xpath=new DOMXPath($doc);
$domnodelist=$xpath->query('//*[local-name()="relatedIdentifier" and @relatedIdentifierType="doi"]');
switch ($domnodelist->length){
	case 0:
		if ($xpath->query('//*[local-name()="relatedResourceIdentifiers"]')->length == 0){
			$domelem=$doc->createElementNS($dom->documentURI,"relatedResourceIdentifiers");
			$logelem=$xpath->query('//*[local-name()="log"]')->item(0);
			$doc->documentElement->insertBefore($domelem,$logelem);
		}
		$relatedresources=$xpath->query('//*[local-name()="relatedResourceIdentifiers"]')->item(0);
		$domelem=$doc->createElementNS($dom->documentURI,"relatedIdentifier");
		$domelem->setAttributeNS($dom->documentURI,"relatedIdentifierType","doi");
		$domelem->setAttributeNS($dom->documentURI,"relationType","IsCitedBy");
		$domelem->nodeValue=$newdoi;
		$relatedresources->appendChild($domelem);
		break;
	case 1:
		$domnodelist->item(0)->nodeValue=$newdoi;
		break;
	default:
		echo ("Error: IGSN has more than one associated DOI - unable to modify");

}



//echo "\n\n***DEPOSIT IGSN Metadata***\n";

//POST METADATA
$postmetadata["url"]=$baseurl."metadata";
$postmetadata["method"]="POST";
$postmetadata["header"]=array("Content-type: application/xml;charset=UTF-8","Authorization: Basic " . base64_encode($username.":".$password));
$postmetadata["payload"]=$doc->saveXML();
$result=httprequest($postmetadata);

if ($result["http_code"] != 201)
	die ("Error: Server ".$baseurl." returned \"".$result["http_code"]." ".$result["content"]."\". Unable to upload XML!\n");

echo "successfully modified DOI\n";




 ?>
