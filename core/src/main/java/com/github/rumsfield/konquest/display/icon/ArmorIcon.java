package com.github.rumsfield.konquest.display.icon;

import com.github.rumsfield.konquest.model.KonArmor;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ArmorIcon implements MenuIcon {

	private final KonArmor armor;
	private final boolean isAvailable;
	private final int population;
	private final int index;
	ItemStack item;
	
	public ArmorIcon(KonArmor armor, boolean isAvailable, int population, int index) {
		this.armor = armor;
		this.isAvailable = isAvailable;
		this.population = population;
		this.index = index;
		this.item = initItem();
	}
	
	private ItemStack initItem() {
		Material itemMaterial;
		if(isAvailable){
			itemMaterial = Material.DIAMOND_CHESTPLATE;
		}else {
			itemMaterial = Material.IRON_BARS;
		}
		ItemStack item = new ItemStack(itemMaterial,1);
		ItemMeta meta = item.getItemMeta();
		assert meta != null;
		meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);
		for(ItemFlag flag : ItemFlag.values()) {
			if(!meta.hasItemFlag(flag)) {
				meta.addItemFlags(flag);
			}
		}
		int totalCost = population * armor.getCost();
		List<String> loreList = new ArrayList<>();
		loreList.add(ChatColor.DARK_AQUA+""+armor.getBlocks());
    	loreList.add(ChatColor.YELLOW+MessagePath.LABEL_COST.getMessage()+": "+ChatColor.AQUA+totalCost);
    	if(isAvailable) {
    		loreList.add(ChatColor.GOLD+MessagePath.MENU_SHIELD_HINT.getMessage());
    	}
    	meta.setDisplayName(ChatColor.GOLD+armor.getId()+" "+MessagePath.LABEL_ARMOR.getMessage());
		meta.setLore(loreList);
		item.setItemMeta(meta);
		return item;
	}
	
	public KonArmor getArmor() {
		return armor;
	}

	@Override
	public int getIndex() {
		return index;
	}

	@Override
	public String getName() {
		return armor.getId();
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
