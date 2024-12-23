package com.github.rumsfield.konquest.display.icon;

import com.github.rumsfield.konquest.manager.DisplayManager;
import com.github.rumsfield.konquest.model.KonArmor;
import com.github.rumsfield.konquest.utility.CompatibilityUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ArmorIcon extends MenuIcon {

	private final KonArmor armor;
	private final int population;
	private final int land;
	ItemStack item;

	private final String loreColor = DisplayManager.loreFormat;
	private final String valueColor = DisplayManager.valueFormat;
	private final String hintColor = DisplayManager.hintFormat;
	
	public ArmorIcon(KonArmor armor, int population, int land, int index) {
		super(index);
		this.armor = armor;
		this.population = population;
		this.land = land;
		this.item = initItem();
	}
	
	private ItemStack initItem() {
		Material itemMaterial = Material.DIAMOND_CHESTPLATE;
		int totalCost = armor.getCost() + (armor.getCostPerResident()*population) + (armor.getCostPerLand()*land);
		List<String> loreList = new ArrayList<>();
		loreList.add(ChatColor.DARK_AQUA+""+armor.getBlocks());
    	loreList.add(loreColor+MessagePath.LABEL_COST.getMessage()+": "+valueColor+totalCost);
		loreList.add(hintColor+MessagePath.MENU_SHIELD_HINT.getMessage());
		String name = ChatColor.GOLD+armor.getId()+" "+MessagePath.LABEL_ARMOR.getMessage();
		return CompatibilityUtil.buildItem(itemMaterial, name, loreList, true);
	}
	
	public KonArmor getArmor() {
		return armor;
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
		return true;
	}
}
