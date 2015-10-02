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

public class ChannelDbFieldMatcher {

	public final int channelID;
	public final String fieldName;
	
	public ChannelDbFieldMatcher (int channelID, String fieldName) {
		this.channelID = channelID;
		this.fieldName = fieldName;
	}
	
	@Override
	public boolean equals (Object m) {
		if (m.getClass().equals(ChannelDbFieldMatcher.class)) {
			ChannelDbFieldMatcher compare = (ChannelDbFieldMatcher) m;
			return (compare.channelID == this.channelID && compare.fieldName.equalsIgnoreCase(this.fieldName));			
		} else {
			return false;
		}	
	}
	
	public static ChannelDbFieldMatcher[] getChannelDbFields (int channelID) {
		ChannelDbFieldMatcher[] r = {new ChannelDbFieldMatcher(channelID, "channelName"),
			new ChannelDbFieldMatcher(channelID, "channelAbbr"),
			new ChannelDbFieldMatcher(channelID, "permissions")};
		return r;
	}
}
