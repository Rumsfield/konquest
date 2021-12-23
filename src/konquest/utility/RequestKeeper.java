package konquest.utility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

public class RequestKeeper {

	private HashMap<UUID,Boolean> joinRequests; // Player UUID, invite direction (true = requesting join to resident (invite), false = requesting add from lord/knight)
	
	public RequestKeeper() {
		this.joinRequests = new HashMap<UUID,Boolean>();
	}
	
	public void clearRequests() {
		joinRequests.clear();
	}
	
	// Players who have tried joining but need to be added
	public List<OfflinePlayer> getJoinRequests() {
		ArrayList<OfflinePlayer> result = new ArrayList<OfflinePlayer>();
		for(UUID id : joinRequests.keySet()) {
			if(joinRequests.get(id) == false) {
				result.add(Bukkit.getOfflinePlayer(id));
			}
		}
		return result;
	}
	
	// Players who have been added but need to accept the invite to join
	public List<OfflinePlayer> getJoinInvites() {
		ArrayList<OfflinePlayer> result = new ArrayList<OfflinePlayer>();
		for(UUID id : joinRequests.keySet()) {
			if(joinRequests.get(id) == true) {
				result.add(Bukkit.getOfflinePlayer(id));
			}
		}
		return result;
	}
	
	public boolean addJoinRequest(UUID id, Boolean type) {
		boolean result = false;
		if(!joinRequests.containsKey(id)) {
			joinRequests.put(id, type);
			result = true;
		}
		return result;
	}
	
	// Does the player have an existing request to be added?
	public boolean isJoinRequestValid(UUID id) {
		boolean result = false;
		if(joinRequests.containsKey(id)) {
			result = (joinRequests.get(id) == false);
		}
		return result;
	}
	
	// Does the player have an existing invite to join?
	public boolean isJoinInviteValid(UUID id) {
		boolean result = false;
		if(joinRequests.containsKey(id)) {
			result = (joinRequests.get(id) == true);
		}
		return result;
	}
	
	public void removeJoinRequest(UUID id) {
		joinRequests.remove(id);
	}
}
