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
package com.sundays.chat.io;

import java.util.Map;

/**
 * 
 * @author Francis
 */
public interface ChannelIndex {
	
	public static enum SearchType {
		ALL, CONTAINS;
	}

	/**
	 * Fetches the ID of the channel with the specified name
	 * @param name The channel name
	 * @return the ID of the channel, or -1 if the channel does not exist
	 */
	public int lookupByName (String name);
	
	/**
	 * Fetches a lookup map of channel names and IDs based on the provided search term and type 
	 * @param term The search term. This can be an empty string or null if SearchTerm.ALL is used
	 * @param type The type of search to perform
	 * @param limit The maximum number of results to return
	 * @return a map containing the names linked to ids of matching channels
	 */
	public Map<String, Integer> search (String term, SearchType type, int limit);
	
	public boolean channelExists (int id);
}
