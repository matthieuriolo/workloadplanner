import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Date;

import java.time.temporal.ChronoUnit;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class DateRange {
	private LocalDateTime start;
	private LocalDateTime end;
	
	
	public DateRange(LocalDateTime from, LocalDateTime to) throws Exception {
		initRange(from, to);
	}
	
	public DateRange(Date from, Date to) throws Exception {
		LocalDateTime f = convertDateToLocaleDateTime(from);
		LocalDateTime t = convertDateToLocaleDateTime(to);
		initRange(f, t);
	}
	
	public DateRange(Date from, long duration) throws Exception {
		LocalDateTime f = convertDateToLocaleDateTime(from);
		LocalDateTime t = f.plusHours(duration);
		initRange(f, t);
	}
	
	public DateRange(Date from, Date to, long tolerance) throws Exception {
		LocalDateTime f = convertDateToLocaleDateTime(from);
		LocalDateTime t = convertDateToLocaleDateTime(to);
		
		f = f.minusHours(tolerance);
		t = t.plusHours(tolerance);
		
		initRange(f, t);
	}
	
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
	
	
	public boolean collision(DateRange range) {
		return (collision(range.getStart()) || collision(range.getEnd()))
				||
				(range.collision(getStart()) || range.collision(getEnd()))
				||
				(range.getStart().equals(getStart()) && range.getEnd().equals(getEnd()))
		;
	}
	
	public boolean collision(LocalDateTime date) {
		return date.isAfter(getStart()) && date.isBefore(getEnd());
	}
	
	public LocalDateTime getStart() {
		return start;
	}


	public LocalDateTime getEnd() {
		return end;
	}
	
	
	public long getDuration() {
		return start.until(end, ChronoUnit.HOURS);
	}
	
	public void setDuration(long hours) throws Exception {
		if(hours < 1) {
			throw new Exception("hours must be more than 0");
		}
		
		end = start.plus(hours, ChronoUnit.HOURS);
	}



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
		return new ArrayList<DateRange>(looper.stream().filter(range -> range.getDuration() > 0).collect(Collectors.toList()));
	}
}
