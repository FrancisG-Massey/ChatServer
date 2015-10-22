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

import org.junit.After;
import org.junit.Before;

import com.google.common.io.Files;
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

}
