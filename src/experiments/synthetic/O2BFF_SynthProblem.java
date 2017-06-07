package experiments.synthetic;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import algorithm.BFF;
import algorithm.BFF_Greedy;
import algorithm.Counter;
import algorithm.DCS_Greedy;
import experiments.MetricScore;
import system.Config;
import vg.Graph;
import vg.LoadGraph;

/**
 * O^2 BFF synthetic Experiments class k time-off problem
 * 
 * @author ksemer
 */
public class O2BFF_SynthProblem {
	private Callable<?> r1, r2;
	private static final Logger _log = Logger.getLogger(O2BFF_SynthProblem.class.getName());

	/**
	 * Constructor
	 */
	public O2BFF_SynthProblem() {
		ExecutorService executor = Executors.newCachedThreadPool();

		r1 = () -> {
			BitSet iQ = new BitSet();
			iQ.set(0, 10);
			runMetrics("rand", iQ, 2, 8, 2);
			return true;
		};

		r2 = () -> {
			BitSet iQ = new BitSet();
			iQ.set(0, 10);
			runMetrics("rew_rand", iQ, 2, 8, 2);
			return true;
		};

		executor.submit(r1);
		executor.submit(r2);

		r1 = () -> {
			for (int i = 9; i <= 9; i++) {
				BitSet iQ = new BitSet();
				iQ.set(0, 10);
				runMetrics("synthetic_pr:0." + i + "_s:", iQ, 2, 8, 2);
			}
			return true;
		};

		executor.submit(r1);
	}

	/**
	 * Run each metric in a thread
	 * 
	 * @param iQ
	 * @param dataset
	 * @param sk
	 * @param ek
	 * @param step
	 */
	private void runMetrics(String dataset, BitSet iQ, int sk, int ek, int step) {

		if (Config.RUN_FIND_BFF) {
			IntStream.range(1, 5).parallel().forEach(metric -> runInitializations(iQ, dataset, metric, sk, ek, step));
			IntStream.range(7, 9).parallel().forEach(metric -> runInitializations(iQ, dataset, metric, sk, ek, step));
		}

		if (Config.RUN_DCS)
			IntStream.range(9, 10).parallel().forEach(metric -> runInitializations(iQ, dataset, metric, sk, ek, step));

		if (Config.RUN_FIND_GREEDY)
			IntStream.range(5, 7).parallel().forEach(metric -> runInitializations(iQ, dataset, metric, sk, ek, step));
	}

	/**
	 * Run O^2 BFF with different initializations
	 * 
	 * @param iQ
	 * @param dataset
	 * @param metric
	 * @param sk
	 * @param ek
	 * @param step
	 */
	private void runInitializations(BitSet iQ, String dataset, int metric, int sk, int ek, int step) {
		try {
			for (int it = 0; it < Config.ITERATIONS; it++) {
				// for k = sk until ek with step
				for (int k = sk; k <= ek; k += step) {
					if (Config.RUN_O2_ITERATIVE) {
						if (Config.RUN_BESTK_CONT)
							runIterative(iQ, dataset, k, k, metric, Config.CONT_BEST_K, -1, it);

						// union
						if (Config.RUN_ATLEAST_K)
							runIterative(iQ, dataset, k, k, metric, Config.AT_LEAST_K, -1, it);

						if (Config.RUN_AGGR)
							runIterative(iQ, dataset, k, k, metric, Config.AGGR, -1, it);

						// random
						if (Config.RUN_RANDOM) {
							for (int i = 1; i <= Config.ITERATIONS; i++)
								runIterative(iQ, dataset, k, k, metric, Config.RANDOM, i, it);
						}
					}

					if (Config.RUN_O2_INCREMENTAL) {
						if (Config.RUN_DENS_IN)
							runIncremental(iQ, dataset, k, k, metric, Config.DENS_IN, it);

						if (Config.RUN_SET_IN)
							runIncremental(iQ, dataset, k, k, metric, Config.SET_IN, it);
					}
				}
			}
		} catch (IOException e) {
			_log.log(Level.SEVERE, "(metric, dataset)->" + "(" + metric + ", " + dataset + ")", e);
		}
	}

