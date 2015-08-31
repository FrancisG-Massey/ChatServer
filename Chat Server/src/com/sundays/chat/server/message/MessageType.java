/*******************************************************************************
 * Copyright (c) 2015 Francis G.
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
package com.sundays.chat.server.message;

/**
 * Defines the type of message sent to a user.
 * 
 * @author Francis
 */
public enum MessageType {
	UNUSED0(0),
	UNUSED1(1),
	UNUSED2(2),
	CHANNEL_SYSTEM_LOCAL(3),
	CHANNEL_SYSTEM_GLOBAL(4),
	CHANNEL_STANDARD(5),
	
	/**
	 * Adds a user to the channel list.<br /><br />
	 * 
	 * Contains the following properties:
	 * <ul>
	 * <li>userID - The ID of the user to add.</li>
	 * <li>username - The name of the user to add.</li>
	 * <li>group - The group which the user belongs to.</li>
	 * <li>rank - The legacy rank ID of the user (NOTE: This is deprecated and may be removed in the future).</li>
	 * </ul>
	 */
	CHANNEL_LIST_ADDITION(6),
	
	/**
	 * Removes a user from the channel list.<br /><br />
	 * 
	 * Contains the following properties:
	 * <ul>
	 * <li>userID - The ID of the user to remove.</li>
	 * </ul>
	 */
	CHANNEL_LIST_REMOVAL(7),
	
	/**
	 * Updates a user on the channel list.<br /><br />
	 * 
	 * Contains the following properties:
	 * <ul>
	 * <li>userID - The ID of the user to update.</li>
	 * <li>username - The new username of the user.</li>
	 * <li>group - The new group which the user belongs to.</li>
	 * <li>rank - The legacy rank ID of the user (NOTE: This is deprecated and may be removed in the future).</li>
	 * </ul>
	 */
	CHANNEL_LIST_UPDATE(8),
	PERMISSION_UPDATE(9),
	CHANNEL_REMOVAL(10),
	
	/**
	 * Adds a user to the channel rank list.<br /><br />
	 * 
	 * Contains the following properties:
	 * <ul>
	 * <li>userID - The ID of the user to add.</li>
	 * <li>username - The name of the user to add.</li>
	 * <li>group - The group which the user belongs to.</li>
	 * <li>rank - The legacy rank ID of the user (NOTE: This is deprecated and may be removed in the future).</li>
	 * </ul>
	 */
	RANK_LIST_ADDITION(11),
	
	/**
	 * Removes a user from the channel rank list.<br /><br />
	 * 
	 * Contains the following properties:
	 * <ul>
	 * <li>userID - The ID of the user to remove.</li>
	 * </ul>
	 */
	RANK_LIST_REMOVAL(12),
	
	/**
	 * Updates a user on the channel rank list.<br /><br />
	 * 
	 * Contains the following properties:
	 * <ul>
	 * <li>userID - The ID of the user to update.</li>
	 * <li>username - The new username of the user.</li>
	 * <li>group - The new group which the user belongs to.</li>
	 * <li>rank - The legacy rank ID of the user (NOTE: This is deprecated and may be removed in the future).</li>
	 * </ul>
	 */
	RANK_LIST_UPDATE(13),
	
	/**
	 * Adds a user to the channel ban list.<br /><br />
	 * 
	 * Contains the following properties:
	 * <ul>
	 * <li>userID - The ID of the user to add.</li>
	 * <li>username - The name of the user to add.</li>
	 * </ul>
	 */
	BAN_LIST_ADDITION(14),
	
	/**
	 * Removes a user from the channel ban list.<br /><br />
	 * 
	 * Contains the following properties:
	 * <ul>
	 * <li>userID - The ID of the user to remove.</li>
	 * </ul>
	 */
	BAN_LIST_REMOVAL(15),
	RANK_NAME_UPDATE(16),
	CHANNEL_DETAIL_UPDATE(17),
	RANK_UPDATE(18);
	
	private final int id;
	
	private MessageType (int id) {
		this.id = id;
	}
	
	public int getID () { 
		return this.id; 
	}
}