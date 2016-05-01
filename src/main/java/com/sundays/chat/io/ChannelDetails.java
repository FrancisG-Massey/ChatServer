/*******************************************************************************
 * Copyright (c) 2013, 2016 Francis G.
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
package com.sundays.chat.io;

import java.util.UUID;

/**
 * Used to communicate channel details between the persistence layer and the application layer.
 * 
 * @author Francis
 */
public final class ChannelDetails {	
	private int id;//Channel ID	
	private UUID uuid;
	private String name;
	private String alias;
	private String description;
	private int owner;
	
	public ChannelDetails () {
		
	}
	
	public ChannelDetails (ChannelDetails details) {
		this.id = details.id;
		this.uuid = details.uuid;
		this.name = details.name;
		this.alias = details.alias;
		this.owner = details.owner;
	}
	
	public ChannelDetails (int id, UUID uuid, String name, String alias, int owner) {
		//Full constructor used when data can be added all at once
		this.id = id;
		this.uuid = uuid;
		this.name = name;
		this.alias = alias;
		this.owner = owner;
	}
	
	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}

	public UUID getUuid() {
		return uuid;
	}

	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Gets the user ID of the channel owner
	 * @return The owner's user ID
	 */
	public int getOwner() {
		return owner;
	}

	/**
	 * Sets the ID of the channel owner
	 * @param owner The user ID for the channel owner
	 */
	public void setOwner(int owner) {
		this.owner = owner;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ChannelDetails [id=" + id + ", name=" + name + ", alias=" + alias + ", description="
				+ description + ", owner=" + owner + "]";
	}
}
