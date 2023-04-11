package com.github.rumsfield.konquest.command.admin;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.command.CommandBase;
import com.github.rumsfield.konquest.model.KonKingdom;
import com.github.rumsfield.konquest.model.KonOfflinePlayer;
import com.github.rumsfield.konquest.model.KonTown;
import com.github.rumsfield.konquest.model.KonUpgrade;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.CorePath;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TownAdminCommand extends CommandBase {

	/*
	 * The admin town command is different than the player town command.
	 * This command is mostly text-based, and does not use the management menu.
	 * It directly opens some menus that support an admin mode.
	 * Otherwise, it uses more text commands to give more control over town properties.
	 */
	
	public TownAdminCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }

	@Override
	public void execute() {
		// k admin town create|destroy|add|kick|rename|upgrade|shield|armor|plots|options|specialize <town> [<name>] [<arg>]
		if (getArgs().length != 4 && getArgs().length != 5 && getArgs().length != 6) {
			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
		} else {
        	Player bukkitPlayer = (Player) getSender();
        	World bukkitWorld = bukkitPlayer.getWorld();
        	
        	String subCmd = getArgs()[2];
        	String townName = getArgs()[3];

        	KonTown town = null;
        	
        	// Pre-checks based on sub-commands
        	if(subCmd.equalsIgnoreCase("create")) {
        		// Creating a town
        		// Check for valid world location
            	if(!getKonquest().isWorldValid(bukkitWorld)) {
            		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_WORLD.getMessage());
                    return;
            	}
            	// Check town name
            	if(getKonquest().validateName(townName,bukkitPlayer) != 0) {
            		// Messages handled within method
            		return;
            	}
        	} else if(subCmd.equalsIgnoreCase("destroy")) {
        		// Removing a town
        		if(getKonquest().getKingdomManager().isTown(townName)) {
        			town = getKonquest().getKingdomManager().getTown(townName);
        		} else {
        			// Cannot remove the capital with this command
        			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_BAD_NAME.getMessage(townName));
                    return;
        		}
        	} else {
        		// Command on existing town
        		// Verify town or capital exists
        		boolean isTown = getKonquest().getKingdomManager().isTown(townName);
        		boolean isCapital = getKonquest().getKingdomManager().isCapital(townName);
        		if(isTown) {
            		town = getKonquest().getKingdomManager().getTown(townName);
            	} else if(isCapital) {
            		town = getKonquest().getKingdomManager().getCapital(townName);
            	} else {
            		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(townName));
            		return;
            	}
        	}

        	// Action based on sub-command
        	switch(subCmd.toLowerCase()) {
        	case "options":
        		// Sanity check for town
            	if(town == null) {
            		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
            		return;
            	}
        		// Directly open options menu
        		getKonquest().getDisplayManager().displayTownOptionsMenu(bukkitPlayer, town);
        		break;
        		
        	case "plots":
        		// Sanity check for town
            	if(town == null) {
            		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
            		return;
            	}
        		// Directly open plots menu
	        	getKonquest().getDisplayManager().displayTownPlotMenu(bukkitPlayer, town);
        		break;

			case "specialize":
				// Sanity check for town
				if(town == null) {
					ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
					return;
				}
				// Directly open specialize menu
				getKonquest().getDisplayManager().displayTownSpecializationMenu(bukkitPlayer, town, true);
				break;

        	case "create":
        		if (getArgs().length == 5) {
					String kingdomName = getArgs()[4];
					// Verify kingdom name up-front
					if(!getKonquest().getKingdomManager().isKingdom(kingdomName)) {
						ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_BAD_NAME.getMessage(kingdomName));
	                    return;
					}
					// Attempt to create the town
					int exitCode = getKonquest().getKingdomManager().createTown(bukkitPlayer.getLocation(), townName, kingdomName);
		        	if(exitCode == 0) {
		        		KonTown createdTown = getKonquest().getKingdomManager().getTown(townName);
		        		if(createdTown != null) {
		        			bukkitPlayer.teleport(getKonquest().getKingdomManager().getKingdom(kingdomName).getTown(townName).getSpawnLoc());
			        		// Update labels
			        		getKonquest().getMapHandler().drawDynmapLabel(createdTown);
			        		getKonquest().getMapHandler().drawDynmapLabel(createdTown.getKingdom().getCapital());
			        		ChatUtil.sendNotice((Player) getSender(), MessagePath.COMMAND_SETTLE_NOTICE_SUCCESS.getMessage(townName));
		        		} else {
		        			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
		            		return;
		        		}
		        	} else {
		        		int distance;
		        		switch(exitCode) {
		        		case 1:
		        			ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_SETTLE_ERROR_FAIL_OVERLAP.getMessage());
		        			break;
		        		case 2:
		        			ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_SETTLE_ERROR_FAIL_PLACEMENT.getMessage());
		        			break;
		        		case 3:
		        			ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_SETTLE_ERROR_FAIL_NAME.getMessage());
		        			break;
		        		case 4:
		        			ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_SETTLE_ERROR_FAIL_TEMPLATE.getMessage());
		        			break;
		        		case 5:
		        			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_WORLD.getMessage());
		        			break;
		        		case 6:
		        			distance = getKonquest().getTerritoryManager().getDistanceToClosestTerritory(bukkitPlayer.getLocation());
		        			int min_distance_sanc = getKonquest().getCore().getInt(CorePath.TOWNS_MIN_DISTANCE_SANCTUARY.getPath());
		        			int min_distance_town = getKonquest().getCore().getInt(CorePath.TOWNS_MIN_DISTANCE_TOWN.getPath());
		        			int min_distance = Math.min(min_distance_sanc, min_distance_town);
							ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_SETTLE_ERROR_FAIL_PROXIMITY.getMessage(distance,min_distance));
		        			break;
		        		case 7:
		        			distance = getKonquest().getTerritoryManager().getDistanceToClosestTerritory(bukkitPlayer.getLocation());
		        			int max_distance_all = getKonquest().getCore().getInt(CorePath.TOWNS_MAX_DISTANCE_ALL.getPath());
		        			ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_SETTLE_ERROR_FAIL_MAX.getMessage(distance,max_distance_all));
		        			break;
		        		case 21:
		        			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
		        			break;
		        		case 22:
		        			ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_SETTLE_ERROR_FAIL_FLAT.getMessage());
		        			break;
		        		case 23:
		        			ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_SETTLE_ERROR_FAIL_HEIGHT.getMessage());
		        			break;
		        		case 12:
		        			ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_SETTLE_ERROR_FAIL_HEIGHT.getMessage());
		        			break;
		        		case 13:
		        			ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_SETTLE_ERROR_FAIL_INIT.getMessage());
		        			break;
		        		case 14:
		        			ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_SETTLE_ERROR_FAIL_AIR.getMessage());
		        			break;
		        		case 15:
		        			ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_SETTLE_ERROR_FAIL_WATER.getMessage());
		        			break;
		        		case 16:
		        			ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_SETTLE_ERROR_FAIL_CONTAINER.getMessage());
		        			break;
		        		default:
		        			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
		        			return;
		        		}
		        	}
        		} else {
			    	ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
	        		return;
			    }
        		break;
        		
        	case "destroy":
        		if (getArgs().length == 5) {
					String kingdomName = getArgs()[4];
					// Verify kingdom name up-front
					if(!getKonquest().getKingdomManager().isKingdom(kingdomName)) {
						ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_BAD_NAME.getMessage(kingdomName));
	                    return;
					}
					// Attempt to remove the town
					boolean status = getKonquest().getKingdomManager().removeTown(townName, kingdomName);
		        	if(status) {
		        		ChatUtil.sendNotice((Player) getSender(), MessagePath.GENERIC_NOTICE_SUCCESS.getMessage());
		        	} else {
		        		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(townName));
		                return;
		        	}
        		} else {
        			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
	        		return;
        		}
        		break;
        	
			case "add":
				if (getArgs().length == 5) {
					String playerName = getArgs()[4];
			    	KonOfflinePlayer offlinePlayer = getKonquest().getPlayerManager().getOfflinePlayerFromName(playerName);
			    	if(offlinePlayer == null) {
						ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(playerName));
		        		return;
					}
			    	if(!offlinePlayer.getKingdom().equals(town.getKingdom())) {
			    		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_ENEMY_PLAYER.getMessage());
		        		return;
			    	}
			    	// Add the player as a resident
			    	if(town.addPlayerResident(offlinePlayer.getOfflineBukkitPlayer(),false)) {
			    		ChatUtil.sendNotice((Player) getSender(), MessagePath.GENERIC_NOTICE_SUCCESS.getMessage());
			    	} else {
			    		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_FAILED.getMessage());
			    	}
			    } else {
			    	ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
	        		return;
			    }
			    break;
			    
			case "kick":
				if (getArgs().length == 5) {
					String playerName = getArgs()[4];
					KonOfflinePlayer offlinePlayer = getKonquest().getPlayerManager().getOfflinePlayerFromName(playerName);
					if(offlinePlayer == null) {
						ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(playerName));
		        		return;
					}
			    	// Remove the player as a resident
			    	if(town.removePlayerResident(offlinePlayer.getOfflineBukkitPlayer())) {
			    		ChatUtil.sendNotice((Player) getSender(), MessagePath.COMMAND_TOWN_NOTICE_KICK_RESIDENT.getMessage(playerName,townName));
			    	} else {
			    		ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_TOWN_ERROR_KICK_FAIL.getMessage(playerName,townName));
			    	}
			    } else {
			    	ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
	        		return;
			    }
				break;
				
			case "lord":
				if (getArgs().length == 5) {
					String playerName = getArgs()[4];
        			// Give lordship
        			KonOfflinePlayer offlinePlayer = getKonquest().getPlayerManager().getOfflinePlayerFromName(playerName);
        			if(offlinePlayer == null) {
						ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(playerName));
		        		return;
					}
        			if(!offlinePlayer.getKingdom().equals(town.getKingdom())) {
			    		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_ENEMY_PLAYER.getMessage());
		        		return;
			    	}
        			town.setPlayerLord(offlinePlayer.getOfflineBukkitPlayer());
        			ChatUtil.sendNotice((Player) getSender(), MessagePath.COMMAND_TOWN_NOTICE_LORD_SUCCESS.getMessage(townName,playerName));
			    } else {
			    	ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
	        		return;
			    }
				break;
				
			case "knight":
				if (getArgs().length == 5) {
					String playerName = getArgs()[4];
			    	KonOfflinePlayer offlinePlayer = getKonquest().getPlayerManager().getOfflinePlayerFromName(playerName);
			    	if(offlinePlayer == null) {
						ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(playerName));
		        		return;
					}
			    	if(!offlinePlayer.getKingdom().equals(town.getKingdom())) {
			    		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_ENEMY_PLAYER.getMessage());
		        		return;
			    	}
					if(town.isPlayerLord(offlinePlayer.getOfflineBukkitPlayer())) {
						ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
						return;
					}
			    	// Set resident's elite status
			    	if(town.isPlayerResident(offlinePlayer.getOfflineBukkitPlayer())) {
			    		if(town.isPlayerKnight(offlinePlayer.getOfflineBukkitPlayer())) {
			    			// Clear elite
			    			if(town.setPlayerKnight(offlinePlayer.getOfflineBukkitPlayer(), false)) {
								ChatUtil.sendNotice((Player) getSender(), MessagePath.COMMAND_TOWN_NOTICE_KNIGHT_CLEAR.getMessage(playerName,townName));
							} else {
								ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
							}
			    		} else {
			    			// Set elite
			    			if(town.setPlayerKnight(offlinePlayer.getOfflineBukkitPlayer(), true)) {
								ChatUtil.sendNotice((Player) getSender(), MessagePath.COMMAND_TOWN_NOTICE_KNIGHT_SET.getMessage(playerName,townName));
							} else {
								ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
							}
			    		}
			    	} else {
			    		ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_TOWN_ERROR_KNIGHT_RESIDENT.getMessage());
			    	}
			    } else {
			    	ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
	        		return;
			    }
			    break;
			    
			case "rename":
	        	if (getArgs().length == 5) {
					String newTownName = getArgs()[4];
	        		// Rename the town
	        		boolean success = town.getKingdom().renameTown(townName, newTownName);
	        		if(success) {
	        			ChatUtil.sendNotice((Player) getSender(), MessagePath.GENERIC_NOTICE_SUCCESS.getMessage());
	        		} else {
						ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_FAILED.getMessage());
					}
	        	} else {
	        		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
	        		return;
	        	}
	        	break;
	        	
			case "upgrade":
				if (getArgs().length == 6) {
					String upgradeName = getArgs()[4];
					String upgradeLevelStr = getArgs()[5];
	        		KonUpgrade upgrade = KonUpgrade.getUpgrade(upgradeName);
	        		if(upgrade == null) {
	        			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage());
		        		return;
	        		}
	        		if(!upgradeLevelStr.equalsIgnoreCase("")) {
	        			int upgradeLevel;
	        			try {
	        				upgradeLevel = Integer.parseInt(upgradeLevelStr);
	        			} 
	        			catch(NumberFormatException e) {
	        				ChatUtil.printDebug("Failed to parse string as int: "+e.getMessage());
	        				ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
			        		return;
	        			}
	        			if(!getKonquest().getUpgradeManager().isEnabled()) {
	        				ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_DISABLED.getMessage());
	        				return;
	        			}
	        			if(upgradeLevel < 0 || upgradeLevel > upgrade.getMaxLevel()) {
	        				ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
			        		return;
	        			}
	        			// Set town upgrade and level
	        			boolean status = getKonquest().getUpgradeManager().applyTownUpgrade(town, upgrade, upgradeLevel);
	        			if(status) {
	        				ChatUtil.sendNotice((Player) getSender(), MessagePath.MENU_UPGRADE_ADD.getMessage(upgrade.getDescription(),upgradeLevel,town.getName()));
	        			}
	        			
	        		} else {
	        			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
		        		return;
	        		}
	        	} else {
	        		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
	        		return;
	        	}
	        	break;
	        	
			case "shield":
				// /k admin forcetown shield clear|set|add [#]
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
						int shieldVal;
						if(!shieldValStr.equals("")) {
							try {
								shieldVal = Integer.parseInt(shieldValStr);
		        			} 
		        			catch(NumberFormatException e) {
		        				ChatUtil.printDebug("Failed to parse string as int: "+e.getMessage());
		        				ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INTERNAL_MESSAGE.getMessage(e.getMessage()));
				        		return;
		        			}
							// Parse remaining sub-command options
							if(shieldSubCmd.equalsIgnoreCase("set")) {
								if(getKonquest().getShieldManager().shieldSet(town, shieldVal)) {
									ChatUtil.sendNotice((Player) getSender(), MessagePath.GENERIC_NOTICE_SUCCESS.getMessage());
								} else {
									// Shields cannot be negative
									ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
								}
							} else if(shieldSubCmd.equalsIgnoreCase("add")) {
								if(getKonquest().getShieldManager().shieldAdd(town, shieldVal)) {
									ChatUtil.sendNotice((Player) getSender(), MessagePath.GENERIC_NOTICE_SUCCESS.getMessage());
								} else {
									// Shields cannot be negative
									ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
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
						int armorVal;
						if(!armorValStr.equals("")) {
							try {
								armorVal = Integer.parseInt(armorValStr);
		        			} 
		        			catch(NumberFormatException e) {
		        				ChatUtil.printDebug("Failed to parse string as int: "+e.getMessage());
		        				ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INTERNAL_MESSAGE.getMessage(e.getMessage()));
				        		return;
		        			}
							// Parse remaining sub-command options
							if(armorSubCmd.equalsIgnoreCase("set")) {
								if(getKonquest().getShieldManager().armorSet(town, armorVal)) {
									ChatUtil.sendNotice((Player) getSender(), MessagePath.GENERIC_NOTICE_SUCCESS.getMessage());
								} else {
									ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_FAILED.getMessage());
								}
							} else if(armorSubCmd.equalsIgnoreCase("add")) {
								if(getKonquest().getShieldManager().armorAdd(town, armorVal)) {
									ChatUtil.sendNotice((Player) getSender(), MessagePath.GENERIC_NOTICE_SUCCESS.getMessage());
								} else {
									ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_FAILED.getMessage());
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
			
        	default:
        		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
			}
        }
	}

	@Override
	public List<String> tabComplete() {
		// k admin town create|remove|add|kick|lord|knight|rename|upgrade|shield|armor|plots|options <town> [<name>] [<arg>]
		List<String> tabList = new ArrayList<>();
		final List<String> matchedTabList = new ArrayList<>();
		if(getArgs().length == 3) {
			// suggest sub-commands
			tabList.add("create");
			tabList.add("destroy");
			tabList.add("add");
			tabList.add("kick");
			tabList.add("lord");
			tabList.add("knight");
			tabList.add("rename");
			tabList.add("upgrade");
			tabList.add("shield");
			tabList.add("armor");
			tabList.add("plots");
			tabList.add("options");
			tabList.add("specialize");
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[2], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		} else if(getArgs().length == 4) {
			// Town name
			String subCommand = getArgs()[2];
			if(subCommand.equalsIgnoreCase("create")) {
				// Suggest new name
				tabList.add("***");
			} else if(subCommand.equalsIgnoreCase("destroy")) {
				// Suggest existing town names only
				tabList.addAll(getKonquest().getKingdomManager().getTownNames());
			} else {
				// Suggest existing town + capital names
				tabList.addAll(getKonquest().getKingdomManager().getTownNames());
				tabList.addAll(getKonquest().getKingdomManager().getKingdomNames());
			}
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[3], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		} else if(getArgs().length == 5) {
			// suggest appropriate arguments
			String subCommand = getArgs()[2];
			String townName = getArgs()[3];
			String name;
			if(subCommand.equalsIgnoreCase("add") || subCommand.equalsIgnoreCase("kick") || subCommand.equalsIgnoreCase("lord") || subCommand.equalsIgnoreCase("knight")) {
				// Player name that belongs to kingdom of town/capital
				KonKingdom townKingdom = null;
				if(getKonquest().getKingdomManager().isCapital(townName)) {
					townKingdom = getKonquest().getKingdomManager().getCapital(townName).getKingdom();
				} else if(getKonquest().getKingdomManager().isTown(townName)) {
					townKingdom = getKonquest().getKingdomManager().getTown(townName).getKingdom();
				}
				if(townKingdom != null) {
					for(KonOfflinePlayer offlinePlayer : getKonquest().getPlayerManager().getAllPlayersInKingdom(townKingdom)) {
						name = offlinePlayer.getOfflineBukkitPlayer().getName();
						if(name != null) {
							tabList.add(name);
						}
					}
				}
			} else if(subCommand.equalsIgnoreCase("create") || subCommand.equalsIgnoreCase("destroy")) {
				// Kingdom name
				tabList.addAll(getKonquest().getKingdomManager().getKingdomNames());
			} else if(subCommand.equalsIgnoreCase("rename")) {
				// New name
				tabList.add("***");
			} else if(subCommand.equalsIgnoreCase("upgrade")) {
				for(KonUpgrade upgrade : KonUpgrade.values()) {
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
			String subCommand = getArgs()[2];
			if(subCommand.equalsIgnoreCase("upgrade")) {
				String upgradeName = getArgs()[4];
				KonUpgrade upgrade = KonUpgrade.getUpgrade(upgradeName);
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
