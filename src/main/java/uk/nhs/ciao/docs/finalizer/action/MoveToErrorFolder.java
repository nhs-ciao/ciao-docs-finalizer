package uk.nhs.ciao.docs.finalizer.action;

import static uk.nhs.ciao.logging.CiaoLogMessage.logMsg;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import uk.nhs.ciao.docs.finalizer.state.DocumentTransferProcess;
import uk.nhs.ciao.docs.finalizer.state.Transition;
import uk.nhs.ciao.docs.finalizer.state.TransitionListener;
import uk.nhs.ciao.logging.CiaoLogger;

/**
 * Action to move a document transfer folder from in-progress to its
 * configured completed error folder.
 * 
 * @see DocumentTransferProcess#getCompletedFolder()
 */
public class MoveToErrorFolder implements TransitionListener {
	private static final CiaoLogger LOGGER = CiaoLogger.getLogger(MoveToErrorFolder.class);
	
	@Override
	public void onTransition(final DocumentTransferProcess process, final Transition transition) {
		LOGGER.info(logMsg("Moving files to error folder")
				.documentId(process.getCorrelationId())
				.state(process.getState())
				.set("ErrorFolder", process.getErrorFolder())
				.eventName("move-to-error-folder"));
		
		final File target = new File(process.getErrorFolder());
		if (!target.getParentFile().exists()) {
			target.getParentFile().mkdirs();
		}

		try {
			FileUtils.moveDirectory(process.getRootFolder(), target);
		} catch (IOException e) {
			LOGGER.error(logMsg("Failed to move files to error folder")
				.documentId(process.getCorrelationId())
				.state(process.getState())
				.set("ErrorFolder", process.getErrorFolder())
				.eventName("move-to-error-folder-failed"),
				e);
		}
	}
}
