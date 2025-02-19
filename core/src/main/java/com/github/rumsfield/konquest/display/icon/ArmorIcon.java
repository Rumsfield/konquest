package com.github.rumsfield.konquest.display.icon;

import com.github.rumsfield.konquest.KonquestPlugin;
import com.github.rumsfield.konquest.manager.DisplayManager;
import com.github.rumsfield.konquest.model.KonArmor;
import com.github.rumsfield.konquest.utility.CompatibilityUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class ArmorIcon extends MenuIcon {

	private final KonArmor armor;
	
	public ArmorIcon(KonArmor armor, int cost, int index) {
		super(index);
		this.armor = armor;
		// Item Lore
		addNameValue(MessagePath.LABEL_ARMOR.getMessage(), ""+ChatColor.DARK_AQUA+armor.getBlocks());
		addNameValue(MessagePath.LABEL_COST.getMessage(), KonquestPlugin.getCurrencyFormat(cost));
		addHint(MessagePath.MENU_HINT_CHARGE.getMessage());
	}
	
	public KonArmor getArmor() {
		return armor;
	}

	@Override
	public String getName() {
		return DisplayManager.nameFormat+armor.getId()+" "+MessagePath.LABEL_ARMOR.getMessage();
	}

	@Override
	public ItemStack getItem() {
		return CompatibilityUtil.buildItem(Material.CHAINMAIL_CHESTPLATE, getName(), getLore(), true);
	}

	@Override
	public boolean isClickable() {
		return true;
	}
}
