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
package com.sundays.chat.server;

import com.sundays.chat.io.IOManager;
import com.sundays.chat.server.channel.ChannelAPI;
import com.sundays.chat.server.channel.ChannelManager;
import com.sundays.chat.server.user.UserManager;

public abstract class ChatServer {
	
	private UserManager userManager;
	protected IOManager ioManager;
	private ChannelManager channelManager;
	private ChannelAPI channelAPI;
	private ServerTaskQueue serverTaskScheduler;
	
	public IOManager getIO() {
		if (ioManager == null) {
			throw new IllegalStateException("IO manager not initialised!");
		}
		return ioManager;
	}
	
	public ChannelManager getChannelManager() {
		if (channelManager == null) {
			channelManager = new ChannelManager(this);
		}
		return channelManager;
	}

	public ChannelAPI getChannelAPI() {
		if (channelAPI == null) {
			channelAPI = new ChannelAPI(this);
		}
		return channelAPI;
	}
	
	public UserManager getUserManager() {
		if (userManager == null) {
			userManager = new UserManager(this);
		}
		return userManager;
	}
	
	public ServerTaskQueue serverTaskScheduler() {
		if (serverTaskScheduler == null) {
			serverTaskScheduler = new ServerTaskQueue();
		}
		return serverTaskScheduler;
	}
	
	public void shutdown() throws Exception {// Clean up resources here
		this.serverTaskScheduler().lock();// Prevents new tasks from being cued
		// channelManager().shutdown();//Run the final cleanup tasks
		if (ioManager != null) {
			ioManager.close();// Shutdown the persistence layer resources (to prevent exceptions from being thrown).
		}
		this.serverTaskScheduler().shutdown();// Runs all pending server tasks
												// instantly then shuts down the
												// timer cue
	}

}
