package com.github.rumsfield.konquest.display.icon;

import com.github.rumsfield.konquest.manager.DisplayManager;
import com.github.rumsfield.konquest.model.KonUpgrade;
import com.github.rumsfield.konquest.utility.CompatibilityUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.inventory.ItemStack;

public class UpgradeIcon extends MenuIcon{

	private final KonUpgrade upgrade;
	private final int level;

	
	public UpgradeIcon(KonUpgrade upgrade, int level, int index, int cost, int pop) {
		super(index);
		this.upgrade = upgrade;
		this.level = level;
		// Item Lore
		addNameValue(MessagePath.LABEL_LEVEL.getMessage(),level);
		addNameValue(MessagePath.LABEL_COST.getMessage(),cost);
		addNameValue(MessagePath.LABEL_POPULATION.getMessage(),pop);
		addDescription(upgrade.getLevelDescription(level));
	}
	
	public KonUpgrade getUpgrade() {
		return upgrade;
	}
	
	public int getLevel() {
		return level;
	}

	@Override
	public String getName() {
		return DisplayManager.nameFormat + upgrade.getDescription();
	}

	@Override
	public ItemStack getItem() {
		return CompatibilityUtil.buildItem(upgrade.getIcon(), getName(), getLore());
	}
	
	@Override
	public boolean isClickable() {
		return true;
	}

}