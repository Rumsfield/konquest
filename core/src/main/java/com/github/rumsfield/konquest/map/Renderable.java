package com.github.rumsfield.konquest.map;

public interface Renderable {

    /**
     * Initialize the API associated with the renderer.
     */
    void initialize();

    /**
     * Draw the region of a territory on the map.
     * Adds the region if it doesn't already exist.
     * Updates the region if it exists.
     * @param area The territory area to update
     */
    void drawUpdate(AreaTerritory area);

    /**
     * Remove the region of a territory from the map.
     * @param area The territory area to remove
     */
    void drawRemove(AreaTerritory area);

    /**
     * Update the label of a territory.
     * @param area The territory area to label
     */
    void drawLabel(AreaTerritory area);

    /**
     * Post a message to the web map.
     * @param message The message to send
     */
    void postBroadcast(String message);

    /**
     * Get the name of the mapping service.
     * @return The map plugin's name
     */
    String getMapName();

}
