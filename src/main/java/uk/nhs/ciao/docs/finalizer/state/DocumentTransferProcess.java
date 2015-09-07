package uk.nhs.ciao.docs.finalizer.state;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.io.CharStreams;
import com.google.common.io.Closeables;

public class DocumentTransferProcess {
	private static final Logger LOGGER = LoggerFactory.getLogger(DocumentTransferProcess.class);
	
	/**
	 * ISO-8601 timestamp format used in the start-time control file
	 */
	private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZoneUTC();
	
	/**
	 * Regular expression pattern matching state files (e.g. 20150903-153545804-bus-ack-received)
	 * <p>
	 * Group 1 contains the timestamp portion (20150903-153545804) - see {@link #STATE_NAME_TIMESTAMP_FORMATTER}
	 * <p>
	 * Group 2 the file suffix (bus-ack-received) - see {@link #EVENTS_BY_FILE_SUFFIX}
	 */
	private static final Pattern STATE_NAME_PATTERN = Pattern.compile("(\\d{8}-\\d{9})-(.+)");
	
	/**
	 * Timestamp format used in state file-names
	 */
	private static final DateTimeFormatter STATE_NAME_TIMESTAMP_FORMATTER = DateTimeFormat.forPattern("yyyyMMdd-HHmmssSSS").withZoneUTC();
	
	
	/**
	 * Mapping of state file suffix to event
	 */
	private static final Map<String, Event> EVENTS_BY_FILE_SUFFIX;
	static {
		final Map<String, Event> map = Maps.newHashMap();
		for (final Event event: Event.values()) {
			if (event.fileSuffix != null) {
				map.put(event.fileSuffix, event);
			}
		}
		EVENTS_BY_FILE_SUFFIX = map;
	}
	
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
	private volatile long startTime;
	private volatile String completedFolder;
	private volatile String errorFolder;
	private volatile boolean infAckWanted;
	private volatile boolean busAckWanted;
	
