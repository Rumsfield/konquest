package com.github.rumsfield.konquest.api.event.server;

import com.github.rumsfield.konquest.api.KonquestAPI;
import com.github.rumsfield.konquest.api.event.KonquestEvent;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * Indicates when a Global Event ends on the server.
 * <p>
 * Global Events are set up by admins and scheduled for specific start and end dates, with or without repetition.
 * These events may include one or more effects applied to the entire server while the event is active.
 * When a Global Event ends, its effects are no longer active on the server.
 * This event cannot be cancelled.
 * </p>
 *
 * @author Rumsfield
 *
 */
public class KonquestGlobalEventEndEvent extends KonquestEvent {

    private static final HandlerList handlers = new HandlerList();

    private final String eventName;
    private final double repetitionDays;
    private final List<String> effectNames;
    private final List<String> effectDescriptions;

    /**
     * Default constructor
     * @param konquest The API instance
     * @param eventName The global event name
     * @param repetitionDays The repetition interval of the event in days
     * @param effectNames The names of effects included with this event
     * @param effectDescriptions The descriptions of effects included with this event
     */
    public KonquestGlobalEventEndEvent(KonquestAPI konquest, String eventName, double repetitionDays, List<String> effectNames, List<String> effectDescriptions) {
        super(konquest);
        this.eventName = eventName;
        this.repetitionDays = repetitionDays;
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
     * Gets the repetition interval of the Global Event that is ending.
     * If the event does not repeat, the repetition is 0.
     *
     * @return The repetition interval in days, or 0 if the event does not repeat
     */
    public double getRepetitionDays() {
        return repetitionDays;
    }

    /**
     * Gets the repetition interval of the Global Event that is starting, formatted as a string number with 2 decimal points.
     *
     * @return The repetition interval in days, as a formatted string
     */
    public String getRepetitionDaysFormat() {
        return String.format("%.2f",repetitionDays);
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

