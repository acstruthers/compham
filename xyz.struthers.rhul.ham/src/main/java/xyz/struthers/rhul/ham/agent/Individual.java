/**
 * 
 */
package xyz.struthers.rhul.ham.agent;

import java.util.List;

import xyz.struthers.rhul.ham.process.Employer;
import xyz.struthers.rhul.ham.process.NodePayment;

/**
 * Each instance of this class uses about 191 bytes of RAM. There are
 * approximately 25 million instances of this class in the model, so they will
 * consume approximately 4.6 GB of RAM.
 * 
 * @author Adam Struthers
 * @since 25-Jan-2019
 */
public final class Individual extends Agent {

	private static final long serialVersionUID = 1L;

	// Agent relationships (3 pointers = 24 bytes)
	protected int paymentClearingIndex;
	private Household household;
	private Employer employer; // can be null if not employed

	// "Personal" Details (8 bytes + approx. 7 bytes of Strings = 15 bytes)
	private int age;
	private String sex; // "M" / "F"
	private String employmentIndustry; // "A" to "S", can be null if not employed
	private String localGovernmentAreaCode; // 5 chars
	private int mainIncomeSource;

	// P&L (96 bytes)
	private float pnlWagesSalaries;
	private float pnlUnemploymentBenefits;
	private float pnlOtherSocialSecurityIncome;
	private float pnlInvestmentIncome; // other income (including superannuation & dividends)
	private float pnlInterestIncome;
	private float pnlRentIncome; // income from investment properties
	private float pnlForeignIncome;
	private float pnlOtherIncome;
	private float pnlIncomeTaxExpense;

	private float pnlWorkRelatedExpenses;
	private float pnlRentInterestExpense; // assume interest-only loan
	private float pnlDonations;

	// Bal Sht (24 bytes)
	private float bsBankDeposits;
	// private float bsLoans;
	private float bsStudentLoans; // HELP debt

	// Interest rates (24 bytes)
	protected float interestRateDeposits;
	protected float interestRateLoans;
	protected float interestRateStudentLoans; // in Australia this is always CPI (by law)

	/**
	 * Default constructor
	 */
	public Individual() {
		super();
		this.init();
	}

	/**
	 * Copy constructor (used in Household calibration).
	 * 
	 * WARNING: Does not copy links to other agents.
	 * 
	 * @param individual - the Individual to copy
	 */
	public Individual(Individual individual) {
		super();
		this.age = individual.age;
		this.sex = individual.sex;
		this.employmentIndustry = individual.employmentIndustry;
		this.localGovernmentAreaCode = individual.localGovernmentAreaCode;
		this.mainIncomeSource = individual.mainIncomeSource;
		this.pnlWagesSalaries = individual.pnlWagesSalaries;
		this.pnlUnemploymentBenefits = individual.pnlUnemploymentBenefits;
		this.pnlOtherSocialSecurityIncome = individual.pnlOtherSocialSecurityIncome;
		this.pnlInvestmentIncome = individual.pnlInvestmentIncome;
		this.pnlInterestIncome = individual.pnlInterestIncome;
		this.pnlRentIncome = individual.pnlRentIncome;
		this.pnlForeignIncome = individual.pnlForeignIncome;
		this.pnlOtherIncome = individual.pnlOtherIncome;
		this.pnlIncomeTaxExpense = individual.pnlIncomeTaxExpense;
		this.pnlWorkRelatedExpenses = individual.pnlWorkRelatedExpenses;
		this.pnlRentInterestExpense = individual.pnlRentInterestExpense;
		this.pnlDonations = individual.pnlDonations;
		this.bsBankDeposits = individual.bsBankDeposits;
		this.bsStudentLoans = individual.bsStudentLoans;
		this.interestRateDeposits = individual.interestRateDeposits;
		this.interestRateLoans = individual.interestRateLoans;
		this.interestRateStudentLoans = individual.interestRateStudentLoans;
	}

	public float getGrossIncome() {
		return this.pnlWagesSalaries + this.pnlUnemploymentBenefits + this.pnlOtherSocialSecurityIncome
				+ this.pnlInvestmentIncome + this.pnlInterestIncome + this.pnlRentIncome + this.pnlForeignIncome
				+ this.pnlOtherIncome;
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
		/*
		 * Individuals don't participate directly in the Payments Clearing Vector
		 * algorithm - only through the Households they belong to.
		 */
		return null;
	}

	protected void init() {
		// agents
		this.household = null;
		this.employer = null;

		// Demographic
		this.age = 0;
		this.sex = null;
		this.employmentIndustry = null;
		this.localGovernmentAreaCode = null;
		this.mainIncomeSource = 0;

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

		this.pnlWorkRelatedExpenses = 0f;
		this.pnlRentInterestExpense = 0f;
		this.pnlDonations = 0f;

		// Bal Sht
		this.bsBankDeposits = 0f;
		this.bsStudentLoans = 0f;
	}

	/**
	 * @return the household
	 */
	public Household getHousehold() {
		return household;
	}

	/**
	 * @param household the household to set
	 */
	public void setHousehold(Household household) {
		this.household = household;
	}

	/**
	 * @return the employer
	 */
	public Employer getEmployer() {
		return employer;
	}

	/**
	 * @param employer the employer to set
	 */
	public void setEmployer(Employer employer) {
		this.employer = employer;
	}

	/**
	 * @return the age
	 */
	public int getAge() {
		return age;
	}

	/**
	 * @param age the age to set
	 */
	public void setAge(int age) {
		this.age = age;
	}

	/**
	 * @return the sex
	 */
	public String getSex() {
		return sex;
	}

	/**
	 * @param sex the sex to set
	 */
	public void setSex(String sex) {
		this.sex = sex;
	}

	/**
	 * @return the employmentIndustry
	 */
	public String getEmploymentIndustry() {
		return employmentIndustry;
	}

	/**
	 * @param employmentIndustry the employmentIndustry to set
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
	 * @param localGovernmentAreaCode the localGovernmentAreaCode to set
	 */
	public void setLocalGovernmentAreaCode(String localGovernmentAreaCode) {
		this.localGovernmentAreaCode = localGovernmentAreaCode;
	}

	/**
	 * @return the mainIncomeSource
	 */
	public int getMainIncomeSource() {
		return mainIncomeSource;
	}

	/**
	 * @param mainIncomeSource the mainIncomeSource to set
	 */
	public void setMainIncomeSource(int mainIncomeSource) {
		this.mainIncomeSource = mainIncomeSource;
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
		this.pnlIncomeTaxExpense = pnlIncomeTaxExpense;
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
	 * @return the interestRateDeposits
	 */
	public float getInterestRateDeposits() {
		return interestRateDeposits;
	}

	/**
	 * @param interestRateDeposits the interestRateDeposits to set
	 */
	public void setInterestRateDeposits(float interestRateDeposits) {
		this.interestRateDeposits = interestRateDeposits;
	}

	/**
	 * @return the interestRateLoans
	 */
	public float getInterestRateLoans() {
		return interestRateLoans;
	}

	/**
	 * @param interestRateLoans the interestRateLoans to set
	 */
	public void setInterestRateLoans(float interestRateLoans) {
		this.interestRateLoans = interestRateLoans;
	}

	/**
	 * @return the interestRateStudentLoans
	 */
	public float getInterestRateStudentLoans() {
		return interestRateStudentLoans;
	}

	/**
	 * @param interestRateStudentLoans the interestRateStudentLoans to set
	 */
	public void setInterestRateStudentLoans(float interestRateStudentLoans) {
		this.interestRateStudentLoans = interestRateStudentLoans;
	}
}
