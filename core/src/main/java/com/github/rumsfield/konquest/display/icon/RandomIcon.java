package com.github.rumsfield.konquest.display.icon;

import com.github.rumsfield.konquest.manager.DisplayManager;
import com.github.rumsfield.konquest.utility.CompatibilityUtil;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

public class RandomIcon extends MenuIcon{

    private final String name;
    private final boolean isClickable;

    public RandomIcon(String name, int index, boolean isClickable) {
        super(index);
        this.name = name;
        this.isClickable = isClickable;
    }

    @Override
    public String getName() {
        return DisplayManager.nameFormat+name;
    }

    @Override
    public ItemStack getItem() {
        boolean isItem = false;
        int matIndex;
        Material iconMat = null;
        int watchdog = 100;
        while (!isItem && watchdog > 0) {
            matIndex = ThreadLocalRandom.current().nextInt(0, Material.values().length);
            iconMat = Material.values()[matIndex];
            isItem = iconMat.isItem();
            watchdog--;
        }
        return CompatibilityUtil.buildItem(iconMat, getName(), getLore());
    }

    @Override
    public boolean isClickable() {
        return isClickable;
    }

}
