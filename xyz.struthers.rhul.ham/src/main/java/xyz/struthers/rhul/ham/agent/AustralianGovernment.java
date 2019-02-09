/**
 * 
 */
package xyz.struthers.rhul.ham.agent;

import java.util.HashMap;
import java.util.Map;

/**
 * Each instance of this class stores 19 doubles, so will consume approximately
 * 152 bytes of RAM. There is only one instance of this class in the model.
 * 
 * @author Adam Struthers
 * @since 25-Jan-2019
 */
public final class AustralianGovernment extends Agent {

	private static final long serialVersionUID = 1L;

	// P&L
	private double pnlTaxIncome;
	private double pnlSaleOfGoodsAndServices;
	private double pnlInterestIncome;
	private double pnlOtherIncome;

	private double pnlPersonnelExpenses;
	private double pnlInterestExpense;
	private double pnlDepreciationAmortisation;
	private double pnlOtherExpenses;

	private double pnlNetAcquisitionOfNonFinancialAssets;

	// Bal Sht
	private double bsCash;
	private double bsInvestmentsLoansPlacements;
	private double bsEquityAssets;
	private double bsOtherFinancialAssets;
	private double bsLandAndFixedAssets;
	private double bsOtherNonFinancialAssets;

	private double bsCurrencyOnIssue;
	private double bsDepositsHeld;
	private double bsBorrowings;
	private double bsOtherLiabilities;

	/**
	 * Default constructor
	 */
	public AustralianGovernment() {
		super();
		this.init();
	}

	/**
	 * Initialisation constructor
	 * 
	 * @param balSht
	 * @param profitLoss
	 */
	public AustralianGovernment(Map<String, Double> balSht, Map<String, Double> profitLoss) {
		super();
		this.init();

		// P&L
		this.pnlTaxIncome = profitLoss.get("Taxation revenue");
		this.pnlSaleOfGoodsAndServices = profitLoss.get("Sale of goods and services");
		this.pnlInterestIncome = profitLoss.get("Interest income");
		this.pnlOtherIncome = profitLoss.get("Total GFS revenue") - this.pnlTaxIncome - this.pnlSaleOfGoodsAndServices
				- this.pnlInterestIncome;

		this.pnlPersonnelExpenses = profitLoss.get("Employee expenses");
		this.pnlInterestExpense = profitLoss.get("Nominal superannuation interest expenses")
				+ profitLoss.get("Other interest expenses");
		this.pnlDepreciationAmortisation = profitLoss.get("Depreciation");
		this.pnlOtherExpenses = profitLoss.get("Total GFS expenses") - this.pnlPersonnelExpenses
				- this.pnlInterestExpense - this.pnlDepreciationAmortisation;

		this.pnlNetAcquisitionOfNonFinancialAssets = profitLoss.get("Total net acquisition of non-financial assets");

		// Bal Sht
		this.bsCash = balSht.get("Cash and deposits");
		this.bsInvestmentsLoansPlacements = balSht.get("Investments, loans and placements");
		this.bsEquityAssets = balSht.get("Equity");
		this.bsOtherFinancialAssets = balSht.get("Total finanical assets") - this.bsCash
				- this.bsInvestmentsLoansPlacements - this.bsEquityAssets;
		this.bsLandAndFixedAssets = balSht.get("Land and fixed assets");
		this.bsOtherNonFinancialAssets = balSht.get("Other non-financial assets");

		this.bsCurrencyOnIssue = balSht.get("Currency on issue");
		this.bsDepositsHeld = balSht.get("Deposits held");
		this.bsBorrowings = balSht.get("Borrowing");
		this.bsOtherLiabilities = balSht.get("Total liabilities") - this.bsCurrencyOnIssue - this.bsDepositsHeld
				- this.bsBorrowings;
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
		Map<String, Double> result = new HashMap<String, Double>(19);

		// P&L
		result.put("Taxation revenue", this.pnlTaxIncome);
		result.put("Sale of goods and services", this.pnlSaleOfGoodsAndServices);
		result.put("Interest income", this.pnlInterestIncome );
		result.put("Other income", this.pnlOtherIncome);

		result.put("Employee expenses", this.pnlPersonnelExpenses );
		result.put("Interest expense",this.pnlInterestExpense);
		result.put("Depreciation",this.pnlDepreciationAmortisation );
		result.put("Other expenses", this.pnlOtherExpenses);

		result.put("Total net acquisition of non-financial assets", this.pnlNetAcquisitionOfNonFinancialAssets);

		// Bal Sht
		result.put("Cash and deposits",this.bsCash );
		result.put("Investments, loans and placements",this.bsInvestmentsLoansPlacements );
		result.put("Equity",this.bsEquityAssets );
		result.put("Other financial assets", this.bsOtherFinancialAssets);
		result.put("Land and fixed assets",this.bsLandAndFixedAssets );
		result.put("Other non-financial assets",this.bsOtherNonFinancialAssets );

		result.put("Currency on issue",this.bsCurrencyOnIssue );
		result.put("Deposits held",this.bsDepositsHeld );
		result.put("Borrowing",this.bsBorrowings );
		result.put("Other liabilities", this.bsOtherLiabilities);

		return result;
	}

	protected void init() {

		// Agent details
		super.name = "Australian Government";

		// P&L
		this.pnlTaxIncome = 0d;
		this.pnlSaleOfGoodsAndServices = 0d;
		this.pnlInterestIncome = 0d;
		this.pnlOtherIncome = 0d;

		this.pnlPersonnelExpenses = 0d;
		this.pnlInterestExpense = 0d;
		this.pnlDepreciationAmortisation = 0d;
		this.pnlOtherExpenses = 0d;

		this.pnlNetAcquisitionOfNonFinancialAssets = 0d;

		// Bal Sht
		this.bsCash = 0d;
		this.bsInvestmentsLoansPlacements = 0d;
		this.bsEquityAssets = 0d;
		this.bsOtherFinancialAssets = 0d;
		this.bsLandAndFixedAssets = 0d;
		this.bsOtherNonFinancialAssets = 0d;

		this.bsCurrencyOnIssue = 0d;
		this.bsDepositsHeld = 0d;
		this.bsBorrowings = 0d;
		this.bsOtherLiabilities = 0d;
	}

}
