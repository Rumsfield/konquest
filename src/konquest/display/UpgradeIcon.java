package konquest.display;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import konquest.Konquest;
import konquest.api.model.KonquestUpgrade;
import konquest.utility.MessagePath;

public class UpgradeIcon implements MenuIcon{

	KonquestUpgrade upgrade;
	int level;
	int cost;
	int pop;
	int index;
	ItemStack item;
	
	public UpgradeIcon(KonquestUpgrade upgrade, int level, int index, int cost, int pop) {
		this.upgrade = upgrade;
		this.level = level;
		this.index = index;
		this.cost = cost;
		this.pop = pop;
		this.item = initItem();
	}

	private ItemStack initItem() {
		ItemStack item = new ItemStack(upgrade.getIcon(),1);
		ItemMeta meta = item.getItemMeta();
		for(ItemFlag flag : ItemFlag.values()) {
			if(!meta.hasItemFlag(flag)) {
				meta.addItemFlags(flag);
			}
		}
		List<String> loreList = new ArrayList<String>();
		loreList.add(ChatColor.YELLOW+MessagePath.LABEL_LEVEL.getMessage()+" "+level);
		loreList.add(ChatColor.YELLOW+MessagePath.LABEL_COST.getMessage()+": "+ChatColor.AQUA+cost);
		loreList.add(ChatColor.YELLOW+MessagePath.LABEL_POPULATION.getMessage()+": "+ChatColor.AQUA+pop);
		for(String line : Konquest.stringPaginate(upgrade.getLevelDescription(level))) {
			loreList.add(ChatColor.RED+line);
		}
		meta.setDisplayName(ChatColor.GOLD+upgrade.getDescription());
		meta.setLore(loreList);
		item.setItemMeta(meta);
		return item;
	}
	
	public KonquestUpgrade getUpgrade() {
		return upgrade;
	}
	
	public int getLevel() {
		return level;
	}

	@Override
	public int getIndex() {
		return index;
	}

	@Override
	public String getName() {
		return upgrade.getDescription();
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
public class UpgradeIcon extends MenuIcon{

	KonUpgrade upgrade;
	int level;
	int cost;
	int pop;
	
	public UpgradeIcon(KonUpgrade upgrade, int level, int index, int cost, int pop) {
		super(upgrade.getDescription(), index);
		this.upgrade = upgrade;
		this.level = level;
		this.cost = cost;
		this.pop = pop;
		setItem(initItem());
	}

	@Override
	public ItemStack initItem() {
		ItemStack item = new ItemStack(upgrade.getIcon(),1);
		ItemMeta meta = item.getItemMeta();
		for(ItemFlag flag : ItemFlag.values()) {
			if(!meta.hasItemFlag(flag)) {
				meta.addItemFlags(flag);
			}
		}
		List<String> loreList = new ArrayList<String>();
		loreList.add(ChatColor.YELLOW+"Level "+level);
		loreList.add(ChatColor.YELLOW+"Cost: "+ChatColor.AQUA+cost);
		loreList.add(ChatColor.YELLOW+"Population: "+ChatColor.AQUA+pop);
		for(String line : Konquest.stringPaginate(upgrade.getLevelDescription(level))) {
			loreList.add(ChatColor.RED+line);
		}
		meta.setDisplayName(ChatColor.GOLD+upgrade.getDescription());
		meta.setLore(loreList);
		item.setItemMeta(meta);
		return item;
	}
	
	public KonUpgrade getUpgrade() {
		return upgrade;
	}
	
	public int getLevel() {
		return level;
	}

}
*/