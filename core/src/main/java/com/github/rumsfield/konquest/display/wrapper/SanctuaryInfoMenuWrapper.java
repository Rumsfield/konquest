package com.github.rumsfield.konquest.display.wrapper;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.display.icon.MenuIcon;
import com.github.rumsfield.konquest.manager.DisplayManager;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.model.KonSanctuary;

import java.util.List;

public class SanctuaryInfoMenuWrapper extends MenuWrapper {

    private KonSanctuary infoSanctuary;

    public SanctuaryInfoMenuWrapper(Konquest konquest, KonSanctuary infoSanctuary) {
        super(konquest);
        this.infoSanctuary = infoSanctuary;
    }

    @Override
    public void constructMenu() {

        String pageLabel;
        List<String> loreList;

        String titleColor = DisplayManager.titleFormat;
        String loreColor = DisplayManager.loreFormat;
        String valueColor = DisplayManager.valueFormat;

        /*
         * Display info about this Sanctuary:
         *  All properties
         *  Total land size, world name
         *  A paged list of templates
         */

    }

    @Override
    public void onIconClick(KonPlayer clickPlayer, MenuIcon clickedIcon) {

    }
}
