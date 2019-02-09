/**
 * 
 */
package xyz.struthers.rhul.ham.agent;

import java.util.HashMap;
import java.util.Map;

/**
 * Each instance of this class stores 21 doubles, so will consume approximately
 * 168 bytes of RAM. There is only one instance of this class in the model.
 * 
 * @author Adam Struthers
 * @since 25-Jan-2019
 */
public final class ReserveBankOfAustralia extends Agent {

	private static final long serialVersionUID = 1L;

	// P&L
	private double pnlInterestIncome;
	private double pnlInterestExpense;
	private double pnlCommittedLiquidityFacilityFees;
	private double pnlForeignExchangeGainsLosses;
	private double pnlAudSecurities;
	private double pnlOtherIncome;

	private double pnlPersonnelExpenses;
	private double pnlDepreciationAmortisation;
	private double pnlOtherExpenses;

	private double pnlDistributionPayableToCommonwealth;

	// Bal Sht
	private double bsCash;
	private double bsAudInvestments;
	private double bsForeignInvestments;
	private double bsGold;
	private double bsOtherAssets;

	private double bsDeposits;
	private double bsDistributionPayableToCommonwealth;
	private double bsBanknotesOnIssue;
	private double bsOtherLiabilities;

	private double bsCapital;
	private double bsReserves;

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
	public ReserveBankOfAustralia(Map<String, Double> balSht, Map<String, Double> profitLoss) {
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
	public Map<Agent, Double> getAmountsReceivable(int iteration) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Agent, Double> getAmountsPayable(int iteration) {
		// TODO Auto-generated method stub
		return null;
	}

	public Map<String, Double> getFinancialStatements() {
		// initialised to the number of fields in this class
		Map<String, Double> result = new HashMap<String, Double>(21);

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
		this.pnlInterestIncome = 0d;
		this.pnlInterestExpense = 0d;
		this.pnlCommittedLiquidityFacilityFees = 0d;
		this.pnlForeignExchangeGainsLosses = 0d;
		this.pnlAudSecurities = 0d;
		this.pnlOtherIncome = 0d;

		this.pnlPersonnelExpenses = 0d;
		this.pnlDepreciationAmortisation = 0d;
		this.pnlOtherExpenses = 0d;

		this.pnlDistributionPayableToCommonwealth = 0d;

		// Bal Sht
		this.bsCash = 0d;
		this.bsAudInvestments = 0d;
		this.bsForeignInvestments = 0d;
		this.bsGold = 0d;
		this.bsOtherAssets = 0d;

		this.bsDeposits = 0d;
		this.bsDistributionPayableToCommonwealth = 0d;
		this.bsBanknotesOnIssue = 0d;
		this.bsOtherLiabilities = 0d;

		this.bsCapital = 0d;
		this.bsReserves = 0d;
	}

}
