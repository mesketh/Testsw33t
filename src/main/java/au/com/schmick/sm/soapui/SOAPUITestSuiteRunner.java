package au.com.schmick.sm.soapui;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.lang.ProcessBuilder.Redirect;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.ConfigurationUtils;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

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

	class SOAPUITestSuiteDescriptor {

		private String suiteName;
		private Configuration cfg;

		public SOAPUITestSuiteDescriptor(String suiteName, Configuration cfg) {
			super();
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

	// runs each test suite after resolving all properties for it
	private Map<String, SOAPUITestSuiteDescriptor> assembleTestSuites(
			String soapSuitesFile) throws ConfigurationException {

		PropertiesConfiguration defaultCfg = new PropertiesConfiguration(
				soapSuitesFile);

		StringTokenizer st = new StringTokenizer(
				defaultCfg.getString("soapui-test-suites"), ",");

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

	private void runSuites(String runnerPropsFile)
			throws ConfigurationException {

		Map<String, SOAPUITestSuiteDescriptor> suiteDescriptors = assembleTestSuites(runnerPropsFile);

		for (SOAPUITestSuiteDescriptor nextDesc : suiteDescriptors.values()) {

			try {
				runAsBatchCommand(nextDesc);
			} catch (IOException ioe) {
				System.err.println(ioe);
			}
		}
	}

	private void runAsBatchCommand(SOAPUITestSuiteDescriptor desc)
			throws IOException {

		StringBuffer buf = new StringBuffer(desc.toCommandLine()).insert(0,
				"setlocal\n").append("\nendlocal");
		FileWriter fw = new FileWriter("suiterunner.cmd");
		IOUtils.copy(new StringReader(buf.toString()), fw);
		IOUtils.closeQuietly(fw);
		createProcessBuilder("suiterunner.cmd", desc.getSuiteOverridesFile())
				.start();
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
			new SOAPUITestSuiteRunner().runSuites(RUNNER_CFG_PROP);
		} catch (ConfigurationException e) {
			System.err.print("Can't find runner.properties");
		}

	}
}
