/**
 * 
 */
package xyz.struthers.rhul.ham.process;

/**
 * @author Adam Struthers
 * @since 19-Feb-2019
 */
public abstract class Tax {

	private static final Double NUM_MONTHS = 12d;

	public static final Double COMPANY_TAX_THRESHOLD = 50000000d;
	public static final Double COMPANY_TAX_RATE_SMALL = 0.275d;
	public static final Double COMPANY_TAX_RATE_LARGE = 0.30d;

	public static final Double PAYROLL_TAX_THRESHOLD_NSW = 850000d;
	public static final Double PAYROLL_TAX_THRESHOLD_VIC = 650000d;
	public static final Double PAYROLL_TAX_THRESHOLD_QLD = 1100000d;
	public static final Double PAYROLL_TAX_THRESHOLD_SA = 1500000d;
	public static final Double PAYROLL_TAX_THRESHOLD_WA = 850000d;
	public static final Double PAYROLL_TAX_THRESHOLD_TAS1 = 1250000d;
	public static final Double PAYROLL_TAX_THRESHOLD_TAS2 = 2000000d;
	public static final Double PAYROLL_TAX_THRESHOLD_NT = 1500000d;
	public static final Double PAYROLL_TAX_THRESHOLD_ACT = 2000000d;
	public static final Double PAYROLL_TAX_RATE_NSW = 0.0545d;
	public static final Double PAYROLL_TAX_RATE_VIC_REGIONAL = 0.02425d;
	public static final Double PAYROLL_TAX_RATE_VIC_CAPITAL_CITY = 0.0485d;
	public static final Double PAYROLL_TAX_RATE_QLD = 0.0475d;
	public static final Double PAYROLL_TAX_RATE_SA = 0.0495d;
	public static final Double PAYROLL_TAX_RATE_WA = 0.055d;
	public static final Double PAYROLL_TAX_RATE_TAS1 = 0.04d;
	public static final Double PAYROLL_TAX_RATE_TAS2 = 0.061d;
	public static final Double PAYROLL_TAX_RATE_NT = 0.055d;
	public static final Double PAYROLL_TAX_RATE_ACT = 0.0685d;

