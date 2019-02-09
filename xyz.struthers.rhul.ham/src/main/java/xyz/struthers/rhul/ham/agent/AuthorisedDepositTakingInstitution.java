/**
 * 
 */
package xyz.struthers.rhul.ham.agent;

import java.util.Map;

/**
 * Each instance of this class stores 41 doubles and 4 strings, so will consume
 * approximately 388 bytes of RAM (assuming 60 chars total in the strings).
 * There are 86 instances of this class in the model, so they will consume
 * approximately 32.59 kB of RAM.
 * 
 * @author Adam Struthers
 * @since 20-Dec-2018
 */
public abstract class AuthorisedDepositTakingInstitution extends Agent {

	private static final long serialVersionUID = 1L;

	// Company details (approx. 60 chars)
	protected String australianBusinessNumber;
	protected String shortName;
	protected String adiCategory;

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

}
