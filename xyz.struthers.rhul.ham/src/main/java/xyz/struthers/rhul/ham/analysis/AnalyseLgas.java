package xyz.struthers.rhul.ham.analysis;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;

import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.map.TObjectFloatMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectFloatHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import xyz.struthers.rhul.ham.config.PropertiesXml;
import xyz.struthers.rhul.ham.config.PropertiesXmlFactory;

/**
 * FIXME: add metrics (counts) so I can calcualte the tables
 * 
 * @author Adam Struthers
 * @since 2019-08-14
 */
public class AnalyseLgas {

	public static final int NUM_LGAS = 550;

	private static PropertiesXml properties;

	public AnalyseLgas() {
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
		processLgaMetricsCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline_SUMMARY_Household_000.csv",
				"Baseline", false, 0);

		// append to file
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline_SUMMARY_Household_012.csv");
		processLgaMetricsCsv("D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline_SUMMARY_Household_012.csv",
				"Baseline", true, 12);

		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing Inflation-05pc_SUMMARY_Household_000.csv");
		processLgaMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Inflation-05pc_SUMMARY_Household_000.csv",
				"Inflation-05pc", true, 0);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing Inflation-05pc_SUMMARY_Household_012.csv");
		processLgaMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Inflation-05pc_SUMMARY_Household_012.csv",
				"Inflation-05pc", true, 12);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing Inflation-10pc_SUMMARY_Household_012.csv");
		processLgaMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Inflation-10pc_SUMMARY_Household_012.csv",
				"Inflation-10pc", true, 12);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing Inflation-75pc_SUMMARY_Household_012.csv");
		processLgaMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Inflation-75pc_SUMMARY_Household_012.csv",
				"Inflation-75pc", true, 12);
		System.out.println(
				new Date(System.currentTimeMillis()) + ": processing Inflation-15pc-5yrs_SUMMARY_Household_060.csv");
		processLgaMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Inflation-15pc-5yrs_SUMMARY_Household_060.csv",
				"Inflation-15pc-5yrs", true, 60);

		System.out.println(new Date(System.currentTimeMillis()) + ": FINISHED");
	}

	/**
	 * mortgage distress<br>
	 * wealth inequality
	 * 
	 * @param inFileResourceLocation
	 * @param scenario
	 * @param append
	 * @param iteration
	 */
	private static void processLgaMetricsCsv(String inFileResourceLocation, String scenario, boolean append,
			int iteration) {

		// declare local variables
		TObjectFloatMap<String> housingCostsOver30pc = new TObjectFloatHashMap<String>(NUM_LGAS);
		TObjectFloatMap<String> incomeEarnedTop5pc = new TObjectFloatHashMap<String>(NUM_LGAS);
		TObjectFloatMap<String> incomeEarnedTop5pcAU = new TObjectFloatHashMap<String>(NUM_LGAS);
		TObjectIntMap<String> housingCostsOver30pcRank = new TObjectIntHashMap<String>(NUM_LGAS);
		TObjectIntMap<String> incomeEarnedTop5pcRank = new TObjectIntHashMap<String>(NUM_LGAS);
		TObjectIntMap<String> incomeEarnedTop5pcAURank = new TObjectIntHashMap<String>(NUM_LGAS);

		// working variables
		TObjectIntMap<String> lgaHouseholdCount = new TObjectIntHashMap<String>(NUM_LGAS);
		TObjectIntMap<String> lgaHousingCostsOver30pcCount = new TObjectIntHashMap<String>(NUM_LGAS);
		TObjectFloatMap<String> lgaTotalIncome = new TObjectFloatHashMap<String>(NUM_LGAS);
		TObjectFloatMap<String> lgaTotalIncomeTop1pc = new TObjectFloatHashMap<String>(NUM_LGAS);
		TObjectFloatMap<String> lgaTotalIncomeTop5pc = new TObjectFloatHashMap<String>(NUM_LGAS);
		TObjectFloatMap<String> nationalTotalIncomeTop1pc = new TObjectFloatHashMap<String>(NUM_LGAS);
		TObjectFloatMap<String> nationalTotalIncomeTop5pc = new TObjectFloatHashMap<String>(NUM_LGAS);

		// open file and sort incomes within each LGA, and nationally
		// then calculate top 1% and top 5% threshold incomes
		Map<String, TFloatArrayList> lgaIncomeLists = new HashMap<String, TFloatArrayList>(NUM_LGAS);
		TFloatArrayList nationalIncomeList = new TFloatArrayList(10000000);
		CSVReader reader = null;
		try {
			Reader fr = new FileReader(inFileResourceLocation);
			reader = new CSVReader(fr);
			String[] line = reader.readNext(); // read and discard header row
			while ((line = reader.readNext()) != null) {
				try {
					// update national income list
					float income = Float.valueOf(line[6].replace(",", ""));
					nationalIncomeList.add(income);

					// update LGA income list
					String lgaCode = line[2];
					if (!lgaIncomeLists.containsKey(lgaCode)) {
						lgaIncomeLists.put(lgaCode, new TFloatArrayList());
					}
					TFloatArrayList lgaList = lgaIncomeLists.get(lgaCode);
					lgaList.add(income);
					lgaIncomeLists.put(lgaCode, lgaList); // this line might be redundant
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
		// sort national income list, and determine thresholds
		nationalIncomeList.sort();
		float nationalThresholdTop1pc = nationalIncomeList.get((int) (nationalIncomeList.size() * 0.99f));
		float nationalThresholdTop5pc = nationalIncomeList.get((int) (nationalIncomeList.size() * 0.95f));
		TObjectFloatMap<String> lgaThresholdTop1pc = new TObjectFloatHashMap<String>();
		TObjectFloatMap<String> lgaThresholdTop5pc = new TObjectFloatHashMap<String>();
		for (String lga : lgaIncomeLists.keySet()) {
			TFloatArrayList lgaList = lgaIncomeLists.get(lga);
			lgaList.sort();
			// lgaIncomeLists.put(lga, lgaList); // this line might be redundant

			float threshold = lgaList.get((int) (lgaList.size() * 0.99f));
			lgaThresholdTop1pc.put(lga, threshold);
			threshold = lgaList.get((int) (lgaList.size() * 0.95f));
			lgaThresholdTop5pc.put(lga, threshold);
		}

		/**
		 * nationalThresholdTop1pc, nationalThresholdTop5pc<br>
		 * lgaThresholdTop1pc, lgaThresholdTop5pc
		 */
		// open file and calculate metrics
		reader = null;
		try {
			Reader fr = new FileReader(inFileResourceLocation);
			reader = new CSVReader(fr);
			String[] line = reader.readNext(); // read and discard header row
			while ((line = reader.readNext()) != null) {
				try {
					// update counts in LGA map
					String lgaCode = line[2];
					int householdCount = 0;
					int housingCostsOver30pcCount = 0;
					float totalIncome = 0f;
					float totalIncomeTop1pc = 0f;
					float totalIncomeTop5pc = 0f;
					float totalIncomeTop1pcAU = 0f;
					float totalIncomeTop5pcAU = 0f;

					if (lgaHouseholdCount.containsKey(lgaCode)) {
						householdCount = lgaHouseholdCount.get(lgaCode);
						housingCostsOver30pcCount = lgaHousingCostsOver30pcCount.get(lgaCode);
						totalIncome = lgaTotalIncome.get(lgaCode);
						totalIncomeTop1pc = lgaTotalIncomeTop1pc.get(lgaCode);
						totalIncomeTop5pc = lgaTotalIncomeTop5pc.get(lgaCode);
						totalIncomeTop1pcAU = nationalTotalIncomeTop1pc.get(lgaCode);
						totalIncomeTop5pcAU = nationalTotalIncomeTop5pc.get(lgaCode);
					}
					/*
					 * We could measure separately: the top 1% and top 5% based on income within
					 * that LGA; and also the top 1% and 5% based on national income. The former
					 * would reveal where inequality exists within an LGA, while the latter would
					 * show how the rich and poor are distributed across LGAs.
					 */
					float income = Float.valueOf(line[6].replace(",", ""));
					float housingCosts = Float.valueOf(line[10].replace(",", ""))
							+ Float.valueOf(line[11].replace(",", ""));
					int mtgDistress = housingCosts > (income * 0.3f) ? 1 : 0;
					housingCostsOver30pcCount += mtgDistress;

					householdCount++;
					lgaHouseholdCount.put(lgaCode, householdCount);
					lgaHousingCostsOver30pcCount.put(lgaCode, housingCostsOver30pcCount);
					lgaTotalIncome.put(lgaCode, totalIncome + income);
					if (income > lgaThresholdTop5pc.get(lgaCode)) {
						lgaTotalIncomeTop5pc.put(lgaCode, totalIncomeTop5pc + income);
						if (income > lgaThresholdTop1pc.get(lgaCode)) {
							lgaTotalIncomeTop1pc.put(lgaCode, totalIncomeTop1pc + income);
						}
					}
					if (income > nationalThresholdTop5pc) {
						nationalTotalIncomeTop5pc.put(lgaCode, totalIncomeTop5pcAU + income);
						if (income > nationalThresholdTop1pc) {
							nationalTotalIncomeTop1pc.put(lgaCode, totalIncomeTop1pcAU + income);
						}
					}

					// incomeList.add(income);
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

		// calculate ratios for each LGA
		for (String lga : lgaHouseholdCount.keySet()) {
			housingCostsOver30pc.put(lga,
					Float.valueOf(lgaHousingCostsOver30pcCount.get(lga)) / Float.valueOf(lgaHouseholdCount.get(lga)));
			incomeEarnedTop5pc.put(lga,
					Float.valueOf(lgaTotalIncomeTop5pc.get(lga)) / Float.valueOf(lgaTotalIncome.get(lga)));
			incomeEarnedTop5pcAU.put(lga,
					Float.valueOf(nationalTotalIncomeTop5pc.get(lga)) / Float.valueOf(lgaTotalIncome.get(lga)));
		}

		// calculate scale for shading the heat map
		float minimumScaleHousingCostsOver30pc = 1f;
		float minimumScaleIncomeEarnedTop5pc = 1f;
		float minimumScaleIncomeEarnedTop5pcAU = 1f;
		float maximumScaleHousingCostsOver30pc = 0f;
		float maximumScaleIncomeEarnedTop5pc = 0f;
		float maximumScaleIncomeEarnedTop5pcAU = 0f;
		for (String lga : lgaHouseholdCount.keySet()) {
			minimumScaleHousingCostsOver30pc = Math.min(minimumScaleHousingCostsOver30pc,
					housingCostsOver30pc.get(lga));
			minimumScaleIncomeEarnedTop5pc = Math.min(minimumScaleIncomeEarnedTop5pc, incomeEarnedTop5pc.get(lga));
			minimumScaleIncomeEarnedTop5pcAU = Math.min(minimumScaleIncomeEarnedTop5pcAU,
					incomeEarnedTop5pcAU.get(lga));
			maximumScaleHousingCostsOver30pc = Math.max(maximumScaleHousingCostsOver30pc,
					housingCostsOver30pc.get(lga));
			maximumScaleIncomeEarnedTop5pc = Math.max(maximumScaleIncomeEarnedTop5pc, incomeEarnedTop5pc.get(lga));
			maximumScaleIncomeEarnedTop5pcAU = Math.max(maximumScaleIncomeEarnedTop5pcAU,
					incomeEarnedTop5pcAU.get(lga));
		}
		float[] scaleThresholdHousingCostsOver30pc = new float[9];
		float[] scaleThresholdIncomeEarnedTop5pc = new float[9];
		float[] scaleThresholdIncomeEarnedTop5pcAU = new float[9];
		for (int i = 0; i < 9; i++) {
			scaleThresholdHousingCostsOver30pc[i] = (maximumScaleHousingCostsOver30pc
					- minimumScaleHousingCostsOver30pc) * Float.valueOf(i);
			scaleThresholdIncomeEarnedTop5pc[i] = (maximumScaleIncomeEarnedTop5pc - minimumScaleIncomeEarnedTop5pc)
					* Float.valueOf(i);
			scaleThresholdIncomeEarnedTop5pcAU[i] = (maximumScaleIncomeEarnedTop5pcAU
					- minimumScaleIncomeEarnedTop5pcAU) * Float.valueOf(i);
		}
		for (String lga : lgaHouseholdCount.keySet()) {
			// add rank for housing stress (controls shading in heat map)
			int rank = 0;
			float value = housingCostsOver30pc.get(lga);
			if (value > 0.8) {
				rank = 5;
			} else if (value > 0.6) {
				rank = 4;
			} else if (value > 0.4) {
				rank = 3;
			} else if (value > 0.2) {
				rank = 2;
			} else {
				rank = 1;
			}
			housingCostsOver30pcRank.put(lga, rank);

			// add rank for top 5% income (controls shading in heat map)
			rank = 0;
			value = incomeEarnedTop5pc.get(lga);
			if (value > 0.8) {
				rank = 5;
			} else if (value > 0.6) {
				rank = 4;
			} else if (value > 0.4) {
				rank = 3;
			} else if (value > 0.2) {
				rank = 2;
			} else {
				rank = 1;
			}
			incomeEarnedTop5pcRank.put(lga, rank);

			// add rank for top 5% income (controls shading in heat map)
			rank = 0;
			value = incomeEarnedTop5pcAU.get(lga);
			if (value > 0.8) {
				rank = 5;
			} else if (value > 0.6) {
				rank = 4;
			} else if (value > 0.4) {
				rank = 3;
			} else if (value > 0.2) {
				rank = 2;
			} else {
				rank = 1;
			}
			incomeEarnedTop5pcAURank.put(lga, rank);
		}

		// write LGA metrics to file
		// save CSV file in a format that R can graph
		DecimalFormat wholeNumber = new DecimalFormat("000");
		String outFilename = properties.getOutputDirectory() + "R_GRAPH_LGA.csv";
		Writer writer;
		try {
			writer = new FileWriter(outFilename, append); // overwrites existing file if append == false
			ICSVWriter csvWriter = new CSVWriterBuilder(writer).build();
			String[] entries = { "scenario", "iteration", "agentType", "metric", "LGA", "value", "rank" };
			if (!append) {
				// first file, so write column headers
				csvWriter.writeNext(entries);
			}

			// write metrics to file
			for (String lga : housingCostsOver30pc.keySet()) {
				entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
						+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "housingCostsOver30pcCount"
						+ properties.getCsvSeparator() + lga + properties.getCsvSeparator()
						+ housingCostsOver30pc.get(lga) + properties.getCsvSeparator()
						+ housingCostsOver30pcRank.get(lga)).split(properties.getCsvSeparator());
				csvWriter.writeNext(entries);
				entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
						+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "incomeEarnedTop5pcCount"
						+ properties.getCsvSeparator() + lga + properties.getCsvSeparator()
						+ incomeEarnedTop5pc.get(lga) + properties.getCsvSeparator() + incomeEarnedTop5pcRank.get(lga))
								.split(properties.getCsvSeparator());
				csvWriter.writeNext(entries);
				entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
						+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "incomeEarnedTop5pcAUCount"
						+ properties.getCsvSeparator() + lga + properties.getCsvSeparator()
						+ incomeEarnedTop5pcAU.get(lga) + properties.getCsvSeparator()
						+ incomeEarnedTop5pcAURank.get(lga)).split(properties.getCsvSeparator());
				csvWriter.writeNext(entries);

				entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
						+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "lgaHouseholdCount"
						+ properties.getCsvSeparator() + lga + properties.getCsvSeparator()
						+ lgaHouseholdCount.get(lga) + properties.getCsvSeparator()
						+ lgaHouseholdCount.get(lga)).split(properties.getCsvSeparator());
				csvWriter.writeNext(entries);
				entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
						+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "lgaHousingCostsOver30pcCount"
						+ properties.getCsvSeparator() + lga + properties.getCsvSeparator()
						+ lgaHousingCostsOver30pcCount.get(lga) + properties.getCsvSeparator()
						+ lgaHousingCostsOver30pcCount.get(lga)).split(properties.getCsvSeparator());
				csvWriter.writeNext(entries);
				entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
						+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "lgaTotalIncome"
						+ properties.getCsvSeparator() + lga + properties.getCsvSeparator()
						+ lgaTotalIncome.get(lga) + properties.getCsvSeparator()
						+ lgaTotalIncome.get(lga)).split(properties.getCsvSeparator());
				csvWriter.writeNext(entries);
				entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
						+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "lgaTotalIncomeTop1pc"
						+ properties.getCsvSeparator() + lga + properties.getCsvSeparator()
						+ lgaTotalIncomeTop1pc.get(lga) + properties.getCsvSeparator()
						+ lgaTotalIncomeTop1pc.get(lga)).split(properties.getCsvSeparator());
				csvWriter.writeNext(entries);
				entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
						+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "lgaTotalIncomeTop5pc"
						+ properties.getCsvSeparator() + lga + properties.getCsvSeparator()
						+ lgaTotalIncomeTop5pc.get(lga) + properties.getCsvSeparator()
						+ lgaTotalIncomeTop5pc.get(lga)).split(properties.getCsvSeparator());
				csvWriter.writeNext(entries);
				entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
						+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "nationalTotalIncomeTop1pc"
						+ properties.getCsvSeparator() + lga + properties.getCsvSeparator()
						+ nationalTotalIncomeTop1pc.get(lga) + properties.getCsvSeparator()
						+ nationalTotalIncomeTop1pc.get(lga)).split(properties.getCsvSeparator());
				csvWriter.writeNext(entries);
				entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
						+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "nationalTotalIncomeTop5pc"
						+ properties.getCsvSeparator() + lga + properties.getCsvSeparator()
						+ nationalTotalIncomeTop5pc.get(lga) + properties.getCsvSeparator()
						+ nationalTotalIncomeTop5pc.get(lga)).split(properties.getCsvSeparator());
				csvWriter.writeNext(entries);

				/*
				 *   
				 *   
				 * 
				 * 
				 */
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
