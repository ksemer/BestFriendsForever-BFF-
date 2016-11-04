package vg;
import java.io.Serializable;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This class represents the Node objects
 * @author ksemer
 */
public class Node implements Serializable {

	private static final long serialVersionUID = 1L;
	//=================================================================
	private int id;
	private int removedAt;
	private Map<Node, BitSet> adjacencies;
	//=================================================================

	/**
	 * Constructor
	 * @param id
	 */
	public Node(int id) {
		this.id = id;
		this.removedAt = 0;
		this.adjacencies = new HashMap<>();
	}

	/**
	 * Add new edge or update existed
	 * @param node
	 * @param time
	 */
	public void addEdge(Node node, int time) {
		BitSet lifespan = adjacencies.get(node);
		
		if (lifespan == null) {
			lifespan= new BitSet();
			adjacencies.put(node, lifespan);
		}
		
		lifespan.set(time);
	}
	
	/**
	 * Return node's adjacency as a Map
	 * @return
	 */
	public Map<Node, BitSet> getAdjacencyAsMap() {
		return adjacencies;
	}
	
	/**
	 * Return edge object for neighbor node n
	 * @param n
	 * @return
	 */
	public BitSet getEdgeLifespan(Node n) {
		return adjacencies.get(n);
	}
	
	/**
	 * Return the k step that node has been removed
	 * @return
	 */
	public int getRemovalStep() {
		return removedAt;
	}
	
	/**
	 * Set removal step
	 * @param step
	 * @return
	 */
	public void setRemovalStep(int step) {
		removedAt = step;
	}

	/**
	 * Returns node's id
	 * @return
	 */
	public int getID() {
		return id;
	}

	/**
	 * Remove neighbor node n
	 * @param n
	 */
	public void removeEdge(Node n) {
		adjacencies.remove(n);
	}

	/**
	 * Return node's adjacency
	 * @return 
	 * @return
	 */
	public Set<Node> getAdjacency() {
		return adjacencies.keySet();
	}
}