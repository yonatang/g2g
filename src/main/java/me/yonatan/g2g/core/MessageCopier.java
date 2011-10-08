package me.yonatan.g2g.core;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.New;
import javax.inject.Inject;
import javax.mail.Flags;
import javax.mail.Message;
import javax.mail.MessagingException;

import me.yonatan.g2g.core.cdi.Trace;
import me.yonatan.g2g.core.event.CopyMessageEvent;
import me.yonatan.g2g.core.imap.ImapConnector;
import me.yonatan.g2g.core.jms.annotation.JmsInbound;
import me.yonatan.g2g.core.jms.annotation.JmsOutbound;
import me.yonatan.g2g.core.model.GmailCredentials;
import me.yonatan.g2g.core.model.SourceAccount;
import me.yonatan.g2g.core.model.TargetAccount;

import org.jboss.weld.environment.se.contexts.ThreadScoped;
import org.slf4j.Logger;

@ThreadScoped
@Trace
public class MessageCopier extends MessageHandler {
	// ThreadScope promises us to have a MessageCopier per consumerThread (Which
	// will not create plenty of connections)

	private static final int MAX_RETRIES = 5;

	@Inject
	private Logger log;

	@Inject
	@New
	private ImapConnector source;

	@Inject
	@New
	private ImapConnector target;

	@Inject
	@JmsOutbound
	private Event<CopyMessageEvent> outCopyMessageEvent;

	@Inject
	@SourceAccount
	private Instance<GmailCredentials> sourceCredentialsInstance;

	@Inject
	@TargetAccount
	private Instance<GmailCredentials> targetCredentialsInstance;

	@PostConstruct
	void initConnectors() throws MessagingException {
		reconnect();
	}

	void reconnect() throws MessagingException {

		GmailCredentials sourceCredentials = sourceCredentialsInstance.get();
		GmailCredentials targetCredentials = targetCredentialsInstance.get();

		source.connect(sourceCredentials);
		log.info("Connected to {} succesfully using {}", sourceCredentials, source);
		target.connect(targetCredentials, true);
		log.info("Connected to {} succesfully using {}", targetCredentials, target);
	}

	public void copyMessage(@Observes @JmsInbound CopyMessageEvent event) throws MessagingException {
		// TODO verify against DB we didn't handled/partial handled it
		// TODO support custom stars

		try {
			String gid = event.getMessageData().getGid();
			log.info("Starting to copy message {}", gid);
			Message message = source.getMessageByGID(gid);
			long uid = source.getMessageUID(message);
			Flags flags = message.getFlags();

			List<String> labels = source.getMessageLabels(uid);
			log.debug("Message {} has {} labels", gid, labels.size());

			Message[] clonedMessages = target.addMessage(message);
			Message clonedMessage = clonedMessages[0];
			log.debug("Message {} was copied from {} to {}", new Object[] { gid, source, target });
			long newUid = target.getMessageUID(clonedMessage);
			String newGid = target.getMessageGmailId(newUid);
			log.debug("New message gid {}, new message uid {} at {}", new Object[] { newGid, newUid, target });

			clonedMessage.getFlags().add(flags);
			List<String> newLabels = target.addMessageLabels(clonedMessage, labels);
			log.debug("Cloned message labels applied. Message {} have new {} labels", newGid, newLabels.size());
			log.info("Message {} was copied to {} successfully from {} to {}", new Object[] { gid, newGid, source, target });
		} catch (Exception e) {
			handleMessageException(e, outCopyMessageEvent, event);
		}
	}

	@Override
	int getMaxRetries() {
		return MAX_RETRIES;
	}
}
