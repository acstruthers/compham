/**
 * 
 */
package xyz.struthers.rhul.ham.process;

/**
 * @author Adam Struthers
 * @since 19-Feb-2019
 */
public class Tax {

	public static final Double MILLION = 1000000d;

	public static final Double COMPANY_TAX_THRESHOLD = 50d * MILLION;
	public static final Double COMPANY_TAX_RATE_SMALL = 0.275d;
	public static final Double COMPANY_TAX_RATE_LARGE = 0.30d;

	/**
	 * 
	 */
	public Tax() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * Calculates company tax expense.
	 * 
	 * @param grossRevenue  - the business's gross revenue
	 * @param taxableIncome - the business's taxable income
	 * @return the amount of tax due to the ATO (i.e. AustralianGovernment)
	 */
	public double calculateCompanyTax(double grossRevenue, double taxableIncome) {
		return taxableIncome * grossRevenue < COMPANY_TAX_THRESHOLD ? COMPANY_TAX_RATE_SMALL : COMPANY_TAX_RATE_LARGE;
	}

	public double calculatePayrollTax(double wages, String state, boolean isGccsa) {
		// TODO: implement payroll tax calcs
		double taxPayable = 0d;
		switch (state) {
		case "NSW":
			taxPayable = wages > 850000d ? wages * 0.0545d : 0d;
			break;
		case "VIC":
			if (isGccsa) {
				// FIXME: check that I have these the right way around
				taxPayable = wages > 650000d ? wages * 0.0485d : 0d;
			} else {
				taxPayable = wages > 650000d ? wages * 0.02425d : 0d;
			}
			break;
		case "QLD":
			taxPayable = wages > 1100000d ? wages * 0.0475d : 0d;
			break;
		case "SA":
			taxPayable = wages > 1500000d ? wages * 0.0495d : 0d;
			break;
		case "WA":
			taxPayable = wages > 850000d ? wages * 0.055d : 0d;
			break;
		case "TAS":
			if (wages > 2000000d) {
				taxPayable = wages * 0.061d;
			} else if (wages > 1250000d) {
				taxPayable = wages * 0.04d;
			}
			break;
		case "NT":
			taxPayable = wages > 1500000d ? wages * 0.055d : 0d;
			break;
		case "ACT":
			taxPayable = wages > 2000000d ? wages * 0.0685d : 0d;
			break;
		case "Other":
			taxPayable = 0d;
			break;
		}
		return taxPayable;
	}

	/**
	 * 
	 * @param taxableIncomePerMonth - the individual's taxable income, measured on a
	 *                              monthly basis
	 * @return
	 */
	public double calculateIndividualIncomeTax(double taxableIncomePerMonth) {
		// TODO: implement individual income tax calcs
		return 0d;
	}
}
