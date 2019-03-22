/**
 * 
 */
package xyz.struthers.rhul.ham.agent;

import java.util.ArrayList;
import java.util.List;

import xyz.struthers.lang.CustomMath;
import xyz.struthers.rhul.ham.process.NodePayment;
import xyz.struthers.rhul.ham.process.Tax;

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

	// Agent relationships (approx. 28 bytes)
	protected int paymentClearingIndex;
	private Individual[] individuals; // get employers from individuals
	private int numAdults;
	private int numChildren;

	private AuthorisedDepositTakingInstitution depositAdi; // can be null if no deposits (e.g. children)
	private AuthorisedDepositTakingInstitution loanAdi; // can be null if no loan
	private ArrayList<Business> suppliers; // household spending goes to these per ABS 6530.0
	private ArrayList<Float> supplierRatios; // per ABS 6530.0
	private Household landlord;
	private AustralianGovernment govt;

	// P&L (72 bytes)
	private float pnlWagesSalaries;
	private float pnlUnemploymentBenefits;
	private float pnlOtherSocialSecurityIncome;
	private float pnlInvestmentIncome; // other income (including superannuation & dividends)
	private float pnlInterestIncome;
	private float pnlRentIncome; // income from investment properties
	private float pnlForeignIncome;
	private float pnlOtherIncome;
	private float pnlIncomeTaxExpense;

	private float pnlLivingExpenses; // Henderson poverty line (excl. housing costs)
	private float pnlRentExpense;
	private float pnlMortgageRepayments;
	private float pnlWorkRelatedExpenses;
	private float pnlRentInterestExpense; // assume interest-only loan
	private float pnlDonations;
	private float pnlOtherDiscretionaryExpenses; // TODO: review this ... it might be unnecessary

	// Bal Sht (48 bytes)
	private float bsBankDeposits;
	private float bsSuperannuation;
	private float bsEquities;
	private float bsOtherFinancialAssets;

	private float bsResidentialLandAndDwellings;
	private float bsOtherNonFinancialAssets;

	private float bsTotalAssets;

	private float bsLoans;
	private float bsStudentLoans; // HELP debt
	private float bsOtherLiabilities;
	private float bsTotalLiabilities;

	private float bsNetWorth;

	// Loan details
	private int loanCurrentMonth; // at calibration

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
		if (this.suppliers != null && this.pnlLivingExpenses > 0d) {
			numberOfCreditors += this.suppliers.size();
		}
		if (this.landlord != null && this.pnlRentExpense > 0d) {
			numberOfCreditors++;
		}
		if (this.loanAdi != null && this.bsLoans > 0d) {
			numberOfCreditors++;
		}
		ArrayList<NodePayment> liabilities = new ArrayList<NodePayment>(numberOfCreditors);

		// calculate amounts due to domestic suppliers
		if (this.suppliers != null && this.pnlLivingExpenses > 0d) {
			float totalExpense = this.pnlLivingExpenses + this.pnlWorkRelatedExpenses
					+ this.pnlOtherDiscretionaryExpenses;
			for (int supIdx = 0; supIdx < this.suppliers.size(); supIdx++) {
				int index = this.suppliers.get(supIdx).getPaymentClearingIndex();
				// split expenses per the ABS 6530.0 ratios
				float expense = totalExpense * this.supplierRatios.get(index);
				liabilities.add(new NodePayment(index, expense));
			}
		}

		// calculate rent due to landlord
		if (this.landlord != null && this.pnlRentExpense > 0d) {
			liabilities.add(new NodePayment(this.landlord.getPaymentClearingIndex(), this.pnlRentExpense));
		}

		// calculate loan repayment due to bank
		if (this.loanAdi != null && (this.pnlMortgageRepayments > 0d || this.pnlRentInterestExpense > 0d)) {
			liabilities.add(new NodePayment(this.loanAdi.getPaymentClearingIndex(),
					this.pnlMortgageRepayments + this.pnlRentInterestExpense));
		}

		// calculate income tax due to government
		float incomeTax = 0f;
		for (int indivIdx = 0; indivIdx < this.individuals.length; indivIdx++) {
			incomeTax += Tax.calculateIndividualIncomeTax(individuals[indivIdx].getGrossIncome());
		}
		liabilities.add(new NodePayment(this.govt.getPaymentClearingIndex(), incomeTax));

		liabilities.trimToSize();
		return null;
	}

	/**
	 * Calculates the interest component of the loan repayment due to bank. Assumes
	 * home loans are principal and interest, while investment property loans are
	 * interest-only.
	 * 
	 * @param iteration
	 * @return interest component of loan repayments
	 */
	public float getInterestDueToBank(int iteration) {
		float interest = 0f;
		if (this.loanAdi != null && (this.pnlMortgageRepayments > 0d || this.pnlRentInterestExpense > 0d)) {
			float loanBal = CustomMath.getCurrentLoanBalance(this.pnlMortgageRepayments,
					this.loanAdi.getLoanRate(iteration), this.bsResidentialLandAndDwellings,
					this.loanCurrentMonth + iteration);
			interest = loanBal * this.loanAdi.getLoanRate(iteration) + this.pnlRentInterestExpense;
		}
		return interest;
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

	public float getGrossIncome() {
		return this.pnlWagesSalaries + this.pnlUnemploymentBenefits + this.pnlOtherSocialSecurityIncome
				+ this.pnlInvestmentIncome + this.pnlInterestIncome + this.pnlRentIncome + this.pnlForeignIncome
				+ this.pnlOtherIncome;
	}

	protected void init() {
		this.individuals = null;
		this.numAdults = 0;
		this.numChildren = 0;

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

		this.pnlLivingExpenses = 0f;
		this.pnlRentExpense = 0f;
		this.pnlMortgageRepayments = 0f;
		this.pnlRentInterestExpense = 0f;
		this.pnlDonations = 0f;
		this.pnlOtherDiscretionaryExpenses = 0f;

		// Bal Sht
		this.bsBankDeposits = 0f;
		this.bsOtherFinancialAssets = 0f;
		this.bsResidentialLandAndDwellings = 0f;
		this.bsOtherNonFinancialAssets = 0f;
		this.bsTotalAssets = 0f;

		this.bsLoans = 0f;
		this.bsStudentLoans = 0f;
		this.bsOtherLiabilities = 0f;
		this.bsTotalLiabilities = 0f;

		this.bsNetWorth = 0f;
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
	 * @return the depositAdi
	 */
	public AuthorisedDepositTakingInstitution getDepositAdi() {
		return depositAdi;
	}

	/**
	 * @param depositAdi the depositAdi to set
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
	 * @param loanAdi the loanAdi to set
	 */
	public void setLoanAdi(AuthorisedDepositTakingInstitution loanAdi) {
		this.loanAdi = loanAdi;
	}

	/**
	 * @return the suppliers
	 */
	public ArrayList<Business> getSuppliers() {
		return suppliers;
	}

	/**
	 * @param suppliers the suppliers to set
	 */
	public void setSuppliers(ArrayList<Business> suppliers) {
		this.suppliers = suppliers;
	}

	/**
	 * @return the supplierRatios
	 */
	public ArrayList<Float> getSupplierRatios() {
		return supplierRatios;
	}

	/**
	 * @param supplierRatios the supplierRatios to set
	 */
	public void setSupplierRatios(ArrayList<Float> supplierRatios) {
		this.supplierRatios = supplierRatios;
	}

	/**
	 * @return the landlord
	 */
	public Household getLandlord() {
		return landlord;
	}

	/**
	 * @param landlord the landlord to set
	 */
	public void setLandlord(Household landlord) {
		this.landlord = landlord;
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
	 * @return the loanCurrentMonth
	 */
	public int getLoanCurrentMonth() {
		return loanCurrentMonth;
	}

	/**
	 * @param loanCurrentMonth the loanCurrentMonth to set
	 */
	public void setLoanCurrentMonth(int loanCurrentMonth) {
		this.loanCurrentMonth = loanCurrentMonth;
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
	 * @return the pnlLivingExpenses
	 */
	public float getPnlLivingExpenses() {
		return pnlLivingExpenses;
	}

	/**
	 * @param pnlLivingExpenses the pnlLivingExpenses to set
	 */
	public void setPnlLivingExpenses(float pnlLivingExpenses) {
		this.pnlLivingExpenses = pnlLivingExpenses;
	}

	/**
	 * @return the pnlRentExpense
	 */
	public float getPnlRentExpense() {
		return pnlRentExpense;
	}

	/**
	 * @param pnlRentExpense the pnlRentExpense to set
	 */
	public void setPnlRentExpense(float pnlRentExpense) {
		this.pnlRentExpense = pnlRentExpense;
	}

	/**
	 * @return the pnlMortgageRepayments
	 */
	public float getPnlMortgageRepayments() {
		return pnlMortgageRepayments;
	}

	/**
	 * @param pnlMortgageRepayments the pnlMortgageRepayments to set
	 */
	public void setPnlMortgageRepayments(float pnlMortgageRepayments) {
		this.pnlMortgageRepayments = pnlMortgageRepayments;
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
	 * @return the pnlOtherDiscretionaryExpenses
	 */
	public float getPnlOtherDiscretionaryExpenses() {
		return pnlOtherDiscretionaryExpenses;
	}

	/**
	 * @param pnlOtherDiscretionaryExpenses the pnlOtherDiscretionaryExpenses to set
	 */
	public void setPnlOtherDiscretionaryExpenses(float pnlOtherDiscretionaryExpenses) {
		this.pnlOtherDiscretionaryExpenses = pnlOtherDiscretionaryExpenses;
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
	 * @return the bsSuperannuation
	 */
	public float getBsSuperannuation() {
		return bsSuperannuation;
	}

	/**
	 * @param bsSuperannuation the bsSuperannuation to set
	 */
	public void setBsSuperannuation(float bsSuperannuation) {
		this.bsSuperannuation = bsSuperannuation;
	}

	/**
	 * @return the bsEquities
	 */
	public float getBsEquities() {
		return bsEquities;
	}

	/**
	 * @param bsEquities the bsEquities to set
	 */
	public void setBsEquities(float bsEquities) {
		this.bsEquities = bsEquities;
	}

	/**
	 * @return the bsOtherFinancialAssets
	 */
	public float getBsOtherFinancialAssets() {
		return bsOtherFinancialAssets;
	}

	/**
	 * @param bsOtherFinancialAssets the bsOtherFinancialAssets to set
	 */
	public void setBsOtherFinancialAssets(float bsOtherFinancialAssets) {
		this.bsOtherFinancialAssets = bsOtherFinancialAssets;
	}

	/**
	 * @return the bsResidentialLandAndDwellings
	 */
	public float getBsResidentialLandAndDwellings() {
		return bsResidentialLandAndDwellings;
	}

	/**
	 * @param bsResidentialLandAndDwellings the bsResidentialLandAndDwellings to set
	 */
	public void setBsResidentialLandAndDwellings(float bsResidentialLandAndDwellings) {
		this.bsResidentialLandAndDwellings = bsResidentialLandAndDwellings;
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
	 * @return the bsTotalAssets
	 */
	public float getBsTotalAssets() {
		return bsTotalAssets;
	}

	/**
	 * @param bsTotalAssets the bsTotalAssets to set
	 */
	public void setBsTotalAssets(float bsTotalAssets) {
		this.bsTotalAssets = bsTotalAssets;
	}

	/**
	 * @return the bsLoans
	 */
	public float getBsLoans() {
		return bsLoans;
	}

	/**
	 * @param bsLoans the bsLoans to set
	 */
	public void setBsLoans(float bsLoans) {
		this.bsLoans = bsLoans;
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
	 * @return the bsTotalLiabilities
	 */
	public float getBsTotalLiabilities() {
		return bsTotalLiabilities;
	}

	/**
	 * @param bsTotalLiabilities the bsTotalLiabilities to set
	 */
	public void setBsTotalLiabilities(float bsTotalLiabilities) {
		this.bsTotalLiabilities = bsTotalLiabilities;
	}

	/**
	 * @return the bsNetWorth
	 */
	public float getBsNetWorth() {
		return bsNetWorth;
	}

	/**
	 * @param bsNetWorth the bsNetWorth to set
	 */
	public void setBsNetWorth(float bsNetWorth) {
		this.bsNetWorth = bsNetWorth;
	}

}
