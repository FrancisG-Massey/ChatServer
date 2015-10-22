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
package com.sundays.chat.io;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

import java.util.List;
import java.util.Map;

import org.apache.log4j.BasicConfigurator;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sundays.chat.server.Settings;

public abstract class ChannelSaveTest {
	
	protected ChannelDataManager saveTest;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		BasicConfigurator.configure();//Initialise logging
	}

	@Before
	public abstract void setUp() throws Exception;

	@After
	public abstract void tearDown() throws Exception;

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
	
	@Test
	public void testAddMember() {
		saveTest.addRank(100, 109);
		Map<Integer, Byte> members = saveTest.getChannelRanks(100);
		System.out.println(members);
		assertTrue(members.containsKey(109));
		assertEquals(Settings.DEFAULT_RANK, members.get(109).byteValue());
	}
	
	@Test
	public void testUpdateMember() {
		Map<Integer, Byte> members = saveTest.getChannelRanks(100);
		assumeTrue(members.containsKey(101));//Since this behavior is only defined for existing members, we should assume they exist first. 
		saveTest.changeRank(100, 101, (byte) 5);
		
		members = saveTest.getChannelRanks(100);
		assertTrue(members.containsKey(101));
		assertEquals(5, members.get(101).byteValue());
	}
	
	@Test
	public void testRemoveMember() {
		Map<Integer, Byte> members = saveTest.getChannelRanks(100);
		assumeTrue(members.containsKey(101));
		saveTest.removeRank(100, 101);
		
		members = saveTest.getChannelRanks(100);
		assertFalse(members.containsKey(101));
	}

}
