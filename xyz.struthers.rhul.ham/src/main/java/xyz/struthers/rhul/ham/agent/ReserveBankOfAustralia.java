/**
 * 
 */
package xyz.struthers.rhul.ham.agent;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gnu.trove.map.hash.TObjectFloatHashMap;
import xyz.struthers.rhul.ham.config.PropertiesXml;
import xyz.struthers.rhul.ham.config.PropertiesXmlFactory;
import xyz.struthers.rhul.ham.process.Clearable;
import xyz.struthers.rhul.ham.process.Employer;
import xyz.struthers.rhul.ham.process.NodePayment;

/**
 * Each instance of this class stores 21 floats, so will consume approximately
 * 168 bytes of RAM. There is only one instance of this class in the model.
 * 
 * @author Adam Struthers
 * @since 25-Jan-2019
 */
public final class ReserveBankOfAustralia extends Agent implements Employer {

	private static boolean DEBUG = true;

	private static final long serialVersionUID = 1L;

	private final float NUMBER_MONTHS = 12f;
	private static PropertiesXml properties = PropertiesXmlFactory.getProperties();

	// interest rate scenarios
	// public static final float OCR_INITIAL = 0.015f;
	public static final int RATES_SAME = 0;
	public static final int RATES_CUSTOM = 1;

	// RBA details
	protected String name;
	protected char industryDivisionCode;

	// agent relationships
	protected int paymentClearingIndex;
	protected ArrayList<Individual> employees; // calculate wages & super
	protected ArrayList<AuthorisedDepositTakingInstitution> adiDepositors; // major and other domestic banks
	private AustralianGovernment govt;
	private int defaultIteration;
	private int defaultOrder;

	// cash rate
	protected ArrayList<Float> cashRate;

	// P&L
	private float pnlInterestIncome;
	private float pnlInterestExpense;
	private float pnlCommittedLiquidityFacilityFees;
	private float pnlForeignExchangeGainsLosses; // TODO: add FX GOL to exogeneous cashflow, with FX rates
	private float pnlAudSecurities;
	private float pnlOtherIncome;

	private float pnlPersonnelExpenses;
	private float pnlDepreciationAmortisation;
	private float pnlOtherExpenses;

	private float pnlDistributionPayableToCommonwealth;

	// Bal Sht
	private float bsCash;
	private float bsAudInvestments;
	private float bsForeignInvestments;
	private float bsGold;
	private float bsOtherAssets;

	private float bsDeposits;
	private float bsDistributionPayableToCommonwealth;
	private float bsBanknotesOnIssue;
	private float bsOtherLiabilities;

	private float bsCapital;
	private float bsReserves;
	private float bsCurrentYearEarnings;

	/**
	 * 
	 */
	public ReserveBankOfAustralia() {
		super();
		this.init();
	}

