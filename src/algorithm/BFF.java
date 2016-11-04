package algorithm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;

import system.Config;
import vg.Graph;
import vg.Node;

/**
 * DurableDenseGraph class
 * 
 * @author ksemer
 */
public class BFF {

	private Graph lvg;

	private BitSet iQ;

	// solution S
	private Set<Integer> S;

	private int metric, step;

	// Used for minimum average metric
	private int numberOfNodes;

	private Set<Integer> seedNodes;

	// For each node its min score for MM and AM and the average score for AA
	// and MA
	private Map<Node, Double> nodesMinScore;

	private TreeMap<Double, Set<Node>> timeScoreIndex;

	private Map<Integer, Counter> numberOfEdgesPerTimeInstant;

	private Map<Double, List<Integer>> stepsOfMaximumScore;

	// keeps for each node the degree per time instant
	private Map<Node, Map<Integer, Counter>> nodesScorePerTimeInstant;

	private Map<Integer, TreeMap<Integer, Set<Node>>> perTimeInstantScore;

	/**
	 * Abstract Constructor
	 * 
	 * @param iQ
	 * @param lvg
	 * @param metric
	 * @param seedNodes
	 * @param connectivity
	 * @throws IOException
	 * @throws Exception
	 */
	public BFF(Graph lvg, BitSet iQ, int metric, Set<Integer> seedNodes) {
		// initialization
		this.timeScoreIndex = new TreeMap<>();
		this.stepsOfMaximumScore = new TreeMap<>();
		this.S = new HashSet<>();
		this.metric = metric;
		this.lvg = lvg;
		this.iQ = iQ;
		this.seedNodes = seedNodes;

		Set<Node> conn = new HashSet<>();

		// check if seedNodes exist in lvg
		for (int id : seedNodes) {
			if (lvg.getNode(id) == null) {
				System.out.println("There is not any solution for Metric: " + metric + " ,Interval: " + iQ
						+ " ,seedNodes: " + seedNodes);
				return;
			}
		}

		createDegreeIndex();

		if (this.lvg.isEmpty()) {
			System.out.println("There is not any solution for Metric: " + metric + " ,Interval: " + iQ);
			return;
		}

		initialScore();

		// run Charikar algorithm
		runCharikar();

		if (!stepsOfMaximumScore.isEmpty()) {
			int step = stepsOfMaximumScore.entrySet().iterator().next().getValue().get(0);

			for (Node n : lvg.getNodes()) {
				// add node id in solution set
				if (n.getRemovalStep() > step || step == 0) {
					S.add(n.getID());
					conn.add(n);
				}
			}
		}

		if (!seedNodes.isEmpty()) {
			WQuickUnionPC wqup = new WQuickUnionPC(conn);
			List<Node> seedN = new ArrayList<>();

			for (int n : seedNodes)
				seedN.add(lvg.getNode(n));

			int seedNode = seedNodes.iterator().next();
			int componentID = wqup.getComponentID(lvg.getNode(seedNode));

			for (Node n : conn) {
				if (wqup.getComponentID(n) != componentID)
					S.remove(n.getID());
			}
		}
	}

