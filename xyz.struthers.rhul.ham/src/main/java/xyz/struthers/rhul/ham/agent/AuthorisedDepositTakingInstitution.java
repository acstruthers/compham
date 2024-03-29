/**
 * 
 */
package xyz.struthers.rhul.ham.agent;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.map.hash.TObjectFloatHashMap;
import xyz.struthers.rhul.ham.config.PropertiesXml;
import xyz.struthers.rhul.ham.config.PropertiesXmlFactory;
import xyz.struthers.rhul.ham.data.CalibrateEconomy;
import xyz.struthers.rhul.ham.process.AustralianEconomy;
import xyz.struthers.rhul.ham.process.Clearable;
import xyz.struthers.rhul.ham.process.Employer;
import xyz.struthers.rhul.ham.process.NodePayment;
import xyz.struthers.rhul.ham.process.Tax;

/**
 * Each instance of this class stores 41 floats and 4 strings, so will consume
 * approximately 388 bytes of RAM (assuming 60 chars total in the strings).
 * There are 86 instances of this class in the model, so they will consume
 * approximately 32.59 kB of RAM.
 * 
 * @author Adam Struthers
 * @since 20-Dec-2018
 */
public abstract class AuthorisedDepositTakingInstitution extends Agent implements Employer {

	private static final boolean DEBUG = false;

	private static final long serialVersionUID = 1L;

	public static final float NUMBER_MONTHS = 12f; // for interest calcs
	private static PropertiesXml properties = PropertiesXmlFactory.getProperties();

	// Company details (approx. 60 chars)
	protected String name;
	protected String australianBusinessNumber;
	protected String shortName;
	protected String adiCategory;
	protected char industryDivisionCode;
	protected String state; // MAYBE: implement state in ADIs
	protected boolean isGccsa; // MAYBE: implement capital city in ADIs

	// agent relationships
	protected int paymentClearingIndex;
	protected ArrayList<Individual> employees; // calculate wages & super
	protected ArrayList<Business> domesticSuppliers;
	protected TFloatArrayList domesticSupplierRatios;
	protected ArrayList<Household> retailDepositors;
	protected ArrayList<Business> commercialDepositors;
	protected ArrayList<AuthorisedDepositTakingInstitution> adiInvestors;
	protected TFloatArrayList adiInvestorAmounts;
	protected AustralianGovernment govt;
	protected ReserveBankOfAustralia rba;
	protected int defaultIteration;
	protected int defaultOrder;

	// P&L (40 bytes)
	protected float pnlInterestIncome;
	protected float pnlInterestExpense;
	protected float pnlTradingIncome;
	protected float pnlInvestmentIncome;
	protected float pnlOtherIncome;

	protected float pnlPersonnelExpenses;
	protected float pnlLoanImpairmentExpense;
	protected float pnlCommittedLiquidityFacilityFees;
	protected float pnlDepreciationAmortisation;
	protected float pnlOtherExpenses;

	protected float pnlIncomeTaxExpense;

	// Bal Sht (76 bytes)
	protected float bsCash;
	protected float bsTradingSecurities;
	protected float bsDerivativeAssets;
	protected float bsInvestments;
	protected float bsLoansPersonal;
	protected float bsLoansHome;
	protected float bsLoansBusiness;
	protected float bsLoansADI;
	protected float bsLoansGovernment;
	protected float bsOtherNonFinancialAssets;

	protected float bsDepositsAtCall;
	protected float bsDepositsTerm;
	protected float bsDepositsAdiRepoEligible;
	protected float bsDerivativeLiabilities;
	protected float bsBondsNotesBorrowings;
	protected float bsOtherLiabilities;

	protected float bsRetainedEarnings;
	protected float bsReserves;
	protected float bsOtherEquity;

	// Metrics (48 bytes)
	protected float rateCash;
	protected float rateTrading;
	protected float rateInvestment;
	protected float rateAdiLoan;
	protected float rateGovernmentLoan;
	protected float rateTotalLoans;
	protected float rateTotalDeposits;
	protected float rateBondsNotesBorrowings;
	protected float capitalTotalRatio;
	protected float capitalTotalAmount;
	protected float capitalTotalRWA;
	protected float capitalCreditRWA;

	// effective interest rates
	protected TFloatArrayList depositRate;
	protected TFloatArrayList loanRate;
	protected TFloatArrayList borrowingsRate;
	protected TFloatArrayList govtBondRate;

	/**
	 * Default constructor
	 */
	public AuthorisedDepositTakingInstitution() {
		super();
	}

