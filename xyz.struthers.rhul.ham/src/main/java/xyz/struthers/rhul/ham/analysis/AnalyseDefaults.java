package xyz.struthers.rhul.ham.analysis;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.text.DecimalFormat;
import java.util.Date;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;

import xyz.struthers.rhul.ham.config.PropertiesXml;
import xyz.struthers.rhul.ham.config.PropertiesXmlFactory;

/**
 * Doesn't transform the records - just copies them into the one CSV file.
 * 
 * @author Adam Struthers
 * @since 2019-08-21
 */
public class AnalyseDefaults {

	private static PropertiesXml properties;

	public AnalyseDefaults() {
		super();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		PropertiesXmlFactory.propertiesXmlFilename = "D:\\compham-config\\4.1_baseline.xml";
		properties = PropertiesXmlFactory.getProperties();

		// FIXME: defaults haven't been included in the FX and inflation scenarios yet

		// overwrite existing file

		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline_SUMMARY_Household_000.csv");
		processDefaultsCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline_SUMMARY_Household_000.csv",
				"Baseline", false, 0);

		// append to file

		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline_SUMMARY_Household_012.csv");
		processDefaultsCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline_SUMMARY_Household_012.csv",
				"Baseline", true, 12);
		/*
		 * System.out.println(new Date(System.currentTimeMillis()) +
		 * ": processing Baseline-01_SUMMARY_Household_000.csv");
		 * processDefaultsCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-01_SUMMARY_Household_000.csv"
		 * , "Baseline-01", true, 0); System.out.println(new
		 * Date(System.currentTimeMillis()) +
		 * ": processing Baseline-01_SUMMARY_Household_012.csv");
		 * processDefaultsCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-01_SUMMARY_Household_012.csv"
		 * , "Baseline-01", true, 12); System.out.println(new
		 * Date(System.currentTimeMillis()) +
		 * ": processing Baseline-02_SUMMARY_Household_000.csv");
		 * processDefaultsCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-02_SUMMARY_Household_000.csv"
		 * , "Baseline-02", true, 0); System.out.println(new
		 * Date(System.currentTimeMillis()) +
		 * ": processing Baseline-02_SUMMARY_Household_012.csv");
		 * processDefaultsCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-02_SUMMARY_Household_012.csv"
		 * , "Baseline-02", true, 12); System.out.println(new
		 * Date(System.currentTimeMillis()) +
		 * ": processing Baseline-03_SUMMARY_Household_000.csv");
		 * processDefaultsCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-03_SUMMARY_Household_000.csv"
		 * , "Baseline-03", true, 0); System.out.println(new
		 * Date(System.currentTimeMillis()) +
		 * ": processing Baseline-03_SUMMARY_Household_012.csv");
		 * processDefaultsCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-03_SUMMARY_Household_012.csv"
		 * , "Baseline-03", true, 12); System.out.println(new
		 * Date(System.currentTimeMillis()) +
		 * ": processing Baseline-04_SUMMARY_Household_000.csv");
		 * processDefaultsCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-04_SUMMARY_Household_000.csv"
		 * , "Baseline-04", true, 0); System.out.println(new
		 * Date(System.currentTimeMillis()) +
		 * ": processing Baseline-04_SUMMARY_Household_012.csv");
		 * processDefaultsCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-04_SUMMARY_Household_012.csv"
		 * , "Baseline-04", true, 12); System.out.println(new
		 * Date(System.currentTimeMillis()) +
		 * ": processing Baseline-05_SUMMARY_Household_000.csv");
		 * processDefaultsCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-05_SUMMARY_Household_000.csv"
		 * , "Baseline-05", true, 0); System.out.println(new
		 * Date(System.currentTimeMillis()) +
		 * ": processing Baseline-05_SUMMARY_Household_012.csv");
		 * processDefaultsCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-05_SUMMARY_Household_012.csv"
		 * , "Baseline-05", true, 12); System.out.println(new
		 * Date(System.currentTimeMillis()) +
		 * ": processing Baseline-06_SUMMARY_Household_000.csv");
		 * processDefaultsCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-06_SUMMARY_Household_000.csv"
		 * , "Baseline-06", true, 0); System.out.println(new
		 * Date(System.currentTimeMillis()) +
		 * ": processing Baseline-06_SUMMARY_Household_012.csv");
		 * processDefaultsCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-06_SUMMARY_Household_012.csv"
		 * , "Baseline-06", true, 12); System.out.println(new
		 * Date(System.currentTimeMillis()) +
		 * ": processing Baseline-07_SUMMARY_Household_000.csv");
		 * processDefaultsCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-07_SUMMARY_Household_000.csv"
		 * , "Baseline-07", true, 0); System.out.println(new
		 * Date(System.currentTimeMillis()) +
		 * ": processing Baseline-07_SUMMARY_Household_012.csv");
		 * processDefaultsCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-07_SUMMARY_Household_012.csv"
		 * , "Baseline-07", true, 12); System.out.println(new
		 * Date(System.currentTimeMillis()) +
		 * ": processing Baseline-08_SUMMARY_Household_000.csv");
		 * processDefaultsCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-08_SUMMARY_Household_000.csv"
		 * , "Baseline-08", true, 0); System.out.println(new
		 * Date(System.currentTimeMillis()) +
		 * ": processing Baseline-08_SUMMARY_Household_012.csv");
		 * processDefaultsCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-08_SUMMARY_Household_012.csv"
		 * , "Baseline-08", true, 12); System.out.println(new
		 * Date(System.currentTimeMillis()) +
		 * ": processing Baseline-09_SUMMARY_Household_000.csv");
		 * processDefaultsCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-09_SUMMARY_Household_000.csv"
		 * , "Baseline-09", true, 0); System.out.println(new
		 * Date(System.currentTimeMillis()) +
		 * ": processing Baseline-09_SUMMARY_Household_012.csv");
		 * processDefaultsCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-09_SUMMARY_Household_012.csv"
		 * , "Baseline-09", true, 12);
		 */

		// country currency crashes
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing FX-Rates-10pc-CNY_SUMMARY_Household_000.csv");
		processDefaultsCsv(
				"D:\\OneDrive\\Dissertation\\Results\\Summary Data\\FX-Rates-10pc-CNY_SUMMARY_Household_000.csv",
				"FX-Rates-10pc-CNY", true, 0);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing FX-Rates-10pc-CNY_SUMMARY_Household_012.csv");
		processDefaultsCsv(
				"D:\\OneDrive\\Dissertation\\Results\\Summary Data\\FX-Rates-10pc-CNY_SUMMARY_Household_012.csv",
				"FX-Rates-10pc-CNY", true, 12);
		/*
		 * System.out.println( new Date(System.currentTimeMillis()) +
		 * ": processing FX-Rates-10pc-EUR_SUMMARY_Household_000.csv");
		 * processDefaultsCsv(
		 * "D:\\OneDrive\\Dissertation\\Results\\Summary Data\\FX-Rates-10pc-EUR_SUMMARY_Household_000.csv"
		 * , "FX-Rates-10pc-EUR", true, 0); System.out.println( new
		 * Date(System.currentTimeMillis()) +
		 * ": processing FX-Rates-10pc-EUR_SUMMARY_Household_012.csv");
		 * processDefaultsCsv(
		 * "D:\\OneDrive\\Dissertation\\Results\\Summary Data\\FX-Rates-10pc-EUR_SUMMARY_Household_012.csv"
		 * , "FX-Rates-10pc-EUR", true, 12); System.out.println( new
		 * Date(System.currentTimeMillis()) +
		 * ": processing FX-Rates-10pc-INR_SUMMARY_Household_000.csv");
		 * processDefaultsCsv(
		 * "D:\\OneDrive\\Dissertation\\Results\\Summary Data\\FX-Rates-10pc-INR_SUMMARY_Household_000.csv"
		 * , "FX-Rates-10pc-INR", true, 0); System.out.println( new
		 * Date(System.currentTimeMillis()) +
		 * ": processing FX-Rates-10pc-INR_SUMMARY_Household_012.csv");
		 * processDefaultsCsv(
		 * "D:\\OneDrive\\Dissertation\\Results\\Summary Data\\FX-Rates-10pc-INR_SUMMARY_Household_012.csv"
		 * , "FX-Rates-10pc-INR", true, 12); System.out.println( new
		 * Date(System.currentTimeMillis()) +
		 * ": processing FX-Rates-10pc-JPY_SUMMARY_Household_000.csv");
		 * processDefaultsCsv(
		 * "D:\\OneDrive\\Dissertation\\Results\\Summary Data\\FX-Rates-10pc-JPY_SUMMARY_Household_000.csv"
		 * , "FX-Rates-10pc-JPY", true, 0); System.out.println( new
		 * Date(System.currentTimeMillis()) +
		 * ": processing FX-Rates-10pc-JPY_SUMMARY_Household_012.csv");
		 * processDefaultsCsv(
		 * "D:\\OneDrive\\Dissertation\\Results\\Summary Data\\FX-Rates-10pc-JPY_SUMMARY_Household_012.csv"
		 * , "FX-Rates-10pc-JPY", true, 12); System.out.println( new
		 * Date(System.currentTimeMillis()) +
		 * ": processing FX-Rates-10pc-KRW_SUMMARY_Household_000.csv");
		 * processDefaultsCsv(
		 * "D:\\OneDrive\\Dissertation\\Results\\Summary Data\\FX-Rates-10pc-KRW_SUMMARY_Household_000.csv"
		 * , "FX-Rates-10pc-KRW", true, 0); System.out.println( new
		 * Date(System.currentTimeMillis()) +
		 * ": processing FX-Rates-10pc-KRW_SUMMARY_Household_012.csv");
		 * processDefaultsCsv(
		 * "D:\\OneDrive\\Dissertation\\Results\\Summary Data\\FX-Rates-10pc-KRW_SUMMARY_Household_012.csv"
		 * , "FX-Rates-10pc-KRW", true, 12); System.out.println( new
		 * Date(System.currentTimeMillis()) +
		 * ": processing FX-Rates-10pc-MYR_SUMMARY_Household_000.csv");
		 * processDefaultsCsv(
		 * "D:\\OneDrive\\Dissertation\\Results\\Summary Data\\FX-Rates-10pc-MYR_SUMMARY_Household_000.csv"
		 * , "FX-Rates-10pc-MYR", true, 0); System.out.println( new
		 * Date(System.currentTimeMillis()) +
		 * ": processing FX-Rates-10pc-MYR_SUMMARY_Household_012.csv");
		 * processDefaultsCsv(
		 * "D:\\OneDrive\\Dissertation\\Results\\Summary Data\\FX-Rates-10pc-MYR_SUMMARY_Household_012.csv"
		 * , "FX-Rates-10pc-MYR", true, 12); System.out.println( new
		 * Date(System.currentTimeMillis()) +
		 * ": processing FX-Rates-10pc-NZD_SUMMARY_Household_000.csv");
		 * processDefaultsCsv(
		 * "D:\\OneDrive\\Dissertation\\Results\\Summary Data\\FX-Rates-10pc-NZD_SUMMARY_Household_000.csv"
		 * , "FX-Rates-10pc-NZD", true, 0); System.out.println( new
		 * Date(System.currentTimeMillis()) +
		 * ": processing FX-Rates-10pc-NZD_SUMMARY_Household_012.csv");
		 * processDefaultsCsv(
		 * "D:\\OneDrive\\Dissertation\\Results\\Summary Data\\FX-Rates-10pc-NZD_SUMMARY_Household_012.csv"
		 * , "FX-Rates-10pc-NZD", true, 12); System.out.println( new
		 * Date(System.currentTimeMillis()) +
		 * ": processing FX-Rates-10pc-SGD_SUMMARY_Household_000.csv");
		 * processDefaultsCsv(
		 * "D:\\OneDrive\\Dissertation\\Results\\Summary Data\\FX-Rates-10pc-SGD_SUMMARY_Household_000.csv"
		 * , "FX-Rates-10pc-SGD", true, 0); System.out.println( new
		 * Date(System.currentTimeMillis()) +
		 * ": processing FX-Rates-10pc-SGD_SUMMARY_Household_012.csv");
		 * processDefaultsCsv(
		 * "D:\\OneDrive\\Dissertation\\Results\\Summary Data\\FX-Rates-10pc-SGD_SUMMARY_Household_012.csv"
		 * , "FX-Rates-10pc-SGD", true, 12); System.out.println( new
		 * Date(System.currentTimeMillis()) +
		 * ": processing FX-Rates-10pc-THB_SUMMARY_Household_000.csv");
		 * processDefaultsCsv(
		 * "D:\\OneDrive\\Dissertation\\Results\\Summary Data\\FX-Rates-10pc-THB_SUMMARY_Household_000.csv"
		 * , "FX-Rates-10pc-THB", true, 0); System.out.println( new
		 * Date(System.currentTimeMillis()) +
		 * ": processing FX-Rates-10pc-THB_SUMMARY_Household_012.csv");
		 * processDefaultsCsv(
		 * "D:\\OneDrive\\Dissertation\\Results\\Summary Data\\FX-Rates-10pc-THB_SUMMARY_Household_012.csv"
		 * , "FX-Rates-10pc-THB", true, 12); System.out.println( new
		 * Date(System.currentTimeMillis()) +
		 * ": processing FX-Rates-10pc-USD_SUMMARY_Household_000.csv");
		 * processDefaultsCsv(
		 * "D:\\OneDrive\\Dissertation\\Results\\Summary Data\\FX-Rates-10pc-USD_SUMMARY_Household_000.csv"
		 * , "FX-Rates-10pc-USD", true, 0); System.out.println( new
		 * Date(System.currentTimeMillis()) +
		 * ": processing FX-Rates-10pc-USD_SUMMARY_Household_012.csv");
		 * processDefaultsCsv(
		 * "D:\\OneDrive\\Dissertation\\Results\\Summary Data\\FX-Rates-10pc-USD_SUMMARY_Household_012.csv"
		 * , "FX-Rates-10pc-USD", true, 12);
		 */

		// inflation rates

		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing Inflation-01pc_SUMMARY_Household_000.csv");
		processDefaultsCsv(
				"D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Inflation-01pc_SUMMARY_Household_000.csv",
				"Inflation-01pc", true, 0);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing Inflation-01pc_SUMMARY_Household_012.csv");
		processDefaultsCsv(
				"D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Inflation-01pc_SUMMARY_Household_012.csv",
				"Inflation-01pc", true, 12);
		/*
		 * System.out.println( new Date(System.currentTimeMillis()) +
		 * ": processing Inflation-02pc_SUMMARY_Household_000.csv"); processDefaultsCsv(
		 * "D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Inflation-02pc_SUMMARY_Household_000.csv"
		 * , "Inflation-02pc", true, 0); System.out.println( new
		 * Date(System.currentTimeMillis()) +
		 * ": processing Inflation-02pc_SUMMARY_Household_012.csv"); processDefaultsCsv(
		 * "D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Inflation-02pc_SUMMARY_Household_012.csv"
		 * , "Inflation-02pc", true, 12); System.out.println( new
		 * Date(System.currentTimeMillis()) +
		 * ": processing Inflation-03pc_SUMMARY_Household_000.csv"); processDefaultsCsv(
		 * "D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Inflation-03pc_SUMMARY_Household_000.csv"
		 * , "Inflation-03pc", true, 0); System.out.println( new
		 * Date(System.currentTimeMillis()) +
		 * ": processing Inflation-03pc_SUMMARY_Household_012.csv"); processDefaultsCsv(
		 * "D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Inflation-03pc_SUMMARY_Household_012.csv"
		 * , "Inflation-03pc", true, 12); System.out.println( new
		 * Date(System.currentTimeMillis()) +
		 * ": processing Inflation-04pc_SUMMARY_Household_000.csv"); processDefaultsCsv(
		 * "D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Inflation-04pc_SUMMARY_Household_000.csv"
		 * , "Inflation-04pc", true, 0); System.out.println( new
		 * Date(System.currentTimeMillis()) +
		 * ": processing Inflation-04pc_SUMMARY_Household_012.csv"); processDefaultsCsv(
		 * "D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Inflation-04pc_SUMMARY_Household_012.csv"
		 * , "Inflation-04pc", true, 12);
		 */
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing Inflation-05pc_SUMMARY_Household_012.csv");
		processDefaultsCsv(
				"D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Inflation-05pc_SUMMARY_Household_012.csv",
				"Inflation-05pc", true, 12);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing Inflation-10pc_SUMMARY_Household_012.csv");
		processDefaultsCsv(
				"D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Inflation-10pc_SUMMARY_Household_012.csv",
				"Inflation-10pc", true, 12);
		/*
		 * System.out.println( new Date(System.currentTimeMillis()) +
		 * ": processing Inflation-15pc_SUMMARY_Household_000.csv"); processDefaultsCsv(
		 * "D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Inflation-15pc_SUMMARY_Household_000.csv"
		 * , "Inflation-15pc", true, 0); System.out.println( new
		 * Date(System.currentTimeMillis()) +
		 * ": processing Inflation-15pc_SUMMARY_Household_012.csv"); processDefaultsCsv(
		 * "D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Inflation-15pc_SUMMARY_Household_012.csv"
		 * , "Inflation-15pc", true, 12); System.out.println( new
		 * Date(System.currentTimeMillis()) +
		 * ": processing Inflation-20pc_SUMMARY_Household_000.csv"); processDefaultsCsv(
		 * "D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Inflation-20pc_SUMMARY_Household_000.csv"
		 * , "Inflation-20pc", true, 0); System.out.println( new
		 * Date(System.currentTimeMillis()) +
		 * ": processing Inflation-20pc_SUMMARY_Household_012.csv"); processDefaultsCsv(
		 * "D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Inflation-20pc_SUMMARY_Household_012.csv"
		 * , "Inflation-20pc", true, 12);
		 */

		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing Inflation-25pc_SUMMARY_Household_012.csv");
		processDefaultsCsv(
				"D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Inflation-25pc_SUMMARY_Household_012.csv",
				"Inflation-25pc", true, 12);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing Inflation-50pc_SUMMARY_Household_012.csv");
		processDefaultsCsv(
				"D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Inflation-50pc_SUMMARY_Household_012.csv",
				"Inflation-50pc", true, 12);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing Inflation-60pc_SUMMARY_Household_012.csv");
		processDefaultsCsv(
				"D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Inflation-60pc_SUMMARY_Household_012.csv",
				"Inflation-60pc", true, 12);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing Inflation-75pc_SUMMARY_Household_012.csv");
		processDefaultsCsv(
				"D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Inflation-75pc_SUMMARY_Household_012.csv",
				"Inflation-75pc", true, 12);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing Inflation-15pc-5yrs_SUMMARY_Household_048.csv");
		processDefaultsCsv(
				"D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Inflation-15pc-5yrs_SUMMARY_Household_048.csv",
				"Inflation-15pc-4yrs", true, 48);

		System.out.println(new Date(System.currentTimeMillis()) + ": FINISHED ANALYSING DEFAULTS");
	}

	private static void processDefaultsCsv(String inFileResourceLocation, String scenario, boolean append,
			int iteration) {

		// read CSV file, identify defaults, and save to new file
		DecimalFormat wholeNumber = new DecimalFormat("000");
		String outFilename = properties.getOutputDirectory() + "R_GRAPH_Defaults.csv";
		Writer writer = null;
		CSVReader reader = null;
		try {
			writer = new FileWriter(outFilename, append); // overwrites existing file if append == false
			ICSVWriter csvWriter = new CSVWriterBuilder(writer).build();

			Reader fr = new FileReader(inFileResourceLocation);
			reader = new CSVReader(fr);
			String[] line = reader.readNext(); // read and discard header row
			while ((line = reader.readNext()) != null) {
				try {
					int defaultIteration = Integer.valueOf(line[17].replace(",", "")); // default iteration
					if (defaultIteration > 0) {
						String[] entries = new String[line.length + 2];
						for (int i = 0; i < line.length; i++) {
							entries[i] = line[i];
						}
						entries[line.length] = wholeNumber.format(iteration); // add iteration
						entries[line.length] = wholeNumber.format(inFileResourceLocation); // add filename
						csvWriter.writeNext(entries);
					}
				} catch (NumberFormatException e) {
					// do nothing and leave it as zero.
				}
			}
			writer.close();
			reader.close();
			reader = null;
		} catch (FileNotFoundException e) {
			// open file
			e.printStackTrace();
		} catch (IOException e) {
			// new FileWriter
			// read next
			e.printStackTrace();
		} finally {
			writer = null;
			reader = null;
		}
	}
}
