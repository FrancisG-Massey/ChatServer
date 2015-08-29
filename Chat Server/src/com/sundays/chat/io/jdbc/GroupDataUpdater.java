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
package com.sundays.chat.io.jdbc;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.log4j.Logger;

import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;
import com.sundays.chat.io.ChannelGroupData;
import com.sundays.chat.server.Settings;

public class GroupDataUpdater {
	
	private static final Logger logger = Logger.getLogger(GroupDataUpdater.class);
	
	public static final String RANK_TABLE_NAME = JDBCChannelManager.RANK_TABLE_NAME;
	public static final String BAN_TABLE_NAME = JDBCChannelManager.BAN_TABLE_NAME;
	public static final String GROUP_TABLE_NAME = JDBCChannelManager.GROUP_TABLE_NAME;
	
	public GroupDataUpdater () {
		
	}
	
	//Rank database update queues
	private final CopyOnWriteArrayList<ChannelUserMatcher> rankAdditions = new CopyOnWriteArrayList<ChannelUserMatcher>();
	private final CopyOnWriteArrayList<ChannelUserMatcher> rankRemovals = new CopyOnWriteArrayList<ChannelUserMatcher>();
	private final ConcurrentHashMap<ChannelUserMatcher, Integer> rankChanges = new ConcurrentHashMap<ChannelUserMatcher, Integer>();
	
	//Ban database update queues
	private final CopyOnWriteArrayList<ChannelUserMatcher> banAdditions = new CopyOnWriteArrayList<ChannelUserMatcher>();
	private final CopyOnWriteArrayList<ChannelUserMatcher> banRemovals = new CopyOnWriteArrayList<ChannelUserMatcher>();
	
	//Group update queues
	private final CopyOnWriteArrayList<ChannelGroupData> groupUpdates = new CopyOnWriteArrayList<ChannelGroupData>();
	
	public synchronized void addRank(int channelID, int userID) {
		//Updates the rank change queue with the specified rank addition
		ChannelUserMatcher rankAdditionKey = new ChannelUserMatcher(channelID, userID);
		
		if (rankAdditions.contains(rankAdditionKey)) {
			//Rank addition already queued
			return;
		}
		if (rankRemovals.contains(rankAdditionKey)) {
			//Rank removal was queued beforehand. The addition request cancels this out. Replace with a request to 
			rankRemovals.remove(rankAdditionKey);
			changeRank(channelID, userID, Settings.DEFAULT_RANK);
			return;
		}		
		rankAdditions.add(rankAdditionKey);//Place the user in the addition queues
		/*try {
            UpdateQuery q = new UpdateQuery(ChatServer.server.dbCon, "INSERT INTO `channelRanks` SET `channel` = ?, `user` = ?");
            q.addInt(1, channelID);
            q.addInt(2, userID);
            q.execute();
            rankAdditions.remove(rankAdditionKey);
        } catch (SQLException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        }*/
	}

	public synchronized void changeRank(int channelID, int userID, int rankID) {
		//Updates the rank change queue with the specified rank change
		ChannelUserMatcher rankChangeKey = new ChannelUserMatcher(channelID, userID);
		
		if (rankRemovals.contains(rankChangeKey)) {
			//Rank removal was queued beforehand. Since the removal takes priority, the rank cannot be changed.
			return;
		}	
		if (rankChanges.containsKey(rankChangeKey)) {
			//A rank update was already cued. Remove the previous update from the queue, as this new change overrides it
			rankChanges.remove(rankChangeKey);
		}	
		rankChanges.put(rankChangeKey, rankID);//Place the user in the rank change cue
		/*try {
            UpdateQuery q = new UpdateQuery(ChatServer.server.dbCon, "UPDATE `channelRanks` SET `rank` = ? WHERE `channel` = ? AND `user` = ?");
            q.addInt(1, rankID);
            q.addInt(2, channelID);
            q.addInt(3, userID);
            q.execute();
            rankChanges.remove(rankChangeKey);
        } catch (SQLException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        }*/
	}

