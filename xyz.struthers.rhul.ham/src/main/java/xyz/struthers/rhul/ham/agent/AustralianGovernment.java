/**
 * 
 */
package xyz.struthers.rhul.ham.agent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import xyz.struthers.rhul.ham.config.Properties;
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

	public static final float NUMBER_MONTHS = 12f; // for interest calcs

	// agent relationships
	protected int paymentClearingIndex;
	private ArrayList<Household> welfareRecipients;
	private ArrayList<AuthorisedDepositTakingInstitution> bondInvestors;

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

	// Interest rates
	protected float interestRateStudentLoans; // in Australia this is always CPI (by law)

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
		int numberOfCreditors = 0;
		if (this.welfareRecipients != null) {
			numberOfCreditors += this.welfareRecipients.size();
		}
		if (this.bondInvestors != null) {
			numberOfCreditors += this.bondInvestors.size();
		}
		ArrayList<NodePayment> liabilities = new ArrayList<NodePayment>(numberOfCreditors);

		// calculate welfare payments due to recipients
		if (this.welfareRecipients != null) {
			for (Household recipient : this.welfareRecipients) {
				int index = recipient.getPaymentClearingIndex();
				float welfarePayment = recipient.getPnlUnemploymentBenefits()
						+ recipient.getPnlOtherSocialSecurityIncome();
				liabilities.add(new NodePayment(index, welfarePayment));
			}
		}

		// calculate bond interest due to ADIs
		for (AuthorisedDepositTakingInstitution adi : this.bondInvestors) {
			int index = adi.getPaymentClearingIndex();
			float monthlyInterest = adi.getBsLoansGovernment() * adi.getGovtBondRate(iteration) / NUMBER_MONTHS;
			liabilities.add(new NodePayment(index, monthlyInterest));
		}

		liabilities.trimToSize();
		return liabilities;
	}

	/**
	 * @return the welfareRecipients
	 */
	public ArrayList<Household> getWelfareRecipients() {
		return welfareRecipients;
	}

	/**
	 * @param welfareRecipients the welfareRecipients to set
	 */
	public void setWelfareRecipients(ArrayList<Household> welfareRecipients) {
		this.welfareRecipients = welfareRecipients;
	}

	/**
	 * @return the bondInvestors
	 */
	public ArrayList<AuthorisedDepositTakingInstitution> getBondInvestors() {
		return bondInvestors;
	}

	/**
	 * @param bondInvestors the bondInvestors to set
	 */
	public void setBondInvestors(ArrayList<AuthorisedDepositTakingInstitution> bondInvestors) {
		this.bondInvestors = bondInvestors;
	}

	/**
	 * @return the interestRateStudentLoans
	 */
	public float getInterestRateStudentLoans() {
		return interestRateStudentLoans;
	}

	/**
	 * @param interestRateStudentLoans the interestRateStudentLoans to set
	 */
	public void setInterestRateStudentLoans(float interestRateStudentLoans) {
		this.interestRateStudentLoans = interestRateStudentLoans;
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

	/**
	 * @return the pnlTaxIncome
	 */
	public float getPnlTaxIncome() {
		return pnlTaxIncome;
	}

	/**
	 * @param pnlTaxIncome the pnlTaxIncome to set
	 */
	public void setPnlTaxIncome(float pnlTaxIncome) {
		this.pnlTaxIncome = pnlTaxIncome;
	}

	/**
	 * @return the pnlSaleOfGoodsAndServices
	 */
	public float getPnlSaleOfGoodsAndServices() {
		return pnlSaleOfGoodsAndServices;
	}

	/**
	 * @param pnlSaleOfGoodsAndServices the pnlSaleOfGoodsAndServices to set
	 */
	public void setPnlSaleOfGoodsAndServices(float pnlSaleOfGoodsAndServices) {
		this.pnlSaleOfGoodsAndServices = pnlSaleOfGoodsAndServices;
	}

	/**
	 * @return the pnlInterestIncome
	 */
	public float getPnlInterestIncome() {
		return pnlInterestIncome;
	}

	/**
	 * @param pnlInterestIncome the pnlInterestIncome to set
	 */
	public void setPnlInterestIncome(float pnlInterestIncome) {
		this.pnlInterestIncome = pnlInterestIncome;
	}

	/**
	 * @return the pnlOtherIncome
	 */
	public float getPnlOtherIncome() {
		return pnlOtherIncome;
	}

	/**
	 * @param pnlOtherIncome the pnlOtherIncome to set
	 */
	public void setPnlOtherIncome(float pnlOtherIncome) {
		this.pnlOtherIncome = pnlOtherIncome;
	}

	/**
	 * @return the pnlPersonnelExpenses
	 */
	public float getPnlPersonnelExpenses() {
		return pnlPersonnelExpenses;
	}

	/**
	 * @param pnlPersonnelExpenses the pnlPersonnelExpenses to set
	 */
	public void setPnlPersonnelExpenses(float pnlPersonnelExpenses) {
		this.pnlPersonnelExpenses = pnlPersonnelExpenses;
	}

	/**
	 * @return the pnlInterestExpense
	 */
	public float getPnlInterestExpense() {
		return pnlInterestExpense;
	}

	/**
	 * @param pnlInterestExpense the pnlInterestExpense to set
	 */
	public void setPnlInterestExpense(float pnlInterestExpense) {
		this.pnlInterestExpense = pnlInterestExpense;
	}

	/**
	 * @return the pnlDepreciationAmortisation
	 */
	public float getPnlDepreciationAmortisation() {
		return pnlDepreciationAmortisation;
	}

	/**
	 * @param pnlDepreciationAmortisation the pnlDepreciationAmortisation to set
	 */
	public void setPnlDepreciationAmortisation(float pnlDepreciationAmortisation) {
		this.pnlDepreciationAmortisation = pnlDepreciationAmortisation;
	}

	/**
	 * @return the pnlOtherExpenses
	 */
	public float getPnlOtherExpenses() {
		return pnlOtherExpenses;
	}

	/**
	 * @param pnlOtherExpenses the pnlOtherExpenses to set
	 */
	public void setPnlOtherExpenses(float pnlOtherExpenses) {
		this.pnlOtherExpenses = pnlOtherExpenses;
	}

	/**
	 * @return the pnlNetAcquisitionOfNonFinancialAssets
	 */
	public float getPnlNetAcquisitionOfNonFinancialAssets() {
		return pnlNetAcquisitionOfNonFinancialAssets;
	}

	/**
	 * @param pnlNetAcquisitionOfNonFinancialAssets the
	 *                                              pnlNetAcquisitionOfNonFinancialAssets
	 *                                              to set
	 */
	public void setPnlNetAcquisitionOfNonFinancialAssets(float pnlNetAcquisitionOfNonFinancialAssets) {
		this.pnlNetAcquisitionOfNonFinancialAssets = pnlNetAcquisitionOfNonFinancialAssets;
	}

	/**
	 * @return the bsCash
	 */
	public float getBsCash() {
		return bsCash;
	}

	/**
	 * @param bsCash the bsCash to set
	 */
	public void setBsCash(float bsCash) {
		this.bsCash = bsCash;
	}

	/**
	 * @return the bsInvestmentsLoansPlacements
	 */
	public float getBsInvestmentsLoansPlacements() {
		return bsInvestmentsLoansPlacements;
	}

	/**
	 * @param bsInvestmentsLoansPlacements the bsInvestmentsLoansPlacements to set
	 */
	public void setBsInvestmentsLoansPlacements(float bsInvestmentsLoansPlacements) {
		this.bsInvestmentsLoansPlacements = bsInvestmentsLoansPlacements;
	}

	/**
	 * @return the bsEquityAssets
	 */
	public float getBsEquityAssets() {
		return bsEquityAssets;
	}

	/**
	 * @param bsEquityAssets the bsEquityAssets to set
	 */
	public void setBsEquityAssets(float bsEquityAssets) {
		this.bsEquityAssets = bsEquityAssets;
	}

	/**
	 * @return the bsOtherFinancialAssets
	 */
	public float getBsOtherFinancialAssets() {
		return bsOtherFinancialAssets;
	}

	/**
	 * @param bsOtherFinancialAssets the bsOtherFinancialAssets to set
	 */
	public void setBsOtherFinancialAssets(float bsOtherFinancialAssets) {
		this.bsOtherFinancialAssets = bsOtherFinancialAssets;
	}

	/**
	 * @return the bsLandAndFixedAssets
	 */
	public float getBsLandAndFixedAssets() {
		return bsLandAndFixedAssets;
	}

	/**
	 * @param bsLandAndFixedAssets the bsLandAndFixedAssets to set
	 */
	public void setBsLandAndFixedAssets(float bsLandAndFixedAssets) {
		this.bsLandAndFixedAssets = bsLandAndFixedAssets;
	}

	/**
	 * @return the bsOtherNonFinancialAssets
	 */
	public float getBsOtherNonFinancialAssets() {
		return bsOtherNonFinancialAssets;
	}

	/**
	 * @param bsOtherNonFinancialAssets the bsOtherNonFinancialAssets to set
	 */
	public void setBsOtherNonFinancialAssets(float bsOtherNonFinancialAssets) {
		this.bsOtherNonFinancialAssets = bsOtherNonFinancialAssets;
	}

	/**
	 * @return the bsCurrencyOnIssue
	 */
	public float getBsCurrencyOnIssue() {
		return bsCurrencyOnIssue;
	}

	/**
	 * @param bsCurrencyOnIssue the bsCurrencyOnIssue to set
	 */
	public void setBsCurrencyOnIssue(float bsCurrencyOnIssue) {
		this.bsCurrencyOnIssue = bsCurrencyOnIssue;
	}

	/**
	 * @return the bsDepositsHeld
	 */
	public float getBsDepositsHeld() {
		return bsDepositsHeld;
	}

	/**
	 * @param bsDepositsHeld the bsDepositsHeld to set
	 */
	public void setBsDepositsHeld(float bsDepositsHeld) {
		this.bsDepositsHeld = bsDepositsHeld;
	}

	/**
	 * @return the bsBorrowings
	 */
	public float getBsBorrowings() {
		return bsBorrowings;
	}

	/**
	 * @param bsBorrowings the bsBorrowings to set
	 */
	public void setBsBorrowings(float bsBorrowings) {
		this.bsBorrowings = bsBorrowings;
	}

	/**
	 * @return the bsOtherLiabilities
	 */
	public float getBsOtherLiabilities() {
		return bsOtherLiabilities;
	}

	/**
	 * @param bsOtherLiabilities the bsOtherLiabilities to set
	 */
	public void setBsOtherLiabilities(float bsOtherLiabilities) {
		this.bsOtherLiabilities = bsOtherLiabilities;
	}

}
