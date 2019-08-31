package xyz.struthers.rhul.ham.analysis;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;

import xyz.struthers.rhul.ham.config.PropertiesXml;
import xyz.struthers.rhul.ham.config.PropertiesXmlFactory;

/**
 * @author Adam Struthers
 * @since 2019-08-12
 */
public class AnalyseMetrics {

	private static PropertiesXml properties;

	public AnalyseMetrics() {
		super();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		PropertiesXmlFactory.propertiesXmlFilename = "D:\\compham-config\\4.1_baseline.xml";
		properties = PropertiesXmlFactory.getProperties();

		// overwrite existing file

		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline_SUMMARY_Household_000.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline_SUMMARY_Household_000.csv", "Baseline",
				false, 0);

		// append to file
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline_SUMMARY_Household_012.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline_SUMMARY_Household_012.csv", "Baseline",
				true, 12);
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline-01_SUMMARY_Household_000.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-01_SUMMARY_Household_000.csv",
				"Baseline-01", true, 0);
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline-01_SUMMARY_Household_012.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-01_SUMMARY_Household_012.csv",
				"Baseline-01", true, 12);
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline-02_SUMMARY_Household_000.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-02_SUMMARY_Household_000.csv",
				"Baseline-02", true, 0);
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline-02_SUMMARY_Household_012.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-02_SUMMARY_Household_012.csv",
				"Baseline-02", true, 12);
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline-03_SUMMARY_Household_000.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-03_SUMMARY_Household_000.csv",
				"Baseline-03", true, 0);
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline-03_SUMMARY_Household_012.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-03_SUMMARY_Household_012.csv",
				"Baseline-03", true, 12);
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline-04_SUMMARY_Household_000.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-04_SUMMARY_Household_000.csv",
				"Baseline-04", true, 0);
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline-04_SUMMARY_Household_012.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-04_SUMMARY_Household_012.csv",
				"Baseline-04", true, 12);
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline-05_SUMMARY_Household_000.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-05_SUMMARY_Household_000.csv",
				"Baseline-05", true, 0);
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline-05_SUMMARY_Household_012.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-05_SUMMARY_Household_012.csv",
				"Baseline-05", true, 12);
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline-06_SUMMARY_Household_000.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-06_SUMMARY_Household_000.csv",
				"Baseline-06", true, 0);
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline-06_SUMMARY_Household_012.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-06_SUMMARY_Household_012.csv",
				"Baseline-06", true, 12);
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline-07_SUMMARY_Household_000.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-07_SUMMARY_Household_000.csv",
				"Baseline-07", true, 0);
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline-07_SUMMARY_Household_012.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-07_SUMMARY_Household_012.csv",
				"Baseline-07", true, 12);
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline-08_SUMMARY_Household_000.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-08_SUMMARY_Household_000.csv",
				"Baseline-08", true, 0);
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline-08_SUMMARY_Household_012.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-08_SUMMARY_Household_012.csv",
				"Baseline-08", true, 12);
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline-09_SUMMARY_Household_000.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-09_SUMMARY_Household_000.csv",
				"Baseline-09", true, 0);
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline-09_SUMMARY_Household_012.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-09_SUMMARY_Household_012.csv",
				"Baseline-09", true, 12);

		// ADI failure
		System.out.println(new Date(System.currentTimeMillis()) + ": processing ADI-CBA_SUMMARY_Household_000.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results\\Summary Data\\ADI-CBA_SUMMARY_Household_000.csv", "ADI-CBA", true,
				0);
		System.out.println(new Date(System.currentTimeMillis()) + ": processing ADI-CBA_SUMMARY_Household_012.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results\\Summary Data\\ADI-CBA_SUMMARY_Household_012.csv", "ADI-CBA", true,
				12);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing ADI-CBA-no-limit_SUMMARY_Household_000.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results\\Summary Data\\ADI-CBA-no-limit_SUMMARY_Household_000.csv",
				"ADI-CBA-no-limit", true, 0);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing ADI-CBA-no-limit_SUMMARY_Household_012.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results\\Summary Data\\ADI-CBA-no-limit_SUMMARY_Household_012.csv",
				"ADI-CBA-no-limit", true, 12);
		System.out.println(new Date(System.currentTimeMillis()) + ": processing ADI-Mutuals_SUMMARY_Household_000.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results\\Summary Data\\ADI-Mutuals_SUMMARY_Household_000.csv",
				"ADI-Mutuals", true, 0);
		System.out.println(new Date(System.currentTimeMillis()) + ": processing ADI-Mutuals_SUMMARY_Household_012.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results\\Summary Data\\ADI-Mutuals_SUMMARY_Household_012.csv",
				"ADI-Mutuals", true, 12);

		// country currency crashes
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing FX-Rates-10pc-CNY_SUMMARY_Household_012.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results\\Summary Data\\FX-Rates-10pc-CNY_SUMMARY_Household_012.csv",
				"FX-Rates-10pc-CNY", true, 12);

		/*
		 * System.out.println( new Date(System.currentTimeMillis()) +
		 * ": processing FX-Rates-10pc-EUR_SUMMARY_Household_012.csv");
		 * processHouseholdMetricsCsv(
		 * "D:\\OneDrive\\Dissertation\\Results\\Summary Data\\FX-Rates-10pc-EUR_SUMMARY_Household_012.csv"
		 * , "FX-Rates-10pc-EUR", true, 12); System.out.println( new
		 * Date(System.currentTimeMillis()) +
		 * ": processing FX-Rates-10pc-INR_SUMMARY_Household_012.csv");
		 * processHouseholdMetricsCsv(
		 * "D:\\OneDrive\\Dissertation\\Results\\Summary Data\\FX-Rates-10pc-INR_SUMMARY_Household_012.csv"
		 * , "FX-Rates-10pc-INR", true, 12); System.out.println( new
		 * Date(System.currentTimeMillis()) +
		 * ": processing FX-Rates-10pc-JPY_SUMMARY_Household_012.csv");
		 * processHouseholdMetricsCsv(
		 * "D:\\OneDrive\\Dissertation\\Results\\Summary Data\\FX-Rates-10pc-JPY_SUMMARY_Household_012.csv"
		 * , "FX-Rates-10pc-JPY", true, 12); System.out.println( new
		 * Date(System.currentTimeMillis()) +
		 * ": processing FX-Rates-10pc-KRW_SUMMARY_Household_012.csv");
		 * processHouseholdMetricsCsv(
		 * "D:\\OneDrive\\Dissertation\\Results\\Summary Data\\FX-Rates-10pc-KRW_SUMMARY_Household_012.csv"
		 * , "FX-Rates-10pc-KRW", true, 12); System.out.println( new
		 * Date(System.currentTimeMillis()) +
		 * ": processing FX-Rates-10pc-MYR_SUMMARY_Household_012.csv");
		 * processHouseholdMetricsCsv(
		 * "D:\\OneDrive\\Dissertation\\Results\\Summary Data\\FX-Rates-10pc-MYR_SUMMARY_Household_012.csv"
		 * , "FX-Rates-10pc-MYR", true, 12); System.out.println( new
		 * Date(System.currentTimeMillis()) +
		 * ": processing FX-Rates-10pc-NZD_SUMMARY_Household_012.csv");
		 * processHouseholdMetricsCsv(
		 * "D:\\OneDrive\\Dissertation\\Results\\Summary Data\\FX-Rates-10pc-NZD_SUMMARY_Household_012.csv"
		 * , "FX-Rates-10pc-NZD", true, 12); System.out.println( new
		 * Date(System.currentTimeMillis()) +
		 * ": processing FX-Rates-10pc-SGD_SUMMARY_Household_012.csv");
		 * processHouseholdMetricsCsv(
		 * "D:\\OneDrive\\Dissertation\\Results\\Summary Data\\FX-Rates-10pc-SGD_SUMMARY_Household_012.csv"
		 * , "FX-Rates-10pc-SGD", true, 12); System.out.println( new
		 * Date(System.currentTimeMillis()) +
		 * ": processing FX-Rates-10pc-THB_SUMMARY_Household_012.csv");
		 * processHouseholdMetricsCsv(
		 * "D:\\OneDrive\\Dissertation\\Results\\Summary Data\\FX-Rates-10pc-THB_SUMMARY_Household_012.csv"
		 * , "FX-Rates-10pc-THB", true, 12); System.out.println( new
		 * Date(System.currentTimeMillis()) +
		 * ": processing FX-Rates-10pc-USD_SUMMARY_Household_012.csv");
		 * processHouseholdMetricsCsv(
		 * "D:\\OneDrive\\Dissertation\\Results\\Summary Data\\FX-Rates-10pc-USD_SUMMARY_Household_012.csv"
		 * , "FX-Rates-10pc-USD", true, 12);
		 */

		// inflation rates
		/*
		 * System.out.println( new Date(System.currentTimeMillis()) +
		 * ": processing Inflation-01pc_SUMMARY_Household_012.csv");
		 * processHouseholdMetricsCsv(
		 * "D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Inflation-01pc_SUMMARY_Household_012.csv"
		 * , "Inflation-01pc", true, 12); System.out.println( new
		 * Date(System.currentTimeMillis()) +
		 * ": processing Inflation-02pc_SUMMARY_Household_012.csv");
		 * processHouseholdMetricsCsv(
		 * "D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Inflation-02pc_SUMMARY_Household_012.csv"
		 * , "Inflation-02pc", true, 12); System.out.println( new
		 * Date(System.currentTimeMillis()) +
		 * ": processing Inflation-03pc_SUMMARY_Household_012.csv");
		 * processHouseholdMetricsCsv(
		 * "D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Inflation-03pc_SUMMARY_Household_012.csv"
		 * , "Inflation-03pc", true, 12); System.out.println( new
		 * Date(System.currentTimeMillis()) +
		 * ": processing Inflation-04pc_SUMMARY_Household_012.csv");
		 * processHouseholdMetricsCsv(
		 * "D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Inflation-04pc_SUMMARY_Household_012.csv"
		 * , "Inflation-04pc", true, 12);
		 */

		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing Inflation-05pc_SUMMARY_Household_012.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Inflation-05pc_SUMMARY_Household_012.csv",
				"Inflation-05pc", true, 12);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing Inflation-10pc_SUMMARY_Household_012.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Inflation-10pc_SUMMARY_Household_012.csv",
				"Inflation-10pc", true, 12);

		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing Inflation-15pc_SUMMARY_Household_012.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Inflation-15pc_SUMMARY_Household_012.csv",
				"Inflation-15pc", true, 12);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing Inflation-20pc_SUMMARY_Household_012.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Inflation-20pc_SUMMARY_Household_012.csv",
				"Inflation-20pc", true, 12);

		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing Inflation-25pc_SUMMARY_Household_012.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Inflation-25pc_SUMMARY_Household_012.csv",
				"Inflation-25pc", true, 12);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing Inflation-50pc_SUMMARY_Household_012.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Inflation-50pc_SUMMARY_Household_012.csv",
				"Inflation-50pc", true, 12);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing Inflation-75pc_SUMMARY_Household_012.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Inflation-75pc_SUMMARY_Household_012.csv",
				"Inflation-75pc", true, 12);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing Inflation-15pc-5yrs_SUMMARY_Household_060.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Inflation-15pc-5yrs_SUMMARY_Household_060.csv",
				"Inflation-15pc-5yrs", true, 60);
	}

	private static void processHouseholdMetricsCsv(String inFileResourceLocation, String scenario, boolean append,
			int iteration) {

		// overall mean metrics (to test model error)
		float meanIncome = 0f;
		float meanExpenses = 0f;
		float debtFree = 0f;
		float mtgCostsOver30pc = 0f;
		float mtgCostsOver30pcDecile1 = 0f;
		float mtgCostsOver30pcDecile2 = 0f;
		float mtgCostsOver30pcDecile3 = 0f;
		float mtgCostsOver30pcDecile4 = 0f;
		float mtgCostsOver30pcDecile5 = 0f;
		float mtgCostsOver30pcDecile6 = 0f;
		float mtgCostsOver30pcDecile7 = 0f;
		float mtgCostsOver30pcDecile8 = 0f;
		float mtgCostsOver30pcDecile9 = 0f;
		float mtgCostsOver30pcDecile10 = 0f;
		float mtgCostsOver30pcOwnerOccupied = 0f;
		float rentCostsOver30pcDecile1 = 0f;
		float rentCostsOver30pcDecile2 = 0f;
		float rentCostsOver30pcDecile3 = 0f;
		float rentCostsOver30pcDecile4 = 0f;
		float rentCostsOver30pcDecile5 = 0f;
		float rentCostsOver30pcDecile6 = 0f;
		float rentCostsOver30pcDecile7 = 0f;
		float rentCostsOver30pcDecile8 = 0f;
		float rentCostsOver30pcDecile9 = 0f;
		float rentCostsOver30pcDecile10 = 0f;
		float housingCostsOver30pc = 0f; // % of households in mtg distress
		float housingCostsOver30pcTop1pc = 0f;
		float housingCostsOver30pcTop5pc = 0f;
		float housingCostsOver30pcBottom95pc = 0f;
		float housingCostsOver30pcDecile1 = 0f;
		float housingCostsOver30pcDecile2 = 0f;
		float housingCostsOver30pcDecile3 = 0f;
		float housingCostsOver30pcDecile4 = 0f;
		float housingCostsOver30pcDecile5 = 0f;
		float housingCostsOver30pcDecile6 = 0f;
		float housingCostsOver30pcDecile7 = 0f;
		float housingCostsOver30pcDecile8 = 0f;
		float housingCostsOver30pcDecile9 = 0f;
		float housingCostsOver30pcDecile10 = 0f;
		float totalIncomePercentEarnedByTop1pc = 0f;
		float totalIncomePercentEarnedByTop5pc = 0f; // % of assets controlled by top 5% of income (Kumhof, et al, 2015)
		float totalIncomePercentEarnedByDecile1 = 0f; // NEW *******************
		float totalIncomePercentEarnedByDecile2 = 0f;
		float totalIncomePercentEarnedByDecile3 = 0f;
		float totalIncomePercentEarnedByDecile4 = 0f;
		float totalIncomePercentEarnedByDecile5 = 0f;
		float totalIncomePercentEarnedByDecile6 = 0f;
		float totalIncomePercentEarnedByDecile7 = 0f;
		float totalIncomePercentEarnedByDecile8 = 0f;
		float totalIncomePercentEarnedByDecile9 = 0f;
		float totalIncomePercentEarnedByDecile10 = 0f;
		float debtToIncome = 0f; // household debt-to-income ratio (Kumhof, et al, 2015)
		float debtToIncomeTop1pc = 0f;
		float debtToIncomeTop5pc = 0f;
		float debtToIncomeBottom95pc = 0f;
		float debtToIncomeDecile1 = 0f; // NEW ******************
		float debtToIncomeDecile2 = 0f;
		float debtToIncomeDecile3 = 0f;
		float debtToIncomeDecile4 = 0f;
		float debtToIncomeDecile5 = 0f;
		float debtToIncomeDecile6 = 0f;
		float debtToIncomeDecile7 = 0f;
		float debtToIncomeDecile8 = 0f;
		float debtToIncomeDecile9 = 0f;
		float debtToIncomeDecile10 = 0f;
		float debtToNetWorth = 0f; // household debt-to-net worth ratio (Kumhof, et al, 2015)
		float debtToNetWorthTop1pc = 0f;
		float debtToNetWorthTop5pc = 0f;
		float debtToNetWorthBottom95pc = 0f;
		float wealthPercentHeldByTop1pc = 0f;
		float wealthPercentHeldByTop5pc = 0f; // share of net worth held by top 5% of income (Kumhof, et al, 2015)
		float incomeToHendersonPercent = 0f;
		float incomeToHendersonPercentTop1pc = 0f;
		float incomeToHendersonPercentTop5pc = 0f;
		float incomeToHendersonPercentBottom95pc = 0f;
		float incomeToHendersonPercentDecile1 = 0f; // NEW **************
		float incomeToHendersonPercentDecile2 = 0f;
		float incomeToHendersonPercentDecile3 = 0f;
		float incomeToHendersonPercentDecile4 = 0f;
		float incomeToHendersonPercentDecile5 = 0f;
		float incomeToHendersonPercentDecile6 = 0f;
		float incomeToHendersonPercentDecile7 = 0f;
		float incomeToHendersonPercentDecile8 = 0f;
		float incomeToHendersonPercentDecile9 = 0f;
		float incomeToHendersonPercentDecile10 = 0f;
		float incomeToHendersonAndHousingPercentDecile1 = 0f; // NEW **************
		float incomeToHendersonAndHousingPercentDecile2 = 0f;
		float incomeToHendersonAndHousingPercentDecile3 = 0f;
		float incomeToHendersonAndHousingPercentDecile4 = 0f;
		float incomeToHendersonAndHousingPercentDecile5 = 0f;
		float incomeToHendersonAndHousingPercentDecile6 = 0f;
		float incomeToHendersonAndHousingPercentDecile7 = 0f;
		float incomeToHendersonAndHousingPercentDecile8 = 0f;
		float incomeToHendersonAndHousingPercentDecile9 = 0f;
		float incomeToHendersonAndHousingPercentDecile10 = 0f;

		// local working variables
		int householdCount = 0;
		int householdCountOwnerOccupied = 0;
		int householdCountTop5pc = 0;
		int householdCountTop1pc = 0;
		int householdCountDecile1 = 0;
		int householdCountDecile2 = 0;
		int householdCountDecile3 = 0;
		int householdCountDecile4 = 0;
		int householdCountDecile5 = 0;
		int householdCountDecile6 = 0;
		int householdCountDecile7 = 0;
		int householdCountDecile8 = 0;
		int householdCountDecile9 = 0;
		int householdCountDecile10 = 0;
		int debtFreeCount = 0;
		int mtgCostsOver30pcCount = 0;
		int mtgCostsOver30pcCountOwnerOccupied = 0;
		int mtgCostsOver30pcCountDecile1 = 0;
		int mtgCostsOver30pcCountDecile2 = 0;
		int mtgCostsOver30pcCountDecile3 = 0;
		int mtgCostsOver30pcCountDecile4 = 0;
		int mtgCostsOver30pcCountDecile5 = 0;
		int mtgCostsOver30pcCountDecile6 = 0;
		int mtgCostsOver30pcCountDecile7 = 0;
		int mtgCostsOver30pcCountDecile8 = 0;
		int mtgCostsOver30pcCountDecile9 = 0;
		int mtgCostsOver30pcCountDecile10 = 0;
		int rentCostsOver30pcCountDecile1 = 0;
		int rentCostsOver30pcCountDecile2 = 0;
		int rentCostsOver30pcCountDecile3 = 0;
		int rentCostsOver30pcCountDecile4 = 0;
		int rentCostsOver30pcCountDecile5 = 0;
		int rentCostsOver30pcCountDecile6 = 0;
		int rentCostsOver30pcCountDecile7 = 0;
		int rentCostsOver30pcCountDecile8 = 0;
		int rentCostsOver30pcCountDecile9 = 0;
		int rentCostsOver30pcCountDecile10 = 0;
		int housingCostsOver30pcCount = 0;
		int housingCostsOver30pcCountTop1pc = 0;
		int housingCostsOver30pcCountTop5pc = 0;
		int housingCostsOver30pcCountBottom95pc = 0;
		int housingCostsOver30pcCountDecile1 = 0;
		int housingCostsOver30pcCountDecile2 = 0;
		int housingCostsOver30pcCountDecile3 = 0;
		int housingCostsOver30pcCountDecile4 = 0;
		int housingCostsOver30pcCountDecile5 = 0;
		int housingCostsOver30pcCountDecile6 = 0;
		int housingCostsOver30pcCountDecile7 = 0;
		int housingCostsOver30pcCountDecile8 = 0;
		int housingCostsOver30pcCountDecile9 = 0;
		int housingCostsOver30pcCountDecile10 = 0;
		float totalDebt = 0f;
		float totalDebtTop1pc = 0f;
		float totalDebtTop5pc = 0f;
		float totalDebtBottom95pc = 0f;
		float totalDebtDecile1 = 0f;
		float totalDebtDecile2 = 0f;
		float totalDebtDecile3 = 0f;
		float totalDebtDecile4 = 0f;
		float totalDebtDecile5 = 0f;
		float totalDebtDecile6 = 0f;
		float totalDebtDecile7 = 0f;
		float totalDebtDecile8 = 0f;
		float totalDebtDecile9 = 0f;
		float totalDebtDecile10 = 0f;
		float totalIncome = 0f;
		float totalIncomeTop1pc = 0f;
		float totalIncomeTop5pc = 0f;
		float totalIncomeBottom95pc = 0f;
		float totalIncomeDecile1 = 0f;
		float totalIncomeDecile2 = 0f;
		float totalIncomeDecile3 = 0f;
		float totalIncomeDecile4 = 0f;
		float totalIncomeDecile5 = 0f;
		float totalIncomeDecile6 = 0f;
		float totalIncomeDecile7 = 0f;
		float totalIncomeDecile8 = 0f;
		float totalIncomeDecile9 = 0f;
		float totalIncomeDecile10 = 0f;
		float totalExpenses = 0f;
		float totalNetWorth = 0f;
		float totalNetWorthTop1pc = 0f;
		float totalNetWorthTop5pc = 0f;
		float totalNetWorthBottom95pc = 0f;
		float totalHenderson = 0f;
		float totalHendersonTop1pc = 0f;
		float totalHendersonTop5pc = 0f;
		float totalHendersonBottom95pc = 0f;
		float totalHendersonDecile1 = 0f;
		float totalHendersonDecile2 = 0f;
		float totalHendersonDecile3 = 0f;
		float totalHendersonDecile4 = 0f;
		float totalHendersonDecile5 = 0f;
		float totalHendersonDecile6 = 0f;
		float totalHendersonDecile7 = 0f;
		float totalHendersonDecile8 = 0f;
		float totalHendersonDecile9 = 0f;
		float totalHendersonDecile10 = 0f;
		float totalHousingDecile1 = 0f;
		float totalHousingDecile2 = 0f;
		float totalHousingDecile3 = 0f;
		float totalHousingDecile4 = 0f;
		float totalHousingDecile5 = 0f;
		float totalHousingDecile6 = 0f;
		float totalHousingDecile7 = 0f;
		float totalHousingDecile8 = 0f;
		float totalHousingDecile9 = 0f;
		float totalHousingDecile10 = 0f;

		ArrayList<Float> incomeList = new ArrayList<Float>();
		float top1pcThreshold = 0f;
		float top5pcThreshold = 0f;
		float decileThreshold1 = 0f; // top 10% of all income
		float decileThreshold2 = 0f; // top 20-11% of all income
		float decileThreshold3 = 0f; // top 30-21% of all income
		float decileThreshold4 = 0f; // top 40-31% of all income
		float decileThreshold5 = 0f; // top 50-41% of all income
		float decileThreshold6 = 0f; // top 60-51% of all income
		float decileThreshold7 = 0f; // top 70-61% of all income
		float decileThreshold8 = 0f; // top 80-71% of all income
		float decileThreshold9 = 0f; // top 90-81% of all income
		/*
		 * Read in the file, sort by income, and determine 5% and 1% income thresholds
		 * 
		 * Read it a second time, and calculate metrics
		 */

		// read CSV file, sort by income, and determine income thresholds
		CSVReader reader = null;
		try {
			Reader fr = new FileReader(inFileResourceLocation);
			reader = new CSVReader(fr);
			String[] line = reader.readNext(); // read and discard header row
			while ((line = reader.readNext()) != null) {
				try {
					float income = Float.valueOf(line[6].replace(",", "")); // gross income
					incomeList.add(income);
				} catch (NumberFormatException e) {
					// do nothing and leave it as zero.
				}
			}
			reader.close();
			reader = null;
		} catch (FileNotFoundException e) {
			// open file
			e.printStackTrace();
		} catch (IOException e) {
			// read next
			e.printStackTrace();
		}
		// sort income and determine 5% and 1% thresholds
		incomeList.trimToSize();
		householdCount = incomeList.size();
		Collections.sort(incomeList);
		top5pcThreshold = incomeList.get((int) (householdCount * 0.95f));
		top1pcThreshold = incomeList.get((int) (householdCount * 0.99f));
		decileThreshold1 = incomeList.get((int) (householdCount * 0.9f));
		decileThreshold2 = incomeList.get((int) (householdCount * 0.8f));
		decileThreshold3 = incomeList.get((int) (householdCount * 0.7f));
		decileThreshold4 = incomeList.get((int) (householdCount * 0.6f));
		decileThreshold5 = incomeList.get((int) (householdCount * 0.5f));
		decileThreshold6 = incomeList.get((int) (householdCount * 0.4f));
		decileThreshold7 = incomeList.get((int) (householdCount * 0.3f));
		decileThreshold8 = incomeList.get((int) (householdCount * 0.2f));
		decileThreshold9 = incomeList.get((int) (householdCount * 0.1f));

		System.out.println("Thresholds: " + top1pcThreshold + ", " + top5pcThreshold);
		System.out.println("Thresholds: (1) = " + decileThreshold1 + ", (2) = " + decileThreshold2 + ", (3) = "
				+ decileThreshold3 + ", (4) = " + decileThreshold4 + ", (5) = " + decileThreshold5 + ", (6) = "
				+ decileThreshold6 + ", (7) = " + decileThreshold7 + ", (8) = " + decileThreshold8 + ", (9) = "
				+ decileThreshold9);

		// read CSV file a second time, and calculate metrics
		reader = null;
		try {
			Reader fr = new FileReader(inFileResourceLocation);
			reader = new CSVReader(fr);
			String[] line = reader.readNext(); // read and discard header row
			while ((line = reader.readNext()) != null) {
				try {
					// get values for this household
					float income = Float.valueOf(line[6].replace(",", ""));
					float mtgCosts = Float.valueOf(line[11].replace(",", ""));
					float rentCosts = Float.valueOf(line[10].replace(",", ""));
					float housingCosts = rentCosts + mtgCosts;
					float henderson = Float.valueOf(line[9].replace(",", ""));
					float expenses = housingCosts + henderson + Float.valueOf(line[12].replace(",", ""));
					float debt = Float.valueOf(line[14].replace(",", ""));
					float netWorth = Float.valueOf(line[13].replace(",", ""))
							- Float.valueOf(line[16].replace(",", ""));

					// update metrics
					if (income > top5pcThreshold) {
						// add to top 5% metrics
						householdCountTop5pc++;
						totalIncomeTop5pc += income;
						totalHendersonTop5pc += henderson;
						totalDebtTop5pc += debt;
						totalNetWorthTop5pc += netWorth;
						if (housingCosts > 0.3f * income) {
							// mortgage distress in top 5% of income
							housingCostsOver30pcCountTop5pc++;
						}
						if (income > top1pcThreshold) {
							// add to top 1% metrics
							householdCountTop1pc++;
							totalIncomeTop1pc += income;
							totalHendersonTop1pc += henderson;
							totalDebtTop1pc += debt;
							totalNetWorthTop1pc += netWorth;
							if (housingCosts > 0.3f * income) {
								// mortgage distress in top 1% of income
								housingCostsOver30pcCountTop1pc++;
							}
						}
					} else {
						// add to bottom 95% metrics
						totalIncomeBottom95pc += income;
						totalHendersonBottom95pc += henderson;
						totalDebtBottom95pc += debt;
						totalNetWorthBottom95pc += netWorth;
						if (housingCosts > 0.3f * income) {
							// mortgage distress in bottom 95% of income
							housingCostsOver30pcCountBottom95pc++;
						}
					}
					if (housingCosts > 0.3f * income) {
						// mortgage distress
						housingCostsOver30pcCount++;
					}
					if (mtgCosts > 0.3f * income) {
						// mortgage distress
						mtgCostsOver30pcCount++;
						if (rentCosts <= 1f) {
							mtgCostsOver30pcCountOwnerOccupied++;
						}
					}
					if (rentCosts <= 1f && mtgCosts > 0f) {
						// owner occupied borrowing households (van Onselen, 2019)
						householdCountOwnerOccupied++;
					}
					if (debt <= 1f) {
						debtFreeCount++;
					}
					// add to overall metrics
					totalIncome += income;
					totalHenderson += henderson;
					totalExpenses += expenses;
					totalDebt += debt;
					totalNetWorth += netWorth;

					// decile metrics
					if (income > decileThreshold1) {
						householdCountDecile1++;
						totalDebtDecile1 += debt;
						totalIncomeDecile1 += income;
						totalHendersonDecile1 += henderson;
						totalHousingDecile1 += rentCosts > 0f ? rentCosts : mtgCosts;
						if (housingCosts > 0.3f * income) {
							housingCostsOver30pcCountDecile1++;
						}
						if (mtgCosts > 0.3f * income) {
							mtgCostsOver30pcCountDecile1++;
						}
						if (rentCosts > 0.3f * income) {
							rentCostsOver30pcCountDecile1++;
						}
					} else if (income > decileThreshold2) {
						householdCountDecile2++;
						totalDebtDecile2 += debt;
						totalIncomeDecile2 += income;
						totalHendersonDecile2 += henderson;
						totalHousingDecile2 += rentCosts > 0f ? rentCosts : mtgCosts;
						if (housingCosts > 0.3f * income) {
							housingCostsOver30pcCountDecile2++;
						}
						if (mtgCosts > 0.3f * income) {
							mtgCostsOver30pcCountDecile2++;
						}
						if (rentCosts > 0.3f * income) {
							rentCostsOver30pcCountDecile2++;
						}
					} else if (income > decileThreshold3) {
						householdCountDecile3++;
						totalDebtDecile3 += debt;
						totalIncomeDecile3 += income;
						totalHendersonDecile3 += henderson;
						totalHousingDecile3 += rentCosts > 0f ? rentCosts : mtgCosts;
						if (housingCosts > 0.3f * income) {
							housingCostsOver30pcCountDecile3++;
						}
						if (mtgCosts > 0.3f * income) {
							mtgCostsOver30pcCountDecile3++;
						}
						if (rentCosts > 0.3f * income) {
							rentCostsOver30pcCountDecile3++;
						}
					} else if (income > decileThreshold4) {
						householdCountDecile4++;
						totalDebtDecile4 += debt;
						totalIncomeDecile4 += income;
						totalHendersonDecile4 += henderson;
						totalHousingDecile4 += rentCosts > 0f ? rentCosts : mtgCosts;
						if (housingCosts > 0.3f * income) {
							housingCostsOver30pcCountDecile4++;
						}
						if (mtgCosts > 0.3f * income) {
							mtgCostsOver30pcCountDecile4++;
						}
						if (rentCosts > 0.3f * income) {
							rentCostsOver30pcCountDecile4++;
						}
					} else if (income > decileThreshold5) {
						householdCountDecile5++;
						totalDebtDecile5 += debt;
						totalIncomeDecile5 += income;
						totalHendersonDecile5 += henderson;
						totalHousingDecile5 += rentCosts > 0f ? rentCosts : mtgCosts;
						if (housingCosts > 0.3f * income) {
							housingCostsOver30pcCountDecile5++;
						}
						if (mtgCosts > 0.3f * income) {
							mtgCostsOver30pcCountDecile5++;
						}
						if (rentCosts > 0.3f * income) {
							rentCostsOver30pcCountDecile5++;
						}
					} else if (income > decileThreshold6) {
						householdCountDecile6++;
						totalDebtDecile6 += debt;
						totalIncomeDecile6 += income;
						totalHendersonDecile6 += henderson;
						totalHousingDecile6 += rentCosts > 0f ? rentCosts : mtgCosts;
						if (housingCosts > 0.3f * income) {
							housingCostsOver30pcCountDecile6++;
						}
						if (mtgCosts > 0.3f * income) {
							mtgCostsOver30pcCountDecile6++;
						}
						if (rentCosts > 0.3f * income) {
							rentCostsOver30pcCountDecile6++;
						}
					} else if (income > decileThreshold7) {
						householdCountDecile7++;
						totalDebtDecile7 += debt;
						totalIncomeDecile7 += income;
						totalHendersonDecile7 += henderson;
						totalHousingDecile7 += rentCosts > 0f ? rentCosts : mtgCosts;
						if (housingCosts > 0.3f * income) {
							housingCostsOver30pcCountDecile7++;
						}
						if (mtgCosts > 0.3f * income) {
							mtgCostsOver30pcCountDecile7++;
						}
						if (rentCosts > 0.3f * income) {
							rentCostsOver30pcCountDecile7++;
						}
					} else if (income > decileThreshold8) {
						householdCountDecile8++;
						totalDebtDecile8 += debt;
						totalIncomeDecile8 += income;
						totalHendersonDecile8 += henderson;
						totalHousingDecile8 += rentCosts > 0f ? rentCosts : mtgCosts;
						if (housingCosts > 0.3f * income) {
							housingCostsOver30pcCountDecile8++;
						}
						if (mtgCosts > 0.3f * income) {
							mtgCostsOver30pcCountDecile8++;
						}
						if (rentCosts > 0.3f * income) {
							rentCostsOver30pcCountDecile8++;
						}
					} else if (income > decileThreshold9) {
						householdCountDecile9++;
						totalDebtDecile9 += debt;
						totalIncomeDecile9 += income;
						totalHendersonDecile9 += henderson;
						totalHousingDecile9 += rentCosts > 0f ? rentCosts : mtgCosts;
						if (housingCosts > 0.3f * income) {
							housingCostsOver30pcCountDecile9++;
						}
						if (mtgCosts > 0.3f * income) {
							mtgCostsOver30pcCountDecile9++;
						}
						if (rentCosts > 0.3f * income) {
							rentCostsOver30pcCountDecile9++;
						}
					} else {
						householdCountDecile10++;
						totalDebtDecile10 += debt;
						totalIncomeDecile10 += income;
						totalHendersonDecile10 += henderson;
						totalHousingDecile10 += rentCosts > 0f ? rentCosts : mtgCosts;
						if (housingCosts > 0.3f * income) {
							housingCostsOver30pcCountDecile10++;
						}
						if (mtgCosts > 0.3f * income) {
							mtgCostsOver30pcCountDecile10++;
						}
						if (rentCosts > 0.3f * income) {
							rentCostsOver30pcCountDecile10++;
						}
					}
				} catch (NumberFormatException e) {
					// do nothing and leave it as zero.
				}
			}
			reader.close();
			reader = null;
		} catch (

		FileNotFoundException e) {
			// open file
			e.printStackTrace();
		} catch (IOException e) {
			// read next
			e.printStackTrace();
		}

		// calculate ratio metrics
		meanIncome = totalIncome / Float.valueOf(householdCount);
		meanExpenses = totalExpenses / Float.valueOf(householdCount);
		debtFree = Float.valueOf(debtFreeCount) / Float.valueOf(householdCount);
		mtgCostsOver30pc = Float.valueOf(mtgCostsOver30pcCount) / Float.valueOf(householdCount);
		mtgCostsOver30pcOwnerOccupied = Float.valueOf(mtgCostsOver30pcCountOwnerOccupied)
				/ Float.valueOf(householdCount);
		mtgCostsOver30pcDecile1 = Float.valueOf(mtgCostsOver30pcCountDecile1) / Float.valueOf(householdCountDecile1);
		mtgCostsOver30pcDecile2 = Float.valueOf(mtgCostsOver30pcCountDecile2) / Float.valueOf(householdCountDecile2);
		mtgCostsOver30pcDecile3 = Float.valueOf(mtgCostsOver30pcCountDecile3) / Float.valueOf(householdCountDecile3);
		mtgCostsOver30pcDecile4 = Float.valueOf(mtgCostsOver30pcCountDecile4) / Float.valueOf(householdCountDecile4);
		mtgCostsOver30pcDecile5 = Float.valueOf(mtgCostsOver30pcCountDecile5) / Float.valueOf(householdCountDecile5);
		mtgCostsOver30pcDecile6 = Float.valueOf(mtgCostsOver30pcCountDecile6) / Float.valueOf(householdCountDecile6);
		mtgCostsOver30pcDecile7 = Float.valueOf(mtgCostsOver30pcCountDecile7) / Float.valueOf(householdCountDecile7);
		mtgCostsOver30pcDecile8 = Float.valueOf(mtgCostsOver30pcCountDecile8) / Float.valueOf(householdCountDecile8);
		mtgCostsOver30pcDecile9 = Float.valueOf(mtgCostsOver30pcCountDecile9) / Float.valueOf(householdCountDecile9);
		mtgCostsOver30pcDecile10 = Float.valueOf(mtgCostsOver30pcCountDecile10) / Float.valueOf(householdCountDecile10);
		rentCostsOver30pcDecile1 = Float.valueOf(rentCostsOver30pcCountDecile1) / Float.valueOf(householdCountDecile1);
		rentCostsOver30pcDecile2 = Float.valueOf(rentCostsOver30pcCountDecile2) / Float.valueOf(householdCountDecile2);
		rentCostsOver30pcDecile3 = Float.valueOf(rentCostsOver30pcCountDecile3) / Float.valueOf(householdCountDecile3);
		rentCostsOver30pcDecile4 = Float.valueOf(rentCostsOver30pcCountDecile4) / Float.valueOf(householdCountDecile4);
		rentCostsOver30pcDecile5 = Float.valueOf(rentCostsOver30pcCountDecile5) / Float.valueOf(householdCountDecile5);
		rentCostsOver30pcDecile6 = Float.valueOf(rentCostsOver30pcCountDecile6) / Float.valueOf(householdCountDecile6);
		rentCostsOver30pcDecile7 = Float.valueOf(rentCostsOver30pcCountDecile7) / Float.valueOf(householdCountDecile7);
		rentCostsOver30pcDecile8 = Float.valueOf(rentCostsOver30pcCountDecile8) / Float.valueOf(householdCountDecile8);
		rentCostsOver30pcDecile9 = Float.valueOf(rentCostsOver30pcCountDecile9) / Float.valueOf(householdCountDecile9);
		rentCostsOver30pcDecile10 = Float.valueOf(rentCostsOver30pcCountDecile10)
				/ Float.valueOf(householdCountDecile10);
		housingCostsOver30pc = Float.valueOf(housingCostsOver30pcCount) / Float.valueOf(householdCount);
		housingCostsOver30pcTop5pc = Float.valueOf(housingCostsOver30pcCountTop5pc)
				/ Float.valueOf(householdCountTop5pc);
		housingCostsOver30pcTop1pc = Float.valueOf(housingCostsOver30pcCountTop1pc)
				/ Float.valueOf(householdCountTop1pc);
		housingCostsOver30pcBottom95pc = Float.valueOf(housingCostsOver30pcCountBottom95pc)
				/ Float.valueOf(householdCount - householdCountTop5pc);
		totalIncomePercentEarnedByTop1pc = totalIncomeTop1pc / totalIncome;
		totalIncomePercentEarnedByTop5pc = totalIncomeTop5pc / totalIncome;
		totalIncomePercentEarnedByDecile1 = totalIncomeDecile1 / totalIncome;
		totalIncomePercentEarnedByDecile2 = totalIncomeDecile2 / totalIncome;
		totalIncomePercentEarnedByDecile3 = totalIncomeDecile3 / totalIncome;
		totalIncomePercentEarnedByDecile4 = totalIncomeDecile4 / totalIncome;
		totalIncomePercentEarnedByDecile5 = totalIncomeDecile5 / totalIncome;
		totalIncomePercentEarnedByDecile6 = totalIncomeDecile6 / totalIncome;
		totalIncomePercentEarnedByDecile7 = totalIncomeDecile7 / totalIncome;
		totalIncomePercentEarnedByDecile8 = totalIncomeDecile8 / totalIncome;
		totalIncomePercentEarnedByDecile9 = totalIncomeDecile9 / totalIncome;
		totalIncomePercentEarnedByDecile10 = totalIncomeDecile10 / totalIncome;
		debtToIncome = totalDebt / totalIncome; // household
												// debt-to-income
												// ratio
												// (Kumhof,
												// et
												// al,
												// 2015)
		debtToIncomeTop1pc = totalDebtTop1pc / totalIncomeTop1pc;
		debtToIncomeTop5pc = totalDebtTop5pc / totalIncomeTop5pc;
		debtToIncomeBottom95pc = totalDebtBottom95pc / totalIncomeBottom95pc;
		debtToIncomeDecile1 = totalDebtDecile1 / totalIncomeDecile1;
		debtToIncomeDecile2 = totalDebtDecile2 / totalIncomeDecile2;
		debtToIncomeDecile3 = totalDebtDecile3 / totalIncomeDecile3;
		debtToIncomeDecile4 = totalDebtDecile4 / totalIncomeDecile4;
		debtToIncomeDecile5 = totalDebtDecile5 / totalIncomeDecile5;
		debtToIncomeDecile6 = totalDebtDecile6 / totalIncomeDecile6;
		debtToIncomeDecile7 = totalDebtDecile7 / totalIncomeDecile7;
		debtToIncomeDecile8 = totalDebtDecile8 / totalIncomeDecile8;
		debtToIncomeDecile9 = totalDebtDecile9 / totalIncomeDecile9;
		debtToIncomeDecile10 = totalDebtDecile10 / totalIncomeDecile10;
		debtToNetWorth = totalDebt / totalNetWorth;
		debtToNetWorthTop1pc = totalDebtTop1pc / totalNetWorthTop1pc;
		debtToNetWorthTop5pc = totalDebtTop5pc / totalNetWorthTop5pc;
		debtToNetWorthBottom95pc = totalDebtBottom95pc / totalNetWorthBottom95pc;
		wealthPercentHeldByTop1pc = totalNetWorthTop1pc / totalNetWorth;
		wealthPercentHeldByTop5pc = totalNetWorthTop5pc / totalNetWorth;
		incomeToHendersonPercent = totalIncome / totalHenderson;
		incomeToHendersonPercentTop1pc = totalIncomeTop1pc / totalHendersonTop1pc;
		incomeToHendersonPercentTop5pc = totalIncomeTop5pc / totalHendersonTop5pc;
		incomeToHendersonPercentBottom95pc = totalIncomeBottom95pc / totalHendersonBottom95pc;
		incomeToHendersonPercentDecile1 = totalIncomeDecile1 / totalHendersonDecile1;
		incomeToHendersonPercentDecile2 = totalIncomeDecile2 / totalHendersonDecile2;
		incomeToHendersonPercentDecile3 = totalIncomeDecile3 / totalHendersonDecile3;
		incomeToHendersonPercentDecile4 = totalIncomeDecile4 / totalHendersonDecile4;
		incomeToHendersonPercentDecile5 = totalIncomeDecile5 / totalHendersonDecile5;
		incomeToHendersonPercentDecile6 = totalIncomeDecile6 / totalHendersonDecile6;
		incomeToHendersonPercentDecile7 = totalIncomeDecile7 / totalHendersonDecile7;
		incomeToHendersonPercentDecile8 = totalIncomeDecile8 / totalHendersonDecile8;
		incomeToHendersonPercentDecile9 = totalIncomeDecile9 / totalHendersonDecile9;
		incomeToHendersonPercentDecile10 = totalIncomeDecile10 / totalHendersonDecile10;
		incomeToHendersonAndHousingPercentDecile1 = totalIncomeDecile1 / (totalHendersonDecile1 + totalHousingDecile1);
		incomeToHendersonAndHousingPercentDecile2 = totalIncomeDecile2 / (totalHendersonDecile2 + totalHousingDecile2);
		incomeToHendersonAndHousingPercentDecile3 = totalIncomeDecile3 / (totalHendersonDecile3 + totalHousingDecile3);
		incomeToHendersonAndHousingPercentDecile4 = totalIncomeDecile4 / (totalHendersonDecile4 + totalHousingDecile4);
		incomeToHendersonAndHousingPercentDecile5 = totalIncomeDecile5 / (totalHendersonDecile5 + totalHousingDecile5);
		incomeToHendersonAndHousingPercentDecile6 = totalIncomeDecile6 / (totalHendersonDecile6 + totalHousingDecile6);
		incomeToHendersonAndHousingPercentDecile7 = totalIncomeDecile7 / (totalHendersonDecile7 + totalHousingDecile7);
		incomeToHendersonAndHousingPercentDecile8 = totalIncomeDecile8 / (totalHendersonDecile8 + totalHousingDecile8);
		incomeToHendersonAndHousingPercentDecile9 = totalIncomeDecile9 / (totalHendersonDecile9 + totalHousingDecile9);
		incomeToHendersonAndHousingPercentDecile10 = totalIncomeDecile10
				/ (totalHendersonDecile10 + totalHousingDecile10);

		housingCostsOver30pcDecile1 = Float.valueOf(housingCostsOver30pcCountDecile1)
				/ Float.valueOf(householdCountDecile1);
		housingCostsOver30pcDecile2 = Float.valueOf(housingCostsOver30pcCountDecile2)
				/ Float.valueOf(householdCountDecile2);
		housingCostsOver30pcDecile3 = Float.valueOf(housingCostsOver30pcCountDecile3)
				/ Float.valueOf(householdCountDecile3);
		housingCostsOver30pcDecile4 = Float.valueOf(housingCostsOver30pcCountDecile4)
				/ Float.valueOf(householdCountDecile4);
		housingCostsOver30pcDecile5 = Float.valueOf(housingCostsOver30pcCountDecile5)
				/ Float.valueOf(householdCountDecile5);
		housingCostsOver30pcDecile6 = Float.valueOf(housingCostsOver30pcCountDecile6)
				/ Float.valueOf(householdCountDecile6);
		housingCostsOver30pcDecile7 = Float.valueOf(housingCostsOver30pcCountDecile7)
				/ Float.valueOf(householdCountDecile7);
		housingCostsOver30pcDecile8 = Float.valueOf(housingCostsOver30pcCountDecile8)
				/ Float.valueOf(householdCountDecile8);
		housingCostsOver30pcDecile9 = Float.valueOf(housingCostsOver30pcCountDecile9)
				/ Float.valueOf(householdCountDecile9);
		housingCostsOver30pcDecile10 = Float.valueOf(housingCostsOver30pcCountDecile10)
				/ Float.valueOf(householdCountDecile10);

		// save CSV file in a format that R can graph
		DecimalFormat wholeNumber = new DecimalFormat("000");
		String outFilename = properties.getOutputDirectory() + "R_GRAPH_Metrics.csv";
		Writer writer;
		try {
			writer = new FileWriter(outFilename, append); // overwrites existing file if append == false
			ICSVWriter csvWriter = new CSVWriterBuilder(writer).build();
			String[] entries = { "scenario", "iteration", "agentType", "metric", "value" };
			if (!append) {
				// first file, so write column headers
				csvWriter.writeNext(entries);
			}

			// write metrics to file
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "meanIncome"
					+ properties.getCsvSeparator() + meanIncome).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "meanExpenses"
					+ properties.getCsvSeparator() + meanExpenses).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);

			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "householdCount"
					+ properties.getCsvSeparator() + householdCount).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "householdCountOwnerOccupied"
					+ properties.getCsvSeparator() + householdCountOwnerOccupied).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "debtFreeCount"
					+ properties.getCsvSeparator() + debtFreeCount).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "debtFree"
					+ properties.getCsvSeparator() + debtFree).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "mtgCostsOver30pcCount"
					+ properties.getCsvSeparator() + mtgCostsOver30pcCount).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator()
					+ "mtgCostsOver30pcCountOwnerOccupied" + properties.getCsvSeparator()
					+ mtgCostsOver30pcCountOwnerOccupied).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "mtgCostsOver30pcDecile1"
					+ properties.getCsvSeparator() + mtgCostsOver30pcDecile1).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "mtgCostsOver30pcDecile2"
					+ properties.getCsvSeparator() + mtgCostsOver30pcDecile2).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "mtgCostsOver30pcDecile3"
					+ properties.getCsvSeparator() + mtgCostsOver30pcDecile3).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "mtgCostsOver30pcDecile4"
					+ properties.getCsvSeparator() + mtgCostsOver30pcDecile4).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "mtgCostsOver30pcDecile5"
					+ properties.getCsvSeparator() + mtgCostsOver30pcDecile5).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "mtgCostsOver30pcDecile6"
					+ properties.getCsvSeparator() + mtgCostsOver30pcDecile6).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "mtgCostsOver30pcDecile7"
					+ properties.getCsvSeparator() + mtgCostsOver30pcDecile7).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "mtgCostsOver30pcDecile8"
					+ properties.getCsvSeparator() + mtgCostsOver30pcDecile8).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "mtgCostsOver30pcDecile9"
					+ properties.getCsvSeparator() + mtgCostsOver30pcDecile9).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "mtgCostsOver30pcDecile10"
					+ properties.getCsvSeparator() + mtgCostsOver30pcDecile10).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "rentCostsOver30pcDecile1"
					+ properties.getCsvSeparator() + rentCostsOver30pcDecile1).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "rentCostsOver30pcDecile2"
					+ properties.getCsvSeparator() + rentCostsOver30pcDecile2).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "rentCostsOver30pcDecile3"
					+ properties.getCsvSeparator() + rentCostsOver30pcDecile3).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "rentCostsOver30pcDecile4"
					+ properties.getCsvSeparator() + rentCostsOver30pcDecile4).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "rentCostsOver30pcDecile5"
					+ properties.getCsvSeparator() + rentCostsOver30pcDecile5).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "rentCostsOver30pcDecile6"
					+ properties.getCsvSeparator() + rentCostsOver30pcDecile6).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "rentCostsOver30pcDecile7"
					+ properties.getCsvSeparator() + rentCostsOver30pcDecile7).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "rentCostsOver30pcDecile8"
					+ properties.getCsvSeparator() + rentCostsOver30pcDecile8).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "rentCostsOver30pcDecile9"
					+ properties.getCsvSeparator() + rentCostsOver30pcDecile9).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "rentCostsOver30pcDecile10"
					+ properties.getCsvSeparator() + rentCostsOver30pcDecile10).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);

			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "mtgCostsOver30pc"
					+ properties.getCsvSeparator() + mtgCostsOver30pc).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator()
					+ "mtgCostsOver30pcOwnerOccupied" + properties.getCsvSeparator() + mtgCostsOver30pcOwnerOccupied)
							.split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "housingCostsOver30pc"
					+ properties.getCsvSeparator() + housingCostsOver30pc).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "housingCostsOver30pcTop1pc"
					+ properties.getCsvSeparator() + housingCostsOver30pcTop1pc).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "housingCostsOver30pcTop5pc"
					+ properties.getCsvSeparator() + housingCostsOver30pcTop5pc).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator()
					+ "housingCostsOver30pcBottom95pc" + properties.getCsvSeparator() + housingCostsOver30pcBottom95pc)
							.split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator()
					+ "totalIncomePercentEarnedByTop1pc" + properties.getCsvSeparator()
					+ totalIncomePercentEarnedByTop1pc).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator()
					+ "totalIncomePercentEarnedByTop5pc" + properties.getCsvSeparator()
					+ totalIncomePercentEarnedByTop5pc).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator()
					+ "totalIncomePercentEarnedByDecile1" + properties.getCsvSeparator()
					+ totalIncomePercentEarnedByDecile1).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator()
					+ "totalIncomePercentEarnedByDecile2" + properties.getCsvSeparator()
					+ totalIncomePercentEarnedByDecile2).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator()
					+ "totalIncomePercentEarnedByDecile3" + properties.getCsvSeparator()
					+ totalIncomePercentEarnedByDecile3).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator()
					+ "totalIncomePercentEarnedByDecile4" + properties.getCsvSeparator()
					+ totalIncomePercentEarnedByDecile4).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator()
					+ "totalIncomePercentEarnedByDecile5" + properties.getCsvSeparator()
					+ totalIncomePercentEarnedByDecile5).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator()
					+ "totalIncomePercentEarnedByDecile6" + properties.getCsvSeparator()
					+ totalIncomePercentEarnedByDecile6).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator()
					+ "totalIncomePercentEarnedByDecile7" + properties.getCsvSeparator()
					+ totalIncomePercentEarnedByDecile7).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator()
					+ "totalIncomePercentEarnedByDecile8" + properties.getCsvSeparator()
					+ totalIncomePercentEarnedByDecile8).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator()
					+ "totalIncomePercentEarnedByDecile9" + properties.getCsvSeparator()
					+ totalIncomePercentEarnedByDecile9).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator()
					+ "totalIncomePercentEarnedByDecile10" + properties.getCsvSeparator()
					+ totalIncomePercentEarnedByDecile10).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);

			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "debtToIncome"
					+ properties.getCsvSeparator() + debtToIncome).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "debtToIncomeTop1pc"
					+ properties.getCsvSeparator() + debtToIncomeTop1pc).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "debtToIncomeTop5pc"
					+ properties.getCsvSeparator() + debtToIncomeTop5pc).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "debtToIncomeBottom95pc"
					+ properties.getCsvSeparator() + debtToIncomeBottom95pc).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "debtToIncomeDecile1"
					+ properties.getCsvSeparator() + debtToIncomeDecile1).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "debtToIncomeDecile2"
					+ properties.getCsvSeparator() + debtToIncomeDecile2).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "debtToIncomeDecile3"
					+ properties.getCsvSeparator() + debtToIncomeDecile3).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "debtToIncomeDecile4"
					+ properties.getCsvSeparator() + debtToIncomeDecile4).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "debtToIncomeDecile5"
					+ properties.getCsvSeparator() + debtToIncomeDecile5).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "debtToIncomeDecile6"
					+ properties.getCsvSeparator() + debtToIncomeDecile6).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "debtToIncomeDecile7"
					+ properties.getCsvSeparator() + debtToIncomeDecile7).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "debtToIncomeDecile8"
					+ properties.getCsvSeparator() + debtToIncomeDecile8).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "debtToIncomeDecile9"
					+ properties.getCsvSeparator() + debtToIncomeDecile9).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "debtToIncomeDecile10"
					+ properties.getCsvSeparator() + debtToIncomeDecile10).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);

			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "debtToNetWorth"
					+ properties.getCsvSeparator() + debtToNetWorth).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "debtToNetWorthTop1pc"
					+ properties.getCsvSeparator() + debtToNetWorthTop1pc).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "debtToNetWorthTop5pc"
					+ properties.getCsvSeparator() + debtToNetWorthTop5pc).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "debtToNetWorthBottom95pc"
					+ properties.getCsvSeparator() + debtToNetWorthBottom95pc).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "wealthPercentHeldByTop1pc"
					+ properties.getCsvSeparator() + wealthPercentHeldByTop1pc).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "wealthPercentHeldByTop5pc"
					+ properties.getCsvSeparator() + wealthPercentHeldByTop5pc).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "incomeToHendersonPercent"
					+ properties.getCsvSeparator() + incomeToHendersonPercent).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator()
					+ "incomeToHendersonPercentTop1pc" + properties.getCsvSeparator() + incomeToHendersonPercentTop1pc)
							.split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator()
					+ "incomeToHendersonPercentTop5pc" + properties.getCsvSeparator() + incomeToHendersonPercentTop5pc)
							.split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator()
					+ "incomeToHendersonPercentBottom95pc" + properties.getCsvSeparator()
					+ incomeToHendersonPercentBottom95pc).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator()
					+ "incomeToHendersonPercentDecile1" + properties.getCsvSeparator()
					+ incomeToHendersonPercentDecile1).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator()
					+ "incomeToHendersonPercentDecile2" + properties.getCsvSeparator()
					+ incomeToHendersonPercentDecile2).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator()
					+ "incomeToHendersonPercentDecile3" + properties.getCsvSeparator()
					+ incomeToHendersonPercentDecile3).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator()
					+ "incomeToHendersonPercentDecile4" + properties.getCsvSeparator()
					+ incomeToHendersonPercentDecile4).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator()
					+ "incomeToHendersonPercentDecile5" + properties.getCsvSeparator()
					+ incomeToHendersonPercentDecile5).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator()
					+ "incomeToHendersonPercentDecile6" + properties.getCsvSeparator()
					+ incomeToHendersonPercentDecile6).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator()
					+ "incomeToHendersonPercentDecile7" + properties.getCsvSeparator()
					+ incomeToHendersonPercentDecile7).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator()
					+ "incomeToHendersonPercentDecile8" + properties.getCsvSeparator()
					+ incomeToHendersonPercentDecile8).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator()
					+ "incomeToHendersonPercentDecile9" + properties.getCsvSeparator()
					+ incomeToHendersonPercentDecile9).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator()
					+ "incomeToHendersonPercentDecile10" + properties.getCsvSeparator()
					+ incomeToHendersonPercentDecile10).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator()
					+ "incomeToHendersonAndHousingPercentDecile1" + properties.getCsvSeparator()
					+ incomeToHendersonAndHousingPercentDecile1).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator()
					+ "incomeToHendersonAndHousingPercentDecile2" + properties.getCsvSeparator()
					+ incomeToHendersonAndHousingPercentDecile2).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator()
					+ "incomeToHendersonAndHousingPercentDecile3" + properties.getCsvSeparator()
					+ incomeToHendersonAndHousingPercentDecile3).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator()
					+ "incomeToHendersonAndHousingPercentDecile4" + properties.getCsvSeparator()
					+ incomeToHendersonAndHousingPercentDecile4).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator()
					+ "incomeToHendersonAndHousingPercentDecile5" + properties.getCsvSeparator()
					+ incomeToHendersonAndHousingPercentDecile5).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator()
					+ "incomeToHendersonAndHousingPercentDecile6" + properties.getCsvSeparator()
					+ incomeToHendersonAndHousingPercentDecile6).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator()
					+ "incomeToHendersonAndHousingPercentDecile7" + properties.getCsvSeparator()
					+ incomeToHendersonAndHousingPercentDecile7).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator()
					+ "incomeToHendersonAndHousingPercentDecile8" + properties.getCsvSeparator()
					+ incomeToHendersonAndHousingPercentDecile8).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator()
					+ "incomeToHendersonAndHousingPercentDecile9" + properties.getCsvSeparator()
					+ incomeToHendersonAndHousingPercentDecile9).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator()
					+ "incomeToHendersonAndHousingPercentDecile10" + properties.getCsvSeparator()
					+ incomeToHendersonAndHousingPercentDecile10).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);

			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "housingCostsOver30pcDecile1"
					+ properties.getCsvSeparator() + housingCostsOver30pcDecile1).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "housingCostsOver30pcDecile2"
					+ properties.getCsvSeparator() + housingCostsOver30pcDecile2).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "housingCostsOver30pcDecile3"
					+ properties.getCsvSeparator() + housingCostsOver30pcDecile3).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "housingCostsOver30pcDecile4"
					+ properties.getCsvSeparator() + housingCostsOver30pcDecile4).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "housingCostsOver30pcDecile5"
					+ properties.getCsvSeparator() + housingCostsOver30pcDecile5).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "housingCostsOver30pcDecile6"
					+ properties.getCsvSeparator() + housingCostsOver30pcDecile6).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "housingCostsOver30pcDecile7"
					+ properties.getCsvSeparator() + housingCostsOver30pcDecile7).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "housingCostsOver30pcDecile8"
					+ properties.getCsvSeparator() + housingCostsOver30pcDecile8).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "housingCostsOver30pcDecile9"
					+ properties.getCsvSeparator() + housingCostsOver30pcDecile9).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "housingCostsOver30pcDecile10"
					+ properties.getCsvSeparator() + housingCostsOver30pcDecile10).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);

			writer.close();
		} catch (IOException e) {
			// new FileWriter
			e.printStackTrace();
		} finally {
			writer = null;
		}
	}

}
