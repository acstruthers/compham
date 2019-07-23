/**
 * 
 */
package xyz.struthers.rhul.ham.agent;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.map.hash.TObjectFloatHashMap;
import xyz.struthers.rhul.ham.config.PropertiesXml;
import xyz.struthers.rhul.ham.config.PropertiesXmlFactory;
import xyz.struthers.rhul.ham.process.Clearable;
import xyz.struthers.rhul.ham.process.Employer;
import xyz.struthers.rhul.ham.process.NodePayment;

/**
 * Each instance of this class stores 19 floats, so will consume approximately
 * 152 bytes of RAM. There is only one instance of this class in the model.
 * 
 * @author Adam Struthers
 * @since 25-Jan-2019
 */
public final class AustralianGovernment extends Agent implements Employer {

	private static final long serialVersionUID = 1L;

	public static final float NUMBER_MONTHS = 12f; // for interest calcs

	// Government details
	protected String name;
	protected char industryDivisionCode;

	// agent relationships
	protected int paymentClearingIndex;
	protected ArrayList<Individual> employees; // calculate wages & super
	private ArrayList<Household> welfareRecipients;
	private ArrayList<AuthorisedDepositTakingInstitution> bondInvestors;
	private TFloatArrayList bondInvestorAmounts;
	private ArrayList<Business> governmentSuppliers;
	private int defaultIteration;
	private int defaultOrder;

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
	protected TFloatArrayList interestRateStudentLoans; // in Australia this is always CPI (by law)

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
	public AustralianGovernment(TObjectFloatHashMap<String> balSht, TObjectFloatHashMap<String> profitLoss) {
		super();
		this.init();

		// P&L
		this.pnlTaxIncome = profitLoss.get("Taxation revenue");
		this.pnlSaleOfGoodsAndServices = profitLoss.get("Sales of goods and services");
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
		this.bsOtherFinancialAssets = balSht.get("Total financial assets") - this.bsCash
				- this.bsInvestmentsLoansPlacements - this.bsEquityAssets;
		this.bsLandAndFixedAssets = balSht.get("Land and fixed assets");
		this.bsOtherNonFinancialAssets = balSht.get("Other non-financial assets");

		this.bsCurrencyOnIssue = balSht.get("Currency on issue");
		this.bsDepositsHeld = balSht.get("Deposits held");
		this.bsBorrowings = balSht.get("Borrowing");
		this.bsOtherLiabilities = balSht.get("Total liabilities") - this.bsCurrencyOnIssue - this.bsDepositsHeld
				- this.bsBorrowings;
	}

	/**
	 * Gets the column headings, to write to CSV file.
	 * 
	 * @param separator
	 * @return a CSV list of the column headings
	 */
	public String toCsvStringHeaders(String separator) {
		StringBuilder sb = new StringBuilder();

		sb.append("Name" + separator);
		sb.append("PaymentClearingIndex" + separator);
		sb.append("Division" + separator);
		sb.append("EmployeeCount" + separator);
		sb.append("WelfareRecipientCount" + separator);
		sb.append("BondInvestorCount" + separator);
		sb.append("TaxIncome" + separator);
		sb.append("SaleOfGoodsAndServices" + separator);
		sb.append("InterestIncome" + separator);
		sb.append("OtherIncome" + separator);
		sb.append("PersonnelExpenses" + separator);
		sb.append("InterestExpense" + separator);
		sb.append("Depreciation" + separator);
		sb.append("OtherExpenses" + separator);
		sb.append("NetAcquisitionOfNonFinancialAssets" + separator);
		sb.append("Cash" + separator);
		sb.append("InvestmentLoansPlacements" + separator);
		sb.append("EquityAssets" + separator);
		sb.append("OtherFinancialAssets" + separator);
		sb.append("LandAndFixedAssets" + separator);
		sb.append("OtherNonFinancialAssets" + separator);
		sb.append("CurrencyOnIssue" + separator);
		sb.append("DepositsHeld" + separator);
		sb.append("Borrowings" + separator);
		sb.append("OtherLiabilities" + separator);
		sb.append("CurrentInterestRateStudentLoans");

		return sb.toString();
	}

