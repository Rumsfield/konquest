package com.github.rumsfield.konquest.map;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.model.KonKingdom;
import com.github.rumsfield.konquest.model.KonTerritory;
import com.github.rumsfield.konquest.model.KonTown;
import com.github.rumsfield.konquest.utility.ChatUtil;
import xyz.jpenilla.squaremap.api.Squaremap;

public class SquaremapRender implements Renderable {

    private final Konquest konquest;
    private boolean isEnabled;
    private static Squaremap api = null;

    public SquaremapRender(Konquest konquest) {
        this.konquest = konquest;
        this.isEnabled = false;
    }

    @Override
    public void initialize() {
        // Get squaremap API from integration manager
        isEnabled = konquest.getIntegrationManager().getSquaremap().isEnabled();
        if(isEnabled) {
            api = konquest.getIntegrationManager().getSquaremap().getAPI();
            if(api == null) {
                isEnabled = false;
                ChatUtil.printDebug("Failed to initialize SquaremapRender with null API reference.");
            }
        }
    }

    /**
     * Draw the capital and all towns in a kingdom on the map.
     *
     * @param kingdom The kingdom to draw
     */
    @Override
    public void drawUpdate(KonKingdom kingdom) {
        drawUpdate(kingdom.getCapital());
        for (KonTown town : kingdom.getTowns()) {
            drawUpdate(town);
        }
    }

    /**
     * Draw the region of a territory on the map.
     * Adds the region if it doesn't already exist.
     * Updates the region if it exists.
     *
     * @param territory The territory to draw
     */
    @Override
    public void drawUpdate(KonTerritory territory) {

    }

    /**
     * Remove the region of a territory from the map.
     *
     * @param territory The territory to remove
     */
    @Override
    public void drawRemove(KonTerritory territory) {

    }

    /**
     * Update the label of a territory.
     *
     * @param territory The territory to update
     */
    @Override
    public void drawLabel(KonTerritory territory) {

    }

    /**
     * Post a message to the web map.
     *
     * @param message The message to send
     */
    @Override
    public void postBroadcast(String message) {
        // TODO Does squaremap support posting messages to the web map?
    }


}
