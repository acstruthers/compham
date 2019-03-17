/**
 * 
 */
package xyz.struthers.rhul.ham.data;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opencsv.CSVReader;

/**
 * Loads CSV data downloaded using Table Builder from the ABS, RBA, APRA and
 * ATO. This class contains the data that is only needed when calibrating
 * individuals.
 * 
 * @author Adam Struthers
 * @since 2019-03-12
 */
@Component
@Scope(value = "singleton")
public class CalibrationDataHousehold {

	private static final boolean DEBUG = true;

	// map implementation optimisation
	public static final double MAP_LOAD_FACTOR = 0.75d;

	public static final int MAP_INIT_SIZE_LGA = (int) Math.ceil(573 / MAP_LOAD_FACTOR); // 572 (UR) including state
																						// totals (563 Enum)
	public static final int MAP_INIT_SIZE_CDCF = (int) Math.ceil(15 / MAP_LOAD_FACTOR); // 15 (no totals in data)
	public static final int MAP_INIT_SIZE_FINF = (int) Math.ceil(25 / MAP_LOAD_FACTOR); // 25 plus totals
	public static final int MAP_INIT_SIZE_HCFMD = (int) Math.ceil(18 / MAP_LOAD_FACTOR); // 17 plus totals
	public static final int MAP_INIT_SIZE_HCFMF = (int) Math.ceil(18 / MAP_LOAD_FACTOR); // 17 plus totals
	public static final int MAP_INIT_SIZE_HIND = (int) Math.ceil(26 / MAP_LOAD_FACTOR); // 25 plus totals
	public static final int MAP_INIT_SIZE_MRERD = (int) Math.ceil(22 / MAP_LOAD_FACTOR); // 21 plus totals
	public static final int MAP_INIT_SIZE_RNTRD = (int) Math.ceil(26 / MAP_LOAD_FACTOR); // 25 plus totals

	// series names
	public static final String RBA_E2 = "RBA_E2"; // household Balance Sheet ratios

	public static final String ABS1410_0_ECONOMY = "ABS_1410.0_Economy";
	static final String ABS1410_0_FAMILY = "ABS_1410.0_Family";

	public static final String CENSUS_HCFMD_LGA_HIND_RNTRD = "Census HCFMD by LGA by HIND and RNTRD";
	public static final String CENSUS_HCFMD_LGA_HIND_MRERD = "Census HCFMD by LGA by HIND and MRERD";
	public static final String CENSUS_HCFMF_LGA_FINF_CDCF = "Census HCFMF by LGA by FINF and CDCF";

	// beans
	private AreaMapping area;
	private CalibrationData sharedData;

	// data
	private boolean dataLoaded;
	private Map<Date, Integer> totalPopulation;
	private Map<String, Integer> adjustedPeopleByLga;
	private Map<String, List<String>> title;
	private Map<String, List<String>> unitType;

	private Map<String, Map<Date, String>> rbaE2; // AU Bal Sht ratios
	/**
	 * Data by LGA: Economy
	 * 
	 * Housing prices per LGA, both houses and units. These will be used to
	 * calibrate the assets of households with no mortgage or rent payments because
	 * they presumably own their own homes outright.
	 * 
	 * Keys: Year (yyyy), LGA code, Series Title
	 */
	private Map<String, Map<String, Map<String, Double>>> abs1410_0Economy;
	/**
	 * Data by LGA: Family
	 * 
	 * Contains % of families in each LGA where rent/mortgage payments are more/less
	 * than 30% of family income. This is the ABS definition of "household stress"
	 * for housing affordability.<br>
	 * 
	 * Keys: Year (yyyy), LGA code, Series Title
	 */
	private Map<String, Map<String, Map<String, Double>>> abs1410_0Family;
	// Map<String, Map<String, Map<String, Double>>> abs1410_0Income; // Data by
	// LGA: Income
	/**
	 * ABS Census Table Builder data:<br>
	 * HCFMD by LGA by HIND and RNTRD<br>
	 * Rent by household income and composition.
	 * 
	 * Keys: Household Income, Rent Range, LGA, Household Composition Dwelling<br>
	 * Values: Number of dwellings
	 */
	private Map<String, Map<String, Map<String, Map<String, Integer>>>> censusHCFMD_LGA_HIND_RNTRD;
	/**
	 * ABS Census Table Builder data:<br>
	 * HCFMD by LGA by HIND and MRERD<br>
	 * Mortgage payments by household income and composition.
	 * 
	 * Keys: Household Income, Rent Range, LGA, Household Composition Dwelling<br>
	 * Values: Number of dwellings
	 */
	private Map<String, Map<String, Map<String, Map<String, Integer>>>> censusHCFMD_LGA_HIND_MRERD;
	/**
	 * ABS Census Table Builder data:<br>
	 * HCFMF by LGA by FINF and CDCF<br>
	 * Parents & children by family income and composition.
	 * 
	 * Keys: Family Income, Parents & Children (CDCF), LGA, Family Composition
	 * Dwelling<br>
	 * Values: Number of dwellings
	 */
	private Map<String, Map<String, Map<String, Map<String, Integer>>>> censusHCFMF_LGA_FINF_CDCF;

	private boolean initialisedCensusHCFMD_LGA_HIND_RNTRD;
	private boolean initialisedCensusHCFMD_LGA_HIND_MRERD;
	private boolean initialisedCensusHCFMF_LGA_FINF_CDCF;

