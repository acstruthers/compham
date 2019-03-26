/**
 * 
 */
package xyz.struthers.rhul.ham.process;

import java.util.HashMap;
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
 * If an ADI defaults, apply the govt g'tee rules to customer's deposits.
 * Not just the $250k limit per account, but also the $15Bn limit per ADI
 * (double-check those amounts).
 * 
 * Use EJML or JAMA for matrix maths. Will be at least as fast as my code.
 * 
 * Refactor to use lists of connected nodes only (27 million nodes need
 * over 5.5 PB of RAM).
 * 
 * @author Adam Struthers
 * @version 0.1
 * @since 12-Nov-2018
 * @deprecated
 */
public class ClearingPaymentMatrixDEPRECATED {

	// We know the size in advance, so can use arrays to improve speed & reduce
	// memory usage
	// these don't change
	private double[] exogeneousNominalCashFlow; // non-negative cash inflows (i.e. income)
	private double[][] nominalLiabilitiesMatrix; // payments owed in this time period only
	private double[][] relativeLiabilitiesMatrix;
	private double[] totalLiabilitiesOfNode; // total obligation vector
	private double[] totalOwedToNode; // amount owed to node, assuming no defaults
	private double[][] clearingPaymentMatrix; // to work out the exact amounts being paid between parties
	private double[] clearingPaymentVector; // sum of the rows in the clearingPaymentMatrix
	private double[] equityOfNode; // surplus cash flow of each node after paying liabilities
	private int[] defaultOrderOfNode; // which round of the clearing vector algorithm caused the node to default (0 =
										// no default)
	private int matrixSize; // the number of elements in the square matrix

	public ClearingPaymentMatrixDEPRECATED() {
		super();
	}

