package com.github.rumsfield.konquest.utility;

import com.github.rumsfield.konquest.Konquest;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * This class provides static method implementations that work for the full range of supported Spigot API versions.
 */
public class CompatibilityUtil {

    private static final HashMap<Villager.Profession,Material> professionMaterial = new HashMap<>();
    static {
        professionMaterial.put(Villager.Profession.ARMORER,         Material.BLAST_FURNACE);
        professionMaterial.put(Villager.Profession.BUTCHER,         Material.SMOKER);
        professionMaterial.put(Villager.Profession.CARTOGRAPHER,    Material.CARTOGRAPHY_TABLE);
        professionMaterial.put(Villager.Profession.CLERIC,          Material.BREWING_STAND);
        professionMaterial.put(Villager.Profession.FARMER,          Material.COMPOSTER);
        professionMaterial.put(Villager.Profession.FISHERMAN,       Material.BARREL);
        professionMaterial.put(Villager.Profession.FLETCHER,        Material.FLETCHING_TABLE);
        professionMaterial.put(Villager.Profession.LEATHERWORKER,   Material.CAULDRON);
        professionMaterial.put(Villager.Profession.LIBRARIAN,       Material.LECTERN);
        professionMaterial.put(Villager.Profession.MASON,           Material.STONECUTTER);
        professionMaterial.put(Villager.Profession.NITWIT,          Material.PUFFERFISH_BUCKET);
        professionMaterial.put(Villager.Profession.NONE,            Material.GRAVEL);
        professionMaterial.put(Villager.Profession.SHEPHERD,        Material.LOOM);
        professionMaterial.put(Villager.Profession.TOOLSMITH,       Material.SMITHING_TABLE);
        professionMaterial.put(Villager.Profession.WEAPONSMITH,     Material.GRINDSTONE);
    }

    /**
     * Map old enchantment namespaces to new ones
     */
    private enum EnchantComp {
        // Enchantment Field Name   1.16.5 - 1.18.2             1.19.4 - 1.20.4             1.20.6 +

        // Common names to all versions
        BINDING_CURSE               ("binding_curse",           "binding_curse",            "binding_curse"),
        CHANNELING                  ("channeling",              "channeling",               "channeling"),
        DEPTH_STRIDER               ("depth_strider",           "depth_strider",            "depth_strider"),
        FIRE_ASPECT                 ("fire_aspect",             "fire_aspect",              "fire_aspect"),
        FROST_WALKER                ("frost_walker",            "frost_walker",             "frost_walker"),
        IMPALING                    ("impaling",                "impaling",                 "impaling"),
        KNOCKBACK                   ("knockback",               "knockback",                "knockback"),
        LOYALTY                     ("loyalty",                 "loyalty",                  "loyalty"),
        LURE                        ("lure",                    "lure",                     "lure"),
        MENDING                     ("mending",                 "mending",                  "mending"),
        MULTISHOT                   ("multishot",               "multishot",                "multishot"),
        PIERCING                    ("piercing",                "piercing",                 "piercing"),
        QUICK_CHARGE                ("quick_charge",            "quick_charge",             "quick_charge"),
        RIPTIDE                     ("riptide",                 "riptide",                  "riptide"),
        SILK_TOUCH                  ("silk_touch",              "silk_touch",               "silk_touch"),
        SOUL_SPEED                  ("soul_speed",              "soul_speed",               "soul_speed"),
        SWEEPING_EDGE               ("sweeping",                "sweeping",                 "sweeping_edge"),
        THORNS                      ("thorns",                  "thorns",                   "thorns"),
        VANISHING_CURSE             ("vanishing_curse",         "vanishing_curse",          "vanishing_curse"),

        // 1.19.4 and later names
        SWIFT_SNEAK                 ("",                        "swift_sneak",              "swift_sneak"),

