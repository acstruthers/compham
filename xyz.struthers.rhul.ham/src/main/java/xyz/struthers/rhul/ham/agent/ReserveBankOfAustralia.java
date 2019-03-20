/**
 * 
 */
package xyz.struthers.rhul.ham.agent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import xyz.struthers.rhul.ham.process.NodePayment;

/**
 * Each instance of this class stores 21 floats, so will consume approximately
 * 168 bytes of RAM. There is only one instance of this class in the model.
 * 
 * @author Adam Struthers
 * @since 25-Jan-2019
 */
public final class ReserveBankOfAustralia extends Agent {

	private static final long serialVersionUID = 1L;

	// agent relationships
	protected int paymentClearingIndex;
	
	// P&L
	private float pnlInterestIncome;
	private float pnlInterestExpense;
	private float pnlCommittedLiquidityFacilityFees;
	private float pnlForeignExchangeGainsLosses;
	private float pnlAudSecurities;
	private float pnlOtherIncome;

	private float pnlPersonnelExpenses;
	private float pnlDepreciationAmortisation;
	private float pnlOtherExpenses;

	private float pnlDistributionPayableToCommonwealth;

	// Bal Sht
	private float bsCash;
	private float bsAudInvestments;
	private float bsForeignInvestments;
	private float bsGold;
	private float bsOtherAssets;

	private float bsDeposits;
	private float bsDistributionPayableToCommonwealth;
	private float bsBanknotesOnIssue;
	private float bsOtherLiabilities;

	private float bsCapital;
	private float bsReserves;

	/**
	 * 
	 */
	public ReserveBankOfAustralia() {
		super();
		this.init();
	}

	/**
	 * The keys in the data map are the field titles used in the CSV file that the
	 * data is imported from.
	 * 
	 * @param balSht
	 *            - a map of Balance Sheet fields and values
	 * @param profitLoss
	 *            - a map of Profit & Loss Statement fields and values
	 */
	public ReserveBankOfAustralia(Map<String, Float> balSht, Map<String, Float> profitLoss) {
		super();
		this.init();

		// P&L
		this.pnlInterestIncome = profitLoss.get("Interest income");
		this.pnlInterestExpense = profitLoss.get("Interest expense");
		this.pnlCommittedLiquidityFacilityFees = profitLoss.get("Committed Liquidity Facility Fees");
		this.pnlForeignExchangeGainsLosses = profitLoss.get("FX Gains/(Losses)");
		this.pnlAudSecurities = profitLoss.get("AUD securities");
		this.pnlOtherIncome = profitLoss.get("Other income");

		this.pnlPersonnelExpenses = profitLoss.get("Personnel expenses");
		this.pnlDepreciationAmortisation = profitLoss.get("Depreciation and amortisation");
		this.pnlOtherExpenses = profitLoss.get("Other expenses");

		this.pnlDistributionPayableToCommonwealth = profitLoss.get("Distribution payable to the Commonwealth");

		// Bal Sht
		this.bsCash = balSht.get("Cash");
		this.bsAudInvestments = balSht.get("AUD investments");
		this.bsForeignInvestments = balSht.get("Foreign currency investments");
		this.bsGold = balSht.get("Gold");
		this.bsOtherAssets = balSht.get("Other assets");

		this.bsDeposits = balSht.get("Deposits");
		this.bsDistributionPayableToCommonwealth = balSht.get("Distribution payable to the Commonwealth");
		this.bsBanknotesOnIssue = balSht.get("Australian banknotes on issue");
		this.bsOtherLiabilities = balSht.get("Other liabilities");

		this.bsCapital = balSht.get("Capital");
		this.bsReserves = balSht.get("Reserves");
	}

	@Override
	public int getPaymentClearingIndex() {
		return this.paymentClearingIndex;
	}

	@Override
	public void setPaymentClearingIndex(int index) {
		this.paymentClearingIndex = index;
	}

	@Override
	public List<NodePayment> getAmountsPayable(int iteration) {
		// TODO Auto-generated method stub
		return null;
	}

	public Map<String, Float> getFinancialStatements() {
		// initialised to the number of fields in this class
		Map<String, Float> result = new HashMap<String, Float>(21);

		// P&L
		result.put("Interest income", this.pnlInterestIncome);
		result.put("Interest expense", this.pnlInterestExpense);
		result.put("Committed Liquidity Facility Fees", this.pnlCommittedLiquidityFacilityFees);
		result.put("FX Gains/(Losses)", this.pnlForeignExchangeGainsLosses);
		result.put("AUD securities", this.pnlAudSecurities);
		result.put("Other income", this.pnlOtherIncome);

		result.put("Personnel expenses", this.pnlPersonnelExpenses);
		result.put("Depreciation and amortisation", this.pnlDepreciationAmortisation);
		result.put("Other expenses", this.pnlOtherExpenses);

		result.put("Distribution payable to the Commonwealth", this.pnlDistributionPayableToCommonwealth);

		// Bal Sht
		result.put("Cash", this.bsCash);
		result.put("AUD investments", this.bsAudInvestments);
		result.put("Foreign currency investments", this.bsForeignInvestments);
		result.put("Gold", this.bsGold);
		result.put("Other assets", this.bsOtherAssets);

		result.put("Deposits", this.bsDeposits);
		result.put("Distribution payable to the Commonwealth", this.bsDistributionPayableToCommonwealth);
		result.put("Australian banknotes on issue", this.bsBanknotesOnIssue);
		result.put("Other liabilities", this.bsOtherLiabilities);

		result.put("Capital", this.bsCapital);
		result.put("Reserves", this.bsReserves);

		return result;
	}

	protected void init() {

		// Agent details
		super.name = "RBA";

		// P&L
		this.pnlInterestIncome = 0f;
		this.pnlInterestExpense = 0f;
		this.pnlCommittedLiquidityFacilityFees = 0f;
		this.pnlForeignExchangeGainsLosses = 0f;
		this.pnlAudSecurities = 0f;
		this.pnlOtherIncome = 0f;

		this.pnlPersonnelExpenses = 0f;
		this.pnlDepreciationAmortisation = 0f;
		this.pnlOtherExpenses = 0f;

		this.pnlDistributionPayableToCommonwealth = 0f;

		// Bal Sht
		this.bsCash = 0f;
		this.bsAudInvestments = 0f;
		this.bsForeignInvestments = 0f;
		this.bsGold = 0f;
		this.bsOtherAssets = 0f;

		this.bsDeposits = 0f;
		this.bsDistributionPayableToCommonwealth = 0f;
		this.bsBanknotesOnIssue = 0f;
		this.bsOtherLiabilities = 0f;

		this.bsCapital = 0f;
		this.bsReserves = 0f;
	}

}
