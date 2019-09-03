/**
 * 
 */
package xyz.struthers.rhul.ham;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;

import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TIntArrayList;
import xyz.struthers.rhul.ham.process.ClearingPaymentOutputs;
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
	public ClearingPaymentOutputs calculateClearingPaymentVector(List<TFloatArrayList> liabilitiesAmounts,
			List<TIntArrayList> liabilitiesIndices, TFloatArrayList operatingCashFlow, TFloatArrayList liquidAssets,
			int iteration) {

		DecimalFormat formatter = new DecimalFormat("#,##0.00");
		long memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		float megabytesBefore = memoryBefore / 1024f / 1024f;
		System.out.println(new Date(System.currentTimeMillis()) + ": *** MEMORY USAGE BEFORE CALCULATING CPV *** : "
				+ formatter.format(megabytesBefore) + "MB");

		ClearingPaymentVector payment = new ClearingPaymentVector();

		ClearingPaymentOutputs result = payment.calculate(liabilitiesAmounts, liabilitiesIndices, operatingCashFlow,
				liquidAssets);
		result.setIteration(iteration);

		payment.clearInputsAndWorking();
		payment = null;
		System.gc();

		long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		float megabytesAfter = memoryAfter / 1024f / 1024f;
		megabytesBefore = memoryBefore / 1024f / 1024f;
		System.out.println(new Date(System.currentTimeMillis()) + ": *** MEMORY CONSUMED BY CALCULATING CPV *** : "
				+ formatter.format(megabytesAfter - megabytesBefore) + "MB (CURRENT TOTAL IS: "
				+ formatter.format(megabytesAfter) + "MB)");
		memoryBefore = memoryAfter;

		return result;
	}

}
