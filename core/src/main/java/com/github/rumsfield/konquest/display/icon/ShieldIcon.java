package com.github.rumsfield.konquest.display.icon;

import com.github.rumsfield.konquest.manager.DisplayManager;
import com.github.rumsfield.konquest.model.KonShield;
import com.github.rumsfield.konquest.utility.CompatibilityUtil;
import com.github.rumsfield.konquest.utility.HelperUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class ShieldIcon extends MenuIcon {

	private final KonShield shield;
	
	public ShieldIcon(KonShield shield, int cost, int index) {
		super(index);
		this.shield = shield;
		// Item Lore
		addNameValue(MessagePath.LABEL_SHIELD.getMessage(), HelperUtil.getTimeFormat(shield.getDurationSeconds(), ChatColor.DARK_AQUA));
		addNameValue(MessagePath.LABEL_COST.getMessage(), cost);
		addHint(MessagePath.MENU_HINT_CHARGE.getMessage());
	}
	
	public KonShield getShield() {
		return shield;
	}

	@Override
	public String getName() {
		return DisplayManager.nameFormat+shield.getId()+" "+MessagePath.LABEL_SHIELD.getMessage();
	}

	@Override
	public ItemStack getItem() {
		return CompatibilityUtil.buildItem(Material.SHIELD, getName(), getLore(), true);
	}

	@Override
	public boolean isClickable() {
		return true;
	}
}
