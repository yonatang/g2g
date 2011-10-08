package me.yonatan.g2g.core.event;

import java.io.Serializable;

import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import me.yonatan.g2g.core.model.MessageData;

@Data
@SuppressWarnings("serial")
public abstract class AbstractMessageEvent implements Serializable, JmsOutboundEvent {

	@NonNull
	private MessageData messageData;

	@Getter
	private int retryCount = 0;

	public void increaseRetryCount() {
		increaseRetryCount(1);
	}
	
	public void increaseRetryCount(int count){
		retryCount +=count;
	}
}
