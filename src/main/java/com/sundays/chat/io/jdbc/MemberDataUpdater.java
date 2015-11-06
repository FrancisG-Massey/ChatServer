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
package com.sundays.chat.io.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;

import com.google.common.base.Joiner;
import com.sundays.chat.io.ChannelGroupData;

public class MemberDataUpdater {
	
	private static final Logger logger = Logger.getLogger(MemberDataUpdater.class);
		
	private final String memberTableName;
	private final String banTableName;
	private final String groupTableName;
	
	protected MemberDataUpdater (String memberTableName, String banTableName, String groupTableName) {
		this.memberTableName = memberTableName;
		this.banTableName = banTableName;
		this.groupTableName = groupTableName;
	}
	
	//Rank database update queues
	private final Map<KeyMatcher, Integer> memberAdditions = new HashMap<>();
	private final Set<KeyMatcher> memberRemovals = new HashSet<>();
	private final Map<KeyMatcher, Integer> memberUpdates = new HashMap<>();
	
	//Ban database update queues
	private final Set<KeyMatcher> banAdditions = new HashSet<>();
	private final Set<KeyMatcher> banRemovals = new HashSet<>();
	
	//Group update queues
	private final Set<ChannelGroupData> groupUpdates = new HashSet<>();
	
	protected synchronized void addMember(int channelID, int user, int group) {
		//Updates the rank change queue with the specified member addition
		KeyMatcher memberKey = new KeyMatcher(channelID, user);
		
		if (memberAdditions.containsKey(memberKey)) {
			//Member addition already queued
			return;
		}
		if (memberRemovals.contains(memberKey)) {
			//Member removal was queued beforehand. The addition request cancels this out. Replace with a request to 
			memberRemovals.remove(memberKey);
			updateMember(channelID, user, group);
			return;
		}		
		memberAdditions.put(memberKey, group);//Place the user in the addition queues
	}

	protected synchronized void updateMember(int channelID, int userID, int group) {
		//Updates the member change queue with the specified member change
		KeyMatcher memberKey = new KeyMatcher(channelID, userID);
		
		if (memberRemovals.contains(memberKey)) {
			//Member removal was queued beforehand. Since the removal takes priority, the member cannot be changed.
			return;
		}	
		if (memberUpdates.containsKey(memberKey)) {
			//A member update was already queued. Remove the previous update from the queue, as this new change overrides it
			memberUpdates.remove(memberKey);
		}	
		memberUpdates.put(memberKey, group);//Place the user in the member change queue
	}

	protected synchronized void removeMember(int channelID, int userID) {
		//Updates the member change queue with the specified member removal
		KeyMatcher memberKey = new KeyMatcher(channelID, userID);
		
		if (memberRemovals.contains(memberKey)) {
			//Member removal already queued
			return;
		}
		if (memberAdditions.containsKey(memberKey)) {
			//Member addition queued beforehand. Remove the addition from the queue. As both events cancel each other out, there is no need to precede with the removal.
			memberAdditions.remove(memberKey);
			return;
		}	
		if (memberUpdates.containsKey(memberKey)) {
			//A member update was queued beforehand. Remove the update from the cue
			memberUpdates.remove(memberKey);
		}	
		memberRemovals.add(memberKey);//Place the member in the removal cue
	}

	protected synchronized void addBan(int channelID, int userID) {
		//Updates the ban addition queue with the specified ban addition
		KeyMatcher banAdditionKey = new KeyMatcher(channelID, userID);
		
		if (banAdditions.contains(banAdditionKey)) {
			//Ban addition already queued
			return;
		}
		if (banRemovals.contains(banAdditionKey)) {
			//Ban removal was queued beforehand. Remove the removal from the queue. Since both requests cancel each other out, their is no need to proceed with the addition.
			banRemovals.remove(banAdditionKey);
			return;
		}	
		banAdditions.add(banAdditionKey);//Place the user in the addition queue
	}

	protected synchronized void removeBan(int channelID, int userID) {
		//Updates the ban removal queue with the specified rank removal
		KeyMatcher banRemovalKey = new KeyMatcher(channelID, userID);
		
		if (banRemovals.contains(banRemovalKey)) {
			//Ban removal already queued
			return;
		}
		if (banAdditions.contains(banRemovalKey)) {
			//Ban addition was queued beforehand. Since both requests cancel each other out, removing the addition request is sufficient
			banAdditions.remove(banRemovalKey);
			return;
		}	
		banRemovals.add(banRemovalKey);//Place the user in the removal queue
	}
	
