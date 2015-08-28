/*******************************************************************************
 * Copyright (c) 2015 Francis G.
 *
 * This file is part of ChatServer.
 *
 * ChatServer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
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

import java.util.Arrays;

/**
 * 
 * @author Francis
 */
public class UserDetails {
	
	private int userID;
    private String username;
    private String alias;
	public int defaultChannel = 0;
    private char[] hashedPassword;
    
	public UserDetails () {
    	
    }

	public UserDetails (int id, String username, int defaultChannel) {
		this.userID = id;
		this.username = username;
		this.defaultChannel = defaultChannel;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public int getDefaultChannel() {
		return defaultChannel;
	}

	public void setDefaultChannel(int defaultChannel) {
		this.defaultChannel = defaultChannel;
	}

	public char[] getHashedPassword() {
		return hashedPassword;
	}

	public void setHashedPassword(char[] hashedPassword) {
		this.hashedPassword = hashedPassword;
	}

	public int getUserID() {
		return userID;
	}

	public void setUserID(int userID) {
		this.userID = userID;
	}
	
	public void clearPassword () {
		Arrays.fill(hashedPassword, '\0');
	}

	@Override
	public String toString() {
		return "UserDetails [userID=" + userID + ", username=" + username
				+ ", defaultChannel=" + defaultChannel + ", hashedPassword="
				+ Arrays.toString(hashedPassword) + "]";
	}

}
