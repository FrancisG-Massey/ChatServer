package com.sundays.chat.io;

/**
 * An interface used between the server layer and the persistence layer to return the relevant IO managers for the percistance layer used.
 * @author Francis
 */
public interface IOManager {
	
	public UserDataManager getUserIO();
	
	public ChannelIndex getChannelIndex();
	
	public ChannelDataManager getChannelIO();
	
	public void shutdown() throws Exception;

}
