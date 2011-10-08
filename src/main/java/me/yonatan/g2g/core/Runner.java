package me.yonatan.g2g.core;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import lombok.Getter;
import lombok.Setter;
import me.yonatan.g2g.core.cdi.Trace;
import me.yonatan.g2g.core.jms.JmsManager;
import me.yonatan.g2g.core.model.GmailCredentials;
import me.yonatan.g2g.core.model.SourceAccount;
import me.yonatan.g2g.core.model.TargetAccount;

@Trace
@ApplicationScoped
public class Runner {

	@Setter
	@Getter
	private GmailCredentials sourceAccount;

	@Setter
	@Getter
	private GmailCredentials targetAccount;

	@Inject
	private JmsManager jmsManager;

	@Inject
	private MessageEnumerator messageEnumerator;

	@Produces
	@TargetAccount
	GmailCredentials produceSourceCredentials() {
		return sourceAccount;
	}

	@Produces
	@SourceAccount
	GmailCredentials produceTargetCredentials() {
		return targetAccount;
	}

	public void start() {
		try {
			// TODO verify if the queue has active tasks, and therefore no need
			// to initiate the messageEnumerator
			jmsManager.init();
			messageEnumerator.start();
		} catch (Exception e) {
			System.out.println("EXCEPTION! " + e);
		}
	}
}