	public static final Double INCOME_TAX_THRESHOLD_1 = 0d;
	public static final Double INCOME_TAX_THRESHOLD_2 = 18201d;
	public static final Double INCOME_TAX_THRESHOLD_3 = 21981d;
	public static final Double INCOME_TAX_THRESHOLD_4 = 27476d;
	public static final Double INCOME_TAX_THRESHOLD_5 = 37001d;
	public static final Double INCOME_TAX_THRESHOLD_6 = 66668d;
	public static final Double INCOME_TAX_THRESHOLD_7 = 90001d;
	public static final Double INCOME_TAX_THRESHOLD_8 = 180001d;
	public static final Double INCOME_TAX_BASE_AMT_1 = 0d;
	public static final Double INCOME_TAX_BASE_AMT_2 = 0d;
	public static final Double INCOME_TAX_BASE_AMT_3 = 0d;
	public static final Double INCOME_TAX_BASE_AMT_4 = 0d;
	public static final Double INCOME_TAX_BASE_AMT_5 = 3572d;
	public static final Double INCOME_TAX_BASE_AMT_6 = 13213.78d;
	public static final Double INCOME_TAX_BASE_AMT_7 = 20797d;
	public static final Double INCOME_TAX_BASE_AMT_8 = 54097d;
	public static final Double INCOME_TAX_RATE_1 = 0d;
	public static final Double INCOME_TAX_RATE_2 = 0.19d;
	public static final Double INCOME_TAX_RATE_3 = 0.19d;
	public static final Double INCOME_TAX_RATE_4 = 0.19d;
	public static final Double INCOME_TAX_RATE_5 = 0.325d;
	public static final Double INCOME_TAX_RATE_6 = 0.325d;
	public static final Double INCOME_TAX_RATE_7 = 0.37d;
	public static final Double INCOME_TAX_RATE_8 = 0.45d;
	public static final Double MEDICARE_LEVY_RATE_1 = 0d;
	public static final Double MEDICARE_LEVY_RATE_2 = 0d;
	public static final Double MEDICARE_LEVY_RATE_3 = 0.01d;
	public static final Double MEDICARE_LEVY_RATE_4 = 0.02d;
	public static final Double MEDICARE_LEVY_RATE_5 = 0.02d;
	public static final Double MEDICARE_LEVY_RATE_6 = 0.02d;
	public static final Double MEDICARE_LEVY_RATE_7 = 0.02d;
	public static final Double MEDICARE_LEVY_RATE_8 = 0.02d;
	public static final Double MEDICARE_LEVY_BASE_AMT_1 = 0d;
	public static final Double MEDICARE_LEVY_BASE_AMT_2 = MEDICARE_LEVY_BASE_AMT_1
			+ (INCOME_TAX_THRESHOLD_2 - INCOME_TAX_THRESHOLD_1) * MEDICARE_LEVY_RATE_1;
	public static final Double MEDICARE_LEVY_BASE_AMT_3 = MEDICARE_LEVY_BASE_AMT_2
			+ (INCOME_TAX_THRESHOLD_3 - INCOME_TAX_THRESHOLD_2) * MEDICARE_LEVY_RATE_2;
	public static final Double MEDICARE_LEVY_BASE_AMT_4 = MEDICARE_LEVY_BASE_AMT_3
			+ (INCOME_TAX_THRESHOLD_4 - INCOME_TAX_THRESHOLD_3) * MEDICARE_LEVY_RATE_3;
	public static final Double MEDICARE_LEVY_BASE_AMT_5 = MEDICARE_LEVY_BASE_AMT_4
			+ (INCOME_TAX_THRESHOLD_5 - INCOME_TAX_THRESHOLD_4) * MEDICARE_LEVY_RATE_4;
	public static final Double MEDICARE_LEVY_BASE_AMT_6 = MEDICARE_LEVY_BASE_AMT_5
			+ (INCOME_TAX_THRESHOLD_6 - INCOME_TAX_THRESHOLD_5) * MEDICARE_LEVY_RATE_5;
	public static final Double MEDICARE_LEVY_BASE_AMT_7 = MEDICARE_LEVY_BASE_AMT_6
			+ (INCOME_TAX_THRESHOLD_7 - INCOME_TAX_THRESHOLD_6) * MEDICARE_LEVY_RATE_6;
	public static final Double MEDICARE_LEVY_BASE_AMT_8 = MEDICARE_LEVY_BASE_AMT_7
			+ (INCOME_TAX_THRESHOLD_8 - INCOME_TAX_THRESHOLD_7) * MEDICARE_LEVY_RATE_7;
	public static final Double LOW_INCOME_TAX_OFFSET_AMT_1 = 445d;
	public static final Double LOW_INCOME_TAX_OFFSET_AMT_2 = 445d;
	public static final Double LOW_INCOME_TAX_OFFSET_AMT_3 = 445d;
	public static final Double LOW_INCOME_TAX_OFFSET_AMT_4 = 445d;
	public static final Double LOW_INCOME_TAX_OFFSET_AMT_5 = 445d;
	public static final Double LOW_INCOME_TAX_OFFSET_AMT_6 = 0d;
	public static final Double LOW_INCOME_TAX_OFFSET_AMT_7 = 0d;
	public static final Double LOW_INCOME_TAX_OFFSET_AMT_8 = 0d;
	public static final Double LOW_INCOME_TAX_OFFSET_REDUCTION_RATE_1 = 0d;
	public static final Double LOW_INCOME_TAX_OFFSET_REDUCTION_RATE_2 = 0d;
	public static final Double LOW_INCOME_TAX_OFFSET_REDUCTION_RATE_3 = 0d;
	public static final Double LOW_INCOME_TAX_OFFSET_REDUCTION_RATE_4 = 0d;
	public static final Double LOW_INCOME_TAX_OFFSET_REDUCTION_RATE_5 = 0.015d;
	public static final Double LOW_INCOME_TAX_OFFSET_REDUCTION_RATE_6 = 0d;
	public static final Double LOW_INCOME_TAX_OFFSET_REDUCTION_RATE_7 = 0d;
	public static final Double LOW_INCOME_TAX_OFFSET_REDUCTION_RATE_8 = 0d;

