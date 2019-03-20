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
 * Each instance of this class stores 41 doubles and 4 strings, so will consume
 * approximately 388 bytes of RAM (assuming 60 chars total in the strings).
 * There are 86 instances of this class in the model, so they will consume
 * approximately 32.59 kB of RAM.
 * 
 * @author Adam Struthers
 * @since 20-Dec-2018
 */
public abstract class AuthorisedDepositTakingInstitution extends Agent implements Employer {

	private static final long serialVersionUID = 1L;

	public static final double NUMBER_MONTHS = 12d; // for interest calcs

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
	protected ArrayList<Double> supplierRatios;
	protected AustralianGovernment govt;
	protected ArrayList<Household> retailDepositors; // TODO: implement me
	protected ArrayList<Business> commercialDepositors;// TODO: implement me
	protected ArrayList<AuthorisedDepositTakingInstitution> adiInvestors;// TODO: implement me
	protected ArrayList<Double> adiInvestorAmounts;// TODO: implement me

	// P&L (80 bytes)
	protected double pnlInterestIncome;
	protected double pnlInterestExpense;
	protected double pnlTradingIncome;
	protected double pnlInvestmentIncome;
	protected double pnlOtherIncome;

	protected double pnlPersonnelExpenses;
	protected double pnlLoanImpairmentExpense;
	protected double pnlDepreciationAmortisation;
	protected double pnlOtherExpenses;

	protected double pnlIncomeTaxExpense;

	// Bal Sht (152 bytes)
	protected double bsCash;
	protected double bsTradingSecurities;
	protected double bsDerivativeAssets;
	protected double bsInvestments;
	protected double bsLoansPersonal;
	protected double bsLoansHome;
	protected double bsLoansBusiness;
	protected double bsLoansADI;
	protected double bsLoansGovernment;
	protected double bsOtherNonFinancialAssets;

	protected double bsDepositsAtCall;
	protected double bsDepositsTerm;
	protected double bsDepositsAdiRepoEligible;
	protected double bsDerivativeLiabilities;
	protected double bsBondsNotesBorrowings;
	protected double bsOtherLiabilities;

	protected double bsRetainedEarnings;
	protected double bsReserves;
	protected double bsOtherEquity;

