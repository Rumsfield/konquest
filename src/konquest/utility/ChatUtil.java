package konquest.utility;

import konquest.Konquest;
import konquest.model.KonPlayer;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;


public class ChatUtil {

	private static ChatColor noticeColor = ChatColor.GRAY;
	private static ChatColor errorColor = ChatColor.RED;
	private static String tag = "§7[§6Konquest§7]§f ";
	
	public static void formatArgColors(String args) {
		
	}
	
	public static void printDebug(String message) {
		if(Konquest.getInstance().getConfigManager().getConfig("core").getBoolean("core.debug")) {
        	System.out.println("[Konquest DEBUG]: "+message);
        }
	}
	
	public static void printStatus(String message) {
        System.out.println("[Konquest]: " + message);
	}
	
	public static void printConsole(String message) {
		//Bukkit.getLogger().info(message);
		Bukkit.getServer().getConsoleSender().sendMessage(message);
	}
	
	public static void printConsoleError(String message) {
		String error = errorColor + "[Konquest] " + message;
		Bukkit.getServer().getConsoleSender().sendMessage(error);
	}

	public static void sendNotice(Player player, String message) {
		String notice = tag + noticeColor + message;
		player.sendMessage(notice);
	}
	
	public static void sendNotice(Player player, String message, ChatColor color) {
		String notice = tag + color + message;
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
		String error = tag + errorColor + message;
		player.sendMessage(error);
	}
	
	public static void sendBroadcast(String message) {
		String notice = tag + noticeColor + message;
		Bukkit.broadcastMessage(notice);
	}
	
	public static void sendAdminBroadcast(String message) {
		String notice = tag + noticeColor + message;
		Bukkit.broadcast(notice,"konquest.command.admin");
	}
	
	public static void sendKonTitle(KonPlayer player, String title, String subtitle) {
		if(!player.isAdminBypassActive() && !player.isPriorityTitleDisplay()) {
			player.getBukkitPlayer().sendTitle(title, subtitle, 10, 40, 10);
		}
	}
	
	public static void sendKonTitle(KonPlayer player, String title, String subtitle, int duration) {
		if(!player.isAdminBypassActive() && !player.isPriorityTitleDisplay()) {
			player.getBukkitPlayer().sendTitle(title, subtitle, 10, duration, 10);
		}
	}
	
	public static void sendKonPriorityTitle(KonPlayer player, String title, String subtitle, int durationTicks, int fadeInTicks, int fadeOutTicks) {
		//if(!player.isAdminBypassActive() && !player.isPriorityTitleDisplay()) {
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
		player.sendTitle(title, subtitle, 1, 9999999, 1);
	}
	
	public static void resetTitle(Player player) {
		player.resetTitle();
	}
	
	
}
