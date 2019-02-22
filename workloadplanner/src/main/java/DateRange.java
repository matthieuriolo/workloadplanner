import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Date;

import java.time.temporal.ChronoUnit;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * The DateRange Class represant a timespan defined by two LocalDateTime
 * 
 * @author Matthieu Riolo
 *
 */
public class DateRange {
	private LocalDateTime start;
	private LocalDateTime end;
	
	/**
	 * Constructor with parameter start and end
	 * 
	 * @param from
	 * @param to
	 * @throws Exception if `from` is equal or after `to`
	 */
	public DateRange(LocalDateTime from, LocalDateTime to) throws Exception {
		initRange(from, to);
	}
	
	/**
	 * Construct with parameter start and end but for the deprecated type java.util.Date
	 * 
	 * @param from 
	 * @param to
	 * @throws Exception if `from` is equal or after `to`
	 */
	public DateRange(Date from, Date to) throws Exception {
		LocalDateTime f = convertDateToLocaleDateTime(from);
		LocalDateTime t = convertDateToLocaleDateTime(to);
		initRange(f, t);
	}
	
	/**
	 * Constructor with a starting date and a duration in hours
	 * 
	 * @param from
	 * @param duration in hours
	 * @throws Exception if `from` is equal or after `to`
	 */
	public DateRange(Date from, long duration) throws Exception {
		LocalDateTime f = convertDateToLocaleDateTime(from);
		LocalDateTime t = f.plusHours(duration);
		initRange(f, t);
	}
	
	/**
	 * Constructor with starting, end date and a tolerance
	 * @param from
	 * @param to
	 * @param tolerance in hours. Will be subtracted from `start` and added to `end`
	 * @throws Exception if `from` is equal or after `to`
	 */
	public DateRange(Date from, Date to, long tolerance) throws Exception {
		LocalDateTime f = convertDateToLocaleDateTime(from);
		LocalDateTime t = convertDateToLocaleDateTime(to);
		
		f = f.minusHours(tolerance);
		t = t.plusHours(tolerance);
		
		initRange(f, t);
	}
	
	/**
	 * Converts java.util.Date to java.time.LocalDateTime
	 * 
	 * @param date
	 * @return the converted date
	 */
	private LocalDateTime convertDateToLocaleDateTime(Date date) {
		return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
	}
	
	private void initRange(LocalDateTime from, LocalDateTime to) throws Exception {
		if(from.isAfter(to)) {
			throw new Exception("Param from must be before to");
		}
		
		start = from;
		end = to;
	}
	
	/**
	 * Tests if the given range is inside the borders of this range (inclusive)
	 * 
	 * @param range
	 * @return true if the given range is inside this range
	 */
	public boolean insideRange(DateRange range) {
		return (
					range.getStart().isBefore(getStart())
					||
					range.getStart().equals(getStart())
				)
				&&
				(
					range.getEnd().isAfter(getEnd())
					||
					range.getEnd().equals(getEnd())
				);
	}
	
	/**
	 * Tests if the given range overlaps with this range
	 * 
	 * @param range
	 * @return true if there is an overlap
	 */
	public boolean collision(DateRange range) {
		return (collision(range.getStart()) || collision(range.getEnd()))
				||
				(range.collision(getStart()) || range.collision(getEnd()))
				||
				(range.getStart().equals(getStart()) && range.getEnd().equals(getEnd()))
		;
	}
	
	/**
	 * Tests if the given date is between start and end (both exclusive)
	 * 
	 * @param date
	 * @return true if the given date is between start and end
	 */
	public boolean collision(LocalDateTime date) {
		return date.isAfter(getStart()) && date.isBefore(getEnd());
	}
	
	/**
	 * Getter for start property
	 * 
	 * @return initialized start LocalDateTime
	 */
	public LocalDateTime getStart() {
		return start;
	}

	/**
	 * Getter for end property
	 * 
	 * @return initialized end LocalDateTime
	 */
	public LocalDateTime getEnd() {
		return end;
	}
	
	/**
	 * Difference between start and end in hours
	 * 
	 * @return timespan between start and end as number representing hours
	 */
	public long getDuration() {
		return start.until(end, ChronoUnit.HOURS);
	}
	
	/**
	 * Set the timespan between start and end. Only the property end will change
	 * @param hours
	 * @throws Exception
	 */
	public void setDuration(long hours) throws Exception {
		if(hours < 1) {
			throw new Exception("hours must be more than 0");
		}
		
		end = start.plus(hours, ChronoUnit.HOURS);
	}


	/**
	 * Removes if the given range collides with this range subtract that part
	 * 
	 * @param ranges which will be subtracted
	 * @return list of ranges which fits into the border of this range without colliding with the givesn ranges
	 * @throws Exception if the constructor of DateRange fails or if there is an internal failure
	 */
	public List<DateRange> substractCollisions(List<DateRange> ranges) throws Exception {
		List<DateRange> looper = new ArrayList<DateRange>();
		looper.add(this);
		boolean hasCollision;
		
		
		do {
			hasCollision = false;
			List<DateRange> tmp = new ArrayList<DateRange>();
			
			for(DateRange r : looper) {
				boolean coll = false;
				
				for(DateRange range : ranges) {
					if(range.collision(r)) {
						hasCollision = coll = true;
						
						if(r.insideRange(range)) {
							//remove it => do not add it to our new list
						}else if(range.insideRange(r)) {
							//split it
							tmp.add(new DateRange(r.getStart(), range.getStart()));
							tmp.add(new DateRange(range.getEnd(), r.getEnd()));
						}else {
							//substract it
							if(r.collision(range.getEnd())) {
								tmp.add(new DateRange(range.getEnd(), r.getEnd()));
							}else if(r.collision(range.getStart())) {
								tmp.add(new DateRange(r.getStart(), range.getStart()));
							}else {
								
								System.out.println(r.getStart() + " " + r.getEnd());
								System.out.println(range.getStart() + " " + range.getEnd());
								
								throw new Exception("Internal logic error");
							}
						}
						
						break;
					}
				}
				
				if(!coll) {
					tmp.add(r);
				}
			}
			
			looper = tmp;
		}while(hasCollision);
		
		
		//filter away all elements which dont have a duration of 0>
		return new ArrayList<DateRange>(
				looper.stream()
				.filter(range -> range.getDuration() > 0)
				.collect(Collectors.toList())
		);
	}
}
