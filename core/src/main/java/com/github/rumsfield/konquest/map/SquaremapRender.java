package com.github.rumsfield.konquest.map;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.api.model.KonquestTerritoryType;
import com.github.rumsfield.konquest.model.KonTerritory;
import com.github.rumsfield.konquest.utility.ChatUtil;
import xyz.jpenilla.squaremap.api.*;
import xyz.jpenilla.squaremap.api.Point;
import xyz.jpenilla.squaremap.api.marker.Marker;
import xyz.jpenilla.squaremap.api.marker.MarkerOptions;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    @Override
    public String getMapName() {
        return konquest.getIntegrationManager().getSquaremap().getPluginName();
    }

    /*
     * squaremap Marker Key formats:
     * Sanctuaries
     * 		Layer Group:  		    konquest.marker.sanctuary
     * 		Marker Shapes: 	        konquest.area.sanctuary.<name>
     * Ruins
     * 		Layer Group:  		    konquest.marker.ruin
     * 		Marker Shapes: 	        konquest.area.ruin.<name>
     * Camps
     * 		Layer Group:  		    konquest.marker.camp
     * 		Marker Shapes: 	        konquest.area.camp.<name>
     * Kingdoms
     * 		Layer Group:  		    konquest.marker.kingdom
     * 		Capital
     * 		Marker Shapes: 	        konquest.area.kingdom.<kingdom>.capital
     * 		Towns
     * 		Marker Shapes: 	        konquest.area.kingdom.<kingdom>.<town>
     */

    /**
     * Draw the region of a territory on the map.
     * Adds the region if it doesn't already exist.
     * Updates the region if it exists.
     *
     * @param area The territory area to draw
     */
    @Override
    public void drawUpdate(AreaTerritory area) {
        if (!isEnabled) return;

        KonTerritory territory = area.getTerritory();

        Key groupKey = getGroupKey(territory);
        Key areaKey = getAreaKey(territory);
        String groupLabel = MapHandler.getGroupLabel(territory); // The display name of the group
        String areaLabel = MapHandler.getIconLabel(territory); // The display name of the area (icon)
        String areaDetail = MapHandler.getAreaLabel(territory); // The display details of the area
        Color areaColor = getAreaColor(territory);
        Color lineColor = getLineColor(territory);

        // Get the layer provider
        MapWorld mapWorld = api.getWorldIfEnabled(BukkitAdapter.worldIdentifier(territory.getWorld())).orElse(null);
        if (mapWorld == null) {
            ChatUtil.printDebug("Could not draw territory "+territory.getName()+" with invalid world, "+territory.getWorld().getName());
            return;
        }

        // Ensure group layer exists in the registry
        final Registry<LayerProvider> layerRegistry = mapWorld.layerRegistry();
        if (!layerRegistry.hasEntry(groupKey)) {
            ChatUtil.printDebug("Creating new group layer "+groupKey.getKey()+" in squaremap for territory "+territory.getName());
            int index = getGroupIndex(territory);
            layerRegistry.register(groupKey, SimpleLayerProvider.builder(groupLabel)
                    .showControls(true)
                    .defaultHidden(false)
                    .layerPriority(index)
                    .zIndex(index)
                    .build());
        }
        SimpleLayerProvider groupLayerProvider = (SimpleLayerProvider) layerRegistry.get(groupKey);

        // Create an area shape
        List<Point> areaOutline = new ArrayList<>();
        List<List<Point>> areaNegativeSpace = new ArrayList<>();
        for(int i = 0; i < area.getNumContours(); i++) {
            // Build a new shape from the current area
            // First contour is always the outline
            // The rest are negative space
            double[] xPoints = area.getXContour(i);
            double[] zPoints = area.getZContour(i);
            List<Point> countourPoints = new ArrayList<>();
            for(int c = 0; c < xPoints.length; c++) {
                countourPoints.add(Point.of(xPoints[c], zPoints[c]));
            }
            if(i == 0) {
                // First contour is primary shape
                areaOutline.addAll(countourPoints);
            } else {
                // Other contours are holes in the primary shape
                areaNegativeSpace.add(countourPoints);
            }
        }
        Marker areaShape = Marker.polygon(areaOutline, areaNegativeSpace);
        final MarkerOptions markerOptions = MarkerOptions.builder()
                .stroke(true)
                .strokeColor(lineColor)
                .strokeWeight(3)
                .strokeOpacity(0.5)
                .fill(true)
                .fillColor(areaColor)
                .fillOpacity(0.5)
                .clickTooltip(areaDetail)
                .hoverTooltip(areaLabel)
                .build();
        areaShape.markerOptions(markerOptions);

        // Add area to group layer
        groupLayerProvider.addMarker(areaKey, areaShape);

    }

    /**
     * Remove the region of a territory from the map.
     *
     * @param area The territory to remove
     */
    @Override
    public void drawRemove(AreaTerritory area) {
        if (!isEnabled) return;

        KonTerritory territory = area.getTerritory();

        ChatUtil.printDebug("Erasing squaremap area of territory "+territory.getName());
        Key groupKey = getGroupKey(territory);
        Key areaKey = getAreaKey(territory);

        // Get the layer provider
        MapWorld mapWorld = api.getWorldIfEnabled(BukkitAdapter.worldIdentifier(territory.getWorld())).orElse(null);
        if (mapWorld == null) {
            ChatUtil.printDebug("Could not remove territory "+territory.getName()+" from invalid world, "+territory.getWorld().getName());
            return;
        }

        // Get territory group
        final Registry<LayerProvider> layerRegistry = mapWorld.layerRegistry();
        if (layerRegistry.hasEntry(groupKey)) {
            SimpleLayerProvider groupLayerProvider = (SimpleLayerProvider) layerRegistry.get(groupKey);
            groupLayerProvider.removeMarker(areaKey);
            // Delete group if no more areas
            if (groupLayerProvider.getMarkers().isEmpty()) {
                layerRegistry.unregister(groupKey);
                ChatUtil.printDebug("Removing squaremap group of territory "+territory.getName());
            }
        } else {
            ChatUtil.printDebug("Failed to erase from missing squaremap group, territory "+territory.getName());
        }
    }

    /**
     * Update the label of a territory.
     *
     * @param area The territory to label
     */
    @Override
    public void drawLabel(AreaTerritory area) {
        if (!isEnabled) return;

        KonTerritory territory = area.getTerritory();

        Key groupKey = getGroupKey(territory);
        Key areaKey = getAreaKey(territory);
        String areaLabel = MapHandler.getIconLabel(territory); // The display name of the area (icon)
        String areaDetail = MapHandler.getAreaLabel(territory); // The display details of the area
        Color areaColor = getAreaColor(territory);
        Color lineColor = getLineColor(territory);

        // Get the layer provider
        MapWorld mapWorld = api.getWorldIfEnabled(BukkitAdapter.worldIdentifier(territory.getWorld())).orElse(null);
        if (mapWorld == null) {
            ChatUtil.printDebug("Could not label territory "+territory.getName()+" from invalid world, "+territory.getWorld().getName());
            return;
        }
        // Get territory group
        final Registry<LayerProvider> layerRegistry = mapWorld.layerRegistry();
        if (!layerRegistry.hasEntry(groupKey)) {
            ChatUtil.printDebug("Failed to label missing squaremap group, territory "+territory.getName());
            return;
        }
        SimpleLayerProvider groupLayerProvider = (SimpleLayerProvider) layerRegistry.get(groupKey);
        if (!groupLayerProvider.hasMarker(areaKey)) {
            ChatUtil.printDebug("Failed to label missing squaremap marker, territory "+territory.getName());
            return;
        }
        // Get area marker
        Marker areaShape = groupLayerProvider.registeredMarkers().get(areaKey);
        MarkerOptions markerOptions = areaShape.markerOptions().asBuilder()
                .strokeColor(lineColor)
                .fillColor(areaColor)
                .clickTooltip(areaDetail)
                .hoverTooltip(areaLabel)
                .build();
        areaShape.markerOptions(markerOptions);
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

    /* Helper Methods */

    private int getGroupIndex(KonTerritory territory) {
        switch (territory.getTerritoryType()) {
            case SANCTUARY:
                return 40;
            case RUIN:
                return 30;
            case CAMP:
                return 20;
            case CAPITAL:
            case TOWN:
                return 10;
            default:
                return 100;
        }
    }

    private Key getGroupKey(KonTerritory territory) {
        String result = "konquest";
        switch (territory.getTerritoryType()) {
            case SANCTUARY:
                result = result+".marker.sanctuary";
                break;
            case RUIN:
                result = result+".marker.ruin";
                break;
            case CAMP:
                result = result+".marker.camp";
                break;
            case CAPITAL:
            case TOWN:
                result = result+".marker.kingdom";
                break;
            default:
                break;
        }
        return Key.of(result);
    }

    private Key getAreaKey(KonTerritory territory) {
        String result = "konquest";
        switch (territory.getTerritoryType()) {
            case SANCTUARY:
                result = result+".area.sanctuary."+territory.getName().toLowerCase();
                break;
            case RUIN:
                result = result+".area.ruin."+territory.getName().toLowerCase();
                break;
            case CAMP:
                result = result+".area.camp."+territory.getName().toLowerCase();
                break;
            case CAPITAL:
                result = result+".area.kingdom."+territory.getKingdom().getName().toLowerCase()+".capital";
                break;
            case TOWN:
                result = result+".area.kingdom."+territory.getKingdom().getName().toLowerCase()+"."+territory.getName().toLowerCase();
                break;
            default:
                break;
        }
        return Key.of(result);
    }

    private Color getAreaColor(KonTerritory territory) {
        Color result = new Color(0xFFFFFF, false);
        switch (territory.getTerritoryType()) {
            case SANCTUARY:
                result = new Color(MapHandler.sanctuaryColor, false);
                break;
            case RUIN:
                result = new Color(MapHandler.ruinColor, false);
                break;
            case CAMP:
                result = new Color(MapHandler.campColor, false);
                break;
            case CAPITAL:
            case TOWN:
                result = new Color(MapHandler.getWebColor(territory), false);
                break;
            default:
                break;
        }
        return result;
    }

    private Color getLineColor(KonTerritory territory) {
        Color result = new Color(MapHandler.lineDefaultColor, false);
        if (Objects.requireNonNull(territory.getTerritoryType()) == KonquestTerritoryType.CAPITAL) {
            result = new Color(MapHandler.lineCapitalColor, false);
        }
        return result;
    }

}
