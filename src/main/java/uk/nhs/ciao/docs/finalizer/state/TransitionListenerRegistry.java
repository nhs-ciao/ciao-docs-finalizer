package uk.nhs.ciao.docs.finalizer.state;

import java.util.concurrent.ConcurrentMap;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

/**
 * A listener which dispatches transition notifications to delegate listeners determined by
 * the type of transition (e.g. the to-state of the transition)
 */
public class TransitionListenerRegistry implements TransitionListener {
	private ConcurrentMap<State, TransitionListener> toStateListeners = Maps.newConcurrentMap();
	
	public boolean addToStateListener(final State toState, final TransitionListener listener) {
		Preconditions.checkNotNull(toState);
		Preconditions.checkNotNull(listener);
		
		return toStateListeners.putIfAbsent(toState, listener) == null;
	}
	
	public boolean removeToStateListener(final State toState, final TransitionListener listener) {
		return toStateListeners.remove(toState, listener);
	}
	
	@Override
	public void onTransition(final DocumentTransferProcess process, final Transition transition) {
		final TransitionListener listener = toStateListeners.get(transition.getToState());
		if (listener != null) {
			listener.onTransition(process, transition);
		}
	}
}
