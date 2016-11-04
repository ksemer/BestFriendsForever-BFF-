package experiments.synthetic;

import java.io.FileWriter;
import java.io.IOException;
import java.util.BitSet;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import algorithm.BFF;
import algorithm.BFF_Greedy;
import algorithm.DCS_Greedy;
import experiments.MetricScore;
import system.Config;
import vg.Graph;
import vg.LoadGraph;

/**
 * BFF synthetic data experiments class
 * 
 * @author ksemer
 */
public class BFF_SynthProblem {

	private Runnable r1, r2, r3, r4, r5, r6, r7, r8, r9, r10, r11, r12, r13, r14;

	private static final Logger _log = Logger.getLogger(BFF_SynthProblem.class.getName());

	/**
	 * Constructor
	 * 
	 * @throws IOException
	 */
	public BFF_SynthProblem() throws IOException {
		// initialize runnables
		initializeRunnables();

		ExecutorService executor = Executors.newCachedThreadPool();
		executor.submit(r1);
		executor.submit(r2);
		executor.submit(r3);
		executor.submit(r4);
		executor.submit(r5);
		executor.submit(r6);
		executor.submit(r7);
		executor.submit(r8);
		executor.submit(r9);
		executor.submit(r10);
		executor.submit(r11);
		executor.submit(r12);
		executor.submit(r13);
		executor.submit(r14);

		// with competitor dense graph
		for (int i = 1; i <= 9; i++) {
			for (int j = 5; j <= 9; j++) {
				BitSet iQ = new BitSet();
				iQ.set(0, 10);
				runMetrics("synthetic_pr:0." + j + "_s:" + i, iQ);
			}
		}
	}

	/**
	 * Run each metric in a thread
	 * 
	 * @param dataset
	 * @param iQ
	 */
	private void runMetrics(String dataset, BitSet iQ) {
		if (Config.RUN_FIND_BFF) {
			IntStream.range(1, 5).parallel().forEach(metric -> run(iQ, metric, dataset));
			IntStream.range(7, 9).parallel().forEach(metric -> run(iQ, metric, dataset));
		}

		if (Config.RUN_DCS)
			IntStream.range(9, 10).parallel().forEach(metric -> run(iQ, metric, dataset));

		if (Config.RUN_FIND_GREEDY)
			IntStream.range(5, 7).parallel().forEach(metric -> run(iQ, metric, dataset));
	}

	/**
	 * Run method which calls the algorithm
	 * 
	 * @param iQ
	 * @param metric
	 * @param dataset
	 * @throws IOException
	 */
	private void run(BitSet iQ, int metric, String dataset) {

		String outputPath = Config.OUTPUT_PATH + dataset;
		String datasetPath = Config.DATA_PATH + dataset;

		_log.log(Level.INFO, "(metric, dataset)->(" + metric + ", " + dataset + ")");

		try {
			Graph lvg = LoadGraph.loadDataset(iQ, datasetPath);
			Object algorithm = null;
			Set<Integer> S = null;
			MetricScore st = new MetricScore();
			long time = System.currentTimeMillis();
			FileWriter stats = new FileWriter(outputPath + "_" + metric + ".txt");

			if (metric == Config.DCS)
				algorithm = new DCS_Greedy(lvg, iQ, Collections.emptySet());
			else if (metric == Config.TMA || metric == Config.TAM)
				algorithm = new BFF_Greedy(lvg, iQ, metric, Collections.emptySet());
			else
				algorithm = new BFF(lvg, iQ, metric, Collections.emptySet());

			// retrieve solution set S
			if (algorithm instanceof BFF)
				S = ((BFF) algorithm).getSolutionSet();
			else if (algorithm instanceof BFF_Greedy)
				S = ((BFF_Greedy) algorithm).getSolutionSet();
			else if (algorithm instanceof DCS_Greedy)
				S = ((DCS_Greedy) algorithm).getSolutionSet();

			if (algorithm instanceof BFF_Greedy)
				lvg = LoadGraph.loadDataset(iQ, datasetPath);

			if (metric == Config.MM || metric == Config.AM || metric == Config.TAM || metric == Config.MAM)
				st.writeMD(lvg, stats, iQ, S, metric);
			else
				st.writeAD(lvg, stats, iQ, S, metric);

			stats.write("\nTotal time: " + (System.currentTimeMillis() - time) + " (ms)");

			stats.write("\n\n======Nodes======\n");

			String[] token = dataset.split("ds:");

			if (token.length > 1) {
				int sizeOfDense = Integer.parseInt(token[token.length - 1]);
				int denseFound = 0;

				for (int id : S)
					if (id < sizeOfDense)
						denseFound++;

				stats.write("\nDense Nodes found: " + denseFound + "/" + sizeOfDense + "\n");
			} else {
				token = dataset.split("s:");
				int denseAFound = 0, denseBFound = 0;

				// hard coding for the competitor
				for (int id : S) {
					if (id >= lvg.size() - 100)
						denseAFound++;
					else if (id >= lvg.size() - 200)
						denseBFound++;
				}

				stats.write("\nDense Nodes A found: " + denseAFound + "/" + 100 + "\n");
				stats.write("\nDense Nodes B found: " + denseBFound + "/" + 100 + "\n");
			}

			stats.close();
		} catch (IOException e) {
			_log.log(Level.SEVERE, "", e);
		}
	}

