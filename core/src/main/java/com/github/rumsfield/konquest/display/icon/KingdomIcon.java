package com.github.rumsfield.konquest.display.icon;

import com.github.rumsfield.konquest.api.model.KonquestRelationshipType;
import com.github.rumsfield.konquest.model.KonKingdom;
import com.github.rumsfield.konquest.model.KonTown;
import com.github.rumsfield.konquest.utility.CompatibilityUtil;
import com.github.rumsfield.konquest.utility.Labeler;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class KingdomIcon extends MenuIcon {

	private final KonKingdom kingdom;
	private final String contextColor;
	private final KonquestRelationshipType relation;
	private final boolean isClickable;

	public KingdomIcon(KonKingdom kingdom, String contextColor, KonquestRelationshipType relation, int index, boolean isClickable) {
		super(index);
		this.kingdom = kingdom;
		this.contextColor = contextColor;
		this.relation = relation;
		this.isClickable = isClickable;
		// Item Lore
		int numKingdomLand = 0;
		for(KonTown town : kingdom.getCapitalTowns()) {
			numKingdomLand += town.getNumLand();
		}
		if(kingdom.isOfflineProtected()) {
			addAlert(MessagePath.LABEL_PROTECTED.getMessage());
		}
		if(kingdom.isAdminOperated()) {
			addProperty(MessagePath.LABEL_ADMIN_KINGDOM.getMessage());
		} else {
			addProperty(MessagePath.LABEL_KINGDOM.getMessage());
		}
		if (relation != null) {
			addProperty(Labeler.lookup(relation));
		}
		if(kingdom.isPeaceful()) {
			addProperty(MessagePath.LABEL_PEACEFUL.getMessage());
		}
		if(kingdom.isOpen()) {
			addProperty(MessagePath.LABEL_OPEN.getMessage());
		}
		if(kingdom.isSmallest()) {
			addProperty(MessagePath.LABEL_SMALLEST.getMessage());
		}
		addNameValue(MessagePath.LABEL_MEMBERS.getMessage(), kingdom.getNumMembers());
		addNameValue(MessagePath.LABEL_TOWNS.getMessage(), kingdom.getNumTowns());
		addNameValue(MessagePath.LABEL_LAND.getMessage(), numKingdomLand);
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
		Material iconMat = Material.DIAMOND_HELMET;
		if (relation != null && relation.equals(KonquestRelationshipType.FRIENDLY)) {
			iconMat = Material.GOLDEN_HELMET;
		}
		return CompatibilityUtil.buildItem(iconMat, getName(), getLore(), kingdom.isOfflineProtected());
	}

	@Override
	public boolean isClickable() {
		return isClickable;
	}

}
