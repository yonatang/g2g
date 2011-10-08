package me.yonatan.g2g.core.event;

import me.yonatan.g2g.core.jms.JmsManager;
import me.yonatan.g2g.core.model.MessageData;

@SuppressWarnings("serial")
public class FetchMessageEvent extends AbstractMessageEvent {

	public FetchMessageEvent(MessageData messageData) {
		super(messageData);
	}

	@Override
	public String getTargetQueue() {
		return JmsManager.MESSAGE_ENUMERATION_QUEUE;
	}

}