	/**
	 * Run O^2 BFF problem
	 * 
	 * @param iQ
	 * @param dataset
	 * @param k
	 * @param conk
	 * @param metric
	 * @param exec_type
	 * @param rC
	 * @param IT
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	private void runIterative(BitSet iQ, String dataset, int k, int conk, int metric, int exec_type, int rC, int IT)
			throws IOException {
		Set<Integer> S = null, convergedS = new HashSet<>();
		double convergedDensity = -1, solutionDensity = 0;
		int iteration = 1;

		_log.log(Level.INFO, "(metric, dataset, k)->" + "(" + metric + ", " + dataset + ", " + k + ")");

		// same metric for the first execution of the algorithm
		int initMetric = metric;
		long time = System.currentTimeMillis();

		// initial set of time instances
		BitSet iQ_ = (BitSet) iQ.clone(), iQ_old;
		Object algorithm = null;
		MetricScore st = new MetricScore();
		FileWriter stats = null;
		String outputPath;

		File dir = new File(Config.OUTPUT_PATH + "/o2/");

		if (!dir.exists())
			dir.mkdir();

		if (exec_type == Config.RANDOM) {
			dir = new File(Config.OUTPUT_PATH + "/o2/random/");

			if (!dir.exists())
				dir.mkdir();

			outputPath = Config.OUTPUT_PATH + "/o2/random/" + dataset + "_" + k + "_it_" + IT;
		} else
			outputPath = Config.OUTPUT_PATH + "/o2/" + dataset + "_" + k + "_it_" + IT;

		String datasetPath = Config.DATA_PATH + dataset + k + "_it_" + IT;

		Graph lvg = LoadGraph.loadDataset(iQ, datasetPath);

		if (exec_type == Config.RANDOM)
			stats = new FileWriter(outputPath + "_m=" + metric + "_k=" + k + "_it=" + rC + ".txt");
		else if (exec_type == Config.CONT_BEST_K)
			stats = new FileWriter(outputPath + "_m=" + metric + "_k=" + k + ".txt");
		else if (exec_type == Config.AT_LEAST_K)
			stats = new FileWriter(outputPath + "_m=" + metric + "_k=" + k + "_atleast.txt");
		else if (exec_type == Config.AGGR)
			stats = new FileWriter(outputPath + "_m=" + metric + "_k=" + k + "_aggr.txt");

		while (true) {

			if (iteration > 1) {
				Graph lvg_ = Graph.deepClone(lvg);

				if (metric == Config.DCS)
					algorithm = new DCS_Greedy(lvg_, iQ_, Collections.emptySet());
				else if (metric == Config.TMA || metric == Config.TAM)
					algorithm = new BFF_Greedy(lvg_, iQ_, metric, Collections.emptySet());
				else
					algorithm = new BFF(lvg_, iQ_, metric, Collections.emptySet());
			} else {
				if (exec_type == Config.RANDOM)
					algorithm = getRandomKSolution(lvg, iQ, conk, initMetric);
				else if (exec_type == Config.CONT_BEST_K)
					algorithm = getBestKContSolution(lvg, iQ, conk, initMetric);
				else if (exec_type == Config.AT_LEAST_K)
					algorithm = getAtLeastKSolution(lvg, iQ, conk, initMetric);
				else if (exec_type == Config.AGGR)
					algorithm = getAggrSolution(lvg, iQ, conk, initMetric);
			}

			if (algorithm instanceof BFF)
				S = ((BFF) algorithm).getSolutionSet();
			else if (algorithm instanceof BFF_Greedy)
				S = ((BFF_Greedy) algorithm).getSolutionSet();
			else if (algorithm instanceof DCS_Greedy)
				S = ((DCS_Greedy) algorithm).getSolutionSet();
			else
				// from union
				S = ((Set<Integer>) algorithm);

			iQ_old = (BitSet) iQ_.clone();

			// retrieve time instants size of k
			if (metric == Config.MM || metric == Config.AM || metric == Config.TAM || metric == Config.MAM)
				iQ_ = st.getKTimesMD(lvg, stats, iQ, S, k, metric);
			else
				iQ_ = st.getKTimesAD(lvg, stats, iQ, S, k, metric);

			// retrieve solution's density
			if (algorithm instanceof BFF)
				solutionDensity = ((BFF) algorithm).getSolutionDensity();
			else if (algorithm instanceof BFF_Greedy)
				solutionDensity = ((BFF_Greedy) algorithm).getSolutionDensity();
			else if (algorithm instanceof DCS_Greedy)
				solutionDensity = ((DCS_Greedy) algorithm).getSolutionDensity();
			else {
				// If the solution set is returned from union initialization
				solutionDensity = st.getScore();
			}

			// if current density is less than the new one
			if (convergedDensity < solutionDensity) {
				convergedDensity = solutionDensity;
				convergedS = new HashSet<Integer>(S);
			} else if (convergedDensity == solutionDensity) {
				// if density is same with the new one check intervals and sets
				if (iQ_old.equals(iQ_) || convergedS.equals(S))
					break;

				// if there is a new set of nodes update the set
				if (!convergedS.equals(S))
					convergedS = new HashSet<Integer>(S);
			} else
				break;

			stats.write("Iteration: " + iteration++ + " Score: " + solutionDensity + " Size(S): " + S.size()
					+ " Times: " + iQ_ + "\n");
			stats.flush();
		}

		stats.write("Iteration: " + iteration++ + " Score: " + convergedDensity + " Size(S): " + convergedS.size()
				+ " Times: " + iQ_ + "\n");
		stats.flush();
		stats.write("Total time: " + (System.currentTimeMillis() - time) + " (msec)");

		int denseAFound = 0, denseBFound = 0;
		String n_rs = "";

		// write nodes ids
		for (int id : convergedS) {
			if (id >= lvg.size() - 100)
				denseAFound++;
			else if (id >= lvg.size() - 200)
				denseBFound++;

			n_rs += id + "\n";
		}

		stats.write("\nDense Nodes A found: " + denseAFound + "/" + 100 + "\n");
		stats.write("\nDense Nodes B found: " + denseBFound + "/" + 100 + "\n");
		stats.write(n_rs);
		stats.close();
	}

	/**
	 * 
	 * @param iQ
	 * @param dataset
	 * @param k
	 * @param conk
	 * @param metric
	 * @param exec_type
	 * @param IT
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	private void runIncremental(BitSet iQ, String dataset, int k, int conk, int metric, int exec_type, int IT)
			throws IOException {
		Set<Integer> S = null;
		double solutionDensity = 0;

		_log.log(Level.INFO, "(metric, dataset, k)->" + "(" + metric + ", " + dataset + ", " + k + ")");

		// same metric for the first execution of the algorithm
		int initMetric = metric;
		long time = System.currentTimeMillis();

		// initial set of time instances
		BitSet iQ_ = (BitSet) iQ.clone();
		Object algorithm = null;
		MetricScore st = new MetricScore();
		FileWriter stats = null;
		String outputPath;

		File dir = new File(Config.OUTPUT_PATH + "/o2/");

		if (!dir.exists())
			dir.mkdir();

		outputPath = Config.OUTPUT_PATH + "/o2/" + dataset + "_" + k + "_it_" + IT;

		String datasetPath = Config.DATA_PATH + dataset + k + "_it_" + IT;

		Graph lvg = LoadGraph.loadDataset(iQ, datasetPath);

		if (exec_type == Config.SET_IN) {
			stats = new FileWriter(outputPath + "_m=" + metric + "_k=" + k + "_su.txt");
			iQ_ = getKSimilarSolution(lvg, iQ, conk, initMetric);
		} else if (exec_type == Config.DENS_IN) {
			stats = new FileWriter(outputPath + "_m=" + metric + "_k=" + k + "_du.txt");
			iQ_ = getKSimilarDensSolution(lvg, iQ, conk, initMetric);
		}

		// iQ_ = getKSimilarMinAvgSolution(lvg, iQ, conk, initMetric, false);
		// iQ_ = getKSimilarDensMinAvgSolution(lvg, iQ, conk, initMetric,
		// false);

		Graph lvg_ = Graph.deepClone(lvg);

		if (metric == Config.DCS)
			algorithm = new DCS_Greedy(lvg_, iQ_, Collections.emptySet());
		else if (metric == Config.TMA || metric == Config.TAM)
			algorithm = new BFF_Greedy(lvg_, iQ_, metric, Collections.emptySet());
		else
			algorithm = new BFF(lvg_, iQ_, metric, Collections.emptySet());

		if (algorithm instanceof BFF)
			S = ((BFF) algorithm).getSolutionSet();
		else if (algorithm instanceof BFF_Greedy)
			S = ((BFF_Greedy) algorithm).getSolutionSet();
		else if (algorithm instanceof DCS_Greedy)
			S = ((DCS_Greedy) algorithm).getSolutionSet();
		else
			// from union
			S = ((Set<Integer>) algorithm);

		// retrieve time instants size of k
		if (metric == Config.MM || metric == Config.AM || metric == Config.TAM || metric == Config.MAM)
			iQ_ = st.getKTimesMD(lvg, stats, iQ, S, k, metric);
		else
			iQ_ = st.getKTimesAD(lvg, stats, iQ, S, k, metric);

		// retrieve solution's density
		if (algorithm instanceof BFF)
			solutionDensity = ((BFF) algorithm).getSolutionDensity();
		else if (algorithm instanceof BFF_Greedy)
			solutionDensity = ((BFF_Greedy) algorithm).getSolutionDensity();
		else if (algorithm instanceof DCS_Greedy)
			solutionDensity = ((DCS_Greedy) algorithm).getSolutionDensity();
		else {
			// If the solution set is returned from union initialization
			solutionDensity = st.getScore();
		}

		stats.write("Score: " + solutionDensity + " Size(S): " + S.size() + " Times: " + iQ_ + "\n");
		stats.write("Total time: " + (System.currentTimeMillis() - time) + " (msec)");

		int denseAFound = 0, denseBFound = 0;
		String n_rs = "";

		// write nodes ids
		for (int id : S) {
			if (id >= lvg.size() - 100)
				denseAFound++;
			else if (id >= lvg.size() - 200)
				denseBFound++;

			n_rs += id + "\n";
		}

		stats.write("\nDense Nodes A found: " + denseAFound + "/" + 100 + "\n");
		stats.write("\nDense Nodes B found: " + denseBFound + "/" + 100 + "\n");
		stats.write(n_rs);
		stats.close();
	}

	/**
	 * Get random k solution
	 * 
	 * @param lvg
	 * @param iQ
	 * @param conk
	 * @param initMetric
	 * @return
	 * @throws IOException
	 */
	private Object getRandomKSolution(Graph lvg, BitSet iQ, int conk, int initMetric) throws IOException {
		Random rand = new Random();
		int n = 0;
		BitSet iQ_ = new BitSet();
		int first = iQ.nextSetBit(0);

		while (iQ_.cardinality() != conk) {
			n = rand.nextInt(iQ.cardinality()) + 1;
			n = first + n - 1;
			if (!iQ_.get(n))
				iQ_.set(n);
		}

		Graph lvg_ = Graph.deepClone(lvg);
		Object algorithm;

		if (initMetric == Config.DCS)
			algorithm = new DCS_Greedy(lvg_, iQ_, Collections.emptySet());
		else if (initMetric == Config.TMA || initMetric == Config.TAM)
			algorithm = new BFF_Greedy(lvg_, iQ_, initMetric, Collections.emptySet());
		else
			algorithm = new BFF(lvg_, iQ_, initMetric, Collections.emptySet());

		return algorithm;
	}