	/**
	 * RunCharikar algorithm
	 * 
	 * @throws IOException
	 */
	private void runCharikar() {

		// while there are nodes to be examined
		while (timeScoreIndex.size() != 0) {
			step++;

			// get minimum score at this step
			double minScore = timeScoreIndex.firstKey();

			// get one node with min score
			Node n = timeScoreIndex.get(minScore).iterator().next();

			// if there are seed nodes and seed node is the node with the
			// minimum score
			if (seedNodes.contains(n.getID())) {

				for (Node n1 : timeScoreIndex.get(minScore)) {
					if (!seedNodes.contains(n1.getID())) {
						n = n1;
						break;
					}
				}

				if (seedNodes.contains(n.getID())) {
					for (Node n1 : lvg.getNodes())
						if (n1.getRemovalStep() == 0)
							n1.setRemovalStep(step);
					return;
				}
			}

			// for all edges of node n
			for (Iterator<Entry<Node, BitSet>> edge = n.getAdjacencyAsMap().entrySet().iterator(); edge.hasNext();) {

				Entry<Node, BitSet> entry = edge.next();
				Node trg = entry.getKey();

				// this trg node has been checked and removed
				if (trg.getRemovalStep() != 0)
					continue;

				// its the trg's old score
				double oldTrgScore = getScore(trg);

				// update time score index structure by removing trg from its
				// old position
				timeScoreIndex.get(oldTrgScore).remove(trg);

				// if trg's old set position is empty, remove it
				if (timeScoreIndex.get(oldTrgScore).size() == 0)
					timeScoreIndex.remove(oldTrgScore);

				// update trg's edge lifespan by removing the alive points of
				// the deleted edge
				updateEdges(trg, entry.getValue());

				// get trg current score
				double trgScore = getScore(trg);

				Set<Node> set;

				// move trg to the correct position
				if ((set = timeScoreIndex.get(trgScore)) == null) {
					set = new HashSet<>();
					timeScoreIndex.put(trgScore, set);
				}

				// add trg to the new position
				set.add(trg);
			}

			// set in which step n has been removed
			n.setRemovalStep(step);

			// remove node n
			timeScoreIndex.get(minScore).remove(n);

			// if n's old set position is empty, remove it
			if (timeScoreIndex.get(minScore).size() == 0)
				timeScoreIndex.remove(minScore);

			// from now on we use minScore for the metric score

			// For metric minimum average and average average
			if (metric == Config.MA || metric == Config.AA || metric == Config.AMA) {
				numberOfNodes--;

				// in order to not divide by zero
				if (numberOfNodes == 0)
					minScore = 0;
				else if (metric == Config.MA || metric == Config.AMA) {
					minScore = Double.MAX_VALUE;

					// for each time instant of iQ check the average degree and
					// store the minimum
					for (Iterator<Integer> i = iQ.stream().iterator(); i.hasNext();) {
						int numOfE = numberOfEdgesPerTimeInstant.get(i.next()).getValue();

						// compute average degree
						double density = numOfE / (double) numberOfNodes;

						if (density < minScore)
							minScore = density;
					}
				} else if (metric == Config.AA) {
					minScore = 0;

					// for each time instant check the average degree and store
					// the sum
					for (Iterator<Integer> i = iQ.stream().iterator(); i.hasNext();) {
						int numOfE = numberOfEdgesPerTimeInstant.get(i.next()).getValue();

						minScore += numOfE / (double) numberOfNodes;
					}

					// average degree over time
					minScore /= iQ.cardinality();
				}
			} else {
				// if not all nodes have been examined, compute the next MM
				// score
				if (lvg.size() != step)
					minScore = timeScoreIndex.firstKey();
			}

			if (metric == Config.AM || metric == Config.MAM) {
				Map<Integer, Counter> nspti = nodesScorePerTimeInstant.get(n);

				for (Iterator<Integer> i = iQ.stream().iterator(); i.hasNext();) {
					int t = i.next();

					perTimeInstantScore.get(t).get(nspti.get(t).getValue()).remove(n);

					if (perTimeInstantScore.get(t).get(nspti.get(t).getValue()).isEmpty())
						perTimeInstantScore.get(t).remove(nspti.get(t).getValue());
				}
				nodesScorePerTimeInstant.remove(n);

				// all nodes have been examined
				if (lvg.size() == step)
					minScore = 0;
				else // add average minimum as minScore
					minScore = computeMinPerTime();
			}

			if (!seedNodes.isEmpty() && !isConnected()) {
				System.out.println(n.getID() + "\t" + "LLLLLLL " + step + "\t" + seedNodes);
				return;
			}

			double maxScore;
			if (!stepsOfMaximumScore.isEmpty()) {
				if ((maxScore = stepsOfMaximumScore.entrySet().iterator().next().getKey()) <= minScore) {
					if (maxScore < minScore) {
						stepsOfMaximumScore.clear();
						stepsOfMaximumScore.put(minScore, new ArrayList<>(Arrays.asList(step)));
					} else if (maxScore == minScore) {
						stepsOfMaximumScore.get(minScore).add(step);
					}
				}
			} else
				stepsOfMaximumScore.put(minScore, new ArrayList<>(Arrays.asList(step)));
		}
	}

