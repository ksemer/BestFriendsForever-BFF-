package experiments;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import algorithm.BFF;
import algorithm.BFF_Greedy;
import algorithm.DCS_Greedy;
import system.Config;
import vg.Graph;
import vg.LoadGraph;

/**
 * Problem1 Experiments class
 *
 * @author ksemer
 */
public class BFF_Problem {

    // seed nodes that will be part of BFF's solution
    private Set<Integer> seedNodes;

    private static final Logger _log = Logger.getLogger(BFF_Problem.class.getName());

    /**
     * Constructor
     */
    public BFF_Problem() {
        // initialize Callables
        // stores all callable objects
        List<Callable<?>> callables = new ArrayList<>();
        for (int i = 0; i < Config.DATASETS.size(); i++) {
            callables.add(setCallable(Config.INTERVALS.get(i), Config.DATASETS.get(i)));
        }

        seedNodes = new HashSet<>(Config.SEED_NODES);

        ExecutorService executor = Executors.newCachedThreadPool();
        for (Callable<?> c : callables)
            executor.submit(c);
        executor.shutdown();
    }

    /**
     * Run each metric in a thread
     */
    private void runMetrics(String dataset, BitSet iQ) {
        Callable<?> c1, c2, c3;
        ExecutorService executor = Executors.newCachedThreadPool();

        c1 = () -> {
            if (Config.RUN_DCS)
                run(iQ, Config.DCS, dataset);
            return true;
        };

        c2 = () -> {
            if (Config.RUN_FIND_GREEDY)
                IntStream.range(5, 7).parallel().forEach(metric -> run(iQ, metric, dataset));
            return true;
        };

        c3 = () -> {
            if (Config.RUN_FIND_BFF) {
                IntStream.range(1, 5).parallel().forEach(metric -> run(iQ, metric, dataset));
                IntStream.range(7, 9).parallel().forEach(metric -> run(iQ, metric, dataset));
            }
            return true;
        };

        executor.submit(c1);
        executor.submit(c2);
        executor.submit(c3);
        executor.shutdown();
    }

    /**
     * Run method which calls the algorithm
     */
    private void run(BitSet iQ, int metric, String dataset) {

        String datasetPath = Config.DATA_PATH + dataset;
        String outputPath = Config.OUTPUT_PATH + dataset;

        if (dataset.toLowerCase().contains("dblp"))
            outputPath = Config.OUTPUT_PATH + "dblp" + iQ.cardinality();

        _log.log(Level.INFO, "(metric, dataset)->" + "(" + metric + ", " + dataset + ")");

        try {
            Graph lvg = LoadGraph.loadDataset(iQ, datasetPath);
            Object algorithm;
            Set<Integer> S;
            MetricScore st = new MetricScore();
            long time = System.currentTimeMillis();
            FileWriter stats = new FileWriter(outputPath + "_" + metric + ".txt");

            if (metric == Config.DCS)
                algorithm = new DCS_Greedy(lvg, iQ, seedNodes);
            else if (metric == Config.TMA || metric == Config.TAM)
                algorithm = new BFF_Greedy(lvg, iQ, metric, seedNodes);
            else
                algorithm = new BFF(lvg, iQ, metric, seedNodes);

            // retrieve solution set S
            if (algorithm instanceof BFF)
                S = ((BFF) algorithm).getSolutionSet();
            else if (algorithm instanceof BFF_Greedy)
                S = ((BFF_Greedy) algorithm).getSolutionSet();
            else
                S = ((DCS_Greedy) algorithm).getSolutionSet();

            if (S.isEmpty()) {
                stats.write("There is not any solution for Metric: " + metric + " ,Interval: " + iQ);
                stats.write("\nTotal time: " + (System.currentTimeMillis() - time) + " (ms)");
                stats.close();
                return;
            }

            if (metric == Config.MM || metric == Config.AM || metric == Config.TAM || metric == Config.MAM)
                st.writeMD(lvg, stats, iQ, S, metric);
            else
                st.writeAD(lvg, stats, iQ, S, metric);

            stats.write("\nTotal time: " + (System.currentTimeMillis() - time) + " (ms)");
            stats.write("\n\n======Nodes======\n");

            // write authors
            for (int id : S) {
                if (datasetPath.toLowerCase().contains("dblp"))
                    stats.write(id + "\t" + LoadGraph.getAuthor(id) + "\n");
                else if (dataset.toLowerCase().contains("twitter"))
                    stats.write(id + "\t" + LoadGraph.getHashtag(id) + "\n");
                else
                    stats.write(id + "\n");
            }

            stats.close();
        } catch (IOException e) {
            _log.log(Level.SEVERE, "", e);
        }
    }

    /**
     * Return a callable object
     */
    private Callable<?> setCallable(BitSet iQ, String dataset) {
        return () -> {
            runMetrics(dataset, iQ);
            return true;
        };
    }
}