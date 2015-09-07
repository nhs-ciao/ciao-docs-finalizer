package uk.nhs.ciao.docs.finalizer.state;

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