	/**
	 * Returns best k contiguous solution in iQ
	 * 
	 * @param lvg
	 * @param iQ
	 * @param conk
	 * @param initMetric
	 * @return
	 * @throws IOException
	 */
	private Object getBestKContSolution(Graph lvg, BitSet iQ, int conk, int initMetric) throws IOException {
		Graph lvg_;
		BitSet iQ_ = null;
		boolean whole = false;
		int start = iQ.nextSetBit(0);
		double bestScore = 0, solutionScore = 0;
		Object algorithm, bestSolutionAlgo = null;

		// ask for best |iQ| solution
		if (conk == iQ.cardinality()) {
			whole = true;
			conk = 1;
		}

		for (int i = 1; i <= iQ.cardinality() - conk; i++) {
			iQ_ = new BitSet();

			if (whole)
				iQ_ = (BitSet) iQ.clone();
			else
				iQ_.set(start, start + conk);

			start++;

			lvg_ = Graph.deepClone(lvg);

			if (initMetric == Config.DCS)
				algorithm = new DCS_Greedy(lvg_, iQ_, Collections.emptySet());
			else if (initMetric == Config.TMA || initMetric == Config.TAM)
				algorithm = new BFF_Greedy(lvg_, iQ_, initMetric, Collections.emptySet());
			else
				algorithm = new BFF(lvg_, iQ_, initMetric, Collections.emptySet());

			if (algorithm instanceof BFF)
				solutionScore = ((BFF) algorithm).getSolutionDensity();
			else if (algorithm instanceof BFF_Greedy)
				solutionScore = ((BFF_Greedy) algorithm).getSolutionDensity();
			else if (algorithm instanceof DCS_Greedy)
				solutionScore = ((DCS_Greedy) algorithm).getSolutionDensity();

			if (bestScore <= solutionScore) {
				bestScore = solutionScore;
				bestSolutionAlgo = algorithm;
			}

			// if its whole only one run is required
			if (whole)
				return bestSolutionAlgo;
		}

		return bestSolutionAlgo;
	}

