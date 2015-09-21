package uk.nhs.ciao.docs.finalizer.state;

import java.util.Map;

import com.google.common.collect.Maps;

public enum Event {
	DOCUMENT_PARSED("document-parsed") {
		@Override
		public State dispatch(final State state, final DocumentTransferProcess process, final long eventTime) {
			return state.onDocumentParsed(process, eventTime);
		}
	},
	
	DOCUMENT_PREPARATION_TIMEOUT("document-preparation-timeout") {
		@Override
		public State dispatch(final State state, final DocumentTransferProcess process, final long eventTime) {
			return state.onDocumentPreparationTimeout(process, eventTime);
		}
	},
	
	DOCUMENT_PREPARATION_FAILED("document-preparation-failed") {
		@Override
		public State dispatch(final State state, final DocumentTransferProcess process, final long eventTime) {
			return state.onDocumentPreparationFailed(process, eventTime);
		}
	},
	
	DOCUMENT_PREPARED("bus-message-sending") {
		@Override
		public State dispatch(final State state, final DocumentTransferProcess process, final long eventTime) {
			return state.onDocumentPrepared(process, eventTime);
		}
	},
	
	DOCUMENT_SEND_TIMEOUT("document-send-timeout") {
		@Override
		public State dispatch(final State state, final DocumentTransferProcess process, final long eventTime) {
			return state.onDocumentSendTimeout(process, eventTime);
		}
	},		
	DOCUMENT_SEND_FAILED("bus-message-send-failed") {
		@Override
		public State dispatch(final State state, final DocumentTransferProcess process, final long eventTime) {
			return state.onDocumentSendFailed();
		}
	},
	DOCUMENT_SENT("bus-message-sent") {
		@Override
		public State dispatch(final State state, final DocumentTransferProcess process, final long eventTime) {
			return state.onDocumentSent(process, eventTime);
		}
	},
	
	INF_RESPONSE_TIMEOUT("inf-response-timeout") {
		@Override
		public State dispatch(final State state, final DocumentTransferProcess process, final long eventTime) {
			return state.onInfResponseTimeout(process, eventTime);
		}
	},
	INF_ACK_RECEIVED("inf-ack-received") {
		@Override
		public State dispatch(final State state, final DocumentTransferProcess process, final long eventTime) {
			return state.onInfAckReceived(process, eventTime);
		}
	},
	INF_NACK_RECEIVED("inf-nack-received") {
		@Override
		public State dispatch(final State state, final DocumentTransferProcess process, final long eventTime) {
			return state.onInfNackReceived();
		}
	},
	
	BUS_ACK_RECEIVED("bus-ack-received") {
		@Override
		public State dispatch(final State state, final DocumentTransferProcess process, final long eventTime) {
			return state.onBusAckReceived(process, eventTime);
		}
	},
	BUS_NACK_RECEIVED("bus-nack-received") {
		@Override
		public State dispatch(final State state, final DocumentTransferProcess process, final long eventTime) {
			return state.onBusNackReceived();
		}
	},
	BUS_RESPONSE_TIMEOUT("bus-response-timeout") {
		@Override
		public State dispatch(final State state, final DocumentTransferProcess process, final long eventTime) {
			return state.onBusResponseTimeout(process, eventTime);
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
	
	public abstract State dispatch(final State state, final DocumentTransferProcess process, final long eventTime);
}