	/**
	 * Gets the data, to write to CSV file.
	 * 
	 * @param separator
	 * @return a CSV list of the data
	 */
	public String toCsvString(String separator, int iteration) {
		StringBuilder sb = new StringBuilder();

		DecimalFormat decimal = new DecimalFormat("###0.00");
		DecimalFormat wholeNumber = new DecimalFormat("###0");
		DecimalFormat percent = new DecimalFormat("###0.0000");

		sb.append(this.name + separator);
		sb.append(wholeNumber.format(this.paymentClearingIndex) + separator);
		sb.append(this.industryDivisionCode + separator);
		sb.append(wholeNumber.format(this.employees != null ? this.employees.size() : 0) + separator);
		sb.append(wholeNumber.format(this.welfareRecipients != null ? this.welfareRecipients.size() : 0) + separator);
		sb.append(wholeNumber.format(this.bondInvestors != null ? this.bondInvestors.size() : 0) + separator);
		sb.append(decimal.format(this.pnlTaxIncome) + separator);
		sb.append(decimal.format(this.pnlSaleOfGoodsAndServices) + separator);
		sb.append(decimal.format(this.pnlInterestIncome) + separator);
		sb.append(decimal.format(this.pnlOtherIncome) + separator);
		sb.append(decimal.format(this.pnlPersonnelExpenses) + separator);
		sb.append(decimal.format(this.pnlInterestExpense) + separator);
		sb.append(decimal.format(this.pnlDepreciationAmortisation) + separator);
		sb.append(decimal.format(this.pnlOtherExpenses) + separator);
		sb.append(decimal.format(this.pnlNetAcquisitionOfNonFinancialAssets) + separator);
		sb.append(decimal.format(this.bsCash) + separator);
		sb.append(decimal.format(this.bsInvestmentsLoansPlacements) + separator);
		sb.append(decimal.format(this.bsEquityAssets) + separator);
		sb.append(decimal.format(this.bsOtherFinancialAssets) + separator);
		sb.append(decimal.format(this.bsLandAndFixedAssets) + separator);
		sb.append(decimal.format(this.bsOtherNonFinancialAssets) + separator);
		sb.append(decimal.format(this.bsCurrencyOnIssue) + separator);
		sb.append(decimal.format(this.bsDepositsHeld) + separator);
		sb.append(decimal.format(this.bsBorrowings) + separator);
		sb.append(decimal.format(this.bsOtherLiabilities) + separator);
		sb.append(percent
				.format(this.interestRateStudentLoans != null ? this.interestRateStudentLoans.get(iteration) : 0));

		return sb.toString();
	}

	/**
	 * Gets the summary column headings, to write to CSV file.
	 * <p>
	 * The intention is for this data to form the basis of a statistical analysis
	 * (for example, using R).
	 * 
	 * @param separator
	 * @return a CSV list of the column headings
	 */
	public String toCsvSummaryStringHeaders(String separator) {
		// AU Government is small enough I'll just save all the details
		return this.toCsvStringHeaders(separator);
	}

	/**
	 * Gets the summary data, to write to CSV file.
	 * <p>
	 * The intention is for this data to form the basis of a statistical analysis
	 * (for example, using R).
	 * 
	 * @param separator
	 * @return a CSV list of the data
	 */
	public String toCsvSummaryString(String separator, int iteration) {
		// AU Government is small enough I'll just save all the details
		return this.toCsvString(separator, iteration);
	}

	@Override
	public ArrayList<Individual> getEmployees() {
		return this.employees;
	}

	@Override
	public void addEmployee(Individual employee) {
		if (this.employees == null) {
			this.employees = new ArrayList<Individual>(1);
		}
		this.employees.add(employee);
		this.employees.trimToSize();
	}

