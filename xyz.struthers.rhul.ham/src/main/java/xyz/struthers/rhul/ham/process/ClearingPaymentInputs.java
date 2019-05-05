/**
 * 
 */
package xyz.struthers.rhul.ham.process;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TIntArrayList;

/**
 * Just a data structure so it's easy to return all three variables from a
 * single method call.
 * 
 * @author Adam Struthers
 * @since 10-Apr-2019
 */
public class ClearingPaymentInputs implements Serializable {

	private static final long serialVersionUID = 1L;

	// private List<List<Float>> liabilitiesAmounts;
	// private List<List<Integer>> liabilitiesIndices;
	// private List<Float> operatingCashFlow;
	// private List<Float> liquidAssets;
	private List<TFloatArrayList> liabilitiesAmounts;
	private List<TIntArrayList> liabilitiesIndices;
	private TFloatArrayList operatingCashFlow;
	private TFloatArrayList liquidAssets;
	private int iteration;

	public ClearingPaymentInputs() {
		super();
	}

	public ClearingPaymentInputs(List<List<Float>> liabilitiesAmounts, List<List<Integer>> liabilitiesIndices,
			List<Float> operatingCashFlow, List<Float> liquidAssets, int iteration) {
		// this.liabilitiesAmounts = liabilitiesAmounts;
		// this.liabilitiesIndices = liabilitiesIndices;
		// this.operatingCashFlow = operatingCashFlow;
		// this.liquidAssets = liquidAssets;
		this.iteration = iteration;

		this.liabilitiesAmounts = new ArrayList<TFloatArrayList>(liabilitiesAmounts.size());
		for (int i = 0; i < liabilitiesAmounts.size(); i++) {
			float[] liabAmtArray = new float[liabilitiesAmounts.get(i).size()];
			int j = 0;
			for (Float node : liabilitiesAmounts.get(i)) {
				liabAmtArray[j++] = (node != null ? node : 0f);
			}
			this.liabilitiesAmounts.add(TFloatArrayList.wrap(liabAmtArray));
		}
		this.liabilitiesIndices = new ArrayList<TIntArrayList>(liabilitiesIndices.size());
		for (int i = 0; i < liabilitiesIndices.size(); i++) {
			int[] liabIdxArray = new int[liabilitiesIndices.get(i).size()];
			int j = 0;
			for (Integer node : liabilitiesIndices.get(i)) {
				liabIdxArray[j++] = (node != null ? node : 0);
			}
			this.liabilitiesIndices.add(TIntArrayList.wrap(liabIdxArray));
		}
		float[] cashflowArray = new float[operatingCashFlow.size()];
		int i = 0;
		for (Float node : operatingCashFlow) {
			cashflowArray[i++] = (node != null ? node : 0f);
		}
		this.operatingCashFlow = TFloatArrayList.wrap(cashflowArray);
		float[] assetsArray = new float[liquidAssets.size()];
		i = 0;
		for (Float node : liquidAssets) {
			assetsArray[i++] = (node != null ? node : 0f);
		}
		this.liquidAssets = TFloatArrayList.wrap(assetsArray);
	}

	public void clear() {
		for (int i = 0; i < this.liabilitiesAmounts.size(); i++) {
			this.liabilitiesAmounts.get(i).clear();
			this.liabilitiesAmounts.set(i, null);
		}
		this.liabilitiesAmounts.clear();
		this.liabilitiesAmounts = null;
		for (int i = 0; i < this.liabilitiesIndices.size(); i++) {
			this.liabilitiesIndices.get(i).clear();
			this.liabilitiesIndices.set(i, null);
		}
		this.liabilitiesIndices.clear();
		this.liabilitiesIndices = null;
		this.operatingCashFlow.clear();
		this.operatingCashFlow = null;
	}

	/**
	 * @return the liabilitiesAmounts
	 */
	public List<List<Float>> getLiabilitiesAmounts() {
		// return liabilitiesAmounts;
		List<List<Float>> boxedList = new ArrayList<List<Float>>(liabilitiesAmounts.size());
		for (int i = 0; i < liabilitiesAmounts.size(); i++) {
			float[] primitiveArray = this.liabilitiesAmounts.get(i).toArray();
			List<Float> boxed = new ArrayList<Float>(primitiveArray.length);
			for (Float node : primitiveArray) {
				boxed.add(node);
			}
			boxedList.add(boxed);
		}
		return boxedList;
	}

