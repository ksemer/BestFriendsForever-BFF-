package experiments;

import java.io.FileWriter;
import java.io.IOException;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import system.Config;
import vg.Graph;
import vg.Node;

/**
 * MetricScore class Responsible for writing statistics for BFF and O^2BFF
 * problems
 *
 * @author ksemer
 */
public class MetricScore {
    // used to return the metric score for the O^2 problem
    private double o2_score;

    /**
     * Write score for each time instant for minimum degree algorithms
     */
    public void writeMD(Graph lvg, FileWriter stats, BitSet iQ, Set<Integer> S, int metric) throws IOException {
        int minDegree, mMinDegree = Integer.MAX_VALUE;
        double sumMinDegree = 0;

        // for each time instant in iQ
        for (Iterator<Integer> i = iQ.stream().iterator(); i.hasNext(); ) {
            // time instant
            int t = i.next();

            // minimum degree of induced subgraph S in t
            minDegree = Integer.MAX_VALUE;

            for (Integer id : S) {
                // degree per node
                int degree = 0;

                Node n = lvg.getNode(id);

                // for all adjacencies
                for (Entry<Node, BitSet> entry : n.getAdjacencyAsMap().entrySet()) {
                    // trg node
                    Node trg = entry.getKey();

                    // check only the induced subgraph S
                    if (S.contains(trg.getID())) {
                        BitSet lifespan = entry.getValue();

                        if (lifespan.get(t))
                            degree++;
                    }
                }

                if (minDegree > degree)
                    minDegree = degree;
            }

            if (mMinDegree > minDegree)
                mMinDegree = minDegree;

            stats.write("Minimum Degree: " + minDegree + " in time instant: " + t + "\n");
            sumMinDegree += minDegree;
        }
        stats.write("Size of S: " + S.size() + "\n");

        if (metric == Config.AM || metric == Config.TAM || metric == Config.MAM) {
            sumMinDegree = sumMinDegree / iQ.cardinality();
            sumMinDegree = Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", sumMinDegree));
            stats.write("Score: " + sumMinDegree + "\nInterval: " + iQ);
        } else
            stats.write("Score: " + mMinDegree + "\nInterval: " + iQ);
    }

    /**
     * Write score for each time instant for average degree algorithms
     */
    public void writeAD(Graph lvg, FileWriter stats, BitSet iQ, Set<Integer> S, int metric) throws IOException {
        double degreePerTime, averageDegree = 0, minAverageDegree = Double.MAX_VALUE;

        // for each time instant in iQ
        for (Iterator<Integer> i = iQ.stream().iterator(); i.hasNext(); ) {
            // time instant
            int t = i.next();

            degreePerTime = 0;

            for (Integer id : S) {

                Node n = lvg.getNode(id);

                // for all adjacencies
                for (Entry<Node, BitSet> entry : n.getAdjacencyAsMap().entrySet()) {
                    // trg node
                    Node trg = entry.getKey();

                    // check only the induced subgraph S
                    if (S.contains(trg.getID())) {
                        BitSet lifespan = entry.getValue();

                        if (lifespan.get(t))
                            degreePerTime++;
                    }
                }
            }

            if (minAverageDegree > degreePerTime)
                minAverageDegree = degreePerTime;

            averageDegree += degreePerTime;

            stats.write("Average Degree: "
                    + Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", (degreePerTime / S.size())))
                    + " in time instant: " + t + "\n");
        }
        stats.write("Size of S: " + S.size() + "\n");

        if (metric == Config.AA) {
            averageDegree = averageDegree / S.size() / iQ.cardinality();
            stats.write("Score: " + Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", averageDegree))
                    + "\nInterval:" + iQ);
        } else {
            stats.write(
                    "Score: " + Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", (minAverageDegree / S.size())))
                            + "\nInterval:" + iQ);
        }
    }

