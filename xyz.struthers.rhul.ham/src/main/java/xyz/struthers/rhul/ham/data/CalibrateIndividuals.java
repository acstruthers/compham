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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import gnu.trove.map.hash.TObjectFloatHashMap;
import xyz.struthers.lang.CustomMath;
import xyz.struthers.rhul.ham.agent.Individual;
import xyz.struthers.rhul.ham.config.Properties;
import xyz.struthers.rhul.ham.process.Tax;

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

	private static final boolean DEBUG = true;
	private static final boolean DEBUG_DETAIL = false;

	// CONSTANTS
	private static final float EPSILON = 0.1f; // to round business counts so the integer sums match
	public static final float NUM_MONTHS = 12f;
	public static final String CALIBRATION_DATE_ATO = "01/06/2018";
	public static final String CALIBRATION_DATE_RBA = "30/06/2018";
	public static final float POPULATION_MULTIPLIER = 1.2f; // HACK: forces it up to the right number of people

	// map optimisation
	public static final float MAP_LOAD_FACTOR = 0.75f;
	public static final int MAP_LGA_INIT_CAPACITY = (int) Math.ceil(540 / MAP_LOAD_FACTOR) + 1;
	public static final String[] SEX_ARRAY = { "M", "F" };
	private static final String[] STATES_ARRAY = { "NSW", "VIC", "QLD", "SA", "WA", "TAS", "NT", "ACT", "OT" };
	private static final int[] MAP_STATE_POA_CAPACITY = { (int) Math.ceil(612 / MAP_LOAD_FACTOR) + 1,
			(int) Math.ceil(382 / MAP_LOAD_FACTOR) + 1, (int) Math.ceil(429 / MAP_LOAD_FACTOR) + 1,
			(int) Math.ceil(326 / MAP_LOAD_FACTOR) + 1, (int) Math.ceil(338 / MAP_LOAD_FACTOR) + 1,
			(int) Math.ceil(111 / MAP_LOAD_FACTOR) + 1, (int) Math.ceil(43 / MAP_LOAD_FACTOR) + 1,
			(int) Math.ceil(25 / MAP_LOAD_FACTOR) + 1, (int) Math.ceil(1 / MAP_LOAD_FACTOR) + 1 };

	// data series titles
	public static final String[] AGE_ARRAY_ATO = { "a. Under 18", "b. 18 - 24", "c. 25 - 29", "d. 30 - 34",
			"e. 35 - 39", "f. 40 - 44", "g. 45 - 49", "h. 50 - 54", "i. 55 - 59", "j. 60 - 64", "k. 65 - 69",
			"l. 70 - 74", "m. 75 and over" };
	public static final String[] AGE_ARRAY_ABS = { "0-4 years", "5-9 years", "10-14 years", "15-19 years",
			"20-24 years", "25-29 years", "30-34 years", "35-39 years", "40-44 years", "45-49 years", "50-54 years",
			"55-59 years", "60-64 years", "65-69 years", "70-74 years", "75-79 years", "80-84 years", "85-89 years",
			"90-94 years", "95-99 years", "100 years and over" };
	public static final int[] AGE_ARRAY_ABS_MIDPOINT = { 2, 7, 12, 17, 22, 27, 32, 37, 42, 47, 52, 57, 62, 67, 72, 77,
			82, 87, 92, 97, 102 };
	public static final String[] DIVISION_CODE_ARRAY = { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L",
			"M", "N", "O", "P", "Q", "R", "S" }; // S = Other Services
	public static final char[] DIVISION_CODE_CHAR_ARRAY = { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L',
			'M', 'N', 'O', 'P', 'Q', 'R', 'S' }; // S = Other Services
	public static final int NUM_DIVISIONS = DIVISION_CODE_ARRAY.length; // 19
	public static final String[] INDIVIDUAL_INCOME_RANGES_ABS = { "Negative income", "Nil income",
			"$1-$149 ($1-$7,799)", "$150-$299 ($7,800-$15,599)", "$300-$399 ($15,600-$20,799)",
			"$400-$499 ($20,800-$25,999)", "$500-$649 ($26,000-$33,799)", "$650-$799 ($33,800-$41,599)",
			"$800-$999 ($41,600-$51,999)", "$1,000-$1,249 ($52,000-$64,999)", "$1,250-$1,499 ($65,000-$77,999)",
			"$1,500-$1,749 ($78,000-$90,999)", "$1,750-$1,999 ($91,000-$103,999)", "$2,000-$2,999 ($104,000-$155,999)",
			"$3,000 or more ($156,000 or more)", "Not stated" };
	public static final int NUM_INDIVIDUAL_INCOME_RANGES_ABS = 14;
	private static final String[] INDIVIDUAL_INCOME_RANGES_ATO3A = { "a. $6,000 or less", "b. $6,001 to $10,000",
			"c. $10,001 to $18,200", "d. $18,201 to $25,000", "e. $25,001 to $30,000", "f. $30,001 to $37,000",
			"g. $37,001 to $40,000", "h. $40,001 to $45,000", "i. $45,001 to $50,000", "j. $50,001 to $55,000",
			"k. $55,001 to $60,000", "l. $60,001 to $70,000", "m. $70,001 to $80,000", "n. $80,001 to $87,000",
			"o. $87,001 to $90,000", "p. $90,001 to $100,000", "q. $100,001 to $150,000", "r. $150,001 to $180,000",
			"s. $180,001 to $250,000", "t. $250,001 to $500,000", "u. $500,001 to $1,000,000",
			"v. $1,000,001 or more" };
	private static final String[] TAXABLE_STATUS = { "Y", "N" };
	public static final String[] MAIN_INCOME_SOURCE = { "Employed", "Unemployed", "Pension", "Self-funded Retiree",
			"Foreign Income", "No Income" };
	public static final String[] PNL_COMPOSITION = { "Interest Income", "Dividend Income", "Donations", "Rent Income",
			"Rent Interest Deduction", "Other Income", "Student Loan" };

	private static final String ATO_2A_TITLE_TAXABLE_COUNT = "Taxable income or loss3 no.";
	private static final String ATO_2A_TITLE_TAXABLE_AMOUNT = "Taxable income or loss3 $";

	// private static final String ATO_3A_TITLE_TAXABLE_COUNT = "Taxable income or
	// loss2 no.";
	// private static final String ATO_3A_TITLE_TAXABLE_AMOUNT = "Taxable income or
	// loss2 $";
	private static final String ATO_3A_TITLE_TOTAL_INCOME_COUNT = "Total Income or Loss2 no.";
	private static final String ATO_3A_TITLE_TOTAL_INCOME_AMOUNT = "Total Income or Loss2 $";
	private static final String ATO_3A_TITLE_SALARY_COUNT = "Salary or wages no.";
	private static final String ATO_3A_TITLE_SALARY_AMOUNT = "Salary or wages $";
	private static final String ATO_3A_TITLE_ALLOWANCES_COUNT = "Allowances earnings tips directors fees etc no.";
	private static final String ATO_3A_TITLE_ALLOWANCES_AMOUNT = "Allowances earnings tips directors fees etc $";
	private static final String ATO_3A_TITLE_GOVT_ALLOW_COUNT = "Australian government allowances and payments no.";
	private static final String ATO_3A_TITLE_GOVT_ALLOW_AMOUNT = "Australian government allowances and payments $";
	private static final String ATO_3A_TITLE_GOVT_PENSION_COUNT = "Australian government pensions and allowances no.";
	private static final String ATO_3A_TITLE_GOVT_PENSION_AMOUNT = "Australian government pensions and allowances $";
	private static final String ATO_3A_TITLE_SUPER_TAXED_COUNT = "Australian annuities and superannuation income streams taxable component taxed element no.";
	private static final String ATO_3A_TITLE_SUPER_TAXED_AMOUNT = "Australian annuities and superannuation income streams taxable component taxed element $";
	private static final String ATO_3A_TITLE_SUPER_UNTAXED_COUNT = "Australian annuities and superannuation income streams taxable component untaxed element no.";
	private static final String ATO_3A_TITLE_SUPER_UNTAXED_AMOUNT = "Australian annuities and superannuation income streams taxable component untaxed element $";
	private static final String ATO_3A_TITLE_INTEREST_INCOME_COUNT = "Gross interest no."; // if they have no interest,
																							// their cash is half of a
																							// week's pay
	private static final String ATO_3A_TITLE_INTEREST_INCOME_AMOUNT = "Gross interest $";
	private static final String ATO_3A_TITLE_DIVIDENDS_UNFRANKED_COUNT = "Dividends unfranked no.";
	private static final String ATO_3A_TITLE_DIVIDENDS_UNFRANKED_AMOUNT = "Dividends unfranked $";
	private static final String ATO_3A_TITLE_DIVIDENDS_FRANKED_COUNT = "Dividends franked no.";
	private static final String ATO_3A_TITLE_DIVIDENDS_FRANKED_AMOUNT = "Dividends franked $";
	private static final String ATO_3A_TITLE_WORK_RELATED_EXP_COUNT = "Total work related expenses no.";
	private static final String ATO_3A_TITLE_WORK_RELATED_EXP_AMOUNT = "Total work related expenses $";
	// private static final String ATO_3A_TITLE_INTEREST_DEDUCTIONS_COUNT =
	// "Interest deductions no.";
	// private static final String ATO_3A_TITLE_INTEREST_DEDUCTIONS_AMOUNT =
	// "Interest deductions $";
	private static final String ATO_3A_TITLE_DONATIONS_COUNT = "Gifts or donations no.";
	private static final String ATO_3A_TITLE_DONATIONS_AMOUNT = "Gifts or donations $";
	private static final String ATO_3A_TITLE_FOREIGN_INCOME_COUNT = "Foreign source income assessable foreign source income no.";
	private static final String ATO_3A_TITLE_FOREIGN_INCOME_AMOUNT = "Foreign source income assessable foreign source income $";
	private static final String ATO_3A_TITLE_FOREIGN_INCOME2_COUNT = "Foreign source income other net foreign source income no.";
	private static final String ATO_3A_TITLE_FOREIGN_INCOME2_AMOUNT = "Foreign source income other net foreign source income $";
	private static final String ATO_3A_TITLE_RENT_INCOME_COUNT = "Gross rent no.";
	private static final String ATO_3A_TITLE_RENT_INCOME_AMOUNT = "Gross rent $";
	private static final String ATO_3A_TITLE_RENT_INTEREST_COUNT = "Rent interest deductions no.";
	private static final String ATO_3A_TITLE_RENT_INTEREST_AMOUNT = "Rent interest deductions $";
	private static final String ATO_3A_TITLE_HELP_DEBT_COUNT = "HELP debt balance no."; // HECS became HELP in Jan-05
	private static final String ATO_3A_TITLE_HELP_DEBT_AMOUNT = "HELP debt balance $";
	private static final String ATO_3A_TITLE_SFSS_DEBT_COUNT = "SFSS debt balance no."; // SFSS closed on 31-Dec-03
	private static final String ATO_3A_TITLE_SFSS_DEBT_AMOUNT = "SFSS debt balance $";
	private static final String ATO_3A_TITLE_TSL_DEBT_COUNT = "TSL debt balance no."; // from 2014, capped at $20,808
																						// over 4 years
	private static final String ATO_3A_TITLE_TSL_DEBT_AMOUNT = "TSL debt balance $";

	private static final String ATO_9_TITLE_TAXABLE_COUNT = "Taxable income or loss4 no.";
	private static final String ATO_9_TITLE_TAXABLE_AMOUNT = "Taxable income or loss4 $";

	private static final String ATO_6B_TITLE_TAXABLE_COUNT = "Taxable income or loss3 no.";
	private static final String ATO_6B_TITLE_TAXABLE_AMOUNT = "Taxable income or loss3 $";

	// beans
	private AreaMapping area;
	private CalibrationDataIndividual individualData;
	private Properties properties;

	// field variables
	private Random random;
	/**
	 * The matrix of Individual agents.<br>
	 * Keys: postcode, sex, age, industry division, income (ABS categories)
	 */
	private List<List<List<List<List<List<Individual>>>>>> individualMatrix;
	/**
	 * A simplified map to make it easier to populate Households.<br>
	 * Keys: LGA, AGE5P, INCP.<br>
	 * Values: an ArrayList of Individuals for that combination of keys.
	 */
	private Map<String, Map<String, Map<String, ArrayList<Individual>>>> individualMap;
	// private ArrayList<Individual> individualAgents;
	private Date calibrationDateAto;
	private int totalPopulationAU;
	private float populationMultiplier;
	// private Map<String, Integer> lgaPeopleCount; // adjusted to 2018
	private Map<String, Integer> poaIndexMap;

	private static int agentId = 0;

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
	// private Map<String, Map<String, String>> abs1292_0_55_002ANZSIC;
	/**
	 * ATO Individuals Table 2A<br>
	 * Contains P&L and people count by sex and 5-year age range.<br>
	 * Keys: Series Title, State, Age, Gender, Taxable Status, Lodgment Method
	 */
	private Map<String, Map<String, Map<String, Map<String, Map<String, TObjectFloatHashMap<String>>>>>> atoIndividualTable2a;
	/**
	 * ATO Individuals Table 3A<br>
	 * Contains P&L and people count by sex, 5-year age range, and income range.<br>
	 * Keys: Series Title, Income Range, Age, Gender, Taxable Status
	 */
	private Map<String, Map<String, Map<String, Map<String, TObjectFloatHashMap<String>>>>> atoIndividualTable3a;
	/**
	 * ATO Individuals Table 6B<br>
	 * Contains P&L and people count by post code.<br>
	 * Keys: Series Title, Post Code
	 */
	private Map<String, TObjectFloatHashMap<String>> atoIndividualTable6b;
	/**
	 * ATO Individuals Table 9 (Industry Division summary)<br>
	 * Contains count and taxable income, summarised by industry division.<br>
	 * Keys: Series Title, Industry Division Code
	 */
	private Map<String, Map<String, Float>> atoIndividualTable9DivisionSummary;
	/**
	 * ABS Census Table Builder data:<br>
	 * SEXP by POA (UR) by AGE5P, INDP and INCP<br>
	 * Individual income by industry and demographic.
	 * 
	 * Keys: Age5, Industry Division, Personal Income, POA, Sex<br>
	 * Values: Number of persons
	 */
	//private Map<String, Map<String, Map<String, Map<String, Map<String, Float>>>>> censusSEXP_POA_AGE5P_INDP_INCP;
	private Map<String, Map<String, Map<String, Map<String, TObjectFloatHashMap<String>>>>> censusSEXP_POA_AGE5P_INDP_INCP;

	/**
	 * 
	 */
	public CalibrateIndividuals() {
		super();
		this.init();
	}

	private void init() {
		this.individualMatrix = null;
		this.individualMap = null;
		this.calibrationDateAto = null;
		this.totalPopulationAU = 0;
		this.populationMultiplier = 0f;
		// this.lgaPeopleCount = null;
		this.poaIndexMap = null;

		// data sources
		this.atoIndividualTable2a = null;
		this.atoIndividualTable3a = null;
		this.atoIndividualTable6b = null;
		this.atoIndividualTable9DivisionSummary = null;
		this.censusSEXP_POA_AGE5P_INDP_INCP = null;
	}

	/**
	 * A destructor to free up the resources used by this class.
	 * 
	 * Performs a deep delete of variables that won't be needed anymore, then
	 * invokes the garbage collector. The purpose is to free up as much RAM as
	 * possible ready for Household calibration, and then the simulation itself.
	 */
	public void close() {
		long memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

		// leave economy, Individual matrix and Individual List alone

		// just make these null because the classes they came from will do a deep delete
		// at an appropriate time
		this.area = null;
		this.properties = null;
		this.random = null;
		// this.abs1292_0_55_002ANZSIC = null;
		this.atoIndividualTable2a = null;
		this.atoIndividualTable3a = null;
		this.atoIndividualTable6b = null;
		this.atoIndividualTable9DivisionSummary = null;
		this.censusSEXP_POA_AGE5P_INDP_INCP = null;

		// finished with Individual-specific data, so perform a deep delete
		this.individualData.close();
		this.individualData = null;

		// invoke garbage collector
		System.gc();

		// report how much RAM was released
		long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		float megabytesConsumed = (memoryAfter - memoryBefore) / 1024f / 1024f;
		DecimalFormat decimalFormatter = new DecimalFormat("#,##0.00");
		System.out.println(">>> Memory released after creating Individual agents: "
				+ decimalFormatter.format(megabytesConsumed) + "MB");
		System.out.println(
				">>> Current memory consumption: " + decimalFormatter.format(memoryAfter / 1024f / 1024f) + "MB");
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
	 * PART A: ESTIMATING INDIVIDUAL PROFIT & LOSS STATEMENT MULTIPLIERS
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
	 * ------------------------------------------------------------------------<br>
	 * PART B: INDIVIDUAL COUNTS AND POA POPULATION MULTIPLIERS
	 * ------------------------------------------------------------------------<br>
	 *
	 * 4. Census sex, age, industry & income per POA: Get total population
	 * multiplier, and adjust each count so it's now a 2018 equivalent. Map POA to
	 * LGA, make a list of POAs in each LGA (which will be used when assigning
	 * Individuals to Households).<br>
	 * 
	 * 5. Calculate POA population multipliers to adjust ATO 3A P&L statements so
	 * the POA totals equal the POA counts for people with an income.
	 * 
	 * ------------------------------------------------------------------------<br>
	 * PART C: CREATING INDIVIDUAL AGENTS
	 * ------------------------------------------------------------------------<br>
	 *
	 * 6. ATO Individual Table 3A: age/sex count, P&L, Help Debt (by income range).
	 * Financial position will vary more by income than any other metric, so use
	 * table 3A as the base amounts and counts to adjust. Use these amounts, looping
	 * through the extra dimensions and multiplying by the multipliers above. The
	 * number of people is given in table 3A, and only needs to be multiplied by the
	 * population forecast multiplier to adjust them forward to 2018 figures. Need
	 * to assign LGA from POA first so we can get the ratio to adjust the population
	 * by. This ensures, for example, that the right number of people are assigned a
	 * HELP debt, etc.
	 * 
	 * 7. Now start an POA loop, creating Individual agents per ATO data, and
	 * Individuals with no income per ABS data for the other people in each
	 * category. Store them in a map/matrix for now so they're easy to assign into
	 * Households. These are the actual objects that will be used in the model, but
	 * the matrix itself can be dropped once the Individuals are in Households and
	 * calibration is complete.
	 * 
	 * N.B. Need to think about HELP debt and work out how to assign it by age &
	 * gender, not diminish it by including it in ratios too early.
	 * 
	 * This gives P&L and Bal Sht by sex, age, industry division, income, POA
	 * 
	 * ------------------------------------------------------------------------<br>
	 * PART D: HOUSEHOLD COUNTS
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
	 * PART E: ADJUSTING FINANCIALS FOR HOUSEHOLDS
	 * ------------------------------------------------------------------------<br>
	 * 
	 * 10. RBA E2: Use the household debt-to-income and assets-to-income ratios to
	 * calculate total assets and total debt.<br>
	 * 
	 * 11. RBA E1: Calculate the ratios between household Bal Sht items. Use these,
	 * compared to assets and debt, to estimate the other balance sheet items.<br>
	 * 
	 * ROUGH ALGORITHM FOR HOUSEHOLD ADJUSTMENTS:<br>
	 * - Henderson poverty line based on family composition to be a proxy for
	 * inelastic expenses.<br>
	 * - Generally just one mortgage or rent payment per household. This should
	 * probably factor into the algorithm that assigns individuals to
	 * households.<br>
	 * - Reverse engineer home loan size and property price based on mortgage
	 * repayments, banks' interest rate, and how many years into the loan they are.
	 * Assume people buy homes at age 30. If they're younger than that, assume they
	 * just bought it. Might not need the LGA home price data from ABS 1410.0
	 * Economy, so try this algorithm first since I'm going to implement it anyway.
	 * 
	 */
	public void createIndividualAgents() {
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
			this.calibrationDateAto = sdf.parse(CALIBRATION_DATE_ATO);
			// this.calibrationDateRba = sdf.parse(CALIBRATION_DATE_RBA);
		} catch (ParseException e) {
			e.printStackTrace();
		}

		// get raw calibration data
		// this.abs1292_0_55_002ANZSIC = this.commonData.getAbs1292_0_55_002ANZSIC();
		this.atoIndividualTable2a = this.individualData.getAtoIndividualTable2a();
		this.atoIndividualTable3a = this.individualData.getAtoIndividualTable3a();
		this.atoIndividualTable6b = this.individualData.getAtoIndividualTable6b();
		this.atoIndividualTable9DivisionSummary = this.individualData.getAtoIndividualTable9DivisionSummary();
		this.censusSEXP_POA_AGE5P_INDP_INCP = this.individualData.getCensusSEXP_POA_AGE5P_INDP_INCP();

		// get key metrics that will be used across all the data
		// this.lgaPeopleCount =
		// this.area.getAdjustedPeopleByLga(this.calibrationDateAto);
		// this.lgaDwellingsCount =
		// this.area.getAdjustedDwellingsByLga(this.calibrationDateAto);
		this.totalPopulationAU = this.area.getTotalPopulation(this.calibrationDateAto);
		this.populationMultiplier = this.area.getPopulationMultiplier(this.calibrationDateAto);
		// Set<String> lgaCodes = this.lgaPeopleCount.keySet();

		/*
		 * ------------------------------------------------------------------------<br>
		 * PART A: ESTIMATING INDIVIDUAL PROFIT & LOSS STATEMENT MULTIPLIERS
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
		System.out.println(new Date(System.currentTimeMillis()) + ": 1. ATO Individual Table 9 industry multipliers");

		Map<String, Float> divisionTaxableIncomeMultiplier = new HashMap<String, Float>(NUM_DIVISIONS);
		Map<String, Float> divisionCountMultiplier = new HashMap<String, Float>(NUM_DIVISIONS);
		Set<String> divisionCodeSet = this.atoIndividualTable9DivisionSummary.get(ATO_9_TITLE_TAXABLE_COUNT).keySet();
		float divTotalCount9A = 0f;
		float divTotalAmount9A = 0f;
		for (String divCode : divisionCodeSet) {
			// calculate average income per division, and total of all divisions
			float divTaxableCount = this.atoIndividualTable9DivisionSummary.get(ATO_9_TITLE_TAXABLE_COUNT).get(divCode);
			float divTaxableAmount = this.atoIndividualTable9DivisionSummary.get(ATO_9_TITLE_TAXABLE_AMOUNT)
					.get(divCode);
			float divTaxablePerPerson = divTaxableAmount / divTaxableCount;
			// Just an efficient place to hold this. Will be overwritten in the loop below.
			divisionTaxableIncomeMultiplier.put(divCode, divTaxablePerPerson);
			divisionCountMultiplier.put(divCode, divTaxableCount);
			divTotalCount9A += divTaxableCount;
			divTotalAmount9A += divTaxableAmount;
		}
		float divTotalTaxablePerPerson = divTotalCount9A > 0f ? divTotalAmount9A / divTotalCount9A : 0f;
		if (DEBUG_DETAIL) {
			System.out.println("########################");
			System.out.println("# DIVISION MULTIPLIERS #");
			System.out.println("########################");
		}
		for (String divCode : divisionCodeSet) {
			float divTaxableIncomeMultiplier = divisionTaxableIncomeMultiplier.get(divCode) / divTotalTaxablePerPerson;
			float divCountMultiplier = divisionCountMultiplier.get(divCode) / divTotalCount9A;
			divisionTaxableIncomeMultiplier.put(divCode, divTaxableIncomeMultiplier);
			divisionCountMultiplier.put(divCode, divCountMultiplier);
			if (DEBUG_DETAIL) {
				System.out
						.println(divCode + " count: " + divCountMultiplier + ", amount: " + divTaxableIncomeMultiplier);
			}
		}
		if (DEBUG_DETAIL) {
			System.out.println("#################################");
			System.out.println("# DIVISION MULTIPLIERS FINISHED #");
			System.out.println("#################################");
		}

		/*
		 * 2. ATO Individual Table 2A: age/sex count, P&L, Help Debt (by State).
		 * Calculate state multiplier for every age/sex combination.<br>
		 */
		System.out.println(new Date(System.currentTimeMillis()) + ": 2. ATO Individual Table 2A state multipliers");

		// Keys: Series Title, State, Age, Gender, Taxable Status, Lodgment Method

		// initialise state multiplier map
		// Keys for stateMultiplier: Sex (2), Age (13), State (9)
		int sexMapCapacity = (int) Math.ceil(SEX_ARRAY.length / MAP_LOAD_FACTOR);
		int ageMapCapacity = (int) Math.ceil(AGE_ARRAY_ATO.length / MAP_LOAD_FACTOR);
		int stateMapCapacity = (int) Math.ceil(STATES_ARRAY.length / MAP_LOAD_FACTOR);
		Map<String, Map<String, Map<String, Float>>> stateTaxableCount = new HashMap<String, Map<String, Map<String, Float>>>(
				sexMapCapacity);
		Map<String, Map<String, Map<String, Float>>> stateTaxableAmount = new HashMap<String, Map<String, Map<String, Float>>>(
				sexMapCapacity);
		Map<String, Map<String, Map<String, Float>>> stateTaxableIncomeMultiplier = new HashMap<String, Map<String, Map<String, Float>>>(
				sexMapCapacity);
		Map<String, Map<String, Map<String, Float>>> stateCountMultiplier = new HashMap<String, Map<String, Map<String, Float>>>(
				sexMapCapacity);
		Map<String, Map<String, Float>> stateSexAgeNationalTotalCount = new HashMap<String, Map<String, Float>>(
				sexMapCapacity);
		Map<String, Map<String, Float>> stateSexAgeNationalTotalAmount = new HashMap<String, Map<String, Float>>(
				sexMapCapacity);
		for (String sex : SEX_ARRAY) {
			stateTaxableCount.put(sex, new HashMap<String, Map<String, Float>>(ageMapCapacity));
			stateTaxableAmount.put(sex, new HashMap<String, Map<String, Float>>(ageMapCapacity));
			stateTaxableIncomeMultiplier.put(sex, new HashMap<String, Map<String, Float>>(ageMapCapacity));
			stateCountMultiplier.put(sex, new HashMap<String, Map<String, Float>>(ageMapCapacity));
			stateSexAgeNationalTotalCount.put(sex, new HashMap<String, Float>(ageMapCapacity));
			stateSexAgeNationalTotalAmount.put(sex, new HashMap<String, Float>(ageMapCapacity));
			for (String age : AGE_ARRAY_ATO) {
				stateTaxableCount.get(sex).put(age, new HashMap<String, Float>(stateMapCapacity));
				stateTaxableAmount.get(sex).put(age, new HashMap<String, Float>(stateMapCapacity));
				stateTaxableIncomeMultiplier.get(sex).put(age, new HashMap<String, Float>(stateMapCapacity));
				stateCountMultiplier.get(sex).put(age, new HashMap<String, Float>(stateMapCapacity));
				stateSexAgeNationalTotalCount.get(sex).put(age, 0f);
				stateSexAgeNationalTotalAmount.get(sex).put(age, 0f);
				for (String state : STATES_ARRAY) {
					stateTaxableCount.get(sex).get(age).put(state, 0f);
					stateTaxableAmount.get(sex).get(age).put(state, 0f);
					stateTaxableIncomeMultiplier.get(sex).get(age).put(state, 0f);
					stateCountMultiplier.get(sex).get(age).put(state, 0f);
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
							float oldCountVal = stateTaxableCount.get(sex).get(age).get(stateCode);
							float newCountVal = this.atoIndividualTable2a.get(ATO_2A_TITLE_TAXABLE_COUNT).get(state)
									.get(age).get(sex).get(taxableStatus).get(lodgmentStatus);
							stateTaxableCount.get(sex).get(age).put(stateCode, oldCountVal + newCountVal);
							float oldTotalCount = stateSexAgeNationalTotalCount.get(sex).get(age);
							stateSexAgeNationalTotalCount.get(sex).put(age, oldTotalCount + newCountVal);

							float oldAmountVal = stateTaxableAmount.get(sex).get(age).get(stateCode);
							float newAmountVal = this.atoIndividualTable2a.get(ATO_2A_TITLE_TAXABLE_AMOUNT).get(state)
									.get(age).get(sex).get(taxableStatus).get(lodgmentStatus);
							stateTaxableAmount.get(sex).get(age).put(stateCode, oldAmountVal + newAmountVal);
							float oldTotalAmount = stateSexAgeNationalTotalAmount.get(sex).get(age);
							stateSexAgeNationalTotalAmount.get(sex).put(age, oldTotalAmount + newAmountVal);
						}
					}
				}
			}
		}

		if (DEBUG_DETAIL) {
			System.out.println("#####################");
			System.out.println("# STATE MULTIPLIERS #");
			System.out.println("#####################");
		}
		// calculate average income per state, and national average
		for (String sex : SEX_ARRAY) {
			for (String age : AGE_ARRAY_ATO) {
				float sexAgeCount = stateSexAgeNationalTotalCount.get(sex).get(age);
				float sexAgeAmount = stateSexAgeNationalTotalAmount.get(sex).get(age);
				float sexAgeTaxablePerPerson = sexAgeCount > 0f ? sexAgeAmount / sexAgeCount : 0f;
				for (String state : STATES_ARRAY) {
					float stateCount = stateTaxableCount.get(sex).get(age).get(state);
					float stateAmount = stateTaxableAmount.get(sex).get(age).get(state);
					float stateTaxablePerPerson = stateAmount / stateCount;
					float thisStateTaxableIncomeMultiplier = sexAgeTaxablePerPerson > 0f
							? stateTaxablePerPerson / sexAgeTaxablePerPerson
							: 0f;
					float thisStateCountMultiplier = sexAgeCount > 0f ? stateCount / sexAgeCount : 0f;
					stateTaxableIncomeMultiplier.get(sex).get(age).put(state, thisStateTaxableIncomeMultiplier);
					stateCountMultiplier.get(sex).get(age).put(state, thisStateCountMultiplier);

					if (DEBUG_DETAIL) {
						System.out.println(sex + " / " + age + " / " + state + " count: " + thisStateCountMultiplier
								+ ", amount: " + thisStateTaxableIncomeMultiplier);
					}
				}
			}
		}
		if (DEBUG_DETAIL) {
			System.out.println("##############################");
			System.out.println("# STATE MULTIPLIERS FINISHED #");
			System.out.println("##############################");
		}

		/*
		 * 3. ATO Individual Table 6B: people count, taxable income (per POA). Calculate
		 * POA multiplier for each POA, by state.<br>
		 */
		System.out.println(new Date(System.currentTimeMillis()) + ": 3. ATO Individual Table 6B postcode multipliers");

		// ATO 6B Keys: Series Title, Post Code
		// postcodeMultiplier Keys: State Code, Post Code
		Map<String, Map<String, Float>> postcodeStateTaxableIncomeMultiplier = new HashMap<String, Map<String, Float>>(
				stateMapCapacity);
		Map<String, Map<String, Float>> postcodeStateCountMultiplier = new HashMap<String, Map<String, Float>>(
				stateMapCapacity);
		Map<String, Float> postcodeStateTotalCount = new HashMap<String, Float>(stateMapCapacity);
		Map<String, Float> postcodeStateTotalAmount = new HashMap<String, Float>(stateMapCapacity);
		Map<String, Float> postcodeStateTaxablePerPerson = new HashMap<String, Float>(stateMapCapacity);

		// initialise state multiplier map
		for (int i = 0; i < STATES_ARRAY.length; i++) {
			String state = STATES_ARRAY[i];
			postcodeStateTaxableIncomeMultiplier.put(state, new HashMap<String, Float>(MAP_STATE_POA_CAPACITY[i]));
			postcodeStateCountMultiplier.put(state, new HashMap<String, Float>(MAP_STATE_POA_CAPACITY[i]));
			postcodeStateTotalCount.put(state, 0f);
			postcodeStateTotalAmount.put(state, 0f);
		}

		Set<String> postcodeSet = this.atoIndividualTable6b.get(ATO_6B_TITLE_TAXABLE_COUNT).keySet();
		for (String postcode : postcodeSet) {
			String state = this.area.getStateFromPoa(postcode);
			if (state != null && state != "Other") {
				// postcode 3694 returns null, so just skip null values for now
				// calculate average income per postcode, and state totals for all postcodes
				float postcodeTaxableCount = this.atoIndividualTable6b.get(ATO_6B_TITLE_TAXABLE_COUNT).get(postcode);
				float postcodeTaxableAmount = this.atoIndividualTable6b.get(ATO_6B_TITLE_TAXABLE_AMOUNT).get(postcode);
				float postcodeTaxablePerPerson = postcodeTaxableAmount / postcodeTaxableCount;

				// Just an efficient place to hold this. Will be overwritten in the loop below.
				postcodeStateTaxableIncomeMultiplier.get(state).put(postcode, postcodeTaxablePerPerson);
				postcodeStateCountMultiplier.get(state).put(postcode, postcodeTaxableCount);
				float oldStateTotalCount = postcodeStateTotalCount.get(state);
				postcodeStateTotalCount.put(state, oldStateTotalCount + postcodeTaxableCount);
				float oldStateTotalAmount = postcodeStateTotalAmount.get(state);
				postcodeStateTotalAmount.put(state, oldStateTotalAmount + postcodeTaxableAmount);
			}
		}

		// calculate state averages
		for (String state : STATES_ARRAY) {
			float stateCount = postcodeStateTotalCount.containsKey(state) ? postcodeStateTotalCount.get(state) : 0f;
			float stateAmount = postcodeStateTotalAmount.containsKey(state) ? postcodeStateTotalAmount.get(state) : 0f;
			postcodeStateTaxablePerPerson.put(state, stateCount > 0f ? stateAmount / stateCount : 0f);
		}

		if (DEBUG_DETAIL) {
			System.out.println("##############################");
			System.out.println("# POSTCODE STATE MULTIPLIERS #");
			System.out.println("##############################");
		}
		// calculate state multipliers
		for (String postcode : postcodeSet) {
			String state = this.area.getStateFromPoa(postcode);
			if (state != null & state != "Other") {
				float stateTaxablePerPerson = postcodeStateTaxablePerPerson.get(state);
				float stateCount = postcodeStateTotalCount.get(state);
				float postcodeTaxableIncomeMultiplier = postcodeStateTaxableIncomeMultiplier.get(state).get(postcode)
						/ stateTaxablePerPerson;
				float postcodeCountMultiplier = postcodeStateCountMultiplier.get(state).get(postcode) / stateCount;
				postcodeStateTaxableIncomeMultiplier.get(state).put(postcode, postcodeTaxableIncomeMultiplier);
				postcodeStateCountMultiplier.get(state).put(postcode, postcodeCountMultiplier);

				if (DEBUG_DETAIL) {
					if (state.equals("NSW")) {
						System.out.println(state + " / " + postcode + " count: " + postcodeCountMultiplier
								+ ", amount: " + postcodeTaxableIncomeMultiplier);
					}
				}
			}
		}
		if (DEBUG_DETAIL) {
			System.out.println("#######################################");
			System.out.println("# POSTCODE STATE MULTIPLIERS FINISHED #");
			System.out.println("#######################################");
		}

		/*
		 * ------------------------------------------------------------------------<br>
		 * PART B: INDIVIDUAL COUNTS AND POA POPULATION MULTIPLIERS
		 * ------------------------------------------------------------------------<br>
		 *
		 * 4. Census sex, age, industry & income per POA: Get total population
		 * multiplier, and adjust each count so it's now a 2018 equivalent. Map POA to
		 * LGA, make a list of POAs in each LGA (which will be used when assigning
		 * Individuals to Households).<br>
		 */
		System.out.println(new Date(System.currentTimeMillis()) + ": 4. Census ABS/ATO population multipliers");

		// ATO 6B Keys: Series Title, Post Code
		// censusSEXP_POA_AGE5P_INDP_INCP Keys: Age5, Industry Division, Personal
		// Income, POA, Sex<br>
		Set<String> poaSetAto = this.atoIndividualTable6b.get(ATO_6B_TITLE_TAXABLE_COUNT).keySet();
		Set<String> poaSetAbs = this.censusSEXP_POA_AGE5P_INDP_INCP.get(AGE_ARRAY_ABS[5]).get(DIVISION_CODE_ARRAY[0])
				.get(INDIVIDUAL_INCOME_RANGES_ABS[2]).keySet(); // was 1
		Set<String> poaSetIntersection = new HashSet<String>(poaSetAto);
		poaSetIntersection.retainAll(poaSetAbs); // gets just the POAs that appear in both ATO and ABS data
		Map<String, List<String>> poasInEachLga = new HashMap<String, List<String>>(MAP_LGA_INIT_CAPACITY);
		// censusMatrixPersonsPOA Keys: postcode, sex, age, division code, income
		int[][][][][] censusMatrixPersonsAdjustedPOA = new int[poaSetIntersection
				.size()][SEX_ARRAY.length][AGE_ARRAY_ABS.length][DIVISION_CODE_ARRAY.length][INDIVIDUAL_INCOME_RANGES_ABS.length];

		// initialise matrix
		this.poaIndexMap = new HashMap<String, Integer>((int) Math.ceil(poaSetIntersection.size() / MAP_LOAD_FACTOR));
		Map<String, Integer> sexIndexMap = new HashMap<String, Integer>(
				(int) Math.ceil(SEX_ARRAY.length / MAP_LOAD_FACTOR));
		Map<String, Integer> ageIndexMap = new HashMap<String, Integer>(
				(int) Math.ceil(AGE_ARRAY_ABS.length / MAP_LOAD_FACTOR));
		Map<String, Integer> divIndexMap = new HashMap<String, Integer>(
				(int) Math.ceil(DIVISION_CODE_ARRAY.length / MAP_LOAD_FACTOR));
		Map<String, Integer> individualIncomeIndexMap = new HashMap<String, Integer>(
				(int) Math.ceil(INDIVIDUAL_INCOME_RANGES_ABS.length / MAP_LOAD_FACTOR));
		int i = 0;
		for (String poa : poaSetIntersection) {
			this.poaIndexMap.put(poa, i);
			String lgaCode = this.area.getLgaCodeFromPoa(poa);
			if (!poasInEachLga.containsKey(lgaCode)) {
				poasInEachLga.put(lgaCode, new ArrayList<String>());
			}
			poasInEachLga.get(lgaCode).add(poa);
			for (int j = 0; j < SEX_ARRAY.length; j++) {
				sexIndexMap.put(SEX_ARRAY[j], j);
				for (int k = 0; k < AGE_ARRAY_ABS.length; k++) {
					ageIndexMap.put(AGE_ARRAY_ABS[k], k);
					for (int l = 0; l < DIVISION_CODE_ARRAY.length; l++) {
						divIndexMap.put(DIVISION_CODE_ARRAY[l], l);
						for (int m = 0; m < INDIVIDUAL_INCOME_RANGES_ABS.length; m++) {
							individualIncomeIndexMap.put(INDIVIDUAL_INCOME_RANGES_ABS[m], m);
							censusMatrixPersonsAdjustedPOA[i][j][k][l][m] = 0;
						}
					}
				}
			}
			i++;
		}
		// convert CSV map data into matrix, and adjust population forward to 2018
		for (String age : AGE_ARRAY_ABS) {
			int ageIdx = ageIndexMap.get(age);
			for (String divisionCode : DIVISION_CODE_ARRAY) {
				int divIdx = divIndexMap.get(divisionCode);
				// String divDescr = this.abs1292_0_55_002ANZSIC.get("Division Code to
				// Division").get(divisionCode);
				for (String incomeRange : INDIVIDUAL_INCOME_RANGES_ABS) {
					int incomeIdx = individualIncomeIndexMap.get(incomeRange);
					for (String poa : poaSetIntersection) {
						int poaIdx = poaIndexMap.get(poa);
						for (String sex : SEX_ARRAY) {
							int sexIdx = sexIndexMap.get(sex);
							int oldVal = censusMatrixPersonsAdjustedPOA[poaIdx][sexIdx][ageIdx][divIdx][incomeIdx];
							int adjustedPopulation = 0;
							try {
								adjustedPopulation = (int) Math.round(Float
										.valueOf(this.censusSEXP_POA_AGE5P_INDP_INCP.get(age).get(divisionCode)
												.get(incomeRange).get(poa).get(sex))
										* this.populationMultiplier * POPULATION_MULTIPLIER);
							} catch (NumberFormatException e) {
								adjustedPopulation = 0;
							}
							censusMatrixPersonsAdjustedPOA[poaIdx][sexIdx][ageIdx][divIdx][incomeIdx] = oldVal
									+ adjustedPopulation;
						}
					}
				}
			}
		}

		/*
		 * 5. ATO Individual Table 3A: age/sex count, P&L, Help Debt (by income range).
		 * Financial position will vary more by income than any other metric, so use
		 * table 3A as the base amounts and counts to adjust. Use these amounts, looping
		 * through the extra dimensions and multiplying by the multipliers above. The
		 * number of people is given in table 3A, and only needs to be multiplied by the
		 * population forecast multiplier to adjust them forward to 2018 figures. Need
		 * to assign LGA from POA first so we can get the ratio to adjust the population
		 * by. This ensures, for example, that the right number of people are assigned a
		 * HELP debt, etc.
		 * 
		 * N.B. Need to do this before the POA population adjustment multiplier so we
		 * have the "Actual" ATO POA populations to use.
		 * 
		 * 6. Calculate POA population multipliers to adjust ATO 3A P&L statements so
		 * the POA totals equal the POA counts for people with an income. Need to do
		 * this by finer segment so that the totals add up for each ABS segment.
		 */
		System.out.println(new Date(System.currentTimeMillis()) + ": 5. ATO Individual Table 3A P&L figures");

		// matrix Keys: postcode, sex, age, industry division, income (ABS categories)
		/*
		 * if (this.individualMatrix == null) { this.individualMatrix = new
		 * ArrayList<List<List<List<List<List<Individual>>>>>>(poaSetIntersection.size()
		 * ); // initialise matrix for (String poa : poaSetIntersection) { int poaIdx =
		 * poaIndexMap.get(poa); this.individualMatrix.add(new
		 * ArrayList<List<List<List<List<Individual>>>>>(SEX_ARRAY.length)); for (int
		 * sexIdx = 0; sexIdx < SEX_ARRAY.length; sexIdx++) {
		 * this.individualMatrix.get(poaIdx) .add(new
		 * ArrayList<List<List<List<Individual>>>>(AGE_ARRAY_ABS.length)); for (int
		 * ageIdx = 0; ageIdx < AGE_ARRAY_ABS.length; ageIdx++) {
		 * this.individualMatrix.get(poaIdx).get(sexIdx) .add(new
		 * ArrayList<List<List<Individual>>>(DIVISION_CODE_ARRAY.length)); for (int
		 * divIdx = 0; divIdx < DIVISION_CODE_ARRAY.length; divIdx++) {
		 * this.individualMatrix.get(poaIdx).get(sexIdx).get(ageIdx) .add(new
		 * ArrayList<List<Individual>>(INDIVIDUAL_INCOME_RANGES_ABS.length)); for (int
		 * incomeIdx = 0; incomeIdx < INDIVIDUAL_INCOME_RANGES_ABS.length; incomeIdx++)
		 * { this.individualMatrix.get(poaIdx).get(sexIdx).get(ageIdx).get(divIdx)
		 * .add(new ArrayList<Individual>()); } } } } } }
		 */

		// create Individual map
		// Keys: LGA, AGE5P, INCP.
		this.individualMap = new HashMap<String, Map<String, Map<String, ArrayList<Individual>>>>(
				(int) Math.ceil(poasInEachLga.size() / MAP_LOAD_FACTOR) + 1);

		/*
		 * if (this.individualAgents == null) { // add in a 5% buffer so the List
		 * doesn't end up float the size it needs to be int initCapacity = (int)
		 * Math.round(this.totalPopulationAU * 1.05d); this.individualAgents = new
		 * ArrayList<Individual>(initCapacity); }
		 */

		/*
		 * ALGORITHM
		 * 
		 * for income, age, sex, taxable status
		 * 
		 * get taxable count by income, age & sex (don't care about taxable status). Use
		 * sum of count by main income type, rather than just taxable income count.
		 * 
		 * for each industry, multiply by industry count ratio
		 * 
		 * for each state, multiply by state count ratio
		 * 
		 * for each poa, multiply by poa count ratio
		 * 
		 * this gives us taxable count by income, age, sex, industry, poa (same as ABS)
		 * 
		 * within this nested poa loop, divide ABS count by ATO count to get multiplier
		 * 
		 * for each individual type, calculate P&L line items. Store in a nested List of
		 * Individuals.
		 */
		// for income, age, sex, taxable status
		for (int incomeAtoIdx = 0; incomeAtoIdx < INDIVIDUAL_INCOME_RANGES_ATO3A.length; incomeAtoIdx++) {
			String incomeRangeAto = INDIVIDUAL_INCOME_RANGES_ATO3A[incomeAtoIdx];
			List<Integer> incomeIndicesAbs = this.getAbsIncomeIndices(incomeAtoIdx); // deals with the 1:n mappings
			List<Integer> incomeIndicesAto = this.getAtoIncomeIndices(incomeIndicesAbs.get(0)); // deals with the n:1
																								// mappings
			if (DEBUG) {
				System.out.println("incomeRangeAto: " + incomeRangeAto);
			}
			// Skip iterations for n:1 mappings. We need to aggregate the ATO data when
			// calculating ratios.
			// 0-1 ==> 2, 3 ==> 5-4, 4-5 ==> 6, 7-8 ==> 8, 9-11 ==> 9, 13-14 ==> 11, 17-21
			// ==> 14
			if (incomeAtoIdx == 1 || incomeAtoIdx == 5 || incomeAtoIdx == 8 || incomeAtoIdx == 10 || incomeAtoIdx == 11
					|| incomeAtoIdx == 14 || incomeAtoIdx > 17) {
				// skip the rest of this nested loop because this was already dealt with in the
				// earlier part of this multi-index mapping
			} else {
				// continue processing nested loop
				for (int ageIdxAto = 0; ageIdxAto < AGE_ARRAY_ATO.length; ageIdxAto++) {
					String age = AGE_ARRAY_ATO[ageIdxAto];
					List<Integer> ageIndicesAbs = this.getAbsAgeIndices(ageIdxAto);
					if (DEBUG_DETAIL) {
						System.out.println("   age: " + age);
					}
					// [0] under 18 = 0-4, 5-9, 10-14, 15-19
					// [12] 75+ = 75-79, 80-84, 85-89, 90-94, 95-99, 100+
					// [1-11] 18-24 = 20-24; 25-29 = 25-29; ...; 70-74 = 70-74
					for (int sexIdx = 0; sexIdx < SEX_ARRAY.length; sexIdx++) {
						String sex = SEX_ARRAY[sexIdx];
						if (DEBUG_DETAIL) {
							System.out.println("      sex: " + sex);
						}
						// variables to calculate ABS/ATO population multiplier
						float employedCount = 0f;
						float unemployedCount = 0f;
						float pensionCount = 0f;
						float selfFundedRetireeCount = 0f;
						float foreignIncomeCount = 0f;
						float noIncomeCount = 0f;

						// variables to calibrate Individual agents
						int numAtoIncomeIndices = incomeIndicesAto.size();
						List<Integer> atoCountTotal = new ArrayList<Integer>(numAtoIncomeIndices);
						List<Integer> atoCountEmployed = new ArrayList<Integer>(numAtoIncomeIndices);
						List<Integer> atoCountUnemployed = new ArrayList<Integer>(numAtoIncomeIndices);
						List<Integer> atoCountPension = new ArrayList<Integer>(numAtoIncomeIndices);
						List<Integer> atoCountSelfFundedRetiree = new ArrayList<Integer>(numAtoIncomeIndices);
						List<Integer> atoCountForeignIncome = new ArrayList<Integer>(numAtoIncomeIndices);
						List<Integer> atoCountAttributeInterestIncome = new ArrayList<Integer>(numAtoIncomeIndices);
						List<Integer> atoCountAttributeDividendIncome = new ArrayList<Integer>(numAtoIncomeIndices);
						List<Integer> atoCountAttributeWorkRelatedExpenses = new ArrayList<Integer>(
								numAtoIncomeIndices);
						List<Integer> atoCountAttributeDonations = new ArrayList<Integer>(numAtoIncomeIndices);
						List<Integer> atoCountAttributeRentIncome = new ArrayList<Integer>(numAtoIncomeIndices);
						List<Integer> atoCountAttributeRentInterest = new ArrayList<Integer>(numAtoIncomeIndices);
						List<Integer> atoCountAttributeOtherIncome = new ArrayList<Integer>(numAtoIncomeIndices);
						List<Integer> atoCountAttributeTotalIncome = new ArrayList<Integer>(numAtoIncomeIndices);
						List<Integer> atoCountAttributeStudentLoan = new ArrayList<Integer>(numAtoIncomeIndices);
						List<Long> atoAmountEmployed = new ArrayList<Long>(numAtoIncomeIndices);
						List<Long> atoAmountUnemployed = new ArrayList<Long>(numAtoIncomeIndices);
						List<Long> atoAmountPension = new ArrayList<Long>(numAtoIncomeIndices);
						List<Long> atoAmountSelfFundedRetiree = new ArrayList<Long>(numAtoIncomeIndices);
						List<Long> atoAmountForeignIncome = new ArrayList<Long>(numAtoIncomeIndices);
						List<Long> atoAmountAttributeInterestIncome = new ArrayList<Long>(numAtoIncomeIndices);
						List<Long> atoAmountAttributeDividendIncome = new ArrayList<Long>(numAtoIncomeIndices);
						List<Long> atoAmountAttributeWorkRelatedExpenses = new ArrayList<Long>(numAtoIncomeIndices);
						List<Long> atoAmountAttributeDonations = new ArrayList<Long>(numAtoIncomeIndices);
						List<Long> atoAmountAttributeRentIncome = new ArrayList<Long>(numAtoIncomeIndices);
						List<Long> atoAmountAttributeRentInterest = new ArrayList<Long>(numAtoIncomeIndices);
						List<Long> atoAmountAttributeOtherIncome = new ArrayList<Long>(numAtoIncomeIndices);
						List<Long> atoAmountAttributeTotalIncome = new ArrayList<Long>(numAtoIncomeIndices);
						List<Long> atoAmountAttributeStudentLoan = new ArrayList<Long>(numAtoIncomeIndices);

						// initialise the above lists to zero
						atoCountTotal.addAll(Collections.nCopies(numAtoIncomeIndices, 0));
						atoCountEmployed.addAll(Collections.nCopies(numAtoIncomeIndices, 0));
						atoCountUnemployed.addAll(Collections.nCopies(numAtoIncomeIndices, 0));
						atoCountPension.addAll(Collections.nCopies(numAtoIncomeIndices, 0));
						atoCountSelfFundedRetiree.addAll(Collections.nCopies(numAtoIncomeIndices, 0));
						atoCountForeignIncome.addAll(Collections.nCopies(numAtoIncomeIndices, 0));
						atoCountAttributeInterestIncome.addAll(Collections.nCopies(numAtoIncomeIndices, 0));
						atoCountAttributeDividendIncome.addAll(Collections.nCopies(numAtoIncomeIndices, 0));
						atoCountAttributeWorkRelatedExpenses.addAll(Collections.nCopies(numAtoIncomeIndices, 0));
						atoCountAttributeDonations.addAll(Collections.nCopies(numAtoIncomeIndices, 0));
						atoCountAttributeRentIncome.addAll(Collections.nCopies(numAtoIncomeIndices, 0));
						atoCountAttributeRentInterest.addAll(Collections.nCopies(numAtoIncomeIndices, 0));
						atoCountAttributeOtherIncome.addAll(Collections.nCopies(numAtoIncomeIndices, 0));
						atoCountAttributeTotalIncome.addAll(Collections.nCopies(numAtoIncomeIndices, 0));
						atoCountAttributeStudentLoan.addAll(Collections.nCopies(numAtoIncomeIndices, 0));
						atoAmountEmployed.addAll(Collections.nCopies(numAtoIncomeIndices, 0L));
						atoAmountUnemployed.addAll(Collections.nCopies(numAtoIncomeIndices, 0L));
						atoAmountPension.addAll(Collections.nCopies(numAtoIncomeIndices, 0L));
						atoAmountSelfFundedRetiree.addAll(Collections.nCopies(numAtoIncomeIndices, 0L));
						atoAmountForeignIncome.addAll(Collections.nCopies(numAtoIncomeIndices, 0L));
						atoAmountAttributeInterestIncome.addAll(Collections.nCopies(numAtoIncomeIndices, 0L));
						atoAmountAttributeDividendIncome.addAll(Collections.nCopies(numAtoIncomeIndices, 0L));
						atoAmountAttributeWorkRelatedExpenses.addAll(Collections.nCopies(numAtoIncomeIndices, 0L));
						atoAmountAttributeDonations.addAll(Collections.nCopies(numAtoIncomeIndices, 0L));
						atoAmountAttributeRentIncome.addAll(Collections.nCopies(numAtoIncomeIndices, 0L));
						atoAmountAttributeRentInterest.addAll(Collections.nCopies(numAtoIncomeIndices, 0L));
						atoAmountAttributeOtherIncome.addAll(Collections.nCopies(numAtoIncomeIndices, 0L));
						atoAmountAttributeTotalIncome.addAll(Collections.nCopies(numAtoIncomeIndices, 0L));
						atoAmountAttributeStudentLoan.addAll(Collections.nCopies(numAtoIncomeIndices, 0L));

						// need corresponding if statements in the nested loops, retrieving multiple
						// indices' values
						// 0-1 ==> 2, 3 ==> 5-4, 4-5 ==> 6, 7-8 ==> 8, 9-11 ==> 9, 13-14 ==> 11, 17-21
						// ==> 14
						for (int taxableStatusIdx = 0; taxableStatusIdx < TAXABLE_STATUS.length; taxableStatusIdx++) {
							String taxableStatus = TAXABLE_STATUS[taxableStatusIdx];
							if (DEBUG_DETAIL) {
								System.out.println("      -taxableStatus: " + taxableStatus);
							}
							// variables to calculate ABS/ATO population multiplier
							// get taxable count by income, age & sex (don't care about taxable status). Use
							// sum of count by main income type, rather than just taxable income count.

							// handle n:m mappings using a loop
							for (int incomeMapNum = 0; incomeMapNum < numAtoIncomeIndices; incomeMapNum++) {
								incomeRangeAto = INDIVIDUAL_INCOME_RANGES_ATO3A[incomeAtoIdx + incomeMapNum];
								// check that this category has data first (to avoid null pointer exceptions)
								if (this.atoIndividualTable3a.get(ATO_3A_TITLE_SALARY_COUNT).containsKey(incomeRangeAto)
										&& this.atoIndividualTable3a.get(ATO_3A_TITLE_SALARY_COUNT).get(incomeRangeAto)
												.containsKey(age)
										&& this.atoIndividualTable3a.get(ATO_3A_TITLE_SALARY_COUNT).get(incomeRangeAto)
												.get(age).containsKey(sex)
										&& this.atoIndividualTable3a.get(ATO_3A_TITLE_SALARY_COUNT).get(incomeRangeAto)
												.get(age).get(sex).containsKey(taxableStatus)) {

									employedCount += Math.max(
											this.atoIndividualTable3a.get(ATO_3A_TITLE_SALARY_COUNT).get(incomeRangeAto)
													.get(age).get(sex).get(taxableStatus),
											this.atoIndividualTable3a.get(ATO_3A_TITLE_ALLOWANCES_COUNT)
													.get(incomeRangeAto).get(age).get(sex).get(taxableStatus));
									unemployedCount += this.atoIndividualTable3a.get(ATO_3A_TITLE_GOVT_ALLOW_COUNT)
											.get(incomeRangeAto).get(age).get(sex).get(taxableStatus);
									pensionCount += this.atoIndividualTable3a.get(ATO_3A_TITLE_GOVT_PENSION_COUNT)
											.get(incomeRangeAto).get(age).get(sex).get(taxableStatus);
									selfFundedRetireeCount += Math.max(
											this.atoIndividualTable3a.get(ATO_3A_TITLE_SUPER_TAXED_COUNT)
													.get(incomeRangeAto).get(age).get(sex).get(taxableStatus),
											this.atoIndividualTable3a.get(ATO_3A_TITLE_SUPER_UNTAXED_COUNT)
													.get(incomeRangeAto).get(age).get(sex).get(taxableStatus));
									foreignIncomeCount += Math.max(
											this.atoIndividualTable3a.get(ATO_3A_TITLE_FOREIGN_INCOME_COUNT)
													.get(incomeRangeAto).get(age).get(sex).get(taxableStatus),
											this.atoIndividualTable3a.get(ATO_3A_TITLE_FOREIGN_INCOME2_COUNT)
													.get(incomeRangeAto).get(age).get(sex).get(taxableStatus));
									noIncomeCount += this.atoIndividualTable3a.get(ATO_3A_TITLE_TOTAL_INCOME_COUNT)
											.get(incomeRangeAto).get(age).get(sex).get(taxableStatus);

									// variables to calibrate Individual agents
									atoCountEmployed.set(incomeMapNum, Math.round(Math.max(
											this.atoIndividualTable3a.get(ATO_3A_TITLE_SALARY_COUNT).get(incomeRangeAto)
													.get(age).get(sex).get(taxableStatus),
											this.atoIndividualTable3a.get(ATO_3A_TITLE_ALLOWANCES_COUNT)
													.get(incomeRangeAto).get(age).get(sex).get(taxableStatus))));
									atoCountUnemployed.set(incomeMapNum,
											Math.round(this.atoIndividualTable3a.get(ATO_3A_TITLE_GOVT_ALLOW_COUNT)
													.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)));
									atoCountPension.set(incomeMapNum,
											Math.round(this.atoIndividualTable3a.get(ATO_3A_TITLE_GOVT_PENSION_COUNT)
													.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)));
									atoCountSelfFundedRetiree.set(incomeMapNum, Math.round(Math.max(
											this.atoIndividualTable3a.get(ATO_3A_TITLE_SUPER_TAXED_COUNT)
													.get(incomeRangeAto).get(age).get(sex).get(taxableStatus),
											this.atoIndividualTable3a.get(ATO_3A_TITLE_SUPER_UNTAXED_COUNT)
													.get(incomeRangeAto).get(age).get(sex).get(taxableStatus))));
									atoCountForeignIncome.set(incomeMapNum, Math.round(Math.max(
											this.atoIndividualTable3a.get(ATO_3A_TITLE_FOREIGN_INCOME_COUNT)
													.get(incomeRangeAto).get(age).get(sex).get(taxableStatus),
											this.atoIndividualTable3a.get(ATO_3A_TITLE_FOREIGN_INCOME2_COUNT)
													.get(incomeRangeAto).get(age).get(sex).get(taxableStatus))));
									atoCountTotal.set(incomeMapNum,
											atoCountEmployed.get(incomeMapNum) + atoCountUnemployed.get(incomeMapNum)
													+ atoCountPension.get(incomeMapNum)
													+ atoCountSelfFundedRetiree.get(incomeMapNum)
													+ atoCountForeignIncome.get(incomeMapNum));

									atoCountAttributeInterestIncome.set(incomeMapNum,
											Math.round(this.atoIndividualTable3a.get(ATO_3A_TITLE_INTEREST_INCOME_COUNT)
													.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)));
									// 36% of Australian adults own shares, so sum these counts rather than just
									// taking the maximum
									// SOURCE: ASX (2014), 'The Australian Share Ownership Study': Sydney, NSW.
									atoCountAttributeDividendIncome.set(incomeMapNum,
											Math.round(this.atoIndividualTable3a
													.get(ATO_3A_TITLE_DIVIDENDS_UNFRANKED_COUNT).get(incomeRangeAto)
													.get(age).get(sex).get(taxableStatus)
													+ this.atoIndividualTable3a
															.get(ATO_3A_TITLE_DIVIDENDS_FRANKED_COUNT)
															.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)));
									atoCountAttributeWorkRelatedExpenses.set(incomeMapNum,
											Math.round(
													this.atoIndividualTable3a.get(ATO_3A_TITLE_WORK_RELATED_EXP_COUNT)
															.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)));
									atoCountAttributeDonations.set(incomeMapNum,
											Math.round(this.atoIndividualTable3a.get(ATO_3A_TITLE_DONATIONS_COUNT)
													.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)));
									atoCountAttributeRentIncome.set(incomeMapNum,
											Math.round(this.atoIndividualTable3a.get(ATO_3A_TITLE_RENT_INCOME_COUNT)
													.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)));
									atoCountAttributeRentInterest.set(incomeMapNum,
											Math.round(this.atoIndividualTable3a.get(ATO_3A_TITLE_RENT_INTEREST_COUNT)
													.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)));
									atoCountAttributeTotalIncome.set(incomeMapNum,
											Math.round(this.atoIndividualTable3a.get(ATO_3A_TITLE_TOTAL_INCOME_COUNT)
													.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)));
									// SFSS is on top of other loans, so count is max (HELP + TSL, SFSS)
									atoCountAttributeStudentLoan.set(incomeMapNum, Math.round(Math.max(
											this.atoIndividualTable3a.get(ATO_3A_TITLE_HELP_DEBT_COUNT)
													.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)
													+ this.atoIndividualTable3a.get(ATO_3A_TITLE_TSL_DEBT_COUNT)
															.get(incomeRangeAto).get(age).get(sex).get(taxableStatus),
											this.atoIndividualTable3a.get(ATO_3A_TITLE_SFSS_DEBT_COUNT)
													.get(incomeRangeAto).get(age).get(sex).get(taxableStatus))));
									atoAmountEmployed.set(incomeMapNum,
											Float.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_SALARY_AMOUNT)
													.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)
													+ this.atoIndividualTable3a.get(ATO_3A_TITLE_ALLOWANCES_AMOUNT)
															.get(incomeRangeAto).get(age).get(sex).get(taxableStatus))
													.longValue());
									atoAmountUnemployed.set(incomeMapNum,
											Float.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_GOVT_ALLOW_AMOUNT)
													.get(incomeRangeAto).get(age).get(sex).get(taxableStatus))
													.longValue());
									atoAmountPension.set(incomeMapNum, Float
											.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_GOVT_PENSION_AMOUNT)
													.get(incomeRangeAto).get(age).get(sex).get(taxableStatus))
											.longValue());
									atoAmountSelfFundedRetiree.set(incomeMapNum,
											Float.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_SUPER_TAXED_AMOUNT)
													.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)
													+ this.atoIndividualTable3a.get(ATO_3A_TITLE_SUPER_UNTAXED_AMOUNT)
															.get(incomeRangeAto).get(age).get(sex).get(taxableStatus))
													.longValue());
									atoAmountForeignIncome.set(incomeMapNum, Float
											.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_FOREIGN_INCOME_AMOUNT)
													.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)
													+ this.atoIndividualTable3a.get(ATO_3A_TITLE_FOREIGN_INCOME2_AMOUNT)
															.get(incomeRangeAto).get(age).get(sex).get(taxableStatus))
											.longValue());
									atoAmountAttributeInterestIncome.set(incomeMapNum, Float
											.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_INTEREST_INCOME_AMOUNT)
													.get(incomeRangeAto).get(age).get(sex).get(taxableStatus))
											.longValue());
									atoAmountAttributeDividendIncome.set(incomeMapNum,
											Float.valueOf(this.atoIndividualTable3a
													.get(ATO_3A_TITLE_DIVIDENDS_UNFRANKED_AMOUNT).get(incomeRangeAto)
													.get(age).get(sex).get(taxableStatus)
													+ this.atoIndividualTable3a
															.get(ATO_3A_TITLE_DIVIDENDS_FRANKED_AMOUNT)
															.get(incomeRangeAto).get(age).get(sex).get(taxableStatus))
													.longValue());
									atoAmountAttributeWorkRelatedExpenses.set(incomeMapNum, Float
											.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_WORK_RELATED_EXP_AMOUNT)
													.get(incomeRangeAto).get(age).get(sex).get(taxableStatus))
											.longValue());
									atoAmountAttributeDonations.set(incomeMapNum,
											Float.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_DONATIONS_AMOUNT)
													.get(incomeRangeAto).get(age).get(sex).get(taxableStatus))
													.longValue());
									atoAmountAttributeRentIncome.set(incomeMapNum,
											Float.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_RENT_INCOME_AMOUNT)
													.get(incomeRangeAto).get(age).get(sex).get(taxableStatus))
													.longValue());
									atoAmountAttributeRentInterest.set(incomeMapNum, Float
											.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_RENT_INTEREST_AMOUNT)
													.get(incomeRangeAto).get(age).get(sex).get(taxableStatus))
											.longValue());
									atoAmountAttributeTotalIncome.set(incomeMapNum, Float
											.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_TOTAL_INCOME_AMOUNT)
													.get(incomeRangeAto).get(age).get(sex).get(taxableStatus))
											.longValue());
									atoAmountAttributeStudentLoan.set(incomeMapNum,
											Float.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_HELP_DEBT_AMOUNT)
													.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)
													+ this.atoIndividualTable3a.get(ATO_3A_TITLE_TSL_DEBT_AMOUNT)
															.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)
													+ this.atoIndividualTable3a.get(ATO_3A_TITLE_SFSS_DEBT_AMOUNT)
															.get(incomeRangeAto).get(age).get(sex).get(taxableStatus))
													.longValue());
								}
							} // end for income map number
						} // end for taxable status
						noIncomeCount = noIncomeCount - employedCount - unemployedCount - pensionCount
								- selfFundedRetireeCount - foreignIncomeCount;

						// create lists of averages per person
						List<Float> atoPerPersonEmployed = new ArrayList<Float>(numAtoIncomeIndices);
						List<Float> atoPerPersonUnemployed = new ArrayList<Float>(numAtoIncomeIndices);
						List<Float> atoPerPersonPension = new ArrayList<Float>(numAtoIncomeIndices);
						List<Float> atoPerPersonSelfFundedRetiree = new ArrayList<Float>(numAtoIncomeIndices);
						List<Float> atoPerPersonForeignIncome = new ArrayList<Float>(numAtoIncomeIndices);
						List<Float> atoPerPersonAttributeInterestIncome = new ArrayList<Float>(numAtoIncomeIndices);
						List<Float> atoPerPersonAttributeDividendIncome = new ArrayList<Float>(numAtoIncomeIndices);
						List<Float> atoPerPersonAttributeWorkRelatedExpenses = new ArrayList<Float>(
								numAtoIncomeIndices);
						List<Float> atoPerPersonAttributeDonations = new ArrayList<Float>(numAtoIncomeIndices);
						List<Float> atoPerPersonAttributeRentIncome = new ArrayList<Float>(numAtoIncomeIndices);
						List<Float> atoPerPersonAttributeRentInterest = new ArrayList<Float>(numAtoIncomeIndices);
						List<Float> atoPerPersonAttributeOtherIncome = new ArrayList<Float>(numAtoIncomeIndices);
						List<Float> atoPerPersonAttributeTotalIncome = new ArrayList<Float>(numAtoIncomeIndices);
						List<Float> atoPerPersonAttributeStudentLoan = new ArrayList<Float>(numAtoIncomeIndices);

						// for each ATO income category in this iteration
						for (int incomeMapNum = 0; incomeMapNum < numAtoIncomeIndices; incomeMapNum++) {
							// calculate the balancing item
							atoCountAttributeOtherIncome.add(atoCountAttributeTotalIncome.get(incomeMapNum)
									- atoCountEmployed.get(incomeMapNum) - atoCountUnemployed.get(incomeMapNum)
									- atoCountPension.get(incomeMapNum) - atoCountSelfFundedRetiree.get(incomeMapNum)
									- atoCountForeignIncome.get(incomeMapNum));
							atoAmountAttributeOtherIncome.add(atoAmountAttributeTotalIncome.get(incomeMapNum)
									- atoAmountEmployed.get(incomeMapNum) - atoAmountUnemployed.get(incomeMapNum)
									- atoAmountPension.get(incomeMapNum) - atoAmountSelfFundedRetiree.get(incomeMapNum)
									- atoAmountForeignIncome.get(incomeMapNum));

							// calculate averages per person for each main income source and P&L line item
							atoPerPersonEmployed.add(atoCountEmployed.get(incomeMapNum) == 0 ? 0f
									: ((float) atoAmountEmployed.get(incomeMapNum))
											/ (float) atoCountEmployed.get(incomeMapNum));
							atoPerPersonUnemployed.add(atoCountUnemployed.get(incomeMapNum) == 0 ? 0f
									: ((float) atoAmountUnemployed.get(incomeMapNum))
											/ (float) atoCountUnemployed.get(incomeMapNum));
							atoPerPersonPension.add(atoCountPension.get(incomeMapNum) == 0 ? 0f
									: ((float) atoAmountPension.get(incomeMapNum))
											/ (float) atoCountPension.get(incomeMapNum));
							atoPerPersonSelfFundedRetiree.add(atoCountSelfFundedRetiree.get(incomeMapNum) == 0 ? 0f
									: ((float) atoAmountSelfFundedRetiree.get(incomeMapNum))
											/ (float) atoCountSelfFundedRetiree.get(incomeMapNum));
							atoPerPersonForeignIncome.add(atoCountForeignIncome.get(incomeMapNum) == 0 ? 0f
									: ((float) atoAmountForeignIncome.get(incomeMapNum))
											/ (float) atoCountForeignIncome.get(incomeMapNum));
							atoPerPersonAttributeInterestIncome
									.add(atoCountAttributeInterestIncome.get(incomeMapNum) == 0 ? 0f
											: ((float) atoAmountAttributeInterestIncome.get(incomeMapNum))
													/ (float) atoCountAttributeInterestIncome.get(incomeMapNum));
							atoPerPersonAttributeDividendIncome
									.add(atoCountAttributeDividendIncome.get(incomeMapNum) == 0 ? 0f
											: ((float) atoAmountAttributeDividendIncome.get(incomeMapNum))
													/ (float) atoCountAttributeDividendIncome.get(incomeMapNum));
							atoPerPersonAttributeWorkRelatedExpenses
									.add(atoCountAttributeWorkRelatedExpenses.get(incomeMapNum) == 0 ? 0f
											: ((float) atoAmountAttributeWorkRelatedExpenses.get(incomeMapNum))
													/ (float) atoCountAttributeWorkRelatedExpenses.get(incomeMapNum));
							atoPerPersonAttributeDonations.add(atoCountAttributeDonations.get(incomeMapNum) == 0 ? 0f
									: ((float) atoAmountAttributeDonations.get(incomeMapNum))
											/ (float) atoCountAttributeDonations.get(incomeMapNum));
							atoPerPersonAttributeRentIncome.add(atoCountAttributeRentIncome.get(incomeMapNum) == 0 ? 0f
									: ((float) atoAmountAttributeRentIncome.get(incomeMapNum))
											/ (float) atoCountAttributeRentIncome.get(incomeMapNum));
							atoPerPersonAttributeRentInterest
									.add(atoCountAttributeRentInterest.get(incomeMapNum) == 0 ? 0f
											: ((float) atoAmountAttributeRentInterest.get(incomeMapNum))
													/ (float) atoCountAttributeRentInterest.get(incomeMapNum));
							atoPerPersonAttributeOtherIncome
									.add(atoCountAttributeOtherIncome.get(incomeMapNum) == 0 ? 0f
											: ((float) atoAmountAttributeOtherIncome.get(incomeMapNum))
													/ (float) atoCountAttributeOtherIncome.get(incomeMapNum));
							atoPerPersonAttributeTotalIncome
									.add(atoCountAttributeTotalIncome.get(incomeMapNum) == 0 ? 0f
											: ((float) atoAmountAttributeTotalIncome.get(incomeMapNum))
													/ (float) atoCountAttributeTotalIncome.get(incomeMapNum));
							atoPerPersonAttributeStudentLoan
									.add(atoCountAttributeStudentLoan.get(incomeMapNum) == 0 ? 0f
											: ((float) atoAmountAttributeStudentLoan.get(incomeMapNum))
													/ (float) atoCountAttributeStudentLoan.get(incomeMapNum));
						}

						for (int divIdx = 0; divIdx < NUM_DIVISIONS; divIdx++) {
							String division = DIVISION_CODE_ARRAY[divIdx];
							if (DEBUG_DETAIL) {
								System.out.println("         division: " + division);
							}
							// for each industry, multiply by industry count ratio
							float divAmtMult = divisionTaxableIncomeMultiplier.get(division);
							for (String poa : poaSetIntersection) {
								if (DEBUG_DETAIL) {
									System.out.println(" poa: " + poa);
								}
								// reset ATO per person figures inside this loop so we don't sum to infinity
								List<Float> atoPerPersonEmployedDivPoa = new ArrayList<Float>(atoPerPersonEmployed);
								List<Float> atoPerPersonUnemployedDivPoa = new ArrayList<Float>(atoPerPersonUnemployed);
								List<Float> atoPerPersonPensionDivPoa = new ArrayList<Float>(atoPerPersonPension);
								List<Float> atoPerPersonSelfFundedRetireeDivPoa = new ArrayList<Float>(
										atoPerPersonSelfFundedRetiree);
								List<Float> atoPerPersonForeignIncomeDivPoa = new ArrayList<Float>(
										atoPerPersonForeignIncome);
								List<Float> atoPerPersonAttributeInterestIncomeDivPoa = new ArrayList<Float>(
										atoPerPersonAttributeInterestIncome);
								List<Float> atoPerPersonAttributeDividendIncomeDivPoa = new ArrayList<Float>(
										atoPerPersonAttributeDividendIncome);
								List<Float> atoPerPersonAttributeWorkRelatedExpensesDivPoa = new ArrayList<Float>(
										atoPerPersonAttributeWorkRelatedExpenses);
								List<Float> atoPerPersonAttributeDonationsDivPoa = new ArrayList<Float>(
										atoPerPersonAttributeDonations);
								List<Float> atoPerPersonAttributeRentIncomeDivPoa = new ArrayList<Float>(
										atoPerPersonAttributeRentIncome);
								List<Float> atoPerPersonAttributeRentInterestDivPoa = new ArrayList<Float>(
										atoPerPersonAttributeRentInterest);
								List<Float> atoPerPersonAttributeOtherIncomeDivPoa = new ArrayList<Float>(
										atoPerPersonAttributeOtherIncome);
								List<Float> atoPerPersonAttributeTotalIncomeDivPoa = new ArrayList<Float>(
										atoPerPersonAttributeTotalIncome);
								List<Float> atoPerPersonAttributeStudentLoanDivPoa = new ArrayList<Float>(
										atoPerPersonAttributeStudentLoan);

								int poaIdx = poaIndexMap.get(poa);
								String lgaCode = this.area.getLgaCodeFromPoa(poa);
								if (lgaCode != null) {
									String state = this.area.getStateFromPoa(poa);
									if (!state.equals("Other")) { // skip "Other" to solve mapping issues
										// for each state, sex & age, multiply by state count ratio
										float stateAmtMult = stateTaxableIncomeMultiplier.get(sex).get(age).get(state);

										// get ATO taxable count by income, age, sex, industry, poa (same as ABS)
										// for each poa, multiply by poa count ratio
										float poaAmtMult = postcodeStateTaxableIncomeMultiplier.get(state).get(poa);

										// divide ABS count by ATO count to get multiplier, and apply to counts
										float amountMultiplier = divAmtMult * stateAmtMult * poaAmtMult;

										// for each ATO income category in this iteration
										for (int incomeMapNum = 0; incomeMapNum < numAtoIncomeIndices; incomeMapNum++) {
											// multiply ATO 3A averages per person to calibrate them
											atoPerPersonEmployedDivPoa.set(incomeMapNum,
													atoPerPersonEmployedDivPoa.get(incomeMapNum) * amountMultiplier);
											atoPerPersonUnemployedDivPoa.set(incomeMapNum,
													atoPerPersonUnemployedDivPoa.get(incomeMapNum) * amountMultiplier);
											atoPerPersonPensionDivPoa.set(incomeMapNum,
													atoPerPersonPensionDivPoa.get(incomeMapNum) * amountMultiplier);
											atoPerPersonSelfFundedRetireeDivPoa.set(incomeMapNum,
													atoPerPersonSelfFundedRetireeDivPoa.get(incomeMapNum)
															* amountMultiplier);
											atoPerPersonForeignIncomeDivPoa.set(incomeMapNum,
													atoPerPersonForeignIncomeDivPoa.get(incomeMapNum)
															* amountMultiplier);
											atoPerPersonAttributeInterestIncomeDivPoa.set(incomeMapNum,
													atoPerPersonAttributeInterestIncomeDivPoa.get(incomeMapNum)
															* amountMultiplier);
											atoPerPersonAttributeDividendIncomeDivPoa.set(incomeMapNum,
													atoPerPersonAttributeDividendIncomeDivPoa.get(incomeMapNum)
															* amountMultiplier);
											atoPerPersonAttributeWorkRelatedExpensesDivPoa.set(incomeMapNum,
													atoPerPersonAttributeWorkRelatedExpensesDivPoa.get(incomeMapNum)
															* amountMultiplier);
											atoPerPersonAttributeDonationsDivPoa.set(incomeMapNum,
													atoPerPersonAttributeDonationsDivPoa.get(incomeMapNum)
															* amountMultiplier);
											atoPerPersonAttributeRentIncomeDivPoa.set(incomeMapNum,
													atoPerPersonAttributeRentIncomeDivPoa.get(incomeMapNum)
															* amountMultiplier);
											atoPerPersonAttributeRentInterestDivPoa.set(incomeMapNum,
													atoPerPersonAttributeRentInterestDivPoa.get(incomeMapNum)
															* amountMultiplier);
											atoPerPersonAttributeOtherIncomeDivPoa.set(incomeMapNum,
													atoPerPersonAttributeOtherIncomeDivPoa.get(incomeMapNum)
															* amountMultiplier);
											atoPerPersonAttributeTotalIncomeDivPoa.set(incomeMapNum,
													atoPerPersonAttributeTotalIncomeDivPoa.get(incomeMapNum)
															* amountMultiplier);
											atoPerPersonAttributeStudentLoanDivPoa.set(incomeMapNum,
													atoPerPersonAttributeStudentLoanDivPoa.get(incomeMapNum)
															* amountMultiplier);
										}

										/**
										 * RANDOM SAMPLING ALGORITHM Assumptions: I don't care about the ATO counts -
										 * they can be ignored or deleted.<br>
										 * I do care about the average amounts per person so the P&Ls are different.<br>
										 * I also care about the split between various P&L compositions.<br>
										 * 
										 * Algorithm:<br>
										 * 1. Set the flags using percentages so that I can use a pdf lookup then
										 * generate a random float between 0 and 1.<br>
										 * 2. Create an Individual based on this index.<br>
										 * 3. Repeat for as many people as are in the corresponding ABS data.<br>
										 * 
										 * Special cases:<br>
										 * (A) Where there are multiple ATO categories for the one ABS category, put
										 * them all in the one pdf array.<br>
										 * (B) Where there are multiple ABS categories for the one ATO category, loop
										 * through the flag arrays as per usual, but store in the relevant ABS
										 * category's cell.
										 */

										// create probability density functions for the relevant attributes
										float totalCountDenominator = 0f;
										for (int incomeMapNum = 0; incomeMapNum < numAtoIncomeIndices; incomeMapNum++) {
											totalCountDenominator += atoCountTotal.get(incomeMapNum);
										}
										float[] pdfAtoIncomeRange = new float[numAtoIncomeIndices];
										float[][] pdfMainIncomeSource = new float[numAtoIncomeIndices][5];
										float[][] pdfAttributeInterestIncome = new float[numAtoIncomeIndices][2];
										float[][] pdfAttributeDividendIncome = new float[numAtoIncomeIndices][2];
										float[][] pdfAttributeDonations = new float[numAtoIncomeIndices][2];
										float[][] pdfAttributeRentIncomeInterest = new float[numAtoIncomeIndices][3];
										float[][] pdfAttributeOtherIncome = new float[numAtoIncomeIndices][2];
										float[][] pdfAttributeStudentLoan = new float[numAtoIncomeIndices][2];
										for (int incomeMapNum = 0; incomeMapNum < numAtoIncomeIndices; incomeMapNum++) {
											// this determines the first index
											pdfAtoIncomeRange[incomeMapNum] = ((float) atoCountTotal.get(incomeMapNum))
													/ totalCountDenominator;

											// create pdf for the main income source
											pdfMainIncomeSource[incomeMapNum][0] = ((float) atoCountEmployed
													.get(incomeMapNum)) / totalCountDenominator;
											pdfMainIncomeSource[incomeMapNum][1] = ((float) atoCountUnemployed
													.get(incomeMapNum)) / totalCountDenominator;
											pdfMainIncomeSource[incomeMapNum][2] = ((float) atoCountPension
													.get(incomeMapNum)) / totalCountDenominator;
											pdfMainIncomeSource[incomeMapNum][3] = ((float) atoCountSelfFundedRetiree
													.get(incomeMapNum)) / totalCountDenominator;
											pdfMainIncomeSource[incomeMapNum][4] = 1f
													- pdfMainIncomeSource[incomeMapNum][0]
													- pdfMainIncomeSource[incomeMapNum][1]
													- pdfMainIncomeSource[incomeMapNum][2]
													- pdfMainIncomeSource[incomeMapNum][3];

											// create pdfs for the binary attributes
											pdfAttributeInterestIncome[incomeMapNum][0] = ((float) atoCountAttributeInterestIncome
													.get(incomeMapNum)) / totalCountDenominator;
											pdfAttributeInterestIncome[incomeMapNum][1] = 1f
													- pdfAttributeInterestIncome[incomeMapNum][0];
											pdfAttributeDividendIncome[incomeMapNum][0] = ((float) atoCountAttributeDividendIncome
													.get(incomeMapNum)) / totalCountDenominator;
											pdfAttributeDividendIncome[incomeMapNum][1] = 1f
													- pdfAttributeDividendIncome[incomeMapNum][0];
											pdfAttributeDonations[incomeMapNum][0] = ((float) atoCountAttributeDonations
													.get(incomeMapNum)) / totalCountDenominator;
											pdfAttributeDonations[incomeMapNum][1] = 1f
													- pdfAttributeDonations[incomeMapNum][0];
											pdfAttributeOtherIncome[incomeMapNum][0] = ((float) atoCountAttributeOtherIncome
													.get(incomeMapNum)) / totalCountDenominator;
											pdfAttributeOtherIncome[incomeMapNum][1] = 1f
													- pdfAttributeOtherIncome[incomeMapNum][0];
											pdfAttributeStudentLoan[incomeMapNum][0] = ((float) atoCountAttributeStudentLoan
													.get(incomeMapNum)) / totalCountDenominator;
											pdfAttributeStudentLoan[incomeMapNum][1] = 1f
													- pdfAttributeStudentLoan[incomeMapNum][0];

											// create pdf for rental property owners
											int propertyInvestorCount = Math.max(
													atoCountAttributeRentIncome.get(incomeMapNum),
													atoCountAttributeRentInterest.get(incomeMapNum));
											int propertyInvestorWithLoanCount = atoCountAttributeRentInterest
													.get(incomeMapNum);
											int propertyInvestorWithoutLoanCount = propertyInvestorCount
													- propertyInvestorWithLoanCount;
											pdfAttributeRentIncomeInterest[incomeMapNum][0] = ((float) propertyInvestorWithLoanCount)
													/ totalCountDenominator; // rent & interest
											pdfAttributeRentIncomeInterest[incomeMapNum][1] = ((float) propertyInvestorWithoutLoanCount)
													/ totalCountDenominator; // rent only
											pdfAttributeRentIncomeInterest[incomeMapNum][2] = 1f
													- pdfAttributeRentIncomeInterest[incomeMapNum][0]
													- pdfAttributeRentIncomeInterest[incomeMapNum][1]; // neither
										}

										// create Individual agents and store in a List matrix
										// the loops take into account n:m mappings between ATO and ABS
										for (int ageIdxAbs : ageIndicesAbs) {
											// doesn't need to exclude children because it already filters on income
											// range
											for (int incomeIdxAbs : incomeIndicesAbs) {
												int absCount = censusMatrixPersonsAdjustedPOA[poaIdx][sexIdx][ageIdxAbs][divIdx][incomeIdxAbs];
												for (int agentNo = 0; agentNo < absCount; agentNo++) {
													// create one agent for each person in the ABS data
													Individual individual = new Individual();
													individual.setAge(AGE_ARRAY_ABS_MIDPOINT[ageIdxAbs]);
													individual.setSex(SEX_ARRAY[sexIdx]);
													individual.setEmploymentIndustry(DIVISION_CODE_CHAR_ARRAY[divIdx]);
													individual.setLocalGovernmentAreaCode(
															this.area.getLgaCodeFromPoa(poa));

													// P&L
													int incomeMapNum = CustomMath.sample(pdfAtoIncomeRange,
															this.random);
													int incomeSourceNum = CustomMath
															.sample(pdfMainIncomeSource[incomeMapNum], this.random);
													switch (incomeSourceNum) {
													case 0:
														individual.setMainIncomeSource(0); // employed
														individual.setPnlWagesSalaries(
																atoPerPersonEmployedDivPoa.get(incomeMapNum)
																		/ NUM_MONTHS);
														individual.setPnlWorkRelatedExpenses(
																atoPerPersonAttributeWorkRelatedExpensesDivPoa
																		.get(incomeMapNum) / NUM_MONTHS);
														break;
													case 1:
														individual.setMainIncomeSource(1); // unemployed
														individual.setPnlUnemploymentBenefits(
																atoPerPersonUnemployedDivPoa.get(incomeMapNum)
																		/ NUM_MONTHS);
														break;
													case 2:
														individual.setMainIncomeSource(2); // pension
														individual.setPnlOtherSocialSecurityIncome(
																atoPerPersonPensionDivPoa.get(incomeMapNum)
																		/ NUM_MONTHS);
														break;
													case 3:
														individual.setMainIncomeSource(3); // self-funded retiree
														individual.setPnlInvestmentIncome(
																atoPerPersonSelfFundedRetireeDivPoa.get(incomeMapNum)
																		/ NUM_MONTHS);
														break;
													default:
														individual.setMainIncomeSource(4); // foreign income
														individual.setPnlForeignIncome(
																atoPerPersonForeignIncomeDivPoa.get(incomeMapNum)
																		/ NUM_MONTHS);
														break;
													}

													int attributeIdx = CustomMath.sample(
															pdfAttributeInterestIncome[incomeMapNum], this.random);
													if (attributeIdx == 0) {
														individual.setPnlInterestIncome(
																atoPerPersonAttributeInterestIncomeDivPoa
																		.get(incomeMapNum) / NUM_MONTHS);
													}
													attributeIdx = CustomMath.sample(
															pdfAttributeDividendIncome[incomeMapNum], this.random);
													if (attributeIdx == 0) {
														individual.setPnlInvestmentIncome(
																individual.getPnlInvestmentIncome()
																		+ atoPerPersonAttributeDividendIncomeDivPoa
																				.get(incomeMapNum) / NUM_MONTHS);
													}
													attributeIdx = CustomMath
															.sample(pdfAttributeDonations[incomeMapNum], this.random);
													if (attributeIdx == 0) {
														individual.setPnlDonations(
																atoPerPersonAttributeDonationsDivPoa.get(incomeMapNum)
																		/ NUM_MONTHS);
													}
													attributeIdx = CustomMath.sample(
															pdfAttributeRentIncomeInterest[incomeMapNum], this.random);
													if (attributeIdx < 2) {
														individual.setPnlRentIncome(
																atoPerPersonAttributeRentIncomeDivPoa.get(incomeMapNum)
																		/ NUM_MONTHS);
													} else if (attributeIdx == 1) {
														individual.setPnlRentInterestExpense(
																atoPerPersonAttributeRentInterestDivPoa
																		.get(incomeMapNum) / NUM_MONTHS);
													}
													attributeIdx = CustomMath
															.sample(pdfAttributeOtherIncome[incomeMapNum], this.random);
													if (attributeIdx == 0) {
														individual.setPnlOtherIncome(
																atoPerPersonAttributeOtherIncomeDivPoa.get(incomeMapNum)
																		/ NUM_MONTHS);
													}

													// adjust other income if the sum of the income lines is less than
													// total income
													/*
													 * if (individual.getGrossIncome() <
													 * atoPerPersonAttributeTotalIncome.get(incomeMapNum)) { float
													 * newOtherIncome = individual.getPnlOtherIncome() +
													 * (atoPerPersonAttributeTotalIncome.get(incomeMapNum) -
													 * individual.getGrossIncome());
													 * individual.setPnlOtherIncome(newOtherIncome); }
													 */

													// income tax
													individual.setPnlIncomeTaxExpense(Tax
															.calculateIndividualIncomeTax(individual.getGrossIncome()));

													// Bal Sht
													if (individual.getPnlInterestIncome() <= EPSILON) {
														// assume no savings, so the individual uses up their entire
														// income
														// each fortnight. This means the average bank balance is one
														// week's
														// income. Income is recorded monthly, so divide by 4 weeks.
														individual.setBsBankDeposits(individual.getGrossIncome() / 4f);
													}
													attributeIdx = CustomMath
															.sample(pdfAttributeStudentLoan[incomeMapNum], this.random);
													if (attributeIdx == 0) {
														individual.setBsStudentLoans(
																atoPerPersonAttributeStudentLoanDivPoa
																		.get(incomeMapNum));
													}

													// add Individual to matrix (for household calibration) using ABS
													// indices
													// this.individualMatrix.get(poaIdx).get(sexIdx).get(ageIdxAbs).get(divIdx)
													// .get(incomeIdxAbs).add(individual);

													// add individual to simplified map
													// Keys: LGA, AGE5P, INCP
													if (!this.individualMap.containsKey(lgaCode)) {
														this.individualMap.put(lgaCode,
																new HashMap<String, Map<String, ArrayList<Individual>>>(
																		AGE_ARRAY_ABS.length));
													}
													if (!this.individualMap.get(lgaCode)
															.containsKey(AGE_ARRAY_ABS[ageIdxAbs])) {
														this.individualMap.get(lgaCode).put(AGE_ARRAY_ABS[ageIdxAbs],
																new HashMap<String, ArrayList<Individual>>(
																		INDIVIDUAL_INCOME_RANGES_ABS.length));
													}
													if (!this.individualMap.get(lgaCode).get(AGE_ARRAY_ABS[ageIdxAbs])
															.containsKey(INDIVIDUAL_INCOME_RANGES_ABS[incomeIdxAbs])) {
														this.individualMap.get(lgaCode).get(AGE_ARRAY_ABS[ageIdxAbs])
																.put(INDIVIDUAL_INCOME_RANGES_ABS[incomeIdxAbs],
																		new ArrayList<Individual>());
													}
													this.individualMap.get(lgaCode).get(AGE_ARRAY_ABS[ageIdxAbs])
															.get(INDIVIDUAL_INCOME_RANGES_ABS[incomeIdxAbs])
															.add(individual);

													// add Individual to list (for payment clearing algorithm)
													// CHANGE OF MIND: add the chosen Individuals as they are assigned
													// into Households
													// this.individualAgents.add(individual);
													agentId++;
												} // end Individual creation loop
											} // end ABS income indices loop
										} // end ABS age indices loop

									} // end if !state.equals("Other")
								} // end if lgaCode != null
							} // end for POA
						} // end for division
					} // end for sex
				} // end for age
			} // end if multi-index income range mapping
		} // end for ATO income range

		if (DEBUG) {
			int calculatedAdjustedPopulationByLgaTotal = 0;
			for (int poaIdx = 0; poaIdx < censusMatrixPersonsAdjustedPOA.length; poaIdx++) {
				for (int sexIdx = 0; sexIdx < censusMatrixPersonsAdjustedPOA[poaIdx].length; sexIdx++) {
					for (int ageIdx = 0; ageIdx < censusMatrixPersonsAdjustedPOA[poaIdx][sexIdx].length; ageIdx++) {
						for (int divIdx = 0; divIdx < censusMatrixPersonsAdjustedPOA[poaIdx][sexIdx][ageIdx].length; divIdx++) {
							for (int incomeIdx = 0; incomeIdx < censusMatrixPersonsAdjustedPOA[poaIdx][sexIdx][ageIdx][divIdx].length; incomeIdx++) {
								calculatedAdjustedPopulationByLgaTotal += censusMatrixPersonsAdjustedPOA[poaIdx][sexIdx][ageIdx][divIdx][incomeIdx];
							}
						}
					}
				}
			}
			System.out.println(
					"### calculatedAdjustedPopulationByLgaTotal ==> " + calculatedAdjustedPopulationByLgaTotal);
		}

		// release about 7GB of RAM that's no longer needed
		for (String lga : this.individualMap.keySet()) {
			for (String age : this.individualMap.get(lga).keySet()) {
				for (String incp : this.individualMap.get(lga).get(age).keySet()) {
					this.individualMap.get(lga).get(age).get(incp).trimToSize();
				}
			}
		}
		this.close();

		System.out.println(new Date(System.currentTimeMillis()) + ": Finished creating Individual agents");
		System.out.println("Created " + integerFormatter.format(agentId) + " Individual agents");

		if (DEBUG) {
			System.gc();
			long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			float megabytesConsumed = (memoryAfter - memoryBefore) / 1024f / 1024f;
			DecimalFormat decimalFormatter = new DecimalFormat("#,##0.00");
			System.out.println(
					">>> Memory used creating Individual agents: " + decimalFormatter.format(megabytesConsumed) + "MB");
			memoryBefore = memoryAfter;
		}

		/*
		 * ------------------------------------------------------------------------<br>
		 * PART C: CREATING INDIVIDUAL AGENTS
		 * ------------------------------------------------------------------------<br>
		 *
		 * 7. Now start an POA loop, creating Individual agents per ATO data, and
		 * Individuals with no income per ABS data for the other people in each
		 * category. Store them in a map/matrix for now so they're easy to assign into
		 * Households. These are the actual objects that will be used in the model, but
		 * the matrix itself can be dropped once the Individuals are in Households and
		 * calibration is complete.
		 */

		// when reading from the ATO matrix, use modulo to ensure we don't read off the
		// end of the List. It's possible that due to rounding there are more people in
		// the ABS data than the ATO data in a given cell, so just loop back and keep
		// going from the beginning of the List. Will need to make a copy of the
		// Individual on subsequent iterations over the list so we don't end up with
		// multiple pointers to the same person.

		// N.B. Agents are added to economy after being assigned to Households
		// this.addAgentsToEconomy();
	}

	/*
	 * private void addAgentsToEconomy() {
	 * //this.economy.setIndividuals(this.individualAgents);
	 * 
	 * // release about 7GB of RAM that's no longer needed this.close(); }
	 */

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

	@SuppressWarnings("unused")
	private int getAtoAgeIndex(int absAgeIndex) {
		return Math.max(0, Math.min(15, absAgeIndex) - 3);
	}

	private List<Integer> getAbsAgeIndices(int atoAgeIndex) {
		List<Integer> indices = null;
		if (atoAgeIndex == 0) {
			// under 18 = 0-4, 5-9, 10-14, 15-19
			indices = Arrays.asList(new Integer[] { 0, 1, 2, 3 });
		} else if (atoAgeIndex > 11) {
			// 75+ = 75-79, 80-84, 85-89, 90-94, 95-99, 100+
			indices = Arrays.asList(new Integer[] { 15, 16, 17, 18, 19, 20 });
		} else {
			// 18-24 = 20-24; 25-29 = 25-29; ...; 70-74 = 70-74
			indices = Arrays.asList(new Integer[] { atoAgeIndex + 3 });
		}
		return indices;
	}

	/**
	 * Maps indices from ABS individual income ranges to ATO income ranges.
	 * 
	 * @param absIncomeIndex
	 * @return a list of the matching indices, with the best match first
	 */
	private List<Integer> getAtoIncomeIndices(int absIncomeIndex) {
		List<Integer> indices = null;
		if (absIncomeIndex < 3) {
			// <0, 0, <$8k ==> <$6k, $6-10k
			indices = Arrays.asList(new Integer[] { 0, 1 });
		} else if (absIncomeIndex == 3) {
			// $8-16k ==> $10-18k
			indices = Arrays.asList(new Integer[] { 2 });
		} else if (absIncomeIndex == 4 || absIncomeIndex == 5) {
			// $16-20k, $20-26k ==> $18-25k
			indices = Arrays.asList(new Integer[] { 3 });
		} else if (absIncomeIndex == 6) {
			// $26-34k ==> $25-30k, $30-37k
			indices = Arrays.asList(new Integer[] { 4, 5 });
		} else if (absIncomeIndex == 7) {
			// $34-41k ==> $37-40k
			indices = Arrays.asList(new Integer[] { 6 });
		} else if (absIncomeIndex == 8) {
			// $41-52k ==> $40-45k, $45-50k
			indices = Arrays.asList(new Integer[] { 7, 8 });
		} else if (absIncomeIndex == 9) {
			// $52-65k ==> $50-55k, $55-60k, $60-65k
			indices = Arrays.asList(new Integer[] { 9, 10, 11 });
		} else if (absIncomeIndex == 10) {
			// $65-78k ==> $70-80k
			indices = Arrays.asList(new Integer[] { 12 });
		} else if (absIncomeIndex == 11) {
			// $78-91k ==> $80-87k, $87-90k
			indices = Arrays.asList(new Integer[] { 13, 14 });
		} else if (absIncomeIndex == 12) {
			// $91-104k ==> $90-100k
			indices = Arrays.asList(new Integer[] { 15 });
		} else if (absIncomeIndex == 13) {
			// $104-156k ==> $100-150k
			indices = Arrays.asList(new Integer[] { 16 });
		} else if (absIncomeIndex == 14) {
			// $156k+ ==> $150-180k, $180-250k, $250-500k, $500k-$1M, $1M+
			indices = Arrays.asList(new Integer[] { 17, 18, 19, 20, 21 });
		} else {
			// not stated
			indices = null;
		}
		return indices;
	}

	/**
	 * Maps indices from ATO income ranges to ABS individual income ranges.
	 * 
	 * @param atoIncomeIndex
	 * @return a list of the matching indices, with the best match first
	 */
	private List<Integer> getAbsIncomeIndices(int atoIncomeIndex) {
		List<Integer> indices = null;
		if (atoIncomeIndex < 2) {
			// <$6k, $6-10k ==> <0, 0, <$8k
			indices = Arrays.asList(new Integer[] { 0, 1, 2 });
		} else if (atoIncomeIndex == 2) {
			// $10-18k ==> $8-16k
			indices = Arrays.asList(new Integer[] { 3 });
		} else if (atoIncomeIndex == 3) {
			// $18-25k ==> $16-20k, $20-26k
			indices = Arrays.asList(new Integer[] { 4, 5 });
		} else if (atoIncomeIndex == 4 || atoIncomeIndex == 5) {
			// $25-30k, $30-37k ==> $26-34k
			indices = Arrays.asList(new Integer[] { 6 });
		} else if (atoIncomeIndex == 6) {
			// $37-40k ==> $34-41k
			indices = Arrays.asList(new Integer[] { 7 });
		} else if (atoIncomeIndex == 7 || atoIncomeIndex == 8) {
			// $40-45k, $45-50k ==> $41-52k
			indices = Arrays.asList(new Integer[] { 8 });
		} else if (atoIncomeIndex == 9 || atoIncomeIndex == 10 || atoIncomeIndex == 11) {
			// $50-55k, $55-60k, $60-65k ==> $52-65k
			indices = Arrays.asList(new Integer[] { 9 });
		} else if (atoIncomeIndex == 12) {
			// $70-80k ==> $65-78k
			indices = Arrays.asList(new Integer[] { 10 });
		} else if (atoIncomeIndex == 13 || atoIncomeIndex == 14) {
			// $80-87k, $87-90k ==> $78-91k
			indices = Arrays.asList(new Integer[] { 11 });
		} else if (atoIncomeIndex == 15) {
			// $90-100k ==> $91-104k
			indices = Arrays.asList(new Integer[] { 12 });
		} else if (atoIncomeIndex == 16) {
			// $100-150k ==> $104-156k
			indices = Arrays.asList(new Integer[] { 13 });
		} else {
			// $150-180k, $180-250k, $250-500k, $500k-$1M, $1M+ ==> $156k+
			indices = Arrays.asList(new Integer[] { 14 });
		}
		return indices;
	}

	/**
	 * @return the individualMatrix
	 */
	public List<List<List<List<List<List<Individual>>>>>> getIndividualMatrix() {
		return individualMatrix;
	}

	/**
	 * @return the individualMap
	 */
	public Map<String, Map<String, Map<String, ArrayList<Individual>>>> getIndividualMap() {
		return individualMap;
	}

	/**
	 * @return the totalPopulationAU
	 */
	public int getTotalPopulationAU() {
		return totalPopulationAU;
	}

	/**
	 * @return the populationMultiplier
	 */
	public float getPopulationMultiplier() {
		return populationMultiplier;
	}

	/**
	 * @return the poaIndexMap
	 */
	public Map<String, Integer> getPoaIndexMap() {
		return poaIndexMap;
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
	 * @param properties the properties to set
	 */
	@Autowired
	public void setProperties(Properties properties) {
		this.properties = properties;
		this.random = this.properties.getRandom();
	}

}
