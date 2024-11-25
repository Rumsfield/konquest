package com.github.rumsfield.konquest.api.event.player;

import org.bukkit.Location;
import org.bukkit.event.Cancellable;

import com.github.rumsfield.konquest.api.KonquestAPI;
import com.github.rumsfield.konquest.api.model.KonquestPlayer;
import com.github.rumsfield.konquest.api.event.kingdom.KonquestKingdomCreateEvent;

/**
 * Called before a player creates a new kingdom using the "/k kingdom create" command.
 * <p>
 * This event is called before the kingdom is created.
 * It is not called when admins create admin kingdoms, see {@link KonquestKingdomCreateEvent KonquestKingdomCreateEvent}
 * Canceling this event will prevent the kingdom from being created.
 * </p>
 *
 * @author Rumsfield
 *
 */
public class KonquestPlayerCreateKingdomEvent extends KonquestPlayerEvent implements Cancellable {

    private boolean isCancelled;

    private final Location location;
    private final String name;

    /**
     * Default constructor
     * @param konquest The API instance
     * @param player The player
     * @param location The location
     * @param name The name of the kingdom
     */
    public KonquestPlayerCreateKingdomEvent(KonquestAPI konquest, KonquestPlayer player, Location location, String name) {
        super(konquest, player);
        this.isCancelled = false;
        this.location = location;
        this.name = name;
    }

    /**
     * Gets the location where the new kingdom capital will be created.
     *
     * @return The center location of the new kingdom capital
     */
    public Location getLocation() {
        return location;
    }

    /**
     * Gets the name of the new kingdom.
     *
     * @return The name
     */
    public String getName() {
        return name;
    }

    /**
     * Checks whether this event is canceled.
     *
     * @return True when the event is canceled, else false
     */
    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    /**
     * Controls whether the event is canceled.
     * Canceling this event will prevent the kingdom from being created.
     *
     * @param val True to cancel this event, else false
     */
    @Override
    public void setCancelled(boolean val) {
        isCancelled = val;
    }

}
