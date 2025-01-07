package com.github.rumsfield.konquest.display.icon;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.model.KonSanctuary;
import com.github.rumsfield.konquest.utility.CompatibilityUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class SanctuaryIcon  extends MenuIcon {

    private final KonSanctuary sanctuary;
    private final boolean isClickable;

    public SanctuaryIcon(KonSanctuary sanctuary, int index, boolean isClickable) {
        super(index);
        this.sanctuary = sanctuary;
        this.isClickable = isClickable;
        // Item Lore
        addProperty(MessagePath.LABEL_SANCTUARY.getMessage());
        addNameValue(MessagePath.LABEL_WORLD.getMessage(), sanctuary.getWorld().getName());
    }

    public KonSanctuary getSanctuary() {
        return sanctuary;
    }

    @Override
    public String getName() {
        return Konquest.neutralColor2+sanctuary.getName();
    }

    @Override
    public ItemStack getItem() {
        return CompatibilityUtil.buildItem(Material.SMOOTH_QUARTZ, getName(), getLore());
    }

    @Override
    public boolean isClickable() {
        return isClickable;
    }
}
