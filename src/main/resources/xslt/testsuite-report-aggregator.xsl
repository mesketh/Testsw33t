<?xml version="1.0"?>


<!--  
Expects:

<results-aggregator>
  <result-file>TEST-GWA-TestSuite.xml</result-file>
</results-aggregator>

Outputs: 

<aggregated-testsuites> ... </aggregated-testsuites>

 -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:template match="/">
        <xsl:element name="aggregated-testsuites">
            <xsl:apply-templates select="results-aggregator/result-file"/>
        </xsl:element>
    </xsl:template>
    <xsl:template match="result-file">
        <xsl:copy-of select="document(.)"/>
    </xsl:template>
</xsl:stylesheet>