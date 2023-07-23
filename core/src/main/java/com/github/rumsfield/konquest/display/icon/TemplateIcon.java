package com.github.rumsfield.konquest.display.icon;

import com.github.rumsfield.konquest.manager.DisplayManager;
import com.github.rumsfield.konquest.model.KonMonumentTemplate;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class TemplateIcon implements MenuIcon {

	private final KonMonumentTemplate template;
	private final String contextColor;
	private final List<String> lore;
	private final int index;
	private final boolean isClickable;

	private final String loreColor = DisplayManager.loreFormat;
	private final String valueColor = DisplayManager.valueFormat;
	private final String alertColor = DisplayManager.alertFormat;

	public TemplateIcon(KonMonumentTemplate template, String contextColor, List<String> lore, int index, boolean isClickable) {
		this.template = template;
		this.contextColor = contextColor;
		this.lore = lore;
		this.index = index;
		this.isClickable = isClickable;
	}
	
	public KonMonumentTemplate getTemplate() {
		return template;
	}
	
	@Override
	public int getIndex() {
		return index;
	}

	@Override
	public String getName() {
		String result = "";
		if(template != null) {
			result = template.getName();
		}
		return result;
	}

	@Override
	public ItemStack getItem() {
		ItemStack item = new ItemStack(Material.CRAFTING_TABLE,1);
		ItemMeta meta = item.getItemMeta();
		assert meta != null;
		for(ItemFlag flag : ItemFlag.values()) {
			if(!meta.hasItemFlag(flag)) {
				meta.addItemFlags(flag);
			}
		}
		List<String> itemLore = new ArrayList<>();
		if(template != null) {
			if(!template.isValid()) {
				if(template.isBlanking()) {
					itemLore.add(alertColor + MessagePath.LABEL_UNAVAILABLE.getMessage());
				} else {
					itemLore.add(alertColor + MessagePath.LABEL_INVALID.getMessage());
				}
			}
			itemLore.add(loreColor+MessagePath.LABEL_NAME.getMessage()+": "+valueColor+template.getName());
			itemLore.add(loreColor+MessagePath.LABEL_CRITICAL_HITS.getMessage()+": "+valueColor+template.getNumCriticals());
			itemLore.add(loreColor+MessagePath.LABEL_LOOT_CHESTS.getMessage()+": "+valueColor+template.getNumLootChests());
		}
		itemLore.addAll(lore);
		meta.setDisplayName(contextColor+MessagePath.LABEL_MONUMENT_TEMPLATE.getMessage());
		meta.setLore(itemLore);
		item.setItemMeta(meta);
		return item;
	}

	@Override
	public boolean isClickable() {
		return isClickable;
	}

}
