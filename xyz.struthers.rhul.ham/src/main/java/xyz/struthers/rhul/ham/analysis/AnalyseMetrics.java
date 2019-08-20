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
		/*
		 * System.out.println(new Date(System.currentTimeMillis()) +
		 * ": processing Baseline_SUMMARY_Household_000.csv");
		 * processHouseholdMetricsCsv(
		 * "D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\Baseline_SUMMARY_Household_000.csv"
		 * , "Baseline", false, 0);
		 * 
		 * // append to file System.out.println(new Date(System.currentTimeMillis()) +
		 * ": processing Baseline_SUMMARY_Household_012.csv");
		 * processHouseholdMetricsCsv(
		 * "D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\Baseline_SUMMARY_Household_012.csv"
		 * , "Baseline", true, 12); System.out.println(new
		 * Date(System.currentTimeMillis()) +
		 * ": processing Baseline-01_SUMMARY_Household_000.csv");
		 * processHouseholdMetricsCsv(
		 * "D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\Baseline-01_SUMMARY_Household_000.csv"
		 * , "Baseline-01", true, 0); System.out.println(new
		 * Date(System.currentTimeMillis()) +
		 * ": processing Baseline-01_SUMMARY_Household_012.csv");
		 * processHouseholdMetricsCsv(
		 * "D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\Baseline-01_SUMMARY_Household_012.csv"
		 * , "Baseline-01", true, 12); System.out.println(new
		 * Date(System.currentTimeMillis()) +
		 * ": processing Baseline-02_SUMMARY_Household_000.csv");
		 * processHouseholdMetricsCsv(
		 * "D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\Baseline-02_SUMMARY_Household_000.csv"
		 * , "Baseline-02", true, 0); System.out.println(new
		 * Date(System.currentTimeMillis()) +
		 * ": processing Baseline-02_SUMMARY_Household_012.csv");
		 * processHouseholdMetricsCsv(
		 * "D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\Baseline-02_SUMMARY_Household_012.csv"
		 * , "Baseline-02", true, 12); System.out.println(new
		 * Date(System.currentTimeMillis()) +
		 * ": processing Baseline-03_SUMMARY_Household_000.csv");
		 * processHouseholdMetricsCsv(
		 * "D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\Baseline-03_SUMMARY_Household_000.csv"
		 * , "Baseline-03", true, 0); System.out.println(new
		 * Date(System.currentTimeMillis()) +
		 * ": processing Baseline-03_SUMMARY_Household_012.csv");
		 * processHouseholdMetricsCsv(
		 * "D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\Baseline-03_SUMMARY_Household_012.csv"
		 * , "Baseline-03", true, 12); System.out.println(new
		 * Date(System.currentTimeMillis()) +
		 * ": processing Baseline-04_SUMMARY_Household_000.csv");
		 * processHouseholdMetricsCsv(
		 * "D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\Baseline-04_SUMMARY_Household_000.csv"
		 * , "Baseline-04", true, 0); System.out.println(new
		 * Date(System.currentTimeMillis()) +
		 * ": processing Baseline-04_SUMMARY_Household_012.csv");
		 * processHouseholdMetricsCsv(
		 * "D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\Baseline-04_SUMMARY_Household_012.csv"
		 * , "Baseline-04", true, 12); System.out.println(new
		 * Date(System.currentTimeMillis()) +
		 * ": processing Baseline-05_SUMMARY_Household_000.csv");
		 * processHouseholdMetricsCsv(
		 * "D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\Baseline-05_SUMMARY_Household_000.csv"
		 * , "Baseline-05", true, 0); System.out.println(new
		 * Date(System.currentTimeMillis()) +
		 * ": processing Baseline-05_SUMMARY_Household_012.csv");
		 * processHouseholdMetricsCsv(
		 * "D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\Baseline-05_SUMMARY_Household_012.csv"
		 * , "Baseline-05", true, 12); System.out.println(new
		 * Date(System.currentTimeMillis()) +
		 * ": processing Baseline-06_SUMMARY_Household_000.csv");
		 * processHouseholdMetricsCsv(
		 * "D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\Baseline-06_SUMMARY_Household_000.csv"
		 * , "Baseline-06", true, 0); System.out.println(new
		 * Date(System.currentTimeMillis()) +
		 * ": processing Baseline-06_SUMMARY_Household_012.csv");
		 * processHouseholdMetricsCsv(
		 * "D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\Baseline-06_SUMMARY_Household_012.csv"
		 * , "Baseline-06", true, 12); System.out.println(new
		 * Date(System.currentTimeMillis()) +
		 * ": processing Baseline-07_SUMMARY_Household_000.csv");
		 * processHouseholdMetricsCsv(
		 * "D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\Baseline-07_SUMMARY_Household_000.csv"
		 * , "Baseline-07", true, 0); System.out.println(new
		 * Date(System.currentTimeMillis()) +
		 * ": processing Baseline-07_SUMMARY_Household_012.csv");
		 * processHouseholdMetricsCsv(
		 * "D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\Baseline-07_SUMMARY_Household_012.csv"
		 * , "Baseline-07", true, 12); System.out.println(new
		 * Date(System.currentTimeMillis()) +
		 * ": processing Baseline-08_SUMMARY_Household_000.csv");
		 * processHouseholdMetricsCsv(
		 * "D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\Baseline-08_SUMMARY_Household_000.csv"
		 * , "Baseline-08", true, 0); System.out.println(new
		 * Date(System.currentTimeMillis()) +
		 * ": processing Baseline-08_SUMMARY_Household_012.csv");
		 * processHouseholdMetricsCsv(
		 * "D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\Baseline-08_SUMMARY_Household_012.csv"
		 * , "Baseline-08", true, 12); System.out.println(new
		 * Date(System.currentTimeMillis()) +
		 * ": processing Baseline-09_SUMMARY_Household_000.csv");
		 * processHouseholdMetricsCsv(
		 * "D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\Baseline-09_SUMMARY_Household_000.csv"
		 * , "Baseline-09", true, 0); System.out.println(new
		 * Date(System.currentTimeMillis()) +
		 * ": processing Baseline-09_SUMMARY_Household_012.csv");
		 * processHouseholdMetricsCsv(
		 * "D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\Baseline-09_SUMMARY_Household_012.csv"
		 * , "Baseline-09", true, 12);
		 */
		// country currency crashes
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing FX-Rates-10pc-CNY_SUMMARY_Household_000.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\FX-Rates-10pc-CNY_SUMMARY_Household_000.csv",
				"FX-Rates-10pc-CNY", true, 0);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing FX-Rates-10pc-CNY_SUMMARY_Household_012.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\FX-Rates-10pc-CNY_SUMMARY_Household_012.csv",
				"FX-Rates-10pc-CNY", true, 12);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing FX-Rates-10pc-EUR_SUMMARY_Household_000.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\FX-Rates-10pc-EUR_SUMMARY_Household_000.csv",
				"FX-Rates-10pc-EUR", true, 0);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing FX-Rates-10pc-EUR_SUMMARY_Household_012.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\FX-Rates-10pc-EUR_SUMMARY_Household_012.csv",
				"FX-Rates-10pc-EUR", true, 12);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing FX-Rates-10pc-INR_SUMMARY_Household_000.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\FX-Rates-10pc-INR_SUMMARY_Household_000.csv",
				"FX-Rates-10pc-INR", true, 0);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing FX-Rates-10pc-INR_SUMMARY_Household_012.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\FX-Rates-10pc-INR_SUMMARY_Household_012.csv",
				"FX-Rates-10pc-INR", true, 12);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing FX-Rates-10pc-JPY_SUMMARY_Household_000.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\FX-Rates-10pc-JPY_SUMMARY_Household_000.csv",
				"FX-Rates-10pc-JPY", true, 0);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing FX-Rates-10pc-JPY_SUMMARY_Household_012.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\FX-Rates-10pc-JPY_SUMMARY_Household_012.csv",
				"FX-Rates-10pc-JPY", true, 12);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing FX-Rates-10pc-KRW_SUMMARY_Household_000.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\FX-Rates-10pc-KRW_SUMMARY_Household_000.csv",
				"FX-Rates-10pc-KRW", true, 0);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing FX-Rates-10pc-KRW_SUMMARY_Household_012.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\FX-Rates-10pc-KRW_SUMMARY_Household_012.csv",
				"FX-Rates-10pc-KRW", true, 12);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing FX-Rates-10pc-MYR_SUMMARY_Household_000.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\FX-Rates-10pc-MYR_SUMMARY_Household_000.csv",
				"FX-Rates-10pc-MYR", true, 0);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing FX-Rates-10pc-MYR_SUMMARY_Household_012.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\FX-Rates-10pc-MYR_SUMMARY_Household_012.csv",
				"FX-Rates-10pc-MYR", true, 12);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing FX-Rates-10pc-NZD_SUMMARY_Household_000.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\FX-Rates-10pc-NZD_SUMMARY_Household_000.csv",
				"FX-Rates-10pc-NZD", true, 0);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing FX-Rates-10pc-NZD_SUMMARY_Household_012.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\FX-Rates-10pc-NZD_SUMMARY_Household_012.csv",
				"FX-Rates-10pc-NZD", true, 12);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing FX-Rates-10pc-SGD_SUMMARY_Household_000.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\FX-Rates-10pc-SGD_SUMMARY_Household_000.csv",
				"FX-Rates-10pc-SGD", true, 0);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing FX-Rates-10pc-SGD_SUMMARY_Household_012.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\FX-Rates-10pc-SGD_SUMMARY_Household_012.csv",
				"FX-Rates-10pc-SGD", true, 12);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing FX-Rates-10pc-THB_SUMMARY_Household_000.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\FX-Rates-10pc-THB_SUMMARY_Household_000.csv",
				"FX-Rates-10pc-THB", true, 0);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing FX-Rates-10pc-THB_SUMMARY_Household_012.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\FX-Rates-10pc-THB_SUMMARY_Household_012.csv",
				"FX-Rates-10pc-THB", true, 12);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing FX-Rates-10pc-USD_SUMMARY_Household_000.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\FX-Rates-10pc-USD_SUMMARY_Household_000.csv",
				"FX-Rates-10pc-USD", true, 0);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing FX-Rates-10pc-USD_SUMMARY_Household_012.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\FX-Rates-10pc-USD_SUMMARY_Household_012.csv",
				"FX-Rates-10pc-USD", true, 12);

		// inflation rates
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing Inflation-01pc_SUMMARY_Household_000.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\Inflation-01pc_SUMMARY_Household_000.csv",
				"Inflation-01pc", true, 0);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing Inflation-01pc_SUMMARY_Household_012.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\Inflation-01pc_SUMMARY_Household_012.csv",
				"Inflation-01pc", true, 12);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing Inflation-02pc_SUMMARY_Household_000.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\Inflation-02pc_SUMMARY_Household_000.csv",
				"Inflation-02pc", true, 0);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing Inflation-02pc_SUMMARY_Household_012.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\Inflation-02pc_SUMMARY_Household_012.csv",
				"Inflation-02pc", true, 12);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing Inflation-03pc_SUMMARY_Household_000.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\Inflation-03pc_SUMMARY_Household_000.csv",
				"Inflation-03pc", true, 0);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing Inflation-03pc_SUMMARY_Household_012.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\Inflation-03pc_SUMMARY_Household_012.csv",
				"Inflation-03pc", true, 12);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing Inflation-04pc_SUMMARY_Household_000.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\Inflation-04pc_SUMMARY_Household_000.csv",
				"Inflation-04pc", true, 0);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing Inflation-04pc_SUMMARY_Household_012.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\Inflation-04pc_SUMMARY_Household_012.csv",
				"Inflation-04pc", true, 12);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing Inflation-05pc_SUMMARY_Household_000.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\Inflation-05pc_SUMMARY_Household_000.csv",
				"Inflation-05pc", true, 0);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing Inflation-05pc_SUMMARY_Household_012.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\Inflation-05pc_SUMMARY_Household_012.csv",
				"Inflation-05pc", true, 12);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing Inflation-10pc_SUMMARY_Household_000.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\Inflation-10pc_SUMMARY_Household_000.csv",
				"Inflation-10pc", true, 0);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing Inflation-10pc_SUMMARY_Household_012.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\Inflation-10pc_SUMMARY_Household_012.csv",
				"Inflation-10pc", true, 12);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing Inflation-15pc_SUMMARY_Household_000.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\Inflation-15pc_SUMMARY_Household_000.csv",
				"Inflation-15pc", true, 0);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing Inflation-15pc_SUMMARY_Household_012.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\Inflation-15pc_SUMMARY_Household_012.csv",
				"Inflation-15pc", true, 12);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing Inflation-20pc_SUMMARY_Household_000.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\Inflation-20pc_SUMMARY_Household_000.csv",
				"Inflation-20pc", true, 0);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing Inflation-20pc_SUMMARY_Household_012.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\Inflation-20pc_SUMMARY_Household_012.csv",
				"Inflation-20pc", true, 12);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing Inflation-25pc_SUMMARY_Household_000.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\Inflation-25pc_SUMMARY_Household_000.csv",
				"Inflation-25pc", true, 0);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing Inflation-25pc_SUMMARY_Household_012.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\Inflation-25pc_SUMMARY_Household_012.csv",
				"Inflation-25pc", true, 12);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing Inflation-50pc_SUMMARY_Household_000.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\Inflation-50pc_SUMMARY_Household_000.csv",
				"Inflation-50pc", true, 0);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing Inflation-50pc_SUMMARY_Household_012.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\Inflation-50pc_SUMMARY_Household_012.csv",
				"Inflation-50pc", true, 12);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing Inflation-75pc_SUMMARY_Household_000.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\Inflation-75pc_SUMMARY_Household_000.csv",
				"Inflation-75pc", true, 0);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing Inflation-75pc_SUMMARY_Household_012.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\Inflation-75pc_SUMMARY_Household_012.csv",
				"Inflation-75pc", true, 12);
	}

	private static void processHouseholdMetricsCsv(String inFileResourceLocation, String scenario, boolean append,
			int iteration) {

		// overall mean metrics (to test model error)
		float meanIncome = 0f;
		float meanExpenses = 0f;
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
		float debtToIncome = 0f; // household debt-to-income ratio (Kumhof, et al, 2015)
		float debtToIncomeTop1pc = 0f;
		float debtToIncomeTop5pc = 0f;
		float debtToIncomeBottom95pc = 0f;
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

		// local working variables
		int householdCount = 0;
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
		float totalIncome = 0f;
		float totalIncomeTop1pc = 0f;
		float totalIncomeTop5pc = 0f;
		float totalIncomeBottom95pc = 0f;
		float totalExpenses = 0f;
		float totalNetWorth = 0f;
		float totalNetWorthTop1pc = 0f;
		float totalNetWorthTop5pc = 0f;
		float totalNetWorthBottom95pc = 0f;
		float totalHenderson = 0f;
		float totalHendersonTop1pc = 0f;
		float totalHendersonTop5pc = 0f;
		float totalHendersonBottom95pc = 0f;

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

		// read CSV file, sort by income, and determine 5% and 1% income thresholds
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

		// read CSV filea second time, and calculate metrics
		reader = null;
		try {
			Reader fr = new FileReader(inFileResourceLocation);
			reader = new CSVReader(fr);
			String[] line = reader.readNext(); // read and discard header row
			while ((line = reader.readNext()) != null) {
				try {
					// get values for this household
					float income = Float.valueOf(line[6].replace(",", ""));
					float housingCosts = Float.valueOf(line[10].replace(",", ""))
							+ Float.valueOf(line[11].replace(",", ""));
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
						if (housingCosts > 0.3 * income) {
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
							if (housingCosts > 0.3 * income) {
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
						if (housingCosts > 0.3 * income) {
							// mortgage distress in bottom 95% of income
							housingCostsOver30pcCountBottom95pc++;
						}
					}
					if (housingCosts > 0.3 * income) {
						// mortgage distress
						housingCostsOver30pcCount++;
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
						if (housingCosts > 0.3 * income) {
							housingCostsOver30pcCountDecile1++;
						}
					} else if (income > decileThreshold2) {
						householdCountDecile2++;
						if (housingCosts > 0.3 * income) {
							housingCostsOver30pcCountDecile2++;
						}
					} else if (income > decileThreshold3) {
						householdCountDecile3++;
						if (housingCosts > 0.3 * income) {
							housingCostsOver30pcCountDecile3++;
						}
					} else if (income > decileThreshold4) {
						householdCountDecile4++;
						if (housingCosts > 0.3 * income) {
							housingCostsOver30pcCountDecile4++;
						}
					} else if (income > decileThreshold5) {
						householdCountDecile5++;
						if (housingCosts > 0.3 * income) {
							housingCostsOver30pcCountDecile5++;
						}
					} else if (income > decileThreshold6) {
						householdCountDecile6++;
						if (housingCosts > 0.3 * income) {
							housingCostsOver30pcCountDecile6++;
						}
					} else if (income > decileThreshold7) {
						householdCountDecile7++;
						if (housingCosts > 0.3 * income) {
							housingCostsOver30pcCountDecile7++;
						}
					} else if (income > decileThreshold8) {
						householdCountDecile8++;
						if (housingCosts > 0.3 * income) {
							housingCostsOver30pcCountDecile8++;
						}
					} else if (income > decileThreshold9) {
						householdCountDecile9++;
						if (housingCosts > 0.3 * income) {
							housingCostsOver30pcCountDecile9++;
						}
					} else {
						if (housingCosts > 0.3 * income) {
							housingCostsOver30pcCountDecile10++;
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
		meanIncome = totalIncome / householdCount;
		meanExpenses = totalExpenses / householdCount;
		housingCostsOver30pc = housingCostsOver30pcCount / Float.valueOf(householdCount);
		housingCostsOver30pcTop5pc = housingCostsOver30pcCountTop5pc / Float.valueOf(householdCountTop5pc);
		housingCostsOver30pcTop1pc = housingCostsOver30pcCountTop1pc / Float.valueOf(householdCountTop1pc);
		housingCostsOver30pcBottom95pc = housingCostsOver30pcCountBottom95pc
				/ Float.valueOf(householdCount - householdCountTop5pc);
		totalIncomePercentEarnedByTop1pc = totalIncomeTop1pc / totalIncome;
		totalIncomePercentEarnedByTop5pc = totalIncomeTop5pc / totalIncome;
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

		housingCostsOver30pcDecile1 = housingCostsOver30pcCountDecile1 / householdCountDecile1;
		housingCostsOver30pcDecile2 = housingCostsOver30pcCountDecile2 / householdCountDecile2;
		housingCostsOver30pcDecile3 = housingCostsOver30pcCountDecile3 / householdCountDecile3;
		housingCostsOver30pcDecile4 = housingCostsOver30pcCountDecile4 / householdCountDecile4;
		housingCostsOver30pcDecile5 = housingCostsOver30pcCountDecile5 / householdCountDecile5;
		housingCostsOver30pcDecile6 = housingCostsOver30pcCountDecile6 / householdCountDecile6;
		housingCostsOver30pcDecile7 = housingCostsOver30pcCountDecile7 / householdCountDecile7;
		housingCostsOver30pcDecile8 = housingCostsOver30pcCountDecile8 / householdCountDecile8;
		housingCostsOver30pcDecile9 = housingCostsOver30pcCountDecile9 / householdCountDecile9;
		housingCostsOver30pcDecile10 = housingCostsOver30pcCountDecile10 / (householdCount - householdCountDecile9
				- householdCountDecile8 - householdCountDecile7 - householdCountDecile6 - householdCountDecile5
				- householdCountDecile4 - householdCountDecile3 - householdCountDecile2 - householdCountDecile1);

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