        // 1.20.4 and earlier
        WATER_WORKER                ("aqua_affinity",           "aqua_affinity",            "aqua_affinity"),
        DAMAGE_ARTHROPODS           ("bane_of_arthropods",      "bane_of_arthropods",       "bane_of_arthropods"),
        PROTECTION_EXPLOSIONS       ("blast_protection",        "blast_protection",         "blast_protection"),
        DIG_SPEED                   ("efficiency",              "efficiency",               "efficiency"),
        PROTECTION_FALL             ("feather_falling",         "feather_falling",          "feather_falling"),
        PROTECTION_FIRE             ("fire_protection",         "fire_protection",          "fire_protection"),
        ARROW_FIRE                  ("flame",                   "flame",                    "flame"),
        LOOT_BONUS_BLOCKS           ("fortune",                 "fortune",                  "fortune"),
        ARROW_INFINITE              ("infinity",                "infinity",                 "infinity"),
        LOOT_BONUS_MOBS             ("looting",                 "looting",                  "looting"),
        LUCK                        ("luck_of_the_sea",         "luck_of_the_sea",          "luck_of_the_sea"),
        ARROW_DAMAGE                ("power",                   "power",                    "power"),
        PROTECTION_PROJECTILE       ("projectile_protection",   "projectile_protection",    "projectile_protection"),
        PROTECTION_ENVIRONMENTAL    ("protection",              "protection",               "protection"),
        ARROW_KNOCKBACK             ("punch",                   "punch",                    "punch"),
        OXYGEN                      ("respiration",             "respiration",              "respiration"),
        DAMAGE_ALL                  ("sharpness",               "sharpness",                "sharpness"),
        DAMAGE_UNDEAD               ("smite",                   "smite",                    "smite"),
        DURABILITY                  ("unbreaking",              "unbreaking",               "unbreaking"),

        // 1.20.6 and later
        AQUA_AFFINITY               ("aqua_affinity",           "aqua_affinity",            "aqua_affinity"),
        BANE_OF_ARTHROPODS          ("bane_of_arthropods",      "bane_of_arthropods",       "bane_of_arthropods"),
        BLAST_PROTECTION            ("blast_protection",        "blast_protection",         "blast_protection"),
        EFFICIENCY                  ("efficiency",              "efficiency",               "efficiency"),
        FEATHER_FALLING             ("feather_falling",         "feather_falling",          "feather_falling"),
        FIRE_PROTECTION             ("fire_protection",         "fire_protection",          "fire_protection"),
        FLAME                       ("flame",                   "flame",                    "flame"),
        FORTUNE                     ("fortune",                 "fortune",                  "fortune"),
        INFINITY                    ("infinity",                "infinity",                 "infinity"),
        LOOTING                     ("looting",                 "looting",                  "looting"),
        LUCK_OF_THE_SEA             ("luck_of_the_sea",         "luck_of_the_sea",          "luck_of_the_sea"),
        POWER                       ("power",                   "power",                    "power"),
        PROJECTILE_PROTECTION       ("projectile_protection",   "projectile_protection",    "projectile_protection"),
        PROTECTION                  ("protection",              "protection",               "protection"),
        PUNCH                       ("punch",                   "punch",                    "punch"),
        RESPIRATION                 ("respiration",             "respiration",              "respiration"),
        SHARPNESS                   ("sharpness",               "sharpness",                "sharpness"),
        SMITE                       ("smite",                   "smite",                    "smite"),
        UNBREAKING                  ("unbreaking",              "unbreaking",               "unbreaking"),
        BREACH                      ("",                        "",                         "breach"),
        DENSITY                     ("",                        "",                         "density"),
        WIND_BURST                  ("",                        "",                         "wind_burst");

        public final String namespace1;
        public final String namespace2;
        public final String namespace3;

