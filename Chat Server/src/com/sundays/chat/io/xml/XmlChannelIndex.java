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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.log4j.Logger;

import com.sundays.chat.io.ChannelIndex;

/**
 * 
 * @author Francis
 */
public class XmlChannelIndex implements ChannelIndex {

	private static final Logger logger = Logger.getLogger(XmlChannelIndex.class);
	
	private static final String VERSION = "1.0";

	private Map<String, Integer> lookupByName = new HashMap<>();
	private Map<Integer, String> lookupById = new HashMap<>();

	private final File indexFile;

	public XmlChannelIndex(File indexFile) {
		this.indexFile = indexFile;

		try (Reader input = new BufferedReader(new FileReader(indexFile))) {
			readIndex(input);
		} catch (XMLStreamException | IOException ex) {
			logger.error("Failed to load channel index", ex);
		}
	}

	public void readIndex(Reader input) throws XMLStreamException {
		XMLInputFactory factory = XMLInputFactory.newInstance();

		XMLStreamReader reader = factory.createXMLStreamReader(input);
		while (reader.hasNext()) {
			reader.next();
			if (reader.getEventType() == XMLStreamConstants.START_ELEMENT) {
				if (reader.getLocalName().equalsIgnoreCase("channel")) {
					String name = reader.getAttributeValue(null, "name");
					int id = Integer.parseInt(reader.getAttributeValue(null, "id"));
					lookupByName.put(name, id);
					lookupById.put(id, name);
				}
			}
		}
	}

	@Override
	public int lookupByName(String name) {
		return lookupByName.get(name);
	}

	@Override
	public Map<String, Integer> search(String term, SearchType type, int limit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean channelExists(int id) {
		return lookupById.containsKey(id);
	}
	
	public void save () {
		try (Writer writer = new BufferedWriter(new FileWriter(indexFile))) {
			writeIndex(writer);
		} catch (XMLStreamException | IOException ex) {
			logger.error("Failed to save channel index", ex);
		}
	}

	public void writeIndex(Writer output) throws XMLStreamException {
		XMLOutputFactory factory = XMLOutputFactory.newInstance();
		XMLStreamWriter writer = factory.createXMLStreamWriter(output);
		writer.writeStartDocument("UTF-8", "1.1");
		writer.writeStartElement("channelSet");
		writer.writeAttribute("version", VERSION);
		for (Map.Entry<String, Integer> channel : lookupByName.entrySet()) {
			writer.writeEmptyElement("channel");
			writer.writeAttribute("id", channel.getKey());
		}
		writer.writeEndElement();
		writer.writeEndDocument();
	}

}
