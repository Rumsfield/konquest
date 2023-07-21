package com.github.rumsfield.konquest.shop;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.api.model.KonquestTerritoryType;
import com.github.rumsfield.konquest.model.*;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.maxgamer.quickshop.api.QuickShopAPI;
import org.maxgamer.quickshop.api.shop.Shop;

import java.awt.*;
import java.util.Collection;
import java.util.Map;

/**
 * This class manages generic chest shops:
 * - Only allow shops to be created within friendly land, and not in the Wild.
 * - Only allow players to trade with shops in friendly, trade or alliance territory (and optionally sanctuary).
 * - Upon town capture, remove all shops within the town land, leaving behind chests with contents.
 */
public class ShopHandler {

    private Konquest konquest;

    public ShopHandler(Konquest konquest) {
        this.konquest = konquest;
    }



    public void deleteShopsInPoints(Collection<Point> points, World world) {
        if(points.isEmpty() || world == null) return;

        // QuickShop
        if(konquest.getIntegrationManager().getQuickShop().isEnabled()) {
            QuickShopAPI quickshop = konquest.getIntegrationManager().getQuickShop().getAPI();
            if(quickshop != null) {
                for(Point point : points) {
                    int chunkX = point.x;
                    int chunkZ = point.y;
                    Map<Location, Shop> shopList = quickshop.getShopManager().getShops(world.getName(), chunkX, chunkZ);
                    if(shopList != null) {
                        for(Map.Entry<Location, Shop> entry : shopList.entrySet()) {
                            Location shopLoc = entry.getKey();
                            final OfflinePlayer owner = Bukkit.getOfflinePlayer(entry.getValue().getOwner());
                            world.playEffect(shopLoc, Effect.ANVIL_BREAK, null);
                            ChatUtil.printDebug("Deleting shop owned by "+owner.getName());
                            entry.getValue().delete();
                        }
                    }
                }
            }
        }
        // Other shop plugins
    }

    /**
     * Called when a player tries to use (trade with) a shop.
     * @param shopLoc - The location of the chest shop.
     * @param bukkitPlayer - The player attempting to trade.
     * @return True if the trade is allowed, else false to cancel the trade.
     */
    public boolean onShopUse(Location shopLoc, Player bukkitPlayer) {
        if(konquest.isWorldIgnored(shopLoc)) return true;
        KonPlayer player = konquest.getPlayerManager().getPlayer(bukkitPlayer);

        // QuickShop
        if(konquest.getIntegrationManager().getQuickShop().isEnabled()) {
            QuickShopAPI quickshop = konquest.getIntegrationManager().getQuickShop().getAPI();
            if (quickshop != null) {
                // Bypass all checks for admins in bypass mode
                if(player != null && !player.isAdminBypassActive()) {
                    if(konquest.getTerritoryManager().isChunkClaimed(shopLoc)) {
                        KonTerritory territory = konquest.getTerritoryManager().getChunkTerritory(shopLoc);
                        assert territory != null;
                        boolean isRelationAllowed = konquest.getKingdomManager().isPlayerFriendly(player,territory.getKingdom()) ||
                                konquest.getKingdomManager().isPlayerTrade(player,territory.getKingdom()) ||
                                konquest.getKingdomManager().isPlayerAlly(player,territory.getKingdom()) ||
                                territory.getTerritoryType().equals(KonquestTerritoryType.SANCTUARY);
                        if(!isRelationAllowed) {
                            ChatUtil.sendError(bukkitPlayer, MessagePath.QUICKSHOP_ERROR_ENEMY_USE.getMessage());
                            return false;
                        }
                    }
                }
            }
        }
        // Other shop plugins

        return true;
    }

    public boolean onShopCreate(Location shopLoc, Player bukkitPlayer) {
        if(konquest.isWorldIgnored(shopLoc)) return true;
        KonPlayer player = konquest.getPlayerManager().getPlayer(bukkitPlayer);
        boolean isShopClaimed = konquest.getTerritoryManager().isChunkClaimed(shopLoc);
        // Prevent creating any shops in the wild
        if(!isShopClaimed) {
            ChatUtil.sendError(bukkitPlayer, MessagePath.QUICKSHOP_ERROR_WILD.getMessage());
            return false;
        }
        KonTerritory territory = konquest.getTerritoryManager().getChunkTerritory(shopLoc);
        assert territory != null;

        // QuickShop
        if(konquest.getIntegrationManager().getQuickShop().isEnabled()) {
            QuickShopAPI quickshop = konquest.getIntegrationManager().getQuickShop().getAPI();
            if(quickshop != null && player != null && !player.isAdminBypassActive()) {
                if(territory instanceof KonTown) {
                    KonTown town = (KonTown)territory;
                    if(!konquest.getKingdomManager().isPlayerFriendly(player,town.getKingdom())) {
                        ChatUtil.sendError(bukkitPlayer, MessagePath.QUICKSHOP_ERROR_ENEMY_USE.getMessage());
                        return false;
                    }
                    if(town.isLocInsideMonumentProtectionArea(shopLoc)) {
                        ChatUtil.sendError(bukkitPlayer, MessagePath.QUICKSHOP_ERROR_MONUMENT.getMessage());
                        return false;
                    }
                }
                if(territory instanceof KonCamp && !((KonCamp)territory).isPlayerOwner(bukkitPlayer)) {
                    ChatUtil.sendError(bukkitPlayer, MessagePath.QUICKSHOP_ERROR_CAMP.getMessage());
                    return false;
                }
            }
        }
        // Other shop plugins

        return true;
    }
}
