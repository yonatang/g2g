package me.yonatan.g2g.core.imap.extension;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.mail.internet.ParseException;

import lombok.val;
import me.yonatan.g2g.core.imap.extension.GmailIDItem;
import me.yonatan.g2g.core.imap.extension.GmailLabelItem;
import me.yonatan.g2g.core.imap.extension.ItemBase;

import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.sun.mail.imap.protocol.FetchResponse;

@Test
public class GmailItemTests {

	private static final String UTF8_FESHBER_TSUPAR = "\u05E4\u05E9\u05D1\u05E8 \u05E6'\u05D5\u05E4\u05E8";
	private static final String UTF8_DIRA = "\u05D3\u05D9\u05E8\u05D4";

	public void shouldParseLabelWellAsUid() throws Exception {
		FetchResponse fr = Mockito.mock(FetchResponse.class);
		String fetchString = "* 5 FETCH (X-GM-LABELS (&BdMF2QXoBdQ- \"la(bel\" \"Google\\\" Gadgets Workshop\" UBS/Test \"\\\\Sent\" \"&BeQF6QXRBeg- &BeY-'&BdUF5AXo-\" Yonatan.Graber Friends) UID 5)";

		String[] expectedLabels = { UTF8_DIRA, "la(bel", "Google\" Gadgets Workshop", "UBS/Test", "\\Sent", UTF8_FESHBER_TSUPAR,
				"Yonatan.Graber", "Friends" };
		Mockito.when(fr.toString()).thenReturn(fetchString);
		GmailLabelItem gli = new GmailLabelItem(fr);

		Assert.assertEquals(Arrays.asList(expectedLabels), gli.getLabels());
	}
	
	public void shouldParseLabelWell() throws Exception {
		FetchResponse fr = Mockito.mock(FetchResponse.class);
		String fetchString = "* 5 FETCH (X-GM-LABELS (&BdMF2QXoBdQ- \"la(bel\" \"Google\\\" Gadgets Workshop\" UBS/Test \"\\\\Sent\" \"&BeQF6QXRBeg- &BeY-'&BdUF5AXo-\" Yonatan.Graber Friends))";

		String[] expectedLabels = { UTF8_DIRA, "la(bel", "Google\" Gadgets Workshop", "UBS/Test", "\\Sent", UTF8_FESHBER_TSUPAR,
				"Yonatan.Graber", "Friends" };
		Mockito.when(fr.toString()).thenReturn(fetchString);
		GmailLabelItem gli = new GmailLabelItem(fr);

		Assert.assertEquals(Arrays.asList(expectedLabels), gli.getLabels());
	}

	
	public void shouldCreateLabelStringWell() throws Exception {
		List<String> labels=Arrays.asList(UTF8_FESHBER_TSUPAR,UTF8_DIRA);
		String expected="\"&BeQF6QXRBeg- &BeY-'&BdUF5AXo-\" \"&BdMF2QXoBdQ-\"";
		GmailLabelItem gli=new GmailLabelItem(labels);
		Assert.assertEquals(gli.getLabelsUtf7String(), expected);
	}
	
	private class TestList {
		public TestList(String input, String... expected) {
			this.input = input;
			if (expected == null)
				this.expected = new ArrayList<String>();
			else
				this.expected = Arrays.asList(expected);
		}

		private String input;
		private List<String> expected;

		public void doAssert(List<String> actual) {
			doAssert(actual, "Input: " + input + ", expected: " + expected + ", actual: " + actual);
		}

		public void doAssert(List<String> actual, String message) {
			Assert.assertEquals(actual, expected, message);
		}
	}

	public void shouldSplitListProperly() throws Exception {
		ItemBase base = Mockito.mock(ItemBase.class);
		Mockito.when(base.spitString(Mockito.any(String.class))).thenCallRealMethod();
		TestList[] tests = new TestList[] { new TestList("a b", "a", "b"), //
				new TestList(""),//
				new TestList("\"abc\" abc", "abc", "abc"), //
				new TestList("\"abc\"", "abc"), //
				new TestList("abc \"abc\"", "abc", "abc"), //
				new TestList("\"\\\\abc\"", "\\abc"),//
				new TestList("abc \"a bc\"", "abc", "a bc"), //
				new TestList("\"abc\\\" def\"", "abc\" def"), //
				new TestList("\"" + UTF8_DIRA + "\"", UTF8_DIRA) };

		int counter = 0;
		for (val test : tests) {
			++counter;
			test.doAssert(base.spitString(test.input));
		}
		Assert.assertEquals(counter, tests.length);
	}

