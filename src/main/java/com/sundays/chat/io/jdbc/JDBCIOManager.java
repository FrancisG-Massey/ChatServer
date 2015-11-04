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

import java.util.Properties;

import com.sundays.chat.io.ChannelDataIO;
import com.sundays.chat.io.ChannelIndex;
import com.sundays.chat.io.IOManager;
import com.sundays.chat.io.UserDataIO;
import com.sundays.chat.utils.ConfigurationException;

public class JDBCIOManager implements IOManager {
	
	private UserDataIO userManager;
	private ChannelIndex channelIndex;
	private ChannelDataIO channelManager;
	private ConnectionManager dbcon;
	
	public JDBCIOManager () {
		super();
	}

	@Override
	public void init(Properties properties) throws ConfigurationException {		
		this.dbcon = new ConnectionManager(properties);
		this.userManager = new JDBCUserSave(dbcon.getConnection());
		this.channelIndex = new JDBCChannelIndex(dbcon);
		this.channelManager = new JDBCChannelSave(dbcon, properties);
	}

	@Override
	public UserDataIO getUserIO() {
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
	public ChannelDataIO getChannelIO() {
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
