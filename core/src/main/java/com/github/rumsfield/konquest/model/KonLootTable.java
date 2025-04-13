package com.github.rumsfield.konquest.model;

import com.github.rumsfield.konquest.manager.LootManager;
import com.github.rumsfield.konquest.utility.ChatUtil;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class KonLootTable {

    private String name;
    private final HashMap<ItemStack,Integer> loot;
    private final ArrayList<ItemStack> weightedLoot;

    public KonLootTable() {
        this.name = LootManager.defaultLootTableName;
        this.loot = new HashMap<>();
        this.weightedLoot = new ArrayList<>();
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void addLoot(HashMap<ItemStack,Integer> otherLoot) {
        loot.putAll(otherLoot);
        // Populate weighted loot table
        weightedLoot.clear();
        for (ItemStack item : loot.keySet()) {
            int weight = Math.max(loot.get(item),1);
            while (weight > 0) {
                weightedLoot.add(item);
                weight--;
            }
        }
    }

    public void clearLoot() {
        loot.clear();
        weightedLoot.clear();
    }

    public boolean isEmptyLoot() {
        return loot.isEmpty();
    }

    public ItemStack chooseRandomItem() {
        ItemStack item;
        if(!weightedLoot.isEmpty()) {
            int tableIndex = ThreadLocalRandom.current().nextInt(weightedLoot.size());
            item = weightedLoot.get(tableIndex).clone();
        } else {
            item = new ItemStack(Material.DIRT,1);
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
