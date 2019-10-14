<?xml version="1.0" encoding="UTF8" ?>
<xsl:stylesheet version="1.0" 
 	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xmlns:description="http://schema.igsn.org/description/1.0"
	xmlns:date="http://exslt.org/dates-and-times"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
	exclude-result-prefixes="xsl date"
>
	<xsl:output method="xml" media-type="text/xml" version="1.0" encoding="UTF-8" indent="yes"/>
	<xsl:param name="doi" select="'UNRETRIEVEABLE_DOI'"/>


	<!--for completely wrong files -->
	<xsl:template match="/*[(local-name()!='resource' or namespace-uri()!='http://schema.igsn.org/description/1.0') ]">
		<xsl:message terminate="yes">This stylesheet only works for IGSN metadata files</xsl:message>
	</xsl:template>

   <xsl:template match="/description:resource">

		<xsl:variable name="registrant" select="//description:registrant/description:name"/>
		<xsl:variable name="timestamp" ><xsl:value-of select="date:date-time()"/></xsl:variable>
		<xsl:variable name="igsn" select="//description:identifier"/>		
		<xsl:variable name="event" >submitted</xsl:variable>

		<sample xmlns="http://igsn.org/schema/kernel-v.1.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://igsn.org/schema/kernel-v.1.0 http://doidb.wdc-terra.org/igsn/schemas/igsn.org/schema/1.0/igsn.xsd">
		<sampleNumber identifierType="igsn">10273/<xsl:value-of select="$igsn"/></sampleNumber>
		<registrant><registrantName><xsl:value-of select="$registrant"/></registrantName></registrant>
		
		<xsl:if test="normalize-space(//description:parentIdentifier) != '' ">
		
		<relatedResourceIdentifiers>

			<relatedIdentifier relatedIdentifierType="handle" relationType="IsPartOf" >10273/<xsl:value-of select="//description:parentIdentifier"/></relatedIdentifier> 

		</relatedResourceIdentifiers>		
		</xsl:if>


		<log><logElement><xsl:attribute name="event"><xsl:value-of select="$event"/></xsl:attribute><xsl:attribute name="timeStamp"><xsl:value-of select="$timestamp"/></xsl:attribute></logElement></log></sample>

    </xsl:template> 


</xsl:stylesheet>


