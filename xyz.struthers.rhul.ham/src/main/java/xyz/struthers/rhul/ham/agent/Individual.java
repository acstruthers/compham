/**
 * 
 */
package xyz.struthers.rhul.ham.agent;

import java.util.Map;

/**
 * Each instance of this class uses about 192 bytes of RAM. There are
 * approximately 25 million instances of this class in the model, so they will
 * consume approximately 4.6 GB of RAM.
 * 
 * @author Adam Struthers
 * @since 25-Jan-2019
 */
public final class Individual extends Agent {

	private static final long serialVersionUID = 1L;

	// Object relationships (4 pointers = 32 bytes)
	private Household household;
	private Business employer; // can be null if not employed
	private AuthorisedDepositTakingInstitution depositAdi; // can be null if no deposits (e.g. children)
	private AuthorisedDepositTakingInstitution loanAdi; // can be null if no loan

	// "Personal" Details (4 bytes + approx. 20 bytes of Strings = 24 bytes)
	private int age;
	private String employmentIndustry; // can be null if not employed
	private String localGovernmentAreaCode; // 5 chars

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

	// Interest rates (16 bytes)
	protected double interestRateLoans;
	protected double interestRateDeposits;

	/**
	 * Default constructor
	 */
	public Individual() {
		super();
		this.init();
	}
	