	/**
	 * Update score of node n which is a neighbor of a node in charikar method
	 * 
	 * @param n
	 * @param lifespan
	 */
	private void updateEdges(Node n, BitSet lifespan) {
		Map<Integer, Counter> nspti = nodesScorePerTimeInstant.get(n);
		Counter c;
		Set<Node> set;
		int val, t;

		// new score
		double newScore = Double.MAX_VALUE, degreeOverTime = 0;

		// intersect edge lifespan with given iQ
		BitSet l = (BitSet) iQ.clone();

		// update score per time instant structure
		for (Iterator<Integer> i = l.stream().iterator(); i.hasNext();) {
			t = i.next();

			c = nspti.get(t);
			val = c.getValue();

			if ((metric == Config.MA || metric == Config.AA || metric == Config.AMA) & lifespan.get(t))
				numberOfEdgesPerTimeInstant.get(t).decrease();

			// Decrease c for time instant iff edge is active at that t and c is
			// not 0
			if (c.getValue() > 0 && lifespan.get(t)) {

				// for AM update the structure that keeps per t -> score ->
				// nodes
				if (metric == Config.AM || metric == Config.MAM) {
					perTimeInstantScore.get(t).get(val).remove(n);

					if (perTimeInstantScore.get(t).get(val).isEmpty())
						perTimeInstantScore.get(t).remove(val);
				}

				c.decrease();
				val = c.getValue();

				// for AM update the structure that keeps per t -> score ->
				// nodes
				if (metric == Config.AM || metric == Config.MAM) {
					if ((set = perTimeInstantScore.get(t).get(val)) == null) {
						set = new HashSet<>();
						perTimeInstantScore.get(t).put(val, set);
					}
					set.add(n);
				}
			}

			degreeOverTime += val;

			// update min score for MM, MA, and MAM
			if (newScore > val)
				newScore = val;
		}

		// Score for AMA and AA is the sum of all degrees divided by iQ
		// cardinality
		if (metric == Config.AA || metric == Config.AMA || metric == Config.AM)
			newScore = degreeOverTime / iQ.cardinality();

		if ((set = timeScoreIndex.get(newScore)) == null) {
			set = new HashSet<>();
			timeScoreIndex.put(newScore, set);
		}

		set.add(n);

		nodesMinScore.put(n, newScore);
	}

	/**
	 * Create the Degree Index
	 */
	private void createDegreeIndex() {
		nodesMinScore = new HashMap<>();
		graphCleaning();

		// for all remaining nodes
		for (Node n : lvg.getNodes()) {

			// minimum or average score, dependent on metric
			double min;

			if (metric == Config.AA || metric == Config.AM || metric == Config.AMA) {
				min = 0;
				for (Counter c : nodesScorePerTimeInstant.get(n).values())
					min += c.getValue();

				min = min / iQ.cardinality();
			} else { // for MM, MA and MAM
				min = Double.MAX_VALUE;

				for (Counter c : nodesScorePerTimeInstant.get(n).values()) {
					if (c.getValue() < min) {
						min = c.getValue();
					}
				}
			}

			// add min score for node n
			nodesMinScore.put(n, min);

			Set<Node> set = null;

			// update score index
			if ((set = timeScoreIndex.get(min)) == null) {
				set = new HashSet<>();
				timeScoreIndex.put(min, set);
			}

			// add node n in the index
			set.add(n);
		}
	}

