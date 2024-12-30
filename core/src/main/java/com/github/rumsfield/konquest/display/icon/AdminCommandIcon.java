package com.github.rumsfield.konquest.display.icon;

import com.github.rumsfield.konquest.command.admin.AdminCommandType;
import com.github.rumsfield.konquest.manager.DisplayManager;
import com.github.rumsfield.konquest.utility.CompatibilityUtil;
import com.github.rumsfield.konquest.utility.HelperUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class AdminCommandIcon extends MenuIcon{

    private final AdminCommandType command;
    private final boolean permission;
    private final ItemStack item;
    private final List<String> lore;

    private final String adminColor = DisplayManager.adminFormat;
    private final String alertColor = DisplayManager.alertFormat;
    private final String loreColor = DisplayManager.loreFormat;

    public AdminCommandIcon(AdminCommandType command, boolean permission, List<String> lore, int index) {
        super(index);
        this.command = command;
        this.permission = permission;
        this.lore = lore;
        this.item = initItem();
    }

    private ItemStack initItem() {
        List<String> loreList = new ArrayList<>();
        if(!permission) {
            loreList.add(alertColor+ MessagePath.LABEL_NO_PERMISSION.getMessage());
        }
        loreList.addAll(HelperUtil.stringPaginate(command.description(), loreColor));
        loreList.addAll(lore);
        String name = adminColor+getName();
        return CompatibilityUtil.buildItem(command.iconMaterial(), name, loreList);
    }

    public AdminCommandType getCommand() {
        return command;
    }

    @Override
    public String getName() {
        return command.toString();
    }

    @Override
    public ItemStack getItem() {
        return item;
    }

    @Override
    public boolean isClickable() {
        return permission;
    }

}
