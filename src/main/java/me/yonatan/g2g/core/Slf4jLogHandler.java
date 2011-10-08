package me.yonatan.g2g.core;

import java.util.logging.ConsoleHandler;
import java.util.logging.LogRecord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Routes java.util.log messages to slf4j
 * 
 * @author Yonatan
 * 
 */
public class Slf4jLogHandler extends ConsoleHandler {

	private static final int ERROR = 1000;
	private static final int WARN = 900;
	private static final int INFO = 800;
	private static final int DEBUG = 500;
	private static final int TRACE = 300;

	@Override
	public void publish(LogRecord record) {
		int level = record.getLevel().intValue();
		if (level < TRACE)
			return;

		Logger log = LoggerFactory.getLogger(record.getLoggerName());

		if (level >= ERROR) {
			if (log.isErrorEnabled())
				log.error(record.getMessage());
		} else if (level >= WARN) {
			if (log.isWarnEnabled())
				log.warn(record.getMessage());
		} else if (level >= INFO) {
			if (log.isInfoEnabled())
				log.info(record.getMessage());
		} else if (level >= DEBUG) {
			if (log.isDebugEnabled())
				log.debug(record.getMessage());
		} else if (level >= TRACE) {
			if (log.isTraceEnabled())
				log.trace(record.getMessage());
		} else
			super.publish(record);
	}
}
