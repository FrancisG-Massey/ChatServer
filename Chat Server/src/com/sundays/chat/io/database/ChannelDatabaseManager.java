/*******************************************************************************
 * Copyright (c) 2015 Francis G.
 *
 * This file is part of ${enclosing_project}.
 *
 * ${enclosing_project} is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ${enclosing_project} is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ChatServer.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package com.sundays.chat.io.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.sundays.chat.interfaces.ChannelDetails;
import com.sundays.chat.interfaces.ChannelGroupData;
import com.sundays.chat.io.ChannelDataManager;

public class ChannelDatabaseManager implements ChannelDataManager {

	private static final Logger logger = Logger.getLogger(GroupDataUpdater.class);

	public static final String RANK_TABLE_NAME = "channelRanks";
	public static final String BAN_TABLE_NAME = "channelBans";
	public static final String DETAIL_TABLE_NAME = "channels";
	public static final String GROUP_TABLE_NAME = "channelGroups";

	private GroupDataUpdater rankBanUpdater = new GroupDataUpdater();
	private ChannelDataUpdater channelUpdater = new ChannelDataUpdater();
	private final ConnectionManager dbCon;

	private PreparedStatement detailFetchQuery;
	private PreparedStatement rankFetchQuery;
	private PreparedStatement banFetchQuery;
	private PreparedStatement groupFetchQuery;

	public ChannelDatabaseManager(ConnectionManager dbCon) {
		this.dbCon = dbCon;
	}

	@Override
	public void addRank(int channelID, int userID) {
		rankBanUpdater.addRank(channelID, userID);
	}

	@Override
	public void changeRank(int channelID, int userID, int rankID) {
		rankBanUpdater.changeRank(channelID, userID, rankID);
	}

	@Override
	public void removeRank(int channelID, int userID) {
		rankBanUpdater.removeRank(channelID, userID);
	}

	@Override
	public void addBan(int channelID, int userID) {
		rankBanUpdater.addBan(channelID, userID);
	}

	@Override
	public void removeBan(int channelID, int userID) {
		rankBanUpdater.removeBan(channelID, userID);
	}

	@Override
	public void addGroup(int channelID, ChannelGroupData group) {

	}

	@Override
	public void updateGroup(int channelID, ChannelGroupData group) {

	}

	@Override
	public void removeGroup(int channelID, int groupID) {

	}

	@Override
	public void syncDetails(int channelID, ChannelDetails details) {
		channelUpdater.syncDetails(channelID, details);
	}

	@Override
	public void commitPendingChanges() {
		// Commits all pending updates to the database back-end
		rankBanUpdater.commitPendingChanges(dbCon);// Commits the rank and ban changes
		channelUpdater.commitPendingChanges(dbCon);// Commits pending channel details changes
	}

	@Override
	public ChannelDetails getChannelDetails(int channelID) {
		ChannelDetails details = null;
		try {
			if (detailFetchQuery == null) {
				detailFetchQuery = dbCon.getConnection().prepareStatement(
						"SELECT `channelID`, `channelName`, `channelAbbr`, "
								+ "`permissions`, `rankNames`, `openingMessage`, `trackMessages`," + "`owner`" + " FROM `"
								+ DETAIL_TABLE_NAME + "` WHERE `channelID` = ?");
			}
			detailFetchQuery.setInt(1, channelID);
			detailFetchQuery.execute();
			ResultSet res = detailFetchQuery.getResultSet();
			while (res.next()) {
				Map<Integer, String> rankNames = null;
				try {
					rankNames = (res.getBytes(5) == null ? null : ChannelDataUpdater.decompressRankNamesV2(res.getBytes(5)));
				} catch (IllegalArgumentException ex) {
					logger.warn("Invalid rank names data for channel: " + channelID + ". Using default values instead.");
				}
				details = new ChannelDetails(res.getInt(1),// Channel ID
						res.getString(2),// Channel Name
						res.getString(6),// Opening Message
						res.getString(3),// Channel Abbreviation
						ChannelDataUpdater.decompressPermissions(res.getBytes(4)),// Channel Permissions
						rankNames,// Channel Rank Names
						res.getBoolean(7),// Whether or not to track messages
						res.getInt(8));// Channel owner userID
			}
		} catch (SQLException ex) {
			logger.error("Failed to fetch details for channel " + channelID, ex);
		}
		return details;
	}

	@Override
	public List<Integer> getChannelBans(int channelID) {
		List<Integer> bans = new ArrayList<Integer>();
		try {
			if (banFetchQuery == null) {
				banFetchQuery = dbCon.getConnection().prepareStatement("SELECT `user` FROM `" + BAN_TABLE_NAME + "` WHERE `channel` = ?");
			}
			banFetchQuery.setInt(1, channelID);
			banFetchQuery.execute();
			ResultSet res = banFetchQuery.getResultSet();
			while (res.next()) {
				bans.add(res.getInt(1));
			}
			res.last();
		} catch (SQLException ex) {
			logger.error("Failed to fetch bans for channel " + channelID, ex);
		}
		return bans;
	}

	@Override
	public Map<Integer, Byte> getChannelRanks(int channelID) {
		Map<Integer, Byte> ranks = new HashMap<Integer, Byte>();
		try {
			if (rankFetchQuery == null) {
				rankFetchQuery = dbCon.getConnection().prepareStatement(
						"SELECT `user`, `rank` FROM `" + RANK_TABLE_NAME + "` WHERE `channel` = ?");
			}
			rankFetchQuery.setInt(1, channelID);
			rankFetchQuery.execute();
			ResultSet res = rankFetchQuery.getResultSet();
			while (res.next()) {
				ranks.put(res.getInt(1), res.getByte(2));
			}
			res.last();
		} catch (SQLException ex) {
			logger.error("Failed to fetch ranks for channel " + channelID, ex);
		}
		return ranks;
	}

	@Override
	public List<ChannelGroupData> getChannelGroups(int channelID) {
		List<ChannelGroupData> groups = new ArrayList<ChannelGroupData>();
		try {
			if (groupFetchQuery == null) {
				groupFetchQuery = dbCon.getConnection().prepareStatement("SELECT `groupID`, `groupName`, `permissions`, `groupType`, `groupIconUrl`," + " `overrides` FROM `"
				+ GROUP_TABLE_NAME + "` WHERE `channelID` = ?");
			}
			groupFetchQuery.setInt(1, channelID);
			groupFetchQuery.execute();
			ResultSet res = groupFetchQuery.getResultSet();
			while (res.next()) {
				groups.add(new ChannelGroupData(channelID, res.getInt(1), res.getString(2), res.getString(3), res.getString(4), res
						.getString(5)).overrides(res.getInt(6)));
			}
			res.last();
		} catch (SQLException ex) {
			logger.error("Failed to fetch groups for channel " + channelID, ex);
		}
		return groups;
	}

}