	// Metrics (96 bytes)
	protected double rateCash;
	protected double rateTrading;
	protected double rateInvestment;
	protected double rateAdiLoan;
	protected double rateGovernmentLoan;
	protected double rateTotalLoans;
	protected double rateTotalDeposits;
	protected double rateBondsNotesBorrowings;
	protected double capitalTotalRatio;
	protected double capitalTotalAmount;
	protected double capitalTotalRWA;
	protected double capitalCreditRWA;

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
			String adiLongName, String adiType, Map<String, Double> financialStatementAmounts) {
		super();

		// Company details
		super.name = adiLongName;
		this.shortName = adiShortName;
		this.australianBusinessNumber = adiAustralianBusinessNumber;
		this.adiCategory = adiType;

		// P&L
		this.pnlInterestIncome = financialStatementAmounts.get("pnlInterestIncome").doubleValue();
		this.pnlInterestExpense = financialStatementAmounts.get("pnlInterestExpense").doubleValue();
		this.pnlTradingIncome = financialStatementAmounts.get("pnlTradingIncome").doubleValue();
		this.pnlInvestmentIncome = financialStatementAmounts.get("pnlInvestmentIncome").doubleValue();
		this.pnlOtherIncome = financialStatementAmounts.get("pnlOtherIncome").doubleValue();

		this.pnlPersonnelExpenses = financialStatementAmounts.get("pnlPersonnelExpenses").doubleValue();
		this.pnlLoanImpairmentExpense = financialStatementAmounts.get("pnlLoanImpairmentExpense").doubleValue();
		this.pnlDepreciationAmortisation = financialStatementAmounts.get("pnlDepreciationAmortisation").doubleValue();
		this.pnlOtherExpenses = financialStatementAmounts.get("pnlOtherExpenses").doubleValue();

		this.pnlIncomeTaxExpense = financialStatementAmounts.get("pnlIncomeTaxExpense").doubleValue();

		// Bal Sht
		this.bsCash = financialStatementAmounts.get("bsCash").doubleValue();
		this.bsTradingSecurities = financialStatementAmounts.get("bsTradingSecurities").doubleValue();
		this.bsDerivativeAssets = financialStatementAmounts.get("bsDerivativeAssets").doubleValue();
		this.bsInvestments = financialStatementAmounts.get("bsInvestments").doubleValue();
		this.bsLoansPersonal = financialStatementAmounts.get("bsLoansPersonal").doubleValue();
		this.bsLoansHome = financialStatementAmounts.get("bsLoansHome").doubleValue();
		this.bsLoansBusiness = financialStatementAmounts.get("bsLoansBusiness").doubleValue();
		this.bsLoansADI = financialStatementAmounts.get("bsLoansADI").doubleValue();
		this.bsLoansGovernment = financialStatementAmounts.get("bsLoansGovernment").doubleValue();
		this.bsOtherNonFinancialAssets = financialStatementAmounts.get("bsOtherNonFinancialAssets").doubleValue();

		this.bsDepositsAtCall = financialStatementAmounts.get("bsDepositsAtCall").doubleValue();
		this.bsDepositsTerm = financialStatementAmounts.get("bsDepositsTerm").doubleValue();
		this.bsDepositsAdiRepoEligible = financialStatementAmounts.get("bsDepositsAdiRepoEligible").doubleValue();
		this.bsDerivativeLiabilities = financialStatementAmounts.get("bsDerivativeLiabilities").doubleValue();
		this.bsBondsNotesBorrowings = financialStatementAmounts.get("bsBondsNotesBorrowings").doubleValue();
		this.bsOtherLiabilities = financialStatementAmounts.get("bsOtherLiabilities").doubleValue();

		this.bsRetainedEarnings = financialStatementAmounts.get("bsRetainedEarnings").doubleValue();
		this.bsReserves = financialStatementAmounts.get("bsReserves").doubleValue();
		this.bsOtherEquity = financialStatementAmounts.get("bsOtherEquity").doubleValue();

		// Metrics
		this.rateCash = financialStatementAmounts.get("rateCash").doubleValue();
		this.rateTrading = financialStatementAmounts.get("rateTrading").doubleValue();
		this.rateInvestment = financialStatementAmounts.get("rateInvestment").doubleValue();
		this.rateAdiLoan = financialStatementAmounts.get("rateAdiLoan").doubleValue();
		this.rateGovernmentLoan = financialStatementAmounts.get("rateGovernmentLoan").doubleValue();
		this.rateTotalLoans = financialStatementAmounts.get("rateTotalLoans").doubleValue();
		this.rateTotalDeposits = financialStatementAmounts.get("rateTotalDeposits").doubleValue();
		this.rateBondsNotesBorrowings = financialStatementAmounts.get("rateBondsNotesBorrowings").doubleValue();
		this.capitalTotalRatio = financialStatementAmounts.get("capitalTotalRatio").doubleValue();
		this.capitalTotalAmount = financialStatementAmounts.get("capitalTotalAmount").doubleValue();
		this.capitalTotalRWA = financialStatementAmounts.get("capitalTotalRWA").doubleValue();
		this.capitalCreditRWA = financialStatementAmounts.get("capitalCreditRWA").doubleValue();
	}

	public double getTotalIncome() {
		// FIXME: implement me
		return 0d;
	}

	public double getTotalExpenses() {
		// FIXME: implement me
		return 0d;
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
		// there will always be retail depositors
		numberOfCreditors += this.retailDepositors.size();
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
				double monthlyWagesIncludingSuper = employee.getPnlWagesSalaries()
						* (1d + Properties.SUPERANNUATION_RATE);
				liabilities.add(new NodePayment(index, monthlyWagesIncludingSuper));
			}
		}

		// calculate amounts due to domestic suppliers
		if (this.domesticSuppliers != null && this.pnlOtherExpenses > 0d) {
			for (Business supplier : this.domesticSuppliers) {
				int index = supplier.getPaymentClearingIndex();
				double expense = this.pnlOtherExpenses / this.domesticSuppliers.size();
				liabilities.add(new NodePayment(index, expense));
			}
		}

		// calculate tax due to government (payroll & income)
		double payrollTax = 0d;
		if (this.employees != null && this.pnlPersonnelExpenses > 0d) {
			double totalWages = 0d;
			for (Individual employee : this.employees) {
				totalWages += employee.getPnlWagesSalaries();
			}
			payrollTax = Tax.calculatePayrollTax(totalWages, this.state, this.isGccsa);
		}
		double totalTax = payrollTax
				+ Tax.calculateCompanyTax(this.getTotalIncome(), this.getTotalIncome() - this.getTotalExpenses());
		liabilities.add(new NodePayment(govt.getPaymentClearingIndex(), totalTax));

		// calculate amount due to retail depositors
		for (Household depositor : this.retailDepositors) {
			int index = depositor.getPaymentClearingIndex();
			double monthlyInterest = depositor.getBsBankDeposits() * depositor.getInterestRateDeposits()
					/ NUMBER_MONTHS;
			liabilities.add(new NodePayment(index, monthlyInterest));
		}

		// calculate amount due to business depositors
		if (this.commercialDepositors != null) {
			for (Business depositor : this.commercialDepositors) {
				int index = depositor.getPaymentClearingIndex();
				double monthlyInterest = depositor.getBankDeposits() * depositor.getInterestRateDeposits()
						/ NUMBER_MONTHS;
				liabilities.add(new NodePayment(index, monthlyInterest));
			}
		}

		// calculate amount due to ADI investors
		if (this.adiInvestors != null) {
			for (int adiIdx = 0; adiIdx < this.adiInvestors.size(); adiIdx++) {
				AuthorisedDepositTakingInstitution adi = this.adiInvestors.get(adiIdx);
				int index = adi.getPaymentClearingIndex();
				double monthlyInterest = this.adiInvestorAmounts.get(adiIdx) * this.rateBondsNotesBorrowings
						/ NUMBER_MONTHS;
			}
		}

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
	public double getPnlInterestIncome() {
		return pnlInterestIncome;
	}

	/**
	 * @param pnlInterestIncome the pnlInterestIncome to set
	 */
	public void setPnlInterestIncome(double pnlInterestIncome) {
		this.pnlInterestIncome = pnlInterestIncome;
	}

	/**
	 * @return the pnlInterestExpense
	 */
	public double getPnlInterestExpense() {
		return pnlInterestExpense;
	}

	/**
	 * @param pnlInterestExpense the pnlInterestExpense to set
	 */
	public void setPnlInterestExpense(double pnlInterestExpense) {
		this.pnlInterestExpense = pnlInterestExpense;
	}

	/**
	 * @return the pnlTradingIncome
	 */
	public double getPnlTradingIncome() {
		return pnlTradingIncome;
	}

	/**
	 * @param pnlTradingIncome the pnlTradingIncome to set
	 */
	public void setPnlTradingIncome(double pnlTradingIncome) {
		this.pnlTradingIncome = pnlTradingIncome;
	}

	/**
	 * @return the pnlInvestmentIncome
	 */
	public double getPnlInvestmentIncome() {
		return pnlInvestmentIncome;
	}

	/**
	 * @param pnlInvestmentIncome the pnlInvestmentIncome to set
	 */
	public void setPnlInvestmentIncome(double pnlInvestmentIncome) {
		this.pnlInvestmentIncome = pnlInvestmentIncome;
	}

	/**
	 * @return the pnlOtherIncome
	 */
	public double getPnlOtherIncome() {
		return pnlOtherIncome;
	}

	/**
	 * @param pnlOtherIncome the pnlOtherIncome to set
	 */
	public void setPnlOtherIncome(double pnlOtherIncome) {
		this.pnlOtherIncome = pnlOtherIncome;
	}

	/**
	 * @return the pnlPersonnelExpenses
	 */
	public double getPnlPersonnelExpenses() {
		return pnlPersonnelExpenses;
	}

	/**
	 * @param pnlPersonnelExpenses the pnlPersonnelExpenses to set
	 */
	public void setPnlPersonnelExpenses(double pnlPersonnelExpenses) {
		this.pnlPersonnelExpenses = pnlPersonnelExpenses;
	}

	/**
	 * @return the pnlLoanImpairmentExpense
	 */
	public double getPnlLoanImpairmentExpense() {
		return pnlLoanImpairmentExpense;
	}

	/**
	 * @param pnlLoanImpairmentExpense the pnlLoanImpairmentExpense to set
	 */
	public void setPnlLoanImpairmentExpense(double pnlLoanImpairmentExpense) {
		this.pnlLoanImpairmentExpense = pnlLoanImpairmentExpense;
	}

	/**
	 * @return the pnlDepreciationAmortisation
	 */
	public double getPnlDepreciationAmortisation() {
		return pnlDepreciationAmortisation;
	}

	/**
	 * @param pnlDepreciationAmortisation the pnlDepreciationAmortisation to set
	 */
	public void setPnlDepreciationAmortisation(double pnlDepreciationAmortisation) {
		this.pnlDepreciationAmortisation = pnlDepreciationAmortisation;
	}

	/**
	 * @return the pnlOtherExpenses
	 */
	public double getPnlOtherExpenses() {
		return pnlOtherExpenses;
	}

	/**
	 * @param pnlOtherExpenses the pnlOtherExpenses to set
	 */
	public void setPnlOtherExpenses(double pnlOtherExpenses) {
		this.pnlOtherExpenses = pnlOtherExpenses;
	}

	/**
	 * @return the pnlIncomeTaxExpense
	 */
	public double getPnlIncomeTaxExpense() {
		return pnlIncomeTaxExpense;
	}

	/**
	 * @param pnlIncomeTaxExpense the pnlIncomeTaxExpense to set
	 */
	public void setPnlIncomeTaxExpense(double pnlIncomeTaxExpense) {
		this.pnlIncomeTaxExpense = pnlIncomeTaxExpense;
	}

	/**
	 * @return the bsCash
	 */
	public double getBsCash() {
		return bsCash;
	}

	/**
	 * @param bsCash the bsCash to set
	 */
	public void setBsCash(double bsCash) {
		this.bsCash = bsCash;
	}

	/**
	 * @return the bsTradingSecurities
	 */
	public double getBsTradingSecurities() {
		return bsTradingSecurities;
	}

	/**
	 * @param bsTradingSecurities the bsTradingSecurities to set
	 */
	public void setBsTradingSecurities(double bsTradingSecurities) {
		this.bsTradingSecurities = bsTradingSecurities;
	}

	/**
	 * @return the bsDerivativeAssets
	 */
	public double getBsDerivativeAssets() {
		return bsDerivativeAssets;
	}

	/**
	 * @param bsDerivativeAssets the bsDerivativeAssets to set
	 */
	public void setBsDerivativeAssets(double bsDerivativeAssets) {
		this.bsDerivativeAssets = bsDerivativeAssets;
	}

	/**
	 * @return the bsInvestments
	 */
	public double getBsInvestments() {
		return bsInvestments;
	}

	/**
	 * @param bsInvestments the bsInvestments to set
	 */
	public void setBsInvestments(double bsInvestments) {
		this.bsInvestments = bsInvestments;
	}

	/**
	 * @return the bsLoansPersonal
	 */
	public double getBsLoansPersonal() {
		return bsLoansPersonal;
	}

	/**
	 * @param bsLoansPersonal the bsLoansPersonal to set
	 */
	public void setBsLoansPersonal(double bsLoansPersonal) {
		this.bsLoansPersonal = bsLoansPersonal;
	}

	/**
	 * @return the bsLoansHome
	 */
	public double getBsLoansHome() {
		return bsLoansHome;
	}

	/**
	 * @param bsLoansHome the bsLoansHome to set
	 */
	public void setBsLoansHome(double bsLoansHome) {
		this.bsLoansHome = bsLoansHome;
	}

	/**
	 * @return the bsLoansBusiness
	 */
	public double getBsLoansBusiness() {
		return bsLoansBusiness;
	}

	/**
	 * @param bsLoansBusiness the bsLoansBusiness to set
	 */
	public void setBsLoansBusiness(double bsLoansBusiness) {
		this.bsLoansBusiness = bsLoansBusiness;
	}

	/**
	 * @return the bsLoansADI
	 */
	public double getBsLoansADI() {
		return bsLoansADI;
	}

	/**
	 * @param bsLoansADI the bsLoansADI to set
	 */
	public void setBsLoansADI(double bsLoansADI) {
		this.bsLoansADI = bsLoansADI;
	}

	/**
	 * @return the bsLoansGovernment
	 */
	public double getBsLoansGovernment() {
		return bsLoansGovernment;
	}

	/**
	 * @param bsLoansGovernment the bsLoansGovernment to set
	 */
	public void setBsLoansGovernment(double bsLoansGovernment) {
		this.bsLoansGovernment = bsLoansGovernment;
	}

	/**
	 * @return the bsOtherNonFinancialAssets
	 */
	public double getBsOtherNonFinancialAssets() {
		return bsOtherNonFinancialAssets;
	}

	/**
	 * @param bsOtherNonFinancialAssets the bsOtherNonFinancialAssets to set
	 */
	public void setBsOtherNonFinancialAssets(double bsOtherNonFinancialAssets) {
		this.bsOtherNonFinancialAssets = bsOtherNonFinancialAssets;
	}

	/**
	 * @return the bsDepositsAtCall
	 */
	public double getBsDepositsAtCall() {
		return bsDepositsAtCall;
	}

	/**
	 * @param bsDepositsAtCall the bsDepositsAtCall to set
	 */
	public void setBsDepositsAtCall(double bsDepositsAtCall) {
		this.bsDepositsAtCall = bsDepositsAtCall;
	}

	/**
	 * @return the bsDepositsTerm
	 */
	public double getBsDepositsTerm() {
		return bsDepositsTerm;
	}

	/**
	 * @param bsDepositsTerm the bsDepositsTerm to set
	 */
	public void setBsDepositsTerm(double bsDepositsTerm) {
		this.bsDepositsTerm = bsDepositsTerm;
	}

	/**
	 * @return the bsDepositsAdiRepoEligible
	 */
	public double getBsDepositsAdiRepoEligible() {
		return bsDepositsAdiRepoEligible;
	}

	/**
	 * @param bsDepositsAdiRepoEligible the bsDepositsAdiRepoEligible to set
	 */
	public void setBsDepositsAdiRepoEligible(double bsDepositsAdiRepoEligible) {
		this.bsDepositsAdiRepoEligible = bsDepositsAdiRepoEligible;
	}

	/**
	 * @return the bsDerivativeLiabilities
	 */
	public double getBsDerivativeLiabilities() {
		return bsDerivativeLiabilities;
	}

	/**
	 * @param bsDerivativeLiabilities the bsDerivativeLiabilities to set
	 */
	public void setBsDerivativeLiabilities(double bsDerivativeLiabilities) {
		this.bsDerivativeLiabilities = bsDerivativeLiabilities;
	}

	/**
	 * @return the bsBondsNotesBorrowings
	 */
	public double getBsBondsNotesBorrowings() {
		return bsBondsNotesBorrowings;
	}

	/**
	 * @param bsBondsNotesBorrowings the bsBondsNotesBorrowings to set
	 */
	public void setBsBondsNotesBorrowings(double bsBondsNotesBorrowings) {
		this.bsBondsNotesBorrowings = bsBondsNotesBorrowings;
	}

	/**
	 * @return the bsOtherLiabilities
	 */
	public double getBsOtherLiabilities() {
		return bsOtherLiabilities;
	}

	/**
	 * @param bsOtherLiabilities the bsOtherLiabilities to set
	 */
	public void setBsOtherLiabilities(double bsOtherLiabilities) {
		this.bsOtherLiabilities = bsOtherLiabilities;
	}

	/**
	 * @return the bsRetainedEarnings
	 */
	public double getBsRetainedEarnings() {
		return bsRetainedEarnings;
	}

	/**
	 * @param bsRetainedEarnings the bsRetainedEarnings to set
	 */
	public void setBsRetainedEarnings(double bsRetainedEarnings) {
		this.bsRetainedEarnings = bsRetainedEarnings;
	}

	/**
	 * @return the bsReserves
	 */
	public double getBsReserves() {
		return bsReserves;
	}

	/**
	 * @param bsReserves the bsReserves to set
	 */
	public void setBsReserves(double bsReserves) {
		this.bsReserves = bsReserves;
	}

	/**
	 * @return the bsOtherEquity
	 */
	public double getBsOtherEquity() {
		return bsOtherEquity;
	}

	/**
	 * @param bsOtherEquity the bsOtherEquity to set
	 */
	public void setBsOtherEquity(double bsOtherEquity) {
		this.bsOtherEquity = bsOtherEquity;
	}

	/**
	 * @return the rateCash
	 */
	public double getRateCash() {
		return rateCash;
	}

	/**
	 * @param rateCash the rateCash to set
	 */
	public void setRateCash(double rateCash) {
		this.rateCash = rateCash;
	}

	/**
	 * @return the rateTrading
	 */
	public double getRateTrading() {
		return rateTrading;
	}

	/**
	 * @param rateTrading the rateTrading to set
	 */
	public void setRateTrading(double rateTrading) {
		this.rateTrading = rateTrading;
	}

	/**
	 * @return the rateInvestment
	 */
	public double getRateInvestment() {
		return rateInvestment;
	}

	/**
	 * @param rateInvestment the rateInvestment to set
	 */
	public void setRateInvestment(double rateInvestment) {
		this.rateInvestment = rateInvestment;
	}

	/**
	 * @return the rateAdiLoan
	 */
	public double getRateAdiLoan() {
		return rateAdiLoan;
	}

	/**
	 * @param rateAdiLoan the rateAdiLoan to set
	 */
	public void setRateAdiLoan(double rateAdiLoan) {
		this.rateAdiLoan = rateAdiLoan;
	}

	/**
	 * @return the rateGovernmentLoan
	 */
	public double getRateGovernmentLoan() {
		return rateGovernmentLoan;
	}

	/**
	 * @param rateGovernmentLoan the rateGovernmentLoan to set
	 */
	public void setRateGovernmentLoan(double rateGovernmentLoan) {
		this.rateGovernmentLoan = rateGovernmentLoan;
	}

	/**
	 * @return the rateTotalLoans
	 */
	public double getRateTotalLoans() {
		return rateTotalLoans;
	}

	/**
	 * @param rateTotalLoans the rateTotalLoans to set
	 */
	public void setRateTotalLoans(double rateTotalLoans) {
		this.rateTotalLoans = rateTotalLoans;
	}

	/**
	 * @return the rateTotalDeposits
	 */
	public double getRateTotalDeposits() {
		return rateTotalDeposits;
	}

	/**
	 * @param rateTotalDeposits the rateTotalDeposits to set
	 */
	public void setRateTotalDeposits(double rateTotalDeposits) {
		this.rateTotalDeposits = rateTotalDeposits;
	}

	/**
	 * @return the rateBondsNotesBorrowings
	 */
	public double getRateBondsNotesBorrowings() {
		return rateBondsNotesBorrowings;
	}

	/**
	 * @param rateBondsNotesBorrowings the rateBondsNotesBorrowings to set
	 */
	public void setRateBondsNotesBorrowings(double rateBondsNotesBorrowings) {
		this.rateBondsNotesBorrowings = rateBondsNotesBorrowings;
	}

	/**
	 * @return the capitalTotalRatio
	 */
	public double getCapitalTotalRatio() {
		return capitalTotalRatio;
	}

	/**
	 * @param capitalTotalRatio the capitalTotalRatio to set
	 */
	public void setCapitalTotalRatio(double capitalTotalRatio) {
		this.capitalTotalRatio = capitalTotalRatio;
	}

	/**
	 * @return the capitalTotalAmount
	 */
	public double getCapitalTotalAmount() {
		return capitalTotalAmount;
	}

	/**
	 * @param capitalTotalAmount the capitalTotalAmount to set
	 */
	public void setCapitalTotalAmount(double capitalTotalAmount) {
		this.capitalTotalAmount = capitalTotalAmount;
	}

	/**
	 * @return the capitalTotalRWA
	 */
	public double getCapitalTotalRWA() {
		return capitalTotalRWA;
	}

	/**
	 * @param capitalTotalRWA the capitalTotalRWA to set
	 */
	public void setCapitalTotalRWA(double capitalTotalRWA) {
		this.capitalTotalRWA = capitalTotalRWA;
	}

	/**
	 * @return the capitalCreditRWA
	 */
	public double getCapitalCreditRWA() {
		return capitalCreditRWA;
	}

	/**
	 * @param capitalCreditRWA the capitalCreditRWA to set
	 */
	public void setCapitalCreditRWA(double capitalCreditRWA) {
		this.capitalCreditRWA = capitalCreditRWA;
	}

}
