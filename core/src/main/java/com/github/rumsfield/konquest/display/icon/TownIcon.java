package com.github.rumsfield.konquest.display.icon;

import com.github.rumsfield.konquest.api.model.KonquestTerritoryType;
import com.github.rumsfield.konquest.manager.DisplayManager;
import com.github.rumsfield.konquest.model.KonTown;
import com.github.rumsfield.konquest.utility.CompatibilityUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TownIcon extends MenuIcon {

	private final KonTown town;
	private final OfflinePlayer viewer;
	private final String contextColor;
	private final List<String> alerts;
	private final List<String> properties;
	private final List<String> lore;
	private final boolean isClickable;
	private final ItemStack item;

	private final String alertColor = DisplayManager.alertFormat;
	private final String propertyColor = DisplayManager.propertyFormat;
	private final String loreColor = DisplayManager.loreFormat;
	private final String valueColor = DisplayManager.valueFormat;
	
	public TownIcon(KonTown town, String contextColor, List<String> lore, int index, boolean isClickable) {
		super(index);
		this.town = town;
		this.viewer = null;
		this.contextColor = contextColor;
		this.alerts = Collections.emptyList();
		this.properties = Collections.emptyList();
		this.lore = lore;
		this.isClickable = isClickable;
		this.item = initItem();
	}

	public TownIcon(KonTown town, OfflinePlayer viewer, String contextColor, List<String> alerts, List<String> properties, List<String> lore, int index, boolean isClickable) {
		super(index);
		this.town = town;
		this.viewer = viewer;
		this.contextColor = contextColor;
		this.alerts = alerts;
		this.properties = properties;
		this.lore = lore;
		this.isClickable = isClickable;
		this.item = initItem();
	}
	
	private ItemStack initItem() {
		// Determine material
		Material material = Material.OBSIDIAN;
		if(town.isAttacked()) {
			material = Material.RED_WOOL;
		} else if(town.isArmored()) {
			material = Material.STONE_BRICKS;
		}
		// Add applicable labels
		boolean isProtected = false;
		List<String> loreList = new ArrayList<>();
		// Alerts
		if(town.getTerritoryType().equals(KonquestTerritoryType.CAPITAL) &&
				town.getKingdom().isCapitalImmune()) {
			loreList.add(alertColor+MessagePath.LABEL_IMMUNITY.getMessage());
		}
		if(!town.isLordValid()) {
			loreList.add(alertColor+MessagePath.LABEL_NO_LORD.getMessage());
		}
		if(town.isAttacked()) {
			loreList.add(alertColor+MessagePath.PROTECTION_NOTICE_ATTACKED.getMessage());
		}
		loreList.addAll(alerts);
		// Properties
		if(town.getTerritoryType().equals(KonquestTerritoryType.CAPITAL)) {
			loreList.add(propertyColor+MessagePath.TERRITORY_CAPITAL.getMessage());
		} else {
			loreList.add(propertyColor+MessagePath.TERRITORY_TOWN.getMessage());
		}
		if(town.isOpen()) {
			loreList.add(propertyColor+MessagePath.LABEL_OPEN.getMessage());
		}
		if(town.isArmored()) {
			loreList.add(propertyColor+MessagePath.LABEL_ARMOR.getMessage());
		}
		if(town.isShielded()) {
			isProtected = true;
			loreList.add(propertyColor+MessagePath.LABEL_SHIELD.getMessage());
		}
		if(viewer != null) {
			String viewerRole = town.getPlayerRoleName(viewer);
			if (!viewerRole.isEmpty()) {
				loreList.add(propertyColor+viewerRole);
			}
		}
		loreList.addAll(properties);
		// Lore
		loreList.add(loreColor+MessagePath.LABEL_POPULATION.getMessage() + ": " + valueColor + town.getNumResidents());
		loreList.add(loreColor+MessagePath.LABEL_LAND.getMessage() + ": " + valueColor + town.getChunkList().size());
		loreList.addAll(lore);
		String name = contextColor+town.getName();
		return CompatibilityUtil.buildItem(material, name, loreList, isProtected);
	}
	
	public KonTown getTown() {
		return town;
	}

	@Override
	public String getName() {
		return town.getName();
	}

	@Override
	public ItemStack getItem() {
		return item;
	}

	@Override
	public boolean isClickable() {
		return isClickable;
	}

}
