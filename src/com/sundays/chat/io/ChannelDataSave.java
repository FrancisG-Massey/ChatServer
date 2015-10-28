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
package com.sundays.chat.io;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * This interface is used to communicate permanent channel data between the persistence layer and the application.
 * The design is open, so that alterations can occur to any back-end (eg database, xml files, remote server)
 * 
 * @author Francis
 */
public interface ChannelDataSave extends AutoCloseable {	
	
	public static final int VERSION = 5;//The interface version
	public static final int MIN_VERSION = 5;//The minimum version that the back-end and front-end must support in order for this interface to work
	
	/**
	 * Creates a new channel with the specified details.
	 * 
	 * @param details The details of the new channel. At least name and owner must be specified.
	 * @return The ID of the newly created channel.
	 * @throws IOException If an issue occurred during the creation. 
	 */
	public int createChannel (ChannelDetails details) throws IOException;
	
	/**
	 * Removes the channel with the specified ID from the back-end.
	 * 
	 * @param channelID The ID of the channel to remove.
	 * @throws IOException If an issue occurred during the removal.
	 */
	public void removeChannel (int channelID) throws IOException;
	
	/**
	 * Adds the user to the channel's member list
	 * @param channelID The ID of the channel
	 * @param userID The ID of the user to add to the member list
	 * @param groupID The ID of the group to assign the member to
	 * 
	 * @throws IOException If an IO issue occurred during the addition. 
	 */
	public void addMember (int channelID, int userID, int groupID) throws IOException;
	
	/**
	 * Changes the group of a member within the channel
	 * @param channelID The ID of the channel who the user belongs to
	 * @param userID The ID of the user to change
	 * @param groupID The ID of the group to change to
	 * 
	 * @throws IOException If an IO issue occurred during the update. 
	 */
	public void updateMember (int channelID, int userID, int groupID) throws IOException;
	
	/**
	 * Removes the user from the channel's member list.<br />
	 * Calling this method on a user who is not a member has no effect.
	 * 
	 * @param channelID The ID of the channel.
	 * @param userID The ID of the user to remove from the member list.
	 * 
	 * @throws IOException If an IO issue occurred during the removal. 
	 */
	public void removeMember (int channelID, int userID) throws IOException;
	
	/**
	 * Requests that the specified user is added to the channel's ban list
	 * @param channelID The ID of the channel
	 * @param userID The ID of the user to add to the ban list.
	 * 
	 * @throws IOException If an IO issue occurred during the addition. 
	 */
	public void addBan (int channelID, int userID) throws IOException;
	
	/**
	 * Requests that the specified user is removed from the channel's ban list
	 * @param channelID The ID of the channel
	 * @param userID The ID of the user to remove from the ban list
	 * 
	 * @throws IOException If an IO issue occurred during the removal. 
	 */
	public void removeBan (int channelID, int userID) throws IOException;
	
	//Group changes
	public void addGroup (int channelID, ChannelGroupData group) throws IOException;
	
	public void updateGroup (int channelID, ChannelGroupData group) throws IOException;
	
	public void removeGroup (int channelID, int groupID) throws IOException;
	
	//Channel detail changes
	public void updateDetails (int channelID, ChannelDetails details) throws IOException;

	//Data retrieval
	public ChannelDetails getChannelDetails (int channelID) throws IOException;
	
	public List<Integer> getChannelBans (int channelID) throws IOException;
	
	public Map<Integer, Integer> getChannelMembers (int channelID) throws IOException;
	
	public List<ChannelGroupData> getChannelGroups (int channelID) throws IOException;
	
	/**
	 * Called regularly to notify the implementation that any pending changes should be saved to the persistance layer.<br />
	 * This method is included so implementations can use buffering, to avoid setters blocking the application while the data is saved to the back-end.<br />
	 * As this method is run outside the main thread, appropriate synchronisation should be used.
	 * 
	 * @throws IOException if an IO issue occurs during the commit.
	 */
	public void commitChanges () throws IOException;
}
