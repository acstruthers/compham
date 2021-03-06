/**
 * 
 */
package xyz.struthers.rhul.ham.agent;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import gnu.trove.list.array.TFloatArrayList;
import xyz.struthers.lang.CustomMath;
import xyz.struthers.rhul.ham.config.PropertiesXml;
import xyz.struthers.rhul.ham.config.PropertiesXmlFactory;
import xyz.struthers.rhul.ham.process.AustralianEconomy;
import xyz.struthers.rhul.ham.process.Clearable;
import xyz.struthers.rhul.ham.process.NodePayment;
import xyz.struthers.rhul.ham.process.Tax;

/**
 * Each instance of this class uses about 148 bytes of RAM. There are
 * approximately 10 million instances of this class in the model, so they will
 * consume approximately 1.4 GB of RAM.
 * 
 * N.B. Debits are positive unless stated otherwise.
 * 
 * @author Adam Struthers
 * @since 11-Dec-2018
 */
public class Household extends Agent {

	private static final long serialVersionUID = 1L;

	private static PropertiesXml properties = PropertiesXmlFactory.getProperties();

	// Agent relationships (approx. 28 bytes)
	protected int paymentClearingIndex;
	private Individual[] individuals; // get employers from individuals
	// private int numAdults; // calculate instead to reduce memory
	// private int numChildren; // calculate instead to reduce memory
	private String lgaCode;
	private String state;

	private AuthorisedDepositTakingInstitution loanAdi; // can be null if no loan
	private ArrayList<Business> suppliers; // household spending goes to these per ABS 6530.0
	// private ArrayList<Float> supplierRatios; // per ABS 6530.0
	private TFloatArrayList supplierRatios; // per ABS 6530.0
	private Household landlord;
	private AustralianGovernment govt;
	private int defaultIteration;
	private int defaultOrder;

	// P&L (72 bytes)
	private float pnlWagesSalaries;
	private float pnlUnemploymentBenefits;
	private float pnlOtherSocialSecurityIncome;
	private float pnlInvestmentIncome; // other income (including superannuation & dividends)
	private float pnlInterestIncome;
	private float pnlRentIncome; // income from investment properties
	private float pnlForeignIncome; // linked to country with relevant FX rate
	private float pnlOtherIncome;
	private float pnlIncomeTaxExpense;

	private float pnlLivingExpenses; // Henderson poverty line (excl. housing costs)
	// set at CalibrateHouseholds line 809

	private float pnlRentExpense;
	private float pnlMortgageRepayments;
	private float pnlWorkRelatedExpenses;
	private float pnlRentInterestExpense; // assume interest-only loan
	private float pnlDonations;
	private float pnlOtherDiscretionaryExpenses; // TODO: review this ... it might be unnecessary

	// Bal Sht (48 bytes)
	private float bsBankDeposits;
	private float bsSuperannuation;
	private float bsEquities;
	private float bsOtherFinancialAssets;

	private float bsResidentialLandAndDwellings;
	private float bsOtherNonFinancialAssets;

	// private float bsTotalAssets; // calculate instead to reduce memory

	private float bsLoans;
	private float bsStudentLoans; // HELP debt
	private float bsOtherLiabilities;
	// private float bsTotalLiabilities; // calculate instead to reduce memory

	// private float bsNetWorth; // calculate instead to reduce memory

	// Loan details
	private int loanCurrentMonth; // at calibration

	/**
	 * 
	 */
	public Household() {
		super();
		this.init();
	}

