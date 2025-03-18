package com.github.rumsfield.konquest.model;

import com.github.rumsfield.konquest.manager.LootManager;
import com.github.rumsfield.konquest.utility.ChatUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class KonLootTable {

    private String name;
    private final HashMap<ItemStack,Integer> loot;

    public KonLootTable() {
        this.name = LootManager.defaultLootTableName;
        this.loot = new HashMap<>();
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void addLoot(HashMap<ItemStack,Integer> otherLoot) {
        loot.putAll(otherLoot);
    }

    public void clearLoot() {
        loot.clear();
    }

    public boolean isEmptyLoot() {
        return loot.isEmpty();
    }

    public ItemStack chooseRandomItem() {
        ItemStack item = new ItemStack(Material.DIRT,1);
        ItemMeta defaultMeta = item.getItemMeta();
        defaultMeta.setDisplayName(ChatColor.DARK_RED+"Invalid Loot");
        item.setItemMeta(defaultMeta);
        if(!loot.isEmpty()) {
            // Find total item range
            int total = 0;
            for(int p : loot.values()) {
                total = total + p;
            }
            int typeChoice = ThreadLocalRandom.current().nextInt(total);
            int typeWindow = 0;
            for(ItemStack i : loot.keySet()) {
                if(typeChoice < typeWindow + loot.get(i)) {
                    item = i.clone();
                    break;
                }
                typeWindow = typeWindow + loot.get(i);
            }
        }
        // Check for stored enchantment with level 0
        ItemMeta meta = item.getItemMeta();
        if(meta instanceof EnchantmentStorageMeta) {
            EnchantmentStorageMeta enchantMeta = (EnchantmentStorageMeta)meta;
            if(enchantMeta.hasStoredEnchants()) {
                Map<Enchantment,Integer> enchants = enchantMeta.getStoredEnchants();
                if(!enchants.isEmpty()) {
                    for(Enchantment e : enchants.keySet()) {
                        if(enchants.get(e) == 0) {
                            // Choose random level
                            int newLevel = ThreadLocalRandom.current().nextInt(e.getMaxLevel()+1);
                            if(newLevel < e.getStartLevel()) {
                                newLevel = e.getStartLevel();
                            }
                            enchantMeta.removeStoredEnchant(e);
                            enchantMeta.addStoredEnchant(e, newLevel, true);
                            item.setItemMeta(enchantMeta);
                            ChatUtil.printDebug("Enchanted loot item "+ item.getType() +" updated "+e.getKey().getKey()+" from level 0 to "+newLevel);
                        }
                    }
                }
            }
        }
        return item;
    }

}
