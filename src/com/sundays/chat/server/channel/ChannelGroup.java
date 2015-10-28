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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sundays.chat.io.ChannelGroupData;
import com.sundays.chat.io.ChannelGroupType;

public class ChannelGroup {
	
	public final List<Integer> parentGroups = new ArrayList<Integer>();
	public final List<Integer> childGroups = new ArrayList<Integer>();
	public final EnumSet<ChannelPermission> permissions = EnumSet.noneOf(ChannelPermission.class);
	private String groupName, groupIconUrl;
	private final int id;
	private final int channelID;
	public ChannelGroupType groupType = ChannelGroupType.NORMAL;
	
	private byte legacyRank;
	
	public ChannelGroup (int channelID, int groupID) {
		this.id = groupID;
		this.channelID = channelID;
	}
	
	public ChannelGroup (int channelID, int groupID, byte legacyRank) {
		this.id = groupID;
		this.channelID = channelID;
		this.legacyRank = legacyRank;
	}
	
	public ChannelGroup (int channelID, int groupID, String name, String url, ChannelGroupType type) {
		this.id = groupID;
		this.channelID = channelID;
		this.groupName = name;
		this.groupIconUrl = url;
		this.groupType = type;
	}
	
	public ChannelGroup (ChannelGroupData data) {
		//Fixed data fields
		this.id = data.getGroupID();
		this.channelID = data.getChannelID();
		
		//Variable simple fields
		this.groupName = data.getName();
		this.groupIconUrl = data.groupIconUrl;
		this.groupType = data.getType();
		
		//Group permissions
		boolean hasAllPermissions = false;
		for (String permission : data.getPermissions()) {
			ChannelPermission p = ChannelPermission.valueOf(permission.toUpperCase().replace("\"", "").trim());
			if (p == ChannelPermission.ALL) {
				permissions.addAll(EnumSet.allOf(ChannelPermission.class));
				hasAllPermissions = true;
				break;
			}
			permissions.add(p);		
		}
		for (ChannelPermission p : permissions) {
			if (!p.canHavePermission(groupType)) {
				permissions.remove(p);
				if (!hasAllPermissions) {
					Logger.getLogger(this.getClass().getName()).log(Level.WARNING, 
						"Group "+id+" (of channel "+channelID+") cannot hold permission: "+p.toString()+". It has been removed from the group.");
				}				
			}
		}
	}
	
	public ChannelGroup (ChannelGroup oldGroup) {
		this.id = oldGroup.id;
		this.channelID = oldGroup.channelID;
		this.groupName = oldGroup.groupName;
		this.groupIconUrl = oldGroup.groupIconUrl;
		this.groupType = oldGroup.groupType;
	}

	public int getId() {
		return id;
	}

	public String getName () {
		return this.groupName;
	}
	
	public String getIconUrl () {
		return this.groupIconUrl;
	}
	
	protected void setName (String newName) {
		this.groupName = newName;
	}
	
	protected void setIconUrl (String iconUrl) {
		this.groupIconUrl = iconUrl;
	}
	
	public int getLegacyRank () {
		return legacyRank;
	}
	
	public ChannelGroupData encode () {
		Set<String> permissions = new HashSet<>();
		return new ChannelGroupData(channelID, id, groupName, 
				permissions, groupType, groupIconUrl);
	}
	
	/*
	 * Initialises the channel group using data from a JSONObject
	 * If the object is missing data, an exception is thrown
	 * Example format: {"groupName" : "Rank One", "groupId" : 1, "permissions" : [talk, join, kick], parentGroups : [1, 3, 7], childGroups : [2]}
	 */
	
	public ChannelGroup (JSONObject groupDetails) throws JSONException {
		this.id = groupDetails.getInt("groupId");
		this.channelID = 0;
		this.groupName = groupDetails.getString("groupName");
		JSONArray parentGroups = groupDetails.getJSONArray("parentGroups");
		for (int i=0;i<parentGroups.length(); i++) {
			this.parentGroups.add(parentGroups.getInt(i));
		}
		JSONArray childGroups = groupDetails.getJSONArray("childGroups");
		for (int i=0;i<childGroups.length(); i++) {
			this.childGroups.add(childGroups.getInt(i));
		}
		JSONArray permissions = groupDetails.getJSONArray("permissions");
		for (int i=0;i<permissions.length(); i++) {
			this.permissions.add(ChannelPermission.valueOf(permissions.getString(i).toUpperCase()));
		}
	}	
	
	public JSONObject serialiseToJSON () throws JSONException {
		JSONObject returnObject = new JSONObject();
		returnObject.put("groupId", this.id);
		returnObject.put("groupName", this.groupName);
		returnObject.put("parentGroups", this.parentGroups);
		returnObject.put("childGroups", this.childGroups);
		returnObject.put("permissions", this.permissions);
		System.out.println(returnObject);
		return returnObject;
	}
}
