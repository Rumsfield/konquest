package konquest.api.manager;

import org.bukkit.entity.Player;

import konquest.api.model.KonquestPlayer;

public interface KonquestPlayerManager {

	/**
	 * Gets a Konquest player object from the given Bukkit player.
	 * @param bukkitPlayer - The player to look up
	 * @return The Konquest player
	 */
	public KonquestPlayer getPlayer(Player bukkitPlayer);
	
}