	@Override
	public float getInitialWagesExpense() {
		PropertiesXml properties = PropertiesXmlFactory.getProperties();
		return this.pnlPersonnelExpenses / properties.getSuperannuationGuaranteeRate();
	}

	@Override
	public float getActualWagesExpense() {
		return (float) this.employees.stream().mapToDouble(o -> o.getPnlWagesSalaries()).sum();
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

		// calculate government sales due to Businesses
		for (Business govtSupplier : this.governmentSuppliers) {
			int index = govtSupplier.getPaymentClearingIndex();
			float monthlySpend = govtSupplier.getSalesGovernment();
			liabilities.add(new NodePayment(index, monthlySpend));
		}

		liabilities.trimToSize();
		return liabilities;
	}

	@Override
	public void setDefaultedIteration(int iteration, int order) {
		this.defaultIteration = iteration;
		this.defaultOrder = order;
	}

	@Override
	public int getDefaultIteration() {
		return this.defaultIteration;
	}

	@Override
	public int getDefaultOrder() {
		return this.defaultOrder;
	}

	@Override
	public int processClearingPaymentVectorOutput(float nodeEquity, int iteration, int defaultOrder) {
		// update default details
		if (defaultOrder > 0) {
			// update default details unless it defaulted in a previous iteration
			if (this.defaultIteration == 0) {
				// hasn't defaulted in a previous iteration
				this.defaultIteration = iteration;
				this.defaultOrder = defaultOrder;
			}
		}

		// update financials
		this.bsCash += nodeEquity;

		return Clearable.OK; // assume government never defaults
	}

	/**
	 * @return the industryDivisionCode
	 */
	@Override
	public char getIndustryDivisionCode() {
		return industryDivisionCode;
	}

	/**
	 * @param industryDivisionCode the industryDivisionCode to set
	 */
	@Override
	public void setIndustryDivisionCode(char industryDivisionCode) {
		this.industryDivisionCode = industryDivisionCode;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
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
	 * @return the bondInvestorAmounts
	 */
	public TFloatArrayList getBondInvestorAmounts() {
		return bondInvestorAmounts;
	}

	/**
	 * @param bondInvestorAmounts the bondInvestorAmounts to set
	 */
	public void setBondInvestorAmounts(TFloatArrayList bondInvestorAmounts) {
		this.bondInvestorAmounts = bondInvestorAmounts;
	}

	/**
	 * @return the governmentSuppliers
	 */
	public ArrayList<Business> getGovernmentSuppliers() {
		return governmentSuppliers;
	}

	/**
	 * @param governmentSuppliers the governmentSuppliers to set
	 */
	public void setGovernmentSuppliers(ArrayList<Business> governmentSuppliers) {
		this.governmentSuppliers = governmentSuppliers;
	}

	/**
	 * @return the interestRateStudentLoans
	 */
	public TFloatArrayList getInterestRateStudentLoans() {
		return interestRateStudentLoans;
	}

	/**
	 * @param iteration
	 * @return the interest rate for that iteration
	 */
	public float getInterestRateStudentLoans(int iteration) {
		return interestRateStudentLoans.get(iteration);
	}

	/**
	 * @param interestRateStudentLoans the interestRateStudentLoans to set
	 */
	public void addInterestRateStudentLoans(float interestRateStudentLoans) {
		if (this.interestRateStudentLoans == null) {
			this.interestRateStudentLoans = new TFloatArrayList(1);
		}
		this.interestRateStudentLoans.add(interestRateStudentLoans);
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

	public float getTotalIncome() {
		return this.pnlTaxIncome + this.pnlSaleOfGoodsAndServices + this.pnlInterestIncome + this.pnlOtherIncome;
	}

	protected void init() {

		// Agent details
		super.init();
		this.name = "Australian Government";
		this.defaultIteration = 0;
		this.defaultOrder = 0;

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
