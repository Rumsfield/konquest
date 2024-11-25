package com.github.rumsfield.konquest.api.event.kingdom;

import org.bukkit.Location;

import com.github.rumsfield.konquest.api.KonquestAPI;
import com.github.rumsfield.konquest.api.model.KonquestKingdom;
import com.github.rumsfield.konquest.api.model.KonquestPlayer;

/**
 * Called after a kingdom is created, either by players or admins.
 * <p>
 * Kingdoms can be created by normal players and/or admins, depending on the Konquest configuration.
 * </p>
 *
 * @author Rumsfield
 *
 */
public class KonquestKingdomCreateEvent extends KonquestKingdomEvent {

    private final KonquestPlayer player;
    private final Location location;
    private final boolean isAdmin;

    /**
     * Default constructor
     * @param konquest The API instance
     * @param kingdom The kingdom
     * @param player The player
     * @param location The location
     * @param isAdmin Whether the kingdom an admin kingdom
     */
    public KonquestKingdomCreateEvent(KonquestAPI konquest, KonquestKingdom kingdom, KonquestPlayer player, Location location, boolean isAdmin) {
        super(konquest, kingdom);
        this.player = player;
        this.location = location;
        this.isAdmin = isAdmin;
    }

    /**
     * Gets the player that created the new kingdom.
     *
     * @return The player
     */
    public KonquestPlayer getPlayer() {
        return player;
    }

    /**
     * Gets the location where the new kingdom capital was created.
     *
     * @return The center location of the new kingdom capital
     */
    public Location getLocation() {
        return location;
    }

    /**
     * Gets whether the new kingdom is an admin kingdom.
     *
     * @return True when the kingdom is an admin kingdom, else false
     */
    public boolean isAdmin() {
        return isAdmin;
    }

}
