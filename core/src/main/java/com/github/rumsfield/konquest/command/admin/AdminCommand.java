package com.github.rumsfield.konquest.command.admin;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.command.CommandBase;
import com.github.rumsfield.konquest.command.KonquestCommand;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AdminCommand is unique because it is not a command by itself, instead it acts as another
 * handler for all the other admin commands.
 */
public class AdminCommand extends CommandBase {

    public AdminCommand() {
		// Define name and sender support
		super("admin",false, true);
		// <sub-command>
		addArgument(
				newArg("sub-command",false,false)
		);
    }

	@Override
    public void execute(Konquest konquest, CommandSender sender, List<String> args) {
		// Default command is help
		AdminCommandType adminCommand = AdminCommandType.HELP;
		// Extract admin command name, if exists
		if (!args.isEmpty()) {
			String konquestAdminCommandName = args.remove(0);
			adminCommand = AdminCommandType.getCommand(konquestAdminCommandName);
		}
		// Check for permissions
		if (!adminCommand.isSenderHasPermission(sender)) {
			ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_NO_PERMISSION.getMessage()+" "+AdminCommandType.HELP.permission());
			return;
		}
		// Get command
		CommandBase konquestAdminCommand = adminCommand.command();
		// Check for supported sender (player or console)
		if (!konquestAdminCommand.validateSender(sender)) {
			// Sender is not supported for this command
			ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_NO_PLAYER.getMessage());
			return;
		}
		/* Passed all command checks */
		// Execute command
		konquestAdminCommand.execute(konquest, sender, args);
    }

	@Override
    public List<String> tabComplete(Konquest konquest, CommandSender sender, List<String> args) {
		List<String> tabList = new ArrayList<>();
        if (args.size() == 1) {
			// Suggest admin sub-commands when sender has permission
        	List<String> baseList = new ArrayList<>();
        	for(AdminCommandType cmd : AdminCommandType.values()) {
        		if(cmd.isSenderHasPermission(sender)) {
        			baseList.add(cmd.toString().toLowerCase());
        		}
    		}
        	// Trim down completion options based on current input
			StringUtil.copyPartialMatches(args.get(0), baseList, tabList);
			Collections.sort(tabList);
        } else if (args.size() >= 2) {
			// Extract admin command name
			String konquestAdminCommandName = args.remove(0);
			if (AdminCommandType.contains(konquestAdminCommandName)) {
				// Get command type
				AdminCommandType commandArg = AdminCommandType.getCommand(konquestAdminCommandName);
				// Check for permission
				if (commandArg.isSenderHasPermission(sender)) {
					// Tab-Complete command
					tabList.addAll(commandArg.command().tabComplete(konquest, sender, args));
				}
			}
        }
        return tabList;
    }

}
