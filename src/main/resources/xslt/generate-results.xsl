<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xs="http://www.w3.org/2001/XMLSchema"
	xmlns:fn="http://www.w3.org/2005/xpath-functions">

	<xsl:template match="aggregated-testsuites">
		<html>
			<head>
				<title>
					Results of the latest suite sweep as @
					<xsl:value-of select="current-dateTime()" />
				</title>
				<link rel="stylesheet" href="styles/sheet.css" />
				<link rel="stylesheet" href="styles/bootstrap.min.css" />
				<link rel="stylesheet" href="styles/bootstrap-responsive.min.css" />
				<link rel="stylesheet"
					href="http://code.jquery.com/ui/1.10.3/themes/smoothness/jquery-ui.css" />
				<script src="http://code.jquery.com/jquery-1.10.2.min.js"></script>
				<script src="http://code.jquery.com/ui/1.10.3/jquery-ui.js"></script>

				<script>
					$(function() {
					$( "#testsuite-results-accordion" ).accordion({
					heightStyle: "content"
					});
					});

				</script>

			</head>

			<body class="container body-margins">

				<p class="timestamp">
					generated at
					<span>
						&#160;
						<xsl:variable name="dt" as="xs:dateTime" select="current-dateTime()" />
						<xsl:value-of
							select="format-dateTime($dt, '[H01]:[m01] on [D01]/[M01]/[Y0001]')" />
						&#160;
					</span>
				</p>

				<h1>Test Suite Summary</h1>


				<table style="width:100%;clear:both;">
					<tr>
						<td>
							<table class="summaryTable">
								<tr>
									<td>
										<strong>Total No Test Cases run:</strong>
									</td>
									<td>
										<xsl:value-of
											select="count(/aggregated-testsuites/testsuite/*[name()='testcase'])" />
									</td>
								</tr>
								<tr>
									<td>
										<strong>Total No Of Test Case failures:</strong>
									</td>
									<td>
										<xsl:value-of
											select="count(/aggregated-testsuites/testsuite/testcase/*[name()='failure'])" />
									</td>
								</tr>
							</table>
						</td>
					</tr>
				</table>


				<!-- The header only. -->


				<!-- (TODO include soap project name) -->
				<h2 class="suitedescription">Service Test Suite results</h2>
				<div id="testsuite-results-accordion">

					<xsl:for-each select="testsuite">

						<!-- <p class="suiteDescription"> <strong> <xsl:value-of select="substring(@name,9)" 
							/> </strong> </p> -->

						<h3>
							<xsl:value-of select="@name" />
						</h3>
						<div>
							<table class="suite">
								<thead>
									<tr>
										<th class="serviceNameHeader">Service</th>
										<th class="timeColumn">Time</th>
										<th>Status</th>
									</tr>
								</thead>
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
													<xsl:value-of select="failure"
														disable-output-escaping="yes" />
												</td>
											</tr>
										</xsl:when>
										<xsl:otherwise>
											<tr class="passedTest">
												<td class="serviceNameColumn">
													<xsl:value-of select="@name" />
												</td>

												<!-- Execution times lower the 'lower' limit are not highlighted. -->
												<!-- Execution times higher the 'lower' limit are highlighted 
													with yellow color. -->
												<!-- Execution times higher the 'upper' limit are highlighted 
													with red-orange color. -->
												<xsl:variable name="lower" select="8" as="xs:integer" />
												<xsl:variable name="upper" select="15" as="xs:integer" />

												<xsl:choose>
													<xsl:when test="number(@time) &gt;= $upper">
														<td class="timeColumn warning-red">
															<xsl:value-of select="@time" />
														</td>
													</xsl:when>
													<xsl:when test="number(@time) &gt;= $lower">
														<td class="timeColumn warning-yellow">
															<xsl:value-of select="@time" />
														</td>
													</xsl:when>
													<xsl:otherwise>
														<td class="timeColumn">
															<xsl:value-of select="@time" />
														</td>
													</xsl:otherwise>
												</xsl:choose>
												<!--<td class="timeColumn"> <xsl:value-of select="@time" /> </td> -->
												<td>OK</td>
											</tr>
										</xsl:otherwise>
									</xsl:choose>
								</xsl:for-each>
							</table>
						</div>

					</xsl:for-each>
				</div>
			</body>
		</html>
	</xsl:template>
</xsl:stylesheet>
