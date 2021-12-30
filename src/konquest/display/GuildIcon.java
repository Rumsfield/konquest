package konquest.display;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import konquest.model.KonGuild;

public class GuildIcon implements MenuIcon {
	
	private String name;
	private List<String> lore;
	private KonGuild guild;
	private ChatColor contextColor;
	private int index;
	private boolean isClickable;
	
	public GuildIcon(KonGuild guild, boolean isFriendly, boolean isArmistice, List<String> lore, int index, boolean isClickable) {
		this.name = guild.getName()+" Guild";
		this.lore = lore;
		this.guild = guild;
		this.index = index;
		this.isClickable = isClickable;
		if(isFriendly) {
			contextColor = ChatColor.GREEN;
		} else {
			if(isArmistice) {
				contextColor = ChatColor.LIGHT_PURPLE;
			} else {
				contextColor = ChatColor.RED;
			}
		}
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
		ItemStack item = new ItemStack(Material.BELL,1);
		ItemMeta meta = item.getItemMeta();
		for(ItemFlag flag : ItemFlag.values()) {
			if(!meta.hasItemFlag(flag)) {
				meta.addItemFlags(flag);
			}
		}
		meta.setDisplayName(contextColor+getName());
		ChatColor loreColor = ChatColor.YELLOW;
		ChatColor valueColor = ChatColor.AQUA;
		List<String> loreList = new ArrayList<String>();
		if(guild != null) {
			loreList.add(loreColor+"Towns: "+valueColor+guild.getNumTowns());
			loreList.add(loreColor+"Land: "+valueColor+guild.getNumLand());
			loreList.add(loreColor+"Members: "+valueColor+guild.getNumMembers());
			loreList.add(loreColor+"Specialization: "+valueColor+guild.getSpecialization().name());
		}
		loreList.addAll(lore);
		meta.setLore(loreList);
		item.setItemMeta(meta);
		return item;
	}

	@Override
	public boolean isClickable() {
		return isClickable;
	}
	
}
