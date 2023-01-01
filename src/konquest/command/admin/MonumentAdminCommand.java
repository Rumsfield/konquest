package konquest.command.admin;

import konquest.Konquest;
import konquest.command.CommandBase;
import konquest.model.KonMonumentTemplate;
import konquest.model.KonPlayer;
import konquest.model.KonPlayer.RegionType;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

public class MonumentAdminCommand extends CommandBase {
	
	public MonumentAdminCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }

    public void execute() {
    	// k admin monument create|remove|show <name>
    	if (getArgs().length != 4) {
    		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
            return;
        } else {
        	Player bukkitPlayer = (Player) getSender();
        	World bukkitWorld = bukkitPlayer.getWorld();
        	KonPlayer player = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);
        	
        	if(!getKonquest().isWorldValid(bukkitWorld)) {
        		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_WORLD.getMessage());
                return;
        	}
        	
        	if(!getKonquest().getPlayerManager().isOnlinePlayer(bukkitPlayer)) {
    			ChatUtil.printDebug("Failed to find non-existent player");
    			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
    			return;
    		}
        	
        	if(player.isSettingRegion()) {
        		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_REGION.getMessage());
                return;
        	}

        	String cmdMode = getArgs()[2];
        	String templateName = getArgs()[3];
        	if(cmdMode.equalsIgnoreCase("create")) {
        		// Creating a new template
        		// Validate name first
        		if(getKonquest().validateName(templateName,bukkitPlayer) != 0) {
            		return;
            	}
        		// Begin region setting flow
        		player.settingRegion(RegionType.MONUMENT);
        		player.setRegionTemplateName(templateName);
            	// Send flow messages
            	ChatUtil.sendNotice((Player) getSender(), MessagePath.COMMAND_ADMIN_MONUMENT_NOTICE_CREATE_1.getMessage(), ChatColor.LIGHT_PURPLE);
            	ChatUtil.sendNotice((Player) getSender(), MessagePath.GENERIC_NOTICE_CLICK_AIR.getMessage());
            	
        	} else if(cmdMode.equalsIgnoreCase("remove")) {
        		// Confirm name is a template
        		if(!getKonquest().getSanctuaryManager().isTemplate(templateName)) {
        			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(templateName));
        			return;
        		}
        		// Remove template
        		player.settingRegion(RegionType.NONE);
        		getKonquest().getSanctuaryManager().removeMonumentTemplate(templateName);
        		ChatUtil.sendNotice((Player) getSender(), MessagePath.COMMAND_ADMIN_MONUMENT_NOTICE_REMOVE.getMessage(templateName));
        		
        	} else if(cmdMode.equalsIgnoreCase("show")) {
        		//boolean isValid = getKonquest().getKingdomManager().getKingdom(kingdomName).getMonumentTemplate().isValid();
        		//if(isValid) {
        		
        		// Confirm name is a template
        		if(!getKonquest().getSanctuaryManager().isTemplate(templateName)) {
        			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(templateName));
        			return;
        		}
        		
        		// Show template even when invalid
    			player.settingRegion(RegionType.NONE);
    			Location loc0 = getKonquest().getSanctuaryManager().getTemplate(templateName).getCornerOne();
    			Location loc1 = getKonquest().getSanctuaryManager().getTemplate(templateName).getCornerTwo();
    			player.startMonumentShow(loc0, loc1);
    			String sanctuaryName = getKonquest().getSanctuaryManager().getSanctuaryNameOfTemplate(templateName);
    			ChatUtil.sendNotice((Player) getSender(), MessagePath.COMMAND_ADMIN_MONUMENT_NOTICE_SHOW.getMessage(templateName,sanctuaryName));
        			
        		//} else {
        		//	ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_ADMIN_MONUMENT_ERROR_NONE.getMessage(templateName));
        		//}
        		
        	} else if(cmdMode.equalsIgnoreCase("status")) {
        		// Confirm name is a template
        		if(!getKonquest().getSanctuaryManager().isTemplate(templateName)) {
        			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(templateName));
        			return;
        		}
        		/*
        		 * Monument Template status info:
        		 * - Name
        		 * - Sanctuary
        		 * - Is valid
        		 * - Is blanking
        		 * - Total blocks
        		 * - Critical blocks
        		 * - Loot chests
        		 */
        		ChatColor loreColor = ChatColor.YELLOW;
        		ChatColor valueColor = ChatColor.AQUA;
        		KonMonumentTemplate template = getKonquest().getSanctuaryManager().getTemplate(templateName);
        		String tempName = template.getName();
        		String sanctuaryName = getKonquest().getSanctuaryManager().getSanctuaryNameOfTemplate(templateName);
        		String isValid = String.format("%s", template.isValid());
        		String isBlanking = String.format("%s", template.isBlanking());
        		String allBlocks = String.format("%d", template.getNumBlocks());
        		String critBlocks = String.format("%d", template.getNumCriticals());
        		String lootChests = String.format("%d", template.getNumLootChests());
        		
        		ChatUtil.sendNotice(bukkitPlayer, "Template "+tempName+" Status");
        		ChatUtil.sendMessage(bukkitPlayer, loreColor+"Sanctuary: "+valueColor+sanctuaryName);
        		ChatUtil.sendMessage(bukkitPlayer, loreColor+"Valid: "+valueColor+isValid);
        		ChatUtil.sendMessage(bukkitPlayer, loreColor+"Modified: "+valueColor+isBlanking);
        		ChatUtil.sendMessage(bukkitPlayer, loreColor+"Total Blocks: "+valueColor+allBlocks);
        		ChatUtil.sendMessage(bukkitPlayer, loreColor+"Critical Blocks: "+valueColor+critBlocks);
        		ChatUtil.sendMessage(bukkitPlayer, loreColor+"Loot Chests: "+valueColor+lootChests);
        		
        	} else {
        		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
        	}
        }
    }
    
    @Override
	public List<String> tabComplete() {
    	// k admin monument create|remove|show <name>
		List<String> tabList = new ArrayList<>();
		final List<String> matchedTabList = new ArrayList<>();
		if(getArgs().length == 3) {
			tabList.add("create");
			tabList.add("remove");
			tabList.add("show");
			tabList.add("status");
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[2], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		} else if(getArgs().length == 4) {
			String subCmd = getArgs()[2];
			if(subCmd.equalsIgnoreCase("create")) {
				tabList.add("***");
			} else {
				tabList.addAll(getKonquest().getSanctuaryManager().getAllTemplateNames());
			}
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[3], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		}
		return matchedTabList;
	}
}
