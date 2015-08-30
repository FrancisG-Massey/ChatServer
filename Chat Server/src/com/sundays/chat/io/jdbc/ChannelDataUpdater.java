/*******************************************************************************
 * Copyright (c) 2015 Francis G.
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

import java.io.EOFException;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.rowset.serial.SerialBlob;

import org.apache.log4j.Logger;

import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;
import com.sundays.chat.io.ChannelDetails;
import com.sundays.chat.server.Permission;
import com.sundays.chat.server.Settings;
import com.sundays.chat.utils.ByteArrayBuilder;
import com.sundays.chat.utils.ByteArrayExtractor;

public class ChannelDataUpdater {
	
	private static final Logger logger = Logger.getLogger(ChannelDataUpdater.class);
	
	public static final String CHANNEL_TABLE_NAME = JDBCChannelManager.DETAIL_TABLE_NAME;

	public static Integer[] decompressPermissions (byte[] permissions) {
		Integer[] permissionArray = new Integer[Permission.values().length];
		try {
			ByteArrayExtractor extractor = new ByteArrayExtractor(permissions);
			extractor.getByte();//Reads the number of permissions (irrelevant)
			for (int i = 0; i < permissionArray.length; i++) {
                int permission;
                try {
                	permission = extractor.getByte();
                } catch (EOFException e) {
                	//Permission data stream ended prematurely. A later loop should handle this problem
                	break;
                }
                permissionArray[i] = permission;
            }
		} catch (IOException ex) {
			logger.warn("Could not decode permissions for this channel. Using defaults.", ex);
		}
		return permissionArray;		
	}
	
	public static Map<Integer, String> decompressRankNamesV1 (byte[] rankNames) {
		Map<Integer, String> nameArray = new HashMap<Integer, String>();
		try {
			ByteArrayExtractor extractor = new ByteArrayExtractor(rankNames);//Converts the names list into a byte array
            if (extractor.getByte() != 12) {//The first value in v1 compressions was the number of ranks. This used to be fixed at 12.
            	throw new IllegalArgumentException("Invalid rank names data submitted to ChannelDataUpdate.decompressRankNames()");
            }
            int i=0;
            while (true) {
            	String name;
                try {
                    name = extractor.getUTFString();
                } catch (EOFException e) {
                	//Permission data stream ended prematurely. A later loop should handle this problem
                    break;
                }
                nameArray.put(i, name);
                i++;
            }
        } catch (IOException ex) {
        	logger.warn("Could not decode rank names for this channel. Using defaults.", ex);
        }
		return nameArray;
	}
	
	public static Map<Integer, String> decompressRankNamesV2 (byte[] rankNames) {
		Map<Integer, String> nameArray = new HashMap<Integer, String>();
		try {
			ByteArrayExtractor extractor = new ByteArrayExtractor(rankNames);//Converts the names list into a byte array
            if (extractor.getShort() != 2) {//Reads the rank name archive version
            	//If the version is not 2, try version 1
            	return decompressRankNamesV1(rankNames);
            }
            while (true) {
                String name;
                byte rank;
            	try {
            		rank = extractor.getByte();
                    name = extractor.getUTFString();
                } catch (EOFException e) {
                	//Permission data stream ended prematurely. A later loop should handle this problem
                    break;
                }
            	nameArray.put((int) rank, name);
            }
        } catch (IOException ex) {
        	logger.warn("Could not decode rank names for this channel. Using defaults.", ex);
        }
		return nameArray;
	}
	
	private final ConcurrentHashMap<Integer, ChannelDetails> detailChanges = new ConcurrentHashMap<Integer, ChannelDetails>();
	
	protected void syncDetails (int channelID, ChannelDetails details) {
		synchronized (detailChanges) {
			if (detailChanges.containsKey(channelID)) {
				detailChanges.remove(channelID);
			}
			detailChanges.put(channelID, details);
		}
	}
	
	private byte[] compressPermissions (Integer[] permissions) throws IOException {
		ByteArrayBuilder bldr = new ByteArrayBuilder();
		bldr.writeByte((byte) permissions.length);//Number of permissions
		for (Integer p : permissions) {
        	bldr.writeByte(p.byteValue());//Loops through the permissions, writing each one-by-one
        }
		return bldr.getByteArray();
	}
	
	private byte[] compressRankNamesV2 (Map<Integer, String> rankNames) throws IOException {
		ByteArrayBuilder bldr = new ByteArrayBuilder();
		bldr.writeShort(Settings.RANK_NAME_VERSION);//Rank names version
		for (Entry<Integer, String> name : rankNames.entrySet()) {
			bldr.writeByte(name.getKey().byteValue());
        	bldr.writeUTFString(name.getValue());
        }
		return bldr.getByteArray();
	}
	
	private PreparedStatement channelUpdateQuery;
	
	protected void commitPendingChanges (ConnectionManager dbCon) {
		if (detailChanges.size() > 0) {
			try {
				if (channelUpdateQuery == null) {
					channelUpdateQuery = dbCon.getConnection().prepareStatement("UPDATE `"+CHANNEL_TABLE_NAME+"` SET" +
							" `name` = ?, `abbrieviation` = ?, `permissions` = ?, `rankNames` = ?," +
							" `openingMessage` = ?, `trackMessages` = ?, " +
							" `owner` = ? WHERE `id` = ?");
				}
				HashMap<Integer, ChannelDetails> detailChangesCopy = new HashMap<Integer, ChannelDetails>();
				synchronized (detailChanges) {
					detailChangesCopy.putAll(detailChanges);
					detailChanges.clear();
				}
				for (Entry<Integer, ChannelDetails> update : detailChangesCopy.entrySet()) {
					ChannelDetails details = update.getValue();
					channelUpdateQuery.setString(1, details.name);
					channelUpdateQuery.setString(2, details.abbreviation);					
					channelUpdateQuery.setBlob(3, new SerialBlob(compressPermissions(details.permissions)));
					channelUpdateQuery.setBlob(4, new SerialBlob(compressRankNamesV2(details.rankNames)));
					channelUpdateQuery.setString(5, details.openingMessage);
					channelUpdateQuery.setBoolean(6, details.trackMessages);
					channelUpdateQuery.setInt(7, details.owner);
					channelUpdateQuery.setInt(8, details.id);
					try {
						channelUpdateQuery.execute();
		            } catch (MySQLIntegrityConstraintViolationException ex) {
		            	logger.warn("Failed to commit channel detail update request: "+details, ex);
		            	continue;
		            }
					logger.info("Channel data updated in database: "+details);
				}
			} catch (SQLException | IOException ex) {
				logger.error("Failed to commit channel update requests", ex);
			}
		}
	}
}
