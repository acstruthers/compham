/**
 * 
 */
package xyz.struthers.rhul.ham.data;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.lang3.math.NumberUtils;
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
 * @since 2019-02-24
 */
@Component
@Scope(value = "singleton")
public class CalibrationDataIndividual {

	// map implementation optimisation
	public static final double MAP_LOAD_FACTOR = 0.75d;

	public static final int MAP_INIT_SIZE_LGA = (int) Math.ceil(573 / MAP_LOAD_FACTOR); // 572 (UR) including state
																						// totals (563 Enum)
	public static final int MAP_INIT_SIZE_POA = (int) Math.ceil(2600 / MAP_LOAD_FACTOR); // 2567 plus room for totals
	public static final int MAP_INIT_SIZE_AGE5P = (int) Math.ceil(22 / MAP_LOAD_FACTOR); // 21 plus totals
	public static final int MAP_INIT_SIZE_CDCF = (int) Math.ceil(15 / MAP_LOAD_FACTOR); // 15 (no totals in data)
	public static final int MAP_INIT_SIZE_FINF = (int) Math.ceil(25 / MAP_LOAD_FACTOR); // 25 plus totals
	public static final int MAP_INIT_SIZE_HCFMD = (int) Math.ceil(18 / MAP_LOAD_FACTOR); // 17 plus totals
	public static final int MAP_INIT_SIZE_HIND = (int) Math.ceil(26 / MAP_LOAD_FACTOR); // 25 plus totals
	public static final int MAP_INIT_SIZE_INCP = (int) Math.ceil(18 / MAP_LOAD_FACTOR); // 17 plus totals
	public static final int MAP_INIT_SIZE_INDP = (int) Math.ceil(23 / MAP_LOAD_FACTOR); // 22 plus totals
	public static final int MAP_INIT_SIZE_MRERD = (int) Math.ceil(22 / MAP_LOAD_FACTOR); // 21 plus totals
	public static final int MAP_INIT_SIZE_RNTRD = (int) Math.ceil(26 / MAP_LOAD_FACTOR); // 25 plus totals
	public static final int MAP_INIT_SIZE_TEND = (int) Math.ceil(10 / MAP_LOAD_FACTOR); // 9 plus totals
	public static final int MAP_INIT_SIZE_SEXP = (int) Math.ceil(2 / MAP_LOAD_FACTOR); // 2 (no totals in data)

	// series names
	public static final String RBA_E2 = "RBA_E2"; // household Balance Sheet ratios

	public static final String ABS1410_0_ECONOMY = "ABS_1410.0_Economy";
	static final String ABS1410_0_FAMILY = "ABS_1410.0_Family";
	static final String ABS1410_0_INCOME = "ABS_1410.0_Income";

	public static final String ATO_INDIVIDUAL_T2A = "ATO_IndividualTable2A";
	public static final String ATO_INDIVIDUAL_T3A = "ATO_IndividualTable3A";
	public static final String ATO_INDIVIDUAL_T6B = "ATO_IndividualTable6B";
	static final String ATO_INDIVIDUAL_T6C = "ATO_IndividualTable6C";
	public static final String ATO_INDIVIDUAL_T9 = "ATO_IndividualTable9";

	public static final String CENSUS_SEXP_POA_AGE5P_INDP_INCP = "Census SEXP by POA (UR) by AGE5P, INDP and INCP";
	public static final String CENSUS_HCFMD_LGA_HIND_RNTRD = "Census HCFMD by LGA by HIND and RNTRD";
	public static final String CENSUS_HCFMD_LGA_HIND_MRERD = "Census HCFMD by LGA by HIND and MRERD";
	public static final String CENSUS_CDCF_LGA_FINF = "Census CDCF by LGA by FINF";

	static final String CENSUS_SEXP_LGA_AGE5P_INDP_INCP = "Census SEXP by LGA (UR) by AGE5P, INDP and INCP";
	static final String CENSUS_HCFMD_TEND_LGA_HIND_RNTRD = "Census HCFMD and TEND by LGA by HIND and RNTRD";
	static final String CENSUS_HCFMD_TEND_LGA_HIND_MRERD = "Census HCFMD and TEND by LGA by HIND and MRERD";

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
	private Map<String, Map<String, Map<String, String>>> abs1410_0Economy; // Data by LGA: Economy (keys: year, LGA,
																			// series)
	Map<String, Map<String, Map<String, String>>> abs1410_0Family; // Data by LGA: Family (keys: year, LGA,
																	// series)
	Map<String, Map<String, Map<String, String>>> abs1410_0Income; // Data by LGA: Income
	/**
	 * ATO Individuals Table 2A<br>
	 * Contains P&L and people count by sex, 5-year age range, and state.<br>
	 * Keys: Series Title, State, Age, Gender, Taxable Status, Lodgment Method
	 */
	private Map<String, Map<String, Map<String, Map<String, Map<String, Map<String, String>>>>>> atoIndividualTable2a;
	/**
	 * ATO Individuals Table 3A<br>
	 * Contains P&L and people count by sex, 5-year age range, and income range.<br>
	 * Keys: Series Title, Income Range, Age, Gender, Taxable Status
	 */
	private Map<String, Map<String, Map<String, Map<String, Map<String, String>>>>> atoIndividualTable3a;

	/**
	 * ATO Individuals Table 6B<br>
	 * Contains P&L and people count by post code.<br>
	 * Keys: Series Title, Post Code
	 */
	private Map<String, Map<String, String>> atoIndividualTable6b;
	/**
	 * ATO Individuals Table 6C<br>
	 * Contains income ranges people count by post code.<br>
	 * Keys: Series Title, Post Code
	 */
	Map<String, Map<String, String>> atoIndividualTable6c;
	/**
	 * ATO Individuals Table 9<br>
	 * Contains P&L by industry code.<br>
	 * Keys: Series Title, Industry Code
	 */
	private Map<String, Map<String, String>> atoIndividualTable9;
	/**
	 * ATO Individuals Table 9 (Industry Division summary)<br>
	 * Contains count and taxable income, summarised by industry division.<br>
	 * Keys: Series Title, Industry Division Code
	 */
	private Map<String, Map<String, Double>> atoIndividualTable9DivisionSummary;
	/**
	 * ABS Census Table Builder data:<br>
	 * SEXP by POA (UR) by AGE5P, INDP and INCP<br>
	 * Individual income by industry and demographic.
	 * 
	 * Keys: Age5, Industry Division, Personal Income, POA, Sex<br>
	 * Values: Number of persons
	 */
	private Map<String, Map<String, Map<String, Map<String, Map<String, String>>>>> censusSEXP_POA_AGE5P_INDP_INCP;
	/**
	 * ABS Census Table Builder data:<br>
	 * SEXP by LGA (UR) by AGE5P, INDP and INCP<br>
	 * Individual income by industry and demographic.
	 * 
	 * Keys: Age5, Industry Division, Personal Income, LGA, Sex<br>
	 * Values: Number of persons
	 */
	Map<String, Map<String, Map<String, Map<String, Map<String, String>>>>> censusSEXP_LGA_AGE5P_INDP_INCP;
	/**
	 * ABS Census Table Builder data:<br>
	 * HCFMD and TEND by LGA by HIND and RNTRD<br>
	 * Rent by household income and composition.
	 * 
	 * Keys: Household Income, Rent Range, LGA, Household Composition Dwelling,
	 * Tenure<br>
	 * Values: Number of dwellings
	 */
	private Map<String, Map<String, Map<String, Map<String, String>>>> censusHCFMD_LGA_HIND_RNTRD;
	/**
	 * ABS Census Table Builder data:<br>
	 * HCFMD by LGA by HIND and MRERD<br>
	 * Mortgage payments by household income and composition.
	 * 
	 * Keys: Household Income, Rent Range, LGA, Household Composition Dwelling,
	 * Tenure<br>
	 * Values: Number of dwellings
	 */
	private Map<String, Map<String, Map<String, Map<String, String>>>> censusHCFMD_LGA_HIND_MRERD;
	/**
	 * ABS Census Table Builder data:<br>
	 * CDCF by LGA by FINF<br>
	 * Family income by family composition.
	 * 
	 * Keys: Family Income, LGA, Family Composition<br>
	 * Values: Number of families
	 */
	private Map<String, Map<String, Map<String, String>>> censusCDCF_LGA_FINF;
	/**
	 * ABS Census Table Builder data:<br>
	 * HCFMD and TEND by LGA by HIND and RNTRD<br>
	 * Rent by tenure, household income and composition.
	 * 
	 * Keys: Household Income, Rent Range, LGA, Household Composition Dwelling,
	 * Tenure<br>
	 * Values: Number of dwellings
	 */
	Map<String, Map<String, Map<String, Map<String, Map<String, String>>>>> censusHCFMD_TEND_LGA_HIND_RNTRD;
	/**
	 * ABS Census Table Builder data:<br>
	 * HCFMD and TEND by LGA by HIND and MRERD<br>
	 * Mortgage payments by tenure, household income and composition.
	 * 
	 * Keys: Household Income, Rent Range, LGA, Household Composition Dwelling,
	 * Tenure<br>
	 * Values: Number of dwellings
	 */
	Map<String, Map<String, Map<String, Map<String, Map<String, String>>>>> censusHCFMD_TEND_LGA_HIND_MRERD;

	private boolean initialisedCensusSEXP_POA_AGE5P_INDP_INCP;
	private boolean initialisedCensusHCFMD_LGA_HIND_RNTRD;
	private boolean initialisedCensusHCFMD_LGA_HIND_MRERD;
	private boolean initialisedCensusCDCF_LGA_FINF;

	boolean initialisedCensusSEXP_LGA_AGE5P_INDP_INCP;
	boolean initialisedCensusHCFMD_TEND_LGA_HIND_RNTRD;
	boolean initialisedCensusHCFMD_TEND_LGA_HIND_MRERD;

	/**
	 * 
	 */
	public CalibrationDataIndividual() {
		super();
		this.init();
	}

