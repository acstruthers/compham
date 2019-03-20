/**
 * 
 */
package xyz.struthers.rhul.ham.process;

/**
 * @author Adam Struthers
 * @since 19-Feb-2019
 */
public abstract class Tax {

	private static final float NUM_MONTHS = 12f;

	public static final float COMPANY_TAX_THRESHOLD = 50000000f;
	public static final float COMPANY_TAX_RATE_SMALL = 0.275f;
	public static final float COMPANY_TAX_RATE_LARGE = 0.30f;

	public static final float PAYROLL_TAX_THRESHOLD_NSW = 850000f;
	public static final float PAYROLL_TAX_THRESHOLD_VIC = 650000f;
	public static final float PAYROLL_TAX_THRESHOLD_QLD = 1100000f;
	public static final float PAYROLL_TAX_THRESHOLD_SA = 1500000f;
	public static final float PAYROLL_TAX_THRESHOLD_WA = 850000f;
	public static final float PAYROLL_TAX_THRESHOLD_TAS1 = 1250000f;
	public static final float PAYROLL_TAX_THRESHOLD_TAS2 = 2000000f;
	public static final float PAYROLL_TAX_THRESHOLD_NT = 1500000f;
	public static final float PAYROLL_TAX_THRESHOLD_ACT = 2000000f;
	public static final float PAYROLL_TAX_RATE_NSW = 0.0545f;
	public static final float PAYROLL_TAX_RATE_VIC_REGIONAL = 0.02425f;
	public static final float PAYROLL_TAX_RATE_VIC_CAPITAL_CITY = 0.0485f;
	public static final float PAYROLL_TAX_RATE_QLD = 0.0475f;
	public static final float PAYROLL_TAX_RATE_SA = 0.0495f;
	public static final float PAYROLL_TAX_RATE_WA = 0.055f;
	public static final float PAYROLL_TAX_RATE_TAS1 = 0.04f;
	public static final float PAYROLL_TAX_RATE_TAS2 = 0.061f;
	public static final float PAYROLL_TAX_RATE_NT = 0.055f;
	public static final float PAYROLL_TAX_RATE_ACT = 0.0685f;

