package com.github.rumsfield.konquest.utility;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.model.KonPlayer;
import me.lucko.helper.config.typeserializers.HelperTypeSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.boss.BarColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.math.BigInteger;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ChatUtil {

	private static final ChatColor adminBroadcastColor= ChatColor.GOLD;
	private static final ChatColor broadcastColor = ChatColor.LIGHT_PURPLE;
	private static final ChatColor noticeColor = ChatColor.GRAY;
	private static final ChatColor errorColor = ChatColor.RED;
	private static final ChatColor alertColor = ChatColor.GOLD;

	/**
	 * Formats hex color codes, written by user zwrumpy
	 * <a href="https://www.spigotmc.org/threads/hex-color-code-translate.449748/#post-4270781">...</a>
	 * @param message The message to format
	 * @return The formatted message
	 */
	public static String parseHex(String message) {
		Pattern pattern = Pattern.compile("#[a-fA-F0-9]{6}");
        Matcher matcher = pattern.matcher(message);
        while (matcher.find()) {
            String hexCode = message.substring(matcher.start(), matcher.end());
            String replaceSharp = hexCode.replace('#', 'x');
           
            char[] ch = replaceSharp.toCharArray();
            StringBuilder builder = new StringBuilder();
            for (char c : ch) {
                builder.append("&").append(c);
            }
           
            message = message.replace(hexCode, builder.toString());
            matcher = pattern.matcher(message);
        }
        return ChatColor.translateAlternateColorCodes('&', message);
	}
	
	/**
	 * Parse a string color code into usable color
	 * @param input - The name of the ChatColor enum
	 * @return ChatColor enum from name, null if invalid name
	 */
	public static ChatColor parseColorCode(String input) {
		ChatColor result = null;
		try {
			result = ChatColor.valueOf(input);
		} catch (Exception ignored) {}
		return result;
	}
	
	/**
	 * Search base string and replace built-in format tags
	 * @param base Base format that may or may not contain %PREFIX%, %SUFFIX%, %KINGDOM%, %TITLE% or %NAME%.
	 * @param kingdom Kingdom name to replace %KINGDOM% with
	 * @param title Title to replace %TITLE% with
	 * @param name Name to replace %NAME% with
	 * @return Formatted string
	 */
	public static String parseFormat(String base, String prefix, String suffix, String kingdom, String title, String name, String primaryColor, String secondaryColor, String kingdomWebColor) {
		String message = base;
		// Tags
		if(prefix.equals("")) {
			message = message.replaceAll("%PREFIX%\\s*", "");
		} else {
			message = message.replace("%PREFIX%", prefix);
		}
		if(suffix.equals("")) {
			message = message.replaceAll("%SUFFIX%\\s*", "");
		} else {
			message = message.replace("%SUFFIX%", suffix);
		}
		if(kingdom.equals("")) {
			message = message.replaceAll("%KINGDOM%\\s*", "");
		} else {
			message = message.replace("%KINGDOM%", kingdom);
		}
		if(title.equals("")) {
			message = message.replaceAll("%TITLE%\\s*", "");
		} else {
			message = message.replace("%TITLE%", title);
		}
		if(name.equals("")) {
			message = message.replaceAll("%NAME%\\s*", "");
		} else {
			message = message.replace("%NAME%", name);
		}
		// Colors
		message = message.replace("%C1%", primaryColor);
		message = message.replace("%C2%", secondaryColor);
		message = message.replace("%CW%", kingdomWebColor);

		return message;
	}
	
	public static Color lookupColor(ChatColor reference) {
		Color result = Color.WHITE;
		switch(reference) {
			case BLACK:
				result = Color.fromRGB(0x000000);
				break;
			case DARK_BLUE:
				result = Color.fromRGB(0x0000AA);
				break;
			case DARK_GREEN:
				result = Color.fromRGB(0x00AA00);
				break;
			case DARK_AQUA:
				result = Color.fromRGB(0x00AAAA);
				break;
			case DARK_RED:
				result = Color.fromRGB(0xAA0000);
				break;
			case DARK_PURPLE:
				result = Color.fromRGB(0xAA00AA);
				break;
			case GOLD:
				result = Color.fromRGB(0xFFAA00);
				break;
			case GRAY:
				result = Color.fromRGB(0xAAAAAA);
				break;
			case DARK_GRAY:
				result = Color.fromRGB(0x555555);
				break;
			case BLUE:
				result = Color.fromRGB(0x5555FF);
				break;
			case GREEN:
				result = Color.fromRGB(0x55FF55);
				break;
			case AQUA:
				result = Color.fromRGB(0x55FFFF);
				break;
			case RED:
				result = Color.fromRGB(0xFF5555);
				break;
			case LIGHT_PURPLE:
				result = Color.fromRGB(0xFF55FF);
				break;
			case YELLOW:
				result = Color.fromRGB(0xFFFF55);
				break;
			case WHITE:
				result = Color.fromRGB(0xFFFFFF);
				break;
			default:
				break;
		}
		return result;
	}

	public static Color lookupColor(String reference) {
		return lookupColor(lookupChatColor(reference));
	}
	
	public static BarColor mapBarColor(ChatColor reference) {
		BarColor result = BarColor.WHITE;
		switch(reference) {
			case YELLOW:
			case GOLD:
				result = BarColor.YELLOW;
				break;
			case RED:
			case DARK_RED:
				result = BarColor.RED;
				break;
			case BLUE:
			case DARK_BLUE:
			case DARK_AQUA:
			case AQUA:
				result = BarColor.BLUE;
				break;
			case GREEN:
			case DARK_GREEN:
				result = BarColor.GREEN;
				break;
			case BLACK:
			case DARK_PURPLE:
				result = BarColor.PURPLE;
				break;
			case LIGHT_PURPLE:
				result = BarColor.PINK;
				break;
			default:
				break;
		}
		return result;
	}

	public static BarColor mapBarColor(String reference) {
		return mapBarColor(lookupChatColor(reference));
	}

	/**
	 * Get a ChatColor enum from the string char code.
	 * @param colorStr - The char code as a string.
	 * @return The ChatColor from the string, else ChatColor.RESET if none exist.
	 */
	public static ChatColor lookupChatColor(String colorStr) {
		ChatColor result = ChatColor.getByChar(colorStr.charAt(colorStr.length()-1));
		if(result == null) {
			String hexVal = String.format("%040x", new BigInteger(1, colorStr.getBytes()));
			printDebug("Failed to lookup ChatColor \""+hexVal+"\"");
			return ChatColor.RESET;
		}
		return result;
	}

	/**
	 * Get a RGB color int from a string name. Supported names:
	 * 1. Any ColorRGB enum name (maps to Bukkit.Color fields)
	 * 2. A hex color code with format #RRGGBB
	 * @param colorStr The name of the color, or hex code
	 * @return The RGB color integer value, or -1 if no valid color could be found.
	 */
	public static int lookupColorRGB(String colorStr) {
		// First, check for ColorRGB name
		ColorRGB namedColor = ColorRGB.fromName(colorStr);
		if(namedColor != null) {
			// Found valid color
			return namedColor.getColor().asRGB();
		}
		// Second, check for hex color code
		if(colorStr.matches("#[a-fA-F0-9]{6}")) {
			try {
				return Integer.decode(colorStr);
			} catch(NumberFormatException ignored) {}
		}
		// Couldn't find a supported color
		return -1;
	}

	/**
	 * Get the name of the color, or hex value as a string.
	 * @param hexVal The integer RGB color value
	 * @return The string color name or hex numbers
	 */
	public static String reverseLookupColorRGB(int hexVal) {
		// First, check if the hex matches a ColorRGB enum
		ColorRGB hexColor = ColorRGB.fromColor(Color.fromRGB(hexVal));
		if(hexColor != null) {
			// Found valid color
			return hexColor.getName();
		}
		// Second, format the hex value into a string
		return "#"+Integer.toHexString(hexVal);
	}
	
	public static void printDebug(String message) {
		if(Konquest.getInstance().getCore().getBoolean(CorePath.DEBUG.getPath())) {
        	Bukkit.getServer().getConsoleSender().sendMessage("[Konquest DEBUG] " + message);
        }
	}
	
	public static void printConsole(String message) {
		Bukkit.getServer().getConsoleSender().sendMessage("[Konquest] " + message);
	}
	
	public static void printConsoleAlert(String message) {
		Bukkit.getServer().getConsoleSender().sendMessage(alertColor + "[Konquest] " + message);
	}
	
	public static void printConsoleError(String message) {
		Bukkit.getLogger().severe("[Konquest] " + message);
	}

	public static void printConsoleWarning(String message) {
		Bukkit.getLogger().warning("[Konquest] " + message);
	}

	public static void sendNotice(KonPlayer player, String message) {
		sendNotice(player.getBukkitPlayer(), message);
	}

	public static void sendNotice(CommandSender sender, String message) {
		sendNotice(sender, message, noticeColor);
	}
	
	public static void sendNotice(CommandSender sender, String message, ChatColor color) {
		String notice = Konquest.getChatTag() + color + message;
		sender.sendMessage(notice);
	}
	
	public static void sendMessage(CommandSender sender, String message) {
		sender.sendMessage(message);
	}
	
	public static void sendMessage(CommandSender sender, String message, ChatColor color) {
		String notice = color + message;
		sender.sendMessage(notice);
	}

	public static void sendError(KonPlayer player, String message) {
		sendError(player.getBukkitPlayer(), message);
	}

	public static void sendError(CommandSender sender, String message) {
		String error = Konquest.getChatTag() + errorColor + message;
		sender.sendMessage(error);
	}
	
	public static void sendBroadcast(String message) {
		String notice = Konquest.getChatTag() + broadcastColor + message;
		Bukkit.broadcastMessage(notice);
	}
	
	public static void sendAdminBroadcast(String message) {
		String notice = Konquest.getChatTag() + adminBroadcastColor + message;
		Bukkit.broadcast(notice,"konquest.command.admin.*");
	}

	public static void sendCommaMessage(CommandSender sender, List<String> values) {
		sendCommaMessage(sender, values, noticeColor);
	}

	public static void sendCommaMessage(CommandSender sender, List<String> values, ChatColor color) {
		StringBuilder message = new StringBuilder();
		ListIterator<String> listIter = values.listIterator();
		while(listIter.hasNext()) {
			String currentValue = listIter.next();
			message.append(currentValue);
			if(listIter.hasNext()) {
				message.append(", ");
			}
		}
		String notice = "" + color + message;
		sender.sendMessage(notice);
	}
	
	public static void sendKonTitle(KonPlayer player, String title, String subtitle) {
		if(title.equals("")) {
			title = " ";
		}
		if(!player.isAdminBypassActive() && !player.isPriorityTitleDisplay()) {
			player.getBukkitPlayer().sendTitle(title, subtitle, 10, 40, 10);
		}
	}
	
	public static void sendKonTitle(KonPlayer player, String title, String subtitle, int duration) {
		if(title.equals("")) {
			title = " ";
		}
		if(!player.isAdminBypassActive() && !player.isPriorityTitleDisplay()) {
			player.getBukkitPlayer().sendTitle(title, subtitle, 10, duration, 10);
		}
	}
	
	public static void sendKonPriorityTitle(KonPlayer player, String title, String subtitle, int durationTicks, int fadeInTicks, int fadeOutTicks) {
		if(title.equals("")) {
			title = " ";
		}
		if(!player.isAdminBypassActive() && !player.isPriorityTitleDisplay()) {
			int totalDuration = (durationTicks+fadeInTicks+fadeOutTicks)/20;
			if(totalDuration < 1) {
				totalDuration = 1;
			}
			player.setIsPriorityTitleDisplay(true);
			Timer priorityTitleDisplayTimer = player.getPriorityTitleDisplayTimer();
			priorityTitleDisplayTimer.stopTimer();
			priorityTitleDisplayTimer.setTime(totalDuration);
			priorityTitleDisplayTimer.startTimer();
			player.getBukkitPlayer().sendTitle(title, subtitle, fadeInTicks, durationTicks, fadeOutTicks);
		}
	}

	public static void sendConstantTitle(Player player, String title, String subtitle) {
		if(title.equals("")) {
			title = " ";
		}
		player.sendTitle(title, subtitle, 1, 9999999, 1);
	}
	
	public static void resetTitle(Player player) {
		player.resetTitle();
		player.sendTitle("", "", 1, 1, 1);
	}

	public static void sendKonBlockedProtectionTitle(KonPlayer player) {
		printDebug("Blocked Protection by player "+player.getBukkitPlayer().getName());
		showDebugStackTrace();
		sendKonPriorityTitle(player, "", Konquest.blockedProtectionColor+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
	}

	public static void sendKonBlockedFlagTitle(KonPlayer player) {
		printDebug("Blocked Flag by player "+player.getBukkitPlayer().getName());
		showDebugStackTrace();
		sendKonPriorityTitle(player, "", Konquest.blockedFlagColor+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
	}

	public static void sendKonBlockedShieldTitle(KonPlayer player) {
		printDebug("Blocked Shield by player "+player.getBukkitPlayer().getName());
		showDebugStackTrace();
		sendKonPriorityTitle(player, "", Konquest.blockedShieldColor+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
	}

	private static void showDebugStackTrace() {
		if(Konquest.getInstance().getCore().getBoolean(CorePath.DEBUG.getPath())) {
			// Get stack trace
			for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
				String traceStr = element.toString();
				if (traceStr.contains("Konquest")) {
					Bukkit.getServer().getConsoleSender().sendMessage(element.toString());
				}
			}
		}
	}
	
}
