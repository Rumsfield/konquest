package konquest.nms;

import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

public interface TeamPacketSender {

	public void sendPlayerTeamPacket(Player player, List<String> teamNames, Team team);
	
	/*
	public void setPlayersToFriendlies(Player player, List<String> friendlies, Team friendlyTeam);
	
	public void setPlayersToEnemies(Player player, List<String> enemies, Team enemyTeam);
	
	public void setPlayersToBarbarians(Player player, List<String> barbarians, Team barbarianTeam);
	*/
}
