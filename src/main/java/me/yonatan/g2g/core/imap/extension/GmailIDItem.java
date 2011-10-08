package me.yonatan.g2g.core.imap.extension;

import java.util.List;

import javax.mail.internet.ParseException;

import lombok.Getter;

import org.apache.commons.lang.StringUtils;

import com.sun.mail.imap.protocol.FetchResponse;

public class GmailIDItem extends ItemBase {
	@Getter
	private int seqnum;

	@Getter
	private String id;

	String stripMsgidList(String fetchString) {
		int first = fetchString.indexOf('(');
		int last = fetchString.indexOf(')');

		return fetchString.substring(first + 1, last);
	}

	public GmailIDItem(FetchResponse fetchResponse) throws ParseException {

		seqnum = fetchResponse.getNumber();
		String str = getResponseAsUtf8(fetchResponse);
		String msgIdStr = stripMsgidList(str);
		List<String> result = spitString(msgIdStr);
		try {
			assert result.size() >= 2;
			assert GmailIMAPFolder.X_GM_MSGID.equalsIgnoreCase(result.get(0));
			id = result.get(1);
			assert StringUtils.isNumeric(id);
		} catch (AssertionError e) {
			throw new ParseException("Can't parse response " + str);
		}
	}

}
