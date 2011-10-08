package me.yonatan.g2g.core.imap.extension;

import java.util.Collections;
import java.util.List;

import javax.mail.internet.ParseException;

import lombok.Getter;

import org.apache.commons.lang.StringUtils;

import com.sun.mail.imap.protocol.FetchResponse;

public class GmailLabelItem extends ItemBase {
	@Getter
	private int seqnum;

	@Getter
	private final List<String> labels;

	@Getter
	private final String labelsUtf7String;

	String stripLabelList(String fetchString) {
		int first = fetchString.indexOf('(');
		int second = fetchString.indexOf('(', first + 1);
		if (first < 0 || second < 0)
			throw new IndexOutOfBoundsException();

		String firstSplit = fetchString.substring(second + 1);

		String secondSplit = StringUtils.reverse(firstSplit);
		first = secondSplit.indexOf(')');
		second = secondSplit.indexOf(')', first + 1);

		if (first < 0 || second < 0)
			throw new IndexOutOfBoundsException();

		return StringUtils.reverse(secondSplit.substring(second + 1));
	}

	public GmailLabelItem(FetchResponse fetchResponse) throws ParseException {
		String str = null;
		try {
			str = getResponseAsUtf8(fetchResponse);
			seqnum = fetchResponse.getNumber();
			String labelStr = stripLabelList(str);
			labelsUtf7String = getToAsUtf7(labelStr);
			labels = Collections.unmodifiableList(spitString(labelStr));
		} catch (Exception e) {
			throw new ParseException("Can't parse response " + str);
		}
	}

	public GmailLabelItem(List<String> labels) {
		this.labels = Collections.unmodifiableList(labels);
		labelsUtf7String = getToAsUtf7(joinList(labels));
	}
}
