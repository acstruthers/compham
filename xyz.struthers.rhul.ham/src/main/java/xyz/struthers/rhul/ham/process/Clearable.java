/**
 * 
 */
package xyz.struthers.rhul.ham.process;

import java.util.Map;

import xyz.struthers.rhul.ham.agent.Agent;

/**
 * Objects need to implement this interface to be nodes in the
 * ClearingPaymentVector.
 * 
 * @author Adam Struthers
 * @since 02-Feb-2019
 */
public interface Clearable {
	/**
	 * Gets the contractual obligations from other nodes to this node.
	 * 
	 * @param iteration
	 * @return a map of the agents that owe money to this Clearable object, and the
	 *         amounts owed.
	 */
	public Map<Agent, Double> getAmountsReceivable(int iteration);

	/**
	 * Gets the contractual obligations of this node to other nodes.
	 * 
	 * @param iteration
	 * @return a map of the agents that this Clearable object owes money to, and the
	 *         amounts owed.
	 */
	public Map<Agent, Double> getAmountsPayable(int iteration);
}
