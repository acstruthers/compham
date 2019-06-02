/**
 * 
 */
package xyz.struthers.rhul.ham.process;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Just a data structure so it's easy to return all three variables from a
 * single method call.
 * 
 * @author Adam Struthers
 * @since 10-Apr-2019
 */
public class ClearingPaymentInputs implements Serializable {

	private static final long serialVersionUID = 1L;

	// using JDK Collections (slow)
	// private List<List<Float>> liabilitiesAmounts;
	// private List<List<Integer>> liabilitiesIndices;
	// private List<Float> operatingCashFlow;
	// private List<Float> liquidAssets;

	// using Trove (serialization fails)
	// private List<TFloatArrayList> liabilitiesAmounts;
	// private List<TIntArrayList> liabilitiesIndices;
	// private TFloatArrayList operatingCashFlow;
	// private TFloatArrayList liquidAssets;

	// using primitives
	private float[][] liabilitiesAmounts;
	private int[][] liabilitiesIndices;
	private float[] operatingCashFlow;
	private float[] liquidAssets;
	private int iteration;

	public ClearingPaymentInputs() {
		// if super-class is not serializable it will cause an exception
		// SOURCE:
		// https://stackoverflow.com/questions/12067405/deserializing-an-arraylist-no-valid-constructor
		// super();
	}

	public ClearingPaymentInputs(List<List<Float>> liabilitiesAmounts, List<List<Integer>> liabilitiesIndices,
			List<Float> operatingCashFlow, List<Float> liquidAssets, int iteration) {
		// this.liabilitiesAmounts = liabilitiesAmounts;
		// this.liabilitiesIndices = liabilitiesIndices;
		// this.operatingCashFlow = operatingCashFlow;
		// this.liquidAssets = liquidAssets;
		this.iteration = iteration;

		// using Trove collections
		/*
		 * this.liabilitiesAmounts = new
		 * ArrayList<TFloatArrayList>(liabilitiesAmounts.size()); for (int i = 0; i <
		 * liabilitiesAmounts.size(); i++) { float[] liabAmtArray = new
		 * float[liabilitiesAmounts.get(i).size()]; int j = 0; for (Float node :
		 * liabilitiesAmounts.get(i)) { liabAmtArray[j++] = (node != null ? node : 0f);
		 * } this.liabilitiesAmounts.add(TFloatArrayList.wrap(liabAmtArray)); }
		 * this.liabilitiesIndices = new
		 * ArrayList<TIntArrayList>(liabilitiesIndices.size()); for (int i = 0; i <
		 * liabilitiesIndices.size(); i++) { int[] liabIdxArray = new
		 * int[liabilitiesIndices.get(i).size()]; int j = 0; for (Integer node :
		 * liabilitiesIndices.get(i)) { liabIdxArray[j++] = (node != null ? node : 0); }
		 * this.liabilitiesIndices.add(TIntArrayList.wrap(liabIdxArray)); } float[]
		 * cashflowArray = new float[operatingCashFlow.size()]; int i = 0; for (Float
		 * node : operatingCashFlow) { cashflowArray[i++] = (node != null ? node : 0f);
		 * } this.operatingCashFlow = TFloatArrayList.wrap(cashflowArray); float[]
		 * assetsArray = new float[liquidAssets.size()]; i = 0; for (Float node :
		 * liquidAssets) { assetsArray[i++] = (node != null ? node : 0f); }
		 * this.liquidAssets = TFloatArrayList.wrap(assetsArray);
		 */

		// using primitives
		this.liabilitiesAmounts = new float[liabilitiesAmounts.size()][];
		for (int i = 0; i < liabilitiesAmounts.size(); i++) {
			this.liabilitiesAmounts[i] = new float[liabilitiesAmounts.get(i).size()];
			for (int j = 0; j < liabilitiesAmounts.get(i).size(); j++) {
				this.liabilitiesAmounts[i][j] = liabilitiesAmounts.get(i).get(j);
			}
		}
		this.liabilitiesIndices = new int[liabilitiesIndices.size()][];
		for (int i = 0; i < liabilitiesIndices.size(); i++) {
			this.liabilitiesIndices[i] = new int[liabilitiesIndices.get(i).size()];
			for (int j = 0; j < liabilitiesIndices.get(i).size(); j++) {
				this.liabilitiesIndices[i][j] = liabilitiesIndices.get(i).get(j);
			}
		}
		this.operatingCashFlow = new float[operatingCashFlow.size()];
		for (int i = 0; i < operatingCashFlow.size(); i++) {
			this.operatingCashFlow[i] = operatingCashFlow.get(i);
		}
		this.liquidAssets = new float[liquidAssets.size()];
		for (int i = 0; i < liquidAssets.size(); i++) {
			this.liquidAssets[i] = liquidAssets.get(i);
		}
	}

