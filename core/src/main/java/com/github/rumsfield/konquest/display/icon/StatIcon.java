package com.github.rumsfield.konquest.display.icon;

import com.github.rumsfield.konquest.manager.DisplayManager;
import com.github.rumsfield.konquest.model.KonStatsType;
import com.github.rumsfield.konquest.utility.CompatibilityUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.inventory.ItemStack;

public class StatIcon extends MenuIcon {

    private final boolean isClickable;
    private final KonStatsType stat;

    public StatIcon(KonStatsType stat, int level, int index, boolean isClickable) {
        super(index);
        this.stat = stat;
        this.isClickable = isClickable;
        // Item Lore
        addProperty(stat.getCategory().getTitle());
        addDescription(stat.description());
        addNameValue(MessagePath.LABEL_TOTAL.getMessage(),level);
    }

    public KonStatsType getStat() {
        return stat;
    }

    @Override
    public String getName() {
        return DisplayManager.nameFormat+stat.displayName();
    }

    @Override
    public ItemStack getItem() {
        return CompatibilityUtil.buildItem(stat.getMaterial(), getName(), getLore());
    }

    @Override
    public boolean isClickable() {
        return isClickable;
    }
}
