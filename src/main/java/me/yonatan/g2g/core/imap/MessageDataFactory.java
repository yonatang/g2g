package me.yonatan.g2g.core.imap;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.mail.Message;
import javax.mail.MessagingException;

import me.yonatan.g2g.core.cdi.Trace;
import me.yonatan.g2g.core.model.MessageData;

import org.slf4j.Logger;

@ApplicationScoped
@Trace
public class MessageDataFactory {

	@Inject
	private ImapConnector imap;

	@Inject
	private Logger log;

	public MessageData create(Message message) throws MessagingException {
		int id = message.getMessageNumber();
		long uid = imap.getMessageUID(message);
		String gmailId = imap.getMessageGmailId(uid);
		MessageData md = new MessageData(gmailId, uid, id);
		log.debug("MessageData - {}", md);
		return md;
	}
}
