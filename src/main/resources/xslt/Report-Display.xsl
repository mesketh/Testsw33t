<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:fo="http://www.w3.org/1999/XSL/Format"
    xmlns:xs="http://www.w3.org/2001/XMLSchema">
    
    <xsl:param name="currentEnvironment" required="yes" as="xs:string"/>
    <xsl:param name="allAvailableEnvironments" required="yes" as="xs:string"/>
    
    <xsl:template match="testsuites">
        <html>
            
            <head>
                <title>Web Service Monitoring</title>
		<link rel="stylesheet" href="../styles/sheet.css" />
		<link rel="stylesheet" href="../styles/bootstrap.min.css" />
		<link rel="stylesheet" href="../styles/bootstrap-responsive.min.css" />
	  
                <script>
					var timeout;
					
					// Refresh this page every this amount of milliseconds.
					var refreshFrequency = 60000;
					
					// Reloading by default, onload.
					var isReload = true;
					
					/*
					 * Is called once the page is loaded.
					 */
					function checkReloading() {
						if (window.location.hash=="#autoreload" || isReload) {
					        timeout=setTimeout("window.location.reload();", refreshFrequency);
					        document.getElementById("reloadCB").checked=true;
					    }
					}
					
					/*
					 * Switches the autorefresh ON/OFF.
					 */
					function toggleAutoRefresh(cb) {
					    if (cb.checked) {
					        window.location.replace("#autoreload");
					        timeout=setTimeout("window.location.reload();", refreshFrequency);
					        isReload = true;
					    } else {
					        window.location.replace("#");
					        clearTimeout(timeout);
					        isReload = false;
					    }
					}
					
					window.onload=checkReloading;
			      </script>
            </head>
            
            <body class="container body-margins">
                
                <p class="timestamp">
                    generated at
                    <span>
                        &#160;
                        <xsl:for-each select="tokenize(current-dateTime(),'T')">
                            <xsl:value-of select="normalize-space(.)"/>
                            &#160;
                        </xsl:for-each>
                    </span>
                </p>                
                
                <h1>Service Monitoring</h1>
                
                
      <table style="width:100%;clear:both;">
      	<tr>
      		<td>
                <table class="summaryTable">
                    <tr>
                        <td>
                            <strong>Services:</strong>
                        </td>
                        <td>
                            <xsl:value-of select="count(/testsuits/testsuite/*[name()='testcase'])" />
                        </td>
                    </tr>
                    <tr>
                        <td>
                            <strong>Failures:</strong>
                        </td>
                        <td>
                            <xsl:value-of select="count(/testsuits/testsuite/testcase/*[name()='failure'])" />
                        </td>
                    </tr>
                </table>
      		</td>
      		<td>
                <p class="availableEnvironments">

                    <!-- Available Environments. -->
                    <xsl:for-each select="tokenize($allAvailableEnvironments,',')">                       
                        <xsl:choose>
                            <xsl:when test="normalize-space(.) eq $currentEnvironment">
                                <span><xsl:value-of select="normalize-space(.)"/></span>
                            </xsl:when>
                            <xsl:otherwise>
                                <a href="../{normalize-space(.)}/results.html"><xsl:value-of select="normalize-space(.)"/></a>
                            </xsl:otherwise>
                        </xsl:choose>
                        &#160;
                    </xsl:for-each>

                </p>			
      		</td>
      		<td>
                <a href="." class="detailedReports">detailed reports</a>
                <p class="autorefresh">
                <input type="checkbox" onclick="toggleAutoRefresh(this);" id="reloadCB" checked="checked"/> Auto Refresh (1 rpm)
                </p>      		
      		</td>
      	</tr>
      </table>
                
                <!-- The header only. -->
                <table class="suite">
                    <thead>
                        <tr>
                            <th class="serviceNameHeader">Service</th>
                            <th class="timeColumn">Time</th>
                            <th>Status</th>
                        </tr>
                    </thead>
                </table>
                
                <xsl:for-each select="testsuite">
                    
                    <p class="suiteDescription">
                        <strong>
                            <!-- Getting rid of the 'Test.' prefix. -->
                            <xsl:value-of select="substring(@name,9)" />
                        </strong>
                    </p>
                    
                    <table class="suite">
                                            
                        <xsl:for-each select="testcase">
                            <xsl:choose>
                                <xsl:when test="failure">
                                    <tr class="failedTest">
                                        <td class="serviceNameColumn">
                                            <xsl:value-of select="@name" />
                                        </td>
                                        <td class="timeColumn">
                                            <xsl:value-of select="@time" />
                                        </td>
                                        <td>
                                            <xsl:value-of select="failure" disable-output-escaping="yes" />
                                        </td>
                                    </tr>
                                </xsl:when>
                                <xsl:otherwise>
                                    <tr class="passedTest">
                                        <td class="serviceNameColumn">
                                            <xsl:value-of select="@name" />
                                        </td>
                                     
                                     <!-- Execution times lower the 'lower' limit are not highlighted.  -->
                                     <!-- Execution times higher the 'lower' limit are highlighted with yellow color.  -->
                                     <!-- Execution times higher the 'upper' limit are highlighted with red-orange color.  -->
                                    <xsl:variable name="lower" select="8" as="xs:integer"/>
                                    <xsl:variable name="upper" select="15" as="xs:integer"/>
                                    
									<xsl:choose>
									    <xsl:when test="number(@time) &gt;= $upper">
									        <td class="timeColumn warning-red">
									            <xsl:value-of select="@time"/>
									        </td>
									    </xsl:when>
									    <xsl:when test="number(@time) &gt;= $lower">
									        <td class="timeColumn warning-yellow">
									            <xsl:value-of select="@time"/>
									        </td>
									    </xsl:when>
									    <xsl:otherwise>
									        <td class="timeColumn">
									            <xsl:value-of select="@time"/>
									        </td>
									    </xsl:otherwise>
									</xsl:choose>
                                        <!--<td class="timeColumn">
                                            <xsl:value-of select="@time" />
                                        </td>-->
                                        <td>OK</td>
                                    </tr>
                                </xsl:otherwise>
                            </xsl:choose>
                        </xsl:for-each>
                    </table>
                </xsl:for-each>
            </body>
        </html>
    </xsl:template>
</xsl:stylesheet>
