package algorithm.wqu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import algorithm.Counter;
import vg.Node;

/**
 * Weighted quick-union with path compression For the union graph of lvg
 *
 * @author ksemer
 */
public class WQuickUnionPCUnionGraph {
    // id[i] -> parent of i
    private Map<Integer, Integer> id;

    // sz[i] -> size of each subset
    private Map<Integer, Counter> sz;

    private int components;

    /**
     * Constructor
     */
    public WQuickUnionPCUnionGraph(Set<Node> nodes) {
        Counter c;
        id = new HashMap<>(nodes.size());
        sz = new HashMap<>(nodes.size());
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

                if (p.getAdjacency().contains(q))
                    unite(p, q);
            }
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
    public boolean connected(Node p, Node q) {
        return root(p.getID()) == root(q.getID());
    }

    /**
     * Prints for each component the number of nodes
     */
    public void componentsInfo() {
        int r;
        Set<Integer> checkedComps = new HashSet<>();
        System.out.println("Number of components: " + components);

        // for each node
        for (int i : id.keySet()) {
            r = root(i);

            // print the size of its component
            if (!checkedComps.contains(r)) {
                checkedComps.add(r);
                System.out.println("Component: " + r + " -- Size: " + sz.get(r).getValue());
            }
        }

        // for each node print the component
        for (int i : id.keySet())
            System.out.println("Node: " + i + " -- Component: " + root(i));
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