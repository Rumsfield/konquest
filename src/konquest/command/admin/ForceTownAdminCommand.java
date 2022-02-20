package konquest.command.admin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import konquest.Konquest;
import konquest.api.model.KonquestUpgrade;
import konquest.command.CommandBase;
import konquest.model.KonKingdom;
import konquest.model.KonOfflinePlayer;
import konquest.model.KonPlayer;
import konquest.model.KonTown;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;

public class ForceTownAdminCommand extends CommandBase {
	
	public ForceTownAdminCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }

	@Override
	public void execute() {
		// k admin forcetown <name> options|add|kick|knight|lord|rename|upgrade|shield|plots [arg1] [arg2]
		if (getArgs().length != 4 && getArgs().length != 5 && getArgs().length != 6) {
			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
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
    			//ChatUtil.sendError((Player) getSender(), "No such Town");
    			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(townName));
    			return;
    		}

        	// Action based on sub-command
    		String playerName = "";
        	String newTownName = "";
        	String upgradeName = "";
        	//String optionSetting = "";
        	switch(subCmd.toLowerCase()) {
        	case "options":
        		getKonquest().getDisplayManager().displayTownOptionsMenu((Player) getSender(), town);
        		break;
        	/*
        	case "open":
            	// get "true" or "false"
        		if (getArgs().length == 5) {
        			optionSetting = getArgs()[4];
				}
    			if(optionSetting.equalsIgnoreCase("true")) {
    				// Attempt to open the town
    				if(town.isOpen()) {
    					// Already enabled
                		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_FAILED.getMessage());
                	} else {
                		town.setIsOpen(true);
                		ChatUtil.sendNotice((Player) getSender(), MessagePath.GENERIC_NOTICE_SUCCESS.getMessage());
                	}
    			} else if(optionSetting.equalsIgnoreCase("false")) {
    				// Attempt to close the town
    				if(town.isOpen()) {
                		town.setIsOpen(false);
                		ChatUtil.sendNotice((Player) getSender(), MessagePath.GENERIC_NOTICE_SUCCESS.getMessage());
                	} else {
                		// Already disabled
                		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_FAILED.getMessage());
                	}
    			} else {
    				ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
	        		return;
    			}
        		break;
        	case "redstone":
        		// get "true" or "false"
        		if (getArgs().length == 5) {
        			optionSetting = getArgs()[4];
				}
    			if(optionSetting.equalsIgnoreCase("true")) {
    				// Attempt to enable enemy redstone use
    				if(town.isEnemyRedstoneAllowed()) {
    					// Already enabled
                		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_FAILED.getMessage());
                	} else {
                		town.setIsEnemyRedstoneAllowed(true);
                		ChatUtil.sendNotice((Player) getSender(), MessagePath.GENERIC_NOTICE_SUCCESS.getMessage());
                	}
    			} else if(optionSetting.equalsIgnoreCase("false")) {
    				// Attempt to disable enemy redstone use
    				if(town.isEnemyRedstoneAllowed()) {
    					town.setIsEnemyRedstoneAllowed(false);
                		ChatUtil.sendNotice((Player) getSender(), MessagePath.GENERIC_NOTICE_SUCCESS.getMessage());
                	} else {
                		// Already disabled
                		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_FAILED.getMessage());
                	}
    			} else {
    				ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
	        		return;
    			}
        		break;
        	*/
			case "add":
				if (getArgs().length == 5) {
					playerName = getArgs()[4];
				}
			    if(!playerName.equalsIgnoreCase("")) {
			    	KonOfflinePlayer offlinePlayer = getKonquest().getPlayerManager().getOfflinePlayerFromName(playerName);
			    	if(offlinePlayer == null) {
						//ChatUtil.sendError((Player) getSender(), "Invalid player name!");
						ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(playerName));
		        		return;
					}
			    	if(!offlinePlayer.getKingdom().equals(town.getKingdom())) {
			    		//ChatUtil.sendError((Player) getSender(), "That player is an enemy!");
			    		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_ENEMY_PLAYER.getMessage());
		        		return;
			    	}
			    	// Add the player as a resident
			    	if(town.addPlayerResident(offlinePlayer.getOfflineBukkitPlayer(),false)) {
			    		//ChatUtil.sendNotice((Player) getSender(), "Added "+playerName+" as a resident of "+townName);
			    		ChatUtil.sendNotice((Player) getSender(), MessagePath.COMMAND_TOWN_NOTICE_ADD_SUCCESS.getMessage(playerName,townName));
			    	} else {
			    		//ChatUtil.sendError((Player) getSender(), playerName+" is already a resident of "+townName);
			    		ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_TOWN_ERROR_ADD_RESIDENT.getMessage(playerName,townName));
			    	}
			    } else {
			    	//ChatUtil.sendError((Player) getSender(), "Must provide a player name!");
			    	ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
	        		return;
			    }
			    break;
			case "kick":
				if (getArgs().length == 5) {
					playerName = getArgs()[4];
				}
				if(!playerName.equalsIgnoreCase("")) {
					KonOfflinePlayer offlinePlayer = getKonquest().getPlayerManager().getOfflinePlayerFromName(playerName);
					if(offlinePlayer == null) {
						//ChatUtil.sendError((Player) getSender(), "Invalid player name!");
						ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(playerName));
		        		return;
					}
			    	// Remove the player as a resident
			    	if(town.removePlayerResident(offlinePlayer.getOfflineBukkitPlayer())) {
			    		//ChatUtil.sendNotice((Player) getSender(), "Removed resident "+playerName+" from "+townName);
			    		ChatUtil.sendNotice((Player) getSender(), MessagePath.COMMAND_TOWN_NOTICE_KICK_RESIDENT.getMessage(playerName,townName));
			    	} else {
			    		//ChatUtil.sendError((Player) getSender(), playerName+" is not a resident of "+townName);
			    		ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_TOWN_ERROR_KICK_FAIL.getMessage(playerName,townName));
			    	}
			    } else {
			    	//ChatUtil.sendError((Player) getSender(), "Must provide a player name!");
			    	ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
	        		return;
			    }
				break;
			case "lord":
				if (getArgs().length == 5) {
					playerName = getArgs()[4];
				}
				if(!playerName.equalsIgnoreCase("")) {
        			// Give lordship
        			KonOfflinePlayer offlinePlayer = getKonquest().getPlayerManager().getOfflinePlayerFromName(playerName);
        			if(offlinePlayer == null) {
						//ChatUtil.sendError((Player) getSender(), "Invalid player name!");
						ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(playerName));
		        		return;
					}
        			if(!offlinePlayer.getKingdom().equals(town.getKingdom())) {
			    		//ChatUtil.sendError((Player) getSender(), "That player is an enemy!");
			    		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_ENEMY_PLAYER.getMessage());
		        		return;
			    	}
        			town.setPlayerLord(offlinePlayer.getOfflineBukkitPlayer());
        			//ChatUtil.sendNotice((Player) getSender(), "Gave Lordship of "+townName+" to "+playerName);
        			ChatUtil.sendNotice((Player) getSender(), MessagePath.COMMAND_TOWN_NOTICE_LORD_SUCCESS.getMessage(townName,playerName));
			    } else {
			    	//ChatUtil.sendError((Player) getSender(), "Must provide a player name!");
			    	ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(playerName));
	        		return;
			    }
				break;
			case "knight":
				if (getArgs().length == 5) {
					playerName = getArgs()[4];
				}
			    if(!playerName.equalsIgnoreCase("")) {
			    	KonOfflinePlayer offlinePlayer = getKonquest().getPlayerManager().getOfflinePlayerFromName(playerName);
			    	if(offlinePlayer == null) {
						//ChatUtil.sendError((Player) getSender(), "Invalid player name!");
						ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(playerName));
		        		return;
					}
			    	if(!offlinePlayer.getKingdom().equals(town.getKingdom())) {
			    		//ChatUtil.sendError((Player) getSender(), "That player is an enemy!");
			    		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_ENEMY_PLAYER.getMessage());
		        		return;
			    	}
			    	// Set resident's elite status
			    	if(town.isPlayerResident(offlinePlayer.getOfflineBukkitPlayer())) {
			    		if(town.isPlayerKnight(offlinePlayer.getOfflineBukkitPlayer())) {
			    			// Clear elite
			    			town.setPlayerKnight(offlinePlayer.getOfflineBukkitPlayer(), false);
			    			//ChatUtil.sendNotice((Player) getSender(), playerName+" is no longer a Knight in "+townName+". Use this command again to set Knight status.");
			    			ChatUtil.sendNotice((Player) getSender(), MessagePath.COMMAND_TOWN_NOTICE_KNIGHT_CLEAR.getMessage(playerName,townName));
			    		} else {
			    			// Set elite
			    			town.setPlayerKnight(offlinePlayer.getOfflineBukkitPlayer(), true);
			    			//ChatUtil.sendNotice((Player) getSender(), playerName+" is now a Knight resident in "+townName+". Use this command again to remove Knight status.");
			    			ChatUtil.sendNotice((Player) getSender(), MessagePath.COMMAND_TOWN_NOTICE_KNIGHT_SET.getMessage(playerName,townName));
			    		}
			    	} else {
			    		//ChatUtil.sendError((Player) getSender(), "Knights must be added as residents first!");
			    		ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_TOWN_ERROR_KNIGHT_RESIDENT.getMessage());
			    	}
			    } else {
			    	//ChatUtil.sendError((Player) getSender(), "Must provide a player name!");
			    	ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
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
	        			//ChatUtil.sendNotice((Player) getSender(), "Changed town name "+townName+" to "+newTownName);
	        			ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_ADMIN_FORCETOWN_NOTICE_RENAME.getMessage(townName,newTownName));
	        		}
	        	} else {
	        		//ChatUtil.sendError((Player) getSender(), "Must provide a new Town name!");
	        		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
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
	        		KonquestUpgrade upgrade = KonquestUpgrade.getUpgrade(upgradeName);
	        		if(upgrade == null) {
	        			//ChatUtil.sendError((Player) getSender(), "Invalid upgrade name!");
	        			ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_ADMIN_FORCETOWN_ERROR_NO_UPGRADE.getMessage());
		        		return;
	        		}
	        		if(!upgradeLevelStr.equalsIgnoreCase("")) {
	        			int upgradeLevel = 0;
	        			try {
	        				upgradeLevel = Integer.parseInt(upgradeLevelStr);
	        			} 
	        			catch(NumberFormatException e) {
	        				ChatUtil.printDebug("Failed to parse string as int: "+e.getMessage());
	        				//ChatUtil.sendError((Player) getSender(), "Invalid upgrade level!");
	        				ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_ADMIN_FORCETOWN_ERROR_NO_LEVEL.getMessage());
			        		return;
	        			}
	        			if(!getKonquest().getUpgradeManager().isEnabled()) {
	        				ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_DISABLED.getMessage());
	        				return;
	        			}
	        			if(upgradeLevel < 0 || upgradeLevel > upgrade.getMaxLevel()) {
	        				ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_ADMIN_FORCETOWN_ERROR_NO_LEVEL.getMessage());
			        		return;
	        			}
	        			// Set town upgrade and level
	        			boolean status = getKonquest().getUpgradeManager().applyTownUpgrade(town, upgrade, upgradeLevel);
	        			if(status) {
	        				ChatUtil.sendNotice((Player) getSender(), MessagePath.MENU_UPGRADE_ADD.getMessage(upgrade.getDescription(),upgradeLevel,town.getName()));
	        			}
	        			
	        		} else {
	        			//ChatUtil.sendError((Player) getSender(), "Must provide an upgrade level!");
	        			ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_ADMIN_FORCETOWN_ERROR_NO_LEVEL.getMessage());
		        		return;
	        		}
	        	} else {
	        		//ChatUtil.sendError((Player) getSender(), "Must provide an upgrade name!");
	        		ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_ADMIN_FORCETOWN_ERROR_NO_UPGRADE.getMessage());
	        		return;
	        	}
	        	break;
			case "shield":
				// /k admin forcetown shield clear|set|add|sub [#]
				String shieldSubCmd = "";
				String shieldValStr = "";
				if (getArgs().length >= 5) {
					shieldSubCmd = getArgs()[4];
				}
				if (getArgs().length == 6) {
					shieldValStr = getArgs()[5];
					
					
				}
				if(!shieldSubCmd.equals("")) {
					// Parse the sub-command, clear|set|add
					if(shieldSubCmd.equalsIgnoreCase("clear")) {
						town.deactivateShield();
						ChatUtil.sendNotice((Player) getSender(), MessagePath.GENERIC_NOTICE_SUCCESS.getMessage());
					} else {
						int shieldVal = 0;
						if(!shieldValStr.equals("")) {
							try {
								shieldVal = Integer.parseInt(shieldValStr);
		        			} 
		        			catch(NumberFormatException e) {
		        				ChatUtil.printDebug("Failed to parse string as int: "+e.getMessage());
		        				//ChatUtil.sendError((Player) getSender(), "Invalid upgrade level!");
		        				ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INTERNAL_MESSAGE.getMessage(e.getMessage()));
				        		return;
		        			}
							// Parse remaining sub-command options
							if(shieldSubCmd.equalsIgnoreCase("set")) {
								if(getKonquest().getShieldManager().shieldSet(town, shieldVal)) {
									ChatUtil.sendNotice((Player) getSender(), MessagePath.GENERIC_NOTICE_SUCCESS.getMessage());
								} else {
									// Shields cannot be negative
									ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_ADMIN_FORCETOWN_ERROR_BAD_SHIELD.getMessage());
								}
							} else if(shieldSubCmd.equalsIgnoreCase("add")) {
								if(getKonquest().getShieldManager().shieldAdd(town, shieldVal)) {
									ChatUtil.sendNotice((Player) getSender(), MessagePath.GENERIC_NOTICE_SUCCESS.getMessage());
								} else {
									// Shields cannot be negative
									ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_ADMIN_FORCETOWN_ERROR_BAD_SHIELD.getMessage());
								}
							} else {
								ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
				        		return;
							}
						} else {
							ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
			        		return;
						}
					}
				} else {
					ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
	        		return;
				}
				break;
			case "armor":
				// /k admin forcetown armor clear|set|add [#]
				String armorSubCmd = "";
				String armorValStr = "";
				if (getArgs().length >= 5) {
					armorSubCmd = getArgs()[4];
				}
				if (getArgs().length == 6) {
					armorValStr = getArgs()[5];
					
					
				}
				if(!armorSubCmd.equals("")) {
					// Parse the sub-command, clear|set|add
					if(armorSubCmd.equalsIgnoreCase("clear")) {
						town.deactivateArmor();
						ChatUtil.sendNotice((Player) getSender(), MessagePath.GENERIC_NOTICE_SUCCESS.getMessage());
					} else {
						int armorVal = 0;
						if(!armorValStr.equals("")) {
							try {
								armorVal = Integer.parseInt(armorValStr);
		        			} 
		        			catch(NumberFormatException e) {
		        				ChatUtil.printDebug("Failed to parse string as int: "+e.getMessage());
		        				//ChatUtil.sendError((Player) getSender(), "Invalid upgrade level!");
		        				ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INTERNAL_MESSAGE.getMessage(e.getMessage()));
				        		return;
		        			}
							// Parse remaining sub-command options
							if(armorSubCmd.equalsIgnoreCase("set")) {
								if(getKonquest().getShieldManager().armorSet(town, armorVal)) {
									ChatUtil.sendNotice((Player) getSender(), MessagePath.GENERIC_NOTICE_SUCCESS.getMessage());
								} else {
									ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_ADMIN_FORCETOWN_ERROR_BAD_ARMOR.getMessage());
								}
							} else if(armorSubCmd.equalsIgnoreCase("add")) {
								if(getKonquest().getShieldManager().armorAdd(town, armorVal)) {
									ChatUtil.sendNotice((Player) getSender(), MessagePath.GENERIC_NOTICE_SUCCESS.getMessage());
								} else {
									ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_ADMIN_FORCETOWN_ERROR_BAD_ARMOR.getMessage());
								}
							} else {
								ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
				        		return;
							}
						} else {
							ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
			        		return;
						}
					}
				} else {
					ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
	        		return;
				}
				break;
			case "plots":
	        	// Verify plots are enabled
            	boolean isPlotsEnabled = getKonquest().getPlotManager().isEnabled();
            	if(!isPlotsEnabled) {
            		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_DISABLED.getMessage());
            		return;
            	}
	        	getKonquest().getDisplayManager().displayPlotMenu((Player) getSender(), town);
        		break;
        	default:
        		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
        		return;
        	}
        }
	}

	@Override
	public List<String> tabComplete() {
		// k admin forcetown <name> options|add|kick|knight|lord|rename|upgrade|shield|plots [arg1] [arg2]
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
			tabList.add("options");
			tabList.add("add");
			tabList.add("kick");
			tabList.add("lord");
			tabList.add("knight");
			tabList.add("rename");
			tabList.add("upgrade");
			tabList.add("shield");
			tabList.add("armor");
			tabList.add("plots");
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[3], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		} else if(getArgs().length == 5) {
			// suggest appropriate arguments
			String subCommand = getArgs()[3];
			if(subCommand.equalsIgnoreCase("add") || subCommand.equalsIgnoreCase("kick") || subCommand.equalsIgnoreCase("lord") || subCommand.equalsIgnoreCase("knight")) {
				List<String> playerList = new ArrayList<>();
				for(KonPlayer onlinePlayer : getKonquest().getPlayerManager().getPlayersOnline()) {
					playerList.add(onlinePlayer.getBukkitPlayer().getName());
				}
				tabList.addAll(playerList);
			} else if(subCommand.equalsIgnoreCase("rename")) {
				tabList.add("***");
			} else if(subCommand.equalsIgnoreCase("upgrade")) {
				for(KonquestUpgrade upgrade : KonquestUpgrade.values()) {
					tabList.add(upgrade.toString().toLowerCase());
				}
			} else if(subCommand.equalsIgnoreCase("shield") || subCommand.equalsIgnoreCase("armor")) {
				tabList.add("clear");
				tabList.add("set");
				tabList.add("add");
			}
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[4], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		} else if(getArgs().length == 6) {
			// suggest appropriate arguments
			String subCommand = getArgs()[3];
			if(subCommand.equalsIgnoreCase("upgrade")) {
				String upgradeName = getArgs()[4];
				KonquestUpgrade upgrade = KonquestUpgrade.getUpgrade(upgradeName);
				if(upgrade != null) {
					for(int i=0;i<=upgrade.getMaxLevel();i++) {
						tabList.add(String.valueOf(i));
					}
				}
			} else if(subCommand.equalsIgnoreCase("shield") || subCommand.equalsIgnoreCase("armor")) {
				String modifier = getArgs()[4];
				if(modifier.equalsIgnoreCase("set") || modifier.equalsIgnoreCase("add")) {
					tabList.add("#");
				}
			}
			StringUtil.copyPartialMatches(getArgs()[5], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		}
		return matchedTabList;
	}
}
