/**
 * 
 */
package xyz.struthers.rhul.ham.data;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import xyz.struthers.rhul.ham.agent.Individual;
import xyz.struthers.rhul.ham.process.AustralianEconomy;

/**
 * Calibrates the P&L and Bal Shts of individuals. These are later grouped
 * together used to calibrate household P&L and Bal Shts.
 * 
 * N.B. Debits are positive unless stated otherwise.
 * 
 * @author Adam Struthers
 * @since 11-Dec-2018
 */
@Component
@Scope(value = "singleton")
public class CalibrateIndividuals {

	// CONSTANTS
	private static final double MILLION = 1000000d;
	private static final double THOUSAND = 1000d;
	private static final double PERCENT = 0.01d;
	private static final double EPSILON = 0.1d; // to round business counts so the integer sums match

	private static final double NUM_MONTHS = 12d;
	private static final double NUM_WEEKS = 365d / 7d;
	private static final int NUM_DIVISIONS = 19;

	public static final String CALIBRATION_DATE_ATO = "01/06/2018";
	public static final String CALIBRATION_DATE_RBA = "30/06/2018";

	public static final double MAP_LOAD_FACTOR = 0.75d;
	private static final String[] STATES_ARRAY = { "NSW", "VIC", "QLD", "SA", "WA", "TAS", "NT", "ACT", "OT" };
	private static final int[] MAP_STATE_POA_CAPACITY = { (int) Math.ceil(612 / MAP_LOAD_FACTOR) + 1,
			(int) Math.ceil(382 / MAP_LOAD_FACTOR) + 1, (int) Math.ceil(429 / MAP_LOAD_FACTOR) + 1,
			(int) Math.ceil(326 / MAP_LOAD_FACTOR) + 1, (int) Math.ceil(338 / MAP_LOAD_FACTOR) + 1,
			(int) Math.ceil(111 / MAP_LOAD_FACTOR) + 1, (int) Math.ceil(43 / MAP_LOAD_FACTOR) + 1,
			(int) Math.ceil(25 / MAP_LOAD_FACTOR) + 1, (int) Math.ceil(1 / MAP_LOAD_FACTOR) + 1 };

	private static final String ATO_2A_TITLE_COUNT = "Number of individuals no.";
	private static final String ATO_2A_TITLE_TAXABLE_COUNT = "Taxable income or loss3 no.";
	private static final String ATO_2A_TITLE_TAXABLE_AMOUNT = "Taxable income or loss3 $";
	private static final String ATO_9_TITLE_COUNT = "Number of individuals";
	private static final String ATO_9_TITLE_TAXABLE_COUNT = "Taxable income or loss4 no.";
	private static final String ATO_9_TITLE_TAXABLE_AMOUNT = "Taxable income or loss4 $";
	private static final String ATO_6B_TITLE_COUNT = "Number of individuals no.";
	private static final String ATO_6B_TITLE_TAXABLE_COUNT = "Taxable income or loss3 no.";
	private static final String ATO_6B_TITLE_TAXABLE_AMOUNT = "Taxable income or loss3 $";

	private static final String RBA_E1_SERIESID_CASH = "BSPNSHUFAD"; // Household deposits
	private static final String RBA_E1_SERIESID_SUPER = "BSPNSHUFAR"; // Household superannuation
	private static final String RBA_E1_SERIESID_EQUITIES = "BSPNSHUFAS"; // Household equities
	private static final String RBA_E1_SERIESID_OTHER_FIN_ASSETS = "BSPNSHUFAO"; // Household other financial assets
	private static final String RBA_E1_SERIESID_DWELLINGS = "BSPNSHNFD"; // Household dwellings
	private static final String RBA_E1_SERIESID_NONFIN_ASSETS = "BSPNSHNFT"; // Household total non-financial assets
	private static final String RBA_E1_SERIESID_TOTAL_LIABILITIES = "BSPNSHUL"; // Household total liabilities

	private static final String RBA_E2_SERIESID_DEBT_TO_INCOME = "BHFDDIT";
	private static final String RBA_E2_SERIESID_ASSETS_TO_INCOME = "BHFADIT";

	// beans
	private AreaMapping area;
	private CalibrationData commonData;
	private CalibrationDataIndividual individualData;
	private AustralianEconomy economy;

	// field variables
	private List<Individual> individualAgents;
	private Date calibrationDateAto;
	private Date calibrationDateRba;
	private int totalPopulationAU;
	private double populationMultiplier;
	private Map<String, Integer> lgaPeopleCount;
	private Map<String, Integer> lgaDwellingsCount;

