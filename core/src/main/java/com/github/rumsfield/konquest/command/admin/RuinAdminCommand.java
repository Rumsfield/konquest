package com.github.rumsfield.konquest.command.admin;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.command.CommandBase;
import com.github.rumsfield.konquest.manager.LootManager;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.model.KonPlayer.RegionType;
import com.github.rumsfield.konquest.model.KonRuin;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RuinAdminCommand extends CommandBase {

	public RuinAdminCommand() {
		// Define name and sender support
		super("ruin",true, true);
		// Define arguments
		// create <name>
		addArgument(
				newArg("create",true,false)
						.sub( newArg("name",false,false) )
		);
		// rename <ruin> <name>
		addArgument(
				newArg("rename",true,false)
						.sub( newArg("ruin",false,false)
								.sub (newArg("name",false,false) ) )
		);
		// loot <ruin> [<name>|default]
		addArgument(
				newArg("loot",true,false)
						.sub( newArg("ruin",false,true)
								.sub (newArg("name",false,false) ) )
		);
		// reset|remove|criticals|spawns <ruin>
		List<String> argNames = Arrays.asList("reset", "remove", "criticals", "spawns");
		addArgument(
				newArg(argNames,true,false)
						.sub( newArg("ruin",false,false) )
		);
    }

	@Override
	public void execute(Konquest konquest, CommandSender sender, List<String> args) {
		// Sender must be player
		KonPlayer player = konquest.getPlayerManager().getPlayer(sender);
		if (player == null) {
			sendInvalidSenderMessage(sender);
			return;
		}
		if (args.size() != 2 && args.size() != 3) {
			sendInvalidArgMessage(sender);
			return;
		}
		String cmdMode = args.get(0);
		String ruinName = args.get(1);
		// Check for invalid world
		Location playerLoc = player.getBukkitPlayer().getLocation();
		if(konquest.isWorldIgnored(playerLoc.getWorld())) {
			ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_INVALID_WORLD.getMessage());
			return;
		}
		// Parse sub-commands
		if(cmdMode.equalsIgnoreCase("create")) {
			if(konquest.validateName(ruinName,sender) != 0) {
        		return;
        	}
			// Create the new ruin
			if (konquest.getRuinManager().addRuin(playerLoc, ruinName)) {
				ChatUtil.sendNotice(sender, MessagePath.COMMAND_ADMIN_RUIN_NOTICE_CREATE.getMessage(ruinName));
			} else {
				ChatUtil.sendError(sender, MessagePath.COMMAND_ADMIN_RUIN_ERROR_CREATE.getMessage(ruinName));
			}
		} else if(cmdMode.equalsIgnoreCase("remove")) {
			// Check for valid ruin
			if(!konquest.getRuinManager().isRuin(ruinName)) {
				ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(ruinName));
                return;
			}
			// Remove the ruin
			if (konquest.getRuinManager().removeRuin(ruinName)) {
				ChatUtil.sendNotice(sender, MessagePath.COMMAND_ADMIN_RUIN_NOTICE_REMOVE.getMessage(ruinName));
			} else {
				ChatUtil.sendError(sender, MessagePath.COMMAND_ADMIN_RUIN_ERROR_REMOVE.getMessage(ruinName));
			}
		} else if(cmdMode.equalsIgnoreCase("rename")) {
			if (args.size() != 3) {
				sendInvalidArgMessage(sender);
				return;
			}
			String newRuinName = args.get(2);
			// Check for valid ruin
			if(!konquest.getRuinManager().isRuin(ruinName)) {
				ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(ruinName));
				return;
			}
			// Validate new name
			if(konquest.validateName(newRuinName,sender) != 0) {
				return;
			}
			// Rename the ruin
			if (konquest.getRuinManager().renameRuin(ruinName,newRuinName)) {
				ChatUtil.sendNotice(sender, MessagePath.COMMAND_ADMIN_RUIN_NOTICE_RENAME.getMessage(ruinName,newRuinName));
			} else {
				ChatUtil.sendError(sender, MessagePath.COMMAND_ADMIN_RUIN_ERROR_RENAME.getMessage(ruinName,newRuinName));
			}
		}  else if(cmdMode.equalsIgnoreCase("loot")) {
			// Check for valid ruin
			if(!konquest.getRuinManager().isRuin(ruinName)) {
				ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(ruinName));
				return;
			}
			KonRuin ruin = konquest.getRuinManager().getRuin(ruinName);
			if (args.size() == 2) {
				// Display current loot table
				String lootTableName = ruin.getLootTableName();
				ChatUtil.sendNotice(sender, MessagePath.GENERIC_NOTICE_LOOT_SHOW.getMessage(lootTableName));
			} else if (args.size() == 3) {
				// Set new loot table, either default or a custom name
				String newTableName = args.get(2);
				if (!konquest.getLootManager().isRuinLootTable(newTableName)) {
					ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_LOOT_UNKNOWN.getMessage(newTableName));
					return;
				}
				ruin.setLootTableName(newTableName);
				ChatUtil.sendNotice(sender, MessagePath.GENERIC_NOTICE_LOOT_SET.getMessage(newTableName));
			} else {
				sendInvalidArgMessage(sender);
			}
		} else if(cmdMode.equalsIgnoreCase("reset")) {
			// Check for valid ruin
			if(!konquest.getRuinManager().isRuin(ruinName)) {
				ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(ruinName));
				return;
			}
			// Reset ruin (restore critical blocks and respawn golems)
			if (konquest.getRuinManager().resetRuin(ruinName)) {
				ChatUtil.sendNotice(sender, MessagePath.COMMAND_ADMIN_RUIN_NOTICE_RESET.getMessage(ruinName));
			} else {
				ChatUtil.sendError(sender, MessagePath.COMMAND_ADMIN_RUIN_ERROR_RESET.getMessage(ruinName));
			}
		} else if(cmdMode.equalsIgnoreCase("criticals")) {
        	if(player.isSettingRegion()) {
        		ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_REGION.getMessage());
                return;
        	}
        	// Check for valid ruin
			if(!konquest.getRuinManager().isRuin(ruinName)) {
				ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(ruinName));
                return;
			}
			konquest.getRuinManager().getRuin(ruinName).clearCriticalLocations();
			player.settingRegion(RegionType.RUIN_CRITICAL);
        	ChatUtil.sendNotice(sender, MessagePath.COMMAND_ADMIN_RUIN_NOTICE_CRITICALS.getMessage(ruinName), ChatColor.LIGHT_PURPLE);
        	ChatUtil.sendNotice(sender, MessagePath.GENERIC_NOTICE_CLICK_AIR.getMessage());
		} else if(cmdMode.equalsIgnoreCase("spawns")) {
        	if(player.isSettingRegion()) {
        		ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_REGION.getMessage());
                return;
        	}
        	// Check for valid ruin
			if(!konquest.getRuinManager().isRuin(ruinName)) {
				ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(ruinName));
                return;
			}
			konquest.getRuinManager().getRuin(ruinName).clearSpawnLocations();
			player.settingRegion(RegionType.RUIN_SPAWN);
        	ChatUtil.sendNotice(sender, MessagePath.COMMAND_ADMIN_RUIN_NOTICE_SPAWNS.getMessage(ruinName), ChatColor.LIGHT_PURPLE);
        	ChatUtil.sendNotice(sender, MessagePath.GENERIC_NOTICE_CLICK_AIR.getMessage());
		} else {
			sendInvalidArgMessage(sender);
		}
	}

	@Override
	public List<String> tabComplete(Konquest konquest, CommandSender sender, List<String> args) {
		List<String> tabList = new ArrayList<>();
		if (args.size() == 1) {
			tabList.add("create");
			tabList.add("remove");
			tabList.add("rename");
			tabList.add("loot");
			tabList.add("reset");
			tabList.add("criticals");
			tabList.add("spawns");
		} else if (args.size() == 2) {
			switch (args.get(0).toLowerCase()) {
				case "create":
					tabList.add("***");
					break;
				case "remove":
				case "rename":
				case "loot":
				case "reset":
				case "criticals":
				case "spawns":
					tabList.addAll(konquest.getRuinManager().getRuinNames());
					break;
			}
		} else if (args.size() == 3) {
			if (args.get(0).equalsIgnoreCase("rename")) {
				// New name
				tabList.add("***");
			} else if (args.get(0).equalsIgnoreCase("loot")) {
				// Custom loot table name
				tabList.addAll(konquest.getLootManager().getRuinLootTableKeys());
			}
		}
		return matchLastArgToList(tabList,args);
	}
	
}
