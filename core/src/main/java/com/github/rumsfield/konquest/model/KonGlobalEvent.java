package com.github.rumsfield.konquest.model;

import javax.annotation.Nullable;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashSet;

public class KonGlobalEvent {

    private Date startDate;
    private long duration;
    private long repetition;
    private final String name;
    private final HashSet<KonGlobalEventEffect> effects;
    private boolean isEnabled;
    private boolean isActive;

    public KonGlobalEvent(String name) {
        this.name = name;
        this.effects = new HashSet<>();
        this.isEnabled = false;
        this.isActive = false;
        this.startDate = new Date();
        this.duration = 0;
        this.repetition = 0;
    }

    public String getName() {
        return name;
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

    public @Nullable KonGlobalEventEffect getConflictEffect(KonGlobalEventEffect effect) {
        // Check for conflicts
        for (KonGlobalEventEffect eventEffect : effects) {
            if (eventEffect.isGroupConflict(effect)) {
                return eventEffect;
            }
        }
        // Return null if no event effects conflict with given effect
        return null;
    }

    public boolean addEffect(KonGlobalEventEffect effect) {
        if (effect != null) {
            // Check for conflicts
            for (KonGlobalEventEffect eventEffect : effects) {
                if (eventEffect.isGroupConflict(effect)) {
                    return false;
                }
            }
            // Add the effect
            effects.add(effect);
            return true;
        }
        return false;
    }

    public boolean removeEffect(KonGlobalEventEffect effect) {
        if (effect != null && effects.contains(effect)) {
            effects.remove(effect);
            return true;
        }
        return false;
    }

    public void clearEffects() {
        effects.clear();
    }

    public boolean hasEffect(KonGlobalEventEffect effect) {
        return effects.contains(effect);
    }

    public HashSet<KonGlobalEventEffect> getEffects() {
        return new HashSet<>(effects);
    }

    public void setStart(Date val) {
        startDate = val;
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

    public boolean isActive() {
        return isActive;
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
            return date.after(activeStart) && date.before(activeEnd);
        }
        return false;
    }

    public @Nullable Date getNextStart() {
        // Get the next start time after current time, mainly for repeating events
        Date now = new Date();
        if (now.before(startDate)) {
            // Initial start date is next
            return startDate;
        } else if (isRepeating()) {
            // Event repeats, find next start time
            long diffTime = now.getTime() - startDate.getTime();
            int numReps = (int)(diffTime / repetition);
            long startOffset = (numReps+1) * repetition;
            return new Date(startDate.getTime() + startOffset);
        } else {
            // Could not find a start time
            return null;
        }
    }

    public String getStartDateFormat() {
        // Use date format locale
        return DateFormat.getDateInstance().format(startDate);
    }

    public String getNextStartDateFormat() {
        // Use date format locale
        Date nextStart = getNextStart();
        if (nextStart == null) {
            return "?";
        } else {
            return DateFormat.getDateInstance().format(nextStart);
        }
    }

    public String getNextEndDateFormat() {
        // Use date format locale
        Date nextStart = getNextStart();
        if (nextStart == null) {
            return "?";
        } else {
            Date nextEnd = new Date(nextStart.getTime() + duration);
            return DateFormat.getDateInstance().format(nextEnd);
        }
    }

    public int getDurationHours() {
        // 1000 ms in a second, 60 seconds in a minute, 60 minutes in an hour
        return (int)(duration / 1000 / 60 / 60);
    }

    public int getRepetitionDays() {
        // 1000 ms in a second, 60 seconds in a minute, 60 minutes in an hour, 24 hours in a day
        return (int)(repetition / 1000 / 60 / 60 / 24);
    }

}