	// data sets
	/**
	 * ANZSIC industry code mapping<br>
	 * Key 1 is mapping per the titles (e.g. "Class Code to Division")<br>
	 * Key 2 is the code or description (e.g. "Division Code")
	 * 
	 * Stores description-to-code mapping in UPPER CASE, so use toUpperCase() when
	 * getting the mapping from descriptions to codes.
	 * 
	 * Mappings are:<br>
	 * "Division Code to Division"<br>
	 * "Subdivision Code to Subdivision"<br>
	 * "Group Code to Group"<br>
	 * "Class Code to Class"<br>
	 * "Industry Code to Industry"<br>
	 * "Division to Division Code"<br>
	 * "Subdivision to Subdivision Code"<br>
	 * "Group to Group Code"<br>
	 * "Class to Class Code"<br>
	 * "Industry to Industry Code"<br>
	 * "Industry Code to Class Code"<br>
	 * "Industry Code to Group Code"<br>
	 * "Industry Code to Subdivision Code"<br>
	 * "Industry Code to Division Code"<br>
	 * "Class Code to Group Code"<br>
	 * "Class Code to Subdivision Code"<br>
	 * "Class Code to Division Code"<br>
	 * "Group Code to Subdivision Code"<br>
	 * "Group Code to Division Code"<br>
	 * "Subdivision Code to Division Code"<br>
	 */
	private Map<String, Map<String, String>> abs1292_0_55_002ANZSIC;
	/**
	 * Data by LGA: Economy<br>
	 * Contains property prices and counts per LGA<br>
	 * Keys: Year, LGA, data series
	 */
	private Map<String, Map<String, Map<String, String>>> abs1410_0Economy;
	/**
	 * ATO Individuals Table 2A<br>
	 * Contains P&L and people count by sex and 5-year age range.<br>
	 * Keys: Series Title, State, Age, Gender, Taxable Status, Lodgment Method
	 */
	private Map<String, Map<String, Map<String, Map<String, Map<String, Map<String, String>>>>>> atoIndividualTable2a;
	/**
	 * ATO Individuals Table 3A<br>
	 * Contains P&L and people count by sex and 5-year age range.<br>
	 * Keys: Series Title, State, Age, Gender, Taxable Status, Lodgment Method
	 */
	private Map<String, Map<String, Map<String, Map<String, Map<String, String>>>>> atoIndividualTable3a;
	/**
	 * ATO Individuals Table 6B<br>
	 * Contains P&L and people count by post code.<br>
	 * Keys: Series Title, Post Code
	 */
	private Map<String, Map<String, String>> atoIndividualTable6b;
	/**
	 * ATO Individuals Table 9 (Industry Division summary)<br>
	 * Contains count and taxable income, summarised by industry division.<br>
	 * Keys: Series Title, Industry Division Code
	 */
	private Map<String, Map<String, Double>> atoIndividualTable9DivisionSummary;
	/**
	 * RBA E1 Household and Business Balance Sheets<br>
	 * Contains high-level balance sheet amounts at a national level.<br>
	 * Keys: Series ID, Date
	 */
	private Map<String, Map<Date, String>> rbaE1;
	/**
	 * RBA E2 Selected Ratios<br>
	 * Contains ratios that link P&L and Bal Sht.<br>
	 * Keys: Series ID, Date
	 */
	private Map<String, Map<Date, String>> rbaE2;
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
	 * 
	 */
	public CalibrateIndividuals() {
		super();
		this.init();
	}

	private void init() {
		this.individualAgents = null;
		this.calibrationDateAto = null;
		this.calibrationDateRba = null;
		this.totalPopulationAU = 0;
		this.populationMultiplier = 0d;
		this.lgaPeopleCount = null;
		this.lgaDwellingsCount = null;

		// data sources
		this.abs1410_0Economy = null;
		this.atoIndividualTable2a = null;
		this.atoIndividualTable3a = null;
		this.atoIndividualTable6b = null;
		this.atoIndividualTable9DivisionSummary = null;
		this.rbaE1 = null;
		this.rbaE2 = null;
		this.censusSEXP_POA_AGE5P_INDP_INCP = null;
		this.censusHCFMD_LGA_HIND_RNTRD = null;
		this.censusHCFMD_LGA_HIND_MRERD = null;
		this.censusCDCF_LGA_FINF = null;
	}

	/**
	 * A destructor to free up the resources used by this class.
	 */
	public void close() {
		// TODO: do a deep delete of the variables that won't be used in the Individual
		// agents.

		this.init(); // to reset all variables to null
	}

