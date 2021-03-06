package uk.nhs.ciao.docs.finalizer.state;

import static uk.nhs.ciao.logging.CiaoLogMessage.logMsg;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import uk.nhs.ciao.logging.CiaoLogger;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.io.CharStreams;
import com.google.common.io.Closeables;

public class DocumentTransferProcess {
	private static final CiaoLogger LOGGER = CiaoLogger.getLogger(DocumentTransferProcess.class);
	
	/**
	 * Regular expression pattern matching event files (e.g. 20150903-153545804-bus-ack-received)
	 * <p>
	 * Group 1 contains the timestamp portion (20150903-153545804) - see {@link #EVENT_NAME_TIMESTAMP_FORMATTER}
	 * <p>
	 * Group 2 the file suffix (bus-ack-received) - see {@link #EVENTS_BY_FILE_SUFFIX}
	 */
	private static final Pattern EVENT_NAME_PATTERN = Pattern.compile("(\\d{8}-\\d{9})-(.+)");
	
	/**
	 * Timestamp format used in event file-names
	 */
	private static final DateTimeFormatter EVENT_NAME_TIMESTAMP_FORMATTER = DateTimeFormat.forPattern("yyyyMMdd-HHmmssSSS").withZoneUTC();
	
	private final String correlationId;
	private final File rootFolder;
	private final TransitionListener transitionListener;
	
	/**
	 * Internal lock used to synchronise state mutations
	 */
	private final Object lock;	
	
	/*
	 * State properties
	 * mutations are synchronised by the internal lock
	 * declared volatile to allow accessors to return without locking
	 */
	private volatile State state;
	private volatile String completedFolder;
	private volatile String errorFolder;
	private volatile boolean infAckWanted;
	private volatile boolean busAckWanted;
	
	private final Timeout documentPreparationTimeout;
	private final Timeout documentSendTimeout;
	private final Timeout infResponseTimeout;
	private final Timeout busResponseTimeout;
	
	public DocumentTransferProcess(final String correlationId, final File rootFolder, final TransitionListener transitionListener) {
		this.correlationId = Preconditions.checkNotNull(correlationId);
		this.rootFolder = Preconditions.checkNotNull(rootFolder);
		this.transitionListener = Preconditions.checkNotNull(transitionListener);
		this.lock = new Object();
		this.state = State.PARSING;
		
		documentPreparationTimeout = new Timeout();
		documentSendTimeout = new Timeout();
		infResponseTimeout = new Timeout();
		busResponseTimeout = new Timeout();
	}
	
	public String getCorrelationId() {
		return correlationId;
	}
	
	public File getRootFolder() {
		return rootFolder;
	}
	
	public State getState() {
		return state;
	}
	
	public String getCompletedFolder() {
		return completedFolder;
	}
	
	public String getErrorFolder() {
		return errorFolder;
	}
	
	public boolean isInfAckWanted() {
		return infAckWanted;
	}
	
	public boolean isBusAckWanted() {
		return busAckWanted;
	}
	
	public Timeout getDocumentPreparationTimeout() {
		return documentPreparationTimeout;
	}
	
	public Timeout getDocumentSendTimeout() {
		return documentSendTimeout;
	}
	
	public Timeout getInfResponseTimeout() {
		return infResponseTimeout;
	}
	
	public Timeout getBusResponseTimeout() {
		return busResponseTimeout;
	}
	
	@Override
	public boolean equals(final Object obj) {
		if (obj == this) {
			return true;
		} else if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		
		final DocumentTransferProcess other = (DocumentTransferProcess)obj;
		return correlationId.equals(other.correlationId);
	}
	
	@Override
	public int hashCode() {
		return correlationId.hashCode();
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("correlationId", correlationId)
				.add("rootFolder", rootFolder)
				.add("state", state)
				.add("completedFolder", completedFolder)
				.add("errorFolder", errorFolder)
				.add("infAckWanted", infAckWanted)
				.add("busAckWanted", busAckWanted)
				.toString();
	}
	
	// public mutation methods - synchronised by internal lock
	
	public void registerControlFile(final File file) {
		synchronized (lock) {
			final String name = file.getName();
			if ("completed-folder".equals(name)) {
				setCompletedFolder(file);
			}  else if ("error-folder".equals(name)) {
				setErrorFolder(file);
			} else if ("wants-inf-ack".equals(name)) {
				infAckWanted = true;
			} else if ("wants-bus-ack".equals(name)) {
				busAckWanted = true;
			}
		}
	}
	
