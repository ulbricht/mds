<?xml version="1.0" encoding="UTF8" ?>
<xsl:stylesheet version="1.0" 
	xmlns="http://datacite.org/schema/kernel-3" 
 	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
	xmlns:exsl="http://exslt.org/common"
	exclude-result-prefixes="xsl exsl"
>
	<xsl:output method="xml" media-type="text/xml" version="1.0" encoding="UTF8" indent="yes"/>
	<xsl:param name="doi" select="'UNRETRIEVEABLE_DOI'"/>


	<!--for completely wrong files -->
	<xsl:template match="/*[(local-name()!='DIF' or namespace-uri()!='http://gcmd.gsfc.nasa.gov/Aboutus/xml/dif/') and (local-name()!='MD_Metadata' and namespace-uri()!='http://www.isotc211.org/2005/gmd')]">
		<xsl:message terminate="yes">This stylesheet only works for DIF or ISO files</xsl:message>
	</xsl:template>



   <xsl:template match="/*[local-name()='MD_Metadata' and namespace-uri()='http://www.isotc211.org/2005/gmd']">

		
		<xsl:variable name="authors"  select="*[local-name()='identificationInfo']/*[local-name()='MD_DataIdentification']/*[local-name()='citation']/*[local-name()='CI_Citation']/*[local-name()='citedResponsibleParty']/*[local-name()='CI_ResponsibleParty']/*[local-name()='role']/*[local-name()='CI_RoleCode' and normalize-space()= 'author']"/>

		<xsl:variable name="publicationYear" >
			<xsl:for-each select="*[local-name()='identificationInfo']/*[local-name()='MD_DataIdentification']/*[local-name()='citation']/*[local-name()='CI_Citation']/*[local-name()='date']/*[local-name()='CI_Date']/*[local-name()='dateType']/*[local-name()='CI_DateTypeCode' and normalize-space()='publication']">
				<xsl:value-of select="../../*[local-name()='date']/*[local-name()='Date']" />
			</xsl:for-each>
		</xsl:variable>

		<xsl:variable name="publisher" >
			<xsl:for-each select="*[local-name()='identificationInfo']/*[local-name()='MD_DataIdentification']/*[local-name()='citation']/*[local-name()='CI_Citation']/*[local-name()='citedResponsibleParty']/*[local-name()='CI_ResponsibleParty']/*[local-name()='role']/*[local-name()='CI_RoleCode' and normalize-space()= 'publisher']">
				<xsl:value-of select="../../*[local-name()='organisationName']/*[local-name()='CharacterString']" />
			</xsl:for-each>
		</xsl:variable>

		<xsl:variable name="keywords" select="*[local-name()='identificationInfo']/*[local-name()='MD_DataIdentification']/*[local-name()='descriptiveKeywords']/*[local-name()='MD_Keywords']/*[local-name()='keyword']/*[local-name()='CharacterString']"/>

		<xsl:variable name="title" select="*[local-name()='identificationInfo']/*[local-name()='MD_DataIdentification']/*[local-name()='citation']/*[local-name()='CI_Citation']/*[local-name()='title']"/>
		<xsl:variable name="summary" select="*[local-name()='identificationInfo']/*[local-name()='MD_DataIdentification']/*[local-name()='abstract']/*[local-name()='CharacterString']"/>


      <xsl:variable name="north" select="//*[local-name()='northBoundLatitude']/*[local-name()='Decimal']"/>
      <xsl:variable name="south" select="//*[local-name()='southBoundLatitude']/*[local-name()='Decimal']"/>
      <xsl:variable name="west" select="//*[local-name()='westBoundLongitude']/*[local-name()='Decimal']"/>
      <xsl:variable name="east" select="//*[local-name()='eastBoundLongitude']/*[local-name()='Decimal']"/>

