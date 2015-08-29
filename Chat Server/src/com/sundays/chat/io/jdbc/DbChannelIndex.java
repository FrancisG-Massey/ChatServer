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

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.apache.log4j.Logger;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.sundays.chat.io.ChannelIndex;

/**
 * 
 * @author Francis
 */
public class DbChannelIndex implements ChannelIndex {
	
	private static final Logger logger = Logger.getLogger(DbChannelIndex.class);
	
	private final Cache<String, Integer> lookupCache = CacheBuilder.newBuilder().maximumSize(1000).build();
	private final Cache<Integer, Boolean> existsCache = CacheBuilder.newBuilder().maximumSize(1000).build();
	private final ConnectionManager dbCon;
	
	private PreparedStatement idResolveQuery;	
	private PreparedStatement checkExistanceQuery;
	
	
	public DbChannelIndex (ConnectionManager dbCon) {
		this.dbCon = dbCon;
	}
	
	private int findName (String name) throws SQLException {
		if (idResolveQuery == null) {
			idResolveQuery = dbCon.getConnection().prepareStatement("SELECT `channelID` FROM channels WHERE `channelName` = ?");
		}
		idResolveQuery.setString(1, name);
		if (!idResolveQuery.execute()) {
			return -1;
		}
        ResultSet results = idResolveQuery.getResultSet();
		return results.getInt(1);
	}

	@Override
	public int lookupByName(final String name) {
		int id;
		try {
			id = lookupCache.get(name, new Callable<Integer>() {
				@Override
				public Integer call() throws Exception {
					return findName(name);
				}			
			});
		} catch (ExecutionException e) {
			logger.error("Failed to resolve name for channel "+name, e);
			return -1;
		}
		return id;
	}

	@Override
	public boolean channelExists(final int id) {
		boolean exists;
		try {
			exists = existsCache.get(id, new Callable<Boolean>() {
				@Override
				public Boolean call() throws Exception {
					if (checkExistanceQuery == null) {
						checkExistanceQuery = dbCon.getConnection().prepareStatement("SELECT COUNT(*) FROM channels WHERE `channelID` = ?");
					}
					checkExistanceQuery.setInt(1, id);
					if (!checkExistanceQuery.execute()) {
						throw new SQLException();
					}
					ResultSet results = checkExistanceQuery.getResultSet();
					return results.getInt(1) > 0;
				}			
			});
		} catch (ExecutionException e) {
			logger.error("Failed to check if channel "+id+" exists.", e);
			return false;
		}
		return exists;
	}

	@Override
	public Map<String, Integer> search(String term, SearchType type, int limit) {
		Map<String, Integer> results = new HashMap<String, Integer>();
		return results;
	}

	@Override
	public void close() throws Exception {
		// TODO Auto-generated method stub
	}

	@Override
	public void commitChanges() throws IOException {
		// TODO Auto-generated method stub
		
	}

}
