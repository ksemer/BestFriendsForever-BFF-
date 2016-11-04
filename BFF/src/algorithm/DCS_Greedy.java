package algorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import system.Config;
import vg.Graph;
import vg.Node;

/**
 * DCS_Greedy class Improved version of the algorithm Finding Dense Subgraphs in
 * Relational Graphs PKDE 2015
 * 
 * @author ksemer
 *
 */
public class DCS_Greedy {
	private Graph lvg;

	private BitSet iQ;

	private Set<Integer> S;
	
	private Set<Integer> seedNodes;

	private int instanceWithMinimum;
	
	private Map<Double, List<Integer>> maxScoreStep;

	private Map<Integer, Counter> numberOfEdgesPerTimeInstant;

	private Map<Node, Map<Integer, Counter>> nodesScorePerTimeInstant;

	private Map<Integer, TreeMap<Integer, Set<Node>>> perTimeInstantScore;

	/**
	 * Constructor
	 * 
	 * @param lvg
	 * @param iQ
	 * @param seedNodes 
	 */
	public DCS_Greedy(Graph lvg, BitSet iQ, Set<Integer> seedNodes) {
		this.lvg = lvg;
		this.iQ = iQ;
		this.S = new HashSet<>();
		this.maxScoreStep = new TreeMap<>();
		this.seedNodes = seedNodes;
			
		graphCleaning();
		
		if (this.lvg.isEmpty()) {
			System.out.println("There is not any solution for Metric: " + Config.DCS + " ,Interval: " + iQ);
			return;
		}
		
		boolean executeWithSeeds = false;

		// check if seedNodes exist in lvg
		for (int id : seedNodes) {
			if (lvg.getNode(id) == null) {
				executeWithSeeds = true;
				break;
			}
		}

		if (!executeWithSeeds) {	
			// run DCS greedy algorithm
			runDCS();
		}

		if (!maxScoreStep.isEmpty()) {
			int step = maxScoreStep.entrySet().iterator().next().getValue().get(0);
			
			for (Node n : lvg.getNodes()) {
				// n.getRemovalStep() == 0 for the last node which is not removed
				if (n.getRemovalStep() > step || n.getRemovalStep() == 0 || step == 0) {
					// add node id in solution set
					S.add(n.getID());
				}
			}
		}
	}

	/**
	 * Run greedy algorithm
	 */
	private void runDCS() {
		Set<Node> V = new HashSet<>(lvg.getNodes()), set;
		Node n = null;
		double minAD, maxScore;
		Counter c;
		int step = 0, val;

		while (Double.compare((minAD = minAverageDegree(V)), 0) > 0) {
			// System.out.println(step + "\t" + minAD + "\t" + V.size());

			if (!maxScoreStep.isEmpty()) {
				if ((maxScore = maxScoreStep.entrySet().iterator().next().getKey()) <= minAD) {
					if (maxScore < minAD) {
						maxScoreStep.clear();
						maxScoreStep.put(minAD, new ArrayList<>(Arrays.asList(step)));
					} else if (maxScore == minAD) {
						maxScoreStep.get(minAD).add(step);
					}
				}
			} else {
				maxScoreStep.put(minAD, new ArrayList<>(Arrays.asList(step)));
			}

			step++;
			n = getMinimumDegreeNode();
			n.setRemovalStep(step);
			V.remove(n);
			
			// if there are seed nodes and seed node is the node with the minimum score
			if (seedNodes.contains(n.getID())) {
				for (Node n1 : V)
					if (n1.getRemovalStep() == 0)
						n1.setRemovalStep(step);
				return;
			}

			Set<Integer> times = new HashSet<>();

			// for all edges of node n update their degrees in all snapshots
			for (Iterator<Entry<Node, BitSet>> edge = n.getAdjacencyAsMap().entrySet().iterator(); edge.hasNext();) {
				Entry<Node, BitSet> entry = edge.next();

				Node trg = entry.getKey();
				BitSet lifespan = entry.getValue();

				// this trg node has been checked and removed
				if (trg.getRemovalStep() != 0) {
					for (Iterator<Integer> i = lifespan.stream().iterator(); i.hasNext();)
						times.add(i.next());
					continue;
				}

				Map<Integer, Counter> nspti = nodesScorePerTimeInstant.get(trg);

				for (Iterator<Integer> i = lifespan.stream().iterator(); i.hasNext();) {
					int t = i.next();
					numberOfEdgesPerTimeInstant.get(t).decrease();
					times.add(t);

					c = nspti.get(t);
					val = c.getValue();

					// Decrease c for time instant iff edge is active at that t
					// and c is not 0
					if (c.getValue() > 0) {

						// update the structure that keeps per t -> score ->
						// nodes
						perTimeInstantScore.get(t).get(val).remove(trg);

						if (perTimeInstantScore.get(t).get(val).isEmpty())
							perTimeInstantScore.get(t).remove(val);

						c.decrease();
						val = c.getValue();

						// update structure that keeps per t -> score -> nodes
						if ((set = perTimeInstantScore.get(t).get(val)) == null) {
							set = new HashSet<>();
							perTimeInstantScore.get(t).put(val, set);
						}
						set.add(trg);
					}
				}
			}
		}
	}

