package com.github.rumsfield.konquest.display.icon;

import com.github.rumsfield.konquest.model.KonTown;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class TownIcon implements MenuIcon {

	private final KonTown town;
	private final ChatColor contextColor;
	private final List<String> lore;
	private final int index;
	private final boolean isClickable;
	private final ItemStack item;
	
	public TownIcon(KonTown town, ChatColor contextColor, List<String> lore, int index, boolean isClickable) {
		this.town = town;
		this.contextColor = contextColor;
		this.isClickable = isClickable;
		this.lore = lore;
		this.index = index;
		this.item = initItem();
	}
	
	private ItemStack initItem() {
		// Determine material
		Material material = Material.OBSIDIAN;
		if(town.isAttacked()) {
			material = Material.RED_WOOL;
		} else if(town.isArmored()) {
			material = Material.STONE_BRICKS;
			
		}
		ItemStack item = new ItemStack(material,1);
		ItemMeta meta = item.getItemMeta();
		assert meta != null;
		// Add applicable labels
		List<String> loreList = new ArrayList<>();
		if(town.isAttacked()) {
			loreList.add(ChatColor.DARK_RED+""+ChatColor.ITALIC+MessagePath.PROTECTION_NOTICE_ATTACKED.getMessage());
		}
		if(town.isOpen()) {
			loreList.add(ChatColor.LIGHT_PURPLE+""+ChatColor.ITALIC+MessagePath.LABEL_OPEN.getMessage());
		}
		if(town.isArmored()) {
			loreList.add(ChatColor.LIGHT_PURPLE+""+ChatColor.ITALIC+MessagePath.LABEL_ARMOR.getMessage());
		}
		if(town.isShielded()) {
			meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);
			loreList.add(ChatColor.LIGHT_PURPLE+""+ChatColor.ITALIC+MessagePath.LABEL_SHIELD.getMessage());
		}
		//lore.add(ChatColor.GOLD+MessagePath.MENU_SCORE_HINT.getMessage());
		loreList.addAll(lore);
		for(ItemFlag flag : ItemFlag.values()) {
			if(!meta.hasItemFlag(flag)) {
				meta.addItemFlags(flag);
			}
		}
		meta.setDisplayName(contextColor+town.getName());
		meta.setLore(loreList);
		item.setItemMeta(meta);
		return item;
	}
	
	public KonTown getTown() {
		return town;
	}
	
	@Override
	public int getIndex() {
		return index;
	}

	@Override
	public String getName() {
		return town.getName();
	}

	@Override
	public ItemStack getItem() {
		return item;
	}

	@Override
	public boolean isClickable() {
		return isClickable;
	}

}
