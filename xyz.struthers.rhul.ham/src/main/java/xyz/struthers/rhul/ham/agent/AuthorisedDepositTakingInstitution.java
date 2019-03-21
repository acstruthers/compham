/**
 * 
 */
package xyz.struthers.rhul.ham.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import xyz.struthers.rhul.ham.config.Properties;
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

	private static final long serialVersionUID = 1L;

	public static final float NUMBER_MONTHS = 12f; // for interest calcs

	// Company details (approx. 60 chars)
	protected String australianBusinessNumber;
	protected String shortName;
	protected String adiCategory;
	protected String state; // FIXME: implement state
	protected boolean isGccsa; // FIXME: implement capital city

	// agent relationships
	protected int paymentClearingIndex;
	protected ArrayList<Individual> employees; // calculate wages & super
	protected ArrayList<Business> domesticSuppliers;
	protected ArrayList<Float> domesticSupplierRatios;
	protected ArrayList<Household> retailDepositors; // TODO: implement me
	protected ArrayList<Business> commercialDepositors;// TODO: implement me
	protected ArrayList<AuthorisedDepositTakingInstitution> adiInvestors;// TODO: implement me
	protected ArrayList<Float> adiInvestorAmounts;// TODO: implement me
	protected AustralianGovernment govt;
	protected ReserveBankOfAustralia rba;

	// P&L (40 bytes)
	protected float pnlInterestIncome;
	protected float pnlInterestExpense;
	protected float pnlTradingIncome;
	protected float pnlInvestmentIncome;
	protected float pnlOtherIncome;

	protected float pnlPersonnelExpenses;
	protected float pnlLoanImpairmentExpense;
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
	protected ArrayList<Float> depositRate;
	protected ArrayList<Float> loanRate;
	protected ArrayList<Float> borrowingsRate;
	protected ArrayList<Float> govtBondRate;

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
			String adiLongName, String adiType, Map<String, Float> financialStatementAmounts) {
		super();

		// Company details
		super.name = adiLongName;
		this.shortName = adiShortName;
		this.australianBusinessNumber = adiAustralianBusinessNumber;
		this.adiCategory = adiType;

		// P&L
		this.pnlInterestIncome = financialStatementAmounts.get("pnlInterestIncome").floatValue();
		this.pnlInterestExpense = financialStatementAmounts.get("pnlInterestExpense").floatValue();
		this.pnlTradingIncome = financialStatementAmounts.get("pnlTradingIncome").floatValue();
		this.pnlInvestmentIncome = financialStatementAmounts.get("pnlInvestmentIncome").floatValue();
		this.pnlOtherIncome = financialStatementAmounts.get("pnlOtherIncome").floatValue();

		this.pnlPersonnelExpenses = financialStatementAmounts.get("pnlPersonnelExpenses").floatValue();
		this.pnlLoanImpairmentExpense = financialStatementAmounts.get("pnlLoanImpairmentExpense").floatValue();
		this.pnlDepreciationAmortisation = financialStatementAmounts.get("pnlDepreciationAmortisation").floatValue();
		this.pnlOtherExpenses = financialStatementAmounts.get("pnlOtherExpenses").floatValue();

		this.pnlIncomeTaxExpense = financialStatementAmounts.get("pnlIncomeTaxExpense").floatValue();

		// Bal Sht
		this.bsCash = financialStatementAmounts.get("bsCash").floatValue();
		this.bsTradingSecurities = financialStatementAmounts.get("bsTradingSecurities").floatValue();
		this.bsDerivativeAssets = financialStatementAmounts.get("bsDerivativeAssets").floatValue();
		this.bsInvestments = financialStatementAmounts.get("bsInvestments").floatValue();
		this.bsLoansPersonal = financialStatementAmounts.get("bsLoansPersonal").floatValue();
		this.bsLoansHome = financialStatementAmounts.get("bsLoansHome").floatValue();
		this.bsLoansBusiness = financialStatementAmounts.get("bsLoansBusiness").floatValue();
		this.bsLoansADI = financialStatementAmounts.get("bsLoansADI").floatValue();
		this.bsLoansGovernment = financialStatementAmounts.get("bsLoansGovernment").floatValue();
		this.bsOtherNonFinancialAssets = financialStatementAmounts.get("bsOtherNonFinancialAssets").floatValue();

		this.bsDepositsAtCall = financialStatementAmounts.get("bsDepositsAtCall").floatValue();
		this.bsDepositsTerm = financialStatementAmounts.get("bsDepositsTerm").floatValue();
		this.bsDepositsAdiRepoEligible = financialStatementAmounts.get("bsDepositsAdiRepoEligible").floatValue();
		this.bsDerivativeLiabilities = financialStatementAmounts.get("bsDerivativeLiabilities").floatValue();
		this.bsBondsNotesBorrowings = financialStatementAmounts.get("bsBondsNotesBorrowings").floatValue();
		this.bsOtherLiabilities = financialStatementAmounts.get("bsOtherLiabilities").floatValue();

		this.bsRetainedEarnings = financialStatementAmounts.get("bsRetainedEarnings").floatValue();
		this.bsReserves = financialStatementAmounts.get("bsReserves").floatValue();
		this.bsOtherEquity = financialStatementAmounts.get("bsOtherEquity").floatValue();

		// Metrics
		this.rateCash = financialStatementAmounts.get("rateCash").floatValue();
		this.rateTrading = financialStatementAmounts.get("rateTrading").floatValue();
		this.rateInvestment = financialStatementAmounts.get("rateInvestment").floatValue();
		this.rateAdiLoan = financialStatementAmounts.get("rateAdiLoan").floatValue();
		this.rateGovernmentLoan = financialStatementAmounts.get("rateGovernmentLoan").floatValue();
		this.rateTotalLoans = financialStatementAmounts.get("rateTotalLoans").floatValue();
		this.rateTotalDeposits = financialStatementAmounts.get("rateTotalDeposits").floatValue();
		this.rateBondsNotesBorrowings = financialStatementAmounts.get("rateBondsNotesBorrowings").floatValue();
		this.capitalTotalRatio = financialStatementAmounts.get("capitalTotalRatio").floatValue();
		this.capitalTotalAmount = financialStatementAmounts.get("capitalTotalAmount").floatValue();
		this.capitalTotalRWA = financialStatementAmounts.get("capitalTotalRWA").floatValue();
		this.capitalCreditRWA = financialStatementAmounts.get("capitalCreditRWA").floatValue();
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
		float rate = 0f;
		if (this.depositRate != null && this.depositRate.size() > iteration && this.rba != null) {
			// assume rates can't be negative
			rate = this.depositRate.get(0) + this.rba.getCashRateChange(iteration);
			if (!Properties.ALLOW_NEGATIVE_RATES) {
				rate = Math.max(0f, rate);
			}
			this.depositRate.set(iteration, rate);
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
		float rate = 0f;
		if (this.loanRate != null && this.loanRate.size() > iteration && this.rba != null) {
			// assume rates can't be negative
			rate = this.loanRate.get(0) + this.rba.getCashRateChange(iteration);
			if (!Properties.ALLOW_NEGATIVE_RATES) {
				rate = Math.max(0f, rate);
			}
			this.loanRate.set(iteration, rate);
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
		float rate = 0f;
		if (this.borrowingsRate != null && this.borrowingsRate.size() > iteration && this.rba != null) {
			// assume rates can't be negative
			rate = this.borrowingsRate.get(0) + this.rba.getCashRateChange(iteration);
			if (!Properties.ALLOW_NEGATIVE_RATES) {
				rate = Math.max(0f, rate);
			}
			this.borrowingsRate.set(iteration, rate);
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
		float rate = 0f;
		if (this.govtBondRate != null && this.govtBondRate.size() > iteration && this.rba != null) {
			// assume rates can't be negative
			rate = this.govtBondRate.get(0) + this.rba.getCashRateChange(iteration);
			if (!Properties.ALLOW_NEGATIVE_RATES) {
				rate = Math.max(0f, rate);
			}
			this.govtBondRate.set(iteration, rate);
		}
		return rate;
	}

	@Override
	public List<Individual> getEmployees() {
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
	public int getPaymentClearingIndex() {
		return this.paymentClearingIndex;
	}

	@Override
	public void setPaymentClearingIndex(int index) {
		this.paymentClearingIndex = index;
	}

	@Override
	public List<NodePayment> getAmountsPayable(int iteration) {
		int numberOfCreditors = 1; // government
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
		ArrayList<NodePayment> liabilities = new ArrayList<NodePayment>(numberOfCreditors);

		// calculate wages due to employees (incl. superannuation)
		if (this.employees != null && this.pnlPersonnelExpenses > 0d) {
			for (Individual employee : this.employees) {
				int index = employee.getPaymentClearingIndex();
				float monthlyWagesIncludingSuper = employee.getPnlWagesSalaries()
						* (1f + Properties.SUPERANNUATION_RATE);
				liabilities.add(new NodePayment(index, monthlyWagesIncludingSuper));
			}
		}

		// calculate amounts due to domestic suppliers
		if (this.domesticSuppliers != null && this.pnlOtherExpenses > 0d) {
			for (int busIdx = 0; busIdx < this.domesticSuppliers.size(); busIdx++) {
				int index = this.domesticSuppliers.get(busIdx).getPaymentClearingIndex();
				float expense = this.pnlOtherExpenses * this.domesticSupplierRatios.get(busIdx);
				liabilities.add(new NodePayment(index, expense));
			}
		}

		// calculate amount due to retail depositors
		for (Household depositor : this.retailDepositors) {
			int index = depositor.getPaymentClearingIndex();
			float monthlyInterest = depositor.getBsBankDeposits() * this.depositRate.get(iteration) / NUMBER_MONTHS;
			liabilities.add(new NodePayment(index, monthlyInterest));
		}

		// calculate amount due to business depositors
		if (this.commercialDepositors != null) {
			for (Business depositor : this.commercialDepositors) {
				int index = depositor.getPaymentClearingIndex();
				float monthlyInterest = depositor.getBankDeposits() * this.depositRate.get(iteration) / NUMBER_MONTHS;
				liabilities.add(new NodePayment(index, monthlyInterest));
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
		liabilities.add(new NodePayment(govt.getPaymentClearingIndex(), totalTax));

		liabilities.trimToSize();
		return liabilities;
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
