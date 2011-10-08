package me.yonatan.g2g.core.imap.extension;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.ParseException;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang.StringUtils;

import com.google.common.collect.MapMaker;
import com.sun.mail.iap.ProtocolException;
import com.sun.mail.iap.Response;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPStore;
import com.sun.mail.imap.protocol.FetchResponse;
import com.sun.mail.imap.protocol.IMAPResponse;
import com.sun.mail.imap.protocol.ListInfo;

@Slf4j
public class GmailIMAPFolder extends IMAPFolder {
	static {
		Properties props = System.getProperties();
		props.setProperty("mail.store.protocol", "imaps");
		props.setProperty("mail.imaps.folder.class", GmailIMAPFolder.class.getName());
		props.setProperty("mail.imap.folder.class", GmailIMAPFolder.class.getName());
	}

	static final String X_GM_MSGID = "X-GM-MSGID";
	static final String X_GM_LABELS = "X-GM-LABELS";

	/** Using soft-reference, with expiration of 20 minutes, to avoid OOME */
	protected ConcurrentMap<Long, String> gidTable = new MapMaker().softValues().makeMap();

	public GmailIMAPFolder(ListInfo li, IMAPStore store) {
		super(li, store);
	}

	public GmailIMAPFolder(String fullName, char separator, IMAPStore store, Boolean isNamespace) {
		super(fullName, separator, store, isNamespace);
	}

	public Message getMessageByGmailId(String gid) throws MessagingException {
		checkOpened();
		String commandStr = "UID SEARCH " + X_GM_MSGID + " " + gid;
		try {
			Response[] responses = getProtocol().command(commandStr, null);
			for (val response : responses) {
				if (response.isBAD())
					return null;
				if (!(response instanceof IMAPResponse))
					continue;

				IMAPResponse imapResponse = (IMAPResponse) response;
				if (!"SEARCH".equalsIgnoreCase(imapResponse.getKey()))
					continue;

				String[] str = StringUtils.split(imapResponse.toString());
				if (str.length < 3 || !StringUtils.isNumeric(str[2]))
					throw new MessagingException("Can't parse command " + commandStr);
				long uid = Long.parseLong(str[2]);
				return getMessageByUID(uid);
			}
			return null;
		} catch (ProtocolException e) {
			throw new MessagingException(null, e);
		}
	}

	public synchronized String getMessageGmailId(long uid, boolean isUid) throws MessagingException {
		checkOpened();
		GmailIDItem gmailId = null;
		try {
			log.debug("getting gid for id {} uid? {}", uid, isUid);
			if (gidTable.containsKey(uid))
				return gidTable.get(uid);

			String commandStr = (isUid ? "UID " : "") + "FETCH " + uid + " (" + X_GM_MSGID + ")";

			Response[] responses = getProtocol().command(commandStr, null);

			for (val response : responses) {
				if (!(response instanceof FetchResponse)) {
					continue;
				}
				FetchResponse fetchResponse = (FetchResponse) response;
				gmailId = new GmailIDItem(fetchResponse);
				String gid = gmailId.getId();
				log.debug("For {} result {}", uid, gid);
				gidTable.put(uid, gid);
				return gid;
			}
		} catch (ParseException e) {
			throw new MessagingException("Can't parse reponse", e);
		} catch (ProtocolException e) {
			throw new MessagingException(null, e);
		}
		throw new MessagingException("Can't parse reponse");
	}

	public synchronized String[] getMessageGmailId(long[] uids, boolean isUid) throws MessagingException {
		checkOpened();
		if (uids == null)
			throw new NullPointerException("UID array is null");
		String[] results = new String[uids.length];
		GmailIDItem gmailId = null;
		try {

			for (int i = 0; i < uids.length; i++) {
				long uid = uids[i];

				if (gidTable.containsKey(uid)) {
					results[i] = gidTable.get(uid);
					continue;
				}

				String commandStr = (isUid ? "UID " : "") + "FETCH " + uid + " (" + X_GM_MSGID + ")";

				Response[] responses = getProtocol().command(commandStr, null);

				for (val response : responses) {
					if (!(response instanceof FetchResponse)) {
						continue;
					}
					FetchResponse fetchResponse = (FetchResponse) response;
					gmailId = new GmailIDItem(fetchResponse);
					gidTable.put(uid, gmailId.getId());
					results[i] = gmailId.getId();
					break;
				}
				if (results[i] == null)
					throw new MessagingException("Can't parse reponse");
			}
			return results;
		} catch (ParseException e) {
			throw new MessagingException("Can't parse reponse", e);
		} catch (ProtocolException e) {
			throw new MessagingException(null, e);
		}
	}

	public List<String> getMessageLabels(long uid, boolean isUid) throws MessagingException {
		checkOpened();
		GmailLabelItem labels;
		try {
			// Do not cache labels! They might get changed over time.
			String commandStr = (isUid ? "UID " : "") + "FETCH " + uid + " (" + X_GM_LABELS + ")";

			Response[] responses = getProtocol().command(commandStr, null);

			for (val response : responses) {
				if (!(response instanceof FetchResponse)) {
					continue;
				}
				FetchResponse fetchResponse = (FetchResponse) response;
				labels = new GmailLabelItem(fetchResponse);
				return labels.getLabels();
			}

		} catch (ParseException e) {
			throw new MessagingException("Can't parse reponse", e);
		} catch (ProtocolException e) {
			throw new MessagingException(null, e);
		}
		throw new MessagingException("Can't parse reponse");
	}

	public List<String> addMessageLabels(Message message, List<String> labels) throws MessagingException {
		if (labels == null || labels.isEmpty())
			return new ArrayList<String>();
		checkOpened();

		long uid = getUID(message);
		GmailLabelItem labelItem = new GmailLabelItem(labels);
		List<String> result = new ArrayList<String>();
		String commandStr = "UID STORE " + uid + " +" + X_GM_LABELS + " (" + labelItem.getLabelsUtf7String() + ")";
		try {
			Response[] responses = getProtocol().command(commandStr, null);
			for (val response : responses) {
				if (response.isBAD())
					throw new MessagingException("Can't parse command " + commandStr);
				if (response instanceof FetchResponse) {
					result = new GmailLabelItem((FetchResponse) response).getLabels();
				}
			}
		} catch (ProtocolException e) {
			throw new MessagingException(null, e);
		}
		return result;
	}
}
