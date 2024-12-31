package com.github.rumsfield.konquest.display.icon;

import com.github.rumsfield.konquest.manager.DisplayManager;
import com.github.rumsfield.konquest.model.KonMonumentTemplate;
import com.github.rumsfield.konquest.utility.CompatibilityUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class TemplateIcon extends MenuIcon {

	private final KonMonumentTemplate template;
	private final boolean isClickable;

	public TemplateIcon(KonMonumentTemplate template, int index, boolean isClickable) {
		super(index);
		this.template = template;
		this.isClickable = isClickable;
		// Item Lore
		if(template != null) {
			if(!template.isValid()) {
				if(template.isBlanking()) {
					addAlert(MessagePath.LABEL_UNAVAILABLE.getMessage());
				} else {
					addAlert(MessagePath.LABEL_INVALID.getMessage());
				}
			}
			addProperty(MessagePath.LABEL_MONUMENT_TEMPLATE.getMessage());
			addNameValue(MessagePath.LABEL_CRITICAL_HITS.getMessage(), template.getNumCriticals());
			addNameValue(MessagePath.LABEL_LOOT_CHESTS.getMessage(), template.getNumLootChests());
		}
	}
	
	public KonMonumentTemplate getTemplate() {
		return template;
	}

	@Override
	public String getName() {
		String result = "";
		if(template != null) {
			result = DisplayManager.nameFormat + template.getName();
		}
		return result;
	}

	@Override
	public ItemStack getItem() {
		return CompatibilityUtil.buildItem(Material.CRAFTING_TABLE, getName(), getLore());
	}

	@Override
	public boolean isClickable() {
		return isClickable;
	}

}
