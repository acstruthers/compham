package xyz.struthers.rhul.ham;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

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

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd_HHmm");
		String timestamp = simpleDateFormat.format(new Date(System.currentTimeMillis()));
		String progressFilename = "D:/compham-output/MultiSim_progress_" + timestamp + ".txt";
		appendToFile(progressFilename, new Date(System.currentTimeMillis()) + ": STARTED");
		
		// baseline (exogeneous cash flow histograms)
		/*appendToFile(progressFilename, new Date(System.currentTimeMillis()) + ": Running simulation for 4.1_baseline.xml");
		CpvSocketClient.main(new String[] { "D:/compham-config/4.1_baseline.xml" });
		System.gc();

		// using different random seeds (model error box plots)
		appendToFile(progressFilename, new Date(System.currentTimeMillis()) + ": Running simulation for 4.1_baseline_seed-01.xml");
		CpvSocketClient.main(new String[] { "D:/compham-config/4.1_baseline_seed-01.xml" });
		appendToFile(progressFilename, new Date(System.currentTimeMillis()) + ": Running simulation for 4.1_baseline_seed-02.xml");
		CpvSocketClient.main(new String[] { "D:/compham-config/4.1_baseline_seed-02.xml" });
		appendToFile(progressFilename, new Date(System.currentTimeMillis()) + ": Running simulation for 4.1_baseline_seed-03.xml");
		CpvSocketClient.main(new String[] { "D:/compham-config/4.1_baseline_seed-03.xml" });
		appendToFile(progressFilename, new Date(System.currentTimeMillis()) + ": Running simulation for 4.1_baseline_seed-04.xml");
		CpvSocketClient.main(new String[] { "D:/compham-config/4.1_baseline_seed-04.xml" });
		appendToFile(progressFilename, new Date(System.currentTimeMillis()) + ": Running simulation for 4.1_baseline_seed-05.xml");
		CpvSocketClient.main(new String[] { "D:/compham-config/4.1_baseline_seed-05.xml" });
		appendToFile(progressFilename, new Date(System.currentTimeMillis()) + ": Running simulation for 4.1_baseline_seed-06.xml");
		CpvSocketClient.main(new String[] { "D:/compham-config/4.1_baseline_seed-06.xml" });
		appendToFile(progressFilename, new Date(System.currentTimeMillis()) + ": Running simulation for 4.1_baseline_seed-07.xml");
		CpvSocketClient.main(new String[] { "D:/compham-config/4.1_baseline_seed-07.xml" });
		appendToFile(progressFilename, new Date(System.currentTimeMillis()) + ": Running simulation for 4.1_baseline_seed-08.xml");
		CpvSocketClient.main(new String[] { "D:/compham-config/4.1_baseline_seed-08.xml" });
		appendToFile(progressFilename, new Date(System.currentTimeMillis()) + ": Running simulation for 4.1_baseline_seed-09.xml");
		CpvSocketClient.main(new String[] { "D:/compham-config/4.1_baseline_seed-09.xml" });
		System.gc();*/

		// effect of ADI failure on Govt bal sht
		/*appendToFile(progressFilename, new Date(System.currentTimeMillis()) + ": Running simulation for 04_ADI_Failure_Major-Bank-with-FCS-limit.xml");
		CpvSocketClient.main(new String[] { "D:/compham-config/04_ADI_Failure_Major-Bank-with-FCS-limit.xml" });
		appendToFile(progressFilename, new Date(System.currentTimeMillis()) + ": Running simulation for 04_ADI_Failure_Major-Bank-without-FCS-limit.xml");
		CpvSocketClient.main(new String[] { "D:/compham-config/04_ADI_Failure_Major-Bank-without-FCS-limit.xml" });
		appendToFile(progressFilename, new Date(System.currentTimeMillis()) + ": Running simulation for 04_ADI_Failure_Mutual-ADIs-with-FCS-limit.xml");
		CpvSocketClient.main(new String[] { "D:/compham-config/04_ADI_Failure_Mutual-ADIs-with-FCS-limit.xml" });
		System.gc();
		 */

		// 10% change in FX rates by country
		/*appendToFile(progressFilename, new Date(System.currentTimeMillis()) + ": Running simulation for 4.3_FX_Rates_10pc-country-CNY.xml");
		CpvSocketClient.main(new String[] { "D:/compham-config/4.3_FX_Rates_10pc-country-CNY.xml" });
		appendToFile(progressFilename, new Date(System.currentTimeMillis()) + ": Running simulation for 4.3_FX_Rates_10pc-country-JPY.xml");
		CpvSocketClient.main(new String[] { "D:/compham-config/4.3_FX_Rates_10pc-country-JPY.xml" });
		appendToFile(progressFilename, new Date(System.currentTimeMillis()) + ": Running simulation for 4.3_FX_Rates_10pc-country-KRW.xml");
		CpvSocketClient.main(new String[] { "D:/compham-config/4.3_FX_Rates_10pc-country-KRW.xml" });
		appendToFile(progressFilename, new Date(System.currentTimeMillis()) + ": Running simulation for 4.3_FX_Rates_10pc-country-USD.xml");
		CpvSocketClient.main(new String[] { "D:/compham-config/4.3_FX_Rates_10pc-country-USD.xml" });
		appendToFile(progressFilename, new Date(System.currentTimeMillis()) + ": Running simulation for 4.3_FX_Rates_10pc-country-INR.xml");
		CpvSocketClient.main(new String[] { "D:/compham-config/4.3_FX_Rates_10pc-country-INR.xml" });
		appendToFile(progressFilename, new Date(System.currentTimeMillis()) + ": Running simulation for 4.3_FX_Rates_10pc-country-THB.xml");
		CpvSocketClient.main(new String[] { "D:/compham-config/4.3_FX_Rates_10pc-country-THB.xml" });
		appendToFile(progressFilename, new Date(System.currentTimeMillis()) + ": Running simulation for 4.3_FX_Rates_10pc-country-MYR.xml");
		CpvSocketClient.main(new String[] { "D:/compham-config/4.3_FX_Rates_10pc-country-MYR.xml" });
		appendToFile(progressFilename, new Date(System.currentTimeMillis()) + ": Running simulation for 4.3_FX_Rates_10pc-country-SGD.xml");
		CpvSocketClient.main(new String[] { "D:/compham-config/4.3_FX_Rates_10pc-country-SGD.xml" });
		appendToFile(progressFilename, new Date(System.currentTimeMillis()) + ": Running simulation for 4.3_FX_Rates_10pc-country-EUR.xml");
		CpvSocketClient.main(new String[] { "D:/compham-config/4.3_FX_Rates_10pc-country-EUR.xml" });
		appendToFile(progressFilename, new Date(System.currentTimeMillis()) + ": Running simulation for 4.3_FX_Rates_10pc-country-NZD.xml");
		CpvSocketClient.main(new String[] { "D:/compham-config/4.3_FX_Rates_10pc-country-NZD.xml" });
		System.gc();*/

		// Proxying various inflation rates by changing FX rates uniformly
		appendToFile(progressFilename, new Date(System.currentTimeMillis()) + ": Running simulation for 4.5_Inflation_01pc.xml");
		CpvSocketClient.main(new String[] { "D:/compham-config/4.5_Inflation_01pc.xml" });
		appendToFile(progressFilename, new Date(System.currentTimeMillis()) + ": Running simulation for 4.5_Inflation_02pc.xml");
		CpvSocketClient.main(new String[] { "D:/compham-config/4.5_Inflation_02pc.xml" });
		appendToFile(progressFilename, new Date(System.currentTimeMillis()) + ": Running simulation for 4.5_Inflation_03pc.xml");
		CpvSocketClient.main(new String[] { "D:/compham-config/4.5_Inflation_03pc.xml" });
		appendToFile(progressFilename, new Date(System.currentTimeMillis()) + ": Running simulation for 4.5_Inflation_04pc.xml");
		CpvSocketClient.main(new String[] { "D:/compham-config/4.5_Inflation_04pc.xml" });
		appendToFile(progressFilename, new Date(System.currentTimeMillis()) + ": Running simulation for 4.5_Inflation_05pc.xml");
		CpvSocketClient.main(new String[] { "D:/compham-config/4.5_Inflation_05pc.xml" });
		appendToFile(progressFilename, new Date(System.currentTimeMillis()) + ": Running simulation for 4.5_Inflation_10pc.xml");
		CpvSocketClient.main(new String[] { "D:/compham-config/4.5_Inflation_10pc.xml" });
		/*appendToFile(progressFilename, new Date(System.currentTimeMillis()) + ": Running simulation for 4.5_Inflation_15pc.xml");
		CpvSocketClient.main(new String[] { "D:/compham-config/4.5_Inflation_15pc.xml" });
		appendToFile(progressFilename, new Date(System.currentTimeMillis()) + ": Running simulation for 4.5_Inflation_20pc.xml");
		CpvSocketClient.main(new String[] { "D:/compham-config/4.5_Inflation_20pc.xml" });
		appendToFile(progressFilename, new Date(System.currentTimeMillis()) + ": Running simulation for 4.5_Inflation_25pc.xml");
		CpvSocketClient.main(new String[] { "D:/compham-config/4.5_Inflation_25pc.xml" });
		appendToFile(progressFilename, new Date(System.currentTimeMillis()) + ": Running simulation for 4.5_Inflation_50pc.xml");
		CpvSocketClient.main(new String[] { "D:/compham-config/4.5_Inflation_50pc.xml" });
		appendToFile(progressFilename, new Date(System.currentTimeMillis()) + ": Running simulation for 4.5_Inflation_75pc.xml");
		CpvSocketClient.main(new String[] { "D:/compham-config/4.5_Inflation_75pc.xml" });
		System.gc();*/
		
		// 10% change in FX rates
		/*appendToFile(progressFilename, new Date(System.currentTimeMillis()) + ": Running simulation for 06_FX_Rates_10pc-all-currencies.xml");
		CpvSocketClient.main(new String[] { "D:/compham-config/06_FX_Rates_10pc-all-currencies.xml" });
		appendToFile(progressFilename, new Date(System.currentTimeMillis()) + ": Running simulation for 06_FX_Rates_10pc-currency-AED.xml");
		CpvSocketClient.main(new String[] { "D:/compham-config/06_FX_Rates_10pc-currency-AED.xml" });
		CpvSocketClient.main(new String[] { "D:/compham-config/06_FX_Rates_10pc-currency-ARS.xml" });
		CpvSocketClient.main(new String[] { "D:/compham-config/06_FX_Rates_10pc-currency-BGN.xml" });
		CpvSocketClient.main(new String[] { "D:/compham-config/06_FX_Rates_10pc-currency-BHD.xml" });
		CpvSocketClient.main(new String[] { "D:/compham-config/06_FX_Rates_10pc-currency-BND.xml" });
		appendToFile(progressFilename, new Date(System.currentTimeMillis()) + ": Running simulation for 06_FX_Rates_10pc-currency-BRL.xml");
		CpvSocketClient.main(new String[] { "D:/compham-config/06_FX_Rates_10pc-currency-BRL.xml" });
		CpvSocketClient.main(new String[] { "D:/compham-config/06_FX_Rates_10pc-currency-CAD.xml" });
		CpvSocketClient.main(new String[] { "D:/compham-config/06_FX_Rates_10pc-currency-CHF.xml" });
		CpvSocketClient.main(new String[] { "D:/compham-config/06_FX_Rates_10pc-currency-CLP.xml" });
		CpvSocketClient.main(new String[] { "D:/compham-config/06_FX_Rates_10pc-currency-CNY.xml" });
		appendToFile(progressFilename, new Date(System.currentTimeMillis()) + ": Running simulation for 06_FX_Rates_10pc-currency-CZK.xml");
		CpvSocketClient.main(new String[] { "D:/compham-config/06_FX_Rates_10pc-currency-CZK.xml" });
		CpvSocketClient.main(new String[] { "D:/compham-config/06_FX_Rates_10pc-currency-DKK.xml" });
		CpvSocketClient.main(new String[] { "D:/compham-config/06_FX_Rates_10pc-currency-EGP.xml" });
		CpvSocketClient.main(new String[] { "D:/compham-config/06_FX_Rates_10pc-currency-EUR.xml" });
		CpvSocketClient.main(new String[] { "D:/compham-config/06_FX_Rates_10pc-currency-FJD.xml" });
		appendToFile(progressFilename, new Date(System.currentTimeMillis()) + ": Running simulation for 06_FX_Rates_10pc-currency-GBP.xml");
		CpvSocketClient.main(new String[] { "D:/compham-config/06_FX_Rates_10pc-currency-GBP.xml" });
		CpvSocketClient.main(new String[] { "D:/compham-config/06_FX_Rates_10pc-currency-HKD.xml" });
		CpvSocketClient.main(new String[] { "D:/compham-config/06_FX_Rates_10pc-currency-HUF.xml" });
		CpvSocketClient.main(new String[] { "D:/compham-config/06_FX_Rates_10pc-currency-IDR.xml" });
		CpvSocketClient.main(new String[] { "D:/compham-config/06_FX_Rates_10pc-currency-ILS.xml" });
		appendToFile(progressFilename, new Date(System.currentTimeMillis()) + ": Running simulation for 06_FX_Rates_10pc-currency-INR.xml");
		CpvSocketClient.main(new String[] { "D:/compham-config/06_FX_Rates_10pc-currency-INR.xml" });
		CpvSocketClient.main(new String[] { "D:/compham-config/06_FX_Rates_10pc-currency-JPY.xml" });
		CpvSocketClient.main(new String[] { "D:/compham-config/06_FX_Rates_10pc-currency-KRW.xml" });
		CpvSocketClient.main(new String[] { "D:/compham-config/06_FX_Rates_10pc-currency-KWD.xml" });
		CpvSocketClient.main(new String[] { "D:/compham-config/06_FX_Rates_10pc-currency-LKR.xml" });
		appendToFile(progressFilename, new Date(System.currentTimeMillis()) + ": Running simulation for 06_FX_Rates_10pc-currency-MAD.xml");
		CpvSocketClient.main(new String[] { "D:/compham-config/06_FX_Rates_10pc-currency-MAD.xml" });
		CpvSocketClient.main(new String[] { "D:/compham-config/06_FX_Rates_10pc-currency-MGA.xml" });
		CpvSocketClient.main(new String[] { "D:/compham-config/06_FX_Rates_10pc-currency-MXN.xml" });
		CpvSocketClient.main(new String[] { "D:/compham-config/06_FX_Rates_10pc-currency-MYR.xml" });
		CpvSocketClient.main(new String[] { "D:/compham-config/06_FX_Rates_10pc-currency-NOK.xml" });
		appendToFile(progressFilename, new Date(System.currentTimeMillis()) + ": Running simulation for 06_FX_Rates_10pc-currency-NZD.xml");
		CpvSocketClient.main(new String[] { "D:/compham-config/06_FX_Rates_10pc-currency-NZD.xml" });
		CpvSocketClient.main(new String[] { "D:/compham-config/06_FX_Rates_10pc-currency-OMR.xml" });
		CpvSocketClient.main(new String[] { "D:/compham-config/06_FX_Rates_10pc-currency-PEN.xml" });
		CpvSocketClient.main(new String[] { "D:/compham-config/06_FX_Rates_10pc-currency-PGK.xml" });
		CpvSocketClient.main(new String[] { "D:/compham-config/06_FX_Rates_10pc-currency-PHP.xml" });
		appendToFile(progressFilename, new Date(System.currentTimeMillis()) + ": Running simulation for 06_FX_Rates_10pc-currency-PKR.xml");
		CpvSocketClient.main(new String[] { "D:/compham-config/06_FX_Rates_10pc-currency-PKR.xml" });
		CpvSocketClient.main(new String[] { "D:/compham-config/06_FX_Rates_10pc-currency-PLN.xml" });
		CpvSocketClient.main(new String[] { "D:/compham-config/06_FX_Rates_10pc-currency-RUB.xml" });
		CpvSocketClient.main(new String[] { "D:/compham-config/06_FX_Rates_10pc-currency-SAR.xml" });
		CpvSocketClient.main(new String[] { "D:/compham-config/06_FX_Rates_10pc-currency-SCR.xml" });
		appendToFile(progressFilename, new Date(System.currentTimeMillis()) + ": Running simulation for 06_FX_Rates_10pc-currency-SEK.xml");
		CpvSocketClient.main(new String[] { "D:/compham-config/06_FX_Rates_10pc-currency-SEK.xml" });
		CpvSocketClient.main(new String[] { "D:/compham-config/06_FX_Rates_10pc-currency-SGD.xml" });
		CpvSocketClient.main(new String[] { "D:/compham-config/06_FX_Rates_10pc-currency-THB.xml" });
		CpvSocketClient.main(new String[] { "D:/compham-config/06_FX_Rates_10pc-currency-TRY.xml" });
		CpvSocketClient.main(new String[] { "D:/compham-config/06_FX_Rates_10pc-currency-TWD.xml" });
		appendToFile(progressFilename, new Date(System.currentTimeMillis()) + ": Running simulation for 06_FX_Rates_10pc-currency-TZS.xml");
		CpvSocketClient.main(new String[] { "D:/compham-config/06_FX_Rates_10pc-currency-TZS.xml" });
		CpvSocketClient.main(new String[] { "D:/compham-config/06_FX_Rates_10pc-currency-USD.xml" });
		CpvSocketClient.main(new String[] { "D:/compham-config/06_FX_Rates_10pc-currency-VND.xml" });
		CpvSocketClient.main(new String[] { "D:/compham-config/06_FX_Rates_10pc-currency-XOF.xml" });
		CpvSocketClient.main(new String[] { "D:/compham-config/06_FX_Rates_10pc-currency-XPF.xml" });
		appendToFile(progressFilename, new Date(System.currentTimeMillis()) + ": Running simulation for 06_FX_Rates_10pc-currency-ZAR.xml");
		CpvSocketClient.main(new String[] { "D:/compham-config/06_FX_Rates_10pc-currency-ZAR.xml" });
		System.gc();
		*/

		appendToFile(progressFilename, new Date(System.currentTimeMillis()) + ": FINISHED");
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
