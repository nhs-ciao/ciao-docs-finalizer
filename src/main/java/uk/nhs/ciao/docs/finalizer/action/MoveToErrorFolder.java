package uk.nhs.ciao.docs.finalizer.action;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.nhs.ciao.docs.finalizer.state.DocumentTransferProcess;
import uk.nhs.ciao.docs.finalizer.state.Transition;
import uk.nhs.ciao.docs.finalizer.state.TransitionListener;

/**
 * Action to move a document transfer folder from in-progress to its
 * configured completed error folder.
 * 
 * @see DocumentTransferProcess#getCompletedFolder()
 */
public class MoveToErrorFolder implements TransitionListener {
	private static final Logger LOGGER = LoggerFactory.getLogger(MoveToErrorFolder.class);
	
	@Override
	public void onTransition(final DocumentTransferProcess process, final Transition transition) {
		LOGGER.info("Moving files to error folder - correlationId: {}, errorFolder: {}",
				process.getCorrelationId(), process.getErrorFolder());
		
		final File target = new File(process.getErrorFolder());
		if (!target.getParentFile().exists()) {
			target.getParentFile().mkdirs();
		}

		try {
			FileUtils.moveDirectory(process.getRootFolder(), target);
		} catch (IOException e) {
			LOGGER.error("Failed to move files to error folder - correlationId: {}, errorFolder: {}",
				process.getCorrelationId(), target, e);
		}
	}
}
