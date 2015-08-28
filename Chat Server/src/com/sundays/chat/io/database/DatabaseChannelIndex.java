package com.sundays.chat.io.database;

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
public class DatabaseChannelIndex implements ChannelIndex {
	
	private static final Logger logger = Logger.getLogger(DatabaseChannelIndex.class);
	
	private final Cache<String, Integer> lookupCache = CacheBuilder.newBuilder().maximumSize(1000).build();
	private final Cache<Integer, Boolean> existsCache = CacheBuilder.newBuilder().maximumSize(1000).build();
	private final ConnectionManager dbCon;
	
	private PreparedStatement idResolveQuery;	
	private PreparedStatement checkExistanceQuery;
	
	
	public DatabaseChannelIndex (ConnectionManager dbCon) {
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

}
