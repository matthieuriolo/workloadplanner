import java.io.File;


/*
 * 
 * 
 * https://javabeginners.de/XML/XML-Datei_lesen.php
 * 
 * */

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.jdom.Document; 
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

/**
 * Immutable class which reads in a XML configuration file
 * 
 * @author Matthieu Riolo
 *
 */
public class ConfigReader {
	private File file;
	private boolean processed = false;
	
	
	private String name;
	private List<File> icsLocations = new ArrayList<File>();
	private List<Vacancy> vacancies = new ArrayList<Vacancy>();
	private List<Assignment> assignments = new ArrayList<Assignment>();
	
	/**
	 * Constructor of the XML config reader
	 * @param location of the file as String
	 */
	public ConfigReader(String path) {
		file = new File(path);
	}
	
	/**
	 * Constructor of the XML config reader
	 * @param location of the file as File
	 */
	public ConfigReader(File file) {
		this.file = file;
	}
	
	/**
	 * Parses the XML file
	 * @throws Exception if the file cannot be found
	 */
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
	
	/**
	 * Reads the Vacancies, Tasks and Assignments from the XML
	 * @param the parsed XML file as org.jdom.Document
	 * @throws Exception 
	 */
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
		
		/* fetch the ics files */
		Element scheduleNode = calendarNode.getChild("schedules");
		if(scheduleNode == null) {
			throw new Exception("The node 'schedules' is missing");
		}
		
		@SuppressWarnings("unchecked")
		List<Element> urlNodes = scheduleNode.getChildren("url");
		@SuppressWarnings("unchecked")
		List<Element> fileNodes = scheduleNode.getChildren("file");
		
		if((urlNodes == null || urlNodes.isEmpty()) && (fileNodes == null || fileNodes.isEmpty())) {
			throw new Exception("No nodes ('url' or 'file') for schedules have been found - nothing to do");
		}
		
		if(urlNodes != null) {
			for(Element urlNode : urlNodes) {
				URL url = new URL(urlNode.getTextTrim());
				File temp = File.createTempFile("workloadplanner", ".ics");
				FileUtils.copyURLToFile(url, temp);
				icsLocations.add(temp);
			}
		}
		
		if(fileNodes != null) {
			for(Element fileNode : fileNodes) {
				File file = new File(fileNode.getTextTrim());
				if(!file.exists() || !file.isFile()) {
					throw new Exception("The schedule file '" + fileNode.getTextTrim() + "' does not exist");
				}
				
				icsLocations.add(file);
			}
		}
		
		/* read in vacancies (possible working times) */
		Element vacanciesNode = calendarNode.getChild("vacancies");
		if(vacanciesNode == null) {
			throw new Exception("The node 'vacancies' is missing");
		}
		
		@SuppressWarnings("unchecked")
		List<Element> timeNodes = vacanciesNode.getChildren("time");
		if(timeNodes == null || timeNodes.isEmpty()) {
			throw new Exception("No nodes 'time' have been found - nothing to do");
		}
		
		
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
		
		
		vacancies.sort((a, b) -> a.getPriority() - b.getPriority());
		
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
	
	/**
	 * Getter for all ICS files in the configuration
	 * @return list of all ICS files
	 * @throws Exception if the file cannot be found or parsed
	 */
	public List<File> getPathsToICS() throws Exception {
		process();
		return icsLocations;
	}
	
	/**
	 * Getter for the name for the calendar which will be created to hold the overlap of Tasks and Vacancies
	 * @return name of the calendar
	 * @throws Exception if the file cannot be found or parsed
	 */
	public String getName() throws Exception {
		process();
		return name;
	}
	
	/**
	 * Getter for all defined Vacancies in the configuration file
	 * @return all defined Vacancies
	 * @throws Exception if the file cannot be found or parsed
	 */
	public List<Vacancy> getVacancies() throws Exception {
		process();
		return vacancies;
	}
	
	/**
	 * Getter for all defined Assignments in the configuration file
	 * @return all defined assignments
	 * @throws Exception if the file cannot be found or parsed
	 */
	public List<Assignment> getAssignments() throws Exception {
		process();
		return assignments;
	}
}