	// SOURCE:
	// https://www.ato.gov.au/rates/help,-tsl-and-sfss-repayment-thresholds-and-rates/?page=1
	public static final Double HELP_THRESHOLD_1 = 0d;
	public static final Double HELP_THRESHOLD_2 = 51957d;
	public static final Double HELP_THRESHOLD_3 = 57730d;
	public static final Double HELP_THRESHOLD_4 = 64307d;
	public static final Double HELP_THRESHOLD_5 = 70882d;
	public static final Double HELP_THRESHOLD_6 = 74608d;
	public static final Double HELP_THRESHOLD_7 = 80198d;
	public static final Double HELP_THRESHOLD_8 = 86856d;
	public static final Double HELP_THRESHOLD_9 = 91426d;
	public static final Double HELP_THRESHOLD_10 = 100614d;
	public static final Double HELP_THRESHOLD_11 = 107214d;
	public static final Double HELP_RATE_1 = 0d;
	public static final Double HELP_RATE_2 = 0.02d;
	public static final Double HELP_RATE_3 = 0.04d;
	public static final Double HELP_RATE_4 = 0.045d;
	public static final Double HELP_RATE_5 = 0.05d;
	public static final Double HELP_RATE_6 = 0.055d;
	public static final Double HELP_RATE_7 = 0.06d;
	public static final Double HELP_RATE_8 = 0.065d;
	public static final Double HELP_RATE_9 = 0.07d;
	public static final Double HELP_RATE_10 = 0.075d;
	public static final Double HELP_RATE_11 = 0.08d;
	public static final Double HELP_BASE_AMT_1 = 0d;
	public static final Double HELP_BASE_AMT_2 = HELP_BASE_AMT_1 + (HELP_THRESHOLD_2 - HELP_THRESHOLD_1) * HELP_RATE_1;
	public static final Double HELP_BASE_AMT_3 = HELP_BASE_AMT_2 + (HELP_THRESHOLD_3 - HELP_THRESHOLD_2) * HELP_RATE_2;
	public static final Double HELP_BASE_AMT_4 = HELP_BASE_AMT_3 + (HELP_THRESHOLD_4 - HELP_THRESHOLD_3) * HELP_RATE_3;
	public static final Double HELP_BASE_AMT_5 = HELP_BASE_AMT_4 + (HELP_THRESHOLD_5 - HELP_THRESHOLD_4) * HELP_RATE_4;
	public static final Double HELP_BASE_AMT_6 = HELP_BASE_AMT_5 + (HELP_THRESHOLD_6 - HELP_THRESHOLD_5) * HELP_RATE_5;
	public static final Double HELP_BASE_AMT_7 = HELP_BASE_AMT_6 + (HELP_THRESHOLD_7 - HELP_THRESHOLD_6) * HELP_RATE_6;
	public static final Double HELP_BASE_AMT_8 = HELP_BASE_AMT_7 + (HELP_THRESHOLD_8 - HELP_THRESHOLD_7) * HELP_RATE_7;
	public static final Double HELP_BASE_AMT_9 = HELP_BASE_AMT_8 + (HELP_THRESHOLD_9 - HELP_THRESHOLD_8) * HELP_RATE_8;
	public static final Double HELP_BASE_AMT_10 = HELP_BASE_AMT_9
			+ (HELP_THRESHOLD_10 - HELP_THRESHOLD_9) * HELP_RATE_9;
	public static final Double HELP_BASE_AMT_11 = HELP_BASE_AMT_10
			+ (HELP_THRESHOLD_11 - HELP_THRESHOLD_10) * HELP_RATE_10;

	/**
	 * 
	 */
	public Tax() {
		super();
	}

	/**
	 * Calculates a Business's company income tax expense for a single month.
	 * Assumes all businesses are companies - not sole traders whose income tax
	 * falls under the individual marginal tax rates.
	 * 
	 * @param grossRevenue  - the business's monthly gross revenue
	 * @param taxableIncome - the business's monthly taxable income
	 * @return the amount of tax due to the ATO (i.e. AustralianGovernment)
	 */
	public static double calculateCompanyTax(double monthlyGrossRevenue, double monthlyTaxableIncome) {
		// convert to annual equivalent for easier calculation
		double grossRevenue = monthlyGrossRevenue * NUM_MONTHS;
		double taxableIncome = monthlyTaxableIncome * NUM_MONTHS;

		// calculate tax using marginal tax rates
		double taxRate = grossRevenue < COMPANY_TAX_THRESHOLD ? COMPANY_TAX_RATE_SMALL : COMPANY_TAX_RATE_LARGE;
		double taxPayable = taxableIncome * taxRate;

		// convert annual tax amount back to monthly equivalent
		return taxPayable / NUM_MONTHS;
	}

