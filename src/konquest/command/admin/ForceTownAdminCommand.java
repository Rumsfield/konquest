package konquest.command.admin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import konquest.Konquest;
import konquest.command.CommandBase;
import konquest.model.KonKingdom;
import konquest.model.KonOfflinePlayer;
import konquest.model.KonPlayer;
import konquest.model.KonTown;
import konquest.model.KonUpgrade;
import konquest.utility.ChatUtil;
import konquest.utility.MessageStatic;

public class ForceTownAdminCommand extends CommandBase {
	
	public ForceTownAdminCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }

	@Override
	public void execute() {
		
		// k admin forcetown <name> open|close|add|kick|knight|lord|rename|upgrade [arg1] [arg2]
		if (getArgs().length != 4 && getArgs().length != 5 && getArgs().length != 6) {
            ChatUtil.sendError((Player) getSender(), MessageStatic.INVALID_PARAMETERS.toString());
            return;
        } else {
        	String townName = getArgs()[2];
        	String subCmd = getArgs()[3];

        	// Verify town exists
    		KonTown town = null;
    		boolean townExists = false;
    		for(KonKingdom kingdom : getKonquest().getKingdomManager().getKingdoms()) {
    			if(kingdom.hasTown(townName)) {
    				town = kingdom.getTown(townName);
            		townExists = true;
            	}
    		}
    		if(!townExists) {
    			ChatUtil.sendError((Player) getSender(), "No such Town");
    			return;
    		}

        	// Action based on sub-command
    		String playerName = "";
        	String newTownName = "";
        	String upgradeName = "";
        	switch(subCmd.toLowerCase()) {
        	case "open":
            	if(town.isOpen()) {
            		ChatUtil.sendNotice((Player) getSender(), townName+" is already open for access by all players");
            	} else {
            		town.setIsOpen(true);
            		ChatUtil.sendNotice((Player) getSender(), "Opened "+townName+" for access by all players");
            	}
        		break;
        	case "close":
            	if(town.isOpen()) {
            		town.setIsOpen(false);
            		ChatUtil.sendNotice((Player) getSender(), "Closed "+townName+", only residents may build and access containers");
            	} else {
            		ChatUtil.sendNotice((Player) getSender(), townName+" is already closed for residents only");
            	}
        		break;
			case "add":
				if (getArgs().length == 5) {
					playerName = getArgs()[4];
				}
			    if(!playerName.equalsIgnoreCase("")) {
			    	KonOfflinePlayer offlinePlayer = getKonquest().getPlayerManager().getAllPlayerFromName(playerName);
			    	if(offlinePlayer == null) {
						ChatUtil.sendError((Player) getSender(), "Invalid player name!");
		        		return;
					}
			    	if(!offlinePlayer.getKingdom().equals(town.getKingdom())) {
			    		ChatUtil.sendError((Player) getSender(), "That player is an enemy!");
		        		return;
			    	}
			    	// Add the player as a resident
			    	if(town.addPlayerResident(offlinePlayer.getOfflineBukkitPlayer(),false)) {
			    		ChatUtil.sendNotice((Player) getSender(), "Added "+playerName+" as a resident of "+townName);
			    	} else {
			    		ChatUtil.sendError((Player) getSender(), playerName+" is already a resident of "+townName);
			    	}
			    } else {
			    	ChatUtil.sendError((Player) getSender(), "Must provide a player name!");
	        		return;
			    }
			    break;
			case "kick":
				if (getArgs().length == 5) {
					playerName = getArgs()[4];
				}
				if(!playerName.equalsIgnoreCase("")) {
					KonOfflinePlayer offlinePlayer = getKonquest().getPlayerManager().getAllPlayerFromName(playerName);
					if(offlinePlayer == null) {
						ChatUtil.sendError((Player) getSender(), "Invalid player name!");
		        		return;
					}
			    	// Remove the player as a resident
			    	if(town.removePlayerResident(offlinePlayer.getOfflineBukkitPlayer())) {
			    		ChatUtil.sendNotice((Player) getSender(), "Removed resident "+playerName+" from "+townName);
			    	} else {
			    		ChatUtil.sendError((Player) getSender(), playerName+" is not a resident of "+townName);
			    	}
			    } else {
			    	ChatUtil.sendError((Player) getSender(), "Must provide a player name!");
	        		return;
			    }
				break;
			case "lord":
				if (getArgs().length == 5) {
					playerName = getArgs()[4];
				}
				if(!playerName.equalsIgnoreCase("")) {
        			// Give lordship
        			KonOfflinePlayer offlinePlayer = getKonquest().getPlayerManager().getAllPlayerFromName(playerName);
        			if(offlinePlayer == null) {
						ChatUtil.sendError((Player) getSender(), "Invalid player name!");
		        		return;
					}
        			if(!offlinePlayer.getKingdom().equals(town.getKingdom())) {
			    		ChatUtil.sendError((Player) getSender(), "That player is an enemy!");
		        		return;
			    	}
        			town.setPlayerLord(offlinePlayer.getOfflineBukkitPlayer());
        			ChatUtil.sendNotice((Player) getSender(), "Gave Lordship of "+townName+" to "+playerName);
			    } else {
			    	ChatUtil.sendError((Player) getSender(), "Must provide a player name!");
	        		return;
			    }
				break;
			case "knight":
				if (getArgs().length == 5) {
					playerName = getArgs()[4];
				}
			    if(!playerName.equalsIgnoreCase("")) {
			    	KonOfflinePlayer offlinePlayer = getKonquest().getPlayerManager().getAllPlayerFromName(playerName);
			    	if(offlinePlayer == null) {
						ChatUtil.sendError((Player) getSender(), "Invalid player name!");
		        		return;
					}
			    	if(!offlinePlayer.getKingdom().equals(town.getKingdom())) {
			    		ChatUtil.sendError((Player) getSender(), "That player is an enemy!");
		        		return;
			    	}
			    	// Set resident's elite status
			    	if(town.isPlayerResident(offlinePlayer.getOfflineBukkitPlayer())) {
			    		if(town.isPlayerElite(offlinePlayer.getOfflineBukkitPlayer())) {
			    			// Clear elite
			    			town.setPlayerElite(offlinePlayer.getOfflineBukkitPlayer(), false);
			    			ChatUtil.sendNotice((Player) getSender(), playerName+" is no longer a Knight in "+townName+". Use this command again to set Knight status.");
			    		} else {
			    			// Set elite
			    			town.setPlayerElite(offlinePlayer.getOfflineBukkitPlayer(), true);
			    			ChatUtil.sendNotice((Player) getSender(), playerName+" is now a Knight resident in "+townName+". Use this command again to remove Knight status.");
			    		}
			    	} else {
			    		ChatUtil.sendError((Player) getSender(), "Knights must be added as residents first!");
			    	}
			    } else {
			    	ChatUtil.sendError((Player) getSender(), "Must provide a player name!");
	        		return;
			    }
			    break;
			case "rename":
	        	if (getArgs().length == 5) {
					newTownName = getArgs()[4];
				}
	        	if(!newTownName.equalsIgnoreCase("")) {
	        		// Rename the town
	        		boolean success = town.getKingdom().renameTown(townName, newTownName);
	        		if(success) {
	        			ChatUtil.sendNotice((Player) getSender(), "Changed town name "+townName+" to "+newTownName);
	        		}
	        	} else {
	        		ChatUtil.sendError((Player) getSender(), "Must provide a new Town name!");
	        		return;
	        	}
	        	break;
			case "upgrade":
	        	String upgradeLevelStr = "";
				if (getArgs().length == 6) {
					upgradeName = getArgs()[4];
					upgradeLevelStr = getArgs()[5];
				}
	        	if(!upgradeName.equalsIgnoreCase("")) {
	        		KonUpgrade upgrade = KonUpgrade.getUpgrade(upgradeName);
	        		if(upgrade == null) {
	        			ChatUtil.sendError((Player) getSender(), "Invalid upgrade name!");
		        		return;
	        		}
	        		if(!upgradeLevelStr.equalsIgnoreCase("")) {
	        			int upgradeLevel = 0;
	        			try {
	        				upgradeLevel = Integer.parseInt(upgradeLevelStr);
	        			} 
	        			catch(NumberFormatException e) {
	        				ChatUtil.printDebug("Failed to parse string as int: "+e.getMessage());
	        				ChatUtil.sendError((Player) getSender(), "Invalid upgrade level!");
			        		return;
	        			}
	        			if(upgradeLevel < 0 || upgradeLevel > upgrade.getMaxLevel()) {
	        				ChatUtil.sendError((Player) getSender(), "Invalid upgrade level!");
			        		return;
	        			}
	        			// Set town upgrade and level
	        			getKonquest().getUpgradeManager().forceTownUpgrade(town, upgrade, upgradeLevel, (Player)getSender());
	        		} else {
	        			ChatUtil.sendError((Player) getSender(), "Must provide an upgrade level!");
		        		return;
	        		}
	        	} else {
	        		ChatUtil.sendError((Player) getSender(), "Must provide an upgrade name!");
	        		return;
	        	}
	        	break;
        	default:
        		ChatUtil.sendError((Player) getSender(), "Invalid sub-command, expected open|close|add|kick|knight|lord|rename|upgrade");
        		return;
        	}
        	
        }
		
	}

	@Override
	public List<String> tabComplete() {
		// k admin forcetown <name> open|close|add|remove|lord|elite [arg1] [arg2]
		List<String> tabList = new ArrayList<>();
		final List<String> matchedTabList = new ArrayList<>();
		if(getArgs().length == 3) {
			// Suggest town names
			List<String> townList = new ArrayList<String>();
			for(KonKingdom kingdom : getKonquest().getKingdomManager().getKingdoms()) {
				townList.addAll(kingdom.getTownNames());
			}
			tabList.addAll(townList);
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[2], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		} else if(getArgs().length == 4) {
			// suggest sub-commands
			tabList.add("open");
			tabList.add("close");
			tabList.add("add");
			tabList.add("kick");
			tabList.add("lord");
			tabList.add("knight");
			tabList.add("rename");
			tabList.add("upgrade");
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[3], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		} else if(getArgs().length == 5) {
			// suggest appropriate arguments
			String subCommand = getArgs()[3];
			if(subCommand.equalsIgnoreCase("add") || subCommand.equalsIgnoreCase("kick") || subCommand.equalsIgnoreCase("lord") || subCommand.equalsIgnoreCase("elite")) {
				List<String> playerList = new ArrayList<>();
				for(KonPlayer onlinePlayer : getKonquest().getPlayerManager().getPlayersOnline()) {
					playerList.add(onlinePlayer.getBukkitPlayer().getName());
				}
				tabList.addAll(playerList);
			} else if(subCommand.equalsIgnoreCase("upgrade")) {
				for(KonUpgrade upgrade : KonUpgrade.values()) {
					tabList.add(upgrade.toString().toLowerCase());
				}
			}
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[4], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		} else if(getArgs().length == 6) {
			// suggest upgrade levels
			String subCommand = getArgs()[3];
			if(subCommand.equalsIgnoreCase("upgrade")) {
				String upgradeName = getArgs()[4];
				KonUpgrade upgrade = KonUpgrade.getUpgrade(upgradeName);
				if(upgrade != null) {
					for(int i=0;i<=upgrade.getMaxLevel();i++) {
						tabList.add(String.valueOf(i));
					}
				}
			}
			StringUtil.copyPartialMatches(getArgs()[5], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		}
		return matchedTabList;
	}
}
