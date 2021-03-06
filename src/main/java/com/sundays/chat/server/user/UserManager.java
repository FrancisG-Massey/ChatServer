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
package com.sundays.chat.server.user;

import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import org.jasypt.digest.StandardStringDigester;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.sundays.chat.io.GuestDetails;
import com.sundays.chat.io.UserDataIO;
import com.sundays.chat.io.UserDetails;
import com.sundays.chat.server.Launcher;
import com.sundays.chat.server.channel.ChannelUser;

/**
 *
 * @author Francis
 */
public final class UserManager implements UserLookup {    
	
	private static final Logger logger = LoggerFactory.getLogger(UserManager.class);

	private final Map<String, Integer> lookupByUsername = new ConcurrentHashMap<String, Integer>();
	
	private final LoadingCache<Integer, UserDetails> userLookup = CacheBuilder.newBuilder().build(new CacheLoader<Integer, UserDetails>() {

		@Override
		public UserDetails load(Integer userID) throws Exception {
			UserDetails details = userIO.getUserDetails(userID);
			if (details == null) {
				throw new UserNotFoundException("User not found: "+userID);
			}
			lookupByUsername.put(details.getUsername(), userID);
			return details;
		}
		
	});
	
	private final Map<Integer, User> connectedUsers = new ConcurrentHashMap<Integer, User>();
	private final Map<String, User> userSessions = new ConcurrentHashMap<String, User>();
	private final Map<Integer, String> cachedUsernames = new ConcurrentHashMap<Integer, String>();
	private PublicKey encryptKey = null;
    private PrivateKey decryptKey = null;
    private int nextGuestID = -100;//Decrementing integer for guest user ID (all guest IDs are less than -100)
    private final UserDataIO userIO;
    private final Launcher server;
    
    public UserManager (Launcher server) {
    	logger.info("Starting user manager...");
        this.userIO = server.getIO().getUserIO();
        this.server = server;
    	/*
    	 * Use this space for preparing the user management system, such as loading a cache of usernames
    	 */
        /*
        try {
            generateEncryptKeys();
        } catch (NoSuchAlgorithmException ex) {
            System.err.println("Failed to generate login encryption keys: "+ex.getMessage());
        }*/
    }
    
    @Override
    public ChannelUser getUser (int userID) {
    	return connectedUsers.get(userID);
    }
    
    public User getUserSession (String sessionID) {
    	//Returns the User object for a given user session ID
    	if (sessionID == null) {
    		return null;
    	}
    	return userSessions.get(sessionID);
    }
    
    @Override
    public String getUsername (int userID) {
        String un = cachedUsernames.get(userID);
        if (un == null) {
            try {
            	UserDetails details = userIO.getUserDetails(userID);
            	if (details != null) {
            		un = details.getUsername();
            	}
            } catch (IOException ex) {
            	logger.error("Failed to fetch username for user ID "+userID, ex);
            }
            if (un != null) {
            	cachedUsernames.put(userID, un);
                lookupByUsername.put(un.toLowerCase(), userID);
            }
        }
        return un;
    }    

    @Override
	public int getUserID (String username) { 
        int userID = -1;
		if (lookupByUsername.containsKey(username.toLowerCase())) {
			return lookupByUsername.get(username.toLowerCase());
		}
		try {
			userID = userIO.lookupByUsername(username);
		} catch (IOException ex) {
			logger.error("Failed to fetch user ID for username "+username, ex);
		}
		if (userID != -1) {
			cachedUsernames.put(userID, username);
	        lookupByUsername.put(username.toLowerCase(), userID);
		}
    	
        return userID;
    }
    
    public JSONObject manageLogin (String type, JSONObject data) throws JSONException {
    	JSONObject response = new JSONObject();
    	User user;
    	if ("standard".equalsIgnoreCase(type)) {
    		String un = data.getString("username");
    		if (un.equalsIgnoreCase("guest")) {
    			//Use the guest account, with a randomly created user ID
    			user = new User(nextGuestID, new GuestDetails(nextGuestID));
    			nextGuestID--;
    		} else {
    			//Retrieves the user's account
    			int userID = getUserID(un);
    			if (userID == -1) {
    				response.put("status", 404);
    	    		response.put("message", "Username and/or password incorrect.");
    	    		return response;
    			}
    			UserDetails userDetails;
    			try {
					userDetails = userLookup.get(userID);
				} catch (ExecutionException ex) {
					logger.error("Failed to fetch details for user "+userID, ex);
					response.put("status", 500);
		    		response.put("message", "Login failed due to a server error");
		    		return response;
				}
    			if (!passwordCheck(userDetails.getHashedPassword(), data.getString("password").toCharArray())) {
    				response.put("status", 404);
    	    		response.put("message", "Username and/or password incorrect.");
    	    		return response;
    			}
    			user = new User(userID, userDetails);
    			if (connectedUsers.containsKey(user.getId())) {
    				//If the user is already logged in, forcefully log out the user
    				manageLogout(user.getId());  				
    				/*response.put("status", 409);
    				response.put("message", "Your account is already logged into this server.");
    				return response;*/
                }
    		}    		
    	} else {
    		response.put("status", 501);
    		response.put("message", "Invalid login type: "+type);
    		return response;
    	}
    	//Generate the session ID
    	String sessionID;
    	while (true) {
    		SecureRandom random = new SecureRandom();
    		sessionID = new BigInteger(130, random).toString(32);
    		if (userSessions.containsKey(sessionID)) {
    			//If the generated session ID is already in use, generate a new one (should almost never happen, but just to be safe...)
    			continue;
    		} else {
    			//Otherwise, break out of the loop
    			break;
    		}
    	}    	
    	user.setSessionID(sessionID);
    	userSessions.put(sessionID, user);
    	connectedUsers.put(user.getId(), user);
        /*if (u.defaultChannel != 0) {
            ChatServer.getInstance().channelAPI().joinChannel(u, u.defaultChannel);
        	response.put("defaultChannelRank", u.getChannel().getUserRank(u));
        	response.put("defaultChannelDetails", ChatServer.getInstance().channelAPI().getChannelDetails(u.getChannel().channelID));
        }*/
    	cachedUsernames.put(user.getId(), user.getName());
        lookupByUsername.put(user.getName().toLowerCase(), user.getId());
    	response.put("status", 200);
    	response.put("session", sessionID);
    	response.put("userID", user.getId());
    	response.put("username", user.getName());
    	response.put("defaultChannel", user.getDefaultChannel());
		return response;    	
    }
    
