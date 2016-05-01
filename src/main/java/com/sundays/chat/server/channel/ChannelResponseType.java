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

public enum ChannelResponseType {
	/**
	 * Indicates an invalid argument (or parameter) was given to this method which prevented it from succeeding.
	 */
	INVALID_ARGUMENT(10),
	
	/**
	 * Indicates that no channel could be found with the specified ID
	 */
	CHANNEL_NOT_FOUND(11),
	
	/**
	 * Indicates the user on whom the specified action was to be performed could not be found
	 */
	USER_NOT_FOUND(12),
	
	/**
	 * Indicates the action could not be completed as the target user is in an invalid state.
	 * For example, this response is returned when trying to edit a user who isn't a channel member.
	 */
	TARGET_INVALID_STATE(20),
	
	/**
	 * Indicates that the channel with the specified ID is not currently loaded on the server.
	 * This response may also indicate the channel does not exist (as in {@link #CHANNEL_NOT_FOUND}), though this is not guarenteed.
	 */
	CHANNEL_NOT_LOADED(21),
	
	/**
	 * Indicates the user on whom the specified action was to be performed is not currently in the channel.
	 * This response does not guarentee that the user exists.
	 */
	NOT_IN_CHANNEL(22),
	
	/**
	 * Indicates the target user is banned from the channel
	 */
	TARGET_BANNED(23),
	
	/**
	 * Indicates that the user is not authorised to perform the intended action on the channel.
	 * This code indicates the user cannot perform the action regardless of the parameters they give, in contrast to {@link #NOT_AUTHORISED_SPECIFIC}.
	 */
	NOT_AUTHORISED_GENERAL(30),
	
	/**
	 * Indicates that the user is not authorised to perform the intended action on the channel.
	 * This code indicates the user cannot the action with the given parameters (eg userId, groupId), but they may be able to perform the action with different parameters
	 */
	NOT_AUTHORISED_SPECIFIC(31),
	
	/**
	 * Indicates that the user has been blocked from joining the channel
	 */
	BANNED(32),
	
	/**
	 * Indicates that the user has been temporarily blocked from joining the channel
	 */
	BANNED_TEMP(33),
	
	/**
	 * Indicates the action could not be completed due to a temporary lock
	 */
	LOCKED(34),
	
	/**
	 * Indicates the action was performed successfully
	 */
	SUCCESS(40),
	
	/**
	 * Indicates the action was not needed as the change had already been applied.
	 */
	NO_CHANGE(41),
	
	/**
	 * Indicates an error occurred while processing the action but no specific type was determined
	 */
	UNKNOWN_ERROR(50);
	
	private final int id;
	
	/**
	 * @param id The message response code
	  */
	private ChannelResponseType(int id) {
		this.id = id;
	}

	/**
	 * Gets the response code of this channel response
	 * @return The response code
	 */
	public int getId() {
		return id;
	}
}
