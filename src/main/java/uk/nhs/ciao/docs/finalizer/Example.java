package uk.nhs.ciao.docs.finalizer;

import java.io.File;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultProducerTemplate;
import org.apache.camel.impl.SimpleRegistry;
import org.apache.camel.processor.idempotent.MemoryIdempotentRepository;

import uk.nhs.ciao.docs.finalizer.action.MoveToCompletedFolder;
import uk.nhs.ciao.docs.finalizer.action.MoveToErrorFolder;
import uk.nhs.ciao.docs.finalizer.processor.InProgressDirectoryPoller;
import uk.nhs.ciao.docs.finalizer.state.DocumentTransferProcessFactory;
import uk.nhs.ciao.docs.finalizer.state.IdempotentTransitionListener;
import uk.nhs.ciao.docs.finalizer.state.State;
import uk.nhs.ciao.docs.finalizer.state.TransitionListenerRegistry;

public class Example {
	public static void main(final String[] args) throws Exception {
		final SimpleRegistry registry = new SimpleRegistry();
		
		final TransitionListenerRegistry listenerRegistry = new TransitionListenerRegistry();
		
		final DocumentTransferProcessFactory factory = new DocumentTransferProcessFactory(listenerRegistry);
		factory.setDocumentPreparationTimeout(TimeUnit.MINUTES.toMillis(1));
		factory.setDocumentSendTimeout(TimeUnit.MINUTES.toMillis(3));
		factory.setInfResponseTimeout(TimeUnit.MINUTES.toMillis(5));
		factory.setBusResponseTimeout(TimeUnit.DAYS.toMillis(2));
		
		final File inProgressDirectory = new File("..\\ciao-docs-parser\\ciao-docs-parser\\in-progress");
		
		registry.put("inProgressDirectoryPoller", new InProgressDirectoryPoller(factory, inProgressDirectory));
		registry.put("idempotentRepository", new MemoryIdempotentRepository());
		
		final CamelContext context = new DefaultCamelContext(registry);
		final ProducerTemplate producerTemplate = new DefaultProducerTemplate(context);
		
		final PropertiesComponent properties = new PropertiesComponent();
		context.addComponent("properties", properties);		
		properties.setInitialProperties(new Properties());
		properties.getInitialProperties().put("inProgressDirectoryPollPeriod", "5s");
		
		
		listenerRegistry.addToStateListener(State.SUCCEEDED, new IdempotentTransitionListener(
				"direct:idempotentTransitionListener", producerTemplate, new MoveToCompletedFolder()));
		listenerRegistry.addToStateListener(State.FAILED, new IdempotentTransitionListener(
				"direct:idempotentTransitionListener", producerTemplate, new MoveToErrorFolder()));
		
		context.addRoutes(new DocsFinalizerRoutes());
		
		context.start();
		producerTemplate.start();
	}
}
