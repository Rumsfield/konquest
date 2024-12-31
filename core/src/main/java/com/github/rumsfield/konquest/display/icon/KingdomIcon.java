package com.github.rumsfield.konquest.display.icon;

import com.github.rumsfield.konquest.model.KonKingdom;
import com.github.rumsfield.konquest.utility.CompatibilityUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class KingdomIcon extends MenuIcon {

	private final KonKingdom kingdom;
	private final String contextColor;
	private final boolean isClickable;
	private final Material material;
	private final boolean isProtected;

	public KingdomIcon(KonKingdom kingdom, String contextColor, int index, boolean isClickable, boolean isViewer) {
		super(index);
		this.kingdom = kingdom;
		this.contextColor = contextColor;
		this.isClickable = isClickable;
		this.material = isViewer ? Material.GOLDEN_HELMET : Material.DIAMOND_HELMET;
		this.isProtected = kingdom.isOfflineProtected();
		// Item Lore
		if(kingdom.isOfflineProtected()) {
			addAlert(MessagePath.LABEL_PROTECTED.getMessage());
		}
		if(isViewer) {
			addProperty(MessagePath.DIPLOMACY_SELF.getMessage());
		}
		if(kingdom.isAdminOperated()) {
			addProperty(MessagePath.LABEL_ADMIN_KINGDOM.getMessage());
		} else {
			addProperty(MessagePath.LABEL_KINGDOM.getMessage());
		}
		if(kingdom.isPeaceful()) {
			addProperty(MessagePath.LABEL_PEACEFUL.getMessage());
		}
		if(kingdom.isOpen()) {
			addProperty(MessagePath.LABEL_OPEN.getMessage());
		}
		addNameValue(MessagePath.LABEL_TOWNS.getMessage(), kingdom.getNumTowns());
		addNameValue(MessagePath.LABEL_MEMBERS.getMessage(), kingdom.getNumMembers());
	}
	
	public KonKingdom getKingdom() {
		return kingdom;
	}

	@Override
	public String getName() {
		return contextColor+kingdom.getName();
	}

	@Override
	public ItemStack getItem() {
		return CompatibilityUtil.buildItem(material, getName(), getLore(), isProtected);
	}

	@Override
	public boolean isClickable() {
		return isClickable;
	}

}
