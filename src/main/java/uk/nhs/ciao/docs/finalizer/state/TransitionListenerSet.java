package uk.nhs.ciao.docs.finalizer.state;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import com.google.common.collect.Sets;

/**
 * A listener which dispatches transition notifications
 * to a set of delegate listeners
 */
public class TransitionListenerSet extends AbstractSet<TransitionListener> implements TransitionListener {
	private final Set<TransitionListener> listeners;

	/**
	 * Creates a new empty listener
	 */
	public TransitionListenerSet() {
		this.listeners = Sets.newCopyOnWriteArraySet();
	}
	
	/**
	 * Creates a new listener backed by the specified delegate listeners
	 */
	public TransitionListenerSet(final Collection<? extends TransitionListener> listeners) {
		this.listeners = Sets.newCopyOnWriteArraySet(listeners);
	}
	
	// TransitionListener interface
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onTransition(final DocumentTransferProcess process, final Transition transition) {
		for (final TransitionListener listener: listeners) {
			listener.onTransition(process, transition);
		}
	}
	
	// Set interface
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Iterator<TransitionListener> iterator() {
		return listeners.iterator();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int size() {
		return listeners.size();
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isEmpty() {
		return listeners.isEmpty();
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean add(final TransitionListener e) {
		return listeners.add(e);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean remove(final Object o) {
		return listeners.remove(o);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean addAll(final Collection<? extends TransitionListener> c) {
		return listeners.addAll(c);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean removeAll(final Collection<?> c) {
		return listeners.removeAll(c);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean retainAll(final Collection<?> c) {
		return listeners.retainAll(c);
	}
}
