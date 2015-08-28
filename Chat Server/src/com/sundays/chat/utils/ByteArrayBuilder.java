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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 *
 * @author francis
 */
public class ByteArrayBuilder {
    
    private ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
    private DataOutputStream dataOut = new DataOutputStream(byteArray);
    
    @Deprecated
    public ByteArrayBuilder (int typeCode) {
        byteArray.write(typeCode);
    }
    
    public ByteArrayBuilder () {
    	
    }
    
    @Deprecated
    public void writeStringOld (String s) throws IOException {        
        dataOut.writeBytes(s);
    }
    
    public void writeUTFString (String s) throws IOException {
        dataOut.writeUTF(s);
    }
    
    public void writeInt (int i) throws IOException {
        dataOut.writeInt(i);
    }
    
    public void writeByte (byte b) throws IOException {
        dataOut.writeByte(b);
    }    
    
    public void writeBytes (byte[] b) throws IOException {
        dataOut.write(b);
    }
    
    public void writeShort (short s) throws IOException {
        dataOut.writeShort(s);
    }
    
    public String getString () {
        try {
            dataOut.close();
            byteArray.flush();
        } catch (Exception e) {
            
        }        
        return this.byteArray.toString();
    }
    
    public ByteArrayOutputStream getByteArrayStream () {
        return this.byteArray;
    }
    
    public byte[] getByteArray () {
        try {
            dataOut.close();
            byteArray.flush();
        } catch (Exception e) {
            
        }        
        return this.byteArray.toByteArray();
    }
}
