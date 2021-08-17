package konquest.display;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import konquest.model.KonCustomPrefix;

public class PrefixCustomIcon implements MenuIcon {

	private List<String> lore;
	private int index;
	private boolean isClickable;
	private KonCustomPrefix prefix;
	private ItemStack item;
	
	public PrefixCustomIcon(KonCustomPrefix prefix, List<String> lore, int index, boolean isClickable) {
		this.prefix = prefix;
		this.lore = lore;
		this.index = index;
		this.isClickable = isClickable;
		this.item = initItem();
	}
	
	private ItemStack initItem() {
		ItemStack item = new ItemStack(Material.IRON_BARS);
		ItemMeta meta = item.getItemMeta();
		if(isClickable) {
			meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);
		}
		for(ItemFlag flag : ItemFlag.values()) {
			if(!meta.hasItemFlag(flag)) {
				meta.addItemFlags(flag);
			}
		}
		if(isClickable) {
			item.setType(Material.GOLD_BLOCK);
		}
		meta.setDisplayName(prefix.getName());
		meta.setLore(lore);
		item.setItemMeta(meta);
		return item;
	}
	
	public KonCustomPrefix getPrefix() {
		return prefix;
	}
	
	@Override
	public int getIndex() {
		return index;
	}

	@Override
	public String getName() {
		return prefix.getName();
	}

	@Override
	public ItemStack getItem() {
		return item;
	}

	@Override
	public boolean isClickable() {
		return isClickable;
	}
}
