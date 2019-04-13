/**
 * 
 */
package xyz.struthers.rhul.ham;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import xyz.struthers.rhul.ham.config.SpringConfiguration;
import xyz.struthers.rhul.ham.data.AreaMapping;
import xyz.struthers.rhul.ham.data.CalibrateAdis;
import xyz.struthers.rhul.ham.data.CalibrateBusinesses;
import xyz.struthers.rhul.ham.data.CalibrateCountries;
import xyz.struthers.rhul.ham.data.CalibrateCurrencies;
import xyz.struthers.rhul.ham.data.CalibrateEconomy;
import xyz.struthers.rhul.ham.data.CalibrateGovernment;
import xyz.struthers.rhul.ham.data.CalibrateHouseholds;
import xyz.struthers.rhul.ham.data.CalibrateIndividuals;
import xyz.struthers.rhul.ham.data.CalibrateRba;
import xyz.struthers.rhul.ham.data.CalibrationData;
import xyz.struthers.rhul.ham.process.AustralianEconomy;
import xyz.struthers.rhul.ham.process.ClearingPaymentInputs;

/**
 * Initialises the agents and economy, based on raw CSV data.
 * 
 * The main program runs out of RAM, so I've split it up so that this class
 * builds the economy and creates the inputs for the Clearing Payments Vector
 * (CPV). This consumes about 25GB of RAM, so it serializes the economy to disk,
 * passes the CPV inputs back to the caller and drops all the objects to reduce
 * its memory footprint. This should leave enough RAM available for the CPV
 * algorithm to run.
 * 
 * @author Adam Struthers
 * @since 10-Apr-2019
 */
public class InitialiseEconomy {

	AustralianEconomy economy;

	public InitialiseEconomy() {
		super();
	}

