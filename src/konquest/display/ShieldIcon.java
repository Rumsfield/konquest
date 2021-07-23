package konquest.display;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import konquest.model.KonShield;
import konquest.utility.MessagePath;

public class ShieldIcon implements MenuIcon {

	private KonShield shield;
	private boolean isAvailable;
	private int population;
	private int index;
	ItemStack item;
	
	public ShieldIcon(KonShield shield, boolean isAvailable, int population, int index) {
		this.shield = shield;
		this.isAvailable = isAvailable;
		this.population = population;
		this.index = index;
		this.item = initItem();
	}
	
	private ItemStack initItem() {
		ItemStack item = new ItemStack(Material.SHIELD,1);
		ItemMeta meta = item.getItemMeta();
		for(ItemFlag flag : ItemFlag.values()) {
			if(!meta.hasItemFlag(flag)) {
				meta.addItemFlags(flag);
			}
		}
		int totalCost = population * shield.getCost();
		List<String> loreList = new ArrayList<String>();
		loreList.add(ChatColor.BOLD+""+ChatColor.DARK_AQUA+shield.getDurationFormat());
    	loreList.add(ChatColor.WHITE+MessagePath.LABEL_COST.getMessage()+": "+ChatColor.AQUA+totalCost);
    	loreList.add(ChatColor.GOLD+MessagePath.MENU_SHIELD_HINT.getMessage());
    	meta.setDisplayName(ChatColor.GOLD+shield.getId());
		meta.setLore(loreList);
		item.setItemMeta(meta);
		return item;
	}

	@Override
	public int getIndex() {
		return index;
	}

	@Override
	public String getName() {
		return shield.getId();
	}

	@Override
	public ItemStack getItem() {
		return item;
	}

	@Override
	public boolean isClickable() {
		return isAvailable;
	}
}
