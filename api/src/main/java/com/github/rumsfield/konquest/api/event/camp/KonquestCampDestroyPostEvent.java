package com.github.rumsfield.konquest.api.event.camp;

import com.github.rumsfield.konquest.api.model.KonquestOfflinePlayer;
import org.bukkit.Location;

import com.github.rumsfield.konquest.api.KonquestAPI;
import com.github.rumsfield.konquest.api.model.KonquestPlayer;

import javax.annotation.Nullable;

/**
 * Called after a player breaks a camp's bed to destroy it.
 * <p>
 * This event is called when any player breaks the camp's bed, even the camp owner.
 * The camp referenced by this event no longer exists, so the getCamp() method will return null.
 * This event will not be invoked when {@link KonquestCampDestroyEvent KonquestCampDestroyEvent} is cancelled.
 * </p>
 *
 * @author Rumsfield
 *
 */
public class KonquestCampDestroyPostEvent extends KonquestCampEvent {

    private final KonquestOfflinePlayer owner;
    private final KonquestPlayer player;
    private final Location location;

    /**
     * Default constructor
     * @param konquest The API instance
     * @param owner The camp owner
     * @param player The player
     * @param location The location
     */
    public KonquestCampDestroyPostEvent(KonquestAPI konquest, KonquestOfflinePlayer owner, KonquestPlayer player, Location location) {
        super(konquest, null);
        this.owner = owner;
        this.player = player;
        this.location = location;
    }

    /**
     * Gets the owner of the destroyed camp.
     *
     * @return The camp owner, or null if no owner was found
     */
    public @Nullable KonquestOfflinePlayer getOwner() {
        return owner;
    }

    /**
     * Gets the player that destroyed the camp's bed.
     *
     * @return The player
     */
    public KonquestPlayer getPlayer() {
        return player;
    }

    /**
     * Gets the location of camp's bed.
     *
     * @return The location
     */
    public Location getLocation() {
        return location;
    }

}
