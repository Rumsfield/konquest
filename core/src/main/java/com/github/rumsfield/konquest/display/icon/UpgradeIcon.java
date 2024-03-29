package com.github.rumsfield.konquest.display.icon;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.manager.DisplayManager;
import com.github.rumsfield.konquest.model.KonUpgrade;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class UpgradeIcon implements MenuIcon{

	KonUpgrade upgrade;
	int level;
	int cost;
	int pop;
	int index;
	ItemStack item;

	private final String loreColor = DisplayManager.loreFormat;
	private final String valueColor = DisplayManager.valueFormat;
	
	public UpgradeIcon(KonUpgrade upgrade, int level, int index, int cost, int pop) {
		this.upgrade = upgrade;
		this.level = level;
		this.index = index;
		this.cost = cost;
		this.pop = pop;
		this.item = initItem();
	}

	private ItemStack initItem() {
		ItemStack item = new ItemStack(upgrade.getIcon(),1);
		ItemMeta meta = item.getItemMeta();
		assert meta != null;
		for(ItemFlag flag : ItemFlag.values()) {
			if(!meta.hasItemFlag(flag)) {
				meta.addItemFlags(flag);
			}
		}
		List<String> loreList = new ArrayList<>();
		loreList.add(loreColor+MessagePath.LABEL_LEVEL.getMessage()+" "+level);
		loreList.add(loreColor+MessagePath.LABEL_COST.getMessage()+": "+valueColor+cost);
		loreList.add(loreColor+MessagePath.LABEL_POPULATION.getMessage()+": "+valueColor+pop);
		for(String line : Konquest.stringPaginate(upgrade.getLevelDescription(level))) {
			loreList.add(loreColor+line);
		}
		meta.setDisplayName(ChatColor.GOLD+upgrade.getDescription());
		meta.setLore(loreList);
		item.setItemMeta(meta);
		return item;
	}
	
	public KonUpgrade getUpgrade() {
		return upgrade;
	}
	
	public int getLevel() {
		return level;
	}

	@Override
	public int getIndex() {
		return index;
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