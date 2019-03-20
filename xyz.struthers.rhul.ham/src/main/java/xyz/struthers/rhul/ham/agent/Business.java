/**
 * 
 */
package xyz.struthers.rhul.ham.agent;

import java.util.ArrayList;
import java.util.List;

import xyz.struthers.rhul.ham.config.Properties;
import xyz.struthers.rhul.ham.process.Employer;
import xyz.struthers.rhul.ham.process.NodePayment;
import xyz.struthers.rhul.ham.process.Tax;

/**
 * There are approximately 2.25 million businesses (including exporters) in the
 * model. Each instance uses 193 bytes of RAM, so they will consume
 * approximately 414 MB of RAM.
 * 
 * @author Adam Struthers
 * @since 02-Feb-2019
 */
public class Business extends Agent implements Employer {

	private static final long serialVersionUID = 1L;

	/**
	 * Identifies agents that were calibrated using the same industry / size / state
	 * data. They will later differ based on their linkages to other agents in the
	 * economy (e.g. employees and banks).
	 */
	protected int businessTypeId;

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

	// agent relationships
	protected int paymentClearingIndex;
	protected int employeeCountTarget;
	protected ArrayList<Individual> employees; // calculate wages & super
	protected AuthorisedDepositTakingInstitution adi; // loans & deposits
	protected ArrayList<Business> domesticSuppliers;
	protected ArrayList<Double> supplierRatios;
	protected ArrayList<ForeignCountry> foreignSuppliers;
	protected ArrayList<Double> foreignSupplierRatios;
	protected Business landlord;
	protected AustralianGovernment govt;

	// P&L (88 bytes)
	protected double totalIncome;
	protected double salesDomestic;
	protected double salesGovernment;
	protected double salesForeign;
	protected double interestIncome;
	protected double rentIncome;
	protected double otherIncome; // balancing item

	protected double totalExpenses;
	protected double wageExpenses; // wages, super, payroll tax ... ignore w/comp & FBT
	protected double superannuationExpense; // 9.5%
	protected double payrollTaxExpense; // calculated according to each state's rules
	protected double foreignExpenses;
	protected double interestExpense;
	protected double rentExpense;
	protected double depreciationExpense;
	protected double otherExpenses; // balancing item

	// Bal Sht (56 bytes)
	protected double totalAssets;
	protected double bankDeposits;
	protected double foreignEquities;
	protected double otherFinancialAssets;
	protected double otherNonFinancialAssets; // balancing item

	protected double totalLiabilities;
	protected double tradeCreditors;
	protected double loans;
	protected double otherCurrentLiabilities;
	protected double otherNonCurrentLiabilities; // balancing item

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

		this.employeeCountTarget = business.employeeCountTarget;
		// N.B. Don't copy the employeeWages because they're unique to each business.

		// P&L
		this.totalIncome = business.totalIncome;
		this.salesDomestic = business.salesDomestic;
		this.salesGovernment = business.salesGovernment;
		this.salesForeign = business.salesForeign;
		this.interestIncome = business.interestIncome;
		this.rentIncome = business.rentIncome;
		this.otherIncome = business.otherIncome;

		this.totalExpenses = business.totalExpenses;
		this.wageExpenses = business.wageExpenses;
		this.superannuationExpense = business.superannuationExpense;
		this.payrollTaxExpense = business.payrollTaxExpense;
		this.foreignExpenses = business.foreignExpenses;
		this.interestExpense = business.interestExpense;
		this.rentExpense = business.rentExpense;
		this.depreciationExpense = business.depreciationExpense;
		this.otherExpenses = business.otherExpenses;

		// Bal Sht
		this.totalAssets = business.totalAssets;
		this.bankDeposits = business.bankDeposits;
		this.foreignEquities = business.foreignEquities;
		this.otherFinancialAssets = business.otherFinancialAssets;
		this.otherNonFinancialAssets = business.otherNonFinancialAssets;

