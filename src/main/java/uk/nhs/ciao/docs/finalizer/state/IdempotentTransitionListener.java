package uk.nhs.ciao.docs.finalizer.state;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.spi.IdempotentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * A transition listener which ensures a delegate listener is only executed once
 * by using a {@link IdempotentRepository} configured Camel route
 */
public class IdempotentTransitionListener implements TransitionListener {
	private static final Logger LOGGER = LoggerFactory.getLogger(IdempotentTransitionListener.class);
	
	public static final String HEADER_ID = "ciao.idempotentTransitionListener.id";
	public static final String HEADER_PROCESS = "ciao.idempotentTransitionListener.process";
	public static final String HEADER_TRANSITION = "ciao.idempotentTransitionListener.transition";
	
	private final String targetUri;
	private final ProducerTemplate producerTemplate;
	private final TransitionListener delegate;
	
	public IdempotentTransitionListener(final String targetUri, final ProducerTemplate producerTemplate,
			final TransitionListener delegate) {
		this.targetUri = Preconditions.checkNotNull(targetUri);
		this.producerTemplate = Preconditions.checkNotNull(producerTemplate);
		this.delegate = Preconditions.checkNotNull(delegate);
	}
	
	@Override
	public void onTransition(final DocumentTransferProcess process, final Transition transition) {
		final String id = calculateId(process, transition);
		
		final Exchange exchange = new DefaultExchange(producerTemplate.getCamelContext());
		
		final Message in = exchange.getIn();
		in.setHeader(HEADER_ID, id);
		in.setHeader(HEADER_PROCESS, process);
		in.setHeader(HEADER_TRANSITION, transition);
		in.setBody(delegate);
		
		producerTemplate.send(targetUri, exchange);
		if (exchange.getException() != null) {
			LOGGER.error("Unable to process transition - correlationId: {}", process.getCorrelationId(), exchange.getException());
		}
	}
	
	private String calculateId(final DocumentTransferProcess process, final Transition transition) {
		return process.getCorrelationId() + ":" + transition.getEvent() + ":" + transition.getFromState() + ":" + transition.getToState();
	}
}
