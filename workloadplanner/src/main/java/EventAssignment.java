import java.util.Date;

import net.fortuna.ical4j.model.component.VEvent;

/**
 * Class which associates an event with an assignment (tasks)
 * 
 * @author Matthieu Riolo
 *
 */
public class EventAssignment {
	private VEvent event;
	private Assignment assignment;
	
	/**
	 * Constructor for EventAssignment
	 * @param the VEvent which matches the regex from the assignment
	 * @param the Assignment which matches the event
	 */
	public EventAssignment(VEvent event, Assignment assignment) {
		this.event = event;
		this.assignment = assignment;
		
		assignment.addEvent(event);
	}
	
	/**
	 * Returns the previous event which are stored in Assignment relative to the assigned event of this class
	 * @returnthe previous event of assignments
	 * @throws Exception if the event cannot be found
	 */
	public VEvent previousEvent() {
		int pos = assignment.getEvents().indexOf(event);
		
		if(pos < 0) {
			throw new RuntimeException("Internal error: could not find event");
		}
		
		
		if(pos > 0) {
			return assignment.getEvents().get(pos-1);
		}
		
		return null;
	}
	
	/**
	 * Returns the next event which are stored in Assignment relative to the assigned event of this class
	 * @returnthe next event of assignments
	 * @throws Exception if the event cannot be found
	 */
	public VEvent nextEvent() {
		int pos = assignment.getEvents().indexOf(event);
		
		if(pos < 0) {
			throw new RuntimeException("Internal error: could not find event");
		}
		
		if(pos + 1 < assignment.getEvents().size()) {
			return assignment.getEvents().get(pos+1);
		}
		
		return null;
	}
	
	/**
	 * Getter for the property event
	 * @return the assigned event
	 */
	public VEvent getEvent() {
		return event;
	}
	
	/**
	 * Getter for the property assignment
	 * @return the Assignment
	 */
	public Assignment getAssignment() {
		return assignment;
	}
	
	/**
	 * DateRange between ending date of previous event and starting of the assigned event
	 * @return DateRange between previous event and assigned event 
	 * @throws Exception
	 */
	public DateRange beforeRange() {
		Date start;
		if(previousEvent() == null) {
			//init lastDateTime as first found event - 2 weeks
			start = new Date(getEvent().getStartDate().getDate().getTime() - (60 * 60 * 24 * 7 * 2 * 1000));
		}else {
			start = previousEvent().getEndDate().getDate();
		}
		
		return new DateRange(start, getEvent().getStartDate().getDate());
	}
	
	/**
	 * DateRange between ending of the assigned event and start date of next event
	 * @return DateRange between assigned event and next event
	 * @throws Exception
	 */
	public DateRange afterRange() {
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
