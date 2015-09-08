package uk.nhs.ciao.docs.finalizer;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.nhs.ciao.docs.finalizer.state.DocumentTransferProcess;
import uk.nhs.ciao.docs.finalizer.state.DocumentTransferProcessFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class InProgressDirectoryPoller {
	private static final Logger LOGGER = LoggerFactory.getLogger(InProgressDirectoryPoller.class);
	
	private final DocumentTransferProcessFactory factory;
	private final File inProgressDirectory;
	private final Map<String, DocumentTransferState> stateByCorrelationId = Maps.newHashMap();
	
	public InProgressDirectoryPoller(final DocumentTransferProcessFactory factory, final File inProgressDirectory) {
		this.factory = Preconditions.checkNotNull(factory);
		this.inProgressDirectory = Preconditions.checkNotNull(inProgressDirectory);
	}
	
	public void poll(final long now) {
		final String[] correlationIds = inProgressDirectory.list();
		for (int index = 0; index < correlationIds.length; index++) {
			final String correlationId = correlationIds[index];
			final File processDirectory = new File(inProgressDirectory, correlationId);
			if (!processDirectory.isDirectory()) {
				continue;
			}

			LOGGER.debug("Processing in-progress folder - correlationId: {}", correlationId);

			DocumentTransferState state = stateByCorrelationId.get(correlationId);
			if (state == null) {
				final DocumentTransferProcess process = factory.createDocumentTransferProcess(correlationId, processDirectory);
				state = new DocumentTransferState(process);
				stateByCorrelationId.put(correlationId, state);
			}

			processControlFiles(state.process, state.processedFileNames);
			processEventFiles(state.process, state.processedFileNames);
		}
		
		// Clean state map (i.e. remove entries with no matching in-progress folder)
		if (!stateByCorrelationId.isEmpty()) {
			stateByCorrelationId.keySet().retainAll(Arrays.asList(correlationIds));
		}		

		if (!stateByCorrelationId.isEmpty()) {
			for (final DocumentTransferState state : stateByCorrelationId.values()) {
				processTimeouts(now, state.process);
			}
		}
	}

	private void processControlFiles(final DocumentTransferProcess process,
			final Set<String> processedFileNames) {
		final File controlDirectory = new File(process.getRootFolder(), "control");
		if (!controlDirectory.isDirectory()) {
			return;
		}
		
		final String[] fileNames = controlDirectory.list();
		for (int index = 0; index < fileNames.length; index++) {
			final String fileName = fileNames[index];
			if (processedFileNames.contains(fileNames)) {
				continue;
			}
			
			processedFileNames.add(fileName);
			
			final File controlFile = new File(controlDirectory, fileName);
			if (controlFile.isFile()) {
				process.registerControlFile(controlFile);
			}
		}
	}
	
	private void processEventFiles(final DocumentTransferProcess process, final Set<String> processedFileNames) {
		final File eventsDirectory = new File(process.getRootFolder(), "events");
		if (!eventsDirectory.isDirectory()) {
			return;
		}
		
		final String[] fileNames = eventsDirectory.list();
		if (fileNames.length == 0) {
			return;
		}
		
		/*
		 * process in order of ascending time-stamps
		 * the format of file-names allows a lexical sort to be used
		 */
		Arrays.sort(fileNames);
		
		for (int index = 0; index < fileNames.length; index++) {
			final String fileName = fileNames[index];
			
			/*
			 * Ignore any processed files
			 * (it may be more efficient to strip processed files prior to sorting)
			 */
			if (processedFileNames.contains(fileNames)) {
				continue;
			}
			
			processedFileNames.add(fileName);
			
			final File eventFile = new File(eventsDirectory, fileName);
			if (eventFile.isFile()) {
				process.registerEventsFile(eventFile);
			}
		}
	}
	
	private void processTimeouts(final long now, final DocumentTransferProcess process) {
		process.processTimeouts(now);
	}
	
	/**
	 * Maintains state of an in-progress document transfer and previously processed files
	 */
	private static class DocumentTransferState {
		final DocumentTransferProcess process;
		final Set<String> processedFileNames;
		
		public DocumentTransferState(final DocumentTransferProcess process) {
			this.process = Preconditions.checkNotNull(process);
			this.processedFileNames = Sets.newHashSet();
		}
	}
}
