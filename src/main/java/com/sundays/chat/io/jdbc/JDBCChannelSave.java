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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sundays.chat.io.ChannelDataIO;
import com.sundays.chat.io.ChannelDetails;
import com.sundays.chat.io.ChannelGroupData;
import com.sundays.chat.io.ChannelGroupType;
import com.sundays.chat.server.channel.ChannelGroup;

public class JDBCChannelSave implements ChannelDataIO {

	private static final Logger logger = LoggerFactory.getLogger(JDBCChannelSave.class);

	private final String attributeTableName;
	private final String detailTableName;
	private final String memberTableName;
	private final String banTableName;
	private final String groupTableName;

	private final MemberDataUpdater memberUpdater;
	private final ChannelDataUpdater channelUpdater;
	private final ConnectionManager dbCon;

	private PreparedStatement detailFetchQuery;
	private PreparedStatement attrFetchQuery;
	private PreparedStatement memberFetchQuery;
	private PreparedStatement banFetchQuery;
	private PreparedStatement groupFetchQuery;

	protected JDBCChannelSave(ConnectionManager dbCon, Properties properties) {
		this.dbCon = dbCon;
		
		this.detailTableName = properties.getProperty("jdbc.table.detail", "channels");
		this.attributeTableName = properties.getProperty("jdbc.table.attr", "channelAttributes");
		this.memberTableName = properties.getProperty("jdbc.table.member", "channelMembers");
		this.banTableName = properties.getProperty("jdbc.table.ban", "channelBans");
		this.groupTableName = properties.getProperty("jdbc.table.group", "channelGroups");
		
		this.channelUpdater = new ChannelDataUpdater(detailTableName, attributeTableName);
		this.memberUpdater = new MemberDataUpdater(memberTableName, banTableName, groupTableName);
	}

	@Override
	public void addAttribute(int channelID, String key, String value) throws IOException {
		channelUpdater.addAttribute(channelID, key, value);
	}

	@Override
	public void updateAttribute(int channelID, String key, String value) throws IOException {
		channelUpdater.updateAttribute(channelID, key, value);
	}

	@Override
	public void clearAttribute(int channelID, String key) throws IOException {
		channelUpdater.removeAttribute(channelID, key);
	}

	@Override
	public void addMember(int channel, int user, int group) {
		memberUpdater.addMember(channel, user, group);
	}

	@Override
	public void updateMember(int channel, int user, int group) {
		memberUpdater.updateMember(channel, user, group);
	}

	@Override
	public void removeMember(int channel, int user) {
		memberUpdater.removeMember(channel, user);
	}

	@Override
	public void addBan(int channelID, int userID) {
		memberUpdater.addBan(channelID, userID);
	}

	@Override
	public void removeBan(int channelID, int userID) {
		memberUpdater.removeBan(channelID, userID);
	}

	@Override
	public void addGroup(int channelID, ChannelGroupData group) {
		// TODO Auto-generated method stub
	}

	@Override
	public void updateGroup(int channelID, ChannelGroupData group) {
		// TODO Auto-generated method stub
	}

	@Override
	public void removeGroup(int channelID, int groupID) {
		// TODO Auto-generated method stub
	}

	@Override
	public void updateDetails(int channelID, ChannelDetails details) {
		channelUpdater.syncDetails(channelID, details);
	}

	@Override
	public void commitChanges() {
		// Commits all pending updates to the database back-end
		memberUpdater.commitPendingChanges(dbCon);// Commits the rank and ban changes
		channelUpdater.commitPendingChanges(dbCon);// Commits pending channel details changes
	}

	@Override
	public ChannelDetails getChannelDetails(int channelID) {
		ChannelDetails details = null;
		try {
			if (detailFetchQuery == null) {
				detailFetchQuery = dbCon.getConnection().prepareStatement(
						"SELECT `id`, `name`, `abbrieviation`, "
								+ "`permissions`, `rankNames`, `trackMessages`," + "`owner`" + " FROM `"
								+ detailTableName + "` WHERE `id` = ?");
			}
			detailFetchQuery.setInt(1, channelID);
			detailFetchQuery.execute();
			ResultSet res = detailFetchQuery.getResultSet();
			while (res.next()) {
				details = new ChannelDetails(res.getInt(1),// Channel ID
						res.getString(2),// Channel Name
						res.getString(3),// Channel Abbreviation
						res.getBoolean(6),// Whether or not to track messages
						res.getInt(7));// Channel owner userID
			}
		} catch (SQLException ex) {
			logger.error("Failed to fetch details for channel " + channelID, ex);
		}
		return details;
	}