<!-- diverse checks -->
		<xsl:if test="$doi='UNRETRIEVEABLE_DOI'">
				<xsl:message terminate="yes">DOI is missing</xsl:message>
		</xsl:if>

		<xsl:if test="normalize-space($authors) = ''">
				<xsl:message terminate="yes">"MD_Metadata/identificationInfo/MD_DataIdentification/pointOfContact/CI_ResponsibleParty/individualName" - author is missing</xsl:message>
		</xsl:if>
		
		<xsl:if test="normalize-space($title) = ''">
				<xsl:message terminate="yes">"MD_Metadata/identificationInfo/MD_DataIdentification/citation/CI_Citation/title" - title is missing</xsl:message>
		</xsl:if>

		<xsl:if test="normalize-space($publicationYear) = ''">
				<xsl:message terminate="yes">"MD_Metadata/identificationInfo/MD_DataIdentification/citation/CI_Citation/date/Date" - publication date is missing</xsl:message>
		</xsl:if>

		<xsl:if test="translate(substring($publicationYear,1,4),'0123456789','') != ''">
				<xsl:message terminate="yes">"MD_Metadata/identificationInfo/MD_DataIdentification/citation/CI_Citation/date/Date" - publication date not in YYYY-MM-DD format</xsl:message>
		</xsl:if>

		<xsl:if test="normalize-space($publisher) = ''">
				<xsl:message terminate="yes">"MD_Metadata/identificationInfo/MD_DataIdentification/pointOfContact/CI_ResponsibleParty" - no entity with role "publisher"</xsl:message>
		</xsl:if>

<!-- Konvertierung -->
		<resource  xsi:schemaLocation="http://datacite.org/schema/kernel-3 http://schema.datacite.org/meta/kernel-3/metadata.xsd">
			<identifier identifierType="DOI" ><xsl:value-of select="$doi"/></identifier>
			<creators>	
			 	<xsl:for-each select="$authors">
					<creator><creatorName>
						<xsl:value-of select="../../*[local-name()='individualName']/*[local-name()='CharacterString']" />
					</creatorName></creator>
				</xsl:for-each>
			</creators>

			<titles><title><xsl:value-of select="normalize-space($title)"/></title></titles>

			<publisher><xsl:value-of select="normalize-space($publisher)"/></publisher>

			<publicationYear><xsl:value-of select="normalize-space(substring($publicationYear,1,4))" /></publicationYear>
			<xsl:if test="$keywords != ''">
          	<subjects>
                <xsl:for-each select="$keywords">
                     <subject subjectScheme="keyword"><xsl:value-of select="normalize-space()"/></subject>
                </xsl:for-each>
            </subjects>
			</xsl:if>
			<xsl:if test="$summary != ''">
				<descriptions>
					<description descriptionType="Abstract" ><xsl:value-of select="normalize-space($summary)"/></description>
				</descriptions>
			</xsl:if>

      <xsl:if test="$north != '' and $south != '' and $east != '' and $west != ''">
         <geoLocations>
            <geoLocation>
               <xsl:choose>
                  <xsl:when test="$north = $south and $east = $west">
                     <geoLocationPoint><xsl:value-of select="$south"/><xsl:text> </xsl:text><xsl:value-of select="$west"/></geoLocationPoint>
                  </xsl:when>
                  <xsl:otherwise>
                     <geoLocationBox><xsl:value-of select="$south"/><xsl:text> </xsl:text><xsl:value-of select="$west"/><xsl:text> </xsl:text><xsl:value-of select="$north"/><xsl:text> </xsl:text><xsl:value-of select="$east"/></geoLocationBox>
                  </xsl:otherwise>
               </xsl:choose>
            </geoLocation>
         </geoLocations>
			</xsl:if>
		</resource>
      </xsl:template> 


	<xsl:template match="/*[local-name()='DIF' and namespace-uri()='http://gcmd.gsfc.nasa.gov/Aboutus/xml/dif/']">

		<xsl:variable name="authors" select="*[local-name()='Data_Set_Citation']/*[local-name()='Dataset_Creator']" />
		<xsl:variable name="publicationYear" select="*[local-name()='Data_Set_Citation']/*[local-name()='Dataset_Release_Date' and normalize-space()!='']"/>
		<xsl:variable name="publisher" select="*[local-name()='Data_Set_Citation']/*[local-name()='Dataset_Publisher']" />
		<xsl:variable name="title" select="*[local-name()='Data_Set_Citation']/*[local-name()='Dataset_Title']"/>
		<xsl:variable name="difentryid" select="*[local-name()='Entry_ID']"/>
		<xsl:variable name="sizes" select="*[local-name()='Distribution']/*[local-name()='Distribution_Size' and normalize-space()!='']" /> 
		<xsl:variable name="formats" select="*[local-name()='Distribution']/*[local-name()='Distribution_Format' and normalize-space()!='']" /> 
		<xsl:variable name="summary" select="*[local-name()='Summary']"/> 
		<xsl:variable name="projects" select="*[local-name()='Project' and normalize-space()!='']"/>
		<xsl:variable name="sensors" select="*[local-name()='Sensor_Name' and normalize-space()!='']"/>
		<xsl:variable name="sources" select="*[local-name()='Source_Name' and normalize-space()!='']"/>
		<xsl:variable name="keywords" select="*[local-name()='Keyword' and normalize-space()!='']"/>
		<xsl:variable name="investigators" select="*[local-name()='Personnel']/*[local-name()='Role' and (contains(normalize-space(),'nvestigat') or 
							contains(normalize-space(),'NVESTIGAT'))]/.."/>
		<xsl:variable name="techcontacts" select="*[local-name()='Personnel']/*[local-name()='Role' and (contains(normalize-space(),'echnical Contact') or 
							contains(normalize-space(),'chnical contact') or contains(normalize-space(),'TECHNICAL CONTACT')) ]/.."/>
		<xsl:variable name="references" select="*[local-name()='Reference']/*[local-name()='DOI' or local-name()='Online_Resource' or local-name()='ISBN']/.."/>
      
      <xsl:variable name="spatial" select="//*[local-name()='Spatial_Coverage']"/>