	public void registerEventsFile(final File file) {
		synchronized (lock) {
			final Matcher matcher = EVENT_NAME_PATTERN.matcher(file.getName());
			if (!matcher.matches()) {
				LOGGER.debug(logMsg("Unable to register event file - the name does not match expected pattern")
						.documentId(getCorrelationId())
						.state(state)
						.eventName("event-file-registration-failed")
						.fileName(file));
				return;
			}
			
			final long eventTime;
			try {
				eventTime = EVENT_NAME_TIMESTAMP_FORMATTER.parseMillis(matcher.group(1));
			} catch (IllegalArgumentException e) {
				LOGGER.debug(logMsg("Unable to register event file - the timestamp is not valid")
					.documentId(getCorrelationId())
					.state(state)
					.eventName("event-file-registration-failed")
					.fileName(file)
					.set("Timestamp", matcher.group(1)));
				
				return;
			}
			
			final Event event = Event.getByFileSuffix(matcher.group(2));
			if (event != null) {
				transition(eventTime, event);
			}
		}
	}

	public void processTimeouts(final long now) {
		synchronized (lock) {
			processTimeout(documentPreparationTimeout, Event.DOCUMENT_PREPARATION_TIMEOUT, now);
			processTimeout(documentSendTimeout, Event.DOCUMENT_SEND_TIMEOUT, now);
			processTimeout(infResponseTimeout, Event.INF_RESPONSE_TIMEOUT, now);
			processTimeout(busResponseTimeout, Event.BUS_RESPONSE_TIMEOUT, now);
		}
	}
	
	// private methods - synchronisation is handled by the public calling methods
	
	private void processTimeout(final Timeout timeout, final Event event, final long now) {
		if (timeout.isTriggered(now)) {
			final long eventTime = timeout.getTrigger();
			timeout.cancel();
			
			storeTimestampEventFile(event, eventTime);
			transition(eventTime, event);
		}
	}

	private void storeTimestampEventFile(final Event event, final long eventTime) {
		final File eventsFolder = new File(rootFolder, "events");
		final File eventFile = new File(eventsFolder, EVENT_NAME_TIMESTAMP_FORMATTER.print(eventTime) + "-" + event.getFileSuffix());
		
		LOGGER.info(logMsg("Storing event file")
			.documentId(getCorrelationId())
			.state(state)
			.eventName("store-" + event.getFileSuffix())
			.fileName(eventFile));
		
		try {
			// no-content - just stamp a new file to represent the event
			eventFile.createNewFile();
		} catch (IOException e) {
			LOGGER.warn(logMsg("Unable to store event file")
				.documentId(getCorrelationId())
				.state(state)
				.eventName("store-" + event.getFileSuffix())
				.fileName(eventFile));
		}
	}
	
	private void cancelTimeouts() {
		documentPreparationTimeout.cancel();
		documentSendTimeout.cancel();
		infResponseTimeout.cancel();
		busResponseTimeout.cancel();
	}
	
	private void setCompletedFolder(final File file) {
		completedFolder = readFirstLine(file).trim();
	}
	
	private void setErrorFolder(final File file) {
		errorFolder = readFirstLine(file).trim();
	}
	
	private String readFirstLine(final File file) {
		if (file == null || !file.isFile()) {
			return "";
		}
		
		String firstLine = "";
		Reader reader = null;
		try {
			reader = new FileReader(file);
			final List<String> lines = CharStreams.readLines(reader);
			if (!lines.isEmpty()) {
				firstLine = lines.get(0);
			}
		} catch (IOException e) {
			LOGGER.debug(logMsg("Unable to read first line from file")
					.documentId(getCorrelationId())
					.state(state)
					.fileName(file));
		} finally {
			Closeables.closeQuietly(reader);
		}
		
		return firstLine;
	}
	
	private void transition(final long eventTime, final Event event) {
		final State from = this.state;
		this.state = event.dispatch(from, this, eventTime);
		if (from != state) {
			LOGGER.info(logMsg("State transition")
				.documentId(getCorrelationId())
				.fromState(from)
				.toState(state)
				.eventName(event.getFileSuffix()));
			
			if (state.isTerminal()) {
				cancelTimeouts();
			}
			
			final Transition transition = new Transition(from, state, event, eventTime);
			transitionListener.onTransition(this, transition);
		}
	}
}
 