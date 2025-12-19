package com.github.rumsfield.konquest.model;

public class KonCampUpgrade {

    // Upgrade cost to buy
    private double cost;

    // Upgrade attributes for camps
    private int level;
    private int maxGuests;
    private int maxHits;
    private boolean noExplode;
    private boolean miningFatigue;

    public KonCampUpgrade(double cost, int level, int maxGuests, int maxHits, boolean noExplode, boolean miningFatigue) {
        this.cost = cost;
        this.level = level;
        this.maxGuests = maxGuests;
        this.maxHits = maxHits;
        this.noExplode = noExplode;
        this.miningFatigue = miningFatigue;
    }

    /*
     * Getters
     */

    public double getCost() {
        return cost;
    }

    public int getLevel() {
        return level;
    }

    public int getMaxGuests() {
        return maxGuests;
    }

    public int getMaxHits() {
        return maxHits;
    }

    public boolean isNoExplode() {
        return noExplode;
    }

    public boolean isMiningFatigue() {
        return miningFatigue;
    }

}
