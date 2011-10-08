package me.yonatan.g2g.core.event;

import me.yonatan.g2g.core.jms.JmsManager;
import me.yonatan.g2g.core.model.MessageData;

@SuppressWarnings("serial")
public class CopyMessageEvent extends AbstractMessageEvent {
	
	public CopyMessageEvent(MessageData messageData) {
		super(messageData);
	}

	@Override
	public String getTargetQueue() {
		return JmsManager.MESSAGE_COPY_QUEUE;
	}

}