	/**
	 * Calculates a Business's payroll tax expense for a single month.
	 * 
	 * @param monthlyWages  - the business's monthly wages expense
	 * @param state         - the business's primary state
	 * @param isCapitalCity - true if the business is in a Greater Capital City
	 *                      Statistical Area, false otherwise.
	 * @return the amount of tax due to the ATO (i.e. AustralianGovernment)
	 */
	public static double calculatePayrollTax(double monthlyWages, String state, boolean isCapitalCity) {
		// convert to annual equivalent for easier calculation
		double wages = monthlyWages * NUM_MONTHS;

		// calculate tax using marginal tax rates
		double taxPayable = 0d;
		switch (state) {
		case "NSW":
			taxPayable = wages > PAYROLL_TAX_THRESHOLD_NSW ? (wages - PAYROLL_TAX_THRESHOLD_NSW) * PAYROLL_TAX_RATE_NSW
					: 0d;
			break;
		case "VIC":
			if (isCapitalCity) {
				taxPayable = wages > PAYROLL_TAX_THRESHOLD_VIC
						? (wages - PAYROLL_TAX_THRESHOLD_VIC) * PAYROLL_TAX_RATE_VIC_CAPITAL_CITY
						: 0d;
			} else {
				// regional business
				taxPayable = wages > PAYROLL_TAX_THRESHOLD_VIC
						? (wages - PAYROLL_TAX_THRESHOLD_VIC) * PAYROLL_TAX_RATE_VIC_REGIONAL
						: 0d;
			}
			break;
		case "QLD":
			taxPayable = wages > PAYROLL_TAX_THRESHOLD_QLD ? (wages - PAYROLL_TAX_THRESHOLD_QLD) * PAYROLL_TAX_RATE_QLD
					: 0d;
			break;
		case "SA":
			taxPayable = wages > PAYROLL_TAX_THRESHOLD_SA ? (wages - PAYROLL_TAX_THRESHOLD_SA) * PAYROLL_TAX_RATE_SA
					: 0d;
			break;
		case "WA":
			taxPayable = wages > PAYROLL_TAX_THRESHOLD_WA ? (wages - PAYROLL_TAX_THRESHOLD_WA) * PAYROLL_TAX_RATE_WA
					: 0d;
			break;
		case "TAS":
			if (wages > PAYROLL_TAX_THRESHOLD_TAS2) {
				taxPayable = (wages - PAYROLL_TAX_THRESHOLD_TAS2) * PAYROLL_TAX_RATE_TAS2
						+ (PAYROLL_TAX_THRESHOLD_TAS2 - PAYROLL_TAX_THRESHOLD_TAS1) * PAYROLL_TAX_RATE_TAS1;
			} else if (wages > PAYROLL_TAX_THRESHOLD_TAS1) {
				taxPayable = (wages - PAYROLL_TAX_THRESHOLD_TAS1) * PAYROLL_TAX_RATE_TAS1;
			}
			break;
		case "NT":
			taxPayable = wages > PAYROLL_TAX_THRESHOLD_NT ? (wages - PAYROLL_TAX_THRESHOLD_NT) * PAYROLL_TAX_RATE_NT
					: 0d;
			break;
		case "ACT":
			taxPayable = wages > PAYROLL_TAX_THRESHOLD_ACT ? (wages - PAYROLL_TAX_THRESHOLD_ACT) * PAYROLL_TAX_RATE_ACT
					: 0d;
			break;
		case "Other":
			taxPayable = 0d;
			break;
		}

		// convert annual tax amount back to monthly equivalent
		return taxPayable / NUM_MONTHS;
	}

