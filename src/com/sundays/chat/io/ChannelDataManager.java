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
public interface ChannelDataManager {	
	
	public static final int VERSION = 4;//The interface version
	public static final int MIN_VERSION = 3;//The minimum version that the back-end and front-end must support in order for this interface to work
	
	//Rank changes
	public void addRank (int channelID, int userID);
	
	/**
	 * Requests that the rank for the specified user be changed
	 * @param channelID The ID of the channel who the user belongs to
	 * @param userID The ID of the user to change
	 * @param rankID The ID of the rank to change to
	 */
	public void changeRank (int channelID, int userID, byte rankID);
	
	/**
	 * Requests that the rank is removed from the specified user within the specified channel.<br />
	 * Calling this method on a user who does not hold a rank should have no effect.
	 * 
	 * @param channelID The ID of the channel from which to remove the rank.
	 * @param userID The ID of the user to remove the rank from.
	 */
	public void removeRank (int channelID, int userID);
	
	/**
	 * Requests that the specified user is added to the channel's ban list
	 * @param channelID The ID of the channel
	 * @param userID The ID of the user to ban
	 */
	public void addBan (int channelID, int userID);
	
	/**
	 * Requests that the specified user is removed from the channel's ban list
	 * @param channelID The ID of the channel
	 * @param userID The ID of the user to unban
	 */
	public void removeBan (int channelID, int userID);
	
	//Group changes
	public void addGroup (int channelID, ChannelGroupData group);
	
	public void updateGroup (int channelID, ChannelGroupData group);
	
	public void removeGroup (int channelID, int groupID);
	
	//Channel detail changes
	public void syncDetails (int channelID, ChannelDetails details);

	//Data retrieval
	public ChannelDetails getChannelDetails (int channelID);
	
	public List<Integer> getChannelBans (int channelID);
	
	public Map<Integer, Byte> getChannelRanks (int channelID);
	
	public List<ChannelGroupData> getChannelGroups (int channelID);
	
	/**
	 * Called regularly to notify the implementation that any pending changes should be saved to the persistance layer.<br />
	 * This method is included so implementations can use buffering, to avoid setters blocking the application while the data is saved to the back-end.<br />
	 * As this method is run outside the main thread, appropriate synchronisation should be used.
	 * @throws IOException if an IO issue occurs during the commit.
	 */
	public void commitChanges () throws IOException;
}
