package konquest.display.icon;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import konquest.model.KonArmor;
import konquest.utility.MessagePath;

public class ArmorIcon implements MenuIcon {

	private KonArmor armor;
	private boolean isAvailable;
	private int population;
	private int index;
	ItemStack item;
	
	public ArmorIcon(KonArmor armor, boolean isAvailable, int population, int index) {
		this.armor = armor;
		this.isAvailable = isAvailable;
		this.population = population;
		this.index = index;
		this.item = initItem();
	}
	
	private ItemStack initItem() {
		Material mat = Material.DIAMOND_CHESTPLATE;
		if(!isAvailable) {
			mat = Material.IRON_BARS;
		}
		ItemStack item = new ItemStack(mat,1);
		ItemMeta meta = item.getItemMeta();
		meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);
		for(ItemFlag flag : ItemFlag.values()) {
			if(!meta.hasItemFlag(flag)) {
				meta.addItemFlags(flag);
			}
		}
		int totalCost = population * armor.getCost();
		List<String> loreList = new ArrayList<String>();
		loreList.add(ChatColor.DARK_AQUA+""+armor.getBlocks());
    	loreList.add(ChatColor.YELLOW+MessagePath.LABEL_COST.getMessage()+": "+ChatColor.AQUA+totalCost);
    	if(isAvailable) {
    		loreList.add(ChatColor.GOLD+MessagePath.MENU_SHIELD_HINT.getMessage());
    	}
    	meta.setDisplayName(ChatColor.GOLD+armor.getId()+" "+MessagePath.LABEL_ARMOR.getMessage());
		meta.setLore(loreList);
		item.setItemMeta(meta);
		return item;
	}
	
	public KonArmor getArmor() {
		return armor;
	}

	@Override
	public int getIndex() {
		return index;
	}

	@Override
	public String getName() {
		return armor.getId();
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
