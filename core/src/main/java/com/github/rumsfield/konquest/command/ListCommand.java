package com.github.rumsfield.konquest.command;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ListCommand extends CommandBase {

	public enum ListType {

		KINGDOM     (MessagePath.LABEL_KINGDOM.getMessage()),
		TOWN        (MessagePath.LABEL_TOWN.getMessage()),
		CAMP        (MessagePath.LABEL_CAMP.getMessage()),
		RUIN        (MessagePath.LABEL_RUIN.getMessage()),
		SANCTUARY   (MessagePath.LABEL_SANCTUARY.getMessage()),
		TEMPLATE	(MessagePath.LABEL_MONUMENT_TEMPLATE.getMessage());

		private final String label;

		ListType(String label) {
			this.label = label;
		}

		public String getLabel() {
			return label;
		}

	}

	public ListCommand() {
		// Define name and sender support
		super("list",false, false);
		// None
		setOptionalArgs(true);
		// [kingdom|town|camp|sanctuary|ruin|template] [<page>]
		List<String> argNames = Arrays.asList("kingdom", "town", "camp", "ruin", "sanctuary", "template");
		addArgument(
				newArg(argNames,true,true)
						.sub( newArg("page",false,false) )
		);
    }
	
	// Display a paged list of names
    public void execute(Konquest konquest, CommandSender sender, List<String> args) {
    	if (args.size() > 2) {
			ChatUtil.printDebug("List arg size is "+args.size());
			sendInvalidArgMessage(sender);
			return;
		}
		// Determine list mode
		ListType mode = ListType.KINGDOM;
		if (args.size() >= 1) {
			switch (args.get(0).toLowerCase()) {
				case "kingdom":
					mode = ListType.KINGDOM;
					break;
				case "town":
					mode = ListType.TOWN;
					break;
				case "camp":
					mode = ListType.CAMP;
					break;
				case "sanctuary":
					mode = ListType.SANCTUARY;
					break;
				case "ruin":
					mode = ListType.RUIN;
					break;
				case "template":
					mode = ListType.TEMPLATE;
					break;
				default:
					sendInvalidArgMessage(sender);
					return;
			}
		}
		// Populate list lines
		List<String> lines = new ArrayList<>();
		switch(mode) {
			case KINGDOM:
				lines.addAll(konquest.getKingdomManager().getKingdomNames());
				break;
			case TOWN:
				lines.addAll(konquest.getKingdomManager().getTownNames());
				break;
			case CAMP:
				lines.addAll(konquest.getCampManager().getCampNames());
				break;
			case RUIN:
				lines.addAll(konquest.getRuinManager().getRuinNames());
				break;
			case SANCTUARY:
				lines.addAll(konquest.getSanctuaryManager().getSanctuaryNames());
				break;
			case TEMPLATE:
				lines.addAll(konquest.getSanctuaryManager().getAllTemplateNames());
				break;
			default :
				ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
				return;
		}
		Collections.sort(lines);

		if(lines.isEmpty()) {
			// Nothing to display
			String header = MessagePath.COMMAND_LIST_NOTICE_HEADER.getMessage(mode.getLabel()) + " 0/0";
			ChatUtil.sendNotice(sender, header);
		} else {
			// Display paged lines to player
			int numLines = lines.size(); // should be 1 or more
			int MAX_LINES = 8;
			int totalPages = (int)Math.ceil(((double)numLines)/MAX_LINES); // 1-based
			totalPages = Math.max(totalPages, 1);
			int page = 1; // 1-based
			if (args.size() == 2) {
				try {
					page = Integer.parseInt(args.get(1));
				}
				catch (NumberFormatException ex) {
					ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_INTERNAL_MESSAGE.getMessage(ex.getMessage()));
					return;
				}
			}
			// Clamp page index
			page = Math.min(page,totalPages);
			page = Math.max(page,1);
			// Determine line start and end
			int startIdx = (page-1) * MAX_LINES;
			int endIdx = startIdx + MAX_LINES;
			// Display lines to player
			String header = MessagePath.COMMAND_LIST_NOTICE_HEADER.getMessage(mode.getLabel()) + " "+page+"/"+totalPages;
			ChatUtil.sendNotice(sender,header);
			List<String> pageLines = new ArrayList<>();
			for (int i = startIdx; i < endIdx && i < numLines; i++) {
				String line = ""+ChatColor.GOLD+(i+1)+". "+ChatColor.AQUA+lines.get(i);
				pageLines.add(line);
			}
			ChatUtil.sendCommaMessage(sender,pageLines);
		}

    }
    
    @Override
	public List<String> tabComplete(Konquest konquest, CommandSender sender, List<String> args) {
		List<String> tabList = new ArrayList<>();
		if(args.size() == 1) {
			tabList.add("kingdom");
			tabList.add("town");
			tabList.add("camp");
			tabList.add("ruin");
			tabList.add("sanctuary");
			tabList.add("template");
		} else if(args.size() == 2) {
			tabList.add("#");
		}
		return matchLastArgToList(tabList,args);
	}

}