	/**
	 * Initialisation constructor
	 * 
	 * @param householdIndividuals
	 */
	public Household(Individual[] householdIndividuals) {
		super();
		this.init();
		this.individuals = householdIndividuals;
		this.initialiseFinancialsFromIndividuals();
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
		sb.append("AdultCount" + separator);
		sb.append("ChildCount" + separator);
		sb.append("LoanAdiID" + separator);
		sb.append("SupplierCount" + separator);
		sb.append("LandlordID" + separator);
		sb.append("GovtID" + separator);
		sb.append("WagesSalaries" + separator);
		sb.append("UnemploymentBenefits" + separator);
		sb.append("OtherSocialSecurityIncome" + separator);
		sb.append("InvestmentIncome" + separator);
		sb.append("InterestIncome" + separator);
		sb.append("RentIncome" + separator);
		sb.append("ForeignIncome" + separator);
		sb.append("OtherIncome" + separator);
		sb.append("IncomeTaxExpense" + separator);
		sb.append("LivingExpenses" + separator);
		sb.append("RentExpense" + separator);
		sb.append("MortgageRepayments" + separator);
		sb.append("WorkRelatedExpenses" + separator);
		sb.append("RentInterestExpense" + separator);
		sb.append("Donations" + separator);
		sb.append("OtherDiscretionaryExpenses" + separator);
		sb.append("BankDeposits" + separator);
		sb.append("SuperannuationBalance" + separator);
		sb.append("Equities" + separator);
		sb.append("OtherFinancialAssets" + separator);
		sb.append("ResidentialLandAndDwellings" + separator);
		sb.append("OtherNonFinancialAssets" + separator);
		sb.append("TotalAssets" + separator);
		sb.append("Loans" + separator);
		sb.append("StudentLoans" + separator);
		sb.append("OtherLiabilities" + separator);
		sb.append("TotalLiabilities" + separator);
		sb.append("NetWorth");

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
		// DecimalFormat percent = new DecimalFormat("###0.0000");

		sb.append(separator); // name
		sb.append(wholeNumber.format(this.paymentClearingIndex) + separator);
		sb.append(wholeNumber.format(this.getNumAdults()) + separator);
		sb.append(wholeNumber.format(this.getNumChildren()) + separator);
		sb.append(wholeNumber.format(this.loanAdi != null ? this.loanAdi.getPaymentClearingIndex() : 0) + separator);
		sb.append(wholeNumber.format(this.suppliers != null ? this.suppliers.size() : 0) + separator);
		sb.append(wholeNumber.format(this.landlord != null ? this.landlord.getPaymentClearingIndex() : 0) + separator);
		sb.append(wholeNumber.format(this.govt != null ? this.govt.getPaymentClearingIndex() : 0) + separator);
		sb.append(decimal.format(this.pnlWagesSalaries) + separator);
		sb.append(decimal.format(this.pnlUnemploymentBenefits) + separator);
		sb.append(decimal.format(this.pnlOtherSocialSecurityIncome) + separator);
		sb.append(decimal.format(this.pnlInvestmentIncome) + separator);
		sb.append(decimal.format(this.pnlInterestIncome) + separator);
		sb.append(decimal.format(this.pnlRentIncome) + separator);
		sb.append(decimal.format(this.pnlForeignIncome) + separator);
		sb.append(decimal.format(this.pnlOtherIncome) + separator);
		sb.append(decimal.format(this.pnlIncomeTaxExpense) + separator);
		sb.append(decimal.format(this.pnlLivingExpenses) + separator);
		sb.append(decimal.format(this.pnlRentExpense) + separator);
		sb.append(decimal.format(this.pnlMortgageRepayments) + separator);
		sb.append(decimal.format(this.pnlWorkRelatedExpenses) + separator);
		sb.append(decimal.format(this.pnlRentInterestExpense) + separator);
		sb.append(decimal.format(this.pnlDonations) + separator);
		sb.append(decimal.format(this.pnlOtherDiscretionaryExpenses) + separator);
		sb.append(decimal.format(this.bsBankDeposits) + separator);
		sb.append(decimal.format(this.bsSuperannuation) + separator);
		sb.append(decimal.format(this.bsEquities) + separator);
		sb.append(decimal.format(this.bsOtherFinancialAssets) + separator);
		sb.append(decimal.format(this.bsResidentialLandAndDwellings) + separator);
		sb.append(decimal.format(this.bsOtherNonFinancialAssets) + separator);
		sb.append(decimal.format(this.getBsTotalAssets()) + separator);
		sb.append(decimal.format(this.bsLoans) + separator);
		sb.append(decimal.format(this.bsStudentLoans) + separator);
		sb.append(decimal.format(this.bsOtherLiabilities) + separator);
		sb.append(decimal.format(this.getBsTotalLiabilities()) + separator);
		sb.append(decimal.format(this.getBsNetWorth()));

		return sb.toString();
	}

	/**
	 * Gets the column headings, to write to summary CSV file.
	 * 
	 * @param separator
	 * @return a CSV list of the column headings
	 */
	public String toCsvSummaryStringHeaders(String separator) {
		StringBuilder sb = new StringBuilder();

		sb.append("PaymentClearingIndex" + separator);
		sb.append("LgaCode" + separator);
		sb.append("ChildCount" + separator);
		sb.append("AdultCount" + separator);
		sb.append("AdultMeanAge" + separator);

		sb.append("GrossIncome" + separator);
		sb.append("ForeignIncome" + separator);
		sb.append("IncomeTaxExpense" + separator);

		sb.append("LivingExpenses" + separator);
		sb.append("RentExpense" + separator);
		sb.append("MortgageRepayments" + separator);
		sb.append("Donations" + separator);

		sb.append("TotalAssets" + separator);
		sb.append("Loans" + separator);
		sb.append("StudentLoans" + separator);
		sb.append("TotalLiabilities" + separator);

		sb.append("DefaultIteration" + separator);
		sb.append("DefaultOrder");

		return sb.toString();
	}

