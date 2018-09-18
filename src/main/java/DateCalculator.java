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
	private ArrayList<DateRange> usedRanges;
	ArrayList<CalendarModule> modules;
	
	
	/* parses the file and fetches all relevant events */
	private void fetchEvents(ConfigReader reader) throws Exception {
		/* parse ics file */
		FileInputStream fin = new FileInputStream(reader.getPathToICS());
		CalendarBuilder builder = new CalendarBuilder();
		Calendar calendar = builder.build(fin);
		
		/* fetch all relevant events */
		
		ComponentList<VEvent> comps = calendar.getComponents("VEVENT");
		
		modules = new ArrayList<CalendarModule>();
		usedRanges = new ArrayList<DateRange>();
		
		for(VEvent comp : comps) {
			//ignore unconfirmed events
			if(comp.getStatus() == null || !comp.getStatus().getValue().equals("CONFIRMED")) {
				continue;
			}
			
			//test if the summary/title matches a regex
			if(comp.getSummary() == null || comp.getSummary().getValue().length() == 0) {
				continue;
			}
			
			for(Module module : reader.getModules()) {
				usedRanges.add(new DateRange(
						new Date(comp.getStartDate().getDate().getTime() - module.getTravelHours() * (1000 * 60 * 60)),
						new Date(comp.getEndDate().getDate().getTime() + module.getTravelHours() * (1000 * 60 * 60))
				));
				
				if(comp.getSummary().getValue().matches(module.getRegex())) {
					modules.add(new CalendarModule(comp, module));
				}
			}
			
			/*
			example:
			  DTSTART 20190105T084500Z
			  DTEND 20190105T120000Z
			  SUMMARY WebG.BSc INF 2018.BE2.HS18/19 - WebG: Web-Grundlagen
			 */
			
		}
		
		// sort ascending
		modules.sort(new Comparator<CalendarModule>() {    
		    public int compare(CalendarModule e1, CalendarModule e2) {
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
	
	private void processEvent(ArrayList<CalendarComponent> ret, ConfigReader reader, CalendarModule cm, Worktype type, Date from, Date to) throws Exception {
		VEvent evt = null;
		int hours = type.getDuration();
		
		//Date date = cm.startDate();
		for(Vacancy vakanz : reader.getWorktimes()) {
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
				ArrayList<DateRange> ranges = possibleRange.substractCollisions(usedRanges);
				
				if(ranges.isEmpty()) {
					date = increaseDateByDay(date);
					continue;
				}
				
				possibleRange = ranges.get(0);
				
				if(possibleRange.getDuration() > hours) {
					possibleRange.setDuration(hours);
				}
				
				hours -= possibleRange.getDuration();
				
				usedRanges.add(possibleRange);
				
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
		
		if(modules.isEmpty()) {
			System.out.println("No modules are matching any of the given events!");
			return ret;
		}
		
		for(CalendarModule cm : modules) {
			for(Worktype type : cm.getModule().getWorktypesBefore()) {
				DateRange r = cm.beforeRange();
				processEvent(ret, reader, cm, type, r.getStart(), r.getEnd());
			}
			
			for(Worktype type : cm.getModule().getWorktypesAfter()) {
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