	/**
	 * Copy constructor
	 */
	public Individual(Individual individual) {
		super();
		this.init();
		
		// TODO: implement copy constructor for Individual
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
	
	protected void init() {
		// Demographic
		this.age = 0;

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

	/**
	 * @return the household
	 */
	public Household getHousehold() {
		return household;
	}

	/**
	 * @param household
	 *            the household to set
	 */
	public void setHousehold(Household household) {
		this.household = household;
	}

	/**
	 * @return the employer
	 */
	public Business getEmployer() {
		return employer;
	}

	/**
	 * @param employer
	 *            the employer to set
	 */
	public void setEmployer(Business employer) {
		this.employer = employer;
	}

	/**
	 * @return the depositAdi
	 */
	public AuthorisedDepositTakingInstitution getDepositAdi() {
		return depositAdi;
	}

	/**
	 * @param depositAdi
	 *            the depositAdi to set
	 */
	public void setDepositAdi(AuthorisedDepositTakingInstitution depositAdi) {
		this.depositAdi = depositAdi;
	}

	/**
	 * @return the loanAdi
	 */
	public AuthorisedDepositTakingInstitution getLoanAdi() {
		return loanAdi;
	}

	/**
	 * @param loanAdi
	 *            the loanAdi to set
	 */
	public void setLoanAdi(AuthorisedDepositTakingInstitution loanAdi) {
		this.loanAdi = loanAdi;
	}

	/**
	 * @return the age
	 */
	public int getAge() {
		return age;
	}

	/**
	 * @param age
	 *            the age to set
	 */
	public void setAge(int age) {
		this.age = age;
	}

	/**
	 * @return the employmentIndustry
	 */
	public String getEmploymentIndustry() {
		return employmentIndustry;
	}

	/**
	 * @param employmentIndustry
	 *            the employmentIndustry to set
	 */
	public void setEmploymentIndustry(String employmentIndustry) {
		this.employmentIndustry = employmentIndustry;
	}

	/**
	 * @return the localGovernmentAreaCode
	 */
	public String getLocalGovernmentAreaCode() {
		return localGovernmentAreaCode;
	}

	/**
	 * @param localGovernmentAreaCode
	 *            the localGovernmentAreaCode to set
	 */
	public void setLocalGovernmentAreaCode(String localGovernmentAreaCode) {
		this.localGovernmentAreaCode = localGovernmentAreaCode;
	}

	/**
	 * @return the pnlWagesSalaries
	 */
	public double getPnlWagesSalaries() {
		return pnlWagesSalaries;
	}

	/**
	 * @param pnlWagesSalaries
	 *            the pnlWagesSalaries to set
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
	 * @param pnlUnemploymentBenefits
	 *            the pnlUnemploymentBenefits to set
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
	 * @param pnlOtherSocialSecurityIncome
	 *            the pnlOtherSocialSecurityIncome to set
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
	 * @param pnlInvestmentIncome
	 *            the pnlInvestmentIncome to set
	 */
	public void setPnlInvestmentIncome(double pnlInvestmentIncome) {
		this.pnlInvestmentIncome = pnlInvestmentIncome;
	}

	/**
	 * @return the pnlIncomeTaxExpense
	 */
	public double getPnlIncomeTaxExpense() {
		return pnlIncomeTaxExpense;
	}

	/**
	 * @param pnlIncomeTaxExpense
	 *            the pnlIncomeTaxExpense to set
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
	 * @param pnlLivingExpenses
	 *            the pnlLivingExpenses to set
	 */
	public void setPnlLivingExpenses(double pnlLivingExpenses) {
		this.pnlLivingExpenses = pnlLivingExpenses;
	}

	/**
	 * @return the pnlRent
	 */
	public double getPnlRent() {
		return pnlRent;
	}

	/**
	 * @param pnlRent
	 *            the pnlRent to set
	 */
	public void setPnlRent(double pnlRent) {
		this.pnlRent = pnlRent;
	}

	/**
	 * @return the pnlMortgageRepayments
	 */
	public double getPnlMortgageRepayments() {
		return pnlMortgageRepayments;
	}

	/**
	 * @param pnlMortgageRepayments
	 *            the pnlMortgageRepayments to set
	 */
	public void setPnlMortgageRepayments(double pnlMortgageRepayments) {
		this.pnlMortgageRepayments = pnlMortgageRepayments;
	}

	/**
	 * @return the pnlOtherDiscretionaryExpenses
	 */
	public double getPnlOtherDiscretionaryExpenses() {
		return pnlOtherDiscretionaryExpenses;
	}

	/**
	 * @param pnlOtherDiscretionaryExpenses
	 *            the pnlOtherDiscretionaryExpenses to set
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
	 * @param bsBankDeposits
	 *            the bsBankDeposits to set
	 */
	public void setBsBankDeposits(double bsBankDeposits) {
		this.bsBankDeposits = bsBankDeposits;
	}

	/**
	 * @return the bsOtherFinancialAssets
	 */
	public double getBsOtherFinancialAssets() {
		return bsOtherFinancialAssets;
	}

	/**
	 * @param bsOtherFinancialAssets
	 *            the bsOtherFinancialAssets to set
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
	 * @param bsResidentialLandAndDwellings
	 *            the bsResidentialLandAndDwellings to set
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
	 * @param bsOtherNonFinancialAssets
	 *            the bsOtherNonFinancialAssets to set
	 */
	public void setBsOtherNonFinancialAssets(double bsOtherNonFinancialAssets) {
		this.bsOtherNonFinancialAssets = bsOtherNonFinancialAssets;
	}

	/**
	 * @return the bsLoans
	 */
	public double getBsLoans() {
		return bsLoans;
	}

	/**
	 * @param bsLoans
	 *            the bsLoans to set
	 */
	public void setBsLoans(double bsLoans) {
		this.bsLoans = bsLoans;
	}

	/**
	 * @return the bsOtherLiabilities
	 */
	public double getBsOtherLiabilities() {
		return bsOtherLiabilities;
	}

	/**
	 * @param bsOtherLiabilities
	 *            the bsOtherLiabilities to set
	 */
	public void setBsOtherLiabilities(double bsOtherLiabilities) {
		this.bsOtherLiabilities = bsOtherLiabilities;
	}

	/**
	 * @return the interestRateLoans
	 */
	public double getInterestRateLoans() {
		return interestRateLoans;
	}

	/**
	 * @param interestRateLoans
	 *            the interestRateLoans to set
	 */
	public void setInterestRateLoans(double interestRateLoans) {
		this.interestRateLoans = interestRateLoans;
	}

	/**
	 * @return the interestRateDeposits
	 */
	public double getInterestRateDeposits() {
		return interestRateDeposits;
	}

	/**
	 * @param interestRateDeposits
	 *            the interestRateDeposits to set
	 */
	public void setInterestRateDeposits(double interestRateDeposits) {
		this.interestRateDeposits = interestRateDeposits;
	}

}
