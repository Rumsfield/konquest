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

	public HelpCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }

    public void execute() {
		// k help [<page>]
		Player bukkitPlayer = (Player) getSender();
		final int MAX_LINES_PER_PAGE = 6;

		// Populate help lines
		List<String> lines = new ArrayList<>();
        for(CommandType cmd : CommandType.values()) {
        	String alias = "";
			if(!cmd.alias().equals("")) {
				alias = " ("+cmd.alias()+")";
			}
			String commandFormat = Labeler.format(cmd);
			String message = commandFormat+ChatColor.WHITE+": "+cmd.description()+ChatColor.LIGHT_PURPLE+alias;
			lines.add(message);
        }
		// Check for any help lines
		if(lines.isEmpty()) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
			return;
		}
		// Max number of pages given help lines
		int numLines = lines.size();
		int maxPages = (int)Math.ceil(((double)numLines)/MAX_LINES_PER_PAGE);
		// Get page index to display
		int page = 1;
		if (getArgs().length >= 2) {
			String pageArg = getArgs()[1];
			try {
				page = Integer.parseInt(pageArg);
			}
			catch (NumberFormatException ignored) {
				ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_HELP_ERROR_PAGE.getMessage());
				sendInvalidArgMessage(bukkitPlayer,CommandType.HELP);
				return;
			}
			page = Math.max(page,1);
			page = Math.min(page,maxPages);
		}
		// Display help lines
		String pageDisplay = page + "/" + maxPages;
		ChatUtil.sendNotice(bukkitPlayer, pageDisplay + " " + MessagePath.COMMAND_HELP_NOTICE_MESSAGE.getMessage());
		int startIdx = (page-1) * MAX_LINES_PER_PAGE;
		int endIdx = startIdx + MAX_LINES_PER_PAGE;
		for (int i = startIdx; i < endIdx && i < numLines; i++) {
			ChatUtil.sendMessage(bukkitPlayer, lines.get(i));
		}

    }
    
    @Override
	public List<String> tabComplete() {
		// k help [<page>]
		List<String> tabList = new ArrayList<>();
		final List<String> matchedTabList = new ArrayList<>();

		if(getArgs().length == 2) {
			tabList.add("#");

			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[1], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		}
		return matchedTabList;
	}
}
