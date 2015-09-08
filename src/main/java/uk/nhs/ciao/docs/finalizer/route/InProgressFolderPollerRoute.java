package uk.nhs.ciao.docs.finalizer.route;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;

import uk.nhs.ciao.docs.finalizer.processor.InProgressFolderPoller;

public class InProgressFolderPollerRoute extends RouteBuilder {
	private String timerName = "inProgressFolderPoller";
	private String period = "5s";
	private boolean daemon = false;
	private InProgressFolderPoller inProgressFolderPoller;
	
	public void setTimerName(final String timerName) {
		this.timerName = timerName;
	}
	
	public void setPeriod(final String period) {
		this.period = period;
	}
	
	public void setInProgressFolderPoller(final InProgressFolderPoller inProgressFolderPoller) {
		this.inProgressFolderPoller = inProgressFolderPoller;
	}
	
	public void setDaemon(final boolean daemon) {
		this.daemon = daemon;
	}
	
	@Override
	public void configure() throws Exception {
		from("timer://" + timerName + "?daemon=" + daemon + "&period=" + period)
			.bean(inProgressFolderPoller, "poll(${header." + Exchange.TIMER_FIRED_TIME + "})")
		.end();
	}
}
