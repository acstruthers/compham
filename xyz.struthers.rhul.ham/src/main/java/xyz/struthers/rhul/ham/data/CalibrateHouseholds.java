/**
 * 
 */
package xyz.struthers.rhul.ham.data;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import xyz.struthers.lang.CustomMath;
import xyz.struthers.rhul.ham.agent.Household;
import xyz.struthers.rhul.ham.agent.Individual;
import xyz.struthers.rhul.ham.config.Properties;
import xyz.struthers.rhul.ham.process.AustralianEconomy;

/**
 * Calibrates the P&L and Bal Shts of households by grouping together the P&L
 * and Bal Shts of individuals.
 * 
 * @author Adam Struthers
 * @since 10-Dec-2018
 */
@Component
@Scope(value = "singleton")
public class CalibrateHouseholds {

	private static final boolean DEBUG = true;

	// CONSTANTS
	private static final double MILLION = 1000000d;
	private static final double THOUSAND = 1000d;
	private static final double PERCENT = 0.01d;

	private static final double NUM_MONTHS = 12d;
	private static final double NUM_WEEKS = 365d / 7d;

	public static final String ABS_1410_YEAR = "2016";
	public static final String CALIBRATION_DATE_ABS = "01/06/2018";
	public static final String CALIBRATION_DATE_RBA = "30/06/2018";

	private static final int AGENT_LIST_INIT_SIZE = 10000000; // 10 million households
	private static final int AGENT_LIST_CDCF_INIT_SIZE = 5; // initial size of the lists in each cell

	// map optimisation
	public static final double MAP_LOAD_FACTOR = 0.75d;
	public static final int MAP_LGA_INIT_CAPACITY = (int) Math.ceil(540 / MAP_LOAD_FACTOR) + 1;

	// Series Titles
	private static final String[] ABS_HIND_RANGES = { "Negative income", "Nil income", "$1-$149 ($1-$7,799)",
			"$150-$299 ($7,800-$15,599)", "$300-$399 ($15,600-$20,799)", "$400-$499 ($20,800-$25,999)",
			"$500-$649 ($26,000-$33,799)", "$650-$799 ($33,800-$41,599)", "$800-$999 ($41,600-$51,999)",
			"$1,000-$1,249 ($52,000-$64,999)", "$1,250-$1,499 ($65,000-$77,999)", "$1,500-$1,749 ($78,000-$90,999)",
			"$1,750-$1,999 ($91,000-$103,999)", "$2,000-$2,499 ($104,000-$129,999)",
			"$2,500-$2,999 ($130,000-$155,999)", "$3,000-$3,499 ($156,000-$181,999)",
			"$3,500-$3,999 ($182,000-$207,999)", "$4,000-$4,499 ($208,000-$233,999)",
			"$4,500-$4,999 ($234,000-$259,999)", "$5,000-$5,999 ($260,000-$311,999)",
			"$6,000-$7,999 ($312,000-$415,999)", "$8,000 or more ($416,000 or more)" };
	// , "Partial income stated", "All incomes not stated", "Not applicable" };
	private static final String[] ABS_FINF_RANGES = { "Negative income", "Nil income", "$1-$149 ($1-$7,799)",
			"$150-$299 ($7,800-$15,599)", "$300-$399 ($15,600-$20,799)", "$400-$499 ($20,800-$25,999)",
			"$500-$649 ($26,000-$33,799)", "$650-$799 ($33,800-$41,599)", "$800-$999 ($41,600-$51,999)",
			"$1,000-$1,249 ($52,000-$64,999)", "$1,250-$1,499 ($65,000-$77,999)", "$1,500-$1,749 ($78,000-$90,999)",
			"$1,750-$1,999 ($91,000-$103,999)", "$2,000-$2,499 ($104,000-$129,999)",
			"$2,500-$2,999 ($130,000-$155,999)", "$3,000-$3,499 ($156,000-$181,999)",
			"$3,500-$3,999 ($182,000-$207,999)", "$4,000-$4,499 ($208,000-$233,999)",
			"$4,500-$4,999 ($234,000-$259,999)", "$5,000-$5,999 ($260,000-$311,999)",
			"$6,000-$7,999 ($312,000-$415,999)", "$8,000 or more ($416,000 or more)", "Partial income stated",
			"All incomes not stated", "Not applicable" };
	private static final String[] ABS_MRERD_RANGES = { "Nil repayments", "$1-$149", "$150-$299", "$300-$449",
			"$450-$599", "$600-$799", "$800-$999", "$1,000-$1,199", "$1,200-$1,399", "$1,400-$1,599", "$1,600-$1,799",
			"$1,800-$1,999", "$2,000-$2,199", "$2,200-$2,399", "$2,400-$2,599", "$2,600-$2,999", "$3,000-$3,999",
			"$4,000-$4,999", "$5000 and over", "Not stated", "Not applicable" };
	/**
	 * ABS_MRERD_MIDPOINT is an array of the monthly mortgage repayments that the
	 * Households will be calibrated with. It takes the midpoint of each range, and
	 * combines "Nil payments", "Not states" and "Not applicable" into the $0
	 * category.
	 */
	private static final int[] ABS_MRERD_MIDPOINT = { 0, 75, 225, 375, 525, 700, 900, 1100, 1300, 1500, 1700, 1900,
			2100, 2300, 2500, 2800, 3500, 4500, 6000 };
	private static final String[] ABS_RNTRD_RANGES = { "Nil payments", "$1-$74", "$75-$99", "$100-$124", "$125-$149",
			"$150-$174", "$175-$199", "$200-$224", "$225-$249", "$250-$274", "$275-$299", "$300-$324", "$325-$349",
			"$350-$374", "$375-$399", "$400-$424", "$425-$449", "$450-$549", "$550-$649", "$650-$749", "$750-$849",
			"$850-$949", "$950 and over", "Not stated", "Not applicable" };
	/**
	 * ABS_RNTRD_MIDPOINT is an array of the monthly rental payments that the
	 * Households will be calibrated with. It takes the midpoint of each range, and
	 * combines "Nil payments", "Not stated" and "Not applicable" into the $0
	 * category.
	 */
	private static final int[] ABS_RNTRD_MIDPOINT = { 0, 160, 380, 490, 600, 700, 815, 925, 1030, 1140, 1250, 1360,
			1465, 1575, 1685, 1795, 1900, 2170, 2600, 3040, 3475, 3910, 4565 };
	private static final String[] ABS_HCFMD = { "One family household: Couple family with no children",
			"One family household: Couple family with children", "One family household: One parent family",
			"One family household: Other family", "Two family household: Couple family with no children",
			"Two family household: Couple family with children", "Two family household: One parent family",
			"Two family household: Other family", "Three or more family household: Couple family with no children",
			"Three or more family household: Couple family with children",
			"Three or more family household: One parent family", "Three or more family household: Other family",
			"Lone person household", "Group household", "Visitors only household", "Other non-classifiable household",
			"Not applicable" };
	private static final String[] ABS_HCFMF = { "One family household: Couple family with no children",
			"One family household: Couple family with children", "One family household: One parent family",
			"One family household: Other family", "Two family household: Couple family with no children",
			"Two family household: Couple family with children", "Two family household: One parent family",
			"Two family household: Other family", "Three or more family household: Couple family with no children",
			"Three or more family household: Couple family with children",
			"Three or more family household: One parent family", "Three or more family household: Other family",
			"Not applicable" };
	private static final String[] ABS_CDCF = { "Couple family with: No dependent children",
			"Couple family with: One dependent child", "Couple family with: Two dependent children",
			"Couple family with: Three dependent children", "Couple family with: Four dependent children",
			"Couple family with: Five dependent children", "Couple family with: Six or more dependent children",
			"One parent family with: No dependent children", "One parent family with: One dependent child",
			"One parent family with: Two dependent children", "One parent family with: Three dependent children",
			"One parent family with: Four dependent children", "One parent family with: Five dependent children",
			"One parent family with: Six or more dependent children", "Not applicable" }; // , "Total" };
	// assume one adult in "Lone person household" and four in "Group household"
	private static final int[] ABS_CDCF_ADULT_COUNT = { 2, 2, 2, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1, 1, 1, 4 };
	// assume there are no children in "Lone person household" or "Group household"
	private static final int[] ABS_CDCF_CHILD_COUNT = { 0, 1, 2, 3, 4, 5, 6, 0, 1, 2, 3, 4, 5, 6, 0, 0 };
	// Henderson poverty line amounts based on CDCF
	// SOURCE: Table 1: Poverty Lines: Australia, June Quarter, 2018
	// https://melbourneinstitute.unimelb.edu.au/publications/poverty-lines
	private static final double[] CDCF_HENDERSON_EXCL_HOUSING = { 505.82, 628.75, 751.69, 874.62, 996.2, 1117.78,
			1239.36, 347.93, 477.85, 600.79, 723.72, 846.66, 969.6, 1092.54 };
	private static final double HENDERSON_EXCL_HOUSING_LONE_PERSON = CDCF_HENDERSON_EXCL_HOUSING[7];
	private static final double HENDERSON_EXCL_HOUSING_GROUP = 821.60; // assumes 4 adults, no children

