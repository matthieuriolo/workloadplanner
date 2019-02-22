import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import java.text.DateFormat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

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


/**
 * Generates and ICS schedule based on the vacancies and tasks given by the ConfigReader
 * 
 * @author Matthieu Riolo
 *
 */
public class DateCalculator {
	private List<DateRange> reservedRanges;
	List<EventAssignment> eventAssignments;
	
	/**
	 * Parses the ICS files and fetches all relevant events
	 * @param retrieves from the reader all location of the ICS files
	 * @throws Exception
	 */
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
	
	/**
	 * Creates a new event based on a Task, EventAssignment and a DateRange
	 * @param the Task which should occur
	 * @param the assocation of Event and the corresponding Assignment
	 * @param the DateRange for the task to occur
	 * @param if the task gets splitted into multiple chunks/subtask the pageIdx will tell you which chunk gets used
	 * @param total amount of chunks
	 * @return a new VEvent for the occuring Task
	 */
	private VEvent createEvent(Task type, EventAssignment cm, DateRange range, int pageIdx, int pageTotal) {
		DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
		DateTimeFormatter dtformatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);
		
		String summary = type.getName();
		Map<String, String> arguments = new HashMap<>();
		
		arguments.put("page.index", String.valueOf(pageIdx));
		arguments.put("page.total", String.valueOf(pageTotal));
		
		arguments.put("event.name", cm.getEvent().getName());
		arguments.put("event.start", formatter.format(cm.getEvent().getStartDate().getDate()));
		arguments.put("event.end", formatter.format(cm.getEvent().getEndDate().getDate()));
		arguments.put("event.duration", String.valueOf(range.getDuration()));
		
		arguments.put("from", range.getStart().format(dtformatter));
		arguments.put("to", range.getEnd().format(dtformatter));
		arguments.put("duration", String.valueOf(type.getDuration()));
		
		for(String key : arguments.keySet()) {
			String value = arguments.get(key);
			summary = summary.replace("{" + key + "}", value);
		}
		
		VEvent event = new VEvent(
				new DateTime(DateTime.from(range.getStart().atZone(ZoneId.systemDefault()).toInstant())),
				new DateTime(DateTime.from(range.getEnd().atZone(ZoneId.systemDefault()).toInstant())),
				summary
		);
		
		event.getProperties().add(new Uid(UUID.randomUUID().toString()));
		
		TimeZoneRegistry registry = TimeZoneRegistryFactory.getInstance().createRegistry();
		TimeZone timezone = registry.getTimeZone("GMT");
		VTimeZone tz = timezone.getVTimeZone();
		
		event.getProperties().add(tz.getTimeZoneId());
		
		return event;
	}
	
	/**
	 * Finds an overlap between a Task and the vacancy
	 * @param objects holding the found overlap
	 * @param the available Vacancy
	 * @param the EventAssignment which contains the task 
	 * @param the Task which a overlapped is searched for
	 * @param starting DateTime for the range to search an overlap
	 * @param end DateTime for the range to search an overlap
	 */
	private void processEvent(List<CalendarComponent> ret, List<Vacancy> vacancies, EventAssignment cm, Task type, LocalDateTime from, LocalDateTime to) {
		DateFormat formatter = DateFormat.getDateTimeInstance();
		int hours = type.getDuration();
		
		List<DateRange> pages = new LinkedList<>();
		
		for(Vacancy vakanz : vacancies) {
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
				
				pages.add(possibleRange);
			}
		}
		
		
		int total = pages.size();
		if(hours > 0) {
			//we are not able to find free space for the given worktype - add a note and tell the user that he has not enough time
			System.out.println("Missing vacancy (" + hours + "h) for " + type.getName() + " " + formatter.format(cm.getEvent().getStartDate().getDate()));
			ret.add(createEvent(type, cm, new DateRange(cm.getEvent().getStartDate().getDate(), hours), total, ++total));
		}
		
		int idx = 0;
		for(DateRange range : pages) {
			idx++;
			ret.add(createEvent(type, cm, range, idx, total));
		}
	}
	
	/**
	 * Finds all overlapping between all tasks and vacancies
	 * @param the ConfigReader containing all the Assignments, Task and Events
	 * @return the found overlap of Task and Vacancy
	 * @throws Exception
	 */
	public List<CalendarComponent > calculateEvents(ConfigReader reader) throws Exception {
		fetchEvents(reader);
		
		List<CalendarComponent> ret = new ArrayList<CalendarComponent>();
		
		if(eventAssignments.isEmpty()) {
			System.out.println("No assignments are matching any of the given events!");
			return ret;
		}
		
		for(EventAssignment cm : eventAssignments) {
			for(Task type : cm.getAssignment().getTasksBefore()) {
				DateRange r = cm.beforeRange();
				processEvent(ret, reader.getVacancies(), cm, type, r.getStart(), r.getEnd());
			}
			
			for(Task type : cm.getAssignment().getTasksAfter()) {
				DateRange r = cm.afterRange();
				processEvent(ret, reader.getVacancies(), cm, type, r.getStart(), r.getEnd());
			}
		}
		
		return ret;
	}
	
	/**
	 * Increasing the given date by one day and removes the hours, minutes and seconds portion
	 * @param the date which should be increase by one day
	 * @return date increased by one day
	 */
	private LocalDateTime increaseDateByDay(LocalDateTime initialDate) {
		initialDate.minusHours(initialDate.getHour());
		initialDate.minusMinutes(initialDate.getMinute());
		initialDate.minusSeconds(initialDate.getSecond());
		initialDate.minusNanos(initialDate.getNano());
		
		return initialDate.plusDays(1);
	}
	
	/**
	 * Finds all overlapping Tasks and Vacancies and saves them as an ICS
	 * @param the ConfigReader which contains all the informations
	 * @param the location to store the file for found overlap
	 * @return true the file can be saved
	 * @throws Exception
	 */
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
