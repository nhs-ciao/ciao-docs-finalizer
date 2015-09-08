package uk.nhs.ciao.docs.finalizer.state;

import java.io.File;

import com.google.common.base.Preconditions;

public class DocumentTransferProcessFactory {
	private TransitionListener transitionListener;
	private long documentPreparationTimeout;
	private long documentSendTimeout;
	private long infResponseTimeout;
	private long busResponseTimeout;
	
	public DocumentTransferProcessFactory(final TransitionListener transitionListener) {
		this.transitionListener = Preconditions.checkNotNull(transitionListener);
	}
	
	public void setTransitionListener(final TransitionListener transitionListener) {
		this.transitionListener = transitionListener;
	}
	
	public void setDocumentPreparationTimeout(final long documentPreparationTimeout) {
		this.documentPreparationTimeout = documentPreparationTimeout;
	}
	
	public void setDocumentSendTimeout(final long documentSendTimeout) {
		this.documentSendTimeout = documentSendTimeout;
	}
	
	public void setInfResponseTimeout(final long infResponseTimeout) {
		this.infResponseTimeout = infResponseTimeout;
	}
	
	public void setBusResponseTimeout(final long busResponseTimeout) {
		this.busResponseTimeout = busResponseTimeout;
	}
	
	public DocumentTransferProcess createDocumentTransferProcess(final String correlationId, final File rootFolder) {
		final DocumentTransferProcess process = new DocumentTransferProcess(correlationId, rootFolder, transitionListener);
		
		process.getDocumentPreparationTimeout().setThreshold(documentPreparationTimeout);
		process.getDocumentSendTimeout().setThreshold(documentSendTimeout);
		process.getInfResponseTimeout().setThreshold(infResponseTimeout);
		process.getBusResponseTimeout().setThreshold(busResponseTimeout);
		
		return process;
	}
}
