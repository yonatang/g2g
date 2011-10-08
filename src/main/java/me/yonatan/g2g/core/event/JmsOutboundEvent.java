package me.yonatan.g2g.core.event;

public interface JmsOutboundEvent {

	public String getTargetQueue();

	public int getRetryCount();

	public void increaseRetryCount();
}
