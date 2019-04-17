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
	// public Map<Agent, Float> getAmountsReceivable(int iteration);

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

	/**
	 * Sets which iteration and order the Agent defaulted on their payments.
	 * 
	 * @param iteration
	 * @param order
	 */
	public void setDefaultedIteration(int iteration, int order);

	/**
	 * Gets the iteration that the Agent defaulted on their payments. If they
	 * haven't defaulted yet, this is zero.
	 * 
	 * @return
	 */
	public int getDefaultIteration();

	/**
	 * Gets the order within the iteration that the Agent defaulted on their
	 * payments.
	 * 
	 * @return the order that he agent defaulted within the iteration that they
	 *         defaulted
	 */
	public int getDefaultOrder();

	/**
	 * Updates the Agent based on the output of the Clearing Payment Vector. If the
	 * Agent defaulted in a previous iteration those values are not updated. If an
	 * Agent defaulted for the first time in this iteration then their financial
	 * statements and any affected linked Agents are updated to reflect the
	 * situation.
	 * 
	 * Examples of how defaults are processed include:<br>
	 * 1. Businesses that default result in employees that switch from wages to
	 * unemployment benefits.<br>
	 * 2. ADIs that default result in staff becoming unemployed and depositors being
	 * paid out per the Financial Claims Scheme.<br>
	 * 3. Households that default lose their house of they have one, switching from
	 * mortgage repayments to rent payments.
	 * 
	 * @param nodeEquity   - the net cash flow to/(from) the node in this iteration.
	 * @param iteration    - the iteration number.
	 * @param defaultOrder - the order that the node defaulted in this iteration (0
	 *                     if node did not default).
	 */
	public void processClearingPaymentVectorOutput(float nodeEquity, int iteration, int defaultOrder);
	
}