		this.totalLiabilities = business.totalLiabilities;
		this.tradeCreditors = business.tradeCreditors;
		this.loans = business.loans;
		this.otherCurrentLiabilities = business.otherCurrentLiabilities;
		this.otherNonCurrentLiabilities = business.otherNonCurrentLiabilities;

		this.totalEquity = business.totalEquity;

		// Interest rates
		this.interestRateLoans = business.interestRateLoans;
		this.interestRateDeposits = business.interestRateDeposits;
	}

	@Override
	public List<Individual> getEmployees() {
		return this.employees;
	}

	@Override
	public void addEmployee(Individual employee) {
		if (this.employees == null) {
			this.employees = new ArrayList<Individual>(1);
		}
		this.employees.add(employee);
		this.employees.trimToSize();

		// FIXME: re-calc wage exp, super exp, payroll tax (and total exp)
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
		if (this.employees != null && this.wageExpenses > 0d) {
			numberOfCreditors += this.employees.size();
		}
		if (this.domesticSuppliers != null && this.otherExpenses > 0d) {
			numberOfCreditors += this.domesticSuppliers.size();
		}
		if (this.foreignSuppliers != null && this.foreignExpenses > 0d) {
			numberOfCreditors += this.foreignSuppliers.size();
		}
		if (this.landlord != null && this.rentExpense > 0d) {
			numberOfCreditors++;
		}
		if (this.adi != null && this.interestExpense > 0d) {
			numberOfCreditors++;
		}
		ArrayList<NodePayment> liabilities = new ArrayList<NodePayment>(numberOfCreditors);

		// calculate wages due to employees (incl. superannuation)
		if (this.employees != null && this.wageExpenses > 0d) {
			for (Individual employee : this.employees) {
				int index = employee.getPaymentClearingIndex();
				double monthlyWagesIncludingSuper = employee.getPnlWagesSalaries()
						* (1d + Properties.SUPERANNUATION_RATE);
				liabilities.add(new NodePayment(index, monthlyWagesIncludingSuper));
			}
		}

		// calculate amounts due to domestic suppliers
		if (this.domesticSuppliers != null && this.otherExpenses > 0d) {
			for (Business supplier : this.domesticSuppliers) {
				int index = supplier.getPaymentClearingIndex();
				double expense = this.otherExpenses / this.domesticSuppliers.size();
				liabilities.add(new NodePayment(index, expense));
			}
		}

		// calculate amounts due to foreign suppliers
		if (this.foreignSuppliers != null && this.foreignExpenses > 0d) {
			for (ForeignCountry supplier : this.foreignSuppliers) {
				int index = supplier.getPaymentClearingIndex();
				double expense = this.foreignExpenses / this.domesticSuppliers.size();
				liabilities.add(new NodePayment(index, expense));
			}
		}

		// calculate rent due to landlord
		if (this.landlord != null && this.rentExpense > 0d) {
			liabilities.add(new NodePayment(this.landlord.getPaymentClearingIndex(), this.rentExpense));
		}

		// calculate tax due to government (payroll & income)
		double totalTax = this.payrollTaxExpense
				+ Tax.calculateCompanyTax(this.totalIncome, this.totalIncome - this.totalExpenses);
		liabilities.add(new NodePayment(govt.getPaymentClearingIndex(), totalTax));

		// calculate interest due to bank
		if (this.adi != null && this.interestExpense > 0d) {
			liabilities.add(new NodePayment(this.adi.getPaymentClearingIndex(), this.interestExpense));
		}

		liabilities.trimToSize();
		return liabilities;
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

		this.employeeCountTarget = 0;
		this.employees = null;

		// P&L
		this.totalIncome = 0d;
		this.salesDomestic = 0d;
		this.salesGovernment = 0d;
		this.salesForeign = 0d;
		this.interestIncome = 0d;
		this.rentIncome = 0d;
		this.otherIncome = 0d;

		this.totalExpenses = 0d;
		this.wageExpenses = 0d;
		this.superannuationExpense = 0d;
		this.payrollTaxExpense = 0d;
		this.foreignExpenses = 0d;
		this.interestExpense = 0d;
		this.rentExpense = 0d;
		this.depreciationExpense = 0d;
		this.otherExpenses = 0d;

		// Bal Sht
		this.totalAssets = 0d;
		this.bankDeposits = 0d;
		this.foreignEquities = 0d;
		this.otherFinancialAssets = 0d;
		this.otherNonFinancialAssets = 0d;

		this.totalLiabilities = 0d;
		this.tradeCreditors = 0d;
		this.loans = 0d;
		this.otherCurrentLiabilities = 0d;
		this.otherNonCurrentLiabilities = 0d;

		this.totalEquity = 0d;

		// Interest Rates
		this.interestRateLoans = 0d;
		this.interestRateDeposits = 0d;
	}

	public double getGrossProfit() {
		return this.getTotalIncome() - this.getTotalExpenses();
	}

	/**
	 * Calculates tax rates per ATO company tax rates, taking into account the lower
	 * rate for small businesses.
	 * 
	 * @return the tax expense
	 */
	public double getTax() {
		return this.getGrossProfit() * Tax.calculateCompanyTax(this.totalIncome, this.totalIncome - this.totalExpenses);
	}

	public double getNetProfit() {
		return this.getGrossProfit() - this.getTax();
	}

	/**
	 * @return the businessTypeId
	 */
	public int getBusinessTypeId() {
		return businessTypeId;
	}

	/**
	 * @param businessTypeId the businessTypeId to set
	 */
	public void setBusinessTypeId(int businessTypeId) {
		this.businessTypeId = businessTypeId;
	}

	/**
	 * @return the industryDivisionCode
	 */
	public char getIndustryDivisionCode() {
		return industryDivisionCode;
	}

	/**
	 * @param industryDivisionCode the industryDivisionCode to set
	 */
	public void setIndustryDivisionCode(char industryDivisionCode) {
		this.industryDivisionCode = industryDivisionCode;
	}

	/**
	 * @return the industrySubdivisionCode
	 */
	public String getIndustrySubdivisionCode() {
		return industrySubdivisionCode;
	}

	/**
	 * @param industrySubdivisionCode the industrySubdivisionCode to set
	 */
	@SuppressWarnings("unused")
	private void setIndustrySubdivisionCode(String industrySubdivisionCode) {
		this.industrySubdivisionCode = industrySubdivisionCode;
	}

	/**
	 * @return the industryGroupCode
	 */
	public String getIndustryGroupCode() {
		return industryGroupCode;
	}

	/**
	 * @param industryGroupCode the industryGroupCode to set
	 */
	@SuppressWarnings("unused")
	private void setIndustryGroupCode(String industryGroupCode) {
		this.industryGroupCode = industryGroupCode;
	}

	/**
	 * @return the industryClassCode
	 */
	public String getIndustryClassCode() {
		return industryClassCode;
	}

	/**
	 * @param industryClassCode the industryClassCode to set
	 */
	@SuppressWarnings("unused")
	private void setIndustryClassCode(String industryClassCode) {
		this.industryClassCode = industryClassCode;
	}

	/**
	 * @return the industryCode
	 */
	public String getIndustryCode() {
		return industryCode;
	}

	/**
	 * @param industryCode the industryCode to set
	 */
	public void setIndustryCode(String industryCode) {
		this.industryCode = industryCode;
	}

	/**
	 * @return the state
	 */
	public String getState() {
		return state;
	}

	/**
	 * @param state the state to set
	 */
	public void setState(String state) {
		this.state = state;
	}

	/**
	 * @return the lgaCode
	 */
	public String getLgaCode() {
		return lgaCode;
	}

	/**
	 * @param lgaCode the lgaCode to set
	 */
	public void setLgaCode(String lgaCode) {
		this.lgaCode = lgaCode;
	}

	/**
	 * @return the size
	 */
	public char getSize() {
		return size;
	}

	/**
	 * @param size the size to set
	 */
	public void setSize(char size) {
		this.size = size;
	}

	/**
	 * @return the isExporter
	 */
	public boolean isExporter() {
		return isExporter;
	}

	/**
	 * @param isExporter the isExporter to set
	 */
	public void setExporter(boolean isExporter) {
		this.isExporter = isExporter;
	}

	/**
	 * @return the employeeCountTarget
	 */
	public int getEmployeeCountTarget() {
		return employeeCountTarget;
	}

	/**
	 * @param employeeCountTarget the employeeCountTarget to set
	 */
	public void setEmployeeCountTarget(int employeeCountTarget) {
		this.employeeCountTarget = employeeCountTarget;
	}

	/**
	 * @return the totalIncome
	 */
	public double getTotalIncome() {
		return totalIncome;
	}

	/**
	 * @param totalIncome the totalIncome to set
	 */
	public void setTotalIncome(double totalIncome) {
		this.totalIncome = totalIncome;
	}

	/**
	 * @return the salesDomestic
	 */
	public double getSalesDomestic() {
		return salesDomestic;
	}

	/**
	 * @param salesDomestic the salesDomestic to set
	 */
	public void setSalesDomestic(double salesDomestic) {
		this.salesDomestic = salesDomestic;
	}

	/**
	 * @return the salesGovernment
	 */
	public double getSalesGovernment() {
		return salesGovernment;
	}

	/**
	 * @param salesGovernment the salesGovernment to set
	 */
	public void setSalesGovernment(double salesGovernment) {
		this.salesGovernment = salesGovernment;
	}

	/**
	 * @return the salesForeign
	 */
	public double getSalesForeign() {
		return salesForeign;
	}

	/**
	 * @param salesForeign the salesForeign to set
	 */
	public void setSalesForeign(double salesForeign) {
		this.salesForeign = salesForeign;
	}

	/**
	 * @return the interestIncome
	 */
	public double getInterestIncome() {
		return interestIncome;
	}

	/**
	 * @param interestIncome the interestIncome to set
	 */
	public void setInterestIncome(double interestIncome) {
		this.interestIncome = interestIncome;
	}

	/**
	 * @return the rentIncome
	 */
	public double getRentIncome() {
		return rentIncome;
	}

	/**
	 * @param rentIncome the rentIncome to set
	 */
	public void setRentIncome(double rentIncome) {
		this.rentIncome = rentIncome;
	}

	/**
	 * @return the otherIncome
	 */
	public double getOtherIncome() {
		return otherIncome;
	}

	/**
	 * @return the totalExpenses
	 */
	public double getTotalExpenses() {
		return totalExpenses;
	}

	/**
	 * @param totalExpenses the totalExpenses to set
	 */
	public void setTotalExpenses(double totalExpenses) {
		this.totalExpenses = totalExpenses;
	}

	/**
	 * @return the personnelExpenses
	 */
	public double getWageExpenses() {
		return wageExpenses;
	}

	/**
	 * @param personnelExpenses the personnelExpenses to set
	 */
	public void setWageExpenses(double personnelExpenses) {
		this.wageExpenses = personnelExpenses;
	}

	/**
	 * @return the superannuationExpense
	 */
	public double getSuperannuationExpense() {
		return superannuationExpense;
	}

	/**
	 * @param superannuationExpense the superannuationExpense to set
	 */
	public void setSuperannuationExpense(double superannuationExpense) {
		this.superannuationExpense = superannuationExpense;
	}

	/**
	 * @return the payrollTaxExpense
	 */
	public double getPayrollTaxExpense() {
		return payrollTaxExpense;
	}

	/**
	 * @param payrollTaxExpense the payrollTaxExpense to set
	 */
	public void setPayrollTaxExpense(double payrollTaxExpense) {
		this.payrollTaxExpense = payrollTaxExpense;
	}

	/**
	 * @return the foreignExpenses
	 */
	public double getForeignExpenses() {
		return foreignExpenses;
	}

	/**
	 * @param foreignExpenses the foreignExpenses to set
	 */
	public void setForeignExpenses(double foreignExpenses) {
		this.foreignExpenses = foreignExpenses;
	}

	/**
	 * @return the interestExpense
	 */
	public double getInterestExpense() {
		return interestExpense;
	}

	/**
	 * @param interestExpense the interestExpense to set
	 */
	public void setInterestExpense(double interestExpense) {
		this.interestExpense = interestExpense;
	}

	/**
	 * @return the rentExpense
	 */
	public double getRentExpense() {
		return rentExpense;
	}

	/**
	 * @param rentExpense the rentExpense to set
	 */
	public void setRentExpense(double rentExpense) {
		this.rentExpense = rentExpense;
	}

	/**
	 * @return the depreciationExpense
	 */
	public double getDepreciationExpense() {
		return depreciationExpense;
	}

	/**
	 * @param depreciationExpense the depreciationExpense to set
	 */
	public void setDepreciationExpense(double depreciationExpense) {
		this.depreciationExpense = depreciationExpense;
	}

	/**
	 * @return the otherExpenses
	 */
	public double getOtherExpenses() {
		return otherExpenses;
	}

	/**
	 * @return the totalAssets
	 */
	public double getTotalAssets() {
		return totalAssets;
	}

	/**
	 * @param totalAssets the totalAssets to set
	 */
	public void setTotalAssets(double totalAssets) {
		this.totalAssets = totalAssets;
	}

	/**
	 * @return the bankDeposits
	 */
	public double getBankDeposits() {
		return bankDeposits;
	}

	/**
	 * @param bankDeposits the bankDeposits to set
	 */
	public void setBankDeposits(double bankDeposits) {
		this.bankDeposits = bankDeposits;
	}

	/**
	 * @return the foreignEquities
	 */
	public double getForeignEquities() {
		return foreignEquities;
	}

	/**
	 * @param foreignEquities the foreignEquities to set
	 */
	public void setForeignEquities(double foreignEquities) {
		this.foreignEquities = foreignEquities;
	}

	/**
	 * @return the otherFinancialAssets
	 */
	public double getOtherFinancialAssets() {
		return otherFinancialAssets;
	}

	/**
	 * @param otherFinancialAssets the otherFinancialAssets to set
	 */
	public void setOtherFinancialAssets(double otherFinancialAssets) {
		this.otherFinancialAssets = otherFinancialAssets;
	}

	/**
	 * @return the otherNonFinancialAssets
	 */
	public double getOtherNonFinancialAssets() {
		return otherNonFinancialAssets;
	}

	/**
	 * @return the totalLiabilities
	 */
	public double getTotalLiabilities() {
		return totalLiabilities;
	}

	/**
	 * @param totalLiabilities the totalLiabilities to set
	 */
	public void setTotalLiabilities(double totalLiabilities) {
		this.totalLiabilities = totalLiabilities;
	}

	/**
	 * @return the tradeCreditors
	 */
	public double getTradeCreditors() {
		return tradeCreditors;
	}

	/**
	 * @param tradeCreditors the tradeCreditors to set
	 */
	public void setTradeCreditors(double tradeCreditors) {
		this.tradeCreditors = tradeCreditors;
	}

	/**
	 * @return the loans
	 */
	public double getLoans() {
		return loans;
	}

	/**
	 * @param loans the loans to set
	 */
	public void setLoans(double loans) {
		this.loans = loans;
	}

	/**
	 * @return the otherCurrentLiabilities
	 */
	public double getOtherCurrentLiabilities() {
		return otherCurrentLiabilities;
	}

	/**
	 * @param otherCurrentLiabilities the otherCurrentLiabilities to set
	 */
	public void setOtherCurrentLiabilities(double otherCurrentLiabilities) {
		this.otherCurrentLiabilities = otherCurrentLiabilities;
	}

	/**
	 * @return the otherLiabilities
	 */
	public double getOtherNonCurrentLiabilities() {
		return otherNonCurrentLiabilities;
	}

	/**
	 * @return the totalEquity
	 */
	public double getTotalEquity() {
		return totalEquity;
	}

	/**
	 * @param totalEquity the totalEquity to set
	 */
	public void setTotalEquity(double totalEquity) {
		this.totalEquity = totalEquity;
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

}
