import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.UUID;

import org.apache.commons.lang.time.DateUtils;

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
	private ArrayList<DateRange> reservedRanges;
	ArrayList<EventAssignment> eventAssignments;
	
	
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
					new Date(event.getStartDate().getDate().getTime()),
					new Date(event.getEndDate().getDate().getTime())
				));
				
				//test if the summary/title matches a regex
				if(event.getSummary() == null || event.getSummary().getValue().length() == 0) {
					continue;
				}
				
				for(Assignment assignment : reader.getAssignments()) {
					if(event.getSummary().getValue().matches(assignment.getRegex())) {
						reservedRanges.add(new DateRange(
							new Date(event.getStartDate().getDate().getTime() - assignment.getTravelHours() * (1000 * 60 * 60)),
							new Date(event.getEndDate().getDate().getTime() + assignment.getTravelHours() * (1000 * 60 * 60))
						));
						
						eventAssignments.add(new EventAssignment(event, assignment));
					}
				}
			}
		}
		
		// sort ascending
		eventAssignments.sort(new Comparator<EventAssignment>() {    
		    public int compare(EventAssignment e1, EventAssignment e2) {
		        Date d1 = e1.getEvent().getStartDate().getDate();
		        Date d2 = e2.getEvent().getStartDate().getDate();
		        return d1.compareTo(d2);
		    }
		});
	}
	
	private VEvent createEvent(DateRange range, String summary) {
		return createEvent(range.getStart(), range.getEnd(), summary);
	}
	
	
	private VEvent createEvent(Date start, Date end, String summary) {
		VEvent event = new VEvent(
				new DateTime(start.getTime()),
				new DateTime(end.getTime()),
				summary
		);
		
		event.getProperties().add(new Uid(UUID.randomUUID().toString()));
		
		TimeZoneRegistry registry = TimeZoneRegistryFactory.getInstance().createRegistry();
		TimeZone timezone = registry.getTimeZone("GMT");
		VTimeZone tz = timezone.getVTimeZone();
		
		event.getProperties().add(tz.getTimeZoneId());
		
		return event;
	}
	
	private void processEvent(ArrayList<CalendarComponent> ret, ConfigReader reader, EventAssignment cm, Task type, Date from, Date to) throws Exception {
		VEvent evt = null;
		int hours = type.getDuration();
		
		//Date date = cm.startDate();
		for(Vacancy vakanz : reader.getVacancies()) {
			if(hours <= 0) {
				break;
			}
			
			Date date = (Date) from.clone();
			
			while(hours > 0 && date.before(to)) {
				if(!vakanz.sameWeekday(date)) {
					date = increaseDateByDay(date);
					continue;
				}
				
				DateRange possibleRange = vakanz.getRange(date);
				ArrayList<DateRange> ranges = possibleRange.substractCollisions(reservedRanges);
				
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
				
				evt = createEvent(possibleRange, type.getName() + " for " + cm.getEvent().getStartDate().getDate().toLocaleString());
				ret.add(evt);
			}
		}
		
		
		
		if(hours > 0) {
			//we are not able to find free space for the given worktype - add a note and tell the user that he has not enough time
			System.out.println("Missing vacancy (" + hours + "h) for " + type.getName() + " " + cm.getEvent().getStartDate().getDate().toLocaleString());
			ret.add(type.createMissingVacancy(cm, hours));
		}
	}
	
	public ArrayList<CalendarComponent > calculateEvents(ConfigReader reader) throws Exception {
		fetchEvents(reader);
		
		ArrayList<CalendarComponent> ret = new ArrayList<CalendarComponent>();
		
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
	
	
	private Date increaseDateByDay(Date initialDate) {
		initialDate = DateUtils.setHours(initialDate, 0);
		initialDate = DateUtils.setMinutes(initialDate, 0);
		initialDate = DateUtils.setSeconds(initialDate, 0);
		
		return DateUtils.addDays(initialDate, 1);
	}
	
	public boolean calculateAndSave(ConfigReader reader, File location) throws Exception {
		ArrayList<CalendarComponent> components = calculateEvents(reader);
		
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