	/**
	 * The keys in the data map are the field titles used in the CSV file that the
	 * data is imported from.
	 * 
	 * @param balSht     - a map of Balance Sheet fields and values
	 * @param profitLoss - a map of Profit & Loss Statement fields and values
	 */
	public ReserveBankOfAustralia(TObjectFloatHashMap<String> balSht, TObjectFloatHashMap<String> profitLoss) {
		super();
		this.init();

		// P&L
		this.pnlInterestIncome = profitLoss.get("Interest income");
		this.pnlInterestExpense = profitLoss.get("Interest expense");
		this.pnlCommittedLiquidityFacilityFees = profitLoss.get("Committed Liquidity Facility Fees");
		this.pnlForeignExchangeGainsLosses = profitLoss.get("FX Gains/(Losses)");
		this.pnlAudSecurities = profitLoss.get("AUD securities");
		this.pnlOtherIncome = profitLoss.get("Other income");

		this.pnlPersonnelExpenses = profitLoss.get("Personnel expenses");
		this.pnlDepreciationAmortisation = profitLoss.get("Depreciation and amortisation");
		this.pnlOtherExpenses = profitLoss.get("Other expenses");

		this.pnlDistributionPayableToCommonwealth = profitLoss.get("Distribution payable to the Commonwealth");

		// Bal Sht
		this.bsCash = balSht.get("Cash");
		this.bsAudInvestments = balSht.get("AUD investments");
		this.bsForeignInvestments = balSht.get("Foreign currency investments");
		this.bsGold = balSht.get("Gold");
		this.bsOtherAssets = balSht.get("Other assets");

		this.bsDeposits = balSht.get("Deposits");
		this.bsDistributionPayableToCommonwealth = balSht.get("Distribution payable to the Commonwealth");
		this.bsBanknotesOnIssue = balSht.get("Australian banknotes on issue");
		this.bsOtherLiabilities = balSht.get("Other liabilities");

		this.bsCapital = balSht.get("Capital");
		this.bsReserves = balSht.get("Reserves");
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
		sb.append("Division" + separator);
		sb.append("PaymentClearingIndex" + separator);
		sb.append("EmployeeCount" + separator);
		sb.append("AdiDepositorCount" + separator);
		sb.append("CashRate" + separator);
		sb.append("InterestIncome" + separator);
		sb.append("InterestExpense" + separator);
		sb.append("ClfFees" + separator);
		sb.append("FxGainOrLoss" + separator);
		sb.append("AudSecurities" + separator);
		sb.append("OtherIncome" + separator);
		sb.append("PersonnelExpenses" + separator);
		sb.append("Depreciation" + separator);
		sb.append("OtherExpenses" + separator);
		sb.append("PnlDistributionToCommonwealth" + separator);
		sb.append("Cash" + separator);
		sb.append("AudInvestments" + separator);
		sb.append("ForeignInvestments" + separator);
		sb.append("Gold" + separator);
		sb.append("OtherAssets" + separator);
		sb.append("Deposits" + separator);
		sb.append("BalShtDistributionToCommonwealth" + separator);
		sb.append("BanknotesOnIssue" + separator);
		sb.append("OtherLiabilities" + separator);
		sb.append("Capital" + separator);
		sb.append("Reserves");

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
		sb.append(this.industryDivisionCode + separator);
		sb.append(wholeNumber.format(this.paymentClearingIndex) + separator);
		sb.append(wholeNumber.format(this.employees != null ? this.employees.size() : 0) + separator);
		sb.append(wholeNumber.format(this.adiDepositors != null ? this.adiDepositors.size() : 0) + separator);
		if (DEBUG) {
			if (this.cashRate != null) {
				System.out.print("RBA cashRate for iteration " + iteration + " = ");
				for (int i = 0; i < this.cashRate.size(); i++) {
					System.out.print(this.cashRate.get(i) + ",");
				}
				System.out.println(".");
			} else {
				System.out.println("cashRate is null");
			}
			// RBA cashRate for iteration 1 = 0.015,.
		}
		sb.append(percent.format(this.cashRate != null ? this.cashRate.get(iteration) : 0) + separator);
		sb.append(decimal.format(this.pnlInterestIncome) + separator);
		sb.append(decimal.format(this.pnlInterestExpense) + separator);
		sb.append(decimal.format(this.pnlCommittedLiquidityFacilityFees) + separator);
		sb.append(decimal.format(this.pnlForeignExchangeGainsLosses) + separator);
		sb.append(decimal.format(this.pnlAudSecurities) + separator);
		sb.append(decimal.format(this.pnlOtherIncome) + separator);
		sb.append(decimal.format(this.pnlPersonnelExpenses) + separator);
		sb.append(decimal.format(this.pnlDepreciationAmortisation) + separator);
		sb.append(decimal.format(this.pnlOtherExpenses) + separator);
		sb.append(decimal.format(this.pnlDistributionPayableToCommonwealth) + separator);
		sb.append(decimal.format(this.bsCash) + separator);
		sb.append(decimal.format(this.bsAudInvestments) + separator);
		sb.append(decimal.format(this.bsForeignInvestments) + separator);
		sb.append(decimal.format(this.bsGold) + separator);
		sb.append(decimal.format(this.bsOtherAssets) + separator);
		sb.append(decimal.format(this.bsDeposits) + separator);
		sb.append(decimal.format(this.bsDistributionPayableToCommonwealth) + separator);
		sb.append(decimal.format(this.bsBanknotesOnIssue) + separator);
		sb.append(decimal.format(this.bsOtherLiabilities) + separator);
		sb.append(decimal.format(this.bsCapital) + separator);
		sb.append(decimal.format(this.bsReserves));

		return sb.toString();
	}

	/**
	 * Gets the summary column headings, to write to CSV file.
	 * <p>
	 * The intention is for this data to form the basis of a statistical analysis
	 * (for example, using R).
	 * 
	 * @param separator
	 * @return a CSV list of the column headings
	 */
	public String toCsvSummaryStringHeaders(String separator) {
		// RBA is small enough I'll just save all the details
		return this.toCsvStringHeaders(separator);
	}

