package com.github.rumsfield.konquest.utility;

import org.bukkit.Color;

/**
 * Wrapper enum for Bukkit's Color class's static fields.
 */
public enum ColorRGB {

    AQUA        ("Aqua",        Color.AQUA),
    BLACK       ("Black",       Color.BLACK),
    BLUE        ("Blue",        Color.BLUE),
    FUCHSIA     ("Fuchsia",     Color.FUCHSIA),
    GRAY        ("Gray",        Color.GRAY),
    GREEN       ("Green",       Color.GREEN),
    LIME        ("Lime",        Color.LIME),
    MAROON      ("Maroon",      Color.MAROON),
    NAVY        ("Navy",        Color.NAVY),
    OLIVE       ("Olive",       Color.OLIVE),
    ORANGE      ("Orange",      Color.ORANGE),
    PURPLE      ("Purple",      Color.PURPLE),
    RED         ("Red",         Color.RED),
    SILVER      ("Silver",      Color.SILVER),
    TEAL        ("Teal",        Color.TEAL),
    WHITE       ("White",       Color.WHITE),
    YELLOW      ("Yellow",      Color.YELLOW);

    private final String name;
    private final Color color;

    ColorRGB(String name, Color color) {
        this.name = name;
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public Color getColor() {
        return color;
    }

    /**
     * Gets a ColorRGB enum given a string name.
     * @param name The string name of the ColorRGB
     * @return ColorRGB - Corresponding enum
     */
    public static ColorRGB fromName(String name) {
        ColorRGB result = null;
        for(ColorRGB colorInst : ColorRGB.values()) {
            if(colorInst.getName().equalsIgnoreCase(name)) {
                result = colorInst;
                break;
            }
        }
        return result;
    }

    /**
     * Gets a ColorRGB enum given a color object.
     * @param color The Color field of the ColorRGB
     * @return ColorRGB - Corresponding enum
     */
    public static ColorRGB fromColor(Color color) {
        ColorRGB result = null;
        for(ColorRGB colorInst : ColorRGB.values()) {
            if(colorInst.getColor().equals(color)) {
                result = colorInst;
                break;
            }
        }
        return result;
    }
}