	public void shouldReadMessageIdProperly() throws Exception {
		FetchResponse fr = Mockito.mock(FetchResponse.class);
		String maxMsgId = BigInteger.valueOf(2).pow(64).add(BigInteger.valueOf(-1)).toString();
		String[] msgIds = { "1204541023637292164", "1204541023637292165", maxMsgId, maxMsgId };
		Mockito.when(fr.toString()).thenReturn("* 5 FETCH (X-GM-MSGID " + msgIds[0] + ")",
				"* 5 FETCH (X-GM-MSGID " + msgIds[1] + " UID 5)", "* 5 FETCH (X-GM-MSGID " + msgIds[2] + ")",
				"* 5 FETCH (X-GM-MSGID " + msgIds[3] + " UID 5)");

		for (val msgId : msgIds) {
			Assert.assertEquals(new GmailIDItem(fr).getId(), msgId);
		}
	}

	@Test(expectedExceptions = ParseException.class)
	public void shouldFailOnBadGmailIdResponse1() throws Exception {
		FetchResponse fr = Mockito.mock(FetchResponse.class);
		Mockito.when(fr.toString()).thenReturn("* 5 FETCH (X-GM-MSGID)");
		new GmailIDItem(fr);
	}

	@Test(expectedExceptions = ParseException.class)
	public void shouldFailOnBadGmailIdResponse2() throws Exception {
		FetchResponse fr = Mockito.mock(FetchResponse.class);
		Mockito.when(fr.toString()).thenReturn("* 5 FETCH (X-HOTMAIL-MSGID 1204541023637292165)");
		new GmailIDItem(fr);
	}

	@Test(expectedExceptions = ParseException.class)
	public void shouldFailOnBadGmailIdResponse3() throws Exception {
		FetchResponse fr = Mockito.mock(FetchResponse.class);
		Mockito.when(fr.toString()).thenReturn("* 5 FETCH (X-GM-MSGID 1204541023637ABC)");
		new GmailIDItem(fr);
	}

	public void shouldGetMessageSequenceForGmailId() throws Exception {
		FetchResponse fr = Mockito.mock(FetchResponse.class);
		Mockito.when(fr.toString()).thenReturn("* 5 FETCH (X-GM-MSGID 1204541023637)");
		Mockito.when(fr.getNumber()).thenReturn(5);

		GmailIDItem gii = new GmailIDItem(fr);
		Assert.assertEquals(gii.getSeqnum(), 5);
	}

	@Test(expectedExceptions = ParseException.class)
	public void shouldFailOnBadGmailLabelResponse1() throws Exception {
		FetchResponse fr = Mockito.mock(FetchResponse.class);
		Mockito.when(fr.toString()).thenReturn("* 5 FETCH (X-GM-LABELS (&BdMF2QXoBdQ- \"la(bel\" \"Google\\\" Gadgets Workshop\" UBS/Test \"\\\\Sent\" \"&BeQF6QXRBeg- &BeY-'&BdUF5AXo-\" Yonatan.Graber Friends)");
		
		new GmailLabelItem(fr);
	}
	@Test(expectedExceptions = ParseException.class)
	public void shouldFailOnBadGmailLabelResponse2() throws Exception {
		FetchResponse fr = Mockito.mock(FetchResponse.class);
		Mockito.when(fr.toString()).thenReturn("* 5 FETCH X-GM-LABELS &BdMF2QXoBdQ- \"la(bel\" \"Google\\\" Gadgets Workshop\" UBS/Test \"\\\\Sent\" \"&BeQF6QXRBeg- &BeY-'&BdUF5AXo-\" Yonatan.Graber Friends)");
		
		new GmailLabelItem(fr);
	}

}
