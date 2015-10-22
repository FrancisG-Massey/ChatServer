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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.log4j.Logger;

import com.sundays.chat.io.UserDataManager;
import com.sundays.chat.io.UserDetails;

public final class XmlUserManager implements UserDataManager {

	private static final Logger logger = Logger.getLogger(XmlUserManager.class);
	
	private static final String VERSION = "1.0";
	
	private Map<Integer, UserDetails> lookupByID;
	private Map<String, UserDetails> lookupByUsername;

	private final File indexFile;
	
	private boolean changesPending = false;
	
	private int nextUserID = 100;

	public XmlUserManager(File indexFile) {
		this.indexFile = indexFile;
		if (!indexFile.exists()) {
			logger.warn("User index file "+indexFile.getAbsolutePath()+" does not exist - creating empty index.");
			commitChanges();
		} else {
			try (Reader input = new BufferedReader(new FileReader(indexFile))) {
				readUserData(input);
				logger.info("Loaded user index - "+lookupByID.size()+" users.");
			} catch (XMLStreamException | IOException ex) {
				logger.error("Failed to load user index", ex);
			}
		}
	}

	private void readUserData(Reader input) throws XMLStreamException {
		XMLInputFactory factory = XMLInputFactory.newInstance();

		XMLStreamReader reader = factory.createXMLStreamReader(input);
		while (reader.hasNext()) {
			reader.next();
			if (reader.getEventType() == XMLStreamConstants.START_ELEMENT) {
				if (reader.getLocalName().equalsIgnoreCase("user")) {
					int id = Integer.parseInt(reader.getAttributeValue(null, "id"));
					String username = reader.getAttributeValue(null, "username");
					String hashedPassword = reader.getAttributeValue(null, "hashedPassword");
					String alias = reader.getAttributeValue(null, "alias");
					int defaultChannel = Integer.parseInt(reader.getAttributeValue(null, "defaultChannel"));
					UserDetails details = new UserDetails();
					details.setUserID(id);
					details.setUsername(username);
					details.setHashedPassword(hashedPassword.toCharArray());
					details.setAlias(alias);
					details.setDefaultChannel(defaultChannel);
					lookupByID.put(id, details);
					lookupByUsername.put(username, details);
					if (id > nextUserID) {
						nextUserID = id+1;
					}
				}
			}
		}
	}

	@Override
	public int createUser(String username, char[] hashedPassword) throws IOException {
		int id = nextUserID++;
		UserDetails details = new UserDetails();
		details.setUsername(username);
		details.setUserID(id);
		details.setHashedPassword(hashedPassword);
		synchronized (this) {
			lookupByID.put(id, details);
			lookupByUsername.put(username, details);
			changesPending = true;
		}
		return id;
	}

	@Override
	public void saveUserData(UserDetails user) throws IOException {
		synchronized (this) {
			lookupByID.put(user.getUserID(), user);
			lookupByUsername.put(user.getUsername(), user);
			changesPending = true;
		}
	}

	@Override
	public int lookupByUsername(String username) throws IOException {
		synchronized (this) {
			if (!lookupByUsername.containsKey(username)) {
				return -1;
			}
			return lookupByUsername.get(username).getUserID();
		}
	}

	@Override
	public UserDetails getUserDetails(int id) throws IOException {
		synchronized (this) {
			return lookupByID.get(id);
		}
	}

	@Override
	public boolean userExists(int id) throws IOException {
		return lookupByID.containsKey(id);
	}

	@Override
	public void close() throws Exception {
		if (changesPending) {
			commitChanges();
		}
	}
	
	@Override
	public synchronized void commitChanges () {
		try (Writer writer = new BufferedWriter(new FileWriter(indexFile))) {
			writeIndex(writer);
		} catch (XMLStreamException | IOException ex) {
			logger.error("Failed to save user index", ex);
		}
	}

	private void writeIndex(Writer output) throws XMLStreamException {
		XMLOutputFactory factory = XMLOutputFactory.newInstance();
		XMLStreamWriter writer = factory.createXMLStreamWriter(output);
		writer.writeStartDocument("UTF-8", "1.1");
		writer.writeStartElement("userSet");
		writer.writeAttribute("version", VERSION);
		for (UserDetails user : lookupByID.values()) {
			writer.writeEmptyElement("user");
			writer.writeAttribute("id", Integer.toString(user.getUserID()));
			writer.writeAttribute("username", user.getUsername());
			writer.writeAttribute("hashedPassword", String.copyValueOf(user.getHashedPassword()));
			writer.writeAttribute("alias", user.getAlias());
			writer.writeAttribute("defaultChannel", Integer.toString(user.getDefaultChannel()));
		}
		writer.writeEndElement();
		writer.writeEndDocument();
		changesPending = false;
	}

}
