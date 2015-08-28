package com.sundays.chat.interfaces;

public class ChannelGroupData {
	
	public final int groupID, channelID;
	public String groupName;
	public String permissions;
	public String type;

	public String groupIconUrl;
	public int overrides;
	
	public ChannelGroupData (int channelID, int groupID, String groupName, String permissions, 
			String type, String groupIconUrl) {
		this.channelID = channelID;
		this.groupID = groupID;
		this.groupName = groupName;
		this.permissions = permissions;
		this.type = type;
		this.groupIconUrl = groupIconUrl;
	}
	
	public ChannelGroupData overrides (int overrides) {
		this.overrides = overrides;
		return this;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ChannelGroupData [groupID=" + groupID + ", channelID="
				+ channelID + ", groupName=" + groupName + ", permissions="
				+ permissions + ", type=" + type + ", groupIconUrl="
				+ groupIconUrl + ", overrides=" + overrides + "]";
	}
}
