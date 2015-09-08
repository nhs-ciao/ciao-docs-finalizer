package uk.nhs.ciao.docs.finalizer.state;

import com.google.common.base.MoreObjects;

public final class Timeout {
	private volatile long start;
	private volatile long threshold;
	
	public Timeout() {
		// NOOP
	}
	
	public Timeout(final long threshold) {
		this.threshold = threshold;
	}
	
	public long getStart() {
		return start;
	}
	
	public long getThreshold() {
		return threshold;
	}
	
	public void start(final long now) {
		this.start = now;
	}
	
	public void cancel() {
		this.start = 0;
	}
	
	public void setThreshold(final long threshold) {
		this.threshold = threshold;
	}
	
	public boolean isStarted() {
		return start > 0 && threshold > 0;
	}
	
	public long getTrigger() {
		return start + threshold;
	}
	
	public boolean isTriggered(final long now) {
		return isStarted() && (getTrigger() <= now);
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("start", start)
				.add("threshold", threshold)
				.toString();
	}
}
