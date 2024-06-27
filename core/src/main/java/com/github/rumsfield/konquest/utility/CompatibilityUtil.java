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

    enum SpigotApiVersion {
        INVALID,
        V1_16_5,
        V1_17_1,
        V1_18_2,
        V1_19_4,
        V1_20_4,
        V1_20_6,
        V1_21_0
    }

    private static SpigotApiVersion getApiVersion() {
        String bukkitVersion = Bukkit.getVersion();
        if (bukkitVersion.contains("1.16.5")) {
            return SpigotApiVersion.V1_16_5;
        } else if (bukkitVersion.contains("1.17.1")) {
            return SpigotApiVersion.V1_17_1;
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
            case V1_20_6:
            case V1_21_0:
                result = Registry.EFFECT.get(NamespacedKey.minecraft("MINING_FATIGUE"));
                break;
            default:
                result = Registry.EFFECT.get(NamespacedKey.minecraft("SLOW_DIGGING"));
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
            case V1_20_6:
            case V1_21_0:
                result = Registry.ENCHANTMENT.get(NamespacedKey.minecraft("PROTECTION"));
                break;
            default:
                result = Registry.ENCHANTMENT.get(NamespacedKey.minecraft("PROTECTION_ENVIRONMENTAL"));
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
                case V1_20_6:
                case V1_21_0:
                    if (type.equals("dust")) {
                        return Particle.valueOf("DUST");
                    } else if (type.equals("spell")) {
                        return Particle.valueOf("EFFECT");
                    }
                    break;
                default:
                    if (type.equals("dust")) {
                        return Particle.valueOf("REDSTONE");
                    } else if (type.equals("spell")) {
                        return Particle.valueOf("SPELL_MOB_AMBIENT");
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
                case V1_20_6:
                case V1_21_0:
                    if (type.equals("item")) {
                        return EntityType.valueOf("ITEM");
                    } else if (type.equals("potion")) {
                        return EntityType.valueOf("POTION");
                    }
                    break;
                default:
                    if (type.equals("item")) {
                        return EntityType.valueOf("DROPPED_ITEM");
                    } else if (type.equals("potion")) {
                        return EntityType.valueOf("SPLASH_POTION");
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
            case V1_20_6:
            case V1_21_0:
                meta.setBasePotionType(lookupPotionType(type,isExtended,isUpgraded));
                break;
            default:
                try {
                    meta.setBasePotionData(new org.bukkit.potion.PotionData(type, isExtended, isUpgraded));
                } catch(IllegalArgumentException e) {
                    meta.setBasePotionData(new org.bukkit.potion.PotionData(type, false, false));
                    ChatUtil.printConsoleError("Invalid options extended="+isExtended+", upgraded="+isUpgraded+" for potion "+type.name()+" in loot.yml");
                }
                break;
        }
        return meta;
    }



}
