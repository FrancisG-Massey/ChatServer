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

import java.util.Properties;

import com.sundays.chat.io.ChannelDataManager;
import com.sundays.chat.io.ChannelIndex;
import com.sundays.chat.io.IOManager;
import com.sundays.chat.io.UserDataManager;
import com.sundays.chat.utils.ConfigurationException;

public class JDBCIOManager implements IOManager {
	
	private UserDataManager userManager;
	private ChannelIndex channelIndex;
	private ChannelDataManager channelManager;
	private ConnectionManager dbcon;
	
	public JDBCIOManager () {
		
	}

	@Override
	public void init(Properties properties) throws ConfigurationException {
		String uri = properties.getProperty("jdbc.uri");
		String username = properties.getProperty("jdbc.username");
		String password = properties.getProperty("jdbc.password");
		if (uri == null) {
			throw new ConfigurationException("jdbc.uri not specfied!");
		}
		if (username == null) {
			throw new ConfigurationException("jdbc.username not specfied!");
		}
		if (password == null) {
			throw new ConfigurationException("jdbc.password not specfied!");
		}
		this.dbcon = new ConnectionManager(uri, username, password);
		this.userManager = new JDBCUserManager(dbcon.getConnection());
		this.channelIndex = new JDBCChannelIndex(dbcon);
		this.channelManager = new JDBCChannelManager(dbcon);
	}

	@Override
	public UserDataManager getUserIO() {
		if (userManager == null) {
			throw new IllegalStateException("User IO is not initialised.");
		}
		return userManager;
	}

	@Override
	public ChannelIndex getChannelIndex() {
		if (channelIndex == null) {
			throw new IllegalStateException("Channel index is not initialised.");
		}
		return channelIndex;
	}

	@Override
	public ChannelDataManager getChannelIO() {
		if (channelManager == null) {
			throw new IllegalStateException("Channel manager is not initialised.");
		}
		return channelManager;
	}

	@Override
	public void close() throws Exception {
		if (dbcon != null) {
			dbcon.close();
		}
	}

}
