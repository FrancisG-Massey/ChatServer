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

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.sundays.chat.io.ChannelDataIO;
import com.sundays.chat.io.ChannelDetails;
import com.sundays.chat.io.ChannelGroupData;
import com.sundays.chat.io.ChannelGroupType;
import com.sundays.chat.server.Settings;

public class JDBCChannelSave implements ChannelDataIO {

	private static final Logger logger = Logger.getLogger(JDBCChannelSave.class);

	public static final String MEMBER_TABLE_NAME = "channelRanks";
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

	public JDBCChannelSave(ConnectionManager dbCon) {
		this.dbCon = dbCon;
	}

	@Override
	public void addMember(int channelID, int userID, int group) {
		rankBanUpdater.addRank(channelID, userID);
	}

	@Override
	public void updateMember(int channelID, int userID, int group) {
		rankBanUpdater.changeRank(channelID, userID, group);
	}

	@Override
	public void removeMember(int channelID, int userID) {
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
	public void updateDetails(int channelID, ChannelDetails details) {
		channelUpdater.syncDetails(channelID, details);
	}

	@Override
	public void commitChanges() {
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
						"SELECT `id`, `name`, `abbrieviation`, "
								+ "`permissions`, `rankNames`, `openingMessage`, `trackMessages`," + "`owner`" + " FROM `"
								+ DETAIL_TABLE_NAME + "` WHERE `id` = ?");
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
	public Map<Integer, Integer> getChannelMembers(int channelID) {
		Map<Integer, Integer> ranks = new HashMap<>();
		try {
			if (rankFetchQuery == null) {
				rankFetchQuery = dbCon.getConnection().prepareStatement(
						"SELECT `user`, `rank` FROM `" + MEMBER_TABLE_NAME + "` WHERE `channel` = ?");
			}
			rankFetchQuery.setInt(1, channelID);
			rankFetchQuery.execute();
			ResultSet res = rankFetchQuery.getResultSet();
			while (res.next()) {
				ranks.put(res.getInt(1), res.getInt(2));
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
				groupFetchQuery = dbCon.getConnection().prepareStatement("SELECT `id`, `name`, `permissions`, `type`, `icon`," + " `overrides` FROM `"
				+ GROUP_TABLE_NAME + "` WHERE `channelID` = ?");
			}
			groupFetchQuery.setInt(1, channelID);
			groupFetchQuery.execute();
			ResultSet res = groupFetchQuery.getResultSet();
			while (res.next()) {
				ChannelGroupType groupType = ChannelGroupType.getByName(res.getString(4));
				groups.add(new ChannelGroupData(channelID, res.getInt(1), res.getString(2), res.getString(3).split(","), groupType, res
						.getString(5)));
			}
			res.last();
		} catch (SQLException ex) {
			logger.error("Failed to fetch groups for channel " + channelID, ex);
		}
		return groups;
	}

	@Override
	public void close() throws Exception {
		commitChanges();		
	}

	@Override
	public int createChannel(ChannelDetails details) throws IOException {
		Connection con = dbCon.getConnection();
		try {
			con.setAutoCommit(false);//Either all queries will succeed or none will.
			
			PreparedStatement statement = con.prepareStatement("INSERT INTO `"+DETAIL_TABLE_NAME+"` (`name`,`owner`) VALUES ?, ?", Statement.RETURN_GENERATED_KEYS);
			statement.setString(1, details.getName());
			statement.setInt(2, details.getOwner());
			statement.execute();//Create the details
			
			if (statement.getUpdateCount() == 0) {
				con.rollback();
				throw new IOException("Failed to insert channel data into database.");
			}
			ResultSet res = statement.getGeneratedKeys();
			
			int channelID = res.getInt(1);
			
			statement = con.prepareStatement("INSERT INTO `"+MEMBER_TABLE_NAME+"` SET `channel` = ?, `user` = ?, `rank` = ?");
			statement.setInt(1, channelID);
			statement.setInt(2, details.getOwner());
			statement.setInt(3, Settings.OWNER_RANK);
			statement.execute();//Add the owner to the member list
			
			con.commit();
			return channelID;
		} catch (SQLException ex) {
			throw new IOException("Failed to remove channel from database", ex);
		} finally {
			try {
				con.setAutoCommit(true);
			} catch (SQLException ex) {
				throw new IOException("Failed to remove channel from database", ex);
			}			
		}
	}

	@Override
	public void removeChannel(int channelID) throws IOException {
		Connection con = dbCon.getConnection();
		try {
			con.setAutoCommit(false);//Either all queries will succeed or none will.
			
			PreparedStatement statement = con.prepareStatement("DELETE FROM `"+DETAIL_TABLE_NAME+"` WHERE `id` = ? LIMIT 1");
			statement.setInt(1, channelID);
			statement.execute();//Remove the details
			
			statement = con.prepareStatement("DELETE FROM `"+MEMBER_TABLE_NAME+"` WHERE `channel` = ? LIMIT 1");
			statement.setInt(1, channelID);
			statement.execute();//Remove the members
			
			statement = con.prepareStatement("DELETE FROM `"+BAN_TABLE_NAME+"` WHERE `channel` = ? LIMIT 1");
			statement.setInt(1, channelID);
			statement.execute();//Remove the bans
			
			statement = con.prepareStatement("DELETE FROM `"+GROUP_TABLE_NAME+"` WHERE `channel` = ? LIMIT 1");
			statement.setInt(1, channelID);
			statement.execute();//Remove the groups
			
			con.commit();
		} catch (SQLException ex) {
			throw new IOException("Failed to remove channel from database", ex);
		} finally {
			try {
				con.setAutoCommit(true);
			} catch (SQLException ex) {
				throw new IOException("Failed to remove channel from database", ex);
			}			
		}
	}

}
