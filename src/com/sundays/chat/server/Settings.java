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

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sundays.chat.server.channel.ChannelGroup;
import com.sundays.chat.utils.GeneralTools;

public class Settings {	
	   
    public static final Map<Byte, String> defaultRanks = new HashMap<>(12);
    public static final Map<Integer, ChannelGroup> systemGroups = new HashMap<Integer, ChannelGroup>();
    public static final int rankNameMax = 25, rankNameMin = 2;
    public static final short TOTAL_RANKS, PERMISSION_VERSION = 2, RANK_NAME_VERSION = 2;
    public static final String VERSION_NAME = "0.36";
    public static final int VERSION_NUMBER = 25;
    public static final int channelCleanupThreadFrequency = 60;//Runs channel management tasks once every minute
    public static final int CHANNEL_CACHE_SIZE = 100;
	
	public static enum GroupType {NORM,MOD,ADMIN,OWN,SYS};
    
    private static Settings settings;
    
    public static Settings initSettings (Properties p) {
    	if (settings == null) {
    		settings = new Settings(p);
    	}
		return settings;
    }
    /*Integer parameters*/
    public enum IntParam {
    	CHANNEL_CLEANUP_FREQUENCY (100),
    	RANK_NAME_MAX (25), 
    	RANK_NAME_MIN (2),
    	CHANNEL_CACHE_SIZE1 (100);
    	public int def;
    	IntParam (int def) { this.def = def; }
    }
    
    public int CHANNEL_CLEANUP_FREQUENCY,
	RANK_NAME_MAX, 
	RANK_NAME_MIN,
	CHANNEL_CACHE_SIZE1;
    
    public EnumMap<IntParam, Integer> intParams = new EnumMap<IntParam, Integer>(IntParam.class);
    
    private Settings (Properties p) {
    	String[] ipn = new String[]{"channel_cleanup_frequency","rank_name_max","rank_name_min","channel_cache_size"};
    	if (p.containsKey(ipn[0]) && GeneralTools.isPositiveInteger(p.getProperty(ipn[0]))) {
    		CHANNEL_CLEANUP_FREQUENCY = Integer.parseInt(p.getProperty(ipn[0]));
		} else { 
			Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Invalid or missing parameter given for 'channel_cleanup_frequency'. Expected positive integer.");
			CHANNEL_CLEANUP_FREQUENCY = 100; 
		}
    	
    	if (p.containsKey(ipn[1]) && GeneralTools.isPositiveInteger(p.getProperty(ipn[1]))) {
    		RANK_NAME_MAX = Integer.parseInt(p.getProperty(ipn[1]));
		} else { 
			Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Invalid or missing parameter given for 'rank_name_max'. Expected positive integer.");
			RANK_NAME_MAX = 25; 
		}
    	
    	if (p.containsKey(ipn[2]) && GeneralTools.isPositiveInteger(p.getProperty(ipn[2])) && Integer.parseInt(p.getProperty(ipn[2])) <= RANK_NAME_MAX) {
    		RANK_NAME_MIN = Integer.parseInt(p.getProperty(ipn[2]));
		} else {
			Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Invalid or missing parameter given for 'rank_name_min'. Expected positive integer less than 'rank_name_max'.");
			RANK_NAME_MIN = 2; 
		}
    	
    	if (p.containsKey(ipn[3]) && GeneralTools.isPositiveInteger(p.getProperty(ipn[3]))) {
    		CHANNEL_CACHE_SIZE1 = Integer.parseInt(p.getProperty(ipn[3]));
		} else {
			Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Invalid or missing parameter given for 'channel_cache_size'. Expected positive integer.");
			CHANNEL_CACHE_SIZE1 = 100; 
		}
    	
    	for (IntParam pr : IntParam.values()) {
    		String key = pr.name().toLowerCase(Locale.ENGLISH);
    		if (p.containsKey(key) && GeneralTools.isInteger(p.getProperty(key))) {
    			intParams.put(pr, Integer.parseInt(p.getProperty(key)));
    		} else {
    			
    		}
    	}
    }    
    /*private enum Parameter {
    	CHANNEL_CLEANUP_FREQUENCY(ValidTypes.Integer);
    	ValidTypes type;
    	Parameter (ValidTypes type) {
    		this.type = type;
    	}
    	
    	public boolean isValid (String key) {
    		switch (type) {
    		case Integer:
    			return GeneralTools.isInteger(key);
    		case String:
    			return true;
    		default:
    			return false;
    		}
    	}
    }*/
    
    //Predefine the IDs for commonly used rank levels (aka 'system ranks')
    public enum PresetRanks {
    	GUEST_RANK (0),
    	DEFAULT_RANK (1),
    	MOD_RANK (5),
    	ADMIN_RANK (9),
    	OWNER_RANK (11);
    	byte id;
    	PresetRanks(int id) {
    		this.id = (byte) id;
    	}
    	
    	public byte value() { return id; }
    	
    	protected void setValue (byte value) { this.id = value; }
    	
    	@Override
    	public String toString () { return id+""; }
    }
    public static final byte DEFAULT_RANK = 1;//The rank that all users will be automatically assigned when they are added to the channel's rank data
    public static final byte GUEST_RANK = 0;//The rank that any users who are not in the channel's rank data will receive
    public static final byte MOD_RANK = 5;//A system rank for the position of 'channel moderator'. This holds moderative permissions by default, but can be changed in each channel
    public static final byte ADMIN_RANK = 9;//A system rank for the position of 'channel administrator'. Holds administrative permissions by default, but this can be changed in each channel
    public static final byte OWNER_RANK = 11;//The highest channel-specific rank available. Can only be held by a single person at a time, and holds all available permissions.

    public enum ReportTypes {
    	CHANNEL,
    	USER
    }
    
    static {       
    	defaultRanks.clear();
        defaultRanks.put(GUEST_RANK, "Guest");
        defaultRanks.put(DEFAULT_RANK, "Rank one");
        defaultRanks.put((byte) 2, "Rank two");
        defaultRanks.put((byte) 3, "Rank three");
        defaultRanks.put((byte) 4, "Rank four");
        defaultRanks.put(MOD_RANK, "Moderator");
        defaultRanks.put((byte) 6, "Rank six");
        defaultRanks.put((byte) 7, "Rank seven");
        defaultRanks.put((byte) 8, "Rank eight");
        defaultRanks.put(ADMIN_RANK, "Administrator");
        defaultRanks.put((byte) 10, "Rank ten");
        defaultRanks.put(OWNER_RANK, "Owner");
		TOTAL_RANKS = (short) defaultRanks.size();
    }
    
    static {       
    	systemGroups.clear();
    	systemGroups.put(53, new ChannelGroup(50, 53, "Unknown", null, GroupType.NORM).overrides(-2));
    }
}
