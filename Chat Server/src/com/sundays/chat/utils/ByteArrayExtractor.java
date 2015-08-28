/*******************************************************************************
 * Copyright (c) 2015 Francis G.
 *
 * This file is part of ChatServer.
 *
 * ChatServer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
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
package com.sundays.chat.utils;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;

/**
 *
 * @author francis
 */
public class ByteArrayExtractor {
	
    private final DataInputStream dataIn;
    
    public ByteArrayExtractor (byte[] input) throws IOException {
        dataIn = new DataInputStream(new ByteArrayInputStream(input));
    }
    
    public int getInt () throws IOException {
        return dataIn.readInt();
    }
    
    @Deprecated
    public String getStringOld (int length) throws IndexOutOfBoundsException, IOException {
        byte[] stringData = new byte[length];
        dataIn.read(stringData, 0, length);
        return new String(stringData, "UTF-8");
    }
    
    public String getUTFString () throws IOException, EOFException {
        return dataIn.readUTF();
    }
    
    public byte getByte () throws IOException, EOFException {
        return dataIn.readByte();         
    }
    
    public short getShort () throws IOException, EOFException {
        return dataIn.readShort();
    }
}
