import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;

import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.component.VEvent;

public class Module {
	private String regex;
	private ArrayList<Worktype> worktypes;
	private ArrayList<VEvent> events;
	private int hours;
	
	private boolean eventsSorted = false;
	
	public Module(String reg, int hours) throws Exception {
		if(reg.length() == 0) {
			throw new Exception("You must give a regex pattern");
		}
		
		if(hours < 0) {
			throw new Exception("Travel hours cannot be negative");
		}
		
		regex = reg;
		this.hours = hours;
		
		worktypes = new ArrayList<Worktype>();
		events = new ArrayList<VEvent>();
	}
	
	public String getRegex() {
		return regex;
	}

	public int getTravelHours() {
		return hours;
	}
	
	public ArrayList<Worktype> getWorktypes() {
		return worktypes;
	}
	
	public ArrayList<Worktype> getWorktypesBefore() {
		return new ArrayList<Worktype>(worktypes.stream().filter(w -> w.isBefore()).collect(Collectors.toList()));
	}
	
	public ArrayList<Worktype> getWorktypesAfter() {
		return new ArrayList<Worktype>(worktypes.stream().filter(w -> !w.isBefore()).collect(Collectors.toList()));
	}
	
	
	public ArrayList<VEvent> getEvents() {
		return getEvents(true);
	}
	
	public ArrayList<VEvent> getEvents(boolean sorted) {
		if(sorted) {
			if(!eventsSorted) {
				events.sort(new Comparator<VEvent>() {    
				    public int compare(VEvent e1, VEvent e2) {
				        Date d1 = e1.getStartDate().getDate();
				        Date d2 = e2.getStartDate().getDate();
				        return d1.compareTo(d2);
				    }
				});
				
				eventsSorted = true;
			}
		}
		
		return events;
	}
	
	public void addEvent(VEvent event) {
		eventsSorted = false;
		events.add(event);
	}
	
	public void addWorktype(Worktype type) {
		worktypes.add(type);
	}
	
	public void addWorktype(String name, boolean isBefore, int duration) throws Exception {
		worktypes.add(new Worktype(name, isBefore, duration));
	}
	
	public int beforeHours() {
		return worktypes.stream()
				.filter(w -> w.isBefore())
				.map(w -> w.getDuration())
				.reduce(0, (x,y) -> x+y)
		;
	}
	
	public int afterHours() {
		return worktypes.stream()
			.filter(w -> !w.isBefore())
			.map(w -> w.getDuration())
			.reduce(0, (x,y) -> x+y)
		;
	}
}
