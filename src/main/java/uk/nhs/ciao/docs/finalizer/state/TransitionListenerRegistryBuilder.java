package uk.nhs.ciao.docs.finalizer.state;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import joptsimple.internal.Strings;

import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.FactoryBean;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Spring factory bean to simplify the creation of a {@link TransitionListenerRegistry}
 * with options to make delegate notifications idempotent.
 * <p>
 * Rule syntax:
 * <ul>
 * <li>to-state rule - <code>to=${STATE} > ${actionClassName}</code>
 * <ul>
 * Multiple rules can be specified via {@link #setRules(String)}, where rules are separated by a new-line.
 * <p>
 * The <code>defaultPackage</code> property is used to determine the full class name to load if no package
 * has been specified. Action classes should provide a no-argument, default constructor.
 */
public class TransitionListenerRegistryBuilder implements FactoryBean<TransitionListenerRegistry> {
	private static final Pattern NEW_LINE_PATTERN = Pattern.compile("[\\r\\n]+", Pattern.MULTILINE);
	private static final Pattern TO_STATE_RULE_PATTERN = Pattern.compile("to=(\\w+)\\s*>\\s*([\\w\\.]+)");
	
	private final Map<State, List<TransitionListener>> toStateListeners = Maps.newLinkedHashMap();
	private boolean idempotent;
	private String idempotentTargetUri;
	private ProducerTemplate producerTemplate;
	private String defaultPackage = "uk.nhs.ciao.docs.finalizer.action";
	
	@Override
	public boolean isSingleton() {
		return true;
	}
	
	@Override
	public Class<?> getObjectType() {
		return TransitionListenerRegistry.class;
	}
	
	public void setIdempotent(final boolean idempotent) {
		this.idempotent = idempotent;
	}
	
	public void setIdempotentTargetUri(final String idempotentTargetUri) {
		this.idempotentTargetUri = idempotentTargetUri;
	}
	
	public void setProducerTemplate(final ProducerTemplate producerTemplate) {
		this.producerTemplate = producerTemplate;
	}
	
	public void setDefaultPackage(final String defaultPackage) {
		this.defaultPackage = Preconditions.checkNotNull(defaultPackage);
	}
	
	public void addToStateListener(final State state, final TransitionListener listener) {
		Preconditions.checkNotNull(state);
		Preconditions.checkNotNull(listener);
		
		List<TransitionListener> listeners = toStateListeners.get(state);
		if (listeners == null) {
			listeners = Lists.newArrayList();
			toStateListeners.put(state, listeners);
		}
		listeners.add(listener);
	}
	
	/**
	 * Interprets string-encoded rules
	 * <p>
	 * Each new line is treated as separate rule
	 * 
	 * @see #addRule(String)
	 */
	public void setRules(final String rules) throws ClassNotFoundException, InstantiationException, IllegalAccessException  {
		if (rules == null) {
			return;
		}
		
		for (final String rule: NEW_LINE_PATTERN.split(rules)) {
			addRule(rule);
		}
	}
	
	/**
	 * Interprets a string-encoded rule
	 * e.g. <code>to=SUCCEEDED > MoveToCompletedFolder</code>
	 */
	public void addRule(final String rule) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		if (rule == null || rule.trim().isEmpty()) {
			return;
		}
		
		final Matcher matcher = TO_STATE_RULE_PATTERN.matcher(rule);
		if (matcher.find()) {
			final State state = State.valueOf(matcher.group(1));
			final String className = getFullClassName(matcher.group(2));			

			final Class<?> clazz = Class.forName(className);
			final TransitionListener listener = (TransitionListener)clazz.newInstance();
			addToStateListener(state, listener);
		}
	}
	
	@Override
	public TransitionListenerRegistry getObject() throws Exception {
		final TransitionListenerRegistry registry = new TransitionListenerRegistry();
		
		for (final Entry<State, List<TransitionListener>> entry: toStateListeners.entrySet()) {
			TransitionListener listener = null;
			
			if (entry.getValue().size() == 1) {
				listener = entry.getValue().get(0);
			} else {
				listener = new TransitionListenerSet(entry.getValue());
			}			

			registry.addToStateListener(entry.getKey(), makeIdempotent(listener));
		}

		return registry;
	}
	
	private String getFullClassName(final String className) {		
		if (className.contains(".") || Strings.isNullOrEmpty(defaultPackage)) {
			return className;
		} else if (defaultPackage.endsWith(".")) {
			return defaultPackage + className;
		} else {
			return defaultPackage + "." + className;
		}
	}

	private TransitionListener makeIdempotent(final TransitionListener listener) {
		return idempotent ? listener : new IdempotentTransitionListener(
				idempotentTargetUri, producerTemplate, listener);
	}
}
