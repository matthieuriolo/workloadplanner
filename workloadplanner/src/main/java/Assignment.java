import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import net.fortuna.ical4j.model.component.VEvent;

/**
 * Immutable class Assignment (connection between events and tasks)
 * 
 * @author Matthieu Riolo
 *
 */
public class Assignment {
	private String regex;
	private List<Task> tasks;
	private List<VEvent> events;
	private int travelHours;
	
	private boolean eventsSorted = false;
	
	/**
	 * Constructor for assignments
	 * @param regex which must match the event name
	 * @param travelHours is a timespan which will be added before and after the event
	 * @throws an Exception if no regex is passed or a negative travel hours
	 */
	public Assignment(String regex, int travelHours) throws Exception {
		if(regex.length() == 0) {
			throw new Exception("You must give a regex pattern");
		}
		
		if(travelHours < 0) {
			throw new Exception("Travel hours cannot be negative");
		}
		
		this.regex = regex;
		this.travelHours = travelHours;
		
		tasks = new ArrayList<Task>();
		events = new ArrayList<VEvent>();
	}
	
	/**
	 * Getter for the property regex
	 * @return regex passed to the constructor
	 */
	public String getRegex() {
		return regex;
	}
	
	/**
	 * Getter for the property travelHours
	 * @return travelHours passed to the constructor
	 */
	public int getTravelHours() {
		return travelHours;
	}
	
	/**
	 * Returns all tasks assigned to this assignment
	 * @return assigned tasks
	 */
	public List<Task> getTasks() {
		return tasks;
	}
	
	/**
	 * Returns tasks (labeled before) assigned to this assignment
	 * @return assigned tasks which the property isBefore is set
	 */
	public List<Task> getTasksBefore() {
		return new ArrayList<Task>(
				tasks.stream()
				.filter(w -> w.isBefore())
				.collect(Collectors.toList())
		);
	}
	
	/**
	 * Returns tasks (not labeled before) assigned to this assignment
	 * @return assigned tasks which the property isBefore is not set
	 */
	public List<Task> getTasksAfter() {
		return new ArrayList<Task>(
				tasks.stream()
				.filter(w -> !w.isBefore())
				.collect(Collectors.toList())
		);
	}
	
	/**
	 * Returns all events assigned to this assignment
	 * @return events sorted by the property start in ascending order
	 */
	public List<VEvent> getEvents() {
		return getEvents(true);
	}
	
	/**
	 * Returns all events assigned to this assignment
	 * @param if true then the returned events are sorted ascendingly by their property start
	 * @return events assigned to this assignment
	 */
	public List<VEvent> getEvents(boolean sorted) {
		if(sorted) {
			if(!eventsSorted) {
				events.sort((a, b) -> a.getStartDate().getDate().compareTo(b.getStartDate().getDate()));
				eventsSorted = true;
			}
		}
		
		return events;
	}
	
	/**
	 * Assigns an event to an assignment
	 * @param the event to assign to
	 */
	public void addEvent(VEvent event) {
		eventsSorted = false;
		events.add(event);
	}
	
	/**
	 * Assign a task to this assignment
	 * @param the task to assign (should not have been assigned to another assignment before)
	 */
	public void addTask(Task type) {
		tasks.add(type);
	}
	
	/**
	 * Creates a new task and assigns it to this assignment
	 * @param name of the new task
	 * @param isBefore if the task should occur before or after the event
	 * @param duration of the task
	 * @throws Exception is thrown when duration is less than 1
	 */
	public void addTask(String name, boolean isBefore, int duration) throws Exception {
		tasks.add(new Task(name, isBefore, duration));
	}
}