	/**
	 * Calculates and Individual's tax expense for a single month.
	 * 
	 * SOURCES:<br>
	 * https://www.ato.gov.au/rates/individual-income-tax-rates/<br>
	 * https://www.ato.gov.au/Individuals/Medicare-levy/<br>
	 * https://www.ato.gov.au/individuals/medicare-levy/medicare-levy-reduction-for-low-income-earners/<br>
	 * https://www.ato.gov.au/individuals/income-and-deductions/offsets-and-rebates/low-income-earners/<br>
	 * 
	 * @param taxableIncomePerMonth - the individual's taxable income, measured on a
	 *                              monthly basis
	 * @return the amount of tax due to the ATO (i.e. AustralianGovernment)
	 */
	public static double calculateIndividualIncomeTax(double taxableIncomePerMonth) {
		// convert to annual equivalent for easier calculation
		double taxableIncome = taxableIncomePerMonth * NUM_MONTHS;

		// calculate tax using marginal tax rates
		double incomeTax = 0d;
		if (taxableIncome > INCOME_TAX_THRESHOLD_8) {
			incomeTax = INCOME_TAX_BASE_AMT_8 + MEDICARE_LEVY_BASE_AMT_8
					+ (taxableIncome - INCOME_TAX_THRESHOLD_8) * (INCOME_TAX_RATE_8 + MEDICARE_LEVY_RATE_8)
					- LOW_INCOME_TAX_OFFSET_AMT_8
					+ (taxableIncome - INCOME_TAX_THRESHOLD_8) * LOW_INCOME_TAX_OFFSET_REDUCTION_RATE_8;
		} else if (taxableIncome > INCOME_TAX_THRESHOLD_7) {
			incomeTax = INCOME_TAX_BASE_AMT_7 + MEDICARE_LEVY_BASE_AMT_7
					+ (taxableIncome - INCOME_TAX_THRESHOLD_7) * (INCOME_TAX_RATE_7 + MEDICARE_LEVY_RATE_7)
					- LOW_INCOME_TAX_OFFSET_AMT_7
					+ (taxableIncome - INCOME_TAX_THRESHOLD_7) * LOW_INCOME_TAX_OFFSET_REDUCTION_RATE_7;
		} else if (taxableIncome > INCOME_TAX_THRESHOLD_6) {
			incomeTax = INCOME_TAX_BASE_AMT_6 + MEDICARE_LEVY_BASE_AMT_6
					+ (taxableIncome - INCOME_TAX_THRESHOLD_6) * (INCOME_TAX_RATE_6 + MEDICARE_LEVY_RATE_6)
					- LOW_INCOME_TAX_OFFSET_AMT_6
					+ (taxableIncome - INCOME_TAX_THRESHOLD_6) * LOW_INCOME_TAX_OFFSET_REDUCTION_RATE_6;
		} else if (taxableIncome > INCOME_TAX_THRESHOLD_5) {
			incomeTax = INCOME_TAX_BASE_AMT_5 + MEDICARE_LEVY_BASE_AMT_5
					+ (taxableIncome - INCOME_TAX_THRESHOLD_5) * (INCOME_TAX_RATE_5 + MEDICARE_LEVY_RATE_5)
					- LOW_INCOME_TAX_OFFSET_AMT_5
					+ (taxableIncome - INCOME_TAX_THRESHOLD_5) * LOW_INCOME_TAX_OFFSET_REDUCTION_RATE_5;
		} else if (taxableIncome > INCOME_TAX_THRESHOLD_4) {
			incomeTax = INCOME_TAX_BASE_AMT_4 + MEDICARE_LEVY_BASE_AMT_4
					+ (taxableIncome - INCOME_TAX_THRESHOLD_4) * (INCOME_TAX_RATE_4 + MEDICARE_LEVY_RATE_4)
					- LOW_INCOME_TAX_OFFSET_AMT_4
					+ (taxableIncome - INCOME_TAX_THRESHOLD_4) * LOW_INCOME_TAX_OFFSET_REDUCTION_RATE_4;
		} else if (taxableIncome > INCOME_TAX_THRESHOLD_3) {
			incomeTax = INCOME_TAX_BASE_AMT_3 + MEDICARE_LEVY_BASE_AMT_3
					+ (taxableIncome - INCOME_TAX_THRESHOLD_3) * (INCOME_TAX_RATE_3 + MEDICARE_LEVY_RATE_3)
					- LOW_INCOME_TAX_OFFSET_AMT_3
					+ (taxableIncome - INCOME_TAX_THRESHOLD_3) * LOW_INCOME_TAX_OFFSET_REDUCTION_RATE_3;
		} else if (taxableIncome > INCOME_TAX_THRESHOLD_2) {
			incomeTax = INCOME_TAX_BASE_AMT_2 + MEDICARE_LEVY_BASE_AMT_2
					+ (taxableIncome - INCOME_TAX_THRESHOLD_2) * (INCOME_TAX_RATE_2 + MEDICARE_LEVY_RATE_2)
					- LOW_INCOME_TAX_OFFSET_AMT_2
					+ (taxableIncome - INCOME_TAX_THRESHOLD_2) * LOW_INCOME_TAX_OFFSET_REDUCTION_RATE_2;
		} else if (taxableIncome > INCOME_TAX_THRESHOLD_1) {
			incomeTax = INCOME_TAX_BASE_AMT_1 + MEDICARE_LEVY_BASE_AMT_1
					+ (taxableIncome - INCOME_TAX_THRESHOLD_1) * (INCOME_TAX_RATE_1 + MEDICARE_LEVY_RATE_1)
					- LOW_INCOME_TAX_OFFSET_AMT_1
					+ (taxableIncome - INCOME_TAX_THRESHOLD_1) * LOW_INCOME_TAX_OFFSET_REDUCTION_RATE_1;
		} else {
			incomeTax = 0d;
		}

		// convert annual tax amount back to monthly equivalent
		return incomeTax / NUM_MONTHS;
	}

