package com.github.rumsfield.konquest.display.icon;

import com.github.rumsfield.konquest.command.CommandType;
import com.github.rumsfield.konquest.manager.DisplayManager;
import com.github.rumsfield.konquest.utility.CompatibilityUtil;
import com.github.rumsfield.konquest.utility.HelperUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class CommandIcon extends MenuIcon{

	private final CommandType command;
	private final boolean permission;
	private final int cost;
	private final int cost_incr;
	private final ItemStack item;

	private final String alertColor = DisplayManager.alertFormat;
	private final String loreColor = DisplayManager.loreFormat;
	private final String valueColor = DisplayManager.valueFormat;

	public CommandIcon(CommandType command, boolean permission, int cost, int cost_incr, int index) {
		super(index);
		this.command = command;
		this.permission = permission;
		this.cost = cost;
		this.cost_incr = cost_incr;
		this.item = initItem();
	}

	private ItemStack initItem() {
		List<String> loreList = new ArrayList<>();
		if(!permission) {
			loreList.add(alertColor+MessagePath.LABEL_NO_PERMISSION.getMessage());
		}
		if(cost > 0) {
			loreList.add(loreColor+MessagePath.LABEL_COST.getMessage()+": "+valueColor+cost);
		}
		if(cost_incr > 0) {
			loreList.add(loreColor+MessagePath.LABEL_INCREMENT_COST.getMessage()+": "+valueColor+cost_incr);
		}
		loreList.addAll(HelperUtil.stringPaginate(command.description(),loreColor));
		String name = ChatColor.GOLD+getName();
		return CompatibilityUtil.buildItem(command.iconMaterial(), name, loreList);
	}
	
	public CommandType getCommand() {
		return command;
	}

	@Override
	public String getName() {
		return command.toString();
	}

	@Override
	public ItemStack getItem() {
		return item;
	}

	@Override
	public boolean isClickable() {
		return permission;
	}

}