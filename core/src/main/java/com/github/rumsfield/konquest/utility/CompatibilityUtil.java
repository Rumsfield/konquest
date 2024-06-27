package com.github.rumsfield.konquest.utility;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

/**
 * This class provides static method implementations that work for the full range of supported Spigot API versions.
 */
public class CompatibilityUtil {

    /**
     * Map old enchantment namespaces to new ones
     */
    public enum EnchantComp {
        // Enchantment              Pre 1.20.6                      1.20.6 and later
        // Common
        BINDING_CURSE               ("binding_curse",               "binding_curse"),
        CHANNELING                  ("channeling",                  "channeling"),
        DEPTH_STRIDER               ("depth_strider",               "depth_strider"),
        FIRE_ASPECT                 ("fire_aspect",                 "fire_aspect"),
        FROST_WALKER                ("frost_walker",                "frost_walker"),
        IMPALING                    ("impaling",                    "impaling"),
        KNOCKBACK                   ("knockback",                   "knockback"),
        LOYALTY                     ("loyalty",                     "loyalty"),
        LURE                        ("lure",                        "lure"),
        MENDING                     ("mending",                     "mending"),
        MULTISHOT                   ("multishot",                   "multishot"),
        PIERCING                    ("piercing",                    "piercing"),
        QUICK_CHARGE                ("quick_charge",                "quick_charge"),
        RIPTIDE                     ("riptide",                     "riptide"),
        SILK_TOUCH                  ("silk_touch",                  "silk_touch"),
        SOUL_SPEED                  ("soul_speed",                  "soul_speed"),
        SWEEPING_EDGE               ("sweeping_edge",               "sweeping_edge"),
        SWIFT_SNEAK                 ("swift_sneak",                 "swift_sneak"),
        THORNS                      ("thorns",                      "thorns"),
        VANISHING_CURSE             ("vanishing_curse",             "vanishing_curse"),

        // 1.20.4 and earlier
        WATER_WORKING               ("water_working",               "aqua_afinity"),
        DAMAGE_ARTHROPODS           ("damage_arthropods",           "bane_of_arthropods"),
        PROTECTION_EXPLOSIONS       ("protection_explosions",       "blast_protection"),
        DIG_SPEED                   ("dig_speed",                   "efficiency"),
        PROTECTION_FALL             ("protection_fall",             "feather_falling"),
        PROTECTION_FIRE             ("protection_fire",             "fire_protection"),
        ARROW_FIRE                  ("arrow_fire",                  "flame"),
        LOOT_BONUS_BLOCKS           ("loot_bonus_blocks",           "fortune"),
        ARROW_INFINITE              ("arrow_infinite",              "infinity"),
        LOOT_BONUS_MOBS             ("loot_bonus_mobs",             "looting"),
        LUCK                        ("luck",                        "luck_of_the_sea"),
        ARROW_DAMAGE                ("arrow_damage",                "power"),
        PROTECTION_PROJECTILE       ("protection_projectile",       "projectile_protection"),
        PROTECTION_ENVIRONMENTAL    ("protection_environmental",    "protection"),
        ARROW_KNOCKBACK             ("arrow_knockback",             "punch"),
        OXYGEN                      ("oxygen",                      "respiration"),
        DAMAGE_ALL                  ("damage_all",                  "sharpness"),
        DAMAGE_UNDEAD               ("damage_undead",               "smite"),
        DURABILITY                  ("durability",                  "unbreaking"),

