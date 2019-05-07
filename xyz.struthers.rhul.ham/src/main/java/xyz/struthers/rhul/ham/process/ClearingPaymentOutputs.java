/**
 * 
 */
package xyz.struthers.rhul.ham.process;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TIntArrayList;

/**
 * Just a data structure so it's easy to return multiple variables from a single
 * method return statement.
 * 
 * @author Adam Struthers
 * @since 15-Apr-2019
 */
public class ClearingPaymentOutputs implements Serializable {

	private static final long serialVersionUID = 1L;

	// net cash flow of each node after paying liabilities
	// private List<Float> equityOfNode;
	private TFloatArrayList equityOfNode;
	// private float[] equityOfNode;

	// Which round of the CPV algorithm caused the node to default. (0 = no default)
	// private List<Integer> defaultOrderOfNode;
	private TIntArrayList defaultOrderOfNode;
	// private int[] defaultOrderOfNode;

	private int iteration;

	public ClearingPaymentOutputs() {
		// super-class is not serializable, which causes an exception
		// SOURCE:
		// https://stackoverflow.com/questions/12067405/deserializing-an-arraylist-no-valid-constructor
		// super();
	}

	/**
	 * @return the equityOfNode
	 */
	public List<Float> getEquityOfNode() {
		// public TFloatArrayList getEquityOfNode() {
		float[] primitiveArray = this.equityOfNode.toArray();
		List<Float> boxedList = new ArrayList<Float>(primitiveArray.length);
		for (Float node : primitiveArray) {
			boxedList.add(node);
		}
		return boxedList;
	}

	/**
	 * @param equityOfNode the equityOfNode to set
	 */
	public void setEquityOfNode(List<Float> equityOfNode) {
		float[] primitiveArray = new float[equityOfNode.size()];
		int i = 0;
		for (Float node : equityOfNode) {
			primitiveArray[i++] = (node != null ? node : 0f);
		}
		this.equityOfNode = TFloatArrayList.wrap(primitiveArray);
	}

	/**
	 * @param equityOfNode the equityOfNode to set
	 */
	public void setEquityOfNode(TFloatArrayList equityOfNode) {
		this.equityOfNode = equityOfNode;
	}

	/**
	 * @return the defaultOrderOfNode
	 */
	public List<Integer> getDefaultOrderOfNode() {
		// public TIntArrayList getDefaultOrderOfNode() {
		int[] primitiveArray = this.defaultOrderOfNode.toArray();
		List<Integer> boxedList = new ArrayList<Integer>(primitiveArray.length);
		for (Integer node : primitiveArray) {
			boxedList.add(node);
		}
		return boxedList;
	}

	/**
	 * @param defaultOrderOfNode the defaultOrderOfNode to set
	 */
	public void setDefaultOrderOfNode(List<Integer> defaultOrderOfNode) {
		// public void setDefaultOrderOfNode(TIntArrayList defaultOrderOfNode) {
		int[] primitiveArray = new int[defaultOrderOfNode.size()];
		int i = 0;
		for (Integer node : defaultOrderOfNode) {
			primitiveArray[i++] = (node != null ? node : 0);
		}
		this.defaultOrderOfNode = TIntArrayList.wrap(primitiveArray);
	}

	/**
	 * @param defaultOrderOfNode the defaultOrderOfNode to set
	 */
	public void setDefaultOrderOfNode(TIntArrayList defaultOrderOfNode) {
		this.defaultOrderOfNode = defaultOrderOfNode;
	}

	/**
	 * @return the iteration
	 */
	public int getIteration() {
		return iteration;
	}

	/**
	 * @param iteration the iteration to set
	 */
	public void setIteration(int iteration) {
		this.iteration = iteration;
	}

}