	// series titles
	private static final String RBA_E1_SERIESID_CASH = "BSPNSHUFAD"; // Household deposits
	private static final String RBA_E1_SERIESID_SUPER = "BSPNSHUFAR"; // Household superannuation
	private static final String RBA_E1_SERIESID_EQUITIES = "BSPNSHUFAS"; // Household equities
	private static final String RBA_E1_SERIESID_OTHER_FIN_ASSETS = "BSPNSHUFAO"; // Household other financial assets
	private static final String RBA_E1_SERIESID_DWELLINGS = "BSPNSHNFD"; // Household dwellings
	private static final String RBA_E1_SERIESID_NONFIN_ASSETS = "BSPNSHNFT"; // Household total non-financial assets
	private static final String RBA_E1_SERIESID_TOTAL_LIABILITIES = "BSPNSHUL"; // Household total liabilities

	private static final String RBA_E2_SERIESID_DEBT_TO_INCOME = "BHFDDIT";
	private static final String RBA_E2_SERIESID_ASSETS_TO_INCOME = "BHFADIT";

	private static final String ABS_1410_ECONOMY_HOUSE_COUNT = "Houses - number of transfers";
	private static final String ABS_1410_ECONOMY_HOUSE_AMOUNT = "Houses - median sale price";
	private static final String ABS_1410_ECONOMY_UNIT_COUNT = "Attached Dwellings - number of transfers";
	private static final String ABS_1410_ECONOMY_UNIT_AMOUNT = "Attached Dwellings - median sale price";

	private static final String ABS_1410_FAMILY_MRERD_OVER_30 = "Households with mortgage repayments greater than or equal to 30% of household income";
	private static final String ABS_1410_FAMILY_RNTRD_OVER_30 = "Households with rent payments greater than or equal to 30% of household income";

	// beans
	private CalibrationData commonData;
	private CalibrationDataHousehold householdData;
	private CalibrateIndividuals calibrateIndividuals;
	private AreaMapping area;
	private AustralianEconomy economy;
	private Properties properties;

	// field variables
	private Random random;
	private Date calibrationDateAbs;
	private Date calibrationDateRba;
	private int totalPopulationAU;
	private double populationMultiplier;
	private Map<String, Integer> lgaPeopleCount; // adjusted to 2018
	private Map<String, Integer> lgaDwellingsCount; // adjusted to 2018
	private Map<String, Integer> poaIndexMap; // from Individual agent data
	private Map<String, Integer> lgaIndexMap;

	private static int agentNo = 0;

	// agents
	/**
	 * A simplified map to make it easier to populate Households.<br>
	 * Keys: LGA, AGE5P, INCP.<br>
	 * Values: an ArrayList of Individuals for that combination of keys.
	 */
	private Map<String, Map<String, Map<String, ArrayList<Individual>>>> individualMap;
	private ArrayList<Individual> individualAgents;
	private ArrayList<Household> householdAgents; // households should have an LGA
	/**
	 * A matrix of Household agents.<br>
	 * Keys: LGA code, HIND, CDCF Values: list of households in each LGA
	 */
	private ArrayList<ArrayList<ArrayList<ArrayList<Household>>>> householdMatrix;

	private static int agentId = 0;

	// data sets
	/**
	 * RBA E1 Household and Business Balance Sheets<br>
	 * 
	 * Contains high-level balance sheet amounts at a national level. This model
	 * uses the ratios between the line items to estimate the Household Bal Sht
	 * after using RBA E2 to link a few line items between the P&L and Bal Sht.<br>
	 * 
	 * Keys: Series ID, Date
	 */
	private Map<String, Map<Date, String>> rbaE1;
	/**
	 * RBA E2 Household Finances - Selected Ratios<br>
	 * 
	 * Contains Balance Sheet ratios. This model uses the debt-to-income and
	 * assets-to-income ratios to link the amounts in the P&L with those in the Bal
	 * Sht.<br>
	 * 
	 * Keys: Series ID, Date
	 */
	private Map<String, Map<Date, String>> rbaE2;
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

	/**
	 * Default constructor
	 */
	public CalibrateHouseholds() {
		super();
		this.init();
	}

	/**
	 * Works out household composition and how many of each to create, calibrates
	 * household financials, then adds them to the economy.
	 */
	public void createHouseholdAgents() {
		long memoryBefore = 0L; // for debugging memory consumption
		if (DEBUG) {
			System.gc();
			memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		}

		System.out.println(new Date(System.currentTimeMillis()) + ": Starting creation of Household agents");
		DecimalFormat integerFormatter = new DecimalFormat("#,##0");

		// set the calibration date
		DateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);
		try {
			this.calibrationDateAbs = sdf.parse(CALIBRATION_DATE_ABS);
			this.calibrationDateRba = sdf.parse(CALIBRATION_DATE_RBA);
		} catch (ParseException e) {
			e.printStackTrace();
		}

		// get raw calibration data
		this.abs1410_0Economy = this.householdData.getAbs1410_0Economy();
		this.abs1410_0Family = this.householdData.getAbs1410_0Family();
		this.rbaE1 = this.commonData.getRbaE1();
		this.rbaE2 = this.householdData.getRbaE2();
		this.censusHCFMD_LGA_HIND_RNTRD = this.householdData.getCensusHCFMD_LGA_HIND_RNTRD();
		this.censusHCFMD_LGA_HIND_MRERD = this.householdData.getCensusHCFMD_LGA_HIND_MRERD();
		this.censusHCFMF_LGA_FINF_CDCF = this.householdData.getCensusHCFMF_LGA_FINF_CDCF();
		this.random = this.properties.getRandom();
		this.totalPopulationAU = this.calibrateIndividuals.getTotalPopulationAU();

		// get key metrics that will be used across all the data
		this.lgaDwellingsCount = this.area.getAdjustedDwellingsByLga(this.calibrationDateAbs);
		this.populationMultiplier = this.area.getPopulationMultiplier(this.calibrationDateAbs);

		// create list of Individuals with enough initial capacity
		if (this.individualAgents == null) {
			// add in a 5% buffer so the List doesn't end up double the size it needs to be
			int initCapacity = (int) Math.round(this.totalPopulationAU * 1.05d);
			this.individualAgents = new ArrayList<Individual>(initCapacity);
		}

		// get list of LGAs from the matrix of Individuals
		this.poaIndexMap = this.calibrateIndividuals.getPoaIndexMap();
		Set<String> poaCodes = this.poaIndexMap.keySet();
		Set<String> lgaCodesIndividual = new HashSet<String>();
		for (String poa : poaCodes) {
			lgaCodesIndividual.add(this.area.getLgaCodeFromPoa(poa));
		}
		// Keys: HIND, RNTRD, LGA, HCFMD
		Set<String> lgaCodesRNTRD = this.censusHCFMD_LGA_HIND_RNTRD.get(ABS_HIND_RANGES[0]).get(ABS_RNTRD_RANGES[0])
				.keySet();
		// Keys: HIND, MRERD, LGA, HCFMD
		Set<String> lgaCodesMRERD = this.censusHCFMD_LGA_HIND_MRERD.get(ABS_HIND_RANGES[0]).get(ABS_MRERD_RANGES[0])
				.keySet();
		// Keys: FINF, CDCF, LGA, HCFMD
		Set<String> lgaCodesCDCF = this.censusHCFMF_LGA_FINF_CDCF.get(ABS_FINF_RANGES[0]).get(ABS_CDCF[0]).keySet();

