package com.github.rumsfield.konquest.display.wrapper;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.display.icon.InfoIcon;
import com.github.rumsfield.konquest.display.icon.MenuIcon;
import com.github.rumsfield.konquest.manager.DisplayManager;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.model.KonPropertyFlag;
import com.github.rumsfield.konquest.model.KonRuin;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

public class RuinInfoMenuWrapper extends MenuWrapper {

    private final KonRuin infoRuin;

    public RuinInfoMenuWrapper(Konquest konquest, KonRuin infoRuin) {
        super(konquest);
        this.infoRuin = infoRuin;
    }

    @Override
    public void constructMenu() {

        String pageLabel;
        List<String> loreList;

        String neutralColor = Konquest.neutralColor2;
        String titleColor = DisplayManager.titleFormat;
        String loreColor = DisplayManager.loreFormat;
        String valueColor = DisplayManager.valueFormat;

        /*
         * Display info about this Ruin:
         *  All properties
         *  Capture status
         *  Total land size, world name, criticals, spawns
         */

        // Page 0
        pageLabel = titleColor+ MessagePath.LABEL_RUIN.getMessage()+" "+infoRuin.getName();
        getMenu().addPage(0, 1, pageLabel);

        /* Flags Info Icon (3) */
        loreList = new ArrayList<>();
        for(KonPropertyFlag flag : KonPropertyFlag.values()) {
            if(infoRuin.hasPropertyValue(flag)) {
                String flagDisplaySymbol = DisplayManager.boolean2Symbol(infoRuin.getPropertyValue(flag));
                loreList.add(loreColor+flag.getName()+": "+flagDisplaySymbol);
            }
        }
        InfoIcon propertyInfo = new InfoIcon(neutralColor+MessagePath.LABEL_FLAGS.getMessage(), loreList, Material.REDSTONE_TORCH, 3, false);
        getMenu().getPage(0).addIcon(propertyInfo);

        /* Capture Status Icon (4) */
        loreList = new ArrayList<>();
        Material iconMat;
        if(infoRuin.isCaptureDisabled()) {
            // Currently on capture cooldown
            loreList.addAll(Konquest.stringPaginate(loreColor+MessagePath.PROTECTION_ERROR_CAPTURE.getMessage(infoRuin.getCaptureCooldownString())));
            iconMat = Material.FIRE;
        } else {
            // Can be captured
            loreList.addAll(Konquest.stringPaginate(loreColor+MessagePath.COMMAND_INFO_NOTICE_RUIN_CAPTURE.getMessage()));
            iconMat = Material.IRON_BLOCK;
        }
        InfoIcon captureInfo = new InfoIcon(neutralColor+MessagePath.COMMAND_INFO_NOTICE_RUIN_STATUS.getMessage(), loreList, iconMat, 4, false);

        /* General Info Icon (5) */
        loreList.add(loreColor + MessagePath.MAP_CRITICAL_HITS.getMessage() + ": " + valueColor + infoRuin.getMaxCriticalHits());
        loreList.add(loreColor + MessagePath.MAP_GOLEM_SPAWNS.getMessage() + ": " + valueColor + infoRuin.getSpawnLocations().size());
        loreList.add(loreColor + MessagePath.LABEL_LAND.getMessage() + ": " + valueColor + infoRuin.getChunkList().size());
        loreList.add(loreColor + MessagePath.LABEL_WORLD.getMessage() + ": " + valueColor + infoRuin.getWorld().getName());
        InfoIcon generalInfo = new InfoIcon(neutralColor+MessagePath.LABEL_INFORMATION.getMessage(), loreList, Material.ENDER_EYE, 5, false);
        getMenu().getPage(0).addIcon(generalInfo);

        getMenu().refreshNavigationButtons();
        getMenu().setPageIndex(0);
    }

    @Override
    public void onIconClick(KonPlayer clickPlayer, MenuIcon clickedIcon) {
        // No clickable icons
    }
}
