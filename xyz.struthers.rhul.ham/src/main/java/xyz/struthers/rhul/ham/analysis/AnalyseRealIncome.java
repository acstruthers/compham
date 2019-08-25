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
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline_SUMMARY_Household_000.csv");
		processCsv("D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\Baseline_SUMMARY_Household_000.csv",
				"Baseline", false, 0);

		// append to file
		System.out.println(new Date(System.currentTimeMillis()) + ": processing Baseline_SUMMARY_Household_012.csv");
		processCsv("D:\\OneDrive\\Dissertation\\Results & Analysis\\Summary Data\\Baseline_SUMMARY_Household_012.csv",
				"Baseline", true, 12);
	}

	private static void processCsv(String inFileResourceLocation, String scenario, boolean append, int iteration) {
		// declare local variables

		// working variables

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