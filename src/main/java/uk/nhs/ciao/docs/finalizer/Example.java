package uk.nhs.ciao.docs.finalizer;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.nhs.ciao.docs.finalizer.state.DocumentTransferProcess;
import uk.nhs.ciao.docs.finalizer.state.DocumentTransferProcessFactory;
import uk.nhs.ciao.docs.finalizer.state.State;
import uk.nhs.ciao.docs.finalizer.state.Transition;
import uk.nhs.ciao.docs.finalizer.state.TransitionListener;
import uk.nhs.ciao.docs.finalizer.state.TransitionListenerRegistry;

public class Example {
	private static final Logger LOGGER = LoggerFactory.getLogger(Example.class);
	
	public static void main(final String[] args) throws Exception {
		final TransitionListenerRegistry listenerRegistry = new TransitionListenerRegistry();
		final DocumentTransferProcessFactory factory = new DocumentTransferProcessFactory(listenerRegistry);
		
		final File inProgressDirectory = new File("..\\ciao-docs-parser\\ciao-docs-parser\\in-progress");
		LOGGER.info("In progress directory: {}, exists={}", inProgressDirectory.getCanonicalFile(), inProgressDirectory.isDirectory());
		
		final InProgressDirectoryPoller poller = new InProgressDirectoryPoller(factory, inProgressDirectory);
		
		listenerRegistry.addToStateListener(State.SUCCEEDED, new TransitionListener() {
			@Override
			public void onTransition(final DocumentTransferProcess process, final Transition transition) {
				LOGGER.info("Moving files to completed folder - correlationId: {}, completedFolder: {}",
						process.getCorrelationId(), process.getCompletedFolder());
				
				final File target = new File(process.getCompletedFolder());
				if (!target.getParentFile().exists()) {
					target.getParentFile().mkdirs();
				}

				// TODO: rename can fail - maybe try commons FileUtils?
				process.getRootFolder().renameTo(target);
			}
		});
		
		listenerRegistry.addToStateListener(State.FAILED, new TransitionListener() {
			@Override
			public void onTransition(final DocumentTransferProcess process, final Transition transition) {
				LOGGER.info("Moving files to error folder - correlationId: {}, errorFolder: {}",
						process.getCorrelationId(), process.getErrorFolder());
				
				final File target = new File(process.getErrorFolder());
				if (!target.getParentFile().exists()) {
					target.getParentFile().mkdirs();
				}

				// TODO: rename can fail - maybe try commons FileUtils?
				process.getRootFolder().renameTo(target);
			}
		});
		
		while (true) {
			poller.poll(System.currentTimeMillis());
			
			Thread.sleep(5000);
		}
	}
}
