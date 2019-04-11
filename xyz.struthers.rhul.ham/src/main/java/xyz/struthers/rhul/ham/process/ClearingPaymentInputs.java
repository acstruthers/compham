/**
 * 
 */
package xyz.struthers.rhul.ham.process;

import java.util.List;

/**
 * Just a data structure so it's easy to return all three variables from a
 * single method call.
 * 
 * @author Adam Struthers
 * @since 10-Apr-2019
 */
public class ClearingPaymentInputs {

	private List<List<Float>> liabilitiesAmounts;
	private List<List<Integer>> liabilitiesIndices;
	private List<Float> operatingCashFlow;

	/**
	 * 
	 */
	public ClearingPaymentInputs(List<List<Float>> liabilitiesAmounts, List<List<Integer>> liabilitiesIndices,
			List<Float> operatingCashFlow) {
		this.liabilitiesAmounts = liabilitiesAmounts;
		this.liabilitiesIndices = liabilitiesIndices;
		this.operatingCashFlow = operatingCashFlow;
	}

	public void clear() {
		for (int i = 0; i < this.liabilitiesAmounts.size(); i++) {
			this.liabilitiesAmounts.clear();
			this.liabilitiesAmounts.set(i, null);
		}
		this.liabilitiesAmounts.clear();
		this.liabilitiesAmounts = null;
		for (int i = 0; i < this.liabilitiesIndices.size(); i++) {
			this.liabilitiesIndices.clear();
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
		return liabilitiesAmounts;
	}

	/**
	 * @param liabilitiesAmounts the liabilitiesAmounts to set
	 */
	public void setLiabilitiesAmounts(List<List<Float>> liabilitiesAmounts) {
		this.liabilitiesAmounts = liabilitiesAmounts;
	}

	/**
	 * @return the liabilitiesIndices
	 */
	public List<List<Integer>> getLiabilitiesIndices() {
		return liabilitiesIndices;
	}

	/**
	 * @param liabilitiesIndices the liabilitiesIndices to set
	 */
	public void setLiabilitiesIndices(List<List<Integer>> liabilitiesIndices) {
		this.liabilitiesIndices = liabilitiesIndices;
	}

	/**
	 * @return the operatingCashFlow
	 */
	public List<Float> getOperatingCashFlow() {
		return operatingCashFlow;
	}

	/**
	 * @param operatingCashFlow the operatingCashFlow to set
	 */
	public void setOperatingCashFlow(List<Float> operatingCashFlow) {
		this.operatingCashFlow = operatingCashFlow;
	}

}