	/**
	 * Aggregate solution
	 * 
	 * @param lvg
	 * @param iQ
	 * @param conk
	 * @param initMetric
	 * @return
	 */
	private Object getAggrSolution(Graph lvg, BitSet iQ, int conk, int initMetric) {
		Object algorithm;
		Graph lvg_;
		Set<Integer> S = new HashSet<>();

		lvg_ = Graph.deepClone(lvg);

		algorithm = new BFF(lvg_, (BitSet) iQ.clone(), Config.AA, Collections.emptySet());
		S = ((BFF) algorithm).getSolutionSet();

		return S;
	}

	/**
	 * Get the nodes that are part of at least k solutions
	 * 
	 * @param lvg
	 * @param iQ
	 * @param k
	 * @param initMetric
	 * @return
	 * @throws IOException
	 */
	private Set<Integer> getAtLeastKSolution(Graph lvg, BitSet iQ, int k, int initMetric) throws IOException {
		BitSet iQ_ = null;
		Object algorithm;
		Graph lvg_;
		Set<Integer> S = new HashSet<>(), solSet = null;
		List<Set<Integer>> solutions = new ArrayList<>();

		TreeMap<Integer, Set<Integer>> scores = new TreeMap<>(Collections.reverseOrder());

		for (int i = iQ.nextSetBit(0); i >= 0; i = iQ.nextSetBit(i + 1)) {
			iQ_ = new BitSet();
			iQ_.set(i);

			lvg_ = Graph.deepClone(lvg);

			if (initMetric == Config.DCS)
				algorithm = new DCS_Greedy(lvg_, iQ_, Collections.emptySet());
			else if (initMetric == Config.TMA || initMetric == Config.TAM)
				algorithm = new BFF_Greedy(lvg_, iQ_, initMetric, Collections.emptySet());
			else
				algorithm = new BFF(lvg_, iQ_, initMetric, Collections.emptySet());

			if (algorithm instanceof BFF)
				solSet = ((BFF) algorithm).getSolutionSet();
			else if (algorithm instanceof BFF_Greedy)
				solSet = ((BFF_Greedy) algorithm).getSolutionSet();
			else if (algorithm instanceof DCS_Greedy)
				solSet = ((DCS_Greedy) algorithm).getSolutionSet();

			solutions.add(solSet);
		}

		for (int i = 1; i <= iQ.cardinality(); i++)
			scores.put(i, new HashSet<>());

		Map<Integer, Counter> nodes_score = new HashMap<>();
		Counter c;

		for (Set<Integer> set : solutions) {
			for (int n : set) {
				if ((c = nodes_score.get(n)) == null) {
					c = new Counter();
					nodes_score.put(n, c);
				}
				c.increase();
			}
		}

		for (Entry<Integer, Counter> entry : nodes_score.entrySet())
			scores.get(entry.getValue().getValue()).add(entry.getKey());

		int mul = 0;

		for (Entry<Integer, Set<Integer>> entry : scores.entrySet()) {
			if (entry.getValue().isEmpty())
				continue;

			if (entry.getKey() < k)
				return S;

			mul++;
			S.addAll(entry.getValue());

			if (mul == k)
				return S;
		}

		return S;
	}

