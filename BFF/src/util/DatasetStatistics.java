package util;

import java.io.IOException;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import algorithm.Counter;

import java.util.Set;

import vg.Graph;
import vg.LoadGraph;
import vg.Node;

/**
 * Computes dataset statistics
 * @author ksemer
 *
 */
public class DatasetStatistics {
	private static String BASE = "/home/ksemer/workspaces/BFF/data/";

	public static void main(String[] args) throws IOException {
		show("as", null);
		show("soc", null);
		show("caida", null);
		show("oregon1", null);
		show("oregon2", null);

		BitSet iQ = new BitSet();
		iQ.set(47, 57);
		show("DBLP_Graph_DB1+ALL", iQ);

		iQ.clear();
		iQ.set(52, 57);
		show("DBLP_Graph_DB1+ALL", iQ);

		show("twitter", null);

	}

	private static void show(String path, BitSet iQ) throws IOException {
		Graph g = LoadGraph.loadDataset(iQ, BASE + path);
		TreeMap<Integer, Counter> nodes = new TreeMap<>();
		TreeMap<Integer, Counter> edgesPerT = new TreeMap<>();
		Counter c;
		Counter avgD = new Counter();

		System.out.println(BASE + path);

		for (Node n : g.getNodes()) {
			Set<Integer> s = new HashSet<>();

			for (Iterator<Entry<Node, BitSet>> e = n.getAdjacencyAsMap().entrySet().iterator(); e.hasNext();) {
				Entry<Node, BitSet> entry = e.next();

				for (Iterator<Integer> i = entry.getValue().stream().iterator(); i.hasNext();) {
					int t = i.next();
					avgD.increase();

					if (!s.contains(t)) {
						if ((c = nodes.get(t)) == null) {
							c = new Counter();
							nodes.put(t, c);
						}
						c.increase();
						s.add(t);
					}

					if ((c = edgesPerT.get(t)) == null) {
						c = new Counter();
						edgesPerT.put(t, c);
					}
					c.increase();
				}
			}
		}

		int count = 0;

		for (Map.Entry<Integer, Counter> entry : edgesPerT.entrySet())
			count += entry.getValue().getValue();

		System.out.println("Total nodes: " + g.size());
		System.out.println("Avg degree: " + (double) avgD.getValue() / edgesPerT.size() / g.size());
		System.out.println("Avg edges: " + count / (2 * edgesPerT.size()));
		System.out.println("Interval: " + edgesPerT.size());
		System.out.println("==================================");
	}
}