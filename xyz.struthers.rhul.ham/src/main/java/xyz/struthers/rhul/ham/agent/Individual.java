/**
 * 
 */
package xyz.struthers.rhul.ham.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
	private Business employer; // can be null if not employed

	// "Personal" Details (8 bytes + approx. 7 bytes of Strings = 15 bytes)
	private int age;
	private String sex; // "M" / "F"
	private String employmentIndustry; // "A" to "S", can be null if not employed
	private String localGovernmentAreaCode; // 5 chars
	private int mainIncomeSource;

	// P&L (96 bytes)
	private double pnlWagesSalaries;
	private double pnlUnemploymentBenefits;
	private double pnlOtherSocialSecurityIncome;
	private double pnlInvestmentIncome; // other income (including superannuation & dividends)
	private double pnlInterestIncome;
	private double pnlRentIncome; // income from investment properties
	private double pnlForeignIncome;
	private double pnlOtherIncome;
	private double pnlIncomeTaxExpense;

	private double pnlWorkRelatedExpenses;
	private double pnlRentInterestExpense; // assume interest-only loan
	private double pnlDonations;

	// Bal Sht (24 bytes)
	private double bsBankDeposits;
	// private double bsLoans;
	private double bsStudentLoans; // HELP debt

	// Interest rates (24 bytes)
	protected double interestRateDeposits;
	protected double interestRateLoans;
	protected double interestRateStudentLoans; // in Australia this is always CPI (by law)

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

	public double getGrossIncome() {
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
		// TODO Auto-generated method stub
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
		this.pnlWagesSalaries = 0d;
		this.pnlUnemploymentBenefits = 0d;
		this.pnlOtherSocialSecurityIncome = 0d;
		this.pnlInvestmentIncome = 0d;
		this.pnlInterestIncome = 0d;
		this.pnlRentIncome = 0d;
		this.pnlForeignIncome = 0d;
		this.pnlOtherIncome = 0d;
		this.pnlIncomeTaxExpense = 0d;

		this.pnlWorkRelatedExpenses = 0d;
		this.pnlRentInterestExpense = 0d;
		this.pnlDonations = 0d;

		// Bal Sht
		this.bsBankDeposits = 0d;
		this.bsStudentLoans = 0d;
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
	public Business getEmployer() {
		return employer;
	}

	/**
	 * @param employer the employer to set
	 */
	public void setEmployer(Business employer) {
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
