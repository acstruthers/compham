/**
 * 
 */
package xyz.struthers.rhul.ham.process;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Based on Eisenberg & Noe (2001), with the following extensions:<br>
 * 1. It includes businesses and individuals, not just the inter-bank
 * market.<br>
 * 2. The order of default is assumed to be:<br>
 * (a) First, businesses fail due to exogeneous market forces, resulting in
 * employees not receiving the salaries they were expecting.<br>
 * (b) Second, individuals fail to meet their loan repayments because they lost
 * their job when the business that employed them failed. This may or may not
 * cause a default depending on the strength of their balance sheet.<br>
 * (c) Finally, ADIs have a non-performing loan to deal with. This may or may
 * not cause a default depending on the strength of their balance sheet.
 * 
 * The regularity assumption holds for individuals because the government
 * supplies welfare payments to individuals who have no other source of income,
 * and governments are not part of the financial system being modelled. It is
 * not obvious that the same holds for businesses and ADIs because business
 * revenues come from other parts of the economy and are only exogeneous if
 * they're due to exports, and unless an ADI has foreign investments or foreign
 * customers then by definition all of its income comes from within the
 * financial system that is being modelled. Regularity ensures the existence of
 * a unique clearing vector.
 * 
 * This class is written to generate a clearing payment vector for a single
 * point in time. It does not maintain a history of liabilities, payments,
 * equity, etc.
 * 
 * Each instance of this class stores 5 double arrays, 3 double matrices, 1
 * integer array, and 1 integer. With 27 million agents it will consume
 * approximately 5.5 petabytes of RAM. This is clearly impractical for a dataset
 * of this size, so needs to be refactored.
 * 
 * TODO: If an ADI defaults, apply the govt g'tee rules to customer's deposits.
 * Not just the $250k limit per account, but also the $15Bn limit per ADI
 * (double-check those amounts).
 * 
 * TODO: Use EJML or JAMA for matrix maths. Will be at least as fast as my code.
 * 
 * TODO: Refactor to use lists of connected nodes only (27 million nodes need
 * over 5.5 PB of RAM).
 * 
 * @author Adam Struthers
 * @version 0.1
 * @since 12-Nov-2018
 */
public class ClearingPaymentVector {

	// We know the size in advance, so can use arrays to improve speed & reduce
	// memory usage
	// these don't change
	private List<Double> exogeneousNominalCashFlow; // Non-negative cash inflows (i.e. income).
	private List<List<Double>> nominalLiabilitiesAmount; // payments owed in this time period only
	private List<List<Double>> relativeLiabilitiesAmount;
	/**
	 * Index of the agent that payments are owed to for nominal liabilities,
	 * relative liabilities, and clearing payments.
	 * 
	 * It's more efficient to store this in a separate List than to store Maps in
	 * each of the other lists.
	 */
	private List<List<Integer>> liabilitiesIndex;
	private List<Double> totalLiabilitiesOfNode; // total obligation vector
	private List<Double> totalOwedToNode; // amount owed to node, assuming no defaults
	private List<List<Double>> clearingPaymentAmount; // to work out the exact amounts being paid between parties
	private List<Double> clearingPaymentVector; // sum of the rows in the clearingPaymentMatrix
	private List<Double> equityOfNode; // surplus cash flow of each node after paying liabilities
	/**
	 * Which round of the clearing vector algorithm caused the node to default.<br>
	 * (0 = no default)
	 */
	private List<Integer> defaultOrderOfNode;
	private int agentCount; // the number of agents in the clearing algorithm

	public ClearingPaymentVector() {
		super();
		this.init();
	}

	private void init() {
		this.exogeneousNominalCashFlow = null;
		this.nominalLiabilitiesAmount = null;
		this.relativeLiabilitiesAmount = null;
		this.liabilitiesIndex = null;
		this.totalLiabilitiesOfNode = null;
		this.totalOwedToNode = null;
		this.clearingPaymentAmount = null;
		this.clearingPaymentVector = null;
		this.equityOfNode = null;
		this.defaultOrderOfNode = null;
		this.agentCount = 0;
	}

