package au.com.schmick.sm.soapui;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.lang.ProcessBuilder.Redirect;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.ConfigurationUtils;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import com.thoughtworks.xstream.converters.basic.AbstractSingleValueConverter;

/**
 * Copyright - Mark Hesketh ☻ 2013.
 * 
 * Utility to assist in automated testing of web services that have existing
 * test suites via SOAP UI. Builds on the value of these unit tests by providing
 * a tool to run and parameterise them (via properties files) say via Jenkins or
 * some other context. The results of runing the tests are then transformed and
 * presented via html suitable for publishing as links in Jenkins builds further
 * augmenting and building value in a CI setup.
 * 
 * Runs test suites via SOAP UI headless. Performs property expansion e.g.
 * 
 * <pre>
 * ${myprop}
 * </pre>
 * 
 * in your test suites prior to running their tests. Driven off
 * runner.properties (root of classpath) which defines base properties for
 * runner as well as any default properties used in across any test suite. Test
 * suites requiring custom overrides of default properties should provide them
 * via naming convention:
 * 
 * <pre>
 * {Test Suite Name}.properties
 * </pre>
 * 
 * .
 * 
 * <p>
 * TODO Transform results via XSLT to html for display. Doco - add a README,
 * example SOAP UI project, tidy and polish, release under LGPL/Apache/CDDL?,
 * Support system property override of the internal runner.properties, find
 * workaround to invasive change to testrunner.bat in SOAPUI(!)
 * 
 * @since November 2013
 * @author Mark Hesketh
 * @version 1.0.0b
 */
public class SOAPUITestSuiteRunner {

	private final static String RUNNER_CFG_PROP = "runner.properties";

	Logger logger = Logger.getLogger(SOAPUITestSuiteDescriptor.class);

	Configuration toolCfg = null;

	@XStreamAlias("result-file")
	class SOAPUITestSuiteDescriptor {

		@XStreamOmitField
		private String suiteName;

		@XStreamOmitField
		private Configuration cfg;

		public SOAPUITestSuiteDescriptor(String suiteName, Configuration cfg) {
			this.suiteName = suiteName;
			this.cfg = cfg;
		}

		private String getSuiteName() {
			return suiteName;
		}

		public String getProjectPath() {
			return cfg.getString("soapui.project");
		}

		public String getReportDir() {
			return cfg.getString("soapui.report.dir");
		}

		public String toCommandLine() {
			StringBuilder builder = new StringBuilder(
					cfg.getString("soapui.testrunner.path"));
			builder.insert(0, "\"").append("\\testrunner.bat\" ").append(" -s")
					.append(this.suiteName).append(" -r -a -j -I ")
					.append("-f").append(getReportDir()).append(" ")
					.append(getProjectPath());

			return builder.toString();
		}

		/**
		 * Get the file path to a local properties file containing this test
		 * suites' required properties for expansion.
		 * 
		 * @return The file path
		 */
		public String getSuiteOverridesFile() {

			File suitePropsFile = null;
			// dump a copy of the configuration to run the suite against...
			try {
				suitePropsFile = File.createTempFile(getSuiteName(),
						".properties");
				suitePropsFile.deleteOnExit();
				FileWriter fw = new FileWriter(suitePropsFile);
				PrintWriter pw = new PrintWriter(fw);
				ConfigurationUtils.dump(this.cfg, pw);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return suitePropsFile.getAbsolutePath();
		}

	}

	@XStreamAlias("results-aggregator")
	public static class SOAPUITestSuiteDescriptorAggregator {

		private Collection<SOAPUITestSuiteDescriptor> suiteDescriptors;

		public SOAPUITestSuiteDescriptorAggregator(
				Collection<SOAPUITestSuiteDescriptor> suiteDescriptors) {
			this.suiteDescriptors = suiteDescriptors;
		}
	}

	/**
	 * Custom converter for the {@link SOAPUITestSuiteDescriptorConverter} for
	 * use in aggregation of test suites results.
	 * 
	 * @author Mark
	 * 
	 */
	static class SOAPUITestSuiteDescriptorConverter extends
			AbstractSingleValueConverter {

		@Override
		public boolean canConvert(Class type) {
			return SOAPUITestSuiteDescriptor.class.equals(type);
		}

		@Override
		public String toString(Object source) {
			return String.format("TEST-%s.xml",
					((SOAPUITestSuiteDescriptor) source).getSuiteName());
		}

		@Override
		public Object fromString(String arg0) {
			throw new UnsupportedOperationException("Not implemented");
		}
	}

	// runs each test suite after resolving all properties for it
	private Map<String, SOAPUITestSuiteDescriptor> assembleTestSuites(
			String soapSuitesFile) throws ConfigurationException {

		PropertiesConfiguration defaultCfg = new PropertiesConfiguration(
				soapSuitesFile);

		// extract the core tool configuration for reuse later
		this.toolCfg = defaultCfg.subset("soapui");

		StringTokenizer st = new StringTokenizer(
				this.toolCfg.getString("test-suites"), ";");

		Map<String, SOAPUITestSuiteDescriptor> suiteDescriptors = new HashMap<String, SOAPUITestSuiteDescriptor>();

		// iterate over all suites specified with a combined configuration
		do {
			String nextSuite = st.nextToken();
			Configuration runnerCfg = createRunnerConfig(defaultCfg, nextSuite);

			SOAPUITestSuiteDescriptor nextDesc = new SOAPUITestSuiteDescriptor(
					nextSuite, runnerCfg);
			suiteDescriptors.put(nextSuite, nextDesc);
		} while (st.hasMoreTokens());

		return suiteDescriptors;

	}

