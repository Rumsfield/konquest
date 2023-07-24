package com.github.rumsfield.konquest.map;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.model.KonKingdom;
import com.github.rumsfield.konquest.model.KonTerritory;
import de.bluecolored.bluemap.api.BlueMapAPI;

import java.util.HashMap;

public class BlueMapRender implements Renderable {

    private final Konquest konquest;
    private boolean isEnabled;
    private static BlueMapAPI bapi = null;
    private final HashMap<KonTerritory,AreaTerritory> areaCache;

    public BlueMapRender(Konquest konquest) {
        this.konquest = konquest;
        this.isEnabled = false;
        this.areaCache = new HashMap<>();
    }

    public void initialize() {
        // Get BlueMap API from integration manager
        isEnabled = konquest.getIntegrationManager().getBlueMap().isEnabled();
        if(isEnabled) {
            bapi = konquest.getIntegrationManager().getBlueMap().getAPI();
        }
    }



    @Override
    public void drawUpdate(KonKingdom kingdom) {

    }

    @Override
    public void drawUpdate(KonTerritory territory) {

    }

    @Override
    public void drawRemove(KonTerritory territory) {

    }

    @Override
    public void drawLabel(KonTerritory territory) {

    }
}
