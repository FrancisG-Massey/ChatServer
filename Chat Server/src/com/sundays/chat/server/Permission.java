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
package com.sundays.chat.server;

import static com.sundays.chat.server.Settings.ADMIN_RANK;
import static com.sundays.chat.server.Settings.DEFAULT_RANK;
import static com.sundays.chat.server.Settings.GUEST_RANK;
import static com.sundays.chat.server.Settings.MOD_RANK;
import static com.sundays.chat.server.Settings.OWNER_RANK;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sundays.chat.server.Settings.PresetRanks;


public enum Permission {
    	/* This enumeration defines information about permissions within channels
    	 * The first value represents the permissionID
    	 * The second value represents the default rank for that permission
    	 * The third value represents the minimum rank that may hold that permission
    	 * The fourth value represents the maximum rank that the permission may be set to (this means that ranks above and including this will ALWAYS hold the permission)
    	 * If any of the values are going to be set to system ranks, you should use their variables (defined above) rather than manually entering the rank ID
    	 * WARNING: the permission ID (first value) MUST be the same as the permission's position in the enumeration. If required, use dummy fields to fill in gaps.
    	 */
    	JOIN (0, DEFAULT_RANK, GUEST_RANK, OWNER_RANK),
    	TALK (1, DEFAULT_RANK, GUEST_RANK, OWNER_RANK),
    	KICK (2, MOD_RANK, 3, OWNER_RANK),
    	TEMPBAN (3, MOD_RANK, 3, OWNER_RANK),
    	PERMBAN (4, ADMIN_RANK, MOD_RANK, OWNER_RANK),
    	RESET (5, ADMIN_RANK, MOD_RANK, OWNER_RANK),
    	RANKCHANGE (6, 10, ADMIN_RANK, OWNER_RANK),
    	PERMISSIONCHANGE (7, 10, ADMIN_RANK, OWNER_RANK),
    	DETAILCHANGE (8, 10, ADMIN_RANK, OWNER_RANK),
    	LOCKCHANNEL (9, ADMIN_RANK, MOD_RANK, OWNER_RANK);
    	
    	
    	private int id, minValue, maxValue, defaultValue;
    	Permission (int id, int defaultV, int min, int max) {
    		this.id = id;
    		this.minValue = min;
    		this.maxValue = max;
    		this.defaultValue = defaultV;
    	}
    	
    	public int id () { return this.id; }
    	public int maxValue () { return this.maxValue; }
    	public int minValue () { return this.minValue; }
    	public int defaultValue () { return this.defaultValue; }
    	
    	private static byte validateRankFromString (String rankN) throws IllegalArgumentException {
    		byte rank;
    		try {
    			rank = (byte) Integer.parseInt(rankN);//First, try to change the value directly into an integer.
			} catch (NumberFormatException ex) {
				try {
					rank = PresetRanks.valueOf(rankN).value();//If that isn't possible, try decoding it into one of the preset ranks.
				} catch (IllegalArgumentException e) {//If neither options work, thrown an exception
					throw new IllegalArgumentException();
				}    					
			}
			if (rank > PresetRanks.OWNER_RANK.value() || rank < PresetRanks.GUEST_RANK.value()) {//If the value is not within an acceptable range, throw an exception.
				throw new IllegalArgumentException();
				//"Invalid value for permission "+i+", parameter "+v+". Expected integer between "+PresetRanks.GUEST_RANK.value()+" and "+PresetRanks.OWNER_RANK.value()
			}
			return rank;
    	}
    	
    	/**
    	 * Decodes the "permissions" parameter from the initial configuration file, then places all the relevant values into the permissions settings.
    	 * The JSON string provided MUST be valid and well-formed. Any invalid area in the string will cause the method to immediately throw an exception and stop parsing the rest of the values.
    	 * 
    	 * @param permissions               A JSON string containing the permission values. Each permission should be combined into an array, and each permission should contain a "name", "default", "max" and "min" value.
    	 * @throws IllegalArgumentException If anything is wrong with the string provided, an exception will be thrown.
    	 * @throws JSONException            If the string provided was not valid JSON, or there was a missing parameter in any of the areas, this exception will be thrown.
    	 */
    	protected static void setPermissionDefaults (String permissionString) throws IllegalArgumentException, JSONException {
    		JSONArray permissionObject = new JSONArray(permissionString);
    		for (int i=0;i<permissionObject.length();i++) {
    			JSONObject permission = permissionObject.getJSONObject(i);
    			try {
    				Permission.valueOf(permission.getString("name")).defaultValue =  validateRankFromString(permission.getString("default"));
    				Permission.valueOf(permission.getString("name")).maxValue =  validateRankFromString(permission.getString("max"));
    				Permission.valueOf(permission.getString("name")).minValue =  validateRankFromString(permission.getString("min"));
    			} catch (IllegalArgumentException e) {
    				throw new IllegalArgumentException("Invalid value for permission "+i+". Expected integer between "+PresetRanks.GUEST_RANK.value()+" and "+PresetRanks.OWNER_RANK.value());
    			}
    		}
    		
    		/*String[] permission = permissionString.split(Pattern.quote(";"));//Splits the string into separate values for each permission
    		
    		for (int i=0;i<permission.length;i++) {//Loops through each permission provided
    			String[] pData = permission[i].split(Pattern.quote(","));//Splits each permission into 4 values, separated by commas
    			if (pData.length != 4) {//If there are not 4 parameters, thrown any exception.
    				throw new IllegalArgumentException("Invalid number of paramaters for permission "+i+". Expected: 4");
    			}
    			byte[] pValues = new byte[3];
    			for (int v=1;v<4;v++) {//Loops over the last three parameters and attempts to decode them into ranks
    				try {
    					pValues[v-1] = (byte) Integer.parseInt(pData[v]);//First, try to change the value directly into an integer.
    				} catch (NumberFormatException ex) {
    					try {
    						pValues[v-1] = PresetRanks.valueOf(pData[v]).value();//If that isn't possible, try decoding it into one of the preset ranks.
    					} catch (IllegalArgumentException e) {//If neither options work, thrown an exception
    						throw new IllegalArgumentException("Invalid value for permission "+i+", paramater "+v+". Expected integer between "+GUEST_RANK+" and "+OWNER_RANK);
    					}    					
    				}
    				if (pValues[v-1] > PresetRanks.OWNER_RANK.value() || pValues[v-1] < PresetRanks.GUEST_RANK.value()) {//If the value is not within an aceptable range, throw an exception.
    					throw new IllegalArgumentException("Invalid value for permission "+i+", paramater "+v+". Expected integer between "+PresetRanks.GUEST_RANK.value()+" and "+PresetRanks.OWNER_RANK.value());
    				}
    			}
    			Permission.valueOf(pData[0]).defaultValue = pValues[0];
    			Permission.valueOf(pData[0]).maxValue = pValues[1];
    			Permission.valueOf(pData[0]).minValue = pValues[2];
    		}*/
    	}
        
        public static Permission getPermissionFromID (int id) {
        	Permission p = null;
        	for (Permission p1 : Permission.values()) {
        		if (p1.id() == id) {
        			p = p1;
        			break;
        		}
        	}
        	if (p == null) {
        		throw new IllegalArgumentException("Permission id="+id+" does not exist. Maybe it is not supported in this implementation?");
        	}
    		return p;    	
        }
        
        public static boolean permissionExists (int id) {
        	for (Permission p1 : Permission.values()) {
        		if (p1.id() == id) {
        			return true;
        		}
        	}
        	return false;
        }
    }