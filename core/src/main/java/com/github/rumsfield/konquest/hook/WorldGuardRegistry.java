package com.github.rumsfield.konquest.hook;

import com.github.rumsfield.konquest.utility.ChatUtil;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class WorldGuardRegistry {

    public static StateFlag CLAIM;
    public static StateFlag UNCLAIM;
    public static StateFlag TRAVEL_ENTER;
    public static StateFlag TRAVEL_EXIT;

    public static boolean isAvailable = false; // Is the WorldGuard plugin loaded and all flags registered?

    /**
     * Load custom WorldGuard flags into the registry.
     * @return True when flags were successfully registered, else false (like when WorldGuard is not present).
     */
    public static boolean load() {

        Plugin worldGuard = Bukkit.getPluginManager().getPlugin("WorldGuard");

        // WorldGuard must be present but not yet enabled to register flags.
        if(worldGuard == null){
            return false;
        }
        if(worldGuard.isEnabled()){
            ChatUtil.printDebug("Failed to register WorldGuard flags, plugin is already enabled.");
            return false;
        }

        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();

        /* Claim Flag */
        try {
            StateFlag flag = new StateFlag("konquest-claim", true);
            registry.register(flag);
            CLAIM = flag;
        } catch (FlagConflictException e) {
            // some other plugin registered a flag by the same name already.
            ChatUtil.printConsoleError("Konquest failed to register a custom flag with WorldGuard, konquest-claim, because another plugin has already registered it.");
            return false;
        }

        /* Unclaim Flag */
        try {
            StateFlag flag = new StateFlag("konquest-unclaim", true);
            registry.register(flag);
            UNCLAIM = flag;
        } catch (FlagConflictException e) {
            // some other plugin registered a flag by the same name already.
            ChatUtil.printConsoleError("Konquest failed to register a custom flag with WorldGuard, konquest-unclaim, because another plugin has already registered it.");
            return false;
        }

        /* Travel Enter Flag */
        try {
            StateFlag flag = new StateFlag("konquest-travel-enter", true);
            registry.register(flag);
            TRAVEL_ENTER = flag;
        } catch (FlagConflictException e) {
            // some other plugin registered a flag by the same name already.
            ChatUtil.printConsoleError("Konquest failed to register a custom flag with WorldGuard, konquest-travel-enter, because another plugin has already registered it.");
            return false;
        }

        /* Travel Exit Flag */
        try {
            StateFlag flag = new StateFlag("konquest-travel-exit", true);
            registry.register(flag);
            TRAVEL_EXIT = flag;
        } catch (FlagConflictException e) {
            // some other plugin registered a flag by the same name already.
            ChatUtil.printConsoleError("Konquest failed to register a custom flag with WorldGuard, konquest-travel-exit, because another plugin has already registered it.");
            return false;
        }

        isAvailable = true;
        return true;
    }

}