	/**
	 * Gets the data, to write to summary CSV file.
	 * 
	 * @param separator
	 * @return a CSV list of the data
	 */
	public String toCsvSummaryString(String separator, int iteration) {
		StringBuilder sb = new StringBuilder();

		DecimalFormat decimal = new DecimalFormat("###0.00");
		DecimalFormat wholeNumber = new DecimalFormat("###0");
		// DecimalFormat percent = new DecimalFormat("###0.0000");

		sb.append(wholeNumber.format(this.paymentClearingIndex) + separator);
		sb.append(this.lgaCode + separator);
		sb.append(wholeNumber.format(this.getNumChildren()) + separator);
		sb.append(wholeNumber.format(this.getNumAdults()) + separator);
		float adultMeanAge = 0f;
		int adultCount = 0;
		for (Individual person : this.individuals) {
			if (person.getAge() > 18) {
				adultMeanAge += person.getAge();
				adultCount++;
			}
		}
		adultMeanAge = adultMeanAge / (adultCount > 0 ? adultCount : 1);
		sb.append(decimal.format(adultMeanAge) + separator);

		// GrossIncome
		sb.append(decimal.format(this.pnlWagesSalaries + this.pnlUnemploymentBenefits
				+ this.pnlOtherSocialSecurityIncome + this.pnlInvestmentIncome + this.pnlInterestIncome
				+ this.pnlRentIncome + this.pnlForeignIncome + this.pnlOtherIncome) + separator);
		sb.append(decimal.format(this.pnlForeignIncome) + separator);
		sb.append(decimal.format(this.pnlIncomeTaxExpense) + separator);

		sb.append(decimal.format(this.pnlLivingExpenses) + separator);
		sb.append(decimal.format(this.pnlRentExpense) + separator);
		sb.append(decimal.format(this.pnlMortgageRepayments) + separator);
		sb.append(decimal.format(this.pnlDonations) + separator);

		sb.append(decimal.format(this.getBsTotalAssets()) + separator);
		sb.append(decimal.format(this.bsLoans) + separator);
		sb.append(decimal.format(this.bsStudentLoans) + separator);
		sb.append(decimal.format(this.getBsTotalLiabilities()) + separator);

		sb.append(this.defaultIteration + separator);
		sb.append(this.defaultOrder);

		return sb.toString();
	}

	/**
	 * Gets the column headings, to write to CSV file.
	 * 
	 * @param separator
	 * @return a CSV list of the column headings
	 */
	public String toCsvIncomeBySourceStringHeaders(String separator) {
		StringBuilder sb = new StringBuilder();

		sb.append("PaymentClearingIndex" + separator);
		sb.append("LgaCode" + separator);

		sb.append("Income" + separator);
		sb.append("Source" + separator);

		return sb.toString();
	}

	/**
	 * Gets the data, to write to summary CSV file.
	 * 
	 * @param separator
	 * @return a CSV list of the data
	 */
	public List<String> toCsvIncomeBySourceString(String separator, int iteration) {
		DecimalFormat decimal = new DecimalFormat("###0.00");
		DecimalFormat wholeNumber = new DecimalFormat("###0");
		// DecimalFormat percent = new DecimalFormat("###0.0000");

		StringBuilder sb = new StringBuilder();
		List<String> strings = new ArrayList<String>(5);

		sb.append(wholeNumber.format(this.paymentClearingIndex) + separator);
		sb.append(this.lgaCode + separator);
		String identifier = sb.toString();

		if (this.pnlWagesSalaries > 0f) {
			sb = new StringBuilder(identifier);
			sb.append(decimal.format(this.pnlWagesSalaries) + separator);
			sb.append("Wages");
			strings.add(sb.toString());
		}

		if (this.pnlUnemploymentBenefits > 0f) {
			sb = new StringBuilder(identifier);
			sb.append(decimal.format(this.pnlUnemploymentBenefits) + separator);
			sb.append("Unemployment");
			strings.add(sb.toString());
		}

		if (this.pnlOtherSocialSecurityIncome > 0f) {
			sb = new StringBuilder(identifier);
			sb.append(decimal.format(this.pnlOtherSocialSecurityIncome) + separator);
			sb.append("OtherSocialSecurity");
			strings.add(sb.toString());
		}

		if (this.pnlInvestmentIncome > 0f) {
			sb = new StringBuilder(identifier);
			sb.append(decimal.format(this.pnlInvestmentIncome) + separator);
			sb.append("Investment");
			strings.add(sb.toString());
		}

		if (this.pnlInterestIncome > 0f) {
			sb = new StringBuilder(identifier);
			sb.append(decimal.format(this.pnlInterestIncome) + separator);
			sb.append("Interest");
			strings.add(sb.toString());
		}

		if (this.pnlRentIncome > 0f) {
			sb = new StringBuilder(identifier);
			sb.append(decimal.format(this.pnlRentIncome) + separator);
			sb.append("Rent");
			strings.add(sb.toString());
		}

		if (this.pnlForeignIncome > 0f) {
			sb = new StringBuilder(identifier);
			sb.append(decimal.format(this.pnlForeignIncome) + separator);
			sb.append("Foreign");
			strings.add(sb.toString());
		}

		if (this.pnlOtherIncome > 0f) {
			sb = new StringBuilder(identifier);
			sb.append(decimal.format(this.pnlOtherIncome) + separator);
			sb.append("Other");
			strings.add(sb.toString());
		}

		return strings;
	}

	/**
	 * Gets the column headings, to write to CSV file.
	 * 
	 * @param separator
	 * @return a CSV list of the column headings
	 */
	public String toCsvSalaryByIndustryStringHeaders(String separator) {
		StringBuilder sb = new StringBuilder();

		sb.append("PaymentClearingIndex" + separator);
		sb.append("LgaCode" + separator);

		sb.append("Salary" + separator);
		sb.append("IndustryDivisionCode" + separator);
		sb.append("EmployeeAge" + separator);

		return sb.toString();
	}

