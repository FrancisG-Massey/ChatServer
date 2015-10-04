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
package com.sundays.chat.server.user;

/**
 * An object which resolves usernames from user IDs and vice versa.
 * 
 * <p>This interface should be used in all places where only the methods listed are required. 
 * In other cases, the full user manager class should be used.</p>
 * 
 * @author Francis
 */
public interface UserLookup {
	
	public User getUser (int userID);
	
	public String getUsername (int userID);
	
	public int getUserID (String username);

}
