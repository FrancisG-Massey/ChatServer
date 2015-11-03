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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.sundays.chat.io.ChannelDataIO;
import com.sundays.chat.io.ChannelDetails;
import com.sundays.chat.io.ChannelGroupData;
import com.sundays.chat.io.ChannelGroupType;
import com.sundays.chat.utils.NamespaceContextMap;

/**
 * An XML back-end for channel data storage. Uses one file for each channel and stores data conforming to the channel.xsd schema.
 * 
 * @author Francis
 */
public final class XmlChannelSave implements ChannelDataIO {

	private static final Logger logger = Logger.getLogger(XmlChannelSave.class);
	
	private final File saveFolder;
	private DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	private XPath xPath = XPathFactory.newInstance().newXPath();
	
	private LoadingCache<Integer, Document> docCache = CacheBuilder.newBuilder().expireAfterAccess(5, TimeUnit.MINUTES).build(new CacheLoader<Integer, Document>() {

		@Override
		public Document load(Integer channelID) throws Exception {
			Document doc;
			File file = new File(saveFolder, channelID+".xml");
			if (!file.exists()) {
				throw new IOException("Permanent data for channel "+channelID+" not found at "+file.getAbsolutePath());
			}
			try {
				DocumentBuilder builder = factory.newDocumentBuilder();
				try {
					doc = builder.parse(file);
				} catch (SAXException ex) {
					throw new IOException("Channel data for "+channelID+" is invalid.", ex);
				}
			} catch (IOException | ParserConfigurationException ex) {
				throw new IOException("Failed to fetch data for channel "+channelID, ex);
			}
			
			doc.getDocumentElement().normalize();
			return doc;
		}
		
	});
	private Set<Document> savePending = Collections.synchronizedSet(new HashSet<Document>());

	//Keep a copy of the lookup expressions in memory, to make future lookups slightly faster.
	private XPathExpression idLookup;
	private XPathExpression nameLookup;
	private XPathExpression aliasLookup;
	private XPathExpression welcomeMessageLookup;
	private XPathExpression descriptionLookup;
	private XPathExpression ownerLookup;	
	
	private XPathExpression banLookup;
	private XPathExpression memberLookup;
	private XPathExpression groupLookup;
	private XPathExpression memberListLookup;
	private XPathExpression banListLookup;

	public XmlChannelSave(File folder, File schemaFile) {
		this.saveFolder = folder;
		factory.setNamespaceAware(true);
		if (schemaFile == null || !schemaFile.exists() || !schemaFile.isFile()) {
			logger.warn("No channel schema specified (or specified schema does not exist).");
		} else {
			Schema schema = loadSchema(schemaFile);
			factory.setSchema(schema);
		}
		xPath.setNamespaceContext(new NamespaceContextMap("csc", "http://www.example.org/chatserver/channel"));
	}
	
	private final Schema loadSchema (File schemaFile) {
		SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Source schemaSource = new StreamSource(schemaFile);
	    Schema schema;
		try {
			schema = factory.newSchema(schemaSource);
		} catch (SAXException ex) {
			logger.warn("Error parsing channel XML schema.", ex);
			schema = null;
		}
		return schema;
	}
	
	private void saveChannelDoc (Document document) throws IOException {
		
		if (idLookup == null) {
			try {
				idLookup = xPath.compile("/csc:channel/@id");
			} catch (XPathExpressionException ex) {
				throw new IOException("Failed to compile id lookup expression. This probably indicates a configuration or program error.", ex);
			}
		}
		int channelID;
		try {
			channelID = Integer.parseInt(idLookup.evaluate(document));
		} catch (XPathExpressionException ex) {
			throw new IOException("Failed to find ID for channel data.", ex);
		}
		try {
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			//transformerFactory.setAttribute("indent-number", 4);
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			DOMSource source = new DOMSource(document);
			StreamResult result = new StreamResult(new File(saveFolder,  Integer.toString(channelID) + ".xml"));
			transformer.transform(source, result);
		} catch (TransformerException ex) {
			throw new IOException("Problem saving data for channel "+channelID, ex);
		}
	}

