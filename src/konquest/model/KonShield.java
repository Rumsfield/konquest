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
	
	public int getDuration() {
		return duration;
	}
	
	public int getCost() {
		return cost;
	}
}
