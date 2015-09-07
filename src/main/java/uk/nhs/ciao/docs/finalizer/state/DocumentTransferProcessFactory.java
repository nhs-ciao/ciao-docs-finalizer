package uk.nhs.ciao.docs.finalizer.state;

import java.io.File;

import com.google.common.base.Preconditions;

public class DocumentTransferProcessFactory {
	private TransitionListener transitionListener;
	
	public DocumentTransferProcessFactory(final TransitionListener transitionListener) {
		this.transitionListener = Preconditions.checkNotNull(transitionListener);
	}
	
	public DocumentTransferProcess createDocumentTransferProcess(final String correlationId, final File rootFolder) {
		return new DocumentTransferProcess(correlationId, rootFolder, transitionListener);
	}
}
