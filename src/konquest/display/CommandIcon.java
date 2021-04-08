package konquest.display;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import konquest.Konquest;
import konquest.command.CommandType;

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
