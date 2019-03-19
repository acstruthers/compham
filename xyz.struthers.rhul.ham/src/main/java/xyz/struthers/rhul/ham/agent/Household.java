/**
 * 
 */
package xyz.struthers.rhul.ham.agent;

import java.util.ArrayList;
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
	private Individual[] individuals; // get employers from individuals
	private int numAdults;
	private int numChildren;

	private AuthorisedDepositTakingInstitution depositAdi; // can be null if no deposits (e.g. children)
	private AuthorisedDepositTakingInstitution loanAdi; // can be null if no loan
	private ArrayList<Business> suppliers; // household spending goes to these per ABS 6530.0

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

	private double pnlLivingExpenses; // Henderson poverty line (excl. housing costs)
	private double pnlRentExpense;
	private double pnlMortgageRepayments;
	private double pnlWorkRelatedExpenses;
	private double pnlRentInterestExpense; // assume interest-only loan
	private double pnlDonations;
	private double pnlOtherDiscretionaryExpenses; // TODO: review this ... it might be unnecessary

	// Bal Sht (48 bytes)
	private double bsBankDeposits;
	private double bsSuperannuation;
	private double bsEquities;
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
		this.initialiseFinancialsFromIndividuals();
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
	 * Sets the household's financials, based on the financials of the Individuals
	 * that it comprises of.
	 * 
	 * Does not set living expenses because the Henderson Poverty Line has already
	 * been calculated and set when the Household was instantiated.
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

		// TODO: calculate Henderson, Bal Sht ratios, etc.
	}

	public double getGrossIncome() {
		return this.pnlWagesSalaries + this.pnlUnemploymentBenefits + this.pnlOtherSocialSecurityIncome
				+ this.pnlInvestmentIncome + this.pnlInterestIncome + this.pnlRentIncome + this.pnlForeignIncome
				+ this.pnlOtherIncome;
	}

	protected void init() {
		this.individuals = null;
		this.numAdults = 0;
		this.numChildren = 0;

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
		return numAdults;
	}

	/**
	 * @param numAdults the numAdults to set
	 */
	public void setNumAdults(int numAdults) {
		this.numAdults = numAdults;
	}

	/**
	 * @return the numChildren
	 */
	public int getNumChildren() {
		return numChildren;
	}

	/**
	 * @param numChildren the numChildren to set
	 */
	public void setNumChildren(int numChildren) {
		this.numChildren = numChildren;
	}

	/**
	 * @return the pnlWagesSalaries
	 */
	public double getPnlWagesSalaries() {
		return pnlWagesSalaries;
	}

	/**
	 * @param pnlWagesSalaries the pnlWagesSalaries to set
	 */
	public void setPnlWagesSalaries(double pnlWagesSalaries) {
		this.pnlWagesSalaries = pnlWagesSalaries;
	}

	/**
	 * @return the pnlUnemploymentBenefits
	 */
	public double getPnlUnemploymentBenefits() {
		return pnlUnemploymentBenefits;
	}

	/**
	 * @param pnlUnemploymentBenefits the pnlUnemploymentBenefits to set
	 */
	public void setPnlUnemploymentBenefits(double pnlUnemploymentBenefits) {
		this.pnlUnemploymentBenefits = pnlUnemploymentBenefits;
	}

	/**
	 * @return the pnlOtherSocialSecurityIncome
	 */
	public double getPnlOtherSocialSecurityIncome() {
		return pnlOtherSocialSecurityIncome;
	}

	/**
	 * @param pnlOtherSocialSecurityIncome the pnlOtherSocialSecurityIncome to set
	 */
	public void setPnlOtherSocialSecurityIncome(double pnlOtherSocialSecurityIncome) {
		this.pnlOtherSocialSecurityIncome = pnlOtherSocialSecurityIncome;
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
	 * @return the pnlRentIncome
	 */
	public double getPnlRentIncome() {
		return pnlRentIncome;
	}

	/**
	 * @param pnlRentIncome the pnlRentIncome to set
	 */
	public void setPnlRentIncome(double pnlRentIncome) {
		this.pnlRentIncome = pnlRentIncome;
	}

	/**
	 * @return the pnlForeignIncome
	 */
	public double getPnlForeignIncome() {
		return pnlForeignIncome;
	}

	/**
	 * @param pnlForeignIncome the pnlForeignIncome to set
	 */
	public void setPnlForeignIncome(double pnlForeignIncome) {
		this.pnlForeignIncome = pnlForeignIncome;
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
	 * @return the pnlLivingExpenses
	 */
	public double getPnlLivingExpenses() {
		return pnlLivingExpenses;
	}

	/**
	 * @param pnlLivingExpenses the pnlLivingExpenses to set
	 */
	public void setPnlLivingExpenses(double pnlLivingExpenses) {
		this.pnlLivingExpenses = pnlLivingExpenses;
	}

	/**
	 * @return the pnlRentExpense
	 */
	public double getPnlRentExpense() {
		return pnlRentExpense;
	}

	/**
	 * @param pnlRentExpense the pnlRentExpense to set
	 */
	public void setPnlRentExpense(double pnlRentExpense) {
		this.pnlRentExpense = pnlRentExpense;
	}

	/**
	 * @return the pnlMortgageRepayments
	 */
	public double getPnlMortgageRepayments() {
		return pnlMortgageRepayments;
	}

	/**
	 * @param pnlMortgageRepayments the pnlMortgageRepayments to set
	 */
	public void setPnlMortgageRepayments(double pnlMortgageRepayments) {
		this.pnlMortgageRepayments = pnlMortgageRepayments;
	}

	/**
	 * @return the pnlWorkRelatedExpenses
	 */
	public double getPnlWorkRelatedExpenses() {
		return pnlWorkRelatedExpenses;
	}

	/**
	 * @param pnlWorkRelatedExpenses the pnlWorkRelatedExpenses to set
	 */
	public void setPnlWorkRelatedExpenses(double pnlWorkRelatedExpenses) {
		this.pnlWorkRelatedExpenses = pnlWorkRelatedExpenses;
	}

	/**
	 * @return the pnlRentInterestExpense
	 */
	public double getPnlRentInterestExpense() {
		return pnlRentInterestExpense;
	}

	/**
	 * @param pnlRentInterestExpense the pnlRentInterestExpense to set
	 */
	public void setPnlRentInterestExpense(double pnlRentInterestExpense) {
		this.pnlRentInterestExpense = pnlRentInterestExpense;
	}

	/**
	 * @return the pnlDonations
	 */
	public double getPnlDonations() {
		return pnlDonations;
	}

	/**
	 * @param pnlDonations the pnlDonations to set
	 */
	public void setPnlDonations(double pnlDonations) {
		this.pnlDonations = pnlDonations;
	}

	/**
	 * @return the pnlOtherDiscretionaryExpenses
	 */
	public double getPnlOtherDiscretionaryExpenses() {
		return pnlOtherDiscretionaryExpenses;
	}

	/**
	 * @param pnlOtherDiscretionaryExpenses the pnlOtherDiscretionaryExpenses to set
	 */
	public void setPnlOtherDiscretionaryExpenses(double pnlOtherDiscretionaryExpenses) {
		this.pnlOtherDiscretionaryExpenses = pnlOtherDiscretionaryExpenses;
	}

	/**
	 * @return the bsBankDeposits
	 */
	public double getBsBankDeposits() {
		return bsBankDeposits;
	}

	/**
	 * @param bsBankDeposits the bsBankDeposits to set
	 */
	public void setBsBankDeposits(double bsBankDeposits) {
		this.bsBankDeposits = bsBankDeposits;
	}

	/**
	 * @return the bsSuperannuation
	 */
	public double getBsSuperannuation() {
		return bsSuperannuation;
	}

	/**
	 * @param bsSuperannuation the bsSuperannuation to set
	 */
	public void setBsSuperannuation(double bsSuperannuation) {
		this.bsSuperannuation = bsSuperannuation;
	}

	/**
	 * @return the bsEquities
	 */
	public double getBsEquities() {
		return bsEquities;
	}

	/**
	 * @param bsEquities the bsEquities to set
	 */
	public void setBsEquities(double bsEquities) {
		this.bsEquities = bsEquities;
	}

	/**
	 * @return the bsOtherFinancialAssets
	 */
	public double getBsOtherFinancialAssets() {
		return bsOtherFinancialAssets;
	}

	/**
	 * @param bsOtherFinancialAssets the bsOtherFinancialAssets to set
	 */
	public void setBsOtherFinancialAssets(double bsOtherFinancialAssets) {
		this.bsOtherFinancialAssets = bsOtherFinancialAssets;
	}

	/**
	 * @return the bsResidentialLandAndDwellings
	 */
	public double getBsResidentialLandAndDwellings() {
		return bsResidentialLandAndDwellings;
	}

	/**
	 * @param bsResidentialLandAndDwellings the bsResidentialLandAndDwellings to set
	 */
	public void setBsResidentialLandAndDwellings(double bsResidentialLandAndDwellings) {
		this.bsResidentialLandAndDwellings = bsResidentialLandAndDwellings;
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
	 * @return the bsTotalAssets
	 */
	public double getBsTotalAssets() {
		return bsTotalAssets;
	}

	/**
	 * @param bsTotalAssets the bsTotalAssets to set
	 */
	public void setBsTotalAssets(double bsTotalAssets) {
		this.bsTotalAssets = bsTotalAssets;
	}

	/**
	 * @return the bsLoans
	 */
	public double getBsLoans() {
		return bsLoans;
	}

	/**
	 * @param bsLoans the bsLoans to set
	 */
	public void setBsLoans(double bsLoans) {
		this.bsLoans = bsLoans;
	}

	/**
	 * @return the bsStudentLoans
	 */
	public double getBsStudentLoans() {
		return bsStudentLoans;
	}

	/**
	 * @param bsStudentLoans the bsStudentLoans to set
	 */
	public void setBsStudentLoans(double bsStudentLoans) {
		this.bsStudentLoans = bsStudentLoans;
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
	 * @return the bsTotalLiabilities
	 */
	public double getBsTotalLiabilities() {
		return bsTotalLiabilities;
	}

	/**
	 * @param bsTotalLiabilities the bsTotalLiabilities to set
	 */
	public void setBsTotalLiabilities(double bsTotalLiabilities) {
		this.bsTotalLiabilities = bsTotalLiabilities;
	}

	/**
	 * @return the bsNetWorth
	 */
	public double getBsNetWorth() {
		return bsNetWorth;
	}

	/**
	 * @param bsNetWorth the bsNetWorth to set
	 */
	public void setBsNetWorth(double bsNetWorth) {
		this.bsNetWorth = bsNetWorth;
	}

	/**
	 * @return the interestRateDeposits
	 */
	public double getInterestRateDeposits() {
		return interestRateDeposits;
	}

	/**
	 * @param interestRateDeposits the interestRateDeposits to set
	 */
	public void setInterestRateDeposits(double interestRateDeposits) {
		this.interestRateDeposits = interestRateDeposits;
	}

	/**
	 * @return the interestRateLoans
	 */
	public double getInterestRateLoans() {
		return interestRateLoans;
	}

	/**
	 * @param interestRateLoans the interestRateLoans to set
	 */
	public void setInterestRateLoans(double interestRateLoans) {
		this.interestRateLoans = interestRateLoans;
	}

	/**
	 * @return the interestRateStudentLoans
	 */
	public double getInterestRateStudentLoans() {
		return interestRateStudentLoans;
	}

	/**
	 * @param interestRateStudentLoans the interestRateStudentLoans to set
	 */
	public void setInterestRateStudentLoans(double interestRateStudentLoans) {
		this.interestRateStudentLoans = interestRateStudentLoans;
	}

}
