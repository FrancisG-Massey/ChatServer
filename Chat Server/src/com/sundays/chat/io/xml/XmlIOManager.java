package com.sundays.chat.io.xml;

import com.sundays.chat.io.ChannelDataManager;
import com.sundays.chat.io.ChannelIndex;
import com.sundays.chat.io.IOManager;
import com.sundays.chat.io.UserDataManager;

public class XmlIOManager implements IOManager {
	
	private UserDataManager userManager;

	@Override
	public UserDataManager getUserIO() {
		return userManager;
	}

	@Override
	public ChannelIndex getChannelIndex() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ChannelDataManager getChannelIO() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void shutdown() throws Exception {
		// TODO Auto-generated method stub

	}

}
