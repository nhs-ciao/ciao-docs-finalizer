package uk.nhs.ciao.docs.finalizer.state;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

public class Transition {
	private final State fromState;
	private final State toState;
	private final Event event;
	private final long time;
	
	public Transition(final State fromState, final State toState, final Event event, final long time) {
		this.fromState = Preconditions.checkNotNull(fromState);
		this.toState = Preconditions.checkNotNull(toState);
		this.event = Preconditions.checkNotNull(event);
		this.time = time;
	}
	
	public State getFromState() {
		return fromState;
	}
	
	public State getToState() {
		return toState;
	}
	
	public Event getEvent() {
		return event;
	}
	
	public long getTime() {
		return time;
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("fromState", fromState)
				.add("toState", toState)
				.add("event", event)
				.add("time", time)
				.toString();
	}
}