	public static final float INCOME_TAX_THRESHOLD_1 = 0f;
	public static final float INCOME_TAX_THRESHOLD_2 = 18201f;
	public static final float INCOME_TAX_THRESHOLD_3 = 21981f;
	public static final float INCOME_TAX_THRESHOLD_4 = 27476f;
	public static final float INCOME_TAX_THRESHOLD_5 = 37001f;
	public static final float INCOME_TAX_THRESHOLD_6 = 66668f;
	public static final float INCOME_TAX_THRESHOLD_7 = 90001f;
	public static final float INCOME_TAX_THRESHOLD_8 = 180001f;
	public static final float INCOME_TAX_BASE_AMT_1 = 0f;
	public static final float INCOME_TAX_BASE_AMT_2 = 0f;
	public static final float INCOME_TAX_BASE_AMT_3 = 0f;
	public static final float INCOME_TAX_BASE_AMT_4 = 0f;
	public static final float INCOME_TAX_BASE_AMT_5 = 3572f;
	public static final float INCOME_TAX_BASE_AMT_6 = 13213.78f;
	public static final float INCOME_TAX_BASE_AMT_7 = 20797f;
	public static final float INCOME_TAX_BASE_AMT_8 = 54097f;
	public static final float INCOME_TAX_RATE_1 = 0f;
	public static final float INCOME_TAX_RATE_2 = 0.19f;
	public static final float INCOME_TAX_RATE_3 = 0.19f;
	public static final float INCOME_TAX_RATE_4 = 0.19f;
	public static final float INCOME_TAX_RATE_5 = 0.325f;
	public static final float INCOME_TAX_RATE_6 = 0.325f;
	public static final float INCOME_TAX_RATE_7 = 0.37f;
	public static final float INCOME_TAX_RATE_8 = 0.45f;
	public static final float MEDICARE_LEVY_RATE_1 = 0f;
	public static final float MEDICARE_LEVY_RATE_2 = 0f;
	public static final float MEDICARE_LEVY_RATE_3 = 0.01f;
	public static final float MEDICARE_LEVY_RATE_4 = 0.02f;
	public static final float MEDICARE_LEVY_RATE_5 = 0.02f;
	public static final float MEDICARE_LEVY_RATE_6 = 0.02f;
	public static final float MEDICARE_LEVY_RATE_7 = 0.02f;
	public static final float MEDICARE_LEVY_RATE_8 = 0.02f;
	public static final float MEDICARE_LEVY_BASE_AMT_1 = 0f;
	public static final float MEDICARE_LEVY_BASE_AMT_2 = MEDICARE_LEVY_BASE_AMT_1
			+ (INCOME_TAX_THRESHOLD_2 - INCOME_TAX_THRESHOLD_1) * MEDICARE_LEVY_RATE_1;
	public static final float MEDICARE_LEVY_BASE_AMT_3 = MEDICARE_LEVY_BASE_AMT_2
			+ (INCOME_TAX_THRESHOLD_3 - INCOME_TAX_THRESHOLD_2) * MEDICARE_LEVY_RATE_2;
	public static final float MEDICARE_LEVY_BASE_AMT_4 = MEDICARE_LEVY_BASE_AMT_3
			+ (INCOME_TAX_THRESHOLD_4 - INCOME_TAX_THRESHOLD_3) * MEDICARE_LEVY_RATE_3;
	public static final float MEDICARE_LEVY_BASE_AMT_5 = MEDICARE_LEVY_BASE_AMT_4
			+ (INCOME_TAX_THRESHOLD_5 - INCOME_TAX_THRESHOLD_4) * MEDICARE_LEVY_RATE_4;
	public static final float MEDICARE_LEVY_BASE_AMT_6 = MEDICARE_LEVY_BASE_AMT_5
			+ (INCOME_TAX_THRESHOLD_6 - INCOME_TAX_THRESHOLD_5) * MEDICARE_LEVY_RATE_5;
	public static final float MEDICARE_LEVY_BASE_AMT_7 = MEDICARE_LEVY_BASE_AMT_6
			+ (INCOME_TAX_THRESHOLD_7 - INCOME_TAX_THRESHOLD_6) * MEDICARE_LEVY_RATE_6;
	public static final float MEDICARE_LEVY_BASE_AMT_8 = MEDICARE_LEVY_BASE_AMT_7
			+ (INCOME_TAX_THRESHOLD_8 - INCOME_TAX_THRESHOLD_7) * MEDICARE_LEVY_RATE_7;
	public static final float LOW_INCOME_TAX_OFFSET_AMT_1 = 445f;
	public static final float LOW_INCOME_TAX_OFFSET_AMT_2 = 445f;
	public static final float LOW_INCOME_TAX_OFFSET_AMT_3 = 445f;
	public static final float LOW_INCOME_TAX_OFFSET_AMT_4 = 445f;
	public static final float LOW_INCOME_TAX_OFFSET_AMT_5 = 445f;
	public static final float LOW_INCOME_TAX_OFFSET_AMT_6 = 0f;
	public static final float LOW_INCOME_TAX_OFFSET_AMT_7 = 0f;
	public static final float LOW_INCOME_TAX_OFFSET_AMT_8 = 0f;
	public static final float LOW_INCOME_TAX_OFFSET_REDUCTION_RATE_1 = 0f;
	public static final float LOW_INCOME_TAX_OFFSET_REDUCTION_RATE_2 = 0f;
	public static final float LOW_INCOME_TAX_OFFSET_REDUCTION_RATE_3 = 0f;
	public static final float LOW_INCOME_TAX_OFFSET_REDUCTION_RATE_4 = 0f;
	public static final float LOW_INCOME_TAX_OFFSET_REDUCTION_RATE_5 = 0.015f;
	public static final float LOW_INCOME_TAX_OFFSET_REDUCTION_RATE_6 = 0f;
	public static final float LOW_INCOME_TAX_OFFSET_REDUCTION_RATE_7 = 0f;
	public static final float LOW_INCOME_TAX_OFFSET_REDUCTION_RATE_8 = 0f;

