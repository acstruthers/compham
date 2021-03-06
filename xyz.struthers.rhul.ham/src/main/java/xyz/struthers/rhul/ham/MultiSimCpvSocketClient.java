package xyz.struthers.rhul.ham;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import xyz.struthers.rhul.ham.analysis.AnalyseAll;

/**
 * <h1>Runs the CpvSocketClient for several different properties files.</h1>
 * <p>
 * This allows several simulations to be run sequentially without the need for
 * manual intervention.
 * 
 * @author Adam Struthers
 * @since 2019-07-23
 */
public class MultiSimCpvSocketClient {

	public MultiSimCpvSocketClient() {
		super();
	}

	// CHECKLIST
	// Baseline - DONE
	// Baseline 01 - DONE
	// Baseline 02 - DONE
	// Baseline 03 - DONE
	// Baseline 04 - DONE
	// Baseline 05 - DONE
	// Baseline 06 - DONE
	// Baseline 07 - DONE
	// Baseline 08 - DONE
	// Baseline 09 - DONE
	// ADI CBA - DONE
	// ADI CBA no limit - DONE
	// ADI Mutuals - DONE
	// Country CNY - DONE (check output)
	// Country EUR
	// Country INR
	// Country JPY - DONE (check output)
	// Country KRW
	// Country MYR
	// Country NZD
	// Country SGD
	// Country THB
	// Country USD
	// Inflation 10% - DONE
	// Inflation 100% - DONE
	// Inflation 25% 4yrs - DONE

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd_HHmm");
		String timestamp = simpleDateFormat.format(new Date(System.currentTimeMillis()));
		String progressFilename = "D:/compham-output/MultiSim_progress_" + timestamp + ".txt";
		appendToFile(progressFilename, new Date(System.currentTimeMillis()) + ": STARTED");

		appendToFile(progressFilename,
				new Date(System.currentTimeMillis()) + ": Running simulation for 4.2_adi-crash-CBA-no-limit.xml");
		CpvSocketClient.main(new String[] { "D:/compham-config/4.2_adi-crash-CBA-no-limit.xml" });
		appendToFile(progressFilename,
				new Date(System.currentTimeMillis()) + ": Running simulation for 4.3_FX_Rates_10pc-country-JPY.xml");
		CpvSocketClient.main(new String[] { "D:/compham-config/4.3_FX_Rates_10pc-country-JPY.xml" });

		// baseline (exogeneous cash flow histograms)
		/*
		 * appendToFile(progressFilename, new Date(System.currentTimeMillis()) +
		 * ": Running simulation for 4.1_baseline.xml"); CpvSocketClient.main(new
		 * String[] { "D:/compham-config/4.1_baseline.xml" }); System.gc();
		 */

		// using different random seeds (model error box plots)
		/*
		 * appendToFile(progressFilename, new Date(System.currentTimeMillis()) +
		 * ": Running simulation for 4.1_baseline_seed-01.xml");
		 * CpvSocketClient.main(new String[] {
		 * "D:/compham-config/4.1_baseline_seed-01.xml" });
		 * appendToFile(progressFilename, new Date(System.currentTimeMillis()) +
		 * ": Running simulation for 4.1_baseline_seed-02.xml");
		 * CpvSocketClient.main(new String[] {
		 * "D:/compham-config/4.1_baseline_seed-02.xml" });
		 * appendToFile(progressFilename, new Date(System.currentTimeMillis()) +
		 * ": Running simulation for 4.1_baseline_seed-03.xml");
		 * CpvSocketClient.main(new String[] {
		 * "D:/compham-config/4.1_baseline_seed-03.xml" });
		 */
		appendToFile(progressFilename,
				new Date(System.currentTimeMillis()) + ": Running simulation for 4.1_baseline_seed-04.xml");
		CpvSocketClient.main(new String[] { "D:/compham-config/4.1_baseline_seed-04.xml" });
		appendToFile(progressFilename,
				new Date(System.currentTimeMillis()) + ": Running simulation for 4.1_baseline_seed-05.xml");
		CpvSocketClient.main(new String[] { "D:/compham-config/4.1_baseline_seed-05.xml" });
		appendToFile(progressFilename,
				new Date(System.currentTimeMillis()) + ": Running simulation for 4.1_baseline_seed-06.xml");
		CpvSocketClient.main(new String[] { "D:/compham-config/4.1_baseline_seed-06.xml" });
		appendToFile(progressFilename,
				new Date(System.currentTimeMillis()) + ": Running simulation for 4.1_baseline_seed-07.xml");
		CpvSocketClient.main(new String[] { "D:/compham-config/4.1_baseline_seed-07.xml" });
		appendToFile(progressFilename,
				new Date(System.currentTimeMillis()) + ": Running simulation for 4.1_baseline_seed-08.xml");
		CpvSocketClient.main(new String[] { "D:/compham-config/4.1_baseline_seed-08.xml" });
		appendToFile(progressFilename,
				new Date(System.currentTimeMillis()) + ": Running simulation for 4.1_baseline_seed-09.xml");
		CpvSocketClient.main(new String[] { "D:/compham-config/4.1_baseline_seed-09.xml" });
		System.gc();

