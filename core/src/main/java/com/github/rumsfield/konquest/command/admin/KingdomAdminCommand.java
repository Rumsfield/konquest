package com.github.rumsfield.konquest.command.admin;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.api.model.KonquestDiplomacyType;
import com.github.rumsfield.konquest.command.CommandBase;
import com.github.rumsfield.konquest.command.CommandType;
import com.github.rumsfield.konquest.model.KonKingdom;
import com.github.rumsfield.konquest.model.KonOfflinePlayer;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.model.KonTown;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.ColorRGB;
import com.github.rumsfield.konquest.utility.CorePath;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.*;

public class KingdomAdminCommand extends CommandBase {

	public KingdomAdminCommand() {
		// Define name and sender support
		super("kingdom",false, true);
		// Define arguments
		// menu <kingdom>
		addArgument(
				newArg("menu",true,false)
						.sub( newArg("kingdom",false,false) )
		);
		// create <template> <name>
		addArgument(
				newArg("create",true,false)
						.sub( newArg("template",false,false)
								.sub( newArg("name",false,false) ) )
		);
		// remove <kingdom>
		addArgument(
				newArg("remove",true,false)
						.sub( newArg("kingdom",false,false) )
		);
		// exile <player>
		addArgument(
				newArg("exile",true,false)
						.sub( newArg("player",false,false) )
		);
		// rename <kingdom> <name>
		addArgument(
				newArg("rename",true,false)
						.sub( newArg("kingdom",false,false)
								.sub( newArg("name",false,false) ) )
		);
		// capital <kingdom> <town>
		addArgument(
				newArg("capital",true,false)
						.sub( newArg("kingdom",false,false)
								.sub( newArg("town",false,false) ) )
		);
		// access <kingdom> open|closed
		List<String> accessArgNames = Arrays.asList("open", "closed");
		addArgument(
				newArg("access",true,false)
						.sub( newArg("kingdom",false,false)
								.sub( newArg(accessArgNames,true,false) ) )
		);
		// type <kingdom> admin|player
		List<String> typeArgNames = Arrays.asList("admin", "player");
		addArgument(
				newArg("type",true,false)
						.sub( newArg("kingdom",false,false)
								.sub( newArg(typeArgNames,true,false) ) )
		);
		// template <kingdom> [<monument>]
		addArgument(
				newArg("template",true,false)
						.sub( newArg("kingdom",false,true)
								.sub( newArg("monument",false,false) ) )
		);
		// webcolor <kingdom> [<color>]
		addArgument(
				newArg("webcolor",true,false)
						.sub( newArg("kingdom",false,true)
								.sub( newArg("color",false,false) ) )
		);
		// diplomacy <kingdom> <kingdom> [<relation>]
		addArgument(
				newArg("diplomacy",true,false)
						.sub( newArg("kingdom",false,false)
								.sub( newArg("kingdom",false,true)
										.sub( newArg("relation",false,false) ) ) )
		);
		// member <kingdom> add|kick|promote|demote|master <player>
		List<String> memberArgNames = Arrays.asList("add", "kick", "promote", "demote", "master");
		addArgument(
				newArg("member",true,false)
						.sub( newArg("kingdom",false,false)
								.sub( newArg(memberArgNames,true,false)
										.sub( newArg("player",false,false) ) ) )
		);
    }