	public DocumentTransferProcess(final String correlationId, final File rootFolder, final TransitionListener transitionListener) {
		this.correlationId = Preconditions.checkNotNull(correlationId);
		this.rootFolder = Preconditions.checkNotNull(rootFolder);
		this.transitionListener = Preconditions.checkNotNull(transitionListener);
		this.lock = new Object();
		this.state = State.PARSING;
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
	
	public long getStartTime() {
		return startTime;
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
				.add("startTime", startTime)
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
			if ("start-time".equals(name)) {
				setStartTime(file);
			} else if ("completed-folder".equals(name)) {
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
	
	public void registerStateFile(final File file) {
		synchronized (lock) {
			final Matcher matcher = STATE_NAME_PATTERN.matcher(file.getName());
			if (!matcher.matches()) {
				LOGGER.debug("Unable to register state file - the name does not match expected pattern - correlationId: {}, file: {}",
						correlationId, file);
				return;
			}
			
			final long eventTime;
			try {
				eventTime = STATE_NAME_TIMESTAMP_FORMATTER.parseMillis(matcher.group(1));
			} catch (IllegalArgumentException e) {
				LOGGER.debug("Unable to register state file - the timestamp is not valid - correlationId: {}, timestamp: {}",
						correlationId, matcher.group(1));
				return;
			}
			
			final Event event = EVENTS_BY_FILE_SUFFIX.get(matcher.group(2));
			if (event != null) {
				transition(eventTime, event);
			}
		}
	}

	public void processTimeouts(final long now) {
		synchronized (lock) {
			
		}
	}
	
	// private methods - synchronisation is handled by the public calling methods
	
	private void setStartTime(final File file) {
		final String value = readFirstLine(file).trim();
		try {
			startTime = TIMESTAMP_FORMATTER.parseMillis(value);
			transition(startTime, Event.DOCUMENT_PARSED);
		} catch (IllegalArgumentException e) {
			LOGGER.error("Unable to process start-time control file  - correlationId: {}, value: {}", correlationId, value, e);
		}
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
			LOGGER.debug("Unable to read first line from file - correlationId: {}, file: {}", correlationId, file, e);
		} finally {
			Closeables.closeQuietly(reader);
		}
		
		return firstLine;
	}
	
	private void transition(final long eventTime, final Event event) {
		final State from = this.state;
		this.state = event.dispatch(from, this);
		if (from != state) {
			LOGGER.info("State transition - correlationId: {}, from: {}, to: {}, event: {}, eventTime: {}",
					correlationId, from, state, event, eventTime);
			
			final Transition transition = new Transition(from, state, event, eventTime);
			transitionListener.onTransition(this, transition);
		}
	}

	// public enums
	
	public enum State {
		PARSING(false) {
			@Override
			public State onDocumentParsed() {
				return PREPARING;
			}
		},
		
		PREPARING(false) {
			@Override
			public State onDocumentPreparationTimeout() {
				return FAILED;
			}
			
			@Override
			public State onDocumentPrepared() {
				return SENDING;
			}
		},
		
		SENDING(false) {
			@Override
			public State onDocumentSendTimeout() {
				return FAILED;
			}
			
			@Override
			public State onDocumentSendFailed() {
				return FAILED;
			}
			
			@Override
			public State onDocumentSent(final boolean wantsInfResponse, final boolean wantsBusResponse) {
				if (wantsInfResponse && wantsBusResponse) {
					return WAITING_INF_AND_BUS_RESPONSE;
				} else if (wantsInfResponse) {
					return WAITING_INF_RESPONSE;
				} else if (wantsBusResponse) {
					return WAITING_BUS_RESPONSE;
				} else {
					return SUCCEEDED;
				}
			}
		},
		
		WAITING_INF_AND_BUS_RESPONSE(false) {
			@Override
			public State onInfAckReceived() {
				return WAITING_BUS_RESPONSE;
			}
			
			@Override
			public State onInfNackReceived() {
				return FAILED;
			}
			
			@Override
			public State onInfResponseTimeout() {
				return FAILED;
			}
			
			@Override
			public State onBusAckReceived() {
				return WAITING_INF_RESPONSE;
			}
			
			@Override
			public State onBusNackReceived() {
				return FAILED;
			}
			
			@Override
			public State onBusResponseTimeout() {
				return FAILED;
			}
		},
		
		WAITING_INF_RESPONSE(false) {
			@Override
			public State onInfAckReceived() {
				return SUCCEEDED;
			}
			
			@Override
			public State onInfNackReceived() {
				return FAILED;
			}
			
			@Override
			public State onInfResponseTimeout() {
				return FAILED;
			}
		},
		
		WAITING_BUS_RESPONSE(false) {
			@Override
			public State onBusAckReceived() {
				return SUCCEEDED;
			}
			
			@Override
			public State onBusNackReceived() {
				return FAILED;
			}
			
			@Override
			public State onBusResponseTimeout() {
				return FAILED;
			}
		},
		
		FAILED(true),
		SUCCEEDED(true);
		
		private final boolean terminal;
		
		private State(final boolean terminal) {
			this.terminal = terminal;
		}
		
		public boolean isTerminal() {
			return terminal;
		}
		
		public State onDocumentParsed() {
			return this;
		}
		
		public State onDocumentPreparationTimeout() {
			return this;
		}
		
		public State onDocumentPrepared() {
			return this;
		}
		
		public State onDocumentSendTimeout() {
			return this;
		}
		
		public State onDocumentSendFailed() {
			return this;
		}
		
		public State onDocumentSent(final boolean wantsInfResponse, final boolean wantsBusResponse) {
			return this;
		}
		
		public State onInfResponseTimeout() {
			return this;
		}
		
		public State onInfAckReceived() {
			return this;
		}
		
		public State onInfNackReceived() {
			return this;
		}
		
		public State onBusAckReceived() {
			return this;
		}
		
		public State onBusNackReceived() {
			return this;
		}
		
		public State onBusResponseTimeout() {
			return this;
		}
	}
	
	public enum Event {
		DOCUMENT_PARSED(null) {
			@Override
			public State dispatch(final State state, final DocumentTransferProcess process) {
				return state.onDocumentParsed();
			}
		},
		
		DOCUMENT_PREPARATION_TIMEOUT(null) {
			@Override
			public State dispatch(final State state, final DocumentTransferProcess process) {
				return state.onDocumentPreparationTimeout();
			}
		},
		DOCUMENT_PREPARED("bus-message-sending") {
			@Override
			public State dispatch(final State state, final DocumentTransferProcess process) {
				return state.onDocumentPrepared();
			}
		},
		
		DOCUMENT_SEND_TIMEOUT(null) {
			@Override
			public State dispatch(final State state, final DocumentTransferProcess process) {
				return state.onDocumentSendTimeout();
			}
		},		
		DOCUMENT_SEND_FAILED("bus-message-send-failed") {
			@Override
			public State dispatch(final State state, final DocumentTransferProcess process) {
				return state.onDocumentSendFailed();
			}
		},
		DOCUMENT_SENT("bus-message-sent") {
			@Override
			public State dispatch(final State state, final DocumentTransferProcess process) {
				return state.onDocumentSent(process.busAckWanted, process.infAckWanted);
			}
		},
		
		INF_RESPONSE_TIMEOUT(null) {
			@Override
			public State dispatch(final State state, final DocumentTransferProcess process) {
				return state.onInfResponseTimeout();
			}
		},
		INF_ACK_RECEIVED("inf-ack-received") {
			@Override
			public State dispatch(final State state, final DocumentTransferProcess process) {
				return state.onInfAckReceived();
			}
		},
		INF_NACK_RECEIVED("inf-nack-received") {
			@Override
			public State dispatch(final State state, final DocumentTransferProcess process) {
				return state.onInfNackReceived();
			}
		},
		
		BUS_ACK_RECEIVED("bus-ack-received") {
			@Override
			public State dispatch(final State state, final DocumentTransferProcess process) {
				return state.onBusAckReceived();
			}
		},
		BUS_NACK_RECEIVED("bus-nack-received") {
			@Override
			public State dispatch(final State state, final DocumentTransferProcess process) {
				return state.onBusNackReceived();
			}
		},
		BUS_RESPONSE_TIMEOUT(null) {
			@Override
			public State dispatch(final State state, final DocumentTransferProcess process) {
				return state.onBusResponseTimeout();
			}
		};
		
		private final String fileSuffix;
		
		private Event(final String fileSuffix) {
			this.fileSuffix = fileSuffix;
		}
		
		public abstract State dispatch(final State state, final DocumentTransferProcess process);
	}
}
 