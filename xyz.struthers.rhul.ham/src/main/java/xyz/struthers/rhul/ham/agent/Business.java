/**
 * 
 */
package xyz.struthers.rhul.ham.agent;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import xyz.struthers.rhul.ham.config.Properties;
import xyz.struthers.rhul.ham.data.CalibrateEconomy;
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

	// exporter fields
	protected boolean isExporter;
	private ArrayList<ForeignCountry> destinationCountries; // average 2.4 countries per exporter
	private ArrayList<Float> destinationCountryInitialRatios;

	// agent relationships
	protected int paymentClearingIndex;
	protected int employeeCountTarget;
	protected ArrayList<Individual> employees; // calculate wages & super
	protected AuthorisedDepositTakingInstitution adi; // loans & deposits
	protected ArrayList<Business> domesticSuppliers;
	protected ArrayList<Float> supplierRatios;
	protected ArrayList<ForeignCountry> foreignSuppliers;
	protected ArrayList<Float> foreignSupplierRatios;
	protected Business landlord;
	protected AustralianGovernment govt;
	private int defaultIteration;
	private int defaultOrder;

	// P&L (64 bytes)
	protected float totalIncome;
	protected float salesDomestic;
	protected float salesGovernment;
	protected float salesForeign; // initial AUD foreign sales during calibration
	protected float interestIncome;
	protected float rentIncome;
	protected float otherIncome; // balancing item

	protected float totalExpenses;
	protected float wageExpenses; // wages, super, payroll tax ... ignore w/comp & FBT
	protected float superannuationExpense; // 9.5%
	protected float payrollTaxExpense; // calculated according to each state's rules
	protected float foreignExpenses;
	protected float interestExpense;
	protected float rentExpense;
	protected float depreciationExpense;
	protected float otherExpenses; // balancing item

	// Bal Sht (44 bytes)
	protected float totalAssets;
	protected float bankDeposits;
	protected float foreignEquities;
	protected float otherFinancialAssets;
	protected float otherNonFinancialAssets; // balancing item

	protected float totalLiabilities;
	protected float tradeCreditors;
	protected float loans;
	protected float otherCurrentLiabilities;
	protected float otherNonCurrentLiabilities; // balancing item

	protected float totalEquity;

	// Interest rates (8 bytes)
	protected float interestRateLoans;
	protected float interestRateDeposits;

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
		if (business.isExporter) {
			this.destinationCountries = new ArrayList<ForeignCountry>(business.destinationCountries);
			this.destinationCountryInitialRatios = new ArrayList<Float>(business.destinationCountryInitialRatios);
		} else {
			this.destinationCountries = null;
			this.destinationCountryInitialRatios = null;
		}

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

	/**
	 * Gets the column headings, to write to CSV file.
	 * 
	 * @param separator
	 * @return a CSV list of the column headings
	 */
	public String toCsvStringHeaders(String separator) {
		StringBuilder sb = new StringBuilder();

		sb.append("Name" + separator);
		sb.append("PaymentClearingIndex" + separator);
		sb.append("BusinessTypeID" + separator);
		sb.append("IndustryDivisionCode" + separator);
		sb.append("IndustryCode" + separator);
		sb.append("State" + separator);
		sb.append("LGA" + separator);
		sb.append("Size" + separator);
		sb.append("IsExporter" + separator);
		sb.append("DestinationCountryCount" + separator);
		sb.append("EmployeeCountTarget" + separator);
		sb.append("EmployeeCount" + separator);
		sb.append("AdiID" + separator);
		sb.append("DomesticSupplierCount" + separator);
		sb.append("ForeignSupplierCount" + separator);
		sb.append("LandlordID" + separator);
		sb.append("GovtID" + separator);
		sb.append("TotalIncome" + separator);
		sb.append("SalesDomestic" + separator);
		sb.append("SalesGovernment" + separator);
		sb.append("SalesForeign" + separator);
		sb.append("InterestIncome" + separator);
		sb.append("RentIncome" + separator);
		sb.append("OtherIncome" + separator);
		sb.append("TotalExpenses" + separator);
		sb.append("Wages" + separator);
		sb.append("Superannuation" + separator);
		sb.append("PayrollTax" + separator);
		sb.append("ForeignExpenses" + separator);
		sb.append("InterestExpense" + separator);
		sb.append("RentExpense" + separator);
		sb.append("Depreciation" + separator);
		sb.append("OtherExpenses" + separator);
		sb.append("TotalAssets" + separator);
		sb.append("BankDeposits" + separator);
		sb.append("ForeignEquities" + separator);
		sb.append("OtherFinancialAssets" + separator);
		sb.append("OtherNonFinancialAssets" + separator);
		sb.append("TotalLiabilities" + separator);
		sb.append("TradeCreditors" + separator);
		sb.append("Loans" + separator);
		sb.append("OtherCurrentLiabilities" + separator);
		sb.append("OtherNonCurrentLiabilities" + separator);
		sb.append("TotalEquity");

		return sb.toString();
	}

	/**
	 * Gets the data, to write to CSV file.
	 * 
	 * @param separator
	 * @return a CSV list of the data
	 */
	public String toCsvString(String separator, int iteration) {
		StringBuilder sb = new StringBuilder();

		DecimalFormat decimal = new DecimalFormat("###0.00");
		DecimalFormat wholeNumber = new DecimalFormat("###0");
		DecimalFormat percent = new DecimalFormat("###0.0000");

		sb.append(this.name + separator);
		sb.append(wholeNumber.format(this.paymentClearingIndex) + separator);
		sb.append(wholeNumber.format(this.businessTypeId) + separator);
		sb.append(this.industryDivisionCode + separator);
		sb.append(this.industryCode + separator);
		sb.append(this.state + separator);
		sb.append(this.lgaCode + separator);
		sb.append(this.size + separator);
		sb.append((this.isExporter ? "Y" : "N") + separator);
		sb.append(wholeNumber.format(this.destinationCountries != null ? this.destinationCountries.size() : 0)
				+ separator);
		sb.append(wholeNumber.format(this.employeeCountTarget) + separator);
		sb.append(wholeNumber.format(this.employees != null ? this.employees.size() : 0) + separator);
		sb.append(wholeNumber.format(this.adi != null ? this.adi.getPaymentClearingIndex() : 0) + separator);
		sb.append(wholeNumber.format(this.domesticSuppliers != null ? this.domesticSuppliers.size() : 0) + separator);
		sb.append(wholeNumber.format(this.foreignSuppliers != null ? this.foreignSuppliers.size() : 0) + separator);
		sb.append(wholeNumber.format(this.landlord != null ? this.landlord.getPaymentClearingIndex() : 0) + separator);
		sb.append(wholeNumber.format(this.govt != null ? this.govt.getPaymentClearingIndex() : 0) + separator);
		sb.append(decimal.format(this.totalIncome) + separator);
		sb.append(decimal.format(this.salesDomestic) + separator);
		sb.append(decimal.format(this.salesGovernment) + separator);
		sb.append(decimal.format(this.salesForeign) + separator);
		sb.append(decimal.format(this.interestIncome) + separator);
		sb.append(decimal.format(this.rentIncome) + separator);
		sb.append(decimal.format(this.otherIncome) + separator);
		sb.append(decimal.format(this.totalExpenses) + separator);
		sb.append(decimal.format(this.wageExpenses) + separator);
		sb.append(decimal.format(this.superannuationExpense) + separator);
		sb.append(decimal.format(this.payrollTaxExpense) + separator);
		sb.append(decimal.format(this.foreignExpenses) + separator);
		sb.append(decimal.format(this.interestExpense) + separator);
		sb.append(decimal.format(this.rentExpense) + separator);
		sb.append(decimal.format(this.depreciationExpense) + separator);
		sb.append(decimal.format(this.otherExpenses) + separator);
		sb.append(decimal.format(this.totalAssets) + separator);
		sb.append(decimal.format(this.bankDeposits) + separator);
		sb.append(decimal.format(this.foreignEquities) + separator);
		sb.append(decimal.format(this.otherFinancialAssets) + separator);
		sb.append(decimal.format(this.otherNonFinancialAssets) + separator);
		sb.append(decimal.format(this.totalLiabilities) + separator);
		sb.append(decimal.format(this.tradeCreditors) + separator);
		sb.append(decimal.format(this.loans) + separator);
		sb.append(decimal.format(this.otherCurrentLiabilities) + separator);
		sb.append(decimal.format(this.otherNonCurrentLiabilities) + separator);
		sb.append(decimal.format(this.totalEquity));

		return sb.toString();
	}

	@Override
	public ArrayList<Individual> getEmployees() {
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
	public float getInitialWagesExpense() {
		return this.wageExpenses;
	}

	@Override
	public float getActualWagesExpense() {
		return (float) this.employees.stream().mapToDouble(o -> o.getPnlWagesSalaries()).sum();
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
				float monthlyWagesIncludingSuper = employee.getPnlWagesSalaries()
						* (1f + Properties.SUPERANNUATION_RATE);
				liabilities.add(new NodePayment(index, monthlyWagesIncludingSuper));
			}
		}

		// calculate amounts due to domestic suppliers
		if (this.domesticSuppliers != null && this.otherExpenses > 0d) {
			for (Business supplier : this.domesticSuppliers) {
				int index = supplier.getPaymentClearingIndex();
				float expense = this.otherExpenses / this.domesticSuppliers.size();
				liabilities.add(new NodePayment(index, expense));
			}
		}

		// calculate amounts due to foreign suppliers
		if (this.foreignSuppliers != null && this.foreignExpenses > 0d) {
			for (int supplierIdx = 0; supplierIdx < this.foreignSuppliers.size(); supplierIdx++) {
				ForeignCountry supplier = this.foreignSuppliers.get(supplierIdx);
				int index = supplier.getPaymentClearingIndex();
				float expense = this.foreignExpenses * this.foreignSupplierRatios.get(supplierIdx);
				// add in the effect of FX rate movements
				ArrayList<Float> exchangeRates = supplier.getExchangeRates();
				float currentExchangeRate = exchangeRates.get(0);
				if (exchangeRates.size() >= iteration && exchangeRates.get(iteration) != null) {
					currentExchangeRate = exchangeRates.get(iteration);
				}
				// adjust in the opposite direction to exports
				float exchRateAdjustment = currentExchangeRate / exchangeRates.get(0);
				expense *= exchRateAdjustment;
				liabilities.add(new NodePayment(index, expense));
			}
		}

		// calculate rent due to landlord
		if (this.landlord != null && this.rentExpense > 0d) {
			liabilities.add(new NodePayment(this.landlord.getPaymentClearingIndex(), this.rentExpense));
		}

		// calculate interest due to bank
		if (this.adi != null && this.interestExpense > 0d) {
			liabilities.add(new NodePayment(this.adi.getPaymentClearingIndex(), this.interestExpense));
		}

		// calculate tax due to government (payroll & income)
		float totalTax = this.payrollTaxExpense
				+ Tax.calculateCompanyTax(this.totalIncome, this.totalIncome - this.totalExpenses);
		liabilities.add(new NodePayment(govt.getPaymentClearingIndex(), totalTax));

		liabilities.trimToSize();
		return liabilities;
	}

	@Override
	public void setDefaultedIteration(int iteration, int order) {
		this.defaultIteration = iteration;
		this.defaultOrder = order;
	}

	@Override
	public int getDefaultIteration() {
		return this.defaultIteration;
	}

	@Override
	public int getDefaultOrder() {
		return this.defaultOrder;
	}

	@Override
	public void processClearingPaymentVectorOutput(float nodeEquity, int iteration, int defaultOrder) {
		// update default details
		if (defaultOrder > 0) {
			// update default details unless it defaulted in a previous iteration
			if (this.defaultIteration == 0) {
				// hasn't defaulted in a previous iteration
				this.defaultIteration = iteration;
				this.defaultOrder = defaultOrder;
			}
		}

		// FIXME: process CPV output in Agent
		// update financials
		if ((this.bankDeposits + nodeEquity) > 0f) {
			this.bankDeposits += nodeEquity;
		} else {
			// negative cashflow is greater than bank balance
			if ((this.bankDeposits + this.otherFinancialAssets * (1f - Properties.INVESTMENT_HAIRCUT)
					+ this.foreignEquities * (1f - Properties.FOREIGN_INVESTMENT_HAIRCUT) + nodeEquity) > 0f) {
				// drawing down on other financial assets will be enough to avoid bankruptcy
				// 1. liquidate domestic investments first
				if (this.otherFinancialAssets * (1f - Properties.INVESTMENT_HAIRCUT) + nodeEquity < 0) {
					// domestic financial assets weren't enough, so liquidate all of them
					nodeEquity += this.otherFinancialAssets * (1f - Properties.INVESTMENT_HAIRCUT);
					this.otherFinancialAssets = 0f;
					this.interestIncome = 0f;
				} else {
					// domestic financial assets were enough, so only partially liquidate them
					this.interestIncome = this.interestIncome
							* (1f - (this.otherFinancialAssets + nodeEquity / (1f - Properties.INVESTMENT_HAIRCUT))
									/ this.otherFinancialAssets);
					this.otherFinancialAssets += nodeEquity / (1f - Properties.INVESTMENT_HAIRCUT);
					nodeEquity = 0f;
				}

				// 2. then liquidate foreign investments
				if (this.foreignEquities * (1f - Properties.FOREIGN_INVESTMENT_HAIRCUT) + nodeEquity < 0) {
					// foreign assets weren't enough, so liquidate all of them
					nodeEquity += this.foreignEquities * (1f - Properties.FOREIGN_INVESTMENT_HAIRCUT);
					this.foreignEquities = 0f;
					this.otherIncome = 0f;
				} else {
					// foreign assets were enough, so only partially liquidate them
					this.otherIncome = this.otherIncome
							* (1f - (this.foreignEquities + nodeEquity / (1f - Properties.FOREIGN_INVESTMENT_HAIRCUT))
									/ this.foreignEquities);
					this.foreignEquities += nodeEquity / (1f - Properties.FOREIGN_INVESTMENT_HAIRCUT);
					nodeEquity = 0f;
				}

				// 3. finally use up remaining cash
				if (this.bankDeposits + nodeEquity < 0) {
					// cash must be enough because of the parent if statement that we're within
					this.bankDeposits += nodeEquity;
				}
			} else {
				// business is bankrupt, so fire all employees
				for (Individual employee : this.employees) {
					employee.fireEmployee();
				}
				
				// TODO: remove business deposits from bank's balances
				
				// TODO: remove business loans from bank's balances

				// business is bankrupt, so zero out all its financials
				// it will have no financial impact on any other agent in future iterations
				this.totalIncome = 0f;
				this.salesDomestic = 0f;
				this.salesGovernment = 0f;
				this.salesForeign = 0f;
				this.interestIncome = 0f;
				this.rentIncome = 0f;
				this.otherIncome = 0f;

				this.totalExpenses = 0f;
				this.wageExpenses = 0f;
				this.superannuationExpense = 0f;
				this.payrollTaxExpense = 0f;
				this.foreignExpenses = 0f;
				this.interestExpense = 0f;
				this.rentExpense = 0f;
				this.depreciationExpense = 0f;
				this.otherExpenses = 0f;

				this.totalAssets = 0f;
				this.bankDeposits = 0f;
				this.foreignEquities = 0f;
				this.otherFinancialAssets = 0f;
				this.otherNonFinancialAssets = 0f;

				this.totalLiabilities = 0f;
				this.tradeCreditors = 0f;
				this.loans = 0f;
				this.otherCurrentLiabilities = 0f;
				this.otherNonCurrentLiabilities = 0f;

				this.totalEquity = 0f;
			}
		}
	}

	/**
	 * @return the industryDivisionCode
	 */
	@Override
	public char getIndustryDivisionCode() {
		return industryDivisionCode;
	}

	/**
	 * @param industryDivisionCode the industryDivisionCode to set
	 */
	@Override
	public void setIndustryDivisionCode(char industryDivisionCode) {
		this.industryDivisionCode = industryDivisionCode;
	}

	protected void init() {
		super.init();

		this.industryDivisionCode = '\0'; // unicode zero
		this.industrySubdivisionCode = null;
		this.industryGroupCode = null;
		this.industryClassCode = null;
		this.industryCode = null;

		this.state = null;
		this.lgaCode = null;
		this.size = '\0';

		this.isExporter = false;
		this.destinationCountries = null;
		this.destinationCountryInitialRatios = null;

		this.employeeCountTarget = 0;
		this.employees = null;

		this.defaultIteration = 0;
		this.defaultOrder = 0;

		// P&L
		this.totalIncome = 0f;
		this.salesDomestic = 0f;
		this.salesGovernment = 0f;
		this.salesForeign = 0f;
		this.interestIncome = 0f;
		this.rentIncome = 0f;
		this.otherIncome = 0f;

		this.totalExpenses = 0f;
		this.wageExpenses = 0f;
		this.superannuationExpense = 0f;
		this.payrollTaxExpense = 0f;
		this.foreignExpenses = 0f;
		this.interestExpense = 0f;
		this.rentExpense = 0f;
		this.depreciationExpense = 0f;
		this.otherExpenses = 0f;

		// Bal Sht
		this.totalAssets = 0f;
		this.bankDeposits = 0f;
		this.foreignEquities = 0f;
		this.otherFinancialAssets = 0f;
		this.otherNonFinancialAssets = 0f;

		this.totalLiabilities = 0f;
		this.tradeCreditors = 0f;
		this.loans = 0f;
		this.otherCurrentLiabilities = 0f;
		this.otherNonCurrentLiabilities = 0f;

		this.totalEquity = 0f;

		// Interest Rates
		this.interestRateLoans = 0f;
		this.interestRateDeposits = 0f;
	}

	public float getGrossProfit() {
		return this.getTotalIncome() - this.getTotalExpenses();
	}

	/**
	 * Calculates tax rates per ATO company tax rates, taking into account the lower
	 * rate for small businesses.
	 * 
	 * @return the tax expense
	 */
	public float getTax() {
		return this.getGrossProfit() * Tax.calculateCompanyTax(this.totalIncome, this.totalIncome - this.totalExpenses);
	}

	public float getNetProfit() {
		return this.getGrossProfit() - this.getTax();
	}

	/**
	 * Domestic expenses figure that is used when forming links between agents.
	 * 
	 * @return domestic expenses
	 */
	public float getDomesticExpenses() {
		return this.otherExpenses;
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
	 * @return the destinationCountries
	 */
	public ArrayList<ForeignCountry> getDestinationCountries() {
		return destinationCountries;
	}

	/**
	 * @param destinationCountries the destinationCountries to set
	 */
	public void setDestinationCountries(ArrayList<ForeignCountry> destinationCountries) {
		this.destinationCountries = destinationCountries;
	}

	/**
	 * @return the destinationCountryInitialRatios
	 */
	public ArrayList<Float> getDestinationCountryInitialRatios() {
		return destinationCountryInitialRatios;
	}

	/**
	 * @param destinationCountryInitialRatios the destinationCountryInitialRatios to
	 *                                        set
	 */
	public void setDestinationCountryInitialRatios(ArrayList<Float> destinationCountryInitialRatios) {
		this.destinationCountryInitialRatios = destinationCountryInitialRatios;
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
	 * @return the adi
	 */
	public AuthorisedDepositTakingInstitution getAdi() {
		return adi;
	}

	/**
	 * @param adi the adi to set
	 */
	public void setAdi(AuthorisedDepositTakingInstitution adi) {
		this.adi = adi;
	}

	/**
	 * @return the domesticSuppliers
	 */
	public ArrayList<Business> getDomesticSuppliers() {
		return domesticSuppliers;
	}

	/**
	 * @param domesticSuppliers the domesticSuppliers to set
	 */
	public void setDomesticSuppliers(ArrayList<Business> domesticSuppliers) {
		this.domesticSuppliers = domesticSuppliers;
	}

	/**
	 * @param domesticSupplier the domesticSupplier to set
	 */
	public void addDomesticSupplier(Business domesticSupplier) {
		if (this.domesticSuppliers == null) {
			this.domesticSuppliers = new ArrayList<Business>(CalibrateEconomy.BUSINESS_SUPPLIER_DIV_CODE.length);
		}
		this.domesticSuppliers.add(domesticSupplier);
	}

	/**
	 * Trims the list to size to minimise memory usage.
	 */
	public void trimDomesticSuppliersList() {
		if (this.domesticSuppliers != null) {
			this.domesticSuppliers.trimToSize();
		}
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
	 * @return the foreignSuppliers
	 */
	public ArrayList<ForeignCountry> getForeignSuppliers() {
		return foreignSuppliers;
	}

	/**
	 * @param foreignSuppliers the foreignSuppliers to set
	 */
	public void setForeignSuppliers(ArrayList<ForeignCountry> foreignSuppliers) {
		this.foreignSuppliers = foreignSuppliers;
	}

	/**
	 * @return the foreignSupplierRatios
	 */
	public ArrayList<Float> getForeignSupplierRatios() {
		return foreignSupplierRatios;
	}

	/**
	 * @param foreignSupplierRatios the foreignSupplierRatios to set
	 */
	public void setForeignSupplierRatios(ArrayList<Float> foreignSupplierRatios) {
		this.foreignSupplierRatios = foreignSupplierRatios;
	}

	/**
	 * @return the landlord
	 */
	public Business getLandlord() {
		return landlord;
	}

	/**
	 * @param landlord the landlord to set
	 */
	public void setLandlord(Business landlord) {
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
	 * @return the totalIncome
	 */
	public float getTotalIncome() {
		return totalIncome;
	}

	/**
	 * @param totalIncome the totalIncome to set
	 */
	public void setTotalIncome(float totalIncome) {
		this.totalIncome = totalIncome;
	}

	/**
	 * @return the salesDomestic
	 */
	public float getSalesDomestic() {
		return salesDomestic;
	}

	/**
	 * @param salesDomestic the salesDomestic to set
	 */
	public void setSalesDomestic(float salesDomestic) {
		this.salesDomestic = salesDomestic;
	}

	/**
	 * @return the salesGovernment
	 */
	public float getSalesGovernment() {
		return salesGovernment;
	}

	/**
	 * @param salesGovernment the salesGovernment to set
	 */
	public void setSalesGovernment(float salesGovernment) {
		this.salesGovernment = salesGovernment;
	}

	/**
	 * @return the salesForeign
	 */
	public float getSalesForeign() {
		return salesForeign;
	}

	/**
	 * @param salesForeign the salesForeign to set
	 */
	public void setSalesForeign(float salesForeign) {
		this.salesForeign = salesForeign;
	}

	/**
	 * @return the interestIncome
	 */
	public float getInterestIncome() {
		return interestIncome;
	}

	/**
	 * @param interestIncome the interestIncome to set
	 */
	public void setInterestIncome(float interestIncome) {
		this.interestIncome = interestIncome;
	}

	/**
	 * @return the rentIncome
	 */
	public float getRentIncome() {
		return rentIncome;
	}

	/**
	 * @param rentIncome the rentIncome to set
	 */
	public void setRentIncome(float rentIncome) {
		this.rentIncome = rentIncome;
	}

	/**
	 * @return the otherIncome
	 */
	public float getOtherIncome() {
		return otherIncome;
	}

	/**
	 * @param otherIncome the otherIncome to set
	 */
	public void setOtherIncome(float otherIncome) {
		this.otherIncome = otherIncome;
	}

	/**
	 * @return the totalExpenses
	 */
	public float getTotalExpenses() {
		return totalExpenses;
	}

	/**
	 * @param totalExpenses the totalExpenses to set
	 */
	public void setTotalExpenses(float totalExpenses) {
		this.totalExpenses = totalExpenses;
	}

	/**
	 * @return the wageExpenses
	 */
	public float getWageExpenses() {
		return wageExpenses;
	}

	/**
	 * @param wageExpenses the wageExpenses to set
	 */
	public void setWageExpenses(float wageExpenses) {
		this.wageExpenses = wageExpenses;
	}

	/**
	 * @return the superannuationExpense
	 */
	public float getSuperannuationExpense() {
		return superannuationExpense;
	}

	/**
	 * @param superannuationExpense the superannuationExpense to set
	 */
	public void setSuperannuationExpense(float superannuationExpense) {
		this.superannuationExpense = superannuationExpense;
	}

	/**
	 * @return the payrollTaxExpense
	 */
	public float getPayrollTaxExpense() {
		return payrollTaxExpense;
	}

	/**
	 * @param payrollTaxExpense the payrollTaxExpense to set
	 */
	public void setPayrollTaxExpense(float payrollTaxExpense) {
		this.payrollTaxExpense = payrollTaxExpense;
	}

	/**
	 * @return the foreignExpenses
	 */
	public float getForeignExpenses() {
		return foreignExpenses;
	}

	/**
	 * @param foreignExpenses the foreignExpenses to set
	 */
	public void setForeignExpenses(float foreignExpenses) {
		this.foreignExpenses = foreignExpenses;
	}

	/**
	 * @return the interestExpense
	 */
	public float getInterestExpense() {
		return interestExpense;
	}

	/**
	 * @param interestExpense the interestExpense to set
	 */
	public void setInterestExpense(float interestExpense) {
		this.interestExpense = interestExpense;
	}

	/**
	 * @return the rentExpense
	 */
	public float getRentExpense() {
		return rentExpense;
	}

	/**
	 * @param rentExpense the rentExpense to set
	 */
	public void setRentExpense(float rentExpense) {
		this.rentExpense = rentExpense;
	}

	/**
	 * @return the depreciationExpense
	 */
	public float getDepreciationExpense() {
		return depreciationExpense;
	}

	/**
	 * @param depreciationExpense the depreciationExpense to set
	 */
	public void setDepreciationExpense(float depreciationExpense) {
		this.depreciationExpense = depreciationExpense;
	}

	/**
	 * @return the otherExpenses
	 */
	public float getOtherExpenses() {
		return otherExpenses;
	}

	/**
	 * @param otherExpenses the otherExpenses to set
	 */
	public void setOtherExpenses(float otherExpenses) {
		this.otherExpenses = otherExpenses;
	}

	/**
	 * @return the totalAssets
	 */
	public float getTotalAssets() {
		return totalAssets;
	}

	/**
	 * @param totalAssets the totalAssets to set
	 */
	public void setTotalAssets(float totalAssets) {
		this.totalAssets = totalAssets;
	}

	/**
	 * @return the bankDeposits
	 */
	public float getBankDeposits() {
		return bankDeposits;
	}

	/**
	 * @param bankDeposits the bankDeposits to set
	 */
	public void setBankDeposits(float bankDeposits) {
		this.bankDeposits = bankDeposits;
	}

	/**
	 * @return the foreignEquities
	 */
	public float getForeignEquities() {
		return foreignEquities;
	}

	/**
	 * @param foreignEquities the foreignEquities to set
	 */
	public void setForeignEquities(float foreignEquities) {
		this.foreignEquities = foreignEquities;
	}

	/**
	 * @return the otherFinancialAssets
	 */
	public float getOtherFinancialAssets() {
		return otherFinancialAssets;
	}

	/**
	 * @param otherFinancialAssets the otherFinancialAssets to set
	 */
	public void setOtherFinancialAssets(float otherFinancialAssets) {
		this.otherFinancialAssets = otherFinancialAssets;
	}

	/**
	 * @return the otherNonFinancialAssets
	 */
	public float getOtherNonFinancialAssets() {
		return otherNonFinancialAssets;
	}

	/**
	 * @param otherNonFinancialAssets the otherNonFinancialAssets to set
	 */
	public void setOtherNonFinancialAssets(float otherNonFinancialAssets) {
		this.otherNonFinancialAssets = otherNonFinancialAssets;
	}

	/**
	 * @return the totalLiabilities
	 */
	public float getTotalLiabilities() {
		return totalLiabilities;
	}

	/**
	 * @param totalLiabilities the totalLiabilities to set
	 */
	public void setTotalLiabilities(float totalLiabilities) {
		this.totalLiabilities = totalLiabilities;
	}

	/**
	 * @return the tradeCreditors
	 */
	public float getTradeCreditors() {
		return tradeCreditors;
	}

	/**
	 * @param tradeCreditors the tradeCreditors to set
	 */
	public void setTradeCreditors(float tradeCreditors) {
		this.tradeCreditors = tradeCreditors;
	}

	/**
	 * @return the loans
	 */
	public float getLoans() {
		return loans;
	}

	/**
	 * @param loans the loans to set
	 */
	public void setLoans(float loans) {
		this.loans = loans;
	}

	/**
	 * @return the otherCurrentLiabilities
	 */
	public float getOtherCurrentLiabilities() {
		return otherCurrentLiabilities;
	}

	/**
	 * @param otherCurrentLiabilities the otherCurrentLiabilities to set
	 */
	public void setOtherCurrentLiabilities(float otherCurrentLiabilities) {
		this.otherCurrentLiabilities = otherCurrentLiabilities;
	}

	/**
	 * @return the otherNonCurrentLiabilities
	 */
	public float getOtherNonCurrentLiabilities() {
		return otherNonCurrentLiabilities;
	}

	/**
	 * @param otherNonCurrentLiabilities the otherNonCurrentLiabilities to set
	 */
	public void setOtherNonCurrentLiabilities(float otherNonCurrentLiabilities) {
		this.otherNonCurrentLiabilities = otherNonCurrentLiabilities;
	}

	/**
	 * @return the totalEquity
	 */
	public float getTotalEquity() {
		return totalEquity;
	}

	/**
	 * @param totalEquity the totalEquity to set
	 */
	public void setTotalEquity(float totalEquity) {
		this.totalEquity = totalEquity;
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
	 * @param employees the employees to set
	 */
	public void setEmployees(ArrayList<Individual> employees) {
		this.employees = employees;
	}

}