	@Override
	public Map<String, String> getChannelAttributes(int channelID) throws IOException {
		Map<String, String> attributes = new HashMap<>();
		try {
			if (attrFetchQuery == null) {
				attrFetchQuery = dbCon.getConnection().prepareStatement(
						"SELECT `key`, `value` FROM `" + attributeTableName + "` WHERE `channel` = ?");
			}
			attrFetchQuery.setInt(1, channelID);
			attrFetchQuery.execute();
			ResultSet res = attrFetchQuery.getResultSet();
			while (res.next()) {
				attributes.put(res.getString(1), res.getString(2));
			}
			res.last();
		} catch (SQLException ex) {
			logger.error("Failed to fetch attributes for channel " + channelID, ex);
		}
		return attributes;
	}

	@Override
	public Set<Integer> getChannelBans(int channelID) {
		Set<Integer> bans = new HashSet<Integer>();
		try {
			if (banFetchQuery == null) {
				banFetchQuery = dbCon.getConnection().prepareStatement("SELECT `user` FROM `" + banTableName + "` WHERE `channel` = ?");
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
			if (memberFetchQuery == null) {
				memberFetchQuery = dbCon.getConnection().prepareStatement(
						"SELECT `user`, `group` FROM `" + memberTableName + "` WHERE `channel` = ?");
			}
			memberFetchQuery.setInt(1, channelID);
			memberFetchQuery.execute();
			ResultSet res = memberFetchQuery.getResultSet();
			while (res.next()) {
				ranks.put(res.getInt(1), res.getInt(2));
			}
			res.last();
		} catch (SQLException ex) {
			logger.error("Failed to fetch members for channel " + channelID, ex);
		}
		return ranks;
	}

	@Override
	public Set<ChannelGroupData> getChannelGroups(int channelID) {
		Set<ChannelGroupData> groups = new HashSet<ChannelGroupData>();
		try {
			if (groupFetchQuery == null) {
				groupFetchQuery = dbCon.getConnection().prepareStatement("SELECT `id`, `name`, `permissions`, `type`, `icon`," + " `overrides` FROM `"
				+ groupTableName + "` WHERE `channelID` = ?");
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
			
			PreparedStatement statement = con.prepareStatement("INSERT INTO `"+detailTableName+"` (`name`,`owner`) VALUES ?, ?", Statement.RETURN_GENERATED_KEYS);
			statement.setString(1, details.getName());
			statement.setInt(2, details.getOwner());
			statement.execute();//Create the details
			
			if (statement.getUpdateCount() == 0) {
				con.rollback();
				throw new IOException("Failed to insert channel data into database.");
			}
			ResultSet res = statement.getGeneratedKeys();
			
			int channelID = res.getInt(1);
			
			statement = con.prepareStatement("INSERT INTO `"+memberTableName+"` SET `channel` = ?, `user` = ?, `rank` = ?");
			statement.setInt(1, channelID);
			statement.setInt(2, details.getOwner());
			statement.setInt(3, ChannelGroup.OWNER_GROUP);
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
			
			PreparedStatement statement = con.prepareStatement("DELETE FROM `"+detailTableName+"` WHERE `id` = ? LIMIT 1");
			statement.setInt(1, channelID);
			statement.execute();//Remove the details
			
			statement = con.prepareStatement("DELETE FROM `"+memberTableName+"` WHERE `channel` = ? LIMIT 1");
			statement.setInt(1, channelID);
			statement.execute();//Remove the members
			
			statement = con.prepareStatement("DELETE FROM `"+banTableName+"` WHERE `channel` = ? LIMIT 1");
			statement.setInt(1, channelID);
			statement.execute();//Remove the bans
			
			statement = con.prepareStatement("DELETE FROM `"+groupTableName+"` WHERE `channel` = ? LIMIT 1");
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
