/*******************************************************************************
 * Copyright (c) 2013 Francis G.
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
 ******************************************************************************/
package com.sundays.chat.io.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sundays.chat.server.ChatServer;

public class ConnectionManager implements AutoCloseable {
	
	private static final Logger logger = Logger.getLogger(ConnectionManager.class);
	
    private final String username;
    private final String password;
    private final String connectionString;    
    private Connection con = null;
    private long lastUsed = System.currentTimeMillis();
    
    public ConnectionManager (String url, String username, String password) {
    	this.connectionString = url;
    	this.username = username;
    	this.password = password;
        connect();
        ChatServer.getInstance().serverTaskScheduler().scheduleStandardTask(new Runnable () {
			@Override
			public void run() {
				if (con != null) {
					if (lastUsed < System.currentTimeMillis()+(5*60*1000)) {
			    		//If the database connection was last used more than 5 minutes ago, close it
						logger.info("Database connection closed due to 5 minutes of inactivity.");
			    		try {
							close();
						} catch (SQLException e) {
							logger.error(e);
						}
			    	}
				}
			}        	
        }, 6, 5, TimeUnit.MINUTES, true);
    }
    
    private void connect () {
    	try {
        	Class.forName("com.mysql.jdbc.Driver");
            con = DriverManager.getConnection(connectionString, username, password);
            logger.info("Successfully connected to the chat database.");
        } catch (SQLException | ClassNotFoundException e) {
        	logger.error(e);
        }        
    }
    
    public void close () throws SQLException {
    	if (con != null) {
    		con.close();
            logger.info("Closed database connection.");
            con = null;
    	}             
    }
    
    public Connection getConnection () {
    	if (con == null) {
    		connect();
    	}
    	lastUsed = System.currentTimeMillis();
        return this.con;
    }
}
