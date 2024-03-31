package com.github.rumsfield.konquest.command;

import com.github.rumsfield.konquest.Konquest;
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

public class HelpCommand extends CommandBase{

	public HelpCommand() {
		super("help",false);
		// Define arguments:
		// /k help [<page>]
		CommandArgument arg0 = new CommandArgument(true);
		arg0.addArgOption("page",false);
		addArgument(arg0);
	}

	@Override
    public void execute(Konquest konquest, CommandSender sender, String[] args) {
		final int MAX_LINES_PER_PAGE = 6;
		// Populate help lines
		List<String> lines = new ArrayList<>();
        for (CommandType cmd : CommandType.values()) {
			if (cmd.isSenderAllowed(sender)) {
				// This command is supported for the current sender
				String alias = "";
				if (!cmd.alias().equals("")) {
					alias = " ("+cmd.alias()+")";
				}
				String message = cmd.usage()+ChatColor.WHITE+": "+cmd.description()+ChatColor.LIGHT_PURPLE+alias;
				lines.add(message);
			}
        }
		// Check for any help lines
		if (lines.isEmpty()) {
			ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
			return;
		}
		// Max number of pages given help lines
		int numLines = lines.size();
		int maxPages = (int)Math.ceil(((double)numLines)/MAX_LINES_PER_PAGE);
		// Get page index to display
		int page = 1;
		// Argument 0
		if (hasValidArg(args,0)) {
			try {
				page = Integer.parseInt(getArg(args,0));
			}
			catch (NumberFormatException ignored) {
				ChatUtil.sendError(sender, MessagePath.COMMAND_HELP_ERROR_PAGE.getMessage());
				sendInvalidArgMessage(sender,false);
				return;
			}
			page = Math.max(page,1);
			page = Math.min(page,maxPages);
		}
		// Display help lines
		String pageDisplay = page + "/" + maxPages;
		ChatUtil.sendNotice(sender, pageDisplay + " " + MessagePath.COMMAND_HELP_NOTICE_MESSAGE.getMessage());
		int startIdx = (page-1) * MAX_LINES_PER_PAGE;
		int endIdx = startIdx + MAX_LINES_PER_PAGE;
		for (int i = startIdx; i < endIdx && i < numLines; i++) {
			ChatUtil.sendMessage(sender, lines.get(i));
		}
    }
    
    @Override
	public List<String> tabComplete(Konquest konquest, CommandSender sender, String[] args) {
		List<String> tabList = new ArrayList<>();
		final List<String> matchedTabList = new ArrayList<>();
		int currentArgIndex = getLastArgIndex(args);
		// Suggestions based on argument index
		switch (currentArgIndex) {
			case 0:
				tabList.add("#");
				break;
		}
		// Trim down completion options based on current input
		StringUtil.copyPartialMatches(getArg(args,currentArgIndex), tabList, matchedTabList);
		Collections.sort(matchedTabList);
		return matchedTabList;
	}
}
