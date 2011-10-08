package me.yonatan.g2g.core;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.mail.MessagingException;

import org.slf4j.Logger;

import me.yonatan.g2g.core.event.JmsOutboundEvent;

public abstract class MessageHandler {
	
	abstract int getMaxRetries();
	
	abstract void reconnect() throws MessagingException;

	@Inject
	private Logger log;

	<T extends JmsOutboundEvent> void handleMessageException(Exception e,Event<T> eventSender, T eventPayload) {
		log.warn("Exception was thrown during handling of event {} - {}. Reconnecting", eventPayload, e);
		try {
			reconnect();
		} catch (Exception e1) {
			// reconnection error - indication for network problem
			log.warn("Cannot reconnect. sleeping for a while");
			eventSender.fire(eventPayload);
			return;
		}
		if (eventPayload.getRetryCount() > getMaxRetries()) {
			// TODO - keep track in applicative log (DB)
			log.warn("Event {} was dropped from copying, due to too much retries", eventPayload);
		} else {
			log.info("Resending event {}", eventPayload);
			eventPayload.increaseRetryCount();
			eventSender.fire(eventPayload);
		}

	}

}