        // 1.20.6 and later
        AQUA_AFINITY                ("water_working",               "aqua_afinity"),
        BANE_OF_ARTHROPODS          ("damage_arthropods",           "bane_of_arthropods"),
        BLAST_PROTECTION            ("protection_explosions",       "blast_protection"),
        BREACH                      ("",                            "breach"),
        DENSITY                     ("",                            "density"),
        EFFICIENCY                  ("dig_speed",                   "efficiency"),
        FEATHER_FALLING             ("protection_fall",             "feather_falling"),
        FIRE_PROTECTION             ("protection_fire",             "fire_protection"),
        FLAME                       ("arrow_fire",                  "flame"),
        FORTUNE                     ("loot_bonus_blocks",           "fortune"),
        INFINITY                    ("arrow_infinite",              "infinity"),
        LOOTING                     ("loot_bonus_mobs",             "looting"),
        LUCK_OF_THE_SEA             ("luck",                        "luck_of_the_sea"),
        POWER                       ("arrow_damage",                "power"),
        PROJECTILE_PROTECTION       ("protection_projectile",       "projectile_protection"),
        PROTECTION                  ("protection_environmental",    "protection"),
        PUNCH                       ("arrow_knockback",             "punch"),
        RESPIRATION                 ("oxygen",                      "respiration"),
        SHARPNESS                   ("damage_all",                  "sharpness"),
        SMITE                       ("damage_undead",               "smite"),
        UNBREAKING                  ("durability",                  "unbreaking"),
        WIND_BURST                  ("",                            "wind_burst");

        public final String namespace1; // Pre 1.20.6
        public final String namespace2; // 1.20.6 and later

        EnchantComp(String namespace1, String namespace2) {
            this.namespace1 = namespace1;
            this.namespace2 = namespace2;
        }

        public static EnchantComp getFromName(String name) {
            for(EnchantComp enchant : EnchantComp.values()) {
                if(enchant.toString().equalsIgnoreCase(name)) {
                    return enchant;
                }
            }
            return null;
        }
    }

    public enum SpigotApiVersion {
        INVALID,
        V1_16_5,
        V1_17_1,
        V1_18_1,
        V1_18_2,
        V1_19_4,
        V1_20_4,
        V1_20_6,
        V1_21_0
    }

    public static SpigotApiVersion getApiVersion() {
        String bukkitVersion = Bukkit.getVersion();
        if (bukkitVersion.contains("1.16.5")) {
            return SpigotApiVersion.V1_16_5;
        } else if (bukkitVersion.contains("1.17.1")) {
            return SpigotApiVersion.V1_17_1;
        } else if (bukkitVersion.contains("1.18.1")) {
            return SpigotApiVersion.V1_18_1;
        } else if (bukkitVersion.contains("1.18.2")) {
            return SpigotApiVersion.V1_18_2;
        } else if (bukkitVersion.contains("1.19.4")) {
            return SpigotApiVersion.V1_19_4;
        } else if (bukkitVersion.contains("1.20.4")) {
            return SpigotApiVersion.V1_20_4;
        } else if (bukkitVersion.contains("1.20.6")) {
            return SpigotApiVersion.V1_20_6;
        } else if (bukkitVersion.contains("1.20.0")) {
            return SpigotApiVersion.V1_21_0;
        } else {
            ChatUtil.printConsoleError("Failed to resolve valid API version for compatibility: "+bukkitVersion);
            ChatUtil.showStackTrace();
            return SpigotApiVersion.INVALID;
        }
    }

    private static PotionType lookupPotionType(PotionType type, boolean isExtended, boolean isUpgraded) {
        switch (type) {
            case FIRE_RESISTANCE:
                if (isExtended) return PotionType.LONG_FIRE_RESISTANCE;
                break;
            case INVISIBILITY:
                if (isExtended) return PotionType.LONG_INVISIBILITY;
                break;
            case LEAPING:
                if (isExtended) return PotionType.LONG_LEAPING;
                if (isUpgraded) return PotionType.STRONG_LEAPING;
                break;
            case NIGHT_VISION:
                if (isExtended) return PotionType.LONG_NIGHT_VISION;
                break;
            case POISON:
                if (isExtended) return PotionType.LONG_POISON;
                if (isUpgraded) return PotionType.STRONG_POISON;
                break;
            case REGENERATION:
                if (isExtended) return PotionType.LONG_REGENERATION;
                if (isUpgraded) return PotionType.STRONG_REGENERATION;
                break;
            case SLOW_FALLING:
                if (isExtended) return PotionType.LONG_SLOW_FALLING;
                break;
            case SLOWNESS:
                if (isExtended) return PotionType.LONG_SLOWNESS;
                if (isUpgraded) return PotionType.STRONG_SLOWNESS;
                break;
            case STRENGTH:
                if (isExtended) return PotionType.LONG_STRENGTH;
                if (isUpgraded) return PotionType.STRONG_STRENGTH;
                break;
            case SWIFTNESS:
                if (isExtended) return PotionType.LONG_SWIFTNESS;
                if (isUpgraded) return PotionType.STRONG_SWIFTNESS;
                break;
            case TURTLE_MASTER:
                if (isExtended) return PotionType.LONG_TURTLE_MASTER;
                if (isUpgraded) return PotionType.STRONG_TURTLE_MASTER;
                break;
            case WATER_BREATHING:
                if (isExtended) return PotionType.LONG_WATER_BREATHING;
                break;
            case WEAKNESS:
                if (isExtended) return PotionType.LONG_WEAKNESS;
                break;
            case HARMING:
                if (isUpgraded) return PotionType.STRONG_HARMING;
                break;
            case HEALING:
                if (isUpgraded) return PotionType.STRONG_HEALING;
                break;
            default:
                break;
        }
        return PotionType.AWKWARD;
    }