	protected synchronized void updateGroup (int channelID, ChannelGroupData group) {
		if (groupUpdates.contains(group)) {
			//Group update already queued
			groupUpdates.remove(group);
		}
		groupUpdates.add(group);
	}
	
	private PreparedStatement memberInsertQuery;
	private PreparedStatement memberUpdateQuery;
	private PreparedStatement memberDeleteQuery;
	private PreparedStatement banInsertQuery;
	private PreparedStatement banDeleteQuery;
	private PreparedStatement groupUpdateQuery;

	/**
	 * This operation commits any pending database changes. It should be called on a regular basis.
	 * Please note that the order of the functions in this method (additions, then updates, then removals) MUST remain the same. Otherwise, queries may not commit correctly.
	 * 
	 * @param dbCon
	 */
	protected void commitPendingChanges(ConnectionManager dbCon) {
		
		//Create a clone for each available queue
		Map<KeyMatcher, Integer> memberAdditionsCopy = new HashMap<>();
		Map<KeyMatcher, Integer> memberChangesCopy = new HashMap<>();
		Set<KeyMatcher> memberRemovalsCopy = new HashSet<>();
		Set<KeyMatcher> banAdditionsCopy = new HashSet<>();
		Set<KeyMatcher> banRemovalsCopy = new HashSet<>();
		Set<ChannelGroupData> groupUpdatesCopy = new HashSet<>();
		
		//Enter a synchronized block, which will prevent any other threads from adding updates while the cues are cloned
		synchronized (this) {
			//Copy the member addition queue, then clear the original
			memberAdditionsCopy.putAll(memberAdditions);
			memberAdditions.clear();
			
			//Copy the member changes queue, then clear the original
			memberChangesCopy.putAll(memberUpdates);
			memberUpdates.clear();
			
			//Copy the member removals queue, then clear the original
			memberRemovalsCopy.addAll(memberRemovals);
			memberRemovals.clear();

			//Copy the ban additions queue, then clear the original
			banAdditionsCopy.addAll(banAdditions);
			banAdditions.clear();
			
			//Copy the ban removals queue, then clear the original
			banRemovalsCopy.addAll(banRemovals);
			banRemovals.clear();

			//Copy the group update queue, then clear the original
			groupUpdatesCopy.addAll(groupUpdates);
			groupUpdates.clear();
		}
		commitMemberUpdates(memberAdditionsCopy, memberChangesCopy, memberRemovalsCopy, dbCon.getConnection());
		commitBanUpdates(banAdditionsCopy, banRemovalsCopy, dbCon.getConnection());
		commitGroupUpdates(groupUpdatesCopy, dbCon.getConnection());
	}
		
	private void commitMemberUpdates (Map<KeyMatcher, Integer> memberAdditions, Map<KeyMatcher, Integer> memberChanges, Iterable<KeyMatcher> memberRemovals, Connection con) {
		//If there are pending member additions, run through them and insert them into the relevant database entries
		try {
			if (memberInsertQuery == null) {
				memberInsertQuery = con.prepareStatement("INSERT INTO `"+memberTableName+"` SET `channel` = ?, `user` = ?, `group` = ?");
			}
		} catch (SQLException ex) {
			logger.error("Failed to compile member addition query", ex);
		}
		for (Map.Entry<KeyMatcher, Integer> addition : memberAdditions.entrySet()) {
			int channelID = (Integer) addition.getKey().getValues()[0];
			int userID = (Integer) addition.getKey().getValues()[1];
			try {
				memberInsertQuery.setInt(1, channelID);
				memberInsertQuery.setInt(2, userID);
				memberInsertQuery.setInt(3, addition.getValue());
				memberInsertQuery.execute();
			} catch (SQLException ex) {
				logger.warn("Failed to commit member addition request: "+addition, ex);
            	continue;
            }
			logger.info("Member added to database: "+addition);
		}
		
		//If there are pending member changes, run through them and update the relevant database fields
		try {
			if (memberUpdateQuery == null) {
				memberUpdateQuery = con.prepareStatement("UPDATE `"+memberTableName+"` SET `group` = ? WHERE `channel` = ? AND `user` = ?");
			}
		} catch (SQLException ex) {
			logger.error("Failed to compile member change query", ex);
		}
		for (Entry<KeyMatcher, Integer> update : memberChanges.entrySet()) {
			int channelID = (Integer) update.getKey().getValues()[0];
			int userID = (Integer) update.getKey().getValues()[1];
            try {
				memberUpdateQuery.setInt(1, update.getValue());
				memberUpdateQuery.setInt(2, channelID);
				memberUpdateQuery.setInt(3, userID);
            	memberUpdateQuery.execute();
            } catch (SQLException ex) {
            	logger.warn("Failed to commit member update request: user="+update.getKey().getValues()[1]+" channel="+update.getKey().getValues()[0]+" group="+update.getValue(), ex);
            	continue;
            }
            logger.info("Member updated in database: user="+update.getKey().getValues()[1]+" channel="+update.getKey().getValues()[0]+" group="+update.getValue());
		}

		//If there are pending member removals, run through them and remove them from the relevant database fields
		try {
			if (memberDeleteQuery == null) {
				memberDeleteQuery = con.prepareStatement("DELETE FROM `"+memberTableName+"` WHERE `channel` = ? AND `user` = ? LIMIT 1");
			}
		} catch (SQLException ex) {
			logger.error("Failed to compile member removal query", ex);
		}
		for (KeyMatcher removal : memberRemovals) {
			int channelID = (Integer) removal.getValues()[0];
			int userID = (Integer) removal.getValues()[1];
            try {
				memberDeleteQuery.setInt(1, channelID);
				memberDeleteQuery.setInt(2, userID);
            	memberDeleteQuery.execute();
            } catch (SQLException ex) {
            	logger.warn("Failed to commit member removal request: "+removal, ex);
            	continue;
            }            
            logger.info("Member removed from database: "+removal);
		}
	}
		
