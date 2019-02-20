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
	public static final Double INCOME_TAX_THRESHOLD_6 = 90001d;
	public static final Double INCOME_TAX_THRESHOLD_7 = 180001d;
	public static final Double INCOME_TAX_BASE_AMT_1 = 0d;
	public static final Double INCOME_TAX_BASE_AMT_2 = 0d;
	public static final Double INCOME_TAX_BASE_AMT_3 = 0d;
	public static final Double INCOME_TAX_BASE_AMT_4 = 0d;
	public static final Double INCOME_TAX_BASE_AMT_5 = 3572d;
	public static final Double INCOME_TAX_BASE_AMT_6 = 20797d;
	public static final Double INCOME_TAX_BASE_AMT_7 = 54097d;
	public static final Double INCOME_TAX_RATE_1 = 0d;
	public static final Double INCOME_TAX_RATE_2 = 0.19d;
	public static final Double INCOME_TAX_RATE_3 = 0.19d;
	public static final Double INCOME_TAX_RATE_4 = 0.19d;
	public static final Double INCOME_TAX_RATE_5 = 0.325d;
	public static final Double INCOME_TAX_RATE_6 = 0.325d;
	public static final Double INCOME_TAX_RATE_7 = 0.45d;
	public static final Double MEDICARE_LEVY_RATE_1 = 0d;
	public static final Double MEDICARE_LEVY_RATE_2 = 0d;
	public static final Double MEDICARE_LEVY_RATE_3 = 0.01d;
	public static final Double MEDICARE_LEVY_RATE_4 = 0.02d;
	public static final Double MEDICARE_LEVY_RATE_5 = 0.02d;
	public static final Double MEDICARE_LEVY_RATE_6 = 0.02d;
	public static final Double MEDICARE_LEVY_RATE_7 = 0.02d;
	public static final Double MEDICARE_LEVY_BASE_AMT_1 = 0d;
	public static final Double MEDICARE_LEVY_BASE_AMT_2 = MEDICARE_LEVY_BASE_AMT_1
			+ INCOME_TAX_THRESHOLD_1 * MEDICARE_LEVY_RATE_1;
	public static final Double MEDICARE_LEVY_BASE_AMT_3 = MEDICARE_LEVY_BASE_AMT_2
			+ (INCOME_TAX_THRESHOLD_2 - INCOME_TAX_THRESHOLD_1) * MEDICARE_LEVY_RATE_2;
	public static final Double MEDICARE_LEVY_BASE_AMT_4 = MEDICARE_LEVY_BASE_AMT_3
			+ (INCOME_TAX_THRESHOLD_3 - INCOME_TAX_THRESHOLD_2) * MEDICARE_LEVY_RATE_3;
	public static final Double MEDICARE_LEVY_BASE_AMT_5 = MEDICARE_LEVY_BASE_AMT_4
			+ (INCOME_TAX_THRESHOLD_4 - INCOME_TAX_THRESHOLD_3) * MEDICARE_LEVY_RATE_4;
	public static final Double MEDICARE_LEVY_BASE_AMT_6 = MEDICARE_LEVY_BASE_AMT_5
			+ (INCOME_TAX_THRESHOLD_5 - INCOME_TAX_THRESHOLD_4) * MEDICARE_LEVY_RATE_5;
	public static final Double MEDICARE_LEVY_BASE_AMT_7 = MEDICARE_LEVY_BASE_AMT_6
			+ (INCOME_TAX_THRESHOLD_6 - INCOME_TAX_THRESHOLD_5) * MEDICARE_LEVY_RATE_6;

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
	 * @param taxableIncomePerMonth - the individual's taxable income, measured on a
	 *                              monthly basis
	 * @return the amount of tax due to the ATO (i.e. AustralianGovernment)
	 */
	public static double calculateIndividualIncomeTax(double taxableIncomePerMonth) {
		// convert to annual equivalent for easier calculation
		double taxableIncome = taxableIncomePerMonth * NUM_MONTHS;

		// calculate tax using marginal tax rates
		double incomeTax = 0d;
		if (taxableIncome > INCOME_TAX_THRESHOLD_7) {
			incomeTax = INCOME_TAX_BASE_AMT_7 + MEDICARE_LEVY_BASE_AMT_7
					+ (taxableIncome - INCOME_TAX_BASE_AMT_7) * (INCOME_TAX_RATE_7 + MEDICARE_LEVY_RATE_7);
		} else if (taxableIncome > INCOME_TAX_THRESHOLD_6) {
			incomeTax = INCOME_TAX_BASE_AMT_6 + MEDICARE_LEVY_BASE_AMT_6
					+ (taxableIncome - INCOME_TAX_BASE_AMT_6) * (INCOME_TAX_RATE_6 + MEDICARE_LEVY_RATE_6);
		} else if (taxableIncome > INCOME_TAX_THRESHOLD_5) {
			incomeTax = INCOME_TAX_BASE_AMT_5 + MEDICARE_LEVY_BASE_AMT_5
					+ (taxableIncome - INCOME_TAX_BASE_AMT_5) * (INCOME_TAX_RATE_5 + MEDICARE_LEVY_RATE_5);
		} else if (taxableIncome > INCOME_TAX_THRESHOLD_4) {
			incomeTax = INCOME_TAX_BASE_AMT_4 + MEDICARE_LEVY_BASE_AMT_4
					+ (taxableIncome - INCOME_TAX_BASE_AMT_4) * (INCOME_TAX_RATE_4 + MEDICARE_LEVY_RATE_4);
		} else if (taxableIncome > INCOME_TAX_THRESHOLD_3) {
			incomeTax = INCOME_TAX_BASE_AMT_3 + MEDICARE_LEVY_BASE_AMT_3
					+ (taxableIncome - INCOME_TAX_BASE_AMT_3) * (INCOME_TAX_RATE_3 + MEDICARE_LEVY_RATE_3);
		} else if (taxableIncome > INCOME_TAX_THRESHOLD_2) {
			incomeTax = INCOME_TAX_BASE_AMT_2 + MEDICARE_LEVY_BASE_AMT_2
					+ (taxableIncome - INCOME_TAX_BASE_AMT_2) * (INCOME_TAX_RATE_2 + MEDICARE_LEVY_RATE_2);
		} else if (taxableIncome > INCOME_TAX_THRESHOLD_1) {
			incomeTax = INCOME_TAX_BASE_AMT_1 + MEDICARE_LEVY_BASE_AMT_1
					+ (taxableIncome - INCOME_TAX_BASE_AMT_1) * (INCOME_TAX_RATE_1 + MEDICARE_LEVY_RATE_1);
		} else {
			incomeTax = 0d;
		}

		// convert annual tax amount back to monthly equivalent
		return incomeTax / NUM_MONTHS;
	}
}
