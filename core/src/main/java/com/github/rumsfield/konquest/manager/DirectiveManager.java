package com.github.rumsfield.konquest.manager;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.KonquestPlugin;
import com.github.rumsfield.konquest.model.KonDirective;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DirectiveManager {
	
	private final Konquest konquest;
	private final HashMap<KonDirective,Double> rewardTable;
	private boolean isEnabled;
	
	public DirectiveManager(Konquest konquest) {
		this.konquest = konquest;
		this.rewardTable = new HashMap<>();
		this.isEnabled = false;
	}
	
	public void initialize() {
		// Populate reward table, defaults to 10
		for(KonDirective dir : KonDirective.values()) {
			String dirName = dir.toString().toLowerCase();
			double reward = 10;
			if(konquest.getConfigManager().getConfig("core").contains("core.favor.rewards."+dirName)) {
				reward = konquest.getConfigManager().getConfig("core").getDouble("core.favor.rewards."+dirName,0.0);
			}
			rewardTable.put(dir,reward);
		}
		isEnabled = konquest.getConfigManager().getConfig("core").getBoolean("core.directive_quests",true);
		ChatUtil.printDebug("Directive Manager is ready, enabled: "+isEnabled);
	}
	
	public boolean isEnabled() {
		return isEnabled;
	}
	
	/**
	 * Primary method for updating progress and giving rewards for completion.
	 * @param player - KonPlayer assessing progress
	 * @param directive - KonDirective to assess
	 */
	public void updateDirectiveProgress(KonPlayer player, KonDirective directive) {
		if(isEnabled) {
			// Check for valid permissions
			if(!player.getBukkitPlayer().hasPermission(directive.permission())) {
				ChatUtil.printDebug("Player "+player.getBukkitPlayer().getName()+" does not have permission for directive "+ directive);
				return;
			}
			// Check to see if player is already at max progress (complete)
			int currentProgress = player.getDirectiveProgress(directive);
			if(currentProgress < directive.stages()) {
				// Increment directive progress
				int newProgress = currentProgress + 1;
				player.setDirectiveProgress(directive, newProgress);
				// Check to see if directive is now complete
				if(newProgress >= directive.stages()) {
					// Give reward
					ChatUtil.printDebug("Directive rewarded to player "+player.getBukkitPlayer().getName()+" for directive "+directive.title());
					double reward = rewardTable.get(directive);
		            if(KonquestPlugin.depositPlayer(player.getBukkitPlayer(), reward)) {
		            	ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.GENERIC_NOTICE_QUEST.getMessage()+": "+ChatColor.LIGHT_PURPLE+""+ChatColor.ITALIC+directive.title());
		            }
				}
			}
		}
	}
	
	public void displayBook(KonPlayer player) {
		if(isEnabled) {
			ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
			List<String> pages = new ArrayList<>();
			BookMeta meta = (BookMeta)Bukkit.getServer().getItemFactory().getItemMeta(Material.WRITTEN_BOOK);
			assert meta != null;
			// Format book cover
			meta.setAuthor("Konquest");
			meta.setGeneration(BookMeta.Generation.ORIGINAL);
			meta.setTitle(MessagePath.MENU_QUEST_TITLE.getMessage());
			String titlePage = "";
			titlePage = titlePage+ChatColor.DARK_PURPLE+ChatColor.BOLD+MessagePath.MENU_QUEST_TITLE.getMessage();
			titlePage = titlePage+ChatColor.RESET+"\n\n";
			titlePage = titlePage+ChatColor.BLACK+MessagePath.MENU_QUEST_INTRO_1.getMessage();
			titlePage = titlePage+ChatColor.RESET+"\n\n";
			titlePage = titlePage+ChatColor.BLACK+MessagePath.MENU_QUEST_INTRO_2.getMessage();
			pages.add(titlePage);
			// Format pages
			for(KonDirective dir : KonDirective.values()) {
				int currentProgress = player.getDirectiveProgress(dir);
				int stages = dir.stages();
				ChatColor progressColor = ChatColor.GRAY;
				if(currentProgress >= stages) {
					progressColor = ChatColor.DARK_GREEN;
				}
				String page = "";
				page = page+ChatColor.DARK_PURPLE+ChatColor.ITALIC+dir.title();
				page = page+ChatColor.RESET+"\n\n";
				page = page+ChatColor.BLACK+dir.description();
				page = page+ChatColor.RESET+"\n\n";
				page = page+progressColor+""+currentProgress+"/"+stages;
				page = page+ChatColor.RESET+"\n\n";
				page = page+ChatColor.BLACK+MessagePath.MENU_QUEST_REWARD.getMessage()+": "+ChatColor.DARK_GREEN+rewardTable.get(dir);
				pages.add(page);
			}
			meta.setPages(pages);
			// Display book
			book.setItemMeta(meta);
			player.getBukkitPlayer().openBook(book);
		}
	}
}
