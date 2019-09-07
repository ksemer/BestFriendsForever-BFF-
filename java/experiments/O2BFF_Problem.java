package experiments;

import java.io.File;
import java.io.FileWriter;
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
import system.Config;
import vg.Graph;
import vg.LoadGraph;

/**
 * Problem2 Experiments class k time-off problem
 *
 * @author ksemer
 */
public class O2BFF_Problem {

    // seed nodes that will be part of O^2 BFF's solution
    private Set<Integer> seedNodes;

    private static final Logger _logger = Logger.getLogger(O2BFF_Problem.class.getName());

    /**
     * Constructor
     */
    public O2BFF_Problem() {
        ExecutorService executor = Executors.newCachedThreadPool();
        seedNodes = new HashSet<>(Config.SEED_NODES);

        // initialize Callables
        // stores all callable objects
        List<Callable<?>> callables = new ArrayList<>();
        for (int i = 0; i < Config.DATASETS.size(); i++) {
            callables.add(setCallable(Config.INTERVALS.get(i), Config.DATASETS.get(i)));
        }

        for (Callable<?> c : callables)
            executor.submit(c);
        executor.shutdown();
    }

    /**
     * Run each metric in a thread
     */
    private void runMetrics(String dataset, BitSet iQ, int sk, int ek, int step) {
        Callable<?> c1, c2, c3;
        ExecutorService executor = Executors.newCachedThreadPool();

        c1 = () -> {
            if (Config.RUN_DCS)
                runInitializations(iQ, dataset, Config.DCS, sk, ek, step);
            return true;
        };

        c2 = () -> {
            if (Config.RUN_FIND_GREEDY)
                IntStream.range(5, 7).parallel()
                        .forEach(metric -> runInitializations(iQ, dataset, metric, sk, ek, step));
            return true;
        };

        c3 = () -> {
            if (Config.RUN_FIND_BFF) {
                IntStream.range(1, 5).parallel()
                        .forEach(metric -> runInitializations(iQ, dataset, metric, sk, ek, step));
                IntStream.range(7, 9).parallel()
                        .forEach(metric -> runInitializations(iQ, dataset, metric, sk, ek, step));
            }
            return true;
        };

        executor.submit(c1);
        executor.submit(c2);
        executor.submit(c3);
        executor.shutdown();
    }

    /**
     * Run O^2 BFF with different initializations
     */
    private void runInitializations(BitSet iQ, String dataset, int metric, int sk, int ek, int step) {
        Callable<?> c1, c2;
        ExecutorService executor = Executors.newCachedThreadPool();

        c1 = () -> {
            // for k = sk until ek with step
            for (int k = sk; k <= ek; k += step) {

                if (Config.RUN_O2_ITERATIVE) {

                    if (Config.RUN_BESTK_CONT)
                        runIterative(iQ, dataset, k, k, metric, Config.CONT_BEST_K, -1);

                    // at least k
                    if (Config.RUN_ATLEAST_K)
                        runIterative(iQ, dataset, k, k, metric, Config.AT_LEAST_K, -1);

                    if (Config.RUN_AGGR)
                        runIterative(iQ, dataset, k, k, metric, Config.AGGR, -1);
                }

                if (Config.RUN_O2_INCREMENTAL) {

                    if (Config.RUN_DENS_IN)
                        runIncremental(iQ, dataset, k, k, metric, Config.DENS_IN);

                    if (Config.RUN_DENS_IN_MIN)
                        runIncremental(iQ, dataset, k, k, metric, Config.DENS_IN_MIN);

                    if (Config.RUN_DENS_IN_AVG)
                        runIncremental(iQ, dataset, k, k, metric, Config.DENS_IN_AVG);

                    if (Config.RUN_SET_IN)
                        runIncremental(iQ, dataset, k, k, metric, Config.SET_IN);

                    if (Config.RUN_SET_IN_MIN)
                        runIncremental(iQ, dataset, k, k, metric, Config.SET_IN_MIN);

                    if (Config.RUN_SET_IN_AVG)
                        runIncremental(iQ, dataset, k, k, metric, Config.SET_IN_AVG);

                    if (Config.RUN_SET_IN_NEW)
                        runIncremental(iQ, dataset, k, k, metric, Config.SET_IN_NEW);
                }

            }
            return true;
        };

        if (Config.RUN_O2_ITERATIVE) {

            c2 = () -> {
                // for k = sk until ek with step
                for (int k = sk; k <= ek; k += step) {

                    // random
                    if (Config.RUN_RANDOM) {
                        for (int i = 1; i <= Config.ITERATIONS; i++)
                            runIterative(iQ, dataset, k, k, metric, Config.RANDOM, i);
                    }

                }
                return true;
            };

            executor.submit(c2);
        }

        executor.submit(c1);
        executor.shutdown();
    }

