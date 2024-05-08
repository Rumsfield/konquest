package com.github.rumsfield.konquest.command.admin;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.command.CommandBase;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SanctuaryAdminCommand extends CommandBase {

	public SanctuaryAdminCommand() {
		// Define name and sender support
		super("sanctuary",true, true);
		// Define arguments
		// create <name>
		addArgument(
				newArg("create",true,false)
						.sub( newArg("name",false,false) )
		);
		// rename <sanctuary> <name>
		addArgument(
				newArg("rename",true,false)
						.sub( newArg("sanctuary",false,false)
								.sub (newArg("name",false,false) ) )
		);
		// remove <sanctuary>
		addArgument(
				newArg("remove",true,false)
						.sub( newArg("sanctuary",false,false) )
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
		String sanctuaryName = args.get(1);
		// Check for invalid world
		Location playerLoc = player.getBukkitPlayer().getLocation();
		if(konquest.isWorldIgnored(playerLoc.getWorld())) {
			ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_INVALID_WORLD.getMessage());
			return;
		}
		// Parse sub-commands
		if(cmdMode.equalsIgnoreCase("create")) {
			if(konquest.validateName(sanctuaryName,sender) != 0) {
        		return;
        	}
			// Create the new sanctuary
			if (konquest.getSanctuaryManager().addSanctuary(playerLoc, sanctuaryName)) {
				ChatUtil.sendNotice(sender, MessagePath.COMMAND_ADMIN_SANCTUARY_NOTICE_CREATE.getMessage(sanctuaryName));
			} else {
				ChatUtil.sendError(sender, MessagePath.COMMAND_ADMIN_SANCTUARY_ERROR_CREATE.getMessage(sanctuaryName));
			}
		} else if(cmdMode.equalsIgnoreCase("remove")) {
			// Check for valid sanctuary
			if(!konquest.getSanctuaryManager().isSanctuary(sanctuaryName)) {
				ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(sanctuaryName));
				return;
			}
			// Remove the sanctuary
			if (konquest.getSanctuaryManager().removeSanctuary(sanctuaryName)) {
				ChatUtil.sendNotice(sender, MessagePath.COMMAND_ADMIN_SANCTUARY_NOTICE_REMOVE.getMessage(sanctuaryName));
			} else {
				ChatUtil.sendError(sender, MessagePath.COMMAND_ADMIN_SANCTUARY_ERROR_REMOVE.getMessage(sanctuaryName));
			}
		} else if(cmdMode.equalsIgnoreCase("rename")) {
			if (args.size() != 3) {
				sendInvalidArgMessage(sender);
				return;
			}
			String newSanctuaryName = args.get(2);
			// Check for valid sanctuary
			if(!konquest.getSanctuaryManager().isSanctuary(sanctuaryName)) {
				ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(sanctuaryName));
				return;
			}
			// Validate new name
			if(konquest.validateName(newSanctuaryName,sender) != 0) {
				return;
			}
			// Rename the sanctuary
			if (konquest.getSanctuaryManager().renameSanctuary(sanctuaryName,newSanctuaryName)) {
				ChatUtil.sendNotice(sender, MessagePath.COMMAND_ADMIN_SANCTUARY_NOTICE_RENAME.getMessage(sanctuaryName,newSanctuaryName));
			} else {
				ChatUtil.sendError(sender, MessagePath.COMMAND_ADMIN_SANCTUARY_ERROR_RENAME.getMessage(sanctuaryName,newSanctuaryName));
			}
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
		} else if (args.size() == 2) {
			switch (args.get(0).toLowerCase()) {
				case "create":
					tabList.add("***");
					break;
				case "remove":
				case "rename":
					tabList.addAll(konquest.getRuinManager().getRuinNames());
					break;
			}
		} else if (args.size() == 3) {
			if (args.get(0).equalsIgnoreCase("rename")) {
				// New name
				tabList.add("***");
			}
		}
		return matchLastArgToList(tabList,args);
	}
	
}