	// SOURCE:
	// https://www.ato.gov.au/rates/help,-tsl-and-sfss-repayment-thresholds-and-rates/?page=1
	public static final float HELP_THRESHOLD_1 = 0f;
	public static final float HELP_THRESHOLD_2 = 51957f;
	public static final float HELP_THRESHOLD_3 = 57730f;
	public static final float HELP_THRESHOLD_4 = 64307f;
	public static final float HELP_THRESHOLD_5 = 70882f;
	public static final float HELP_THRESHOLD_6 = 74608f;
	public static final float HELP_THRESHOLD_7 = 80198f;
	public static final float HELP_THRESHOLD_8 = 86856f;
	public static final float HELP_THRESHOLD_9 = 91426f;
	public static final float HELP_THRESHOLD_10 = 100614f;
	public static final float HELP_THRESHOLD_11 = 107214f;
	public static final float HELP_RATE_1 = 0f;
	public static final float HELP_RATE_2 = 0.02f;
	public static final float HELP_RATE_3 = 0.04f;
	public static final float HELP_RATE_4 = 0.045f;
	public static final float HELP_RATE_5 = 0.05f;
	public static final float HELP_RATE_6 = 0.055f;
	public static final float HELP_RATE_7 = 0.06f;
	public static final float HELP_RATE_8 = 0.065f;
	public static final float HELP_RATE_9 = 0.07f;
	public static final float HELP_RATE_10 = 0.075f;
	public static final float HELP_RATE_11 = 0.08f;
	public static final float HELP_BASE_AMT_1 = 0f;
	public static final float HELP_BASE_AMT_2 = HELP_BASE_AMT_1 + (HELP_THRESHOLD_2 - HELP_THRESHOLD_1) * HELP_RATE_1;
	public static final float HELP_BASE_AMT_3 = HELP_BASE_AMT_2 + (HELP_THRESHOLD_3 - HELP_THRESHOLD_2) * HELP_RATE_2;
	public static final float HELP_BASE_AMT_4 = HELP_BASE_AMT_3 + (HELP_THRESHOLD_4 - HELP_THRESHOLD_3) * HELP_RATE_3;
	public static final float HELP_BASE_AMT_5 = HELP_BASE_AMT_4 + (HELP_THRESHOLD_5 - HELP_THRESHOLD_4) * HELP_RATE_4;
	public static final float HELP_BASE_AMT_6 = HELP_BASE_AMT_5 + (HELP_THRESHOLD_6 - HELP_THRESHOLD_5) * HELP_RATE_5;
	public static final float HELP_BASE_AMT_7 = HELP_BASE_AMT_6 + (HELP_THRESHOLD_7 - HELP_THRESHOLD_6) * HELP_RATE_6;
	public static final float HELP_BASE_AMT_8 = HELP_BASE_AMT_7 + (HELP_THRESHOLD_8 - HELP_THRESHOLD_7) * HELP_RATE_7;
	public static final float HELP_BASE_AMT_9 = HELP_BASE_AMT_8 + (HELP_THRESHOLD_9 - HELP_THRESHOLD_8) * HELP_RATE_8;
	public static final float HELP_BASE_AMT_10 = HELP_BASE_AMT_9 + (HELP_THRESHOLD_10 - HELP_THRESHOLD_9) * HELP_RATE_9;
	public static final Float HELP_BASE_AMT_11 = HELP_BASE_AMT_10
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
	public static float calculateCompanyTax(float monthlyGrossRevenue, float monthlyTaxableIncome) {
		// convert to annual equivalent for easier calculation
		float grossRevenue = monthlyGrossRevenue * NUM_MONTHS;
		float taxableIncome = monthlyTaxableIncome * NUM_MONTHS;

		// calculate tax using marginal tax rates
		float taxRate = grossRevenue < COMPANY_TAX_THRESHOLD ? COMPANY_TAX_RATE_SMALL : COMPANY_TAX_RATE_LARGE;
		float taxPayable = taxableIncome * taxRate;

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
	public static float calculatePayrollTax(float monthlyWages, String state, boolean isCapitalCity) {
		// convert to annual equivalent for easier calculation
		float wages = monthlyWages * NUM_MONTHS;

		// calculate tax using marginal tax rates
		float taxPayable = 0f;
		switch (state) {
		case "NSW":
			taxPayable = wages > PAYROLL_TAX_THRESHOLD_NSW ? (wages - PAYROLL_TAX_THRESHOLD_NSW) * PAYROLL_TAX_RATE_NSW
					: 0f;
			break;
		case "VIC":
			if (isCapitalCity) {
				taxPayable = wages > PAYROLL_TAX_THRESHOLD_VIC
						? (wages - PAYROLL_TAX_THRESHOLD_VIC) * PAYROLL_TAX_RATE_VIC_CAPITAL_CITY
						: 0f;
			} else {
				// regional business
				taxPayable = wages > PAYROLL_TAX_THRESHOLD_VIC
						? (wages - PAYROLL_TAX_THRESHOLD_VIC) * PAYROLL_TAX_RATE_VIC_REGIONAL
						: 0f;
			}
			break;
		case "QLD":
			taxPayable = wages > PAYROLL_TAX_THRESHOLD_QLD ? (wages - PAYROLL_TAX_THRESHOLD_QLD) * PAYROLL_TAX_RATE_QLD
					: 0f;
			break;
		case "SA":
			taxPayable = wages > PAYROLL_TAX_THRESHOLD_SA ? (wages - PAYROLL_TAX_THRESHOLD_SA) * PAYROLL_TAX_RATE_SA
					: 0f;
			break;
		case "WA":
			taxPayable = wages > PAYROLL_TAX_THRESHOLD_WA ? (wages - PAYROLL_TAX_THRESHOLD_WA) * PAYROLL_TAX_RATE_WA
					: 0f;
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
					: 0f;
			break;
		case "ACT":
			taxPayable = wages > PAYROLL_TAX_THRESHOLD_ACT ? (wages - PAYROLL_TAX_THRESHOLD_ACT) * PAYROLL_TAX_RATE_ACT
					: 0f;
			break;
		case "Other":
			taxPayable = 0f;
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
	public static float calculateIndividualIncomeTax(float taxableIncomePerMonth) {
		// convert to annual equivalent for easier calculation
		float taxableIncome = taxableIncomePerMonth * NUM_MONTHS;

		// calculate tax using marginal tax rates
		float incomeTax = 0f;
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
			incomeTax = 0f;
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
	public static float calculateStudentLoanRepayments(float taxableIncomePerMonth) {
		// convert to annual equivalent for easier calculation
		float taxableIncome = taxableIncomePerMonth * NUM_MONTHS;

		// calculate student loan repayment using marginal rates
		float repayment = 0f;
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
			repayment = 0f;
		}

		// convert annual repayment amount back to monthly equivalent
		return repayment / NUM_MONTHS;
	}
}