	@Override
	public void addMember(int channelID, int userID, int group) throws IOException {
		Document channelDoc;
		synchronized (docCache) {
			try {
				channelDoc = docCache.get(channelID);
			} catch (ExecutionException ex) {
				throw new IOException("Failed to find data for channel "+channelID+".", ex);
			}
		}
		
		if (memberListLookup == null) {
			try {
				memberListLookup = xPath.compile("/csc:channel/csc:members");
			} catch (XPathExpressionException ex) {
				throw new IOException("Failed to compile member lookup expression. This probably indicates a configuration or program error.", ex);
			}
		}
		
		synchronized (channelDoc) {
			Element memberList;
			try {
				memberList = (Element) memberListLookup.evaluate(channelDoc, XPathConstants.NODE);
			} catch (XPathExpressionException ex) {
				throw new IOException("Failed to run member search expression.", ex);
			}
			//Need to use 'createElementNS' as the document does not add it with 'createElement'.
			Element newMember = channelDoc.createElementNS(memberList.getNamespaceURI(), "member");
			newMember.setAttribute("user", Integer.toString(userID));
			newMember.setAttribute("group", Integer.toString(group));
			memberList.appendChild(newMember);
		}
		savePending.add(channelDoc);
	}

	@Override
	public void updateMember(int channelID, int userID, int group) throws IOException {
		Document channelDoc;
		synchronized (docCache) {
			try {
				channelDoc = docCache.get(channelID);
			} catch (ExecutionException ex) {
				throw new IOException("Failed to find data for channel "+channelID+".", ex);
			}
		}
		
		synchronized (channelDoc) {
			Element member;
			try {
				member = (Element) xPath.compile("/csc:channel/csc:members/csc:member[@user="+userID+"]").evaluate(channelDoc, XPathConstants.NODE);
			} catch (XPathExpressionException ex) {
				throw new IOException("Failed to run member search expression.", ex);
			}
			if (member != null) {
				member.setAttribute("group", Integer.toString(group));
			}
		}
		savePending.add(channelDoc);
	}

	@Override
	public void removeMember(int channelID, int userID) throws IOException {
		Document channelDoc;
		synchronized (docCache) {
			try {
				channelDoc = docCache.get(channelID);
			} catch (ExecutionException ex) {
				throw new IOException("Failed to find data for channel "+channelID+".", ex);
			}
		}
		
		if (memberListLookup == null) {
			try {
				memberListLookup = xPath.compile("/csc:channel/csc:members");
			} catch (XPathExpressionException ex) {
				throw new IOException("Failed to compile member lookup expression. This probably indicates a configuration or program error.", ex);
			}
		}
		
		synchronized (channelDoc) {
			Element member;
			try {
				member = (Element) xPath.compile("/csc:channel/csc:members/csc:member[@user="+userID+"]").evaluate(channelDoc, XPathConstants.NODE);
			} catch (XPathExpressionException ex) {
				throw new IOException("Failed to run member search expression.", ex);
			}
			if (member != null) {
				Element memberList;
				try {
					memberList = (Element) memberListLookup.evaluate(channelDoc, XPathConstants.NODE);
				} catch (XPathExpressionException ex) {
					throw new IOException("Failed to run member search expression.", ex);
				}
				memberList.removeChild(member);
			}
		}
		savePending.add(channelDoc);
	}

	@Override
	public void addBan(int channelID, int userID) throws IOException {
		Document channelDoc;
		synchronized (docCache) {
			try {
				channelDoc = docCache.get(channelID);
			} catch (ExecutionException ex) {
				throw new IOException("Failed to find data for channel "+channelID+".", ex);
			}
		}
		
		if (banListLookup == null) {
			try {
				banListLookup = xPath.compile("/csc:channel/csc:bans");
			} catch (XPathExpressionException ex) {
				throw new IOException("Failed to compile ban lookup expression. This probably indicates a configuration or program error.", ex);
			}
		}

		synchronized (channelDoc) {
			Element banList;
			try {
				banList = (Element) banListLookup.evaluate(channelDoc, XPathConstants.NODE);
			} catch (XPathExpressionException ex) {
				throw new IOException("Failed to run ban search expression.", ex);
			}
			//Need to use 'createElementNS' as the document does not add it with 'createElement'.
			Element newBan = channelDoc.createElementNS(banList.getNamespaceURI(), "ban");
			newBan.setAttribute("user", Integer.toString(userID));
			banList.appendChild(newBan);
		}
		savePending.add(channelDoc);

	}