<!-- diverse checks -->
		<xsl:if test="$doi='UNRETRIEVEABLE_DOI'">
				<xsl:message terminate="yes">DOI is missing</xsl:message>
		</xsl:if>

		<xsl:if test="normalize-space($authors) = ''">
				<xsl:message terminate="yes">"DIF/Data_Set_Citation/Dataset_Creator" must be "lastname1, firstname1; lastname2, firstname2" or "lastname, firstname" or "lastname"</xsl:message>
		</xsl:if>
		
		<xsl:if test="normalize-space($title) = ''">
				<xsl:message terminate="yes">"DIF/Data_Set_Citation/Dataset_Title" must not be empty</xsl:message>
		</xsl:if>

		<xsl:if test="translate(substring($publicationYear,1,4),'0123456789','') != ''">
				<xsl:message terminate="yes">"DIF/Data_Set_Citation/Dataset_Release_Date" first 4 digits must be release year</xsl:message>
		</xsl:if>

		<xsl:if test="normalize-space($publisher) = ''">
				<xsl:message terminate="yes">"DIF/Data_Set_Citation/Dataset_Publisher" must not be empty"</xsl:message>
		</xsl:if>



<!-- Konvertierung -->
		<resource  xsi:schemaLocation="http://datacite.org/schema/kernel-3 http://schema.datacite.org/meta/kernel-3/metadata.xsd">
			<identifier identifierType="DOI" ><xsl:value-of select="$doi"/></identifier>
			<creators>	 	
				<xsl:call-template name="listauthors">					
					<xsl:with-param name="authors" select="concat(normalize-space($authors), ';')" /> 
				</xsl:call-template>
			</creators>

			<titles><title><xsl:value-of select="$title"/></title></titles>

			<publisher><xsl:value-of select="normalize-space($publisher)"/></publisher>

			<publicationYear><xsl:value-of select="substring($publicationYear,1,4)" /></publicationYear>

			<xsl:if test="$projects != '' or $sensors != '' or $sources != '' or $keywords != ''">
		                <subjects>
		                <xsl:for-each select="$projects">
		                        <subject subjectScheme="project"><xsl:call-template name="printLongShort"/></subject>
		                </xsl:for-each>

		                <xsl:for-each select="$sensors">
		                        <subject subjectScheme="sensor"><xsl:call-template name="printLongShort"/></subject>
		                </xsl:for-each>

		                <xsl:for-each select="$sources">
		                        <subject subjectScheme="platform"><xsl:call-template name="printLongShort"/></subject>
		                </xsl:for-each>

		                <xsl:for-each select="$keywords">
		                        <subject subjectScheme="keyword"><xsl:value-of select="normalize-space()"/></subject>
		                </xsl:for-each>
		                </subjects>
			</xsl:if>

         <xsl:if test="$investigators != '' or $techcontacts != ''">             
             <contributors>
                    <xsl:for-each select="$investigators">
                        <contributor contributorType="ProjectLeader">
                            <contributorName>
                                <xsl:call-template name="personneltoname"/> 
                            </contributorName>
                        </contributor>
                 	  </xsl:for-each>
                    <xsl:for-each select="$techcontacts">                                  
								<contributor contributorType="ContactPerson">
									<contributorName>
						             <xsl:call-template name="personneltoname"/>
									</contributorName>
								</contributor>
                    </xsl:for-each>
             </contributors>
         </xsl:if>

			<language>eng</language>
			<resourceType resourceTypeGeneral="Dataset" />
			<alternateIdentifiers>
				<alternateIdentifier alternateIdentifierType="DIF:Entry_ID"><xsl:value-of select="$difentryid"/></alternateIdentifier>
			</alternateIdentifiers>

			<xsl:if test="$sizes != ''">
				<sizes>			
				<xsl:for-each  select="$sizes" > 	
					<size><xsl:value-of select="normalize-space()"/></size>
		                </xsl:for-each>			
				</sizes>
			</xsl:if>

			<xsl:if test="$formats != ''">
				<formats>			
				<xsl:for-each  select="$formats" > 	
					<format><xsl:value-of select="normalize-space()"/></format>
		                </xsl:for-each>			
				</formats>
			</xsl:if>

			<descriptions>
				<description descriptionType="Abstract" ><xsl:value-of select="$summary"/></description>
			</descriptions>

         <xsl:if test="normalize-space($spatial)!=''">
            <geoLocations>


            <xsl:for-each select="$spatial">
               <geoLocation>
                  <xsl:choose>
                     <xsl:when test="./*[local-name()='Northernmost_Latitude'] = ./*[local-name()='Southernmost_Latitude'] and ./*[local-name()='Easternmost_Longitude'] = ./*[local-name()='Westernmost_Longitude']">
                        <geoLocationPoint><xsl:value-of select="./*[local-name()='Southernmost_Latitude']"/><xsl:text> </xsl:text><xsl:value-of select="./*[local-name()='Westernmost_Longitude']"/></geoLocationPoint>
                     </xsl:when>
                     <xsl:otherwise>
                        <geoLocationBox><xsl:value-of select="./*[local-name()='Southernmost_Latitude']"/><xsl:text> </xsl:text><xsl:value-of select="./*[local-name()='Westernmost_Longitude']"/><xsl:text> </xsl:text><xsl:value-of select="./*[local-name()='Northernmost_Latitude']"/><xsl:text> </xsl:text><xsl:value-of select="./*[local-name()='Easternmost_Longitude']"/></geoLocationBox>
                     </xsl:otherwise>
                  </xsl:choose>
               </geoLocation>
      		</xsl:for-each>

            </geoLocations>
		   </xsl:if> 
		</resource> 
	</xsl:template>

