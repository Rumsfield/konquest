package konquest.model;

public class KonShield {

	private String id;
	private int duration;
	private int cost;
	
	public KonShield(String id, int duration, int cost) {
		this.id = id;
		this.duration = duration;
		this.cost = cost;
	}
	
	public String getId() {
		return id;
	}
	
	public int getDurationSeconds() {
		return duration;
	}
	
	public String getDurationFormat() {
		int days = duration / 86400;
		int hours = duration % 86400 / 3600;
		int minutes = duration % 3600 / 60;
		int seconds = duration % 60;
		
		String result = "";
		
		if(days != 0) {
			result = String.format("%03dD:%02dH:%02dM:%02dS", days, hours, minutes, seconds);
		} else if(hours != 0) {
			result = String.format("%02dH:%02dM:%02dS", hours, minutes, seconds);
		} else if(minutes != 0) {
			result = String.format("%02dM:%02dS", minutes, seconds);
		} else {
			result = String.format("%02dS", seconds);
		}
		
		return result;		
	}
	
	public int getCost() {
		return cost;
	}
}
