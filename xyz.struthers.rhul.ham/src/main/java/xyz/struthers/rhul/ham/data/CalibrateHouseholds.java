/**
 * 
 */
package xyz.struthers.rhul.ham.data;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

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
	private static final double EPSILON = 0.1d; // to round business counts so the integer sums match

	private static final double NUM_MONTHS = 12d;
	private static final double NUM_WEEKS = 365d / 7d;

	public static final String ABS_1410_YEAR = "2016";
	public static final String CALIBRATION_DATE_ABS = "01/06/2018";
	public static final String CALIBRATION_DATE_RBA = "30/06/2018";

	// map optimisation
	public static final double MAP_LOAD_FACTOR = 0.75d;
	public static final int MAP_LGA_INIT_CAPACITY = (int) Math.ceil(540 / MAP_LOAD_FACTOR) + 1;

	// Series Titles
	private static final String[] SEX_ARRAY = { "M", "F" };
	private static final String[] AGE_ARRAY_ABS = { "0-4 years", "5-9 years", "10-14 years", "15-19 years",
			"20-24 years", "25-29 years", "30-34 years", "35-39 years", "40-44 years", "45-49 years", "50-54 years",
			"55-59 years", "60-64 years", "65-69 years", "70-74 years", "75-79 years", "80-84 years", "85-89 years",
			"90-94 years", "95-99 years", "100 years and over" };
	private static final int[] AGE_ARRAY_ABS_MIDPOINT = { 2, 7, 12, 17, 22, 27, 32, 37, 42, 47, 52, 57, 62, 67, 72, 77,
			82, 87, 92, 97, 102 };
	private static final String[] DIVISION_CODE_ARRAY = { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L",
			"M", "N", "O", "P", "Q", "R", "S" }; // S = Other Services
	private static final int NUM_DIVISIONS = DIVISION_CODE_ARRAY.length; // 19
	private static final String[] INDIVIDUAL_INCOME_RANGES_ABS = { "Negative income", "Nil income",
			"$1-$149 ($1-$7,799)", "$150-$299 ($7,800-$15,599)", "$300-$399 ($15,600-$20,799)",
			"$400-$499 ($20,800-$25,999)", "$500-$649 ($26,000-$33,799)", "$650-$799 ($33,800-$41,599)",
			"$800-$999 ($41,600-$51,999)", "$1,000-$1,249 ($52,000-$64,999)", "$1,250-$1,499 ($65,000-$77,999)",
			"$1,500-$1,749 ($78,000-$90,999)", "$1,750-$1,999 ($91,000-$103,999)", "$2,000-$2,999 ($104,000-$155,999)",
			"$3,000 or more ($156,000 or more)", "Not stated" };
	private static final int NUM_INDIVIDUAL_INCOME_RANGES_ABS = INDIVIDUAL_INCOME_RANGES_ABS.length; // 14
	private static final String[] ABS_HIND_RANGES = { "Negative income", "Nil income", "$1-$149 ($1-$7,799)",
			"$150-$299 ($7,800-$15,599)", "$300-$399 ($15,600-$20,799)", "$400-$499 ($20,800-$25,999)",
			"$500-$649 ($26,000-$33,799)", "$650-$799 ($33,800-$41,599)", "$800-$999 ($41,600-$51,999)",
			"$1,000-$1,249 ($52,000-$64,999)", "$1,250-$1,499 ($65,000-$77,999)", "$1,500-$1,749 ($78,000-$90,999)",
			"$1,750-$1,999 ($91,000-$103,999)", "$2,000-$2,499 ($104,000-$129,999)",
			"$2,500-$2,999 ($130,000-$155,999)", "$3,000-$3,499 ($156,000-$181,999)",
			"$3,500-$3,999 ($182,000-$207,999)", "$4,000-$4,499 ($208,000-$233,999)",
			"$4,500-$4,999 ($234,000-$259,999)", "$5,000-$5,999 ($260,000-$311,999)",
			"$6,000-$7,999 ($312,000-$415,999)", "$8,000 or more ($416,000 or more)", "Partial income stated",
			"All incomes not stated", "Not applicable" };
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
			"One parent family with: Six or more dependent children", "Not applicable", "Total" };

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
	/**
	 * The matrix of Individual agents.<br>
	 * Keys: postcode, sex, age, industry division, income (ABS categories)
	 */
	private List<List<List<List<List<List<Individual>>>>>> individualMatrix;
	private Date calibrationDateAbs;
	private Date calibrationDateRba;
	private int totalPopulationAU;
	private double populationMultiplier;
	private Map<String, Integer> lgaPeopleCount; // adjusted to 2018
	private Map<String, Integer> lgaDwellingsCount; // adjusted to 2018
	private Map<String, Integer> poaIndexMap; // from Individual agent data
	private Map<String, Integer> lgaIndexMap;

	private static int agentNo = 0;

	// households should have an LGA
	ArrayList<Household> householdAgents;
	/**
	 * A matrix of Household agents. Keys: LGA code, list of households in each LGA
	 */
	ArrayList<ArrayList<Household>> householdMatrix;

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
		if (true) {
			System.gc();
			memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		}

		System.out.println(new Date(System.currentTimeMillis()) + ": Starting creation of Individual agents");
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

		// get key metrics that will be used across all the data
		this.lgaDwellingsCount = this.area.getAdjustedDwellingsByLga(this.calibrationDateAbs);
		this.populationMultiplier = this.area.getPopulationMultiplier(this.calibrationDateAbs);

		// get list of LGAs from the matrix of Individuals
		// individualMatrix Keys: postcode, sex, age, industry division, income
		this.individualMatrix = this.calibrateIndividuals.getIndividualMatrix();
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
		// PDF Keys: LGA, HCFMD, HIND, RNTRD/MRERD midpoints
		double[][][][] pdfRntrd = new double[lgaCodesIntersection
				.size()][ABS_HCFMD.length][ABS_HIND_RANGES.length][ABS_RNTRD_MIDPOINT.length];
		double[][][][] pdfMrerd = new double[lgaCodesIntersection
				.size()][ABS_HCFMD.length][ABS_HIND_RANGES.length][ABS_RNTRD_MIDPOINT.length];
		int lgaIdx = 0;
		Map<String, Integer> lgaIndexMap = new HashMap<String, Integer>(
				(int) Math.ceil(lgaCodesIntersection.size() / MAP_LOAD_FACTOR) + 1);
		String[] lgaArray = new String[lgaCodesIntersection.size()];
		for (String lgaCode : lgaCodesIntersection) {
			lgaIndexMap.put(lgaCode, lgaIdx);
			lgaArray[lgaIdx] = lgaCode;
			// combine MRERD and RNTRD data, and calculate ratios/multipliers. MRERD and
			// RNTRD ratios for all categories, and family composition ratios for the
			// non-kids family types (the ones that map to N/A in CDCF).
			for (int hindIdx = 0; hindIdx < ABS_HIND_RANGES.length; hindIdx++) {
				String hind = ABS_HIND_RANGES[hindIdx];
				for (int hcfmdIdx = 0; hcfmdIdx < ABS_HCFMD.length; hcfmdIdx++) {
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

					for (int rntrdIdx = 0; rntrdIdx < ABS_RNTRD_MIDPOINT.length; rntrdIdx++) {
						pdfRntrd[lgaIdx][hcfmdIdx][hindIdx][rntrdIdx] = 0d;
						if (rntrdIdx == 21 || rntrdIdx == 22) {
							// map "Not stated" and "Not applicable" into the $0 category
							//FIXME: up to here
						}
					}
				} // end for HCFMD

			} // end for HIND

			// merge the combined MRERD/RNTRD data with the CDCF data (using pdf sampling?)

			// assign individuals to households using PDF sampling

			// add households to matrix and list
			// (matrix to enable easier links when creating network topology)

			lgaIdx++;
		} // end for LGA
		this.householdAgents.trimToSize();

		this.addAgentsToEconomy();
	}

	private void addAgentsToEconomy() {
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
		this.individualMatrix = null;
		this.calibrationDateAbs = null;
		this.calibrationDateRba = null;
		this.populationMultiplier = 0d;
		this.lgaDwellingsCount = null;
		this.poaIndexMap = null;

		// agents
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