	/**
	 * Calculates and Individual's student loan repayments expense for a single
	 * month.
	 * 
	 * SOURCE:<br>
	 * https://www.ato.gov.au/rates/help,-tsl-and-sfss-repayment-thresholds-and-rates/?page=1<br>
	 * 
	 * @param taxableIncomePerMonth - the individual's taxable income, measured on a
	 *                              monthly basis
	 * @return the monthly student loan repayment due to the ATO (i.e.
	 *         AustralianGovernment)
	 */
	public static double calculateStudentLoanRepayments(double taxableIncomePerMonth) {
		// convert to annual equivalent for easier calculation
		double taxableIncome = taxableIncomePerMonth * NUM_MONTHS;

		// calculate student loan repayment using marginal rates
		double repayment = 0d;
		if (taxableIncome > HELP_THRESHOLD_11) {
			repayment = HELP_BASE_AMT_11 + (taxableIncome - HELP_THRESHOLD_11) * HELP_RATE_11;
		} else if (taxableIncome > HELP_THRESHOLD_10) {
			repayment = HELP_BASE_AMT_10 + (taxableIncome - HELP_THRESHOLD_10) * HELP_RATE_10;
		} else if (taxableIncome > HELP_THRESHOLD_9) {
			repayment = HELP_BASE_AMT_9 + (taxableIncome - HELP_THRESHOLD_9) * HELP_RATE_9;
		} else if (taxableIncome > HELP_THRESHOLD_8) {
			repayment = HELP_BASE_AMT_8 + (taxableIncome - HELP_THRESHOLD_8) * HELP_RATE_8;
		} else if (taxableIncome > HELP_THRESHOLD_7) {
			repayment = HELP_BASE_AMT_7 + (taxableIncome - HELP_THRESHOLD_7) * HELP_RATE_7;
		} else if (taxableIncome > HELP_THRESHOLD_6) {
			repayment = HELP_BASE_AMT_6 + (taxableIncome - HELP_THRESHOLD_6) * HELP_RATE_6;
		} else if (taxableIncome > HELP_THRESHOLD_5) {
			repayment = HELP_BASE_AMT_5 + (taxableIncome - HELP_THRESHOLD_5) * HELP_RATE_5;
		} else if (taxableIncome > HELP_THRESHOLD_4) {
			repayment = HELP_BASE_AMT_4 + (taxableIncome - HELP_THRESHOLD_4) * HELP_RATE_4;
		} else if (taxableIncome > HELP_THRESHOLD_3) {
			repayment = HELP_BASE_AMT_3 + (taxableIncome - HELP_THRESHOLD_3) * HELP_RATE_3;
		} else if (taxableIncome > HELP_THRESHOLD_2) {
			repayment = HELP_BASE_AMT_2 + (taxableIncome - HELP_THRESHOLD_2) * HELP_RATE_2;
		} else if (taxableIncome > HELP_THRESHOLD_1) {
			repayment = HELP_BASE_AMT_1 + (taxableIncome - HELP_THRESHOLD_1) * HELP_RATE_1;
		} else {
			repayment = 0d;
		}

		// convert annual repayment amount back to monthly equivalent
		return repayment / NUM_MONTHS;
	}
}
