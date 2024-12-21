package com.github.rumsfield.konquest.display.icon;

import com.github.rumsfield.konquest.manager.DisplayManager;
import com.github.rumsfield.konquest.utility.CompatibilityUtil;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ConfirmationIcon extends MenuIcon {

    private final boolean value;
    private final List<String> lore;
    private final Material mat;
    private final ItemStack item;

    public ConfirmationIcon(boolean value, List<String> lore, Material mat, int index) {
        super(index);
        this.value = value;
        this.lore = lore;
        this.mat = mat;
        this.item = initItem();
    }

    private ItemStack initItem() {
        String name = DisplayManager.boolean2Symbol(value);
        return CompatibilityUtil.buildItem(mat, name, lore);
    }

    @Override
    public String getName() {
        return DisplayManager.boolean2Symbol(value);
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
