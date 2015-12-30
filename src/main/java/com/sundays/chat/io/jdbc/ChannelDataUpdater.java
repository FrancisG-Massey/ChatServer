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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sundays.chat.io.ChannelDetails;

public class ChannelDataUpdater {
	
	private static final Logger logger = LoggerFactory.getLogger(ChannelDataUpdater.class);
		
	private final String detailTableName;
	private final String attributeTableName;
	
	private final Map<KeyMatcher, String> attrAdditions = new HashMap<>();
	private final Map<KeyMatcher, String> attrUpdates = new HashMap<>();
	private final Set<KeyMatcher> attrRemovals = new HashSet<>();

	private final Map<Integer, ChannelDetails> detailChanges = new HashMap<>();
	
	protected ChannelDataUpdater (String detailTableName, String attributeTableName) {
		this.detailTableName = detailTableName;
		this.attributeTableName = attributeTableName;
	}
	
	protected synchronized void addAttribute (int channelID, String key, String value) {
		KeyMatcher attrKey = new KeyMatcher(channelID, key);
		
		if (attrAdditions.containsKey(attrKey)) {
			//Attribute addition already queued
			return;
		}
		if (attrRemovals.contains(attrKey)) {
			//Rank removal was queued beforehand. The addition request cancels this out. Replace with a request to 
			attrRemovals.remove(attrKey);
			updateAttribute(channelID, key, value);
			return;
		}		
		attrAdditions.put(attrKey, value);//Place the user in the addition queues
	}

	protected synchronized void updateAttribute(int channelID, String key, String value) {
		KeyMatcher attrKey = new KeyMatcher(channelID, key);
		
		if (attrRemovals.contains(attrKey)) {
			//Attribute removal was queued beforehand. Since the removal takes priority, the attribute cannot be changed.
			return;
		}	
		if (attrUpdates.containsKey(attrKey)) {
			//An attribute update was already queued. Remove the previous update from the queue, as this new change overrides it
			attrUpdates.remove(attrKey);
		}	
		attrUpdates.put(attrKey, value);//Place the update in the attribute update change queue
	}

	protected synchronized void removeAttribute(int channelID, String key) {
		KeyMatcher attrKey = new KeyMatcher(channelID, key);
		
		if (attrRemovals.contains(attrKey)) {
			//Attribute removal already queued
			return;
		}
		if (attrAdditions.containsKey(attrKey)) {
			//Attribute addition queued beforehand. Remove the addition from the queue. As both events cancel each other out, there is no need to precede with the removal.
			attrAdditions.remove(attrKey);
			return;
		}	
		if (attrUpdates.containsKey(attrKey)) {
			//An attribute update was queued beforehand. Remove the update from the queue
			attrUpdates.remove(attrKey);
		}	
		attrRemovals.add(attrKey);//Place the attribute in the removal queue
	}
	
	protected synchronized void syncDetails (int channelID, ChannelDetails details) {
		detailChanges.put(channelID, details);
	}

	
	private PreparedStatement attrInsertQuery;
	private PreparedStatement attrUpdateQuery;
	private PreparedStatement attrDeleteQuery;
	private PreparedStatement channelUpdateQuery;
	
