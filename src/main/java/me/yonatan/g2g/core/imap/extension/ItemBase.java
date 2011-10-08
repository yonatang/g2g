package me.yonatan.g2g.core.imap.extension;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lombok.Cleanup;
import lombok.SneakyThrows;

import org.apache.commons.lang.StringUtils;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

import com.sun.mail.imap.protocol.IMAPResponse;
import com.sun.mail.imap.protocol.Item;

public abstract class ItemBase implements Item {
	@SneakyThrows(IOException.class)
	List<String> spitString(String str) {
		if (StringUtils.isEmpty(str))
			return new ArrayList<String>();
		@Cleanup
		StringReader reader = new StringReader(str);
		@Cleanup
		CSVReader csvReader = new CSVReader(reader, ' ');
		return Arrays.asList(csvReader.readNext());
	}

	@SneakyThrows(IOException.class)
	String joinList(List<String> list) {
		@Cleanup
		StringWriter writer = new StringWriter();
		@Cleanup
		CSVWriter csvWriter = new CSVWriter(writer, ' ', '"', '\\', "");
		csvWriter.writeNext(list.toArray(new String[0]));
		return StringUtils.chomp(writer.toString().trim());
	}

	private final Charset utf8 = Charset.forName("UTF-8");
	private final Charset utf7 = Charset.forName("X-MODIFIED-UTF-7");

	// Should never be thrown, unless jutf7 is missing from the classpath
	@SneakyThrows(CharacterCodingException.class)
	String getResponseAsUtf8(IMAPResponse response) {
		String asciiStr = response.toString();
		CharsetDecoder decoder = utf7.newDecoder();
		CharsetEncoder encoder = utf8.newEncoder();
		ByteBuffer bbuf = encoder.encode(CharBuffer.wrap(asciiStr));
		CharBuffer cbuf = decoder.decode(bbuf);
		return cbuf.toString();
	}

	// Should never be thrown, unless jutf7 is missing from the classpath
	@SneakyThrows(CharacterCodingException.class)
	String getToAsUtf7(String utf8String) {
		CharsetDecoder decoder = utf8.newDecoder();
		CharsetEncoder encoder = utf7.newEncoder();
		ByteBuffer bbuf = encoder.encode(CharBuffer.wrap(utf8String));
		CharBuffer cbuf = decoder.decode(bbuf);
		return cbuf.toString();
	}
}
