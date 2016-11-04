package system;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import experiments.BFF_Problem;
import experiments.O2BFF_Problem;
import experiments.synthetic.BFF_SynthProblem;
import experiments.synthetic.O2BFF_SynthProblem;

/**
 * Main Class
 * 
 * @author ksemer
 */
public class Main {
	private static final Logger _log = Logger.getLogger(Main.class.getName());

	/**
	 * Main method
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		Config.loadConfig();
		
		ExecutorService executor = Executors.newCachedThreadPool();
		
		Callable<?> bff = () -> {
			if (Config.RUN_BFF) {
				if (Config.RUN_REAL)
					new BFF_Problem();
				
				if (Config.RUN_SYNTHETIC)
					new BFF_SynthProblem();
				
				if (!(Config.RUN_REAL || Config.RUN_SYNTHETIC))
					_log.log(Level.WARNING, "BFF: Not any dataset type was selected");
			}
			return true;
		};

		Callable<?> o2bff = () -> {
			if (Config.RUN_O2_BFF) {
				if (Config.RUN_REAL)
					new O2BFF_Problem();
			
				if (Config.RUN_SYNTHETIC)
					new O2BFF_SynthProblem();
				
				if (!(Config.RUN_REAL || Config.RUN_SYNTHETIC))
					_log.log(Level.WARNING, "O^2BFF: Not any dataset type was selected");			
			}
			return true;
		};
		
		_log.log(Level.INFO, "Submitting BFF Tasks...");

		executor.submit(bff);
		executor.submit(o2bff);				
		executor.shutdown();
	}
}