    /**
     * Run Iterative algorithms for O^2 BFF problem
     */
    @SuppressWarnings("unchecked")
    private void runIterative(BitSet iQ, String dataset, int k, int conk, int metric, int exec_type, int rC) {
        _logger.info("(metric, dataset, k)->" + "(" + metric + ", " + dataset + ", " + k + ")");
        long time = System.currentTimeMillis();

        // initial set of time instances
        BitSet iQ_ = (BitSet) iQ.clone(), iQ_old;
        Object algorithm = null;
        MetricScore st = new MetricScore();
        FileWriter stats = null;
        String outputPath;
        DecimalFormat df = new DecimalFormat("#.##");
        df.setRoundingMode(RoundingMode.CEILING);

        try {

            File dir = new File(Config.OUTPUT_PATH + "/o2/");

            if (!dir.exists()) {
                if (!dir.mkdir())
                    _logger.log(Level.WARNING, dir.getName() + " exists.");
            }

            if (exec_type == Config.RANDOM) {
                dir = new File(Config.OUTPUT_PATH + "/o2/random/");

                if (!dir.exists())
                    if (!dir.mkdir())
                        _logger.log(Level.WARNING, dir.getName() + " exists.");

                outputPath = Config.OUTPUT_PATH + "/o2/random/" + dataset;
            } else
                outputPath = Config.OUTPUT_PATH + "/o2/" + dataset;

            String datasetPath = Config.DATA_PATH + dataset;

            Graph lvg = LoadGraph.loadDataset(iQ, datasetPath);

            if (exec_type == Config.RANDOM)
                stats = new FileWriter(outputPath + "_m=" + metric + "_k=" + k + "_it=" + rC + ".txt");
            else if (exec_type == Config.CONT_BEST_K)
                stats = new FileWriter(outputPath + "_m=" + metric + "_k=" + k + ".txt");
            else if (exec_type == Config.AT_LEAST_K)
                stats = new FileWriter(outputPath + "_m=" + metric + "_k=" + k + "_atleast.txt");
            else if (exec_type == Config.AGGR)
                stats = new FileWriter(outputPath + "_m=" + metric + "_k=" + k + "_aggr.txt");

            int iteration = 1;
            Set<Integer> S, convergedS = new HashSet<>();
            double convergedDensity = -1, solutionDensity;

            while (true) {

                if (iteration > 1) {
                    Graph lvg_ = Graph.deepClone(lvg);

                    if (metric == Config.DCS)
                        algorithm = new DCS_Greedy(lvg_, iQ_, seedNodes);
                    else if (metric == Config.TMA || metric == Config.TAM)
                        algorithm = new BFF_Greedy(lvg_, iQ_, metric, seedNodes);
                    else
                        algorithm = new BFF(lvg_, iQ_, metric, seedNodes);
                } else {
                    if (exec_type == Config.RANDOM)
                        algorithm = getRandomKSolution(lvg, iQ, conk, metric);
                    else if (exec_type == Config.CONT_BEST_K)
                        algorithm = getBestKContSolution(lvg, iQ, conk, metric);
                    else if (exec_type == Config.AT_LEAST_K)
                        algorithm = getAtLeastKSolution(lvg, iQ, conk, metric);
                    else if (exec_type == Config.AGGR)
                        algorithm = getAggrSolution(lvg, iQ);
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

                if (!(iteration == 1 && (Double.compare(Double.NaN, solutionDensity) == 0
                        || solutionDensity == Double.MAX_VALUE || ("" + solutionDensity).contains("E")))) {
                    solutionDensity = Double.parseDouble(df.format(solutionDensity));

                    // if current density is less than the new one
                    if (convergedDensity < solutionDensity) {
                        convergedDensity = solutionDensity;
                        assert S != null;
                        convergedS = new HashSet<>(S);
                    } else if (convergedDensity == solutionDensity) {
                        // if density is same with the new one check intervals
                        // and
                        // sets
                        if (iQ_old.equals(iQ_) || convergedS.equals(S) || convergedDensity == 0)
                            break;

                        // if there is a new set of nodes update the set
                        if (!convergedS.equals(S)) {
                            assert S != null;
                            convergedS = new HashSet<>(S);
                        }
                    } else
                        break;
                }

                assert stats != null;
                stats.write("Iteration: " + iteration++ + " Score: " + solutionDensity + " Size(S): " + S.size()
                        + " Times: " + iQ_ + "\n");
                stats.flush();
            }

            assert stats != null;
            stats.write("Iteration: " + ++iteration + " Score: " + convergedDensity + " Size(S): " + convergedS.size()
                    + " Times: " + iQ_ + "\n");
            stats.flush();

            // write authors
            for (int id : convergedS) {
                if (dataset.toLowerCase().contains("dblp"))
                    stats.write(id + "\t" + LoadGraph.getAuthor(id) + "\n");
                else if (dataset.toLowerCase().contains("twitter"))
                    stats.write(id + "\t" + LoadGraph.getHashtag(id) + "\n");
                else
                    stats.write(id + "\n");
            }
            stats.write("Total time: " + (System.currentTimeMillis() - time) + " (msec)");
            stats.close();
        } catch (Exception e) {
            _logger.log(Level.SEVERE, "(metric, dataset, k)->" + "(" + metric + ", " + dataset + ", " + k + ")", e);
        }
    }

    /**
     * Run Incremental algorithms for O^2 BFF problem
     */
    private void runIncremental(BitSet iQ, String dataset, int k, int conk, int metric, int exec_type) {
        Set<Integer> S;
        double solutionDensity;

        _logger.info("(metric, dataset, k)->" + "(" + metric + ", " + dataset + ", " + k + ")");

        long time = System.currentTimeMillis();

        // initial set of time instances
        BitSet iQ_ = (BitSet) iQ.clone();
        Object algorithm;
        MetricScore st = new MetricScore();
        FileWriter stats = null;
        String outputPath;
        DecimalFormat df = new DecimalFormat("#.##");
        df.setRoundingMode(RoundingMode.CEILING);

        try {
            File dir = new File(Config.OUTPUT_PATH + "/o2/");

            if (!dir.exists()) {
                if (!dir.mkdir())
                    _logger.log(Level.WARNING, dir.getName() + " exists.");
            }

            if (exec_type == Config.RANDOM) {
                dir = new File(Config.OUTPUT_PATH + "/o2/random/");

                if (!dir.exists()) {
                    if (!dir.mkdir())
                        _logger.log(Level.WARNING, dir.getName() + " exists.");
                }

                outputPath = Config.OUTPUT_PATH + "/o2/random/" + dataset;
            } else
                outputPath = Config.OUTPUT_PATH + "/o2/" + dataset;

            String datasetPath = Config.DATA_PATH + dataset;
            Graph lvg = LoadGraph.loadDataset(iQ, datasetPath);

            if (exec_type == Config.SET_IN) {
                stats = new FileWriter(outputPath + "_m=" + metric + "_k=" + k + "_su.txt");
                iQ_ = getKSimilarSolution(lvg, iQ, conk, metric);
            } else if (exec_type == Config.SET_IN_MIN) {
                stats = new FileWriter(outputPath + "_m=" + metric + "_k=" + k + "_sm.txt");
                iQ_ = getKSimilarMinAvgSolution(lvg, iQ, conk, metric, true);
            } else if (exec_type == Config.SET_IN_AVG) {
                stats = new FileWriter(outputPath + "_m=" + metric + "_k=" + k + "_sa.txt");
                iQ_ = getKSimilarMinAvgSolution(lvg, iQ, conk, metric, false);
            } else if (exec_type == Config.SET_IN_NEW) {
                stats = new FileWriter(outputPath + "_m=" + metric + "_k=" + k + "_sn.txt");
                iQ_ = getKSimilarNewSolution(lvg, iQ, conk, metric);
            } else if (exec_type == Config.DENS_IN) {
                stats = new FileWriter(outputPath + "_m=" + metric + "_k=" + k + "_du.txt");
                iQ_ = getKSimilarDensSolution(lvg, iQ, conk, metric);
            } else if (exec_type == Config.DENS_IN_MIN) {
                stats = new FileWriter(outputPath + "_m=" + metric + "_k=" + k + "_dm.txt");
                iQ_ = getKSimilarDensMinAvgSolution(lvg, iQ, conk, metric, true);
            } else if (exec_type == Config.DENS_IN_AVG) {
                stats = new FileWriter(outputPath + "_m=" + metric + "_k=" + k + "_da.txt");
                iQ_ = getKSimilarDensMinAvgSolution(lvg, iQ, conk, metric, false);
            }

            Graph lvg_ = Graph.deepClone(lvg);

            if (metric == Config.DCS)
                algorithm = new DCS_Greedy(lvg_, iQ_, seedNodes);
            else if (metric == Config.TMA || metric == Config.TAM)
                algorithm = new BFF_Greedy(lvg_, iQ_, metric, seedNodes);
            else
                algorithm = new BFF(lvg_, iQ_, metric, seedNodes);

            if (algorithm instanceof BFF)
                S = ((BFF) algorithm).getSolutionSet();
            else if (algorithm instanceof BFF_Greedy)
                S = ((BFF_Greedy) algorithm).getSolutionSet();
            else
                S = ((DCS_Greedy) algorithm).getSolutionSet();

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
            else
                solutionDensity = ((DCS_Greedy) algorithm).getSolutionDensity();

            solutionDensity = Double.parseDouble(df.format(solutionDensity));

            assert stats != null;
            stats.write("Score: " + solutionDensity + " Size(S): " + S.size() + " Times: " + iQ_ + "\n");

            // write authors
            for (int id : S) {
                if (dataset.toLowerCase().contains("dblp"))
                    stats.write(id + "\t" + LoadGraph.getAuthor(id) + "\n");
                else if (dataset.toLowerCase().contains("twitter"))
                    stats.write(id + "\t" + LoadGraph.getHashtag(id) + "\n");
                else
                    stats.write(id + "\n");
            }
            stats.write("Total time: " + (System.currentTimeMillis() - time) + " (msec)");
            stats.close();
        } catch (Exception e) {
            _logger.log(Level.SEVERE, "(metric, dataset, k)->" + "(" + metric + ", " + dataset + ", " + k + ")", e);
        }
    }

    /**
     * Get the nodes that are part of at least k solutions
     */
    private Set<Integer> getAtLeastKSolution(Graph lvg, BitSet iQ, int k, int initMetric) {
        BitSet iQ_;
        Object algorithm;
        Graph lvg_;
        Set<Integer> S = new HashSet<>(), solSet;
        List<Set<Integer>> solutions = new ArrayList<>();
        TreeMap<Integer, Set<Integer>> scores = new TreeMap<>(Collections.reverseOrder());

        for (int i = iQ.nextSetBit(0); i >= 0; i = iQ.nextSetBit(i + 1)) {
            iQ_ = new BitSet();
            iQ_.set(i);

            lvg_ = Graph.deepClone(lvg);

            if (initMetric == Config.DCS)
                algorithm = new DCS_Greedy(lvg_, iQ_, seedNodes);
            else if (initMetric == Config.TMA || initMetric == Config.TAM)
                algorithm = new BFF_Greedy(lvg_, iQ_, initMetric, seedNodes);
            else
                algorithm = new BFF(lvg_, iQ_, initMetric, seedNodes);

            if (algorithm instanceof BFF)
                solSet = ((BFF) algorithm).getSolutionSet();
            else if (algorithm instanceof BFF_Greedy)
                solSet = ((BFF_Greedy) algorithm).getSolutionSet();
            else
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
     * Get random k solution
     */
    private Object getRandomKSolution(Graph lvg, BitSet iQ, int conk, int initMetric) {
        Random rand = new Random();
        int n;
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
            algorithm = new DCS_Greedy(lvg_, iQ_, seedNodes);
        else if (initMetric == Config.TMA || initMetric == Config.TAM)
            algorithm = new BFF_Greedy(lvg_, iQ_, initMetric, seedNodes);
        else
            algorithm = new BFF(lvg_, iQ_, initMetric, seedNodes);

        return algorithm;
    }

    /**
     * Returns best k contiguous solution in iQ
     */
    private Object getBestKContSolution(Graph lvg, BitSet iQ, int conk, int initMetric) {
        Graph lvg_;
        BitSet iQ_;
        boolean whole = false;
        int start = iQ.nextSetBit(0);
        double bestScore = 0, solutionScore;
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
                algorithm = new DCS_Greedy(lvg_, iQ_, seedNodes);
            else if (initMetric == Config.TMA || initMetric == Config.TAM)
                algorithm = new BFF_Greedy(lvg_, iQ_, initMetric, seedNodes);
            else
                algorithm = new BFF(lvg_, iQ_, initMetric, seedNodes);

            if (algorithm instanceof BFF)
                solutionScore = ((BFF) algorithm).getSolutionDensity();
            else if (algorithm instanceof BFF_Greedy)
                solutionScore = ((BFF_Greedy) algorithm).getSolutionDensity();
            else
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
     */
    private Object getAggrSolution(Graph lvg, BitSet iQ) {
        Graph lvg_ = Graph.deepClone(lvg);
        BFF algorithm = new BFF(lvg_, (BitSet) iQ.clone(), Config.AA, Collections.emptySet());
        return algorithm.getSolutionSet();
    }

    /**
     * Incremental similar k solution
     */
    private BitSet getKSimilarSolution(Graph lvg, BitSet iQ, int k, int initMetric) {
        BitSet iQ_;
        Set<Integer> solSet;
        List<Set<Integer>> solutions = new ArrayList<>();
        List<Integer> times = new ArrayList<>();

        for (int i = iQ.nextSetBit(0); i >= 0; i = iQ.nextSetBit(i + 1)) {
            iQ_ = new BitSet();
            iQ_.set(i);
            times.add(i);

            solSet = getSolutionSet(lvg, iQ_, initMetric);
            solutions.add(solSet);
        }

        BitSet iQ_r = new BitSet();

        Set<Integer> union;
        double jaccard;
        DecimalFormat df = new DecimalFormat("#.##");
        df.setRoundingMode(RoundingMode.CEILING);
        int[] p = new int[2];
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

        iQ_r.set(times.get(p[0]));
        iQ_r.set(times.get(p[1]));

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

            assert in != null;
            inter = new HashSet<>(in);
            iQ_r.set(times.get(t));
            checked.add(t);
        }

        return iQ_r;
    }

    private BitSet getKSimilarNewSolution(Graph lvg, BitSet iQ, int k, int initMetric) {
        BitSet iQ_;
        Set<Integer> solSet;
        List<Set<Integer>> solutions = new ArrayList<>();
        List<Integer> times = new ArrayList<>();

        for (int i = iQ.nextSetBit(0); i >= 0; i = iQ.nextSetBit(i + 1)) {
            iQ_ = new BitSet();
            iQ_.set(i);
            times.add(i);
            solSet = Collections.unmodifiableSet(getSolutionSet(lvg, iQ_, initMetric));
            solutions.add(solSet);
        }

        BitSet iQ_r = new BitSet();
        Set<Integer> union;
        double jaccard;
        DecimalFormat df = new DecimalFormat("#.##");
        df.setRoundingMode(RoundingMode.CEILING);
        int[] p = new int[2];
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

        iQ_r.set(times.get(p[0]));
        iQ_r.set(times.get(p[1]));

        if (iQ_r.cardinality() == k)
            return iQ_r;

        Set<Integer> inter = new HashSet<>(getSolutionSet(lvg, iQ_r, initMetric));

        while (iQ_r.cardinality() != k) {
            max = -1;
            int t = -1;

            for (int i = 0; i < solutions.size(); i++) {
                if (iQ_r.get(times.get(i)))
                    continue;

                Set<Integer> a = new HashSet<>(solutions.get(i));
                Set<Integer> c = new HashSet<>(inter);

                c.addAll(a);
                a.retainAll(inter);

                jaccard = Double.parseDouble(df.format((double) a.size() / c.size()));

                if (max < jaccard) {
                    max = jaccard;
                    t = i;
                }
            }

            iQ_r.set(times.get(t));
            inter = new HashSet<>(getSolutionSet(lvg, iQ_r, initMetric));
        }

        return iQ_r;
    }

    private Set<Integer> getSolutionSet(Graph lvg, BitSet iQ, int initMetric) {
        Object algorithm;
        BitSet iQ_ = (BitSet) iQ.clone();
        Set<Integer> solSet;

        Graph lvg_ = Graph.deepClone(lvg);

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
        else
            solSet = ((DCS_Greedy) algorithm).getSolutionSet();

        return solSet;
    }

    /**
     * Incremental Min and Avg set similarity
     */
    private BitSet getKSimilarMinAvgSolution(Graph lvg, BitSet iQ, int k, int initMetric, boolean runMin) {
        BitSet iQ_;
        Object algorithm;
        Graph lvg_;
        Set<Integer> solSet;
        List<Set<Integer>> solutions = new ArrayList<>();
        List<Integer> times = new ArrayList<>();

        for (int i = iQ.nextSetBit(0); i >= 0; i = iQ.nextSetBit(i + 1)) {
            iQ_ = new BitSet();
            iQ_.set(i);
            times.add(i);

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
            else
                solSet = ((DCS_Greedy) algorithm).getSolutionSet();

            solutions.add(solSet);
        }

        Set<Integer> union;
        double jaccard, max = -1;
        DecimalFormat df = new DecimalFormat("#.##");
        df.setRoundingMode(RoundingMode.CEILING);

        double[][] sim = new double[solutions.size()][solutions.size()];
        int[] p = new int[2];

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

        iQ_r.set(times.get(p[0]));
        iQ_r.set(times.get(p[1]));
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

                iQ_r.set(times.get(t));
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

                iQ_r.set(times.get(t));
                all.remove(t);
                checked.add(t);
            }
        }

        return iQ_r;
    }

    /**
     * Incremental k densest solution
     */
    private BitSet getKSimilarDensSolution(Graph lvg, BitSet iQ, int k, int initMetric) {
        BitSet iQ_;
        Object algorithm;
        Graph lvg_;

        double[][] sim = new double[iQ.cardinality()][iQ.cardinality()];
        int[] p = new int[2];
        double max = -1;
        List<Integer> times = new ArrayList<>();

        for (int i = iQ.nextSetBit(0); i >= 0; i = iQ.nextSetBit(i + 1))
            times.add(i);

        for (int i = 0; i < times.size(); i++) {
            iQ_ = new BitSet();
            iQ_.set(times.get(i));

            for (int j = i + 1; j < times.size(); j++) {
                iQ_.set(times.get(j));

                lvg_ = Graph.deepClone(lvg);

                if (initMetric == Config.DCS)
                    algorithm = new DCS_Greedy(lvg_, iQ_, Collections.emptySet());
                else if (initMetric == Config.TMA || initMetric == Config.TAM)
                    algorithm = new BFF_Greedy(lvg_, iQ_, initMetric, Collections.emptySet());
                else
                    algorithm = new BFF(lvg_, iQ_, initMetric, Collections.emptySet());

                assert algorithm instanceof BFF;
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

        iQ_r.set(times.get(p[0]));
        iQ_r.set(times.get(p[1]));
        checked.add(p[0]);
        checked.add(p[1]);

        if (iQ_r.cardinality() == k)
            return iQ_r;

        for (int i = 0; i < times.size(); i++) {
            if (!checked.contains(i))
                all.add(i);
        }

        while (iQ_r.cardinality() != k) {
            max = -1;
            int t = -1;
            double s;

            for (int j : all) {
                iQ_ = (BitSet) iQ_r.clone();
                iQ_.set(times.get(j));

                lvg_ = Graph.deepClone(lvg);

                if (initMetric == Config.DCS)
                    algorithm = new DCS_Greedy(lvg_, iQ_, Collections.emptySet());
                else if (initMetric == Config.TMA || initMetric == Config.TAM)
                    algorithm = new BFF_Greedy(lvg_, iQ_, initMetric, Collections.emptySet());
                else
                    algorithm = new BFF(lvg_, iQ_, initMetric, Collections.emptySet());

                assert algorithm instanceof BFF;
                s = ((BFF) algorithm).getSolutionDensity();

                if (max < s) {
                    max = s;
                    t = j;
                }

            }

            iQ_r.set(times.get(t));
            checked.add(t);
            all.remove(t);
        }

        return iQ_r;
    }

    /**
     * Incremental Min and Avg density similarity
     */
    private BitSet getKSimilarDensMinAvgSolution(Graph lvg, BitSet iQ, int k, int initMetric, boolean runMin) {
        BitSet iQ_;
        Object algorithm;
        Graph lvg_;

        double[][] sim = new double[iQ.cardinality()][iQ.cardinality()];
        int[] p = new int[2];
        double max = -1;
        List<Integer> times = new ArrayList<>();

        for (int i = iQ.nextSetBit(0); i >= 0; i = iQ.nextSetBit(i + 1))
            times.add(i);

        for (int i = 0; i < times.size(); i++) {
            iQ_ = new BitSet();
            iQ_.set(times.get(i));

            for (int j = i + 1; j < times.size(); j++) {
                iQ_.set(times.get(j));

                lvg_ = Graph.deepClone(lvg);

                if (initMetric == Config.DCS)
                    algorithm = new DCS_Greedy(lvg_, iQ_, Collections.emptySet());
                else if (initMetric == Config.TMA || initMetric == Config.TAM)
                    algorithm = new BFF_Greedy(lvg_, iQ_, initMetric, Collections.emptySet());
                else
                    algorithm = new BFF(lvg_, iQ_, initMetric, Collections.emptySet());

                assert algorithm instanceof BFF;
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

        iQ_r.set(times.get(p[0]));
        iQ_r.set(times.get(p[1]));
        checked.add(p[0]);
        checked.add(p[1]);

        if (iQ_r.cardinality() == k)
            return iQ_r;

        for (int i = 0; i < times.size(); i++) {
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

                iQ_r.set(times.get(t));
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

                iQ_r.set(times.get(t));
                all.remove(t);
                checked.add(t);
            }
        }

        return iQ_r;
    }

    /**
     * Return a callable object
     */
    private Callable<?> setCallable(BitSet iQ, String dataset) {
        return () -> {
            int k = (int) (Math.round((iQ.cardinality() * 0.2)));
            runMetrics(dataset, iQ, k, 4 * k, k);
            return true;
        };
    }
}