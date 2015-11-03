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

public class GroupDataUpdater {
	
	private static final Logger logger = Logger.getLogger(GroupDataUpdater.class);
	
	public GroupDataUpdater () {
		
	}
	
	//Rank database update queues
	private final Set<ChannelUserMatcher> memberAdditions = new HashSet<>();
	private final Set<ChannelUserMatcher> memberRemovals = new HashSet<>();
	private final Map<ChannelUserMatcher, Integer> memberUpdates = new HashMap<>();
	
	//Ban database update queues
	private final Set<ChannelUserMatcher> banAdditions = new HashSet<>();
	private final Set<ChannelUserMatcher> banRemovals = new HashSet<>();
	
	//Group update queues
	private final Set<ChannelGroupData> groupUpdates = new HashSet<>();
	
	public synchronized void addRank(int channelID, int userID) {
		//Updates the rank change queue with the specified rank addition
		ChannelUserMatcher rankAdditionKey = new ChannelUserMatcher(channelID, userID);
		
		if (memberAdditions.contains(rankAdditionKey)) {
			//Rank addition already queued
			return;
		}
		if (memberRemovals.contains(rankAdditionKey)) {
			//Rank removal was queued beforehand. The addition request cancels this out. Replace with a request to 
			memberRemovals.remove(rankAdditionKey);
			changeRank(channelID, userID, ChannelGroup.DEFAULT_GROUP);
			return;
		}		
		memberAdditions.add(rankAdditionKey);//Place the user in the addition queues
	}

	public synchronized void changeRank(int channelID, int userID, int group) {
		//Updates the rank change queue with the specified rank change
		ChannelUserMatcher rankChangeKey = new ChannelUserMatcher(channelID, userID);
		
		if (memberRemovals.contains(rankChangeKey)) {
			//Rank removal was queued beforehand. Since the removal takes priority, the rank cannot be changed.
			return;
		}	
		if (memberUpdates.containsKey(rankChangeKey)) {
			//A rank update was already cued. Remove the previous update from the queue, as this new change overrides it
			memberUpdates.remove(rankChangeKey);
		}	
		memberUpdates.put(rankChangeKey, group);//Place the user in the rank change cue
	}

	public synchronized void removeRank(int channelID, int userID) {
		//Updates the rank change cue with the specified rank removal
		ChannelUserMatcher rankRemovalKey = new ChannelUserMatcher(channelID, userID);
		
		if (memberRemovals.contains(rankRemovalKey)) {
			//Rank removal already cued
			return;
		}
		if (memberAdditions.contains(rankRemovalKey)) {
			//Rank addition cued beforehand. Remove the addition from the cue. As both events cancel each other out, there is no need to precede with the removal.
			memberAdditions.remove(rankRemovalKey);
			return;
		}	
		if (memberUpdates.containsKey(rankRemovalKey)) {
			//A rank update was cued beforehand. Remove the update from the cue
			memberUpdates.remove(rankRemovalKey);
		}	
		memberRemovals.add(rankRemovalKey);//Place the user in the removal cue
	}

	public synchronized void addBan(int channelID, int userID) {
		//Updates the ban addition cue with the specified ban addition
		ChannelUserMatcher banAdditionKey = new ChannelUserMatcher(channelID, userID);
		
		if (banAdditions.contains(banAdditionKey)) {
			//Ban addition already cued
			return;
		}
		if (banRemovals.contains(banAdditionKey)) {
			//Ban removal was cued beforehand. Remove the removal from the cue. Since both requests cancel each other out, their is no need to procede with the addition.
			banRemovals.remove(banAdditionKey);
			return;
		}	
		banAdditions.add(banAdditionKey);//Place the user in the addition cue
	}

	public synchronized void removeBan(int channelID, int userID) {
		//Updates the ban removal cue with the specified rank removal
		ChannelUserMatcher banRemovalKey = new ChannelUserMatcher(channelID, userID);
		
		if (banRemovals.contains(banRemovalKey)) {
			//Ban removal already cued
			return;
		}
		if (banAdditions.contains(banRemovalKey)) {
			//Ban addition was cued beforehand. Since both requests cancel each other out, removing the addition request is sufficient
			banAdditions.remove(banRemovalKey);
			return;
		}	
		banRemovals.add(banRemovalKey);//Place the user in the removal cue
	}
	
