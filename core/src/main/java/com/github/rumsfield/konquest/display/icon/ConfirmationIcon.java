package com.github.rumsfield.konquest.display.icon;

import com.github.rumsfield.konquest.manager.DisplayManager;
import com.github.rumsfield.konquest.utility.CompatibilityUtil;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class ConfirmationIcon extends MenuIcon {

    private final boolean value;
    private final Material mat;

    public ConfirmationIcon(boolean value, int index) {
        super(index);
        this.value = value;
        // Item Lore
        if (value) {
            this.mat = Material.GLOWSTONE_DUST;
        } else {
            this.mat = Material.REDSTONE;
        }
    }

    @Override
    public String getName() {
        return DisplayManager.boolean2Symbol(value);
    }

    @Override
    public ItemStack getItem() {
        return CompatibilityUtil.buildItem(mat, getName(), getLore());
    }

    @Override
    public boolean isClickable() {
        return true;
    }
}