	/**
	 * Incremental similar k solution
	 * 
	 * @param lvg
	 * @param iQ
	 * @param k
	 * @param initMetric
	 * @return
	 * @throws IOException
	 */
	private BitSet getKSimilarSolution(Graph lvg, BitSet iQ, int k, int initMetric) throws IOException {
		BitSet iQ_ = null;
		Object algorithm;
		Graph lvg_;
		Set<Integer> solSet = null;
		List<Set<Integer>> solutions = new ArrayList<>();

		for (int i = iQ.nextSetBit(0); i >= 0; i = iQ.nextSetBit(i + 1)) {
			iQ_ = new BitSet();
			iQ_.set(i);

			lvg_ = Graph.deepClone(lvg);

			if (initMetric == Config.DCS)
				algorithm = new DCS_Greedy(lvg_, iQ_, Collections.emptySet());
			else if (initMetric == Config.TMA || initMetric == Config.TAM)
				algorithm = new BFF_Greedy(lvg_, iQ_, initMetric, Collections.emptySet());
			else
				algorithm = new BFF(lvg_, iQ_, initMetric, Collections.emptySet());

			if (algorithm instanceof BFF)
				solSet = ((BFF) algorithm).getSolutionSet();
			else if (algorithm instanceof BFF_Greedy)
				solSet = ((BFF_Greedy) algorithm).getSolutionSet();
			else if (algorithm instanceof DCS_Greedy)
				solSet = ((DCS_Greedy) algorithm).getSolutionSet();

			solutions.add(solSet);
		}

		BitSet iQ_r = new BitSet();

		Set<Integer> union;
		double jaccard;
		DecimalFormat df = new DecimalFormat("#.##");
		df.setRoundingMode(RoundingMode.CEILING);
		int p[] = new int[2];
		double max = -1;

		for (int i = 0; i < solutions.size(); i++) {
			Set<Integer> a = new HashSet<>(solutions.get(i));

			for (int j = i + 1; j < solutions.size(); j++) {
				Set<Integer> b = new HashSet<>(solutions.get(j));

				a.retainAll(b);

				union = new HashSet<>(solutions.get(i));
				union.addAll(b);

				jaccard = Double.parseDouble(df.format((double) a.size() / union.size()));

				if (max < jaccard) {
					max = jaccard;
					p[0] = i;
					p[1] = j;
				}
			}
		}

		Set<Integer> checked = new HashSet<>();
		checked.add(p[0]);
		checked.add(p[1]);

		iQ_r.set(p[0]);
		iQ_r.set(p[1]);

		if (iQ_r.cardinality() == k)
			return iQ_r;

		Set<Integer> inter = new HashSet<>(solutions.get(p[0]));
		inter.retainAll(solutions.get(p[1]));

		while (iQ_r.cardinality() != k) {
			max = -1;
			int t = -1;
			Set<Integer> in = null;

			for (int i = 0; i < solutions.size(); i++) {
				if (checked.contains(i))
					continue;

				Set<Integer> a = new HashSet<>(solutions.get(i));
				Set<Integer> c = new HashSet<>(inter);

				c.addAll(a);
				a.retainAll(inter);

				jaccard = Double.parseDouble(df.format((double) a.size() / c.size()));

				if (max < jaccard) {
					max = jaccard;
					t = i;
					in = new HashSet<>(a);
				}
			}

			inter = new HashSet<>(in);
			iQ_r.set(t);
			checked.add(t);
		}

		return iQ_r;
	}

