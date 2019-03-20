/**
 * 
 */
package xyz.struthers.rhul.ham.agent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import xyz.struthers.rhul.ham.process.NodePayment;

/**
 * Each instance of this class stores 19 floats, so will consume approximately
 * 152 bytes of RAM. There is only one instance of this class in the model.
 * 
 * @author Adam Struthers
 * @since 25-Jan-2019
 */
public final class AustralianGovernment extends Agent {

	private static final long serialVersionUID = 1L;

	// agernt relationships
	protected int paymentClearingIndex;

	// P&L
	private float pnlTaxIncome;
	private float pnlSaleOfGoodsAndServices;
	private float pnlInterestIncome;
	private float pnlOtherIncome;

	private float pnlPersonnelExpenses;
	private float pnlInterestExpense;
	private float pnlDepreciationAmortisation;
	private float pnlOtherExpenses;

	private float pnlNetAcquisitionOfNonFinancialAssets;

	// Bal Sht
	private float bsCash;
	private float bsInvestmentsLoansPlacements;
	private float bsEquityAssets;
	private float bsOtherFinancialAssets;
	private float bsLandAndFixedAssets;
	private float bsOtherNonFinancialAssets;

	private float bsCurrencyOnIssue;
	private float bsDepositsHeld;
	private float bsBorrowings;
	private float bsOtherLiabilities;

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
	public AustralianGovernment(Map<String, Float> balSht, Map<String, Float> profitLoss) {
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
		Map<String, Float> result = new HashMap<String, Float>(19);

		// P&L
		result.put("Taxation revenue", this.pnlTaxIncome);
		result.put("Sale of goods and services", this.pnlSaleOfGoodsAndServices);
		result.put("Interest income", this.pnlInterestIncome);
		result.put("Other income", this.pnlOtherIncome);

		result.put("Employee expenses", this.pnlPersonnelExpenses);
		result.put("Interest expense", this.pnlInterestExpense);
		result.put("Depreciation", this.pnlDepreciationAmortisation);
		result.put("Other expenses", this.pnlOtherExpenses);

		result.put("Total net acquisition of non-financial assets", this.pnlNetAcquisitionOfNonFinancialAssets);

		// Bal Sht
		result.put("Cash and deposits", this.bsCash);
		result.put("Investments, loans and placements", this.bsInvestmentsLoansPlacements);
		result.put("Equity", this.bsEquityAssets);
		result.put("Other financial assets", this.bsOtherFinancialAssets);
		result.put("Land and fixed assets", this.bsLandAndFixedAssets);
		result.put("Other non-financial assets", this.bsOtherNonFinancialAssets);

		result.put("Currency on issue", this.bsCurrencyOnIssue);
		result.put("Deposits held", this.bsDepositsHeld);
		result.put("Borrowing", this.bsBorrowings);
		result.put("Other liabilities", this.bsOtherLiabilities);

		return result;
	}

	protected void init() {

		// Agent details
		super.name = "Australian Government";

		// P&L
		this.pnlTaxIncome = 0f;
		this.pnlSaleOfGoodsAndServices = 0f;
		this.pnlInterestIncome = 0f;
		this.pnlOtherIncome = 0f;

		this.pnlPersonnelExpenses = 0f;
		this.pnlInterestExpense = 0f;
		this.pnlDepreciationAmortisation = 0f;
		this.pnlOtherExpenses = 0f;

		this.pnlNetAcquisitionOfNonFinancialAssets = 0f;

		// Bal Sht
		this.bsCash = 0f;
		this.bsInvestmentsLoansPlacements = 0f;
		this.bsEquityAssets = 0f;
		this.bsOtherFinancialAssets = 0f;
		this.bsLandAndFixedAssets = 0f;
		this.bsOtherNonFinancialAssets = 0f;

		this.bsCurrencyOnIssue = 0f;
		this.bsDepositsHeld = 0f;
		this.bsBorrowings = 0f;
		this.bsOtherLiabilities = 0f;
	}

}