		// find intersection of LGA codes for household and individual data
		Set<String> lgaCodesIntersection = new HashSet<String>(lgaCodesIndividual);
		lgaCodesIntersection.addAll(lgaCodesRNTRD);
		lgaCodesIntersection.addAll(lgaCodesMRERD);
		lgaCodesIntersection.addAll(lgaCodesCDCF);
		lgaCodesIntersection.retainAll(lgaCodesIndividual);
		lgaCodesIntersection.retainAll(lgaCodesRNTRD);
		lgaCodesIntersection.retainAll(lgaCodesMRERD);
		lgaCodesIntersection.retainAll(lgaCodesCDCF);
		String[] lgaCodes = new String[lgaCodesIntersection.size()];
		{ // limit the scope of lgaIdx
			int lgaIdx = 0;
			for (String lga : lgaCodesIntersection) {
				lgaCodes[lgaIdx] = lga;
				lgaIdx++;
			}
		}
		// lgaCodes = Arrays.asList(lgaCodesIntersection).toArray(lgaCodes);
		// FIXME: java.lang.ArrayStoreException: arraycopy: type mismatch: can not copy
		// java.util.Set[] into java.lang.String[]

		// create the Household agent objects
		this.householdAgents = new ArrayList<Household>(AGENT_LIST_INIT_SIZE);
		// Keys: LGA, HIND, CDCF, then values are a list of households in that cell
		this.householdMatrix = new ArrayList<ArrayList<ArrayList<ArrayList<Household>>>>(lgaCodesIntersection.size());
		for (int lgaIdx = 0; lgaIdx < lgaCodesIntersection.size(); lgaIdx++) {
			this.householdMatrix.add(new ArrayList<ArrayList<ArrayList<Household>>>(ABS_HIND_RANGES.length));
			for (int hindIdx = 0; hindIdx < ABS_HIND_RANGES.length; hindIdx++) {
				this.householdMatrix.get(lgaIdx).add(new ArrayList<ArrayList<Household>>(ABS_CDCF.length));
				for (int cdcfIdx = 0; cdcfIdx < ABS_CDCF.length; cdcfIdx++) {
					this.householdMatrix.get(lgaIdx).get(hindIdx)
							.add(new ArrayList<Household>(AGENT_LIST_CDCF_INIT_SIZE));
				}
			}
		}

		/**
		 * ROUGH ALGORITHM:
		 * 
		 * Don't try to map the family compositions in CDCF to the family compositions
		 * in HCFMD ... just use HCFMD to create ratios/multipliers, then apply them to
		 * the CDCF data. Use CDCF for the families with kids, but use HCFMD for the
		 * other families because it has more detail on lone person, group, etc.
		 * Everything else is identical, so it should be an easy mapping. Use the PDF
		 * sampling method from IndividualCalibration so that the counts are correct.
		 * 
		 * If we sample from the RNTRD and MRERD PDFs using rand and (1 - rand) it will
		 * make it more likely for a household to be paying either rent or a mortgage
		 * but not both, with a small degree of overlap in the middle. I can't get the
		 * number of dwellings in each MRERD and RNTRD range because my Basic account on
		 * the Census data tables website is limited to 40 million cells, and that
		 * degree of granularity results in too many cells for my free account. In the
		 * absence of real data, I'm happy to accept whatever correlation between high
		 * and low mortgage/rent payments happens to eventuate using this method. It
		 * will tend to put higher mortgage repayments with lower rent payments, and
		 * vice-versa.
		 * 
		 * N.B. At the end, if an Individual is not assigned to a Household (even a lone
		 * person Household) then they should be removed from the List of Individuals so
		 * they're not included in Clearing Payments Vector calculations. Alternatively,
		 * any remaining Individuals could be assigned to a group Household.
		 * 
		 */
		// TODO: put Individuals into a map that's easier to use when populating
		// Households
		// Keys: LGA, AGE5P, INCP
		this.individualMap = this.calibrateIndividuals.getIndividualMap();

