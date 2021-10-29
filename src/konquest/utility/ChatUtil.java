package konquest.utility;

import konquest.Konquest;
import konquest.model.KonPlayer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;


public class ChatUtil {

	private static ChatColor broadcastColor = ChatColor.LIGHT_PURPLE;
	private static ChatColor noticeColor = ChatColor.GRAY;
	private static ChatColor errorColor = ChatColor.RED;
	private static ChatColor alertColor = ChatColor.GOLD;
	//private static String tag = "§7[§6Konquest§7]§f "; /*Replaced with Konquest.getChatTag()*/
	
	/**
	 * Formats hex color codes, written by user zwrumpy
	 * https://www.spigotmc.org/threads/hex-color-code-translate.449748/#post-4270781
	 * @param message
	 * @return
	 */
	public static String parseHex(String message) {
		Pattern pattern = Pattern.compile("#[a-fA-F0-9]{6}");
        Matcher matcher = pattern.matcher(message);
        while (matcher.find()) {
            String hexCode = message.substring(matcher.start(), matcher.end());
            String replaceSharp = hexCode.replace('#', 'x');
           
            char[] ch = replaceSharp.toCharArray();
            StringBuilder builder = new StringBuilder("");
            for (char c : ch) {
                builder.append("&" + c);
            }
           
            message = message.replace(hexCode, builder.toString());
            matcher = pattern.matcher(message);
        }
        return ChatColor.translateAlternateColorCodes('&', message);
	}
	
	/**
	 * Search base string and replace
	 * 		%PREFIX% with prefix arg
	 * 		%SUFFIX% with suffix arg
	 * 		%KINGDOM% with kingdom arg
	 * 		%TITLE% with title arg
	 * 		%NAME% with name arg
	 * @param base - Base format that may or may not contain %PREFIX%, %SUFFIX%, %KINGDOM%, %TITLE% or %NAME%.
	 * @param kingdom
	 * @param title
	 * @param name
	 * @return
	 */
	public static String parseFormat(String base, String prefix, String suffix, String kingdom, String title, String name, ChatColor teamColor, ChatColor titleColor) {
		String message = base;
		if(prefix.equals("")) {
			message = message.replace("%PREFIX% ", "");
			message = message.replace("%PREFIX%", "");
		} else {
			message = message.replace("%PREFIX%", prefix);
		}
		if(suffix.equals("")) {
			message = message.replace("%SUFFIX% ", "");
			message = message.replace("%SUFFIX%", "");
		} else {
			message = message.replace("%SUFFIX%", suffix);
		}
		if(kingdom.equals("")) {
			message = message.replace("%KINGDOM% ", "");
			message = message.replace("%KINGDOM%", "");
		} else {
			message = message.replace("%KINGDOM%", teamColor+kingdom);
		}
		if(title.equals("")) {
			message = message.replace("%TITLE% ", "");
			message = message.replace("%TITLE%", "");
		} else {
			message = message.replace("%TITLE%", titleColor+title);
		}
		if(name.equals("")) {
			message = message.replace("%NAME% ", "");
			message = message.replace("%NAME%", "");
		} else {
			message = message.replace("%NAME%", teamColor+name);
		}
		return message;
	}
	
	public static void printDebug(String message) {
		if(Konquest.getInstance().getConfigManager().getConfig("core").getBoolean("core.debug")) {
        	Bukkit.getServer().getConsoleSender().sendMessage("[Konquest DEBUG] " + message);
        }
	}
	
	public static void printConsole(String message) {
		Bukkit.getServer().getConsoleSender().sendMessage("[Konquest] " + message);
	}
	
	public static void printConsoleAlert(String message) {
		String alert = alertColor + "[Konquest] " + message;
		Bukkit.getServer().getConsoleSender().sendMessage(alert);
	}
	
	public static void printConsoleError(String message) {
		String error = errorColor + "[Konquest ERROR] " + message;
		Bukkit.getServer().getConsoleSender().sendMessage(error);
	}

	public static void sendNotice(Player player, String message) {
		String notice = Konquest.getChatTag() + noticeColor + message;
		player.sendMessage(notice);
	}
	
	public static void sendNotice(Player player, String message, ChatColor color) {
		String notice = Konquest.getChatTag() + color + message;
		player.sendMessage(notice);
	}
	
	public static void sendMessage(Player player, String message) {
		String notice = message;
		player.sendMessage(notice);
	}
	
	public static void sendMessage(Player player, String message, ChatColor color) {
		String notice = color + message;
		player.sendMessage(notice);
	}

	public static void sendError(Player player, String message) {
		String error = Konquest.getChatTag() + errorColor + message;
		player.sendMessage(error);
	}
	
	public static void sendBroadcast(String message) {
		String notice = Konquest.getChatTag() + broadcastColor + message;
		Bukkit.broadcastMessage(notice);
	}
	
	public static void sendAdminBroadcast(String message) {
		String notice = Konquest.getChatTag() + broadcastColor + message;
		Bukkit.broadcast(notice,"konquest.command.admin");
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
		if(!player.isAdminBypassActive()) {
			int totalDuration = durationTicks/20;
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
	
	/*
	public static void sendTitle(Player player, String title, String subtitle) {
		player.sendTitle(title, subtitle, 10, 40, 10);
	}
	
	public static void sendTitle(Player player, String title, String subtitle, int duration) {
		if(duration < 0) {
			duration = -1;
		}
		player.sendTitle(title, subtitle, 10, duration, 10);
	}
	*/
	
	public static void sendConstantTitle(Player player, String title, String subtitle) {
		if(title.equals("")) {
			title = " ";
		}
		player.sendTitle(title, subtitle, 1, 9999999, 1);
	}
	
	public static void resetTitle(Player player) {
		player.resetTitle();
	}
	
	
}
