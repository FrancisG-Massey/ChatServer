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
package com.sundays.chat.io.xml;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sundays.chat.io.ChannelDataManager;
import com.sundays.chat.io.ChannelDetails;
import com.sundays.chat.io.ChannelGroupData;

public class XmlChannelManager implements ChannelDataManager {

	private static final Logger logger = Logger.getLogger(XmlChannelManager.class);
	
	private static class ChannelData {
		private ChannelDetails details;
		private Map<Integer, Byte> ranks;
		private List<Integer> bans;
		private List<ChannelGroupData> groups;
	}
	
	private final File folder;
	private DocumentBuilderFactory factory;
	
	private Map<Integer, ChannelData> channelDataCache;

	public XmlChannelManager(File folder) {
		this.folder = folder;
		this.factory = DocumentBuilderFactory.newInstance();
	}
	
	private ChannelData fetchChannelData (int channelID) {
		File file = new File(folder, channelID+".xml");
		if (!file.exists()) {
			logger.warn("Permanent data for channel "+channelID+" not found at "+file.getAbsolutePath());
			return null;
		}
		ChannelData data = new ChannelData();
		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(file);
			doc.getDocumentElement().normalize();
			
			
			NodeList nodes = doc.getElementsByTagName("channel");
			Node node = nodes.item(0);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				int id = Integer.parseInt(element.getAttribute("id"));
				int owner = Integer.parseInt(element.getAttribute("owner"));
				if (id != channelID) {
					logger.warn("ChannelID attribute in file does not match actual ID. File="+file.getAbsolutePath()+", channel="+channelID);
					return null;
				}
				String name = element.getElementsByTagName("name").item(0).getTextContent();
				String message = element.getElementsByTagName("message").item(0).getTextContent();
				String abbreviation = element.getElementsByTagName("abbreviation").item(0).getTextContent();
				data.details = new ChannelDetails(id, name, message, abbreviation, null, null, false, owner);
				
				
				node = element.getElementsByTagName("ranks").item(0);
				data.ranks = new HashMap<>();
				if (node.getNodeType() == Node.ELEMENT_NODE) {
					Element rankList = (Element) node;
					nodes = rankList.getElementsByTagName("rank");
					Element rank;
					for (int i = 0; i < nodes.getLength(); i++) {
						rank = (Element) nodes.item(i);
						int userID = Integer.parseInt(rank.getAttribute("user"));
						byte rankID = Byte.parseByte(rank.getAttribute("rank"));
						data.ranks.put(userID, rankID);
					}
				}
				
				node = element.getElementsByTagName("bans").item(0);
				data.bans = new ArrayList<>();
				if (node.getNodeType() == Node.ELEMENT_NODE) {
					Element banList = (Element) node;
					nodes = banList.getElementsByTagName("ban");
					Element ban;
					for (int i = 0; i < nodes.getLength(); i++) {
						ban = (Element) nodes.item(i);
						int userID = Integer.parseInt(ban.getAttribute("user"));
						data.bans.add(userID);
					}
				}
			}
		} catch (Exception ex) {
			logger.error("Failed to fetch data for channel "+channelID, ex);
			return null;
		}
		return data;
	}

	@Override
	public void addRank(int channelID, int userID) {
		// TODO Auto-generated method stub

	}

	@Override
	public void changeRank(int channelID, int userID, byte rankID) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeRank(int channelID, int userID) {
		// TODO Auto-generated method stub

	}

	@Override
	public void addBan(int channelID, int userID) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeBan(int channelID, int userID) {
		// TODO Auto-generated method stub

	}

	@Override
	public void addGroup(int channelID, ChannelGroupData group) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateGroup(int channelID, ChannelGroupData group) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeGroup(int channelID, int groupID) {
		// TODO Auto-generated method stub

	}

	@Override
	public void syncDetails(int channelID, ChannelDetails details) {
		// TODO Auto-generated method stub

	}

	@Override
	public ChannelDetails getChannelDetails(int channelID) {
		synchronized (channelDataCache) {
			if (channelDataCache.containsKey(channelID)) {
				return channelDataCache.get(channelID).details;
			}
			ChannelData data = this.fetchChannelData(channelID);
			if (data != null) {
				channelDataCache.put(channelID, data);
			}
			return data.details;
		}
	}

	@Override
	public List<Integer> getChannelBans(int channelID) {
		synchronized (channelDataCache) {
			if (channelDataCache.containsKey(channelID)) {
				return channelDataCache.get(channelID).bans;
			}
			ChannelData data = this.fetchChannelData(channelID);
			if (data != null) {
				channelDataCache.put(channelID, data);
			}
			return data.bans;
		}
	}

	@Override
	public Map<Integer, Byte> getChannelRanks(int channelID) {
		synchronized (channelDataCache) {
			if (channelDataCache.containsKey(channelID)) {
				return channelDataCache.get(channelID).ranks;
			}
			ChannelData data = this.fetchChannelData(channelID);
			if (data != null) {
				channelDataCache.put(channelID, data);
			}
			return data.ranks;
		}
	}

	@Override
	public List<ChannelGroupData> getChannelGroups(int channelID) {
		synchronized (channelDataCache) {
			if (channelDataCache.containsKey(channelID)) {
				return channelDataCache.get(channelID).groups;
			}
			ChannelData data = this.fetchChannelData(channelID);
			if (data != null) {
				channelDataCache.put(channelID, data);
			}
			return data.groups;
		}
	}

	@Override
	public void commitChanges() throws IOException {
		// TODO Auto-generated method stub

	}

}
