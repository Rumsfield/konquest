package com.github.rumsfield.konquest.display.icon;

import com.github.rumsfield.konquest.command.CommandType;
import com.github.rumsfield.konquest.manager.DisplayManager;
import com.github.rumsfield.konquest.utility.CompatibilityUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.inventory.ItemStack;

public class CommandIcon extends MenuIcon{

	private final CommandType command;
	private final boolean permission;

	public CommandIcon(CommandType command, boolean permission, int cost, int cost_incr, int index) {
		super(index);
		this.command = command;
		this.permission = permission;
		// Item Lore
		if(!permission) {
			addAlert(MessagePath.LABEL_NO_PERMISSION.getMessage());
		}
		if(cost > 0) {
			addNameValue(MessagePath.LABEL_COST.getMessage(), cost);
		}
		if(cost_incr > 0) {
			addNameValue(MessagePath.LABEL_INCREMENT_COST.getMessage(), cost_incr);
		}
		addDescription(command.description());
	}
	
	public CommandType getCommand() {
		return command;
	}

	@Override
	public String getName() {
		return DisplayManager.nameFormat+command.toString();
	}

	@Override
	public ItemStack getItem() {
		return CompatibilityUtil.buildItem(command.iconMaterial(), getName(), getLore());
	}

	@Override
	public boolean isClickable() {
		return permission;
	}

}