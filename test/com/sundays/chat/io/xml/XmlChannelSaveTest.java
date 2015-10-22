package com.sundays.chat.io.xml;

import static org.junit.Assert.*;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.apache.log4j.BasicConfigurator;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.io.Files;

public class XmlChannelSaveTest {
	
	private static File channelsDir = Files.createTempDir();
	private static File channelSchema = new File("WebContent/WEB-INF/xsd/channel.xsd");
	private static File testData = new File("resources/testcase.xml");
		
	@BeforeClass
	public static void setUpClass () throws Exception {
		BasicConfigurator.configure();//Initialise logging
	}
	
	private XmlChannelManager saveTest;
	private File xmlFile;

	@Before
	public void setUp() throws Exception {
		xmlFile = new File(channelsDir, "100.xml");
		Files.copy(testData, xmlFile);
		
		saveTest = new XmlChannelManager(channelsDir, channelSchema);
	}

	@After
	public void tearDown() throws Exception {
		xmlFile.delete();
		saveTest = null;
	}

	@Test
	public void testLoadBans() {
		List<Integer> bans = saveTest.getChannelBans(100);
		assertEquals(2, bans.size());
		assertEquals(103, bans.get(0).intValue());
		assertEquals(107, bans.get(1).intValue());		
	}

	@Test
	public void testLoadMembers() {
		Map<Integer, Byte> members = saveTest.getChannelRanks(100);
		assertEquals(2, members.size());
		assertTrue(members.containsKey(100));
		assertEquals(11, members.get(100).byteValue());
		assertTrue(members.containsKey(101));
		assertEquals(9, members.get(101).byteValue());		
	}

}
