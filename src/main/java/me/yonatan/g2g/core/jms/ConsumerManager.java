package me.yonatan.g2g.core.jms;

import java.util.List;
import java.util.concurrent.Future;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.New;
import javax.inject.Inject;

import lombok.val;
import me.yonatan.g2g.core.cdi.Trace;

import org.hornetq.api.core.HornetQException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@Trace
public class ConsumerManager {

	@Inject
	@New
	private MessageConsumer messageEnumConsumer;

	@Inject
	@New
	private MessageConsumer messageCopyConsumer;

	// public void onInit(@Observes JmsInit event) throws HornetQException {
	// messageEnumConsumer.start(JmsManager.MESSAGE_ENUMERATION_QUEUE, 1);
	// messageCopyConsumer.start(JmsManager.MESSAGE_COPY_QUEUE, 5);
	// Thread t = new Thread(new Watchdog());
	// t.start();
	// }

	public void init() throws HornetQException {
		messageEnumConsumer.start(JmsManager.MESSAGE_ENUMERATION_QUEUE, 1);
		messageCopyConsumer.start(JmsManager.MESSAGE_COPY_QUEUE, 5);
		Thread t = new Thread(new Watchdog());
		t.start();
	}

	private class Watchdog implements Runnable {
		private Logger log = LoggerFactory.getLogger(Watchdog.class);

		@Override
		public void run() {
			boolean stop = false;
			while (!stop) {
				try {
					List<Future<?>> copyConsumerFutures = messageCopyConsumer.getThreads();
					List<Future<?>> enumFutures = messageEnumConsumer.getThreads();
					printFutureStatus(JmsManager.MESSAGE_ENUMERATION_QUEUE, enumFutures);
					printFutureStatus(JmsManager.MESSAGE_COPY_QUEUE, copyConsumerFutures);
					Thread.sleep(10000);
				} catch (InterruptedException e) {
					stop = true;
				}
			}
		}

		private void printFutureStatus(String queue, List<Future<?>> futures) {
			int threads = futures.size();
			int active = 0;
			for (val future : futures) {
				if (!future.isCancelled() && !future.isDone())
					++active;
			}
			log.debug("Queue {} state: {}/{}", new Object[] { queue, active, threads });
		}

	}

}
