/**
 * 
 */
package xyz.struthers.rhul.ham.process;

import java.util.List;

/**
 * Just a data structure so it's easy to return multiple variables from a single
 * method return statement.
 * 
 * @author Adam Struthers
 * @since 15-Apr-2019
 */
public class ClearingPaymentOutputs {

	// net cash flow of each node after paying liabilities
	private List<Float> equityOfNode;

	// Which round of the CPV algorithm caused the node to default. (0 = no default)
	private List<Integer> defaultOrderOfNode;

	public ClearingPaymentOutputs() {
		super();
	}

	/**
	 * @return the equityOfNode
	 */
	public List<Float> getEquityOfNode() {
		return equityOfNode;
	}

	/**
	 * @param equityOfNode the equityOfNode to set
	 */
	public void setEquityOfNode(List<Float> equityOfNode) {
		this.equityOfNode = equityOfNode;
	}

	/**
	 * @return the defaultOrderOfNode
	 */
	public List<Integer> getDefaultOrderOfNode() {
		return defaultOrderOfNode;
	}

	/**
	 * @param defaultOrderOfNode the defaultOrderOfNode to set
	 */
	public void setDefaultOrderOfNode(List<Integer> defaultOrderOfNode) {
		this.defaultOrderOfNode = defaultOrderOfNode;
	}

}
