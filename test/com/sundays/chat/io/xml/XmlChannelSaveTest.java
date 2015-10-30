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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.log4j.BasicConfigurator;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.io.Files;
import com.sundays.chat.io.ChannelDetails;
import com.sundays.chat.server.channel.ChannelGroup;

public class XmlChannelSaveTest {
	
	private static File channelsDir = Files.createTempDir();
	private static File channelSchema = new File("WebContent/WEB-INF/xsd/channel.xsd");
	private static File testData = new File("resources/testcase.xml");

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		BasicConfigurator.configure();//Initialise logging
	}
	
	private File xmlFile;
	
	protected XmlChannelSave saveTest;

	@Before
	public void setUp() throws Exception {
		xmlFile = new File(channelsDir, "100.xml");
		Files.copy(testData, xmlFile);
		saveTest = new XmlChannelSave(channelsDir, channelSchema);
	}

	@After
	public void tearDown() throws Exception {
		xmlFile.delete();
		saveTest = null;
	}
	
	@Test
	public void testFileSave () throws Exception {
		ChannelDetails details = new ChannelDetails();
		details.setId(100);
		details.setName("Name 2");
		details.setAlias("N 2");
		details.setWelcomeMessage("This is a new message...");
		details.setDescription("A new description!");
		details.setOwner(101);
		saveTest.updateDetails(100, details);
		
		saveTest.commitChanges();//Apply the change
		
		//Remove any cached channel data by creating a new instance
		saveTest = new XmlChannelSave(channelsDir, channelSchema);
		
		details = saveTest.getChannelDetails(100);
		assertEquals(100, details.getId());
		assertEquals("Name 2", details.getName());
		assertEquals("N 2", details.getAlias());
		assertEquals("This is a new message...", details.getWelcomeMessage());
		assertEquals("A new description!", details.getDescription());
		assertEquals(101, details.getOwner());
		
	}

	@Test
	public void testLoadDetails() throws IOException {
		ChannelDetails details = saveTest.getChannelDetails(100);
		assertEquals(100, details.getId());
		assertEquals("Test Channel", details.getName());
		assertEquals("T C", details.getAlias());
		assertEquals("Welcome to the Test Channel!", details.getWelcomeMessage());
		assertEquals("The testing channel for this server. Used for ensuring channel features work correctly.", details.getDescription());
		assertEquals(100, details.getOwner());
	}

	@Test
	public void testUpdateDetails() throws IOException {
		ChannelDetails details = new ChannelDetails();
		details.setId(100);
		details.setName("Name 2");
		details.setAlias("N 2");
		details.setWelcomeMessage("This is a new message...");
		details.setDescription("A new description!");
		details.setOwner(101);
		saveTest.updateDetails(100, details);
		
		details = saveTest.getChannelDetails(100);
		assertEquals(100, details.getId());
		assertEquals("Name 2", details.getName());
		assertEquals("N 2", details.getAlias());
		assertEquals("This is a new message...", details.getWelcomeMessage());
		assertEquals("A new description!", details.getDescription());
		assertEquals(101, details.getOwner());
	}

	@Test
	public void testLoadBans() throws IOException {
		List<Integer> bans = saveTest.getChannelBans(100);
		assertEquals(2, bans.size());
		assertEquals(103, bans.get(0).intValue());
		assertEquals(107, bans.get(1).intValue());		
	}

	@Test
	public void testLoadMembers() throws IOException {
		Map<Integer, Integer> members = saveTest.getChannelMembers(100);
		assertEquals(2, members.size());
		assertTrue(members.containsKey(100));
		assertEquals(11, members.get(100).byteValue());
		assertTrue(members.containsKey(101));
		assertEquals(9, members.get(101).byteValue());		
	}
	
	@Test
	public void testAddMember() throws IOException {
		saveTest.addMember(100, 109, ChannelGroup.DEFAULT_GROUP);
		Map<Integer, Integer> members = saveTest.getChannelMembers(100);
		assertTrue(members.containsKey(109));
		assertEquals(ChannelGroup.DEFAULT_GROUP, members.get(109).byteValue());
	}
	
	@Test
	public void testUpdateMember() throws IOException {
		Map<Integer, Integer> members = saveTest.getChannelMembers(100);
		assumeTrue(members.containsKey(101));//Since this behavior is only defined for existing members, we should assume they exist first. 
		saveTest.updateMember(100, 101, 5);
		
		members = saveTest.getChannelMembers(100);
		assertTrue(members.containsKey(101));
		assertEquals(5, members.get(101).byteValue());
	}
	
	@Test
	public void testRemoveMember() throws IOException {
		Map<Integer, Integer> members = saveTest.getChannelMembers(100);
		assumeTrue(members.containsKey(101));
		saveTest.removeMember(100, 101);
		
		members = saveTest.getChannelMembers(100);
		assertFalse(members.containsKey(101));
	}
	
	@Test
	public void testAddBan() throws IOException {
		saveTest.addBan(100, 110);
		List<Integer> bans = saveTest.getChannelBans(100);
		assertTrue(bans.contains(Integer.valueOf(110)));
	}
	
	@Test
	public void testRemoveBan() throws IOException {
		List<Integer> bans = saveTest.getChannelBans(100);
		assertTrue(bans.contains(Integer.valueOf(103)));
		saveTest.removeBan(100, 103);
		
		bans = saveTest.getChannelBans(100);
		assertFalse(bans.contains(Integer.valueOf(103)));
	}
	
	@Test(expected=IOException.class)
	public void testRemoveChannel() throws IOException {
		try {
			saveTest.getChannelDetails(100);//If the channel doesn't exist, this will throw an exception.
		} catch (IOException ex) {
			fail("Channel does not exist!");
		}
		saveTest.removeChannel(100);
		saveTest.getChannelDetails(100);
	}

}