	/**
	 * Initialisation constructor
	 * 
	 * @param adiAustralianBusinessNumber
	 * @param adiShortName
	 * @param adiLongName
	 * @param adiType
	 * @param financialStatementAmounts
	 */
	public AuthorisedDepositTakingInstitution(String adiAustralianBusinessNumber, String adiShortName,
			String adiLongName, String adiType, TObjectFloatHashMap<String> financialStatementAmounts) {
		super();

		// Company details
		this.name = adiLongName;
		this.shortName = adiShortName;
		this.australianBusinessNumber = adiAustralianBusinessNumber;
		this.adiCategory = adiType;

		// P&L
		this.pnlInterestIncome = financialStatementAmounts.get("pnlInterestIncome");
		this.pnlInterestExpense = financialStatementAmounts.get("pnlInterestExpense");
		this.pnlTradingIncome = financialStatementAmounts.get("pnlTradingIncome");
		this.pnlInvestmentIncome = financialStatementAmounts.get("pnlInvestmentIncome");
		this.pnlOtherIncome = financialStatementAmounts.get("pnlOtherIncome");

		this.pnlPersonnelExpenses = financialStatementAmounts.get("pnlPersonnelExpenses");
		this.pnlLoanImpairmentExpense = financialStatementAmounts.get("pnlLoanImpairmentExpense");
		this.pnlDepreciationAmortisation = financialStatementAmounts.get("pnlDepreciationAmortisation");
		this.pnlOtherExpenses = financialStatementAmounts.get("pnlOtherExpenses");

		this.pnlIncomeTaxExpense = financialStatementAmounts.get("pnlIncomeTaxExpense");

		// Bal Sht
		this.bsCash = financialStatementAmounts.get("bsCash");
		this.bsTradingSecurities = financialStatementAmounts.get("bsTradingSecurities");
		this.bsDerivativeAssets = financialStatementAmounts.get("bsDerivativeAssets");
		this.bsInvestments = financialStatementAmounts.get("bsInvestments");
		this.bsLoansPersonal = financialStatementAmounts.get("bsLoansPersonal");
		this.bsLoansHome = financialStatementAmounts.get("bsLoansHome");
		this.bsLoansBusiness = financialStatementAmounts.get("bsLoansBusiness");
		this.bsLoansADI = financialStatementAmounts.get("bsLoansADI");
		this.bsLoansGovernment = financialStatementAmounts.get("bsLoansGovernment");
		this.bsOtherNonFinancialAssets = financialStatementAmounts.get("bsOtherNonFinancialAssets");

		this.bsDepositsAtCall = financialStatementAmounts.get("bsDepositsAtCall");
		this.bsDepositsTerm = financialStatementAmounts.get("bsDepositsTerm");
		this.bsDepositsAdiRepoEligible = financialStatementAmounts.get("bsDepositsAdiRepoEligible");
		this.bsDerivativeLiabilities = financialStatementAmounts.get("bsDerivativeLiabilities");
		this.bsBondsNotesBorrowings = financialStatementAmounts.get("bsBondsNotesBorrowings");
		this.bsOtherLiabilities = financialStatementAmounts.get("bsOtherLiabilities");

		this.bsRetainedEarnings = financialStatementAmounts.get("bsRetainedEarnings");
		this.bsReserves = financialStatementAmounts.get("bsReserves");
		this.bsOtherEquity = financialStatementAmounts.get("bsOtherEquity");

		// Metrics
		this.rateCash = financialStatementAmounts.get("rateCash");
		this.rateTrading = financialStatementAmounts.get("rateTrading");
		this.rateInvestment = financialStatementAmounts.get("rateInvestment");
		this.rateAdiLoan = financialStatementAmounts.get("rateAdiLoan");
		this.rateGovernmentLoan = financialStatementAmounts.get("rateGovernmentLoan");
		this.rateTotalLoans = financialStatementAmounts.get("rateTotalLoans");
		this.rateTotalDeposits = financialStatementAmounts.get("rateTotalDeposits");
		this.rateBondsNotesBorrowings = financialStatementAmounts.get("rateBondsNotesBorrowings");
		this.capitalTotalRatio = financialStatementAmounts.get("capitalTotalRatio");
		this.capitalTotalAmount = financialStatementAmounts.get("capitalTotalAmount");
		this.capitalTotalRWA = financialStatementAmounts.get("capitalTotalRWA");
		this.capitalCreditRWA = financialStatementAmounts.get("capitalCreditRWA");
	}

	public float getTotalIncome() {
		return this.pnlInterestIncome - this.pnlInterestExpense + this.pnlTradingIncome + this.pnlInvestmentIncome
				+ this.pnlOtherIncome;
	}

	public float getTotalExpensesExcludingTax() {
		return this.pnlPersonnelExpenses + this.pnlLoanImpairmentExpense + this.pnlDepreciationAmortisation
				+ this.pnlOtherExpenses;
	}

	public float getDepositRate(int iteration) {
		float rate = 0f;
		if (this.depositRate != null && this.depositRate.size() > iteration) {
			rate = this.depositRate.get(iteration);
		}
		return rate;
	}

	public float setDepositRate(int iteration) {
		if (this.depositRate == null) {
			this.depositRate = new TFloatArrayList(iteration + 1);
			for (int i = 0; i < iteration; i++) {
				this.depositRate.add(this.rateTotalDeposits);
			}
		}
		float rate = 0f;
		if (this.depositRate != null && this.rba != null) {
			// assume rates can't be negative
			if (this.depositRate.size() > 0) {
				rate = this.depositRate.get(0) + this.rba.getCashRateChange(iteration);
			} else {
				rate = this.rateTotalDeposits + this.rba.getCashRateChange(iteration);
			}
			if (!properties.isAllowNegativerates()) {
				rate = Math.max(0f, rate);
			}
			if (this.depositRate.size() > iteration) {
				this.depositRate.set(iteration, rate);
			} else {
				this.depositRate.add(rate);
			}
		}
		return rate;
	}

	public float getLoanRate(int iteration) {
		float rate = 0f;
		if (this.loanRate != null && this.loanRate.size() > iteration) {
			rate = this.loanRate.get(iteration);
		}
		return rate;
	}

	public float setLoanRate(int iteration) {
		if (this.loanRate == null) {
			this.loanRate = new TFloatArrayList(iteration + 1);
			for (int i = 0; i < iteration; i++) {
				this.loanRate.add(this.rateTotalLoans);
			}
		}
		float rate = 0f;
		if (this.loanRate != null && this.rba != null) {
			// assume rates can't be negative
			if (this.loanRate.size() > 0) {
				rate = this.loanRate.get(0) + this.rba.getCashRateChange(iteration);
			} else {
				rate = this.rateTotalLoans + this.rba.getCashRateChange(iteration);
			}
			if (!properties.isAllowNegativerates()) {
				rate = Math.max(0f, rate);
			}
			if (this.loanRate.size() > iteration) {
				this.loanRate.set(iteration, rate);
			} else {
				this.loanRate.add(rate);
			}
		}
		return rate;
	}

	public float getBorrowingsRate(int iteration) {
		float rate = 0f;
		if (this.borrowingsRate != null && this.borrowingsRate.size() > iteration) {
			rate = this.borrowingsRate.get(iteration);
		}
		return rate;
	}

	public float setBorrowingsRate(int iteration) {
		if (this.borrowingsRate == null) {
			this.borrowingsRate = new TFloatArrayList();
			for (int i = 0; i < iteration; i++) {
				this.borrowingsRate.add(this.rateBondsNotesBorrowings);
			}
		}
		float rate = 0f;
		if (this.borrowingsRate != null && this.rba != null) {
			// assume rates can't be negative
			if (this.borrowingsRate.size() > 0) {
				rate = this.borrowingsRate.get(0) + this.rba.getCashRateChange(iteration);
			} else {
				rate = this.rateBondsNotesBorrowings + this.rba.getCashRateChange(iteration);
			}
			if (!properties.isAllowNegativerates()) {
				rate = Math.max(0f, rate);
			}
			if (this.borrowingsRate.size() > iteration) {
				this.borrowingsRate.set(iteration, rate);
			} else {
				this.borrowingsRate.add(rate);
			}
		}
		return rate;
	}

	public float getGovtBondRate(int iteration) {
		float rate = 0f;
		if (this.govtBondRate != null && this.govtBondRate.size() > iteration) {
			rate = this.govtBondRate.get(iteration);
		}
		return rate;
	}

