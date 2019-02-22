import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

/**
 * Immutable class for a vacancy (DateRange when a task should occur)
 * 
 * @author Matthieu Riolo
 *
 */
public class Vacancy {
	private int weekday;
	private LocalTime start;
	private LocalTime end;
	private int priority;
	
	public Vacancy(int weekday, String start, String end, int priority) {
		if(weekday < 1 || weekday > 7) {
			throw new RuntimeException("Weekday must be in range of 1-7");
		}
		
		this.weekday = weekday;
		
		DateTimeFormatter form = DateTimeFormatter.ofPattern("HH:mm");
		
		this.start = LocalTime.parse(start, form);
		this.end = LocalTime.parse(end, form);
		this.priority = priority;
	}
	
	/**
	 * Getter for the property weekday
	 * @return weekday of the vacancy (sunday = 7)
	 */
	public int getWeekday() {
		return weekday;
	}
	
	/**
	 * Getter for the property start
	 * @return time portion of the beginning of the vacancy 
	 */
	public LocalTime getStart() {
		return start;
	}
	
	/**
	 * Getter for the property end
	 * @return time portion of the ending of the vacancy 
	 */
	public LocalTime getEnd() {
		return end;
	}

	/**
	 * Getter for the property priority
	 * @return bigger integer for high priority
	 */
	public int getPriority() {
		return priority;
	}
	
	/**
	 * the beginning LocalDateTime representation for a given LocalDateTime 
	 * @return a datetime containing the given date and the beginning portion of this vacancy
	 */
	public LocalDateTime getStart(LocalDateTime date) {
		return LocalDateTime.of(date.toLocalDate(), start);
	}
	
	/**
	 * the ending LocalDateTime representation for a given LocalDateTime 
	 * @return a datetime containing the given date and the ending portion of this vacancy
	 */
	public LocalDateTime getEnd(LocalDateTime date) {
		return LocalDateTime.of(date.toLocalDate(), end);
	}
	
	
	/**
	 * Test if this vacancy has the same weekday as the given date
	 * @param the LocalDateTime to test for the weekday
	 * @return true if the given date and this vacancy have the same weekday
	 */
	public boolean sameWeekday(LocalDateTime date) {
		return weekday == date.getDayOfWeek().getValue();
	}
	
	/**
	 * Creates a DateRange with the start and end time of this vacancy for a given date
	 * @param the date which the start and end time should be applied to
	 * @return a DateRange with start and end time of the vacancy for the given date
	 * @throws Exception
	 */
	public DateRange getRange(LocalDateTime date) {
		return new DateRange(getStart(date), getEnd(date));
	}
	
	/**
	 * Prints the vacancy
	 */
	public void printVerbose() {
		DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM);
		
		System.out.println("Weekday: " + weekday);
		System.out.println("Start: " + start.format(formatter));
		System.out.println("End: " + end.format(formatter));
		System.out.println("Priority: " + priority);
		System.out.println();
	}
}