		// PDF Keys: LGA, HCFMD, HIND, RNTRD/MRERD midpoints
		double[][][][] pdfRntrd = new double[lgaCodesIntersection.size()][ABS_HCFMF.length
				+ 1][ABS_HIND_RANGES.length][ABS_RNTRD_MIDPOINT.length];
		double[][][][] pdfMrerd = new double[lgaCodesIntersection.size()][ABS_HCFMF.length
				+ 1][ABS_HIND_RANGES.length][ABS_MRERD_MIDPOINT.length];
		// Map<String, Integer> lgaIndexMap = new HashMap<String, Integer>(
		// (int) Math.ceil(lgaCodesIntersection.size() / MAP_LOAD_FACTOR) + 1);
		System.out.println(new Date(System.currentTimeMillis()) + ": Creating households for each LGA");
		for (int lgaIdx = 0; lgaIdx < lgaCodes.length; lgaIdx++) {
			System.out.print(".");
			String lgaCode = lgaCodes[lgaIdx];
			// lgaIndexMap.put(lgaCode, lgaIdx);

			// family members must live in the same LGA, so these must be within LGA loop
			// create count-weighted probability density functions (PDF)

			// children are <20 years old (indexes 0 to 3)
			double[] pdfAgeChild = new double[4];
			double[][] pdfIncpGivenAgeChild = new double[4][CalibrateIndividuals.INDIVIDUAL_INCOME_RANGES_ABS.length];
			int divisor = 0;
			for (int ageIdx = 0; ageIdx < 4; ageIdx++) {
				// initialise PDF for INCP given AGE5P
				for (int incpIdx = 0; incpIdx < CalibrateIndividuals.INDIVIDUAL_INCOME_RANGES_ABS.length; incpIdx++) {
					pdfIncpGivenAgeChild[ageIdx][incpIdx] = 0d;
				}
				// calculate values
				int numInAge = 0;
				for (int incpIdx = 0; incpIdx < CalibrateIndividuals.INDIVIDUAL_INCOME_RANGES_ABS.length; incpIdx++) {
					String incp = CalibrateIndividuals.INDIVIDUAL_INCOME_RANGES_ABS[incpIdx];
					if (this.individualMap.get(lgaCode).containsKey(CalibrateIndividuals.AGE_ARRAY_ABS[ageIdx])
							&& this.individualMap.get(lgaCode).get(CalibrateIndividuals.AGE_ARRAY_ABS[ageIdx])
									.containsKey(incp)) {
						// add the number of people in this cell, if this cell contains data
						int numInCell = this.individualMap.get(lgaCode).get(CalibrateIndividuals.AGE_ARRAY_ABS[ageIdx])
								.get(incp).size();
						pdfIncpGivenAgeChild[ageIdx][incpIdx] = Double.valueOf(numInCell);
						numInAge += numInCell;
					}
				}
				for (int incpIdx = 0; incpIdx < CalibrateIndividuals.INDIVIDUAL_INCOME_RANGES_ABS.length; incpIdx++) {
					if (numInAge == 0) {
						pdfIncpGivenAgeChild[ageIdx][incpIdx] = 0d;
					} else {
						pdfIncpGivenAgeChild[ageIdx][incpIdx] = pdfIncpGivenAgeChild[ageIdx][incpIdx]
								/ Double.valueOf(numInAge);
					}
				}
				pdfAgeChild[ageIdx] = Double.valueOf(numInAge);
				divisor += numInAge;
			}
			for (int ageIdx = 0; ageIdx < 3; ageIdx++) {
				// there should always be people under 20 years old in every LGA, so no need to
				// check that divisor != 0
				pdfAgeChild[ageIdx] = pdfAgeChild[ageIdx] / (double) divisor;
			}
			pdfAgeChild[3] = 1d - pdfAgeChild[0] - pdfAgeChild[1] - pdfAgeChild[2];

			// parents are 20-50 years old (indexes 4 to 9)
			double[] pdfAgeParent = new double[6];
			divisor = 0;
			for (int ageIdx = 4; ageIdx < 10; ageIdx++) {
				int numInAge = 0;
				for (String incp : CalibrateIndividuals.INDIVIDUAL_INCOME_RANGES_ABS) {
					if (this.individualMap.get(lgaCode).containsKey(CalibrateIndividuals.AGE_ARRAY_ABS[ageIdx])
							&& this.individualMap.get(lgaCode).get(CalibrateIndividuals.AGE_ARRAY_ABS[ageIdx])
									.containsKey(incp)) {
						// add the number of people in this cell, if this cell contains data
						numInAge += this.individualMap.get(lgaCode).get(CalibrateIndividuals.AGE_ARRAY_ABS[ageIdx])
								.get(incp).size();
					}
				}
				pdfAgeParent[ageIdx - 4] = Double.valueOf(numInAge);
				divisor += numInAge;
			}
			for (int ageIdx = 0; ageIdx < 5; ageIdx++) {
				pdfAgeParent[ageIdx] = pdfAgeParent[ageIdx] / (double) divisor;
			}
			pdfAgeParent[5] = 1d - pdfAgeParent[0] - pdfAgeParent[1] - pdfAgeParent[2] - pdfAgeParent[3]
					- pdfAgeParent[4];

			// adults are >= 20 years old (indexes 4 to 20)
			double[] pdfAgeAdult = new double[17];
			divisor = 0;
			for (int ageIdx = 4; ageIdx < 21; ageIdx++) {
				int numInAge = 0;
				for (String incp : CalibrateIndividuals.INDIVIDUAL_INCOME_RANGES_ABS) {
					if (this.individualMap.get(lgaCode).containsKey(CalibrateIndividuals.AGE_ARRAY_ABS[ageIdx])
							&& this.individualMap.get(lgaCode).get(CalibrateIndividuals.AGE_ARRAY_ABS[ageIdx])
									.containsKey(incp)) {
						// add the number of poeple in this cell, if this cell contains data
						numInAge += this.individualMap.get(lgaCode).get(CalibrateIndividuals.AGE_ARRAY_ABS[ageIdx])
								.get(incp).size();
					}
				}
				pdfAgeAdult[ageIdx - 4] = Double.valueOf(numInAge);
				divisor += numInAge;
			}
			for (int ageIdx = 0; ageIdx < 16; ageIdx++) {
				pdfAgeAdult[ageIdx] = pdfAgeAdult[ageIdx] / (double) divisor;
			}
			pdfAgeAdult[16] = 1d - pdfAgeAdult[0] - pdfAgeAdult[1] - pdfAgeAdult[2] - pdfAgeAdult[3] - pdfAgeAdult[4]
					- pdfAgeAdult[5] - pdfAgeAdult[6] - pdfAgeAdult[7] - pdfAgeAdult[8] - pdfAgeAdult[9]
					- pdfAgeAdult[10] - pdfAgeAdult[11] - pdfAgeAdult[12] - pdfAgeAdult[13] - pdfAgeAdult[14]
					- pdfAgeAdult[15];

			// Keys: age, income
			Map<String, Map<String, Boolean>> makeCopies = new HashMap<String, Map<String, Boolean>>(
					(int) Math.ceil(CalibrateIndividuals.AGE_ARRAY_ABS.length / MAP_LOAD_FACTOR) + 1);
			Map<String, Map<String, Integer>> nextIndex = new HashMap<String, Map<String, Integer>>(
					(int) Math.ceil(CalibrateIndividuals.AGE_ARRAY_ABS.length / MAP_LOAD_FACTOR) + 1);
			Map<String, Map<String, List<Integer>>> randomIndividualIdx = new HashMap<String, Map<String, List<Integer>>>(
					(int) Math.ceil(CalibrateIndividuals.AGE_ARRAY_ABS.length / MAP_LOAD_FACTOR) + 1);
			for (String age : CalibrateIndividuals.AGE_ARRAY_ABS) {
				makeCopies.put(age,
						new HashMap<String, Boolean>(CalibrateIndividuals.INDIVIDUAL_INCOME_RANGES_ABS.length));
				nextIndex.put(age,
						new HashMap<String, Integer>(CalibrateIndividuals.INDIVIDUAL_INCOME_RANGES_ABS.length));
				randomIndividualIdx.put(age,
						new HashMap<String, List<Integer>>(CalibrateIndividuals.INDIVIDUAL_INCOME_RANGES_ABS.length));
				for (String incp : CalibrateIndividuals.INDIVIDUAL_INCOME_RANGES_ABS) {
					makeCopies.get(age).put(incp, false);
					nextIndex.get(age).put(incp, 0);
					int numIndividualsInCell = 0;
					if (this.individualMap.containsKey(lgaCode) && this.individualMap.get(lgaCode).containsKey(age)
							&& this.individualMap.get(lgaCode).get(age).containsKey(incp)) {
						numIndividualsInCell = this.individualMap.get(lgaCode).get(age).get(incp).size();
					}
					List<Integer> range = IntStream.rangeClosed(0, numIndividualsInCell - 1).boxed()
							.collect(Collectors.toList());
					Collections.shuffle(range, this.random);
					randomIndividualIdx.get(age).put(incp, range);
				}
			}

			// combine MRERD and RNTRD data, and calculate ratios/multipliers. MRERD and
			// RNTRD ratios for all categories, and family composition ratios for the
			// non-kids family types (the ones that map to N/A in CDCF).
			for (int hindIdx = 0; hindIdx < ABS_HIND_RANGES.length; hindIdx++) {
				String hind = ABS_HIND_RANGES[hindIdx];
				for (int hcfmdIdx = 0; hcfmdIdx < ABS_HCFMF.length; hcfmdIdx++) {
					// N.B. Using HCFMF array length to iterate over HCFMD data due to the 4 extra
					// categories in HCFMD discussed below.
					String hcfmd = ABS_HCFMD[hcfmdIdx];

					// N.B. The RNTRD and MRERD data sometimes have different total counts, so take
					// the max and use it when determining the ratios.
					// Keys: HIND, RNTRD, LGA, HCFMD
					int totalDwellingsRntrd = 0;
					for (int rntrdIdx = 0; rntrdIdx < ABS_RNTRD_RANGES.length; rntrdIdx++) {
						String rntrd = ABS_RNTRD_RANGES[rntrdIdx];
						totalDwellingsRntrd += this.censusHCFMD_LGA_HIND_RNTRD.get(hind).get(rntrd).get(lgaCode)
								.get(hcfmd);
					}
					// Keys: HIND, MRERD, LGA, HCFMD
					int totalDwellingsMrerd = 0;
					for (int mrerdIdx = 0; mrerdIdx < ABS_MRERD_RANGES.length; mrerdIdx++) {
						String mrerd = ABS_MRERD_RANGES[mrerdIdx];
						totalDwellingsMrerd += this.censusHCFMD_LGA_HIND_MRERD.get(hind).get(mrerd).get(lgaCode)
								.get(hcfmd);
					}
					int totalDwellingsCell = Math.max(totalDwellingsRntrd, totalDwellingsMrerd);

					/*
					 * HCFMD has these additional categories compared to HCFMF: "Lone person
					 * household", "Group household", "Visitors only household", "Other
					 * non-classifiable household".
					 * 
					 * To simplify, use the ratio between "Lone person household" and
					 * "Group household" to split the count in the "Not applicable" category in the
					 * HCFMF data. To ensure the family count doesn't drop data by rounding down,
					 * multiply by the ratio to get the number of "Lone person" households, then
					 * subtract that number from the total count of "Not applicable" households from
					 * the HCFMF data to ensure that the model ends up with the same total number of
					 * Households.
					 */
					double lonePersonRatio = 0d;
					int lonePersonCountRntrd = 0;
					int lonePersonCountMrerd = 0;
					int loneAndGroupCountRntrd = 0;
					int loneAndGroupCountMrerd = 0;
					if (hcfmdIdx == ABS_HCFMF.length - 1) {
						String hcfmdLone = hcfmd;
						String hcfmdGroup = ABS_HCFMD[hcfmdIdx + 1];
						for (int rntrdIdx = 0; rntrdIdx < ABS_RNTRD_RANGES.length; rntrdIdx++) {
							String rntrd = ABS_RNTRD_RANGES[rntrdIdx];
							int lone = this.censusHCFMD_LGA_HIND_RNTRD.get(hind).get(rntrd).get(lgaCode).get(hcfmdLone);
							int grp = this.censusHCFMD_LGA_HIND_RNTRD.get(hind).get(rntrd).get(lgaCode).get(hcfmdGroup);
							lonePersonCountRntrd += lone;
							loneAndGroupCountRntrd += lone + grp;
						}
						for (int mrerdIdx = 0; mrerdIdx < ABS_MRERD_RANGES.length; mrerdIdx++) {
							String mrerd = ABS_MRERD_RANGES[mrerdIdx];
							int lone = this.censusHCFMD_LGA_HIND_MRERD.get(hind).get(mrerd).get(lgaCode).get(hcfmdLone);
							int grp = this.censusHCFMD_LGA_HIND_MRERD.get(hind).get(mrerd).get(lgaCode).get(hcfmdGroup);
							lonePersonCountMrerd += lone;
							loneAndGroupCountMrerd += lone + grp;
						}
						// "Lone person household" category in the HCFMD data
						if ((lonePersonCountRntrd + lonePersonCountMrerd) == 0) {
							lonePersonRatio = 0d;
						} else if ((loneAndGroupCountRntrd + loneAndGroupCountMrerd) == 0) {
							lonePersonRatio = 1d;
						} else {
							lonePersonRatio = ((double) (lonePersonCountRntrd + lonePersonCountMrerd))
									/ ((double) (loneAndGroupCountRntrd + loneAndGroupCountMrerd));
						}
					} // END IF lone person ratio calc

					// calculate PDF for RNTRD
					double restOfCell = 0d;
					for (int rntrdIdx = 1; rntrdIdx < ABS_RNTRD_MIDPOINT.length; rntrdIdx++) {
						String rntrd = ABS_RNTRD_RANGES[rntrdIdx];
						pdfRntrd[lgaIdx][hcfmdIdx][hindIdx][rntrdIdx] = ((double) this.censusHCFMD_LGA_HIND_RNTRD
								.get(hind).get(rntrd).get(lgaCode).get(hcfmd)) / (double) totalDwellingsCell;
						restOfCell += pdfRntrd[lgaIdx][hcfmdIdx][hindIdx][rntrdIdx];
					}
					pdfRntrd[lgaIdx][hcfmdIdx][hindIdx][0] = 1d - restOfCell; // map "Not stated" and "Not applicable"
																				// into the $0 category
					// calculate PDF for MRERD
					restOfCell = 0d;
					for (int mrerdIdx = 1; mrerdIdx < ABS_MRERD_MIDPOINT.length; mrerdIdx++) {
						String mrerd = ABS_MRERD_RANGES[mrerdIdx];
						pdfMrerd[lgaIdx][hcfmdIdx][hindIdx][mrerdIdx] = this.censusHCFMD_LGA_HIND_MRERD.get(hind)
								.get(mrerd).get(lgaCode).get(hcfmd);
						restOfCell += pdfMrerd[lgaIdx][hcfmdIdx][hindIdx][mrerdIdx];
					}
					pdfMrerd[lgaIdx][hcfmdIdx][hindIdx][0] = 1d - restOfCell; // map "Not stated" and "Not applicable"
																				// into the $0 category

					// add lone person & group household PDF calcs
					if (hcfmdIdx == ABS_HCFMF.length - 1) {
						String hcfmdLone = hcfmd;
						String hcfmdGroup = ABS_HCFMD[hcfmdIdx + 1];

						// calculate PDF for RNTRD
						restOfCell = 0d;
						for (int rntrdIdx = 1; rntrdIdx < ABS_RNTRD_MIDPOINT.length; rntrdIdx++) {
							String rntrd = ABS_RNTRD_RANGES[rntrdIdx];
							pdfRntrd[lgaIdx][hcfmdIdx][hindIdx][rntrdIdx] = ((double) this.censusHCFMD_LGA_HIND_RNTRD
									.get(hind).get(rntrd).get(lgaCode).get(hcfmdLone)) / (double) totalDwellingsCell;
							restOfCell += pdfRntrd[lgaIdx][hcfmdIdx][hindIdx][rntrdIdx];
						}
						pdfRntrd[lgaIdx][hcfmdIdx][hindIdx][0] = 1d - restOfCell; // map "Not stated" and "Not
																					// applicable"
						restOfCell = 0d;
						for (int rntrdIdx = 1; rntrdIdx < ABS_RNTRD_MIDPOINT.length; rntrdIdx++) {
							String rntrd = ABS_RNTRD_RANGES[rntrdIdx];
							pdfRntrd[lgaIdx][hcfmdIdx
									+ 1][hindIdx][rntrdIdx] = ((double) this.censusHCFMD_LGA_HIND_RNTRD.get(hind)
											.get(rntrd).get(lgaCode).get(hcfmdGroup)) / (double) totalDwellingsCell;
							restOfCell += pdfRntrd[lgaIdx][hcfmdIdx + 1][hindIdx][rntrdIdx];
						}
						pdfRntrd[lgaIdx][hcfmdIdx + 1][hindIdx][0] = 1d - restOfCell; // map "Not stated" and "Not
																						// applicable"

						// calculate PDF for MRERD
						restOfCell = 0d;
						for (int mrerdIdx = 1; mrerdIdx < ABS_MRERD_MIDPOINT.length; mrerdIdx++) {
							String mrerd = ABS_MRERD_RANGES[mrerdIdx];
							pdfRntrd[lgaIdx][hcfmdIdx][hindIdx][mrerdIdx] = ((double) this.censusHCFMD_LGA_HIND_MRERD
									.get(hind).get(mrerd).get(lgaCode).get(hcfmdLone)) / (double) totalDwellingsCell;
							restOfCell += pdfMrerd[lgaIdx][hcfmdIdx][hindIdx][mrerdIdx];
						}
						pdfRntrd[lgaIdx][hcfmdIdx][hindIdx][0] = 1d - restOfCell; // map "Not stated" and "Not
																					// applicable"
						restOfCell = 0d;
						for (int mrerdIdx = 1; mrerdIdx < ABS_MRERD_MIDPOINT.length; mrerdIdx++) {
							String mrerd = ABS_MRERD_RANGES[mrerdIdx];
							pdfMrerd[lgaIdx][hcfmdIdx
									+ 1][hindIdx][mrerdIdx] = ((double) this.censusHCFMD_LGA_HIND_MRERD.get(hind)
											.get(mrerd).get(lgaCode).get(hcfmdGroup)) / (double) totalDwellingsCell;
							restOfCell += pdfMrerd[lgaIdx][hcfmdIdx + 1][hindIdx][mrerdIdx];
						}
						pdfRntrd[lgaIdx][hcfmdIdx + 1][hindIdx][0] = 1d - restOfCell; // map "Not stated" and "Not
																						// applicable"
					} // end lone person & group household special case

					/*
					 * That's the end of the logic to create PDFs for RNTRD/MRERD data. Next step is
					 * to map to the CDCF data and cross-reference to calculate the number
					 * Households in each category. We can do this in the same loop because LGA is
					 * the same for both data sets, HIND is the same as FINF, and we're restricting
					 * HCFMD to the indices that are the same as HCFMF, and then splitting the last
					 * index value into lone person and group households using the ratios we just
					 * calculated.
					 */
					for (int cdcfIdx = 0; cdcfIdx < ABS_CDCF.length; cdcfIdx++) {
						String cdcf = ABS_CDCF[cdcfIdx];
						int numAdults = Math.min(ABS_CDCF_ADULT_COUNT[cdcfIdx], 2);
						int numChildren = Math.min(ABS_CDCF_CHILD_COUNT[cdcfIdx], 6);

						// get number of families
						int numCdcfCategories = (cdcfIdx == (ABS_CDCF.length - 1)) ? 2 : 1;
						List<Integer> hcfmdSplitIdx = new ArrayList<Integer>(numCdcfCategories);
						List<Integer> numFamilies = new ArrayList<Integer>(numCdcfCategories);

						// Census CDCF Keys: FINF, CDCF, LGA, HCFMF
						if (this.censusHCFMF_LGA_FINF_CDCF.containsKey(hind)
								&& this.censusHCFMF_LGA_FINF_CDCF.get(hind).containsKey(cdcf)
								&& this.censusHCFMF_LGA_FINF_CDCF.get(hind).get(cdcf).containsKey(lgaCode)
								&& this.censusHCFMF_LGA_FINF_CDCF.get(hind).get(cdcf).get(lgaCode).containsKey(hcfmd)) {
							int numFamiliesInCell = this.censusHCFMF_LGA_FINF_CDCF.get(hind).get(cdcf).get(lgaCode)
									.get(hcfmd);

							// if it's the last HCFMD index, split between lone person & group households
							if (cdcfIdx == ABS_CDCF.length - 1) {
								int hcfmdIdxLone = hcfmdIdx;
								int hcfmdIdxGroup = hcfmdIdx + 1;
								hcfmdSplitIdx.add(hcfmdIdxLone);
								hcfmdSplitIdx.add(hcfmdIdxGroup);
								numFamilies.add((int) Math.round(lonePersonRatio * Double.valueOf(numFamiliesInCell)));
								numFamilies.add(numFamiliesInCell - numFamilies.get(0));
							} else {
								hcfmdSplitIdx.add(hcfmdIdx);
								numFamilies.add(numFamiliesInCell);
							}
						}
						// FIXME: up to here splitting lone/group households

						// randomly sample from PDFs for RNTRD and MRERD
						for (int cdcfSplit = 0; cdcfSplit < numCdcfCategories; cdcfSplit++) {
							for (int familyNum = 0; familyNum < numFamilies.get(cdcfSplit); familyNum++) {
								Household household = new Household();

								household.setNumAdults(numAdults);
								household.setNumChildren(numChildren);

								// determine Henderson poverty line based on family composition
								double henderson = 0d;
								if (cdcfIdx == ABS_CDCF.length - 1) {
									if (cdcfSplit == 0) {
										// lone person household (same as single parent with no children)
										henderson = HENDERSON_EXCL_HOUSING_LONE_PERSON;
									} else {
										// group household (assume four adults, no children)
										henderson = HENDERSON_EXCL_HOUSING_GROUP;
									}
								} else {
									if (numAdults > 1) {
										// couple
										henderson = CDCF_HENDERSON_EXCL_HOUSING[numChildren];
									} else {
										// single
										henderson = CDCF_HENDERSON_EXCL_HOUSING[numChildren + 7];
									}
								}
								household.setPnlLivingExpenses(henderson); // non-discretionary living expenses

								double rand = this.random.nextDouble();
								int attributeIdx = CustomMath
										.sample(pdfMrerd[lgaIdx][hcfmdSplitIdx.get(cdcfSplit)][hindIdx], rand);
								household.setPnlMortgageRepayments(ABS_MRERD_MIDPOINT[attributeIdx]);
								attributeIdx = CustomMath
										.sample(pdfRntrd[lgaIdx][hcfmdSplitIdx.get(cdcfSplit)][hindIdx], 1d - rand);
								household.setPnlRentExpense(ABS_RNTRD_MIDPOINT[attributeIdx]);

								List<Individual> members = new ArrayList<Individual>(numAdults + numChildren);
								int firstAdultIncpIdx = 0;
								// assign adults
								if (numAdults == 1) {
									// get random age for an adult
									int ageIdx = 0;
									if (numChildren == 0) {
										// get random age for an adult
										ageIdx = CustomMath.sample(pdfAgeAdult, this.random) + 4;
									} else {
										// get random age for a parent
										ageIdx = CustomMath.sample(pdfAgeParent, this.random) + 4;
									}
									String age = CalibrateIndividuals.AGE_ARRAY_ABS[ageIdx];

									// FIXME: need to ensure there are people in the chosen cell
									/*
									 * Choose the preferred index. Then in a loop, check if there are any people in
									 * this cell. If there are, that's the chosen index. If not, increment/decrement
									 * the index and try again. Repeat until a populated cell is found.
									 * 
									 * N.B. Age is chosen from a PDF, so there should definitely be people in that
									 * age range for this LGA - it's just the income ranges that aren't guaranteed.
									 */
									// income brackets align fairly well
									String incp = null;
									{ // limit the scope of idx
										int idx = 0;
										// determine the best starting point, based on income mapping
										if (hindIdx < 14) {
											// 1:1 mapping between income brackets
											idx = hindIdx;
										} else if (hindIdx == 14) {
											// 2:1 mapping between income brackets
											idx = 13;
										} else {
											// all higher FINF brackets map to the highest INCP
											idx = 14;
										}
										// now loop through, searching the cells to find one with people in it
										peopleCheck: for (int i = 0; i < 15; i++) {
											if (randomIndividualIdx.get(age).containsKey(
													CalibrateIndividuals.INDIVIDUAL_INCOME_RANGES_ABS[(idx + i) % 15])
													&& randomIndividualIdx.get(age).get(
															CalibrateIndividuals.INDIVIDUAL_INCOME_RANGES_ABS[(idx + i)
																	% 15])
															.size() > 0) {
												idx = (idx + i) % 15;
												break peopleCheck;
											}
										}
										incp = CalibrateIndividuals.INDIVIDUAL_INCOME_RANGES_ABS[idx];
										firstAdultIncpIdx = idx;
									} // end limit the scope of idx

									// get next random individual in this LGA, AGE5P & INCP category
									int nextIdx = 0;
									if (randomIndividualIdx.get(age).get(incp).size() > 0) {
										// stay within the List bounds
										nextIdx = nextIndex.get(age).get(incp)
												% randomIndividualIdx.get(age).get(incp).size();
									}



									int nextIndividualIdx = randomIndividualIdx.get(age).get(incp).get(nextIdx);
									Individual newFamilyMember = null;
									if (makeCopies.get(age).get(incp)) {
										// we're iterating over the list a second time (or more), so make a copy of the
										// Individual so we don't have multiple Households pointing to the same object
										newFamilyMember = new Individual(this.individualMap.get(lgaCode).get(age)
												.get(incp).get(nextIndividualIdx));
									} else {
										// use original Individual instance
										newFamilyMember = this.individualMap.get(lgaCode).get(age).get(incp)
												.get(nextIndividualIdx);
									}
									members.add(newFamilyMember);
								} else {
									// multiple adults, so income brackets not so well behaved
									for (int adultNo = 0; adultNo < numAdults; adultNo++) {
										// get random age for an adult
										int ageIdx = 0;
										if (adultNo > 0) {
											// base the subsequent adults' ages on the previous adult's age
											int adultOneAgeIdx = (members.get(members.size() - 1).getAge() + 3) / 5 - 1;
											if (adultOneAgeIdx == 4) {
												// other adult is older (younger would be a child)
												ageIdx = adultOneAgeIdx + 1;
											} else if (adultOneAgeIdx > 18) {
												// other adult is younger (older would probably be dead)
												ageIdx = adultOneAgeIdx - 1;
											}
										} else {
											// randomly choose an age
											if (numChildren == 0) {
												// get random age for an adult
												ageIdx = CustomMath.sample(pdfAgeAdult, this.random) + 4;
											} else {
												// get random age for a parent
												ageIdx = CustomMath.sample(pdfAgeParent, this.random) + 4;
											}
										}
										String age = CalibrateIndividuals.AGE_ARRAY_ABS[ageIdx];

										// income brackets do not align so well
										String incp = null;
										if (hindIdx < 3) {
											// negative and nil combined into the less than $7,800 category
											firstAdultIncpIdx = 2;
											incp = CalibrateIndividuals.INDIVIDUAL_INCOME_RANGES_ABS[2];
										} else if (hindIdx > 19) {
											// family income over $312k means two individuals over $156k
											firstAdultIncpIdx = 14;
											incp = CalibrateIndividuals.INDIVIDUAL_INCOME_RANGES_ABS[14];
										} else {
											// everything in between can be a complicated combination
											if (adultNo == 1) {
												// Randomly choose an income from the range of values that allows the
												// second
												// adult to bring the family income up to the right range.
												int incpIdx = 0;
												switch (hindIdx) { // for the relevant HIND/FINF income range
												case 3:
													incpIdx = 3;
													break;
												case 4:
													incpIdx = this.random.nextInt(2) + 3;
													break;
												case 5:
													incpIdx = this.random.nextInt(3) + 3;
													break;
												case 6:
													incpIdx = this.random.nextInt(4) + 3;
													break;
												case 7:
													incpIdx = this.random.nextInt(5) + 3;
													break;
												case 8:
													incpIdx = this.random.nextInt(6) + 3;
													break;
												case 9:
													incpIdx = this.random.nextInt(7) + 3;
													break;
												case 10:
													incpIdx = this.random.nextInt(8) + 3;
													break;
												case 11:
													incpIdx = this.random.nextInt(9) + 3;
													break;
												case 12:
													incpIdx = this.random.nextInt(10) + 3;
													break;
												case 13:
													incpIdx = this.random.nextInt(11) + 3;
													break;
												case 14:
													incpIdx = this.random.nextInt(11) + 3;
													break;
												case 15:
													incpIdx = this.random.nextInt(12) + 3;
													break;
												case 16:
													incpIdx = this.random.nextInt(10) + 5;
													break;
												case 17:
													incpIdx = this.random.nextInt(7) + 8;
													break;
												case 18:
													incpIdx = this.random.nextInt(5) + 10;
													break;
												default: // case 19:
													incpIdx = this.random.nextInt(2) + 13;
													break;
												}
												firstAdultIncpIdx = incpIdx;
												incp = CalibrateIndividuals.INDIVIDUAL_INCOME_RANGES_ABS[incpIdx];
											} else {
												// Choose the income that makes the family income correct, given the
												// first
												// adult's income. N.B. Assumes two adults.
												int incpIdx = 0;
												switch (hindIdx) { // for the relevant HIND/FINF income range
												case 3: // HIND $7800
													incpIdx = 2;
													break;
												case 4: // HIND $15600
													switch (firstAdultIncpIdx) {
													case 3:
														incpIdx = 3;
													default: // case 4
														incpIdx = 2;
													}
													break;
												case 5: // HIND $20800
													switch (firstAdultIncpIdx) {
													case 3:
														incpIdx = 4;
													case 4:
														incpIdx = 3;
													default: // case 5
														incpIdx = 2;
													}
													break;
												case 6: // HIND $26000
													switch (firstAdultIncpIdx) {
													case 3:
														incpIdx = 5;
													case 4:
													case 5:
														incpIdx = 3;
													default: // case 6
														incpIdx = 2;
													}
													break;
												case 7: // HIND $33800
													switch (firstAdultIncpIdx) {
													case 3:
														incpIdx = 6;
													case 4:
													case 5:
														incpIdx = 4;
													case 6:
														incpIdx = 3;
													default: // case 7
														incpIdx = 2;
													}
													break;
												case 8: // HIND $41600
													switch (firstAdultIncpIdx) {
													case 3:
														incpIdx = 7;
													case 4:
														incpIdx = 6;
													case 5:
														incpIdx = 5;
													case 6:
														incpIdx = 4;
													case 7:
														incpIdx = 3;
													default: // case 8
														incpIdx = 2;
													}
													break;
												case 9: // HIND $52k
													switch (firstAdultIncpIdx) {
													case 3:
														incpIdx = 8;
													case 4:
													case 5:
														incpIdx = 7;
													case 6:
														incpIdx = 6;
													case 7:
														incpIdx = 4;
													case 8:
														incpIdx = 3;
													default: // case 9
														incpIdx = 2;
													}
													break;
												case 10: // HIND $65k
													switch (firstAdultIncpIdx) {
													case 3:
													case 4:
														incpIdx = 9;
													case 5:
													case 6:
														incpIdx = 8;
													case 7:
														incpIdx = 7;
													case 8:
														incpIdx = 6;
													case 9:
														incpIdx = 3;
													default: // case 10
														incpIdx = 2;
													}
													break;
												case 11: // HIND $78k
													switch (firstAdultIncpIdx) {
													case 3:
													case 4:
														incpIdx = 10;
													case 5:
													case 6:
														incpIdx = 9;
													case 7:
													case 8:
														incpIdx = 8;
													case 9:
														incpIdx = 6;
													case 10:
														incpIdx = 3;
													default: // case 11
														incpIdx = 2;
													}
													break;
												case 12: // HIND $91k
													switch (firstAdultIncpIdx) {
													case 3:
													case 4:
														incpIdx = 11;
													case 5:
													case 6:
														incpIdx = 10;
													case 7:
													case 8:
														incpIdx = 9;
													case 9:
														incpIdx = 8;
													case 10:
														incpIdx = 6;
													case 11:
														incpIdx = 3;
													default: // case 12
														incpIdx = 2;
													}
													break;
												case 13: // HIND $104k
													switch (firstAdultIncpIdx) {
													case 3:
													case 4:
														incpIdx = 12;
													case 5:
													case 6:
														incpIdx = 11;
													case 7:
													case 8:
														incpIdx = 10;
													case 9:
														incpIdx = 9;
													case 10:
														incpIdx = 8;
													case 11:
														incpIdx = 6;
													case 12:
														incpIdx = 3;
													default: // case 13
														incpIdx = 2;
													}
													break;
												case 14: // HIND $130k
													switch (firstAdultIncpIdx) {
													case 3:
													case 4:
													case 5:
													case 6:
														incpIdx = 13;
													case 7:
													case 8:
													case 9:
														incpIdx = 12;
													case 10:
														incpIdx = 10;
													case 11:
													case 12:
														incpIdx = 9;
													default: // case 13
														incpIdx = 6;
													}
													break;
												case 15: // HIND $156k
													switch (firstAdultIncpIdx) {
													case 3:
													case 4:
													case 5:
													case 6:
													case 7:
													case 8:
													case 9:
														incpIdx = 13;
													case 10:
														incpIdx = 12;
													case 11:
														incpIdx = 11;
													case 12:
														incpIdx = 10;
													case 13:
														incpIdx = 9;
													default: // case 14
														incpIdx = 2;
													}
													break;
												case 16: // HIND $182k
													switch (firstAdultIncpIdx) {
													case 5:
													case 6:
													case 7:
														incpIdx = 14;
													case 8:
													case 9:
													case 10:
													case 11:
														incpIdx = 13;
													case 12:
														incpIdx = 12;
													case 13:
														incpIdx = 9;
													default: // case 14
														incpIdx = 6;
													}
													break;
												case 17: // HIND $208k
													switch (firstAdultIncpIdx) {
													case 8:
													case 9:
													case 10:
														incpIdx = 14;
													case 11:
													case 12:
													case 13:
														incpIdx = 13;
													default: // case 14
														incpIdx = 9;
													}
													break;
												case 18: // HIND $234k
													switch (firstAdultIncpIdx) {
													case 10:
													case 11:
														incpIdx = 14;
													case 12:
													case 13:
														incpIdx = 13;
													default: // case 14
														incpIdx = 11;
													}
													break;
												default: // case 19: // HIND $260k
													switch (firstAdultIncpIdx) {
													case 13:
														incpIdx = 14;
													default: // case 14
														incpIdx = 13;
													}
													break;
												}
												incp = CalibrateIndividuals.INDIVIDUAL_INCOME_RANGES_ABS[incpIdx];
											}
										} // end hindIdx if/switch for multiple adults

										// FIXME: up to here making households

										// get next random individual in this LGA, AGE5P & INCP category
										int nextIdx = 0;
										int nextIndividualIdx = 0;
										if (randomIndividualIdx.get(age).get(incp).size() > 0) {
											// stay within the List bounds
											nextIdx = nextIndex.get(age).get(incp)
													% randomIndividualIdx.get(age).get(incp).size();
											nextIndividualIdx = randomIndividualIdx.get(age).get(incp).get(nextIdx);
											nextIdx++;
											nextIndex.get(age).put(incp, nextIdx);
											if (nextIdx % randomIndividualIdx.get(age).get(incp).size() == 0) {
												// reached the upper bound of the List, so set the makeCopies flag to
												// true
												makeCopies.get(age).put(incp, true);
											}
										}
										Individual newFamilyMember = null;
										
										// FIXME: remove debugging code
										System.out.println("LGA: " + lgaCode);
										System.out.println("age: " + age);
										System.out.println("incp: " + incp);
										System.out.println("hindIdx: " + hindIdx);
										System.out.println("nextIdx: " + nextIdx);
										System.out.println("randomIndividualIdx.keySet(): " + randomIndividualIdx.keySet());
										System.out.println("randomIndividualIdx.get(age).keySet(): "
												+ randomIndividualIdx.get(age).keySet());
										System.out.println("randomIndividualIdx.get(age).get(incp): "
												+ randomIndividualIdx.get(age).get(incp));
										System.out.println("this.individualMap.get(lgaCode).keySet(): " + this.individualMap.get(lgaCode).keySet());
										System.out.println("this.individualMap.get(lgaCode).get(age).keySet(): " + this.individualMap.get(lgaCode).get(age).keySet());
										System.out.println("this.individualMap.get(lgaCode).get(age).get(incp): " + this.individualMap.get(lgaCode).get(age).get(incp));
										
										if (makeCopies.get(age).get(incp)) {
											// we're iterating over the list a second time (or more), so make a copy of
											// the Individual so we don't have multiple Households pointing to the same
											// object
											newFamilyMember = new Individual(this.individualMap.get(lgaCode).get(age)
													.get(incp).get(nextIndividualIdx));
										} else {
											// use original Individual instance
											newFamilyMember = this.individualMap.get(lgaCode).get(age).get(incp)
													.get(nextIndividualIdx);
										}
										members.add(newFamilyMember);
									} // end Adult Number loop
								} // end if numAdults == 1

								// assign children (assume under 20 years because dependent)
								for (int childNo = 0; childNo < numChildren; childNo++) {
									// get random age for a child
									int ageIdx = CustomMath.sample(pdfAgeChild, this.random);
									String age = CalibrateIndividuals.AGE_ARRAY_ABS[ageIdx];

									// children's income is immaterial, so just sample randomly
									int incpIdx = CustomMath.sample(pdfIncpGivenAgeChild[ageIdx], this.random);
									String incp = null;
									if (incpIdx < 14) {
										// 1:1 mapping between income brackets
										incp = CalibrateIndividuals.INDIVIDUAL_INCOME_RANGES_ABS[incpIdx];
									} else if (incpIdx == 14) {
										// 2:1 mapping between income brackets
										incp = CalibrateIndividuals.INDIVIDUAL_INCOME_RANGES_ABS[13];
									} else {
										// all higher FINF brackets map to the highest INCP
										incp = CalibrateIndividuals.INDIVIDUAL_INCOME_RANGES_ABS[14];
									}

									// get next random individual in this LGA, AGE5P & INCP category
									int nextIdx = nextIndex.get(age).get(incp)
											% randomIndividualIdx.get(age).get(incp).size(); // stay within the List
																								// bounds
									int nextIndividualIdx = randomIndividualIdx.get(age).get(incp).get(nextIdx);
									nextIdx++;
									nextIndex.get(age).put(incp, nextIdx);
									if (nextIdx % randomIndividualIdx.get(age).get(incp).size() == 0) {
										// reached the upper bound of the List, so set the makeCopies flag to true
										makeCopies.get(age).put(incp, true);
									}
									Individual newFamilyMember = null;
									
									// FIXME: remove debugging code
									System.out.println("LGA: " + lgaCode);
									System.out.println("age: " + age);
									System.out.println("incp: " + incp);
									System.out.println("hindIdx: " + hindIdx);
									System.out.println("nextIdx: " + nextIdx);
									System.out.println("randomIndividualIdx.keySet(): " + randomIndividualIdx.keySet());
									System.out.println("randomIndividualIdx.get(age).keySet(): "
											+ randomIndividualIdx.get(age).keySet());
									System.out.println("randomIndividualIdx.get(age).get(incp): "
											+ randomIndividualIdx.get(age).get(incp));
									System.out.println("this.individualMap.get(lgaCode).keySet(): " + this.individualMap.get(lgaCode).keySet());
									System.out.println("this.individualMap.get(lgaCode).get(age).keySet(): " + this.individualMap.get(lgaCode).get(age).keySet());
									System.out.println("this.individualMap.get(lgaCode).get(age).get(incp): " + this.individualMap.get(lgaCode).get(age).get(incp));
									
									if (makeCopies.get(age).get(incp)) {
										// we're iterating over the list a second time (or more), so make a copy of the
										// Individual so we don't have multiple Households pointing to the same object
										newFamilyMember = new Individual(this.individualMap.get(lgaCode).get(age)
												.get(incp).get(nextIndividualIdx));
									} else {
										// use original Individual instance
										newFamilyMember = this.individualMap.get(lgaCode).get(age).get(incp)
												.get(nextIndividualIdx);
									}
									members.add(newFamilyMember);
								}

								// add family members to Household
								household.setIndividuals(members.toArray(Individual[]::new));

								// consolidate Individual financials into Household financials
								household.initialiseFinancialsFromIndividuals();

								// FIXME: calibrate Bal Sht based on RBA data

								// add Individuals and Households to more permanent data structures
								// add to matrix so it has LGA data and can be used to calibrate topology
								ArrayList<Household> existingHouseholds = this.householdMatrix.get(lgaIdx).get(hindIdx)
										.get(cdcfIdx);
								existingHouseholds.add(household);
								this.householdMatrix.get(lgaIdx).get(hindIdx).add(cdcfIdx, existingHouseholds);

								// add to List so they can be used in Payments Clearing algorithm
								this.householdAgents.add(household);
								this.individualAgents.addAll(members);

								agentId++;

							} // end family number loop
						} // end cdcf split loop

						this.householdMatrix.get(lgaIdx).get(hindIdx).get(cdcfIdx).trimToSize();
					} // end for CDCF

				} // end for HCFMD

			} // end for HIND

			// merge the combined MRERD/RNTRD data with the CDCF data (using pdf sampling?)

			// Individual Matrix Keys: postcode, sex, age, industry division, income (ABS
			// categories)
			// convert to: LGA, income

			/*
			 * HCFMD has these additional categories compared to HCFMF: "Lone person
			 * household", "Group household", "Visitors only household", "Other
			 * non-classifiable household"
			 */

			// assign individuals to households using PDF sampling

			// add households to matrix and list
			// (matrix to enable easier links when creating network topology)

			lgaIdx++;
		} // end for LGA
		this.individualAgents.trimToSize();
		this.householdAgents.trimToSize();
		System.out.println();
		System.out.println(new Date(System.currentTimeMillis()) + ": Finished creating Households");
		System.out.println("Created " + integerFormatter.format(agentId) + " Household agents");