	/**
	 * Initiate all runnables per dataset
	 */
	private void initializeRunnables() {
		r1 = new Runnable() {
			public void run() {
				BitSet iQ = new BitSet();
				iQ.set(1, 10);
				runMetrics("synthetic_pr:0.1_ds:100", iQ);
			}
		};

		r2 = new Runnable() {
			public void run() {
				BitSet iQ = new BitSet();
				iQ.set(1, 10);
				runMetrics("synthetic_pr:0.2_ds:100", iQ);
			}
		};

		r3 = new Runnable() {
			public void run() {
				BitSet iQ = new BitSet();
				iQ.set(1, 10);
				runMetrics("synthetic_pr:0.3_ds:100", iQ);
			}
		};

		r4 = new Runnable() {
			public void run() {
				BitSet iQ = new BitSet();
				iQ.set(1, 10);
				runMetrics("synthetic_pr:0.4_ds:100", iQ);
			}
		};

		r5 = new Runnable() {
			public void run() {
				BitSet iQ = new BitSet();
				iQ.set(1, 10);
				runMetrics("synthetic_pr:0.5_ds:100", iQ);
			}
		};

		r6 = new Runnable() {
			public void run() {
				BitSet iQ = new BitSet();
				iQ.set(1, 10);
				runMetrics("synthetic_pr:0.6_ds:100", iQ);
			}
		};

		r7 = new Runnable() {
			public void run() {
				BitSet iQ = new BitSet();
				iQ.set(1, 10);
				runMetrics("synthetic_pr:0.7_ds:100", iQ);
			}
		};

		r8 = new Runnable() {
			public void run() {
				BitSet iQ = new BitSet();
				iQ.set(1, 10);
				runMetrics("synthetic_pr:0.8_ds:100", iQ);
			}
		};

		r9 = new Runnable() {
			public void run() {
				BitSet iQ = new BitSet();
				iQ.set(1, 10);
				runMetrics("synthetic_pr:0.9_ds:100", iQ);
			}
		};

		r10 = new Runnable() {
			public void run() {
				BitSet iQ = new BitSet();
				iQ.set(1, 10);
				runMetrics("synthetic_pr:0.5_ds:10", iQ);
			}
		};

		r11 = new Runnable() {
			public void run() {
				BitSet iQ = new BitSet();
				iQ.set(1, 10);
				runMetrics("synthetic_pr:0.5_ds:20", iQ);
			}
		};

		r12 = new Runnable() {
			public void run() {
				BitSet iQ = new BitSet();
				iQ.set(1, 10);
				runMetrics("synthetic_pr:0.5_ds:50", iQ);
			}
		};

		r13 = new Runnable() {
			public void run() {
				BitSet iQ = new BitSet();
				iQ.set(1, 10);
				runMetrics("synthetic_pr:0.5_ds:200", iQ);
			}
		};

		r14 = new Runnable() {
			public void run() {
				BitSet iQ = new BitSet();
				iQ.set(1, 10);
				runMetrics("synthetic_pr:0.5_ds:500", iQ);
			}
		};
	}
}