package me.yonatan.g2g.core.jms;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import lombok.Cleanup;
import lombok.val;
import me.yonatan.g2g.core.cdi.Trace;

import org.hornetq.api.core.HornetQException;
import org.hornetq.api.core.SimpleString;
import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.api.core.client.ClientProducer;
import org.hornetq.api.core.client.ClientSession;
import org.hornetq.api.core.client.ClientSessionFactory;
import org.hornetq.api.core.client.HornetQClient;
import org.hornetq.api.core.client.ServerLocator;
import org.hornetq.core.config.Configuration;
import org.hornetq.core.config.impl.FileConfiguration;
import org.hornetq.core.remoting.impl.invm.InVMAcceptorFactory;
import org.hornetq.core.remoting.impl.invm.InVMConnectorFactory;
import org.hornetq.core.server.HornetQServer;
import org.hornetq.core.server.HornetQServers;
import org.hornetq.core.server.JournalType;
import org.slf4j.Logger;

@ApplicationScoped
@Trace
public class JmsManager {

	public static final String MESSAGE_ENUMERATION_QUEUE = "queue.messageEnumerationQueue";

	public static final String MESSAGE_COPY_QUEUE = "queue.messageCopyQueue";

	private String[] queueNames = new String[] { MESSAGE_ENUMERATION_QUEUE, MESSAGE_COPY_QUEUE };

	@Inject
	private Logger log;

	private HornetQServer server;

	@Inject
	private ConsumerManager consumerManager;

	// @Inject
	// private Event<JmsInit> jmsInitEvent;

	private ServerLocator serverLocator;

	/*
	 * Producers and disposers for HornetQ elements (such as ClientSession,
	 * ClientProducer, etc.)
	 */

	@Produces
	ClientSessionFactory produceClientSessionFactory() throws Exception {
		return serverLocator.createSessionFactory();
	}

	void clientSessionFactoryDestructor(@Disposes ClientSessionFactory csf) {
		csf.close();
	}

	@Produces
	ClientSession produceClientSession(ClientSessionFactory csf) throws HornetQException {
		return csf.createSession();
	}

	void clinetSessionDestructor(@Disposes ClientSession cs) throws HornetQException {
		cs.close();
	}

	@Produces
	ClientProducer produceClientProducer(ClientSession cs) throws HornetQException {
		return cs.createProducer();
	}

	void clientProducerDestructor(@Disposes ClientProducer cp) throws HornetQException {
		cp.close();
	}

	/**
	 * Initiate the server and the queues on startup of the application. Fire
	 * JmsInit when everything is ready.
	 * 
	 * @param event
	 */
	// void onContainerStart(@Observes ContainerInitialized event) {
	// // try {
	// // initiateServer();
	// // initiateQueues();
	// // jmsInitEvent.fire(new JmsInit());
	// // } catch (Exception e) {
	// // e.printStackTrace();
	// // } finally {
	// // }
	// }

	public void init() throws Exception {
		initiateServer();
		initiateQueues();
		consumerManager.init();
	}

	/**
	 * Shutdown everything on close
	 */
	@PreDestroy
	void onStop() {
		if (server != null)
			try {
				server.stop();
			} catch (Exception e) {
			}
		if (serverLocator != null)
			serverLocator.close();
	}

	/**
	 * initiate the HornetQ server
	 * 
	 * @throws Exception
	 */
	private void initiateServer() throws Exception {
		Configuration serverConfig = new FileConfiguration();
		serverConfig.setPersistenceEnabled(false);
		serverConfig.setSecurityEnabled(false);
		serverConfig.getAcceptorConfigurations().add(new TransportConfiguration(InVMAcceptorFactory.class.getName()));
		serverConfig.setJournalType(JournalType.NIO);
		server = HornetQServers.newHornetQServer(serverConfig);
		server.start();
	}

	/**
	 * Verify that the required queues exists. If not, create them.
	 * 
	 * @throws Exception
	 */
	private void initiateQueues() throws Exception {
		serverLocator = HornetQClient.createServerLocatorWithoutHA(new TransportConfiguration(InVMConnectorFactory.class.getName()));
		@Cleanup
		val sf = serverLocator.createSessionFactory();
		@Cleanup
		val coreSession = sf.createSession();
		for (val queueName : queueNames) {
			if (!coreSession.queueQuery(new SimpleString(queueName)).isExists()) {
				coreSession.createQueue(queueName, queueName, true);
				log.info("Queue {} created", queueName);
			}
		}
	}

}