	public float setGovtBondRate(int iteration) {
		if (this.govtBondRate == null) {
			this.govtBondRate = new TFloatArrayList();
			for (int i = 0; i < iteration; i++) {
				this.govtBondRate.add(this.rateGovernmentLoan);
			}
		}
		float rate = 0f;
		if (this.govtBondRate != null && this.rba != null) {
			// assume rates can't be negative
			if (this.govtBondRate.size() > 0) {
				rate = this.govtBondRate.get(0) + this.rba.getCashRateChange(iteration);
			} else {
				rate = this.rateGovernmentLoan + this.rba.getCashRateChange(iteration);
			}
			if (!properties.isAllowNegativerates()) {
				rate = Math.max(0f, rate);
			}
			if (this.govtBondRate.size() > iteration) {
				this.govtBondRate.set(iteration, rate);
			} else {
				this.govtBondRate.add(rate);
			}
		}
		return rate;
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
		sb.append("ABN" + separator);
		sb.append("ShortName" + separator);
		sb.append("AdiCategory" + separator);
		sb.append("IndustryDivisionCode" + separator);
		sb.append("State" + separator);
		sb.append("IsGccsa" + separator);
		sb.append("EmployeeCount" + separator);
		sb.append("DomesticSupplierCount" + separator);
		sb.append("RetailDepositorCount" + separator);
		sb.append("CommercialDepositorCount" + separator);
		sb.append("AdiInvestorCount" + separator);
		sb.append("AdiInvestorAmount" + separator);
		sb.append("GovernmentID" + separator);
		sb.append("RbaID" + separator);
		sb.append("InterestIncome" + separator);
		sb.append("InterestExpense" + separator);
		sb.append("TradingIncome" + separator);
		sb.append("InvestmentIncome" + separator);
		sb.append("OtherIncome" + separator);
		sb.append("PersonnelExpenses" + separator);
		sb.append("LoanImpairmentExpense" + separator);
		sb.append("Depreciation" + separator);
		sb.append("OtherExpenses" + separator);
		sb.append("IncomeTaxExpense" + separator);
		sb.append("Cash" + separator);
		sb.append("TradingSecurities" + separator);
		sb.append("DerivativeAssets" + separator);
		sb.append("Investments" + separator);
		sb.append("LoansPersonal" + separator);
		sb.append("LoansHome" + separator);
		sb.append("LoansBusiness" + separator);
		sb.append("LoansADI" + separator);
		sb.append("LoansGovernment" + separator);
		sb.append("OtherNonFinancialAssets" + separator);
		sb.append("DepositsAtCall" + separator);
		sb.append("DepositsTerm" + separator);
		sb.append("DepositsAdiRepoEligible" + separator);
		sb.append("DerivativeLiabilities" + separator);
		sb.append("BondsNotesBorrowings" + separator);
		sb.append("OtherLiabilities" + separator);
		sb.append("RetainedEarnings" + separator);
		sb.append("Reserves" + separator);
		sb.append("OtherEquity" + separator);
		sb.append("RateCash" + separator);
		sb.append("RateTrading" + separator);
		sb.append("RateInvestment" + separator);
		sb.append("RateAdiLoan" + separator);
		sb.append("RateGovernmentLoan" + separator);
		sb.append("RateTotalLoans" + separator);
		sb.append("RateTotalDeposits" + separator);
		sb.append("RateBondsNotesBorrowings" + separator);
		sb.append("CapitalTotalRatio" + separator);
		sb.append("CapitalTotalAmount" + separator);
		sb.append("CapitalTotalRWA" + separator);
		sb.append("CapitalCreditRWA" + separator);
		sb.append("CurrentDepositRate" + separator);
		sb.append("CurrentLoanRate" + separator);
		sb.append("CurrentBorrowingsRate" + separator);
		sb.append("CurrentGovtBondRate" + separator);
		sb.append("DefaultIteration" + separator);
		sb.append("DefaultOrder");

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

		sb.append(this.name.replace(",", " ") + separator);
		sb.append(wholeNumber.format(this.paymentClearingIndex) + separator);
		sb.append(this.australianBusinessNumber + separator);
		sb.append(this.shortName.replace(",", " ") + separator);
		sb.append(this.adiCategory + separator);
		sb.append(this.industryDivisionCode + separator);
		sb.append(this.state + separator);
		sb.append((this.isGccsa ? "Y" : "N") + separator);
		sb.append(wholeNumber.format(this.employees != null ? this.employees.size() : 0) + separator);
		sb.append(wholeNumber.format(this.domesticSuppliers != null ? this.domesticSuppliers.size() : 0) + separator);
		sb.append(wholeNumber.format(this.retailDepositors != null ? this.retailDepositors.size() : 0) + separator);
		sb.append(wholeNumber.format(this.commercialDepositors != null ? this.commercialDepositors.size() : 0)
				+ separator);
		sb.append(wholeNumber.format(this.adiInvestors != null ? this.adiInvestors.size() : 0) + separator);
		if (this.adiInvestorAmounts != null) {
			// float sum = this.adiInvestorAmounts.stream().reduce(0f, Float::sum);
			float sum = 0f;
			for (int investorIdx = 0; investorIdx < this.adiInvestorAmounts.size(); investorIdx++) {
				sum += this.adiInvestorAmounts.get(investorIdx);
			}
			sb.append(decimal.format(sum) + separator);
		} else {
			sb.append(decimal.format(0) + separator);
		}
		sb.append(wholeNumber.format(this.govt != null ? this.govt.getPaymentClearingIndex() : 0) + separator);
		sb.append(wholeNumber.format(this.rba != null ? this.rba.getPaymentClearingIndex() : 0) + separator);
		sb.append(decimal.format(this.pnlInterestIncome) + separator);
		sb.append(decimal.format(this.pnlInterestExpense) + separator);
		sb.append(decimal.format(this.pnlTradingIncome) + separator);
		sb.append(decimal.format(this.pnlInvestmentIncome) + separator);
		sb.append(decimal.format(this.pnlOtherIncome) + separator);
		sb.append(decimal.format(this.pnlPersonnelExpenses) + separator);
		sb.append(decimal.format(this.pnlLoanImpairmentExpense) + separator);
		sb.append(decimal.format(this.pnlDepreciationAmortisation) + separator);
		sb.append(decimal.format(this.pnlOtherExpenses) + separator);
		sb.append(decimal.format(this.pnlIncomeTaxExpense) + separator);
		sb.append(decimal.format(this.bsCash) + separator);
		sb.append(decimal.format(this.bsCash) + separator);
		sb.append(decimal.format(this.bsTradingSecurities) + separator);
		sb.append(decimal.format(this.bsDerivativeAssets) + separator);
		sb.append(decimal.format(this.bsInvestments) + separator);
		sb.append(decimal.format(this.bsLoansPersonal) + separator);
		sb.append(decimal.format(this.bsLoansHome) + separator);
		sb.append(decimal.format(this.bsLoansBusiness) + separator);
		sb.append(decimal.format(this.bsLoansADI) + separator);
		sb.append(decimal.format(this.bsLoansGovernment) + separator);
		sb.append(decimal.format(this.bsOtherNonFinancialAssets) + separator);
		sb.append(decimal.format(this.bsDepositsAtCall) + separator);
		sb.append(decimal.format(this.bsDepositsTerm) + separator);
		sb.append(decimal.format(this.bsDepositsAdiRepoEligible) + separator);
		sb.append(decimal.format(this.bsDerivativeLiabilities) + separator);
		sb.append(decimal.format(this.bsBondsNotesBorrowings) + separator);
		sb.append(decimal.format(this.bsOtherLiabilities) + separator);
		sb.append(decimal.format(this.bsRetainedEarnings) + separator);
		sb.append(decimal.format(this.bsReserves) + separator);
		sb.append(decimal.format(this.bsOtherEquity) + separator);
		sb.append(percent.format(this.rateCash) + separator);
		sb.append(percent.format(this.rateTrading) + separator);
		sb.append(percent.format(this.rateInvestment) + separator);
		sb.append(percent.format(this.rateAdiLoan) + separator);
		sb.append(percent.format(this.rateGovernmentLoan) + separator);
		sb.append(percent.format(this.rateTotalLoans) + separator);
		sb.append(percent.format(this.rateTotalDeposits) + separator);
		sb.append(percent.format(this.rateBondsNotesBorrowings) + separator);
		sb.append(percent.format(this.capitalTotalRatio) + separator);
		sb.append(decimal.format(this.capitalTotalAmount) + separator);
		sb.append(decimal.format(this.capitalTotalRWA) + separator);
		sb.append(decimal.format(this.capitalCreditRWA) + separator);
		sb.append(percent.format(this.depositRate != null ? this.depositRate.get(iteration) : 0) + separator);
		sb.append(percent.format(this.loanRate != null ? this.loanRate.get(iteration) : 0) + separator);
		sb.append(percent.format(this.borrowingsRate != null ? this.borrowingsRate.get(iteration) : 0) + separator);
		sb.append(percent.format(this.govtBondRate != null ? this.govtBondRate.get(iteration) : 0));
		sb.append(decimal.format(this.defaultIteration) + separator);
		sb.append(decimal.format(this.defaultOrder));

		return sb.toString();
	}

