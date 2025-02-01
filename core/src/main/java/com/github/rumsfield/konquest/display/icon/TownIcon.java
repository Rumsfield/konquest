package com.github.rumsfield.konquest.display.icon;

import com.github.rumsfield.konquest.api.model.KonquestRelationshipType;
import com.github.rumsfield.konquest.api.model.KonquestTerritoryType;
import com.github.rumsfield.konquest.model.KonCapital;
import com.github.rumsfield.konquest.model.KonTown;
import com.github.rumsfield.konquest.utility.CompatibilityUtil;
import com.github.rumsfield.konquest.utility.Labeler;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class TownIcon extends MenuIcon {

	private final KonTown town;
	private final String contextColor;
	private final boolean isClickable;
	private final Material material;

	public TownIcon(KonTown town, String contextColor, KonquestRelationshipType relation, int index, boolean isClickable) {
		super(index);
		this.town = town;
		this.contextColor = contextColor;
		this.isClickable = isClickable;
		if(town.isAttacked()) {
			// Town / Capital is under attack
			this.material = Material.REDSTONE_BLOCK;
		} else if (town instanceof KonCapital) {
			// Capital materials
			if (town.getKingdom().isCapitalImmune()) {
				// Capital is immune
				this.material = Material.BEDROCK;
			} else if (town.isArmored()) {
				// Capital has armor
				this.material = Material.NETHER_BRICKS;
			} else {
				// Default capital material
				this.material = Material.NETHERITE_BLOCK;
			}
		} else {
			// Town materials
			if (town.isArmored()) {
				// Town has armor
				this.material = Material.STONE_BRICKS;
			} else {
				// Default town material
				this.material = Material.OBSIDIAN;
			}
		}
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
		if (relation != null) {
			addProperty(Labeler.lookup(relation));
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
		addNameValue(MessagePath.LABEL_POPULATION.getMessage(), town.getNumResidents());
		addNameValue(MessagePath.LABEL_LAND.getMessage(), town.getNumLand());
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
		return CompatibilityUtil.buildItem(material, getName(), getLore(), town.isShielded());
	}

	@Override
	public boolean isClickable() {
		return isClickable;
	}

}
