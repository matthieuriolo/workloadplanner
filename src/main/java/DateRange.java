import java.util.ArrayList;
import java.util.Date;
import java.util.stream.Collectors;

public class DateRange {
	private Date start;
	private Date end;
	
	
	public DateRange(Date from, Date to) throws Exception {
		if(from.after(to)) {
			throw new Exception("Param from must be before to");
		}
		
		start = from;
		end = to;
	}
	
	
	public boolean insideRange(DateRange range) {
		return (
					range.getStart().before(getStart())
					||
					range.getStart().equals(getStart())
				)
				&&
				(
					range.getEnd().after(getEnd())
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
	
	public boolean collision(Date date) {
		return date.after(getStart()) && date.before(getEnd());
	}
	
	public Date getStart() {
		return start;
	}


	public Date getEnd() {
		return end;
	}
	
	
	public int getDuration() {
		return (int) ((end.getTime() - start.getTime()) / (1000 * 60 * 60));
	}
	
	public void setDuration(int hours) throws Exception {
		if(hours < 1) {
			throw new Exception("hours must be more than 0");
		}
		
		end = new Date(start.getTime() + (hours * 1000 * 60 * 60));
	}



	public ArrayList<DateRange> substractCollisions(ArrayList<DateRange> ranges) throws Exception {
		ArrayList<DateRange> looper = new ArrayList<DateRange>();
		looper.add(this);
		boolean hasCollision;
		
		
		do {
			hasCollision = false;
			ArrayList<DateRange> tmp = new ArrayList<DateRange>();
			
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
								
								System.out.println(r.getStart().toGMTString() + " " + r.getEnd().toGMTString());
								System.out.println(range.getStart().toGMTString() + " " + range.getEnd().toGMTString());
								
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
