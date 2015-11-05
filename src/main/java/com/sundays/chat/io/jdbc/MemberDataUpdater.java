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

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;

import com.google.common.base.Joiner;
import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;
import com.sundays.chat.io.ChannelGroupData;
import com.sundays.chat.server.channel.ChannelGroup;

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
	private final Set<KeyMatcher> memberAdditions = new HashSet<>();
	private final Set<KeyMatcher> memberRemovals = new HashSet<>();
	private final Map<KeyMatcher, Integer> memberUpdates = new HashMap<>();
	
	//Ban database update queues
	private final Set<KeyMatcher> banAdditions = new HashSet<>();
	private final Set<KeyMatcher> banRemovals = new HashSet<>();
	
	//Group update queues
	private final Set<ChannelGroupData> groupUpdates = new HashSet<>();
	
	protected synchronized void addMember(int channelID, int userID) {
		//Updates the rank change queue with the specified rank addition
		KeyMatcher memberKey = new KeyMatcher(channelID, userID);
		
		if (memberAdditions.contains(memberKey)) {
			//Rank addition already queued
			return;
		}
		if (memberRemovals.contains(memberKey)) {
			//Rank removal was queued beforehand. The addition request cancels this out. Replace with a request to 
			memberRemovals.remove(memberKey);
			updateMember(channelID, userID, ChannelGroup.DEFAULT_GROUP);
			return;
		}		
		memberAdditions.add(memberKey);//Place the user in the addition queues
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
		if (memberAdditions.contains(memberKey)) {
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
	
	private PreparedStatement rankInsertQuery;
	private PreparedStatement rankUpdateQuery;
	private PreparedStatement rankDeleteQuery;
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
		KeyMatcher[] rankAdditionsCopy = null;
		Map<KeyMatcher, Integer> rankChangesCopy = new HashMap<>();
		KeyMatcher[] rankRemovalsCopy = null;
		KeyMatcher[] banAdditionsCopy = null;
		KeyMatcher[] banRemovalsCopy = null;
		ChannelGroupData[] groupUpdatesCopy = null;
		
		//Enter a synchronized block, which will prevent any other threads from adding updates while the cues are cloned
		synchronized (this) {
			//Copy the member addition queue, then clear the original
			rankAdditionsCopy = new KeyMatcher[memberAdditions.size()];
			rankAdditionsCopy = memberAdditions.toArray(rankAdditionsCopy);
			memberAdditions.clear();
			
			//Copy the member changes queue, then clear the original
			rankChangesCopy.putAll(memberUpdates);
			memberUpdates.clear();
			
			//Copy the member removals queue, then clear the original
			rankRemovalsCopy = new KeyMatcher[memberRemovals.size()];
			rankRemovalsCopy = memberRemovals.toArray(rankRemovalsCopy);
			memberRemovals.clear();

			//Copy the ban additions queue, then clear the original
			banAdditionsCopy = new KeyMatcher[banAdditions.size()];
			banAdditionsCopy = banAdditions.toArray(banAdditionsCopy);
			banAdditions.clear();
			
			//Copy the ban removals queue, then clear the original
			banRemovalsCopy = new KeyMatcher[banRemovals.size()];
			banRemovalsCopy = banRemovals.toArray(banRemovalsCopy);
			banRemovals.clear();

			//Copy the group update queue, then clear the original
			groupUpdatesCopy = new ChannelGroupData[groupUpdates.size()];
			groupUpdatesCopy = groupUpdates.toArray(groupUpdatesCopy);
			groupUpdates.clear();
		}
		
		if (rankAdditionsCopy.length > 0) {
			//If there are pending member additions, run through them and insert them into the relevant database entries
			KeyMatcher lastAddition = null;
			try {
				if (rankInsertQuery == null) {
					rankInsertQuery = dbCon.getConnection().prepareStatement("INSERT INTO `"+memberTableName+"` SET `channel` = ?, `user` = ?");
				}
				for (KeyMatcher addition : rankAdditionsCopy) {
					lastAddition = addition;
					int channelID = (Integer) addition.getValues()[0];
					int userID = (Integer) addition.getValues()[1];
					rankInsertQuery.setInt(1, channelID);
					rankInsertQuery.setInt(2, userID);
					try {
						rankInsertQuery.execute();
					} catch (MySQLIntegrityConstraintViolationException ex) {
						logger.warn("Failed to commit rank addition request: "+addition, ex);
		            	continue;
		            }
					logger.info("Rank added to database: "+addition);
				}
			} catch (SQLException ex) {
				logger.error("Failed to commit rank addition requests. Last addition: "+lastAddition, ex);
			}
		}
		
		if (rankChangesCopy.size() > 0) {
			//If there are pending member changes, run through them and update the relevant database fields
			try {
				if (rankUpdateQuery == null) {
					rankUpdateQuery = dbCon.getConnection().prepareStatement("UPDATE `"+memberTableName+"` SET `rank` = ? WHERE `channel` = ? AND `user` = ?");
				}
				for (Entry<KeyMatcher, Integer> update : rankChangesCopy.entrySet()) {
					int channelID = (Integer) update.getKey().getValues()[0];
					int userID = (Integer) update.getKey().getValues()[1];
					rankUpdateQuery.setInt(1, update.getValue());
					rankUpdateQuery.setInt(2, channelID);
					rankUpdateQuery.setInt(3, userID);
		            try {
		            	rankUpdateQuery.execute();
		            } catch (MySQLIntegrityConstraintViolationException ex) {
		            	logger.warn("Failed to commit rank update request: user="+update.getKey().getValues()[1]+" channel="+update.getKey().getValues()[0]+" rank="+update.getValue(), ex);
		            	continue;
		            }
		            logger.info("Rank updated in database: user="+update.getKey().getValues()[1]+" channel="+update.getKey().getValues()[0]+" rank="+update.getValue());
				}
			} catch (SQLException ex) {
				logger.error("Failed to commit rank change requests", ex);
			}
		}
		
		if (rankRemovalsCopy.length > 0) {
			//If there are pending member removals, run through them and remove them from the relevant database fields
			try {
				if (rankDeleteQuery == null) {
					rankDeleteQuery = dbCon.getConnection().prepareStatement("DELETE FROM `"+memberTableName+"` WHERE `channel` = ? AND `user` = ? LIMIT 1");
				}
				for (KeyMatcher removal : rankRemovalsCopy) {
					int channelID = (Integer) removal.getValues()[0];
					int userID = (Integer) removal.getValues()[1];
					rankDeleteQuery.setInt(1, channelID);
					rankDeleteQuery.setInt(2, userID);
		            try {
		            	rankDeleteQuery.execute();
		            } catch (MySQLIntegrityConstraintViolationException ex) {
		            	logger.warn("Failed to commit rank removal request: "+removal, ex);
		            	continue;
		            }            
		            logger.info("Rank removed from database: "+removal);
				}
			} catch (SQLException ex) {
				logger.error("Failed to commit rank removal requests", ex);
			}
		}
		
		if (banAdditionsCopy.length > 0) {
			//If there are pending ban additions, run through them and insert them into the relevant database entries
			try {
				if (banInsertQuery == null) {
					banInsertQuery = dbCon.getConnection().prepareStatement("INSERT INTO `"+banTableName+"` SET `channel` = ?, `user` = ?");
				}
				for (KeyMatcher addition : banAdditionsCopy) {
					int channelID = (Integer) addition.getValues()[0];
					int userID = (Integer) addition.getValues()[1];
					banInsertQuery.setInt(1, channelID);
					banInsertQuery.setInt(2, userID);
		            try {
		            	banInsertQuery.execute();
		            } catch (MySQLIntegrityConstraintViolationException ex) {
		            	logger.warn("Failed to commit ban addition request: "+addition, ex);
		            	continue;
		            }
		            logger.info("Ban added to database: "+addition);
				}
			} catch (SQLException ex) {
				logger.error("Failed to commit ban addition requests", ex);
			}
		}
		
		if (banRemovalsCopy.length > 0) {
			//If there are pending ban removals, run through them and remove them from the relevant database fields
			try {
				if (banDeleteQuery == null) {
					banDeleteQuery = dbCon.getConnection().prepareStatement("DELETE FROM `"+banTableName+"` WHERE `channel` = ? AND `user` = ? LIMIT 1");
				}
				for (KeyMatcher removal : banRemovalsCopy) {
					int channelID = (Integer) removal.getValues()[0];
					int userID = (Integer) removal.getValues()[1];
					banDeleteQuery.setInt(1, channelID);
					banDeleteQuery.setInt(2, userID);
		            try {
		            	banDeleteQuery.execute();
		            } catch (MySQLIntegrityConstraintViolationException ex) {
		            	logger.warn("Failed to commit ban removal request: "+removal, ex);
		            	continue;
		            }
		            logger.info("Ban removed from database: "+removal);
				}
			} catch (SQLException ex) {
				logger.error("Failed to commit ban removal requests", ex);
			}
		}
		
		if (groupUpdatesCopy.length > 0) {
			//If there are pending group updates, run through them and update all relevant groups
			try {
				if (groupUpdateQuery == null) {
					groupUpdateQuery = dbCon.getConnection().prepareStatement("UPDATE `"+groupTableName+"` SET `groupName` = ?, `permissions` = ?,"
							+" `groupType` = ?, `icon` = ? WHERE `groupID` = ? AND `channelID` = ?");
				}
				for (ChannelGroupData group : groupUpdatesCopy) {
					String permissions = Joiner.on(',').join(group.getPermissions());
					groupUpdateQuery.setString(1, group.getName());
					groupUpdateQuery.setString(2, permissions);
					groupUpdateQuery.setString(3, group.getType().getSimpleName());
					groupUpdateQuery.setString(4, group.groupIconUrl);
					groupUpdateQuery.setInt(5, group.getGroupID());
					groupUpdateQuery.setInt(6, group.getChannelID());
					try {
						groupUpdateQuery.execute();
		            } catch (MySQLIntegrityConstraintViolationException ex) {
		            	logger.warn("Failed to commit group update request: "+group, ex);
		            	continue;
		            }
					logger.info("Group updated in database: "+group);
				}
			} catch (SQLException ex) {
				logger.error("Failed to commit group update requests", ex);
			}
		}
	}

}
