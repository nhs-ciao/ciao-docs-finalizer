package uk.nhs.ciao.docs.finalizer.route;

import org.apache.camel.Body;
import org.apache.camel.Header;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spi.IdempotentRepository;

import uk.nhs.ciao.docs.finalizer.state.DocumentTransferProcess;
import uk.nhs.ciao.docs.finalizer.state.IdempotentTransitionListener;
import uk.nhs.ciao.docs.finalizer.state.Transition;
import uk.nhs.ciao.docs.finalizer.state.TransitionListener;

/**
 * Configures a direct camel route to notify a single listener when a state
 * transition occurs.
 * <p>
 * The route uses a backing {@link IdempotentRepository} and the Idempotent Consumer pattern
 * to ensure that only one listener performs the associated action.
 * 
 * @see IdempotentTransitionListener
 */
public class IdempotentTransitionListenerRoute extends RouteBuilder {
	private String uri;
	private IdempotentRepository<?> idempotentRepository;
	
	public void setUri(final String uri) {
		this.uri = uri;
	}
	
	public void setIdempotentRepository(final IdempotentRepository<?> idempotentRepository) {
		this.idempotentRepository = idempotentRepository;
	}
	
	@Override
	public void configure() throws Exception {
		from(uri)
			.convertBodyTo(TransitionListener.class)
			.idempotentConsumer(header(IdempotentTransitionListener.HEADER_ID), idempotentRepository)
			.bean(new TransitionListenerInvoker())
		.end();
	}
	
	// bean methods - in separate class from the main route to prevent loops in the tracer
	
	public static class TransitionListenerInvoker {
		public void invoke(@Body final TransitionListener listener,
				@Header(IdempotentTransitionListener.HEADER_PROCESS) final DocumentTransferProcess process,
				@Header(IdempotentTransitionListener.HEADER_TRANSITION) final Transition transition) {
			listener.onTransition(process, transition);
		}
	}
}
