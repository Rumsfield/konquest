package com.github.rumsfield.konquest.display.icon;

import com.github.rumsfield.konquest.api.model.KonquestTerritoryType;
import com.github.rumsfield.konquest.model.KonTown;
import com.github.rumsfield.konquest.utility.CompatibilityUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;

public class TownIcon extends MenuIcon {

	private final KonTown town;
	private final String contextColor;
	private final boolean isClickable;
	private final Material material;
	private final boolean isProtected;

	public TownIcon(KonTown town, OfflinePlayer viewer, String contextColor, int index, boolean isClickable) {
		super(index);
		this.town = town;
		this.contextColor = contextColor;
		this.isClickable = isClickable;
		if(town.isAttacked()) {
			this.material = Material.RED_WOOL;
		} else if(town.isArmored()) {
			this.material = Material.STONE_BRICKS;
		} else {
			this.material = Material.OBSIDIAN;
		}
		this.isProtected = town.isShielded();
		// Item Lore
		if(town.getTerritoryType().equals(KonquestTerritoryType.CAPITAL) &&
				town.getKingdom().isCapitalImmune()) {
			addAlert(MessagePath.LABEL_IMMUNITY.getMessage());
		}
		if(!town.isLordValid()) {
			addAlert(MessagePath.LABEL_NO_LORD.getMessage());
		}
		if(town.isAttacked()) {
			addAlert(MessagePath.PROTECTION_NOTICE_ATTACKED.getMessage());
		}
		if(town.getTerritoryType().equals(KonquestTerritoryType.CAPITAL)) {
			addProperty(MessagePath.TERRITORY_CAPITAL.getMessage());
		} else {
			addProperty(MessagePath.TERRITORY_TOWN.getMessage());
		}
		if(town.isOpen()) {
			addProperty(MessagePath.LABEL_OPEN.getMessage());
		}
		if(town.isArmored()) {
			addProperty(MessagePath.LABEL_ARMOR.getMessage());
		}
		if(town.isShielded()) {
			addProperty(MessagePath.LABEL_SHIELD.getMessage());
		}
		if(viewer != null) {
			String viewerRole = town.getPlayerRoleName(viewer);
			if (!viewerRole.isEmpty()) {
				addNameValue(MessagePath.LABEL_TOWN_ROLE.getMessage(), viewerRole);
			}
		}
		addNameValue(MessagePath.LABEL_POPULATION.getMessage(), town.getNumResidents());
		addNameValue(MessagePath.LABEL_POPULATION.getMessage(), town.getNumLand());
	}
	
	public KonTown getTown() {
		return town;
	}

	@Override
	public String getName() {
		return contextColor+town.getName();
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