	public void clear() {
		/*
		 * for (int i = 0; i < this.liabilitiesAmounts.size(); i++) {
		 * this.liabilitiesAmounts.get(i).clear(); this.liabilitiesAmounts.set(i, null);
		 * } this.liabilitiesAmounts.clear(); this.liabilitiesAmounts = null; for (int i
		 * = 0; i < this.liabilitiesIndices.size(); i++) {
		 * this.liabilitiesIndices.get(i).clear(); this.liabilitiesIndices.set(i, null);
		 * } this.liabilitiesIndices.clear(); this.liabilitiesIndices = null;
		 * this.operatingCashFlow.clear(); this.operatingCashFlow = null;
		 */
	}

	/**
	 * @return the liabilitiesAmounts
	 */
	public List<List<Float>> getLiabilitiesAmounts() {
		// using JDK Collections
		// return liabilitiesAmounts;

		// using Trove
		/*
		 * List<List<Float>> boxedList = new
		 * ArrayList<List<Float>>(this.liabilitiesAmounts.size()); for (int i = 0; i <
		 * this.liabilitiesAmounts.size(); i++) { float[] primitiveArray =
		 * this.liabilitiesAmounts.get(i).toArray(); List<Float> boxed = new
		 * ArrayList<Float>(primitiveArray.length); for (Float node : primitiveArray) {
		 * boxed.add(node); } boxedList.add(boxed); } return boxedList;
		 */

		// using primitives
		List<List<Float>> boxedList = new ArrayList<List<Float>>(this.liabilitiesAmounts.length);
		for (int i = 0; i < this.liabilitiesAmounts.length; i++) {
			List<Float> boxed = new ArrayList<Float>(this.liabilitiesAmounts[i].length);
			for (int j = 0; j < this.liabilitiesAmounts[i].length; j++) {
				boxed.add(this.liabilitiesAmounts[i][j]);
			}
			boxedList.add(boxed);
		}
		return boxedList;
	}

	/**
	 * @param liabilitiesAmounts the liabilitiesAmounts to set
	 */
	public void setLiabilitiesAmounts(List<List<Float>> liabilitiesAmounts) {
		// using JDK Collections
		// this.liabilitiesAmounts = liabilitiesAmounts;

		// using Trove
		/*
		 * this.liabilitiesAmounts = new
		 * ArrayList<TFloatArrayList>(liabilitiesAmounts.size()); for (int i = 0; i <
		 * liabilitiesAmounts.size(); i++) { float[] liabAmtArray = new
		 * float[liabilitiesAmounts.get(i).size()]; int j = 0; for (Float node :
		 * liabilitiesAmounts.get(i)) { liabAmtArray[j++] = (node != null ? node : 0f);
		 * } this.liabilitiesAmounts.add(TFloatArrayList.wrap(liabAmtArray)); }
		 */

		// using primitives
		this.liabilitiesAmounts = new float[liabilitiesAmounts.size()][];
		for (int i = 0; i < liabilitiesAmounts.size(); i++) {
			this.liabilitiesAmounts[i] = new float[liabilitiesAmounts.get(i).size()];
			for (int j = 0; j < liabilitiesAmounts.get(i).size(); j++) {
				this.liabilitiesAmounts[i][j] = liabilitiesAmounts.get(i).get(j);
			}
		}
	}

	/**
	 * @return the liabilitiesIndices
	 */
	public List<List<Integer>> getLiabilitiesIndices() {
		// using JDK Collections
		// return liabilitiesIndices;

		// using Trove
		/*
		 * List<List<Integer>> boxedList = new
		 * ArrayList<List<Integer>>(liabilitiesIndices.size()); for (int i = 0; i <
		 * liabilitiesIndices.size(); i++) { int[] primitiveArray =
		 * this.liabilitiesIndices.get(i).toArray(); List<Integer> boxed = new
		 * ArrayList<Integer>(primitiveArray.length); for (Integer node :
		 * primitiveArray) { boxed.add(node); } boxedList.add(boxed); } return
		 * boxedList;
		 */

		// using primitives
		List<List<Integer>> boxedList = new ArrayList<List<Integer>>(this.liabilitiesIndices.length);
		for (int i = 0; i < this.liabilitiesIndices.length; i++) {
			List<Integer> boxed = new ArrayList<Integer>(this.liabilitiesIndices[i].length);
			for (int j = 0; j < this.liabilitiesIndices[i].length; j++) {
				boxed.add(this.liabilitiesIndices[i][j]);
			}
			boxedList.add(boxed);
		}
		return boxedList;
	}