	/**
	 * 
	 * 
	 * @author Adam
	 * @param double[i][j] nominalLiabilitiesMatrix from node i to node j
	 * @param double[] exogeneougOperatingCashFlow to each node
	 * @return a Map with: double[][] ClearingPaymentMatrix, double[]
	 *         ClearingPaymentVector, double[] NodeEquity, int[] NodeDefaultOrder
	 */
	public Map<String, Object> calculate(double[][] liabilities, double[] operatingCashFlow) {
		Map<String, Object> result = null;
		if (liabilities.length == liabilities[0].length && liabilities.length == operatingCashFlow.length) {

			// set input values
			this.matrixSize = liabilities.length;
			this.exogeneousNominalCashFlow = operatingCashFlow;
			this.nominalLiabilitiesMatrix = liabilities;

			// calculate clearing payment vector
			this.calculateLiabilities();
			this.calculatePayments();

			// save output to file for analysis

			// return output to caller
			result = new HashMap<String, Object>();
			result.put("ClearingPaymentMatrix", this.clearingPaymentMatrix); // check that it's happy with me
																				// giving it a primitive not a class
			result.put("ClearingPaymentVector", this.clearingPaymentVector);
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
		double[] oldPaymentClearingVector = new double[this.matrixSize];
		this.clearingPaymentVector = new double[this.matrixSize];
		this.defaultOrderOfNode = new int[this.matrixSize];
		for (int i = 0; i < this.matrixSize; i++) {
			// make a copy so we don't alter the original
			oldPaymentClearingVector[i] = this.totalLiabilitiesOfNode[i];
			this.clearingPaymentVector[i] = this.totalLiabilitiesOfNode[i];
			this.defaultOrderOfNode[i] = 0; // no default
		}

		// iteratively calculate payment clearing vector
		boolean systemCleared = true;
		int order = 0;
		while (order < this.matrixSize && !systemCleared) {
			// 2. If some nodes default even when all the other nodes pay, try to solve the
			// system again, assuming that only these "first-order" default occur. If only
			// first-order defaults occur under this new clearing vector, then terminate the
			// algorithm.
			// 3. If second-order defaults occur, then try to clear again
			// assuming only second-order defaults occur, and so on.
			order++;
			systemCleared = true;
			for (int i = 0; i < this.matrixSize; i++) {
				// use clearing payment vector and rel liab matrix to calc total paid to node
				double paidToNode = 0d;
				for (int j = 0; j < this.matrixSize; j++) {
					paidToNode += oldPaymentClearingVector[j] * this.relativeLiabilitiesMatrix[j][i];
				}
				// check for negative equity to see who defaulted in this round
				if (this.exogeneousNominalCashFlow[i] + paidToNode < this.totalLiabilitiesOfNode[i]) {
					// node defaulted, so reduce its payment so that its cash flow to equity is zero
					systemCleared = false;
					this.defaultOrderOfNode[i] = order; // node defaulted in this round of algorithm
					this.clearingPaymentVector[i] = this.exogeneousNominalCashFlow[i] + paidToNode;
				} else {
					this.clearingPaymentVector[i] = oldPaymentClearingVector[i];
				}
			}
			if (!systemCleared) {
				for (int i = 0; i < this.matrixSize; i++) {
					// reset old payment clearing vector ready for the next round
					oldPaymentClearingVector[i] = this.clearingPaymentVector[i];
				}
			}
		}

		// calculate payment clearing matrix and equity of each node
		this.clearingPaymentMatrix = new double[this.matrixSize][this.matrixSize];
		this.equityOfNode = new double[this.matrixSize];
		for (int i = 0; i < this.matrixSize; i++) {
			double paymentReceived = 0d;
			for (int j = 0; j < this.matrixSize; j++) {
				this.clearingPaymentMatrix[i][j] += this.clearingPaymentVector[i]
						* this.relativeLiabilitiesMatrix[i][j];
				paymentReceived += this.clearingPaymentVector[j] * this.relativeLiabilitiesMatrix[j][i];
			}
			this.equityOfNode[i] = paymentReceived + this.exogeneousNominalCashFlow[i] - this.clearingPaymentVector[i];
		}
	}

	/**
	 * Calculates the total contractual liabilities of each node, and the relative
	 * amount owed to each other node.
	 */
	private void calculateLiabilities() {
		this.enforceLiabilityMatrixConstraints();
		this.totalOwedToNode = new double[this.matrixSize];
		this.totalLiabilitiesOfNode = new double[this.matrixSize];
		this.relativeLiabilitiesMatrix = new double[this.matrixSize][this.matrixSize];
		for (int i = 0; i < this.matrixSize; i++) {
			this.totalLiabilitiesOfNode[i] = 0d;
			this.totalOwedToNode[i] = 0d;
			for (int j = 0; j < this.matrixSize; j++) {
				this.totalLiabilitiesOfNode[i] += this.nominalLiabilitiesMatrix[i][j];
				this.totalOwedToNode[i] += this.nominalLiabilitiesMatrix[j][i];
			}
			for (int j = 0; j < this.matrixSize; j++) {
				if (this.totalLiabilitiesOfNode[i] > 0d) {
					this.relativeLiabilitiesMatrix[i][j] = this.nominalLiabilitiesMatrix[i][j]
							/ this.totalLiabilitiesOfNode[i];
				} else {
					this.relativeLiabilitiesMatrix[i][j] = 0d;
				}
			}
		}
	}

	/**
	 * All elements non-negative, and diagonal is 0.
	 */
	private void enforceLiabilityMatrixConstraints() {
		for (int i = 0; i < this.matrixSize; i++) {
			this.nominalLiabilitiesMatrix[i][i] = 0d;
			for (int j = 0; j < this.matrixSize; j++) {
				if (this.nominalLiabilitiesMatrix[i][j] < 0d) {
					this.nominalLiabilitiesMatrix[i][j] = -this.nominalLiabilitiesMatrix[i][j];
				}
			}
		}
	}
	/*
	 * private double[] vectorMin(double x[], double y[]) { double[] result = null;
	 * if (x.length == y.length) { int n = x.length; result = new double[n]; for
	 * (int i = 0; i < n; i++) { result[i] = Math.min(x[i], y[i]); } } return
	 * result; }
	 * 
	 * private double[] vectorMax(double x[], double y[]) { double[] result = null;
	 * if (x.length == y.length) { int n = x.length; result = new double[n]; for
	 * (int i = 0; i < n; i++) { result[i] = Math.max(x[i], y[i]); } } return
	 * result; }
	 * 
	 * private double[] vectorPositive(double x[]) { int n = x.length; double[]
	 * result = new double[n]; for (int i = 0; i < n; i++) { result[i] =
	 * Math.max(x[i], 0d); } return result; }
	 * 
	 * private double absSum(double[] x) { int n = x.length; double result = 0d; for
	 * (int i = 0; i < n; i++) { result += Math.abs(x[i]); } return result; }
	 */
}
