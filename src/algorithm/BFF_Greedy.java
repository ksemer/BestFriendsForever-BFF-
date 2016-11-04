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

public class BFF_Greedy {
	private int metric, step;

	private Graph lvg;

	private BitSet iQ;

	// solution S
	private Set<Integer> S;

	private Set<Integer> seedNodes;

	private Map<Double, List<Integer>> stepsOfMaximumScore;

	private Map<Integer, Counter> numberOfEdgesPerTimeInstant;

	// keeps for each node the degree per time instant
	private Map<Node, Map<Integer, Counter>> nodesScorePerTimeInstant;

	// reverse structure of nodesScorePerTimeInstant
	private Map<Integer, TreeMap<Integer, Set<Node>>> perTimeInstantScore;

	/**
	 * Constructor
	 * 
	 * @param iQ
	 * @param lvg
	 * @param metric
	 * @param seedNodes
	 * @throws IOException
	 * @throws Exception
	 */
	public BFF_Greedy(Graph lvg, BitSet iQ, int metric, Set<Integer> seedNodes) {
		// initialization
		this.stepsOfMaximumScore = new TreeMap<>();
		this.S = new HashSet<>();
		this.lvg = Graph.deepClone(lvg);
		this.iQ = iQ;
		this.seedNodes = seedNodes;

		if (metric == Config.TMA)
			this.metric = Config.MA;
		else
			this.metric = Config.AM;

		graphCleaning();
		
		if (this.lvg.isEmpty()) {
			System.out.println("There is not any solution for Metric: " + metric + " ,Interval: " + iQ);
			return;
		}

		Graph lvg_ = lvg;

		boolean executeWithSeeds = false;

		// check if seedNodes exist in lvg
		for (int id : seedNodes) {
			if (lvg.getNode(id) == null) {
				executeWithSeeds = true;
				break;
			}
		}

		if (!executeWithSeeds)
			// run Charikar algorithm
			runCharikar(lvg_);

		int step = stepsOfMaximumScore.entrySet().iterator().next().getValue().get(0);

		for (Node n : lvg_.getNodes()) {

			if (n.getRemovalStep() > step || step == 1) {
				// add node id in solution set
				S.add(n.getID());
			}
		}

	}

	/**
	 * RunCharikar algorithm
	 * 
	 * @param lvg_
	 * @throws IOException
	 */
	private void runCharikar(Graph lvg_) {
		int t, numOfE, sumAM = 0, sumAMNeigb = 0, nodeNsumAM = 0;
		Node n = null;
		Map<Integer, Counter> nspti;

		// while there are nodes to be examined
		while (!lvg.isEmpty()) {
			step++;

			double minScore = 0, generalMinScore = 0;

			if (metric == Config.AM) {
				sumAM = computeMinPerTime(null);
				if (sumAM > 0)
					generalMinScore = -1;
				else
					generalMinScore = Double.MAX_VALUE;

				n = null;
			}

			for (Node node : lvg.getNodes()) {
				// for metric MA
				if (metric == Config.MA) {
					minScore = Double.MAX_VALUE;

					// for each time instant of iQ check the average degree and
					// store the minimum
					for (Iterator<Integer> i = iQ.stream().iterator(); i.hasNext();) {
						t = i.next();

						numOfE = numberOfEdgesPerTimeInstant.get(t).getValue();
						numOfE -= nodesScorePerTimeInstant.get(node).get(t).getValue();

						// compute average degree
						double density = numOfE / (double) (lvg.size() - 1);

						if (minScore > density)
							minScore = density;
					}

					if (generalMinScore <= minScore) {
						generalMinScore = minScore;
						n = node;
					}

					if (lvg.size() - 1 == 0)
						generalMinScore = 0;
				} else if (metric == Config.AM) { // for metric AM
					nspti = nodesScorePerTimeInstant.get(node);
					sumAMNeigb = 0;

					for (Iterator<Integer> i = iQ.stream().iterator(); i.hasNext();)
						sumAMNeigb += nspti.get(i.next()).getValue();

					// if AM is zero use AA algorithm
					if (sumAM == 0) {
						if (sumAMNeigb < generalMinScore) {
							generalMinScore = sumAMNeigb;
							n = node;
						}
					} else {
						updateEdges(node, false);

						int temp = computeMinPerTime(node);

						if (temp > generalMinScore) {
							generalMinScore = temp;
							n = node;
							nodeNsumAM = sumAMNeigb;
						}

						else if (temp == generalMinScore && sumAMNeigb < nodeNsumAM) {
							nodeNsumAM = sumAMNeigb;
							n = node;
						}

						// restore edges
						updateEdges(node, true);
					}
				}
			}

			// remove node
			lvg_.getNode(n.getID()).setRemovalStep(step);
			n.setRemovalStep(step);
			updateEdges(n, false);

			if (metric == Config.AM) {
				nspti = nodesScorePerTimeInstant.get(n);

				for (Iterator<Integer> i = iQ.stream().iterator(); i.hasNext();) {
					t = i.next();

					perTimeInstantScore.get(t).get(nspti.get(t).getValue()).remove(n);

					if (perTimeInstantScore.get(t).get(nspti.get(t).getValue()).isEmpty())
						perTimeInstantScore.get(t).remove(nspti.get(t).getValue());
				}
				nodesScorePerTimeInstant.remove(n);
			}

			// remove node
			lvg.removeNode(n.getID());

			// if there are seed nodes and seed node is the node with the
			// minimum score
			if (seedNodes.contains(n.getID())) {
				for (Node n1 : lvg.getNodes())
					if (n1.getRemovalStep() == 0)
						n1.setRemovalStep(step - 1);
				return;
			}

			if (metric == Config.AM) {
				if (lvg.isEmpty())
					generalMinScore = 0;
				else
					generalMinScore = computeMinPerTime(null);
			}

			// Locale.ENGLISH to avoid problem in double values with dot or
			// comma
			generalMinScore = Double.parseDouble(String.format(Locale.ENGLISH, "%.2f", generalMinScore));

			// update structure by adding the step to the highest key
			// Although we use a treemap, it keeps only the highest value
			if (!stepsOfMaximumScore.isEmpty()) {
				if (stepsOfMaximumScore.entrySet().iterator().next().getKey() < generalMinScore) {
					stepsOfMaximumScore.clear();
					stepsOfMaximumScore.put(generalMinScore, new ArrayList<>(Arrays.asList(step)));
				} else if (stepsOfMaximumScore.entrySet().iterator().next().getKey() == generalMinScore)
					stepsOfMaximumScore.get(generalMinScore).add(step);
			} else
				stepsOfMaximumScore.put(generalMinScore, new ArrayList<>(Arrays.asList(step)));
		}
	}

