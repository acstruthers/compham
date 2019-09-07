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

import gnu.trove.list.TFloatList;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TIntArrayList;
import xyz.struthers.rhul.ham.config.PropertiesXml;
import xyz.struthers.rhul.ham.config.PropertiesXmlFactory;

/**
 * @author Adam Struthers
 * @since 2019-08-14
 */
public class AnalyseHomeOwnership {

	public static final float BIN_SIZE = 0.1f;

	private static PropertiesXml properties;

	public AnalyseHomeOwnership() {
		super();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		PropertiesXmlFactory.propertiesXmlFilename = "D:\\compham-config\\4.1_baseline.xml";
		properties = PropertiesXmlFactory.getProperties();

		// overwrite existing file
		System.out.println("START ANALYSING HOME OWNERSHIP");
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline_SUMMARY_Household_000.csv");
		processCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline_SUMMARY_Household_000.csv", "Baseline",
				false, 0);

		// append to file
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline_SUMMARY_Household_012.csv");
		processCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline_SUMMARY_Household_012.csv", "Baseline",
				true, 12);
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline-01_SUMMARY_Household_000.csv");
		processCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-01_SUMMARY_Household_000.csv",
				"Baseline", true, 0);
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline-01_SUMMARY_Household_012.csv");
		processCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-01_SUMMARY_Household_012.csv",
				"Baseline", true, 12);
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline-02_SUMMARY_Household_000.csv");
		processCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-02_SUMMARY_Household_000.csv",
				"Baseline", true, 0);
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline-02_SUMMARY_Household_012.csv");
		processCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-02_SUMMARY_Household_012.csv",
				"Baseline", true, 12);
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline-03_SUMMARY_Household_000.csv");
		processCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-03_SUMMARY_Household_000.csv",
				"Baseline", true, 0);
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline-03_SUMMARY_Household_012.csv");
		processCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-03_SUMMARY_Household_012.csv",
				"Baseline", true, 12);
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline-04_SUMMARY_Household_000.csv");
		processCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-04_SUMMARY_Household_000.csv",
				"Baseline", true, 0);
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline-04_SUMMARY_Household_012.csv");
		processCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-04_SUMMARY_Household_012.csv",
				"Baseline", true, 12);
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline-05_SUMMARY_Household_000.csv");
		processCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-05_SUMMARY_Household_000.csv",
				"Baseline", true, 0);
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline-05_SUMMARY_Household_012.csv");
		processCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-05_SUMMARY_Household_012.csv",
				"Baseline", true, 12);
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline-06_SUMMARY_Household_000.csv");
		processCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-06_SUMMARY_Household_000.csv",
				"Baseline", true, 0);
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline-06_SUMMARY_Household_012.csv");
		processCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-06_SUMMARY_Household_012.csv",
				"Baseline", true, 12);
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline-07_SUMMARY_Household_000.csv");
		processCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-07_SUMMARY_Household_000.csv",
				"Baseline", true, 0);
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline-07_SUMMARY_Household_012.csv");
		processCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-07_SUMMARY_Household_012.csv",
				"Baseline", true, 12);
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline-08_SUMMARY_Household_000.csv");
		processCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-08_SUMMARY_Household_000.csv",
				"Baseline", true, 0);
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline-08_SUMMARY_Household_012.csv");
		processCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-08_SUMMARY_Household_012.csv",
				"Baseline", true, 12);
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline-09_SUMMARY_Household_000.csv");
		processCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-09_SUMMARY_Household_000.csv",
				"Baseline", true, 0);
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline-09_SUMMARY_Household_012.csv");
		processCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-09_SUMMARY_Household_012.csv",
				"Baseline", true, 12);

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

		System.out.println("END ANALYSING HOME OWNERSHIP");
	}

	private static void processCsv(String inFileResourceLocation, String scenario, boolean append, int iteration) {

		// declare local variables
		TFloatList ownDefaultPercent = new TFloatArrayList(10);
		TFloatList rentDefaultPercent = new TFloatArrayList(10);

		// working variables
		// float[] threshold = { 0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f };

		TIntList ownCount = new TIntArrayList(new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 });
		TIntList rentCount = new TIntArrayList(new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 });
		TIntList ownDefaultCount = new TIntArrayList(new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 });
		TIntList rentDefaultCount = new TIntArrayList(new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 });

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
					float grossIncome = Float.valueOf(line[6].replace(",", ""));
					float rent = Float.valueOf(line[10].replace(",", ""));
					float mortgage = Float.valueOf(line[11].replace(",", ""));
					float housingCosts = rent + mortgage;
					int defaultIteration = Integer.valueOf(line[17].replace(",", ""));

					// FIXME: bin calcs gave indices between -9 and 180, so it's probably wrong.
					// change to a series of nested IF statements
					// int bin = (int) Math.floor(housingCosts / grossIncome / BIN_SIZE);
					// bin = Math.min(9, bin);
					// bin = Math.max(0, 9);

					float ratio = grossIncome > 0f ? housingCosts / grossIncome : 1f;
					int bin = 0;
					if (ratio < 0.1f) {
						bin = 0;
					} else if (ratio < 0.2f) {
						bin = 1;
					} else if (ratio < 0.3f) {
						bin = 2;
					} else if (ratio < 0.4f) {
						bin = 3;
					} else if (ratio < 0.5f) {
						bin = 4;
					} else if (ratio < 0.6f) {
						bin = 5;
					} else if (ratio < 0.7f) {
						bin = 6;
					} else if (ratio < 0.8f) {
						bin = 7;
					} else if (ratio < 0.9f) {
						bin = 8;
					} else {
						bin = 9;
					}

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
		for (int i = 0; i < 10; i++) {
			ownDefaultPercent.add(
					ownCount.get(i) > 0 ? Float.valueOf(ownDefaultCount.get(i)) / Float.valueOf(ownCount.get(i)) : 0f);
			rentDefaultPercent
					.add(rentCount.get(i) > 0 ? Float.valueOf(rentDefaultCount.get(i)) / Float.valueOf(rentCount.get(i))
							: 0f);
		}

		// write metrics to file
		// save CSV file in a format that R can graph
		DecimalFormat wholeNumber = new DecimalFormat("000");
		String outFilename = properties.getOutputDirectory() + "R_GRAPH_HomeOwnership.csv";
		Writer writer;
		try {
			writer = new FileWriter(outFilename, append); // overwrites existing file if append == false
			ICSVWriter csvWriter = new CSVWriterBuilder(writer).build();
			String[] entries = { "scenario", "iteration", "agentType", "ownership", "housingCostsPercentOfIncome",
					"defaultPercent" };
			if (!append) {
				// first file, so write column headers
				csvWriter.writeNext(entries);
			}

			// write metrics to file
			for (int i = 0; i < 10; i++) {
				entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
						+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "Own"
						+ properties.getCsvSeparator() + (i / 10f) + properties.getCsvSeparator()
						+ ownDefaultPercent.get(i)).split(properties.getCsvSeparator());
				csvWriter.writeNext(entries);
			}
			for (int i = 0; i < 10; i++) {
				entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
						+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "Rent"
						+ properties.getCsvSeparator() + (i / 10f) + properties.getCsvSeparator()
						+ rentDefaultPercent.get(i)).split(properties.getCsvSeparator());
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