<!-- utilities -->
	<xsl:template name="personneltoname">
		<xsl:variable name="firstname" select="normalize-space(*[local-name()='First_Name'])"/>
		<xsl:variable name="middlename" select="normalize-space(*[local-name()='Middle_Name'])"/>
		<xsl:variable name="lastname" select="normalize-space(*[local-name()='Last_Name'])"/>      
                <xsl:value-of select="$lastname"/>
                <xsl:if test="$firstname!=''">
                        <xsl:text>, </xsl:text> <xsl:value-of select="$firstname"/>
                        <xsl:if test="$middlename!=''">
                                <xsl:text> </xsl:text><xsl:value-of select="$middlename"/>
                        </xsl:if>
                </xsl:if>
	</xsl:template>

	<xsl:template name="listauthors">
	    <xsl:param name="authors" /> 
	    <xsl:variable name="author" select="normalize-space(substring-before($authors, ';'))" /> 
	    <xsl:variable name="remaining" select="substring-after($authors, ';')" /> 
	    <xsl:if test="$author!=''">
		<creator><creatorName><xsl:value-of select="$author"/></creatorName></creator>
	    </xsl:if>
	    <xsl:if test="$remaining">
		<xsl:call-template name="listauthors">
		        <xsl:with-param name="authors" select="$remaining" /> 
		</xsl:call-template>
	    </xsl:if>
	</xsl:template>

       
   <xsl:template name="printLongShort">
                <xsl:variable name="longname" select="normalize-space(*[local-name()='Long_Name'])"/>
                <xsl:variable name="shortname" select="normalize-space(*[local-name()='Short_Name'])"/>         
		<xsl:value-of select="$shortname"/>
                <xsl:if test="$longname!='' and $shortname!=''"><xsl:text>: </xsl:text></xsl:if>
		<xsl:value-of select="$longname"/>
	</xsl:template>

</xsl:stylesheet>