	public synchronized void updateGroup (int channelID, ChannelGroupData group) {
		if (groupUpdates.contains(group)) {
			//Group update already cued
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
		
		//Create a clone for each available cue
		ChannelUserMatcher[] rankAdditionsCopy = null;
		Map<ChannelUserMatcher, Integer> rankChangesCopy = new HashMap<>();
		ChannelUserMatcher[] rankRemovalsCopy = null;
		ChannelUserMatcher[] banAdditionsCopy = null;
		ChannelUserMatcher[] banRemovalsCopy = null;
		ChannelGroupData[] groupUpdatesCopy = null;
		
		//Enter a synchronized block, which will prevent any other threads from adding updates while the cues are cloned
		synchronized (this) {
			//Copy the rank addition cue, then clear the original
			rankAdditionsCopy = new ChannelUserMatcher[memberAdditions.size()];
			rankAdditionsCopy = memberAdditions.toArray(rankAdditionsCopy);
			memberAdditions.clear();
			
			//Copy the rank changes cue, then clear the original
			rankChangesCopy.putAll(memberUpdates);
			memberUpdates.clear();
			
			//Copy the rank removals cue, then clear the original
			rankRemovalsCopy = new ChannelUserMatcher[memberRemovals.size()];
			rankRemovalsCopy = memberRemovals.toArray(rankRemovalsCopy);
			memberRemovals.clear();

			//Copy the ban additions cue, then clear the original
			banAdditionsCopy = new ChannelUserMatcher[banAdditions.size()];
			banAdditionsCopy = banAdditions.toArray(banAdditionsCopy);
			banAdditions.clear();
			
			//Copy the ban removals cue, then clear the original
			banRemovalsCopy = new ChannelUserMatcher[banRemovals.size()];
			banRemovalsCopy = banRemovals.toArray(banRemovalsCopy);
			banRemovals.clear();

			//Copy the group update cue, then clear the original
			groupUpdatesCopy = new ChannelGroupData[groupUpdates.size()];
			groupUpdatesCopy = groupUpdates.toArray(groupUpdatesCopy);
			groupUpdates.clear();
		}
		
		if (rankAdditionsCopy.length > 0) {
			//If there are pending rank additions, run through them and insert them into the relevant database entries
			ChannelUserMatcher lastAddition = null;
			try {
				if (rankInsertQuery == null) {
					rankInsertQuery = dbCon.getConnection().prepareStatement("INSERT INTO `"+JDBCChannelSave.MEMBER_TABLE_NAME+"` SET `channel` = ?, `user` = ?");
				}
				for (ChannelUserMatcher addition : rankAdditionsCopy) {
					lastAddition = addition;
					rankInsertQuery.setInt(1, addition.getChannelID());
					rankInsertQuery.setInt(2, addition.getUserID());
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
			//If there are pending rank changes, run through them and update the relevant database fields
			try {
				if (rankUpdateQuery == null) {
					rankUpdateQuery = dbCon.getConnection().prepareStatement("UPDATE `"+JDBCChannelSave.MEMBER_TABLE_NAME+"` SET `rank` = ? WHERE `channel` = ? AND `user` = ?");
				}
				for (Entry<ChannelUserMatcher, Integer> update : rankChangesCopy.entrySet()) {
					rankUpdateQuery.setInt(1, update.getValue());
					rankUpdateQuery.setInt(2, update.getKey().getChannelID());
					rankUpdateQuery.setInt(3, update.getKey().getUserID());
		            try {
		            	rankUpdateQuery.execute();
		            } catch (MySQLIntegrityConstraintViolationException ex) {
		            	logger.warn("Failed to commit rank update request: user="+update.getKey().getUserID()+" channel="+update.getKey().getChannelID()+" rank="+update.getValue(), ex);
		            	continue;
		            }
		            logger.info("Rank updated in database: user="+update.getKey().getUserID()+" channel="+update.getKey().getChannelID()+" rank="+update.getValue());
				}
			} catch (SQLException ex) {
				logger.error("Failed to commit rank change requests", ex);
			}
		}
		
		if (rankRemovalsCopy.length > 0) {
			//If there are pending rank removals, run through them and remove them from the relevant database fields
			try {
				if (rankDeleteQuery == null) {
					rankDeleteQuery = dbCon.getConnection().prepareStatement("DELETE FROM `"+JDBCChannelSave.MEMBER_TABLE_NAME+"` WHERE `channel` = ? AND `user` = ? LIMIT 1");
				}
				for (ChannelUserMatcher removal : rankRemovalsCopy) {
					rankDeleteQuery.setInt(1, removal.getChannelID());
					rankDeleteQuery.setInt(2, removal.getUserID());
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
					banInsertQuery = dbCon.getConnection().prepareStatement("INSERT INTO `"+JDBCChannelSave.BAN_TABLE_NAME+"` SET `channel` = ?, `user` = ?");
				}
				for (ChannelUserMatcher addition : banAdditionsCopy) {
					banInsertQuery.setInt(1, addition.getChannelID());
					banInsertQuery.setInt(2, addition.getUserID());
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
					banDeleteQuery = dbCon.getConnection().prepareStatement("DELETE FROM `"+JDBCChannelSave.BAN_TABLE_NAME+"` WHERE `channel` = ? AND `user` = ? LIMIT 1");
				}
				for (ChannelUserMatcher removal : banRemovalsCopy) {
					banDeleteQuery.setInt(1, removal.getChannelID());
					banDeleteQuery.setInt(2, removal.getUserID());
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
					groupUpdateQuery = dbCon.getConnection().prepareStatement("UPDATE `"+JDBCChannelSave.GROUP_TABLE_NAME+"` SET `groupName` = ?, `permissions` = ?,"
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
