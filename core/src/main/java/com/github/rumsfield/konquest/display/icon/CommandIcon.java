package com.github.rumsfield.konquest.display.icon;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.command.CommandType;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class CommandIcon implements MenuIcon{

	private final CommandType command;
	private final int cost;
	private final int cost_incr;
	private final int index;
	private final ItemStack item;
	
	public CommandIcon(CommandType command, int cost, int cost_incr, int index) {
		this.command = command;
		this.cost = cost;
		this.cost_incr = cost_incr;
		this.index = index;
		this.item = initItem();
	}

	private ItemStack initItem() {
		ItemStack item = new ItemStack(command.iconMaterial());
		ItemMeta meta = item.getItemMeta();
		assert meta != null;
		for(ItemFlag flag : ItemFlag.values()) {
			if(!meta.hasItemFlag(flag)) {
				meta.addItemFlags(flag);
			}
		}
		List<String> loreList = new ArrayList<>();
		if(cost > 0) {
			loreList.add(ChatColor.GOLD+MessagePath.LABEL_COST.getMessage()+": "+ChatColor.AQUA+cost);
		}
		if(cost_incr > 0) {
			loreList.add(ChatColor.RED+MessagePath.LABEL_INCREMENT_COST.getMessage()+": "+ChatColor.AQUA+cost_incr);
		}
		loreList.addAll(Konquest.stringPaginate(command.description()));
		meta.setDisplayName(ChatColor.GOLD+getName());
		meta.setLore(loreList);
		item.setItemMeta(meta);
		return item;
	}
	
	public CommandType getCommand() {
		return command;
	}

	@Override
	public int getIndex() {
		return index;
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
		return true;
	}

}

/*
public class CommandIcon extends MenuIcon{

	private CommandType command;
	private int cost;
	private int cost_incr;
	
	public CommandIcon(CommandType command, int cost, int cost_incr, int index) {
		super(command.toString(), index);
		this.command = command;
		this.cost = cost;
		this.cost_incr = cost_incr;
		setItem(initItem());
	}

	@Override
	public ItemStack initItem() {
		ItemStack item = new ItemStack(command.iconMaterial());
		ItemMeta meta = item.getItemMeta();
		for(ItemFlag flag : ItemFlag.values()) {
			if(!meta.hasItemFlag(flag)) {
				meta.addItemFlags(flag);
			}
		}
		List<String> loreList = new ArrayList<String>();
		if(cost > 0) {
			loreList.add(ChatColor.GOLD+"Cost: "+ChatColor.AQUA+cost);
		}
		if(cost_incr > 0) {
			loreList.add(ChatColor.RED+"Increment Cost: "+ChatColor.AQUA+cost_incr);
		}
		for(String line : Konquest.stringPaginate(command.description())) {
			loreList.add(line);
		}
		meta.setDisplayName(ChatColor.GOLD+getName());
		meta.setLore(loreList);
		item.setItemMeta(meta);
		return item;
	}
	
	public CommandType getCommand() {
		return command;
	}

}
*/