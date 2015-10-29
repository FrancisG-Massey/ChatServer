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
package com.sundays.chat.io.jdbc;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.sundays.chat.io.UserDataIO;
import com.sundays.chat.io.UserDetails;

public class JDBCUserSave implements UserDataIO {
	
	private Connection dbCon;
	private PreparedStatement userCreation;
	private PreparedStatement userLookup;
	private PreparedStatement usernameLookup;
	private PreparedStatement userDataSave;
	
	public JDBCUserSave (Connection dbCon) {
		if (dbCon == null) {
			throw new NullPointerException();
		}
		this.dbCon = dbCon;
	}

	@Override
	public int createUser(String username, char[] hashedPassword) throws IOException {		
		try {
			if (userCreation == null) {
				userCreation = dbCon.prepareStatement("INSERT INTO `users` SET `username` = ?, `password` = ?, `registered_on` = UTC_TIMESTAMP()", Statement.RETURN_GENERATED_KEYS);
			}
			userCreation.setString(1, username);
			userCreation.setString(2, String.copyValueOf(hashedPassword));
			userCreation.execute();
			if (userCreation.getUpdateCount() == 0) {
				throw new IOException("Failed to create new user: "+username);
			}
			ResultSet res = userCreation.getGeneratedKeys();
			return res.getInt(1);//Gets the user id
		} catch (SQLException ex) {
			//Since SQLException is not a subclass of IOException, it must be rethrown as an IOException
			throw new IOException(ex);
		}
	}

	@Override
	public void saveUserData(UserDetails user) throws IOException {
    	try {
    		if (userDataSave == null) {
    			userDataSave = dbCon.prepareStatement("UPDATE `users` SET `username` = ?, `alias` = ?, `defaultChannel` = ? WHERE `userID` = ?");
			}
    		userDataSave.setString(1, user.getUsername());
    		userDataSave.setString(2, user.getAlias());
    		userDataSave.setInt(3, user.getDefaultChannel());
    		userDataSave.setInt(4, user.getUserID());
    		userDataSave.execute();
		} catch (SQLException ex) {
			//Since SQLException is not a subclass of IOException, it must be rethrown as an IOException
			throw new IOException(ex);
		}
	}

	@Override
	public int lookupByUsername(String username) throws IOException {
		int userID = -1;
		try {
			if (usernameLookup == null) {
				usernameLookup = dbCon.prepareStatement("SELECT `userID` FROM `users` WHERE `username` = ?");
			}
			usernameLookup.setString(1, username);
			if (usernameLookup.execute()) {
		        ResultSet res = usernameLookup.getResultSet();
		        while (res.next()) {
		            userID = res.getInt(1);
		        }
			}
		} catch (SQLException ex) {
			//Since SQLException is not a subclass of IOException, it must be rethrown as an IOException
			throw new IOException(ex);
		}
		return userID;
	}

	@Override
	public UserDetails getUserDetails(int id) throws IOException {
		UserDetails details = null;
        try {
        	if (userLookup == null) {
        		userLookup = dbCon.prepareStatement("SELECT `userID`, `username`, `password`, `alias`, `defaultChannel` FROM users WHERE `userID` = ?");
			}
        	userLookup.setInt(1, id);
        	if (userLookup.execute()) {
        		ResultSet res = userLookup.getResultSet();
	            while (res.next()) {
	                int thisId = res.getInt(1);
	                if (thisId == id) {
	                	details = new UserDetails();
	                	details.setUserID(id);
	                	details.setUsername(res.getString(2));
	                	details.setHashedPassword(res.getString(3).toCharArray());
	                	details.setAlias(res.getString(4));
	                	details.setDefaultChannel(res.getInt(5));
	                	break;
	                }
	            }
        	}
        } catch (SQLException ex) {
        	//Since SQLException is not a subclass of IOException, it must be rethrown as an IOException
			throw new IOException(ex);
        }
		return details;
	}

	@Override
	public boolean userExists(int id) throws IOException {
		// TODO Auto-generated method stub
		return false;
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
