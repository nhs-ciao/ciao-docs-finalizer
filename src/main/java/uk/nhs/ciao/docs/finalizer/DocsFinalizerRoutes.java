package uk.nhs.ciao.docs.finalizer;

import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.spi.IdempotentRepository;

import uk.nhs.ciao.docs.finalizer.processor.InProgressDirectoryPoller;
import uk.nhs.ciao.docs.finalizer.route.IdempotentTransitionListenerRoute;
import uk.nhs.ciao.docs.finalizer.route.InProgressDirectoryPollerRoute;

public class DocsFinalizerRoutes implements RoutesBuilder {
	@Override
	public void addRoutesToCamelContext(final CamelContext context) throws Exception {
		addInProgressDirectoryPoller(context);
		addIdempotentTransitionListener(context);
	}
	
	public void addInProgressDirectoryPoller(final CamelContext context) throws Exception {
		final InProgressDirectoryPollerRoute route = new InProgressDirectoryPollerRoute();
		
		final InProgressDirectoryPoller inProgressDirectoryPoller = context.getRegistry().lookupByNameAndType(
				"inProgressDirectoryPoller", InProgressDirectoryPoller.class);
		
		route.setPeriod("{{inProgressDirectoryPollPeriod}}");
		route.setInProgressDirectoryPoller(inProgressDirectoryPoller);
		
		context.addRoutes(route);
	}
	
	public void addIdempotentTransitionListener(final CamelContext context) throws Exception {
		final IdempotentTransitionListenerRoute route = new IdempotentTransitionListenerRoute();
		
		final IdempotentRepository<?> idempotentRepository = context.getRegistry().lookupByNameAndType(
				"idempotentRepository", IdempotentRepository.class);
		
		route.setUri("direct:idempotentTransitionListener");
		route.setIdempotentRepository(idempotentRepository);
		
		context.addRoutes(route);
	}
}
