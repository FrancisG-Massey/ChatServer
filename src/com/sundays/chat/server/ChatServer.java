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
