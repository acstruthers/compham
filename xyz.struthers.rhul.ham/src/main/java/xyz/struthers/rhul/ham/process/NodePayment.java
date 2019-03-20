/**
 * 
 */
package xyz.struthers.rhul.ham.process;

/**
 * Identifies an Agent's liabilities to other nodes - both which nodes and how
 * much.
 * 
 * @author Adam Struthers
 * @since 2019-03-20
 */
public class NodePayment {
	final int recipientIndex;
	final double liabilityAmount;

	public NodePayment(int recipientIndex, double liabilityAmount) {
		super();
		this.recipientIndex = recipientIndex;
		this.liabilityAmount = liabilityAmount;
	}

	public int getRecipientIndex() {
		return this.recipientIndex;
	}

	public double getLiabilityAmount() {
		return this.liabilityAmount;
	}
}