	/**
	 * Incremental Min and Avg set similarity
	 * 
	 * @param lvg
	 * @param iQ
	 * @param k
	 * @param initMetric
	 * @param runMin
	 * @return
	 */
	@SuppressWarnings("unused")
	private BitSet getKSimilarMinAvgSolution(Graph lvg, BitSet iQ, int k, int initMetric, boolean runMin) {
		BitSet iQ_ = null;
		Object algorithm;
		Graph lvg_;
		Set<Integer> solSet = null;
		List<Set<Integer>> solutions = new ArrayList<>();

		for (int i = iQ.nextSetBit(0); i >= 0; i = iQ.nextSetBit(i + 1)) {
			iQ_ = new BitSet();
			iQ_.set(i);

			lvg_ = Graph.deepClone(lvg);

			if (initMetric == Config.DCS)
				algorithm = new DCS_Greedy(lvg_, iQ_, Collections.emptySet());
			else if (initMetric == Config.TMA || initMetric == Config.TAM)
				algorithm = new BFF_Greedy(lvg_, iQ_, initMetric, Collections.emptySet());
			else
				algorithm = new BFF(lvg_, iQ_, initMetric, Collections.emptySet());

			if (algorithm instanceof BFF)
				solSet = ((BFF) algorithm).getSolutionSet();
			else if (algorithm instanceof BFF_Greedy)
				solSet = ((BFF_Greedy) algorithm).getSolutionSet();
			else if (algorithm instanceof DCS_Greedy)
				solSet = ((DCS_Greedy) algorithm).getSolutionSet();

			solutions.add(solSet);
		}

		Set<Integer> union;
		double jaccard, max = -1;
		DecimalFormat df = new DecimalFormat("#.##");
		df.setRoundingMode(RoundingMode.CEILING);

		double[][] sim = new double[solutions.size()][solutions.size()];
		int p[] = new int[2];

		for (int i = 0; i < solutions.size(); i++) {
			Set<Integer> a = new HashSet<>(solutions.get(i));

			for (int j = i + 1; j < solutions.size(); j++) {
				Set<Integer> b = new HashSet<>(solutions.get(j));

				a.retainAll(b);

				union = new HashSet<>(solutions.get(i));
				union.addAll(b);

				jaccard = Double.parseDouble(df.format((double) a.size() / union.size()));

				sim[i][j] = jaccard;
				sim[j][i] = jaccard;

				if (max < jaccard) {
					max = jaccard;
					p[0] = i;
					p[1] = j;
				}
			}
		}

		BitSet iQ_r = new BitSet();
		Set<Integer> checked = new HashSet<>(), all = new HashSet<>();

		iQ_r.set(p[0]);
		iQ_r.set(p[1]);
		checked.add(p[0]);
		checked.add(p[1]);

		if (iQ_r.cardinality() == k)
			return iQ_r;

		for (int i = 0; i < solutions.size(); i++) {
			if (!checked.contains(i))
				all.add(i);
		}

		if (runMin) {
			while (iQ_r.cardinality() != k) {
				int t = -1;
				max = -1;

				for (int j : all) {
					double min = Double.MAX_VALUE;

					for (int i : checked) {
						double s = sim[i][j];

						if (min > s)
							min = s;
					}

					if (min > max) {
						max = min;
						t = j;
					}
				}

				iQ_r.set(t);
				all.remove(t);
				checked.add(t);
			}
		} else {
			while (iQ_r.cardinality() != k) {
				int t = -1;
				max = -1;

				for (int j : all) {
					double s = 0;

					for (int i : checked)
						s += sim[i][j];

					if (max < s) {
						max = s;
						t = j;
					}
				}

				iQ_r.set(t);
				all.remove(t);
				checked.add(t);
			}
		}

		return iQ_r;
	}