	/**
	 * Calibrates individual financials, and works out how many of each to create,
	 * then adds them to the economy.
	 * 
	 * =============<br>
	 * = ALGORITHM =<br>
	 * =============<br>
	 * 
	 * I think I want to loop through the LGAs one at a time, creating all their
	 * agents and Households before moving onto the next LGA. I need to work out how
	 * to handle "orphan" individuals when aggregating into families. a possible
	 * solution would be to randomly assign them to households. This is not
	 * incongruous with the broader approach because it creates heterogeneity and
	 * recognises that all the agents are built on a series of assumptions anyway.
	 * 
	 * It might be easier to calibrate the Household agents in here at the same time
	 * that the Individual agents are created.
	 * 
	 * For the sake of this model, dwellings, households and families are considered
	 * to be interchangeable.
	 * 
	 * ------------------------------------------------------------------------<br>
	 * PART A: ESTIMATING INDIVIDUAL PROFIT & LOSS STATEMENTS AND BALANCE SHEETS
	 * ------------------------------------------------------------------------<br>
	 * 
	 * N.B. P&L composition will vary more with income, so use ATO 3A as the amounts
	 * and the ratios between line items.<br>
	 * 
	 * 1. ATO Individual Table 9: P&L (by industry code). Only 1.2M of 13.5M
	 * taxpayers specified an industry, so just use the industry data to derive
	 * ratios of taxable income to multiply the the other data by. Do it at an
	 * industry division level to minimise the impact of the small sample. Make it a
	 * multiple of the industry-declaring national average so we can just multiply
	 * the other cells by this multiple rather than trying to split them. Don't use
	 * the total amounts or the ratios between line items as it's too small a
	 * sample. So, the algorithm becomes: Calculate mean taxable income per industry
	 * code. Calculate mean taxable income across all industry codes. Divide each
	 * industry code's mean by the overall mean to produce an industry
	 * multiplier.<br>
	 * 
	 * 2. ATO Individual Table 2A: age/sex count, P&L, Help Debt (by State).
	 * Calculate state multiplier for every age/sex combination.<br>
	 * 
	 * 3. ATO Individual Table 6B: people count, taxable income (per POA). Calculate
	 * POA multiplier for each POA, by state.<br>
	 * 
	 * 4. ATO Individual Table 3A: age/sex count, P&L, Help Debt (by income range).
	 * Financial position will vary more by income than any other metric, so use
	 * table 3A as the base amounts and counts to adjust. Use these amounts, looping
	 * through the extra dimensions and multiplying by the multipliers above. The
	 * number of people is given in table 3A, and only needs to be multiplied by the
	 * population forecast multiplier to adjust them forward to 2018 figures. This
	 * ensures, for example, that the right number of people are assigned a HELP
	 * debt, etc.<br>
	 * 
	 * N.B. Need to think about HELP debt and work out how to assign it by age &
	 * gender, not diminish it by including it in ratios too early.
	 * 
	 * This gives P&L and Bal Sht by sex, age, industry division, income, POA
	 * 
	 * ------------------------------------------------------------------------<br>
	 * PART B: INDIVIDUAL COUNTS
	 * ------------------------------------------------------------------------<br>
	 * 
	 * 5. Census age, industry, income & sex per POA: Get total population
	 * multiplier, and adjust each count so it's now a 2018 equivalent.<br>
	 * 
	 * ROUGH ALGORITHM: Calculate tax-payers per ATO/RBA data as above, then create
	 * everyone else so they can be added into households too.
	 * 
	 * ------------------------------------------------------------------------<br>
	 * PART C: HOUSEHOLD COUNTS
	 * ------------------------------------------------------------------------<br>
	 * 
	 * ROUGH ALGORITHM FOR HOUSEHOLD COUNTS:<br>
	 * - Load Census FINF by CDCF by LGA/POA<br>
	 * - Load MRERD by LGA/POA<br>
	 * - Load RNTRD by LGA/POA<br>
	 * - For each FINF and CDCF, get the corresponding HIND and HCFMD to get income,
	 * rent & mortgage for each family. Assume rent is paid by families with no mtg,
	 * and mortgage paid by families with no rent.<br>
	 * - Calculate the ratios between all these per LGA.<br>
	 * - Multiply by the number of dwellings in each LGA to give an adjusted count.
	 * 
	 * Question: Is the HCFMD detailed enough to be able to just exclude the CDCF
	 * table altogether?
	 * 
	 * ROUGH ALGORITHM TO ASSIGN PEOPLE TO HOUSEHOLDS:
	 * 
	 * ------------------------------------------------------------------------<br>
	 * PART D: ADJUSTING FINANCIALS FOR HOUSEHOLDS
	 * ------------------------------------------------------------------------<br>
	 * 
	 * 4. RBA E2: Use the household debt-to-income and assets-to-income ratios to
	 * calculate total assets and total debt.<br>
	 * 
	 * 5. RBA E1: Calculate the ratios between household Bal Sht items. Use these,
	 * compared to assets and debt, to estimate the other balance sheet items.<br>
	 * 
	 * ROUGH ALGORITHM FOR HOUSEHOLD ADJUSTMENTS:<br>
	 * - Henderson poverty line based on family composition to be a proxy for
	 * inelastic expenses. - Generally just one mortgage or rent payment per
	 * household. This should probably factor into the algorithm that assigns
	 * individuals to households.
	 * 
	 */
	public void createIndividualAgents() {
		// set the calibration date
		DateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);
		try {
			this.calibrationDateAto = sdf.parse(CALIBRATION_DATE_ATO);
			this.calibrationDateRba = sdf.parse(CALIBRATION_DATE_RBA);
		} catch (ParseException e) {
			e.printStackTrace();
		}

		// get raw calibration data
		this.abs1292_0_55_002ANZSIC = this.commonData.getAbs1292_0_55_002ANZSIC();
		this.abs1410_0Economy = this.individualData.getAbs1410_0Economy();
		this.atoIndividualTable2a = this.individualData.getAtoIndividualTable2a();
		this.atoIndividualTable3a = this.individualData.getAtoIndividualTable3a();
		this.atoIndividualTable6b = this.individualData.getAtoIndividualTable6b();
		this.atoIndividualTable9DivisionSummary = this.individualData.getAtoIndividualTable9DivisionSummary();
		this.rbaE1 = this.commonData.getRbaE1();
		this.rbaE2 = this.individualData.getRbaE2();
		this.censusSEXP_POA_AGE5P_INDP_INCP = this.individualData.getCensusSEXP_POA_AGE5P_INDP_INCP();
		this.censusHCFMD_LGA_HIND_RNTRD = this.individualData.getCensusHCFMD_LGA_HIND_RNTRD();
		this.censusHCFMD_LGA_HIND_MRERD = this.individualData.getCensusHCFMD_LGA_HIND_MRERD();
		this.censusCDCF_LGA_FINF = this.individualData.getCensusCDCF_LGA_FINF();

		// get key metrics that will be used across all the data
		this.lgaPeopleCount = this.area.getAdjustedPeopleByLga(this.calibrationDateAto);
		this.lgaDwellingsCount = this.area.getAdjustedDwellingsByLga(this.calibrationDateAto);
		this.totalPopulationAU = this.area.getTotalPopulation(this.calibrationDateAto);
		this.populationMultiplier = this.area.getPopulationMultiplier(this.calibrationDateAto);
		Set<String> lgaCodes = this.lgaPeopleCount.keySet();

