package com.github.rumsfield.konquest.display.icon;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.manager.DisplayManager;
import com.github.rumsfield.konquest.model.KonShield;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ShieldIcon implements MenuIcon {

	private final KonShield shield;
	private final boolean isAvailable;
	private final int population;
	private final int index;
	ItemStack item;

	private final String loreColor = DisplayManager.loreFormat;
	private final String valueColor = DisplayManager.valueFormat;
	private final String hintColor = DisplayManager.hintFormat;
	
	public ShieldIcon(KonShield shield, boolean isAvailable, int population, int index) {
		this.shield = shield;
		this.isAvailable = isAvailable;
		this.population = population;
		this.index = index;
		this.item = initItem();
	}
	
	private ItemStack initItem() {
		Material mat = Material.SHIELD;
		if(!isAvailable) {
			mat = Material.IRON_BARS;
		}
		ItemStack item = new ItemStack(mat,1);
		ItemMeta meta = item.getItemMeta();
		assert meta != null;
		meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);
		for(ItemFlag flag : ItemFlag.values()) {
			if(!meta.hasItemFlag(flag)) {
				meta.addItemFlags(flag);
			}
		}
		int totalCost = population * shield.getCost();
		List<String> loreList = new ArrayList<>();
		loreList.add(Konquest.getTimeFormat(shield.getDurationSeconds(), ChatColor.DARK_AQUA));
    	loreList.add(loreColor+MessagePath.LABEL_COST.getMessage()+": "+valueColor+totalCost);
    	if(isAvailable) {
    		loreList.add(hintColor+MessagePath.MENU_SHIELD_HINT.getMessage());
    	}
    	meta.setDisplayName(ChatColor.GOLD+shield.getId()+" "+MessagePath.LABEL_SHIELD.getMessage());
		meta.setLore(loreList);
		item.setItemMeta(meta);
		return item;
	}
	
	public KonShield getShield() {
		return shield;
	}

	@Override
	public int getIndex() {
		return index;
	}

	@Override
	public String getName() {
		return shield.getId();
	}

	@Override
	public ItemStack getItem() {
		return item;
	}

	@Override
	public boolean isClickable() {
		return isAvailable;
	}
}
