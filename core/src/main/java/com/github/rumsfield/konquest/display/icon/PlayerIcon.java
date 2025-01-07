package com.github.rumsfield.konquest.display.icon;

import com.github.rumsfield.konquest.api.model.KonquestRelationshipType;
import com.github.rumsfield.konquest.manager.DisplayManager;
import com.github.rumsfield.konquest.utility.CompatibilityUtil;
import com.github.rumsfield.konquest.utility.HelperUtil;
import com.github.rumsfield.konquest.utility.Labeler;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;

public class PlayerIcon extends MenuIcon {

	private final OfflinePlayer player;
	private final String contextColor;
	private final boolean isClickable;

	public PlayerIcon(OfflinePlayer player, String contextColor, KonquestRelationshipType relation, int index, boolean isClickable) {
		super(index);
		this.contextColor = contextColor;
		this.player = player;
		this.isClickable = isClickable;
		// Item Lore
		addProperty(MessagePath.LABEL_PLAYER.getMessage());
		if (relation != null) {
			addProperty(Labeler.lookup(relation));
		}
		String lastOnlineFormat = DisplayManager.valueFormat+HelperUtil.getLastSeenFormat(player);
		addDescription(MessagePath.LABEL_LAST_SEEN.getMessage(lastOnlineFormat));
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
