/*******************************************************************************
 * Copyright (c) 2013, 2016 Francis G.
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.sundays.chat.io.ChannelDetails;
import com.sundays.chat.io.ChannelIndex;

/**
 * 
 * @author Francis
 */
public class JDBCChannelIndex implements ChannelIndex {	
	private final Cache<Integer, Optional<ChannelDetails>> idLookup = CacheBuilder.newBuilder().maximumSize(1000).build();
	private final Cache<UUID, Optional<ChannelDetails>> uuidLookup = CacheBuilder.newBuilder().maximumSize(1000).build();
	private final Cache<String, Optional<ChannelDetails>> nameLookup = CacheBuilder.newBuilder().maximumSize(1000).build();
	private final ConnectionManager dbCon;
	
	private PreparedStatement idLookupQuery;
	private PreparedStatement nameLookupQuery;
	private PreparedStatement uuidLookupQuery;
	
	
	public JDBCChannelIndex (ConnectionManager dbCon) {
		this.dbCon = dbCon;
	}
	
	private Optional<ChannelDetails> lookup (int id) throws SQLException {
		if (idLookupQuery == null) {
			idLookupQuery = dbCon.getConnection().prepareStatement("SELECT `id`, `uuid`, `name`, `abbrieviation` FROM channels WHERE `id` = ?");
		}
		idLookupQuery.setInt(1, id);
		if (!idLookupQuery.execute()) {
			return Optional.empty();
		}
		ResultSet results = idLookupQuery.getResultSet();
        if (!results.next()) {
        	return Optional.empty();
        }
        return Optional.of(getDetails(results));
	}
	
	private Optional<ChannelDetails> lookup (String name) throws SQLException {
		if (nameLookupQuery == null) {
			nameLookupQuery = dbCon.getConnection().prepareStatement("SELECT `id`, `uuid`, `name`, `abbrieviation` FROM channels WHERE `name` = ?");
		}
		nameLookupQuery.setString(1, name);
		if (!nameLookupQuery.execute()) {
			return Optional.empty();
		}
        ResultSet results = nameLookupQuery.getResultSet();
        if (!results.next()) {
        	return Optional.empty();
        }
		return Optional.of(getDetails(results));
	}
	
	private Optional<ChannelDetails> lookup (UUID uuid) throws SQLException {
		if (uuidLookupQuery == null) {
			uuidLookupQuery = dbCon.getConnection().prepareStatement("SELECT `id`, `uuid`, `name`, `abbrieviation` FROM channels WHERE `uuid` = ?");
		}
		uuidLookupQuery.setString(1, uuid.toString());
		if (!uuidLookupQuery.execute()) {
			return Optional.empty();
		}
		ResultSet results = uuidLookupQuery.getResultSet();
        if (!results.next()) {
        	return Optional.empty();
        }
        return Optional.of(getDetails(results));
	}
	
	private ChannelDetails getDetails (ResultSet results) throws SQLException {
		ChannelDetails details = new ChannelDetails();
        details.setId(results.getInt("id"));
        details.setUuid(UUID.fromString(results.getString("uuid")));
        details.setName(results.getString("name"));
        details.setAlias(results.getString("abbrieviation"));
        return details;
	}

	@Override
	public Optional<ChannelDetails> lookupById(int id) throws IOException {
		try {
			return idLookup.get(id, () -> lookup(id));
		} catch (ExecutionException e) {
			throw new IOException(e);
		}
	}

	@Override
	public Optional<ChannelDetails> lookupByUuid(UUID uuid) throws IOException {
		try {
			return uuidLookup.get(uuid, () -> lookup(uuid));
		} catch (ExecutionException e) {
			throw new IOException(e);
		}
	}

	@Override
	public Optional<ChannelDetails> lookupByName(String name) throws IOException {
		try {
			return nameLookup.get(name, () -> lookup(name));
		} catch (ExecutionException e) {
			throw new IOException(e);
		}
	}

	@Override
	public Collection<ChannelDetails> search(String term, SearchType type, int limit) {
		Collection<ChannelDetails> results = new ArrayList<ChannelDetails>();
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
