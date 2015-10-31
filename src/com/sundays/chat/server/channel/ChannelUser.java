package com.sundays.chat.server.channel;

import com.sundays.chat.server.message.MessagePayload;
import com.sundays.chat.server.message.MessageType;

public interface ChannelUser {

	public abstract String getUsername();

	public abstract int getUserID();

	public abstract Channel getChannel();

	public abstract void setChannel(Channel newchannel);

	/**
	 * Sends a message to the user. 
	 * Depending on how the user is connected, this message will either be sent immediately to the user or added to their message queue.
	 * @param type The type of message being sent.
	 * @param channelID The ID of the channel the message is from.
	 * @param payload The payload data for the message.
	 */
	public abstract void sendMessage(MessageType type, int channelID, MessagePayload payload);

	public abstract int getDefaultChannel();

	public abstract void setDefaultChannel(int defaultChannel);

}