    public void manageLogout (int userId) {
    	User user = connectedUsers.get(userId);
    	if (user == null) {
    		//User has already logged out
    		return;
    	}
    	userSessions.remove(user.getSessionID());//Removes the user from the session map
    	connectedUsers.remove(userId);//Removes the user from the userID map
    	user.connected = false;//Sets the status of the user to disconnected (just in case some other thread tries to send data to the user)
        int currentChannel = 0;
        if (user.getChannelId() != -1) {
        	//If the user is in a channel, make them leave it
            currentChannel = user.getChannelId();
            server.getChannelManager().leave(user, currentChannel);
        }
        if (user.getDefaultChannel() != -1 && user.getDefaultChannel() != currentChannel) {
            /*
             * If the user is currently in a channel, and their current channel differs from their default channel,
             * set the user's default channel to the last one they were in (so they will automatically join it when they next connect)
             */
        	try {
        		UserDetails details = new UserDetails();
        		details.setUserID(user.getId());
        		details.setUsername(user.getName());
        		details.setDefaultChannel(currentChannel);
        		userIO.saveUserData(details);
        	} catch (IOException ex) {
        		logger.error("Failed to save details for user "+user, ex);
        	}      	
        }
    }
    
    public JSONObject managedAccountCreate (String type, JSONObject data, String userIP) throws JSONException {
    	JSONObject response = new JSONObject();
    	if ("standard".equalsIgnoreCase(type)) {
    		String username = data.getString("username");
    		String	ln = data.getString("loginName");
    		String password = digestPassword(data.getString("password"));
    		if (getUserID(username) != -1) {
    			response.put("status", 409);//Username already registered
    			response.put("message", "An account already exists with the username: "+username+". Please use a different name.");
    		} else {
				try {
					int userID = userIO.createUser(username, password.toCharArray());
                	response.put("status", 200);//Account created successfully
                	response.put("message", "Your account has been created successfully. You can log in from now on using the name: "+ln);
                	response.put("userid", userID);//Gets the user id
				} catch (IOException ex) {
					response.put("status", 500);//Account not created due to unknown error
                	response.put("message", "Your account could not be created due to a system error. Please try again later.");
                	logger.error("Failed to create new user "+ln, ex);
				}
    		}
    	}
		return response;
    }
    
    public void generateEncryptKeys () throws NoSuchAlgorithmException {
        KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance("RSA");
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        keyGenerator.initialize(1024, random);
        KeyPair pair = keyGenerator.generateKeyPair();
        encryptKey = pair.getPublic();
        decryptKey = pair.getPrivate();
    }
    
    /*public void sendPublicKey () throws IOException {
        //userThread.sendObject(TransportProtocol.ENCRYPTKEY, encryptKey);
    }*/
    
    /*private String decryptPassword (char[] p) {
        //System.out.println("Encrypted password: "+new String(p));
        Cipher cipher;
        byte[] encryptCode = new Base64().decode(new String(p).getBytes());
        String password = null;
        try {
            cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, decryptKey);
            byte[] cipherData = cipher.doFinal(encryptCode);
            password = new String(cipherData);
        } catch (Exception ex) {//Lots of exceptions; compressed into 'Exception' class
            System.err.println("Failed to decrypt password: "+ex.getMessage());
        }
        //System.out.println("Decrypted password: "+password+"("+password.length()+" characters long)");
        return password;
    }*/
    
    private String digestPassword (String password) {
        StandardStringDigester pEncrypt = new StandardStringDigester();
        return pEncrypt.digest(password);
    }
    
    private boolean passwordCheck (char[] dbPassword, char[] password) {
        StandardStringDigester pwCheck = new StandardStringDigester();
        return pwCheck.matches(String.valueOf(password), String.valueOf(dbPassword));
    }
    
    public Boolean keysExist () {
        if (encryptKey != null && decryptKey != null) {
            return true;
        }
        return false;
    }
}
