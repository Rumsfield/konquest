package konquest.utility;


import org.bukkit.ChatColor;

public enum MessageStatic {

	PLUGIN_TAG				("[KC]"),
	INVALID_PARAMETERS		("The command did not have valid parameters. Use /k help"),
	INVALID_WORLD			("The command cannot be used in this world."),
	PLAYER_OFFLINE			("%player% is not online."),
	NO_PERMISSION			("You don't have permission to do that."),
	JOIN_INVALID			("You cannot join that."),
	JOIN_VALID				("You have joined %kingdom%"),
	COMMAND_INVALID			("That command is not valid."),
	BAD_NAME				("Bad name, try another."),
	PLAYER_DATA_NONEXISTENT	("Plugin data for %player% does not exist.");
	
	private String message;
	
	MessageStatic(String message) {
		this.message = message;
	}
	
	public String toString() {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
	
}