		/*
		 * ------------------------------------------------------------------------<br>
		 * PART A: ESTIMATING INDIVIDUAL PROFIT & LOSS STATEMENTS
		 * ------------------------------------------------------------------------<br>
		 * 
		 * N.B. P&L composition will vary more with income, so use ATO 3A as the amounts
		 * and the ratios between line items.<br>
		 * 
		 * 1. ATO Individual Table 9: P&L (by industry code). Only 1.2M of 13.5M
		 * taxpayers specified an industry, so just use the industry data to derive
		 * ratios of taxable income to multiply the the other data by. Do it at an
		 * industry division level to minimise the impact of the small sample. Make it a
		 * multiple of the industry-declaring national average so we can just multiply
		 * the other cells by this multiple rather than trying to split them. Don't use
		 * the total amounts or the ratios between line items as it's too small a
		 * sample. So, the algorithm becomes: Calculate mean taxable income per industry
		 * code. Calculate mean taxable income across all industry codes. Divide each
		 * industry code's mean by the overall mean to produce an industry
		 * multiplier.<br>
		 */
		Map<String, Double> divisionMultiplier = new HashMap<String, Double>(NUM_DIVISIONS);
		Set<String> divisionCodeSet = this.atoIndividualTable9DivisionSummary.get(ATO_9_TITLE_TAXABLE_COUNT).keySet();
		double divTotalCount9A = 0d;
		double divTotalAmount9A = 0d;
		for (String divCode : divisionCodeSet) {
			// calculate average income per division, and total of all divisions
			double divTaxableCount = this.atoIndividualTable9DivisionSummary.get(ATO_9_TITLE_TAXABLE_COUNT)
					.get(divCode);
			double divTaxableAmount = this.atoIndividualTable9DivisionSummary.get(ATO_9_TITLE_TAXABLE_AMOUNT)
					.get(divCode);
			double divTaxablePerPerson = divTaxableAmount / divTaxableCount;
			// Just an efficient place to hold this. Will be overwritten in the loop below.
			divisionMultiplier.put(divCode, divTaxablePerPerson);
			divTotalCount9A += divTaxableCount;
			divTotalAmount9A += divTaxableAmount;
		}
		double divTotalTaxablePerPerson = divTotalCount9A > 0d ? divTotalAmount9A / divTotalCount9A : 0d;
		for (String divCode : divisionCodeSet) {
			double divMultiplier = divisionMultiplier.get(divCode) / divTotalTaxablePerPerson;
			divisionMultiplier.put(divCode, divMultiplier);
		}

		/*
		 * 2. ATO Individual Table 2A: age/sex count, P&L, Help Debt (by State).
		 * Calculate state multiplier for every age/sex combination.<br>
		 */
		// Keys: Series Title, State, Age, Gender, Taxable Status, Lodgment Method

		// initialise state multiplier map
		// Keys for stateMultiplier: Sex (2), Age (13), State (9)
		String[] sexArray = { "M", "F" };
		String[] ageArray = { "a. Under 18", "b. 18 - 24", "c. 25 - 29", "d. 30 - 34", "e. 35 - 39", "f. 40 - 44",
				"g. 45 - 49", "h. 50 - 54", "i. 55 - 59", "j. 60 - 64", "k. 65 - 69", "l. 70 - 74", "m. 75 and over" };
		int sexMapCapacity = (int) Math.ceil(sexArray.length / MAP_LOAD_FACTOR);
		int ageMapCapacity = (int) Math.ceil(ageArray.length / MAP_LOAD_FACTOR);
		int stateMapCapacity = (int) Math.ceil(STATES_ARRAY.length / MAP_LOAD_FACTOR);
		Map<String, Map<String, Map<String, Double>>> stateTaxableCount = new HashMap<String, Map<String, Map<String, Double>>>(
				sexMapCapacity);
		Map<String, Map<String, Map<String, Double>>> stateTaxableAmount = new HashMap<String, Map<String, Map<String, Double>>>(
				sexMapCapacity);
		Map<String, Map<String, Map<String, Double>>> stateMultiplier = new HashMap<String, Map<String, Map<String, Double>>>(
				sexMapCapacity);
		Map<String, Map<String, Double>> stateSexAgeNationalTotalCount = new HashMap<String, Map<String, Double>>(
				sexMapCapacity);
		Map<String, Map<String, Double>> stateSexAgeNationalTotalAmount = new HashMap<String, Map<String, Double>>(
				sexMapCapacity);
		for (String sex : sexArray) {
			stateTaxableCount.put(sex, new HashMap<String, Map<String, Double>>(ageMapCapacity));
			stateTaxableAmount.put(sex, new HashMap<String, Map<String, Double>>(ageMapCapacity));
			stateMultiplier.put(sex, new HashMap<String, Map<String, Double>>(ageMapCapacity));
			stateSexAgeNationalTotalCount.put(sex, new HashMap<String, Double>(ageMapCapacity));
			stateSexAgeNationalTotalAmount.put(sex, new HashMap<String, Double>(ageMapCapacity));
			for (String age : ageArray) {
				stateTaxableCount.get(sex).put(age, new HashMap<String, Double>(stateMapCapacity));
				stateTaxableAmount.get(sex).put(age, new HashMap<String, Double>(stateMapCapacity));
				stateMultiplier.get(sex).put(age, new HashMap<String, Double>(stateMapCapacity));
				stateSexAgeNationalTotalCount.get(sex).put(age, 0d);
				stateSexAgeNationalTotalAmount.get(sex).put(age, 0d);
				for (String state : STATES_ARRAY) {
					stateTaxableCount.get(sex).get(age).put(state, 0d);
					stateTaxableAmount.get(sex).get(age).put(state, 0d);
					stateMultiplier.get(sex).get(age).put(state, 0d);
				}
			}
		}

