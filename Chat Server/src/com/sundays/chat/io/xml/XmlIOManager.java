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
package com.sundays.chat.io.xml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.log4j.Logger;

import com.sundays.chat.io.ChannelDataManager;
import com.sundays.chat.io.ChannelIndex;
import com.sundays.chat.io.IOManager;
import com.sundays.chat.io.UserDataManager;
import com.sundays.chat.utils.ConfigurationException;

/**
 * The XML IO Manager class. Used in applications where portability is more important than sustaining a large number of users.
 * 
 * @author Francis
 */
public class XmlIOManager implements IOManager {

	private static final Logger logger = Logger.getLogger(XmlIOManager.class);
	
	private UserDataManager userManager;
	
	private ChannelIndex channelIndex;
	
	public XmlIOManager (String configFile) throws FileNotFoundException, ConfigurationException {
		if (!new File(configFile).exists()) {
			throw new FileNotFoundException("Configuration file does not exist: "+configFile);
		}
		try (Reader input = new BufferedReader(new FileReader(configFile))) {
			readConfig(input);
		} catch (XMLStreamException | IOException ex) {
			logger.error("Failed to load XML IO configuration.", ex);
		}
		if (channelIndex == null) {
			throw new ConfigurationException("No channel index specified.");
		}
		if (userManager == null) {
			throw new ConfigurationException("No user index specified.");
		}
	}
	
	private final void readConfig (Reader input) throws XMLStreamException, ConfigurationException {
		XMLInputFactory factory = XMLInputFactory.newInstance();
		XMLStreamReader reader = factory.createXMLStreamReader(input);		
		while (reader.hasNext()) {
			reader.next();
			if (reader.getEventType() == XMLStreamConstants.START_ELEMENT) {
				if (reader.getLocalName().equalsIgnoreCase("channelIndex")) {
					String filename = reader.getAttributeValue(null, "filename");	
					if (filename == null) {
						throw new ConfigurationException("No channel index filename specified.");
					}
					channelIndex = new XmlChannelIndex(new File(filename));
				} else if (reader.getLocalName().equalsIgnoreCase("userIndex")) {
					String filename = reader.getAttributeValue(null, "filename");	
					if (filename == null) {
						throw new ConfigurationException("No user index filename specified.");
					}
					userManager = new XmlUserManager(new File(filename));
				}
			}
		}
	}

	@Override
	public UserDataManager getUserIO() {
		return userManager;
	}

	@Override
	public ChannelIndex getChannelIndex() {
		return channelIndex;
	}

	@Override
	public ChannelDataManager getChannelIO() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void close() throws Exception {
		channelIndex.close();
	}

}
