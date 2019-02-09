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
	private double pnlInvestmentIncome; // other income
	private double pnlIncomeTaxExpense;

	private double pnlLivingExpenses;
	private double pnlRent;
	private double pnlMortgageRepayments;
	private double pnlOtherDiscretionaryExpenses;

	// Bal Sht (48 bytes)
	private double bsBankDeposits;
	private double bsOtherFinancialAssets;
	private double bsResidentialLandAndDwellings;
	private double bsOtherNonFinancialAssets;

	private double bsLoans;
	private double bsOtherLiabilities;

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

			this.pnlIncomeTaxExpense += i.getPnlIncomeTaxExpense();
			this.pnlLivingExpenses += i.getPnlLivingExpenses();
			this.pnlRent += i.getPnlRent();
			this.pnlMortgageRepayments += i.getPnlMortgageRepayments();
			this.pnlOtherDiscretionaryExpenses += i.getPnlOtherDiscretionaryExpenses();

			// Bal Sht
			this.bsBankDeposits += i.getBsBankDeposits();
			this.bsOtherFinancialAssets += i.getBsOtherFinancialAssets();
			this.bsResidentialLandAndDwellings += i.getBsResidentialLandAndDwellings();
			this.bsOtherNonFinancialAssets += i.getBsOtherNonFinancialAssets();

			this.bsLoans += i.getBsLoans();
			this.bsOtherLiabilities += i.getBsOtherLiabilities();
		}
	}

	public double getGrossIncome() {
		return this.pnlWagesSalaries + this.pnlUnemploymentBenefits + this.pnlOtherSocialSecurityIncome
				+ this.pnlInvestmentIncome;
	}

	public double getNetIncome() {
		return this.getGrossIncome() + this.pnlIncomeTaxExpense;
	}

	public double getTotalExpenses() {
		return this.pnlLivingExpenses + this.pnlRent + this.pnlMortgageRepayments + this.pnlOtherDiscretionaryExpenses;
	}

	public double getNetProfit() {
		return this.getNetIncome() + this.getTotalExpenses();
	}

	public double getTotalFinancialAssets() {
		return this.bsBankDeposits + this.bsOtherFinancialAssets;
	}

	public double getTotalNonFinancialAssets() {
		return this.getTotalAssets() - this.getTotalFinancialAssets();
	}

	public double getTotalAssets() {
		return this.bsBankDeposits + this.bsOtherFinancialAssets + this.bsResidentialLandAndDwellings
				+ this.bsOtherNonFinancialAssets;
	}

	public double getTotalLiabilities() {
		return this.bsLoans + this.bsOtherLiabilities;
	}

	public double getEquity() {
		return this.getTotalAssets() + this.getTotalLiabilities();
	}

	protected void init() {
		this.individuals = null;

		// P&L
		this.pnlWagesSalaries = 0d;
		this.pnlUnemploymentBenefits = 0d;
		this.pnlOtherSocialSecurityIncome = 0d;
		this.pnlInvestmentIncome = 0d;
		this.pnlIncomeTaxExpense = 0d;

		this.pnlLivingExpenses = 0d;
		this.pnlRent = 0d;
		this.pnlMortgageRepayments = 0d;
		this.pnlOtherDiscretionaryExpenses = 0d;

		// Bal Sht
		this.bsBankDeposits = 0d;
		this.bsOtherFinancialAssets = 0d;
		this.bsResidentialLandAndDwellings = 0d;
		this.bsOtherNonFinancialAssets = 0d;

		this.bsLoans = 0d;
		this.bsOtherLiabilities = 0d;
	}
}
