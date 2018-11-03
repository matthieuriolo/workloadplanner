import java.util.Date;

import net.fortuna.ical4j.model.component.VEvent;

public class EventAssignment {
	private VEvent event;
	private Assignment assignment;
	
	public EventAssignment(VEvent event, Assignment assignment) {
		this.event = event;
		this.assignment = assignment;
		
		assignment.addEvent(event);
	}
	
	public VEvent previousEvent() throws Exception {
		int pos = assignment.getEvents().indexOf(event);
		
		if(pos < 0) {
			throw new Exception("Internal error: could not find event");
		}
		
		
		if(pos > 0) {
			return assignment.getEvents().get(pos-1);
		}
		
		return null;
	}
	
	public VEvent nextEvent() throws Exception {
		int pos = assignment.getEvents().indexOf(event);
		
		if(pos < 0) {
			throw new Exception("Internal error: could not find event");
		}
		
		if(pos + 1 < assignment.getEvents().size()) {
			return assignment.getEvents().get(pos+1);
		}
		
		return null;
	}
	
	public VEvent getEvent() {
		return event;
	}
	
	public Assignment getModule() {
		return assignment;
	}
	
	public DateRange beforeRange() throws Exception {
		Date start;
		if(previousEvent() == null) {
			//init lastDateTime as first found event - 2 weeks
			start = new Date(getEvent().getStartDate().getDate().getTime() - (60 * 60 * 24 * 7 * 2 * 1000));
		}else {
			start = previousEvent().getEndDate().getDate();
		}
		
		
		return new DateRange(start, getEvent().getStartDate().getDate());
	}
	
	public DateRange afterRange() throws Exception {
		Date end;
		if(nextEvent() == null) {
			//init lastDateTime as first found event - 2 weeks
			end = new Date(getEvent().getStartDate().getDate().getTime() + (60 * 60 * 24 * 7 * 2 * 1000));
		}else {
			end = nextEvent().getEndDate().getDate();
		}
		
		
		return new DateRange(getEvent().getEndDate().getDate(), end);
	}
}