	@Override
	public void removeBan(int channelID, int userID) throws IOException {
		Document channelDoc;
		synchronized (docCache) {
			try {
				channelDoc = docCache.get(channelID);
			} catch (ExecutionException ex) {
				throw new IOException("Failed to find data for channel "+channelID+".", ex);
			}
		}
		
		if (banListLookup == null) {
			try {
				banListLookup = xPath.compile("/csc:channel/csc:bans");
			} catch (XPathExpressionException ex) {
				throw new IOException("Failed to compile ban lookup expression. This probably indicates a configuration or program error.", ex);
			}
		}
		
		synchronized (channelDoc) {
			Element ban;
			try {
				ban = (Element) xPath.compile("/csc:channel/csc:bans/csc:ban[@user="+userID+"]").evaluate(channelDoc, XPathConstants.NODE);
			} catch (XPathExpressionException ex) {
				throw new IOException("Failed to run ban search expression.", ex);
			}
			if (ban != null) {
				Element banList;
				try {
					banList = (Element) banListLookup.evaluate(channelDoc, XPathConstants.NODE);
				} catch (XPathExpressionException ex) {
					throw new IOException("Failed to run ban search expression.", ex);
				}
				banList.removeChild(ban);
			}
		}
		savePending.add(channelDoc);

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
	public void updateDetails(int channelID, ChannelDetails details) throws IOException {
		Document channelDoc;
		synchronized (docCache) {
			try {
				channelDoc = docCache.get(channelID);
			} catch (ExecutionException ex) {
				throw new IOException("Failed to find data for channel "+channelID+".", ex);
			}
		}
		
		try {
			if (nameLookup == null) {
				nameLookup = xPath.compile("/csc:channel/csc:name");
				aliasLookup = xPath.compile("/csc:channel/csc:alias");
				welcomeMessageLookup = xPath.compile("/csc:channel/csc:welcomeMessage");
				descriptionLookup = xPath.compile("/csc:channel/csc:description");
				ownerLookup = xPath.compile("/csc:channel/csc:owner");
			}
		} catch (XPathExpressionException ex) {
			throw new IOException("Failed to compile details lookup expression. This probably indicates a configuration or program error.", ex);
		}
		
		synchronized (channelDoc) {
			try {
				Element nameElement = (Element) nameLookup.evaluate(channelDoc, XPathConstants.NODE);
				nameElement.setTextContent(details.getName());
				
				Element alaisElement = (Element) aliasLookup.evaluate(channelDoc, XPathConstants.NODE);
				alaisElement.setTextContent(details.getAlias());
				
				Element messageElement = (Element) welcomeMessageLookup.evaluate(channelDoc, XPathConstants.NODE);
				messageElement.setTextContent(details.getWelcomeMessage());
				
				Element descriptionElement = (Element) descriptionLookup.evaluate(channelDoc, XPathConstants.NODE);
				descriptionElement.setTextContent(details.getDescription());
				
				Element ownerElement = (Element) ownerLookup.evaluate(channelDoc, XPathConstants.NODE);
				ownerElement.setTextContent(Integer.toString(details.getOwner()));
			} catch (XPathExpressionException ex) {
				throw new IOException("Failed to evaluate details lookup expression. ", ex);
			}
		}
		savePending.add(channelDoc);
	}

	@Override
	public ChannelDetails getChannelDetails(int channelID) throws IOException {
		Document channelDoc;
		synchronized (docCache) {
			try {
				channelDoc = docCache.get(channelID);
			} catch (ExecutionException ex) {
				throw new IOException("Failed to find data for channel "+channelID+".", ex);
			}
		}
		
		try {
			if (nameLookup == null) {
				nameLookup = xPath.compile("/csc:channel/csc:name");
				aliasLookup = xPath.compile("/csc:channel/csc:alias");
				welcomeMessageLookup = xPath.compile("/csc:channel/csc:welcomeMessage");
				descriptionLookup = xPath.compile("/csc:channel/csc:description");
				ownerLookup = xPath.compile("/csc:channel/csc:owner");
			}
		} catch (XPathExpressionException ex) {
			throw new IOException("Failed to compile details lookup expression. This probably indicates a configuration or program error.", ex);
		}
		
		ChannelDetails details = new ChannelDetails();
		synchronized (channelDoc) {
			details.setId(channelID);
			try {
				details.setName(nameLookup.evaluate(channelDoc));
				details.setAlias(aliasLookup.evaluate(channelDoc));
				details.setWelcomeMessage(welcomeMessageLookup.evaluate(channelDoc));
				details.setDescription(descriptionLookup.evaluate(channelDoc));
				details.setOwner(Integer.parseInt(ownerLookup.evaluate(channelDoc)));
			} catch (XPathExpressionException ex) {
				throw new IOException("Failed to evaluate details lookup expression. ", ex);
			}
		}
		return details;
	}

	@Override
	public Set<Integer> getChannelBans(int channelID) throws IOException {
		Document channelDoc;
		synchronized (docCache) {
			try {
				channelDoc = docCache.get(channelID);
			} catch (ExecutionException ex) {
				throw new IOException("Failed to find data for channel "+channelID+".", ex);
			}
		}
		
		if (banLookup == null) {
			try {
				banLookup = xPath.compile("/csc:channel/csc:bans/csc:ban");
			} catch (XPathExpressionException ex) {
				throw new IOException("Failed to compile ban lookup expression. This probably indicates a configuration or program error.", ex);
			}
		}
		Set<Integer> bans = new HashSet<>();
		synchronized (channelDoc) {
			NodeList bansList;
			try {
				bansList = (NodeList) banLookup.evaluate(channelDoc, XPathConstants.NODESET);
			} catch (XPathExpressionException ex) {
				throw new IOException("Failed to evaluate ban lookup expression.", ex);
			}
			for (int i=0;i<bansList.getLength();i++) {
				Node banNode = bansList.item(i);
				if (banNode instanceof Element) {
					Element banElement = (Element) banNode;
					bans.add(Integer.parseInt(banElement.getAttribute("user")));
				}
			}
		}
		return bans;
	}

	@Override
	public Map<Integer, Integer> getChannelMembers(int channelID) throws IOException {
		Document channelDoc;
		synchronized (docCache) {
			try {
				channelDoc = docCache.get(channelID);
			} catch (ExecutionException ex) {
				throw new IOException("Failed to find data for channel "+channelID+".", ex);
			}
		}
		
		if (memberLookup == null) {
			try {
				memberLookup = xPath.compile("/csc:channel/csc:members/csc:member");
			} catch (XPathExpressionException ex) {
				throw new IOException("Failed to compile member lookup expression. This probably indicates a configuration or program error.", ex);
			}
		}
		
		Map<Integer, Integer> members = new HashMap<>();
		synchronized (channelDoc) {
			NodeList membersList;
			try {
				membersList = (NodeList) memberLookup.evaluate(channelDoc, XPathConstants.NODESET);
			} catch (XPathExpressionException ex) {
				throw new IOException("Failed to evaluate member lookup expression.", ex);
			}
			for (int i=0;i<membersList.getLength();i++) {
				Node memberNode = membersList.item(i);
				if (memberNode instanceof Element) {
					Element memberElement = (Element) memberNode;
					int user = Integer.parseInt(memberElement.getAttribute("user"));
					int group = Integer.parseInt(memberElement.getAttribute("group"));
					members.put(user, group);
				}
			}
		}
		return members;
	}

	@Override
	public Set<ChannelGroupData> getChannelGroups(int channelID) throws IOException {
		Document channelDoc;
		synchronized (docCache) {
			try {
				channelDoc = docCache.get(channelID);
			} catch (ExecutionException ex) {
				throw new IOException("Failed to find data for channel "+channelID+".", ex);
			}
		}
		
		if (groupLookup == null) {
			try {
				groupLookup = xPath.compile("/csc:channel/csc:groups/csc:group");
			} catch (XPathExpressionException ex) {
				throw new IOException("Failed to compile group lookup expression. This probably indicates a configuration or program error.", ex);
			}
		}
		
		Set<ChannelGroupData> groups = new HashSet<>();
		synchronized (channelDoc) {
			NodeList groupsList;
			try {
				groupsList = (NodeList) groupLookup.evaluate(channelDoc, XPathConstants.NODESET);
			} catch (XPathExpressionException ex) {
				throw new IOException("Failed to evaluate group lookup expression.", ex);
			}
			for (int i=0;i<groupsList.getLength();i++) {
				Node groupNode = groupsList.item(i);
				if (groupNode instanceof Element) {
					Element groupElement = (Element) groupNode;
					int groupID = Integer.parseInt(groupElement.getAttribute("id"));
					ChannelGroupData groupData = new ChannelGroupData(groupID, channelID);
					groupData.setName(groupElement.getElementsByTagName("name").item(0).getTextContent());
					groupData.setType(ChannelGroupType.getByName(groupElement.getElementsByTagName("type").item(0).getTextContent()));
					
					groups.add(groupData);
				}
			}
		}
		return groups;
	}

	@Override
	public void commitChanges() throws IOException {
		synchronized (savePending) {
			for (Document channelDoc : savePending) {
				saveChannelDoc(channelDoc);
			}
			savePending.clear();
		}
	}

	@Override
	public void close() throws Exception {
		commitChanges();//Commit any pending changes before shutting down.
	}

	@Override
	public int createChannel(ChannelDetails details) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void removeChannel(int channelID) throws IOException {
		docCache.invalidate(channelID);
		if (!new File(""+channelID+".xml").delete()) {
			throw new IOException("Unable to remove channel xml file.");
		}			
	}

}
