package com.github.rumsfield.konquest.display.icon;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.model.KonCamp;
import com.github.rumsfield.konquest.utility.CompatibilityUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class CampIcon extends MenuIcon {

    private final KonCamp camp;
    private final boolean isClickable;

    public CampIcon(KonCamp camp, int index, boolean isClickable) {
        super(index);
        this.camp = camp;
        this.isClickable = isClickable;
        // Item Lore
        addProperty(MessagePath.TERRITORY_CAMP.getMessage());
        if (camp.isProtected()) {
            addAlert(MessagePath.LABEL_PROTECTED.getMessage());
        }
        addNameValue(MessagePath.LABEL_OWNER.getMessage(), camp.getOwner().getName());
    }

    public KonCamp getCamp() {
        return camp;
    }

    @Override
    public String getName() {
        return Konquest.barbarianColor2+camp.getName();
    }

    @Override
    public ItemStack getItem() {
        return CompatibilityUtil.buildItem(Material.ORANGE_BED, getName(), getLore(), camp.isProtected());
    }

    @Override
    public boolean isClickable() {
        return isClickable;
    }
}