	/**
	 * Clean graph based on lifespan of edges
	 */
	private void graphCleaning() {
		// initialize
		nodesScorePerTimeInstant = new HashMap<>(lvg.size());
		Set<Node> set;
		int t, val;

		if (metric == Config.MA || metric == Config.AA || metric == Config.AMA) {
			numberOfEdgesPerTimeInstant = new HashMap<>(iQ.cardinality());

			for (Iterator<Integer> i = iQ.stream().iterator(); i.hasNext();)
				numberOfEdgesPerTimeInstant.put(i.next(), new Counter());
		} else if (metric == Config.AM || metric == Config.MAM) {
			perTimeInstantScore = new HashMap<>();

			for (Iterator<Integer> i = iQ.stream().iterator(); i.hasNext();)
				perTimeInstantScore.put(i.next(), new TreeMap<>());
		}

		WQuickUnionPC wqup = null;
		int componentID = -1;

		// if there are seed nodes
		if (!seedNodes.isEmpty()) {
			wqup = new WQuickUnionPC(new HashSet<>(lvg.getNodes()));
			List<Integer> sN = new ArrayList<Integer>(seedNodes);

			for (int i = 0; i < sN.size(); i++) {
				for (int j = i + 1; j < sN.size(); j++) {
					if (!wqup.connected(lvg.getNode(sN.get(i)), lvg.getNode(sN.get(j)))) {
						lvg.getNodesAsMap().clear();
						return;
					}
				}
			}

			int seedNode = seedNodes.iterator().next();
			componentID = wqup.getComponentID(lvg.getNode(seedNode));
		}

		// for all nodes
		for (Iterator<Node> it = lvg.getNodes().iterator(); it.hasNext();) {
			// get node n
			Node n = it.next();

			// remove nodes that are not in the same component of the seed nodes
			if (wqup != null && wqup.getComponentID(n) != componentID) {
				it.remove();
				continue;
			}

			// keeps the score size per time instant
			Map<Integer, Counter> scorePerTimeInstant = new HashMap<>(iQ.cardinality());

			// initialize
			for (Iterator<Integer> i = iQ.stream().iterator(); i.hasNext();)
				scorePerTimeInstant.put(i.next(), new Counter());

			// for all edges of node n
			for (Iterator<Entry<Node, BitSet>> edge = n.getAdjacencyAsMap().entrySet().iterator(); edge.hasNext();) {
				Entry<Node, BitSet> entry = edge.next();
				BitSet lifespan = entry.getValue();

				// remove nodes that are not in the same component of the seed
				// nodes
				if (wqup != null && wqup.getComponentID(entry.getKey()) != componentID) {
					edge.remove();
					continue;
				}

				lifespan.and(iQ);

				// remove not active edge
				if (lifespan.isEmpty()) {
					edge.remove();
					continue;
				}

				// increase the degree per time instant
				for (Iterator<Integer> i = lifespan.stream().iterator(); i.hasNext();) {
					t = i.next();
					scorePerTimeInstant.get(t).increase();

					if (metric == Config.MA || metric == Config.AA || metric == Config.AMA)
						numberOfEdgesPerTimeInstant.get(t).increase();
				}
			}

			if (n.getAdjacency().size() == 0) {
				it.remove();

				// if seed node is removed stop
				if (seedNodes.contains(n.getID())) {
					lvg.getNodesAsMap().clear();
					return;
				}
			} else {
				// add nodes n score per time instant info into structure
				nodesScorePerTimeInstant.put(n, scorePerTimeInstant);

				if (metric == Config.AM || metric == Config.MAM) {
					// create the map that returns per time instant a ranking of
					// nodes based on their degree
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
		}

		if (metric == Config.MA || metric == Config.AA || metric == Config.AMA) {
			numberOfNodes = lvg.getNodes().size();

			// correct the degree per time instant
			for (Iterator<Integer> i = iQ.stream().iterator(); i.hasNext();) {
				t = i.next();
				numberOfEdgesPerTimeInstant.get(t).set(numberOfEdgesPerTimeInstant.get(t).getValue() / 2);
			}
		}
	}

	/**
	 * Update minPerTime set the minimums per time instant
	 */
	private int computeMinPerTime() {
		int sum = 0, c, t;

		for (Iterator<Integer> i = iQ.stream().iterator(); i.hasNext();) {
			t = i.next();
			c = perTimeInstantScore.get(t).firstKey();
			sum += c;
		}

		return sum;
	}

	/**
	 * Initial score for all before starting the algorithm (Note) It could be
	 * implemented in charikar but its placed here to avoid any error
	 */
	private void initialScore() {
		double minScore = 0;

		// For metric minimum average and average average
		if (metric == Config.MA || metric == Config.AA || metric == Config.AMA) {

			if (metric == Config.MA || metric == Config.AMA) {
				minScore = Double.MAX_VALUE;

				// for each time instant of iQ check the average degree and
				// store the minimum
				for (Iterator<Integer> i = iQ.stream().iterator(); i.hasNext();) {
					int t = i.next();
					int numOfE = numberOfEdgesPerTimeInstant.get(t).getValue();

					// compute average degree
					double density = numOfE / (double) lvg.size();

					if (density < minScore)
						minScore = density;
				}
			} else if (metric == Config.AA) {
				minScore = 0;

				// for each time instant check the average degree and store the
				// sum
				for (Iterator<Integer> i = iQ.stream().iterator(); i.hasNext();) {
					int t = i.next();
					int numOfE = numberOfEdgesPerTimeInstant.get(t).getValue();

					minScore += numOfE / (double) lvg.size();
				}

				// average degree over time
				minScore /= iQ.cardinality();
			}
		} else
			minScore = timeScoreIndex.firstKey();

		if (metric == Config.AM || metric == Config.MAM)
			minScore = computeMinPerTime();

		minScore = Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", minScore));

		stepsOfMaximumScore.put(minScore, new ArrayList<>(Arrays.asList(step)));
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
		if (stepsOfMaximumScore.isEmpty())
			return 0;
		else if (metric == Config.MA || metric == Config.AA || metric == Config.AMA)
			return stepsOfMaximumScore.entrySet().iterator().next().getKey() * 2;
		else if (metric == Config.AM || metric == Config.MAM)
			return Double.parseDouble(String.format(Locale.ENGLISH, "%.2f",
					(stepsOfMaximumScore.entrySet().iterator().next().getKey() / iQ.cardinality())));
		// for MM
		return stepsOfMaximumScore.entrySet().iterator().next().getKey();
	}

	/**
	 * Get the score of n For MM and AM is the min, for AA and MA is the avg
	 * 
	 * @param n
	 * @return
	 */
	private double getScore(Node n) {
		return nodesMinScore.get(n);
	}

	/**
	 * Checks if solution is connected to the seednodes
	 * 
	 * @return
	 */
	private boolean isConnected() {
		Set<Integer> remainingSeedNodes = new HashSet<>();
		Set<Node> visited = new HashSet<>();
		Queue<Node> queue = new LinkedList<Node>();

		remainingSeedNodes.addAll(seedNodes);

		Node seedNode = lvg.getNode(seedNodes.iterator().next());
		queue.add(seedNode);
		remainingSeedNodes.remove(seedNode.getID());

		if (remainingSeedNodes.isEmpty())
			return true;

		while (!queue.isEmpty()) {
			Node n = queue.remove();

			for (Node trg : n.getAdjacency()) {
				if (trg.getRemovalStep() != 0 || visited.contains(trg))
					continue;

				queue.add(trg);
				visited.add(trg);

				if (remainingSeedNodes.contains(trg.getID()))
					remainingSeedNodes.remove(trg.getID());
			}

			if (remainingSeedNodes.isEmpty())
				return true;
		}
		return false;
	}
}