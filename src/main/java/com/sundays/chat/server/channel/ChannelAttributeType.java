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
package com.sundays.chat.server.channel;

public enum ChannelAttributeType {
	/**
	 * An attribute representing information about the channel.
	 * Changing these attributes should not have any impact on the functionality of the channel.
	 */
	INFO,
	
	/**
	 * An attribute used to change the functionality of the channel.
	 */
	SETTING,
	
	/**
	 * An attribute used by the system to track the state of some channel aspect.
	 * These attributes should not be changed by any channel user; only by the application itself.
	 */
	SYSTEM;
}