		// read data and calculate totals by state
		// N.B. Not every series has every key, so get key sets inside loops
		Set<String> ato2aKeySetState = this.atoIndividualTable2a.get(ATO_2A_TITLE_TAXABLE_COUNT).keySet();
		for (String state : ato2aKeySetState) {
			String stateCode = this.getStateCode(state);
			Set<String> ato2aKeySetAge = this.atoIndividualTable2a.get(ATO_2A_TITLE_TAXABLE_COUNT).get(state).keySet();
			for (String age : ato2aKeySetAge) {
				Set<String> ato2aKeySetSex = this.atoIndividualTable2a.get(ATO_2A_TITLE_TAXABLE_COUNT).get(state)
						.get(age).keySet();
				for (String sex : ato2aKeySetSex) {
					Set<String> ato2aKeySetTaxableStatus = this.atoIndividualTable2a.get(ATO_2A_TITLE_TAXABLE_COUNT)
							.get(state).get(age).get(sex).keySet();
					for (String taxableStatus : ato2aKeySetTaxableStatus) {
						Set<String> ato2aKeySetLodgmentStatus = this.atoIndividualTable2a
								.get(ATO_2A_TITLE_TAXABLE_COUNT).get(state).get(age).get(sex).get(taxableStatus)
								.keySet();
						for (String lodgmentStatus : ato2aKeySetLodgmentStatus) {
							double oldCountVal = stateTaxableCount.get(sex).get(age).get(stateCode);
							double newCountVal = Double
									.valueOf(this.atoIndividualTable2a.get(ATO_2A_TITLE_TAXABLE_COUNT).get(state)
											.get(age).get(sex).get(taxableStatus).get(lodgmentStatus).replace(",", ""));
							stateTaxableCount.get(sex).get(age).put(stateCode, oldCountVal + newCountVal);
							double oldTotalCount = stateSexAgeNationalTotalCount.get(sex).get(age);
							stateSexAgeNationalTotalCount.get(sex).put(age, oldTotalCount + newCountVal);

							double oldAmountVal = stateTaxableAmount.get(sex).get(age).get(stateCode);
							double newAmountVal = Double
									.valueOf(this.atoIndividualTable2a.get(ATO_2A_TITLE_TAXABLE_AMOUNT).get(state)
											.get(age).get(sex).get(taxableStatus).get(lodgmentStatus).replace(",", ""));
							stateTaxableAmount.get(sex).get(age).put(stateCode, oldAmountVal + newAmountVal);
							double oldTotalAmount = stateSexAgeNationalTotalAmount.get(sex).get(age);
							stateSexAgeNationalTotalAmount.get(sex).put(age, oldTotalAmount + newAmountVal);
						}
					}
				}
			}
		}

		// calculate average income per state, and national average
		for (String sex : sexArray) {
			for (String age : ageArray) {
				double sexAgeCount = stateSexAgeNationalTotalCount.get(sex).get(age);
				double sexAgeAmount = stateSexAgeNationalTotalAmount.get(sex).get(age);
				double sexAgeTaxablePerPerson = sexAgeCount > 0d ? sexAgeAmount / sexAgeCount : 0d;
				for (String state : STATES_ARRAY) {
					double stateCount = stateTaxableCount.get(sex).get(age).get(state);
					double stateAmount = stateTaxableAmount.get(sex).get(age).get(state);
					double stateTaxablePerPerson = stateAmount / stateCount;
					double thisStateMultiplier = sexAgeTaxablePerPerson > 0d
							? stateTaxablePerPerson / sexAgeTaxablePerPerson
							: 0d;
					stateMultiplier.get(sex).get(age).put(state, thisStateMultiplier);
				}
			}
		}

		/*
		 * 3. ATO Individual Table 6B: people count, taxable income (per POA). Calculate
		 * POA multiplier for each POA, by state.<br>
		 */

		// ATO 6B Keys: Series Title, Post Code
		// postcodeMultiplier Keys: State Code, Post Code
		Map<String, Map<String, Double>> postcodeStateMultiplier = new HashMap<String, Map<String, Double>>(
				stateMapCapacity);
		Map<String, Double> postcodeStateTotalCount = new HashMap<String, Double>(stateMapCapacity);
		Map<String, Double> postcodeStateTotalAmount = new HashMap<String, Double>(stateMapCapacity);
		Map<String, Double> postcodeStateTaxablePerPerson = new HashMap<String, Double>(stateMapCapacity);

		// initialise state multiplier map
		for (int i = 0; i < STATES_ARRAY.length; i++) {
			String state = STATES_ARRAY[i];
			postcodeStateMultiplier.put(state, new HashMap<String, Double>(MAP_STATE_POA_CAPACITY[i]));
			postcodeStateTotalCount.put(state, 0d);
			postcodeStateTotalAmount.put(state, 0d);
		}

		Set<String> postcodeSet = this.atoIndividualTable6b.get(ATO_6B_TITLE_TAXABLE_COUNT).keySet();
		for (String postcode : postcodeSet) {
			String state = this.area.getStateFromPoa(postcode);

			// calculate average income per postcode, and state totals for all postcodes
			double postcodeTaxableCount = Double
					.valueOf(this.atoIndividualTable6b.get(ATO_6B_TITLE_TAXABLE_COUNT).get(postcode).replace(",", ""));
			double postcodeTaxableAmount = Double
					.valueOf(this.atoIndividualTable6b.get(ATO_6B_TITLE_TAXABLE_AMOUNT).get(postcode).replace(",", ""));
			double postcodeTaxablePerPerson = postcodeTaxableAmount / postcodeTaxableCount;

			// Just an efficient place to hold this. Will be overwritten in the loop below.
			postcodeStateMultiplier.get(state).put(postcode, postcodeTaxablePerPerson);
			double oldStateTotalCount = postcodeStateTotalCount.get(state);
			postcodeStateTotalCount.put(state, oldStateTotalCount + postcodeTaxableCount);
			double oldStateTotalAmount = postcodeStateTotalAmount.get(state);
			postcodeStateTotalAmount.put(state, oldStateTotalAmount + postcodeTaxableAmount);
		}

