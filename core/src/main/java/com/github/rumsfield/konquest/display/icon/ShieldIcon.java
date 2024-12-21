package com.github.rumsfield.konquest.display.icon;

import com.github.rumsfield.konquest.manager.DisplayManager;
import com.github.rumsfield.konquest.model.KonShield;
import com.github.rumsfield.konquest.utility.CompatibilityUtil;
import com.github.rumsfield.konquest.utility.HelperUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ShieldIcon extends MenuIcon {

	private final KonShield shield;
	private final boolean isAvailable;
	private final int population;
	private final int land;
	ItemStack item;

	private final String loreColor = DisplayManager.loreFormat;
	private final String valueColor = DisplayManager.valueFormat;
	private final String hintColor = DisplayManager.hintFormat;
	
	public ShieldIcon(KonShield shield, boolean isAvailable, int population, int land, int index) {
		super(index);
		this.shield = shield;
		this.isAvailable = isAvailable;
		this.population = population;
		this.land = land;
		this.item = initItem();
	}
	
	private ItemStack initItem() {
		Material material = Material.SHIELD;
		if(!isAvailable) {
			material = Material.IRON_BARS;
		}
		int totalCost = shield.getCost() + (shield.getCostPerResident()*population) + (shield.getCostPerLand()*land);
		List<String> loreList = new ArrayList<>();
		loreList.add(HelperUtil.getTimeFormat(shield.getDurationSeconds(), ChatColor.DARK_AQUA));
    	loreList.add(loreColor+MessagePath.LABEL_COST.getMessage()+": "+valueColor+totalCost);
    	if(isAvailable) {
    		loreList.add(hintColor+MessagePath.MENU_SHIELD_HINT.getMessage());
    	}
		String name = ChatColor.GOLD+shield.getId()+" "+MessagePath.LABEL_SHIELD.getMessage();
		return CompatibilityUtil.buildItem(material, name, loreList, true);
	}
	
	public KonShield getShield() {
		return shield;
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
