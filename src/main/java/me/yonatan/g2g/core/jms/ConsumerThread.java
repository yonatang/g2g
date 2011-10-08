package me.yonatan.g2g.core.jms;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import javax.enterprise.event.ObserverException;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;

import lombok.Getter;
import lombok.Synchronized;
import me.yonatan.g2g.core.cdi.Trace;
import me.yonatan.g2g.core.jms.annotation.JmsInbound;

import org.hornetq.api.core.HornetQException;
import org.hornetq.api.core.client.ClientConsumer;
import org.hornetq.api.core.client.ClientMessage;
import org.hornetq.core.client.impl.ClientConsumerImpl;
import org.slf4j.Logger;

@Trace
public class ConsumerThread implements Runnable {

	@Inject
	private Logger log;

	@Getter
	private String queueName;

	@Inject
	private BeanManager beanManager;

	private ClientConsumer clientConsumer;

	private boolean isRunning = false;

	@Synchronized
	public synchronized ConsumerThread setClientConsumer(ClientConsumer clientConsumer) {
		if (isRunning)
			throw new IllegalStateException("Can't set clientConsumer for running thread");
		this.clientConsumer = clientConsumer;
		return this;
	}

	@Override
	@Synchronized
	public synchronized void run() {
		isRunning = true;
		if (clientConsumer == null)
			throw new IllegalStateException("ClientConsumer wasn't initiated");

		if (clientConsumer instanceof ClientConsumerImpl) {
			queueName = ((ClientConsumerImpl) clientConsumer).getQueueName().toString();
		} else {
			queueName = "!!! UNKOWN !!!";
		}

		while (true) {
			log.debug("Listinging for queue {}...", queueName, clientConsumer);
			Thread.yield();

			try {
				ClientMessage cm = clientConsumer.receive();
				if (cm != null) {
					byte[] ba = cm.getBytesProperty(MessageConsumer.OBJECT_PROPERTY);

					ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(ba));
					Object messageObject = ois.readObject();
					log.debug("received message from queue queue {} - {}", queueName, messageObject);
					@SuppressWarnings("serial")
					AnnotationLiteral<JmsInbound> jmsInboudnLiteral = new AnnotationLiteral<JmsInbound>() {
					};
					beanManager.fireEvent(messageObject, jmsInboudnLiteral);
				} else {
					log.warn("ClientMessage receieved a null ClientMessage from queue {}", queueName);
				}
			} catch (ObserverException e) {
				log.warn("Exception was thrown in one of the observers {}", queueName, e);
			} catch (IOException e) {
				log.warn("Could not deserilaize object from queue {} - {}", queueName, e);
			} catch (ClassNotFoundException e) {
				log.warn("Received unknown class from queue {} - {}", queueName, e);
			} catch (HornetQException e) {
				log.warn("Recieved hornetQ exception on queue {} - {}", queueName, e);
			} catch (Exception e) {
				log.warn("Exception was caught during queue {}", queueName, e);
			}

		}

	}
}
