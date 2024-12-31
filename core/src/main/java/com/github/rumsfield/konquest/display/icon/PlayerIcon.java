package com.github.rumsfield.konquest.display.icon;

import com.github.rumsfield.konquest.manager.DisplayManager;
import com.github.rumsfield.konquest.utility.CompatibilityUtil;
import com.github.rumsfield.konquest.utility.HelperUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;

public class PlayerIcon extends MenuIcon {

	public enum PlayerIconAction {
		DISPLAY_SCORE,
		DISPLAY_INFO
    }

	private final OfflinePlayer player;
	private final String contextColor;
	private final boolean isClickable;
	private final PlayerIconAction action;

	public PlayerIcon(OfflinePlayer player, String contextColor, int index, boolean isClickable, PlayerIconAction action) {
		super(index);
		this.contextColor = contextColor;
		this.player = player;
		this.isClickable = isClickable;
		this.action = action;
		// Item Lore
		addProperty(MessagePath.LABEL_PLAYER.getMessage());
		String lastOnlineFormat = DisplayManager.valueFormat+HelperUtil.getLastSeenFormat(player);
		addDescription(MessagePath.LABEL_LAST_SEEN.getMessage(lastOnlineFormat));
	}

	public PlayerIconAction getAction() {
		return action;
	}
	
	public OfflinePlayer getOfflinePlayer() {
		return player;
	}

	@Override
	public String getName() {
		return contextColor+player.getName();
	}

	@Override
	public ItemStack getItem() {
		return CompatibilityUtil.buildItem(null, getName(), getLore(), false, player);
	}
	
	@Override
	public boolean isClickable() {
		return isClickable;
	}
}