	/**
	 * 
	 */
	public CalibrationDataHousehold() {
		super();
		this.init();
	}

	private void init() {
		this.dataLoaded = false;
		this.totalPopulation = null;
		this.adjustedPeopleByLga = null;

		this.title = null;
		this.unitType = null;

		this.rbaE2 = null;

		this.abs1410_0Economy = null;
		this.abs1410_0Family = null;

		this.censusHCFMD_LGA_HIND_MRERD = null;
		this.censusHCFMD_LGA_HIND_RNTRD = null;
		this.censusHCFMF_LGA_FINF_CDCF = null;
		this.initialisedCensusHCFMD_LGA_HIND_MRERD = false;
		this.initialisedCensusHCFMD_LGA_HIND_RNTRD = false;
		this.initialisedCensusHCFMF_LGA_FINF_CDCF = false;

	}

	/**
	 * Deletes all the field variables, freeing up memory.
	 * 
	 * Does not do a deep delete because most objects are passed to other classes by
	 * reference, and the other classes will probably still need to refer to them.
	 */
	@PreDestroy
	public void close() {
		this.init(); // set all the pointers to null
	}

	private void loadData() {
		this.title = new HashMap<String, List<String>>();
		this.unitType = new HashMap<String, List<String>>();

		long memoryBefore = 0L; // for debugging memory consumption

		if (DEBUG) {
			System.gc();
			memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		}

		// load RBA data
		System.out.println(new Date(System.currentTimeMillis()) + ": Loading RBA E2 data");
		int[] rbaE2Columns = { 3, 6 };
		// int[] rbaE2Columns = { 3, 6, 8, 9, 10 };
		int rbaE2MapCapacity = (int) Math.ceil(rbaE2Columns.length / MAP_LOAD_FACTOR);
		this.rbaE2 = new HashMap<String, Map<Date, String>>(rbaE2MapCapacity);
		this.loadRbaDataCsv("/data/RBA/E_HouseholdBusiness/e2-data.csv", RBA_E2, rbaE2Columns, this.title,
				this.unitType, this.rbaE2);

		if (DEBUG) {
			System.gc();
			long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			double megabytesConsumed = (memoryAfter - memoryBefore) / 1024d / 1024d;
			DecimalFormat formatter = new DecimalFormat("#,##0.00");
			System.out.println(">>> Memory used by RBA E2: " + formatter.format(megabytesConsumed) + "MB");
			memoryBefore = memoryAfter;
		}

		// load ABS 1410.0 data
		System.out.println(new Date(System.currentTimeMillis()) + ": Loading ABS 1410.0 Economy data");
		this.abs1410_0Economy = new HashMap<String, Map<String, Map<String, Double>>>(7); // 7 years in the data file
		int[] abs1410_0EconomyColumns = { 49, 50, 51, 52 };
		// int[] abs1410_0EconomyColumns = { 49, 50, 51, 52, 85, 86, 87, 88, 89, 90, 91,
		// 92, 93, 94, 95, 96, 97, 98, 99,
		// 100, 101, 102, 103, 104 };
		String[] abs1410_0EconomyYears = { "2016" };
		this.loadAbsDataCsv_1410_0("/data/ABS/1410.0_DataByRegion/Economy and Industry, LGA, 2011 to 2017.csv",
				ABS1410_0_ECONOMY, abs1410_0EconomyColumns, abs1410_0EconomyYears, this.title, this.unitType,
				this.abs1410_0Economy);

		if (DEBUG) {
			System.gc();
			long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			double megabytesConsumed = (memoryAfter - memoryBefore) / 1024d / 1024d;
			DecimalFormat formatter = new DecimalFormat("#,##0.00");
			System.out.println(">>> Memory used by ABS 1410.0 Economy: " + formatter.format(megabytesConsumed) + "MB");
			memoryBefore = memoryAfter;
		}

		System.out.println(new Date(System.currentTimeMillis()) + ": Loading ABS 1410.0 Family data");
		this.abs1410_0Family = new HashMap<String, Map<String, Map<String, Double>>>(7); // 7 years in the data file
		int[] abs1410_0FamilyColumns = { 61, 63 };
		// int[] abs1410_0FamilyColumns = { 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28,
		// 29, 30, 41, 42, 49, 57, 58, 59, 60,
		// 61, 62, 63 };
		String[] abs1410_0FamilyYears = { "2016" };
		this.loadAbsDataCsv_1410_0("/data/ABS/1410.0_DataByRegion/Family and Community, LGA, 2011 to 2017.csv",
				ABS1410_0_FAMILY, abs1410_0FamilyColumns, abs1410_0FamilyYears, this.title, this.unitType,
				this.abs1410_0Family);

		if (DEBUG) {
			System.gc();
			long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			double megabytesConsumed = (memoryAfter - memoryBefore) / 1024d / 1024d;
			DecimalFormat formatter = new DecimalFormat("#,##0.00");
			System.out.println(">>> Memory used by ABS 1410.0 Family: " + formatter.format(megabytesConsumed) + "MB");
			memoryBefore = memoryAfter;
		}

		/*
		 * System.out.println(new Date(System.currentTimeMillis()) +
		 * ": Loading ABS 1410.0 Income data"); this.abs1410_0Income = new
		 * HashMap<String, Map<String, Map<String, String>>>(7); // 7 years in the data
		 * file int[] abs1410_0IncomeColumns = { 15, 19, 20, 21, 25, 48, 49, 50, 51, 52,
		 * 53, 54, 55, 56, 57, 58, 59, 60 }; String[] abs1410_0IncomeYears = { "2016" };
		 * this.loadAbsDataCsv_1410_0(
		 * "/data/ABS/1410.0_DataByRegion/Income (including Government Allowances), LGA, 2011 to 2017.csv"
		 * , ABS1410_0_INCOME, abs1410_0IncomeColumns, abs1410_0IncomeYears, this.title,
		 * this.unitType, this.abs1410_0Income);
		 * 
		 * if (DEBUG) { System.gc(); long memoryAfter =
		 * Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		 * double megabytesConsumed = (memoryAfter - memoryBefore) / 1024d / 1024d;
		 * DecimalFormat formatter = new DecimalFormat("#,##0.00");
		 * System.out.println(">>> Memory used by ABS 1410.0 Income: " +
		 * formatter.format(megabytesConsumed) + "MB"); memoryBefore = memoryAfter; }
		 */

		// ABS Census HCFMD by LGA by HIND and RNTRD
		System.out.print(
				new Date(System.currentTimeMillis()) + ": Loading ABS Census HCFMD by LGA by HIND and RNTRD data");
		this.censusHCFMD_LGA_HIND_RNTRD = new HashMap<String, Map<String, Map<String, Map<String, Integer>>>>(
				MAP_INIT_SIZE_HIND);
		int fromColumnHCFMD_LGA_HIND_RNTRD = 1;
		int toColumnHCFMD_LGA_HIND_RNTRD = 651;
		System.out.print(": NSW");
		this.loadAbsCensusTableCsv2Columns1Wafer(
				"/data/ABS/CensusTableBuilder2016/HCFMD by LGA by HIND and RNTRD/HCFMD by LGA by HIND and RNTRD - NSW.csv",
				this.initialisedCensusHCFMD_LGA_HIND_RNTRD, fromColumnHCFMD_LGA_HIND_RNTRD,
				toColumnHCFMD_LGA_HIND_RNTRD, this.censusHCFMD_LGA_HIND_RNTRD);
		this.initialisedCensusHCFMD_LGA_HIND_RNTRD = true;
		System.out.print(", VIC");
		this.loadAbsCensusTableCsv2Columns1Wafer(
				"/data/ABS/CensusTableBuilder2016/HCFMD by LGA by HIND and RNTRD/HCFMD by LGA by HIND and RNTRD - VIC.csv",
				this.initialisedCensusHCFMD_LGA_HIND_RNTRD, fromColumnHCFMD_LGA_HIND_RNTRD,
				toColumnHCFMD_LGA_HIND_RNTRD, this.censusHCFMD_LGA_HIND_RNTRD);
		System.out.print(", QLD");
		this.loadAbsCensusTableCsv2Columns1Wafer(
				"/data/ABS/CensusTableBuilder2016/HCFMD by LGA by HIND and RNTRD/HCFMD by LGA by HIND and RNTRD - QLD.csv",
				this.initialisedCensusHCFMD_LGA_HIND_RNTRD, fromColumnHCFMD_LGA_HIND_RNTRD,
				toColumnHCFMD_LGA_HIND_RNTRD, this.censusHCFMD_LGA_HIND_RNTRD);
		System.out.print(", SA");
		this.loadAbsCensusTableCsv2Columns1Wafer(
				"/data/ABS/CensusTableBuilder2016/HCFMD by LGA by HIND and RNTRD/HCFMD by LGA by HIND and RNTRD - SA.csv",
				this.initialisedCensusHCFMD_LGA_HIND_RNTRD, fromColumnHCFMD_LGA_HIND_RNTRD,
				toColumnHCFMD_LGA_HIND_RNTRD, this.censusHCFMD_LGA_HIND_RNTRD);
		System.out.print(", WA");
		this.loadAbsCensusTableCsv2Columns1Wafer(
				"/data/ABS/CensusTableBuilder2016/HCFMD by LGA by HIND and RNTRD/HCFMD by LGA by HIND and RNTRD - WA.csv",
				this.initialisedCensusHCFMD_LGA_HIND_RNTRD, fromColumnHCFMD_LGA_HIND_RNTRD,
				toColumnHCFMD_LGA_HIND_RNTRD, this.censusHCFMD_LGA_HIND_RNTRD);
		System.out.print(", TAS");
		this.loadAbsCensusTableCsv2Columns1Wafer(
				"/data/ABS/CensusTableBuilder2016/HCFMD by LGA by HIND and RNTRD/HCFMD by LGA by HIND and RNTRD - TAS.csv",
				this.initialisedCensusHCFMD_LGA_HIND_RNTRD, fromColumnHCFMD_LGA_HIND_RNTRD,
				toColumnHCFMD_LGA_HIND_RNTRD, this.censusHCFMD_LGA_HIND_RNTRD);
		System.out.print(", NT");
		this.loadAbsCensusTableCsv2Columns1Wafer(
				"/data/ABS/CensusTableBuilder2016/HCFMD by LGA by HIND and RNTRD/HCFMD by LGA by HIND and RNTRD - NT.csv",
				this.initialisedCensusHCFMD_LGA_HIND_RNTRD, fromColumnHCFMD_LGA_HIND_RNTRD,
				toColumnHCFMD_LGA_HIND_RNTRD, this.censusHCFMD_LGA_HIND_RNTRD);
		System.out.print(", ACT");
		this.loadAbsCensusTableCsv2Columns1Wafer(
				"/data/ABS/CensusTableBuilder2016/HCFMD by LGA by HIND and RNTRD/HCFMD by LGA by HIND and RNTRD - ACT.csv",
				this.initialisedCensusHCFMD_LGA_HIND_RNTRD, fromColumnHCFMD_LGA_HIND_RNTRD,
				toColumnHCFMD_LGA_HIND_RNTRD, this.censusHCFMD_LGA_HIND_RNTRD);
		/*
		 * System.out.println(", OT."); this.loadAbsCensusTableCsv2Columns1Wafer(
		 * "/data/ABS/CensusTableBuilder2016/HCFMD by LGA by HIND and RNTRD/HCFMD by LGA by HIND and RNTRD - OT.csv"
		 * , this.initialisedCensusHCFMD_LGA_HIND_RNTRD, fromColumnHCFMD_LGA_HIND_RNTRD,
		 * toColumnHCFMD_LGA_HIND_RNTRD, this.censusHCFMD_LGA_HIND_RNTRD);
		 */

		if (DEBUG) {
			System.gc();
			long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			double megabytesConsumed = (memoryAfter - memoryBefore) / 1024d / 1024d;
			DecimalFormat formatter = new DecimalFormat("#,##0.00");
			System.out.println(">>> Memory used by ABS Census HCFMD by LGA by HIND and RNTRD: "
					+ formatter.format(megabytesConsumed) + "MB");
			memoryBefore = memoryAfter;
		}

		// ABS Census HCFMD by LGA by HIND and MRERD
		System.out.print(
				new Date(System.currentTimeMillis()) + ": Loading ABS Census HCFMD by LGA by HIND and MRERD data");
		this.censusHCFMD_LGA_HIND_MRERD = new HashMap<String, Map<String, Map<String, Map<String, Integer>>>>(
				MAP_INIT_SIZE_HIND);
		int fromColumnHCFMD_LGA_HIND_MRERD = 1;
		int toColumnHCFMD_LGA_HIND_MRERD = 547;
		System.out.print(": NSW");
		this.loadAbsCensusTableCsv2Columns1Wafer(
				"/data/ABS/CensusTableBuilder2016/HCFMD by LGA by HIND and MRERD/HCFMD by LGA by HIND and MRERD - NSW.csv",
				this.initialisedCensusHCFMD_LGA_HIND_MRERD, fromColumnHCFMD_LGA_HIND_MRERD,
				toColumnHCFMD_LGA_HIND_MRERD, this.censusHCFMD_LGA_HIND_MRERD);
		this.initialisedCensusHCFMD_LGA_HIND_MRERD = true;
		System.out.print(", VIC");
		this.loadAbsCensusTableCsv2Columns1Wafer(
				"/data/ABS/CensusTableBuilder2016/HCFMD by LGA by HIND and MRERD/HCFMD by LGA by HIND and MRERD - VIC.csv",
				this.initialisedCensusHCFMD_LGA_HIND_MRERD, fromColumnHCFMD_LGA_HIND_MRERD,
				toColumnHCFMD_LGA_HIND_MRERD, this.censusHCFMD_LGA_HIND_MRERD);
		System.out.print(", VIQLD");
		this.loadAbsCensusTableCsv2Columns1Wafer(
				"/data/ABS/CensusTableBuilder2016/HCFMD by LGA by HIND and MRERD/HCFMD by LGA by HIND and MRERD - QLD.csv",
				this.initialisedCensusHCFMD_LGA_HIND_MRERD, fromColumnHCFMD_LGA_HIND_MRERD,
				toColumnHCFMD_LGA_HIND_MRERD, this.censusHCFMD_LGA_HIND_MRERD);
		System.out.print(", SA");
		this.loadAbsCensusTableCsv2Columns1Wafer(
				"/data/ABS/CensusTableBuilder2016/HCFMD by LGA by HIND and MRERD/HCFMD by LGA by HIND and MRERD - SA.csv",
				this.initialisedCensusHCFMD_LGA_HIND_MRERD, fromColumnHCFMD_LGA_HIND_MRERD,
				toColumnHCFMD_LGA_HIND_MRERD, this.censusHCFMD_LGA_HIND_MRERD);
		System.out.print(", WA");
		this.loadAbsCensusTableCsv2Columns1Wafer(
				"/data/ABS/CensusTableBuilder2016/HCFMD by LGA by HIND and MRERD/HCFMD by LGA by HIND and MRERD - WA.csv",
				this.initialisedCensusHCFMD_LGA_HIND_MRERD, fromColumnHCFMD_LGA_HIND_MRERD,
				toColumnHCFMD_LGA_HIND_MRERD, this.censusHCFMD_LGA_HIND_MRERD);
		System.out.print(", TAS");
		this.loadAbsCensusTableCsv2Columns1Wafer(
				"/data/ABS/CensusTableBuilder2016/HCFMD by LGA by HIND and MRERD/HCFMD by LGA by HIND and MRERD - TAS.csv",
				this.initialisedCensusHCFMD_LGA_HIND_MRERD, fromColumnHCFMD_LGA_HIND_MRERD,
				toColumnHCFMD_LGA_HIND_MRERD, this.censusHCFMD_LGA_HIND_MRERD);
		System.out.print(", NT");
		this.loadAbsCensusTableCsv2Columns1Wafer(
				"/data/ABS/CensusTableBuilder2016/HCFMD by LGA by HIND and MRERD/HCFMD by LGA by HIND and MRERD - NT.csv",
				this.initialisedCensusHCFMD_LGA_HIND_MRERD, fromColumnHCFMD_LGA_HIND_MRERD,
				toColumnHCFMD_LGA_HIND_MRERD, this.censusHCFMD_LGA_HIND_MRERD);
		System.out.print(", ACT");
		this.loadAbsCensusTableCsv2Columns1Wafer(
				"/data/ABS/CensusTableBuilder2016/HCFMD by LGA by HIND and MRERD/HCFMD by LGA by HIND and MRERD - ACT.csv",
				this.initialisedCensusHCFMD_LGA_HIND_MRERD, fromColumnHCFMD_LGA_HIND_MRERD,
				toColumnHCFMD_LGA_HIND_MRERD, this.censusHCFMD_LGA_HIND_MRERD);
		/*
		 * System.out.println(", OT."); this.loadAbsCensusTableCsv2Columns1Wafer(
		 * "/data/ABS/CensusTableBuilder2016/HCFMD by LGA by HIND and MRERD/HCFMD by LGA by HIND and MRERD - OT.csv"
		 * , this.initialisedCensusHCFMD_LGA_HIND_MRERD, fromColumnHCFMD_LGA_HIND_MRERD,
		 * toColumnHCFMD_LGA_HIND_MRERD, this.censusHCFMD_LGA_HIND_MRERD);
		 */

		if (DEBUG) {
			System.gc();
			long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			double megabytesConsumed = (memoryAfter - memoryBefore) / 1024d / 1024d;
			DecimalFormat formatter = new DecimalFormat("#,##0.00");
			System.out.println(">>> Memory used by ABS Census HCFMD by LGA by HIND and MRERD: "
					+ formatter.format(megabytesConsumed) + "MB");
			memoryBefore = memoryAfter;
		}

		// ABS Census HCFMF by LGA by FINF and CDCF
		System.out.print(
				new Date(System.currentTimeMillis()) + ": Loading ABS Census HCFMD by LGA by FINF and CDCF data");
		this.censusHCFMF_LGA_FINF_CDCF = new HashMap<String, Map<String, Map<String, Map<String, Integer>>>>(
				MAP_INIT_SIZE_FINF);
		int fromColumnHCFMF_LGA_FINF_CDCF = 1;
		int toColumnHCFMF_LGA_FINF_CDCF = 416;
		System.out.print(": NSW");
		this.loadAbsCensusTableCsv2Columns1Wafer(
				"/data/ABS/CensusTableBuilder2016/HCFMF by LGA by FINF and CDCF/HCFMF by LGA by FINF and CDCF - NSW.csv",
				this.initialisedCensusHCFMF_LGA_FINF_CDCF, fromColumnHCFMF_LGA_FINF_CDCF, toColumnHCFMF_LGA_FINF_CDCF,
				this.censusHCFMF_LGA_FINF_CDCF);
		this.initialisedCensusHCFMF_LGA_FINF_CDCF = true;
		System.out.print(", VIC");
		this.loadAbsCensusTableCsv2Columns1Wafer(
				"/data/ABS/CensusTableBuilder2016/HCFMF by LGA by FINF and CDCF/HCFMF by LGA by FINF and CDCF - VIC.csv",
				this.initialisedCensusHCFMF_LGA_FINF_CDCF, fromColumnHCFMF_LGA_FINF_CDCF, toColumnHCFMF_LGA_FINF_CDCF,
				this.censusHCFMF_LGA_FINF_CDCF);
		System.out.print(", QLD");
		this.loadAbsCensusTableCsv2Columns1Wafer(
				"/data/ABS/CensusTableBuilder2016/HCFMF by LGA by FINF and CDCF/HCFMF by LGA by FINF and CDCF - QLD.csv",
				this.initialisedCensusHCFMF_LGA_FINF_CDCF, fromColumnHCFMF_LGA_FINF_CDCF, toColumnHCFMF_LGA_FINF_CDCF,
				this.censusHCFMF_LGA_FINF_CDCF);
		System.out.print(", SA");
		this.loadAbsCensusTableCsv2Columns1Wafer(
				"/data/ABS/CensusTableBuilder2016/HCFMF by LGA by FINF and CDCF/HCFMF by LGA by FINF and CDCF - SA.csv",
				this.initialisedCensusHCFMF_LGA_FINF_CDCF, fromColumnHCFMF_LGA_FINF_CDCF, toColumnHCFMF_LGA_FINF_CDCF,
				this.censusHCFMF_LGA_FINF_CDCF);
		System.out.print(", WA");
		this.loadAbsCensusTableCsv2Columns1Wafer(
				"/data/ABS/CensusTableBuilder2016/HCFMF by LGA by FINF and CDCF/HCFMF by LGA by FINF and CDCF - WA.csv",
				this.initialisedCensusHCFMF_LGA_FINF_CDCF, fromColumnHCFMF_LGA_FINF_CDCF, toColumnHCFMF_LGA_FINF_CDCF,
				this.censusHCFMF_LGA_FINF_CDCF);
		System.out.print(", TAS");
		this.loadAbsCensusTableCsv2Columns1Wafer(
				"/data/ABS/CensusTableBuilder2016/HCFMF by LGA by FINF and CDCF/HCFMF by LGA by FINF and CDCF - TAS.csv",
				this.initialisedCensusHCFMF_LGA_FINF_CDCF, fromColumnHCFMF_LGA_FINF_CDCF, toColumnHCFMF_LGA_FINF_CDCF,
				this.censusHCFMF_LGA_FINF_CDCF);
		System.out.print(", NT");
		this.loadAbsCensusTableCsv2Columns1Wafer(
				"/data/ABS/CensusTableBuilder2016/HCFMF by LGA by FINF and CDCF/HCFMF by LGA by FINF and CDCF - NT.csv",
				this.initialisedCensusHCFMF_LGA_FINF_CDCF, fromColumnHCFMF_LGA_FINF_CDCF, toColumnHCFMF_LGA_FINF_CDCF,
				this.censusHCFMF_LGA_FINF_CDCF);
		System.out.print(", ACT");
		this.loadAbsCensusTableCsv2Columns1Wafer(
				"/data/ABS/CensusTableBuilder2016/HCFMF by LGA by FINF and CDCF/HCFMF by LGA by FINF and CDCF - ACT.csv",
				this.initialisedCensusHCFMF_LGA_FINF_CDCF, fromColumnHCFMF_LGA_FINF_CDCF, toColumnHCFMF_LGA_FINF_CDCF,
				this.censusHCFMF_LGA_FINF_CDCF);
		/*
		 * System.out.println(", OT."); this.loadAbsCensusTableCsv2Columns1Wafer(
		 * "/data/ABS/CensusTableBuilder2016/HCFMF by LGA by FINF and CDCF/HCFMF by LGA by FINF and CDCF - OT.csv"
		 * , this.initialisedCensusHCFMF_LGA_FINF_CDCF, fromColumnHCFMF_LGA_FINF_CDCF,
		 * toColumnHCFMF_LGA_FINF_CDCF, this.censusHCFMF_LGA_FINF_CDCF);
		 */

		if (DEBUG) {
			System.gc();
			long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			double megabytesConsumed = (memoryAfter - memoryBefore) / 1024d / 1024d;
			DecimalFormat formatter = new DecimalFormat("#,##0.00");
			System.out.println(
					">>> Memory used by ABS Census CDCF by LGA by FINF: " + formatter.format(megabytesConsumed) + "MB");
			memoryBefore = memoryAfter;
		}

		// set flag so we only load the data once
		System.out.println(new Date(System.currentTimeMillis()) + ": Individual data loaded");
		this.dataLoaded = true;
	}

