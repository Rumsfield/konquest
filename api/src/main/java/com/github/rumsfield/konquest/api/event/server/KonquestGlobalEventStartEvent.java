package com.github.rumsfield.konquest.api.event.server;

import com.github.rumsfield.konquest.api.KonquestAPI;
import com.github.rumsfield.konquest.api.event.KonquestEvent;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * Indicates when a Global Event starts on the server.
 * <p>
 * Global Events are set up by admins and scheduled for specific start and end dates, with or without repetition.
 * These events may include one or more effects applied to the entire server while the event is active.
 * When a Global Event starts, its effects become active.
 * This event cannot be cancelled.
 * </p>
 *
 * @author Rumsfield
 *
 */
public class KonquestGlobalEventStartEvent extends KonquestEvent {

    private static final HandlerList handlers = new HandlerList();

    private final String eventName;
    private final double durationHours;
    private final List<String> effectNames;
    private final List<String> effectDescriptions;

    /**
     * Default constructor
     * @param konquest The API instance
     * @param eventName The global event name
     * @param durationHours The duration of the event in hours
     * @param effectNames The names of effects included with this event
     * @param effectDescriptions The descriptions of effects included with this event
     */
    public KonquestGlobalEventStartEvent(KonquestAPI konquest, String eventName, double durationHours, List<String> effectNames, List<String> effectDescriptions) {
        super(konquest);
        this.eventName = eventName;
        this.durationHours = durationHours;
        this.effectNames = effectNames;
        this.effectDescriptions = effectDescriptions;
    }

    /**
     * Gets the name of the Global Event that is starting.
     *
     * @return The name
     */
    public String getGlobalEventName() {
        return eventName;
    }

    /**
     * Gets the duration of the Global Event that is starting.
     *
     * @return The duration in hours
     */
    public double getDurationHours() {
        return durationHours;
    }

    /**
     * Gets the duration of the Global Event that is starting, formatted as a string number with 2 decimal points.
     *
     * @return The duration in hours, as a formatted string
     */
    public String getDurationHoursFormat() {
        return String.format("%.2f",durationHours);
    }

    /**
     * Gets the list of effect names
     *
     * @return The list of effect names
     */
    public List<String> getEffectNames() {
        return new ArrayList<>(effectNames);
    }

    /**
     * Gets the effect names, formatted as a comma-separated single string
     *
     * @return The list of effect names, as a formatted string
     */
    public String getEffectNamesFormat() {
        StringBuilder message = new StringBuilder();
        ListIterator<String> listIter = effectNames.listIterator();
        while(listIter.hasNext()) {
            String currentValue = listIter.next();
            message.append(currentValue);
            if(listIter.hasNext()) {
                message.append(", ");
            }
        }
        return message.toString();
    }

    /**
     * Gets the list of effect descriptions
     *
     * @return The list of effect descriptions
     */
    public List<String> getEffectDescriptions() {
        return new ArrayList<>(effectDescriptions);
    }

    /**
     * Gets the effect descriptions, formatted as a comma-separated single string
     *
     * @return The list of effect descriptions, as a formatted string
     */
    public String getEffectDescriptionsFormat() {
        StringBuilder message = new StringBuilder();
        ListIterator<String> listIter = effectDescriptions.listIterator();
        while(listIter.hasNext()) {
            String currentValue = listIter.next();
            message.append(currentValue);
            if(listIter.hasNext()) {
                message.append(", ");
            }
        }
        return message.toString();
    }

    /**
     * Get the handler list
     *
     * @return handlers
     */
    public static HandlerList getHandlerList() {
        return handlers;
    }

    /**
     * Get the handler list
     *
     * @return handlers
     */
    @Override
    @NotNull
    public HandlerList getHandlers() {
        return handlers;
    }
}
