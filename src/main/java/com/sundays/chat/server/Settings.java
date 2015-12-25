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
package com.sundays.chat.server;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sundays.chat.utils.GeneralTools;

public class Settings {	
    public static final short PERMISSION_VERSION = 2, RANK_NAME_VERSION = 2;
    public static final String VERSION_NAME = "0.36";
    public static final int VERSION_NUMBER = 25;
    public static final int channelCleanupThreadFrequency = 60;//Runs channel management tasks once every minute
    public static final int CHANNEL_CACHE_SIZE = 100;
	
	private static Settings settings;
    
    public static Settings initSettings (Properties p) {
    	if (settings == null) {
    		settings = new Settings(p);
    	}
		return settings;
    }
    
    public int CHANNEL_CLEANUP_FREQUENCY;
	public int CHANNEL_CACHE_SIZE1;
    
    private Settings (Properties p) {
    	String[] ipn = new String[]{"channel_cleanup_frequency","rank_name_max","rank_name_min","channel_cache_size"};
    	if (p.containsKey(ipn[0]) && GeneralTools.isPositiveInteger(p.getProperty(ipn[0]))) {
    		CHANNEL_CLEANUP_FREQUENCY = Integer.parseInt(p.getProperty(ipn[0]));
		} else { 
			Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Invalid or missing parameter given for 'channel_cleanup_frequency'. Expected positive integer.");
			CHANNEL_CLEANUP_FREQUENCY = 100; 
		}
    	
    	if (p.containsKey(ipn[3]) && GeneralTools.isPositiveInteger(p.getProperty(ipn[3]))) {
    		CHANNEL_CACHE_SIZE1 = Integer.parseInt(p.getProperty(ipn[3]));
		} else {
			Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Invalid or missing parameter given for 'channel_cache_size'. Expected positive integer.");
			CHANNEL_CACHE_SIZE1 = 100; 
		}
    }
}
