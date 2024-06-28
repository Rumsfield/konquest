package com.github.rumsfield.konquest.command.admin;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.command.CommandBase;
import com.github.rumsfield.konquest.command.CommandType;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.Labeler;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HelpAdminCommand extends CommandBase {
	
	public HelpAdminCommand() {
		// Define name and sender support
		super("help",false, true);
		// None
		setOptionalArgs(true);
		// [<page>]
		addArgument(
				newArg("page",false,false)
		);
		// <command> [<page>]
		addArgument(
				newArg("command",false,true)
						.sub( newArg("page",false,false) )
		);
    }

	@Override
	public void execute(Konquest konquest, CommandSender sender, List<String> args) {
		final int MAX_LINES_PER_PAGE = 6;
		String argPage = "";
		String helpCommand = "";
		if (!args.isEmpty()) {
			if (AdminCommandType.contains(args.get(0))) {
				// First argument is a command
				helpCommand = args.get(0);
				// Check for second page argument
				if (args.size() == 2) {
					argPage = args.get(1);
				}
			} else {
				// First argument is a page number
				argPage = args.get(0);
			}
		}
		// Populate help lines
		boolean isBaseHelp = true;
		List<String> lines = new ArrayList<>();
		if (helpCommand.isEmpty()) {
			// Use command base usage
			for (AdminCommandType cmd : AdminCommandType.values()) {
				if (cmd.isSenderAllowed(sender) && sender.hasPermission(cmd.permission())) {
					// This command is supported for the current sender
					String message = cmd.baseUsage()+ChatColor.WHITE+" : "+cmd.description();
					lines.add(message);
				}
			}
		} else {
			// First argument is a command
			lines.addAll(AdminCommandType.getCommand(helpCommand).argumentUsage());
			isBaseHelp = false;
			helpCommand = CommandType.getCommand(helpCommand).toString().toLowerCase();
		}
		// Check for any help lines
		if (lines.isEmpty()) {
			ChatUtil.printConsoleError("Admin Command help is missing lines! Contact the plugin author.");
			ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
			return;
		}
		// Max number of pages given help lines
		int numLines = lines.size();
		int maxPages = (int)Math.ceil(((double)numLines)/MAX_LINES_PER_PAGE);
		// Get page index to display
		int page = 1;
		if (!argPage.isEmpty()) {
			try {
				page = Integer.parseInt(argPage);
			}
			catch (NumberFormatException ignored) {
				ChatUtil.sendError(sender, MessagePath.COMMAND_HELP_ERROR_PAGE.getMessage());
				sendInvalidArgMessage(sender);
				return;
			}
			page = Math.max(page,1);
			page = Math.min(page,maxPages);
		}
		// Display help lines
		String pageDisplay = page + "/" + maxPages;
		ChatUtil.sendNotice(sender, MessagePath.COMMAND_HELP_NOTICE_HEADER.getMessage(pageDisplay));
		if (page == 1 && maxPages > 1) {
			if (isBaseHelp) {
				ChatUtil.sendNotice(sender, MessagePath.COMMAND_ADMIN_HELP_NOTICE_PAGE.getMessage());
			} else {
				ChatUtil.sendNotice(sender, MessagePath.COMMAND_ADMIN_HELP_NOTICE_PAGE_COMMAND.getMessage(helpCommand));
			}
		}
		if (isBaseHelp) {
			ChatUtil.sendNotice(sender, MessagePath.COMMAND_ADMIN_HELP_NOTICE_DETAIL.getMessage());
		} else {
			ChatUtil.sendNotice(sender, MessagePath.COMMAND_HELP_NOTICE_COMMAND.getMessage());
		}
		int startIdx = (page-1) * MAX_LINES_PER_PAGE;
		int endIdx = startIdx + MAX_LINES_PER_PAGE;
		for (int i = startIdx; i < endIdx && i < numLines; i++) {
			ChatUtil.sendMessage(sender, "  "+lines.get(i));
		}
	}

	@Override
	public List<String> tabComplete(Konquest konquest, CommandSender sender, List<String> args) {
		List<String> tabList = new ArrayList<>();
		// Give suggestions
		if(args.size() == 1) {
			// Page number
			tabList.add("#");
			// Admin Command names
			for(AdminCommandType cmd : AdminCommandType.values()) {
				tabList.add(cmd.toString().toLowerCase());
			}
		} else if(args.size() == 2) {
			// Suggest page number when previous argument was a command
			if(AdminCommandType.contains(args.get(0))) {
				// Page number
				tabList.add("#");
			}
		}
		return matchLastArgToList(tabList,args);
	}
}
