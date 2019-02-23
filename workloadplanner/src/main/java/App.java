import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

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
		boolean isVerbose = false;
		String configName = "config.xml";
		String storeFile = "out.ics";
		
		Options options = new Options();
		options.addOption(new Option("h", "help", false, "Help message"));
		options.addOption(new Option("V", "version", false, "Prints version number"));
		options.addOption(new Option("v", "verbose", false, "Verbose mode"));
		options.addOption(new Option("c", "configuration", true, "The location of the XML configuration file"));
		options.addOption(new Option("f", "file", true, "The location where to store the ICS file"));
		
		CommandLineParser parser = new GnuParser();
		CommandLine commandLine = parser.parse(options, args);
		
        if(commandLine.hasOption("c")) {
            configName = commandLine.getOptionValue("c");
        }
        
        if(commandLine.hasOption("f")) {
        	storeFile = commandLine.getOptionValue("f");
        }
        
        if(commandLine.hasOption("v")) {
        	isVerbose = true;
        }
        
        if(commandLine.hasOption("V")) {
        	System.out.println(VERSION);
        	System.exit(0);
        }
        
        if(commandLine.hasOption("h")) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp(
				APPNAME,
				"Create ICS with the overlapping of tasks and vacancies",
				options,
				"Created by " + CREATOR + ", version " + VERSION,
				true
			);
			
        	System.exit(0);
        }
        
		
		/* read in the configuration file */
		ConfigReader conf = new ConfigReader(configName);
		conf.process();
		
		if(isVerbose) {
			conf.printVerbose();
		}
		
		/* calculate dates */
		
		DateCalculator calc = new DateCalculator(isVerbose);
		
		File f = new File(storeFile);
		if(calc.calculateAndSave(conf, f)) {
			System.out.println("Events have been calculated and stored in " + f.getAbsolutePath());
		}
	}
}
