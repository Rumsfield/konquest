package com.github.rumsfield.konquest.map;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.api.model.KonquestTerritoryType;
import com.github.rumsfield.konquest.model.*;
import com.github.rumsfield.konquest.utility.ChatUtil;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class DynmapRender implements Renderable {

    private final Konquest konquest;
    private boolean isEnabled;
    private static DynmapAPI dapi = null;
    private final HashMap<KonTerritory,ArrayList<String>> areaCache;

    public DynmapRender(Konquest konquest) {
        this.konquest = konquest;
        this.isEnabled = false;
        this.areaCache = new HashMap<>();
    }

    @Override
    public void initialize() {
        // Get Dynmap API from integration manager
        isEnabled = konquest.getIntegrationManager().getDynmap().isEnabled();
        if(isEnabled) {
            dapi = konquest.getIntegrationManager().getDynmap().getAPI();
        } else {
            ChatUtil.printDebug("Failed to initialize DynmapRender with disabled API.");
        }
    }

    @Override
    public String getMapName() {
        return konquest.getIntegrationManager().getDynmap().getPluginName();
    }

    /*
     * Dynmap Area ID formats:
     * Territories must be rendered as a set of AreaMarkers for every chunk, where each marker ID
     * contains an index to uniquely identify it.
     * Dynmap does not provide a way to query for all ID keys, so a local cache is necessary
     * to remember what has already been rendered.
     *
     * Sanctuaries
     * 		MarkerSet Group:  		    konquest.marker.sanctuary
     *      MarkerIcon Icon:            konquest.icon.sanctuary.<name>
     * 		AreaMarker Points: 		    konquest.area.sanctuary.<name>.point.<n>
     * 		PolyLineMarker Contours: 	konquest.area.sanctuary.<name>.contour.<n>
     * Ruins
     * 		MarkerSet Group:  		    konquest.marker.ruin
     *      MarkerIcon Icon:            konquest.icon.ruin.<name>
     * 		AreaMarker Points: 		    konquest.area.ruin.<name>.point.<n>
     * 		PolyLineMarker Contours: 	konquest.area.ruin.<name>.contour.<n>
     * Camps
     * 		MarkerSet Group:  		    konquest.marker.camp
     *      MarkerIcon Icon:            konquest.icon.camp.<name>
     * 		AreaMarker Points: 		    konquest.area.camp.<name>.point.<n>
     * 		PolyLineMarker Contours: 	konquest.area.camp.<name>.contour.<n>
     * Kingdoms
     * 		MarkerSet Group:  		    konquest.marker.kingdom
     * 		Capital
     *      MarkerIcon Icon:            konquest.icon.kingdom.<kingdom>.capital
     * 		AreaMarker Points: 		    konquest.area.kingdom.<kingdom>.capital.point.<n>
     * 		PolyLineMarker Contours: 	konquest.area.kingdom.<kingdom>.capital.contour.<n>
     * 		Towns
     *      MarkerIcon Icon:            konquest.icon.kingdom.<kingdom>.<town>
     * 		AreaMarker Points: 		    konquest.area.kingdom.<kingdom>.<town>.point.<n>
     * 		PolyLineMarker Contours: 	konquest.area.kingdom.<kingdom>.<town>.contour.<n>
     */

    @Override
    public void drawUpdate(AreaTerritory area) {
        if (!isEnabled) return;

        KonTerritory territory = area.getTerritory();

        String groupId = getGroupId(territory);
        String groupLabel = MapHandler.getGroupLabel(territory);
        String areaId = getAreaId(territory);
        String areaLabel = MapHandler.getAreaLabel(territory,true);
        int areaColor = getAreaColor(territory);
        int lineColor = getLineColor(territory);
        String iconId = getIconId(territory);
        String iconLabel = MapHandler.getIconLabel(territory);
        MarkerIcon icon = getIconMarker(territory);
        // Get territory group
        MarkerSet territoryGroup = dapi.getMarkerAPI().getMarkerSet(groupId);
        if (territoryGroup == null) {
            territoryGroup = dapi.getMarkerAPI().createMarkerSet(groupId, groupLabel, dapi.getMarkerAPI().getMarkerIcons(), false);
        }
        String pointId;
        String contourId;
        AreaMarker areaPoint; // Points of chunks with filled in color, no edge lines
        AreaMarker areaContour; // Clear areas with edge lines

        // Clear all previous contour and point areas
        if (areaCache.containsKey(territory)) {
            // Territory was previously rendered
            for (String previousAreaId : areaCache.get(territory)) {
                AreaMarker areaMarker = territoryGroup.findAreaMarker(previousAreaId);
                if (areaMarker != null) {
                    // Delete area from group
                    areaMarker.deleteMarker();
                }
            }
            ChatUtil.printDebug("Cleared previously rendered Dynmap territory "+territory.getName());
        }

        // Set all contour lines and area points
        ArrayList<String> areaIdList = new ArrayList<>();
        for(int i = 0; i < area.getNumContours(); i++) {
            contourId = areaId + ".contour." + i;
            areaContour = territoryGroup.findAreaMarker(contourId);
            if (areaContour == null) {
                // Area does not exist, create new
                areaContour = territoryGroup.createAreaMarker(contourId, "", false, area.getWorldName(), area.getXContour(i), area.getZContour(i), false);
                if (areaContour != null) {
                    areaContour.setFillStyle(0, areaColor);
                    areaContour.setLineStyle(1, 1, lineColor);
                }
            } else {
                // Area already exists, update corners and color
                areaContour.setCornerLocations(area.getXContour(i), area.getZContour(i));
                areaContour.setFillStyle(0, areaColor);
            }
            areaIdList.add(contourId);
        }
        for(int i = 0; i < area.getNumPoints(); i++) {
            pointId = areaId + ".point." + i;
            areaPoint = territoryGroup.findAreaMarker(pointId);
            if (areaPoint == null) {
                // Area does not exist, create new
                areaPoint = territoryGroup.createAreaMarker(pointId, areaLabel, true, area.getWorldName(), area.getXPoint(i), area.getZPoint(i), false);
                if (areaPoint != null) {
                    areaPoint.setFillStyle(0.5, areaColor);
                    areaPoint.setLineStyle(0, 0, lineColor);
                    areaPoint.setLabel(areaLabel,true);
                }
            } else {
                // Area already exists, update corners and label and color
                areaPoint.setCornerLocations(area.getXPoint(i), area.getZPoint(i));
                areaPoint.setLabel(areaLabel,true);
                areaPoint.setFillStyle(0.5, areaColor);
            }
            areaIdList.add(pointId);
        }
        areaCache.put(territory,areaIdList);
        ChatUtil.printDebug("Updated Dynmap territory "+territory.getName());
        Marker territoryIcon = territoryGroup.findMarker(iconId);
        if (territoryIcon == null) {
            // Icon does not exist, create new
            territoryGroup.createMarker(iconId, iconLabel, true, area.getWorldName(), area.getCenterX(), area.getCenterY(), area.getCenterZ(), icon, false);
        } else {
            // Icon already exists, update label
            territoryIcon.setLabel(iconLabel);
        }
    }

    @Override
    public void drawRemove(AreaTerritory area) {
        if (!isEnabled) return;

        KonTerritory territory = area.getTerritory();

        String groupId = getGroupId(territory);
        String iconId = getIconId(territory);

        MarkerSet territoryGroup = dapi.getMarkerAPI().getMarkerSet(groupId);
        if (territoryGroup != null) {
            if(areaCache.containsKey(territory)) {
                // Territory is already rendered, remove all points and contours
                for (String previousAreaId : areaCache.get(territory)) {
                    AreaMarker areaMarker = territoryGroup.findAreaMarker(previousAreaId);
                    if (areaMarker != null) {
                        // Delete area from group
                        areaMarker.deleteMarker();
                    }
                }
            } else {
                ChatUtil.printDebug("Failed to erase un-rendered Dynmap territory "+territory.getName());
            }
            Marker territoryIcon = territoryGroup.findMarker(iconId);
            if (territoryIcon != null) {
                // Delete icon
                territoryIcon.deleteMarker();
            }
            if (territoryGroup.getAreaMarkers().isEmpty()) {
                // Delete group if no more areas
                territoryGroup.deleteMarkerSet();
            }
        }
    }

    @Override
    public void drawLabel(AreaTerritory area) {
        if (!isEnabled) return;

        KonTerritory territory = area.getTerritory();

        String groupId = getGroupId(territory);
        String areaLabel = MapHandler.getAreaLabel(territory,true);
        MarkerSet territoryGroup = dapi.getMarkerAPI().getMarkerSet(groupId);
        if (territoryGroup != null) {
            // Update all area point labels
            if(areaCache.containsKey(territory)) {
                // Territory is already rendered, apply new labels
                for (String previousAreaId : areaCache.get(territory)) {
                    AreaMarker areaMarker = territoryGroup.findAreaMarker(previousAreaId);
                    if (areaMarker != null) {
                        // Set the new label
                        areaMarker.setLabel(areaLabel,true);
                    }
                }
            } else {
                ChatUtil.printDebug("Failed to label un-rendered Dynmap territory "+territory.getName());
            }
        }
    }

    @Override
    public void postBroadcast(String message) {
        if (!isEnabled) return;
        dapi.sendBroadcastToWeb("Konquest", message);
    }

    /* Helper Methods */

    private String getGroupId(KonTerritory territory) {
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
        return result;
    }

    private String getAreaId(KonTerritory territory) {
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
        return result;
    }

    private int getAreaColor(KonTerritory territory) {
        int result = 0xFFFFFF;
        switch (territory.getTerritoryType()) {
            case SANCTUARY:
                result = MapHandler.sanctuaryColor;
                break;
            case RUIN:
                result = MapHandler.ruinColor;
                break;
            case CAMP:
                result = MapHandler.campColor;
                break;
            case CAPITAL:
            case TOWN:
                result = MapHandler.getWebColor(territory);
                break;
            default:
                break;
        }
        return result;
    }

    private int getLineColor(KonTerritory territory) {
        int result = MapHandler.lineDefaultColor;
        if (Objects.requireNonNull(territory.getTerritoryType()) == KonquestTerritoryType.CAPITAL) {
            result = MapHandler.lineCapitalColor;
        }
        return result;
    }

    private String getIconId(KonTerritory territory) {
        String result = "konquest";
        switch (territory.getTerritoryType()) {
            case SANCTUARY:
                result = result+".icon.sanctuary."+territory.getName().toLowerCase();
                break;
            case RUIN:
                result = result+".icon.ruin."+territory.getName().toLowerCase();
                break;
            case CAMP:
                result = result+".icon.camp."+territory.getName().toLowerCase();
                break;
            case CAPITAL:
                result = result+".icon.kingdom."+territory.getKingdom().getName().toLowerCase()+".capital";
                break;
            case TOWN:
                result = result+".icon.kingdom."+territory.getKingdom().getName().toLowerCase()+"."+territory.getName().toLowerCase();
                break;
            default:
                break;
        }
        return result;
    }

    private MarkerIcon getIconMarker(KonTerritory territory) {
        MarkerIcon result = null;
        switch (territory.getTerritoryType()) {
            case SANCTUARY:
                result = dapi.getMarkerAPI().getMarkerIcon("temple");
                break;
            case RUIN:
                result = dapi.getMarkerAPI().getMarkerIcon("tower");
                break;
            case CAMP:
                result = dapi.getMarkerAPI().getMarkerIcon("pirateflag");
                break;
            case CAPITAL:
                result = dapi.getMarkerAPI().getMarkerIcon("star");
                break;
            case TOWN:
                result = dapi.getMarkerAPI().getMarkerIcon("orangeflag");
                break;
            default:
                break;
        }
        return result;
    }

}
