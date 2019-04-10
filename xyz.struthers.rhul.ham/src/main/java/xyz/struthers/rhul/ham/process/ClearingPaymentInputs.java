/**
 * 
 */
package xyz.struthers.rhul.ham.process;

import java.util.List;

/**
 * @author acstr
 *
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