	@Override
	public void execute(Konquest konquest, CommandSender sender, List<String> args) {
		// Sender can be player or console
		// Notify sender of missing templates
		if(konquest.getSanctuaryManager().getNumTemplates() == 0) {
			ChatUtil.sendError(sender,MessagePath.COMMAND_ADMIN_KINGDOM_ERROR_NO_TEMPLATES.getMessage());
		}
		// Check if sender is player
		KonPlayer player = konquest.getPlayerManager().getPlayer(sender);
		boolean isSenderPlayer = (player != null);
		// Get sub-command and target kingdom
		if (args.size() < 2) {
			sendInvalidArgMessage(sender);
			return;
		}
		String subCmd = args.get(0);
		String kingdomName = args.get(1);
		KonKingdom kingdom;
		// Execute sub-commands
		switch(subCmd.toLowerCase()) {
			case "menu":
				// Open the kingdom menu
				// Sender must be a player
				if(!isSenderPlayer) {
					ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_NO_PLAYER.getMessage());
					return;
				}
				// Check for valid kingdom
				if(!konquest.getKingdomManager().isKingdom(kingdomName)) {
					ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(kingdomName));
					return;
				}
				kingdom = konquest.getKingdomManager().getKingdom(kingdomName);
				assert kingdom != null;
				konquest.getDisplayManager().displayKingdomMenu(player, kingdom, true);
				break;
			case "create":
				// Create a new kingdom, admin kingdom
				// Sender must be a player
				if(!isSenderPlayer) {
					ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_NO_PLAYER.getMessage());
					return;
				}
				// Needs kingdom name and template name arguments
				if(args.size() != 3) {
					sendInvalidArgMessage(sender);
					return;
				}
				String templateName = args.get(1);
				String newKingdomName = args.get(2);
				// Validate kingdom name
				if(konquest.validateName(newKingdomName,sender) != 0) {
					// Sender receives error message within validateName method
					return;
				}
				// Create the kingdom
				Location settleLoc = player.getBukkitPlayer().getLocation();
				if(!konquest.isWorldValid(settleLoc.getWorld())) {
					ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_INVALID_WORLD.getMessage());
					return;
				}
				int status = konquest.getKingdomManager().createKingdom(settleLoc, newKingdomName, templateName, player, true);
				// Resolve status
				if(status == 0) {
					// Successful kingdom creation
					KonKingdom createdKingdom = konquest.getKingdomManager().getKingdom(newKingdomName);
					ChatUtil.sendBroadcast(MessagePath.COMMAND_ADMIN_KINGDOM_BROADCAST_CREATE.getMessage(newKingdomName));
					// Teleport player to safe place around monument, facing monument
					konquest.getKingdomManager().teleportAwayFromCenter(createdKingdom.getCapital());
					// Open kingdom menu for newly created kingdom
					KonKingdom newKingdom = konquest.getKingdomManager().getKingdom(newKingdomName);
					konquest.getDisplayManager().displayKingdomMenu(player, newKingdom, true);
				} else {
					switch(status) {
						case 1:
							// Note that this error should not be reached due to previous checks
							ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_TAKEN_NAME.getMessage());
							break;
						case 4:
							ChatUtil.sendError(sender, MessagePath.COMMAND_KINGDOM_ERROR_INVALID_TEMPLATE.getMessage(templateName));
							break;
						case 5:
							ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_INVALID_WORLD.getMessage());
							break;
						case 6:
							int distance = konquest.getTerritoryManager().getDistanceToClosestTerritory(settleLoc);
							int min_distance_sanc = konquest.getCore().getInt(CorePath.TOWNS_MIN_DISTANCE_SANCTUARY.getPath());
							int min_distance_town = konquest.getCore().getInt(CorePath.TOWNS_MIN_DISTANCE_TOWN.getPath());
							int min_distance = Math.min(min_distance_sanc, min_distance_town);
							ChatUtil.sendError(sender, MessagePath.COMMAND_SETTLE_ERROR_FAIL_PROXIMITY.getMessage(distance,min_distance));
							break;
						case 7:
							distance = konquest.getTerritoryManager().getDistanceToClosestTerritory(settleLoc);
							int max_distance_all = konquest.getCore().getInt(CorePath.TOWNS_MAX_DISTANCE_ALL.getPath());
							ChatUtil.sendError(sender, MessagePath.COMMAND_SETTLE_ERROR_FAIL_MAX.getMessage(distance,max_distance_all));
							break;
						case 8:
							ChatUtil.sendError(sender, MessagePath.COMMAND_SETTLE_ERROR_FAIL_PLACEMENT.getMessage());
							break;
						case 12:
							ChatUtil.sendError(sender, MessagePath.COMMAND_SETTLE_ERROR_FAIL_HEIGHT.getMessage());
							break;
						case 13:
							ChatUtil.sendError(sender, MessagePath.COMMAND_SETTLE_ERROR_FAIL_INIT.getMessage());
							break;
						case 14:
							ChatUtil.sendError(sender, MessagePath.COMMAND_SETTLE_ERROR_FAIL_AIR.getMessage());
							break;
						case 15:
							ChatUtil.sendError(sender, MessagePath.COMMAND_SETTLE_ERROR_FAIL_WATER.getMessage());
							break;
						case 16:
							ChatUtil.sendError(sender, MessagePath.COMMAND_SETTLE_ERROR_FAIL_CONTAINER.getMessage());
							break;
						case 22:
							ChatUtil.sendError(sender, MessagePath.COMMAND_SETTLE_ERROR_FAIL_FLAT.getMessage());
							break;
						case 23:
							ChatUtil.sendError(sender, MessagePath.COMMAND_SETTLE_ERROR_FAIL_HEIGHT.getMessage());
							break;
						default:
							ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
							break;
					}
				}
				break;
			case "remove":
				// Remove a kingdom
				boolean pass = konquest.getKingdomManager().removeKingdom(kingdomName);
				if(!pass) {
					ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(kingdomName));
					return;
				} else {
					ChatUtil.sendBroadcast(MessagePath.COMMAND_KINGDOM_BROADCAST_DISBAND.getMessage(kingdomName));
				}
				break;
			case "rename":
				// Rename the kingdom
				if(args.size() != 3) {
					sendInvalidArgMessage(sender);
					return;
				}
				String newName = args.get(2);
				// Check for valid kingdom
				if(!konquest.getKingdomManager().isKingdom(kingdomName)) {
					ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(kingdomName));
					return;
				}
				kingdom = konquest.getKingdomManager().getKingdom(kingdomName);
				assert kingdom != null;
				String oldName = kingdom.getName();
				if(konquest.validateName(newName,sender) != 0) {
					// Sender receives error message within validateName method
					return;
				}
				int renameStatus = konquest.getKingdomManager().renameKingdom(oldName, newName, player, true);
				switch(renameStatus) {
					case 0:
						ChatUtil.sendNotice(sender, MessagePath.COMMAND_KINGDOM_NOTICE_RENAME.getMessage(newName));
						ChatUtil.sendBroadcast(MessagePath.COMMAND_KINGDOM_BROADCAST_RENAME.getMessage(oldName,newName));
						break;
					case 1:
						ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage());
						break;
					case 2:
						ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_TAKEN_NAME.getMessage());
						break;
					default:
						ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_FAILED.getMessage());
						break;
				}
				break;
			case "capital":
				// Check for enabled feature
				if(!konquest.getKingdomManager().getIsCapitalSwapEnable()) {
					ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_DISABLED.getMessage());
					return;
				}
				if(args.size() != 3) {
					sendInvalidArgMessage(sender);
					return;
				}
				// Check for valid kingdom
				if(!konquest.getKingdomManager().isKingdom(kingdomName)) {
					ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(kingdomName));
					return;
				}
				kingdom = konquest.getKingdomManager().getKingdom(kingdomName);
				assert kingdom != null;
				String townName = args.get(2);
				// Check valid town name
				if (!kingdom.hasTown(townName)) {
					ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_BAD_NAME.getMessage(townName));
					return;
				}
				// Do capital swap without warmup
				// Includes broadcast
				boolean swapStatus = kingdom.swapCapitalToTown(kingdom.getTown(townName));
				if (swapStatus) {
					ChatUtil.sendNotice(sender, MessagePath.GENERIC_NOTICE_SUCCESS.getMessage());
				} else {
					ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_FAILED.getMessage());
				}
				break;
			case "exile":
				// Force a player to become a barbarian
				if(args.size() != 2) {
					sendInvalidArgMessage(sender);
					return;
				}
				String exilePlayerName = args.get(1);
				KonOfflinePlayer exilePlayer = konquest.getPlayerManager().getOfflinePlayerFromName(exilePlayerName);
				if(exilePlayer == null) {
					ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(exilePlayerName));
					return;
				}
				exilePlayerName = exilePlayer.getOfflineBukkitPlayer().getName();
				UUID exileUUID = exilePlayer.getOfflineBukkitPlayer().getUniqueId();
				int playerExileStatus = konquest.getKingdomManager().exilePlayerBarbarian(exileUUID,true,true,true,true);
				switch(playerExileStatus) {
					case 0:
						// Success
						ChatUtil.sendNotice(sender, MessagePath.COMMAND_KINGDOM_NOTICE_KICK.getMessage(exilePlayerName));
						break;
					case 1:
						// Player is already a barbarian
						ChatUtil.sendError(sender, MessagePath.COMMAND_KINGDOM_ERROR_KICK_BARBARIAN.getMessage());
						break;
					default:
						ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_FAILED.getMessage());
						break;
				}
				break;
			case "access":
				// Make the kingdom open or closed
				if(args.size() != 3) {
					sendInvalidArgMessage(sender);
					return;
				}
				String accessType = args.get(2);
				// Get kingdom from name
				if(!konquest.getKingdomManager().isKingdom(kingdomName)) {
					ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(kingdomName));
					return;
				}
				kingdom = konquest.getKingdomManager().getKingdom(kingdomName);
				assert kingdom != null;
				// Set access
				if (accessType.equalsIgnoreCase("open")) {
					kingdom.setIsOpen(true);
					ChatUtil.sendNotice(sender, MessagePath.COMMAND_KINGDOM_NOTICE_OPEN.getMessage());
				} else if (accessType.equalsIgnoreCase("closed")) {
					kingdom.setIsOpen(false);
					ChatUtil.sendNotice(sender, MessagePath.COMMAND_KINGDOM_NOTICE_CLOSED.getMessage());
				} else {
					sendInvalidArgMessage(sender);
				}
				break;
			case "type":
				// Set whether a kingdom is admin operated
				if(args.size() != 3) {
					sendInvalidArgMessage(sender);
					return;
				}
				String kingdomType = args.get(2);
				// Get kingdom from name
				if(!konquest.getKingdomManager().isKingdom(kingdomName)) {
					ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(kingdomName));
					return;
				}
				kingdom = konquest.getKingdomManager().getKingdom(kingdomName);
				assert kingdom != null;
				// Set kingdom type
				if (kingdomType.equalsIgnoreCase("admin")) {
					// Set admin operated flag
					kingdom.setIsAdminOperated(true);
					// Clear master
					kingdom.clearMaster();
					ChatUtil.sendNotice(sender, MessagePath.COMMAND_ADMIN_KINGDOM_NOTICE_ADMIN_SET.getMessage(kingdom.getName()));
				} else if (kingdomType.equalsIgnoreCase("player")) {
					// Clear admin operated flag
					kingdom.setIsAdminOperated(false);
					ChatUtil.sendNotice(sender, MessagePath.COMMAND_ADMIN_KINGDOM_NOTICE_ADMIN_CLEAR.getMessage(kingdom.getName()));
				} else {
					sendInvalidArgMessage(sender);
				}
				break;
			case "template":
				// View or change the kingdom's monument template
				// Get kingdom from name
				if(!konquest.getKingdomManager().isKingdom(kingdomName)) {
					ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(kingdomName));
					return;
				}
				kingdom = konquest.getKingdomManager().getKingdom(kingdomName);
				assert kingdom != null;
				if(args.size() == 2) {
					// Display current template
					ChatUtil.sendNotice(sender, MessagePath.COMMAND_KINGDOM_NOTICE_TEMPLATE_CURRENT.getMessage(kingdom.getMonumentTemplateName()));
				} else if(args.size() == 3) {
					// Change template
					String newTemplateName = args.get(2);
					if(!konquest.getSanctuaryManager().isValidTemplate(newTemplateName)) {
						ChatUtil.sendError(sender, MessagePath.COMMAND_KINGDOM_ERROR_INVALID_TEMPLATE.getMessage(newTemplateName));
						return;
					}
					konquest.getKingdomManager().menuChangeKingdomTemplate(kingdom, konquest.getSanctuaryManager().getTemplate(newTemplateName), player, true);
				} else {
					sendInvalidArgMessage(sender);
				}
				break;
			case "webcolor":
				// Get kingdom from name
				if(!konquest.getKingdomManager().isKingdom(kingdomName)) {
					ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(kingdomName));
					return;
				}
				kingdom = konquest.getKingdomManager().getKingdom(kingdomName);
				assert kingdom != null;

				if(args.size() == 2) {
					// Display current color
					int currentWebColor = kingdom.getWebColor();
					String colorStr = "default";
					if(currentWebColor != -1) {
						colorStr = ChatUtil.reverseLookupColorRGB(currentWebColor);
					}
					ChatUtil.sendNotice(sender, MessagePath.COMMAND_KINGDOM_NOTICE_WEB_COLOR_SHOW.getMessage(kingdom.getName(),colorStr));
				} else if(args.size() == 3) {
					// Set new color
					String colorStr = args.get(2);
					if(colorStr.equalsIgnoreCase("default")) {
						kingdom.setWebColor(-1);
					} else {
						int newWebColor = ChatUtil.lookupColorRGB(colorStr);
						if(newWebColor != -1) {
							kingdom.setWebColor(newWebColor);
						} else {
							// The provided color string could not be resolved to a color
							ChatUtil.sendError(sender, MessagePath.COMMAND_KINGDOM_ERROR_WEB_COLOR_INVALID.getMessage());
							return;
						}
					}
					// Update map
					konquest.getMapHandler().drawUpdateTerritory(kingdom);
					ChatUtil.sendNotice(sender, MessagePath.COMMAND_KINGDOM_NOTICE_WEB_COLOR_SET.getMessage(kingdom.getName(),colorStr));
				} else {
					// Incorrect arguments
					sendInvalidArgMessage(sender);
				}
				break;
			case "diplomacy":
				// View or change kingdom diplomacy relations
				if(!konquest.getKingdomManager().isKingdom(kingdomName)) {
					ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(kingdomName));
					return;
				}
				kingdom = konquest.getKingdomManager().getKingdom(kingdomName);
				assert kingdom != null;
				if (args.size() < 3) {
					sendInvalidArgMessage(sender);
				}

				String otherKingdomName = args.get(2);
				if (!konquest.getKingdomManager().isKingdom(otherKingdomName)) {
					ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(otherKingdomName));
					return;
				}
				KonKingdom otherKingdom = konquest.getKingdomManager().getKingdom(otherKingdomName);

				if(args.size() == 3) {
					// View relation with kingdom
					ChatUtil.sendNotice(sender, MessagePath.COMMAND_ADMIN_KINGDOM_NOTICE_DIPLOMACY.getMessage(kingdom.getName(),otherKingdom.getName(),kingdom.getActiveRelation(otherKingdom).toString()));
				} else if(args.size() == 4) {
					// Change diplomacy
					if(kingdom.isPeaceful() || otherKingdom.isPeaceful()) {
						// Cannot change diplomacy of peaceful kingdoms
						ChatUtil.sendError(sender, MessagePath.COMMAND_KINGDOM_ERROR_DIPLOMACY_PEACEFUL.getMessage());
						return;
					}
					String relationName = args.get(3);
					KonquestDiplomacyType relation;
					try {
						relation = KonquestDiplomacyType.valueOf(relationName.toUpperCase());
					} catch (IllegalArgumentException ex) {
						ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(relationName));
						return;
					}
					konquest.getKingdomManager().menuChangeKingdomRelation(kingdom, otherKingdom, relation, player, sender, true);
				} else {
					sendInvalidArgMessage(sender);
				}
				break;
			case "member":
				// Get kingdom from name
				if(!konquest.getKingdomManager().isKingdom(kingdomName)) {
					ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(kingdomName));
					return;
				}
				kingdom = konquest.getKingdomManager().getKingdom(kingdomName);
				assert kingdom != null;
				if(args.size() != 4) {
					sendInvalidArgMessage(sender);
					return;
				}
				String memberSubCmd = args.get(2);
				String playerName = args.get(3);
				KonOfflinePlayer offlinePlayer = konquest.getPlayerManager().getOfflinePlayerFromName(playerName);
				if(offlinePlayer == null) {
					ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(playerName));
					return;
				}
				playerName = offlinePlayer.getOfflineBukkitPlayer().getName();
				UUID id = offlinePlayer.getOfflineBukkitPlayer().getUniqueId();
				// Parse sub-command
				switch (memberSubCmd.toLowerCase()) {
					case "add":
						// Force a player to join a kingdom
						int memberAddStatus = konquest.getKingdomManager().assignPlayerKingdom(id,kingdom.getName(),true);
						switch(memberAddStatus) {
							case 0:
								// Success
								// Broadcast successful join
								ChatUtil.sendBroadcast(MessagePath.COMMAND_KINGDOM_BROADCAST_JOIN.getMessage(playerName,kingdom.getName()));
								break;
							case 5:
								// Player is already a member
								ChatUtil.sendError(sender, MessagePath.COMMAND_KINGDOM_ERROR_INVITE_MEMBER.getMessage(playerName));
								break;
							default:
								ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_FAILED.getMessage());
								break;
						}
						break;
					case "kick":
						// Remove a kingdom member
						int memberKickStatus = konquest.getKingdomManager().exilePlayerBarbarian(id,true,false,false,true);
						switch(memberKickStatus) {
							case 0:
								// Success
								ChatUtil.sendNotice(sender, MessagePath.COMMAND_KINGDOM_NOTICE_KICK.getMessage(playerName));
								break;
							case 1:
								// Player is already a barbarian
								ChatUtil.sendError(sender, MessagePath.COMMAND_KINGDOM_ERROR_KICK_BARBARIAN.getMessage());
								break;
							default:
								ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_FAILED.getMessage());
								break;
						}
						break;
					case "promote":
						// Promote a kingdom member to officer
						if(konquest.getKingdomManager().menuPromoteOfficer(offlinePlayer.getOfflineBukkitPlayer(), kingdom)) {
							// Passed
							ChatUtil.sendNotice(sender, MessagePath.GENERIC_NOTICE_SUCCESS.getMessage());
						} else {
							// Failed
							ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
						}
						break;
					case "demote":
						// Demote a kingdom officer to member
						if(konquest.getKingdomManager().menuDemoteOfficer(offlinePlayer.getOfflineBukkitPlayer(), kingdom)) {
							// Passed
							ChatUtil.sendNotice(sender, MessagePath.GENERIC_NOTICE_SUCCESS.getMessage());
						} else {
							// Failed
							ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
						}
						break;
					case "master":
						// Transfer kingdom master to another member
						konquest.getKingdomManager().menuTransferMaster(offlinePlayer.getOfflineBukkitPlayer(), kingdom, player);
						break;
					default:
						sendInvalidArgMessage(sender);
						return;
				}
				break;
			default:
				sendInvalidArgMessage(sender);
				break;
		}
	}

	@Override
	public List<String> tabComplete(Konquest konquest, CommandSender sender, List<String> args) {
		List<String> tabList = new ArrayList<>();
		int numArgs = args.size();
		if (numArgs == 1) {
			// suggest sub-commands
			tabList.add("menu");
			tabList.add("create");
			tabList.add("remove");
			tabList.add("rename");
			tabList.add("exile");
			tabList.add("access");
			tabList.add("type");
			tabList.add("template");
			tabList.add("webcolor");
			tabList.add("diplomacy");
			tabList.add("member");
			if (konquest.getKingdomManager().getIsCapitalSwapEnable()) {
				tabList.add("capital");
			}
		} else if (numArgs == 2) {
			// suggest kingdoms or template
			if(args.get(0).equalsIgnoreCase("create")) {
				tabList.addAll(konquest.getSanctuaryManager().getAllValidTemplateNames());
			} else if (args.get(0).equalsIgnoreCase("exile")) {
				tabList.addAll(konquest.getPlayerManager().getAllPlayerNames());
			} else {
				tabList.addAll(konquest.getKingdomManager().getKingdomNames());
			}
		} else if (numArgs == 3) {
			switch (args.get(0).toLowerCase()) {
				case "create":
				case "rename":
					// new name
					tabList.add("***");
					break;
				case "capital":
					// town name
					String targetKingdomName = args.get(1);
					if (konquest.getKingdomManager().isKingdom(targetKingdomName)) {
						tabList.addAll(konquest.getKingdomManager().getKingdom(targetKingdomName).getTownNames());
					}
					break;
				case "access":
					// Open or close the kingdom
					tabList.add("open");
					tabList.add("closed");
					break;
				case "type":
					// Set kingdom type
					tabList.add("admin");
					tabList.add("player");
					break;
				case "template":
					// Update monument template
					tabList.addAll(konquest.getSanctuaryManager().getAllTemplateNames());
					break;
				case "webcolor":
					// Color codes
					for (ColorRGB color : ColorRGB.values()) {
						tabList.add(color.getName());
					}
					tabList.add("#rrggbb");
					tabList.add("default");
					break;
				case "diplomacy":
					// Other kingdoms
					tabList.addAll(konquest.getKingdomManager().getKingdomNames());
					break;
				case "member":
					// Sub-commands
					tabList.add("add");
					tabList.add("kick");
					tabList.add("promote");
					tabList.add("demote");
					tabList.add("master");
					break;
				default:
					break;
			}
		} else if (numArgs == 4) {
			switch (args.get(0).toLowerCase()) {
				case "diplomacy":
					// relationship types
					String targetKingdomName = args.get(1);
					String otherKingdomName = args.get(2);
					if (konquest.getKingdomManager().isKingdom(targetKingdomName) && konquest.getKingdomManager().isKingdom(otherKingdomName)) {
						for (KonquestDiplomacyType relation : KonquestDiplomacyType.values()) {
							if (konquest.getKingdomManager().isValidRelationChoice(konquest.getKingdomManager().getKingdom(targetKingdomName),konquest.getKingdomManager().getKingdom(otherKingdomName),relation)) {
								tabList.add(relation.toString());
							}
						}
					}
					break;
				case "member":
					// Player membership management
					String memberKingdomName = args.get(1);
					if (!konquest.getKingdomManager().isKingdom(memberKingdomName)) {
						// Given name is not a kingdom
						break;
					}
					KonKingdom kingdom = konquest.getKingdomManager().getKingdom(memberKingdomName);
					switch (args.get(2).toLowerCase()) {
						case "add":
							for(KonOfflinePlayer offlinePlayer : konquest.getPlayerManager().getAllKonquestOfflinePlayers()) {
								String name = offlinePlayer.getOfflineBukkitPlayer().getName();
								if(name != null && !kingdom.isMember(offlinePlayer.getOfflineBukkitPlayer().getUniqueId())) {
									tabList.add(name);
								}
							}
							break;
						case "kick":
							for(KonOfflinePlayer offlinePlayer : konquest.getPlayerManager().getAllKonquestOfflinePlayers()) {
								String name = offlinePlayer.getOfflineBukkitPlayer().getName();
								if(name != null && kingdom.isMember(offlinePlayer.getOfflineBukkitPlayer().getUniqueId())) {
									tabList.add(name);
								}
							}
							break;
						case "promote":
							for(OfflinePlayer offlinePlayer : kingdom.getPlayerMembersOnly()) {
								String name = offlinePlayer.getName();
								if(name != null) {
									tabList.add(name);
								}
							}
							break;
						case "demote":
							for(OfflinePlayer offlinePlayer : kingdom.getPlayerOfficersOnly()) {
								String name = offlinePlayer.getName();
								if(name != null) {
									tabList.add(name);
								}
							}
							break;
						case "master":
							for(OfflinePlayer offlinePlayer : kingdom.getPlayerMembers()) {
								String name = offlinePlayer.getName();
								if(name != null && !kingdom.isMaster(offlinePlayer.getUniqueId())) {
									tabList.add(name);
								}
							}
							break;
					}
					break;
				default:
					break;
			}
		}
		return matchLastArgToList(tabList,args);
	}
}
