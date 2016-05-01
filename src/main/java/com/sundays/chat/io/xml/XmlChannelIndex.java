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
package com.sundays.chat.io.xml;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sundays.chat.io.ChannelDetails;
import com.sundays.chat.io.ChannelIndex;

/**
 * The XML implementation of the {@link ChannelIndex} persistance layer interface.<br /><br />
 * 
 * This class loads channel data from an XML file and stores it in memory.
 * Any changes made to the index are retained in memory until either the {@link #close()} or {@link #commitChanges()} methods are called.<br /><br />
 * 
 * As this class loads and stores the entire index in memory, it is not suitable for large indicies.
 * 
 * @author Francis
 */
public final class XmlChannelIndex implements ChannelIndex {

	private static final Logger logger = LoggerFactory.getLogger(XmlChannelIndex.class);
	
	private static final String VERSION = "1.0";

	private Map<String, ChannelDetails> lookupByName = new HashMap<>();
	private Map<Integer, String> lookupById = new HashMap<>();

	private final File indexFile;
	
	private boolean changesPending = false;

	public XmlChannelIndex(File indexFile) {
		this.indexFile = indexFile;
		if (!indexFile.exists()) {
			logger.warn("Channel index file "+indexFile.getAbsolutePath()+" does not exist - creating empty index.");
			commitChanges();
		} else {
			try (Reader input = new BufferedReader(new FileReader(indexFile))) {
				readIndex(input);
				logger.info("Loaded channel index - "+lookupByName.size()+" channels.");
			} catch (XMLStreamException | IOException ex) {
				logger.error("Failed to load channel index", ex);
			}
		}
	}

	private void readIndex(Reader input) throws XMLStreamException {
		XMLInputFactory factory = XMLInputFactory.newInstance();

		XMLStreamReader reader = factory.createXMLStreamReader(input);
		while (reader.hasNext()) {
			reader.next();
			if (reader.getEventType() == XMLStreamConstants.START_ELEMENT) {
				if (reader.getLocalName().equalsIgnoreCase("channel")) {
					String name = reader.getAttributeValue(null, "name");
					int id = Integer.parseInt(reader.getAttributeValue(null, "id"));
					ChannelDetails details = new ChannelDetails();
					details.setId(id);
					details.setName(name);
					lookupByName.put(name, details);
					lookupById.put(id, name);
				}
			}
		}
	}

	@Override
	public Optional<ChannelDetails> lookupByName(String name) {
		if (lookupByName.containsKey(name)) {
			return Optional.of(lookupByName.get(name));
		} else {
			return Optional.empty();
		}		
	}

	@Override
	public Optional<ChannelDetails> lookupByUuid(UUID uuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Optional<ChannelDetails> lookupById(int id) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<ChannelDetails> search(String term, SearchType type, int limit) {
		Collection<ChannelDetails> results = new HashSet<>();
		int count = 0;
		for (Map.Entry<String, ChannelDetails> channel : lookupByName.entrySet()) {
			if (count > limit) {
				break;
			}
			switch (type) {
			case ALL:
				results.add(channel.getValue());
				break;
			case CONTAINS:
				if (channel.getKey().contains(term)) {
					results.add(channel.getValue());
				}
				break;		
			}
		}
		return results;
	}
	
	@Override
	public synchronized void close () throws Exception {
		if (changesPending) {
			commitChanges();
		}
	}
	
	@Override
	public synchronized void commitChanges () {
		try (Writer writer = new BufferedWriter(new FileWriter(indexFile))) {
			writeIndex(writer);
		} catch (XMLStreamException | IOException ex) {
			logger.error("Failed to save channel index", ex);
		}
	}

	private void writeIndex(Writer output) throws XMLStreamException {
		XMLOutputFactory factory = XMLOutputFactory.newInstance();
		XMLStreamWriter writer = factory.createXMLStreamWriter(output);
		writer.writeStartDocument("UTF-8", "1.1");
		writer.writeStartElement("channelSet");
		writer.writeAttribute("version", VERSION);
		for (Map.Entry<String, ChannelDetails> channel : lookupByName.entrySet()) {
			writer.writeEmptyElement("channel");
			writer.writeAttribute("id", channel.getKey());
		}
		writer.writeEndElement();
		writer.writeEndDocument();
		changesPending = false;
	}

}
