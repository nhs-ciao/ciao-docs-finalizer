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
 * configured completed target folder.
 * 
 * @see DocumentTransferProcess#getCompletedFolder()
 */
public class MoveToCompletedFolder implements TransitionListener {
	private static final CiaoLogger LOGGER = CiaoLogger.getLogger(MoveToCompletedFolder.class);
	
	@Override
	public void onTransition(final DocumentTransferProcess process, final Transition transition) {
		LOGGER.info(logMsg("Moving files to completed folder")
				.documentId(process.getCorrelationId())
				.state(process.getState())
				.set("CompletedFolder", process.getCompletedFolder())
				.eventName("move-to-completed-folder"));
		
		final File target = new File(process.getCompletedFolder());
		if (!target.getParentFile().exists()) {
			target.getParentFile().mkdirs();
		}

		try {
			FileUtils.moveDirectory(process.getRootFolder(), target);
		} catch (IOException e) {
			LOGGER.error(logMsg("Failed to move files to completed folder")
				.documentId(process.getCorrelationId())
				.state(process.getState())
				.set("CompletedFolder", process.getCompletedFolder())
				.eventName("move-to-completed-folder-failed"),
				e);
		}
	}
}
