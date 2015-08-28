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
package com.sundays.chat.io.database;

import com.sundays.chat.io.ChannelDataManager;
import com.sundays.chat.io.ChannelIndex;
import com.sundays.chat.io.IOManager;
import com.sundays.chat.io.UserDataManager;

public class DatabaseIOManager implements IOManager {
	
	private UserDataManager userManager;
	private ChannelIndex channelIndex;
	private ChannelDataManager channelManager;
	private ConnectionManager dbcon;
	
	public DatabaseIOManager (String url, String username, String password) {
		this.dbcon = new ConnectionManager(url, username, password);
		this.userManager = new DatabaseUserManager(dbcon.getConnection());
		this.channelIndex = new DatabaseChannelIndex(dbcon);
		this.channelManager = new ChannelDatabaseManager(dbcon);
	}

	@Override
	public UserDataManager getUserIO() {
		return userManager;
	}

	@Override
	public ChannelIndex getChannelIndex() {
		return channelIndex;
	}

	@Override
	public ChannelDataManager getChannelIO() {
		return channelManager;
	}

	@Override
	public void shutdown() throws Exception {
		dbcon.close();
	}

}
