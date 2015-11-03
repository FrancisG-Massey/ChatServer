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
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import com.sundays.chat.io.ChannelDetails;

public class ChannelDataUpdater {
	
	private static final Logger logger = Logger.getLogger(ChannelDataUpdater.class);
	
	private final Map<Integer, ChannelDetails> detailChanges = new HashMap<>();
	
	protected void syncDetails (int channelID, ChannelDetails details) {
		synchronized (detailChanges) {
			if (detailChanges.containsKey(channelID)) {
				detailChanges.remove(channelID);
			}
			detailChanges.put(channelID, details);
		}
	}
	
	private PreparedStatement channelUpdateQuery;
	
	protected void commitPendingChanges (ConnectionManager dbCon) {
		if (detailChanges.size() > 0) {
			try {
				if (channelUpdateQuery == null) {
					channelUpdateQuery = dbCon.getConnection().prepareStatement("UPDATE `"+JDBCChannelSave.DETAIL_TABLE_NAME+"` SET" +
							" `name` = ?, `abbrieviation` = ?," +
							" `openingMessage` = ?, `trackMessages` = ?, " +
							" `owner` = ? WHERE `id` = ?");
				}
				Map<Integer, ChannelDetails> detailChangesCopy = new HashMap<Integer, ChannelDetails>();
				synchronized (detailChanges) {
					detailChangesCopy.putAll(detailChanges);
					detailChanges.clear();
				}
				for (Entry<Integer, ChannelDetails> update : detailChangesCopy.entrySet()) {
					ChannelDetails details = update.getValue();
					channelUpdateQuery.setString(1, details.getName());
					channelUpdateQuery.setString(2, details.getAlias());
					channelUpdateQuery.setString(3, details.getWelcomeMessage());
					channelUpdateQuery.setBoolean(4, details.isTrackMessages());
					channelUpdateQuery.setInt(5, details.getOwner());
					channelUpdateQuery.setInt(6, details.getId());
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
	}
}
