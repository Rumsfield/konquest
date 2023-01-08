package konquest.display.icon;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import konquest.model.KonMonumentTemplate;

public class TemplateIcon implements MenuIcon {

	private KonMonumentTemplate template;
	private List<String> lore;
	private int index;
	private boolean isClickable;
	
	public TemplateIcon(KonMonumentTemplate template, List<String> lore, int index, boolean isClickable) {
		this.template = template;
		this.lore = lore;
		this.index = index;
		this.isClickable = isClickable;
	}
	
	public KonMonumentTemplate getTemplate() {
		return template;
	}
	
	@Override
	public int getIndex() {
		return index;
	}

	@Override
	public String getName() {
		String result = "";
		if(template != null) {
			result = template.getName();
		}
		return result;
	}

	@Override
	public ItemStack getItem() {
		ItemStack item = new ItemStack(Material.OBSIDIAN,1);
		ItemMeta meta = item.getItemMeta();
		for(ItemFlag flag : ItemFlag.values()) {
			if(!meta.hasItemFlag(flag)) {
				meta.addItemFlags(flag);
			}
		}
		List<String> itemLore = new ArrayList<String>();
		//TODO: Replace with message paths
		itemLore.add(ChatColor.YELLOW+"Total Blocks: "+ChatColor.AQUA+template.getNumBlocks());
		itemLore.add(ChatColor.YELLOW+"Critical Blocks: "+ChatColor.AQUA+template.getNumCriticals());
		itemLore.add(ChatColor.YELLOW+"Loot Chests: "+ChatColor.AQUA+template.getNumLootChests());
		itemLore.addAll(lore);
		meta.setDisplayName(getName());
		meta.setLore(itemLore);
		item.setItemMeta(meta);
		return item;
	}

	@Override
	public boolean isClickable() {
		return isClickable;
	}

}
