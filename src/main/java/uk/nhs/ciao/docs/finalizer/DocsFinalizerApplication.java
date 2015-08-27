package uk.nhs.ciao.docs.finalizer;

import uk.nhs.ciao.camel.CamelApplication;
import uk.nhs.ciao.camel.CamelApplicationRunner;
import uk.nhs.ciao.configuration.CIAOConfig;
import uk.nhs.ciao.exceptions.CIAOConfigurationException;

/**
 * The main ciao-docs-finalizer application
 */
public class DocsFinalizerApplication extends CamelApplication {
	/**
	 * Runs the docs finalizer application
	 * 
	 * @see CIAOConfig#CIAOConfig(String[], String, String, java.util.Properties)
	 * @see CamelApplicationRunner
	 */
	public static void main(final String[] args) throws Exception {
		final CamelApplication application = new DocsFinalizerApplication(args);
		CamelApplicationRunner.runApplication(application);
	}
	
	public DocsFinalizerApplication(final String... args) throws CIAOConfigurationException {
		super("ciao-docs-finalizer.properties", args);
	}
	
	public DocsFinalizerApplication(final CIAOConfig ciaoConfig, final String... args) {
		super(ciaoConfig, args);
	}
}
