package com.github.rumsfield.konquest.display.wrapper;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.display.icon.MenuIcon;
import com.github.rumsfield.konquest.display.icon.ProfessionIcon;
import com.github.rumsfield.konquest.manager.DisplayManager;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.model.KonTown;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.CorePath;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;

import java.util.ArrayList;
import java.util.List;

public class TownSpecializationMenuWrapper extends MenuWrapper {

    private final KonTown town;

    private final Boolean isAdmin;

    public TownSpecializationMenuWrapper(Konquest konquest, KonTown town, boolean isAdmin) {
        super(konquest);
        this.town = town;
        this.isAdmin = isAdmin;
    }

    @Override
    public void constructMenu() {
        String titleColor = DisplayManager.titleFormat;
        String loreColor = DisplayManager.loreFormat;
        String valueColor = DisplayManager.valueFormat;
        String hintColor = DisplayManager.hintFormat;

        // Page 0
        String pageLabel = titleColor+ MessagePath.MENU_TOWN_SPECIAL.getMessage();

        ProfessionIcon icon;
        int numEntries = Villager.Profession.values().length - 1; // Subtract one to omit current specialization choice
        int numRows = (int)Math.ceil((double)numEntries / 9);
        getMenu().addPage(0, numRows, pageLabel);
        int index = 0;
        List<String> loreList = new ArrayList<>();
        loreList.addAll(Konquest.stringPaginate(MessagePath.MENU_TOWN_LORE_SPECIAL.getMessage(),loreColor));
        if(!isAdmin) {
            double costSpecial = getKonquest().getCore().getDouble(CorePath.FAVOR_TOWNS_COST_SPECIALIZE.getPath());
            String cost = String.format("%.2f",costSpecial);
            loreList.add(loreColor+MessagePath.LABEL_COST.getMessage()+": "+valueColor+cost);
        }
        loreList.add(hintColor+MessagePath.MENU_TOWN_HINT_SPECIAL.getMessage());
        for(Villager.Profession profession : Villager.Profession.values()) {
            if(town != null && !profession.equals(town.getSpecialization())) {
                icon = new ProfessionIcon(ChatColor.GOLD+profession.name(),loreList,profession,index,true);
                getMenu().getPage(0).addIcon(icon);
                index++;
            }
        }

        getMenu().refreshNavigationButtons();
        getMenu().setPageIndex(0);
    }

    @Override
    public void onIconClick(KonPlayer clickPlayer, MenuIcon clickedIcon) {
        Player bukkitPlayer = clickPlayer.getBukkitPlayer();
        if(clickedIcon instanceof ProfessionIcon) {
            // Profession Icons close the GUI and attempt to apply a new specialization
            ProfessionIcon icon = (ProfessionIcon)clickedIcon;
            Villager.Profession clickProfession = icon.getProfession();
            boolean status = getKonquest().getKingdomManager().menuChangeTownSpecialization(town, clickProfession, clickPlayer, isAdmin);
            if(status) {
                ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_TOWN_NOTICE_SPECIALIZE.getMessage(town.getName(),clickProfession.name()));
                Konquest.playSuccessSound(bukkitPlayer);
            } else {
                Konquest.playFailSound(bukkitPlayer);
            }
        }
    }
}
