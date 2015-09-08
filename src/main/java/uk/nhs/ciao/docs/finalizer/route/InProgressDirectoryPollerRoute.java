package uk.nhs.ciao.docs.finalizer.route;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;

import uk.nhs.ciao.docs.finalizer.processor.InProgressDirectoryPoller;

public class InProgressDirectoryPollerRoute extends RouteBuilder {
	private String timerName = "inProgressDirectoryPoller";
	private String period = "5s";
	private boolean daemon = false;
	private InProgressDirectoryPoller inProgressDirectoryPoller;
	
	public void setTimerName(final String timerName) {
		this.timerName = timerName;
	}
	
	public void setPeriod(final String period) {
		this.period = period;
	}
	
	public void setInProgressDirectoryPoller(final InProgressDirectoryPoller inProgressDirectoryPoller) {
		this.inProgressDirectoryPoller = inProgressDirectoryPoller;
	}
	
	public void setDaemon(final boolean daemon) {
		this.daemon = daemon;
	}
	
	@Override
	public void configure() throws Exception {
		from("timer://" + timerName + "?daemon=" + daemon + "&period=" + period)
			.bean(inProgressDirectoryPoller, "poll(${header." + Exchange.TIMER_FIRED_TIME + "})")
		.end();
	}
}
