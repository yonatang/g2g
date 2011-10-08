package me.yonatan.g2g.core;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggerProducer {

	@Produces
	Logger propduceLogger(InjectionPoint ip) {
		Bean<?> bean = ip.getBean();
		if (bean == null)
			return null;

		Logger log = LoggerFactory.getLogger(bean.getBeanClass());
		return log;
	}
}
