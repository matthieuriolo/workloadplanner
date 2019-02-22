/**
 * Immutable class representing a task will should occur before or after a certain event
 * 
 * @author Matthieu Riolo
 *
 */

public class Task {
	private boolean isBefore;
	private int duration;
	private String name;
	
	/**
	 * Constructor for the immutable class Task
	 * 
	 * @param name of the given task
	 * @param isBefore indicating if the task should occur before the event
	 * @param duration duration of the task in hours
	 * @throws Exception if duration is smaller than 1
	 */
	public Task(String name, boolean isBefore, int duration) throws Exception {
		if(duration < 1) {
			throw new Exception("The duration has to be at least 1 hour");
		}

		this.name = name;
		this.duration = duration;
		this.isBefore = isBefore;
	}
	
	/**
	 * Getter for the property name
	 * @return name of the task
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Getter for the property before
	 * @return true if the task should occur before the event 
	 */
	public boolean isBefore() {
		return isBefore;
	}
	
	/**
	 * Getter for the property duration
	 * @return the duration of the task in hours
	 */
	public int getDuration() {
		return duration;
	}
}
