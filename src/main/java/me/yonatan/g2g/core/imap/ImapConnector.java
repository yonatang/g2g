package me.yonatan.g2g.core.imap;

import java.util.List;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.ToString;
import me.yonatan.g2g.core.cdi.Trace;
import me.yonatan.g2g.core.imap.extension.GmailIMAPFolder;
import me.yonatan.g2g.core.model.GmailCredentials;

import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;

@ApplicationScoped
@ToString(of = { "email", "writeable" })
public class ImapConnector {
	// TODO Is thread safety required?

	@Inject
	private Logger log;

	public static final String FOLDER_SEPERATOR = "/";

	public static final String GMAIL_FOLDER = "[Gmail]";

	public static final String ALL_MAIL = GMAIL_FOLDER + FOLDER_SEPERATOR + "All Mail";

	private Session session;

	private GmailIMAPFolder allMailFolder;

	private Store store;

	@Getter
	private String email;

	@Getter
	private boolean writeable;

	@PostConstruct
	@Trace
	public void initImap() {
		Properties props = System.getProperties();
		props.setProperty("mail.store.protocol", "imaps");
		props.setProperty("mail.imaps.folder.class", GmailIMAPFolder.class.getName());
		session = Session.getDefaultInstance(props, null);
	}

	public boolean isConnected() {
		return allMailFolder != null && allMailFolder.isOpen();
	}

	@PreDestroy
	void cleanup() throws MessagingException {
		disconnect();
	}

	public Message getMessageById(int id) throws MessagingException {
		return allMailFolder.getMessage(id);
	}

	public Message getMessageByUID(long uid) throws MessagingException {
		return allMailFolder.getMessageByUID(uid);
	}

	public Message getMessageByGID(String gid) throws MessagingException {
		return allMailFolder.getMessageByGmailId(gid);
	}

	public String getMessageGmailId(long uid) throws MessagingException {
		return allMailFolder.getMessageGmailId(uid, true);
	}

	public List<String> getMessageLabels(long uid) throws MessagingException {
		return allMailFolder.getMessageLabels(uid, true);
	}

	public long getMessageUID(Message message) throws MessagingException {
		return allMailFolder.getUID(message);
	}

	public int getMessageCount() throws MessagingException {
		return allMailFolder.getMessageCount();
	}

	public Message[] addMessage(Message... messages) throws MessagingException {
		if (ArrayUtils.isEmpty(messages))
			return new Message[0];
		return allMailFolder.addMessages(messages);
	}

	public void disconnect() throws MessagingException {
		try {
			allMailFolder.close(true);
		} catch (Exception e) {
		}
		try {
			store.close();
		} catch (Exception e) {
		}
		store = null;
		allMailFolder = null;
	}

	public List<String> addMessageLabels(Message message, List<String> labels) throws MessagingException {
		return allMailFolder.addMessageLabels(message, labels);
	}

	public void connect(GmailCredentials credentials) throws MessagingException {
		connect(credentials, false);
	}

	@SneakyThrows(NoSuchProviderException.class)
	@Trace
	public void connect(GmailCredentials credentials, boolean writeable) throws MessagingException {
		try {
			disconnect();
		} catch (Exception e) {
			log.warn("Problem with disconnecting - {}", e.getMessage());
		}
		String username = credentials.getEmail();
		String password = credentials.getPassword();
		store = session.getStore("imaps");
		store.connect("imap.gmail.com", username, password);

		Folder defaultFolder = store.getDefaultFolder();
		allMailFolder = (GmailIMAPFolder) defaultFolder.getFolder(ALL_MAIL);
		allMailFolder.open(writeable ? Folder.READ_WRITE : Folder.READ_ONLY);
		email = credentials.getEmail();
		this.writeable = writeable;

	}
}
