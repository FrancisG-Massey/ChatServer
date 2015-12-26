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
import java.util.Collection;
import java.util.Map;

/**
 * This interface is used to communicate permanent channel data between the persistence layer and the application.
 * The design is open, so that alterations can occur to any back-end (eg database, xml files, remote server)
 * 
 * @author Francis
 */
public interface ChannelDataIO extends AutoCloseable {
	
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
	
	/**
	 * Adds the specified key-value pair as an attribute for the channel.<br />
	 * Note: this method will fail if the attribute already exists. If the attribute exists, use {@link #updateAttribute(int, String, String)} instead.
	 * @param channelID The ID of the channel
	 * @param key The attribute key; used for retrieving the value once stored. Should be a string between 2 and 100 alphanumeric, hyphen, underscore, or dot characters.
	 * @param value The attribute value. Should be a string with less than 2^16 characters.
	 * @throws IOException If an IO issue occurs during the addition.
	 */
	public void addAttribute (int channelID, String key, String value) throws IOException;
	
	/**
	 * Updates the specified attribute for the channel to the provided value.<br />
	 * Note: this method will fail if the attribute does not exist. If this is the case, use {@link #addAttribute(int, String, String)} to add the attribute first.
	 * @param channelID The ID of the channel
	 * @param key The attribute key; used for identifying the value to update. Should be a string between 2 and 100 alphanumeric, hyphen, underscore, or dot characters.
	 * @param value The attribute value. Should be a string with less than 2^16 characters.
	 * @throws IOException If an IO issue occurs during the addition.
	 */
	public void updateAttribute (int channelID, String key, String value) throws IOException;
	
	public void clearAttribute (int channelID, String key) throws IOException;

	//Data retrieval
	public ChannelDetails getChannelDetails (int channelID) throws IOException;
	
	/**
	 * Returns the attributes (aka variables) for the channel.
	 * These are used for storing general information about the channel which are not already stored in the channel details.
	 * 
	 * @param channelID The ID of the channel
	 * @return A key-value pair map containing the channel attributes
	 * @throws IOException If an IO issue occurs during the retrieval
	 */
	public Map<String, String> getChannelAttributes (int channelID) throws IOException;
	
	public Collection<Integer> getChannelBans (int channelID) throws IOException;
	
	public Map<Integer, Integer> getChannelMembers (int channelID) throws IOException;
	
	public Collection<ChannelGroupData> getChannelGroups (int channelID) throws IOException;
	
	/**
	 * Called regularly to notify the implementation that any pending changes should be saved to the persistance layer.<br />
	 * This method is included so implementations can use buffering, to avoid setters blocking the application while the data is saved to the back-end.<br />
	 * As this method is run outside the main thread, appropriate synchronisation should be used.
	 * 
	 * @throws IOException if an IO issue occurs during the commit.
	 */
	public void commitChanges () throws IOException;
}
