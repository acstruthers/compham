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
				"D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\Baseline_SUMMARY_Household_000.csv",
				"Baseline", false, 0);

		// append to file
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline_SUMMARY_Household_012.csv");
		processHouseholdMetricsCsv(
				"D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\Baseline_SUMMARY_Household_012.csv",
				"Baseline-01", true, 0);
	}

	private static void processHouseholdMetricsCsv(String inFileResourceLocation, String scenario, boolean append,
			int iteration) {

		// overall mean metrics (to test model error)
		float meanIncome = 0f;
		float meanExpenses = 0f;
		float housingCostsOver30pc = 0f; // % of households in mtg distress
		float housingCostsOver30pcTop5pc = 0f;
		float housingCostsOver30pcTop1pc = 0f;
		float housingCostsOver30pcBottom95pc = 0f;
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

		// local working variables
		int householdCount = 0;
		int householdCountTop5pc = 0;
		int householdCountTop1pc = 0;
		int housingCostsOver30pcCount = 0;
		int housingCostsOver30pcCountTop5pc = 0;
		int housingCostsOver30pcCountTop1pc = 0;
		int housingCostsOver30pcCountBottom95pc = 0;
		float totalDebt = 0f;
		float totalDebtTop5pc = 0f;
		float totalDebtBottom95pc = 0f;
		float totalIncome = 0f;
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
		float top5pcThreshold = 0f;
		float top1pcThreshold = 0f;
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

					// FIXME: update metrics
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
							totalNetWorthTop1pc += netWorth;
							totalHendersonTop1pc += henderson;
							if (housingCosts > 0.3 * income) {
								// mortgage distress in top 1% of income
								housingCostsOver30pcCountTop1pc++;
							}
						}
					} else {
						// add to bottom 95% metrics
						totalIncomeBottom95pc += income;
						totalDebtBottom95pc += debt;
						totalNetWorthBottom95pc += netWorth;
						totalHendersonBottom95pc += henderson;
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
					totalDebt += debt;
					totalIncome += income;
					totalExpenses += expenses;
					totalNetWorth += netWorth;
					totalHenderson += henderson;

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

		// calculate ratio metrics
		meanIncome = totalIncome / householdCount;
		meanExpenses = totalExpenses / householdCount;
		housingCostsOver30pc = housingCostsOver30pcCount / Float.valueOf(householdCount);
		housingCostsOver30pcTop5pc = housingCostsOver30pcCountTop5pc / Float.valueOf(householdCountTop5pc);
		housingCostsOver30pcTop1pc = housingCostsOver30pcCountTop1pc / Float.valueOf(householdCountTop1pc);
		housingCostsOver30pcBottom95pc = housingCostsOver30pcCountBottom95pc
				/ Float.valueOf(householdCount - householdCountTop5pc);
		totalIncomePercentEarnedByTop5pc = totalIncomeTop5pc / totalIncome;
		debtToIncome = totalDebt / totalIncome; // household debt-to-income ratio (Kumhof, et al, 2015)
		debtToIncomeTop5pc = totalDebtTop5pc / totalIncomeTop5pc;
		debtToIncomeBottom95pc = totalDebtBottom95pc / totalIncomeBottom95pc;
		debtToNetWorth = totalDebt / totalNetWorth;
		debtToNetWorthTop5pc = totalDebtTop5pc / totalNetWorthTop5pc;
		debtToNetWorthBottom95pc = totalDebtBottom95pc / totalNetWorthBottom95pc;
		wealthPercentHeldByTop5pc = totalNetWorthTop5pc / totalNetWorth;
		wealthPercentHeldByTop1pc = totalNetWorthTop1pc / totalNetWorth;
		incomeToHendersonPercent = totalIncome / totalHenderson;

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
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator()
					+ "totalIncomePercentEarnedByTop5pc" + properties.getCsvSeparator()
					+ totalIncomePercentEarnedByTop5pc).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "debtToIncome"
					+ properties.getCsvSeparator() + debtToIncome).split(properties.getCsvSeparator());
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
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "debtToNetWorthTop5pc"
					+ properties.getCsvSeparator() + debtToNetWorthTop5pc).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "debtToNetWorthBottom95pc"
					+ properties.getCsvSeparator() + debtToNetWorthBottom95pc).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "wealthPercentHeldByTop5pc"
					+ properties.getCsvSeparator() + wealthPercentHeldByTop5pc).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "wealthPercentHeldByTop1pc"
					+ properties.getCsvSeparator() + wealthPercentHeldByTop1pc).split(properties.getCsvSeparator());
			csvWriter.writeNext(entries);
			entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
					+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + "incomeToHendersonPercent"
					+ properties.getCsvSeparator() + incomeToHendersonPercent).split(properties.getCsvSeparator());
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
