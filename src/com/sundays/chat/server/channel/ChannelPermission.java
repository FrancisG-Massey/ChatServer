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
package com.sundays.chat.server.channel;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.sundays.chat.io.ChannelGroupType;

public enum ChannelPermission {
	/* This enumeration defines information about permissions within channels
	 * The first value represents the permissionID
	 * The second value represents the default rank for that permission
	 * The third value represents the minimum rank that may hold that permission
	 * The fourth value represents the maximum rank that the permission may be set to (this means that ranks above and including this will ALWAYS hold the permission)
	 * If any of the values are going to be set to system ranks, you should use their variables (defined above) rather than manually entering the rank ID
	 * WARNING: the permission ID (first value) MUST be the same as the permission's position in the enumeration. If required, use dummy fields to fill in gaps.
	 */
	
	JOIN (0, "Groups with this permission are allowed to join this channel.", 		
			new ChannelGroupType[]{ChannelGroupType.NORMAL, ChannelGroupType.MODERATOR, ChannelGroupType.ADMINISTRATOR, ChannelGroupType.OWNER, ChannelGroupType.SYSTEM}),
	TALK (1, "Groups with this permission are allowed to send messages in this channel.", 		
			new ChannelGroupType[]{ChannelGroupType.NORMAL, ChannelGroupType.MODERATOR, ChannelGroupType.ADMINISTRATOR, ChannelGroupType.OWNER, ChannelGroupType.SYSTEM}),
	KICK (2, "Groups with this permission are allowed to remove other users from this channel.", 			
			new ChannelGroupType[]{ChannelGroupType.MODERATOR, ChannelGroupType.ADMINISTRATOR, ChannelGroupType.OWNER, ChannelGroupType.SYSTEM}),
	TEMPBAN (3, "Groups with this permission are allowed to temporarily prevent other users from joining this channel.", 		
			new ChannelGroupType[]{ChannelGroupType.MODERATOR, ChannelGroupType.ADMINISTRATOR, ChannelGroupType.OWNER, ChannelGroupType.SYSTEM}),
	PERMBAN (4, "Groups with this permission are allowed to permanently prevent other users from joining this channel.", 	
			new ChannelGroupType[]{ChannelGroupType.MODERATOR, ChannelGroupType.ADMINISTRATOR, ChannelGroupType.OWNER, ChannelGroupType.SYSTEM}),
	RESET (5, "Groups with this permission are allowed to clear this channel, causing all instanced data (including temporary bans) to be removed.", 		
			new ChannelGroupType[]{ChannelGroupType.MODERATOR, ChannelGroupType.ADMINISTRATOR, ChannelGroupType.OWNER, ChannelGroupType.SYSTEM}),
	RANKCHANGE (6, "Groups with this permission are allowed to change the groups of other users in this channel.", 			
			new ChannelGroupType[]{ChannelGroupType.ADMINISTRATOR, ChannelGroupType.OWNER, ChannelGroupType.SYSTEM}),
	PERMISSIONCHANGE (7, "Groups with this permission are allowed to change other groups in this channel.",	
			new ChannelGroupType[]{ChannelGroupType.ADMINISTRATOR, ChannelGroupType.OWNER, ChannelGroupType.SYSTEM}),
	DETAILCHANGE (8, "Groups with this permission are allowed to change the basic details of this channel.", 		
			new ChannelGroupType[]{ChannelGroupType.ADMINISTRATOR, ChannelGroupType.OWNER, ChannelGroupType.SYSTEM}),
	LOCKCHANNEL (9, "Groups with this permission are allowed to lock this channel, which prevents any new users from joining.", 
			new ChannelGroupType[]{ChannelGroupType.MODERATOR, ChannelGroupType.ADMINISTRATOR, ChannelGroupType.OWNER, ChannelGroupType.SYSTEM}),
	ALL (10, "Groups with this permission will inherit all permissions that are avaliable to them.",	
			new ChannelGroupType[]{ChannelGroupType.ADMINISTRATOR, ChannelGroupType.OWNER, ChannelGroupType.SYSTEM});
	
	private int id;
	private String description;
	private Set<ChannelGroupType> avaliableTo;
	ChannelPermission (int id, String description, ChannelGroupType[] avaliableTo) {
		this.id = id;
		this.description = description;
		this.avaliableTo = new HashSet<>(Arrays.asList(avaliableTo));
	}
	
	public int id () { return this.id; }
	public String description () { return this.description; }
	
	public boolean canHavePermission (ChannelGroupType t) {
		return avaliableTo.contains(t);
	}
    
    public static ChannelPermission getPermissionFromID (int id) {
    	ChannelPermission p = null;
    	for (ChannelPermission p1 : ChannelPermission.values()) {
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
    	for (ChannelPermission p1 : ChannelPermission.values()) {
    		if (p1.id() == id) {
    			return true;
    		}
    	}
    	return false;
    }
}
