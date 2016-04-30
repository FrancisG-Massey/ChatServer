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
package com.sundays.chat.io;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * The persistence layer interface for resolving channel names and IDs.
 * 
 * The application will not use caching when calling methods in this interface, so implementations should use their own caching where appropriate.
 * 
 * @author Francis
 */
public interface ChannelIndex extends AutoCloseable {
	
	public static enum SearchType {
		ALL, CONTAINS;
	}

	/**
	 * Fetches the details for channel whos name matches the specified name.
	 * NOTE: This search is case insensitive.
	 * @param name The channel name to search for. 
	 * @return An {@link Optional} wrapper around {@link ChannelDetails}, where {@link Optional#isPresent()} returns false if no match was found.
	 */
	public Optional<ChannelDetails> lookupByName (String name) throws IOException;
	
	/**
	 * Fetches the details for channel whos uuid matches the specified uuid.
	 * @param uuid The channel uuid to search for. 
	 * @return An {@link Optional} wrapper around {@link ChannelDetails}, where {@link Optional#isPresent()} returns false if no match was found.
	 */
	public Optional<ChannelDetails> lookupByUuid (UUID uuid) throws IOException;
	
	/**
	 * Fetches the details for with the specified internal ID.
	 * @param id The internal channel id, which is used only within this application instance
	 * @return An {@link Optional} wrapper around {@link ChannelDetails}, where {@link Optional#isPresent()} returns false if no match was found.
	 */
	public Optional<ChannelDetails> lookupById (int id) throws IOException;
	
	/**
	 * Fetches a lookup map of channel names and IDs based on the provided search term and type 
	 * @param term The search term. This can be an empty string or null if SearchTerm.ALL is used
	 * @param type The type of search to perform
	 * @param limit The maximum number of results to return
	 * @return a collection containing the details of matching channels
	 */
	public Collection<ChannelDetails> search (String term, SearchType type, int limit);
	
	public void commitChanges () throws IOException;
}