	/**
	 * Return node with the minimum degree in the sparsest snapshot
	 * 
	 * @return
	 */
	private Node getMinimumDegreeNode() {
		int c = perTimeInstantScore.get(instanceWithMinimum).firstKey();
		Node minNode = perTimeInstantScore.get(instanceWithMinimum).get(c).iterator().next();
		Map<Integer, Counter> nspti = nodesScorePerTimeInstant.get(minNode);

		for (Iterator<Integer> i = iQ.stream().iterator(); i.hasNext();) {
			int t = i.next();

			perTimeInstantScore.get(t).get(nspti.get(t).getValue()).remove(minNode);

			if (perTimeInstantScore.get(t).get(nspti.get(t).getValue()).isEmpty())
				perTimeInstantScore.get(t).remove(nspti.get(t).getValue());
		}

		nodesScorePerTimeInstant.remove(minNode);

		return minNode;
	}

	/**
	 * Computes the average degree of the sparsest snapshot
	 * 
	 * @return
	 */
	private double minAverageDegree(Set<Node> V) {
		double averageDegree = 0.0, minAverageDegree = Double.MAX_VALUE;
		int t;

		// for each time instant in iQ
		for (Iterator<Integer> i = iQ.stream().iterator(); i.hasNext();) {
			// time instant
			t = i.next();

			averageDegree = (double) numberOfEdgesPerTimeInstant.get(t).getValue() / V.size();
			averageDegree = Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", averageDegree));

			if (minAverageDegree > averageDegree) {
				minAverageDegree = averageDegree;
				instanceWithMinimum = t;
			}
		}

		return minAverageDegree;
	}

	/**
	 * Clean graph based on lifespan of edges
	 */
	private void graphCleaning() {
		Set<Node> set;
		int t, val;
		numberOfEdgesPerTimeInstant = new HashMap<>(iQ.cardinality());
		perTimeInstantScore = new HashMap<>(iQ.cardinality());
		nodesScorePerTimeInstant = new HashMap<>(lvg.size());

		for (Iterator<Integer> i = iQ.stream().iterator(); i.hasNext();) {
			t = i.next();
			numberOfEdgesPerTimeInstant.put(t, new Counter());
			perTimeInstantScore.put(t, new TreeMap<>());
		}

		// for all nodes
		for (Iterator<Node> it = lvg.getNodes().iterator(); it.hasNext();) {
			// get node n
			Node n = it.next();

			// keeps the score size per time instant
			Map<Integer, Counter> scorePerTimeInstant = new HashMap<>(iQ.cardinality());

			// initialize
			for (Iterator<Integer> i = iQ.stream().iterator(); i.hasNext();)
				scorePerTimeInstant.put(i.next(), new Counter());

			Set<Integer> times = new HashSet<>();

			// for all edges of node n
			for (Iterator<Entry<Node, BitSet>> edge = n.getAdjacencyAsMap().entrySet().iterator(); edge.hasNext();) {

				Entry<Node, BitSet> entry = edge.next();
				BitSet lifespan = entry.getValue();

				lifespan.and(iQ);

				// remove not active edge
				if (lifespan.isEmpty()) {
					edge.remove();
					continue;
				}

				// increase the degree per time instant
				for (Iterator<Integer> i = lifespan.stream().iterator(); i.hasNext();) {
					t = i.next();

					numberOfEdgesPerTimeInstant.get(t).increase();
					scorePerTimeInstant.get(t).increase();
					times.add(t);
				}
			}

			if (n.getAdjacency().size() == 0) {
				it.remove();
			} else {
				// add nodes n score per time instant info into structure
				nodesScorePerTimeInstant.put(n, scorePerTimeInstant);

				for (Iterator<Integer> i = iQ.stream().iterator(); i.hasNext();) {
					t = i.next();
					val = scorePerTimeInstant.get(t).getValue();

					if ((set = perTimeInstantScore.get(t).get(val)) == null) {
						set = new HashSet<>();
						perTimeInstantScore.get(t).put(val, set);
					}
					set.add(n);
				}
			}
		}

		// correct the degree per time instant
		for (Iterator<Integer> i = iQ.stream().iterator(); i.hasNext();) {
			t = i.next();
			numberOfEdgesPerTimeInstant.get(t).set(numberOfEdgesPerTimeInstant.get(t).getValue() / 2);
		}
	}

	/**
	 * Return solution set
	 * 
	 * @return
	 */
	public Set<Integer> getSolutionSet() {
		return S;
	}

	/**
	 * Return solutions' density
	 * 
	 * @return
	 */
	public double getSolutionDensity() {
		if (maxScoreStep.isEmpty())
			return 0;
		else
			return maxScoreStep.entrySet().iterator().next().getKey() * 2;
	}
}