		// calculate state averages
		for (String state : STATES_ARRAY) {
			double stateCount = postcodeStateTotalCount.containsKey(state) ? postcodeStateTotalCount.get(state) : 0d;
			double stateAmount = postcodeStateTotalAmount.containsKey(state) ? postcodeStateTotalAmount.get(state) : 0d;
			postcodeStateTaxablePerPerson.put(state, stateCount > 0d ? stateAmount / stateCount : 0d);
		}

		// calculate state multipliers
		for (String postcode : postcodeSet) {
			String state = this.area.getStateFromPoa(postcode);
			double stateTaxablePerPerson = postcodeStateTaxablePerPerson.get(state);
			double postcodeMultiplier = postcodeStateMultiplier.get(state).get(postcode) / stateTaxablePerPerson;
			postcodeStateMultiplier.get(state).put(postcode, postcodeMultiplier);
		}

		/*
		 * 4. ATO Individual Table 3A: age/sex count, P&L, Help Debt (by income range).
		 * Financial position will vary more by income than any other metric, so use
		 * table 3A as the base amounts and counts to adjust. Use these amounts, looping
		 * through the extra dimensions and multiplying by the multipliers above. The
		 * number of people is given in table 3A, and only needs to be multiplied by the
		 * population forecast multiplier to adjust them forward to 2018 figures. This
		 * ensures, for example, that the right number of people are assigned a HELP
		 * debt, etc.<br>
		 */

		// FIXME: implement me

		/*
		 * ------------------------------------------------------------------------<br>
		 * PART D: ADJUSTING FINANCIALS FOR HOUSEHOLDS
		 * ------------------------------------------------------------------------<br>
		 * 
		 * D1. RBA E2: Use the debt-to-income and assets-to-income ratios to calculate
		 * total assets and total debt.<br>
		 */
		// RBA E2 Keys: Series Name, Date
		double debtToIncomeRatioRbaE2 = Double
				.valueOf(this.rbaE2.get(RBA_E2_SERIESID_DEBT_TO_INCOME).get(this.calibrationDateRba)) * PERCENT;
		double assetsToIncomeRatioRbaE2 = Double
				.valueOf(this.rbaE2.get(RBA_E2_SERIESID_ASSETS_TO_INCOME).get(this.calibrationDateRba)) * PERCENT;

		/*
		 * D2. RBA E1: Calculate the ratios between Bal Sht items. Use these, compared
		 * to assets and debt, to estimate the other balance sheet items.<br>
		 */
		// RBA E1 Keys: Series Name, Date
		// get RBA E1 amounts ($ billions)
		double cashRbaE1 = Double.valueOf(this.rbaE1.get(RBA_E1_SERIESID_CASH).get(this.calibrationDateRba));
		double superRbaE1 = Double.valueOf(this.rbaE1.get(RBA_E1_SERIESID_SUPER).get(this.calibrationDateRba));
		double equitiesRbaE1 = Double.valueOf(this.rbaE1.get(RBA_E1_SERIESID_EQUITIES).get(this.calibrationDateRba));
		double otherFinAssetsRbaE1 = Double
				.valueOf(this.rbaE1.get(RBA_E1_SERIESID_OTHER_FIN_ASSETS).get(this.calibrationDateRba));
		double totalFinancialAssetsRbaE1 = cashRbaE1 + superRbaE1 + equitiesRbaE1 + otherFinAssetsRbaE1;
		double dwellingsRbaE1 = Double.valueOf(this.rbaE1.get(RBA_E1_SERIESID_DWELLINGS).get(this.calibrationDateRba));
		double totalNonFinancialAssetsRbaE1 = Double
				.valueOf(this.rbaE1.get(RBA_E1_SERIESID_NONFIN_ASSETS).get(this.calibrationDateRba));
		double otherNonFinancialAssetsRbaE1 = totalNonFinancialAssetsRbaE1 - dwellingsRbaE1;
		double totalAssetsRbaE1 = totalFinancialAssetsRbaE1 + totalNonFinancialAssetsRbaE1;
		double totalLiabilitiesRbaE1 = Double
				.valueOf(this.rbaE1.get(RBA_E1_SERIESID_TOTAL_LIABILITIES).get(this.calibrationDateRba));

		// calculate ratios within balance sheet
		double cashToAssetsRbaE1 = cashRbaE1 / totalAssetsRbaE1;
		double superToAssetsRbaE1 = superRbaE1 / totalAssetsRbaE1;
		double equitiesToAssetsRbaE1 = equitiesRbaE1 / totalAssetsRbaE1;
		double otherFinAssetsToAssetsRbaE1 = otherFinAssetsRbaE1 / totalAssetsRbaE1;
		double dwellingsToAssetsRbaE1 = dwellingsRbaE1 / totalAssetsRbaE1;
		double otherNonFinAssetsToAssetsRbaE1 = otherNonFinancialAssetsRbaE1 / totalAssetsRbaE1;
		double totalLiabilitiesToAssetsRbaE1 = totalLiabilitiesRbaE1 / totalAssetsRbaE1;
		// use debt-to-income ratio to determine total debt, then subtract from total
		// liabilities to get other liabilities

