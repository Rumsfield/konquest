package com.github.rumsfield.konquest.display.icon;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.manager.DisplayManager;
import com.github.rumsfield.konquest.utility.CompatibilityUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class PlayerIcon implements MenuIcon {

	public enum PlayerIconAction {
		DISPLAY_SCORE,
		DISPLAY_INFO
    }
	
	private final String name;
	private final List<String> lore;
	private final OfflinePlayer player;
	private final int index;
	private final boolean isClickable;
	private final PlayerIconAction action;

	private final String propertyColor = DisplayManager.propertyFormat;
	private final String loreColor = DisplayManager.loreFormat;
	private final String valueColor = DisplayManager.valueFormat;

	public PlayerIcon(String name, List<String> lore, OfflinePlayer player, int index, boolean isClickable, PlayerIconAction action) {
		this.name = name;
		this.lore = lore;
		this.player = player;
		this.index = index;
		this.isClickable = isClickable;
		this.action = action;
	}

	public PlayerIconAction getAction() {
		return action;
	}
	
	public OfflinePlayer getOfflinePlayer() {
		return player;
	}

	@Override
	public int getIndex() {
		return index;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public ItemStack getItem() {
		List<String> loreList = new ArrayList<>();
		String lastOnlineFormat = Konquest.getLastSeenFormat(player);
		loreList.add(propertyColor+MessagePath.LABEL_PLAYER.getMessage());
		loreList.add(loreColor+ MessagePath.LABEL_LAST_SEEN.getMessage(valueColor+lastOnlineFormat));
		loreList.addAll(lore);
		String name = getName();
		return CompatibilityUtil.buildItem(null, name, loreList, false, player);
	}
	
	@Override
	public boolean isClickable() {
		return isClickable;
	}
}
