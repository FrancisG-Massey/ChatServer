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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sundays.chat.api.servlet.ServletLauncher;
import com.sundays.chat.utils.ConfigurationException;

public class ConnectionManager implements AutoCloseable {
	
	private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);
	
    private final String username;
    private final String password;
    private final String connectionString;
    private Connection con = null;
    private long lastUsed = System.currentTimeMillis();
    
    protected ConnectionManager (Properties properties) throws ConfigurationException {
    	String uri = properties.getProperty("jdbc.uri");
		String username = properties.getProperty("jdbc.username");
		String password = properties.getProperty("jdbc.password");
		String conClassName = properties.getProperty("jdbc.class");
		if (conClassName == null) {
			throw new ConfigurationException("jdbc.class not specfied!");
		}
		if (uri == null) {
			throw new ConfigurationException("jdbc.uri not specfied!");
		}
		if (username == null) {
			throw new ConfigurationException("jdbc.username not specfied!");
		}
		if (password == null) {
			throw new ConfigurationException("jdbc.password not specfied!");
		}
		try {
			Class.forName(conClassName);
		} catch (ClassNotFoundException ex) {
			throw new ConfigurationException("jdbc class '"+conClassName+"' could not be found!");
		}
    	this.connectionString = uri;
    	this.username = username;
    	this.password = password;
        connect();
        ServletLauncher.getInstance().getTaskScheduler().scheduleStandardTask(new Runnable () {
			@Override
			public void run() {
				if (con != null) {
					if (lastUsed < System.currentTimeMillis()+(5*60*1000) && con != null) {
			    		//If the database connection was last used more than 5 minutes ago, close it
						logger.info("Database connection closed due to 5 minutes of inactivity.");
			    		try {
							close();
						} catch (SQLException ex) {
							logger.error("Error closing database", ex);
						}
			    	}
				}
			}        	
        }, 6, 5, TimeUnit.MINUTES, true);
    }
    
    private void connect () {
    	try {
            con = DriverManager.getConnection(connectionString, username, password);
            logger.info("Successfully connected to the chat database.");
        } catch (SQLException ex) {
        	logger.error("Problem connecting to database", ex);
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
