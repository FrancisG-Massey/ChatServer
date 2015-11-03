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
package com.sundays.chat.server.user;

import com.sundays.chat.server.message.MessagePayload;
import com.sundays.chat.server.message.MessageType;


/**
 * Represents a message to be sent to a user.
 * 
 * @author Francis
 */
public class UserMessageWrapper {	
	
	private int orderID;
	private MessageType type;
	private long timestamp;
	private MessagePayload payload;
	private int targetUser;
	
	/**
	 * @param orderID
	 * @param type
	 * @param timestamp
	 * @param targetUser
	 * @param payload
	 */
	public UserMessageWrapper(int orderID, MessageType type, long timestamp, int targetUser, MessagePayload payload) {
		super();
		this.orderID = orderID;
		this.type = type;
		this.timestamp = timestamp;
		this.targetUser = targetUser;
		this.payload = payload;
	}

	/**
	 * @return the orderID
	 */
	public int getOrderID() {
		return orderID;
	}

	/**
	 * @return the type
	 */
	public MessageType getType() {
		return type;
	}

	/**
	 * @return the timestamp
	 */
	public long getTimestamp() {
		return timestamp;
	}

	/**
	 * @return the payload
	 */
	public MessagePayload getPayload() {
		return payload;
	}

	/**
	 * @return the targetUser
	 */
	public int getTargetUser() {
		return targetUser;
	}
	
}
