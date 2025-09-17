package com.github.rumsfield.konquest.display.icon;

import com.github.rumsfield.konquest.manager.DisplayManager;
import com.github.rumsfield.konquest.utility.CompatibilityUtil;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Date;

public class DateIcon extends MenuIcon {

    private final String name;
    private final Material mat;
    private final Date date;
    private final boolean isToday;
    private final boolean isClickable;

    public DateIcon(String name, Date date, Material mat, int index, boolean isToday, boolean isClickable) {
        super(index);
        this.name = name;
        this.mat = mat;
        this.date = date;
        this.isToday = isToday;
        this.isClickable = isClickable;
    }

    public Date getDate() {
        return date;
    }

    @Override
    public String getName() {
        return DisplayManager.nameFormat+name;
    }

    @Override
    public ItemStack getItem() {
        return CompatibilityUtil.buildItem(mat, getName(), getLore(), isToday);
    }

    @Override
    public boolean isClickable() {
        return isClickable;
    }
}
