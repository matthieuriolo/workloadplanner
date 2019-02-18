import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;
import java.time.ZoneId;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;
import net.fortuna.ical4j.model.component.CalendarComponent;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.Name;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Version;
import net.fortuna.ical4j.model.property.XProperty;

public class DateCalculator {
	private List<DateRange> reservedRanges;
	List<EventAssignment> eventAssignments;
	
	
	/* parses the file and fetches all relevant events */
	private void fetchEvents(ConfigReader reader) throws Exception {
		/* parse ics file */
		eventAssignments = new ArrayList<EventAssignment>();
		reservedRanges = new ArrayList<DateRange>();
		
		
		for(File path : reader.getPathsToICS()) {
			FileInputStream fin = new FileInputStream(path);
			CalendarBuilder builder = new CalendarBuilder();
			Calendar calendar = builder.build(fin);
			fin.close();
			
			/* fetch all relevant events */
			ComponentList<VEvent> events = calendar.getComponents("VEVENT");
			
			for(VEvent event : events) {
				//ignore unconfirmed events
				if(event.getStatus() == null || !event.getStatus().getValue().equals("CONFIRMED")) {
					continue;
				}
				
				reservedRanges.add(new DateRange(
					event.getStartDate().getDate(),
					event.getEndDate().getDate()
				));
				
				//test if the summary/title matches a regex
				if(event.getSummary() == null || event.getSummary().getValue().length() == 0) {
					continue;
				}
				
				for(Assignment assignment : reader.getAssignments()) {
					if(event.getSummary().getValue().matches(assignment.getRegex())) {
						reservedRanges.add(new DateRange(
							event.getStartDate().getDate(),
							event.getEndDate().getDate(),
							assignment.getTravelHours()
						));
						
						eventAssignments.add(new EventAssignment(event, assignment));
					}
				}
			}
		}
		
		// sort ascending
		eventAssignments.sort(
				(a, b) -> a.getEvent().getStartDate().getDate()
					.compareTo(b.getEvent().getStartDate().getDate())
		);
	}
	
	private VEvent createEvent(DateRange range, String summary) {
		return createEvent(range.getStart(), range.getEnd(), summary);
	}
	
	
	private VEvent createEvent(LocalDateTime start, LocalDateTime end, String summary) {
		VEvent event = new VEvent(
				new DateTime(DateTime.from(start.atZone(ZoneId.systemDefault()).toInstant())),
				new DateTime(DateTime.from(end.atZone(ZoneId.systemDefault()).toInstant())),
				summary
		);
		
		event.getProperties().add(new Uid(UUID.randomUUID().toString()));
		
		TimeZoneRegistry registry = TimeZoneRegistryFactory.getInstance().createRegistry();
		TimeZone timezone = registry.getTimeZone("GMT");
		VTimeZone tz = timezone.getVTimeZone();
		
		event.getProperties().add(tz.getTimeZoneId());
		
		return event;
	}
	
	private void processEvent(List<CalendarComponent> ret, ConfigReader reader, EventAssignment cm, Task type, LocalDateTime from, LocalDateTime to) throws Exception {
		DateFormat formatter = DateFormat.getDateTimeInstance();
		VEvent evt = null;
		int hours = type.getDuration();
		
		//Date date = cm.startDate();
		for(Vacancy vakanz : reader.getVacancies()) {
			if(hours <= 0) {
				break;
			}
			
			LocalDateTime date = from;
			
			while(hours > 0 && date.isBefore(to)) {
				if(!vakanz.sameWeekday(date)) {
					date = increaseDateByDay(date);
					continue;
				}
				
				DateRange possibleRange = vakanz.getRange(date);
				List<DateRange> ranges = possibleRange.substractCollisions(reservedRanges);
				
				if(ranges.isEmpty()) {
					date = increaseDateByDay(date);
					continue;
				}
				
				possibleRange = ranges.get(0);
				
				if(possibleRange.getDuration() > hours) {
					possibleRange.setDuration(hours);
				}
				
				hours -= possibleRange.getDuration();
				
				reservedRanges.add(possibleRange);
				
				evt = createEvent(possibleRange, type.getName() + " for " + formatter.format(cm.getEvent().getStartDate().getDate()));
				ret.add(evt);
			}
		}
		
		
		
		if(hours > 0) {
			//we are not able to find free space for the given worktype - add a note and tell the user that he has not enough time
			System.out.println("Missing vacancy (" + hours + "h) for " + type.getName() + " " + formatter.format(cm.getEvent().getStartDate().getDate()));
			ret.add(type.createMissingVacancy(cm, hours));
		}
	}
	
	public List<CalendarComponent > calculateEvents(ConfigReader reader) throws Exception {
		fetchEvents(reader);
		
		List<CalendarComponent> ret = new ArrayList<CalendarComponent>();
		
		if(eventAssignments.isEmpty()) {
			System.out.println("No assignments are matching any of the given events!");
			return ret;
		}
		
		for(EventAssignment cm : eventAssignments) {
			for(Task type : cm.getModule().getTasksBefore()) {
				DateRange r = cm.beforeRange();
				processEvent(ret, reader, cm, type, r.getStart(), r.getEnd());
			}
			
			for(Task type : cm.getModule().getTasksAfter()) {
				DateRange r = cm.afterRange();
				processEvent(ret, reader, cm, type, r.getStart(), r.getEnd());
			}
		}
		
		return ret;
	}
	
	
	private LocalDateTime increaseDateByDay(LocalDateTime initialDate) {
		initialDate.minusHours(initialDate.getHour());
		initialDate.minusMinutes(initialDate.getMinute());
		initialDate.minusSeconds(initialDate.getSecond());
		initialDate.minusNanos(initialDate.getNano());
		
		return initialDate.plusDays(1);
	}
	
	public boolean calculateAndSave(ConfigReader reader, File location) throws Exception {
		List<CalendarComponent> components = calculateEvents(reader);
		
		if(components.isEmpty()) {
			System.out.println("No events has been generated!");
			return false;
		}
		
		
		//create basic calendar
		Calendar calendar = new Calendar();
		
		calendar.getProperties().add(new ProdId("-//Matthieu Riolo//workloadplanner 1.0//EN"));
		calendar.getProperties().add(Version.VERSION_2_0);
		calendar.getProperties().add(CalScale.GREGORIAN);
		
		//add name
		calendar.getProperties().add(new Name(reader.getName()));
		calendar.getProperties().add(new XProperty("X-WR-CALNAME", reader.getName()));
		
		//add events/notes to calendar
		components.forEach(c -> calendar.getComponents().add(c));
		
		
		//write calendar to file
		FileOutputStream fout = new FileOutputStream(location);

		CalendarOutputter outputter = new CalendarOutputter();
		outputter.output(calendar, fout);
		
		return true;
	}
}
