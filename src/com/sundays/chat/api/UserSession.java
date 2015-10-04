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
package com.sundays.chat.api;

import com.sundays.chat.server.message.MessageWrapper;

/**
 * 
 * @author Francis
 */
public interface UserSession {
	
	/**
     * Sends a message to the user. Implementations may either send the message directly to the user, or add it to a queue.
     * @param channelID The ID of the channel the message is from.
     * @param message The message to send to the user
     */
	public void sendMessage (int channelID, MessageWrapper message);
	
	/**
	 * Forcefully disconnect the user from the server.
	 */
	public void disconnectUser();

}
