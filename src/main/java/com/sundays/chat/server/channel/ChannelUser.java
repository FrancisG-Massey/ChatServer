/*******************************************************************************
 * Copyright (c) 2013, 2015 Francis G.
 *
 * This file is part of ChatServer.
 *
 * ChatServer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * ChatServer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ChatServer.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
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