		if (DEBUG) {
			System.gc();
			long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			double megabytesConsumed = (memoryAfter - memoryBefore) / 1024d / 1024d;
			DecimalFormat decimalFormatter = new DecimalFormat("#,##0.00");
			System.out.println(
					">>> Memory used creating Household agents: " + decimalFormatter.format(megabytesConsumed) + "MB");
			memoryBefore = memoryAfter;
		}

		this.addAgentsToEconomy();
	}

	private void addAgentsToEconomy() {
		this.economy.setIndividuals(this.individualAgents);
		this.economy.setHouseholds(this.householdAgents);
	}

	private void init() {
		// calibration data
		this.rbaE1 = null;
		this.rbaE2 = null;
		this.abs1410_0Economy = null;
		this.abs1410_0Family = null;
		this.censusHCFMD_LGA_HIND_RNTRD = null;
		this.censusHCFMD_LGA_HIND_MRERD = null;
		this.censusHCFMF_LGA_FINF_CDCF = null;

		// field variables
		this.random = null;
		this.calibrationDateAbs = null;
		this.calibrationDateRba = null;
		this.populationMultiplier = 0d;
		this.lgaDwellingsCount = null;
		this.poaIndexMap = null;

		// agents
		this.individualAgents = null;
		this.householdMatrix = null;
		this.householdAgents = null;
	}

	/**
	 * @param data the data to set
	 */
	@Autowired
	public void setCommonData(CalibrationData data) {
		this.commonData = data;
	}

	/**
	 * @param householdData the householdData to set
	 */
	@Autowired
	public void setHouseholdData(CalibrationDataHousehold householdData) {
		this.householdData = householdData;
	}

	/**
	 * @param calibrateIndividuals the calibrateIndividuals to set
	 */
	@Autowired
	public void setCalibrateIndividuals(CalibrateIndividuals calibrateIndividuals) {
		this.calibrateIndividuals = calibrateIndividuals;
	}

	/**
	 * @param area the area to set
	 */
	@Autowired
	public void setArea(AreaMapping area) {
		this.area = area;
	}

	/**
	 * @param economy the economy to set
	 */
	@Autowired
	public void setEconomy(AustralianEconomy economy) {
		this.economy = economy;
	}

	/**
	 * @param properties the properties to set
	 */
	@Autowired
	public void setProperties(Properties properties) {
		this.properties = properties;
	}

}
