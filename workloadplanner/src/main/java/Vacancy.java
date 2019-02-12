import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

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
	
	public LocalDateTime getStart(LocalDateTime date) {
		return LocalDateTime.of(date.toLocalDate(), start);
	}
	
	public LocalDateTime getEnd(LocalDateTime date) {
		return LocalDateTime.of(date.toLocalDate(), end);
	}
	
	

	public boolean sameWeekday(LocalDateTime date) {
		return weekday == date.getDayOfWeek().getValue();
	}

	public DateRange getRange(LocalDateTime date) throws Exception {
		return new DateRange(getStart(date), getEnd(date));
	}
}
