import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.component.VEvent;

public class Assignment {
	private String regex;
	private List<Task> tasks;
	private List<VEvent> events;
	private int hours;
	
	private boolean eventsSorted = false;
	
	public Assignment(String reg, int hours) throws Exception {
		if(reg.length() == 0) {
			throw new Exception("You must give a regex pattern");
		}
		
		if(hours < 0) {
			throw new Exception("Travel hours cannot be negative");
		}
		
		regex = reg;
		this.hours = hours;
		
		tasks = new ArrayList<Task>();
		events = new ArrayList<VEvent>();
	}
	
	public String getRegex() {
		return regex;
	}

	public int getTravelHours() {
		return hours;
	}
	
	public List<Task> getTasks() {
		return tasks;
	}
	
	public List<Task> getTasksBefore() {
		return new ArrayList<Task>(tasks.stream().filter(w -> w.isBefore()).collect(Collectors.toList()));
	}
	
	public List<Task> getTasksAfter() {
		return new ArrayList<Task>(tasks.stream().filter(w -> !w.isBefore()).collect(Collectors.toList()));
	}
	
	
	public List<VEvent> getEvents() {
		return getEvents(true);
	}
	
	public List<VEvent> getEvents(boolean sorted) {
		if(sorted) {
			if(!eventsSorted) {
				events.sort((a, b) -> a.getStartDate().getDate().compareTo(b.getStartDate().getDate()));
				eventsSorted = true;
			}
		}
		
		return events;
	}
	
	public void addEvent(VEvent event) {
		eventsSorted = false;
		events.add(event);
	}
	
	public void addTask(Task type) {
		tasks.add(type);
	}
	
	public void addTask(String name, boolean isBefore, int duration) throws Exception {
		tasks.add(new Task(name, isBefore, duration));
	}
	
	public int beforeHours() {
		return tasks.stream()
				.filter(w -> w.isBefore())
				.map(w -> w.getDuration())
				.reduce(0, (x,y) -> x+y)
		;
	}
	
	public int afterHours() {
		return tasks.stream()
			.filter(w -> !w.isBefore())
			.map(w -> w.getDuration())
			.reduce(0, (x,y) -> x+y)
		;
	}
}
