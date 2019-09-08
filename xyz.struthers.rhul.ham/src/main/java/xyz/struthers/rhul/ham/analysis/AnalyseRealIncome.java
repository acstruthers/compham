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

import gnu.trove.map.TIntFloatMap;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntFloatHashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import xyz.struthers.rhul.ham.config.PropertiesXml;
import xyz.struthers.rhul.ham.config.PropertiesXmlFactory;

/**
 * @author Adam Struthers
 * @since 2019-08-16
 */
public class AnalyseRealIncome {

	private static PropertiesXml properties;

	public AnalyseRealIncome() {
		super();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		PropertiesXmlFactory.propertiesXmlFilename = "D:\\compham-config\\4.1_baseline.xml";
		properties = PropertiesXmlFactory.getProperties();

		// overwrite existing file
		System.out.println("START ANALYSING REAL INCOME");
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline_SUMMARY_Household_000.csv");
		processCsv("D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\Baseline_SUMMARY_Household_000.csv",
				"Baseline", false, 0);

		// append to file
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline_SUMMARY_Household_012.csv");
		processCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline_SUMMARY_Household_012.csv", "Baseline",
				true, 12);
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline-01_SUMMARY_Household_012.csv");
		processCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-01_SUMMARY_Household_012.csv",
				"Baseline-01", true, 12);
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline-02_SUMMARY_Household_012.csv");
		processCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-02_SUMMARY_Household_012.csv",
				"Baseline-02", true, 12);
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline-03_SUMMARY_Household_012.csv");
		processCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-03_SUMMARY_Household_012.csv",
				"Baseline-03", true, 12);
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline-04_SUMMARY_Household_012.csv");
		processCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-04_SUMMARY_Household_012.csv",
				"Baseline-04", true, 12);
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline-05_SUMMARY_Household_012.csv");
		processCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-05_SUMMARY_Household_012.csv",
				"Baseline-05", true, 12);
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline-06_SUMMARY_Household_012.csv");
		processCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-06_SUMMARY_Household_012.csv",
				"Baseline-06", true, 12);
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline-07_SUMMARY_Household_012.csv");
		processCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-07_SUMMARY_Household_012.csv",
				"Baseline-07", true, 12);
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline-08_SUMMARY_Household_012.csv");
		processCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-08_SUMMARY_Household_012.csv",
				"Baseline-08", true, 12);
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline-09_SUMMARY_Household_012.csv");
		processCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-09_SUMMARY_Household_012.csv",
				"Baseline-09", true, 12);

		// ADI crash
		System.out.println(new Date(System.currentTimeMillis()) + ": processing ADI-CBA_SUMMARY_Household_012.csv");
		processCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\ADI-CBA_SUMMARY_Household_012.csv", "ADI-CBA",
				true, 12);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing ADI-CBA-no-limit_SUMMARY_Household_012.csv");
		processCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\ADI-CBA-no-limit_SUMMARY_Household_012.csv",
				"ADI-CBA-no-limit", true, 12);
		System.out.println(new Date(System.currentTimeMillis()) + ": processing ADI-Mutuals_SUMMARY_Household_012.csv");
		processCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\ADI-Mutuals_SUMMARY_Household_012.csv",
				"ADI-Mutuals", true, 12);

		// foreign country
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing FX-Rates-10pc-CNY_SUMMARY_Household_012.csv");
		processCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\FX-Rates-10pc-CNY_SUMMARY_Household_012.csv",
				"FX-Rates-10pc-CNY", true, 12);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing FX-Rates-10pc-JPY_SUMMARY_Household_012.csv");
		processCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\FX-Rates-10pc-JPY_SUMMARY_Household_012.csv",
				"FX-Rates-10pc-JPY", true, 12);

		// inflation
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing Inflation-10pc_SUMMARY_Household_012.csv");
		processCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Inflation-10pc_SUMMARY_Household_012.csv",
				"Inflation-10pc", true, 12);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing Inflation-25pc-4yrs_SUMMARY_Household_012.csv");
		processCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Inflation-25pc-4yrs_SUMMARY_Household_012.csv",
				"Inflation-25pc-4yrs", true, 12);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing Inflation-25pc-4yrs_SUMMARY_Household_024.csv");
		processCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Inflation-25pc-4yrs_SUMMARY_Household_024.csv",
				"Inflation-25pc-4yrs", true, 24);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing Inflation-25pc-4yrs_SUMMARY_Household_036.csv");
		processCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Inflation-25pc-4yrs_SUMMARY_Household_036.csv",
				"Inflation-25pc-4yrs", true, 36);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing Inflation-25pc-4yrs_SUMMARY_Household_048.csv");
		processCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Inflation-25pc-4yrs_SUMMARY_Household_048.csv",
				"Inflation-25pc-4yrs", true, 48);

		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing Inflation100pc_SUMMARY_Household_012.csv");
		processCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Inflation-100pc_SUMMARY_Household_012.csv",
				"Inflation-100pc", true, 12);

		System.out.println("END ANALYSING REAL INCOME");
	}

	private static void processCsv(String inFileResourceLocation, String scenario, boolean append, int iteration) {
		// declare local variables
		TIntFloatMap meanIncomeByAge = new TIntFloatHashMap();

		// working variables
		TIntFloatMap totalIncomeByAge = new TIntFloatHashMap();
		TIntIntMap totalCountByAge = new TIntIntHashMap();

		// open file and calculate metrics
		CSVReader reader = null;
		try {
			Reader fr = new FileReader(inFileResourceLocation);
			reader = new CSVReader(fr);
			String[] line = reader.readNext(); // read and discard header row
			while ((line = reader.readNext()) != null) {
				try {
					// update national income list
					// float netIncome = Float.valueOf(line[6].replace(",", "")) -
					// Float.valueOf(line[8].replace(",", ""));
					float childCount = Integer.valueOf(line[3].replace(",", ""));
					float adultCount = Integer.valueOf(line[4].replace(",", ""));
					float age = Integer.valueOf(line[5].replace(",", ""));
					float grossIncome = Float.valueOf(line[6].replace(",", ""));

					int bin = (int) Math.floor(housingCosts / grossIncome / BIN_SIZE);
					if (rent > 0f) {
						// renting
						rentCount.set(bin, rentCount.get(bin) + 1);
						if (defaultIteration > 0) {
							// household has defaulted
							rentDefaultCount.set(bin, rentDefaultCount.get(bin) + 1);
						}
					} else {
						// own their own home
						ownCount.set(bin, ownCount.get(bin) + 1);
						if (defaultIteration > 0) {
							// household has defaulted
							ownDefaultCount.set(bin, ownDefaultCount.get(bin) + 1);
						}
					}
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

		// write metrics to file
		// save CSV file in a format that R can graph
		DecimalFormat wholeNumber = new DecimalFormat("000");
		String outFilename = properties.getOutputDirectory() + "R_GRAPH_Income.csv";
		Writer writer;
		try {
			writer = new FileWriter(outFilename, append); // overwrites existing file if append == false
			ICSVWriter csvWriter = new CSVWriterBuilder(writer).build();
			String[] entries = { "scenario", "iteration", "age", "metric", "value" };
			if (!append) {
				// first file, so write column headers
				csvWriter.writeNext(entries);
			}

			// write metrics to file
			for (int age : meanIncomeByAge) {
				entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
						+ properties.getCsvSeparator() + age + properties.getCsvSeparator() + "meanIncome"
						+ properties.getCsvSeparator() + meanIncomeByAge.get(age)).split(properties.getCsvSeparator());
				csvWriter.writeNext(entries);
			}

			writer.close();
		} catch (IOException e) {
			// new FileWriter
			e.printStackTrace();
		} finally {
			writer = null;
		}

	}
}
