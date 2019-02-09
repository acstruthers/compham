/**
 * 
 */
package xyz.struthers.rhul.ham.agent;

import java.util.Map;

/**
 * There are approximately 2.25 million businesses (including exporters) in the
 * model. Each instance uses 172 bytes of RAM, so they will consume
 * approximately 361 MB of RAM.
 * 
 * @author Adam Struthers
 * @since 02-Feb-2019
 */
public class Business extends Agent {

	private static final long serialVersionUID = 1L;

	public static final double COMPANY_TAX_RATE = 0.30d;

	// Business Details (20 bytes)
	protected char industryDivisionCode;
	protected String industrySubdivisionCode; // 2 chars
	protected String industryGroupCode; // 3 chars
	protected String industryClassCode; // 4 chars

	protected String state; // 2 or 3 chars
	protected String lgaCode; // 5 chars

	protected char size; // S = small, M = medium, L = large
	protected boolean isExporter;

	protected Map<Individual, Double> employeeWages;
	
	// P&L (72 bytes)
	protected double salesDomestic;
	protected double salesGovernment;
	protected double interestIncome;
	protected double otherIncome;

	protected double personnelExpenses;
	protected double rentExpense;
	protected double interestExpense;
	protected double foreignExpenses;
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

		this.industryDivisionCode = business.industryDivisionCode;
		this.industrySubdivisionCode = business.industrySubdivisionCode;
		this.industryGroupCode = business.industryGroupCode;
		this.industryClassCode = business.industryClassCode;

		this.state = business.state;
		this.lgaCode = business.lgaCode;

		this.size = business.size;
		this.isExporter = business.isExporter;
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

		this.state = null;
		this.lgaCode = null;

		this.size = '\0';
		this.isExporter = false;
		
		this.employeeWages = null;
		
		// P&L
		this.salesDomestic = 0d;
		this.salesGovernment = 0d;
		this.interestIncome = 0d;
		this.otherIncome = 0d;
		
		this.personnelExpenses = 0d;
		this.rentExpense = 0d;
		this.interestExpense = 0d;
		this.foreignExpenses = 0d;
		this.otherExpenses = 0d;
		
		this.bankDeposits = 0d;
		this.foreignEquities = 0d;
		this.otherFinancialAssets = 0d;
		this.otherNonFinancialAssets = 0d;
		
		this.loans = 0d;
		this.otherLiabilities = 0d;
		
		this.totalEquity = 0d;
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

}