	// combines the default and current suite configurations ready for dumping
	// to properties prior to running soap ui
	private Configuration createRunnerConfig(Configuration defaultCfg,
			String suiteCfgName) throws ConfigurationException {

		PropertiesConfiguration suiteCfg = new PropertiesConfiguration(
				suiteCfgName + ".properties");

		CompositeConfiguration combinedCfg = new CompositeConfiguration();
		combinedCfg.addConfiguration(suiteCfg);
		combinedCfg.addConfiguration(defaultCfg);

		return combinedCfg;
	}

	private void run() throws ConfigurationException, IOException,
			TransformerException {

		File testSuiteResultsFile = toAggregateResults(runSuites(RUNNER_CFG_PROP));

		transformResultsForDisplay(testSuiteResultsFile);
	}

	private void transformResultsForDisplay(File testSuiteResultsFile)
			throws TransformerException {

		TransformerFactory _factory = TransformerFactory.newInstance();

		Transformer resultsTransformer = _factory
				.newTransformer(new StreamSource(getClass()
						.getResourceAsStream("/xslt/generate-results.xsl")));
		resultsTransformer.transform(new StreamSource(testSuiteResultsFile),
				new StreamResult(new File(this.toolCfg.getString("report.dir")
						+ File.separator + "report.html")));

		// TODO transform the results xml for all test suites to a single html
		// file rendered with jquery.
	}

	// dump all results to a single file ready for transformation
	private File toAggregateResults(
			Map<String, SOAPUITestSuiteDescriptor> suiteDescriptors)
			throws IOException, TransformerConfigurationException {

		XStream _xstream = new XStream();
		_xstream.autodetectAnnotations(true);
		_xstream.addImplicitCollection(
				SOAPUITestSuiteDescriptorAggregator.class, "suiteDescriptors");
		_xstream.registerConverter(new SOAPUITestSuiteDescriptorConverter());

		File filesListFile = new File(this.toolCfg.getString("report.dir")
				+ File.separator + "all-testsuites.xml");
		FileWriter fw = new FileWriter(filesListFile);
		_xstream.toXML(
				new SOAPUITestSuiteDescriptorAggregator(suiteDescriptors
						.values()), fw);
		fw.flush();
		fw.close();

		File aggregateResultsFile = generateResults(filesListFile);

		// filesListFile.delete();

		return aggregateResultsFile;

	}

	private File generateResults(File filesListFile) {

		// aggregate the suite results so they can be transformed to html
		TransformerFactory _factory = TransformerFactory.newInstance();
		File aggregateResultsFile = null;
		try {
			StreamSource transformSource = new StreamSource(this.getClass()
					.getResourceAsStream(
							"/xslt/testsuite-report-aggregator.xsl"));
			Transformer resultsTransformer = _factory
					.newTransformer(transformSource);

			aggregateResultsFile = new File(
					this.toolCfg.getString("report.dir") + File.separator
							+ "all-testsuite-results.xml");

			resultsTransformer.setURIResolver(new URIResolver() {

				@Override
				public Source resolve(String href, String base)
						throws TransformerException {
					String inputResultsFileUri = SOAPUITestSuiteRunner.this.toolCfg
							.getString("report.dir") + File.separator + href;
					return new StreamSource(inputResultsFileUri);
				}

			});
			resultsTransformer.transform(new StreamSource(filesListFile),
					new StreamResult(aggregateResultsFile));
		} catch (TransformerException e) {
			throw new RuntimeException("Configuration issue or file issue", e);

		}
		return aggregateResultsFile;
	}

	private Map<String, SOAPUITestSuiteDescriptor> runSuites(
			String runnerPropsFile) {

		Map<String, SOAPUITestSuiteDescriptor> suiteDescriptors;
		String cmdFile = null;
		try {
			// build meta data about testsuites
			suiteDescriptors = assembleTestSuites(runnerPropsFile);

			// execute all tests in each testsuite...
			for (SOAPUITestSuiteDescriptor nextDesc : suiteDescriptors.values()) {
				cmdFile = runAsBatchCommand(nextDesc);
			}
		} catch (ConfigurationException | IOException e) {
			throw new RuntimeException("Configuration issue or file issue", e);
		} finally {
			// remove the batch file...
			if (cmdFile != null) {
				new File(cmdFile).delete();
			}
		}

		return suiteDescriptors;
	}

	// wraps the soapui command line with a dummy script with local EV scope to
	// get around command line length limit
	private String runAsBatchCommand(SOAPUITestSuiteDescriptor desc)
			throws IOException {

		StringBuffer buf = new StringBuffer(desc.toCommandLine()).insert(0,
				"setlocal\n").append("\nendlocal");
		File suiteCmdFile = File.createTempFile("suiterunner", ".cmd");
		FileWriter fw = new FileWriter(suiteCmdFile);
		IOUtils.copy(new StringReader(buf.toString()), fw);
		IOUtils.closeQuietly(fw);
		createProcessBuilder(suiteCmdFile.getAbsolutePath(), desc.getSuiteOverridesFile())
				.start();

		return suiteCmdFile.getAbsolutePath();
	}

	private ProcessBuilder createProcessBuilder(String command,
			String overrideFile) {

		ProcessBuilder pb = new ProcessBuilder(command);
		pb.environment().clear();
		pb.environment().put("OVERRIDE", overrideFile);
		File log = new File("SOAPUITestSuiteRunner.log");
		pb.redirectErrorStream(true);
		pb.redirectOutput(Redirect.appendTo(log));
		return pb;
	}

	public static void main(String[] args) {

		try {
			new SOAPUITestSuiteRunner().run();
		} catch (ConfigurationException | TransformerException | IOException e) {
			// TODO error to user
			System.err.println("Failed!");
		}

	}
}
