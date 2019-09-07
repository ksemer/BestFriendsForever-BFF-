package algorithm;

/**
 * Counter class
 * @author ksemer
 */
public class Counter {
	private int counter;

	/**
	 * Constructor
	 */
	public Counter() {
		counter = 0;
	}
	
	/**
	 * Increase counter
	 */
	public void increase() {
		counter++;
	}
	
	/**
	 * Add c to counter
	 */
	public void increase(int c) {
		counter+=c;
	}
	
	/**
	 * Decrease counter
	 */
	public void decrease() {
		counter--;
	}
	
	/**
	 * Set the counter to val
	 */
	public void set(int val) {
		counter = val;
	}
	
	/**
	 * Return counter
	 */
	public int getValue() {
		return counter;
	}
}