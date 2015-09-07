package uk.nhs.ciao.docs.finalizer;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.nhs.ciao.docs.finalizer.action.MoveToCompletedFolder;
import uk.nhs.ciao.docs.finalizer.action.MoveToErrorFolder;
import uk.nhs.ciao.docs.finalizer.state.DocumentTransferProcessFactory;
import uk.nhs.ciao.docs.finalizer.state.State;
import uk.nhs.ciao.docs.finalizer.state.TransitionListenerRegistry;

public class Example {
	private static final Logger LOGGER = LoggerFactory.getLogger(Example.class);
	
	public static void main(final String[] args) throws Exception {
		final long pollingPeriod = 5000;
		
		final TransitionListenerRegistry listenerRegistry = new TransitionListenerRegistry();
		listenerRegistry.addToStateListener(State.SUCCEEDED, new MoveToCompletedFolder());
		listenerRegistry.addToStateListener(State.FAILED, new MoveToErrorFolder());
		
		final File inProgressDirectory = new File("..\\ciao-docs-parser\\ciao-docs-parser\\in-progress");
		LOGGER.info("In progress directory: {}, exists={}", inProgressDirectory.getCanonicalFile(), inProgressDirectory.isDirectory());
		
		final InProgressDirectoryPoller poller = new InProgressDirectoryPoller(
				new DocumentTransferProcessFactory(listenerRegistry), inProgressDirectory);
		
		while (true) {
			poller.poll(System.currentTimeMillis());
			
			Thread.sleep(pollingPeriod);
		}
	}
}
