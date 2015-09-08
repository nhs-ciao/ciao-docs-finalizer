package uk.nhs.ciao.docs.finalizer;

import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.spi.IdempotentRepository;

import uk.nhs.ciao.docs.finalizer.processor.InProgressFolderPoller;
import uk.nhs.ciao.docs.finalizer.route.IdempotentTransitionListenerRoute;
import uk.nhs.ciao.docs.finalizer.route.InProgressFolderPollerRoute;

public class DocsFinalizerRoutes implements RoutesBuilder {
	@Override
	public void addRoutesToCamelContext(final CamelContext context) throws Exception {
		addInProgressFolderPoller(context);
		addIdempotentTransitionListener(context);
	}
	
	private void addInProgressFolderPoller(final CamelContext context) throws Exception {
		final InProgressFolderPollerRoute route = new InProgressFolderPollerRoute();
		
		final InProgressFolderPoller inProgressDirectoryPoller = context.getRegistry().lookupByNameAndType(
				"inProgressFolderPoller", InProgressFolderPoller.class);
		
		route.setPeriod("{{inProgressFolderPollPeriod}}");
		route.setInProgressFolderPoller(inProgressDirectoryPoller);
		
		context.addRoutes(route);
	}
	
	private void addIdempotentTransitionListener(final CamelContext context) throws Exception {
		final IdempotentTransitionListenerRoute route = new IdempotentTransitionListenerRoute();
		
		final IdempotentRepository<?> idempotentRepository = context.getRegistry().lookupByNameAndType(
				"idempotentRepository", IdempotentRepository.class);
		
		route.setUri("direct:idempotentTransitionListener");
		route.setIdempotentRepository(idempotentRepository);
		
		context.addRoutes(route);
	}
}
