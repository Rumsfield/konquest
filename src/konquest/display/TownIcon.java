package konquest.display;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import konquest.model.KonTown;

public class TownIcon implements MenuIcon {

	private KonTown town;
	private ChatColor contextColor;
	private Material material;
	private List<String> lore;
	private int index;
	private ItemStack item;
	
	public TownIcon(KonTown town, boolean isFriendly, Material material, List<String> lore, int index) {
		this.town = town;
		if(isFriendly) {
			contextColor = ChatColor.GREEN;
		} else {
			contextColor = ChatColor.RED;
		}
		this.material = material;
		this.lore = lore;
		this.index = index;
		this.item = initItem();
	}
	
	private ItemStack initItem() {
		ItemStack item = new ItemStack(material,1);
		ItemMeta meta = item.getItemMeta();
		for(ItemFlag flag : ItemFlag.values()) {
			if(!meta.hasItemFlag(flag)) {
				meta.addItemFlags(flag);
			}
		}
		meta.setDisplayName(contextColor+town.getName());
		meta.setLore(lore);
		item.setItemMeta(meta);
		return item;
	}
	
	@Override
	public int getIndex() {
		return index;
	}

	@Override
	public String getName() {
		return town.getName();
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
