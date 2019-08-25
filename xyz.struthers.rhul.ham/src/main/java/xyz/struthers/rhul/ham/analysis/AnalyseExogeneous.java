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

import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TIntArrayList;
import xyz.struthers.rhul.ham.config.PropertiesXml;
import xyz.struthers.rhul.ham.config.PropertiesXmlFactory;

/**
 * 
 * 
 * @author Adam Struthers
 * @since 2019-08-09
 */
public class AnalyseExogeneous {

	// the maximum value of each bin for the exogeneous income ratio histogram
	public static final float[] BIN_MAX = { 0.05f, 0.1f, 0.15f, 0.2f, 0.25f, 0.3f, 0.35f, 0.4f, 0.45f, 0.5f, 0.55f,
			0.6f, 0.65f, 0.7f, 0.75f, 0.8f, 0.85f, 0.9f, 0.95f, 1f };

	public static final float BIN_SIZE = 0.05f;

	private static PropertiesXml properties;

	public AnalyseExogeneous() {
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
		processExogeneousDataCsv(
				"D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline_EXOGENEOUS_000.csv", "Baseline",
				false, 0);

		// append to file
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline-01_EXOGENEOUS_000.csv");
		processExogeneousDataCsv(
				"D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-01_EXOGENEOUS_000.csv",
				"Baseline-01", true, 0);
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline-02_EXOGENEOUS_000.csv");
		processExogeneousDataCsv(
				"D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-02_EXOGENEOUS_000.csv",
				"Baseline-02", true, 0);
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline-03_EXOGENEOUS_000.csv");
		processExogeneousDataCsv(
				"D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-03_EXOGENEOUS_000.csv",
				"Baseline-03", true, 0);
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline-04_EXOGENEOUS_000.csv");
		processExogeneousDataCsv(
				"D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-04_EXOGENEOUS_000.csv",
				"Baseline-04", true, 0);
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline-05_EXOGENEOUS_000.csv");
		processExogeneousDataCsv(
				"D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-05_EXOGENEOUS_000.csv",
				"Baseline-05", true, 0);
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline-06_EXOGENEOUS_000.csv");
		processExogeneousDataCsv(
				"D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-06_EXOGENEOUS_000.csv",
				"Baseline-06", true, 0);
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline-07_EXOGENEOUS_000.csv");
		processExogeneousDataCsv(
				"D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-07_EXOGENEOUS_000.csv",
				"Baseline-07", true, 0);
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline-08_EXOGENEOUS_000.csv");
		processExogeneousDataCsv(
				"D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-08_EXOGENEOUS_000.csv",
				"Baseline-08", true, 0);
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline-09_EXOGENEOUS_000.csv");
		processExogeneousDataCsv(
				"D:\\OneDrive\\Dissertation\\Results\\Summary Data\\Baseline-09_EXOGENEOUS_000.csv",
				"Baseline-09", true, 0);

		System.out.println(new Date(System.currentTimeMillis()) + ": FINISHED");
		System.out.println("See " + properties.getOutputDirectory() + "R_GRAPH_Exogeneous.csv for results");
	}

	private static void processExogeneousDataCsv(String inFileResourceLocation, String scenario, boolean append,
			int iteration) {

		TIntArrayList adiCount = new TIntArrayList(
				new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 });
		TIntArrayList businessCount = new TIntArrayList(
				new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 });
		TIntArrayList householdCount = new TIntArrayList(
				new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 });
		TFloatArrayList adiPercent = new TFloatArrayList(adiCount.size());
		TFloatArrayList businessPercent = new TFloatArrayList(businessCount.size());
		TFloatArrayList householdPercent = new TFloatArrayList(householdCount.size());

		// read CSV file
		CSVReader reader = null;
		try {
			// InputStream is = this.getClass().getResourceAsStream(inFileResourceLocation);
			// reader = new CSVReader(new InputStreamReader(is));

			Reader fr = new FileReader(inFileResourceLocation);
			reader = new CSVReader(fr);
			String[] line = reader.readNext(); // read and discard header row
			while ((line = reader.readNext()) != null) {
				// calculate exogeneous income ratio
				float exogeneousRatio = 0f;
				int binNumber = 0;
				try {
					float exogeneousIncome = Float.valueOf(line[2].replace(",", ""));
					float totalIncome = Float.valueOf(line[3].replace(",", ""));
					exogeneousRatio = totalIncome <= 0f ? 0f : exogeneousIncome / totalIncome;
					binNumber = getBin(exogeneousRatio);
				} catch (NumberFormatException e) {
					// do nothing and leave it as zero.
				}

				// update count in each bin
				switch (line[1]) {
				case "A": // ADI
					adiCount.set(binNumber, adiCount.get(binNumber) + 1);
					break;
				case "B": // business
					businessCount.set(binNumber, businessCount.get(binNumber) + 1);
					break;
				case "H": // household
					householdCount.set(binNumber, householdCount.get(binNumber) + 1);
					break;
				default:
					// do nothing
					break;
				}
			}

			// calculate percentage in each bin
			float adiTotal = 0f;
			float businessTotal = 0f;
			float householdTotal = 0f;
			for (int i = 0; i < adiCount.size(); i++) {
				adiTotal += adiCount.get(i);
				businessTotal += businessCount.get(i);
				householdTotal += householdCount.get(i);
			}
			for (int i = 0; i < adiCount.size(); i++) {
				adiPercent.add(adiCount.get(i) / adiTotal);
				businessPercent.add(businessCount.get(i) / businessTotal);
				householdPercent.add(householdCount.get(i) / householdTotal);
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

		// save CSV file in a format that R can graph
		DecimalFormat wholeNumber = new DecimalFormat("000");
		String outFilename = properties.getOutputDirectory() + "R_GRAPH_Exogeneous.csv";
		Writer writer;
		try {
			writer = new FileWriter(outFilename, append); // overwrites existing file if append == false
			ICSVWriter csvWriter = new CSVWriterBuilder(writer).build();
			String[] entries = { "scenario", "iteration", "agentType", "bin", "exogeneousPercent" };
			if (!append) {
				// first file, so write column headers
				csvWriter.writeNext(entries);
			}
			for (int row = 0; row < adiPercent.size(); row++) {
				entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
						+ properties.getCsvSeparator() + "A" + properties.getCsvSeparator() + row * BIN_SIZE
						+ properties.getCsvSeparator() + adiPercent.get(row)).split(properties.getCsvSeparator());
				csvWriter.writeNext(entries);
			}
			for (int row = 0; row < businessPercent.size(); row++) {
				entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
						+ properties.getCsvSeparator() + "B" + properties.getCsvSeparator() + row * BIN_SIZE
						+ properties.getCsvSeparator() + businessPercent.get(row)).split(properties.getCsvSeparator());
				csvWriter.writeNext(entries);
			}
			for (int row = 0; row < householdPercent.size(); row++) {
				entries = (scenario + properties.getCsvSeparator() + wholeNumber.format(iteration)
						+ properties.getCsvSeparator() + "H" + properties.getCsvSeparator() + row * BIN_SIZE
						+ properties.getCsvSeparator() + householdPercent.get(row)).split(properties.getCsvSeparator());
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

	private static int getBin(float value) {
		int bin = -1;

		while (value >= 0f) {
			value -= BIN_SIZE;
			bin++;
		}

		return Math.max(0, bin);
	}

}