	/**
	 * Household and Business Balance Sheets – E1
	 * 
	 * REFERENCE: RBA (2018) 'Household and Business Balance Sheets – E1',
	 * Statistical Tables: Household and Business Finances: June 2018, Sydney, NSW:
	 * Reserve Bank of Australia.
	 * 
	 * Loads ABS pre-prepared catalogue data.
	 * 
	 * File pre-conditions:<br>
	 * 1. The column titles row has "Title" in the first column.<br>
	 * 2. The unit type row has "Units" in the first column.<br>
	 * 3. The last header row has "Series ID" in the first column.<br>
	 * 4. The first column contains the dates in the format MMM-yyyy.
	 * 
	 * Data sources this works for include: E1 Household & Business Balance Sheets
	 * E2 Household Finances - Selected Ratios
	 * 
	 * @param fileResourceLocation - the URI of the file to import
	 * @param dataSourceName       - the name used to store this series' data in the
	 *                             maps
	 * @param columnsToImport      - a zero-based array of integers specifying which
	 *                             columns to import (i.e. the first column is
	 *                             column 0). The first column is assumed to be the
	 *                             date and is imported only as the key for the
	 *                             other columns' data.
	 * @param titles               - column titles in CSV file
	 * @param units                - unit type (e.g. $Billions, Number, '000)
	 * @param data                 - the data map that the values are returned in
	 */
	private void loadRbaDataCsv(String fileResourceLocation, String dataSourceName, int[] columnsToImport,
			Map<String, List<String>> titles, Map<String, List<String>> units, Map<String, Map<Date, String>> data) {

		CSVReader reader = null;
		try {
			InputStream is = this.getClass().getResourceAsStream(fileResourceLocation);
			reader = new CSVReader(new InputStreamReader(is));
			boolean header = true;
			boolean footer = false;
			String[] seriesId = new String[columnsToImport.length];
			String[] line = null;
			DateFormat dateFormat = new SimpleDateFormat("MMM-yyyy", Locale.ENGLISH);
			while ((line = reader.readNext()) != null && !footer) {
				if (header) {
					if (line[0].equals("Title")) {
						// store title
						titles.put(dataSourceName, new ArrayList<String>(columnsToImport.length));
						for (int i = 0; i < columnsToImport.length; i++) {
							titles.get(dataSourceName).add(line[columnsToImport[i]]);
						}
					} else if (line[0].equals("Units")) {
						// store unit types
						units.put(dataSourceName, new ArrayList<String>(columnsToImport.length));
						for (int i = 0; i < columnsToImport.length; i++) {
							units.get(dataSourceName).add(line[columnsToImport[i]]);
						}
					} else if (line[0].equals("Series ID")) {
						// store series ID as key with blank collections to populate with data below
						for (int i = 0; i < columnsToImport.length; i++) {
							seriesId[i] = line[columnsToImport[i]];
							data.put(line[columnsToImport[i]], new HashMap<Date, String>());
						}
						header = false;
					}
				} else {
					if (line[0].isEmpty()) {
						footer = true;
					} else {
						for (int i = 0; i < columnsToImport.length; i++) {
							// parse the body of the data
							data.get(seriesId[i]).put(dateFormat.parse(line[0]), line[columnsToImport[i]]);
						}
					}
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
		} catch (ParseException e) {
			// parsing date from string
			e.printStackTrace();
		}
	}

	/**
	 * Loads ABS 1410.0 catalogue data.
	 * 
	 * The size of the key in a HashMap is largely irrelevant, so the tiele lengths
	 * are fine. SOURCE:
	 * https://stackoverflow.com/questions/16506593/at-which-length-is-a-string-key-of-a-hashmap-considered-bad-practice
	 * 
	 * @param fileResourceLocation - the URI of the file to import
	 * @param catalogueName        - the name used to store this series' data in the
	 *                             maps
	 * @param columnsToImport      - a zero-based array of integers specifying which
	 *                             columns to import (i.e. the first column is
	 *                             column 0). The first column is assumed to be the
	 *                             date and is imported only as the key for the
	 *                             other columns' data.
	 * @param yearsToImport        - a list of years to import data for (e.g.
	 *                             "2016")
	 * @param titles               - column titles in CSV file
	 * @param units                - unit type (e.g. $Billions, Number, '000)
	 * @param data                 - the data map that the values are returned in.
	 *                             Keys are: Year, Series ID, LGA.
	 */
	private void loadAbsDataCsv_1410_0(String fileResourceLocation, String catalogueName, int[] columnsToImport,
			String[] yearsToImport, Map<String, List<String>> titles, Map<String, List<String>> units,
			Map<String, Map<String, Map<String, Double>>> data) {

		CSVReader reader = null;
		try {
			InputStream is = this.getClass().getResourceAsStream(fileResourceLocation);
			reader = new CSVReader(new InputStreamReader(is));
			boolean header = true;
			int currentRow = 1;
			final int titleRow = 7;
			final int yearCol = 2;
			String[] seriesId = new String[columnsToImport.length];
			String[] line = null;
			while ((line = reader.readNext()) != null) {
				if (header) {
					if (currentRow == titleRow) {
						// store title
						titles.put(catalogueName, new ArrayList<String>(columnsToImport.length));
						for (int i = 0; i < columnsToImport.length; i++) {
							titles.get(catalogueName).add(line[columnsToImport[i]]);
							seriesId[i] = new String(line[columnsToImport[i]]);
						}

						// store series ID as key with blank collections to populate with data below
						for (int i = 0; i < yearsToImport.length; i++) {
							data.put(yearsToImport[i], new HashMap<String, Map<String, Double>>());
							for (int j = 0; j < columnsToImport.length; j++) {
								data.get(yearsToImport[i]).put(line[columnsToImport[j]], new HashMap<String, Double>());
							}
						}
					} else if (line[0].equals("CODE")) {
						// store unit types
						units.put(catalogueName, new ArrayList<String>(columnsToImport.length));
						for (int i = 0; i < columnsToImport.length; i++) {
							units.get(catalogueName).add(line[columnsToImport[i]]);
						}
						header = false;
					}
					currentRow++;
				} else {
					// check if we should import this year
					boolean importThisRow = false;
					for (int i = 0; i < yearsToImport.length; i++) {
						if (line[yearCol].equals(yearsToImport[i])) {
							importThisRow = true;
							break;
						}
					}
					if (importThisRow) {
						// for (int j = 1; j < columnsToImport.length; j++) {
						for (int j = 0; j < columnsToImport.length; j++) {
							// parse the body of the data
							double value = 0d;
							try {
								if (line[columnsToImport[j]].trim().replace(",", "").equals("-")) {
									value = 0d; // change "-" into 0d
								} else {
									value = Double.valueOf(line[columnsToImport[j]].trim().replace(",", ""));
								}
							} catch (NumberFormatException e) {
								value = 0d;
							}
							data.get(line[yearCol]).get(seriesId[j]).put(line[0], value);
						}
					}
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
	}

	/**
	 * Loads ABS Census Table Builder tables with one row series, two column series,
	 * and one wafer series.
	 * 
	 * File pre-conditions:<br>
	 * 1. Row 10 contains the first wafer title.<br>
	 * 2. The rows after each wafer title are the column titles.<br>
	 * 3. Row 4 column 1 contains the series title.<br>
	 * 4. Data starts on row 15.<br>
	 * 5. The first column contains the LGA names (not codes).<br>
	 * 6. The first row in the footer begins with "Data Source".
	 * 
	 * @param fileResourceLocation - the URI of the file to import.
	 * @param isInitialised        - true if any file has already been imported for
	 *                             this data series, false otherwise.
	 * @param fromColumnIndex      - import data from this column index (inclusive).
	 * @param toColumnIndex        - import data to this column index (exclusive).
	 * @param data                 - the data map that the values are returned in.
	 *                             Keys are: 2 columns, row, wafer. (HIND,
	 *                             RNTRD/MRERD, LGA, HCFMD)
	 */
	private void loadAbsCensusTableCsv2Columns1Wafer(String fileResourceLocation, boolean isInitialised,
			int fromColumnIndex, int toColumnIndex, Map<String, Map<String, Map<String, Map<String, Integer>>>> data) {

		CSVReader reader = null;
		try {
			InputStream is = this.getClass().getResourceAsStream(fileResourceLocation);
			reader = new CSVReader(new InputStreamReader(is));
			boolean header = true;
			boolean footer = false;
			int currentRow = 1;
			int lastHeaderRow = 9; // the row before the first wafer's title row
			boolean prevRowIsBlank = true; // there's a blank row before wafer names
			String waferName = null;
			int waferNumber = 0;
			int columnSeriesNumber = Integer.MAX_VALUE;
			final int columnSeriesMax = 2; // because the dataset contains 2 column series
			String[][] columnTitles = new String[columnSeriesMax][toColumnIndex - fromColumnIndex];

			String[] line = null;
			while ((line = reader.readNext()) != null) {
				if (header) {
					if (currentRow++ == lastHeaderRow) {
						header = false;
					}
				} else if (!footer) {
					if (line[0].length() > 11 && line[0].substring(0, 11).equals("Data Source")) {
						footer = true;
					} else {
						if (prevRowIsBlank && !line[0].isBlank()) {
							// set wafer name
							waferName = line[0].trim();
							columnSeriesNumber = 0;
							waferNumber++;
							prevRowIsBlank = false;
						} else {
							if (columnSeriesNumber < columnSeriesMax) {
								// set series ID
								String thisTitle = null;
								for (int i = 0; i < toColumnIndex - fromColumnIndex; i++) {
									thisTitle = line[i + fromColumnIndex].isEmpty() ? thisTitle
											: line[i + fromColumnIndex];
									columnTitles[columnSeriesNumber][i] = thisTitle;
								}
								columnSeriesNumber++;
							} else if (columnSeriesNumber == columnSeriesMax && !isInitialised) {
								// add blank maps to data, so they can be populated below
								if (waferNumber == 1) {
									for (int i = 0; i < toColumnIndex - fromColumnIndex; i++) {
										if (!columnTitles[0][i].isBlank() && !data.containsKey(columnTitles[0][i])) {
											// add column series 1 key
											data.put(columnTitles[0][i],
													new HashMap<String, Map<String, Map<String, Integer>>>(
															MAP_INIT_SIZE_RNTRD));
										}
										if (!columnTitles[1][i].isBlank()
												&& !data.get(columnTitles[0][i]).containsKey(columnTitles[1][i])) {
											// add column series 2 key
											data.get(columnTitles[0][i]).put(columnTitles[1][i],
													new HashMap<String, Map<String, Integer>>(MAP_INIT_SIZE_LGA));
										}
									}
								}
								columnSeriesNumber++; // make sure this is only executed once
							} else if (line.length > 1 && !line[1].isBlank()) {
								// parse the body of the data
								// WISHLIST: modify this so it can use POA, state, etc. not just LGA
								String lgaCode = this.area.getLgaCodeFromName(line[0]);
								if (lgaCode != null) {
									// null check excludes invalid LGAs
									for (int i = 0; i < toColumnIndex - fromColumnIndex; i++) {
										if (waferNumber == 1) {
											data.get(columnTitles[0][i]).get(columnTitles[1][i]).put(lgaCode,
													new HashMap<String, Integer>(MAP_INIT_SIZE_HCFMD));
										}
										int value = 0;
										try {
											value = Integer.valueOf(line[i + fromColumnIndex]);
										} catch (NumberFormatException e) {
											value = 0;
										}
										data.get(columnTitles[0][i]).get(columnTitles[1][i]).get(lgaCode).put(waferName,
												value);
									}
								}
							} else if (line[0].isBlank()) {
								prevRowIsBlank = true;
							}
						}
					}
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
	}

	/**
	 * @return the rbaE2
	 */
	public Map<String, Map<Date, String>> getRbaE2() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return rbaE2;
	}

	/**
	 * @return the abs1410_0Economy
	 */
	public Map<String, Map<String, Map<String, Double>>> getAbs1410_0Economy() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs1410_0Economy;
	}

	/**
	 * @return the abs1410_0Family
	 */
	public Map<String, Map<String, Map<String, Double>>> getAbs1410_0Family() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs1410_0Family;
	}

	/**
	 * @return the censusHCFMD_LGA_HIND_MRERD
	 */
	public Map<String, Map<String, Map<String, Map<String, Integer>>>> getCensusHCFMD_LGA_HIND_MRERD() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return censusHCFMD_LGA_HIND_MRERD;
	}

	/**
	 * @return the censusHCFMD_LGA_HIND_RNTRD
	 */
	public Map<String, Map<String, Map<String, Map<String, Integer>>>> getCensusHCFMD_LGA_HIND_RNTRD() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return censusHCFMD_LGA_HIND_RNTRD;
	}

	/**
	 * @return the censusCDCF_LGA_FINF
	 */
	public Map<String, Map<String, Map<String, Map<String, Integer>>>> getCensusHCFMF_LGA_FINF_CDCF() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return censusHCFMF_LGA_FINF_CDCF;
	}

	/**
	 * @param area the area to set
	 */
	@Autowired
	public void setArea(AreaMapping area) {
		this.area = area;
	}

	/**
	 * @param sharedData the sharedData to set
	 */
	@Autowired
	public void setSharedData(CalibrationData sharedData) {
		this.sharedData = sharedData;
	}
}
