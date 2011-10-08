package me.yonatan.g2g.core.jms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import lombok.Getter;
import lombok.val;
import me.yonatan.g2g.core.cdi.Trace;

import org.hornetq.api.core.HornetQException;
import org.hornetq.api.core.client.ClientSession;

@ApplicationScoped
@Trace
public class MessageConsumer {

	static final String OBJECT_PROPERTY = "object";

	@Getter
	private String queueName;

	@Inject
	private ClientSession session;

	@Inject
	private Instance<ConsumerThread> ctc;

	private final List<Future<?>> threads = new ArrayList<Future<?>>();

	@Getter
	private boolean running = false;

	public List<Future<?>> getThreads() {
		return Collections.unmodifiableList(threads);
	}

	public void start(String queueName, int consumers) throws HornetQException {
		this.queueName = queueName;
		ExecutorService executorService = Executors.newFixedThreadPool(consumers);
		session.start();
		for (int i = 0; i < consumers; i++) {
			threads.add(executorService.submit(ctc.get().setClientConsumer(session.createConsumer(queueName))));
		}
		running = true;
	}

	@PreDestroy
	public void stop() throws HornetQException {
		for (val thread : threads) {
			thread.cancel(true);
		}
		session.stop();
		running = false;
	}

}
