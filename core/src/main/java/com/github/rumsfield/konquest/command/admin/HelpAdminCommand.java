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
	
	public HelpAdminCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }

    public void execute() {
		// k admin help [<page>]
		Player bukkitPlayer = (Player) getSender();
		final int MAX_LINES_PER_PAGE = 6;

		// Populate help lines
		List<String> lines = new ArrayList<>();
		for(AdminCommandType cmd : AdminCommandType.values()) {
			String commandFormat = Labeler.format(cmd);
			String message = commandFormat+ChatColor.WHITE+": "+cmd.description();
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
		if (getArgs().length >= 3) {
			String pageArg = getArgs()[2];
			try {
				page = Integer.parseInt(pageArg);
			}
			catch (NumberFormatException ignored) {
				ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_HELP_ERROR_PAGE.getMessage());
				sendInvalidArgMessage(bukkitPlayer,AdminCommandType.HELP);
				return;
			}
			page = Math.max(page,1);
			page = Math.min(page,maxPages);
		}
		// Display help lines
		String pageDisplay = page + "/" + maxPages;
		ChatUtil.sendNotice(bukkitPlayer, pageDisplay + " " + MessagePath.COMMAND_ADMIN_HELP_NOTICE_MESSAGE.getMessage());
		int startIdx = (page-1) * MAX_LINES_PER_PAGE;
		int endIdx = startIdx + MAX_LINES_PER_PAGE;
		for (int i = startIdx; i < endIdx && i < numLines; i++) {
			ChatUtil.sendMessage(bukkitPlayer, lines.get(i));
		}
    }
    
    @Override
	public List<String> tabComplete() {
		// k admin help [<page>]
		List<String> tabList = new ArrayList<>();
		final List<String> matchedTabList = new ArrayList<>();

		if(getArgs().length == 3) {
			tabList.add("#");

			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[2], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		}
		return matchedTabList;
	}
}
