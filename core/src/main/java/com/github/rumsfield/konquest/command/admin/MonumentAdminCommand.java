package com.github.rumsfield.konquest.command.admin;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.command.CommandBase;
import com.github.rumsfield.konquest.model.KonMonumentTemplate;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.model.KonPlayer.RegionType;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MonumentAdminCommand extends CommandBase {
	
	public MonumentAdminCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }

    public void execute() {
    	// k admin monument create|remove|reset|show|status <name> [<cost>]
    	if (getArgs().length != 4 && getArgs().length != 5) {
    		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS_ADMIN.getMessage());
		} else {
        	Player bukkitPlayer = (Player) getSender();
        	World bukkitWorld = bukkitPlayer.getWorld();
        	KonPlayer player = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);
        	
        	if(!getKonquest().isWorldValid(bukkitWorld)) {
        		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INVALID_WORLD.getMessage());
                return;
        	}
        	
        	if(!getKonquest().getPlayerManager().isOnlinePlayer(bukkitPlayer)) {
    			ChatUtil.printDebug("Failed to find non-existent player");
    			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
    			return;
    		}
        	
        	if(player.isSettingRegion()) {
        		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_REGION.getMessage());
                return;
        	}

        	String cmdMode = getArgs()[2];
        	String templateName = getArgs()[3];
        	if(cmdMode.equalsIgnoreCase("create")) {
				// Creating a new template
				double costNum = 0;
				if (getArgs().length == 5) {
					try {
						costNum = Double.parseDouble(getArgs()[4]);
					} catch (NumberFormatException e) {
						ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_ADMIN_MONUMENT_ERROR_VALUE.getMessage());
						ChatUtil.sendError(bukkitPlayer, e.getMessage());
						return;
					}
					costNum = Math.max(costNum, 0);
				}
				// Validate name first
				if (getKonquest().validateName(templateName, bukkitPlayer) != 0) {
					return;
				}
				// Begin region setting flow
				player.settingRegion(RegionType.MONUMENT);
				player.setRegionTemplateName(templateName);
				player.setRegionTemplateCost(costNum);
				// Send flow messages
				ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_ADMIN_MONUMENT_NOTICE_CREATE_1.getMessage(), ChatColor.LIGHT_PURPLE);
				ChatUtil.sendNotice(bukkitPlayer, MessagePath.GENERIC_NOTICE_CLICK_AIR.getMessage());
			} else if(cmdMode.equalsIgnoreCase("reset")) {
				// Resetting an existing template fields
				if (getArgs().length != 5) {
					ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INVALID_PARAMETERS_ADMIN.getMessage());
					return;
				}
				double costNum = 0;
				try {
					costNum = Double.parseDouble(getArgs()[4]);
				} catch (NumberFormatException e) {
					ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_ADMIN_MONUMENT_ERROR_VALUE.getMessage());
					ChatUtil.sendError(bukkitPlayer, e.getMessage());
					return;
				}
				costNum = Math.max(costNum, 0);
				// Check for existing valid template name
				if(!getKonquest().getSanctuaryManager().isTemplate(templateName)) {
					ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(templateName));
					return;
				}
				// Stop any template blanking
				String sanctuaryName = getKonquest().getSanctuaryManager().getSanctuaryNameOfTemplate(templateName);
				if(getKonquest().getSanctuaryManager().isSanctuary(sanctuaryName)) {
					getKonquest().getSanctuaryManager().getSanctuary(sanctuaryName).stopTemplateBlanking(templateName);
				}
				// Begin region setting flow
				player.settingRegion(RegionType.MONUMENT);
				player.setRegionTemplateName(templateName);
				player.setRegionTemplateCost(costNum);
				// Send flow messages
				ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_ADMIN_MONUMENT_NOTICE_CREATE_1.getMessage(), ChatColor.LIGHT_PURPLE);
				ChatUtil.sendNotice(bukkitPlayer, MessagePath.GENERIC_NOTICE_CLICK_AIR.getMessage());

        	} else if(cmdMode.equalsIgnoreCase("remove")) {
        		// Confirm name is a template
        		if(!getKonquest().getSanctuaryManager().isTemplate(templateName)) {
        			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(templateName));
        			return;
        		}
        		// Remove template
        		player.settingRegion(RegionType.NONE);
        		getKonquest().getSanctuaryManager().removeMonumentTemplate(templateName);
        		ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_ADMIN_MONUMENT_NOTICE_REMOVE.getMessage(templateName));
        		
        	} else if(cmdMode.equalsIgnoreCase("show")) {
        		// Confirm name is a template
        		if(!getKonquest().getSanctuaryManager().isTemplate(templateName)) {
        			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(templateName));
        			return;
        		}
        		// Show template even when invalid
    			player.settingRegion(RegionType.NONE);
    			Location loc0 = getKonquest().getSanctuaryManager().getTemplate(templateName).getCornerOne();
    			Location loc1 = getKonquest().getSanctuaryManager().getTemplate(templateName).getCornerTwo();
    			player.startMonumentShow(loc0, loc1);
    			String sanctuaryName = getKonquest().getSanctuaryManager().getSanctuaryNameOfTemplate(templateName);
    			ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_ADMIN_MONUMENT_NOTICE_SHOW.getMessage(templateName,sanctuaryName));

        	} else if(cmdMode.equalsIgnoreCase("status")) {
        		// Confirm name is a template
        		if(!getKonquest().getSanctuaryManager().isTemplate(templateName)) {
        			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(templateName));
        			return;
        		}
        		ChatColor loreColor = ChatColor.YELLOW;
        		ChatColor valueColor = ChatColor.AQUA;
        		KonMonumentTemplate template = getKonquest().getSanctuaryManager().getTemplate(templateName);
        		String tempName = template.getName();
        		String sanctuaryName = getKonquest().getSanctuaryManager().getSanctuaryNameOfTemplate(templateName);
				String cost = String.format("%f", template.getCost());
        		String isValid = String.format("%s", template.isValid());
        		String isBlanking = String.format("%s", template.isBlanking());
        		String allBlocks = String.format("%d", template.getNumBlocks());
        		String critBlocks = String.format("%d", template.getNumCriticals());
        		String lootChests = String.format("%d", template.getNumLootChests());
        		ChatUtil.sendNotice(bukkitPlayer, MessagePath.LABEL_MONUMENT_TEMPLATE.getMessage()+" "+tempName);
				ChatUtil.sendMessage(bukkitPlayer, loreColor+MessagePath.LABEL_SANCTUARY.getMessage()+": "+valueColor+sanctuaryName);
				ChatUtil.sendMessage(bukkitPlayer, loreColor+MessagePath.LABEL_COST.getMessage()+": "+valueColor+cost);
        		ChatUtil.sendMessage(bukkitPlayer, loreColor+MessagePath.LABEL_VALID.getMessage()+": "+valueColor+isValid);
        		ChatUtil.sendMessage(bukkitPlayer, loreColor+MessagePath.LABEL_MODIFIED.getMessage()+": "+valueColor+isBlanking);
        		ChatUtil.sendMessage(bukkitPlayer, loreColor+MessagePath.LABEL_BLOCKS.getMessage()+": "+valueColor+allBlocks);
        		ChatUtil.sendMessage(bukkitPlayer, loreColor+MessagePath.LABEL_CRITICAL_HITS.getMessage()+": "+valueColor+critBlocks);
        		ChatUtil.sendMessage(bukkitPlayer, loreColor+MessagePath.LABEL_LOOT_CHESTS.getMessage()+": "+valueColor+lootChests);
        	} else {
        		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INVALID_PARAMETERS_ADMIN.getMessage());
        	}
        }
    }
    
    @Override
	public List<String> tabComplete() {
    	// k admin monument create|remove|reset|show <name> [<cost>]
		List<String> tabList = new ArrayList<>();
		final List<String> matchedTabList = new ArrayList<>();
		if(getArgs().length == 3) {
			// Suggest sub-commands
			tabList.add("create");
			tabList.add("remove");
			tabList.add("reset");
			tabList.add("show");
			tabList.add("status");
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[2], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		} else if(getArgs().length == 4) {
			// Suggest new name or existing name
			String subCmd = getArgs()[2];
			if(subCmd.equalsIgnoreCase("create")) {
				tabList.add("***");
			} else {
				tabList.addAll(getKonquest().getSanctuaryManager().getAllTemplateNames());
			}
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[3], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		} else if(getArgs().length == 5) {
			// Suggest cost
			String subCmd = getArgs()[2];
			if(subCmd.equalsIgnoreCase("create") || subCmd.equalsIgnoreCase("reset")) {
				tabList.add("#");
			}
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[4], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		}
		return matchedTabList;
	}
}