		// effect of ADI failure on Govt bal sht
		/*
		 * appendToFile(progressFilename, new Date(System.currentTimeMillis()) +
		 * ": Running simulation for 4.2_adi-crash-CBA.xml"); CpvSocketClient.main(new
		 * String[] { "D:/compham-config/4.2_adi-crash-CBA.xml" });
		 * appendToFile(progressFilename, new Date(System.currentTimeMillis()) +
		 * ": Running simulation for 4.2_adi-crash-CBA-no-limit.xml");
		 * CpvSocketClient.main(new String[] {
		 * "D:/compham-config/4.2_adi-crash-CBA-no-limit.xml" });
		 * appendToFile(progressFilename, new Date(System.currentTimeMillis()) +
		 * ": Running simulation for 4.2_adi-crash-mutuals.xml");
		 * CpvSocketClient.main(new String[] {
		 * "D:/compham-config/4.2_adi-crash-mutuals.xml" }); System.gc();
		 */

		// 10% change in FX rates by country
		/*
		 * appendToFile(progressFilename, new Date(System.currentTimeMillis()) +
		 * ": Running simulation for 4.3_FX_Rates_10pc-country-CNY.xml");
		 * CpvSocketClient.main(new String[] {
		 * "D:/compham-config/4.3_FX_Rates_10pc-country-CNY.xml" });
		 */
		/*
		 * appendToFile(progressFilename, new Date(System.currentTimeMillis()) +
		 * ": Running simulation for 4.3_FX_Rates_10pc-country-JPY.xml");
		 * CpvSocketClient.main(new String[] {
		 * "D:/compham-config/4.3_FX_Rates_10pc-country-JPY.xml" });
		 * appendToFile(progressFilename, new Date(System.currentTimeMillis()) +
		 * ": Running simulation for 4.3_FX_Rates_10pc-country-KRW.xml");
		 * CpvSocketClient.main(new String[] {
		 * "D:/compham-config/4.3_FX_Rates_10pc-country-KRW.xml" });
		 * appendToFile(progressFilename, new Date(System.currentTimeMillis()) +
		 * ": Running simulation for 4.3_FX_Rates_10pc-country-USD.xml");
		 * CpvSocketClient.main(new String[] {
		 * "D:/compham-config/4.3_FX_Rates_10pc-country-USD.xml" });
		 * appendToFile(progressFilename, new Date(System.currentTimeMillis()) +
		 * ": Running simulation for 4.3_FX_Rates_10pc-country-INR.xml");
		 * CpvSocketClient.main(new String[] {
		 * "D:/compham-config/4.3_FX_Rates_10pc-country-INR.xml" });
		 * appendToFile(progressFilename, new Date(System.currentTimeMillis()) +
		 * ": Running simulation for 4.3_FX_Rates_10pc-country-THB.xml");
		 * CpvSocketClient.main(new String[] {
		 * "D:/compham-config/4.3_FX_Rates_10pc-country-THB.xml" });
		 * appendToFile(progressFilename, new Date(System.currentTimeMillis()) +
		 * ": Running simulation for 4.3_FX_Rates_10pc-country-MYR.xml");
		 * CpvSocketClient.main(new String[] {
		 * "D:/compham-config/4.3_FX_Rates_10pc-country-MYR.xml" });
		 * appendToFile(progressFilename, new Date(System.currentTimeMillis()) +
		 * ": Running simulation for 4.3_FX_Rates_10pc-country-SGD.xml");
		 * CpvSocketClient.main(new String[] {
		 * "D:/compham-config/4.3_FX_Rates_10pc-country-SGD.xml" });
		 * appendToFile(progressFilename, new Date(System.currentTimeMillis()) +
		 * ": Running simulation for 4.3_FX_Rates_10pc-country-EUR.xml");
		 * CpvSocketClient.main(new String[] {
		 * "D:/compham-config/4.3_FX_Rates_10pc-country-EUR.xml" });
		 * appendToFile(progressFilename, new Date(System.currentTimeMillis()) +
		 * ": Running simulation for 4.3_FX_Rates_10pc-country-NZD.xml");
		 * CpvSocketClient.main(new String[] {
		 * "D:/compham-config/4.3_FX_Rates_10pc-country-NZD.xml" }); System.gc();
		 */

