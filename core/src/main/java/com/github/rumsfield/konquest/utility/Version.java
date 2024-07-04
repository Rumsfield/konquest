package com.github.rumsfield.konquest.utility;

import org.jetbrains.annotations.NotNull;

/**
 * Copied from stackoverflow
 * <a href="https://stackoverflow.com/questions/198431/how-do-you-compare-two-version-strings-in-java">Stackoverflow</a>
 * @author alex
 *
 */
public class Version implements Comparable<Version> {

    private final String version;

    public final String get() {
        return this.version;
    }

    public Version(String version) {
        if(version == null)
            throw new IllegalArgumentException("Version can not be null");
        if(!version.matches("[0-9]+(\\.[0-9]+)*"))
            throw new IllegalArgumentException("Invalid version format");
        this.version = version;
    }

    /**
     * Compares versions
     * @param that the object to be compared.
     * @return 0 when versions are equal, -1 when this version is less than that version,
     *         and 1 when this version is greater than that version.
     */
    @Override public int compareTo(@NotNull Version that) {
        String[] thisParts = this.get().split("\\.");
        String[] thatParts = that.get().split("\\.");
        int length = Math.max(thisParts.length, thatParts.length);
        for(int i = 0; i < length; i++) {
            int thisPart = i < thisParts.length ?
                Integer.parseInt(thisParts[i]) : 0;
            int thatPart = i < thatParts.length ?
                Integer.parseInt(thatParts[i]) : 0;
            if(thisPart < thatPart)
                return -1;
            if(thisPart > thatPart)
                return 1;
        }
        return 0;
    }

    @Override public boolean equals(Object that) {
        if(this == that)
            return true;
        if(that == null)
            return false;
        if(this.getClass() != that.getClass())
            return false;
        return this.compareTo((Version) that) == 0;
    }

}
