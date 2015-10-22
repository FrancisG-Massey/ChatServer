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

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.io.Files;
import com.sundays.chat.io.ChannelDetails;
import com.sundays.chat.io.ChannelSaveTest;

public class XmlChannelSaveTest extends ChannelSaveTest {
	
	private static File channelsDir = Files.createTempDir();
	private static File channelSchema = new File("WebContent/WEB-INF/xsd/channel.xsd");
	private static File testData = new File("resources/testcase.xml");
	
	private File xmlFile;

	@Before
	public void setUp() throws Exception {
		xmlFile = new File(channelsDir, "100.xml");
		Files.copy(testData, xmlFile);
		System.out.println();
		saveTest = new XmlChannelManager(channelsDir, channelSchema);
	}

	@After
	public void tearDown() throws Exception {
		xmlFile.delete();
		saveTest = null;
	}
	
	@Test
	public void testFileSave () throws Exception {
		ChannelDetails details = new ChannelDetails();
		details.id = 100;
		details.name = "Name 2";
		details.abbreviation = "N 2";
		details.openingMessage = "This is a new message...";
		details.owner = 101;
		saveTest.syncDetails(100, details);
		
		saveTest.commitChanges();//Apply the change
		
		//Remove any cached channel data by creating a new instance
		saveTest = new XmlChannelManager(channelsDir, channelSchema);
		
		details = saveTest.getChannelDetails(100);
		assertEquals(100, details.id);
		assertEquals("Name 2", details.name);
		assertEquals("N 2", details.abbreviation);	
		assertEquals("This is a new message...", details.openingMessage);
		assertEquals(101, details.owner);
		
	}

}
