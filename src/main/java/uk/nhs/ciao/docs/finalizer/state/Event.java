package uk.nhs.ciao.docs.finalizer.state;

import java.util.Map;

import com.google.common.collect.Maps;

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
			return state.onDocumentSent(process.isBusAckWanted(), process.isInfAckWanted());
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
	
	public static Event getByFileSuffix(final String fileSuffix) {
		return EVENTS_BY_FILE_SUFFIX.get(fileSuffix);
	}
	
	private final String fileSuffix;
	
	private Event(final String fileSuffix) {
		this.fileSuffix = fileSuffix;
	}
	
	public String getFileSuffix() {
		return fileSuffix;
	}
	
	public abstract State dispatch(final State state, final DocumentTransferProcess process);
}