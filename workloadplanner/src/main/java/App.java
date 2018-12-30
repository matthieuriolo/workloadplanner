import java.io.File;

public class App {
	public static void main(String[] args) throws Exception {
		String configName = "config.xml";
		
		
		/* read in the configuration file */
		ConfigReader conf = new ConfigReader(configName);
		conf.process();
		
		/* calculate dates */
		
		DateCalculator calc = new DateCalculator();
		
		File f = new File("out.ics");
		if(calc.calculateAndSave(conf, f)) {
			System.out.println("Events have been calculated and stored in " + f.getAbsolutePath());
		}
	}
}