    /* Class Field Name Requests */

    public static PotionEffectType getMiningFatigue() {
        PotionEffectType result;
        switch(getApiVersion()) {
            case V1_16_5:
            case V1_17_1:
            case V1_18_1:
            case V1_18_2:
            case V1_19_4:
            case V1_20_4:
                // Older versions
                result = Registry.EFFECT.get(NamespacedKey.minecraft("slow_digging"));
                break;
            default:
                // Latest versions
                result = Registry.EFFECT.get(NamespacedKey.minecraft("mining_fatigue"));
                break;
        }
        if (result == null) {
            ChatUtil.printConsoleError("Failed to get Mining Fatigue PotionEffectType for Bukkit version "+Bukkit.getVersion());
            ChatUtil.printConsole("Valid NamespacedKeys for Potion Effects are:");
            for (PotionEffectType effect : Registry.EFFECT) {
                ChatUtil.printConsole(effect.getKey().toString());
            }
        }
        return result;
    }

    public static Enchantment getProtectionEnchantment() {
        Enchantment result;
        switch(getApiVersion()) {
            case V1_16_5:
            case V1_17_1:
            case V1_18_1:
            case V1_18_2:
            case V1_19_4:
            case V1_20_4:
                // Older versions
                result = Registry.ENCHANTMENT.get(NamespacedKey.minecraft("protection_environmental"));
                break;
            default:
                // Latest versions
                result = Registry.ENCHANTMENT.get(NamespacedKey.minecraft("protection"));
                break;
        }
        if (result == null) {
            ChatUtil.printConsoleError("Failed to get Protection Enchantment for Bukkit version "+Bukkit.getVersion());
            ChatUtil.printConsole("Valid NamespacedKeys for Enchantments are:");
            for (Enchantment enchant : Registry.ENCHANTMENT) {
                ChatUtil.printConsole(enchant.getKey().toString());
            }
        }
        return result;
    }