	/**
	 * Gets the data, to write to summary CSV file.
	 * 
	 * @param separator
	 * @return a CSV list of the data
	 */
	public List<String> toCsvSalaryByIndustryString(String separator, int iteration) {
		DecimalFormat decimal = new DecimalFormat("###0.00");
		DecimalFormat wholeNumber = new DecimalFormat("###0");
		// DecimalFormat percent = new DecimalFormat("###0.0000");

		StringBuilder sb = new StringBuilder();
		List<String> strings = new ArrayList<String>(5);

		sb.append(wholeNumber.format(this.paymentClearingIndex) + separator);
		sb.append(this.lgaCode + separator);
		String identifier = sb.toString();

		// add salary by industry for each person in household
		for (Individual person : this.individuals) {
			if (person.getPnlWagesSalaries() > 0f) {
				// person is employee
				sb = new StringBuilder(identifier);
				sb.append(decimal.format(this.pnlWagesSalaries) + separator);
				sb.append(person.getEmploymentIndustry() + separator);
				sb.append(person.getAge());
				strings.add(sb.toString());
			}
		}

		return strings;
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
		// MAYBE: donations don't currently go anywhere

		float payable = 0f;
		float calibratedExpenses = this.getTotalExpenses();

		int numberOfCreditors = 2; // government + exogeneous
		if (this.suppliers != null && this.pnlLivingExpenses > 0d) {
			numberOfCreditors += this.suppliers.size();
		}
		if (this.landlord != null && this.pnlRentExpense > 0d) {
			numberOfCreditors++;
		}
		if (this.loanAdi != null && this.bsLoans > 0d) {
			numberOfCreditors++;
		}
		ArrayList<NodePayment> liabilities = new ArrayList<NodePayment>(numberOfCreditors);

		// calculate amounts due to domestic suppliers
		if (this.suppliers != null && this.pnlLivingExpenses > 0d) {
			float totalExpense = this.pnlLivingExpenses + this.pnlWorkRelatedExpenses
					+ this.pnlOtherDiscretionaryExpenses;
			for (int supIdx = 0; supIdx < this.suppliers.size(); supIdx++) {
				int index = this.suppliers.get(supIdx).getPaymentClearingIndex();
				// split expenses per the ABS 6530.0 ratios
				float expense = totalExpense * this.supplierRatios.get(supIdx);
				liabilities.add(new NodePayment(index, expense));
				payable += expense;
			}
		}

		// calculate rent due to landlord
		if (this.landlord != null && this.pnlRentExpense > 0d) {
			liabilities.add(new NodePayment(this.landlord.getPaymentClearingIndex(), this.pnlRentExpense));
			payable += this.pnlRentExpense;
		}

		// calculate loan repayment due to bank
		if (this.loanAdi != null && (this.pnlMortgageRepayments > 0d || this.pnlRentInterestExpense > 0d)) {
			liabilities.add(new NodePayment(this.loanAdi.getPaymentClearingIndex(),
					this.pnlMortgageRepayments + this.pnlRentInterestExpense));
			payable += this.pnlMortgageRepayments + this.pnlRentInterestExpense;
		}

		// calculate income tax due to government
		float incomeTax = 0f;
		for (int indivIdx = 0; indivIdx < this.individuals.length; indivIdx++) {
			incomeTax += Tax.calculateIndividualIncomeTax(individuals[indivIdx].getGrossIncome());
		}
		liabilities.add(new NodePayment(this.govt.getPaymentClearingIndex(), incomeTax));
		payable += incomeTax;

		// calculate exogeneous expenses
		float exogeneousExpenseMultiplier = properties.getExogeneousExpenseMultiplier();
		float exogeneousExpenses = Math.max(0f, calibratedExpenses - payable) * exogeneousExpenseMultiplier;
		liabilities.add(new NodePayment(AustralianEconomy.getExogeneousExpenseAgent().getPaymentClearingIndex(),
				exogeneousExpenses));

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

	/**
	 * Australians can draw down on their superannuation savings if they find
	 * themselves in financial hardship and unable to meet their mortgage
	 * repayments. However, they are not obliged to, so if drawing down on their
	 * superannuation is not enough to get them out of financial hardship then they
	 * can leave their superannuation intact and declare bankruptcy instead.
	 * 
	 * Assume that equity and debt markets are illiquid due to the financial crisis
	 * and so liquidating equities or borrowing to get through a period of negative
	 * cash flow are not possible in the current circumstances.
	 * 
	 * If they have to sell their house, any equity is consumed by bank fees and
	 * legal fees, and they switch to rent payments at a percentage of the former
	 * mortgage repayments.
	 * 
	 * If they have defaulted then they are assumed to cut out all discretionary
	 * spending, so any donations to charities cease too.
	 */
	@Override
	public int processClearingPaymentVectorOutput(float nodeEquity, int iteration, int defaultOrder) {
		int status = Clearable.OK;
		// update default details
		if (defaultOrder > 0) {
			// update default details unless it defaulted in a previous iteration
			if (this.defaultIteration == 0) {
				// hasn't defaulted in a previous iteration
				this.defaultIteration = iteration;
				this.defaultOrder = defaultOrder;
				status = this.makeHouseholdBankrupt(iteration);
			}
		} else {
			// update financials
			if ((this.bsBankDeposits + nodeEquity) > 0f) {
				this.bsBankDeposits += nodeEquity;
			} else {
				// negative cashflow is greater than bank balance
				if ((this.bsBankDeposits + this.bsSuperannuation * (1f - properties.getSuperannuationHaircut())
						+ nodeEquity) > 0f) {
					// drawing down on superannuation will be enough to avoid bankruptcy
					this.bsSuperannuation += (nodeEquity + this.bsBankDeposits)
							* (1f + properties.getSuperannuationHaircut());
					this.bsBankDeposits = 0f;
				} else {
					status = this.makeHouseholdBankrupt(iteration);
				}
				// cut down on discretionary spending too
				this.pnlOtherDiscretionaryExpenses = 0f;
				this.pnlDonations = 0f;
			}
		}
		return status;
	}

	public void applyInflation(float monthlyInflationRate, int iteration) {
		// changes in wage expenses are delayed
		// EBAs last for 3 years on average, so divide the rate by 3
		// TODO: increase income
		this.pnlWagesSalaries = this.pnlWagesSalaries * (1f + monthlyInflationRate / 36f);
		this.pnlUnemploymentBenefits = this.pnlUnemploymentBenefits * (1f + monthlyInflationRate / 36f);
		this.pnlOtherSocialSecurityIncome = this.pnlOtherSocialSecurityIncome * (1f + monthlyInflationRate / 36f);
		// shares are an inflation hedge, so increase investment income at inflation
		this.pnlInvestmentIncome = this.pnlInvestmentIncome * (1f + monthlyInflationRate);

		// N.B. Foreign income responds to FX rates, not inflation

		// mortgages never respond to inflation - only interest rates

		// rent responds to inflation, but with a 6 month lag
		if (iteration >= 6) {
			// increase rent income & expense
			this.pnlRentIncome = this.pnlRentIncome * (1f + monthlyInflationRate);
			this.pnlRentExpense = this.pnlRentExpense * (1f + monthlyInflationRate);
		}

		// N.B. Donations do not respond to inflation

		// increase living expenses, work expenses & other expenses by inflation rate
		this.pnlLivingExpenses = this.pnlLivingExpenses * (1f + monthlyInflationRate);
		this.pnlWorkRelatedExpenses = this.pnlWorkRelatedExpenses * (1f + monthlyInflationRate);
		this.pnlOtherDiscretionaryExpenses = this.pnlOtherDiscretionaryExpenses * (1f + monthlyInflationRate);
	}

	private int makeHouseholdBankrupt(int iteration) {
		// leave superannuation alone because it's not enough to avoid bankruptcy
		this.bsBankDeposits = 0f;
		if (this.pnlMortgageRepayments > 0f) {
			// home owner, so sell home and switch to renting
			this.bsResidentialLandAndDwellings = 0f;
			this.pnlRentExpense = this.pnlMortgageRepayments * properties.getMortgageRentConversionRatio();
			this.pnlMortgageRepayments = 0f;
		}
		// cut down on discretionary spending too
		this.pnlOtherDiscretionaryExpenses = 0f;
		this.pnlDonations = 0f;

		return Clearable.BANKRUPT;
	}

	/**
	 * Calculates the interest component of the loan repayment due to bank. Assumes
	 * home loans are principal and interest, while investment property loans are
	 * interest-only.
	 * 
	 * @param iteration
	 * @return interest component of loan repayments
	 */
	public float getInterestDueToBank(int iteration) {
		float interest = 0f;
		if (this.loanAdi != null && (this.pnlMortgageRepayments > 0d || this.pnlRentInterestExpense > 0d)) {
			float loanBal = CustomMath.getCurrentLoanBalance(this.pnlMortgageRepayments,
					this.loanAdi.getLoanRate(iteration), this.bsResidentialLandAndDwellings,
					this.loanCurrentMonth + iteration);
			interest = loanBal * this.loanAdi.getLoanRate(iteration) + this.pnlRentInterestExpense;
		}
		return interest;
	}

	/**
	 * Sets the household's financials, based on the financials of the Individuals
	 * that it comprises of.
	 * 
	 * Does not set living expenses because the Henderson Poverty Line has already
	 * been calculated and set when the Household was instantiated.
	 * 
	 * The caller sets balance sheet amounts based on ratios after this method has
	 * finished adding the P&L data which it needs.
	 */
	public void initialiseFinancialsFromIndividuals() {
		for (Individual i : this.individuals) {
			// P&L
			this.pnlWagesSalaries += i.getPnlWagesSalaries();
			this.pnlUnemploymentBenefits += i.getPnlUnemploymentBenefits();
			this.pnlOtherSocialSecurityIncome += i.getPnlOtherSocialSecurityIncome();
			this.pnlInvestmentIncome += i.getPnlInvestmentIncome();
			this.pnlInterestIncome += i.getPnlInterestIncome();
			this.pnlRentIncome += i.getPnlRentIncome();
			this.pnlForeignIncome += i.getPnlForeignIncome();
			this.pnlOtherIncome += i.getPnlOtherIncome();

			this.pnlIncomeTaxExpense += i.getPnlIncomeTaxExpense();
			this.pnlWorkRelatedExpenses += i.getPnlWorkRelatedExpenses();
			this.pnlRentInterestExpense += i.getPnlRentInterestExpense();
			this.pnlDonations += i.getPnlDonations();

			// Bal Sht
			this.bsBankDeposits += i.getBsBankDeposits();
			this.bsStudentLoans += i.getBsStudentLoans();
		}
	}

	public float getGrossIncome() {
		return this.pnlWagesSalaries + this.pnlUnemploymentBenefits + this.pnlOtherSocialSecurityIncome
				+ this.pnlInvestmentIncome + this.pnlInterestIncome + this.pnlRentIncome + this.pnlForeignIncome
				+ this.pnlOtherIncome;
	}

	public float getIncomeAfterTax() {
		// TODO: check if the +/- sign is the right way for income tax
		return this.getGrossIncome() - this.pnlIncomeTaxExpense;
	}

	public float getTotalExpenses() {
		return this.pnlLivingExpenses + this.pnlRentExpense + this.pnlMortgageRepayments + this.pnlWorkRelatedExpenses
				+ this.pnlRentInterestExpense + this.pnlDonations + this.pnlOtherDiscretionaryExpenses;
	}

	protected void init() {
		this.individuals = null;
		// this.numAdults = 0;
		// this.numChildren = 0;
		this.lgaCode = null;
		this.state = null;

		this.loanAdi = null;
		this.suppliers = null;
		this.supplierRatios = null;
		this.landlord = null;
		this.govt = null;
		this.defaultIteration = 0;
		this.defaultOrder = 0;

		// P&L
		this.pnlWagesSalaries = 0f;
		this.pnlUnemploymentBenefits = 0f;
		this.pnlOtherSocialSecurityIncome = 0f;
		this.pnlInvestmentIncome = 0f;
		this.pnlInterestIncome = 0f;
		this.pnlRentIncome = 0f;
		this.pnlForeignIncome = 0f;
		this.pnlOtherIncome = 0f;
		this.pnlIncomeTaxExpense = 0f;

		this.pnlLivingExpenses = 0f;
		this.pnlRentExpense = 0f;
		this.pnlMortgageRepayments = 0f;
		this.pnlRentInterestExpense = 0f;
		this.pnlDonations = 0f;
		this.pnlOtherDiscretionaryExpenses = 0f;

		// Bal Sht
		this.bsBankDeposits = 0f;
		this.bsOtherFinancialAssets = 0f;
		this.bsResidentialLandAndDwellings = 0f;
		this.bsOtherNonFinancialAssets = 0f;
		// this.bsTotalAssets = 0f;

		this.bsLoans = 0f;
		this.bsStudentLoans = 0f;
		this.bsOtherLiabilities = 0f;
		// this.bsTotalLiabilities = 0f;

		// this.bsNetWorth = 0f;
	}

	/**
	 * @return the individuals
	 */
	public Individual[] getIndividuals() {
		return individuals;
	}

	/**
	 * @param individuals the individuals to set
	 */
	public void setIndividuals(Individual[] individuals) {
		this.individuals = individuals;
	}

	/**
	 * @return the numAdults
	 */
	public int getNumAdults() {
		int numAdults = 0;
		for (int i = 0; i < this.individuals.length; i++) {
			if (this.individuals[i].getAge() > 17) {
				// adults are 18+ years old
				numAdults++;
			}
		}
		return numAdults;
	}

	/**
	 * @return the numChildren
	 */
	public int getNumChildren() {
		int numChildren = 0;
		for (int i = 0; i < this.individuals.length; i++) {
			if (this.individuals[i].getAge() < 18) {
				// children are under 18 years old
				numChildren++;
			}
		}
		return numChildren;
	}

	/**
	 * @return the lgaCode
	 */
	public String getLgaCode() {
		return lgaCode;
	}

	/**
	 * @param lgaCode the lgaCode to set
	 */
	public void setLgaCode(String lgaCode) {
		this.lgaCode = lgaCode;
	}

	/**
	 * @return the state
	 */
	public String getState() {
		return state;
	}

	/**
	 * @param state the state to set
	 */
	public void setState(String state) {
		this.state = state;
	}

	/**
	 * @return the loanAdi
	 */
	public AuthorisedDepositTakingInstitution getLoanAdi() {
		return loanAdi;
	}

	/**
	 * @param loanAdi the loanAdi to set
	 */
	public void setLoanAdi(AuthorisedDepositTakingInstitution loanAdi) {
		this.loanAdi = loanAdi;
	}

	/**
	 * @return the suppliers
	 */
	public ArrayList<Business> getSuppliers() {
		return suppliers;
	}

	/**
	 * @param suppliers the suppliers to set
	 */
	public void setSuppliers(ArrayList<Business> suppliers) {
		this.suppliers = suppliers;
	}

	/**
	 * @return the supplierRatios
	 */
	public ArrayList<Float> getSupplierRatios() {
		// return supplierRatios;
		float[] primitiveArray = this.supplierRatios.toArray();
		ArrayList<Float> boxedList = new ArrayList<Float>(primitiveArray.length);
		for (Float node : primitiveArray) {
			boxedList.add(node);
		}
		return boxedList;
	}

	/**
	 * @param supplierRatios the supplierRatios to set
	 */
	public void setSupplierRatios(TFloatArrayList supplierRatios) {
		this.supplierRatios = supplierRatios;
	}

	/**
	 * @return the landlord
	 */
	public Household getLandlord() {
		return landlord;
	}

	/**
	 * @param landlord the landlord to set
	 */
	public void setLandlord(Household landlord) {
		this.landlord = landlord;
	}

	/**
	 * @return the govt
	 */
	public AustralianGovernment getGovt() {
		return govt;
	}

	/**
	 * @param govt the govt to set
	 */
	public void setGovt(AustralianGovernment govt) {
		this.govt = govt;
	}

	/**
	 * @return the loanCurrentMonth
	 */
	public int getLoanCurrentMonth() {
		return loanCurrentMonth;
	}

	/**
	 * @param loanCurrentMonth the loanCurrentMonth to set
	 */
	public void setLoanCurrentMonth(int loanCurrentMonth) {
		this.loanCurrentMonth = loanCurrentMonth;
	}

	/**
	 * @return the pnlWagesSalaries
	 */
	public float getPnlWagesSalaries() {
		return pnlWagesSalaries;
	}

	/**
	 * @param pnlWagesSalaries the pnlWagesSalaries to set
	 */
	public void setPnlWagesSalaries(float pnlWagesSalaries) {
		this.pnlWagesSalaries = pnlWagesSalaries;
	}

	/**
	 * @return the pnlUnemploymentBenefits
	 */
	public float getPnlUnemploymentBenefits() {
		return pnlUnemploymentBenefits;
	}

	/**
	 * @param pnlUnemploymentBenefits the pnlUnemploymentBenefits to set
	 */
	public void setPnlUnemploymentBenefits(float pnlUnemploymentBenefits) {
		this.pnlUnemploymentBenefits = pnlUnemploymentBenefits;
	}

	/**
	 * @return the pnlOtherSocialSecurityIncome
	 */
	public float getPnlOtherSocialSecurityIncome() {
		return pnlOtherSocialSecurityIncome;
	}

	/**
	 * @param pnlOtherSocialSecurityIncome the pnlOtherSocialSecurityIncome to set
	 */
	public void setPnlOtherSocialSecurityIncome(float pnlOtherSocialSecurityIncome) {
		this.pnlOtherSocialSecurityIncome = pnlOtherSocialSecurityIncome;
	}

	/**
	 * @return the pnlInvestmentIncome
	 */
	public float getPnlInvestmentIncome() {
		return pnlInvestmentIncome;
	}

	/**
	 * @param pnlInvestmentIncome the pnlInvestmentIncome to set
	 */
	public void setPnlInvestmentIncome(float pnlInvestmentIncome) {
		this.pnlInvestmentIncome = pnlInvestmentIncome;
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
	 * @return the pnlRentIncome
	 */
	public float getPnlRentIncome() {
		return pnlRentIncome;
	}

	/**
	 * @param pnlRentIncome the pnlRentIncome to set
	 */
	public void setPnlRentIncome(float pnlRentIncome) {
		this.pnlRentIncome = pnlRentIncome;
	}

	/**
	 * @return the pnlForeignIncome
	 */
	public float getPnlForeignIncome() {
		return pnlForeignIncome;
	}

	/**
	 * @param pnlForeignIncome the pnlForeignIncome to set
	 */
	public void setPnlForeignIncome(float pnlForeignIncome) {
		this.pnlForeignIncome = pnlForeignIncome;
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
	 * @return the pnlIncomeTaxExpense
	 */
	public float getPnlIncomeTaxExpense() {
		return pnlIncomeTaxExpense;
	}

	/**
	 * @param pnlIncomeTaxExpense the pnlIncomeTaxExpense to set
	 */
	public void setPnlIncomeTaxExpense(float pnlIncomeTaxExpense) {
		// FIXME: income tax expense often exceeds income. FIX THIS
		this.pnlIncomeTaxExpense = pnlIncomeTaxExpense;
	}

	/**
	 * @return the pnlLivingExpenses
	 */
	public float getPnlLivingExpenses() {
		return pnlLivingExpenses;
	}

	/**
	 * @param pnlLivingExpenses the pnlLivingExpenses to set
	 */
	public void setPnlLivingExpenses(float pnlLivingExpenses) {
		this.pnlLivingExpenses = pnlLivingExpenses;
	}

	/**
	 * @return the pnlRentExpense
	 */
	public float getPnlRentExpense() {
		return pnlRentExpense;
	}

	/**
	 * @param pnlRentExpense the pnlRentExpense to set
	 */
	public void setPnlRentExpense(float pnlRentExpense) {
		this.pnlRentExpense = pnlRentExpense;
	}

	/**
	 * @return the pnlMortgageRepayments
	 */
	public float getPnlMortgageRepayments() {
		return pnlMortgageRepayments;
	}

	/**
	 * @param pnlMortgageRepayments the pnlMortgageRepayments to set
	 */
	public void setPnlMortgageRepayments(float pnlMortgageRepayments) {
		this.pnlMortgageRepayments = pnlMortgageRepayments;
	}

	/**
	 * @return the pnlWorkRelatedExpenses
	 */
	public float getPnlWorkRelatedExpenses() {
		return pnlWorkRelatedExpenses;
	}

	/**
	 * @param pnlWorkRelatedExpenses the pnlWorkRelatedExpenses to set
	 */
	public void setPnlWorkRelatedExpenses(float pnlWorkRelatedExpenses) {
		this.pnlWorkRelatedExpenses = pnlWorkRelatedExpenses;
	}

	/**
	 * @return the pnlRentInterestExpense
	 */
	public float getPnlRentInterestExpense() {
		return pnlRentInterestExpense;
	}

	/**
	 * @param pnlRentInterestExpense the pnlRentInterestExpense to set
	 */
	public void setPnlRentInterestExpense(float pnlRentInterestExpense) {
		this.pnlRentInterestExpense = pnlRentInterestExpense;
	}

	/**
	 * @return the pnlDonations
	 */
	public float getPnlDonations() {
		return pnlDonations;
	}

	/**
	 * @param pnlDonations the pnlDonations to set
	 */
	public void setPnlDonations(float pnlDonations) {
		this.pnlDonations = pnlDonations;
	}

	/**
	 * @return the pnlOtherDiscretionaryExpenses
	 */
	public float getPnlOtherDiscretionaryExpenses() {
		return pnlOtherDiscretionaryExpenses;
	}

	/**
	 * @param pnlOtherDiscretionaryExpenses the pnlOtherDiscretionaryExpenses to set
	 */
	public void setPnlOtherDiscretionaryExpenses(float pnlOtherDiscretionaryExpenses) {
		this.pnlOtherDiscretionaryExpenses = pnlOtherDiscretionaryExpenses;
	}

	/**
	 * @return the bsBankDeposits
	 */
	public float getBsBankDeposits() {
		return bsBankDeposits;
	}

	/**
	 * @param bsBankDeposits the bsBankDeposits to set
	 */
	public void setBsBankDeposits(float bsBankDeposits) {
		this.bsBankDeposits = bsBankDeposits;
	}

	/**
	 * @return the bsSuperannuation
	 */
	public float getBsSuperannuation() {
		return bsSuperannuation;
	}

	/**
	 * @param bsSuperannuation the bsSuperannuation to set
	 */
	public void setBsSuperannuation(float bsSuperannuation) {
		this.bsSuperannuation = bsSuperannuation;
	}

	/**
	 * @return the bsEquities
	 */
	public float getBsEquities() {
		return bsEquities;
	}

	/**
	 * @param bsEquities the bsEquities to set
	 */
	public void setBsEquities(float bsEquities) {
		this.bsEquities = bsEquities;
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
	 * @return the bsResidentialLandAndDwellings
	 */
	public float getBsResidentialLandAndDwellings() {
		return bsResidentialLandAndDwellings;
	}

	/**
	 * @param bsResidentialLandAndDwellings the bsResidentialLandAndDwellings to set
	 */
	public void setBsResidentialLandAndDwellings(float bsResidentialLandAndDwellings) {
		this.bsResidentialLandAndDwellings = bsResidentialLandAndDwellings;
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
	 * @return the bsTotalAssets
	 */
	public float getBsTotalAssets() {
		return this.bsBankDeposits + this.bsSuperannuation + this.bsEquities + this.bsOtherFinancialAssets
				+ this.bsResidentialLandAndDwellings + this.bsOtherNonFinancialAssets;
	}

	/**
	 * @param bsTotalAssets the bsTotalAssets to set
	 */
	// public void setBsTotalAssets(float bsTotalAssets) {
	// this.bsTotalAssets = bsTotalAssets;
	// }

	/**
	 * @return the bsLoans
	 */
	public float getBsLoans() {
		return bsLoans;
	}

	/**
	 * @param bsLoans the bsLoans to set
	 */
	public void setBsLoans(float bsLoans) {
		this.bsLoans = bsLoans;
	}

	/**
	 * @return the bsStudentLoans
	 */
	public float getBsStudentLoans() {
		return bsStudentLoans;
	}

	/**
	 * @param bsStudentLoans the bsStudentLoans to set
	 */
	public void setBsStudentLoans(float bsStudentLoans) {
		this.bsStudentLoans = bsStudentLoans;
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

	/**
	 * @return the bsTotalLiabilities
	 */
	public float getBsTotalLiabilities() {
		return this.bsLoans + this.bsStudentLoans + this.bsOtherLiabilities;
	}

	/**
	 * @param bsTotalLiabilities the bsTotalLiabilities to set
	 */
	// public void setBsTotalLiabilities(float bsTotalLiabilities) {
	// this.bsTotalLiabilities = bsTotalLiabilities;
	// }

	/**
	 * @return the bsNetWorth
	 */
	public float getBsNetWorth() {
		return this.getBsTotalAssets() - this.getBsTotalLiabilities();
	}

	/**
	 * @param bsNetWorth the bsNetWorth to set
	 */
	// public void setBsNetWorth(float bsNetWorth) {
	// this.bsNetWorth = bsNetWorth;
	// }

}
