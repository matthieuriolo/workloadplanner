
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.Uid;

public class Worktype {
	private boolean isBefore;
	private int duration;
	private String name;
	
	public Worktype(String name, boolean isBefore, int duration) throws Exception {
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

	public VEvent createMissingVacancy(CalendarModule cm, int hours) {
		Date start = cm.getEvent().getStartDate().getDate();
		Date end = new Date(start.getTime() + (hours * 60 * 60 * 1000));
		VEvent event = new VEvent(
				new DateTime(start.getTime()),
				new DateTime(end.getTime()),
				"Missing vacancy: " + this.getName()
		);
		
		event.getProperties().add(new Uid(UUID.randomUUID().toString()));
		
		return event;
	}
}
