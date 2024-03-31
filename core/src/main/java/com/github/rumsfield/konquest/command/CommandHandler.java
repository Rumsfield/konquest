package com.github.rumsfield.konquest.command;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.command.admin.AdminCommand;
import com.github.rumsfield.konquest.command.admin.AdminCommandType;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class CommandHandler implements TabExecutor {

	private final Konquest konquest;
	
	public CommandHandler(Konquest konquest) {
        this.konquest = konquest;
    }

	/**
	 * There is only 1 command: /konquest (alias /k).
	 * All functional commands are sub-commands of /k, specified in args.
	 * Parse the args to determine which sub-command is being used.
	 * Return false only when no sub-commands could be matched, else return true.
	 */
	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		// Special reload command for console
		if (sender instanceof ConsoleCommandSender) {
			if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
				// Console Reload
				konquest.reload();
				ChatUtil.printConsoleAlert("Reloaded Konquest configuration files.");
				return true;
			}
		}
		// Check for base permission
		if (!sender.hasPermission("konquest.command")) {
			ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_NO_PERMISSION.getMessage() + " konquest.command");
			return true;
		}
		// Special case when no sub-command given
		if (args.length == 0) {
			new KonquestCommand().execute(konquest, sender, args);
			return true;
		}
		// Check for unknown command
		if (!CommandType.contains(args[0])) {
			ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
			return false;
		}
		//Get command type. If args[0] is not a command, defaults to HELP
		CommandType commandArg = CommandType.getCommand(args[0]);
		// Handle admin commands differently from normal commands
		if (commandArg.equals(CommandType.ADMIN)) {
			// Command is an admin command
			// TODO change this
			new AdminCommand(konquest, sender, args).execute();
		} else {
			// Command is a normal command
			CommandBase konquestCommand = commandArg.command();
			if (!sender.hasPermission(commandArg.permission())) {
				// Sender does not have permission for this command
				ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_NO_PERMISSION.getMessage()+" "+commandArg.permission());
				return true;
			}
			if (!konquestCommand.validateSender(sender)) {
				// Sender is not supported for this command
				ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_NO_PLAYER.getMessage());
				return true;
			}
			int argStatus = konquestCommand.validateArgs(args);
			if (argStatus != 0) {
				// Failed argument validation
				if (argStatus == -1) {
					// No arguments or no matching command name (this should never happen)
					ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
				} else {
					// Get the invalid argument
					String requiredArg = konquestCommand.getSingleArgumentString(argStatus);
					ChatUtil.sendError(sender,requiredArg);
				}
				return false;
			}
			/* Passed all command checks */
			// Execute command
			konquestCommand.execute(konquest, sender, args);
		}
        return true;
    }

	@Override
	public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
		if (!sender.hasPermission("konquest.command")) {
			return Collections.emptyList();
		}
		List<String> tabList = new ArrayList<>();
		if (args.length == 1) {
			List<String> baseList = new ArrayList<>();
			for (CommandType cmd : CommandType.values()) {
				String suggestion = cmd.toString().toLowerCase();
				if (cmd.equals(CommandType.ADMIN)) {
					// Suggest admin command if any sub-command permissions are valid
					for (AdminCommandType subcmd : AdminCommandType.values()) {
						if (sender.hasPermission(subcmd.permission()) && !baseList.contains(suggestion)) {
							baseList.add(suggestion);
						}
					}
				} else if (sender.hasPermission(cmd.permission())) {
					// Suggest command if permission is valid
					baseList.add(suggestion);
				}
			}
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(args[0], baseList, tabList);
			Collections.sort(tabList);
		} else if (args.length >= 1){
			//Get command type. If args[0] is not a command, defaults to HELP
			CommandType commandArg = CommandType.getCommand(args[0]);
			// Handle admin commands differently from normal commands
			if (commandArg.equals(CommandType.ADMIN)) {
				// Command is an admin command
				// TODO change this
				tabList.addAll(new AdminCommand(konquest, sender, args).tabComplete());
			} else if (sender.hasPermission(commandArg.permission())) {
				// Command is a normal command and has permission
				// Tab-Complete command
				tabList.addAll(commandArg.command().tabComplete(konquest, sender, args));
			}
		}
        return tabList;
	}

}