	private void commitBanUpdates (Iterable<KeyMatcher> banAdditions, Iterable<KeyMatcher> banRemovals, Connection con) {
		//If there are pending ban additions, run through them and insert them into the relevant database entries
		try {
			if (banInsertQuery == null) {
				banInsertQuery = con.prepareStatement("INSERT INTO `"+banTableName+"` SET `channel` = ?, `user` = ?");
			}
		} catch (SQLException ex) {
			logger.error("Failed to compile ban addition query", ex);
		}
		for (KeyMatcher addition : banAdditions) {
			int channelID = (Integer) addition.getValues()[0];
			int userID = (Integer) addition.getValues()[1];
            try {
				banInsertQuery.setInt(1, channelID);
				banInsertQuery.setInt(2, userID);
            	banInsertQuery.execute();
            } catch (SQLException ex) {
            	logger.warn("Failed to commit ban addition request: "+addition, ex);
            	continue;
            }
            logger.info("Ban added to database: "+addition);
		}
		
		//If there are pending ban removals, run through them and remove them from the relevant database fields
		try {
			if (banDeleteQuery == null) {
				banDeleteQuery = con.prepareStatement("DELETE FROM `"+banTableName+"` WHERE `channel` = ? AND `user` = ? LIMIT 1");
			}
		} catch (SQLException ex) {
			logger.error("Failed to compile ban removal query", ex);
		}
		for (KeyMatcher removal : banRemovals) {
			int channelID = (Integer) removal.getValues()[0];
			int userID = (Integer) removal.getValues()[1];
            try {
				banDeleteQuery.setInt(1, channelID);
				banDeleteQuery.setInt(2, userID);
            	banDeleteQuery.execute();
            } catch (SQLException ex) {
            	logger.warn("Failed to commit ban removal request: "+removal, ex);
            	continue;
            }
            logger.info("Ban removed from database: "+removal);
		}
	}
	
	private void commitGroupUpdates (Iterable<ChannelGroupData> groupUpdates, Connection con) {
		//If there are pending group updates, run through them and update all relevant groups
		try {
			if (groupUpdateQuery == null) {
				groupUpdateQuery = con.prepareStatement("UPDATE `"+groupTableName+"` SET `groupName` = ?, `permissions` = ?,"
						+" `groupType` = ?, `icon` = ? WHERE `groupID` = ? AND `channelID` = ?");
			}
		} catch (SQLException ex) {
			logger.error("Failed to compile group update query", ex);
		}
		for (ChannelGroupData group : groupUpdates) {
			String permissions = Joiner.on(',').join(group.getPermissions());
			try {
				groupUpdateQuery.setString(1, group.getName());
				groupUpdateQuery.setString(2, permissions);
				groupUpdateQuery.setString(3, group.getType().getSimpleName());
				groupUpdateQuery.setString(4, group.groupIconUrl);
				groupUpdateQuery.setInt(5, group.getGroupID());
				groupUpdateQuery.setInt(6, group.getChannelID());
				groupUpdateQuery.execute();
            } catch (SQLException ex) {
            	logger.warn("Failed to commit group update request: "+group, ex);
            	continue;
            }
			logger.info("Group updated in database: "+group);
		}
	}

}
