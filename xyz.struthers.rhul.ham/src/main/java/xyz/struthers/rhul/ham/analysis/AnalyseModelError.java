package xyz.struthers.rhul.ham.analysis;

import java.util.Date;

import xyz.struthers.rhul.ham.config.PropertiesXml;
import xyz.struthers.rhul.ham.config.PropertiesXmlFactory;

/**
 * @author Adam Struthers
 * @since 2019-08-12
 */
public class AnalyseModelError {

	private static PropertiesXml properties;

	public AnalyseModelError() {
		super();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		PropertiesXmlFactory.propertiesXmlFilename = "D:\\compham-config\\4.1_baseline.xml";
		properties = PropertiesXmlFactory.getProperties();

		// overwrite existing file
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline_EXOGENEOUS_000.csv");
		processMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\Baseline_EXOGENEOUS_000.csv", "Baseline",
				false, 0);

		// append to file
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline-01_EXOGENEOUS_000.csv");
		processMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\Baseline-01_EXOGENEOUS_000.csv",
				"Baseline-01", true, 0);
	}

	private static void processMetricsCsv(String inFileResourceLocation, String scenario, boolean append,
			int iteration) {

		float meanIncome = 0f;
		float meanExpenses = 0f;
		float housingCostsOver30pc = 0f; // % of households in mtg distress
		float totalIncomePercentEarnedByTop5pc = 0f; // % of assets controlled by top 5% of income (Kumhof, et al, 2015)
		float debtToIncome = 0f; // household debt-to-income ratio (Kumhof, et al, 2015)
		float debtToIncomeTop5pc = 0f;
		float debtToIncomeBottom95pc = 0f;
		float debtToNetWorth = 0f; // household debt-to-net worth ratio (Kumhof, et al, 2015)
		float debtToNetWorthTop5pc = 0f;
		float debtToNetWorthBottom95pc = 0f;
		float wealthPercentHeldByTop5pc = 0f; // share of net worth held by top 5% of income distribution (Kumhof, et
												// al, 2015)
		float wealthPercentHeldByTop1pc = 0f;
		float incomeToHendersonPercent = 0f;

	}

}
