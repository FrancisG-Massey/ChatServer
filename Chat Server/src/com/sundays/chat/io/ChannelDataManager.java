/*******************************************************************************
 * Copyright (c) 2015 Francis G.
 *
 * This file is part of ChatServer.
 *
 * ChatServer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
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
	public void changeRank (int channelID, int userID, int rankID);
	
	public void removeRank (int channelID, int userID);
	
	//Ban changes
	/**
	 * Notifies the channel IO that the specified user should be added to the channel's ban list
	 * @param channelID The ID of the channel
	 * @param userID The ID of the user to ban
	 */
	public void addBan (int channelID, int userID);
	
	/**
	 * Notifies the channel IO that the specified user should be removed from the channel's ban list
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
	
	//Synchronise all changes
	/**
	 * Called 
	 */
	public void commitPendingChanges ();

	//Data retrieval
	public ChannelDetails getChannelDetails (int channelID);
	
	public List<Integer> getChannelBans (int channelID);
	
	public Map<Integer, Byte> getChannelRanks (int channelID);
	
	public List<ChannelGroupData> getChannelGroups (int channelID);
}
