package konquest.display;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import konquest.model.KonGuild;

public class GuildIcon implements MenuIcon {
	
	private String name;
	private List<String> lore;
	private KonGuild guild;
	private int index;
	private boolean isClickable;
	
	public GuildIcon(String name, List<String> lore, KonGuild guild, int index, boolean isClickable) {
		this.name = name;
		this.lore = lore;
		this.guild = guild;
		this.index = index;
		this.isClickable = isClickable;
	}
	
	public KonGuild getGuild() {
		return guild;
	}

	@Override
	public int getIndex() {
		return index;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public ItemStack getItem() {
		ItemStack item = new ItemStack(Material.QUARTZ_PILLAR,1);
		ItemMeta meta = item.getItemMeta();
		for(ItemFlag flag : ItemFlag.values()) {
			if(!meta.hasItemFlag(flag)) {
				meta.addItemFlags(flag);
			}
		}
		meta.setDisplayName(getName());
		meta.setLore(lore);
		item.setItemMeta(meta);
		return item;
	}

	@Override
	public boolean isClickable() {
		return isClickable;
	}
	
}
