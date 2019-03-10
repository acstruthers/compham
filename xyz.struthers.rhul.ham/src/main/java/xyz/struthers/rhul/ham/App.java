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
import java.util.Random;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import it.unimi.dsi.util.XoRoShiRo128PlusRandom;
import xyz.struthers.rhul.ham.config.SpringConfiguration;
import xyz.struthers.rhul.ham.data.AreaMapping;
import xyz.struthers.rhul.ham.data.CalibrateBusinesses;
import xyz.struthers.rhul.ham.data.CalibrateIndividuals;

/**
 * @author Adam
 *
 */
// @SpringBootApplication
public class App {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		DecimalFormat formatter = new DecimalFormat("#,##0.00");
		long memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		double megabytesBefore = memoryBefore / 1024d / 1024d;
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
		 * abs1292_0_55_002ANZSIC.get("Division to Division Code").
		 * get("Agriculture, Forestry and Fishing".toUpperCase());
		 * System.out.println("divCode: " + divCode +
		 * " (Agriculture, Forestry and Fishing)"); System.out.println("keySet: " +
		 * abs1292_0_55_002ANZSIC.get("Division to Division Code").keySet());
		 */
		
		if (true) {
		
		System.out.println(
				"MEMORY USAGE BEFORE: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));

		System.out.println("Started MeshblockMapping: " + new Date(System.currentTimeMillis()));
		AreaMapping mb = ctx.getBean(AreaMapping.class);
		String gccsa = mb.getGccsaCodeFromLga("10050");
		System.out.println("GCCSA is: " + gccsa + " (should be 1RNSW)");
		gccsa = mb.getGccsaCodeFromLga("16350");
		System.out.println("GCCSA is: " + gccsa + " (should be 1GSYD)");
		System.out.println("Finished MeshblockMapping: " + new Date(System.currentTimeMillis()));

		// System.out.println("Started Calibration Data Load: " + new
		// Date(System.currentTimeMillis()));
		// CalibrationData data = ctx.getBean(CalibrationData.class);

		DateFormat df = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);
		Date date = null;
		try {
			date = df.parse("01/06/2018");
			System.out.println("Date is: " + date);

			int totalPop = mb.getTotalPopulation(date);
			System.out.println("Total Population is: " + totalPop);
			int lgaPop = mb.getAdjustedPeopleByLga("10050", date);
			System.out.println("LGA 10050 Population is: " + lgaPop);
		} catch (ParseException e) {
			// date parsing failed
			e.printStackTrace();
		}

		// System.out.println("Finished Calibration Data Load: " + new
		// Date(System.currentTimeMillis()));

		System.out.println("Starting Business agent calibration: " + new Date(System.currentTimeMillis()));
		// CalibrateBusinesses calBus = new CalibrateBusinesses();
		CalibrateBusinesses calBus = ctx.getBean(CalibrateBusinesses.class);
		calBus.createBusinessAgents();
		System.out.println("Finished Business agent calibration: " + new Date(System.currentTimeMillis()));

		System.gc();
		long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		double megabytesAfter = memoryAfter / 1024d / 1024d;
		System.out.println("MEMORY USAGE AFTER: " + formatter.format(megabytesAfter) + "MB");
		System.out.println(
				"MEMORY CONSUMED BY BUSINESSES, ETC.: " + formatter.format(megabytesAfter - megabytesBefore) + "MB");

		memoryBefore = memoryAfter;

		CalibrateIndividuals calIndiv = ctx.getBean(CalibrateIndividuals.class);
		calIndiv.createIndividualAgents();

		System.gc();
		memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		megabytesAfter = memoryAfter / 1024d / 1024d;
		System.out.println("MEMORY USAGE AFTER: " + formatter.format(megabytesAfter) + "MB");
		System.out.println(
				"MEMORY CONSUMED BY INDIVIDUALS: " + formatter.format(megabytesAfter - megabytesBefore) + "MB");

		while (true) {
		}}
		// ctx.close();
	}

	public void areaMappingTestHarness() {
		System.out.println("Started MeshblockMapping: " + new Date(System.currentTimeMillis()));
		AreaMapping mb = new AreaMapping();
		String gccsa = mb.getGccsaCodeFromLga("10050");
		System.out.println("GCCSA is: " + gccsa + " (should be 1RNSW)");
		gccsa = mb.getGccsaCodeFromLga("16350");
		System.out.println("GCCSA is: " + gccsa + " (should be 1GSYD)");
		System.out.println("Finished MeshblockMapping: " + new Date(System.currentTimeMillis()));
	}
}
