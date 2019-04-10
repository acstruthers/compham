/**
 * 
 */
package xyz.struthers.rhul.ham;

import java.util.List;
import java.util.Map;

import xyz.struthers.rhul.ham.process.ClearingPaymentVector;

/**
 * Calculates the Clearing Payments Vector (CPV) fir a single period.
 * 
 * The main program runs out of RAM, so I've split it up so that this class
 * calculates the Clearing Payments Vector (CPV) and passes the result back to
 * the caller. It has no dependencies on the agents themselves, so can be run
 * without the agents in memory.
 * 
 * @author Adam Struthers
 * @since 10-Apr-2019
 */
public class RunSimulation {

	public RunSimulation() {
		super();
	}

	/**
	 * Runs the Clearing Payment Vector algorithm for a given set of input data.
	 * Clears as much memory as possible when it's finished, including calling the
	 * garbage collector.
	 * 
	 * @param liabilitiesAmounts - amounts owed by every node to the other nodes
	 *                           they're connected to
	 * @param liabilitiesIndices - indices of the sub-list to designate which node
	 *                           they relate to
	 * @param operatingCashFlow  - exogeneous cash inflows to each node
	 * @param iteration          - the iteration number (i.e. which month this is
	 *                           being calculated for)
	 * @return a map containing:<br>
	 *         List<Float> ClearingPaymentVector,<br>
	 *         List<List<Float>> ClearingPaymentMatrix,<br>
	 *         List<List<Integer>> ClearingPaymentIndices,<br>
	 *         List<Float> NodeEquity, and<br>
	 *         List<Integer> NodeDefaultOrder.
	 */
	public Map<String, Object> calculateClearingPaymentVector(List<List<Float>> liabilitiesAmounts,
			List<List<Integer>> liabilitiesIndices, List<Float> operatingCashFlow, int iteration) {

		ClearingPaymentVector payment = new ClearingPaymentVector();

		Map<String, Object> result = payment.calculate(liabilitiesAmounts, liabilitiesIndices, operatingCashFlow);

		payment.clearInputsAndWorking();
		payment = null;
		System.gc();

		return result;
	}

}
