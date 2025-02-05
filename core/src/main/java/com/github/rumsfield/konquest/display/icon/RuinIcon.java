package com.github.rumsfield.konquest.display.icon;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.model.KonRuin;
import com.github.rumsfield.konquest.utility.CompatibilityUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class RuinIcon extends MenuIcon {

    private final KonRuin ruin;
    private final boolean isClickable;

    public RuinIcon(KonRuin ruin, int index, boolean isClickable) {
        super(index);
        this.ruin = ruin;
        this.isClickable = isClickable;
        // Item Lore
        addProperty(MessagePath.TERRITORY_RUIN.getMessage());
        if (ruin.isCaptureDisabled()) {
            addProperty(MessagePath.LABEL_CAPTURED.getMessage());
        }
        addNameValue(MessagePath.LABEL_WORLD.getMessage(), ruin.getWorld().getName());
    }

    public KonRuin getRuin() {
        return ruin;
    }

    @Override
    public String getName() {
        return Konquest.neutralColor2+ruin.getName();
    }

    @Override
    public ItemStack getItem() {
        return CompatibilityUtil.buildItem(Material.MOSSY_COBBLESTONE, getName(), getLore(), ruin.isCaptureDisabled());
    }

    @Override
    public boolean isClickable() {
        return isClickable;
    }
}