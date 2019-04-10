/**
 * 
 */
package xyz.struthers.rhul.ham;

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

		int iteration = 0;
		RunSimulation sim = new RunSimulation();
		sim.calculateClearingPaymentVector(cpvInputs.getLiabilitiesAmounts(), cpvInputs.getLiabilitiesIndices(),
				cpvInputs.getOperatingCashFlow(), iteration);

		// TODO process CPV outputs
		// read agents back in from object file, then update financial statements
	}

}