	/**
	 * Gets the summary data, to write to CSV file.
	 * <p>
	 * The intention is for this data to form the basis of a statistical analysis
	 * (for example, using R).
	 * 
	 * @param separator
	 * @return a CSV list of the data
	 */
	public String toCsvSummaryString(String separator, int iteration) {
		// AU Government is small enough I'll just save all the details
		return this.toCsvString(separator, iteration);
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
	}

	@Override
	public float getInitialWagesExpense() {
		return this.pnlPersonnelExpenses / properties.getSuperannuationGuaranteeRate();
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
		int numberOfCreditors = 1;
		if (this.adiDepositors != null) {
			numberOfCreditors = this.adiDepositors.size();
		}
		ArrayList<NodePayment> liabilities = new ArrayList<NodePayment>(numberOfCreditors);

		// bank balances of major and other domestic banks, paid at cash rate
		for (int adiIdx = 0; adiIdx < this.adiDepositors.size(); adiIdx++) {
			AuthorisedDepositTakingInstitution adi = this.adiDepositors.get(adiIdx);
			int index = adi.getPaymentClearingIndex();
			float balance = adi.getBsCash();
			float rate = 0f;
			if (this.cashRate != null && this.cashRate.size() > iteration) {
				rate = this.cashRate.get(iteration);
			}
			liabilities.add(new NodePayment(index, balance * rate / NUMBER_MONTHS));
		}

		// RBA pays its net profit to govt annually
		if (iteration % 12 == 0) {
			// current year earnings is added to monthly, and paid to the govt annually
			liabilities.add(new NodePayment(this.govt.getPaymentClearingIndex(), this.bsCurrentYearEarnings));
			this.bsCurrentYearEarnings = 0f;
		}

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
	public int processClearingPaymentVectorOutput(float nodeEquity, int iteration, int defaultOrder) {
		// update default details
		if (defaultOrder > 0) {
			// update default details unless it defaulted in a previous iteration
			if (this.defaultIteration == 0) {
				// hasn't defaulted in a previous iteration
				this.defaultIteration = iteration;
				this.defaultOrder = defaultOrder;
			}
		}

		// update financials
		if (nodeEquity < -this.bsCash) {
			// net cash outflow was greater than cash reserves, so fund through new
			// borrowings (could be "printing money" by borrowing from the government)
			float newBorrowings = -nodeEquity - this.bsCash;
			this.bsCash = 0f;
			this.bsOtherLiabilities += newBorrowings;
		} else {
			this.bsCash += nodeEquity;
		}
		this.bsCurrentYearEarnings += nodeEquity;

		return Clearable.OK; // assume RBA never defaults
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

	public void applyInflation(float monthlyInflationRate, int iteration) {
		// interest income & expense don't respond to inflation
		// CLF fees don't vary with inflation
		/// FX doesnt' vary with inflation
		// AUD securities don't vary with inflation

		this.pnlOtherIncome = this.pnlOtherIncome * (1f + monthlyInflationRate);

		// changes in wages are delayed by 12 months
		if ((iteration % 12) == 0) {
			// TODO: increase personnel expenses

			// re-calculate income tax expense
		}

		// Depreciation expense responds to inflation with a 12 month lag
		if ((iteration % 12) == 0) {
			// TODO increase depreciation 20% at a time, assuming 5 year life of assets

		}

		this.pnlOtherExpenses = this.pnlOtherExpenses * (1f + monthlyInflationRate);

		// TODO: update distribution payable to Commonwealth
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the adiDepositors
	 */
	public ArrayList<AuthorisedDepositTakingInstitution> getAdiDepositors() {
		return adiDepositors;
	}

	/**
	 * @param adiDepositors the adiDepositors to set
	 */
	public void setAdiDepositors(ArrayList<AuthorisedDepositTakingInstitution> adiDepositors) {
		this.adiDepositors = adiDepositors;
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
	 * Gets the movement in the cash rate since the initial calibration. This can be
	 * added to the ADI rates to get the current effective rates.
	 * 
	 * @param iteration
	 * @return the movement in the cash rate since calibration
	 */
	public float getCashRateChange(int iteration) {
		float rate = 0f;
		if (this.cashRate != null && this.cashRate.size() > iteration) {
			rate = this.cashRate.get(iteration) - this.cashRate.get(0);
		}
		return rate;
	}

	public float getCashRate(int iteration) {
		float rate = 0f;
		if (this.cashRate != null && this.cashRate.size() > iteration) {
			rate = this.cashRate.get(iteration);
		}
		return rate;
	}

	public void setCashRate(int iteration, float rate) {
		if (this.cashRate == null) {
			this.cashRate = new ArrayList<Float>(iteration);
		}
		if (iteration > this.cashRate.size()) {
			ArrayList<Float> tmp = this.cashRate;
			this.cashRate = new ArrayList<Float>(iteration);
			this.cashRate.addAll(tmp);
		}
		this.cashRate.set(iteration, rate);
	}

	public float setCashRateSame(int iteration) {
		if (this.cashRate == null) {
			this.cashRate = new ArrayList<Float>(12); // assume we model at least a year
			this.cashRate.add(properties.getInitialCashRate());
		}
		float interestRate = 0f;
		if (this.cashRate.size() > iteration) {
			interestRate = (iteration == 0 ? properties.getInitialCashRate() : this.cashRate.get(iteration - 1));
			this.cashRate.set(iteration, interestRate);
		} else if (this.cashRate.size() == iteration) {
			interestRate = (iteration == 0 ? properties.getInitialCashRate() : this.cashRate.get(iteration - 1));
			this.cashRate.add(interestRate);
		} else {
			interestRate = properties.getInitialCashRate();
			this.cashRate.add(interestRate);
		}
		return interestRate;
	}

	public float setCashRateCustomPath(int iteration, float[] customRates) {
		if (this.cashRate == null) {
			this.cashRate = new ArrayList<Float>(customRates.length); // assume we model at least a year
			this.cashRate.add(properties.getInitialCashRate());
		}
		float interestRate = 0f;
		if (this.cashRate.size() > iteration) {
			if (customRates.length > iteration) {
				interestRate = (iteration == 0 ? properties.getInitialCashRate() : customRates[iteration]);
			} else {
				// iteration would read past end of the array, so use the previous rate
				interestRate = (iteration == 0 ? properties.getInitialCashRate() : this.cashRate.get(iteration - 1));
			}
			this.cashRate.set(iteration, interestRate);
		} else if (this.cashRate.size() == iteration) {
			if (customRates.length > iteration) {
				interestRate = (iteration == 0 ? properties.getInitialCashRate() : customRates[iteration]);
			} else {
				// iteration would read past end of the array, so use the previous rate
				interestRate = (iteration == 0 ? properties.getInitialCashRate() : this.cashRate.get(iteration - 1));
			}
			this.cashRate.add(interestRate);
		} else {
			interestRate = properties.getInitialCashRate();
			this.cashRate.add(interestRate);
		}
		return interestRate;
	}

	public Map<String, Float> getFinancialStatements() {
		// initialised to the number of fields in this class
		final int NUM_LINE_ITEMS = 21;
		Map<String, Float> result = new HashMap<String, Float>(NUM_LINE_ITEMS);

		// P&L
		result.put("Interest income", this.pnlInterestIncome);
		result.put("Interest expense", this.pnlInterestExpense);
		result.put("Committed Liquidity Facility Fees", this.pnlCommittedLiquidityFacilityFees);
		result.put("FX Gains/(Losses)", this.pnlForeignExchangeGainsLosses);
		result.put("AUD securities", this.pnlAudSecurities);
		result.put("Other income", this.pnlOtherIncome);

		result.put("Personnel expenses", this.pnlPersonnelExpenses);
		result.put("Depreciation and amortisation", this.pnlDepreciationAmortisation);
		result.put("Other expenses", this.pnlOtherExpenses);

		result.put("Distribution payable to the Commonwealth", this.pnlDistributionPayableToCommonwealth);

		// Bal Sht
		result.put("Cash", this.bsCash);
		result.put("AUD investments", this.bsAudInvestments);
		result.put("Foreign currency investments", this.bsForeignInvestments);
		result.put("Gold", this.bsGold);
		result.put("Other assets", this.bsOtherAssets);

		result.put("Deposits", this.bsDeposits);
		result.put("Distribution payable to the Commonwealth", this.bsDistributionPayableToCommonwealth);
		result.put("Australian banknotes on issue", this.bsBanknotesOnIssue);
		result.put("Other liabilities", this.bsOtherLiabilities);

		result.put("Capital", this.bsCapital);
		result.put("Reserves", this.bsReserves);

		return result;
	}

	public float getTotalIncome() {
		return this.pnlInterestIncome + this.pnlCommittedLiquidityFacilityFees + this.pnlForeignExchangeGainsLosses
				+ this.pnlAudSecurities + this.pnlOtherIncome;
	}

	protected void init() {

		// Agent details
		super.init();
		this.name = "RBA";

		// agent relationships
		this.paymentClearingIndex = 0;
		this.adiDepositors = null;
		this.defaultIteration = 0;
		this.defaultOrder = 0;

		// cash rate
		this.cashRate = null;

		// P&L
		this.pnlInterestIncome = 0f;
		this.pnlInterestExpense = 0f;
		this.pnlCommittedLiquidityFacilityFees = 0f;
		this.pnlForeignExchangeGainsLosses = 0f;
		this.pnlAudSecurities = 0f;
		this.pnlOtherIncome = 0f;

		this.pnlPersonnelExpenses = 0f;
		this.pnlDepreciationAmortisation = 0f;
		this.pnlOtherExpenses = 0f;

		this.pnlDistributionPayableToCommonwealth = 0f;

		// Bal Sht
		this.bsCash = 0f;
		this.bsAudInvestments = 0f;
		this.bsForeignInvestments = 0f;
		this.bsGold = 0f;
		this.bsOtherAssets = 0f;

		this.bsDeposits = 0f;
		this.bsDistributionPayableToCommonwealth = 0f;
		this.bsBanknotesOnIssue = 0f;
		this.bsOtherLiabilities = 0f;

		this.bsCapital = 0f;
		this.bsReserves = 0f;
		this.bsCurrentYearEarnings = 0f;
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
	 * @return the pnlInterestExpense
	 */
	public float getPnlInterestExpense() {
		return pnlInterestExpense;
	}

	/**
	 * @param pnlInterestExpense the pnlInterestExpense to set
	 */
	public void setPnlInterestExpense(float pnlInterestExpense) {
		this.pnlInterestExpense = pnlInterestExpense;
	}

	/**
	 * @return the pnlCommittedLiquidityFacilityFees
	 */
	public float getPnlCommittedLiquidityFacilityFees() {
		return pnlCommittedLiquidityFacilityFees;
	}

	/**
	 * @param pnlCommittedLiquidityFacilityFees the
	 *                                          pnlCommittedLiquidityFacilityFees to
	 *                                          set
	 */
	public void setPnlCommittedLiquidityFacilityFees(float pnlCommittedLiquidityFacilityFees) {
		this.pnlCommittedLiquidityFacilityFees = pnlCommittedLiquidityFacilityFees;
	}

	/**
	 * @return the pnlForeignExchangeGainsLosses
	 */
	public float getPnlForeignExchangeGainsLosses() {
		return pnlForeignExchangeGainsLosses;
	}

	/**
	 * @param pnlForeignExchangeGainsLosses the pnlForeignExchangeGainsLosses to set
	 */
	public void setPnlForeignExchangeGainsLosses(float pnlForeignExchangeGainsLosses) {
		this.pnlForeignExchangeGainsLosses = pnlForeignExchangeGainsLosses;
	}

	/**
	 * @return the pnlAudSecurities
	 */
	public float getPnlAudSecurities() {
		return pnlAudSecurities;
	}

	/**
	 * @param pnlAudSecurities the pnlAudSecurities to set
	 */
	public void setPnlAudSecurities(float pnlAudSecurities) {
		this.pnlAudSecurities = pnlAudSecurities;
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
	 * @return the pnlPersonnelExpenses
	 */
	public float getPnlPersonnelExpenses() {
		return pnlPersonnelExpenses;
	}

	/**
	 * @param pnlPersonnelExpenses the pnlPersonnelExpenses to set
	 */
	public void setPnlPersonnelExpenses(float pnlPersonnelExpenses) {
		this.pnlPersonnelExpenses = pnlPersonnelExpenses;
	}

	/**
	 * @return the pnlDepreciationAmortisation
	 */
	public float getPnlDepreciationAmortisation() {
		return pnlDepreciationAmortisation;
	}

	/**
	 * @param pnlDepreciationAmortisation the pnlDepreciationAmortisation to set
	 */
	public void setPnlDepreciationAmortisation(float pnlDepreciationAmortisation) {
		this.pnlDepreciationAmortisation = pnlDepreciationAmortisation;
	}

	/**
	 * @return the pnlOtherExpenses
	 */
	public float getPnlOtherExpenses() {
		return pnlOtherExpenses;
	}

	/**
	 * @param pnlOtherExpenses the pnlOtherExpenses to set
	 */
	public void setPnlOtherExpenses(float pnlOtherExpenses) {
		this.pnlOtherExpenses = pnlOtherExpenses;
	}

	/**
	 * @return the pnlDistributionPayableToCommonwealth
	 */
	public float getPnlDistributionPayableToCommonwealth() {
		return pnlDistributionPayableToCommonwealth;
	}

	/**
	 * @param pnlDistributionPayableToCommonwealth the
	 *                                             pnlDistributionPayableToCommonwealth
	 *                                             to set
	 */
	public void setPnlDistributionPayableToCommonwealth(float pnlDistributionPayableToCommonwealth) {
		this.pnlDistributionPayableToCommonwealth = pnlDistributionPayableToCommonwealth;
	}

	/**
	 * @return the bsCash
	 */
	public float getBsCash() {
		return bsCash;
	}

	/**
	 * @param bsCash the bsCash to set
	 */
	public void setBsCash(float bsCash) {
		this.bsCash = bsCash;
	}

	/**
	 * @return the bsAudInvestments
	 */
	public float getBsAudInvestments() {
		return bsAudInvestments;
	}

	/**
	 * @param bsAudInvestments the bsAudInvestments to set
	 */
	public void setBsAudInvestments(float bsAudInvestments) {
		this.bsAudInvestments = bsAudInvestments;
	}

	/**
	 * @return the bsForeignInvestments
	 */
	public float getBsForeignInvestments() {
		return bsForeignInvestments;
	}

	/**
	 * @param bsForeignInvestments the bsForeignInvestments to set
	 */
	public void setBsForeignInvestments(float bsForeignInvestments) {
		this.bsForeignInvestments = bsForeignInvestments;
	}

	/**
	 * @return the bsGold
	 */
	public float getBsGold() {
		return bsGold;
	}

	/**
	 * @param bsGold the bsGold to set
	 */
	public void setBsGold(float bsGold) {
		this.bsGold = bsGold;
	}

	/**
	 * @return the bsOtherAssets
	 */
	public float getBsOtherAssets() {
		return bsOtherAssets;
	}

	/**
	 * @param bsOtherAssets the bsOtherAssets to set
	 */
	public void setBsOtherAssets(float bsOtherAssets) {
		this.bsOtherAssets = bsOtherAssets;
	}

	/**
	 * @return the bsDeposits
	 */
	public float getBsDeposits() {
		return bsDeposits;
	}

	/**
	 * @param bsDeposits the bsDeposits to set
	 */
	public void setBsDeposits(float bsDeposits) {
		this.bsDeposits = bsDeposits;
	}

	/**
	 * @return the bsDistributionPayableToCommonwealth
	 */
	public float getBsDistributionPayableToCommonwealth() {
		return bsDistributionPayableToCommonwealth;
	}

	/**
	 * @param bsDistributionPayableToCommonwealth the
	 *                                            bsDistributionPayableToCommonwealth
	 *                                            to set
	 */
	public void setBsDistributionPayableToCommonwealth(float bsDistributionPayableToCommonwealth) {
		this.bsDistributionPayableToCommonwealth = bsDistributionPayableToCommonwealth;
	}

	/**
	 * @return the bsBanknotesOnIssue
	 */
	public float getBsBanknotesOnIssue() {
		return bsBanknotesOnIssue;
	}

	/**
	 * @param bsBanknotesOnIssue the bsBanknotesOnIssue to set
	 */
	public void setBsBanknotesOnIssue(float bsBanknotesOnIssue) {
		this.bsBanknotesOnIssue = bsBanknotesOnIssue;
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
	 * @return the bsCapital
	 */
	public float getBsCapital() {
		return bsCapital;
	}

	/**
	 * @param bsCapital the bsCapital to set
	 */
	public void setBsCapital(float bsCapital) {
		this.bsCapital = bsCapital;
	}

	/**
	 * @return the bsReserves
	 */
	public float getBsReserves() {
		return bsReserves;
	}

	/**
	 * @param bsReserves the bsReserves to set
	 */
	public void setBsReserves(float bsReserves) {
		this.bsReserves = bsReserves;
	}

	/**
	 * @return the bsCurrentYearEarnings
	 */
	public float getBsCurrentYearEarnings() {
		return bsCurrentYearEarnings;
	}

	/**
	 * @param bsCurrentYearEarnings the bsCurrentYearEarnings to set
	 */
	public void setBsCurrentYearEarnings(float bsCurrentYearEarnings) {
		this.bsCurrentYearEarnings = bsCurrentYearEarnings;
	}

}