	/**
	 * Incremental k densest solution
	 * 
	 * @param lvg
	 * @param iQ
	 * @param k
	 * @param initMetric
	 * @return
	 * @throws IOException
	 */
	private BitSet getKSimilarDensSolution(Graph lvg, BitSet iQ, int k, int initMetric) throws IOException {
		BitSet iQ_ = null;
		Object algorithm;
		Graph lvg_;

		double[][] sim = new double[iQ.cardinality()][iQ.cardinality()];
		int[] p = new int[2];
		double max = -1;

		for (int i = iQ.nextSetBit(0); i >= 0; i = iQ.nextSetBit(i + 1)) {
			iQ_ = new BitSet();
			iQ_.set(i);

			for (int j = iQ.nextSetBit(i + 1); j >= 0; j = iQ.nextSetBit(j + 1)) {
				iQ_.set(j);

				lvg_ = Graph.deepClone(lvg);

				if (initMetric == Config.DCS)
					algorithm = new DCS_Greedy(lvg_, iQ_, Collections.emptySet());
				else if (initMetric == Config.TMA || initMetric == Config.TAM)
					algorithm = new BFF_Greedy(lvg_, iQ_, initMetric, Collections.emptySet());
				else
					algorithm = new BFF(lvg_, iQ_, initMetric, Collections.emptySet());

				sim[i][j] = ((BFF) algorithm).getSolutionDensity();
				sim[j][i] = ((BFF) algorithm).getSolutionDensity();

				if (max < sim[i][j]) {
					max = sim[i][j];
					p[0] = i;
					p[1] = j;
				}
			}
		}

		BitSet iQ_r = new BitSet();
		Set<Integer> checked = new HashSet<>(), all = new HashSet<>();

		iQ_r.set(p[0]);
		iQ_r.set(p[1]);
		checked.add(p[0]);
		checked.add(p[1]);

		if (iQ_r.cardinality() == k)
			return iQ_r;

		for (int i = iQ.nextSetBit(0); i >= 0; i = iQ.nextSetBit(i + 1)) {
			if (!checked.contains(i))
				all.add(i);
		}

		while (iQ_r.cardinality() != k) {
			max = -1;
			int t = -1;
			iQ_ = new BitSet();
			double s;

			for (int j : all) {
				iQ_ = (BitSet) iQ_r.clone();
				iQ_.set(j);

				lvg_ = Graph.deepClone(lvg);

				if (initMetric == Config.DCS)
					algorithm = new DCS_Greedy(lvg_, iQ_, Collections.emptySet());
				else if (initMetric == Config.TMA || initMetric == Config.TAM)
					algorithm = new BFF_Greedy(lvg_, iQ_, initMetric, Collections.emptySet());
				else
					algorithm = new BFF(lvg_, iQ_, initMetric, Collections.emptySet());

				s = ((BFF) algorithm).getSolutionDensity();

				if (max < s) {
					max = s;
					t = j;
				}

			}

			iQ_r.set(t);
			checked.add(t);
			all.remove(t);
		}

		return iQ_r;
	}

