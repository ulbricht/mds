<?xml version="1.0" encoding="UTF8" ?>
<xsl:stylesheet version="1.0" 
	xmlns="http://datacite.org/schema/kernel-3" 
 	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xmlns:igsnaa="http://doidb.wdc-terra.org/igsnaa"
	xmlns:date="http://exslt.org/dates-and-times"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
	exclude-result-prefixes="xsl date igsnaa"
>
	<xsl:output method="xml" media-type="text/xml" version="1.0" encoding="UTF8" indent="yes"/>
	<xsl:param name="doi" select="'UNRETRIEVEABLE_DOI'"/>


	<!--for completely wrong files -->
	<xsl:template match="/*[(local-name()!='samples' or namespace-uri()!='http://doidb.wdc-terra.org/igsnaa') ]">
		<xsl:message terminate="yes">This stylesheet only works for IGSN metadata files</xsl:message>
	</xsl:template>

   <xsl:template match="/igsnaa:samples">

		<xsl:variable name="registrant" >GFZ Potsdam</xsl:variable>
		<xsl:variable name="timestamp" ><xsl:value-of select="date:date-time()"/></xsl:variable>
		<xsl:variable name="igsn" select="//igsnaa:igsn"/>		
		<xsl:variable name="event" >submitted</xsl:variable>

		<sample xmlns="http://igsn.org/schema/kernel-v.1.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://igsn.org/schema/kernel-v.1.0 http://doidb.wdc-terra.org/igsn/schemas/igsn.org/schema/1.0/igsn.xsd">
		<sampleNumber identifierType="igsn">10273/<xsl:value-of select="$igsn"/></sampleNumber>
		<registrant><registrantName><xsl:value-of select="$registrant"/></registrantName></registrant>
		<log><logElement><xsl:attribute name="event"><xsl:value-of select="$event"/></xsl:attribute><xsl:attribute name="timeStamp"><xsl:value-of select="$timestamp"/></xsl:attribute></logElement></log></sample>

    </xsl:template> 


</xsl:stylesheet>
