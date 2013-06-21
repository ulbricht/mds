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
$igsn="10273/TEST/TESTGFZ21";
$username="USERNAME";
$password="SECRET";


echo "\n\n***Register/Update IGSN***\n";

//POST IGSN
$mintdata["url"]=$baseurl."igsn";
$mintdata["method"]="POST";
$mintdata["header"]=array("Content-type: application/xml;charset=UTF-8","Accept: application/xml","Authorization: Basic " . base64_encode($username.":".$password));
$mintdata["payload"]="igsn=".$igsn."\n"."url=http://www.gfz-potsdam.de/test";
$result=httprequest($mintdata);
var_dump($result["http_code"]);
var_dump($result["content"]);

 
echo "\n\n***GET IGSN***\n";

//GET IGSN
$getigsn["url"]=$baseurl."igsn/".$igsn;
$getigsn["method"]="GET";
$getigsn["header"]=array("Content-type: application/xml;charset=UTF-8","Accept: application/xml","Authorization: Basic " . base64_encode($username.":".$password));
$result=httprequest($getigsn);
var_dump($result["http_code"]);
var_dump($result["content"]);



echo "\n\n***DEPOSIT IGSN Metadata***\n";

//POST METADATA
$postmetadata["url"]=$baseurl."metadata";
$postmetadata["method"]="POST";
$postmetadata["header"]=array("Content-type: application/xml;charset=UTF-8","Accept: application/xml","Authorization: Basic " . base64_encode($username.":".$password));
$postmetadata["payload"]=
'<sample xmlns="http://igsn.org/schema/kernel-v.1.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://igsn.org/schema/kernel-v.1.0 http://doidb.wdc-terra.org/igsn/schemas/igsn.org/schema/1.0/igsn.xsd">

	<sampleNumber identifierType="igsn">'.$igsn.'</sampleNumber>

	<registrant>
		<registrantName>Damian Ulbricht</registrantName>
 		<nameIdentifier nameIdentifierScheme="orcid">Test</nameIdentifier>
	</registrant>

	<relatedResourceIdentifiers>
		<relatedIdentifier relatedIdentifierType="doi" relationType="IsCitedBy">10.5072/GFZTEST/TESTTESTTST</relatedIdentifier>	
		<relatedIdentifier relatedIdentifierType="handle" relationType="IsReferencedBy">10.5072/GFZTEST2/TESTTEST</relatedIdentifier>
	</relatedResourceIdentifiers>

	<log>
		<logElement event="registered" timeStamp="2002-09-24T20:00:00" comment="comment">This is an optional text</logElement>
	</log>

</sample>';
$result=httprequest($postmetadata);
var_dump($result["http_code"]);
var_dump($result["content"]);

echo "\n\n***GET IGSN Metadata***\n";

//GET METADATA
$getmetadata["url"]=$baseurl."metadata/".$igsn;
$getmetadata["method"]="GET";
$getmetadata["header"]=array("Content-type: application/xml;charset=UTF-8","Accept: application/xml","Authorization: Basic " . base64_encode($username.":".$password));
$result=httprequest($getmetadata);
var_dump($result["http_code"]);
var_dump($result["content"]);



echo "\n\n***SET IGSN Metadata inactive***\n";

//DELETE METADATA
$deletemetadata["url"]=$getmetadata["url"]=$baseurl."metadata/".$igsn;
$deletemetadata["method"]="DELETE";
$deletemetadata["header"]=array("Content-type: application/xml;charset=UTF-8","Accept: application/xml","Authorization: Basic " . base64_encode($username.":".$password));
$result=httprequest($deletemetadata);
var_dump($result["http_code"]);
var_dump($result["content"]);

 ?>
