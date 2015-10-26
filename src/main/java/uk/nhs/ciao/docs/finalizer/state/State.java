package uk.nhs.ciao.docs.finalizer.state;

public enum State {
	PARSING(false) {
		@Override
		public State onDocumentParsed(final DocumentTransferProcess process, final long eventTime) {
			process.getDocumentPreparationTimeout().start(eventTime);
			return PREPARING;
		}
	},
	
	PREPARING(false) {
		@Override
		public State onDocumentPreparationTimeout(final DocumentTransferProcess process, final long eventTime) {
			process.getDocumentPreparationTimeout().cancel();
			return FAILED;
		}
		
		@Override
		public State onDocumentPreparationFailed(final DocumentTransferProcess process, final long eventTime) {
			process.getDocumentPreparationTimeout().cancel();
			return FAILED;
		}
		
		@Override
		public State onDocumentPrepared(final DocumentTransferProcess process, final long eventTime) {
			process.getDocumentPreparationTimeout().cancel();
			process.getDocumentSendTimeout().start(eventTime);
			return SENDING;
		}
	},
	
	SENDING(false) {
		@Override
		public State onDocumentSendTimeout(final DocumentTransferProcess process, final long eventTime) {
			process.getDocumentSendTimeout().cancel();
			return FAILED;
		}
		
		@Override
		public State onDocumentSendFailed(final DocumentTransferProcess process, final long eventTime) {
			return FAILED;
		}
		
		@Override
		public State onDocumentSent(final DocumentTransferProcess process, final long eventTime) {
			process.getDocumentSendTimeout().cancel();
			
			if (process.isInfAckWanted()) {
				process.getInfResponseTimeout().start(eventTime);
			}
			
			if (process.isBusAckWanted()) {
				process.getBusResponseTimeout().start(eventTime);
			}
			
			if (process.isInfAckWanted() && process.isBusAckWanted()) {
				return WAITING_INF_AND_BUS_RESPONSE;
			} else if (process.isInfAckWanted()) {
				return WAITING_INF_RESPONSE;
			} else if (process.isBusAckWanted()) {
				return WAITING_BUS_RESPONSE;
			} else {
				return SUCCEEDED;
			}
		}
	},
	
	WAITING_INF_AND_BUS_RESPONSE(false) {
		@Override
		public State onDocumentSendFailed(final DocumentTransferProcess process, final long eventTime) {
			process.getInfResponseTimeout().cancel();
			return FAILED;
		}
		
		@Override
		public State onInfAckReceived(final DocumentTransferProcess process, final long eventTime) {
			process.getInfResponseTimeout().cancel();
			return WAITING_BUS_RESPONSE;
		}
		
		@Override
		public State onInfNackReceived() {
			return FAILED;
		}
		
		@Override
		public State onInfResponseTimeout(final DocumentTransferProcess process, final long eventTime) {
			process.getInfResponseTimeout().cancel();
			return FAILED;
		}
		
		@Override
		public State onBusAckReceived(final DocumentTransferProcess process, final long eventTime) {
			process.getBusResponseTimeout().cancel();
			return WAITING_INF_RESPONSE;
		}
		
		@Override
		public State onBusNackReceived() {
			return FAILED;
		}
		
		@Override
		public State onBusResponseTimeout(final DocumentTransferProcess process, final long eventTime) {
			process.getBusResponseTimeout().cancel();
			return FAILED;
		}
	},
	
	WAITING_INF_RESPONSE(false) {
		@Override
		public State onDocumentSendFailed(final DocumentTransferProcess process, final long eventTime) {
			process.getInfResponseTimeout().cancel();
			return FAILED;
		}
		
		@Override
		public State onInfAckReceived(final DocumentTransferProcess process, final long eventTime) {
			process.getInfResponseTimeout().cancel();
			return SUCCEEDED;
		}
		
		@Override
		public State onInfNackReceived() {
			return FAILED;
		}
		
		@Override
		public State onInfResponseTimeout(final DocumentTransferProcess process, final long eventTime) {
			process.getInfResponseTimeout().cancel();
			return FAILED;
		}
	},
	
	WAITING_BUS_RESPONSE(false) {
		@Override
		public State onDocumentSendFailed(final DocumentTransferProcess process, final long eventTime) {
			process.getBusResponseTimeout().cancel();
			return FAILED;
		}
		
		@Override
		public State onBusAckReceived(final DocumentTransferProcess process, final long eventTime) {
			process.getBusResponseTimeout().cancel();
			return SUCCEEDED;
		}
		
		@Override
		public State onBusNackReceived() {
			return FAILED;
		}
		
		@Override
		public State onBusResponseTimeout(final DocumentTransferProcess process, final long eventTime) {
			process.getBusResponseTimeout().cancel();
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
	
	public State onDocumentParsed(final DocumentTransferProcess process, final long eventTime) {
		return this;
	}
	
	public State onDocumentPreparationTimeout(final DocumentTransferProcess process, final long eventTime) {
		return this;
	}
	
	public State onDocumentPreparationFailed(final DocumentTransferProcess process, final long eventTime) {
		return this;
	}
	
	public State onDocumentPrepared(final DocumentTransferProcess process, final long eventTime) {
		return this;
	}
	
	public State onDocumentSendTimeout(final DocumentTransferProcess process, final long eventTime) {
		return this;
	}
	
	public State onDocumentSendFailed(final DocumentTransferProcess process, final long eventTime) {
		return this;
	}
	
	public State onDocumentSent(final DocumentTransferProcess process, final long eventTime) {
		return this;
	}
	
	public State onInfResponseTimeout(final DocumentTransferProcess process, final long eventTime) {
		return this;
	}
	
	public State onInfAckReceived(final DocumentTransferProcess process, final long eventTime) {
		return this;
	}
	
	public State onInfNackReceived() {
		return this;
	}
	
	public State onBusAckReceived(final DocumentTransferProcess process, final long eventTime) {
		return this;
	}
	
	public State onBusNackReceived() {
		return this;
	}
	
	public State onBusResponseTimeout(final DocumentTransferProcess process, final long eventTime) {
		return this;
	}
}