    /**
     * Gets the Enchantment field that matches the given name.
     * When the Bukkit version is 1.20.6 or later, this method attempts to map current namespace to older ones.
     * When the Bukkit version is before 1.20.6, this method attempts to map names to the appropriate version namespace.
     * @param name The name of the enchantment, either old or new names
     * @return The Enchantment appropriate for the current version
     */
    public static Enchantment getEnchantment(String name) {
        EnchantComp enchant = EnchantComp.getFromName(name);
        if (enchant == null) {
            // Could not match name to a known enchantment
            ChatUtil.printConsoleError("Failed to find enchantment from name, \""+name+"\", make sure the name matches a known enchantment from the Spigot API.");
            return null;
        }
        String enchantNamespace;
        switch (getApiVersion()) {
            case V1_16_5:
            case V1_17_1:
            case V1_18_1:
            case V1_18_2:
            case V1_19_4:
            case V1_20_4:
                // Older versions
                enchantNamespace = enchant.namespace1;
                if (enchantNamespace.isEmpty()) {
                    ChatUtil.printConsoleError("Failed to match enchantment \""+name+"\" to versions 1.20.4 or earlier. Enchantment does not exist.");
                    return null;
                }
                break;
            default:
                // Latest versions
                enchantNamespace = enchant.namespace2;
                if (enchantNamespace.isEmpty()) {
                    ChatUtil.printConsoleError("Failed to match enchantment \""+name+"\" to versions 1.20.6 or later. Enchantment does not exist.");
                    return null;
                }
                break;
        }
        // Get the enchantment from the namespace registry
        Enchantment result;
        try {
            result = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(enchantNamespace));
        } catch (IllegalArgumentException exception) {
            ChatUtil.printConsoleError("Failed to get Enchantment using minecraft namespace \""+enchantNamespace+"\" for Bukkit version "+Bukkit.getVersion());
            return null;
        }
        return result;
    }

    /* Enum Name Requests */

    /**
     * Gets the specified Particle for the correct version enum name.
     * @param type Which Particle to get, accepted values are:
     *             "dust"
     *             "spell"
     * @return The Particle enum for the correct Bukkit version
     */
    public static Particle getParticle(String type) {
        try {
            switch (getApiVersion()) {
                case V1_16_5:
                case V1_17_1:
                case V1_18_1:
                case V1_18_2:
                case V1_19_4:
                case V1_20_4:
                    // Older versions
                    if (type.equals("dust")) {
                        return Particle.valueOf("REDSTONE");
                    } else if (type.equals("spell")) {
                        return Particle.valueOf("SPELL_MOB_AMBIENT");
                    }
                    break;
                default:
                    // Latest versions
                    if (type.equals("dust")) {
                        return Particle.valueOf("DUST");
                    } else if (type.equals("spell")) {
                        return Particle.valueOf("EFFECT");
                    }
                    break;
            }
        } catch (IllegalArgumentException exception) {
            ChatUtil.printConsoleError("Failed to get Particle for Bukkit version "+Bukkit.getVersion());
        }
        // default return value
        return Particle.FLAME;
    }

    /**
     * Gets the specified EntityType for the correct version enum name.
     * @param type Which EntityType to get, accepted values are:
     *             "item"
     *             "potion"
     * @return The EntityType enum for the correct Bukkit version
     */
    public static EntityType getEntityType(String type) {
        try {
            switch (getApiVersion()) {
                case V1_16_5:
                case V1_17_1:
                case V1_18_1:
                case V1_18_2:
                case V1_19_4:
                case V1_20_4:
                    // Older versions
                    if (type.equals("item")) {
                        return EntityType.valueOf("DROPPED_ITEM");
                    } else if (type.equals("potion")) {
                        return EntityType.valueOf("SPLASH_POTION");
                    }
                    break;
                default:
                    // Latest versions
                    if (type.equals("item")) {
                        return EntityType.valueOf("ITEM");
                    } else if (type.equals("potion")) {
                        return EntityType.valueOf("POTION");
                    }
                    break;
            }
        } catch (IllegalArgumentException exception) {
            ChatUtil.printConsoleError("Failed to get Entity Type for Bukkit version "+Bukkit.getVersion());
        }
        // default return value
        return EntityType.EGG;
    }

    /* Utility Operations */

    @SuppressWarnings({"removal","deprecation"})
    public static PotionMeta setPotionData(PotionMeta meta, PotionType type, boolean isExtended, boolean isUpgraded) {
        switch(getApiVersion()) {
            case V1_16_5:
            case V1_17_1:
            case V1_18_1:
            case V1_18_2:
            case V1_19_4:
            case V1_20_4:
                // Older versions
                try {
                    meta.setBasePotionData(new org.bukkit.potion.PotionData(type, isExtended, isUpgraded));
                } catch(IllegalArgumentException e) {
                    meta.setBasePotionData(new org.bukkit.potion.PotionData(type, false, false));
                    ChatUtil.printConsoleError("Invalid options extended="+isExtended+", upgraded="+isUpgraded+" for potion "+type.name()+" in loot.yml");
                }
                break;
            default:
                // Latest versions
                meta.setBasePotionType(lookupPotionType(type,isExtended,isUpgraded));
                break;
        }
        return meta;
    }



}