	/**
	 * 
	 * @param node
	 */
	private void updateEdges(Node node, boolean restore) {
		Set<Node> set;
		int t;
		BitSet l;
		Counter c;
		Node trg;
		Map<Integer, Counter> nspti;

		for (Iterator<Entry<Node, BitSet>> edge = node.getAdjacencyAsMap().entrySet().iterator(); edge.hasNext();) {
			Entry<Node, BitSet> entry = edge.next();
			trg = entry.getKey();

			// this trg node has been checked and removed
			if (trg.getRemovalStep() != 0) {
				edge.remove();
				continue;
			}

			nspti = nodesScorePerTimeInstant.get(trg);

			// intersect edge lifespan with given iQ
			l = (BitSet) entry.getValue().clone();
			l.and(iQ);

			// update score per time instant structure
			for (Iterator<Integer> i = l.stream().iterator(); i.hasNext();) {
				t = i.next();

				c = nspti.get(t);

				if (metric == Config.MA)
					numberOfEdgesPerTimeInstant.get(t).decrease();

				if (metric == Config.AM) {
					perTimeInstantScore.get(t).get(c.getValue()).remove(trg);

					if (perTimeInstantScore.get(t).get(c.getValue()).isEmpty())
						perTimeInstantScore.get(t).remove(c.getValue());
				}

				if (restore)
					c.increase();
				else
					c.decrease();

				if (metric == Config.AM) {
					if ((set = perTimeInstantScore.get(t).get(c.getValue())) == null) {
						set = new HashSet<>();
						perTimeInstantScore.get(t).put(c.getValue(), set);
					}
					set.add(trg);
				}
			}
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

		if (metric == Config.MA) {
			numberOfEdgesPerTimeInstant = new HashMap<>(iQ.cardinality());

			for (Iterator<Integer> i = iQ.stream().iterator(); i.hasNext();)
				numberOfEdgesPerTimeInstant.put(i.next(), new Counter());
		} else if (metric == Config.AM) {
			perTimeInstantScore = new HashMap<>();

			for (Iterator<Integer> i = iQ.stream().iterator(); i.hasNext();)
				perTimeInstantScore.put(i.next(), new TreeMap<>());
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
					scorePerTimeInstant.get(t).increase();

					if (metric == Config.MA)
						numberOfEdgesPerTimeInstant.get(t).increase();
				}
			}

			if (n.getAdjacency().size() == 0) {
				it.remove();
			} else {
				// add nodes n score per time instant info into structure
				nodesScorePerTimeInstant.put(n, scorePerTimeInstant);

				if (metric == Config.AM) {
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

		if (metric == Config.MA) {
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
	private int computeMinPerTime(Node n) {
		int sum = 0, c, t;

		for (Iterator<Integer> i = iQ.stream().iterator(); i.hasNext();) {
			t = i.next();

			c = perTimeInstantScore.get(t).firstKey();

			// check if n is the minimum so after its potential deletion which
			// is the minimum
			if (n != null) {
				Set<Node> x = perTimeInstantScore.get(t).get(c);
				if (x.size() == 1 && x.contains(n))
					c = perTimeInstantScore.get(t).ceilingKey(c + 1);
			}

			sum += c;
		}

		// return the sum of am
		return sum;
	}

	/**
	 * Return solution set
	 * 
	 * @return
	 */
	public Set<Integer> getSolutionSet() {
		return this.S;
	}

	/**
	 * Return solutions' density
	 * 
	 * @return
	 */
	public double getSolutionDensity() {
		if (stepsOfMaximumScore.isEmpty())
			return 0;
		else if (metric == Config.MA)
			return 2 * stepsOfMaximumScore.entrySet().iterator().next().getKey();
		else // metric == Config.AM
			return Double.parseDouble(
					String.format(Locale.ENGLISH, "%.2f", (stepsOfMaximumScore.entrySet().iterator().next().getKey() / iQ.cardinality())));
	}
}