	/**
	 * 
	 * @param liabilitiesAmounts - amounts owed by every node to the other nodes
	 *                           they're connected to
	 * @param liabilitiesIndices - indices of the sub-list to designate which node
	 *                           they relate to
	 * @param operatingCashFlow  - exogeneous cash inflows to each node
	 * @return a map containing:<br>
	 *         List<Double> ClearingPaymentVector,<br>
	 *         List<List<Double>> ClearingPaymentMatrix,<br>
	 *         List<List<Integer>> ClearingPaymentIndices,<br>
	 *         List<Double> NodeEquity, and<br>
	 *         List<Integer> NodeDefaultOrder.
	 * @author Adam Struthers
	 * @since 2019-03-18
	 */
	public Map<String, Object> calculate(List<List<Double>> liabilitiesAmounts, List<List<Integer>> liabilitiesIndices,
			List<Double> operatingCashFlow) {
		Map<String, Object> result = null;
		if (liabilitiesAmounts.size() == liabilitiesIndices.size()
				&& liabilitiesAmounts.size() == operatingCashFlow.size()) {
			// must be the same number of agents in each argument

			// set input values
			this.agentCount = liabilitiesAmounts.size();
			this.exogeneousNominalCashFlow = operatingCashFlow;
			this.nominalLiabilitiesAmount = liabilitiesAmounts;
			this.liabilitiesIndex = liabilitiesIndices;

			// calculate clearing payment vector
			this.calculateLiabilities();
			this.calculatePayments();

			// TODO: save output to file for analysis

			// return output to caller
			result = new HashMap<String, Object>((int) Math.ceil(5d / 0.75d));
			result.put("ClearingPaymentVector", this.clearingPaymentVector);
			result.put("ClearingPaymentMatrix", this.clearingPaymentAmount);
			result.put("ClearingPaymentIndices", this.liabilitiesIndex);
			result.put("NodeEquity", this.equityOfNode);
			result.put("NodeDefaultOrder", this.defaultOrderOfNode);
		}
		return result;
	}

	/**
	 * The algorithm to find the clearing payment vector is:<br>
	 * 1. Determine each node's payout, assuming that all other nodes satisfy their
	 * obligations. If all obligations are satisfied, terminate the algorithm.<br>
	 * 2. If some nodes default even when all the other nodes pay, try to solve the
	 * system again, assuming that only these "first-order" default occur. If only
	 * first-order defaults occur under this new clearing vector, then terminate the
	 * algorithm.<br>
	 * 3. If second-order defaults occur, then try to clear again assuming only
	 * second-order defaults occur, and so on.
	 * 
	 * It is clear that since there are only n nodes this process must terminate
	 * after n iterations. The point at which a node default under the algorithm is
	 * a measure of the node's exposure to systemic risks faced by the clearing
	 * system.
	 * 
	 * SOURCE: Eisenberg & Noe (2001: 243)
	 */
	private void calculatePayments() {
		// initialise variables
		List<Double> oldPaymentClearingVector = new ArrayList<Double>(this.agentCount);
		this.clearingPaymentVector = new ArrayList<Double>(this.agentCount);
		this.defaultOrderOfNode = new ArrayList<Integer>(this.agentCount);
		for (int fromIdx = 0; fromIdx < this.agentCount; fromIdx++) {
			// make a copy so we don't alter the original
			oldPaymentClearingVector.add(fromIdx, this.totalLiabilitiesOfNode.get(fromIdx));
			this.clearingPaymentVector.add(fromIdx, this.totalLiabilitiesOfNode.get(fromIdx));
			this.defaultOrderOfNode.add(fromIdx, 0); // no default
		}

		// iteratively calculate payment clearing vector
		boolean systemCleared = true;
		int order = 0;
		while (order < this.agentCount && !systemCleared) {
			// 2. If some nodes default even when all the other nodes pay, try to solve the
			// system again, assuming that only these "first-order" default occur. If only
			// first-order defaults occur under this new clearing vector, then terminate the
			// algorithm.
			// 3. If second-order defaults occur, then try to clear again
			// assuming only second-order defaults occur, and so on.
			order++;
			systemCleared = true;
			for (int fromIdx = 0; fromIdx < this.agentCount; fromIdx++) {
				// use clearing payment vector and rel liab matrix to calc total paid to node
				double paidToNode = 0d;
				for (int j = 0; j < this.agentCount; j++) {
					paidToNode += oldPaymentClearingVector.get(j) * this.relativeLiabilitiesAmount.get(j).get(fromIdx);
				}
				// check for negative equity to see who defaulted in this round
				if (this.exogeneousNominalCashFlow.get(fromIdx) + paidToNode < this.totalLiabilitiesOfNode
						.get(fromIdx)) {
					// node defaulted, so reduce its payment so that its cash flow to equity is zero
					systemCleared = false;
					this.defaultOrderOfNode.set(fromIdx, order); // node defaulted in this round of algorithm
					this.clearingPaymentVector.set(fromIdx, this.exogeneousNominalCashFlow.get(fromIdx) + paidToNode);
				} else {
					this.clearingPaymentVector.set(fromIdx, oldPaymentClearingVector.get(fromIdx));
				}
			}
			if (!systemCleared) {
				for (int fromIdx = 0; fromIdx < this.agentCount; fromIdx++) {
					// reset old payment clearing vector ready for the next round
					oldPaymentClearingVector.set(fromIdx, this.clearingPaymentVector.get(fromIdx));
				}
			}
		}

		// calculate payment clearing matrix and equity of each node
		this.clearingPaymentAmount = new ArrayList<List<Double>>(this.agentCount); // FIXME: refactor (too big)
		for (int i = 0; i < this.agentCount; i++) {
			this.clearingPaymentAmount.add(new ArrayList<Double>(this.agentCount));
		}
		this.equityOfNode = new ArrayList<Double>(this.agentCount);
		for (int fromIdx = 0; fromIdx < this.agentCount; fromIdx++) {
			double paymentReceived = 0d;
			for (int toIdx = 0; toIdx < this.agentCount; toIdx++) {
				this.clearingPaymentAmount.get(fromIdx).set(toIdx,
						this.clearingPaymentAmount.get(fromIdx).get(toIdx) + this.clearingPaymentVector.get(fromIdx)
								* this.relativeLiabilitiesAmount.get(fromIdx).get(toIdx));
				paymentReceived += this.clearingPaymentVector.get(toIdx)
						* this.relativeLiabilitiesAmount.get(toIdx).get(fromIdx);
			}
			this.equityOfNode.set(fromIdx, paymentReceived + this.exogeneousNominalCashFlow.get(fromIdx)
					- this.clearingPaymentVector.get(fromIdx));
		}
	}

