package xyz.struthers.rhul.ham;

import java.util.ArrayList;
import java.util.List;

import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TIntArrayList;
import xyz.struthers.rhul.ham.process.ClearingPaymentOutputs;
import xyz.struthers.rhul.ham.process.ClearingPaymentVector;

public class CpvTestHarness {

	public CpvTestHarness() {
		super();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// prepare variables with test data
		List<TFloatArrayList> liabilitiesAmounts = new ArrayList<TFloatArrayList>(5);
		List<TIntArrayList> liabilitiesIndices = new ArrayList<TIntArrayList>(5);
		TFloatArrayList operatingCashFlow = new TFloatArrayList(new float[] { 100f, 100f, 100f, 100f, 10f });
		TFloatArrayList liquidAssets = new TFloatArrayList(new float[] { 50f, 50f, 50f, 50f, 0f });
		int iteration = 5;

		liabilitiesAmounts.add(new TFloatArrayList(new float[] { 25f, 25f, 25f }));
		liabilitiesAmounts.add(new TFloatArrayList(new float[] { 50f, 50f, 50f }));
		liabilitiesAmounts.add(new TFloatArrayList(new float[] { 30f, 90f }));
		liabilitiesAmounts.add(new TFloatArrayList(new float[] { 40f, 40f, 40f, 40f }));
		liabilitiesAmounts.add(new TFloatArrayList(new float[] { 150f, 100f }));

		liabilitiesIndices.add(new TIntArrayList(new int[] { 1, 2, 4 }));
		liabilitiesIndices.add(new TIntArrayList(new int[] { 0, 2, 4 }));
		liabilitiesIndices.add(new TIntArrayList(new int[] { 0, 4 }));
		liabilitiesIndices.add(new TIntArrayList(new int[] { 0, 1, 2, 4 }));
		liabilitiesIndices.add(new TIntArrayList(new int[] { 1, 2 }));

		ClearingPaymentVector cpv = new ClearingPaymentVector();
		ClearingPaymentOutputs cpvOutput = cpv.calculate(liabilitiesAmounts, liabilitiesIndices, operatingCashFlow,
				liquidAssets);
		cpvOutput.setIteration(iteration);

		TFloatArrayList equityOfNode = cpvOutput.getEquityOfNode();
		// Which round of the CPV algorithm caused the node to default (0 = no default)
		TIntArrayList defaultOrderOfNode = cpvOutput.getDefaultOrderOfNode();
		int outputIteration = cpvOutput.getIteration();

		System.out.println("Iteration: " + outputIteration);
		for (int i = 0; i < 5; i++) {
			System.out.println("Equity of node " + i + ": " + equityOfNode.get(i));
			System.out.println("Default order of node " + i + ": " + defaultOrderOfNode.get(i));
		}
	}

}
