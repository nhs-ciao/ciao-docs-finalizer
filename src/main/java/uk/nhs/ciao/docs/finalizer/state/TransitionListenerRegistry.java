package uk.nhs.ciao.docs.finalizer.state;

import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import uk.nhs.ciao.docs.finalizer.state.DocumentTransferProcess.State;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class TransitionListenerRegistry implements TransitionListener {
	private ConcurrentMap<State, Set<TransitionListener>> toStateListeners = Maps.newConcurrentMap();
	
	public void addToStateListener(final State toState, final TransitionListener action) {
		Preconditions.checkNotNull(toState);
		Preconditions.checkNotNull(action);
		
		Set<TransitionListener> listeners = toStateListeners.get(toState);
		if (listeners == null) {
			listeners = Sets.newCopyOnWriteArraySet();
			final Set<TransitionListener> previous = toStateListeners.putIfAbsent(toState, listeners);
			if (previous != null) {
				listeners = previous;
			}
		}
		
		listeners.add(action);
	}
	
	public void removeToStateListener(final State toState, final TransitionListener action) {
		final Set<TransitionListener> listeners = toStateListeners.get(toState);
		if (listeners != null) {
			listeners.remove(action);
		}
	}
	
	@Override
	public void onTransition(final DocumentTransferProcess process, final Transition transition) {
		final Set<TransitionListener> listeners = toStateListeners.get(transition.getToState());
		if (listeners == null || listeners.isEmpty()) {
			return;
		}
		
		for (final TransitionListener action: listeners) {
			action.onTransition(process, transition);
		}
	}
}
