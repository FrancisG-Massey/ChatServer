/*******************************************************************************
 * Copyright (c) 2013, 2016 Francis G.
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

import java.io.Serializable;
import java.util.Collection;

import com.sundays.chat.io.ChannelDetails;

/**
 * 
 * @author Francis
 */
public interface ChannelAPI {
	
	/**
	 * Checks whether the specified channel is currently loaded on the server
	 * @param channelId The internal ID of the channel to check
	 * @return True if the channel is loaded, false otherwise
	 */
	public boolean isLoaded (int channelId);
	
	/**
	 * Gets the details of the provided channel
	 * @param channelId The internal ID of the channel
	 * @return The channel details
	 * @throws IllegalStateException If no channel with channelId is currently loaded on this server
	 */
	public ChannelDetails getDetails (int channelId) throws IllegalStateException;
	
	/**
	 * Gets the list of users currently in the specified channel
	 * @param channelId The internal ID of the channel
	 * @return An unmodifiable collection containing channel users
	 * @throws IllegalStateException If no channel with channelId is currently loaded on this server
	 */
	public Collection<ChannelUser> getUsers (int channelId) throws IllegalStateException;
	
	/**
	 * Gets the list of users currently banned from the specified channel
	 * @param channelId The internal ID of the channel
	 * @return A collection of internal user IDs representing users banned from the channel
	 * @throws ChannelNotFoundException If the ID specified does not match any registered channel
	 */
	public Collection<Integer> getBans (int channelId) throws ChannelNotFoundException;

	
	public ChannelResponse join(ChannelUser user, int channelId) throws ChannelNotFoundException;

	/**
	 * 
	 * @param user
	 * @param channelId
	 * @param message
	 * @return
	 */
	public ChannelResponse sendMessage(ChannelUser user, int channelId, String message);

	public ChannelResponse leave(ChannelUser user, int channelId);

	/*
	 * Moderator functions
	 */
	public ChannelResponse reset(ChannelUser user, int channelId);

	public ChannelResponse kickUser(ChannelUser user, int channelId, int kickTargetId);

	public ChannelResponse tempBanUser(ChannelUser user, int channelId, int banTargetId, int durationMins);

	public ChannelResponse setAttribute(ChannelUser user, int channelId, String key, Serializable value);

	public ChannelResponse addMember(ChannelUser user, int channelId, int userId);

	public ChannelResponse updateMember(ChannelUser user, int channelId, int userId, int groupId);

	public ChannelResponse removeMember(ChannelUser user, int channelId, int userId);

	public ChannelResponse addBan(ChannelUser user, int channelId, int userID);

	public ChannelResponse removeBan(ChannelUser user, int channelId, int userID);

}