	/**
	 * Calculates the total contractual liabilities of each node, and the relative
	 * amount owed to each other node.
	 */
	private void calculateLiabilities() {
		this.enforceLiabilityMatrixConstraints();
		this.totalOwedToNode = new ArrayList<Double>(this.agentCount);
		this.totalLiabilitiesOfNode = new ArrayList<Double>(this.agentCount);
		this.relativeLiabilitiesAmount = new ArrayList<List<Double>>(this.agentCount);
		for (int fromIdx = 0; fromIdx < this.agentCount; fromIdx++) {
			// initialise array
			int toSize = this.liabilitiesIndex.get(fromIdx).size();
			this.relativeLiabilitiesAmount.add(new ArrayList<Double>(toSize));
		}
		for (int fromIdx = 0; fromIdx < this.agentCount; fromIdx++) {
			this.totalLiabilitiesOfNode.set(fromIdx, 0d);
			this.totalOwedToNode.set(fromIdx, 0d);
			for (int toIdx = 0; toIdx < this.agentCount; toIdx++) {
				this.totalLiabilitiesOfNode.set(fromIdx, this.totalLiabilitiesOfNode.get(fromIdx)
						+ this.nominalLiabilitiesAmount.get(fromIdx).get(toIdx));
				this.totalOwedToNode.set(fromIdx,
						this.totalOwedToNode.get(fromIdx) + this.nominalLiabilitiesAmount.get(toIdx).get(fromIdx));
			}
			for (int toIdx = 0; toIdx < this.agentCount; toIdx++) {
				if (this.totalLiabilitiesOfNode.get(fromIdx) > 0d) {
					this.relativeLiabilitiesAmount.get(fromIdx).set(toIdx,
							this.nominalLiabilitiesAmount.get(fromIdx).get(toIdx)
									/ this.totalLiabilitiesOfNode.get(fromIdx));
				} else {
					this.relativeLiabilitiesAmount.get(fromIdx).set(toIdx, 0d);
				}
			}
		}
	}

	/**
	 * All elements non-negative, and diagonal is 0.
	 * 
	 * @since 2019-03-18
	 */
	private void enforceLiabilityMatrixConstraints() {
		for (int fromIdx = 0; fromIdx < this.agentCount; fromIdx++) {
			for (int to = 0; to < this.nominalLiabilitiesAmount.get(fromIdx).size(); to++) {
				int toIdx = this.liabilitiesIndex.get(fromIdx).get(to);
				if (fromIdx == toIdx) {
					// set diagonal to zero
					this.nominalLiabilitiesAmount.get(fromIdx).set(to, 0d);
				}
				if (this.nominalLiabilitiesAmount.get(fromIdx).get(to) < 0d) {
					// ensure amounts are non-negative
					this.nominalLiabilitiesAmount.get(fromIdx).set(to,
							-this.nominalLiabilitiesAmount.get(fromIdx).get(to));
				}
			}
		}
	}

}
