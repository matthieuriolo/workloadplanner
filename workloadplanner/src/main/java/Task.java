public class Task {
	private boolean isBefore;
	private int duration;
	private String name;
	
	public Task(String name, boolean isBefore, int duration) throws Exception {
		if(duration < 1) {
			throw new Exception("The duration has to be at least 1 hour");
		}

		this.name = name;
		this.duration = duration;
		this.isBefore = isBefore;
	}
	
	public String getName() {
		return name;
	}

	public boolean isBefore() {
		return isBefore;
	}
	
	public int getDuration() {
		return duration;
	}
}
