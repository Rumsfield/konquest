package com.github.rumsfield.konquest.display.icon;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.command.admin.AdminCommandType;
import com.github.rumsfield.konquest.manager.DisplayManager;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class AdminCommandIcon  implements MenuIcon{

    private final AdminCommandType command;
    private final int index;
    private final ItemStack item;

    private final String loreColor = DisplayManager.loreFormat;

    public AdminCommandIcon(AdminCommandType command, int index) {
        this.command = command;
        this.index = index;
        this.item = initItem();
    }

    private ItemStack initItem() {
        ItemStack item = new ItemStack(command.iconMaterial());
        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        for(ItemFlag flag : ItemFlag.values()) {
            if(!meta.hasItemFlag(flag)) {
                meta.addItemFlags(flag);
            }
        }
        List<String> loreList = new ArrayList<>();
        loreList.addAll(Konquest.stringPaginate(command.description(),loreColor));
        meta.setDisplayName(""+ChatColor.GOLD+ChatColor.ITALIC+getName());
        meta.setLore(loreList);
        item.setItemMeta(meta);
        return item;
    }

    public AdminCommandType getCommand() {
        return command;
    }

    @Override
    public int getIndex() {
        return index;
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
        return true;
    }

}
