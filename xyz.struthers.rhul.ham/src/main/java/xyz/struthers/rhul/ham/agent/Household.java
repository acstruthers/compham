/**
 * 
 */
package xyz.struthers.rhul.ham.agent;

import java.util.Map;

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

	// Object relationships (approx. 28 bytes)
	private Individual[] individuals;

	// P&L (72 bytes)
	private double pnlWagesSalaries;
	private double pnlUnemploymentBenefits;
	private double pnlOtherSocialSecurityIncome;
	private double pnlInvestmentIncome; // other income (including superannuation & dividends)
	private double pnlInterestIncome;
	private double pnlRentIncome; // income from investment properties
	private double pnlForeignIncome;
	private double pnlOtherIncome;
	private double pnlIncomeTaxExpense;

	private double pnlLivingExpenses;
	private double pnlRentExpense;
	private double pnlMortgageRepayments;
	private double pnlRentInterestExpense; // assume interest-only loan
	private double pnlDonations;
	private double pnlOtherDiscretionaryExpenses;

	// Bal Sht (48 bytes)
	private double bsBankDeposits;
	private double bsOtherFinancialAssets;
	private double bsResidentialLandAndDwellings;
	private double bsOtherNonFinancialAssets;
	private double bsTotalAssets;

	private double bsLoans;
	private double bsStudentLoans; // HELP debt
	private double bsOtherLiabilities;
	private double bsTotalLiabilities;

	private double bsNetWorth;

	// Interest rates (16 bytes)
	protected double interestRateDeposits;
	protected double interestRateLoans;
	protected double interestRateStudentLoans; // in Australia this is always CPI (by law)

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
		this.initialiseFinancials();
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

	/**
	 * Sets the household's financials, based on the financials of the individuals
	 * that it comprises of. Adjusts for household composition when calculating the
	 * Henderson Poverty Line, which is being used as a proxy for non-discretionary
	 * living expenses.
	 */
	private void initialiseFinancials() {
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
			this.pnlLivingExpenses += i.getPnlLivingExpenses();
			this.pnlRentExpense += i.getPnlRentExpense();
			this.pnlMortgageRepayments += i.getPnlMortgageRepayments();
			this.pnlRentInterestExpense += i.getPnlRentInterestExpense();
			this.pnlDonations += i.getPnlDonations();
			this.pnlOtherDiscretionaryExpenses += i.getPnlOtherDiscretionaryExpenses();

			// Bal Sht
			this.bsBankDeposits += i.getBsBankDeposits();
			this.bsLoans += i.getBsLoans();
			this.bsStudentLoans += i.getBsStudentLoans();
		}

		// TODO: calculate Henderson, Bal Sht ratios, etc.
	}

	protected void init() {
		this.individuals = null;

		// P&L
		this.pnlWagesSalaries = 0d;
		this.pnlUnemploymentBenefits = 0d;
		this.pnlOtherSocialSecurityIncome = 0d;
		this.pnlInvestmentIncome = 0d;
		this.pnlInterestIncome = 0d;
		this.pnlRentIncome = 0d;
		this.pnlForeignIncome = 0d;
		this.pnlOtherIncome = 0d;
		this.pnlIncomeTaxExpense = 0d;

		this.pnlLivingExpenses = 0d;
		this.pnlRentExpense = 0d;
		this.pnlMortgageRepayments = 0d;
		this.pnlRentInterestExpense = 0d;
		this.pnlDonations = 0d;
		this.pnlOtherDiscretionaryExpenses = 0d;

		// Bal Sht
		this.bsBankDeposits = 0d;
		this.bsOtherFinancialAssets = 0d;
		this.bsResidentialLandAndDwellings = 0d;
		this.bsOtherNonFinancialAssets = 0d;
		this.bsTotalAssets = 0d;

		this.bsLoans = 0d;
		this.bsStudentLoans = 0d;
		this.bsOtherLiabilities = 0d;
		this.bsTotalLiabilities = 0d;

		this.bsNetWorth = 0d;

		// Interest rates
		this.interestRateDeposits = 0d;
		this.interestRateLoans = 0d;
		this.interestRateStudentLoans = 0d;
	}
}