	/**
	 * @param liabilitiesAmounts the liabilitiesAmounts to set
	 */
	public void setLiabilitiesAmounts(List<List<Float>> liabilitiesAmounts) {
		// this.liabilitiesAmounts = liabilitiesAmounts;
		this.liabilitiesAmounts = new ArrayList<TFloatArrayList>(liabilitiesAmounts.size());
		for (int i = 0; i < liabilitiesAmounts.size(); i++) {
			float[] liabAmtArray = new float[liabilitiesAmounts.get(i).size()];
			int j = 0;
			for (Float node : liabilitiesAmounts.get(i)) {
				liabAmtArray[j++] = (node != null ? node : 0f);
			}
			this.liabilitiesAmounts.add(TFloatArrayList.wrap(liabAmtArray));
		}
	}

	/**
	 * @return the liabilitiesIndices
	 */
	public List<List<Integer>> getLiabilitiesIndices() {
		// return liabilitiesIndices;
		List<List<Integer>> boxedList = new ArrayList<List<Integer>>(liabilitiesIndices.size());
		for (int i = 0; i < liabilitiesIndices.size(); i++) {
			int[] primitiveArray = this.liabilitiesIndices.get(i).toArray();
			List<Integer> boxed = new ArrayList<Integer>(primitiveArray.length);
			for (Integer node : primitiveArray) {
				boxed.add(node);
			}
			boxedList.add(boxed);
		}
		return boxedList;
	}

	/**
	 * @param liabilitiesIndices the liabilitiesIndices to set
	 */
	public void setLiabilitiesIndices(List<List<Integer>> liabilitiesIndices) {
		// this.liabilitiesIndices = liabilitiesIndices;
		this.liabilitiesIndices = new ArrayList<TIntArrayList>(liabilitiesIndices.size());
		for (int i = 0; i < liabilitiesIndices.size(); i++) {
			int[] liabIdxArray = new int[liabilitiesIndices.get(i).size()];
			int j = 0;
			for (Integer node : liabilitiesIndices.get(i)) {
				liabIdxArray[j++] = (node != null ? node : 0);
			}
			this.liabilitiesIndices.add(TIntArrayList.wrap(liabIdxArray));
		}
	}

	/**
	 * @return the operatingCashFlow
	 */
	public List<Float> getOperatingCashFlow() {
		// return operatingCashFlow;
		float[] primitiveArray = this.operatingCashFlow.toArray();
		List<Float> boxedList = new ArrayList<Float>(primitiveArray.length);
		for (Float node : primitiveArray) {
			boxedList.add(node);
		}
		return boxedList;
	}

	/**
	 * @param operatingCashFlow the operatingCashFlow to set
	 */
	public void setOperatingCashFlow(List<Float> operatingCashFlow) {
		// this.operatingCashFlow = operatingCashFlow;
		float[] cashflowArray = new float[operatingCashFlow.size()];
		int i = 0;
		for (Float node : operatingCashFlow) {
			cashflowArray[i++] = (node != null ? node : 0f);
		}
		this.operatingCashFlow = TFloatArrayList.wrap(cashflowArray);
	}

	/**
	 * @return the liquidAssets
	 */
	public List<Float> getLiquidAssets() {
		// return liquidAssets;
		float[] primitiveArray = this.liquidAssets.toArray();
		List<Float> boxedList = new ArrayList<Float>(primitiveArray.length);
		for (Float node : primitiveArray) {
			boxedList.add(node);
		}
		return boxedList;
	}

	/**
	 * @param liquidAssets the liquidAssets to set
	 */
	public void setLiquidAssets(List<Float> liquidAssets) {
		// this.liquidAssets = liquidAssets;
		float[] assetsArray = new float[liquidAssets.size()];
		int i = 0;
		for (Float node : liquidAssets) {
			assetsArray[i++] = (node != null ? node : 0f);
		}
		this.liquidAssets = TFloatArrayList.wrap(assetsArray);
	}

	/**
	 * @return the iteration
	 */
	public int getIteration() {
		return iteration;
	}

	/**
	 * @param iteration the iteration to set
	 */
	public void setIteration(int iteration) {
		this.iteration = iteration;
	}

}
