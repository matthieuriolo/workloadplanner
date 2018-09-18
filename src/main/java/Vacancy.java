import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.Uid;

public class Vacancy {
	private int weekday;
	private LocalTime start;
	private LocalTime end;
	private int priority;
	
	public Vacancy(int weekday, String start, String end, int priority) throws Exception {
		
		if(weekday < 1 || weekday > 7) {
			throw new Exception("Weekday must be in range of 1-7");
		}
		
		
		this.weekday = weekday;

		
		
		DateTimeFormatter form = DateTimeFormatter.ofPattern("HH:mm");
		
		this.start = LocalTime.parse(start, form);
		this.end = LocalTime.parse(end, form);
		this.priority = priority;
	}
	
	
	public int getWeekday() {
		return weekday;
	}
	
	public LocalTime getStart() {
		return start;
	}
	
	public LocalTime getEnd() {
		return end;
	}


	public int getPriority() {
		return priority;
	}
	
	public Date getStart(Date date) {
		return createDate(date, start);
	}
	
	public Date getEnd(Date date) {
		return createDate(date, end);
	}
	
	

	public boolean sameWeekday(Date date) {
		//https://stackoverflow.com/questions/5270272/how-to-determine-day-of-week-by-passing-specific-date#5270292
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
		
		if(dayOfWeek != getWeekday()) {
			return false;
		}
		
		return true;
	}
	
	
	private Date createDate(Date date, LocalTime time) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeZone(TimeZone.getDefault());
		cal.setTime(date);
		cal.set(Calendar.HOUR_OF_DAY, time.getHour());
		cal.set(Calendar.MINUTE, time.getMinute());
		cal.set(Calendar.SECOND, 0);
		
		return cal.getTime();
	}


	public DateRange getRange(Date date) throws Exception {
		return new DateRange(getStart(date), getEnd(date));
	}
}
