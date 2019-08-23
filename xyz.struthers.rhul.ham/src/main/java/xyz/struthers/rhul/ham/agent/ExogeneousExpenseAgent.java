package xyz.struthers.rhul.ham.agent;

import java.util.ArrayList;
import java.util.List;

import xyz.struthers.rhul.ham.process.NodePayment;

/**
 * This is just a dummy agent to allow the exogeneous expenses of the other
 * agents to flow somewhere during calculation of the Clearing Payments Vector.
 * 
 * @author Adam Struthers
 * @since 23-Aug-2019
 */
public class ExogeneousExpenseAgent extends Agent {

	private static final long serialVersionUID = 1L;

	// agent relationships
	protected int paymentClearingIndex;

	public ExogeneousExpenseAgent() {
		super();
	}

	/**
	 * @param agent
	 */
	public ExogeneousExpenseAgent(Agent agent) {
		super(agent);
	}

	@Override
	public int getPaymentClearingIndex() {
		return this.paymentClearingIndex;
	}

	@Override
	public void setPaymentClearingIndex(int index) {
		this.paymentClearingIndex = index;
	}

	@Override
	public List<NodePayment> getAmountsPayable(int iteration) {
		// the exogeneous expense agent is just a proxy - it doesn't have any
		// liabilities of its own
		return new ArrayList<NodePayment>();
	}

	@Override
	public void setDefaultedIteration(int iteration, int order) {
		// do nothing
	}

	@Override
	public int getDefaultIteration() {
		return 0;
	}

	@Override
	public int getDefaultOrder() {
		return 0;
	}

	@Override
	public int processClearingPaymentVectorOutput(float nodeEquity, int iteration, int defaultOrder) {
		return 0;
	}

}
