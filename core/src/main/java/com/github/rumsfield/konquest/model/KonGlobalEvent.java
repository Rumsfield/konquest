package com.github.rumsfield.konquest.model;

import java.util.Date;
import java.util.HashSet;

public class KonGlobalEvent {

    private Date startDate;
    private long duration;
    private long repetition;
    private final String name;
    private boolean doBroadcastOnStart;
    private final HashSet<KonGlobalEventEffect> effects;
    private boolean isEnabled;
    private boolean isActive;

    public KonGlobalEvent(String name) {
        this.name = name;
        this.effects = new HashSet<>();
        this.isEnabled = false;
        this.isActive = false;
        this.doBroadcastOnStart = true;
        this.startDate = new Date();
        this.duration = 0;
        this.repetition = 0;
    }

    public String getName() {
        return name;
    }

    public void setBroadcast(boolean val) {
        doBroadcastOnStart = val;
    }

    public boolean isBroadcast() {
        return doBroadcastOnStart;
    }

    public void setEnabled(boolean val) {
        isEnabled = val;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public boolean isRepeating() {
        return repetition > 0;
    }

    public void addEffect(KonGlobalEventEffect effect) {
        if (effect != null) {
            // Check for conflicts
            for (KonGlobalEventEffect eventEffect : effects) {
                if (eventEffect.isConflict(effect)) {
                    return;
                }
            }
            // Add the effect
            effects.add(effect);
        }
    }

    public void clearEffects() {
        effects.clear();
    }

    public boolean hasEffect(KonGlobalEventEffect effect) {
        return effects.contains(effect);
    }

    public void setStart(long val) {
        startDate = new Date(val);
    }

    public void setDuration(long val) {
        duration = val;
    }

    public void setRepetition(long val) {
        repetition = val;
    }

    public long getStartTime() {
        return startDate.getTime();
    }

    public long getDurationTime() {
        return duration;
    }

    public long getRepetitionTime() {
        return repetition;
    }

    public boolean refreshActive() {
        // Is the event currently active now?
        Date now = new Date();
        isActive = isActiveOnDate(now);
        return isActive;
    }

    public boolean isActiveOnDate(Date date) {
        // Is the event active on the given date?
        if (date.after(startDate)) {
            // Date is after event start
            // Find how many repetition intervals
            long startOffset = 0;
            if (repetition > 0) {
                long diffTime = date.getTime() - startDate.getTime();
                int numReps = (int)(diffTime / repetition);
                startOffset = numReps * repetition;
            }
            // Derive start and end dates
            Date activeStart = new Date(startDate.getTime() + startOffset);
            Date activeEnd = new Date(activeStart.getTime() + duration);
            return date.after(activeStart) && date.before(activeEnd); // cache result

        }
        return false;
    }

    public boolean isActive() {
        return isActive;
    }

}
