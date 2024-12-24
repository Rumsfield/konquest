package com.github.rumsfield.konquest.display.icon;

import com.github.rumsfield.konquest.manager.DisplayManager;
import com.github.rumsfield.konquest.model.KonUpgrade;
import com.github.rumsfield.konquest.utility.CompatibilityUtil;
import com.github.rumsfield.konquest.utility.HelperUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class UpgradeIcon extends MenuIcon{

	private final KonUpgrade upgrade;
	private final int level;
	private final int cost;
	private final int pop;
	private final ItemStack item;

	private final String nameColor = DisplayManager.nameFormat;
	private final String loreColor = DisplayManager.loreFormat;
	private final String valueColor = DisplayManager.valueFormat;
	
	public UpgradeIcon(KonUpgrade upgrade, int level, int index, int cost, int pop) {
		super(index);
		this.upgrade = upgrade;
		this.level = level;
		this.cost = cost;
		this.pop = pop;
		this.item = initItem();
	}

	private ItemStack initItem() {
		List<String> loreList = new ArrayList<>();
		loreList.add(loreColor+MessagePath.LABEL_LEVEL.getMessage()+" "+level);
		loreList.add(loreColor+MessagePath.LABEL_COST.getMessage()+": "+valueColor+cost);
		loreList.add(loreColor+MessagePath.LABEL_POPULATION.getMessage()+": "+valueColor+pop);
		for(String line : HelperUtil.stringPaginate(upgrade.getLevelDescription(level))) {
			loreList.add(loreColor+line);
		}
		String name = nameColor+upgrade.getDescription();
		return CompatibilityUtil.buildItem(upgrade.getIcon(), name, loreList);
	}
	
	public KonUpgrade getUpgrade() {
		return upgrade;
	}
	
	public int getLevel() {
		return level;
	}

	@Override
	public String getName() {
		return upgrade.getDescription();
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