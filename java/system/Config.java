package system;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * System's Configuration Class
 * 
 * @author ksemer
 */
public class Config {
	// Algorithms for BFF problem
	public static final int MM = 1, MA = 2, AM = 3, AA = 4, TAM = 5, TMA = 6, AMA = 7, MAM = 8, DCS = 9;

	// O^2 BFF
	public static final int RANDOM = 0, CONT_BEST_K = 1, AT_LEAST_K = 2, AGGR = 3, DENS_IN = 4, DENS_IN_MIN = 5,
			DENS_IN_AVG = 6, SET_IN = 7, SET_IN_MIN = 8, SET_IN_AVG = 9, SET_IN_NEW = 10;

	public static int ITERATIONS;

	public static boolean RUN_REAL;
	public static boolean RUN_SYNTHETIC;
	public static boolean CONNECTIVITY;

	public static boolean RUN_BFF;
	public static boolean RUN_O2_BFF;
	public static boolean RUN_O2_INCREMENTAL;
	public static boolean RUN_O2_ITERATIVE;

	public static boolean RUN_RANDOM;
	public static boolean RUN_ATLEAST_K;
	public static boolean RUN_BESTK_CONT;
	public static boolean RUN_AGGR;

	public static boolean RUN_DENS_IN;
	public static boolean RUN_DENS_IN_MIN;
	public static boolean RUN_DENS_IN_AVG;
	public static boolean RUN_SET_IN;
	public static boolean RUN_SET_IN_MIN;
	public static boolean RUN_SET_IN_AVG;
	public static boolean RUN_SET_IN_NEW;

	public static boolean RUN_DCS;
	public static boolean RUN_FIND_BFF;
	public static boolean RUN_FIND_GREEDY;

	public static Set<Integer> SEED_NODES;

	public static List<String> DATASETS;
	public static List<BitSet> INTERVALS;

	public static String DATA_PATH;
	public static String OUTPUT_PATH;
	private static final Logger _log = Logger.getLogger(Config.class.getName());

	public static void loadConfig() {
		final String SETTINGS_FILE = "./config/settings.properties";

		try {
			Properties Settings = new Properties();
			InputStream is = new FileInputStream(new File(SETTINGS_FILE));
			Settings.load(is);
			is.close();

			// ============================================================
			RUN_REAL = Boolean.parseBoolean(Settings.getProperty("RunReal", "false"));
			RUN_SYNTHETIC = Boolean.parseBoolean(Settings.getProperty("RunSynthetic", "false"));
			CONNECTIVITY = Boolean.parseBoolean(Settings.getProperty("Connectivity", "false"));

			RUN_BFF = Boolean.parseBoolean(Settings.getProperty("RunBFF", "false"));
			RUN_O2_BFF = Boolean.parseBoolean(Settings.getProperty("RunO2BFF", "false"));
			RUN_O2_ITERATIVE = Boolean.parseBoolean(Settings.getProperty("RunO2Iter", "false"));
			RUN_O2_INCREMENTAL = Boolean.parseBoolean(Settings.getProperty("RunO2Incr", "false"));

			RUN_AGGR = Boolean.parseBoolean(Settings.getProperty("RunAggr", "false"));
			RUN_RANDOM = Boolean.parseBoolean(Settings.getProperty("RunRandom", "false"));
			RUN_ATLEAST_K = Boolean.parseBoolean(Settings.getProperty("RunAtLeastK", "false"));
			RUN_BESTK_CONT = Boolean.parseBoolean(Settings.getProperty("RunBestK", "false"));
			
			RUN_SET_IN = Boolean.parseBoolean(Settings.getProperty("RunSetIncr", "false"));
			RUN_SET_IN_MIN = Boolean.parseBoolean(Settings.getProperty("RunSetIncrM", "false"));
			RUN_SET_IN_AVG = Boolean.parseBoolean(Settings.getProperty("RunSetIncrA", "false"));
			RUN_SET_IN_NEW = Boolean.parseBoolean(Settings.getProperty("RunSetIncrN", "false"));

			RUN_DENS_IN = Boolean.parseBoolean(Settings.getProperty("RunDensIncr", "false"));
			RUN_DENS_IN_MIN = Boolean.parseBoolean(Settings.getProperty("RunDensIncrM", "false"));
			RUN_DENS_IN_AVG = Boolean.parseBoolean(Settings.getProperty("RunDensIncrA", "false"));
			
			ITERATIONS = Integer.parseInt(Settings.getProperty("ITERATIONS", "5"));

			RUN_DCS = Boolean.parseBoolean(Settings.getProperty("RunDCS", "false"));
			RUN_FIND_BFF = Boolean.parseBoolean(Settings.getProperty("RunFindBFF", "false"));
			RUN_FIND_GREEDY = Boolean.parseBoolean(Settings.getProperty("RunFindGreedy", "false"));

			String[] seeds = Settings.getProperty("SeedNodes", "").split(",");
			SEED_NODES = new HashSet<Integer>(seeds.length);

			for (String seed : seeds) {
				if (!seed.trim().isEmpty())
					SEED_NODES.add(Integer.parseInt(seed.trim()));
			}

			DATA_PATH = Settings.getProperty("DataPath", "");
			OUTPUT_PATH = Settings.getProperty("OutputPath", "");

			String[] datasets = Settings.getProperty("Datasets", "").split(";");
			DATASETS = new ArrayList<>(datasets.length);

			for (int i = 0; i < datasets.length; i++) {
				datasets[i] = datasets[i].trim();

				if (!datasets[i].isEmpty()) {
					DATASETS.add(datasets[i]);
				}
			}

			if (DATASETS.isEmpty() && RUN_REAL) {
				_log.log(Level.SEVERE, "datasets are empty." + ". Abborted.", new Exception());
				System.exit(0);
			}

			String[] intervals = Settings.getProperty("Intervals", "").split(";");
			INTERVALS = new ArrayList<>(intervals.length);

			for (int i = 0; i < intervals.length; i++) {
				BitSet iQ = new BitSet();
				String[] inter = intervals[i].trim().split(",");

				try {
					iQ.set(Integer.parseInt(inter[0]), Integer.parseInt(inter[1]) + 1);
					INTERVALS.add(iQ);
				} catch (Exception e) {
					_log.log(Level.SEVERE, "intervals are not set correctly." + ". Abborted.", e);
					System.exit(0);
				}
			}

			if (DATASETS.size() > INTERVALS.size()) {
				_log.log(Level.SEVERE, "datasets and intervals do not have the same size." + ". Abborted.",
						new Exception());
				System.exit(0);
			} else if (DATASETS.size() < INTERVALS.size())
				_log.log(Level.WARNING, "datasets size is smaller than intervals size." + ". Ignored.");

			// ============================================================
		} catch (Exception e) {
			_log.log(Level.SEVERE, "Failed to Load " + SETTINGS_FILE + " File.", e);
		}
	}
}