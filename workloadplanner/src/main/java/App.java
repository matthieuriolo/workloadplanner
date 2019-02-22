import java.io.File;

/**
 * Class containing the main method
 * 
 * @author Matthieu Riolo
 *
 */
public class App {
	final static String VERSION = "1.1";
	final static String APPNAME = "workloader";
	final static String CREATOR = "Matthieu Riolo";
	
	/**
	 * Main method - entry point of the application
	 * 
	 * @param application arguments
	 * @throws Exception when there is a failure in the reader or calculator
	 */
	public static void main(String[] args) throws Exception {
		boolean isVerbose = true;
		
		for(int idx = 0; idx < args.length; idx++) {
			String argument = args[idx];
			
			switch(argument) {
				case "--verbose":
				case "-v":
					isVerbose = true;
					break;
			}
		}
		
		String configName = "config.xml";
		
		
		/* read in the configuration file */
		ConfigReader conf = new ConfigReader(configName);
		conf.process();
		
		if(isVerbose) {
			conf.printVerbose();
		}
		
		/* calculate dates */
		
		DateCalculator calc = new DateCalculator(isVerbose);
		
		File f = new File("out.ics");
		if(calc.calculateAndSave(conf, f)) {
			System.out.println("Events have been calculated and stored in " + f.getAbsolutePath());
		}
	}
}