	/**
	 * @param liabilitiesIndices the liabilitiesIndices to set
	 */
	public void setLiabilitiesIndices(List<List<Integer>> liabilitiesIndices) {
		// using JDK Collections
		// this.liabilitiesIndices = liabilitiesIndices;

		// using Trove
		/*
		 * this.liabilitiesIndices = new
		 * ArrayList<TIntArrayList>(liabilitiesIndices.size()); for (int i = 0; i <
		 * liabilitiesIndices.size(); i++) { int[] liabIdxArray = new
		 * int[liabilitiesIndices.get(i).size()]; int j = 0; for (Integer node :
		 * liabilitiesIndices.get(i)) { liabIdxArray[j++] = (node != null ? node : 0); }
		 * this.liabilitiesIndices.add(TIntArrayList.wrap(liabIdxArray)); }
		 */

		// using primitives
		this.liabilitiesIndices = new int[liabilitiesIndices.size()][];
		for (int i = 0; i < liabilitiesIndices.size(); i++) {
			this.liabilitiesIndices[i] = new int[liabilitiesIndices.get(i).size()];
			for (int j = 0; j < liabilitiesIndices.get(i).size(); j++) {
				this.liabilitiesIndices[i][j] = liabilitiesIndices.get(i).get(j);
			}
		}
	}

	/**
	 * @return the operatingCashFlow
	 */
	public List<Float> getOperatingCashFlow() {
		// using JDK Collections
		// return operatingCashFlow;

		// using Trove
		/*
		 * float[] primitiveArray = this.operatingCashFlow.toArray(); List<Float>
		 * boxedList = new ArrayList<Float>(primitiveArray.length); for (Float node :
		 * primitiveArray) { boxedList.add(node); } return boxedList;
		 */

		// using primitives
		List<Float> boxedList = new ArrayList<Float>(this.operatingCashFlow.length);
		for (int i = 0; i < this.operatingCashFlow.length; i++) {
			boxedList.add(this.operatingCashFlow[i]);
		}
		return boxedList;
	}

	/**
	 * @param operatingCashFlow the operatingCashFlow to set
	 */
	public void setOperatingCashFlow(List<Float> operatingCashFlow) {
		// using JDK Collections
		// this.operatingCashFlow = operatingCashFlow;

		// using Trove
		/*
		 * float[] cashflowArray = new float[operatingCashFlow.size()]; int i = 0; for
		 * (Float node : operatingCashFlow) { cashflowArray[i++] = (node != null ? node
		 * : 0f); } this.operatingCashFlow = TFloatArrayList.wrap(cashflowArray);
		 */

		// using primitives
		this.operatingCashFlow = new float[operatingCashFlow.size()];
		for (int i = 0; i < operatingCashFlow.size(); i++) {
			this.operatingCashFlow[i] = operatingCashFlow.get(i);
		}
	}

	/**
	 * @return the liquidAssets
	 */
	public List<Float> getLiquidAssets() {
		// using JDK Collections
		// return liquidAssets;

		// using Trove
		/*
		 * float[] primitiveArray = this.liquidAssets.toArray(); List<Float> boxedList =
		 * new ArrayList<Float>(primitiveArray.length); for (Float node :
		 * primitiveArray) { boxedList.add(node); } return boxedList;
		 */

		// using primitives
		List<Float> boxedList = new ArrayList<Float>(this.liquidAssets.length);
		for (int i = 0; i < this.liquidAssets.length; i++) {
			boxedList.add(this.liquidAssets[i]);
		}
		return boxedList;
	}

	/**
	 * @param liquidAssets the liquidAssets to set
	 */
	public void setLiquidAssets(List<Float> liquidAssets) {
		// using JDK Collections
		// this.liquidAssets = liquidAssets;

		// using Trove
		/*
		 * float[] assetsArray = new float[liquidAssets.size()]; int i = 0; for (Float
		 * node : liquidAssets) { assetsArray[i++] = (node != null ? node : 0f); }
		 * this.liquidAssets = TFloatArrayList.wrap(assetsArray);
		 */

		// using primitives
		this.liquidAssets = new float[liquidAssets.size()];
		for (int i = 0; i < liquidAssets.size(); i++) {
			this.liquidAssets[i] = liquidAssets.get(i);
		}
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