	/**
	 * Incremental Min and Avg density similarity
	 * 
	 * @param lvg
	 * @param iQ
	 * @param k
	 * @param initMetric
	 * @param runMin
	 * @return
	 * @throws IOException
	 */
	@SuppressWarnings("unused")
	private BitSet getKSimilarDensMinAvgSolution(Graph lvg, BitSet iQ, int k, int initMetric, boolean runMin)
			throws IOException {
		BitSet iQ_ = null;
		Object algorithm;
		Graph lvg_;

		double[][] sim = new double[iQ.cardinality()][iQ.cardinality()];
		int[] p = new int[2];
		double max = -1;

		for (int i = iQ.nextSetBit(0); i >= 0; i = iQ.nextSetBit(i + 1)) {
			iQ_ = new BitSet();
			iQ_.set(i);

			for (int j = iQ.nextSetBit(i + 1); j >= 0; j = iQ.nextSetBit(j + 1)) {
				iQ_.set(j);

				lvg_ = Graph.deepClone(lvg);

				if (initMetric == Config.DCS)
					algorithm = new DCS_Greedy(lvg_, iQ_, Collections.emptySet());
				else if (initMetric == Config.TMA || initMetric == Config.TAM)
					algorithm = new BFF_Greedy(lvg_, iQ_, initMetric, Collections.emptySet());
				else
					algorithm = new BFF(lvg_, iQ_, initMetric, Collections.emptySet());

				sim[i][j] = ((BFF) algorithm).getSolutionDensity();
				sim[j][i] = ((BFF) algorithm).getSolutionDensity();

				if (max < sim[i][j]) {
					max = sim[i][j];
					p[0] = i;
					p[1] = j;
				}
			}
		}

		BitSet iQ_r = new BitSet();
		Set<Integer> checked = new HashSet<>(), all = new HashSet<>();

		iQ_r.set(p[0]);
		iQ_r.set(p[1]);
		checked.add(p[0]);
		checked.add(p[1]);

		if (iQ_r.cardinality() == k)
			return iQ_r;

		for (int i = iQ.nextSetBit(0); i >= 0; i = iQ.nextSetBit(i + 1)) {
			if (!checked.contains(i))
				all.add(i);
		}

		if (runMin) {
			while (iQ_r.cardinality() != k) {
				int t = -1;
				max = -1;

				for (int j : all) {
					double min = Double.MAX_VALUE;

					for (int i : checked) {
						double s = sim[i][j];

						if (min > s)
							min = s;
					}

					if (min > max) {
						max = min;
						t = j;
					}
				}

				iQ_r.set(t);
				all.remove(t);
				checked.add(t);
			}
		} else {
			while (iQ_r.cardinality() != k) {
				int t = -1;
				max = -1;

				for (int j : all) {
					double s = 0;

					for (int i : checked)
						s += sim[i][j];

					if (max < s) {
						max = s;
						t = j;
					}
				}

				iQ_r.set(t);
				all.remove(t);
				checked.add(t);
			}
		}

		return iQ_r;
	}
}