package me.yonatan.g2g.core.jms;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import lombok.Cleanup;
import me.yonatan.g2g.core.cdi.Trace;
import me.yonatan.g2g.core.event.JmsOutboundEvent;
import me.yonatan.g2g.core.jms.annotation.JmsOutbound;

import org.hornetq.api.core.HornetQException;
import org.hornetq.api.core.client.ClientMessage;
import org.hornetq.api.core.client.ClientProducer;
import org.hornetq.api.core.client.ClientSession;
import org.slf4j.Logger;

@ApplicationScoped
@Trace
public class ProducerManager {

	@Inject
	private ClientSession session;

	@Inject
	private ClientProducer producer;

	@Inject
	private Logger log;

	void sendMessage(@Observes @JmsOutbound JmsOutboundEvent event) throws HornetQException {
		ClientMessage m = session.createMessage(true);
		try {
			String targetQueueName = event.getTargetQueue();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			@Cleanup
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(event);
			oos.flush();

			m.putBytesProperty(MessageConsumer.OBJECT_PROPERTY, baos.toByteArray());
			log.debug("Sending {} to queue {}", event, targetQueueName);
			producer.send(targetQueueName, m);
		} catch (IOException e) {
			log.error("Couldn''t send event {} - {}", event, e.getMessage());
			throw new HornetQException(0, "", e);
		}
	}
}