		// Proxying various inflation rates by changing FX rates uniformly
		/*
		 * appendToFile(progressFilename, new Date(System.currentTimeMillis()) +
		 * ": Running simulation for 4.5_Inflation_01pc.xml"); CpvSocketClient.main(new
		 * String[] { "D:/compham-config/4.5_Inflation_01pc.xml" });
		 * appendToFile(progressFilename, new Date(System.currentTimeMillis()) +
		 * ": Running simulation for 4.5_Inflation_02pc.xml"); CpvSocketClient.main(new
		 * String[] { "D:/compham-config/4.5_Inflation_02pc.xml" });
		 * appendToFile(progressFilename, new Date(System.currentTimeMillis()) +
		 * ": Running simulation for 4.5_Inflation_03pc.xml"); CpvSocketClient.main(new
		 * String[] { "D:/compham-config/4.5_Inflation_03pc.xml" });
		 * appendToFile(progressFilename, new Date(System.currentTimeMillis()) +
		 * ": Running simulation for 4.5_Inflation_04pc.xml"); CpvSocketClient.main(new
		 * String[] { "D:/compham-config/4.5_Inflation_04pc.xml" });
		 * appendToFile(progressFilename, new Date(System.currentTimeMillis()) +
		 * ": Running simulation for 4.5_Inflation_05pc.xml"); CpvSocketClient.main(new
		 * String[] { "D:/compham-config/4.5_Inflation_05pc.xml" });
		 * appendToFile(progressFilename, new Date(System.currentTimeMillis()) +
		 * ": Running simulation for 4.5_Inflation_10pc.xml"); CpvSocketClient.main(new
		 * String[] { "D:/compham-config/4.5_Inflation_10pc.xml" });
		 * appendToFile(progressFilename, new Date(System.currentTimeMillis()) +
		 * ": Running simulation for 4.5_Inflation_15pc.xml"); CpvSocketClient.main(new
		 * String[] { "D:/compham-config/4.5_Inflation_15pc.xml" });
		 * appendToFile(progressFilename, new Date(System.currentTimeMillis()) +
		 * ": Running simulation for 4.5_Inflation_20pc.xml"); CpvSocketClient.main(new
		 * String[] { "D:/compham-config/4.5_Inflation_20pc.xml" });
		 * appendToFile(progressFilename, new Date(System.currentTimeMillis()) +
		 * ": Running simulation for 4.5_Inflation_25pc.xml"); CpvSocketClient.main(new
		 * String[] { "D:/compham-config/4.5_Inflation_25pc.xml" });
		 * appendToFile(progressFilename, new Date(System.currentTimeMillis()) +
		 * ": Running simulation for 4.5_Inflation_50pc.xml"); CpvSocketClient.main(new
		 * String[] { "D:/compham-config/4.5_Inflation_50pc.xml" });
		 * appendToFile(progressFilename, new Date(System.currentTimeMillis()) +
		 * ": Running simulation for 4.5_Inflation_75pc.xml"); CpvSocketClient.main(new
		 * String[] { "D:/compham-config/4.5_Inflation_75pc.xml" });
		 * appendToFile(progressFilename, new Date(System.currentTimeMillis()) +
		 * ": Running simulation for 4.5_Inflation_100pc.xml"); CpvSocketClient.main(new
		 * String[] { "D:/compham-config/4.5_Inflation_100pc.xml" });
		 * appendToFile(progressFilename, new Date(System.currentTimeMillis()) +
		 * ": Running simulation for 4.5_Inflation_25pc-4yrs.xml");
		 * CpvSocketClient.main(new String[] {
		 * "D:/compham-config/4.5_Inflation_25pc-4yrs.xml" }); System.gc();
		 */

		appendToFile(progressFilename, new Date(System.currentTimeMillis()) + ": FINISHED");
		AnalyseAll.main(args);
		appendToFile(progressFilename, "...I think I deserve a drink now!");
	}

	private static void appendToFile(String filename, String message) {
		BufferedWriter writer;
		try {
			writer = new BufferedWriter(new FileWriter(filename, true)); // Set true for append mode
			writer.newLine(); // Add new line
			writer.write(message);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			writer = null;
		}
	}

}
