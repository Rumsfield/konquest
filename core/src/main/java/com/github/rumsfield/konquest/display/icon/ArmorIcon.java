package com.github.rumsfield.konquest.display.icon;

import com.github.rumsfield.konquest.manager.DisplayManager;
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
	private final int land;
	private final int index;
	ItemStack item;

	private final String loreColor = DisplayManager.loreFormat;
	private final String valueColor = DisplayManager.valueFormat;
	private final String hintColor = DisplayManager.hintFormat;
	
	public ArmorIcon(KonArmor armor, boolean isAvailable, int population, int land, int index) {
		this.armor = armor;
		this.isAvailable = isAvailable;
		this.population = population;
		this.land = land;
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
		int totalCost = armor.getCost() + (armor.getCostPerResident()*population) + (armor.getCostPerLand()*land);
		List<String> loreList = new ArrayList<>();
		loreList.add(ChatColor.DARK_AQUA+""+armor.getBlocks());
    	loreList.add(loreColor+MessagePath.LABEL_COST.getMessage()+": "+valueColor+totalCost);
    	if(isAvailable) {
    		loreList.add(hintColor+MessagePath.MENU_SHIELD_HINT.getMessage());
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