	public int getAdjustedPeopleByLga(String lgaCode) {
		return this.getAdjustedPeopleByLga(lgaCode);
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

		// load RBA data
		System.out.println(new Date(System.currentTimeMillis()) + ": Loading RBA E2 data");
		int[] rbaE2Columns = { 3, 6, 8, 9, 10 };
		int rbaE2MapCapacity = (int) Math.ceil(rbaE2Columns.length / MAP_LOAD_FACTOR);
		this.rbaE2 = new HashMap<String, Map<Date, String>>(rbaE2MapCapacity);
		this.loadRbaDataCsv("/data/RBA/E_HouseholdBusiness/e2-data.csv", RBA_E2, rbaE2Columns, this.title,
				this.unitType, this.rbaE2);

		// load ABS 1410.0 data
		System.out.println(new Date(System.currentTimeMillis()) + ": Loading ABS 1410.0 Economy data");
		this.abs1410_0Economy = new HashMap<String, Map<String, Map<String, String>>>(7); // 7 years in the data file
		int[] abs1410_0EconomyColumns = { 49, 50, 51, 52, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99,
				100, 101, 102, 103, 104 };
		String[] abs1410_0EconomyYears = { "2016" };
		this.loadAbsDataCsv_1410_0("/data/ABS/1410.0_DataByRegion/Economy and Industry, LGA, 2011 to 2017.csv",
				ABS1410_0_ECONOMY, abs1410_0EconomyColumns, abs1410_0EconomyYears, this.title, this.unitType,
				this.abs1410_0Economy);

		System.out.println(new Date(System.currentTimeMillis()) + ": Loading ABS 1410.0 Family data");
		this.abs1410_0Family = new HashMap<String, Map<String, Map<String, String>>>(7); // 7 years in the data file
		int[] abs1410_0FamilyColumns = { 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 41, 42, 49, 57, 58, 59, 60,
				61, 62, 63 };
		String[] abs1410_0FamilyYears = { "2016" };
		this.loadAbsDataCsv_1410_0("/data/ABS/1410.0_DataByRegion/Family and Community, LGA, 2011 to 2017.csv",
				ABS1410_0_FAMILY, abs1410_0FamilyColumns, abs1410_0FamilyYears, this.title, this.unitType,
				this.abs1410_0Family);

		System.out.println(new Date(System.currentTimeMillis()) + ": Loading ABS 1410.0 Income data");
		this.abs1410_0Income = new HashMap<String, Map<String, Map<String, String>>>(7); // 7 years in the data file
		int[] abs1410_0IncomeColumns = { 15, 19, 20, 21, 25, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60 };
		String[] abs1410_0IncomeYears = { "2016" };
		this.loadAbsDataCsv_1410_0(
				"/data/ABS/1410.0_DataByRegion/Income (including Government Allowances), LGA, 2011 to 2017.csv",
				ABS1410_0_INCOME, abs1410_0IncomeColumns, abs1410_0IncomeYears, this.title, this.unitType,
				this.abs1410_0Income);

		// ABS Census SEXP by POA (UR) by AGE5P, INDP and INCP
		System.out.print(new Date(System.currentTimeMillis())
				+ ": Loading ABS Census SEXP by POA (UR) by AGE5P, INDP and INCP data");
		this.censusSEXP_POA_AGE5P_INDP_INCP = new HashMap<String, Map<String, Map<String, Map<String, Map<String, String>>>>>(
				MAP_INIT_SIZE_AGE5P);
		int fromColumnSEXP_POA_AGE5P_INDP_INCP = 1;
		int toColumnSEXP_POA_AGE5P_INDP_INCP = 8229;
		System.out.print(": NSW");
		this.loadAbsCensusTableCsv3Columns1Wafer(
				"/data/ABS/CensusTableBuilder2016/SEXP by POA (UR) by AGE5P, INDP and INCP/SEXP by POA (UR) by AGE5P, INDP and INCP - NSW.csv",
				this.initialisedCensusSEXP_POA_AGE5P_INDP_INCP, fromColumnSEXP_POA_AGE5P_INDP_INCP,
				toColumnSEXP_POA_AGE5P_INDP_INCP, this.censusSEXP_POA_AGE5P_INDP_INCP, "POA");
		this.initialisedCensusSEXP_POA_AGE5P_INDP_INCP = true;
		System.out.print(", VIC");
		this.loadAbsCensusTableCsv3Columns1Wafer(
				"/data/ABS/CensusTableBuilder2016/SEXP by POA (UR) by AGE5P, INDP and INCP/SEXP by POA (UR) by AGE5P, INDP and INCP - VIC.csv",
				this.initialisedCensusSEXP_POA_AGE5P_INDP_INCP, fromColumnSEXP_POA_AGE5P_INDP_INCP,
				toColumnSEXP_POA_AGE5P_INDP_INCP, this.censusSEXP_POA_AGE5P_INDP_INCP, "POA");
		System.out.print(", QLD");
		this.loadAbsCensusTableCsv3Columns1Wafer(
				"/data/ABS/CensusTableBuilder2016/SEXP by POA (UR) by AGE5P, INDP and INCP/SEXP by POA (UR) by AGE5P, INDP and INCP - QLD.csv",
				this.initialisedCensusSEXP_POA_AGE5P_INDP_INCP, fromColumnSEXP_POA_AGE5P_INDP_INCP,
				toColumnSEXP_POA_AGE5P_INDP_INCP, this.censusSEXP_POA_AGE5P_INDP_INCP, "POA");
		System.out.print(", SA");
		this.loadAbsCensusTableCsv3Columns1Wafer(
				"/data/ABS/CensusTableBuilder2016/SEXP by POA (UR) by AGE5P, INDP and INCP/SEXP by POA (UR) by AGE5P, INDP and INCP - SA.csv",
				this.initialisedCensusSEXP_POA_AGE5P_INDP_INCP, fromColumnSEXP_POA_AGE5P_INDP_INCP,
				toColumnSEXP_POA_AGE5P_INDP_INCP, this.censusSEXP_POA_AGE5P_INDP_INCP, "POA");
		System.out.print(", WA");
		this.loadAbsCensusTableCsv3Columns1Wafer(
				"/data/ABS/CensusTableBuilder2016/SEXP by POA (UR) by AGE5P, INDP and INCP/SEXP by POA (UR) by AGE5P, INDP and INCP - WA.csv",
				this.initialisedCensusSEXP_POA_AGE5P_INDP_INCP, fromColumnSEXP_POA_AGE5P_INDP_INCP,
				toColumnSEXP_POA_AGE5P_INDP_INCP, this.censusSEXP_POA_AGE5P_INDP_INCP, "POA");
		System.out.print(", TAS");
		this.loadAbsCensusTableCsv3Columns1Wafer(
				"/data/ABS/CensusTableBuilder2016/SEXP by POA (UR) by AGE5P, INDP and INCP/SEXP by POA (UR) by AGE5P, INDP and INCP - TAS.csv",
				this.initialisedCensusSEXP_POA_AGE5P_INDP_INCP, fromColumnSEXP_POA_AGE5P_INDP_INCP,
				toColumnSEXP_POA_AGE5P_INDP_INCP, this.censusSEXP_POA_AGE5P_INDP_INCP, "POA");
		System.out.print(", NT");
		this.loadAbsCensusTableCsv3Columns1Wafer(
				"/data/ABS/CensusTableBuilder2016/SEXP by POA (UR) by AGE5P, INDP and INCP/SEXP by POA (UR) by AGE5P, INDP and INCP - NT.csv",
				this.initialisedCensusSEXP_POA_AGE5P_INDP_INCP, fromColumnSEXP_POA_AGE5P_INDP_INCP,
				toColumnSEXP_POA_AGE5P_INDP_INCP, this.censusSEXP_POA_AGE5P_INDP_INCP, "POA");
		System.out.print(", ACT");
		this.loadAbsCensusTableCsv3Columns1Wafer(
				"/data/ABS/CensusTableBuilder2016/SEXP by POA (UR) by AGE5P, INDP and INCP/SEXP by POA (UR) by AGE5P, INDP and INCP - ACT.csv",
				this.initialisedCensusSEXP_POA_AGE5P_INDP_INCP, fromColumnSEXP_POA_AGE5P_INDP_INCP,
				toColumnSEXP_POA_AGE5P_INDP_INCP, this.censusSEXP_POA_AGE5P_INDP_INCP, "POA");
		System.out.println(", OT.");
		this.loadAbsCensusTableCsv3Columns1Wafer(
				"/data/ABS/CensusTableBuilder2016/SEXP by POA (UR) by AGE5P, INDP and INCP/SEXP by POA (UR) by AGE5P, INDP and INCP - OT.csv",
				this.initialisedCensusSEXP_POA_AGE5P_INDP_INCP, fromColumnSEXP_POA_AGE5P_INDP_INCP,
				toColumnSEXP_POA_AGE5P_INDP_INCP, this.censusSEXP_POA_AGE5P_INDP_INCP, "POA");

		/*
		 * // ABS Census SEXP by LGA (UR) by AGE5P, INDP and INCP System.out.print(new
		 * Date(System.currentTimeMillis()) +
		 * ": Loading ABS Census SEXP by LGA (UR) by AGE5P, INDP and INCP data");
		 * this.censusSEXP_LGA_AGE5P_INDP_INCP = new HashMap<String, Map<String,
		 * Map<String, Map<String, Map<String, String>>>>>( MAP_INIT_SIZE_AGE5P); int
		 * fromColumnSEXP_LGA_AGE5P_INDP_INCP = 1; int toColumnSEXP_LGA_AGE5P_INDP_INCP
		 * = 8229; System.out.print(": NSW"); this.loadAbsCensusTableCsv3Columns1Wafer(
		 * "/data/ABS/CensusTableBuilder2016/SEXP by LGA (UR) by AGE5P, INDP and INCP/SEXP by LGA (UR) by AGE5P, INDP and INCP - NSW.csv"
		 * , this.initialisedCensusSEXP_LGA_AGE5P_INDP_INCP,
		 * fromColumnSEXP_LGA_AGE5P_INDP_INCP, toColumnSEXP_LGA_AGE5P_INDP_INCP,
		 * this.censusSEXP_LGA_AGE5P_INDP_INCP, "LGA");
		 * this.initialisedCensusSEXP_LGA_AGE5P_INDP_INCP = true;
		 * System.out.print(", VIC"); this.loadAbsCensusTableCsv3Columns1Wafer(
		 * "/data/ABS/CensusTableBuilder2016/SEXP by LGA (UR) by AGE5P, INDP and INCP/SEXP by LGA (UR) by AGE5P, INDP and INCP - VIC.csv"
		 * , this.initialisedCensusSEXP_LGA_AGE5P_INDP_INCP,
		 * fromColumnSEXP_LGA_AGE5P_INDP_INCP, toColumnSEXP_LGA_AGE5P_INDP_INCP,
		 * this.censusSEXP_LGA_AGE5P_INDP_INCP, "LGA"); System.out.print(", QLD");
		 * this.loadAbsCensusTableCsv3Columns1Wafer(
		 * "/data/ABS/CensusTableBuilder2016/SEXP by LGA (UR) by AGE5P, INDP and INCP/SEXP by LGA (UR) by AGE5P, INDP and INCP - QLD.csv"
		 * , this.initialisedCensusSEXP_LGA_AGE5P_INDP_INCP,
		 * fromColumnSEXP_LGA_AGE5P_INDP_INCP, toColumnSEXP_LGA_AGE5P_INDP_INCP,
		 * this.censusSEXP_LGA_AGE5P_INDP_INCP, "LGA"); System.out.print(", SA");
		 * this.loadAbsCensusTableCsv3Columns1Wafer(
		 * "/data/ABS/CensusTableBuilder2016/SEXP by LGA (UR) by AGE5P, INDP and INCP/SEXP by LGA (UR) by AGE5P, INDP and INCP - SA.csv"
		 * , this.initialisedCensusSEXP_LGA_AGE5P_INDP_INCP,
		 * fromColumnSEXP_LGA_AGE5P_INDP_INCP, toColumnSEXP_LGA_AGE5P_INDP_INCP,
		 * this.censusSEXP_LGA_AGE5P_INDP_INCP, "LGA"); System.out.print(", WA");
		 * this.loadAbsCensusTableCsv3Columns1Wafer(
		 * "/data/ABS/CensusTableBuilder2016/SEXP by LGA (UR) by AGE5P, INDP and INCP/SEXP by LGA (UR) by AGE5P, INDP and INCP - WA.csv"
		 * , this.initialisedCensusSEXP_LGA_AGE5P_INDP_INCP,
		 * fromColumnSEXP_LGA_AGE5P_INDP_INCP, toColumnSEXP_LGA_AGE5P_INDP_INCP,
		 * this.censusSEXP_LGA_AGE5P_INDP_INCP, "LGA"); System.out.print(", TAS");
		 * this.loadAbsCensusTableCsv3Columns1Wafer(
		 * "/data/ABS/CensusTableBuilder2016/SEXP by LGA (UR) by AGE5P, INDP and INCP/SEXP by LGA (UR) by AGE5P, INDP and INCP - TAS.csv"
		 * , this.initialisedCensusSEXP_LGA_AGE5P_INDP_INCP,
		 * fromColumnSEXP_LGA_AGE5P_INDP_INCP, toColumnSEXP_LGA_AGE5P_INDP_INCP,
		 * this.censusSEXP_LGA_AGE5P_INDP_INCP, "LGA"); System.out.print(", NT");
		 * this.loadAbsCensusTableCsv3Columns1Wafer(
		 * "/data/ABS/CensusTableBuilder2016/SEXP by LGA (UR) by AGE5P, INDP and INCP/SEXP by LGA (UR) by AGE5P, INDP and INCP - NT.csv"
		 * , this.initialisedCensusSEXP_LGA_AGE5P_INDP_INCP,
		 * fromColumnSEXP_LGA_AGE5P_INDP_INCP, toColumnSEXP_LGA_AGE5P_INDP_INCP,
		 * this.censusSEXP_LGA_AGE5P_INDP_INCP, "LGA"); System.out.print(", ACT");
		 * this.loadAbsCensusTableCsv3Columns1Wafer(
		 * "/data/ABS/CensusTableBuilder2016/SEXP by LGA (UR) by AGE5P, INDP and INCP/SEXP by LGA (UR) by AGE5P, INDP and INCP - ACT.csv"
		 * , this.initialisedCensusSEXP_LGA_AGE5P_INDP_INCP,
		 * fromColumnSEXP_LGA_AGE5P_INDP_INCP, toColumnSEXP_LGA_AGE5P_INDP_INCP,
		 * this.censusSEXP_LGA_AGE5P_INDP_INCP, "LGA"); System.out.println(", OT.");
		 * this.loadAbsCensusTableCsv3Columns1Wafer(
		 * "/data/ABS/CensusTableBuilder2016/SEXP by LGA (UR) by AGE5P, INDP and INCP/SEXP by LGA (UR) by AGE5P, INDP and INCP - OT.csv"
		 * , this.initialisedCensusSEXP_LGA_AGE5P_INDP_INCP,
		 * fromColumnSEXP_LGA_AGE5P_INDP_INCP, toColumnSEXP_LGA_AGE5P_INDP_INCP,
		 * this.censusSEXP_LGA_AGE5P_INDP_INCP, "LGA");
		 * 
		 */

		// ABS Census HCFMD by LGA by HIND and RNTRD
		System.out.print(
				new Date(System.currentTimeMillis()) + ": Loading ABS Census HCFMD by LGA by HIND and RNTRD data");
		this.censusHCFMD_LGA_HIND_RNTRD = new HashMap<String, Map<String, Map<String, Map<String, String>>>>(
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
		System.out.println(", OT.");
		this.loadAbsCensusTableCsv2Columns1Wafer(
				"/data/ABS/CensusTableBuilder2016/HCFMD by LGA by HIND and RNTRD/HCFMD by LGA by HIND and RNTRD - OT.csv",
				this.initialisedCensusHCFMD_LGA_HIND_RNTRD, fromColumnHCFMD_LGA_HIND_RNTRD,
				toColumnHCFMD_LGA_HIND_RNTRD, this.censusHCFMD_LGA_HIND_RNTRD);

		// ABS Census HCFMD and TEND by LGA by HIND and MRERD
		System.out.print(
				new Date(System.currentTimeMillis()) + ": Loading ABS Census HCFMD by LGA by HIND and MRERD data");
		this.censusHCFMD_LGA_HIND_MRERD = new HashMap<String, Map<String, Map<String, Map<String, String>>>>(
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
		System.out.println(", OT.");
		this.loadAbsCensusTableCsv2Columns1Wafer(
				"/data/ABS/CensusTableBuilder2016/HCFMD by LGA by HIND and MRERD/HCFMD by LGA by HIND and MRERD - OT.csv",
				this.initialisedCensusHCFMD_LGA_HIND_MRERD, fromColumnHCFMD_LGA_HIND_MRERD,
				toColumnHCFMD_LGA_HIND_MRERD, this.censusHCFMD_LGA_HIND_MRERD);

		/*
		 * // ABS Census HCFMD and TEND by LGA by HIND and RNTRD System.out.print(new
		 * Date(System.currentTimeMillis()) +
		 * ": Loading ABS Census HCFMD and TEND by LGA by HIND and RNTRD data");
		 * this.censusHCFMD_TEND_LGA_HIND_RNTRD = new HashMap<String, Map<String,
		 * Map<String, Map<String, Map<String, String>>>>>( MAP_INIT_SIZE_HIND); int
		 * fromColumnHCFMD_TEND_LGA_HIND_RNTRD = 1; int
		 * toColumnHCFMD_TEND_LGA_HIND_RNTRD = 651; System.out.print(": NSW");
		 * this.loadAbsCensusTableCsv2Columns2Wafers(
		 * "/data/ABS/CensusTableBuilder2016/HCFMD and TEND by LGA by HIND and RNTRD/HCFMD and TEND by LGA by HIND and RNTRD - NSW.csv"
		 * , this.initialisedCensusHCFMD_TEND_LGA_HIND_RNTRD,
		 * fromColumnHCFMD_TEND_LGA_HIND_RNTRD, toColumnHCFMD_TEND_LGA_HIND_RNTRD,
		 * this.censusHCFMD_TEND_LGA_HIND_RNTRD);
		 * this.initialisedCensusHCFMD_TEND_LGA_HIND_RNTRD = true;
		 * System.out.print(", VIC"); this.loadAbsCensusTableCsv2Columns2Wafers(
		 * "/data/ABS/CensusTableBuilder2016/HCFMD and TEND by LGA by HIND and RNTRD/HCFMD and TEND by LGA by HIND and RNTRD - VIC.csv"
		 * , this.initialisedCensusHCFMD_TEND_LGA_HIND_RNTRD,
		 * fromColumnHCFMD_TEND_LGA_HIND_RNTRD, toColumnHCFMD_TEND_LGA_HIND_RNTRD,
		 * this.censusHCFMD_TEND_LGA_HIND_RNTRD); System.out.print(", QLD");
		 * this.loadAbsCensusTableCsv2Columns2Wafers(
		 * "/data/ABS/CensusTableBuilder2016/HCFMD and TEND by LGA by HIND and RNTRD/HCFMD and TEND by LGA by HIND and RNTRD - QLD.csv"
		 * , this.initialisedCensusHCFMD_TEND_LGA_HIND_RNTRD,
		 * fromColumnHCFMD_TEND_LGA_HIND_RNTRD, toColumnHCFMD_TEND_LGA_HIND_RNTRD,
		 * this.censusHCFMD_TEND_LGA_HIND_RNTRD); System.out.print(", SA");
		 * this.loadAbsCensusTableCsv2Columns2Wafers(
		 * "/data/ABS/CensusTableBuilder2016/HCFMD and TEND by LGA by HIND and RNTRD/HCFMD and TEND by LGA by HIND and RNTRD - SA.csv"
		 * , this.initialisedCensusHCFMD_TEND_LGA_HIND_RNTRD,
		 * fromColumnHCFMD_TEND_LGA_HIND_RNTRD, toColumnHCFMD_TEND_LGA_HIND_RNTRD,
		 * this.censusHCFMD_TEND_LGA_HIND_RNTRD); System.out.print(", WA");
		 * this.loadAbsCensusTableCsv2Columns2Wafers(
		 * "/data/ABS/CensusTableBuilder2016/HCFMD and TEND by LGA by HIND and RNTRD/HCFMD and TEND by LGA by HIND and RNTRD - WA.csv"
		 * , this.initialisedCensusHCFMD_TEND_LGA_HIND_RNTRD,
		 * fromColumnHCFMD_TEND_LGA_HIND_RNTRD, toColumnHCFMD_TEND_LGA_HIND_RNTRD,
		 * this.censusHCFMD_TEND_LGA_HIND_RNTRD); System.out.print(", TAS");
		 * this.loadAbsCensusTableCsv2Columns2Wafers(
		 * "/data/ABS/CensusTableBuilder2016/HCFMD and TEND by LGA by HIND and RNTRD/HCFMD and TEND by LGA by HIND and RNTRD - TAS.csv"
		 * , this.initialisedCensusHCFMD_TEND_LGA_HIND_RNTRD,
		 * fromColumnHCFMD_TEND_LGA_HIND_RNTRD, toColumnHCFMD_TEND_LGA_HIND_RNTRD,
		 * this.censusHCFMD_TEND_LGA_HIND_RNTRD); System.out.print(", NT");
		 * this.loadAbsCensusTableCsv2Columns2Wafers(
		 * "/data/ABS/CensusTableBuilder2016/HCFMD and TEND by LGA by HIND and RNTRD/HCFMD and TEND by LGA by HIND and RNTRD - NT.csv"
		 * , this.initialisedCensusHCFMD_TEND_LGA_HIND_RNTRD,
		 * fromColumnHCFMD_TEND_LGA_HIND_RNTRD, toColumnHCFMD_TEND_LGA_HIND_RNTRD,
		 * this.censusHCFMD_TEND_LGA_HIND_RNTRD); System.out.print(", ACT");
		 * this.loadAbsCensusTableCsv2Columns2Wafers(
		 * "/data/ABS/CensusTableBuilder2016/HCFMD and TEND by LGA by HIND and RNTRD/HCFMD and TEND by LGA by HIND and RNTRD - ACT.csv"
		 * , this.initialisedCensusHCFMD_TEND_LGA_HIND_RNTRD,
		 * fromColumnHCFMD_TEND_LGA_HIND_RNTRD, toColumnHCFMD_TEND_LGA_HIND_RNTRD,
		 * this.censusHCFMD_TEND_LGA_HIND_RNTRD); System.out.println(", OT.");
		 * this.loadAbsCensusTableCsv2Columns2Wafers(
		 * "/data/ABS/CensusTableBuilder2016/HCFMD and TEND by LGA by HIND and RNTRD/HCFMD and TEND by LGA by HIND and RNTRD - OT.csv"
		 * , this.initialisedCensusHCFMD_TEND_LGA_HIND_RNTRD,
		 * fromColumnHCFMD_TEND_LGA_HIND_RNTRD, toColumnHCFMD_TEND_LGA_HIND_RNTRD,
		 * this.censusHCFMD_TEND_LGA_HIND_RNTRD);
		 * 
		 * 
		 * // ABS Census HCFMD and TEND by LGA by HIND and MRERD System.out.print(new
		 * Date(System.currentTimeMillis()) +
		 * ": Loading ABS Census HCFMD and TEND by LGA by HIND and MRERD data");
		 * this.censusHCFMD_TEND_LGA_HIND_MRERD = new HashMap<String, Map<String,
		 * Map<String, Map<String, Map<String, String>>>>>( MAP_INIT_SIZE_HIND); int
		 * fromColumnHCFMD_TEND_LGA_HIND_MRERD = 1; int
		 * toColumnHCFMD_TEND_LGA_HIND_MRERD = 547; System.out.print(": NSW");
		 * this.loadAbsCensusTableCsv2Columns2Wafers(
		 * "/data/ABS/CensusTableBuilder2016/HCFMD and TEND by LGA by HIND and MRERD/HCFMD and TEND by LGA by HIND and MRERD - NSW.csv"
		 * , this.initialisedCensusHCFMD_TEND_LGA_HIND_MRERD,
		 * fromColumnHCFMD_TEND_LGA_HIND_MRERD, toColumnHCFMD_TEND_LGA_HIND_MRERD,
		 * this.censusHCFMD_TEND_LGA_HIND_MRERD);
		 * this.initialisedCensusHCFMD_TEND_LGA_HIND_MRERD = true;
		 * System.out.print(", VIC"); this.loadAbsCensusTableCsv2Columns2Wafers(
		 * "/data/ABS/CensusTableBuilder2016/HCFMD and TEND by LGA by HIND and MRERD/HCFMD and TEND by LGA by HIND and MRERD - VIC.csv"
		 * , this.initialisedCensusHCFMD_TEND_LGA_HIND_MRERD,
		 * fromColumnHCFMD_TEND_LGA_HIND_MRERD, toColumnHCFMD_TEND_LGA_HIND_MRERD,
		 * this.censusHCFMD_TEND_LGA_HIND_MRERD); System.out.print(", QLD");
		 * this.loadAbsCensusTableCsv2Columns2Wafers(
		 * "/data/ABS/CensusTableBuilder2016/HCFMD and TEND by LGA by HIND and MRERD/HCFMD and TEND by LGA by HIND and MRERD - QLD.csv"
		 * , this.initialisedCensusHCFMD_TEND_LGA_HIND_MRERD,
		 * fromColumnHCFMD_TEND_LGA_HIND_MRERD, toColumnHCFMD_TEND_LGA_HIND_MRERD,
		 * this.censusHCFMD_TEND_LGA_HIND_MRERD); System.out.print(", SA");
		 * this.loadAbsCensusTableCsv2Columns2Wafers(
		 * "/data/ABS/CensusTableBuilder2016/HCFMD and TEND by LGA by HIND and MRERD/HCFMD and TEND by LGA by HIND and MRERD - SA.csv"
		 * , this.initialisedCensusHCFMD_TEND_LGA_HIND_MRERD,
		 * fromColumnHCFMD_TEND_LGA_HIND_MRERD, toColumnHCFMD_TEND_LGA_HIND_MRERD,
		 * this.censusHCFMD_TEND_LGA_HIND_MRERD); System.out.print(", WA");
		 * this.loadAbsCensusTableCsv2Columns2Wafers(
		 * "/data/ABS/CensusTableBuilder2016/HCFMD and TEND by LGA by HIND and MRERD/HCFMD and TEND by LGA by HIND and MRERD - WA.csv"
		 * , this.initialisedCensusHCFMD_TEND_LGA_HIND_MRERD,
		 * fromColumnHCFMD_TEND_LGA_HIND_MRERD, toColumnHCFMD_TEND_LGA_HIND_MRERD,
		 * this.censusHCFMD_TEND_LGA_HIND_MRERD); System.out.print(", TAS");
		 * this.loadAbsCensusTableCsv2Columns2Wafers(
		 * "/data/ABS/CensusTableBuilder2016/HCFMD and TEND by LGA by HIND and MRERD/HCFMD and TEND by LGA by HIND and MRERD - TAS.csv"
		 * , this.initialisedCensusHCFMD_TEND_LGA_HIND_MRERD,
		 * fromColumnHCFMD_TEND_LGA_HIND_MRERD, toColumnHCFMD_TEND_LGA_HIND_MRERD,
		 * this.censusHCFMD_TEND_LGA_HIND_MRERD); System.out.print(", NT");
		 * this.loadAbsCensusTableCsv2Columns2Wafers(
		 * "/data/ABS/CensusTableBuilder2016/HCFMD and TEND by LGA by HIND and MRERD/HCFMD and TEND by LGA by HIND and MRERD - NT.csv"
		 * , this.initialisedCensusHCFMD_TEND_LGA_HIND_MRERD,
		 * fromColumnHCFMD_TEND_LGA_HIND_MRERD, toColumnHCFMD_TEND_LGA_HIND_MRERD,
		 * this.censusHCFMD_TEND_LGA_HIND_MRERD); System.out.print(", ACT");
		 * this.loadAbsCensusTableCsv2Columns2Wafers(
		 * "/data/ABS/CensusTableBuilder2016/HCFMD and TEND by LGA by HIND and MRERD/HCFMD and TEND by LGA by HIND and MRERD - ACT.csv"
		 * , this.initialisedCensusHCFMD_TEND_LGA_HIND_MRERD,
		 * fromColumnHCFMD_TEND_LGA_HIND_MRERD, toColumnHCFMD_TEND_LGA_HIND_MRERD,
		 * this.censusHCFMD_TEND_LGA_HIND_MRERD); System.out.println(", OT.");
		 * this.loadAbsCensusTableCsv2Columns2Wafers(
		 * "/data/ABS/CensusTableBuilder2016/HCFMD and TEND by LGA by HIND and MRERD/HCFMD and TEND by LGA by HIND and MRERD - OT.csv"
		 * , this.initialisedCensusHCFMD_TEND_LGA_HIND_MRERD,
		 * fromColumnHCFMD_TEND_LGA_HIND_MRERD, toColumnHCFMD_TEND_LGA_HIND_MRERD,
		 * this.censusHCFMD_TEND_LGA_HIND_MRERD);
		 */

		// ABS Census CDCF by LGA by FINF
		System.out.print(new Date(System.currentTimeMillis()) + ": Loading ABS Census CDCF by LGA by FINF data");
		this.censusCDCF_LGA_FINF = new HashMap<String, Map<String, Map<String, String>>>(MAP_INIT_SIZE_FINF);
		int fromColumnCDCF_LGA_FINF = 1;
		int toColumnCDCF_LGA_FINF = 27;
		System.out.print(": NSW");
		this.loadAbsCensusTableCsv1Column1Wafer(
				"/data/ABS/CensusTableBuilder2016/CDCF by LGA by FINF/CDCF by LGA by FINF - NSW.csv",
				this.initialisedCensusCDCF_LGA_FINF, fromColumnCDCF_LGA_FINF, toColumnCDCF_LGA_FINF,
				this.censusCDCF_LGA_FINF);
		this.initialisedCensusCDCF_LGA_FINF = true;
		System.out.print(", VIC");
		this.loadAbsCensusTableCsv1Column1Wafer(
				"/data/ABS/CensusTableBuilder2016/CDCF by LGA by FINF/CDCF by LGA by FINF - VIC.csv",
				this.initialisedCensusCDCF_LGA_FINF, fromColumnCDCF_LGA_FINF, toColumnCDCF_LGA_FINF,
				this.censusCDCF_LGA_FINF);
		System.out.print(", QLD");
		this.loadAbsCensusTableCsv1Column1Wafer(
				"/data/ABS/CensusTableBuilder2016/CDCF by LGA by FINF/CDCF by LGA by FINF - QLD.csv",
				this.initialisedCensusCDCF_LGA_FINF, fromColumnCDCF_LGA_FINF, toColumnCDCF_LGA_FINF,
				this.censusCDCF_LGA_FINF);
		System.out.print(", SA");
		this.loadAbsCensusTableCsv1Column1Wafer(
				"/data/ABS/CensusTableBuilder2016/CDCF by LGA by FINF/CDCF by LGA by FINF - SA.csv",
				this.initialisedCensusCDCF_LGA_FINF, fromColumnCDCF_LGA_FINF, toColumnCDCF_LGA_FINF,
				this.censusCDCF_LGA_FINF);
		System.out.print(", WA");
		this.loadAbsCensusTableCsv1Column1Wafer(
				"/data/ABS/CensusTableBuilder2016/CDCF by LGA by FINF/CDCF by LGA by FINF - WA.csv",
				this.initialisedCensusCDCF_LGA_FINF, fromColumnCDCF_LGA_FINF, toColumnCDCF_LGA_FINF,
				this.censusCDCF_LGA_FINF);
		System.out.print(", TAS");
		this.loadAbsCensusTableCsv1Column1Wafer(
				"/data/ABS/CensusTableBuilder2016/CDCF by LGA by FINF/CDCF by LGA by FINF - TAS.csv",
				this.initialisedCensusCDCF_LGA_FINF, fromColumnCDCF_LGA_FINF, toColumnCDCF_LGA_FINF,
				this.censusCDCF_LGA_FINF);
		System.out.print(", NT");
		this.loadAbsCensusTableCsv1Column1Wafer(
				"/data/ABS/CensusTableBuilder2016/CDCF by LGA by FINF/CDCF by LGA by FINF - NT.csv",
				this.initialisedCensusCDCF_LGA_FINF, fromColumnCDCF_LGA_FINF, toColumnCDCF_LGA_FINF,
				this.censusCDCF_LGA_FINF);
		System.out.print(", ACT");
		this.loadAbsCensusTableCsv1Column1Wafer(
				"/data/ABS/CensusTableBuilder2016/CDCF by LGA by FINF/CDCF by LGA by FINF - ACT.csv",
				this.initialisedCensusCDCF_LGA_FINF, fromColumnCDCF_LGA_FINF, toColumnCDCF_LGA_FINF,
				this.censusCDCF_LGA_FINF);
		System.out.println(", OT.");
		this.loadAbsCensusTableCsv1Column1Wafer(
				"/data/ABS/CensusTableBuilder2016/CDCF by LGA by FINF/CDCF by LGA by FINF - OT.csv",
				this.initialisedCensusCDCF_LGA_FINF, fromColumnCDCF_LGA_FINF, toColumnCDCF_LGA_FINF,
				this.censusCDCF_LGA_FINF);

		// Load ATO Individuals Table data
		System.out.println(new Date(System.currentTimeMillis()) + ": Loading ATO Individuals Table 2A data");
		// int[] atoIndividualTable2aColumns = { 5, 6, 7, 18, 19, 20, 21, 24, 25, 30,
		// 31, 32, 33, 34, 35, 36, 37, 38, 39,
		// 40, 41, 42, 43, 46, 47, 62, 63, 66, 67, 96, 97, 98, 99, 102, 103, 104, 105,
		// 140, 141, 142, 143, 144,
		// 145 };
		int[] atoIndividualTable2aColumns = { 5, 6, 7 };
		int ato2aMapCapacity = (int) Math.ceil(atoIndividualTable2aColumns.length / MAP_LOAD_FACTOR);
		this.atoIndividualTable2a = new HashMap<String, Map<String, Map<String, Map<String, Map<String, Map<String, String>>>>>>(
				ato2aMapCapacity);
		this.loadAtoIndividualsTable2a("/data/ATO/Individual/IndividualsTable2A.csv", ATO_INDIVIDUAL_T2A,
				atoIndividualTable2aColumns, this.title, this.atoIndividualTable2a);

		System.out.println(new Date(System.currentTimeMillis()) + ": Loading ATO Individuals Table 3A data");

		int[] atoIndividualTable3aColumns = { 5, 6, 7, 20, 21, 24, 25, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41,
				42, 43, 62, 63, 66, 67, 96, 97, 98, 99, 102, 103, 104, 105, 140, 141, 142, 143, 144, 145 };
		int ato3aMapCapacity = (int) Math.ceil(atoIndividualTable3aColumns.length / MAP_LOAD_FACTOR);
		this.atoIndividualTable3a = new HashMap<String, Map<String, Map<String, Map<String, Map<String, String>>>>>(
				ato3aMapCapacity);
		this.loadAtoIndividualsTable3a("/data/ATO/Individual/IndividualsTable3A.csv", ATO_INDIVIDUAL_T3A,
				atoIndividualTable3aColumns, this.title, this.atoIndividualTable3a);

		System.out.println(new Date(System.currentTimeMillis()) + ": Loading ATO Individuals Table 6B data");
		// int[] atoIndividualTable6bColumns = { 2, 3, 4, 15, 16, 17, 18, 21, 22, 28,
		// 29, 30, 31, 32, 33, 34, 35, 36, 37,
		// 38, 39, 40, 41, 44, 45, 59, 60, 63, 64, 93, 94, 95, 96, 99, 100, 101, 102 };
		int[] atoIndividualTable6bColumns = { 2, 3, 4 };
		int ato6bMapCapacity = (int) Math.ceil(atoIndividualTable6bColumns.length / MAP_LOAD_FACTOR);
		this.atoIndividualTable6b = new HashMap<String, Map<String, String>>(ato6bMapCapacity);
		this.loadAtoIndividualsTable6("/data/ATO/Individual/IndividualsTable6B.csv", ATO_INDIVIDUAL_T6B,
				atoIndividualTable6bColumns, this.title, this.atoIndividualTable6b);

		/*
		 * System.out.println(new Date(System.currentTimeMillis()) +
		 * ": Loading ATO Individuals Table 6C data"); this.atoIndividualTable6c = new
		 * HashMap<String, Map<String, String>>(); int[] atoIndividualTable6cColumns = {
		 * 10, 11, 12, 13, 14, 32, 33, 34, 35, 36 }; this.loadAtoIndividualsTable6(
		 * "/data/ATO/Individual/IndividualsTable6C_transformed.csv",
		 * ATO_INDIVIDUAL_T6C, atoIndividualTable6cColumns, this.title,
		 * this.atoIndividualTable6c);
		 */

		System.out.println(new Date(System.currentTimeMillis()) + ": Loading ATO Individuals Table 9 data");
		// int[] atoIndividualTable9Columns = { 2, 3, 4, 15, 16, 17, 18, 21, 22, 27, 28,
		// 29, 30, 31, 32, 33, 34, 35, 36,
		// 37, 38, 39, 40, 43, 44, 93, 94, 95, 96, 99, 100, 101, 102 };
		int[] atoIndividualTable9Columns = { 2, 3, 4 };
		int ato9MapCapacity = (int) Math.ceil(atoIndividualTable9Columns.length / MAP_LOAD_FACTOR);
		this.atoIndividualTable9DivisionSummary = new HashMap<String, Map<String, Double>>(ato9MapCapacity);
		this.loadAtoIndividualsTable9DivisionSummary("/data/ATO/Individual/IndividualsTable9.csv", ATO_INDIVIDUAL_T9,
				atoIndividualTable9Columns, this.title, this.atoIndividualTable9DivisionSummary);
		// this.atoIndividualTable9 = new HashMap<String, Map<String,
		// String>>(ato9MapCapacity);
		// this.loadAtoIndividualsTable9("/data/ATO/Individual/IndividualsTable9.csv",
		// ATO_INDIVIDUAL_T9,
		// atoIndividualTable9Columns, this.title, this.atoIndividualTable9);

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
						for (int i = 1; i < columnsToImport.length; i++) {
							titles.get(dataSourceName).add(line[columnsToImport[i]]);
						}
					} else if (line[0].equals("Units")) {
						// store unit types
						units.put(dataSourceName, new ArrayList<String>(columnsToImport.length));
						for (int i = 1; i < columnsToImport.length; i++) {
							units.get(dataSourceName).add(line[columnsToImport[i]]);
						}
					} else if (line[0].equals("Series ID")) {
						// store series ID as key with blank collections to populate with data below
						for (int i = 1; i < columnsToImport.length; i++) {
							seriesId[i] = line[columnsToImport[i]];
							data.put(line[columnsToImport[i]], new HashMap<Date, String>());
						}
						header = false;
					}
				} else {
					if (line[0].isEmpty()) {
						footer = true;
					} else {
						for (int i = 1; i < columnsToImport.length; i++) {
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
			Map<String, Map<String, Map<String, String>>> data) {

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
							data.put(yearsToImport[i], new HashMap<String, Map<String, String>>());
							for (int j = 0; j < columnsToImport.length; j++) {
								data.get(yearsToImport[i]).put(line[columnsToImport[j]], new HashMap<String, String>());
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
							data.get(line[yearCol]).get(seriesId[j]).put(line[0], line[columnsToImport[j]]);
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
	 * Loads ABS Census Table Builder tables with two dimensions (row & column).
	 * 
	 * File pre-conditions:<br>
	 * 1. Row 10 contains the column titles.<br>
	 * 2. Row 4 column 1 contains the series title.<br>
	 * 3. Data starts on row 12.<br>
	 * 4. The first column contains the LGA names (not codes).
	 * 
	 * @param fileResourceLocation - the URI of the file to import
	 * @param columnsToImport      - a zero-based array of integers specifying which
	 *                             columns to import (i.e. the first column is
	 *                             column 0). The first column is assumed to be the
	 *                             date and is imported only as the key for the
	 *                             other columns' data.
	 * @param titles               - column titles in CSV file
	 * @param data                 - the data map that the values are returned in
	 */
	private void loadAbsCensusTableCsv2D(String fileResourceLocation, String tableName, int[] columnsToImport,
			boolean isInitialised, Map<String, List<String>> titles, Map<String, Map<String, String>> data) {

		CSVReader reader = null;
		try {
			InputStream is = this.getClass().getResourceAsStream(fileResourceLocation);
			reader = new CSVReader(new InputStreamReader(is));
			boolean header = true;
			boolean footer = false;
			int currentRow = 1;
			int titleRow = 10;
			int lastHeaderRow = 11;
			String[] seriesId = new String[columnsToImport.length];

			String[] line = null;
			while ((line = reader.readNext()) != null) {
				if (header) {
					if (currentRow == titleRow) {
						for (int i = 0; i < columnsToImport.length; i++) {
							seriesId[i] = line[columnsToImport[i]];
						}
						if (!isInitialised) {
							titles.put(tableName, new ArrayList<String>(columnsToImport.length));
							for (int i = 0; i < columnsToImport.length; i++) {
								// store title
								titles.get(tableName).add(line[columnsToImport[i]]);

								// store series ID as key with blank collections to populate with data below
								data.put(line[columnsToImport[i]], new HashMap<String, String>());
							}
						}
					} else if (currentRow == lastHeaderRow) {
						header = false;
					}
				} else if (!footer) {
					if (line.length > 0 && line[0].equals("Total")) {
						footer = true;
					} else {
						for (int i = 0; i < columnsToImport.length; i++) {
							// parse the body of the data
							String lgaCode = this.area.getLgaCodeFromName(line[0]);
							data.get(seriesId[i]).put(lgaCode, line[columnsToImport[i]]);
							// WISHLIST: modify this so it can use POA, state, etc. not just LGA
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
	 * Loads ABS Census Table Builder tables with three dimensions (row, column &
	 * wafer).
	 * 
	 * File pre-conditions:<br>
	 * 1. Row 10 contains the first wafer title.<br>
	 * 2. The row after each wafer title is the column titles.<br>
	 * 3. Row 4 column 1 contains the series title.<br>
	 * 4. Data starts on row 12.<br>
	 * 5. The first column contains the LGA names (not codes).<br>
	 * 6. The first row in the footer begins with "Data Source".
	 * 
	 * @param fileResourceLocation - the URI of the file to import
	 * @param columnsToImport      - a zero-based array of integers specifying which
	 *                             columns to import (i.e. the first column is
	 *                             column 0). The first column is assumed to be the
	 *                             date and is imported only as the key for the
	 *                             other columns' data.
	 * @param titles               - column titles in CSV file
	 * @param data                 - the data map that the values are returned in.
	 *                             Keys are: column, row, wafer. (div, LGA, income)
	 */
	private void loadAbsCensusTableCsv1Column1Wafer(String fileResourceLocation, boolean isInitialised,
			int fromColumnIndex, int toColumnIndex, Map<String, Map<String, Map<String, String>>> data) {

		CSVReader reader = null;
		try {
			InputStream is = this.getClass().getResourceAsStream(fileResourceLocation);
			reader = new CSVReader(new InputStreamReader(is));
			boolean header = true;
			boolean footer = false;
			int currentRow = 1;
			int lastHeaderRow = 9; // the row before the first wafer's title row
			boolean prevRowIsBlank = true; // there's a blank row before wafer names
			boolean prevRowIsWaferName = false;
			String waferName = null;
			int waferNumber = 0;
			String[] seriesId = new String[toColumnIndex - fromColumnIndex];

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
							prevRowIsWaferName = true;
							waferNumber++;
							prevRowIsBlank = false;
						} else {
							if (prevRowIsWaferName) {
								if (waferNumber == 1) {
									// set series ID
									for (int i = 0; i < toColumnIndex - fromColumnIndex; i++) {
										seriesId[i] = line[i + fromColumnIndex];
									}
									if (!isInitialised) {
										for (int i = 0; i < toColumnIndex - fromColumnIndex; i++) {
											// store series ID as key with blank collections to populate with data below
											data.put(line[i + fromColumnIndex],
													new HashMap<String, Map<String, String>>(MAP_INIT_SIZE_LGA));
										}
									}
								}
								prevRowIsWaferName = false;
							} else if (line.length > 1 && !line[1].isBlank()) {
								// parse the body of the data
								// WISHLIST: modify this so it can use POA, state, etc. not just LGA
								String lgaCode = this.area.getLgaCodeFromName(line[0]);
								if (lgaCode != null) {
									// null check excludes invalid LGAs
									for (int i = 0; i < toColumnIndex - fromColumnIndex; i++) {
										if (waferNumber == 1) {
											data.get(seriesId[i]).put(lgaCode,
													new HashMap<String, String>(MAP_INIT_SIZE_CDCF));
										}
										data.get(seriesId[i]).get(lgaCode).put(waferName, line[i + fromColumnIndex]);
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
	 * Loads ABS Census Table Builder tables with one row series, three column
	 * series, and one wafer series.
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
	 *                             Keys are: 3 columns, row, wafer. (AGE5P, INDP,
	 *                             INCP, LGA, SEXP)
	 */
	private void loadAbsCensusTableCsv3Columns1Wafer(String fileResourceLocation, boolean isInitialised,
			int fromColumnIndex, int toColumnIndex,
			Map<String, Map<String, Map<String, Map<String, Map<String, String>>>>> data, String lgaOrPoa) {

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
			final int columnSeriesMax = 3; // because the dataset contains 3 column series
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
										if (!data.containsKey(columnTitles[0][i])) {
											// add column series 1 key
											data.put(columnTitles[0][i],
													new HashMap<String, Map<String, Map<String, Map<String, String>>>>(
															MAP_INIT_SIZE_INDP));
										}
										if (!data.get(columnTitles[0][i]).containsKey(columnTitles[1][i])) {
											// add column series 2 key
											data.get(columnTitles[0][i]).put(columnTitles[1][i],
													new HashMap<String, Map<String, Map<String, String>>>(
															MAP_INIT_SIZE_INCP));
										}
										if (!data.get(columnTitles[0][i]).get(columnTitles[1][i])
												.containsKey(columnTitles[2][i])) {
											// add column series 3 key
											data.get(columnTitles[0][i]).get(columnTitles[1][i]).put(columnTitles[2][i],
													new HashMap<String, Map<String, String>>(MAP_INIT_SIZE_LGA));
										}
									}
								}
								columnSeriesNumber++; // make sure this is only executed once
							} else if (line.length > 1 && !line[1].isBlank()) {
								// parse the body of the data
								String areaCode = null;
								if (lgaOrPoa.equalsIgnoreCase("LGA")) {
									areaCode = this.area.getLgaCodeFromName(line[0]);
								} else {
									// assume it's POA in the format NNNN, SS
									areaCode = line[0].substring(0, line[0].indexOf(","));
								}
								if (areaCode != null) {
									// null check excludes invalid LGAs
									for (int i = 0; i < toColumnIndex - fromColumnIndex; i++) {
										if (waferNumber == 1) {
											data.get(columnTitles[0][i]).get(columnTitles[1][i]).get(columnTitles[2][i])
													.put(areaCode, new HashMap<String, String>(MAP_INIT_SIZE_SEXP));
										}
										data.get(columnTitles[0][i]).get(columnTitles[1][i]).get(columnTitles[2][i])
												.get(areaCode).put(waferName, line[i + fromColumnIndex]);
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
			int fromColumnIndex, int toColumnIndex, Map<String, Map<String, Map<String, Map<String, String>>>> data) {

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
			final int columnSeriesMax = 3; // because the dataset contains 2 column series
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
										if (!data.containsKey(columnTitles[0][i])) {
											// add column series 1 key
											data.put(columnTitles[0][i],
													new HashMap<String, Map<String, Map<String, String>>>(
															MAP_INIT_SIZE_RNTRD));
										}
										if (!data.get(columnTitles[0][i]).containsKey(columnTitles[1][i])) {
											// add column series 2 key
											data.get(columnTitles[0][i]).put(columnTitles[1][i],
													new HashMap<String, Map<String, String>>(MAP_INIT_SIZE_LGA));
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
													new HashMap<String, String>(MAP_INIT_SIZE_HCFMD));
										}
										data.get(columnTitles[0][i]).get(columnTitles[1][i]).get(lgaCode).put(waferName,
												line[i + fromColumnIndex]);
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
	 * Loads ABS Census Table Builder tables with one row series, two column series,
	 * and two wafer series.
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
	 *                             Keys are: 2 columns, row, 2 wafers. (HIND,
	 *                             RNTRD/MRERD, LGA, HCFMD, TEND)
	 */
	private void loadAbsCensusTableCsv2Columns2Wafers(String fileResourceLocation, boolean isInitialised,
			int fromColumnIndex, int toColumnIndex,
			Map<String, Map<String, Map<String, Map<String, Map<String, String>>>>> data) {

		CSVReader reader = null;
		try {
			InputStream is = this.getClass().getResourceAsStream(fileResourceLocation);
			reader = new CSVReader(new InputStreamReader(is));
			boolean header = true;
			boolean footer = false;
			int currentRow = 1;
			int lastHeaderRow = 9; // the row before the first wafer's title row
			boolean prevRowIsBlank = true; // there's a blank row before wafer names
			final int numWafers = 2;
			String[] waferName = new String[numWafers];
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
							String[] tmp = line[0].split(","); // wafer names are separated by commas
							for (int i = 0; i < numWafers; i++) {
								waferName[i] = tmp[i].trim();
							}
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
										if (!data.containsKey(columnTitles[0][i])) {
											// add column series 1 key
											data.put(columnTitles[0][i],
													new HashMap<String, Map<String, Map<String, Map<String, String>>>>(
															Math.max(MAP_INIT_SIZE_RNTRD, MAP_INIT_SIZE_MRERD)));
										}
										if (!data.get(columnTitles[0][i]).containsKey(columnTitles[1][i])) {
											// add column series 2 key
											data.get(columnTitles[0][i]).put(columnTitles[1][i],
													new HashMap<String, Map<String, Map<String, String>>>(
															MAP_INIT_SIZE_LGA));
										}
									}
								}
							} else if (line.length > 1 && !line[1].isBlank()) {
								// parse the body of the data
								// WISHLIST: modify this so it can use POA, state, etc. not just LGA
								String lgaCode = this.area.getLgaCodeFromName(line[0]);
								if (lgaCode != null) {
									// null check excludes invalid LGAs
									for (int i = 0; i < toColumnIndex - fromColumnIndex; i++) {
										if (waferNumber == 1) {
											data.get(columnTitles[0][i]).get(columnTitles[1][i]).put(lgaCode,
													new HashMap<String, Map<String, String>>(MAP_INIT_SIZE_HCFMD));
										}
										if (!data.get(columnTitles[0][i]).get(columnTitles[1][i]).get(lgaCode)
												.containsKey(waferName[0])) {
											// add map if this is the first time the wafer 1 has been read
											data.get(columnTitles[0][i]).get(columnTitles[1][i]).get(lgaCode)
													.put(waferName[0], new HashMap<String, String>(MAP_INIT_SIZE_TEND));
										}
										data.get(columnTitles[0][i]).get(columnTitles[1][i]).get(lgaCode)
												.get(waferName[0]).put(waferName[1], line[i + fromColumnIndex]);
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
	 * Load data from ATO Individuals Table 2A
	 * 
	 * @param fileResourceLocation - the URI of the file to import
	 * @param dataSourceName       - the name used to identify this data source (in
	 *                             the shared maps)
	 * @param tableName            - the name used to store this series' data in the
	 *                             maps
	 * @param columnsToImport      - a zero-based array of integers specifying which
	 *                             columns to import (i.e. the first column is
	 *                             column 0).
	 * @param titles               - column titles in CSV file
	 * @param data                 - the data map that the values are returned
	 *                             in.<br>
	 *                             Keys: Series Title, State, Age, Gender, Taxable
	 *                             Status, Lodgment Method
	 */
	private void loadAtoIndividualsTable2a(String fileResourceLocation, String tableName, int[] columnsToImport,
			Map<String, List<String>> titles,
			Map<String, Map<String, Map<String, Map<String, Map<String, Map<String, String>>>>>> data) {

		CSVReader reader = null;
		try {
			InputStream is = this.getClass().getResourceAsStream(fileResourceLocation);
			reader = new CSVReader(new InputStreamReader(is));
			boolean header = true;
			boolean footer = false;
			String[] seriesId = new String[columnsToImport.length];

			String[] line = null;
			while ((line = reader.readNext()) != null && !footer) {
				if (header) {
					if (line[0].equals("Lodgment method")) {
						// title row
						List<String> thesecolumnNames = new ArrayList<String>(columnsToImport.length);
						for (int i = 0; i < columnsToImport.length; i++) {
							// store title
							seriesId[i] = line[columnsToImport[i]];
							thesecolumnNames.add(line[columnsToImport[i]]);

							// store series ID as key with blank collections to populate with data below
							data.put(line[columnsToImport[i]],
									new HashMap<String, Map<String, Map<String, Map<String, Map<String, String>>>>>());
						}
						titles.put(tableName, thesecolumnNames);
						header = false;
					}
				} else {
					if (!line[0].isBlank()) {
						// Keys: Series Title, State, Age, Gender, Taxable Status, Lodgment Method
						String thisState = line[3].trim().length() > 3 ? "Other" : line[3].trim();
						String thisAge = line[4];
						String thisSex = line[1].substring(0, 1).toUpperCase();
						String thisTaxableStatus = line[2].substring(0, 1).equals("N") ? "N" : "Y";
						String thisLodgmentMethod = line[0].substring(0, 1);

						for (int i = 0; i < columnsToImport.length; i++) {
							// create nested maps for new data categories
							if (!data.get(seriesId[i]).containsKey(thisState)) {
								data.get(seriesId[i]).put(thisState,
										new HashMap<String, Map<String, Map<String, Map<String, String>>>>());
							}
							if (!data.get(seriesId[i]).get(thisState).containsKey(thisAge)) {
								data.get(seriesId[i]).get(thisState).put(thisAge,
										new HashMap<String, Map<String, Map<String, String>>>());
							}
							if (!data.get(seriesId[i]).get(thisState).get(thisAge).containsKey(thisSex)) {
								data.get(seriesId[i]).get(thisState).get(thisAge).put(thisSex,
										new HashMap<String, Map<String, String>>());
							}
							if (!data.get(seriesId[i]).get(thisState).get(thisAge).get(thisSex)
									.containsKey(thisTaxableStatus)) {
								data.get(seriesId[i]).get(thisState).get(thisAge).get(thisSex).put(thisTaxableStatus,
										new HashMap<String, String>());
							}

							// parse the body of the data
							data.get(seriesId[i]).get(thisState).get(thisAge).get(thisSex).get(thisTaxableStatus)
									.put(thisLodgmentMethod, line[columnsToImport[i]]);
						}
					} else {
						footer = true;
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
	 * Load data from ATO Individuals Table 3A
	 * 
	 * @param fileResourceLocation - the URI of the file to import
	 * @param dataSourceName       - the name used to identify this data source (in
	 *                             the shared maps)
	 * @param tableName            - the name used to store this series' data in the
	 *                             maps
	 * @param columnsToImport      - a zero-based array of integers specifying which
	 *                             columns to import (i.e. the first column is
	 *                             column 0).
	 * @param titles               - column titles in CSV file
	 * @param data                 - the data map that the values are returned
	 *                             in.<br>
	 *                             Keys: Series Title, Income Range, Age, Gender,
	 *                             Taxable Status
	 */
	private void loadAtoIndividualsTable3a(String fileResourceLocation, String tableName, int[] columnsToImport,
			Map<String, List<String>> titles,
			Map<String, Map<String, Map<String, Map<String, Map<String, String>>>>> data) {

		CSVReader reader = null;
		try {
			InputStream is = this.getClass().getResourceAsStream(fileResourceLocation);
			reader = new CSVReader(new InputStreamReader(is));
			boolean header = true;
			boolean footer = false;
			String[] seriesId = new String[columnsToImport.length];

			String[] line = null;
			while ((line = reader.readNext()) != null && !footer) {
				if (header) {
					if (line[0].equals("Gender")) {
						// title row
						List<String> theseColumnNames = new ArrayList<String>(columnsToImport.length);
						for (int i = 0; i < columnsToImport.length; i++) {
							// store title
							seriesId[i] = line[columnsToImport[i]];
							theseColumnNames.add(line[columnsToImport[i]]);

							// store series ID as key with blank collections to populate with data below
							data.put(line[columnsToImport[i]],
									new HashMap<String, Map<String, Map<String, Map<String, String>>>>());
						}
						titles.put(tableName, theseColumnNames);
						header = false;
					}
				} else {
					if (!line[0].isBlank()) {
						// Keys: Series Title, Income Range, Age, Gender, Taxable Status
						String thisIncomeRange = line[3].trim().length() > 3 ? "Other" : line[3].trim();
						String thisAge = line[2];
						String thisSex = line[0].substring(0, 1).toUpperCase();
						String thisTaxableStatus = line[1].substring(0, 1).equals("N") ? "N" : "Y";

						for (int i = 0; i < columnsToImport.length; i++) {
							// create nested maps for new data categories
							if (!data.get(seriesId[i]).containsKey(thisIncomeRange)) {
								data.get(seriesId[i]).put(thisIncomeRange,
										new HashMap<String, Map<String, Map<String, String>>>());
							}
							if (!data.get(seriesId[i]).get(thisIncomeRange).containsKey(thisAge)) {
								data.get(seriesId[i]).get(thisIncomeRange).put(thisAge,
										new HashMap<String, Map<String, String>>());
							}
							if (!data.get(seriesId[i]).get(thisIncomeRange).get(thisAge).containsKey(thisSex)) {
								data.get(seriesId[i]).get(thisIncomeRange).get(thisAge).put(thisSex,
										new HashMap<String, String>());
							}

							// parse the body of the data
							data.get(seriesId[i]).get(thisIncomeRange).get(thisAge).get(thisSex).put(thisTaxableStatus,
									line[columnsToImport[i]]);
						}
					} else {
						footer = true;
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
	 * Load data from ATO Individuals Table 6B, 6C
	 * 
	 * @param fileResourceLocation - the URI of the file to import
	 * @param dataSourceName       - the name used to identify this data source (in
	 *                             the shared maps)
	 * @param tableName            - the name used to store this series' data in the
	 *                             maps
	 * @param columnsToImport      - a zero-based array of integers specifying which
	 *                             columns to import (i.e. the first column is
	 *                             column 0).
	 * @param titles               - column titles in CSV file
	 * @param data                 - the data map that the values are returned in
	 */
	private void loadAtoIndividualsTable6(String fileResourceLocation, String tableName, int[] columnsToImport,
			Map<String, List<String>> titles, Map<String, Map<String, String>> data) {

		CSVReader reader = null;
		try {
			InputStream is = this.getClass().getResourceAsStream(fileResourceLocation);
			reader = new CSVReader(new InputStreamReader(is));
			boolean header = true;
			boolean footer = false;
			String[] seriesId = new String[columnsToImport.length];

			String[] line = null;
			while ((line = reader.readNext()) != null && !footer) {
				if (header) {
					if (line[0].equals("State/ Territory1")) {
						// title row
						List<String> thesecolumnNames = new ArrayList<String>(columnsToImport.length);
						for (int i = 0; i < columnsToImport.length; i++) {
							// store title
							seriesId[i] = line[columnsToImport[i]];
							thesecolumnNames.add(line[columnsToImport[i]]);

							// store series ID as key with blank collections to populate with data below
							data.put(line[columnsToImport[i]], new HashMap<String, String>());
						}
						titles.put(tableName, thesecolumnNames);
						header = false;
					}
				} else {
					if (!line[0].isBlank()) {
						// Check if line[1] is numeric, and skip this row if it's not because we
						// can't map a "state other" category to an LGA
						if (NumberUtils.isCreatable(line[1])) {
							for (int i = 0; i < columnsToImport.length; i++) {
								// parse the body of the data
								data.get(seriesId[i]).put(line[1], line[columnsToImport[i]]);
							}
						}
					} else {
						footer = true;
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
	 * Load data from ATO Individuals Table 9
	 * 
	 * @param fileResourceLocation - the URI of the file to import
	 * @param dataSourceName       - the name used to identify this data source (in
	 *                             the shared maps)
	 * @param tableName            - the name used to store this series' data in the
	 *                             maps
	 * @param columnsToImport      - a zero-based array of integers specifying which
	 *                             columns to import (i.e. the first column is
	 *                             column 0).
	 * @param titles               - column titles in CSV file
	 * @param data                 - the data map that the values are returned in
	 */
	private void loadAtoIndividualsTable9(String fileResourceLocation, String tableName, int[] columnsToImport,
			Map<String, List<String>> titles, Map<String, Map<String, String>> data) {

		CSVReader reader = null;
		try {
			InputStream is = this.getClass().getResourceAsStream(fileResourceLocation);
			reader = new CSVReader(new InputStreamReader(is));
			boolean header = true;
			boolean footer = false;
			String[] seriesId = new String[columnsToImport.length];

			String[] line = null;
			while ((line = reader.readNext()) != null && !footer) {
				if (header) {
					if (line[0].equals("Broad Industry1")) {
						// title row
						List<String> thesecolumnNames = new ArrayList<String>(columnsToImport.length);
						for (int i = 0; i < columnsToImport.length; i++) {
							// store title
							seriesId[i] = line[columnsToImport[i]];
							thesecolumnNames.add(line[columnsToImport[i]]);

							// store series ID as key with blank collections to populate with data below
							data.put(line[columnsToImport[i]], new HashMap<String, String>());
						}
						titles.put(tableName, thesecolumnNames);
						header = false;
					}
				} else {
					if (!line[1].equals("Other individuals")) {
						String fineIndustryCode = line[1].substring(0, 5);
						for (int i = 0; i < columnsToImport.length; i++) {
							// parse the body of the data
							data.get(seriesId[i]).put(fineIndustryCode, line[columnsToImport[i]]);
						}
					} else {
						footer = true;
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
	 * Load data from ATO Individuals Table 9
	 * 
	 * @param fileResourceLocation - the URI of the file to import
	 * @param dataSourceName       - the name used to identify this data source (in
	 *                             the shared maps)
	 * @param tableName            - the name used to store this series' data in the
	 *                             maps
	 * @param columnsToImport      - a zero-based array of integers specifying which
	 *                             columns to import (i.e. the first column is
	 *                             column 0).
	 * @param titles               - column titles in CSV file
	 * @param data                 - the data map that the values are returned in
	 */
	private void loadAtoIndividualsTable9DivisionSummary(String fileResourceLocation, String tableName,
			int[] columnsToImport, Map<String, List<String>> titles, Map<String, Map<String, Double>> data) {

		CSVReader reader = null;
		try {
			InputStream is = this.getClass().getResourceAsStream(fileResourceLocation);
			reader = new CSVReader(new InputStreamReader(is));
			boolean header = true;
			boolean footer = false;
			String[] seriesId = new String[columnsToImport.length];

			String[] line = null;
			while ((line = reader.readNext()) != null && !footer) {
				if (header) {
					if (line[0].equals("Broad Industry1")) {
						// title row
						List<String> thesecolumnNames = new ArrayList<String>(columnsToImport.length);
						int divisionMapCapacity = (int) Math.ceil(19 / MAP_LOAD_FACTOR + 1); // 19 divisions imported
						for (int i = 0; i < columnsToImport.length; i++) {
							// store title
							seriesId[i] = line[columnsToImport[i]];
							thesecolumnNames.add(line[columnsToImport[i]]);

							// store series ID as key with blank collections to populate with data below
							data.put(line[columnsToImport[i]], new HashMap<String, Double>(divisionMapCapacity));
						}
						titles.put(tableName, thesecolumnNames);
						header = false;
					}
				} else {
					if (!line[1].equals("Other individuals")) {
						String divisionCode = line[0].substring(0, 1).toUpperCase();
						for (int i = 0; i < columnsToImport.length; i++) {
							// parse the body of the data
							double oldVal = 0d;
							if (data.get(seriesId[i]).get(divisionCode) != null) {
								oldVal = data.get(seriesId[i]).get(divisionCode);
							}
							data.get(seriesId[i]).put(divisionCode,
									oldVal + Double.valueOf(line[columnsToImport[i]].replace(",", "")));
						}
					} else {
						footer = true;
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

	@PostConstruct
	private void init() {
		this.dataLoaded = false;
		this.totalPopulation = null;
		this.adjustedPeopleByLga = null;

		this.title = null;
		this.unitType = null;

		this.rbaE2 = null;

		this.abs1410_0Economy = null;
		this.abs1410_0Family = null;
		this.abs1410_0Income = null;

		this.atoIndividualTable2a = null;
		this.atoIndividualTable3a = null;
		this.atoIndividualTable6b = null;
		this.atoIndividualTable6c = null;
		this.atoIndividualTable9 = null;
		this.atoIndividualTable9DivisionSummary = null;

		this.censusCDCF_LGA_FINF = null;
		this.censusHCFMD_TEND_LGA_HIND_MRERD = null;
		this.censusHCFMD_TEND_LGA_HIND_RNTRD = null;
		this.censusSEXP_LGA_AGE5P_INDP_INCP = null;
		this.censusSEXP_POA_AGE5P_INDP_INCP = null;
		this.initialisedCensusSEXP_POA_AGE5P_INDP_INCP = false;
		this.initialisedCensusHCFMD_LGA_HIND_MRERD = false;
		this.initialisedCensusHCFMD_LGA_HIND_RNTRD = false;
		this.initialisedCensusCDCF_LGA_FINF = false;

		this.initialisedCensusHCFMD_TEND_LGA_HIND_MRERD = false;
		this.initialisedCensusHCFMD_TEND_LGA_HIND_RNTRD = false;
		this.initialisedCensusSEXP_LGA_AGE5P_INDP_INCP = false;

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
	public Map<String, Map<String, Map<String, String>>> getAbs1410_0Economy() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs1410_0Economy;
	}

	/**
	 * @return the abs1410_0Family
	 */
	public Map<String, Map<String, Map<String, String>>> getAbs1410_0Family() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs1410_0Family;
	}

	/**
	 * @return the abs1410_0Income
	 */
	public Map<String, Map<String, Map<String, String>>> getAbs1410_0Income() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs1410_0Income;
	}

	/**
	 * @return the atoIndividualTable2a
	 */
	public Map<String, Map<String, Map<String, Map<String, Map<String, Map<String, String>>>>>> getAtoIndividualTable2a() {
		return atoIndividualTable2a;
	}

	/**
	 * @return the atoIndividualTable3a
	 */
	public Map<String, Map<String, Map<String, Map<String, Map<String, String>>>>> getAtoIndividualTable3a() {
		return atoIndividualTable3a;
	}

	/**
	 * @return the atoIndividualTable6b
	 */
	public Map<String, Map<String, String>> getAtoIndividualTable6b() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return atoIndividualTable6b;
	}

	/**
	 * @return the atoIndividualTable6c
	 */
	public Map<String, Map<String, String>> getAtoIndividualTable6c() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return atoIndividualTable6c;
	}

	/**
	 * @return the atoIndividualTable9
	 */
	public Map<String, Map<String, String>> getAtoIndividualTable9() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return atoIndividualTable9;
	}

	/**
	 * @return the atoIndividualTable9DivisionSummary
	 */
	public Map<String, Map<String, Double>> getAtoIndividualTable9DivisionSummary() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return atoIndividualTable9DivisionSummary;
	}

	/**
	 * @return the censusHCFMD_LGA_HIND_MRERD
	 */
	public Map<String, Map<String, Map<String, Map<String, String>>>> getCensusHCFMD_LGA_HIND_MRERD() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return censusHCFMD_LGA_HIND_MRERD;
	}

	/**
	 * @return the censusSEXP_POA_AGE5P_INDP_INCP
	 */
	public Map<String, Map<String, Map<String, Map<String, Map<String, String>>>>> getCensusSEXP_POA_AGE5P_INDP_INCP() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return censusSEXP_POA_AGE5P_INDP_INCP;
	}

	/**
	 * @return the censusHCFMD_LGA_HIND_RNTRD
	 */
	public Map<String, Map<String, Map<String, Map<String, String>>>> getCensusHCFMD_LGA_HIND_RNTRD() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return censusHCFMD_LGA_HIND_RNTRD;
	}

	/**
	 * @return the censusCDCF_LGA_FINF
	 */
	public Map<String, Map<String, Map<String, String>>> getCensusCDCF_LGA_FINF() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return censusCDCF_LGA_FINF;
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
