package algorithm.wqu;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import algorithm.Counter;
import vg.Node;

/**
 * Weighted quick-union with path compression For the union graph of lvg
 *
 * @author ksemer
 */
@SuppressWarnings("unused")
public class WQuickUnionTimeInstant {
    // id[i] -> parent of i
    private Map<Integer, Integer> id;

    // sz[i] -> size of each subset
    private Map<Integer, Counter> sz;

    private Set<Node> nodes;

    private int components;

    /**
     * Constructor
     */
    public WQuickUnionTimeInstant(Set<Node> nodes, BitSet iQ) {
        for (Iterator<Integer> iq = iQ.stream().iterator(); iq.hasNext(); ) {
            int t = iq.next();

            Counter c;
            id = new HashMap<>(nodes.size());
            sz = new HashMap<>(nodes.size());
            this.nodes = nodes;
            components = nodes.size();

            // for all nodes
            for (Node n : nodes) {
                // set id of each node to itself
                id.put(n.getID(), n.getID());
                c = new Counter();
                c.increase();
                sz.put(n.getID(), c);
            }

            List<Node> nodes_l = new ArrayList<>(nodes);

            for (int i = 0; i < nodes.size(); i++) {
                for (int j = i + 1; j < nodes.size(); j++) {
                    Node p = nodes_l.get(i);
                    Node q = nodes_l.get(j);

                    if (connected(p, q))
                        continue;

                    if (p.getAdjacency().contains(q) && p.getEdgeLifespan(q).get(t))
                        unite(p, q);
                }
            }

            System.out.println("---------------------\nTime instant: " + t);
            componentsInfo();
        }
    }

    /**
     * Return root
     */
    private int root(int i) {
        // chase parent parents until reach root
        while (i != id.get(i)) {
            // halve the path length by making every
            // other node in path point to its grandparent
            id.put(i, id.get(id.get(i)));
            i = id.get(i);
        }
        return i;
    }

    /**
     * Change root of p to point to root of q
     */
    private void unite(Node p, Node q) {
        int i = root(p.getID()), j = root(q.getID());

        // merge smaller tree into larger tree
        if (sz.get(i).getValue() < sz.get(j).getValue()) {
            id.put(i, j);
            sz.get(j).increase(sz.get(i).getValue());
        } else {
            id.put(j, i);
            sz.get(i).increase(sz.get(j).getValue());
        }
        components--;
    }

    /**
     * Check if p and q have same root
     */
    private boolean connected(Node p, Node q) {
        return root(p.getID()) == root(q.getID());
    }

    /**
     * Prints for each component the number of nodes
     */
    private void componentsInfo() {
        int r, max_comp = 0, size = Integer.MIN_VALUE;
        Set<Integer> checkedComps = new HashSet<>();
        System.out.println("Number of components: " + components);

        // for each node
        for (int i : id.keySet()) {
            r = root(i);

            // print the size of its component
            if (!checkedComps.contains(r)) {
                checkedComps.add(r);

                if (size < sz.get(r).getValue()) {
                    size = sz.get(r).getValue();
                    max_comp = r;
                }
                System.out.println("Component: " + r + " -- Size: " + sz.get(r).getValue());
            }
        }

        // for each node print the component
        for (int i : id.keySet())
            System.out.println("Node: " + i + " -- Component: " + root(i));

        Map<Node, Counter> st = new HashMap<>();

        for (Node n : this.nodes) {
            st.put(n, new Counter());
        }

        int sum = 0;

        for (int i = 47; i < 57; i++) {
            for (Node n : this.nodes) {

                if (root(n.getID()) == max_comp) {
                    for (Entry<Node, BitSet> entry : n.getAdjacencyAsMap().entrySet()) {
                        Node trg = entry.getKey();

                        if (!nodes.contains(trg) || root(trg.getID()) != max_comp)
                            continue;

                        if (entry.getValue().get(i)) {
                            st.get(n).increase();
                        }
                    }
                }
            }

            int min = Integer.MAX_VALUE;

            for (Node n : this.nodes) {
                if (root(n.getID()) != max_comp)
                    continue;

                sum += st.get(n).getValue();

                if (st.get(n).getValue() < min)
                    min = st.get(n).getValue();
            }

            System.out.println(min + "\t" + sum);
        }
    }

    /**
     * Return the number of components
     */
    public int size() {
        return components;
    }

    /**
     * Return component id of node p
     */
    public int getComponentID(Node p) {
        return root(p.getID());
    }
}