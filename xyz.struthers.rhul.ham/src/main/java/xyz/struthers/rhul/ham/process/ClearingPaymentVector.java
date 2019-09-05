/**
 * 
 */
package xyz.struthers.rhul.ham.process;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TIntArrayList;

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
 * Each instance of this class stores 5 float lists, 3 float nested lists, 1
 * integer list, 1 integer nested list, 1 nested NodeLink list, and 1 integer.
 * With 27 million agents and 20 links per node, it will consume approximately
 * 17GB of RAM. At 37 million agents this becomes 23.5GB of RAM, and with only
 * 12.5 million agents it drops down to 7.9GB of RAM.
 * 
 * TODO: If an ADI defaults, apply the govt g'tee rules to customer's deposits.
 * Not just the $250k limit per account, but also the $15Bn limit per ADI
 * (float-check those amounts).
 * 
 * @author Adam Struthers
 * @version 0.1
 * @since 12-Nov-2018
 */
public class ClearingPaymentVector implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final boolean DEBUG_RAM_USAGE = true;
	public static final boolean DEBUG_DEFAULTS = false;

	// We know the size in advance, so can use arrays to improve speed & reduce
	// memory usage because these don't change
	// private List<Float> exogeneousNominalCashFlow; // Non-negative cash inflows
	// (i.e. income).
	private TFloatArrayList exogeneousNominalCashFlow; // Non-negative cash inflows (i.e. income).
	// private List<List<Float>> nominalLiabilitiesAmount; // payments owed in this
	// time period only
	private List<TFloatArrayList> nominalLiabilitiesAmount; // payments owed in this time period only
	// private List<List<Float>> relativeLiabilitiesAmount;
	private List<TFloatArrayList> relativeLiabilitiesAmount;
	/**
	 * Index of the agent that payments are owed to for nominal liabilities,
	 * relative liabilities, and clearing payments.
	 * 
	 * It's more efficient to store this in a separate List than to store Maps in
	 * each of the other lists.
	 */
	// private List<List<Integer>> liabilitiesIndex;
	private List<TIntArrayList> liabilitiesIndex;
	// private List<Float> totalLiabilitiesOfNode; // total obligation vector
	private TFloatArrayList totalLiabilitiesOfNode; // total obligation vector
	/**
	 * The node link details to easily identify which nodes owe money to this node.
	 */
	private ArrayList<ArrayList<NodeLink>> receivablesIndex;
	// private List<Float> totalOwedToNode; // amount owed to node, assuming no
	// defaults
	private TFloatArrayList totalOwedToNode; // amount owed to node, assuming no defaults
	/// private List<List<Float>> clearingPaymentAmount; // to work out the exact
	/// amounts being paid between parties
	private List<TFloatArrayList> clearingPaymentAmount; // to work out the exact amounts being paid between parties
	// private List<Float> clearingPaymentVector; // sum of the rows in the
	// clearingPaymentMatrix
	private TFloatArrayList clearingPaymentVector; // sum of the rows in the clearingPaymentMatrix
	// private List<Float> equityOfNode; // net cash flow of each node after paying
	// liabilities
	private TFloatArrayList equityOfNode; // net cash flow of each node after paying liabilities
	/**
	 * Which round of the clearing vector algorithm caused the node to default.<br>
	 * (0 = no default)
	 */
	// private List<Integer> defaultOrderOfNode;
	private TIntArrayList defaultOrderOfNode;
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
		this.receivablesIndex = null;
		this.totalOwedToNode = null;
		this.clearingPaymentAmount = null;
		this.clearingPaymentVector = null;
		this.equityOfNode = null;
		this.defaultOrderOfNode = null;
		this.agentCount = 0;
	}

	public ClearingPaymentOutputs calculate(List<List<Float>> liabilitiesAmounts,
			List<List<Integer>> liabilitiesIndices, List<Float> operatingCashFlow, List<Float> liquidAssets) {

		List<TFloatArrayList> troveLiabilitiesAmounts = new ArrayList<TFloatArrayList>(liabilitiesAmounts.size());
		for (int i = 0; i < liabilitiesAmounts.size(); i++) {
			float[] liabAmtArray = new float[liabilitiesAmounts.get(i).size()];
			int j = 0;
			for (Float node : liabilitiesAmounts.get(i)) {
				liabAmtArray[j++] = (node != null ? node : 0f);
			}
			troveLiabilitiesAmounts.add(TFloatArrayList.wrap(liabAmtArray));
		}

		List<TIntArrayList> troveLiabilitiesIndices = new ArrayList<TIntArrayList>(liabilitiesIndices.size());
		for (int i = 0; i < liabilitiesIndices.size(); i++) {
			int[] liabIdxArray = new int[liabilitiesIndices.get(i).size()];
			int j = 0;
			for (Integer node : liabilitiesIndices.get(i)) {
				liabIdxArray[j++] = (node != null ? node : 0);
			}
			troveLiabilitiesIndices.add(TIntArrayList.wrap(liabIdxArray));
		}

		float[] primitiveArray = new float[operatingCashFlow.size()];
		int i = 0;
		for (Float node : operatingCashFlow) {
			primitiveArray[i++] = (node != null ? node : 0f);
		}
		TFloatArrayList troveOperatingCashFlow = TFloatArrayList.wrap(primitiveArray);

		float[] assetsArray = new float[liquidAssets.size()];
		i = 0;
		for (Float node : liquidAssets) {
			assetsArray[i++] = (node != null ? node : 0f);
		}
		TFloatArrayList troveLiquidAssets = TFloatArrayList.wrap(assetsArray);

		return this.calculate(troveLiabilitiesAmounts, troveLiabilitiesIndices, troveOperatingCashFlow,
				troveLiquidAssets);
	}

	/**
	 * Calculates the payments that will clear the whole economy, noting in which
	 * order nodes defaulted.
	 * 
	 * N.B. In the event of an ADI defaulting, the caller needs to apply the
	 * Financial Claims Scheme (FCS) rules when calculating the inputs for this
	 * method. This assumes that an ADI default is an exogeneous event, which is
	 * partly true because the government declares when the FCS applies - not the
	 * ADI or even the market. An ADI would typically be declared before it is
	 * technically insolvent, so should never get to the point where this payment
	 * clearing vector algorithm finds it defaulting on its obligations.
	 * 
	 * NOTE: The caller needs to subtract the liquid assets from the CPV outputs.
	 * This algorithm includes them as if they're an exogeneous cash flow. That
	 * ensures that nodes don't default just because they made a loss in a single
	 * period - they only default when they make a loss that's big enough to use up
	 * all their liquid assets too.
	 * 
	 * @param liabilitiesAmounts - amounts owed by every node to the other nodes
	 *                           they're connected to
	 * @param liabilitiesIndices - indices of the sub-list to designate which node
	 *                           they relate to
	 * @param operatingCashFlow  - exogeneous cash inflows to each node
	 * @param liquidAssets       - cash at bank, etc. which can buffer a node
	 *                           against defaulting if their liabilities exceed
	 *                           their income in this iteration.
	 * @return a map containing:<br>
	 *         List<Float> ClearingPaymentVector,<br>
	 *         List<List<Float>> ClearingPaymentMatrix,<br>
	 *         List<List<Integer>> ClearingPaymentIndices,<br>
	 *         List<Float> NodeEquity, and<br>
	 *         List<Integer> NodeDefaultOrder.
	 * @author Adam Struthers
	 * @since 2019-03-18
	 */
	public ClearingPaymentOutputs calculate(List<TFloatArrayList> liabilitiesAmounts,
			List<TIntArrayList> liabilitiesIndices, TFloatArrayList operatingCashFlow, TFloatArrayList liquidAssets) {
		System.gc();

		ClearingPaymentOutputs result = null;

		if (liabilitiesAmounts.size() == liabilitiesIndices.size()
				&& liabilitiesAmounts.size() == operatingCashFlow.size()
				&& liabilitiesAmounts.size() == liquidAssets.size()) {
			// must be the same number of agents in each argument

			// set input values
			this.agentCount = liabilitiesAmounts.size();
			this.exogeneousNominalCashFlow = operatingCashFlow;
			for (int i = 0; i < liquidAssets.size(); i++) {
				this.exogeneousNominalCashFlow.set(i, this.exogeneousNominalCashFlow.get(i) + liquidAssets.get(i));
			}
			this.nominalLiabilitiesAmount = liabilitiesAmounts;
			this.liabilitiesIndex = liabilitiesIndices;

			// store the links to the nodes, based on links from each node
			this.receivablesIndex = new ArrayList<ArrayList<NodeLink>>(this.agentCount);
			for (int fromIdx = 0; fromIdx < this.agentCount; fromIdx++) {
				// initialise
				this.receivablesIndex.add(new ArrayList<NodeLink>(20));
			}
			for (int fromIdx = 0; fromIdx < this.agentCount; fromIdx++) {
				// store node links
				for (int to = 0; to < this.liabilitiesIndex.get(fromIdx).size(); to++) {
					int toIdx = this.liabilitiesIndex.get(fromIdx).get(to);
					NodeLink link = new NodeLink(fromIdx, to);
					this.receivablesIndex.get(toIdx).add(link);
				}
			}
			for (int fromIdx = 0; fromIdx < this.agentCount; fromIdx++) {
				// trim to reduce memory footprint
				this.receivablesIndex.get(fromIdx).trimToSize();
			}

			long memoryBefore = 0L;
			long memoryAfter = 0L;
			DecimalFormat formatter = new DecimalFormat("#,##0.00");
			if (DEBUG_RAM_USAGE) {
				System.gc();
				memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
				float megabytesAfter = memoryAfter / 1024f / 1024f;
				System.out.println(new Date(System.currentTimeMillis())
						+ ": MEMORY USAGE BEFORE CACLULATING LIABILITIES: " + formatter.format(megabytesAfter) + "MB");
				memoryBefore = memoryAfter;
			}

			// calculate clearing payment vector
			System.out.println(new Date(System.currentTimeMillis()) + ": CPV calculating liabilities");
			this.calculateLiabilities();
			if (DEBUG_RAM_USAGE) {
				System.gc();
				memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
				float megabytesAfter = memoryAfter / 1024f / 1024f;
				float megabytesBefore = memoryBefore / 1024f / 1024f;
				System.out
						.println(new Date(System.currentTimeMillis()) + ": MEMORY CONSUMED AFTER PREPARING CPV INPUTS: "
								+ formatter.format(megabytesAfter - megabytesBefore) + "MB (CURRENT TOTAL IS: "
								+ formatter.format(megabytesAfter) + "MB)");
			}
			System.gc();
			System.out.println(new Date(System.currentTimeMillis()) + ": CPV calculating payments");
			this.calculatePayments();

			// MAYBE: save output to file for analysis

			/**
			 * I only really care about total cash in & out of each node ... maybe even only
			 * the net amount. Can probably exclude the indices and matrix from the return
			 * value. Reducing the amount of data returned will help both memory footprint
			 * and speed.
			 */

			// return output to caller
			// Map<String, Object> result = null;
			// result = new HashMap<String, Object>((int) Math.ceil(5f / 0.75f));
			result = new ClearingPaymentOutputs();
			// result.put("ClearingPaymentVector", this.clearingPaymentVector);
			// result.put("ClearingPaymentMatrix", this.clearingPaymentAmount);
			// result.put("ClearingPaymentIndices", this.liabilitiesIndex);
			result.setEquityOfNode(this.equityOfNode); // net cash flow
			result.setDefaultOrderOfNode(this.defaultOrderOfNode);
		}
		return result;
	}

	public String saveCpvOutoutsToFile(int iteration) {
		// MAYBE: implement saveCpvOutoutsToFile(int)
		String filename = "CPV_output" + iteration + ".csv";

		return filename;
	}

	/**
	 * Releases memory for everything other than the variables that are returned to
	 * the caller.
	 */
	public void clearInputsAndWorking() {
		this.exogeneousNominalCashFlow.clear();
		this.exogeneousNominalCashFlow = null;
		for (int i = 0; i < this.nominalLiabilitiesAmount.size(); i++) {
			this.nominalLiabilitiesAmount.get(i).clear();
			this.nominalLiabilitiesAmount.set(i, null);
		}
		this.nominalLiabilitiesAmount.clear();
		this.nominalLiabilitiesAmount = null;
		for (int i = 0; i < this.relativeLiabilitiesAmount.size(); i++) {
			this.relativeLiabilitiesAmount.get(i).clear();
			this.relativeLiabilitiesAmount.set(i, null);
		}
		this.relativeLiabilitiesAmount.clear();
		this.relativeLiabilitiesAmount = null;
		for (int i = 0; i < this.liabilitiesIndex.size(); i++) {
			this.liabilitiesIndex.get(i).clear();
			this.liabilitiesIndex.set(i, null);
		}
		this.liabilitiesIndex.clear();
		this.liabilitiesIndex = null;
		this.totalLiabilitiesOfNode.clear();
		this.totalLiabilitiesOfNode = null;
		for (int i = 0; i < this.receivablesIndex.size(); i++) {
			this.receivablesIndex.get(i).clear();
			this.receivablesIndex.set(i, null);
		}
		this.receivablesIndex.clear();
		this.receivablesIndex = null;
		this.totalOwedToNode.clear();
		this.totalOwedToNode = null;
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
	 * 
	 * @author Adam Struthers
	 * @since 2019-03-18
	 */
	private void calculatePayments() {
		System.gc(); // it's a memory hog, so clean up first

		// initialise variables, making copies so we don't alter the originals
		TFloatArrayList oldPaymentClearingVector = new TFloatArrayList(this.totalLiabilitiesOfNode);
		this.clearingPaymentVector = new TFloatArrayList(this.totalLiabilitiesOfNode);
		int[] zeros = new int[this.agentCount];
		Arrays.fill(zeros, 0);
		this.defaultOrderOfNode = new TIntArrayList(zeros); // no default

		// iteratively calculate payment clearing vector
		boolean systemCleared = false; // FIXME: this was true. Make it false
		int order = 0;
		if (DEBUG_DEFAULTS) {
			System.out.println("order " + order);
			System.out.println("this.agentCount " + this.agentCount);
			System.out.println("systemCleared " + systemCleared);
			for (int i = 0; i < oldPaymentClearingVector.size(); i++) {
				System.out.println("old CPV[" + i + "] = " + oldPaymentClearingVector.get(i));
			}
			for (int i = 0; i < this.clearingPaymentVector.size(); i++) {
				System.out.println("this CPV[" + i + "] = " + this.clearingPaymentVector.get(i));
			}
			for (int i = 0; i < this.defaultOrderOfNode.size(); i++) {
				System.out.println("default order[" + i + "] = " + this.defaultOrderOfNode.get(i));
			}
		}
		while ((order < this.agentCount) && !systemCleared) {
			if (DEBUG_DEFAULTS) {
				System.out.println("Calculating CPV for round " + order + " (i.e. default order " + order + ")");
			}
			// 2. If some nodes default even when all the other nodes pay, try to solve the
			// system again, assuming that only these "first-order" default occur. If only
			// first-order defaults occur under this new clearing vector, then terminate the
			// algorithm.
			// 3. If second-order defaults occur, then try to clear again
			// assuming only second-order defaults occur, and so on.
			order++;
			systemCleared = true;
			for (int fromIdx = 0; fromIdx < this.agentCount; fromIdx++) {
				if (DEBUG_DEFAULTS) {
					System.out.println("fromIdx " + fromIdx);
				}

				// use clearing payment vector and rel liab matrix to calc total paid to node
				float paidToNode = 0f;
				for (int link = 0; link < this.receivablesIndex.get(fromIdx).size(); link++) {
					int from = this.receivablesIndex.get(fromIdx).get(link).getFromIndex();
					int to = this.receivablesIndex.get(fromIdx).get(link).getTo();
					paidToNode += oldPaymentClearingVector.get(from) * this.relativeLiabilitiesAmount.get(from).get(to);

					if (DEBUG_DEFAULTS) {
						System.out.println("link " + link);
						System.out.println("from " + from);
						System.out.println("to " + to);
						System.out.println(
								"oldPaymentClearingVector.get(from) * this.relativeLiabilitiesAmount.get(from).get(to) "
										+ oldPaymentClearingVector.get(from)
												* this.relativeLiabilitiesAmount.get(from).get(to));
						System.out.println("paidToNode " + paidToNode);
					}
				}

				/**
				 * Need to add in each node's cash balance because they don't default simply
				 * because they have negative net income this iteration - they default if they
				 * run out of cash too.
				 */

				if (DEBUG_DEFAULTS) {
					System.out.println("this.exogeneousNominalCashFlow.get(" + fromIdx + "): "
							+ this.exogeneousNominalCashFlow.get(fromIdx));
					System.out.println("paidToNode: " + paidToNode);
					System.out.println("this.totalLiabilitiesOfNode.get(" + fromIdx + "): "
							+ this.totalLiabilitiesOfNode.get(fromIdx));
				}
				// check for negative equity to see who defaulted in this round
				// if (this.exogeneousNominalCashFlow.get(fromIdx) + paidToNode <
				// this.totalLiabilitiesOfNode
				// .get(fromIdx)) {
				if (this.exogeneousNominalCashFlow.get(fromIdx) + paidToNode < oldPaymentClearingVector.get(fromIdx)) {
					// node defaulted, so reduce its payment so that its cash flow to equity is zero
					systemCleared = false;
					this.defaultOrderOfNode.set(fromIdx, order); // node defaulted in this round of algorithm
					this.clearingPaymentVector.set(fromIdx, this.exogeneousNominalCashFlow.get(fromIdx) + paidToNode);
					if (DEBUG_DEFAULTS) {
						System.out.println(
								"INSIDE DEFAULT IF STATEMENT (fromIdx = " + fromIdx + ", order = " + order + ")");
					}
				} else {
					this.clearingPaymentVector.set(fromIdx, oldPaymentClearingVector.get(fromIdx));
					if (DEBUG_DEFAULTS) {
						System.out.println(
								"INSIDE DEFAULT ELSE STATEMENT (fromIdx = " + fromIdx + ", order = " + order + ")");
					}
				}
			}
			if (!systemCleared) {
				// reset old payment clearing vector ready for the next round
				oldPaymentClearingVector = new TFloatArrayList(this.clearingPaymentVector);
			}
		}

		// calculate payment clearing matrix and equity of each node
		this.clearingPaymentAmount = new ArrayList<TFloatArrayList>(this.agentCount);
		for (int fromIdx = 0; fromIdx < this.agentCount; fromIdx++) {
			this.clearingPaymentAmount.add(new TFloatArrayList(this.nominalLiabilitiesAmount.get(fromIdx).size()));
		}
		this.equityOfNode = new TFloatArrayList(this.agentCount);
		for (int fromIdx = 0; fromIdx < this.agentCount; fromIdx++) {
			// amounts paid by this node
			for (int to = 0; to < this.nominalLiabilitiesAmount.get(fromIdx).size(); to++) {
				this.clearingPaymentAmount.get(fromIdx).add(
						this.clearingPaymentVector.get(fromIdx) * this.relativeLiabilitiesAmount.get(fromIdx).get(to));
			}

			// amounts received by this node
			float paymentReceived = 0f;
			for (int link = 0; link < this.receivablesIndex.get(fromIdx).size(); link++) {
				int from = this.receivablesIndex.get(fromIdx).get(link).getFromIndex();
				int to = this.receivablesIndex.get(fromIdx).get(link).getTo();
				paymentReceived += this.clearingPaymentVector.get(from)
						* this.relativeLiabilitiesAmount.get(from).get(to);
			}

			// equity of this node
			this.equityOfNode.add(paymentReceived + this.exogeneousNominalCashFlow.get(fromIdx)
					- this.clearingPaymentVector.get(fromIdx));
		}
	}

	/**
	 * Calculates the total contractual liabilities (and receivables) of each node,
	 * and the relative amount owed to each other node.
	 * 
	 * @author Adam Struthers
	 * @since 2019-03-18
	 */
	private void calculateLiabilities() {
		this.enforceLiabilityMatrixConstraints();
		this.totalOwedToNode = new TFloatArrayList(this.agentCount);
		this.totalLiabilitiesOfNode = new TFloatArrayList(this.agentCount);
		this.relativeLiabilitiesAmount = new ArrayList<TFloatArrayList>(this.agentCount);
		for (int fromIdx = 0; fromIdx < this.agentCount; fromIdx++) {
			// initialise array
			int toSize = this.liabilitiesIndex.get(fromIdx).size();
			this.relativeLiabilitiesAmount.add(new TFloatArrayList(toSize));
		}
		for (int fromIdx = 0; fromIdx < this.agentCount; fromIdx++) {
			// calculate contractual liabilities
			float liabilities = 0f;
			for (int to = 0; to < this.nominalLiabilitiesAmount.get(fromIdx).size(); to++) {
				liabilities += this.nominalLiabilitiesAmount.get(fromIdx).get(to);
			}
			this.totalLiabilitiesOfNode.add(liabilities);

			// calculate contractual receivables
			float receivables = 0f;
			for (int link = 0; link < this.receivablesIndex.get(fromIdx).size(); link++) {
				int from = this.receivablesIndex.get(fromIdx).get(link).getFromIndex();
				int to = this.receivablesIndex.get(fromIdx).get(link).getTo();
				receivables += this.nominalLiabilitiesAmount.get(from).get(to);
			}
			// this.totalOwedToNode.set(fromIdx, receivables);
			this.totalOwedToNode.add(receivables);

			// calculate relative liabilities
			for (int to = 0; to < this.nominalLiabilitiesAmount.get(fromIdx).size(); to++) {
				if (liabilities > 0f) {
					// this.relativeLiabilitiesAmount.get(fromIdx).set(to,
					// this.nominalLiabilitiesAmount.get(fromIdx).get(to) / liabilities);
					this.relativeLiabilitiesAmount.get(fromIdx)
							.add(this.nominalLiabilitiesAmount.get(fromIdx).get(to) / liabilities);
				} else {
					// this.relativeLiabilitiesAmount.get(fromIdx).set(to, 0f);
					this.relativeLiabilitiesAmount.get(fromIdx).add(0f);
				}
			}
		}
	}

	/**
	 * All elements non-negative, and diagonal is 0.
	 * 
	 * @author Adam Struthers
	 * @since 2019-03-18
	 */
	private void enforceLiabilityMatrixConstraints() {
		for (int fromIdx = 0; fromIdx < this.agentCount; fromIdx++) {
			for (int to = 0; to < this.nominalLiabilitiesAmount.get(fromIdx).size(); to++) {
				int toIdx = this.liabilitiesIndex.get(fromIdx).get(to);
				if (fromIdx == toIdx) {
					// set diagonal to zero
					this.nominalLiabilitiesAmount.get(fromIdx).set(to, 0f);
				}
				if (this.nominalLiabilitiesAmount.get(fromIdx).get(to) < 0f) {
					// ensure amounts are non-negative
					this.nominalLiabilitiesAmount.get(fromIdx).set(to,
							-this.nominalLiabilitiesAmount.get(fromIdx).get(to));
				}
			}
		}
	}

	/**
	 * A utility class to make it easier to look up which nodes owe money to a given
	 * node. The fromIndex and toIndex correspond to coordinates in the
	 * nominalLiabilitiesAmount nested Lists.
	 * 
	 * @author Adam Struthers
	 * @since 2019-03-18
	 */
	private class NodeLink {
		final int fromIndex;
		final int to;

		protected NodeLink(final int fromNodeIndex, final int to) {
			super();
			this.fromIndex = fromNodeIndex;
			this.to = to;
		}

		protected int getFromIndex() {
			return this.fromIndex;
		}

		protected int getTo() {
			return this.to;
		}
	}

}
