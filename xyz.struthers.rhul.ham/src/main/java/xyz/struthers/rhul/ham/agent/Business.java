/**
 * 
 */
package xyz.struthers.rhul.ham.agent;

import java.util.Map;

/**
 * There are approximately 2.25 million businesses (including exporters) in the
 * model. Each instance uses 193 bytes of RAM, so they will consume
 * approximately 414 MB of RAM.
 * 
 * @author Adam Struthers
 * @since 02-Feb-2019
 */
public class Business extends Agent {

	private static final long serialVersionUID = 1L;

	public static final double COMPANY_TAX_RATE = 0.30d;

	// Business Details (25 bytes)
	protected char industryDivisionCode;
	protected String industrySubdivisionCode; // 2 chars
	protected String industryGroupCode; // 3 chars
	protected String industryClassCode; // 4 chars
	protected String industryCode; // 5 chars (used by ATO)

	protected String state; // 2 or 3 chars
	protected String lgaCode; // 5 chars

	protected char size; // S = small, M = medium, L = large
	protected boolean isExporter;

	protected Map<Individual, Double> employeeWages;

	// P&L (88 bytes)
	protected double salesDomestic;
	protected double salesGovernment;
	protected double salesForeign;
	protected double interestIncome;
	protected double rentIncome;
	protected double otherIncome;

	protected double personnelExpenses; // (wages, super, w/comp, FBT, payroll tax)
	protected double foreignExpenses;
	protected double interestExpense;
	protected double rentExpense;
	protected double depreciationExpense;
	protected double otherExpenses;

	// Bal Sht (56 bytes)
	protected double bankDeposits;
	protected double foreignEquities;
	protected double otherFinancialAssets;
	protected double otherNonFinancialAssets;

	protected double loans;
	protected double otherLiabilities;

	protected double totalEquity;

	// Interest rates (16 bytes)
	protected double interestRateLoans;
	protected double interestRateDeposits;

	/**
	 * Default constructor
	 */
	public Business() {
		super();
		this.init();
	}

	/**
	 * Copy constructor
	 * 
	 * @param business
	 */
	public Business(Business business) {
		super(business);
		this.init();

		// Business Details
		this.industryDivisionCode = business.industryDivisionCode;
		this.industrySubdivisionCode = business.industrySubdivisionCode;
		this.industryGroupCode = business.industryGroupCode;
		this.industryClassCode = business.industryClassCode;
		this.industryCode = business.industryCode;

		this.state = business.state;
		this.lgaCode = business.lgaCode;

		this.size = business.size;
		this.isExporter = business.isExporter;

		// N.B. Don't copy the employeeWages because they're unique to each business.

		// P&L
		this.salesDomestic = business.salesDomestic;
		this.salesGovernment = business.salesGovernment;
		this.salesForeign = business.salesForeign;
		this.interestIncome = business.interestIncome;
		this.rentIncome = business.rentIncome;
		this.otherIncome = business.otherIncome;

		this.personnelExpenses = business.personnelExpenses;
		this.foreignExpenses = business.foreignExpenses;
		this.interestExpense = business.interestExpense;
		this.rentExpense = business.rentExpense;
		this.otherExpenses = business.otherExpenses;

		// Bal Sht
		this.bankDeposits = business.bankDeposits;
		this.foreignEquities = business.foreignEquities;
		this.otherFinancialAssets = business.otherFinancialAssets;
		this.otherNonFinancialAssets = business.otherNonFinancialAssets;

		this.loans = business.loans;
		this.otherLiabilities = business.otherLiabilities;

		this.totalEquity = business.totalEquity;

		// Interest rates
		this.interestRateLoans = business.interestRateLoans;
		this.interestRateDeposits = business.interestRateDeposits;
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
		this.industryDivisionCode = '\0'; // unicode zero
		this.industrySubdivisionCode = null;
		this.industryGroupCode = null;
		this.industryClassCode = null;
		this.industryCode = null;

		this.state = null;
		this.lgaCode = null;

		this.size = '\0';
		this.isExporter = false;

		this.employeeWages = null;

		// P&L
		this.salesDomestic = 0d;
		this.salesGovernment = 0d;
		this.salesForeign = 0d;
		this.interestIncome = 0d;
		this.rentIncome = 0d;
		this.otherIncome = 0d;

		this.personnelExpenses = 0d;
		this.foreignExpenses = 0d;
		this.interestExpense = 0d;
		this.rentExpense = 0d;
		this.otherExpenses = 0d;

		// Bal Sht
		this.bankDeposits = 0d;
		this.foreignEquities = 0d;
		this.otherFinancialAssets = 0d;
		this.otherNonFinancialAssets = 0d;

		this.loans = 0d;
		this.otherLiabilities = 0d;

		this.totalEquity = 0d;

		// Interest Rates
		this.interestRateLoans = 0d;
		this.interestRateDeposits = 0d;
	}

	public double getTotalIncome() {
		return this.salesDomestic + this.salesGovernment + this.interestIncome + this.otherIncome;
	}

	public double getTotalExpenses() {
		return this.personnelExpenses + this.rentExpense + this.interestExpense + this.foreignExpenses
				+ this.otherExpenses;
	}

	public double getGrossProfit() {
		return this.getTotalIncome() - this.getTotalExpenses();
	}

	public double getTax() {
		return this.getGrossProfit() * Business.COMPANY_TAX_RATE;
	}

	public double getNetProfit() {
		return this.getGrossProfit() - this.getTax();
	}

	// FIXME: implement getters & setters once the fields stop changing

}
