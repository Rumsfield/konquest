package com.github.rumsfield.konquest.command;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.KonquestPlugin;
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
import java.util.Arrays;
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
		// Get arguments
		List<String> arguments = new ArrayList<>(Arrays.asList(args));
		// Check for no arguments
		if (arguments.isEmpty() || arguments.get(0).isEmpty()) {
			new KonquestCommand().execute(konquest, sender, Collections.emptyList());
			return true;
		}
		// Extract command name
		String konquestCommandName = arguments.remove(0);
		// Special console commands
		if (sender instanceof ConsoleCommandSender) {
			// Reload command
			if (konquestCommandName.equalsIgnoreCase("reload")) {
				konquest.reload();
				ChatUtil.printConsoleAlert("Reloaded Konquest configuration files.");
				return true;
			}
			// Version command
			if (konquestCommandName.equalsIgnoreCase("version")) {
				KonquestPlugin.printLogo();
				konquest.getPlugin().printVersion();
				return true;
			}
		}
		// Check for base permission
		if (!sender.hasPermission("konquest.command")) {
			ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_NO_PERMISSION.getMessage() + " konquest.command");
			return true;
		}
		// Get command type. If the name is not a command, defaults to HELP
		CommandType commandType = CommandType.getCommand(konquestCommandName);
		// Check for command-specific permission
		if (!commandType.isSenderHasPermission(sender)) {
			// Sender does not have permission for this command
			ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_NO_PERMISSION.getMessage()+" "+commandType.permission());
			return true;
		}
		// Get command
		CommandBase konquestCommand = commandType.command();
		// Check for supported sender (player or console)
		if (!konquestCommand.validateSender(sender)) {
			// Sender is not supported for this command
			ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_NO_PLAYER.getMessage());
			return true;
		}
		// Check for correct command arguments
		// TODO revisit this
		/*
		int argStatus = konquestCommand.validateArgs(args);
		if (argStatus != 0) {
			// Failed argument validation
			if (argStatus == -1) {
				// No arguments (this should never happen)
				ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
			} else {
				// Get the invalid argument
				String requiredArg = konquestCommand.getSingleArgumentString(argStatus);
				ChatUtil.sendError(sender,requiredArg);
			}
			return false;
		}
		 */
		/* Passed all command checks */
		// Execute command
		konquestCommand.execute(konquest, sender, arguments);
        return true;
    }

	@Override
	public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
		if (!sender.hasPermission("konquest.command")) {
			return Collections.emptyList();
		}
		// Get arguments
		List<String> arguments = new ArrayList<>(Arrays.asList(args));
		List<String> tabList = new ArrayList<>();
		if (arguments.size() == 1) {
			// Suggest command names
			String konquestCommandName = arguments.get(0);
			List<String> baseList = new ArrayList<>();
			for (CommandType cmd : CommandType.values()) {
				String suggestion = cmd.toString().toLowerCase();
				if (cmd.equals(CommandType.ADMIN)) {
					// Suggest admin command only if any sub-command permissions are valid
					for (AdminCommandType subcmd : AdminCommandType.values()) {
						if (sender.hasPermission(subcmd.permission())) {
							baseList.add(suggestion);
							break;
						}
					}
				} else if (sender.hasPermission(cmd.permission())) {
					// Suggest command if permission is valid
					baseList.add(suggestion);
				}
			}
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(konquestCommandName, baseList, tabList);
			Collections.sort(tabList);
		} else if (arguments.size() > 1) {
			// Suggest command arguments
			// Extract command name
			String konquestCommandName = arguments.remove(0);
			// Get command type. If it's not a command, defaults to HELP
			CommandType commandArg = CommandType.getCommand(konquestCommandName);
			// Check for permission
			if (commandArg.isSenderHasPermission(sender)) {
				// Tab-Complete command
				tabList.addAll(commandArg.command().tabComplete(konquest, sender, arguments));
			}
		}
        return tabList;
	}

}
