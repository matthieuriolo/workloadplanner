import java.util.UUID;

import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.Uid;

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

	public VEvent createMissingVacancy(EventAssignment cm, int hours) {
		//new DateTime(DateTime.from(start.atZone(ZoneId.systemDefault()).toInstant())),
		DateTime start = new DateTime(cm.getEvent().getStartDate().getDate().getTime());
		DateTime end = new DateTime(start.getTime() + (hours * 60 * 60 * 1000));
		VEvent event = new VEvent(
				start,
				end,
				"Missing vacancy: " + this.getName()
		);
		
		event.getProperties().add(new Uid(UUID.randomUUID().toString()));
		
		return event;
	}
}