	public ClearingPaymentInputs initialiseEconomy() {
		ClearingPaymentInputs cpvInputs = null;

		DecimalFormat formatter = new DecimalFormat("#,##0.00");
		long memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		float megabytesBefore = memoryBefore / 1024f / 1024f;
		System.out.println("MEMORY USAGE BEFORE: " + formatter.format(megabytesBefore) + "MB");

		// load Spring context
		// https://www.tutorialspoint.com/spring/spring_java_based_configuration.htm
		// ApplicationContext ctx = new
		// ClassPathXmlApplicationContext("spring/applicationContext.xml");
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(SpringConfiguration.class);

		/*
		 * CalibrationData data = ctx.getBean(CalibrationData.class); Map<String,
		 * Map<String, String>> abs1292_0_55_002ANZSIC =
		 * data.getAbs1292_0_55_002ANZSIC(); String divCode =
		 * abs1292_0_55_002ANZSIC.get("Division to Division Code")
		 * .get("Agriculture, Forestry and Fishing".toUpperCase());
		 * System.out.println("divCode: " + divCode +
		 * " (Agriculture, Forestry and Fishing)"); System.out.println("keySet: " +
		 * abs1292_0_55_002ANZSIC.get("Division to Division Code").keySet());
		 */

		long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		float megabytesAfter = memoryAfter / 1024f / 1024f;
		System.out.println("MEMORY USAGE BEFORE: " + formatter.format(megabytesAfter) + "MB");
		memoryBefore = memoryAfter;

		System.out.println("Started MeshblockMapping: " + new Date(System.currentTimeMillis()));
		AreaMapping mb = ctx.getBean(AreaMapping.class);
		/*
		 * String gccsa = mb.getGccsaCodeFromLga("10050");
		 * System.out.println("GCCSA is: " + gccsa + " (should be 1RNSW)"); gccsa =
		 * mb.getGccsaCodeFromLga("16350"); System.out.println("GCCSA is: " + gccsa +
		 * " (should be 1GSYD)"); System.out.println("Finished MeshblockMapping: " + new
		 * Date(System.currentTimeMillis()));
		 * 
		 * // System.out.println("Started Calibration Data Load: " + new //
		 * Date(System.currentTimeMillis())); // CalibrationData data =
		 * ctx.getBean(CalibrationData.class);
		 * 
		 * DateFormat df = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH); Date date
		 * = null; try { date = df.parse("01/06/2018"); System.out.println("Date is: " +
		 * date);
		 * 
		 * int totalPop = mb.getTotalPopulation(date);
		 * System.out.println("Total Population is: " + totalPop); int lgaPop =
		 * mb.getAdjustedPeopleByLga("10050", date);
		 * System.out.println("LGA 10050 Population is: " + lgaPop); } catch
		 * (ParseException e) { // date parsing failed e.printStackTrace(); }
		 */

		System.gc();
		memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		megabytesAfter = memoryAfter / 1024f / 1024f;
		megabytesBefore = memoryBefore / 1024f / 1024f;
		System.out.println("MEMORY CONSUMED BY AREA MAPPING: " + formatter.format(megabytesAfter - megabytesBefore)
				+ "MB (CURRENT TOTAL IS: " + formatter.format(megabytesAfter) + "MB)");
		memoryBefore = memoryAfter;

		CalibrateGovernment calGov = ctx.getBean(CalibrateGovernment.class);
		calGov.createGovernmentAgent();
		CalibrateRba calRba = ctx.getBean(CalibrateRba.class);
		calRba.createRbaAgent();
		CalibrateAdis calAdi = ctx.getBean(CalibrateAdis.class);
		calAdi.createAdiAgents();
		CalibrateCurrencies calCcy = ctx.getBean(CalibrateCurrencies.class);
		calCcy.createExchangeRates();
		System.out.println(new Date(System.currentTimeMillis()) + ": CALIBRATING COUNTRIES");
		CalibrateCountries calCountry = ctx.getBean(CalibrateCountries.class);
		calCountry.createCountryAgents();
		System.out.println("calCountry: " + calCountry);
		System.out.println(new Date(System.currentTimeMillis()) + ": FINISHED CALIBRATING COUNTRIES");

		System.gc();
		memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		megabytesAfter = memoryAfter / 1024f / 1024f;
		megabytesBefore = memoryBefore / 1024f / 1024f;
		System.out.println("MEMORY CONSUMED BY RBA, ADI, GOVT, CCY AND COUNTRY: "
				+ formatter.format(megabytesAfter - megabytesBefore) + "MB (CURRENT TOTAL IS: "
				+ formatter.format(megabytesAfter) + "MB)");
		memoryBefore = memoryAfter;

		// System.out.println("Finished Calibration Data Load: " + new
		// Date(System.currentTimeMillis()));

		System.out.println("Starting Business agent calibration: " + new Date(System.currentTimeMillis()));
		// CalibrateBusinesses calBus = new CalibrateBusinesses();
		CalibrateBusinesses calBus = ctx.getBean(CalibrateBusinesses.class);
		calBus.createBusinessAgents();
		System.out.println("Finished Business agent calibration: " + new Date(System.currentTimeMillis()));

		System.gc();
		memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		megabytesAfter = memoryAfter / 1024f / 1024f;
		megabytesBefore = memoryBefore / 1024f / 1024f;
		System.out.println("MEMORY CONSUMED BY BUSINESSES, ETC.: " + formatter.format(megabytesAfter - megabytesBefore)
				+ "MB (CURRENT TOTAL IS: " + formatter.format(megabytesAfter) + "MB)");

		memoryBefore = memoryAfter;

		CalibrateIndividuals calIndiv = ctx.getBean(CalibrateIndividuals.class);
		calIndiv.createIndividualAgents();

		System.gc();
		memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		megabytesAfter = memoryAfter / 1024f / 1024f;
		megabytesBefore = memoryBefore / 1024f / 1024f;
		System.out.println("MEMORY CONSUMED BY INDIVIDUALS: " + formatter.format(megabytesAfter - megabytesBefore)
				+ "MB (CURRENT TOTAL IS: " + formatter.format(megabytesAfter) + "MB)");
		memoryBefore = memoryAfter;

		CalibrateHouseholds calHouse = ctx.getBean(CalibrateHouseholds.class);
		calHouse.createHouseholdAgents();

		System.gc();
		memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		megabytesAfter = memoryAfter / 1024f / 1024f;
		megabytesBefore = memoryBefore / 1024f / 1024f;
		System.out.println("MEMORY CONSUMED BY HOUSEHOLDS: " + formatter.format(megabytesAfter - megabytesBefore)
				+ "MB (CURRENT TOTAL IS: " + formatter.format(megabytesAfter) + "MB)");
		memoryBefore = memoryAfter;
		/*
		 * System.out.println(new Date(System.currentTimeMillis()) +
		 * ": Starting export of agents to CSV"); AustralianEconomy auEconomy =
		 * ctx.getBean(AustralianEconomy.class); int iteration = 0; Set<String>
		 * filenames = auEconomy.saveDetailsToFile(iteration);
		 * System.out.println(filenames.size() + " DETAILED FILES SAVED TO:"); for
		 * (String file : filenames) { System.out.println(file); }
		 * System.out.println(new Date(System.currentTimeMillis()) +
		 * ": Finished export of agents to CSV"); System.exit(0);
		 */
		CalibrateEconomy calibrateEconomy = ctx.getBean(CalibrateEconomy.class);
		calibrateEconomy.linkAllAgents();

		this.economy = ctx.getBean(AustralianEconomy.class);
		// economy.simulateOneMonth(0);
		cpvInputs = economy.prepareOneMonth(0);

		System.gc();
		memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		megabytesAfter = memoryAfter / 1024f / 1024f;
		megabytesBefore = memoryBefore / 1024f / 1024f;
		System.out.println("MEMORY CONSUMED BY LINKING ECONOMY: " + formatter.format(megabytesAfter - megabytesBefore)
				+ "MB (CURRENT TOTAL IS: " + formatter.format(megabytesAfter) + "MB)");
		memoryBefore = memoryAfter;

		ctx.close();

		return cpvInputs;
	}

	/**
	 * @return the economy
	 */
	public AustralianEconomy getEconomy() {
		return economy;
	}

}
