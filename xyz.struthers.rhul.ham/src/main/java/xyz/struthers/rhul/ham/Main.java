/**
 * 
 */
package xyz.struthers.rhul.ham;

import java.text.DecimalFormat;
import java.util.Map;

import xyz.struthers.rhul.ham.process.ClearingPaymentInputs;

/**
 * @author acstr
 *
 */
public class Main {

	public Main() {
		super();
	}

	/**
	 * Runs the simulation in a series of stages, releasing memory as it goes so it
	 * will run on a computer with 32GB of RAM.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		InitialiseEconomy init = new InitialiseEconomy();
		ClearingPaymentInputs cpvInputs = init.initialiseEconomy();
		System.gc();

		DecimalFormat formatter = new DecimalFormat("#,##0.00");
		long memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		float megabytesBefore = memoryBefore / 1024f / 1024f;
		System.out.println("MEMORY USAGE IN MAIN AFTER MAKING CPV INPUTS AND DROPPING AGENTS: "
				+ formatter.format(megabytesBefore) + "MB");

		int iteration = 0;
		RunSimulation sim = new RunSimulation();
		Map<String, Object> cpvOutputs = sim.calculateClearingPaymentVector(cpvInputs.getLiabilitiesAmounts(),
				cpvInputs.getLiabilitiesIndices(), cpvInputs.getOperatingCashFlow(), iteration);

		cpvInputs.clear();
		cpvInputs = null;
		System.gc();
		long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		float megabytesAfter = memoryAfter / 1024f / 1024f;
		System.out.println("MEMORY USAGE IN MAIN AFTER CALCULATING CPV: " + formatter.format(megabytesAfter) + "MB)");
		memoryBefore = memoryAfter;

		// TODO process CPV outputs
		// read agents back in from object file, then update financial statements
	}

}