	/**
	 * Gets the summary column headings, to write to CSV file.
	 * 
	 * @param separator
	 * @return a CSV list of the column headings
	 */
	public String toCsvSummaryStringHeaders(String separator) {
		return this.toCsvStringHeaders(separator);
	}

	/**
	 * Gets the summary data, to write to CSV file.
	 * 
	 * @param separator
	 * @return a CSV list of the data
	 */
	public String toCsvSummaryString(String separator, int iteration) {
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

		float payable = 0f;
		float calibratedExpenses = this.getTotalExpensesExcludingTax() + this.getPnlIncomeTaxExpense();

		int numberOfCreditors = 2; // government + exogeneous
		if (this.employees != null && this.pnlPersonnelExpenses > 0d) {
			numberOfCreditors += this.employees.size();
		}
		if (this.domesticSuppliers != null && this.pnlOtherExpenses > 0d) {
			numberOfCreditors += this.domesticSuppliers.size();
		}
		if (this.retailDepositors != null) {
			numberOfCreditors += this.retailDepositors.size();
		}
		if (this.commercialDepositors != null) {
			numberOfCreditors += this.commercialDepositors.size();
		}
		if (this.adiInvestors != null) {
			numberOfCreditors += this.adiInvestors.size();
		}
		if (this.pnlCommittedLiquidityFacilityFees > 0f) {
			numberOfCreditors++; // RBA
		}
		ArrayList<NodePayment> liabilities = new ArrayList<NodePayment>(numberOfCreditors);

		// calculate wages due to employees (incl. superannuation)
		if (this.employees != null && this.pnlPersonnelExpenses > 0d) {
			for (Individual employee : this.employees) {
				int index = employee.getPaymentClearingIndex();
				float monthlyWagesIncludingSuper = employee.getPnlWagesSalaries()
						* (1f + properties.getSuperannuationGuaranteeRate());
				liabilities.add(new NodePayment(index, monthlyWagesIncludingSuper));
				payable += monthlyWagesIncludingSuper;
			}
		}

		// calculate amounts due to domestic suppliers
		if (this.domesticSuppliers != null && this.pnlOtherExpenses > 0d) {
			for (int busIdx = 0; busIdx < this.domesticSuppliers.size(); busIdx++) {
				int index = this.domesticSuppliers.get(busIdx).getPaymentClearingIndex();
				float expense = this.pnlOtherExpenses * this.domesticSupplierRatios.get(busIdx);
				liabilities.add(new NodePayment(index, expense));
				payable += expense;
			}
		}

		// calculate amount due to retail depositors
		for (Household depositor : this.retailDepositors) {
			int index = depositor.getPaymentClearingIndex();
			if (DEBUG) {
				if (this.depositRate != null) {
					System.out.print(this.shortName + " depositRate = ");
					for (int i = 0; i < this.depositRate.size(); i++) {
						System.out.print(this.depositRate.get(i) + ",");
					}
					System.out.println(".");
				} else {
					System.out.println("depositRate is null");
				}
			}
			float monthlyInterest = depositor.getBsBankDeposits() * this.depositRate.get(iteration) / NUMBER_MONTHS;
			liabilities.add(new NodePayment(index, monthlyInterest));
			payable += monthlyInterest;
		}

		// calculate amount due to business depositors
		if (this.commercialDepositors != null) {
			for (Business depositor : this.commercialDepositors) {
				int index = depositor.getPaymentClearingIndex();
				float monthlyInterest = depositor.getBankDeposits() * this.depositRate.get(iteration) / NUMBER_MONTHS;
				liabilities.add(new NodePayment(index, monthlyInterest));
				payable += monthlyInterest;
			}
		}

		// calculate amount due to ADI investors
		if (this.adiInvestors != null) {
			for (int adiIdx = 0; adiIdx < this.adiInvestors.size(); adiIdx++) {
				AuthorisedDepositTakingInstitution adi = this.adiInvestors.get(adiIdx);
				int index = adi.getPaymentClearingIndex();
				int borrowingsRateMonth = Math.max(iteration - 3, 0); // assumes 90-day funding
				float monthlyInterest = this.adiInvestorAmounts.get(adiIdx)
						* this.borrowingsRate.get(borrowingsRateMonth) / NUMBER_MONTHS;
				liabilities.add(new NodePayment(index, monthlyInterest));
				payable += monthlyInterest;
			}
		}

		// calculate tax due to government (payroll & income)
		float payrollTax = 0f;
		if (this.employees != null && this.pnlPersonnelExpenses > 0d) {
			float totalWages = 0f;
			for (Individual employee : this.employees) {
				totalWages += employee.getPnlWagesSalaries();
			}
			payrollTax = Tax.calculatePayrollTax(totalWages, this.state, this.isGccsa);
		}
		float totalTax = payrollTax + Tax.calculateCompanyTax(this.getTotalIncome(),
				this.getTotalIncome() - this.getTotalExpensesExcludingTax() - payrollTax);
		liabilities.add(new NodePayment(this.govt.getPaymentClearingIndex(), totalTax));
		payable += totalTax;

		// calculate Committed Liquidity Facility (CLF) fees due to RBA
		if (this.pnlCommittedLiquidityFacilityFees > 0f) {
			liabilities
					.add(new NodePayment(this.rba.getPaymentClearingIndex(), this.pnlCommittedLiquidityFacilityFees));
			payable += this.pnlCommittedLiquidityFacilityFees;
		}

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
				status = this.makeAdiBankrupt(iteration);
			}
		} else {
			// update financials
			if ((this.bsCash + nodeEquity) > 0f) {
				// cash is enough, so just update cash balance
				this.bsCash += nodeEquity;
			} else {
				if ((this.bsCash + this.bsInvestments * properties.getAdiHqlaProportion() + nodeEquity) > 0f) {
					// the ADI holds sufficient liquid assets, so liquidate the amount needed
					nodeEquity += this.bsCash;
					this.bsCash = 0f;
					float newInvestmentIncome = this.pnlInvestmentIncome * (1f - (-nodeEquity / this.bsInvestments));
					this.pnlInvestmentIncome = newInvestmentIncome;
					this.bsInvestments += nodeEquity;
				} else {
					// the ADI holds insufficient liquid assets, so shut it down
					status = this.makeAdiBankrupt(iteration);
				}
			}
		}
		return status;
	}

	public void applyInflation(float monthlyInflationRate, int iteration) {
		// interest income & expense don't respond to inflation, only interest rates
		// trading income doesn't respond to inflation
		// investment income doesn't respond to inflation

		this.pnlOtherIncome = this.pnlOtherIncome * (1f + monthlyInflationRate);

		// changes in wages are delayed by 12 months
		if ((iteration % 12) == 0) {
			// TODO: increase personnel expenses

			// re-calculate income tax expense
		}

		// loan impairment doesn't respond to inflation
		// CLF fees don't vary with inflation
		// Depreciation expense responds to inflation with a 12 month lag
		if ((iteration % 12) == 0) {
			// TODO increase depreciation 20% at a time, assuming 5 year life of assets

		}

		this.pnlOtherExpenses = this.pnlOtherExpenses * (1f + monthlyInflationRate);

		// re-calculate income tax expense
		float grossRevenue = this.getTotalIncome();
		float taxableRevenue = grossRevenue - this.getTotalExpensesExcludingTax();
		this.pnlIncomeTaxExpense = Tax.calculateCompanyTax(this.getTotalIncome(), taxableRevenue);
	}

	private int makeAdiBankrupt(int iteration) {
		// ADI is bankrupt, so fire all employees
		for (Individual employee : this.employees) {
			employee.fireEmployee();
		}

		// assign everyone to a new bank
		// apply the Financial Claims Scheme to all depositors
		// interest payments were processed by the CPV, now we're distributing "capital"
		for (int adiIdx = 0; adiIdx < this.adiInvestors.size(); adiIdx++) {
			// under the FCS legislation it could be inferred that secured creditors take
			// precedence over depositors, so we process them first.
			AuthorisedDepositTakingInstitution adi = this.adiInvestors.get(adiIdx);
			float liquidationValue = this.adiInvestorAmounts.get(adiIdx) * properties.getInvestmentHaircut();
			float newCashBalance = adi.getBsCash() + liquidationValue;
			adi.setBsCash(newCashBalance);
		}
		// calculate FCS limit
		float fcsAdiLimit = properties.getFcsLimitPerAdi();
		float totalDeposits = this.bsDepositsAtCall + this.bsDepositsTerm + this.bsDepositsAdiRepoEligible;
		float fcsGuaranteedRatio = totalDeposits > fcsAdiLimit ? fcsAdiLimit / totalDeposits : 1f;
		// distribute deposits to
		if (this.retailDepositors != null) {
			for (Household household : this.retailDepositors) {
				float newDepositBal = Math.min(household.getBsBankDeposits() * fcsGuaranteedRatio,
						properties.getFcsLimitPerDepositor());
				household.setBsBankDeposits(newDepositBal);
			}
		}
		if (this.commercialDepositors != null) {
			for (Business business : this.commercialDepositors) {
				float newDepositBal = Math.min(business.getBankDeposits() * fcsGuaranteedRatio,
						properties.getFcsLimitPerDepositor());
				business.setBankDeposits(newDepositBal);
			}
		}

		// ADI is bankrupt, so zero out all its financials
		// it will have no financial impact on any other agent in future iterations
		// P&L
		this.pnlInterestIncome = 0f;
		this.pnlInterestExpense = 0f;
		this.pnlTradingIncome = 0f;
		this.pnlInvestmentIncome = 0f;
		this.pnlOtherIncome = 0f;
		this.pnlPersonnelExpenses = 0f;
		this.pnlLoanImpairmentExpense = 0f;
		this.pnlCommittedLiquidityFacilityFees = 0f;
		this.pnlDepreciationAmortisation = 0f;
		this.pnlOtherExpenses = 0f;
		this.pnlIncomeTaxExpense = 0f;

		// Bal Sht
		this.bsCash = 0f;
		this.bsTradingSecurities = 0f;
		this.bsDerivativeAssets = 0f;
		this.bsInvestments = 0f;
		this.bsLoansPersonal = 0f;
		this.bsLoansHome = 0f;
		this.bsLoansBusiness = 0f;
		this.bsLoansADI = 0f;
		this.bsLoansGovernment = 0f;
		this.bsOtherNonFinancialAssets = 0f;
		this.bsDepositsAtCall = 0f;
		this.bsDepositsTerm = 0f;
		this.bsDepositsAdiRepoEligible = 0f;
		this.bsDerivativeLiabilities = 0f;
		this.bsBondsNotesBorrowings = 0f;
		this.bsOtherLiabilities = 0f;
		this.bsRetainedEarnings = 0f;
		this.bsReserves = 0f;
		this.bsOtherEquity = 0f;

		return Clearable.BANKRUPT;

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

	protected void init() {
		super.init();

		// Company details
		this.australianBusinessNumber = null;
		this.shortName = null;
		this.adiCategory = null;
		this.industryDivisionCode = '\0';
		this.state = null;
		this.isGccsa = false;

		// agent relationships
		this.paymentClearingIndex = 0;
		this.employees = null;
		this.domesticSuppliers = null;
		this.domesticSupplierRatios = null;
		this.retailDepositors = null;
		this.commercialDepositors = null;
		this.adiInvestors = null;
		this.adiInvestorAmounts = null;
		this.govt = null;
		this.rba = null;
		this.defaultIteration = 0;
		this.defaultOrder = 0;

		// P&L
		this.pnlInterestIncome = 0f;
		this.pnlInterestExpense = 0f;
		this.pnlTradingIncome = 0f;
		this.pnlInvestmentIncome = 0f;
		this.pnlOtherIncome = 0f;
		this.pnlPersonnelExpenses = 0f;
		this.pnlLoanImpairmentExpense = 0f;
		this.pnlCommittedLiquidityFacilityFees = 0f;
		this.pnlDepreciationAmortisation = 0f;
		this.pnlOtherExpenses = 0f;
		this.pnlIncomeTaxExpense = 0f;

		// Bal Sht
		this.bsCash = 0f;
		this.bsTradingSecurities = 0f;
		this.bsDerivativeAssets = 0f;
		this.bsInvestments = 0f;
		this.bsLoansPersonal = 0f;
		this.bsLoansHome = 0f;
		this.bsLoansBusiness = 0f;
		this.bsLoansADI = 0f;
		this.bsLoansGovernment = 0f;
		this.bsOtherNonFinancialAssets = 0f;
		this.bsDepositsAtCall = 0f;
		this.bsDepositsTerm = 0f;
		this.bsDepositsAdiRepoEligible = 0f;
		this.bsDerivativeLiabilities = 0f;
		this.bsBondsNotesBorrowings = 0f;
		this.bsOtherLiabilities = 0f;
		this.bsRetainedEarnings = 0f;
		this.bsReserves = 0f;
		this.bsOtherEquity = 0f;

		// Metrics
		this.rateCash = 0f;
		this.rateTrading = 0f;
		this.rateInvestment = 0f;
		this.rateAdiLoan = 0f;
		this.rateGovernmentLoan = 0f;
		this.rateTotalLoans = 0f;
		this.rateTotalDeposits = 0f;
		this.rateBondsNotesBorrowings = 0f;
		this.capitalTotalRatio = 0f;
		this.capitalTotalAmount = 0f;
		this.capitalTotalRWA = 0f;
		this.capitalCreditRWA = 0f;

		// effective interest rates
		this.depositRate = null;
		this.loanRate = null;
		this.borrowingsRate = null;
		this.govtBondRate = null;
	}

	/**
	 * @return the domesticSuppliers
	 */
	public ArrayList<Business> getDomesticSuppliers() {
		return domesticSuppliers;
	}

	/**
	 * @param domesticSuppliers the domesticSuppliers to set
	 */
	public void setDomesticSuppliers(ArrayList<Business> domesticSuppliers) {
		this.domesticSuppliers = domesticSuppliers;
	}

	/**
	 * @param domesticSupplier the domesticSupplier to set
	 */
	public void addDomesticSupplier(Business domesticSupplier) {
		if (this.domesticSuppliers == null) {
			this.domesticSuppliers = new ArrayList<Business>(CalibrateEconomy.BUSINESS_SUPPLIER_DIV_CODE.length);
		}
		this.domesticSuppliers.add(domesticSupplier);
	}

	/**
	 * Trims the list to size to minimise memory usage.
	 */
	public void trimDomesticSuppliersList() {
		if (this.domesticSuppliers != null) {
			this.domesticSuppliers.trimToSize();
		}
	}

	/**
	 * @return the domesticSupplierRatios
	 */
	public TFloatArrayList getDomesticSupplierRatios() {
		return domesticSupplierRatios;
	}

	/**
	 * @param domesticSupplierRatios the domesticSupplierRatios to set
	 */
	public void setDomesticSupplierRatios(TFloatArrayList domesticSupplierRatios) {
		this.domesticSupplierRatios = domesticSupplierRatios;
	}

	/**
	 * @return the retailDepositors
	 */
	public ArrayList<Household> getRetailDepositors() {
		return retailDepositors;
	}

	/**
	 * @param retailDepositors the retailDepositors to set
	 */
	public void setRetailDepositors(ArrayList<Household> retailDepositors) {
		this.retailDepositors = retailDepositors;
	}

	/**
	 * @param retailDepositor the retailDepositor to set
	 */
	public void addRetailDepositor(Household retailDepositor) {
		if (this.retailDepositors == null) {
			this.retailDepositors = new ArrayList<Household>();
		}
		this.retailDepositors.add(retailDepositor);
	}

	/**
	 * @return the commercialDepositors
	 */
	public ArrayList<Business> getCommercialDepositors() {
		return commercialDepositors;
	}

	/**
	 * @param commercialDepositors the commercialDepositors to set
	 */
	public void setCommercialDepositors(ArrayList<Business> commercialDepositors) {
		this.commercialDepositors = commercialDepositors;
	}

	/**
	 * @param commercialDepositor the commercialDepositor to set
	 */
	public void addCommercialDepositor(Business commercialDepositor) {
		if (this.commercialDepositors == null) {
			this.commercialDepositors = new ArrayList<Business>();
		}
		this.commercialDepositors.add(commercialDepositor);
	}

	/**
	 * Trims the Commercial Depositors list to minimise memory consumption.
	 */
	public void trimCommercialDepositorList() {
		if (this.commercialDepositors != null) {
			this.commercialDepositors.trimToSize();
		}
	}

	/**
	 * @return the adiInvestors
	 */
	public ArrayList<AuthorisedDepositTakingInstitution> getAdiInvestors() {
		return adiInvestors;
	}

	/**
	 * @param adiInvestors the adiInvestors to set
	 */
	public void setAdiInvestors(ArrayList<AuthorisedDepositTakingInstitution> adiInvestors) {
		this.adiInvestors = adiInvestors;
	}

	/**
	 * @return the adiInvestorAmounts
	 */
	public TFloatArrayList getAdiInvestorAmounts() {
		return adiInvestorAmounts;
	}

	/**
	 * @param adiInvestorAmounts the adiInvestorAmounts to set
	 */
	public void setAdiInvestorAmounts(TFloatArrayList adiInvestorAmounts) {
		this.adiInvestorAmounts = adiInvestorAmounts;
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
	 * @return the rba
	 */
	public ReserveBankOfAustralia getRba() {
		return rba;
	}

	/**
	 * @param rba the rba to set
	 */
	public void setRba(ReserveBankOfAustralia rba) {
		this.rba = rba;
	}

	/**
	 * @param employees the employees to set
	 */
	public void setEmployees(ArrayList<Individual> employees) {
		this.employees = employees;
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
	 * @return the australianBusinessNumber
	 */
	public String getAustralianBusinessNumber() {
		return australianBusinessNumber;
	}

	/**
	 * @param australianBusinessNumber the australianBusinessNumber to set
	 */
	public void setAustralianBusinessNumber(String australianBusinessNumber) {
		this.australianBusinessNumber = australianBusinessNumber;
	}

	/**
	 * @return the shortName
	 */
	public String getShortName() {
		return shortName;
	}

	/**
	 * @param shortName the shortName to set
	 */
	public void setShortName(String shortName) {
		this.shortName = shortName;
	}

	/**
	 * @return the adiCategory
	 */
	public String getAdiCategory() {
		return adiCategory;
	}

	/**
	 * @param adiCategory the adiCategory to set
	 */
	public void setAdiCategory(String adiCategory) {
		this.adiCategory = adiCategory;
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
	 * @return the isGccsa
	 */
	public boolean isGccsa() {
		return isGccsa;
	}

	/**
	 * @param isGccsa the isGccsa to set
	 */
	public void setGccsa(boolean isGccsa) {
		this.isGccsa = isGccsa;
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
	 * @return the pnlTradingIncome
	 */
	public float getPnlTradingIncome() {
		return pnlTradingIncome;
	}

	/**
	 * @param pnlTradingIncome the pnlTradingIncome to set
	 */
	public void setPnlTradingIncome(float pnlTradingIncome) {
		this.pnlTradingIncome = pnlTradingIncome;
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
	 * @return the pnlLoanImpairmentExpense
	 */
	public float getPnlLoanImpairmentExpense() {
		return pnlLoanImpairmentExpense;
	}

	/**
	 * @param pnlLoanImpairmentExpense the pnlLoanImpairmentExpense to set
	 */
	public void setPnlLoanImpairmentExpense(float pnlLoanImpairmentExpense) {
		this.pnlLoanImpairmentExpense = pnlLoanImpairmentExpense;
	}

	/**
	 * @return the pnlCommittedLiquidityFacilityFees
	 */
	public float getPnlCommittedLiquidityFacilityFees() {
		return pnlCommittedLiquidityFacilityFees;
	}

	/**
	 * @param pnlCommittedLiquidityFacilityFees the
	 *                                          pnlCommittedLiquidityFacilityFees to
	 *                                          set
	 */
	public void setPnlCommittedLiquidityFacilityFees(float pnlCommittedLiquidityFacilityFees) {
		this.pnlCommittedLiquidityFacilityFees = pnlCommittedLiquidityFacilityFees;
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
	 * @return the pnlIncomeTaxExpense
	 */
	public float getPnlIncomeTaxExpense() {
		return pnlIncomeTaxExpense;
	}

	/**
	 * @param pnlIncomeTaxExpense the pnlIncomeTaxExpense to set
	 */
	public void setPnlIncomeTaxExpense(float pnlIncomeTaxExpense) {
		this.pnlIncomeTaxExpense = pnlIncomeTaxExpense;
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
	 * @return the bsTradingSecurities
	 */
	public float getBsTradingSecurities() {
		return bsTradingSecurities;
	}

	/**
	 * @param bsTradingSecurities the bsTradingSecurities to set
	 */
	public void setBsTradingSecurities(float bsTradingSecurities) {
		this.bsTradingSecurities = bsTradingSecurities;
	}

	/**
	 * @return the bsDerivativeAssets
	 */
	public float getBsDerivativeAssets() {
		return bsDerivativeAssets;
	}

	/**
	 * @param bsDerivativeAssets the bsDerivativeAssets to set
	 */
	public void setBsDerivativeAssets(float bsDerivativeAssets) {
		this.bsDerivativeAssets = bsDerivativeAssets;
	}

	/**
	 * @return the bsInvestments
	 */
	public float getBsInvestments() {
		return bsInvestments;
	}

	/**
	 * @param bsInvestments the bsInvestments to set
	 */
	public void setBsInvestments(float bsInvestments) {
		this.bsInvestments = bsInvestments;
	}

	/**
	 * @return the bsLoansPersonal
	 */
	public float getBsLoansPersonal() {
		return bsLoansPersonal;
	}

	/**
	 * @param bsLoansPersonal the bsLoansPersonal to set
	 */
	public void setBsLoansPersonal(float bsLoansPersonal) {
		this.bsLoansPersonal = bsLoansPersonal;
	}

	/**
	 * @return the bsLoansHome
	 */
	public float getBsLoansHome() {
		return bsLoansHome;
	}

	/**
	 * @param bsLoansHome the bsLoansHome to set
	 */
	public void setBsLoansHome(float bsLoansHome) {
		this.bsLoansHome = bsLoansHome;
	}

	/**
	 * @return the bsLoansBusiness
	 */
	public float getBsLoansBusiness() {
		return bsLoansBusiness;
	}

	/**
	 * @param bsLoansBusiness the bsLoansBusiness to set
	 */
	public void setBsLoansBusiness(float bsLoansBusiness) {
		this.bsLoansBusiness = bsLoansBusiness;
	}

	/**
	 * @return the bsLoansADI
	 */
	public float getBsLoansADI() {
		return bsLoansADI;
	}

	/**
	 * @param bsLoansADI the bsLoansADI to set
	 */
	public void setBsLoansADI(float bsLoansADI) {
		this.bsLoansADI = bsLoansADI;
	}

	/**
	 * @return the bsLoansGovernment
	 */
	public float getBsLoansGovernment() {
		return bsLoansGovernment;
	}

	/**
	 * @param bsLoansGovernment the bsLoansGovernment to set
	 */
	public void setBsLoansGovernment(float bsLoansGovernment) {
		this.bsLoansGovernment = bsLoansGovernment;
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
	 * @return the bsDepositsAtCall
	 */
	public float getBsDepositsAtCall() {
		return bsDepositsAtCall;
	}

	/**
	 * @param bsDepositsAtCall the bsDepositsAtCall to set
	 */
	public void setBsDepositsAtCall(float bsDepositsAtCall) {
		this.bsDepositsAtCall = bsDepositsAtCall;
	}

	/**
	 * @return the bsDepositsTerm
	 */
	public float getBsDepositsTerm() {
		return bsDepositsTerm;
	}

	/**
	 * @param bsDepositsTerm the bsDepositsTerm to set
	 */
	public void setBsDepositsTerm(float bsDepositsTerm) {
		this.bsDepositsTerm = bsDepositsTerm;
	}

	/**
	 * @return the bsDepositsAdiRepoEligible
	 */
	public float getBsDepositsAdiRepoEligible() {
		return bsDepositsAdiRepoEligible;
	}

	/**
	 * @param bsDepositsAdiRepoEligible the bsDepositsAdiRepoEligible to set
	 */
	public void setBsDepositsAdiRepoEligible(float bsDepositsAdiRepoEligible) {
		this.bsDepositsAdiRepoEligible = bsDepositsAdiRepoEligible;
	}

	/**
	 * @return the bsDerivativeLiabilities
	 */
	public float getBsDerivativeLiabilities() {
		return bsDerivativeLiabilities;
	}

	/**
	 * @param bsDerivativeLiabilities the bsDerivativeLiabilities to set
	 */
	public void setBsDerivativeLiabilities(float bsDerivativeLiabilities) {
		this.bsDerivativeLiabilities = bsDerivativeLiabilities;
	}

	/**
	 * @return the bsBondsNotesBorrowings
	 */
	public float getBsBondsNotesBorrowings() {
		return bsBondsNotesBorrowings;
	}

	/**
	 * @param bsBondsNotesBorrowings the bsBondsNotesBorrowings to set
	 */
	public void setBsBondsNotesBorrowings(float bsBondsNotesBorrowings) {
		this.bsBondsNotesBorrowings = bsBondsNotesBorrowings;
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
	 * @return the bsRetainedEarnings
	 */
	public float getBsRetainedEarnings() {
		return bsRetainedEarnings;
	}

	/**
	 * @param bsRetainedEarnings the bsRetainedEarnings to set
	 */
	public void setBsRetainedEarnings(float bsRetainedEarnings) {
		this.bsRetainedEarnings = bsRetainedEarnings;
	}

	/**
	 * @return the bsReserves
	 */
	public float getBsReserves() {
		return bsReserves;
	}

	/**
	 * @param bsReserves the bsReserves to set
	 */
	public void setBsReserves(float bsReserves) {
		this.bsReserves = bsReserves;
	}

	/**
	 * @return the bsOtherEquity
	 */
	public float getBsOtherEquity() {
		return bsOtherEquity;
	}

	/**
	 * @param bsOtherEquity the bsOtherEquity to set
	 */
	public void setBsOtherEquity(float bsOtherEquity) {
		this.bsOtherEquity = bsOtherEquity;
	}

	/**
	 * @return the rateCash
	 */
	public float getRateCash() {
		return rateCash;
	}

	/**
	 * @param rateCash the rateCash to set
	 */
	public void setRateCash(float rateCash) {
		this.rateCash = rateCash;
	}

	/**
	 * @return the rateTrading
	 */
	public float getRateTrading() {
		return rateTrading;
	}

	/**
	 * @param rateTrading the rateTrading to set
	 */
	public void setRateTrading(float rateTrading) {
		this.rateTrading = rateTrading;
	}

	/**
	 * @return the rateInvestment
	 */
	public float getRateInvestment() {
		return rateInvestment;
	}

	/**
	 * @param rateInvestment the rateInvestment to set
	 */
	public void setRateInvestment(float rateInvestment) {
		this.rateInvestment = rateInvestment;
	}

	/**
	 * @return the rateAdiLoan
	 */
	public float getRateAdiLoan() {
		return rateAdiLoan;
	}

	/**
	 * @param rateAdiLoan the rateAdiLoan to set
	 */
	public void setRateAdiLoan(float rateAdiLoan) {
		this.rateAdiLoan = rateAdiLoan;
	}

	/**
	 * @return the rateGovernmentLoan
	 */
	public float getRateGovernmentLoan() {
		return rateGovernmentLoan;
	}

	/**
	 * @param rateGovernmentLoan the rateGovernmentLoan to set
	 */
	public void setRateGovernmentLoan(float rateGovernmentLoan) {
		this.rateGovernmentLoan = rateGovernmentLoan;
	}

	/**
	 * @return the rateTotalLoans
	 */
	public float getRateTotalLoans() {
		return rateTotalLoans;
	}

	/**
	 * @param rateTotalLoans the rateTotalLoans to set
	 */
	public void setRateTotalLoans(float rateTotalLoans) {
		this.rateTotalLoans = rateTotalLoans;
	}

	/**
	 * @return the rateTotalDeposits
	 */
	public float getRateTotalDeposits() {
		return rateTotalDeposits;
	}

	/**
	 * @param rateTotalDeposits the rateTotalDeposits to set
	 */
	public void setRateTotalDeposits(float rateTotalDeposits) {
		this.rateTotalDeposits = rateTotalDeposits;
	}

	/**
	 * @return the rateBondsNotesBorrowings
	 */
	public float getRateBondsNotesBorrowings() {
		return rateBondsNotesBorrowings;
	}

	/**
	 * @param rateBondsNotesBorrowings the rateBondsNotesBorrowings to set
	 */
	public void setRateBondsNotesBorrowings(float rateBondsNotesBorrowings) {
		this.rateBondsNotesBorrowings = rateBondsNotesBorrowings;
	}

	/**
	 * @return the capitalTotalRatio
	 */
	public float getCapitalTotalRatio() {
		return capitalTotalRatio;
	}

	/**
	 * @param capitalTotalRatio the capitalTotalRatio to set
	 */
	public void setCapitalTotalRatio(float capitalTotalRatio) {
		this.capitalTotalRatio = capitalTotalRatio;
	}

	/**
	 * @return the capitalTotalAmount
	 */
	public float getCapitalTotalAmount() {
		return capitalTotalAmount;
	}

	/**
	 * @param capitalTotalAmount the capitalTotalAmount to set
	 */
	public void setCapitalTotalAmount(float capitalTotalAmount) {
		this.capitalTotalAmount = capitalTotalAmount;
	}

	/**
	 * @return the capitalTotalRWA
	 */
	public float getCapitalTotalRWA() {
		return capitalTotalRWA;
	}

	/**
	 * @param capitalTotalRWA the capitalTotalRWA to set
	 */
	public void setCapitalTotalRWA(float capitalTotalRWA) {
		this.capitalTotalRWA = capitalTotalRWA;
	}

	/**
	 * @return the capitalCreditRWA
	 */
	public float getCapitalCreditRWA() {
		return capitalCreditRWA;
	}

	/**
	 * @param capitalCreditRWA the capitalCreditRWA to set
	 */
	public void setCapitalCreditRWA(float capitalCreditRWA) {
		this.capitalCreditRWA = capitalCreditRWA;
	}

}