	protected void commitPendingChanges (ConnectionManager dbCon) {

		Map<KeyMatcher, String> attrAdditionCopy = new HashMap<>();
		Map<KeyMatcher, String> attrUpdateCopy = new HashMap<>();
		Set<KeyMatcher> attrRemovalCopy = new HashSet<>();
		
		Map<Integer, ChannelDetails> detailChangesCopy = new HashMap<>();
		
		synchronized (this) {
			detailChangesCopy.putAll(detailChanges);
			detailChanges.clear();
			
			attrAdditionCopy.putAll(attrAdditions);
			attrAdditions.clear();
			
			attrUpdateCopy.putAll(attrUpdates);
			attrUpdates.clear();
			
			attrRemovalCopy.addAll(attrRemovals);
			attrRemovals.clear();
		}
		
		if (detailChanges.size() > 0) {
			try {
				if (channelUpdateQuery == null) {
					channelUpdateQuery = dbCon.getConnection().prepareStatement("UPDATE `"+detailTableName+"` SET" +
							" `name` = ?, `abbrieviation` = ?," +
							" `trackMessages` = ?, " +
							" `owner` = ? WHERE `id` = ?");
				}
				for (Entry<Integer, ChannelDetails> update : detailChangesCopy.entrySet()) {
					ChannelDetails details = update.getValue();
					channelUpdateQuery.setString(1, details.getName());
					channelUpdateQuery.setString(2, details.getAlias());
					channelUpdateQuery.setBoolean(3, details.isTrackMessages());
					channelUpdateQuery.setInt(4, details.getOwner());
					channelUpdateQuery.setInt(5, details.getId());
					try {
						channelUpdateQuery.execute();
		            } catch (SQLException ex) {
		            	logger.warn("Failed to commit channel detail update request: "+details, ex);
		            	continue;
		            }
					logger.info("Channel data updated in database: "+details);
				}
			} catch (SQLException ex) {
				logger.error("Failed to commit channel update requests", ex);
			}
		}
		
		if (attrAdditionCopy.size() > 0) {
			//If there are pending attribute additions, run through them and insert them into the relevant database entries
			KeyMatcher lastAddition = null;
			try {
				if (attrInsertQuery == null) {
					attrInsertQuery = dbCon.getConnection().prepareStatement("INSERT INTO `"+attributeTableName+"` SET `channel` = ?, `key` = ?, `value` = ?");
					
				}
				for (Map.Entry<KeyMatcher, String> addition : attrAdditionCopy.entrySet()) {
					lastAddition = addition.getKey();
					int channelID = (Integer) addition.getKey().getValues()[0];
					String key = (String) addition.getKey().getValues()[1];
					attrInsertQuery.setInt(1, channelID);
					attrInsertQuery.setString(2, key);
					try {
						attrInsertQuery.execute();
					} catch (SQLException ex) {
						logger.warn("Failed to commit attribute addition request: "+addition, ex);
		            	continue;
		            }
					logger.info("Rank added to database: "+addition);
				}
			} catch (SQLException ex) {
				logger.error("Failed to commit attribute addition requests. Last addition: "+lastAddition, ex);
			}
		}
		
		if (attrUpdateCopy.size() > 0) {
			//If there are pending attribute changes, run through them and update the relevant database fields
			try {
				if (attrUpdateQuery == null) {
					attrUpdateQuery = dbCon.getConnection().prepareStatement("UPDATE `"+attributeTableName+"` SET `value` = ? WHERE `channel` = ? AND `key` = ?");
				}
				for (Entry<KeyMatcher, String> update : attrUpdateCopy.entrySet()) {
					int channelID = (Integer) update.getKey().getValues()[0];
					String key = (String) update.getKey().getValues()[1];
					attrUpdateQuery.setString(1, update.getValue());
					attrUpdateQuery.setInt(2, channelID);
					attrUpdateQuery.setString(3, key);
		            try {
		            	attrUpdateQuery.execute();
		            } catch (SQLException ex) {
		            	logger.warn("Failed to commit attribute update request: key="+update.getKey().getValues()[1]+" channel="+update.getKey().getValues()[0]+" value="+update.getValue(), ex);
		            	continue;
		            }
		            logger.info("Attribute updated in database: key="+update.getKey().getValues()[1]+" channel="+update.getKey().getValues()[0]+" value="+update.getValue());
				}
			} catch (SQLException ex) {
				logger.error("Failed to commit rank change requests", ex);
			}
		}
		
		if (attrRemovalCopy.size() > 0) {
			//If there are pending member removals, run through them and remove them from the relevant database fields
			try {
				if (attrDeleteQuery == null) {
					attrDeleteQuery = dbCon.getConnection().prepareStatement("DELETE FROM `"+attributeTableName+"` WHERE `channel` = ? AND `key` = ? LIMIT 1");
				}
				for (KeyMatcher removal : attrRemovalCopy) {
					int channelID = (Integer) removal.getValues()[0];
					String key = (String) removal.getValues()[1];
					attrDeleteQuery.setInt(1, channelID);
					attrDeleteQuery.setString(2, key);
		            try {
		            	attrDeleteQuery.execute();
		            } catch (SQLException ex) {
		            	logger.warn("Failed to commit attribute removal request: "+removal, ex);
		            	continue;
		            }            
		            logger.info("Attribute removed from database: "+removal);
				}
			} catch (SQLException ex) {
				logger.error("Failed to commit attribute removal requests", ex);
			}
		}
	}
}