    /**
     * Get k best times for average degree algorithms
     */
    public BitSet getKTimesAD(Graph lvg, FileWriter stats, BitSet iQ, Set<Integer> S, int k, int metric)
            throws IOException {
        // minAvg value -> set of time instants with the average degree
        TreeMap<Double, Set<Integer>> minAvgTimeInstants = new TreeMap<>();

        // the return bit set which contains the k time instants
        BitSet kTimes = new BitSet(iQ.cardinality());

        // for each time instant in iQ
        for (Iterator<Integer> i = iQ.stream().iterator(); i.hasNext(); ) {
            // time instant
            int t = i.next();

            double degreePerTime = 0;

            for (Integer id : S) {

                Node n = lvg.getNode(id);

                // for all adjacencies
                for (Entry<Node, BitSet> entry : n.getAdjacencyAsMap().entrySet()) {
                    // trg node
                    Node trg = entry.getKey();

                    // check only the induced subgraph S
                    if (S.contains(trg.getID())) {
                        BitSet lifespan = entry.getValue();

                        if (lifespan.get(t))
                            degreePerTime++;
                    }
                }
            }

            // average degree
            degreePerTime = degreePerTime / S.size();

            // Locale.ENGLISH to avoid problem in double values with dot or
            // comma
            degreePerTime = Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", degreePerTime));

            stats.write("Average Degree: " + degreePerTime + " in time instant: " + t + "\n");

            Set<Integer> setOfTimes;

            if ((setOfTimes = minAvgTimeInstants.get(degreePerTime)) == null) {
                setOfTimes = new HashSet<>();
                minAvgTimeInstants.put(degreePerTime, setOfTimes);
            }

            setOfTimes.add(t);
        }

        final boolean m = metric == Config.MA || metric == Config.AMA || metric == Config.TMA || metric == Config.DCS;
        if (m) o2_score = Double.MAX_VALUE;
        else
            o2_score = 0;

        // find the best k time instants and compute the metric score for them
        for (Entry<Double, Set<Integer>> entry : minAvgTimeInstants.descendingMap().entrySet()) {
            Set<Integer> times = entry.getValue();

            if (o2_score > entry.getKey()
                    && (m))
                o2_score = entry.getKey();

            for (int t : times) {
                if (metric == Config.AA)
                    o2_score += entry.getKey();

                kTimes.set(t);

                if (kTimes.cardinality() == k) {
                    if (metric == Config.AA)
                        o2_score /= kTimes.cardinality();

                    o2_score = Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", o2_score));

                    return kTimes;
                }
            }
        }

        if (metric == Config.AA)
            o2_score /= kTimes.cardinality();

        o2_score = Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", o2_score));

        return kTimes;
    }

    /**
     * Get k best times for minimum degree algorithms
     */
    public BitSet getKTimesMD(Graph lvg, FileWriter stats, BitSet iQ, Set<Integer> S, int k, int metric)
            throws IOException {

        // minDegree value -> set of time instants with this min degree
        TreeMap<Integer, Set<Integer>> minDegreeTimeInstants = new TreeMap<>();

        // the return bit set which contains the k time instants
        BitSet kTimes = new BitSet(iQ.size());

        o2_score = 0;

        // for each time instant in iQ
        for (Iterator<Integer> i = iQ.stream().iterator(); i.hasNext(); ) {
            // time instant
            int t = i.next();

            // minimum degree of induced subgraph S in t
            int minDegree = Integer.MAX_VALUE;

            for (Integer id : S) {
                // degree per node
                int degree = 0;

                Node n = lvg.getNode(id);

                // for all adjacencies
                for (Entry<Node, BitSet> entry : n.getAdjacencyAsMap().entrySet()) {
                    // trg node
                    Node trg = entry.getKey();

                    // check only the induced subgraph S
                    if (S.contains(trg.getID())) {
                        BitSet lifespan = entry.getValue();

                        if (lifespan.get(t))
                            degree++;
                    }
                }

                if (minDegree > degree)
                    minDegree = degree;
            }

            stats.write("Minimum Degree: " + minDegree + " in time instant: " + t + "\n");

            Set<Integer> setOfTimes;

            if ((setOfTimes = minDegreeTimeInstants.get(minDegree)) == null) {
                setOfTimes = new HashSet<>();
                minDegreeTimeInstants.put(minDegree, setOfTimes);
            }

            setOfTimes.add(t);
        }

        if (metric == Config.MM)
            o2_score = Double.MAX_VALUE;
        else
            o2_score = 0;

        // find the best k time instants and compute the metric score for them
        for (Entry<Integer, Set<Integer>> entry : minDegreeTimeInstants.descendingMap().entrySet()) {
            Set<Integer> times = entry.getValue();

            if (metric == Config.MM && o2_score > entry.getKey())
                o2_score = entry.getKey();

            for (int t : times) {
                if (metric == Config.AM || metric == Config.MAM || metric == Config.TAM)
                    o2_score += entry.getKey();

                kTimes.set(t);

                if (kTimes.cardinality() == k) {
                    if (metric == Config.AM || metric == Config.MAM || metric == Config.TAM)
                        o2_score /= kTimes.cardinality();

                    o2_score = Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", o2_score));

                    return kTimes;
                }
            }
        }

        if (metric == Config.AM || metric == Config.MAM || metric == Config.TAM)
            o2_score /= kTimes.cardinality();

        o2_score = Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", o2_score));

        return kTimes;

    }

    /**
     * Return metric score for O^2 problem
     */
    public double getScore() {
        return o2_score;
    }
}