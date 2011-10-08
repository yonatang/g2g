package me.yonatan.g2g.core;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.mail.Message;
import javax.mail.MessagingException;

import me.yonatan.g2g.core.cdi.Trace;
import me.yonatan.g2g.core.event.CopyMessageEvent;
import me.yonatan.g2g.core.event.FetchMessageEvent;
import me.yonatan.g2g.core.event.JmsInit;
import me.yonatan.g2g.core.imap.ImapConnector;
import me.yonatan.g2g.core.imap.MessageDataFactory;
import me.yonatan.g2g.core.jms.annotation.JmsInbound;
import me.yonatan.g2g.core.jms.annotation.JmsOutbound;
import me.yonatan.g2g.core.model.GmailCredentials;
import me.yonatan.g2g.core.model.MessageData;
import me.yonatan.g2g.core.model.SourceAccount;

import org.slf4j.Logger;

@ApplicationScoped
@Trace
public class MessageEnumerator extends MessageHandler {

	private static final int MAX_RETRIES = 5;

	@Inject
	private ImapConnector imap;

	@Inject
	private MessageDataFactory mdf;

	@Inject
	@JmsOutbound
	private Event<FetchMessageEvent> fetchMessageEvent;

	@Inject
	@JmsOutbound
	private Event<CopyMessageEvent> copyMessageEvent;

	@Inject
	private Logger log;

	@Inject
	@SourceAccount
	private Instance<GmailCredentials> sourceCredentialsInstance;

	public void start() {
		try {
			reconnect();
			// TODO - check in DB what is the last message to process
			Message firstMessage = imap.getMessageById(1);

			MessageData md = mdf.create(firstMessage);
			System.out.println(md);
			fetchMessageEvent.fire(new FetchMessageEvent(md));

		} catch (MessagingException e1) {
			e1.printStackTrace();
		}
	}

	public void start(@Observes JmsInit e) {
		start();
	}

	void reconnect() throws MessagingException {
		GmailCredentials sourceCredentials = sourceCredentialsInstance.get();
		imap.connect(sourceCredentials);
		log.info("Connected to {} succesfully using", sourceCredentials, imap);
	}

	public void nextMessage(@Observes @JmsInbound FetchMessageEvent event) throws MessagingException {
		// TODO Handle situation where that message doesn't exists anymore.
		// Guess which message is next or something (might happen if next
		// enumerated message is deleted during run)

		try {
			MessageData messageData = event.getMessageData();
			copyMessageEvent.fire(new CopyMessageEvent(messageData));

			Message message = imap.getMessageByGID(messageData.getGid());
			if (log.isDebugEnabled())
				log.debug("Starting with message gid {}", messageData.getGid());
			int count = imap.getMessageCount();
			if (count > message.getMessageNumber()) {
				Message nextMessage = imap.getMessageById(message.getMessageNumber() + 1);
				MessageData md = mdf.create(nextMessage);
				fetchMessageEvent.fire(new FetchMessageEvent(md));
			} else {
				System.out.println("DONE SCANNING MAILBOX!");
			}
			log.info("Message {} was enumorated succesfully", messageData.getGid());
		} catch (Exception e) {
			handleMessageException(e, fetchMessageEvent, event);
		}
	}

	@Override
	int getMaxRetries() {
		return MAX_RETRIES;
	}
}
