import java.io.File;


/*
 * 
 * 
 * https://javabeginners.de/XML/XML-Datei_lesen.php
 * 
 * */

import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.jdom.Document; 
import org.jdom.Element;
import org.jdom.input.SAXBuilder;


public class ConfigReader {
	private File file;
	private boolean processed = false;
	
	
	private String name;
	private String temporaryLocation;
	private ArrayList<Vacancy> vacancies;
	private ArrayList<Assignment> assignments;
	
	public ConfigReader(String path) {
		file = new File(path);
		vacancies = new ArrayList<Vacancy>();
		assignments = new ArrayList<Assignment>();
	}
	
	public ConfigReader(File file) {
		this.file = file;
		vacancies = new ArrayList<Vacancy>();
		assignments = new ArrayList<Assignment>();
	}
	
	
	public void process() throws Exception {
		if(processed) {
			return;
		}
		
		//test if file exists
		if(!file.exists()) {
			throw new Exception("The file is missing in " + file.getAbsolutePath());
		}
		
		
		//test if file can be parsed
		SAXBuilder builder = new SAXBuilder(); 
        Document document = builder.build(file); 
        
        processDocument(document);
		
		
		processed = true;
	}
	
	private void processDocument(Document doc) throws Exception {
		Element calendarNode = doc.getRootElement();
		
		if(!calendarNode.getName().equals("calendar")) {
			throw new Exception("Root element must be <calendar>");
		}
		
		/* get the calendar name */
		if(calendarNode.getAttributeValue("name") == null ) {
			throw new Exception("The node 'calendar' is missing the 'name' attribute");
		}
		
		name = calendarNode.getAttributeValue("name");
		
		/* fetch the ics file */
		
		if(calendarNode.getAttributeValue("url") == null ) {
			throw new Exception("The node 'calendar' is missing the 'url' attribute");
		}
		
		URL url = new URL(calendarNode.getAttributeValue("url"));
		File temp = File.createTempFile("workloadplanner", ".ics");
		FileUtils.copyURLToFile(url, temp);
		temporaryLocation = temp.getAbsolutePath();
		
		
		/* read in vacancies (possible working times) */
		
		Element vacanciesNode = calendarNode.getChild("vacancies");
		if(vacanciesNode == null) {
			throw new Exception("The node 'vacancies' is missing");
		}
		
		@SuppressWarnings("unchecked")
		List<Element> timeNodes = vacanciesNode.getChildren("time");
		
		for(Element timeNode : timeNodes) {
			vacancies.add(new Vacancy(
					Integer.parseInt(timeNode.getAttributeValue("day")),
					timeNode.getAttributeValue("from"),
					timeNode.getAttributeValue("to"),
					timeNode.getAttributeValue("priority") == null 
					? Integer.MAX_VALUE
					: Integer.parseInt(timeNode.getAttributeValue("priority"))
			));
		}
		
		vacancies.sort(new Comparator<Vacancy>() {
			public int compare(Vacancy e1, Vacancy e2) {
				return e1.getPriority() - e2.getPriority();
			}
		});
		
		
		/* read in the assignments we are looking for */
		
		Element assignmentsNode = calendarNode.getChild("assignments");
		if(assignmentsNode == null) {
			throw new Exception("The node 'assignments' is missing");
		}
		
		@SuppressWarnings("unchecked")
		List<Element> assignmentNodes = assignmentsNode.getChildren("assignment");
		
		if(assignmentNodes == null) {
			throw new Exception("No nodes 'assignment' have been found - nothing to do");
		}
		
		for(Element assignmentNode : assignmentNodes) {
			if(assignmentNode.getAttributeValue("pattern") == null) {
				throw new Exception("The attribute 'pattern' is missing");
			}
			
			Assignment assignment = new Assignment(
					assignmentNode.getAttributeValue("pattern"),
					assignmentNode.getAttributeValue("travelhours") == null
					? 0
					: Integer.parseInt(assignmentNode.getAttributeValue("travelhours"))
			);
			
			@SuppressWarnings("unchecked")
			List<Element> taskNodes = assignmentNode.getChildren("task");
			if(taskNodes == null) {
				throw new Exception("No nodes 'task' have been found in the assignement with the pattern '" + assignment.getRegex() + "'- nothing to do");
			}
			
			for(Element taskNode : taskNodes) {
				if(taskNode.getAttributeValue("name") == null) {
					throw new Exception("The attribute 'name' is missing");
				}
				
				if(taskNode.getAttributeValue("type") == null) {
					throw new Exception("The attribute 'type' is missing");
				}
				
				if(taskNode.getAttributeValue("hours") == null) {
					throw new Exception("The attribute 'hours' is missing");
				}
				
				
				assignment.addTask(
						taskNode.getAttributeValue("name"),
						taskNode.getAttributeValue("type").equals("before"),
						Integer.parseInt(taskNode.getAttributeValue("hours"))
				);
			}
			
			assignments.add(assignment);
		}
	}
	
	public String getPathToICS() throws Exception {
		process();
		return temporaryLocation;
	}
	
	public String getName() throws Exception {
		process();
		return name;
	}
	
	public ArrayList<Vacancy> getVacancies() throws Exception {
		process();
		return vacancies;
	}
	
	public ArrayList<Assignment> getAssignments() throws Exception {
		process();
		return assignments;
	}
}
