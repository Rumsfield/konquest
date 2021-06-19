package konquest.display;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import konquest.model.KonKingdom;

public class KingdomIcon implements MenuIcon {

	private KonKingdom kingdom;
	private ChatColor contextColor;
	private Material material;
	private List<String> lore;
	private int index;
	private ItemStack item;
	
	public KingdomIcon(KonKingdom kingdom, boolean isFriendly, Material material, List<String> lore, int index) {
		this.kingdom = kingdom;
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
		meta.setDisplayName(contextColor+kingdom.getName());
		meta.setLore(lore);
		item.setItemMeta(meta);
		return item;
	}
	
	public KonKingdom getKingdom() {
		return kingdom;
	}
	
	@Override
	public int getIndex() {
		return index;
	}

	@Override
	public String getName() {
		return kingdom.getName();
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
