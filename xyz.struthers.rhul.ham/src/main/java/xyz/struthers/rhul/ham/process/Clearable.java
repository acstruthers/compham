/**
 * 
 */
package xyz.struthers.rhul.ham.process;

import java.util.List;

/**
 * Objects need to implement this interface to be nodes in the
 * ClearingPaymentVector.
 * 
 * @author Adam Struthers
 * @since 02-Feb-2019
 */
public interface Clearable {

	/**
	 * Gets the Unique ID that is the Agent's array index number in the Payments
	 * Clearing Vector algorithm. This number should never change.
	 * 
	 * @return an integer index number
	 */
	public int getPaymentClearingIndex();

	/**
	 * Sets the Unique ID that is the Agent's array index number in the Payments
	 * Clearing Vector algorithm. This number should never change.
	 * 
	 * @param index
	 */
	public void setPaymentClearingIndex(int index);

	/**
	 * Gets the contractual obligations from other nodes to this node.
	 * 
	 * @param iteration
	 * @return a map of the agents that owe money to this Clearable object, and the
	 *         amounts owed.
	 */
	// public Map<Agent, Double> getAmountsReceivable(int iteration);

	/**
	 * Gets the contractual obligations of this node to other nodes. It doesn't
	 * matter what order they're returned in because the NodePayments contain the
	 * index of the recipient.
	 * 
	 * @param iteration
	 * @return a map of the agents that this Clearable object owes money to, and the
	 *         amounts owed.
	 */
	public List<NodePayment> getAmountsPayable(int iteration);
}
