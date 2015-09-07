package uk.nhs.ciao.docs.finalizer.state;

public interface TransitionListener {
	void onTransition(final DocumentTransferProcess process, final Transition transition);
}
