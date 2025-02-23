package com.github.rumsfield.konquest.map;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.api.model.KonquestTerritoryType;
import com.github.rumsfield.konquest.model.*;
import com.github.rumsfield.konquest.utility.ChatUtil;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.Marker;
import org.dynmap.markers.MarkerIcon;
import org.dynmap.markers.MarkerSet;

import java.util.HashMap;
import java.util.Objects;

public class DynmapRender implements Renderable {

    private final Konquest konquest;
    private boolean isEnabled;
    private static DynmapAPI dapi = null;
    private final HashMap<KonTerritory,AreaTerritory> areaCache;

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
        }
    }

    /*
     * Dynmap Area ID formats:
     * Sanctuaries
     * 		MarkerSet Group:  		konquest.marker.sanctuary
     * 		AreaMarker Points: 		konquest.area.sanctuary.<name>.point.<n>
     * 		AreaMarker Contours: 	konquest.area.sanctuary.<name>.contour.<n>
     * Ruins
     * 		MarkerSet Group:  		konquest.marker.ruin
     * 		AreaMarker Points: 		konquest.area.ruin.<name>.point.<n>
     * 		AreaMarker Contours: 	konquest.area.ruin.<name>.contour.<n>
     * Camps
     * 		MarkerSet Group:  		konquest.marker.camp
     * 		AreaMarker Points: 		konquest.area.camp.<name>.point.<n>
     * 		AreaMarker Contours: 	konquest.area.camp.<name>.contour.<n>
     * Kingdoms
     * 		MarkerSet Group:  		konquest.marker.kingdom
     * 		Capital
     * 		AreaMarker Points: 		konquest.area.kingdom.<kingdom>.capital.point.<n>
     * 		AreaMarker Contours: 	konquest.area.kingdom.<kingdom>.capital.contour.<n>
     * 		Towns
     * 		AreaMarker Points: 		konquest.area.kingdom.<kingdom>.<town>.point.<n>
     * 		AreaMarker Contours: 	konquest.area.kingdom.<kingdom>.<town>.contour.<n>
     */

    @Override
    public void drawUpdate(AreaTerritory area) {
        if (!isEnabled) return;

        KonTerritory territory = area.getTerritory();

        String groupId = getGroupId(territory);
        String groupLabel = MapHandler.getGroupLabel(territory);
        String areaId = getAreaId(territory);
        String areaLabel = MapHandler.getAreaLabel(territory);
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
        AreaMarker areaPoint;
        AreaMarker areaContour;
        // Prune any points and contours
        if(areaCache.containsKey(territory)) {
            // Territory is already rendered
            AreaTerritory oldArea = areaCache.get(territory);
            for(int i = (oldArea.getNumContours()-1); i >= area.getNumContours(); i--) {
                contourId = areaId + ".contour." + i;
                areaContour = territoryGroup.findAreaMarker(contourId);
                if (areaContour != null) {
                    // Delete area from group
                    areaContour.deleteMarker();
                }
            }
            for(int i = (oldArea.getNumPoints()-1); i >= area.getNumPoints(); i--) {
                pointId = areaId + ".point." + i;
                areaPoint = territoryGroup.findAreaMarker(pointId);
                if (areaPoint != null) {
                    // Delete area from group
                    areaPoint.deleteMarker();
                }
            }
        }
        areaCache.put(territory, area);
        // Update or create all points and contours
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
        }
        Marker territoryIcon = territoryGroup.findMarker(iconId);
        if (territoryIcon == null) {
            // Icon does not exist, create new
            territoryIcon = territoryGroup.createMarker(iconId, iconLabel, true, area.getWorldName(), area.getCenterX(), area.getCenterY(), area.getCenterZ(), icon, false);
        } else {
            // Icon already exists, update label
            territoryIcon.setLabel(iconLabel);
        }
    }

    @Override
    public void drawRemove(AreaTerritory area) {
        if (!isEnabled) return;

        KonTerritory territory = area.getTerritory();

        ChatUtil.printDebug("Erasing Dynmap area of territory "+territory.getName());
        String groupId = getGroupId(territory);
        String areaId = getAreaId(territory);
        String iconId = getIconId(territory);

        MarkerSet territoryGroup = dapi.getMarkerAPI().getMarkerSet(groupId);
        if (territoryGroup != null) {
            if(areaCache.containsKey(territory)) {
                // Territory is already rendered, remove all points and contours
                String pointId;
                String contourId;
                AreaMarker areaPoint;
                AreaMarker areaContour;
                AreaTerritory oldArea = areaCache.get(territory);
                for(int i = 0; i < oldArea.getNumContours(); i++) {
                    contourId = areaId + ".contour." + i;
                    areaContour = territoryGroup.findAreaMarker(contourId);
                    if (areaContour != null) {
                        // Delete area from group
                        areaContour.deleteMarker();
                    }
                }
                for(int i = 0; i < oldArea.getNumPoints(); i++) {
                    pointId = areaId + ".point." +i ;
                    areaPoint = territoryGroup.findAreaMarker(pointId);
                    if (areaPoint != null) {
                        // Delete area from group
                        areaPoint.deleteMarker();
                    }
                }
            } else {
                ChatUtil.printDebug("Failed to erase un-rendered territory "+territory.getName());
            }
            Marker territoryIcon = territoryGroup.findMarker(iconId);
            if (territoryIcon != null) {
                // Delete icon
                territoryIcon.deleteMarker();
                ChatUtil.printDebug("Removing Dynmap icon of territory "+territory.getName());
            }
            if (territoryGroup.getAreaMarkers().isEmpty()) {
                // Delete group if no more areas
                territoryGroup.deleteMarkerSet();
                ChatUtil.printDebug("Removing Dynmap group of territory "+territory.getName());
            }
        }
    }

    @Override
    public void drawLabel(AreaTerritory area) {
        if (!isEnabled) return;

        KonTerritory territory = area.getTerritory();

        String groupId = getGroupId(territory);
        String areaId = getAreaId(territory);
        String areaLabel = MapHandler.getAreaLabel(territory);
        MarkerSet territoryGroup = dapi.getMarkerAPI().getMarkerSet(groupId);
        if (territoryGroup != null) {
            // Update all area point labels
            if(areaCache.containsKey(territory)) {
                // Territory is already rendered, remove all points and contours
                String pointId;
                AreaMarker areaPoint;
                AreaTerritory oldArea = areaCache.get(territory);
                for(int i = 0; i < oldArea.getNumPoints(); i++) {
                    pointId = areaId + ".point." +i ;
                    areaPoint = territoryGroup.findAreaMarker(pointId);
                    if (areaPoint != null) {
                        // Area already exists, update label
                        areaPoint.setLabel(areaLabel,true);
                    }
                }
            } else {
                ChatUtil.printDebug("Failed to label un-rendered territory "+territory.getName());
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
