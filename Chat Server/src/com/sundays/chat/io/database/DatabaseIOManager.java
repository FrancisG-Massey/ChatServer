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
