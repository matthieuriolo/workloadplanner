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
	private ArrayList<Vacancy> workingtimes;
	private ArrayList<Module> modules;
	
	public ConfigReader(String path) {
		file = new File(path);
		workingtimes = new ArrayList<Vacancy>();
		modules = new ArrayList<Module>();
	}
	
	public ConfigReader(File file) {
		this.file = file;
		workingtimes = new ArrayList<Vacancy>();
		modules = new ArrayList<Module>();
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
		Element element = doc.getRootElement();
		
		if(!element.getName().equals("calendar")) {
			throw new Exception("Root element must be <calendar>");
		}
		
		/* get the calendar name */
		if(element.getAttributeValue("name") == null ) {
			throw new Exception("The node 'calendar' is missing the 'name' attribute");
		}
		
		name = element.getAttributeValue("name");
		
		/* fetch the ics file */
		
		if(element.getAttributeValue("url") == null ) {
			throw new Exception("The node 'calendar' is missing the 'url' attribute");
		}
		
		URL url = new URL(element.getAttributeValue("url"));
		File temp = File.createTempFile("workloadplanner", ".ics");
		FileUtils.copyURLToFile(url, temp);
		temporaryLocation = temp.getAbsolutePath();
		
		
		/* read in possible working times */
		
		Element workingDays = element.getChild("vacancies");
		if(workingDays == null) {
			throw new Exception("The node 'vacancies' is missing");
		}
		
		@SuppressWarnings("unchecked")
		List<Element> days = workingDays.getChildren("time");
		
		for(Element day : days) {
			workingtimes.add(new Vacancy(
					Integer.parseInt(day.getAttributeValue("day")),
					day.getAttributeValue("from"),
					day.getAttributeValue("to"),
					day.getAttributeValue("priority") == null 
					? Integer.MAX_VALUE
					: Integer.parseInt(day.getAttributeValue("priority"))
			));
		}
		
		workingtimes.sort(new Comparator<Vacancy>() {
			public int compare(Vacancy e1, Vacancy e2) {
				return e1.getPriority() - e2.getPriority();
			}
		});
		
		
		/* read in the assignments we are looking for */
		
		Element modulesNode = element.getChild("assignments");
		if(modulesNode == null) {
			throw new Exception("The node 'assignments' is missing");
		}
		
		@SuppressWarnings("unchecked")
		List<Element> moduleNodes = modulesNode.getChildren("assignment");
		
		if(moduleNodes == null) {
			throw new Exception("No nodes 'assignment' have been found - nothing to do");
		}
		
		for(Element moduleNode : moduleNodes) {
			if(moduleNode.getAttributeValue("pattern") == null) {
				throw new Exception("The attribute 'pattern' is missing");
			}
			
			Module m = new Module(
					moduleNode.getAttributeValue("pattern"),
					moduleNode.getAttributeValue("travelhours") == null
					? 0
					: Integer.parseInt(moduleNode.getAttributeValue("travelhours"))
			);
			
			@SuppressWarnings("unchecked")
			List<Element> works = moduleNode.getChildren("task");
			if(works == null) {
				throw new Exception("No nodes 'task' have been found in the assignement with the pattern '" + m.getRegex() + "'- nothing to do");
			}
			
			for(Element workNode : works) {
				if(workNode.getAttributeValue("name") == null) {
					throw new Exception("The attribute 'name' is missing");
				}
				
				if(workNode.getAttributeValue("type") == null) {
					throw new Exception("The attribute 'type' is missing");
				}
				
				if(workNode.getAttributeValue("hours") == null) {
					throw new Exception("The attribute 'hours' is missing");
				}
				
				
				m.addWorktype(
						workNode.getAttributeValue("name"),
						workNode.getAttributeValue("type").equals("before"),
						Integer.parseInt(workNode.getAttributeValue("hours"))
				);
			}
			
			modules.add(m);
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
	
	public ArrayList<Vacancy> getWorktimes() throws Exception {
		process();
		return workingtimes;
	}
	
	public ArrayList<Module> getModules() throws Exception {
		process();
		return modules;
	}
}
