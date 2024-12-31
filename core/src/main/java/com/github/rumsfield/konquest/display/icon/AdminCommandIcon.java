package com.github.rumsfield.konquest.display.icon;

import com.github.rumsfield.konquest.command.admin.AdminCommandType;
import com.github.rumsfield.konquest.manager.DisplayManager;
import com.github.rumsfield.konquest.utility.CompatibilityUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.inventory.ItemStack;

public class AdminCommandIcon extends MenuIcon{

    private final AdminCommandType command;
    private final boolean permission;

    public AdminCommandIcon(AdminCommandType command, boolean permission, int index) {
        super(index);
        this.command = command;
        this.permission = permission;
        // Item Lore
        if(!permission) {
            addAlert(MessagePath.LABEL_NO_PERMISSION.getMessage());
        }
        addDescription(command.description());
    }

    public AdminCommandType getCommand() {
        return command;
    }

    @Override
    public String getName() {
        return DisplayManager.adminFormat+command.toString();
    }

    @Override
    public ItemStack getItem() {
        return CompatibilityUtil.buildItem(command.iconMaterial(), getName(), getLore());
    }

    @Override
    public boolean isClickable() {
        return permission;
    }

}