		this.addAgentsToEconomy();
	}

	private void addAgentsToEconomy() {
		this.economy.setIndividuals(this.individualAgents);
	}

	/*
	 * private Map<String, List<Double>> segmentLgaByIncp(String lgaCode, String
	 * financialYear, Date date) { // local constants final int numBins = 14;
	 * 
	 * // local variables // String[] title = { "$0", "$3900", "$11700", "$18200",
	 * "$23400", "$29900", // "$37700", "$46800", "$58500", // "$71500", "$84500",
	 * "$97500", "$130000", "$156000+" }; Double[] from = new Double[] { 0d, 1d,
	 * 7800d, 15600d, 20800d, 26000d, 33800d, 41600d, 52000d, 65000d, 78000d,
	 * 91000d, 104000d, 156000d }; Double[] to = new Double[] { 0d, 7799d, 15599d,
	 * 20799d, 25999d, 33799d, 41599d, 51999d, 64999d, 77999d, 90999d, 103999d,
	 * 155999d, 999999d }; // the last element is calculated in the code as a
	 * balancing item Double[] meanIncome = new Double[numBins]; Double[] persons =
	 * new Double[numBins];
	 * 
	 * // get number of people from census data LGA by INCP Map<String, Map<String,
	 * String>> census = this.data.getCensusLgaByINCP(); persons[0] =
	 * Double.valueOf(census.get("Negative income").get(lgaCode)); persons[0] +=
	 * Double.valueOf(census.get("Nil income").get(lgaCode)); // persons[0] +=
	 * Double.valueOf(census.get("Not stated").get(lgaCode)); // // exclude these
	 * from the sample persons[0] +=
	 * Double.valueOf(census.get("Not applicable").get(lgaCode)); persons[1] +=
	 * Double.valueOf(census.get("$1-$149 ($1-$7,799)").get(lgaCode)); persons[2] +=
	 * Double.valueOf(census.get("$150-$299 ($7,800-$15,599)").get(lgaCode));
	 * persons[3] +=
	 * Double.valueOf(census.get("$300-$399 ($15,600-$20,799)").get(lgaCode));
	 * persons[4] +=
	 * Double.valueOf(census.get("$400-$499 ($20,800-$25,999)").get(lgaCode));
	 * persons[5] +=
	 * Double.valueOf(census.get("$500-$649 ($26,000-$33,799)").get(lgaCode));
	 * persons[6] +=
	 * Double.valueOf(census.get("$650-$799 ($33,800-$41,599)").get(lgaCode));
	 * persons[7] +=
	 * Double.valueOf(census.get("$800-$999 ($41,600-$51,999)").get(lgaCode));
	 * persons[8] +=
	 * Double.valueOf(census.get("$1,000-$1,249 ($52,000-$64,999)").get(lgaCode));
	 * persons[9] +=
	 * Double.valueOf(census.get("$1,250-$1,499 ($65,000-$77,999)").get(lgaCode));
	 * persons[10] +=
	 * Double.valueOf(census.get("$1,500-$1,749 ($78,000-$90,999)").get(lgaCode));
	 * persons[11] +=
	 * Double.valueOf(census.get("$1,750-$1,999 ($91,000-$103,999)").get(lgaCode));
	 * persons[12] +=
	 * Double.valueOf(census.get("$2,000-$2,999 ($104,000-$155,999)").get(lgaCode));
	 * persons[13] +=
	 * Double.valueOf(census.get("$3,000 or more ($156,000 or more)").get(lgaCode));
	 * double unadjustedTotalPersons = 0d; for (int i = 0; i < numBins; i++) {
	 * unadjustedTotalPersons += persons[i]; }
	 * 
	 * // get total employee income, and calculate mean income of highest bracket
	 * double totalIncome = Double
	 * .valueOf(data.getAbs6524_055_002EmployeeTable5().get(financialYear).get(
	 * "Income").get(lgaCode)); double highestBracketIncome = totalIncome; for (int
	 * i = 0; i < numBins - 1; i++) { meanIncome[i] = (from[i] + to[i]) / 2d;
	 * highestBracketIncome -= persons[i] * meanIncome[i]; } meanIncome[numBins - 1]
	 * = highestBracketIncome / persons[numBins - 1]; to[numBins - 1] =
	 * meanIncome[numBins - 1] + (meanIncome[numBins - 1] - from[numBins - 1]);
	 * 
	 * // multiply by adjusted LGA population double factor =
	 * this.data.getAdjustedPeopleByLga(lgaCode, date) / unadjustedTotalPersons;
	 * Double[] personsAdjusted = new Double[numBins]; for (int i = 0; i < numBins;
	 * i++) { personsAdjusted[i] = persons[i] * factor; }
	 * 
	 * // populate result Map<String, List<Double>> result = new HashMap<String,
	 * List<Double>>(); result.put("from", Arrays.asList(from)); result.put("to",
	 * Arrays.asList(to)); result.put("meanIncome", Arrays.asList(meanIncome));
	 * result.put("adjustedPeople", Arrays.asList(personsAdjusted));
	 * 
	 * return result; }
	 * 
	 * private Map<String, List<Double>> segmentLgaByMrerd(String lgaCode, String
	 * financialYear, Date date) { // local constants final int numBins = 19;
	 * 
	 * // local variables Double[] from = new Double[] { 0d, 1d * 12d, 150d * 12d,
	 * 300d * 12d, 450d * 12d, 600d * 12d, 800d * 12d, 1000d * 12d, 1200d * 12d,
	 * 1400d * 12d, 1600d * 12d, 1800d * 12d, 2000d * 12d, 2200d * 12d, 2400d * 12d,
	 * 2600d * 12d, 3000d * 12d, 4000d * 12d, 5000d * 12d }; Double[] to = new
	 * Double[] { 0d, 1d * 149d, 299d * 12d, 449d * 12d, 599d * 12d, 799d * 12d,
	 * 999d * 12d, 1199d * 12d, 1399d * 12d, 1599d * 12d, 1799d * 12d, 1999d * 12d,
	 * 2999d * 12d, 3999d * 12d, 4999d * 12d, 5999d * 12d }; // the last element is
	 * calculated in the code as a balancing item Double[] meanMortgageRepayments =
	 * new Double[numBins]; Double[] dwellings = new Double[numBins];
	 * 
	 * // get number of people from census data LGA by INCP Map<String, Map<String,
	 * String>> census = this.data.getCensusLgaByMRERD(); dwellings[0] =
	 * Double.valueOf(census.get("Nil repayments").get(lgaCode)); // persons[0] +=
	 * Double.valueOf(census.get("Not stated").get(lgaCode)); // // exclude these
	 * from the sample dwellings[0] +=
	 * Double.valueOf(census.get("Not applicable").get(lgaCode)); dwellings[1] +=
	 * Double.valueOf(census.get("$1-$149").get(lgaCode)); dwellings[2] +=
	 * Double.valueOf(census.get("$150-$299").get(lgaCode)); dwellings[3] +=
	 * Double.valueOf(census.get("$300-$449").get(lgaCode)); dwellings[4] +=
	 * Double.valueOf(census.get("$450-$599").get(lgaCode)); dwellings[5] +=
	 * Double.valueOf(census.get("$600-$799").get(lgaCode)); dwellings[6] +=
	 * Double.valueOf(census.get("$800-$999").get(lgaCode)); dwellings[7] +=
	 * Double.valueOf(census.get("$1,000-$1,199").get(lgaCode)); dwellings[8] +=
	 * Double.valueOf(census.get("$1,200-$1,399").get(lgaCode)); dwellings[9] +=
	 * Double.valueOf(census.get("$1,400-$1,599").get(lgaCode)); dwellings[10] +=
	 * Double.valueOf(census.get("$1,600-$1,799").get(lgaCode)); dwellings[11] +=
	 * Double.valueOf(census.get("$1,800-$1,999").get(lgaCode)); dwellings[12] +=
	 * Double.valueOf(census.get("$2,000-$2,199").get(lgaCode)); dwellings[13] +=
	 * Double.valueOf(census.get("$2,200-$2,399").get(lgaCode)); dwellings[14] +=
	 * Double.valueOf(census.get("$2,400-$2,599").get(lgaCode)); dwellings[15] +=
	 * Double.valueOf(census.get("$2,600-$2,999").get(lgaCode)); dwellings[16] +=
	 * Double.valueOf(census.get("$3,000-$3,999").get(lgaCode)); dwellings[17] +=
	 * Double.valueOf(census.get("$4,000-$4,999").get(lgaCode)); dwellings[18] +=
	 * Double.valueOf(census.get("$5000 and over").get(lgaCode)); double
	 * unadjustedTotalDwellings = 0d; for (int i = 0; i < numBins; i++) {
	 * unadjustedTotalDwellings += dwellings[i]; meanMortgageRepayments[i] =
	 * (from[i] + to[i]) / 2d; }
	 * 
	 * // multiply by adjusted LGA population // TODO: use ABS 3236.0 to adjust this
	 * to the projected number of dwellings. double factor =
	 * this.data.getAdjustedPeopleByLga(lgaCode, date) / unadjustedTotalDwellings;
	 * Double[] dwellingsAdjusted = new Double[numBins]; for (int i = 0; i <
	 * numBins; i++) { dwellingsAdjusted[i] = dwellings[i] * factor; }
	 * 
	 * // populate result Map<String, List<Double>> result = new HashMap<String,
	 * List<Double>>(); result.put("from", Arrays.asList(from)); result.put("to",
	 * Arrays.asList(to)); result.put("meanMortgageRepayments",
	 * Arrays.asList(meanMortgageRepayments)); result.put("adjustedDwellings",
	 * Arrays.asList(dwellingsAdjusted));
	 * 
	 * return result; }
	 */

	/**
	 * @param data the data to set
	 */
	@Autowired
	public void setCommonData(CalibrationData commonData) {
		this.commonData = commonData;
	}

	private String getStateCode(String stateName) {
		String stateCode = "NA";
		switch (stateName.toUpperCase()) {
		case "NSW":
		case "NEW SOUTH WALES":
			stateCode = "NSW";
			break;
		case "VIC":
		case "VICTORIA":
			stateCode = "VIC";
			break;
		case "QLD":
		case "QUEENSLAND":
			stateCode = "QLD";
			break;
		case "SA":
		case "SOUTH AUTSTRALIA":
			stateCode = "SA";
			break;
		case "WA":
		case "WESTERN AUSTRALIA":
			stateCode = "WA";
			break;
		case "TAS":
		case "TASMANIA":
			stateCode = "TAS";
			break;
		case "NT":
		case "NORTHERN TERRITORY":
			stateCode = "NT";
			break;
		case "ACT":
		case "AUSTRALIAN CAPITAL TERRITORY":
			stateCode = "ACT";
			break;
		default:
			stateCode = "OT";
			break;
		}
		return stateCode;
	}

	/**
	 * @param individualData the individualData to set
	 */
	@Autowired
	public void setIndividualData(CalibrationDataIndividual individualData) {
		this.individualData = individualData;
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

}