        EnchantComp(String namespace1, String namespace2, String namespace3) {
            this.namespace1 = namespace1; // 1.16.5 - 1.18.2
            this.namespace2 = namespace2; // 1.19.4 - 1.20.4
            this.namespace3 = namespace3; // 1.20.6 +
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

    @SuppressWarnings("deprecation")
    public static boolean runBIT() {
        boolean pass = true;
        int errorCode = 0;
        // Loop over all Spigot API Enchantment fields only
        for (Enchantment enchant : Enchantment.values()) {
            String apiName = enchant.getName(); // This name may be from a data pack
            if (EnchantComp.getFromName(apiName) == null) {
                // Skip enchantments that are not found in the API name list
                continue;
            }
            Enchantment enchantComp = getEnchantment(apiName);
            if (enchantComp == null) {
                pass = false;
                errorCode |= 1;
            }
        }
        // Mining Fatigue
        PotionEffectType type = getMiningFatigue();
        if (type == null) {
            pass = false;
            errorCode |= 2;
        }
        // Particles
        Particle dust = getParticle("dust");
        Particle spell = getParticle("spell");
        if (dust.equals(Particle.FLAME) || spell.equals(Particle.FLAME)) {
            pass = false;
            errorCode |= 4;
        }
        // Entities
        EntityType item = getEntityType("item");
        EntityType potion = getEntityType("potion");
        if (item.equals(EntityType.EGG) || potion.equals(EntityType.EGG)) {
            pass = false;
            errorCode |= 8;
        }
        // Villager Professions
        for (Villager.Profession profession : getProfessions()) {
            Material professionMaterial = getProfessionMaterial(profession);
            if (professionMaterial.equals(Material.EMERALD)) {
                pass = false;
                errorCode |= 16;
            }
        }
        // Result message
        if (pass) {
            ChatUtil.printConsoleAlert("Successfully validated API compatibility");
        } else {
            ChatUtil.printConsoleError("Failed to validate some API compatibilities, report this as a bug to the plugin author! Error "+errorCode);
        }
        return pass;
    }

    public static Version apiVersion = getApiVersion();

    private static Version getApiVersion() {
        String bukkitVersionName = Bukkit.getServer().getBukkitVersion();
        String versionNum;
        try {
            // Version strings should look like this: 1.20.4-R0.1-SNAPSHOT
            // Split the string by dashes "-" and use the first element: 1.20.4
            versionNum = bukkitVersionName.split("-")[0];
        } catch (ArrayIndexOutOfBoundsException e) {
            ChatUtil.printConsoleError("Failed to determine server version: "+bukkitVersionName);
            return null;
        }

        Version bukkitVersion;
        try {
            bukkitVersion = new Version(versionNum);
        } catch (IllegalArgumentException ex) {
            ChatUtil.printConsoleError("Failed to resolve valid API version: "+bukkitVersionName+", "+versionNum);
            ChatUtil.printConsoleError(ex.getMessage());
            return null;
        }

        return bukkitVersion;
    }

    private static PotionType lookupPotionType(PotionType type, boolean isExtended, boolean isUpgraded) {
        // Try to change type to LONG or STRONG variant
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
        // Otherwise, return type as-is
        return type;
    }

    /* Class Field Name Requests */

    @SuppressWarnings("deprecation")
    public static PotionEffectType getMiningFatigue() {
        PotionEffectType result;
        // First, try the latest API approach
        try {
            // Latest versions uses namespace with Registry
            try {
                result = Registry.EFFECT.get(NamespacedKey.minecraft("mining_fatigue"));
            } catch (IllegalArgumentException exception) {
                result = null;
            }
            if (result == null) {
                ChatUtil.printConsoleError("Failed to get Mining Fatigue PotionEffectType using Registry for Bukkit version "+Bukkit.getVersion());
                ChatUtil.printConsole("Valid NamespacedKeys for Potion Effects are:");
                for (PotionEffectType effect : Registry.EFFECT) {
                    ChatUtil.printConsole(effect.getKey().toString());
                }
            }
        } catch (NoSuchFieldError ignored) {
            // Older versions use direct name lookup
            ChatUtil.printDebug("Could not use Registry to get Mining Fatigue effect, trying name lookup...");
            result = PotionEffectType.getByName("SLOW_DIGGING");
            if (result == null) {
                ChatUtil.printConsoleError("Failed to get Mining Fatigue PotionEffectType using getByName for Bukkit version "+Bukkit.getVersion());
                ChatUtil.printConsole("Valid Names for Potion Effects are:");
                for (PotionEffectType effect : PotionEffectType.values()) {
                    ChatUtil.printConsole(effect.getName());
                }
            }
        }
        return result;
    }

    /**
     * Gets the Enchantment field that matches the given name.
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
        if (apiVersion.compareTo(new Version("1.18.2")) <= 0) {
            // Version 1.16.5 - 1.18.2
            enchantNamespace = enchant.namespace1;
            if (enchantNamespace.isEmpty()) {
                ChatUtil.printConsoleError("Failed to match enchantment \""+name+"\" to versions 1.16.5 - 1.18.2. Enchantment does not exist.");
                return null;
            }
        } else if (apiVersion.compareTo(new Version("1.20.4")) <= 0) {
            // Versions 1.19.4 - 1.20.4
            enchantNamespace = enchant.namespace2;
            if (enchantNamespace.isEmpty()) {
                ChatUtil.printConsoleError("Failed to match enchantment \""+name+"\" to versions 1.19.4 - 1.20.4. Enchantment does not exist.");
                return null;
            }
        } else {
            // Latest versions
            enchantNamespace = enchant.namespace3;
            if (enchantNamespace.isEmpty()) {
                ChatUtil.printConsoleError("Failed to match enchantment \""+name+"\" to versions 1.20.6 or later. Enchantment does not exist.");
                return null;
            }
        }
        // Get the enchantment from the namespace registry
        Enchantment result;
        try {
            result = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(enchantNamespace));
        } catch (IllegalArgumentException exception) {
            result = null;
        }
        if (result == null) {
            ChatUtil.printConsoleError("Failed to get Enchantment using minecraft namespace \""+enchantNamespace+"\" for Bukkit version "+Bukkit.getVersion());
            ChatUtil.printConsole("Valid NamespacedKeys for Enchantments are:");
            for (Enchantment enchantment : Registry.ENCHANTMENT) {
                ChatUtil.printConsole(enchantment.getKey().toString());
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
            if (apiVersion.compareTo(new Version("1.20.4")) <= 0) {
                // Older versions
                if (type.equals("dust")) {
                    return Particle.valueOf("REDSTONE");
                } else if (type.equals("spell")) {
                    return Particle.valueOf("SPELL_MOB_AMBIENT");
                }
            } else {
                // Latest versions
                if (type.equals("dust")) {
                    return Particle.valueOf("DUST");
                } else if (type.equals("spell")) {
                    return Particle.valueOf("ENTITY_EFFECT");
                }
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
            if (apiVersion.compareTo(new Version("1.20.4")) <= 0) {
                // Older versions
                if (type.equals("item")) {
                    return EntityType.valueOf("DROPPED_ITEM");
                } else if (type.equals("potion")) {
                    return EntityType.valueOf("SPLASH_POTION");
                }
            } else {
                // Latest versions
                if (type.equals("item")) {
                    return EntityType.valueOf("ITEM");
                } else if (type.equals("potion")) {
                    return EntityType.valueOf("POTION");
                }
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
        // First, try the latest API approach
        try {
            // Latest versions
            meta.setBasePotionType(lookupPotionType(type, isExtended, isUpgraded));
        } catch (NoSuchMethodError | NoSuchFieldError ignored) {
            // Older versions
            try {
                meta.setBasePotionData(new org.bukkit.potion.PotionData(type, isExtended, isUpgraded));
            } catch (IllegalArgumentException e) {
                meta.setBasePotionData(new org.bukkit.potion.PotionData(type, false, false));
                ChatUtil.printConsoleError("Invalid options extended=" + isExtended + ", upgraded=" + isUpgraded + " for potion " + type.name() + " in loot.yml");
            }
        }
        return meta;
    }

    public static void playerSpawnEffect(Player target, Location loc, Color color) {
        if (apiVersion.compareTo(new Version("1.20.4")) <= 0) {
            // Older versions
            double red = color.getRed() / 255D;
            double green = color.getGreen() / 255D;
            double blue = color.getBlue() / 255D;
            target.spawnParticle(CompatibilityUtil.getParticle("spell"), loc, 0, red, green, blue, 1);
        } else {
            // Latest versions
            target.spawnParticle(CompatibilityUtil.getParticle("spell"), loc, 1, 0, 0, 0, color);
        }
    }

    public static ItemStack buildItem(Material mat, String name, List<String> loreList) {
        return buildItem(mat, name, loreList, false, null);
    }

    public static ItemStack buildItem(Material mat, String name, List<String> loreList, boolean hasProtection) {
        return buildItem(mat, name, loreList, hasProtection, null);
    }

    @SuppressWarnings({"deprecation","removal"})
    public static ItemStack buildItem(Material mat, String name, List<String> loreList, boolean hasProtection, OfflinePlayer playerHead) {
        ItemStack item;
        if (playerHead == null && mat != null) {
            item = new ItemStack(mat);
        } else if (playerHead != null) {
            item = Konquest.getInstance().getPlayerHead(playerHead);
        } else {
            item = new ItemStack(Material.DIRT);
        }
        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, new AttributeModifier("foo",0,AttributeModifier.Operation.MULTIPLY_SCALAR_1)); // This is necessary as of 1.20.6
        for(ItemFlag flag : ItemFlag.values()) {
            meta.addItemFlags(flag);
        }
        if (hasProtection) {
            Enchantment protectionEnchant = CompatibilityUtil.getEnchantment("protection_environmental");
            if (protectionEnchant != null) {
                meta.addEnchant(protectionEnchant, 1, true);
            }
        }
        meta.setDisplayName(name);
        meta.setLore(loreList);
        item.setItemMeta(meta);
        return item;
    }

    //

    /**
     * Attempts to apply a health attribute modifier if it is not already applied.
     * In 1.21, the names of deprecated attribute modifiers return as UUIDs, which might be a Spigot bug?
     * Check for names that look like UUIDs and remove those mods.
     * @param player The player to add a new health attribute modifier
     * @param modAmount The amount of health to add
     * @param modName The name of the modifier for version 1.20.6 and older
     * @param modKey The key of the modifier for version 1.21 and newer
     */
    @SuppressWarnings({"deprecation","removal"})
    public static void applyHealthModifier(Player player, double modAmount, String modName, NamespacedKey modKey) {
        // Check for existing modifier
        boolean isModActive = false;
        AttributeInstance playerHealthAtt = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        assert playerHealthAtt != null;
        for (AttributeModifier mod : playerHealthAtt.getModifiers()) {
            ChatUtil.printDebug("Checking health mod: "+mod.getName());
            // UUIDs look like this: 08c78410-53fc-453d-981d-35b16a50eeb4
            // (8)-(4)-(4)-(4)-(12)
            if (mod.getName().matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")) {
                ChatUtil.printDebug("  Found bad mod, removing!");
                playerHealthAtt.removeModifier(mod);
            } else {
                // Check for existing health mod
                if (apiVersion.compareTo(new Version("1.20.6")) <= 0) {
                    // Legacy names in version 1.20.6 and older
                    if (mod.getName().equals(modName)) {
                        isModActive = true;
                        break;
                    }
                } else {
                    // NamespacedKey in version 1.21 and newer
                    if (mod.getKey().equals(modKey)) {
                        isModActive = true;
                        break;
                    }
                }
            }
        }
        // Apply modifier
        double baseHealth = playerHealthAtt.getBaseValue();
        if(!isModActive) {
            AttributeModifier modification;
            if (apiVersion.compareTo(new Version("1.20.6")) <= 0) {
                // Legacy names in version 1.20.6 and older
                modification = new AttributeModifier(modName, modAmount, AttributeModifier.Operation.ADD_NUMBER);
            } else {
                // NamespacedKey in version 1.21 and newer
                modification = new AttributeModifier(modKey, modAmount, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY);
            }
            playerHealthAtt.addModifier(modification);
            ChatUtil.printDebug("Applied max health attribute modifier "+modAmount+" to player "+player.getName()+" with base health "+baseHealth);
        } else {
            ChatUtil.printDebug("Skipped existing max health attribute modifier on player "+player.getName()+" with base health "+baseHealth);
        }
    }

    public static void removeHealthModifier(Player player, String modName, NamespacedKey modKey) {
        // Search for modifier and remove
        AttributeInstance playerHealthAtt = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        assert playerHealthAtt != null;
        for (AttributeModifier mod : playerHealthAtt.getModifiers()) {
            ChatUtil.printDebug("Checking health mod: "+mod.getName());
            // UUIDs look like this: 08c78410-53fc-453d-981d-35b16a50eeb4
            // (8)-(4)-(4)-(4)-(12)
            if (mod.getName().matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")) {
                ChatUtil.printDebug("  Found bad mod, removing!");
                playerHealthAtt.removeModifier(mod);
            } else {
                // Check for existing health mod
                if (apiVersion.compareTo(new Version("1.20.6")) <= 0) {
                    // Legacy names in version 1.20.6 and older
                    if (mod.getName().equals(modName)) {
                        playerHealthAtt.removeModifier(mod);
                        ChatUtil.printDebug("Removed max health attribute modifier by name for player "+player.getName());
                    }
                } else {
                    // NamespacedKey in version 1.21 and newer
                    if (mod.getKey().equals(modKey)) {
                        playerHealthAtt.removeModifier(mod);
                        ChatUtil.printDebug("Removed max health attribute modifier by namespace key for player "+player.getName());
                    }
                }
            }
        }
    }

    /*
     * Villager Professions
     * Before version 1.21, Villager.Profession is an Enum.
     * In version 1.21 and later, it is an interface.
     */

    @SuppressWarnings("deprecation")
    public static ArrayList<Villager.Profession> getProfessions() {
        ArrayList<Villager.Profession> result = new ArrayList<>();
        try {
            for (Villager.Profession profession : Registry.VILLAGER_PROFESSION) {
                result.add(profession);
            }
        } catch (NoSuchFieldError ignored) {
            // When registry doesn't have the VILLAGER_PROFESSION field
            try {
                result.addAll(Arrays.asList(Villager.Profession.values()));
            } catch (IncompatibleClassChangeError dumb) {
                // I give up
            }
        }
        return result;
    }

    /**
     * Look up the name of the profession field.
     * @param profession The villager profession to match to a Material
     * @return A Material that matches the given profession
     */
    public static Material getProfessionMaterial(Villager.Profession profession) {
        if (professionMaterial.containsKey(profession)) {
            return professionMaterial.get(profession);
        }
        // Default
        return Material.EMERALD;
    }

    /* Java Class Reflection */

    /**
     * In API versions 1.20.6 and earlier, InventoryView is a class.
     * In versions 1.21 and later, it is an interface.
     * This method uses reflection to get the top Inventory object from the
     * InventoryView associated with an InventoryEvent, to avoid runtime errors.
     * @param event The generic InventoryEvent with an InventoryView to inspect.
     * @return The top Inventory object from the event's InventoryView.
     */
    public static Inventory getTopInventory(InventoryEvent event) {
        try {
            Object view = event.getView();
            Method getTopInventory = view.getClass().getMethod("getTopInventory");
            getTopInventory.setAccessible(true);
            return (Inventory) getTopInventory.invoke(view);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Before version 1.21, Villager.Profession is an Enum.
     * In version 1.21 and later, it is an interface.
     * Use deprecated methods to look up the profession field by name.
     * @param name The name of the villager profession
     * @return The villager profession that matches a name, else null
     */
    public static Villager.Profession getProfessionFromName(String name) {
        try {
            for (Object profession : getProfessions()) {
                Method getName = profession.getClass().getMethod("name");
                getName.setAccessible(true);
                String professionName = (String) getName.invoke(profession);
                if (professionName.equalsIgnoreCase(name)) {
                    return (Villager.Profession)profession;
                }
            }
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        // Could not match a name
        return null;
    }

    public static String getProfessionName(Villager.Profession profession) {
        try {
            Object prof = profession;
            Method getName = prof.getClass().getMethod("name");
            getName.setAccessible(true);
            return (String) getName.invoke(prof);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isProfessionEqual(Villager.Profession thisProfession, Villager.Profession thatProfession) {
        try {
            Object thisProf = thisProfession;
            Object thatProf = thatProfession;
            Method isEqual = thisProf.getClass().getMethod("equals", Object.class);
            isEqual.setAccessible(true);
            return (Boolean) isEqual.invoke(thisProf, thatProf);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

}