	public synchronized void removeRank(int channelID, int userID) {
		//Updates the rank change cue with the specified rank removal
		ChannelUserMatcher rankRemovalKey = new ChannelUserMatcher(channelID, userID);
		
		if (rankRemovals.contains(rankRemovalKey)) {
			//Rank removal already cued
			return;
		}
		if (rankAdditions.contains(rankRemovalKey)) {
			//Rank addition cued beforehand. Remove the addition from the cue. As both events cancel each other out, there is no need to precede with the removal.
			rankAdditions.remove(rankRemovalKey);
			return;
		}	
		if (rankChanges.containsKey(rankRemovalKey)) {
			//A rank update was cued beforehand. Remove the update from the cue
			rankChanges.remove(rankRemovalKey);
		}	
		rankRemovals.add(rankRemovalKey);//Place the user in the removal cue
		/*try {
            DeleteQuery q = new DeleteQuery(ChatServer.server.dbCon, "DELETE FROM `channelRanks` WHERE `channel` = ? AND `user` = ? LIMIT 1");
            q.addInt(1, channelID);
            q.addInt(2, userID);
            q.execute();
            rankRemovals.remove(rankRemovalKey);
        } catch (SQLException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        }*/
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
		/*try {
            InsertQuery q = new InsertQuery(ChatServer.server.dbCon, "INSERT INTO `channelBans` SET `channel` = ?, `user` = ?");
            q.addInt(1, channelID);
            q.addInt(2, userID);
            q.execute();
            banAdditions.remove(banAdditionKey);
        } catch (SQLException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        }*/
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
		/*try {
            DeleteQuery q = new DeleteQuery(ChatServer.server.dbCon, "DELETE FROM `channelBans` WHERE `channel` = ? AND `user` = ? LIMIT 1");
            q.addInt(1, channelID);
            q.addInt(2, userID);
            q.execute();
            banRemovals.remove(banRemovalKey);
        } catch (SQLException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        }*/
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

	protected void commitPendingChanges(ConnectionManager dbCon) {
		/*
		 * This operation commits any pending database changes. It is called from the ChatServer module on a regular basis, in order to commit any pending changes
		 * Please note that the order of the functions in this method (additions, then updates, then removals) MUST remain the same. Otherwise, queries may not commit correctly.
		 */
		
		//Create a clone for each available cue
		ChannelUserMatcher[] rankAdditionsCopy = null;
		HashMap<ChannelUserMatcher, Integer> rankChangesCopy = new HashMap<ChannelUserMatcher, Integer>();
		ChannelUserMatcher[] rankRemovalsCopy = null;
		ChannelUserMatcher[] banAdditionsCopy = null;
		ChannelUserMatcher[] banRemovalsCopy = null;
		ChannelGroupData[] groupUpdatesCopy = null;
		
		//Enter a synchronized block, which will prevent any other threads from adding updates while the cues are cloned
		synchronized (this) {
			//Copy the rank addition cue, then clear the original
			rankAdditionsCopy = new ChannelUserMatcher[rankAdditions.size()];
			rankAdditionsCopy = rankAdditions.toArray(rankAdditionsCopy);
			rankAdditions.clear();
			
			//Copy the rank changes cue, then clear the original
			rankChangesCopy.putAll(rankChanges);
			rankChanges.clear();
			
			//Copy the rank removals cue, then clear the original
			rankRemovalsCopy = new ChannelUserMatcher[rankRemovals.size()];
			rankRemovalsCopy = rankRemovals.toArray(rankRemovalsCopy);
			rankRemovals.clear();

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
					rankInsertQuery = dbCon.getConnection().prepareStatement("INSERT INTO `"+RANK_TABLE_NAME+"` SET `channel` = ?, `user` = ?");
				}
				for (ChannelUserMatcher addition : rankAdditionsCopy) {
					lastAddition = addition;
					rankInsertQuery.setInt(1, addition.channelID);
					rankInsertQuery.setInt(2, addition.userID);
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
					rankUpdateQuery = dbCon.getConnection().prepareStatement("UPDATE `"+RANK_TABLE_NAME+"` SET `rank` = ? WHERE `channel` = ? AND `user` = ?");
				}
				for (Entry<ChannelUserMatcher, Integer> update : rankChangesCopy.entrySet()) {
					rankUpdateQuery.setInt(1, update.getValue());
					rankUpdateQuery.setInt(2, update.getKey().channelID);
					rankUpdateQuery.setInt(3, update.getKey().userID);
		            try {
		            	rankUpdateQuery.execute();
		            } catch (MySQLIntegrityConstraintViolationException ex) {
		            	logger.warn("Failed to commit rank update request: user="+update.getKey().userID+" channel="+update.getKey().channelID+" rank="+update.getValue(), ex);
		            	continue;
		            }
		            logger.info("Rank updated in database: user="+update.getKey().userID+" channel="+update.getKey().channelID+" rank="+update.getValue());
				}
			} catch (SQLException ex) {
				logger.error("Failed to commit rank change requests", ex);
			}
		}
		
		if (rankRemovalsCopy.length > 0) {
			//If there are pending rank removals, run through them and remove them from the relevant database fields
			try {
				if (rankDeleteQuery == null) {
					rankDeleteQuery = dbCon.getConnection().prepareStatement("DELETE FROM `"+RANK_TABLE_NAME+"` WHERE `channel` = ? AND `user` = ? LIMIT 1");
				}
				for (ChannelUserMatcher removal : rankRemovalsCopy) {
					rankDeleteQuery.setInt(1, removal.channelID);
					rankDeleteQuery.setInt(2, removal.userID);
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
					banInsertQuery = dbCon.getConnection().prepareStatement("INSERT INTO `"+BAN_TABLE_NAME+"` SET `channel` = ?, `user` = ?");
				}
				for (ChannelUserMatcher addition : banAdditionsCopy) {
					banInsertQuery.setInt(1, addition.channelID);
					banInsertQuery.setInt(2, addition.userID);
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
					banDeleteQuery = dbCon.getConnection().prepareStatement("DELETE FROM `"+BAN_TABLE_NAME+"` WHERE `channel` = ? AND `user` = ? LIMIT 1");
				}
				for (ChannelUserMatcher removal : banRemovalsCopy) {
					banDeleteQuery.setInt(1, removal.channelID);
					banDeleteQuery.setInt(2, removal.userID);
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
					groupUpdateQuery = dbCon.getConnection().prepareStatement("UPDATE `"+GROUP_TABLE_NAME+"` SET `groupName` = ?, `permissions` = ?,"
							+" `groupType` = ?, `groupIconUrl` = ? WHERE `groupID` = ? AND `channelID` = ?");
				}
				for (ChannelGroupData group : groupUpdatesCopy) {
					groupUpdateQuery.setString(1, group.groupName);
					groupUpdateQuery.setString(2, group.permissions);
					groupUpdateQuery.setString(3, group.type);
					groupUpdateQuery.setString(4, group.groupIconUrl);
					groupUpdateQuery.setInt(5, group.groupID);
					groupUpdateQuery.setInt(6, group.channelID);
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
