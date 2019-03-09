/**
 * 
 */
package xyz.struthers.rhul.ham.data;

import java.text.DateFormat;
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
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import xyz.struthers.rhul.ham.agent.Individual;
import xyz.struthers.rhul.ham.config.Properties;
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

	private static final boolean DEBUG = true;

	// CONSTANTS
	private static final double MILLION = 1000000d;
	private static final double THOUSAND = 1000d;
	private static final double PERCENT = 0.01d;
	private static final double EPSILON = 0.1d; // to round business counts so the integer sums match

	private static final double NUM_MONTHS = 12d;
	private static final double NUM_WEEKS = 365d / 7d;

	public static final String CALIBRATION_DATE_ATO = "01/06/2018";
	public static final String CALIBRATION_DATE_RBA = "30/06/2018";

	public static final double MAP_LOAD_FACTOR = 0.75d;
	public static final int MAP_LGA_INIT_CAPACITY = (int) Math.ceil(540 / MAP_LOAD_FACTOR) + 1;
	private static final String[] SEX_ARRAY = { "M", "F" };
	private static final String[] STATES_ARRAY = { "NSW", "VIC", "QLD", "SA", "WA", "TAS", "NT", "ACT", "OT" };
	private static final int[] MAP_STATE_POA_CAPACITY = { (int) Math.ceil(612 / MAP_LOAD_FACTOR) + 1,
			(int) Math.ceil(382 / MAP_LOAD_FACTOR) + 1, (int) Math.ceil(429 / MAP_LOAD_FACTOR) + 1,
			(int) Math.ceil(326 / MAP_LOAD_FACTOR) + 1, (int) Math.ceil(338 / MAP_LOAD_FACTOR) + 1,
			(int) Math.ceil(111 / MAP_LOAD_FACTOR) + 1, (int) Math.ceil(43 / MAP_LOAD_FACTOR) + 1,
			(int) Math.ceil(25 / MAP_LOAD_FACTOR) + 1, (int) Math.ceil(1 / MAP_LOAD_FACTOR) + 1 };
	private static final String[] AGE_ARRAY_ATO = { "a. Under 18", "b. 18 - 24", "c. 25 - 29", "d. 30 - 34",
			"e. 35 - 39", "f. 40 - 44", "g. 45 - 49", "h. 50 - 54", "i. 55 - 59", "j. 60 - 64", "k. 65 - 69",
			"l. 70 - 74", "m. 75 and over" };
	private static final String[] AGE_ARRAY_ABS = { "0-4 years", "5-9 years", "10-14 years", "15-19 years",
			"20-24 years", "25-29 years", "30-34 years", "35-39 years", "40-44 years", "45-49 years", "50-54 years",
			"55-59 years", "60-64 years", "65-69 years", "70-74 years", "75-79 years", "80-84 years", "85-89 years",
			"90-94 years", "95-99 years", "100 years and over" };
	private static final String[] DIVISION_CODE_ARRAY = { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L",
			"M", "N", "O", "P", "Q", "R", "S" }; // S = Other Services
	private static final int NUM_DIVISIONS = DIVISION_CODE_ARRAY.length; // 19
	private static final String[] INDIVIDUAL_INCOME_RANGES_ABS = { "Negative income", "Nil income",
			"$1-$149 ($1-$7,799)", "$150-$299 ($7,800-$15,599)", "$300-$399 ($15,600-$20,799)",
			"$400-$499 ($20,800-$25,999)", "$500-$649 ($26,000-$33,799)", "$650-$799 ($33,800-$41,599)",
			"$800-$999 ($41,600-$51,999)", "$1,000-$1,249 ($52,000-$64,999)", "$1,250-$1,499 ($65,000-$77,999)",
			"$1,500-$1,749 ($78,000-$90,999)", "$1,750-$1,999 ($91,000-$103,999)", "$2,000-$2,999 ($104,000-$155,999)",
			"$3,000 or more ($156,000 or more)", "Not stated" };
	private static final int NUM_INDIVIDUAL_INCOME_RANGES_ABS = 14;
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

	private static final String ATO_2A_TITLE_COUNT = "Number of individuals no.";
	private static final String ATO_2A_TITLE_TAXABLE_COUNT = "Taxable income or loss3 no.";
	private static final String ATO_2A_TITLE_TAXABLE_AMOUNT = "Taxable income or loss3 $";

	private static final String ATO_3A_TITLE_TAXABLE_COUNT = "Taxable income or loss2 no.";
	private static final String ATO_3A_TITLE_TAXABLE_AMOUNT = "Taxable income or loss2 $";
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
	private static final String ATO_3A_TITLE_INTEREST_DEDUCTIONS_COUNT = "Interest deductions no.";
	private static final String ATO_3A_TITLE_INTEREST_DEDUCTIONS_AMOUNT = "Interest deductions $";
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
	private Properties properties;

	// field variables
	private Random random;
	/**
	 * The matrix of Individual agents.<br>
	 * Keys: postcode, sex, age, industry division, income (ABS categories)
	 */
	private List<List<List<List<List<List<Individual>>>>>> individualMatrix;
	private ArrayList<Individual> individualAgents;
	private Date calibrationDateAto;
	private Date calibrationDateRba;
	private int totalPopulationAU;
	private double populationMultiplier;
	private Map<String, Integer> lgaPeopleCount; // adjusted to 2018
	private Map<String, Integer> lgaDwellingsCount; // adjusted to 2018

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
		this.individualMatrix = null;
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
		System.out.println(new Date(System.currentTimeMillis()) + ": Starting creation of Individual agents");

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

		Map<String, Double> divisionTaxableIncomeMultiplier = new HashMap<String, Double>(NUM_DIVISIONS);
		Map<String, Double> divisionCountMultiplier = new HashMap<String, Double>(NUM_DIVISIONS);
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
			divisionTaxableIncomeMultiplier.put(divCode, divTaxablePerPerson);
			divisionCountMultiplier.put(divCode, divTaxableCount);
			divTotalCount9A += divTaxableCount;
			divTotalAmount9A += divTaxableAmount;
		}
		double divTotalTaxablePerPerson = divTotalCount9A > 0d ? divTotalAmount9A / divTotalCount9A : 0d;
		for (String divCode : divisionCodeSet) {
			double divTaxableIncomeMultiplier = divisionTaxableIncomeMultiplier.get(divCode) / divTotalTaxablePerPerson;
			double divCountMultiplier = divisionCountMultiplier.get(divCode) / divTotalCount9A;
			divisionTaxableIncomeMultiplier.put(divCode, divTaxableIncomeMultiplier);
			divisionCountMultiplier.put(divCode, divCountMultiplier);
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
		Map<String, Map<String, Map<String, Double>>> stateTaxableCount = new HashMap<String, Map<String, Map<String, Double>>>(
				sexMapCapacity);
		Map<String, Map<String, Map<String, Double>>> stateTaxableAmount = new HashMap<String, Map<String, Map<String, Double>>>(
				sexMapCapacity);
		Map<String, Map<String, Map<String, Double>>> stateTaxableIncomeMultiplier = new HashMap<String, Map<String, Map<String, Double>>>(
				sexMapCapacity);
		Map<String, Map<String, Map<String, Double>>> stateCountMultiplier = new HashMap<String, Map<String, Map<String, Double>>>(
				sexMapCapacity);
		Map<String, Map<String, Double>> stateSexAgeNationalTotalCount = new HashMap<String, Map<String, Double>>(
				sexMapCapacity);
		Map<String, Map<String, Double>> stateSexAgeNationalTotalAmount = new HashMap<String, Map<String, Double>>(
				sexMapCapacity);
		for (String sex : SEX_ARRAY) {
			stateTaxableCount.put(sex, new HashMap<String, Map<String, Double>>(ageMapCapacity));
			stateTaxableAmount.put(sex, new HashMap<String, Map<String, Double>>(ageMapCapacity));
			stateTaxableIncomeMultiplier.put(sex, new HashMap<String, Map<String, Double>>(ageMapCapacity));
			stateCountMultiplier.put(sex, new HashMap<String, Map<String, Double>>(ageMapCapacity));
			stateSexAgeNationalTotalCount.put(sex, new HashMap<String, Double>(ageMapCapacity));
			stateSexAgeNationalTotalAmount.put(sex, new HashMap<String, Double>(ageMapCapacity));
			for (String age : AGE_ARRAY_ATO) {
				stateTaxableCount.get(sex).put(age, new HashMap<String, Double>(stateMapCapacity));
				stateTaxableAmount.get(sex).put(age, new HashMap<String, Double>(stateMapCapacity));
				stateTaxableIncomeMultiplier.get(sex).put(age, new HashMap<String, Double>(stateMapCapacity));
				stateCountMultiplier.get(sex).put(age, new HashMap<String, Double>(stateMapCapacity));
				stateSexAgeNationalTotalCount.get(sex).put(age, 0d);
				stateSexAgeNationalTotalAmount.get(sex).put(age, 0d);
				for (String state : STATES_ARRAY) {
					stateTaxableCount.get(sex).get(age).put(state, 0d);
					stateTaxableAmount.get(sex).get(age).put(state, 0d);
					stateTaxableIncomeMultiplier.get(sex).get(age).put(state, 0d);
					stateCountMultiplier.get(sex).get(age).put(state, 0d);
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
		for (String sex : SEX_ARRAY) {
			for (String age : AGE_ARRAY_ATO) {
				double sexAgeCount = stateSexAgeNationalTotalCount.get(sex).get(age);
				double sexAgeAmount = stateSexAgeNationalTotalAmount.get(sex).get(age);
				double sexAgeTaxablePerPerson = sexAgeCount > 0d ? sexAgeAmount / sexAgeCount : 0d;
				for (String state : STATES_ARRAY) {
					double stateCount = stateTaxableCount.get(sex).get(age).get(state);
					double stateAmount = stateTaxableAmount.get(sex).get(age).get(state);
					double stateTaxablePerPerson = stateAmount / stateCount;
					double thisStateTaxableIncomeMultiplier = sexAgeTaxablePerPerson > 0d
							? stateTaxablePerPerson / sexAgeTaxablePerPerson
							: 0d;
					double thisStateCountMultiplier = sexAgeCount > 0d ? stateCount / sexAgeCount : 0d;
					stateTaxableIncomeMultiplier.get(sex).get(age).put(state, thisStateTaxableIncomeMultiplier);
					stateCountMultiplier.get(sex).get(age).put(state, thisStateCountMultiplier);
				}
			}
		}

		/*
		 * 3. ATO Individual Table 6B: people count, taxable income (per POA). Calculate
		 * POA multiplier for each POA, by state.<br>
		 */
		System.out.println(new Date(System.currentTimeMillis()) + ": 3. ATO Individual Table 6B postcode multipliers");

		// ATO 6B Keys: Series Title, Post Code
		// postcodeMultiplier Keys: State Code, Post Code
		Map<String, Map<String, Double>> postcodeStateTaxableIncomeMultiplier = new HashMap<String, Map<String, Double>>(
				stateMapCapacity);
		Map<String, Map<String, Double>> postcodeStateCountMultiplier = new HashMap<String, Map<String, Double>>(
				stateMapCapacity);
		Map<String, Double> postcodeStateTotalCount = new HashMap<String, Double>(stateMapCapacity);
		Map<String, Double> postcodeStateTotalAmount = new HashMap<String, Double>(stateMapCapacity);
		Map<String, Double> postcodeStateTaxablePerPerson = new HashMap<String, Double>(stateMapCapacity);

		// initialise state multiplier map
		for (int i = 0; i < STATES_ARRAY.length; i++) {
			String state = STATES_ARRAY[i];
			postcodeStateTaxableIncomeMultiplier.put(state, new HashMap<String, Double>(MAP_STATE_POA_CAPACITY[i]));
			postcodeStateCountMultiplier.put(state, new HashMap<String, Double>(MAP_STATE_POA_CAPACITY[i]));
			postcodeStateTotalCount.put(state, 0d);
			postcodeStateTotalAmount.put(state, 0d);
		}

		Set<String> postcodeSet = this.atoIndividualTable6b.get(ATO_6B_TITLE_TAXABLE_COUNT).keySet();
		for (String postcode : postcodeSet) {
			String state = this.area.getStateFromPoa(postcode);
			if (state != null && state != "Other") {
				// postcode 3694 returns null, so just skip null values for now
				// calculate average income per postcode, and state totals for all postcodes
				double postcodeTaxableCount = Double.valueOf(
						this.atoIndividualTable6b.get(ATO_6B_TITLE_TAXABLE_COUNT).get(postcode).replace(",", ""));
				double postcodeTaxableAmount = Double.valueOf(
						this.atoIndividualTable6b.get(ATO_6B_TITLE_TAXABLE_AMOUNT).get(postcode).replace(",", ""));
				double postcodeTaxablePerPerson = postcodeTaxableAmount / postcodeTaxableCount;

				// Just an efficient place to hold this. Will be overwritten in the loop below.
				postcodeStateTaxableIncomeMultiplier.get(state).put(postcode, postcodeTaxablePerPerson);
				postcodeStateCountMultiplier.get(state).put(postcode, postcodeTaxableCount);
				double oldStateTotalCount = postcodeStateTotalCount.get(state);
				postcodeStateTotalCount.put(state, oldStateTotalCount + postcodeTaxableCount);
				double oldStateTotalAmount = postcodeStateTotalAmount.get(state);
				postcodeStateTotalAmount.put(state, oldStateTotalAmount + postcodeTaxableAmount);
			}
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
			if (state != null & state != "Other") {
				double stateTaxablePerPerson = postcodeStateTaxablePerPerson.get(state);
				double stateCount = postcodeStateTotalCount.get(state);
				double postcodeTaxableIncomeMultiplier = postcodeStateTaxableIncomeMultiplier.get(state).get(postcode)
						/ stateTaxablePerPerson;
				double postcodeCountMultiplier = postcodeStateCountMultiplier.get(state).get(postcode) / stateCount;
				postcodeStateTaxableIncomeMultiplier.get(state).put(postcode, postcodeTaxableIncomeMultiplier);
				postcodeStateCountMultiplier.get(state).put(postcode, postcodeCountMultiplier);
			}
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
		Map<String, Integer> poaIndexMap = new HashMap<String, Integer>(
				(int) Math.ceil(poaSetIntersection.size() / MAP_LOAD_FACTOR));
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
			poaIndexMap.put(poa, i);
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
				String divDescr = this.abs1292_0_55_002ANZSIC.get("Division Code to Division").get(divisionCode);
				for (String incomeRange : INDIVIDUAL_INCOME_RANGES_ABS) {
					int incomeIdx = individualIncomeIndexMap.get(incomeRange);
					for (String poa : poaSetIntersection) {
						int poaIdx = poaIndexMap.get(poa);
						for (String sex : SEX_ARRAY) {
							int sexIdx = sexIndexMap.get(sex);
							int oldVal = censusMatrixPersonsAdjustedPOA[poaIdx][sexIdx][ageIdx][divIdx][incomeIdx];
							int adjustedPopulation = 0;
							try {
								adjustedPopulation = (int) Math
										.round(Double
												.valueOf(this.censusSEXP_POA_AGE5P_INDP_INCP.get(age).get(divisionCode)
														.get(incomeRange).get(poa).get(sex))
												* this.populationMultiplier);
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
		if (this.individualMatrix == null) {
			this.individualMatrix = new ArrayList<List<List<List<List<List<Individual>>>>>>(poaSetIntersection.size());
			// initialise matrix
			for (String poa : poaSetIntersection) {
				int poaIdx = poaIndexMap.get(poa);
				this.individualMatrix.add(new ArrayList<List<List<List<List<Individual>>>>>(SEX_ARRAY.length));
				for (int sexIdx = 0; sexIdx < SEX_ARRAY.length; sexIdx++) {
					this.individualMatrix.get(poaIdx)
							.add(new ArrayList<List<List<List<Individual>>>>(AGE_ARRAY_ABS.length));
					for (int ageIdx = 0; ageIdx < AGE_ARRAY_ABS.length; ageIdx++) {
						this.individualMatrix.get(poaIdx).get(sexIdx)
								.add(new ArrayList<List<List<Individual>>>(DIVISION_CODE_ARRAY.length));
						for (int divIdx = 0; divIdx < DIVISION_CODE_ARRAY.length; divIdx++) {
							this.individualMatrix.get(poaIdx).get(sexIdx).get(ageIdx)
									.add(new ArrayList<List<Individual>>(INDIVIDUAL_INCOME_RANGES_ABS.length));
							for (int incomeIdx = 0; incomeIdx < INDIVIDUAL_INCOME_RANGES_ABS.length; incomeIdx++) {
								this.individualMatrix.get(poaIdx).get(sexIdx).get(ageIdx).get(divIdx)
										.add(new ArrayList<Individual>());
							}
						}
					}
				}
			}
		}
		if (this.individualAgents == null) {
			// add in a 5% buffer so the List doesn't end up double the size it needs to be
			int initCapacity = (int) Math.round(this.totalPopulationAU * 1.05d);
			this.individualAgents = new ArrayList<Individual>(initCapacity);
		}

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
		double atoAbsPopMult = 0d;
		// for income, age, sex, taxable status
		for (int incomeAtoIdx = 0; incomeAtoIdx < INDIVIDUAL_INCOME_RANGES_ATO3A.length; incomeAtoIdx++) {
			String incomeRangeAto = INDIVIDUAL_INCOME_RANGES_ATO3A[incomeAtoIdx];
			List<Integer> incomeIndicesAbs = this.getAbsIncomeIndices(incomeAtoIdx);
			if (DEBUG) {
				System.out.println("incomeRangeAto: " + incomeRangeAto);
			}
			// Skip iterations for n:1 mappings. We need to aggregate the ATO data when
			// calculating ratios.
			// 0-1 ==> 2, 3 ==> 5-4, 4-5 ==> 6, 7-8 ==> 8, 9-11 ==> 9, 13-14 ==> 11
			if (incomeAtoIdx == 1 || incomeAtoIdx == 5 || incomeAtoIdx == 8 || incomeAtoIdx == 10 || incomeAtoIdx == 11
					|| incomeAtoIdx == 14) {
				// skip the rest of this nested loop because this was already dealt with in the
				// earlier part of this multi-index mapping
			} else {
				// continue processing nested loop
				for (int ageIdxAto = 0; ageIdxAto < AGE_ARRAY_ATO.length; ageIdxAto++) {
					String age = AGE_ARRAY_ATO[ageIdxAto];
					List<Integer> ageIndicesAbs = this.getAbsAgeIndices(ageIdxAto);
					if (DEBUG) {
						System.out.println("   age: " + age);
					}
					// [0] under 18 = 0-4, 5-9, 10-14, 15-19
					// [12] 75+ = 75-79, 80-84, 85-89, 90-94, 95-99, 100+
					// [1-11] 18-24 = 20-24; 25-29 = 25-29; ...; 70-74 = 70-74
					for (int sexIdx = 0; sexIdx < SEX_ARRAY.length; sexIdx++) {
						String sex = SEX_ARRAY[sexIdx];
						if (DEBUG) {
							System.out.println("      sex: " + sex);
						}
						// variables to calculate ABS/ATO population multiplier
						double employedCount = 0d;
						double unemployedCount = 0d;
						double pensionCount = 0d;
						double selfFundedRetireeCount = 0d;
						double foreignIncomeCount = 0d;
						double noIncomeCount = 0d;

						// variables to calibrate Individual agents
						int[] atoCountEmployed = new int[] { 0, 0, 0 };
						int[] atoCountUnemployed = new int[] { 0, 0, 0 };
						int[] atoCountPension = new int[] { 0, 0, 0 };
						int[] atoCountSelfFundedRetiree = new int[] { 0, 0, 0 };
						int[] atoCountForeignIncome = new int[] { 0, 0, 0 };
						int[] atoCountAttributeInterestIncome = new int[] { 0, 0, 0 };
						int[] atoCountAttributeDividendIncome = new int[] { 0, 0, 0 };
						int[] atoCountAttributeDonations = new int[] { 0, 0, 0 };
						int[] atoCountAttributeRentIncome = new int[] { 0, 0, 0 };
						int[] atoCountAttributeRentInterest = new int[] { 0, 0, 0 };
						int[] atoCountAttributeOtherIncome = new int[] { 0, 0, 0 };
						int[] atoCountAttributeTotalIncome = new int[] { 0, 0, 0 };
						int[] atoCountAttributeStudentLoan = new int[] { 0, 0, 0 };
						long[] atoAmountEmployed = new long[] { 0, 0, 0 };
						long[] atoAmountUnemployed = new long[] { 0, 0, 0 };
						long[] atoAmountPension = new long[] { 0, 0, 0 };
						long[] atoAmountSelfFundedRetiree = new long[] { 0, 0, 0 };
						long[] atoAmountForeignIncome = new long[] { 0, 0, 0 };
						long[] atoAmountAttributeInterestIncome = new long[] { 0, 0, 0 };
						long[] atoAmountAttributeDividendIncome = new long[] { 0, 0, 0 };
						long[] atoAmountAttributeDonations = new long[] { 0, 0, 0 };
						long[] atoAmountAttributeRentIncome = new long[] { 0, 0, 0 };
						long[] atoAmountAttributeRentInterest = new long[] { 0, 0, 0 };
						long[] atoAmountAttributeOtherIncome = new long[] { 0, 0, 0 };
						long[] atoAmountAttributeTotalIncome = new long[] { 0, 0, 0 };
						long[] atoAmountAttributeStudentLoan = new long[] { 0, 0, 0 };

						// need corresponding if statements in the nested loops, retrieving multiple
						// indices' values
						// 0-1 ==> 2, 3 ==> 5-4, 4-5 ==> 6, 7-8 ==> 8, 9-11 ==> 9, 13-14 ==> 11
						for (int taxableStatusIdx = 0; taxableStatusIdx < TAXABLE_STATUS.length; taxableStatusIdx++) {
							String taxableStatus = TAXABLE_STATUS[taxableStatusIdx];
							if (DEBUG) {
								System.out.println("      -taxableStatus: " + taxableStatus);
							}
							// variables to calculate ABS/ATO population multiplier
							// get taxable count by income, age & sex (don't care about taxable status). Use
							// sum of count by main income type, rather than just taxable income count.

							// check that this category has data first (to avoid null pointer exceptions)
							if (this.atoIndividualTable3a.get(ATO_3A_TITLE_SALARY_COUNT).containsKey(incomeRangeAto)
									&& this.atoIndividualTable3a.get(ATO_3A_TITLE_SALARY_COUNT).get(incomeRangeAto)
											.containsKey(age)
									&& this.atoIndividualTable3a.get(ATO_3A_TITLE_SALARY_COUNT).get(incomeRangeAto)
											.get(age).containsKey(sex)
									&& this.atoIndividualTable3a.get(ATO_3A_TITLE_SALARY_COUNT).get(incomeRangeAto)
											.get(age).get(sex).containsKey(taxableStatus)) {

								System.out.println("ATO_3A_TITLE_SALARY_COUNT: " + ATO_3A_TITLE_SALARY_COUNT);
								System.out.println("incomeRangeAto: " + incomeRangeAto);
								System.out.println("age: " + age);
								System.out.println("sex: " + sex);
								System.out.println("taxableStatus: " + taxableStatus);
								System.out.println(
										"this.atoIndividualTable3a.keySet(): " + this.atoIndividualTable3a.keySet());
								System.out.println("...get(ATO_3A_TITLE_SALARY_COUNT).keySet(): "
										+ this.atoIndividualTable3a.get(ATO_3A_TITLE_SALARY_COUNT).keySet());
								System.out.println("...get(incomeRangeAto).keySet(): " + this.atoIndividualTable3a
										.get(ATO_3A_TITLE_SALARY_COUNT).get(incomeRangeAto).keySet());
								System.out.println("...get(age).keySet(): " + this.atoIndividualTable3a
										.get(ATO_3A_TITLE_SALARY_COUNT).get(incomeRangeAto).get(age).keySet());
								System.out.println("...get(sex).keySet(): " + this.atoIndividualTable3a
										.get(ATO_3A_TITLE_SALARY_COUNT).get(incomeRangeAto).get(age).get(sex).keySet());
								
								employedCount += Math.max(
										Double.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_SALARY_COUNT)
												.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)
												.replace(",", "")),
										Double.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_ALLOWANCES_COUNT)
												.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)
												.replace(",", "")));
								unemployedCount += Double.valueOf(
										this.atoIndividualTable3a.get(ATO_3A_TITLE_GOVT_ALLOW_COUNT).get(incomeRangeAto)
												.get(age).get(sex).get(taxableStatus).replace(",", ""));
								pensionCount += Double.valueOf(this.atoIndividualTable3a
										.get(ATO_3A_TITLE_GOVT_PENSION_COUNT).get(incomeRangeAto).get(age).get(sex)
										.get(taxableStatus).replace(",", ""));
								selfFundedRetireeCount += Math.max(
										Double.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_SUPER_TAXED_COUNT)
												.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)
												.replace(",", "")),
										Double.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_SUPER_UNTAXED_COUNT)
												.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)
												.replace(",", "")));
								foreignIncomeCount += Math.max(
										Double.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_FOREIGN_INCOME_COUNT)
												.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)
												.replace(",", "")),
										Double.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_FOREIGN_INCOME2_COUNT)
												.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)
												.replace(",", "")));
								noIncomeCount += Double.valueOf(this.atoIndividualTable3a
										.get(ATO_3A_TITLE_TOTAL_INCOME_COUNT).get(incomeRangeAto).get(age).get(sex)
										.get(taxableStatus).replace(",", ""));

								// variables to calibrate Individual agents
								atoCountEmployed[0] = Math.max(
										Integer.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_SALARY_COUNT)
												.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)
												.replace(",", "")),
										Integer.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_ALLOWANCES_COUNT)
												.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)
												.replace(",", "")));
								atoCountUnemployed[0] = Integer.valueOf(
										this.atoIndividualTable3a.get(ATO_3A_TITLE_GOVT_ALLOW_COUNT).get(incomeRangeAto)
												.get(age).get(sex).get(taxableStatus).replace(",", ""));
								atoCountPension[0] = Integer.valueOf(this.atoIndividualTable3a
										.get(ATO_3A_TITLE_GOVT_PENSION_COUNT).get(incomeRangeAto).get(age).get(sex)
										.get(taxableStatus).replace(",", ""));
								atoCountSelfFundedRetiree[0] = Math.max(
										Integer.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_SUPER_TAXED_COUNT)
												.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)
												.replace(",", "")),
										Integer.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_SUPER_UNTAXED_COUNT)
												.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)
												.replace(",", "")));
								atoCountForeignIncome[0] = Math.max(
										Integer.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_FOREIGN_INCOME_COUNT)
												.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)
												.replace(",", "")),
										Integer.valueOf(this.atoIndividualTable3a
												.get(ATO_3A_TITLE_FOREIGN_INCOME2_COUNT).get(incomeRangeAto).get(age)
												.get(sex).get(taxableStatus).replace(",", "")));
								atoCountAttributeInterestIncome[0] = Integer.valueOf(this.atoIndividualTable3a
										.get(ATO_3A_TITLE_INTEREST_INCOME_COUNT).get(incomeRangeAto).get(age).get(sex)
										.get(taxableStatus).replace(",", ""));
								// 36% of Australian adults own shares, so sum these counts rather than just
								// taking the maximum
								// SOURCE: ASX (2014), 'The Australian Share Ownership Study': Sydney, NSW.
								atoCountAttributeDividendIncome[0] = Integer
										.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_DIVIDENDS_UNFRANKED_COUNT)
												.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)
												.replace(",", ""))
										+ Integer.valueOf(this.atoIndividualTable3a
												.get(ATO_3A_TITLE_DIVIDENDS_FRANKED_COUNT).get(incomeRangeAto).get(age)
												.get(sex).get(taxableStatus).replace(",", ""));
								atoCountAttributeDonations[0] = Integer.valueOf(
										this.atoIndividualTable3a.get(ATO_3A_TITLE_DONATIONS_COUNT).get(incomeRangeAto)
												.get(age).get(sex).get(taxableStatus).replace(",", ""));
								atoCountAttributeRentIncome[0] = Integer.valueOf(this.atoIndividualTable3a
										.get(ATO_3A_TITLE_RENT_INCOME_COUNT).get(incomeRangeAto).get(age).get(sex)
										.get(taxableStatus).replace(",", ""));
								atoCountAttributeRentInterest[0] = Integer.valueOf(this.atoIndividualTable3a
										.get(ATO_3A_TITLE_RENT_INTEREST_COUNT).get(incomeRangeAto).get(age).get(sex)
										.get(taxableStatus).replace(",", ""));
								atoCountAttributeTotalIncome[0] = Integer.valueOf(this.atoIndividualTable3a
										.get(ATO_3A_TITLE_TOTAL_INCOME_COUNT).get(incomeRangeAto).get(age).get(sex)
										.get(taxableStatus).replace(",", ""));
								// SFSS is on top of other loans, so count is max (HELP + TSL, SFSS)
								atoCountAttributeStudentLoan[0] = Math.max(
										Integer.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_HELP_DEBT_COUNT)
												.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)
												.replace(",", ""))
												+ Integer.valueOf(this.atoIndividualTable3a
														.get(ATO_3A_TITLE_TSL_DEBT_COUNT).get(incomeRangeAto).get(age)
														.get(sex).get(taxableStatus).replace(",", "")),
										Integer.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_SFSS_DEBT_COUNT)
												.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)
												.replace(",", "")));
								atoAmountEmployed[0] = Long
										.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_SALARY_AMOUNT)
												.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)
												.replace(",", ""))
										+ Long.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_ALLOWANCES_AMOUNT)
												.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)
												.replace(",", ""));
								atoAmountUnemployed[0] = Long.valueOf(this.atoIndividualTable3a
										.get(ATO_3A_TITLE_GOVT_ALLOW_AMOUNT).get(incomeRangeAto).get(age).get(sex)
										.get(taxableStatus).replace(",", ""));
								atoAmountPension[0] = Long.valueOf(this.atoIndividualTable3a
										.get(ATO_3A_TITLE_GOVT_PENSION_AMOUNT).get(incomeRangeAto).get(age).get(sex)
										.get(taxableStatus).replace(",", ""));
								atoAmountSelfFundedRetiree[0] = Long
										.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_SUPER_TAXED_AMOUNT)
												.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)
												.replace(",", ""))
										+ Long.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_SUPER_UNTAXED_AMOUNT)
												.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)
												.replace(",", ""));
								atoAmountForeignIncome[0] = Long
										.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_FOREIGN_INCOME_AMOUNT)
												.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)
												.replace(",", ""))
										+ Long.valueOf(this.atoIndividualTable3a
												.get(ATO_3A_TITLE_FOREIGN_INCOME2_AMOUNT).get(incomeRangeAto).get(age)
												.get(sex).get(taxableStatus).replace(",", ""));
								atoAmountAttributeInterestIncome[0] = Long.valueOf(this.atoIndividualTable3a
										.get(ATO_3A_TITLE_INTEREST_INCOME_AMOUNT).get(incomeRangeAto).get(age).get(sex)
										.get(taxableStatus).replace(",", ""));
								atoAmountAttributeDividendIncome[0] = Long
										.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_DIVIDENDS_UNFRANKED_AMOUNT)
												.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)
												.replace(",", ""))
										+ Long.valueOf(this.atoIndividualTable3a
												.get(ATO_3A_TITLE_DIVIDENDS_FRANKED_AMOUNT).get(incomeRangeAto).get(age)
												.get(sex).get(taxableStatus).replace(",", ""));
								atoAmountAttributeDonations[0] = Long.valueOf(
										this.atoIndividualTable3a.get(ATO_3A_TITLE_DONATIONS_AMOUNT).get(incomeRangeAto)
												.get(age).get(sex).get(taxableStatus).replace(",", ""));
								atoAmountAttributeRentIncome[0] = Long.valueOf(this.atoIndividualTable3a
										.get(ATO_3A_TITLE_RENT_INCOME_AMOUNT).get(incomeRangeAto).get(age).get(sex)
										.get(taxableStatus).replace(",", ""));
								atoAmountAttributeRentInterest[0] = Long.valueOf(this.atoIndividualTable3a
										.get(ATO_3A_TITLE_RENT_INTEREST_AMOUNT).get(incomeRangeAto).get(age).get(sex)
										.get(taxableStatus).replace(",", ""));
								atoAmountAttributeTotalIncome[0] = Long.valueOf(this.atoIndividualTable3a
										.get(ATO_3A_TITLE_TOTAL_INCOME_AMOUNT).get(incomeRangeAto).get(age).get(sex)
										.get(taxableStatus).replace(",", ""));
								atoAmountAttributeStudentLoan[0] = Long
										.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_HELP_DEBT_AMOUNT)
												.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)
												.replace(",", ""))
										+ Long.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_TSL_DEBT_AMOUNT)
												.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)
												.replace(",", ""))
										+ Long.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_SFSS_DEBT_AMOUNT)
												.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)
												.replace(",", ""));
							}

							if (incomeAtoIdx == 0 || incomeAtoIdx == 4 || incomeAtoIdx == 7 || incomeAtoIdx == 9
									|| incomeAtoIdx == 13) {
								// add sums for extra indices where it's a 2:1 mapping
								// 0-1 ==> 2, 3 ==> 5-4, 4-5 ==> 6, 7-8 ==> 8, 9-11 ==> 9, 13-14 ==> 11

								// variables to calculate ABS/ATO population multiplier
								incomeRangeAto = INDIVIDUAL_INCOME_RANGES_ATO3A[incomeAtoIdx + 1];

								// check that this category has data first (to avoid null pointer exceptions)
								if (this.atoIndividualTable3a.get(ATO_3A_TITLE_SALARY_COUNT).containsKey(incomeRangeAto)
										&& this.atoIndividualTable3a.get(ATO_3A_TITLE_SALARY_COUNT).get(incomeRangeAto)
												.containsKey(age)
										&& this.atoIndividualTable3a.get(ATO_3A_TITLE_SALARY_COUNT).get(incomeRangeAto)
												.get(age).containsKey(sex)
										&& this.atoIndividualTable3a.get(ATO_3A_TITLE_SALARY_COUNT).get(incomeRangeAto)
												.get(age).get(sex).containsKey(taxableStatus)) {

									employedCount += Math.max(
											Double.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_SALARY_COUNT)
													.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)
													.replace(",", "")),
											Double.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_ALLOWANCES_COUNT)
													.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)
													.replace(",", "")));
									unemployedCount += Double.valueOf(this.atoIndividualTable3a
											.get(ATO_3A_TITLE_GOVT_ALLOW_COUNT).get(incomeRangeAto).get(age).get(sex)
											.get(taxableStatus).replace(",", ""));
									pensionCount += Double.valueOf(this.atoIndividualTable3a
											.get(ATO_3A_TITLE_GOVT_PENSION_COUNT).get(incomeRangeAto).get(age).get(sex)
											.get(taxableStatus).replace(",", ""));
									selfFundedRetireeCount += Math.max(
											Double.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_SUPER_TAXED_COUNT)
													.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)
													.replace(",", "")),
											Double.valueOf(this.atoIndividualTable3a
													.get(ATO_3A_TITLE_SUPER_UNTAXED_COUNT).get(incomeRangeAto).get(age)
													.get(sex).get(taxableStatus).replace(",", "")));
									foreignIncomeCount += Math.max(
											Double.valueOf(this.atoIndividualTable3a
													.get(ATO_3A_TITLE_FOREIGN_INCOME_COUNT).get(incomeRangeAto).get(age)
													.get(sex).get(taxableStatus).replace(",", "")),
											Double.valueOf(this.atoIndividualTable3a
													.get(ATO_3A_TITLE_FOREIGN_INCOME2_COUNT).get(incomeRangeAto)
													.get(age).get(sex).get(taxableStatus).replace(",", "")));
									noIncomeCount += Double.valueOf(this.atoIndividualTable3a
											.get(ATO_3A_TITLE_TOTAL_INCOME_COUNT).get(incomeRangeAto).get(age).get(sex)
											.get(taxableStatus).replace(",", ""));

									// variables to calibrate Individual agents
									atoCountEmployed[1] = Math.max(
											Integer.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_SALARY_COUNT)
													.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)
													.replace(",", "")),
											Integer.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_ALLOWANCES_COUNT)
													.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)
													.replace(",", "")));
									atoCountUnemployed[1] = Integer.valueOf(this.atoIndividualTable3a
											.get(ATO_3A_TITLE_GOVT_ALLOW_COUNT).get(incomeRangeAto).get(age).get(sex)
											.get(taxableStatus).replace(",", ""));
									atoCountPension[1] = Integer.valueOf(this.atoIndividualTable3a
											.get(ATO_3A_TITLE_GOVT_PENSION_COUNT).get(incomeRangeAto).get(age).get(sex)
											.get(taxableStatus).replace(",", ""));
									atoCountSelfFundedRetiree[1] = Math.max(
											Integer.valueOf(this.atoIndividualTable3a
													.get(ATO_3A_TITLE_SUPER_TAXED_COUNT).get(incomeRangeAto).get(age)
													.get(sex).get(taxableStatus).replace(",", "")),
											Integer.valueOf(this.atoIndividualTable3a
													.get(ATO_3A_TITLE_SUPER_UNTAXED_COUNT).get(incomeRangeAto).get(age)
													.get(sex).get(taxableStatus).replace(",", "")));
									atoCountForeignIncome[1] = Math.max(
											Integer.valueOf(this.atoIndividualTable3a
													.get(ATO_3A_TITLE_FOREIGN_INCOME_COUNT).get(incomeRangeAto).get(age)
													.get(sex).get(taxableStatus).replace(",", "")),
											Integer.valueOf(this.atoIndividualTable3a
													.get(ATO_3A_TITLE_FOREIGN_INCOME2_COUNT).get(incomeRangeAto)
													.get(age).get(sex).get(taxableStatus).replace(",", "")));
									atoCountAttributeInterestIncome[1] = Integer.valueOf(this.atoIndividualTable3a
											.get(ATO_3A_TITLE_INTEREST_INCOME_COUNT).get(incomeRangeAto).get(age)
											.get(sex).get(taxableStatus).replace(",", ""));
									// 36% of Australian adults own shares, so sum these counts rather than just
									// taking the maximum
									// SOURCE: ASX (2014), 'The Australian Share Ownership Study': Sydney, NSW.
									atoCountAttributeDividendIncome[1] = Integer
											.valueOf(this.atoIndividualTable3a
													.get(ATO_3A_TITLE_DIVIDENDS_UNFRANKED_COUNT).get(incomeRangeAto)
													.get(age).get(sex).get(taxableStatus).replace(",", ""))
											+ Integer.valueOf(this.atoIndividualTable3a
													.get(ATO_3A_TITLE_DIVIDENDS_FRANKED_COUNT).get(incomeRangeAto)
													.get(age).get(sex).get(taxableStatus).replace(",", ""));
									atoCountAttributeDonations[1] = Integer.valueOf(this.atoIndividualTable3a
											.get(ATO_3A_TITLE_DONATIONS_COUNT).get(incomeRangeAto).get(age).get(sex)
											.get(taxableStatus).replace(",", ""));
									atoCountAttributeRentIncome[1] = Integer.valueOf(this.atoIndividualTable3a
											.get(ATO_3A_TITLE_RENT_INCOME_COUNT).get(incomeRangeAto).get(age).get(sex)
											.get(taxableStatus).replace(",", ""));
									atoCountAttributeRentInterest[1] = Integer.valueOf(this.atoIndividualTable3a
											.get(ATO_3A_TITLE_RENT_INTEREST_COUNT).get(incomeRangeAto).get(age).get(sex)
											.get(taxableStatus).replace(",", ""));
									atoCountAttributeTotalIncome[1] = Integer.valueOf(this.atoIndividualTable3a
											.get(ATO_3A_TITLE_TOTAL_INCOME_COUNT).get(incomeRangeAto).get(age).get(sex)
											.get(taxableStatus).replace(",", ""));
									// SFSS is on top of other loans, so count is max (HELP + TSL, SFSS)
									atoCountAttributeStudentLoan[1] = Math.max(
											Integer.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_HELP_DEBT_COUNT)
													.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)
													.replace(",", ""))
													+ Integer.valueOf(this.atoIndividualTable3a
															.get(ATO_3A_TITLE_TSL_DEBT_COUNT).get(incomeRangeAto)
															.get(age).get(sex).get(taxableStatus).replace(",", "")),
											Integer.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_SFSS_DEBT_COUNT)
													.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)
													.replace(",", "")));
									atoAmountEmployed[1] = Long
											.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_SALARY_AMOUNT)
													.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)
													.replace(",", ""))
											+ Long.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_ALLOWANCES_AMOUNT)
													.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)
													.replace(",", ""));
									atoAmountUnemployed[1] = Long.valueOf(this.atoIndividualTable3a
											.get(ATO_3A_TITLE_GOVT_ALLOW_AMOUNT).get(incomeRangeAto).get(age).get(sex)
											.get(taxableStatus).replace(",", ""));
									atoAmountPension[1] = Long.valueOf(this.atoIndividualTable3a
											.get(ATO_3A_TITLE_GOVT_PENSION_AMOUNT).get(incomeRangeAto).get(age).get(sex)
											.get(taxableStatus).replace(",", ""));
									atoAmountSelfFundedRetiree[1] = Long
											.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_SUPER_TAXED_AMOUNT)
													.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)
													.replace(",", ""))
											+ Long.valueOf(this.atoIndividualTable3a
													.get(ATO_3A_TITLE_SUPER_UNTAXED_AMOUNT).get(incomeRangeAto).get(age)
													.get(sex).get(taxableStatus).replace(",", ""));
									atoAmountForeignIncome[1] = Long
											.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_FOREIGN_INCOME_AMOUNT)
													.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)
													.replace(",", ""))
											+ Long.valueOf(this.atoIndividualTable3a
													.get(ATO_3A_TITLE_FOREIGN_INCOME2_AMOUNT).get(incomeRangeAto)
													.get(age).get(sex).get(taxableStatus).replace(",", ""));
									atoAmountAttributeInterestIncome[1] = Long.valueOf(this.atoIndividualTable3a
											.get(ATO_3A_TITLE_INTEREST_INCOME_AMOUNT).get(incomeRangeAto).get(age)
											.get(sex).get(taxableStatus).replace(",", ""));
									atoAmountAttributeDividendIncome[1] = Long
											.valueOf(this.atoIndividualTable3a
													.get(ATO_3A_TITLE_DIVIDENDS_UNFRANKED_AMOUNT).get(incomeRangeAto)
													.get(age).get(sex).get(taxableStatus).replace(",", ""))
											+ Long.valueOf(this.atoIndividualTable3a
													.get(ATO_3A_TITLE_DIVIDENDS_FRANKED_AMOUNT).get(incomeRangeAto)
													.get(age).get(sex).get(taxableStatus).replace(",", ""));
									atoAmountAttributeDonations[1] = Long.valueOf(this.atoIndividualTable3a
											.get(ATO_3A_TITLE_DONATIONS_AMOUNT).get(incomeRangeAto).get(age).get(sex)
											.get(taxableStatus).replace(",", ""));
									atoAmountAttributeRentIncome[1] = Long.valueOf(this.atoIndividualTable3a
											.get(ATO_3A_TITLE_RENT_INCOME_AMOUNT).get(incomeRangeAto).get(age).get(sex)
											.get(taxableStatus).replace(",", ""));
									atoAmountAttributeRentInterest[1] = Long.valueOf(this.atoIndividualTable3a
											.get(ATO_3A_TITLE_RENT_INTEREST_AMOUNT).get(incomeRangeAto).get(age)
											.get(sex).get(taxableStatus).replace(",", ""));
									atoAmountAttributeTotalIncome[1] = Long.valueOf(this.atoIndividualTable3a
											.get(ATO_3A_TITLE_TOTAL_INCOME_AMOUNT).get(incomeRangeAto).get(age).get(sex)
											.get(taxableStatus).replace(",", ""));
									atoAmountAttributeStudentLoan[1] = Long
											.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_HELP_DEBT_AMOUNT)
													.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)
													.replace(",", ""))
											+ Long.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_TSL_DEBT_AMOUNT)
													.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)
													.replace(",", ""))
											+ Long.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_SFSS_DEBT_AMOUNT)
													.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)
													.replace(",", ""));
								}
							} // end if 2:1 mapping
							if (incomeAtoIdx == 9) {
								// add sums for extra indices where it's a 3:1 mapping
								// 9-11 ==> 9

								// variables to calculate ABS/ATO population multiplier
								incomeRangeAto = INDIVIDUAL_INCOME_RANGES_ATO3A[incomeAtoIdx + 2];

								// check that this category has data first (to avoid null pointer exceptions)
								if (this.atoIndividualTable3a.get(ATO_3A_TITLE_SALARY_COUNT).containsKey(incomeRangeAto)
										&& this.atoIndividualTable3a.get(ATO_3A_TITLE_SALARY_COUNT).get(incomeRangeAto)
												.containsKey(age)
										&& this.atoIndividualTable3a.get(ATO_3A_TITLE_SALARY_COUNT).get(incomeRangeAto)
												.get(age).containsKey(sex)
										&& this.atoIndividualTable3a.get(ATO_3A_TITLE_SALARY_COUNT).get(incomeRangeAto)
												.get(age).get(sex).containsKey(taxableStatus)) {

									employedCount += Math.max(
											Double.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_SALARY_COUNT)
													.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)
													.replace(",", "")),
											Double.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_ALLOWANCES_COUNT)
													.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)
													.replace(",", "")));
									unemployedCount += Double.valueOf(this.atoIndividualTable3a
											.get(ATO_3A_TITLE_GOVT_ALLOW_COUNT).get(incomeRangeAto).get(age).get(sex)
											.get(taxableStatus).replace(",", ""));
									pensionCount += Double.valueOf(this.atoIndividualTable3a
											.get(ATO_3A_TITLE_GOVT_PENSION_COUNT).get(incomeRangeAto).get(age).get(sex)
											.get(taxableStatus).replace(",", ""));
									selfFundedRetireeCount += Math.max(
											Double.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_SUPER_TAXED_COUNT)
													.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)
													.replace(",", "")),
											Double.valueOf(this.atoIndividualTable3a
													.get(ATO_3A_TITLE_SUPER_UNTAXED_COUNT).get(incomeRangeAto).get(age)
													.get(sex).get(taxableStatus).replace(",", "")));
									foreignIncomeCount += Math.max(
											Double.valueOf(this.atoIndividualTable3a
													.get(ATO_3A_TITLE_FOREIGN_INCOME_COUNT).get(incomeRangeAto).get(age)
													.get(sex).get(taxableStatus).replace(",", "")),
											Double.valueOf(this.atoIndividualTable3a
													.get(ATO_3A_TITLE_FOREIGN_INCOME2_COUNT).get(incomeRangeAto)
													.get(age).get(sex).get(taxableStatus).replace(",", "")));
									noIncomeCount += Double.valueOf(this.atoIndividualTable3a
											.get(ATO_3A_TITLE_TOTAL_INCOME_COUNT).get(incomeRangeAto).get(age).get(sex)
											.get(taxableStatus).replace(",", ""));

									// variables to calibrate Individual agents
									atoCountEmployed[2] = Math.max(
											Integer.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_SALARY_COUNT)
													.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)
													.replace(",", "")),
											Integer.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_ALLOWANCES_COUNT)
													.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)
													.replace(",", "")));
									atoCountUnemployed[2] = Integer.valueOf(this.atoIndividualTable3a
											.get(ATO_3A_TITLE_GOVT_ALLOW_COUNT).get(incomeRangeAto).get(age).get(sex)
											.get(taxableStatus).replace(",", ""));
									atoCountPension[2] = Integer.valueOf(this.atoIndividualTable3a
											.get(ATO_3A_TITLE_GOVT_PENSION_COUNT).get(incomeRangeAto).get(age).get(sex)
											.get(taxableStatus).replace(",", ""));
									atoCountSelfFundedRetiree[2] = Math.max(
											Integer.valueOf(this.atoIndividualTable3a
													.get(ATO_3A_TITLE_SUPER_TAXED_COUNT).get(incomeRangeAto).get(age)
													.get(sex).get(taxableStatus).replace(",", "")),
											Integer.valueOf(this.atoIndividualTable3a
													.get(ATO_3A_TITLE_SUPER_UNTAXED_COUNT).get(incomeRangeAto).get(age)
													.get(sex).get(taxableStatus).replace(",", "")));
									atoCountForeignIncome[2] = Math.max(
											Integer.valueOf(this.atoIndividualTable3a
													.get(ATO_3A_TITLE_FOREIGN_INCOME_COUNT).get(incomeRangeAto).get(age)
													.get(sex).get(taxableStatus).replace(",", "")),
											Integer.valueOf(this.atoIndividualTable3a
													.get(ATO_3A_TITLE_FOREIGN_INCOME2_COUNT).get(incomeRangeAto)
													.get(age).get(sex).get(taxableStatus).replace(",", "")));
									atoCountAttributeInterestIncome[2] = Integer.valueOf(this.atoIndividualTable3a
											.get(ATO_3A_TITLE_INTEREST_INCOME_COUNT).get(incomeRangeAto).get(age)
											.get(sex).get(taxableStatus).replace(",", ""));
									// 36% of Australian adults own shares, so sum these counts rather than just
									// taking the maximum
									// SOURCE: ASX (2014), 'The Australian Share Ownership Study': Sydney, NSW.
									atoCountAttributeDividendIncome[2] = Integer
											.valueOf(this.atoIndividualTable3a
													.get(ATO_3A_TITLE_DIVIDENDS_UNFRANKED_COUNT).get(incomeRangeAto)
													.get(age).get(sex).get(taxableStatus).replace(",", ""))
											+ Integer.valueOf(this.atoIndividualTable3a
													.get(ATO_3A_TITLE_DIVIDENDS_FRANKED_COUNT).get(incomeRangeAto)
													.get(age).get(sex).get(taxableStatus).replace(",", ""));
									atoCountAttributeDonations[2] = Integer.valueOf(this.atoIndividualTable3a
											.get(ATO_3A_TITLE_DONATIONS_COUNT).get(incomeRangeAto).get(age).get(sex)
											.get(taxableStatus).replace(",", ""));
									atoCountAttributeRentIncome[2] = Integer.valueOf(this.atoIndividualTable3a
											.get(ATO_3A_TITLE_RENT_INCOME_COUNT).get(incomeRangeAto).get(age).get(sex)
											.get(taxableStatus).replace(",", ""));
									atoCountAttributeRentInterest[2] = Integer.valueOf(this.atoIndividualTable3a
											.get(ATO_3A_TITLE_RENT_INTEREST_COUNT).get(incomeRangeAto).get(age).get(sex)
											.get(taxableStatus).replace(",", ""));
									atoCountAttributeTotalIncome[2] = Integer.valueOf(this.atoIndividualTable3a
											.get(ATO_3A_TITLE_TOTAL_INCOME_COUNT).get(incomeRangeAto).get(age).get(sex)
											.get(taxableStatus).replace(",", ""));
									// SFSS is on top of other loans, so count is max (HELP + TSL, SFSS)
									atoCountAttributeStudentLoan[2] = Math.max(
											Integer.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_HELP_DEBT_COUNT)
													.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)
													.replace(",", ""))
													+ Integer.valueOf(this.atoIndividualTable3a
															.get(ATO_3A_TITLE_TSL_DEBT_COUNT).get(incomeRangeAto)
															.get(age).get(sex).get(taxableStatus).replace(",", "")),
											Integer.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_SFSS_DEBT_COUNT)
													.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)
													.replace(",", "")));
									atoAmountEmployed[2] = Long
											.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_SALARY_AMOUNT)
													.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)
													.replace(",", ""))
											+ Long.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_ALLOWANCES_AMOUNT)
													.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)
													.replace(",", ""));
									atoAmountUnemployed[2] = Long.valueOf(this.atoIndividualTable3a
											.get(ATO_3A_TITLE_GOVT_ALLOW_AMOUNT).get(incomeRangeAto).get(age).get(sex)
											.get(taxableStatus).replace(",", ""));
									atoAmountPension[2] = Long.valueOf(this.atoIndividualTable3a
											.get(ATO_3A_TITLE_GOVT_PENSION_AMOUNT).get(incomeRangeAto).get(age).get(sex)
											.get(taxableStatus).replace(",", ""));
									atoAmountSelfFundedRetiree[2] = Long
											.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_SUPER_TAXED_AMOUNT)
													.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)
													.replace(",", ""))
											+ Long.valueOf(this.atoIndividualTable3a
													.get(ATO_3A_TITLE_SUPER_UNTAXED_AMOUNT).get(incomeRangeAto).get(age)
													.get(sex).get(taxableStatus).replace(",", ""));
									atoAmountForeignIncome[2] = Long
											.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_FOREIGN_INCOME_AMOUNT)
													.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)
													.replace(",", ""))
											+ Long.valueOf(this.atoIndividualTable3a
													.get(ATO_3A_TITLE_FOREIGN_INCOME2_AMOUNT).get(incomeRangeAto)
													.get(age).get(sex).get(taxableStatus).replace(",", ""));
									atoAmountAttributeInterestIncome[2] = Long.valueOf(this.atoIndividualTable3a
											.get(ATO_3A_TITLE_INTEREST_INCOME_AMOUNT).get(incomeRangeAto).get(age)
											.get(sex).get(taxableStatus).replace(",", ""));
									atoAmountAttributeDividendIncome[2] = Long
											.valueOf(this.atoIndividualTable3a
													.get(ATO_3A_TITLE_DIVIDENDS_UNFRANKED_AMOUNT).get(incomeRangeAto)
													.get(age).get(sex).get(taxableStatus).replace(",", ""))
											+ Long.valueOf(this.atoIndividualTable3a
													.get(ATO_3A_TITLE_DIVIDENDS_FRANKED_AMOUNT).get(incomeRangeAto)
													.get(age).get(sex).get(taxableStatus).replace(",", ""));
									atoAmountAttributeDonations[2] = Long.valueOf(this.atoIndividualTable3a
											.get(ATO_3A_TITLE_DONATIONS_AMOUNT).get(incomeRangeAto).get(age).get(sex)
											.get(taxableStatus).replace(",", ""));
									atoAmountAttributeRentIncome[2] = Long.valueOf(this.atoIndividualTable3a
											.get(ATO_3A_TITLE_RENT_INCOME_AMOUNT).get(incomeRangeAto).get(age).get(sex)
											.get(taxableStatus).replace(",", ""));
									atoAmountAttributeRentInterest[2] = Long.valueOf(this.atoIndividualTable3a
											.get(ATO_3A_TITLE_RENT_INTEREST_AMOUNT).get(incomeRangeAto).get(age)
											.get(sex).get(taxableStatus).replace(",", ""));
									atoAmountAttributeTotalIncome[2] = Long.valueOf(this.atoIndividualTable3a
											.get(ATO_3A_TITLE_TOTAL_INCOME_AMOUNT).get(incomeRangeAto).get(age).get(sex)
											.get(taxableStatus).replace(",", ""));
									atoAmountAttributeStudentLoan[2] = Long
											.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_HELP_DEBT_AMOUNT)
													.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)
													.replace(",", ""))
											+ Long.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_TSL_DEBT_AMOUNT)
													.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)
													.replace(",", ""))
											+ Long.valueOf(this.atoIndividualTable3a.get(ATO_3A_TITLE_SFSS_DEBT_AMOUNT)
													.get(incomeRangeAto).get(age).get(sex).get(taxableStatus)
													.replace(",", ""));
								}
							} // end if 3:1 mapping
						} // end for taxable status
						noIncomeCount = noIncomeCount - employedCount - unemployedCount - pensionCount
								- selfFundedRetireeCount - foreignIncomeCount;

						atoCountAttributeOtherIncome[0] = atoCountAttributeTotalIncome[0] - atoCountEmployed[0]
								- atoCountUnemployed[0] - atoCountPension[0] - atoCountSelfFundedRetiree[0]
								- atoCountForeignIncome[0];
						atoCountAttributeOtherIncome[1] = atoCountAttributeTotalIncome[1] - atoCountEmployed[1]
								- atoCountUnemployed[1] - atoCountPension[1] - atoCountSelfFundedRetiree[1]
								- atoCountForeignIncome[1];
						atoCountAttributeOtherIncome[2] = atoCountAttributeTotalIncome[2] - atoCountEmployed[2]
								- atoCountUnemployed[2] - atoCountPension[2] - atoCountSelfFundedRetiree[2]
								- atoCountForeignIncome[2];
						atoAmountAttributeOtherIncome[0] = atoAmountAttributeTotalIncome[0] - atoAmountEmployed[0]
								- atoAmountUnemployed[0] - atoAmountPension[0] - atoAmountSelfFundedRetiree[0]
								- atoAmountForeignIncome[0];
						atoAmountAttributeOtherIncome[1] = atoAmountAttributeTotalIncome[1] - atoAmountEmployed[1]
								- atoAmountUnemployed[1] - atoAmountPension[1] - atoAmountSelfFundedRetiree[1]
								- atoAmountForeignIncome[1];
						atoAmountAttributeOtherIncome[2] = atoAmountAttributeTotalIncome[2] - atoAmountEmployed[2]
								- atoAmountUnemployed[2] - atoAmountPension[2] - atoAmountSelfFundedRetiree[2]
								- atoAmountForeignIncome[2];

						// calculate averages per person for each main income source and P&L line item
						double[] atoPerPersonEmployed = new double[] {
								atoCountEmployed[0] == 0 ? 0d
										: ((double) atoAmountEmployed[0]) / (double) atoCountEmployed[0],
								atoCountEmployed[1] == 0 ? 0d
										: ((double) atoAmountEmployed[1]) / (double) atoCountEmployed[1],
								atoCountEmployed[2] == 0 ? 0d
										: ((double) atoAmountEmployed[2]) / (double) atoCountEmployed[2] };
						double[] atoPerPersonUnemployed = new double[] {
								atoCountUnemployed[0] == 0 ? 0d
										: ((double) atoAmountUnemployed[0]) / (double) atoCountUnemployed[0],
								atoCountUnemployed[1] == 0 ? 0d
										: ((double) atoAmountUnemployed[1]) / (double) atoCountUnemployed[1],
								atoCountUnemployed[2] == 0 ? 0d
										: ((double) atoAmountUnemployed[2]) / (double) atoCountUnemployed[2] };
						double[] atoPerPersonPension = new double[] {
								atoCountPension[0] == 0 ? 0d
										: ((double) atoAmountPension[0]) / (double) atoCountPension[0],
								atoCountPension[1] == 0 ? 0d
										: ((double) atoAmountPension[1]) / (double) atoCountPension[1],
								atoCountPension[2] == 0 ? 0d
										: ((double) atoAmountPension[2]) / (double) atoCountPension[2] };
						double[] atoPerPersonSelfFundedRetiree = new double[] {
								atoCountSelfFundedRetiree[0] == 0 ? 0d
										: ((double) atoAmountSelfFundedRetiree[0])
												/ (double) atoCountSelfFundedRetiree[0],
								atoCountSelfFundedRetiree[1] == 0 ? 0d
										: ((double) atoAmountSelfFundedRetiree[1])
												/ (double) atoCountSelfFundedRetiree[1],
								atoCountSelfFundedRetiree[2] == 0 ? 0d
										: ((double) atoAmountSelfFundedRetiree[2])
												/ (double) atoCountSelfFundedRetiree[2] };
						double[] atoPerPersonForeignIncome = new double[] {
								atoCountForeignIncome[0] == 0 ? 0d
										: ((double) atoAmountForeignIncome[0]) / (double) atoCountForeignIncome[0],
								atoCountForeignIncome[1] == 0 ? 0d
										: ((double) atoAmountForeignIncome[1]) / (double) atoCountForeignIncome[1],
								atoCountForeignIncome[2] == 0 ? 0d
										: ((double) atoAmountForeignIncome[2]) / (double) atoCountForeignIncome[2] };
						double[] atoPerPersonAttributeInterestIncome = new double[] {
								atoCountAttributeInterestIncome[0] == 0 ? 0d
										: ((double) atoAmountAttributeInterestIncome[0])
												/ (double) atoCountAttributeInterestIncome[0],
								atoCountAttributeInterestIncome[1] == 0 ? 0d
										: ((double) atoAmountAttributeInterestIncome[1])
												/ (double) atoCountAttributeInterestIncome[1],
								atoCountAttributeInterestIncome[2] == 0 ? 0d
										: ((double) atoAmountAttributeInterestIncome[2])
												/ (double) atoCountAttributeInterestIncome[2] };
						double[] atoPerPersonAttributeDividendIncome = new double[] {
								atoCountAttributeDividendIncome[0] == 0 ? 0d
										: ((double) atoAmountAttributeDividendIncome[0])
												/ (double) atoCountAttributeDividendIncome[0],
								atoCountAttributeDividendIncome[1] == 0 ? 0d
										: ((double) atoAmountAttributeDividendIncome[1])
												/ (double) atoCountAttributeDividendIncome[1],
								atoCountAttributeDividendIncome[2] == 0 ? 0d
										: ((double) atoAmountAttributeDividendIncome[2])
												/ (double) atoCountAttributeDividendIncome[2] };
						double[] atoPerPersonAttributeDonations = new double[] {
								atoCountAttributeDonations[0] == 0 ? 0d
										: ((double) atoAmountAttributeDonations[0])
												/ (double) atoCountAttributeDonations[0],
								atoCountAttributeDonations[1] == 0 ? 0d
										: ((double) atoAmountAttributeDonations[1])
												/ (double) atoCountAttributeDonations[1],
								atoCountAttributeDonations[2] == 0 ? 0d
										: ((double) atoAmountAttributeDonations[2])
												/ (double) atoCountAttributeDonations[2] };
						double[] atoPerPersonAttributeRentIncome = new double[] {
								atoCountAttributeRentIncome[0] == 0 ? 0d
										: ((double) atoAmountAttributeRentIncome[0])
												/ (double) atoCountAttributeRentIncome[0],
								atoCountAttributeRentIncome[1] == 0 ? 0d
										: ((double) atoAmountAttributeRentIncome[1])
												/ (double) atoCountAttributeRentIncome[1],
								atoCountAttributeRentIncome[2] == 0 ? 0d
										: ((double) atoAmountAttributeRentIncome[2])
												/ (double) atoCountAttributeRentIncome[2] };
						double[] atoPerPersonAttributeRentInterest = new double[] {
								atoCountAttributeRentInterest[0] == 0 ? 0d
										: ((double) atoAmountAttributeRentInterest[0])
												/ (double) atoCountAttributeRentInterest[0],
								atoCountAttributeRentInterest[1] == 0 ? 0d
										: ((double) atoAmountAttributeRentInterest[1])
												/ (double) atoCountAttributeRentInterest[1],
								atoCountAttributeRentInterest[2] == 0 ? 0d
										: ((double) atoAmountAttributeRentInterest[2])
												/ (double) atoCountAttributeRentInterest[2] };
						double[] atoPerPersonAttributeOtherIncome = new double[] {
								atoCountAttributeOtherIncome[0] == 0 ? 0d
										: ((double) atoAmountAttributeOtherIncome[0])
												/ (double) atoCountAttributeOtherIncome[0],
								atoCountAttributeOtherIncome[1] == 0 ? 0d
										: ((double) atoAmountAttributeOtherIncome[1])
												/ (double) atoCountAttributeOtherIncome[1],
								atoCountAttributeOtherIncome[2] == 0 ? 0d
										: ((double) atoAmountAttributeOtherIncome[2])
												/ (double) atoCountAttributeOtherIncome[2] };
						double[] atoPerPersonAttributeTotalIncome = new double[] {
								atoCountAttributeTotalIncome[0] == 0 ? 0d
										: ((double) atoAmountAttributeTotalIncome[0])
												/ (double) atoCountAttributeTotalIncome[0],
								atoCountAttributeTotalIncome[1] == 0 ? 0d
										: ((double) atoAmountAttributeTotalIncome[1])
												/ (double) atoCountAttributeTotalIncome[1],
								atoCountAttributeTotalIncome[2] == 0 ? 0d
										: ((double) atoAmountAttributeTotalIncome[2])
												/ (double) atoCountAttributeTotalIncome[2] };
						double[] atoPerPersonAttributeStudentLoan = new double[] {
								atoCountAttributeStudentLoan[0] == 0 ? 0d
										: ((double) atoAmountAttributeStudentLoan[0])
												/ (double) atoCountAttributeStudentLoan[0],
								atoCountAttributeStudentLoan[1] == 0 ? 0d
										: ((double) atoAmountAttributeStudentLoan[1])
												/ (double) atoCountAttributeStudentLoan[1],
								atoCountAttributeStudentLoan[2] == 0 ? 0d
										: ((double) atoAmountAttributeStudentLoan[2])
												/ (double) atoCountAttributeStudentLoan[2] };

						for (int divIdx = 0; divIdx < NUM_DIVISIONS; divIdx++) {
							String division = DIVISION_CODE_ARRAY[divIdx];
							if (DEBUG) {
								System.out.println("         division: " + division);
							}
							// for each industry, multiply by industry count ratio
							double divCountMult = divisionCountMultiplier.get(division);
							double divAmtMult = divisionTaxableIncomeMultiplier.get(division);
							for (String poa : poaSetIntersection) {
								if (DEBUG) {
									//System.out.println("            poa: " + poa);
								}
								int poaIdx = poaIndexMap.get(poa);
								String state = this.area.getStateFromPoa(poa);
								if (!state.equals("Other")) { // skip "Other" to solve mapping issues
									// for each state, sex & age, multiply by state count ratio
									double stateCountMult = stateCountMultiplier.get(sex).get(age).get(state);
									double stateAmtMult = stateTaxableIncomeMultiplier.get(sex).get(age).get(state);

									// get ATO taxable count by income, age, sex, industry, poa (same as ABS)
									// for each poa, multiply by poa count ratio
									double poaCountMult = postcodeStateCountMultiplier.get(state).get(poa);
									double poaAmtMult = postcodeStateTaxableIncomeMultiplier.get(state).get(poa);
									double totalAtoCount = (employedCount + unemployedCount + pensionCount
											+ selfFundedRetireeCount + foreignIncomeCount)/* + noIncomeCount) */
											* divCountMult * stateCountMult * poaCountMult;

									// need to take into account 1:n and n:1 mappings between ATO and ABS
									int totalAbsCount = 0;
									for (int ageIdxAbs : ageIndicesAbs) {
										// doesn't need to exclude young children because it already filters on income
										// range
										for (int incomeIdxAbs : incomeIndicesAbs) {
											totalAbsCount += censusMatrixPersonsAdjustedPOA[poaIdx][sexIdx][ageIdxAbs][divIdx][incomeIdxAbs];
										}
									}

									// divide ABS count by ATO count to get multiplier, and apply to counts
									atoAbsPopMult = totalAbsCount / totalAtoCount;
									double countMultiplier = divCountMult * stateCountMult * poaCountMult
											* atoAbsPopMult;
									double amountMultiplier = divAmtMult * stateAmtMult * poaAmtMult;

									for (int idx = 0; idx < 3; idx++) {
										// multiply ATO 3A counts to gross up to ABS figures
										atoCountEmployed[idx] = (int) Math
												.round(((double) atoCountEmployed[idx]) * countMultiplier);
										atoCountUnemployed[idx] = (int) Math
												.round(((double) atoCountUnemployed[idx]) * countMultiplier);
										atoCountPension[idx] = (int) Math
												.round(((double) atoCountPension[idx]) * countMultiplier);
										atoCountSelfFundedRetiree[idx] = (int) Math
												.round(((double) atoCountSelfFundedRetiree[idx]) * countMultiplier);
										atoCountForeignIncome[idx] = (int) Math
												.round(((double) atoCountForeignIncome[idx]) * countMultiplier);
										atoCountAttributeInterestIncome[idx] = (int) Math.round(
												((double) atoCountAttributeInterestIncome[idx]) * countMultiplier);
										atoCountAttributeDividendIncome[idx] = (int) Math.round(
												((double) atoCountAttributeDividendIncome[idx]) * countMultiplier);
										atoCountAttributeDonations[idx] = (int) Math
												.round(((double) atoCountAttributeDonations[idx]) * countMultiplier);
										atoCountAttributeRentIncome[idx] = (int) Math
												.round(((double) atoCountAttributeRentIncome[idx]) * countMultiplier);
										atoCountAttributeRentInterest[idx] = (int) Math
												.round(((double) atoCountAttributeRentInterest[idx]) * countMultiplier);
										atoCountAttributeOtherIncome[idx] = (int) Math
												.round(((double) atoCountAttributeOtherIncome[idx]) * countMultiplier);
										atoCountAttributeTotalIncome[idx] = (int) Math
												.round(((double) atoCountAttributeTotalIncome[idx]) * countMultiplier);
										atoCountAttributeStudentLoan[idx] = (int) Math
												.round(((double) atoCountAttributeStudentLoan[idx]) * countMultiplier);

										// multiply ATO 3A averages per person to calibrate them
										atoPerPersonEmployed[idx] *= amountMultiplier;
										atoPerPersonUnemployed[idx] *= amountMultiplier;
										atoPerPersonPension[idx] *= amountMultiplier;
										atoPerPersonSelfFundedRetiree[idx] *= amountMultiplier;
										atoPerPersonForeignIncome[idx] *= amountMultiplier;
										atoPerPersonAttributeInterestIncome[idx] *= amountMultiplier;
										atoPerPersonAttributeDividendIncome[idx] *= amountMultiplier;
										atoPerPersonAttributeDonations[idx] *= amountMultiplier;
										atoPerPersonAttributeRentIncome[idx] *= amountMultiplier;
										atoPerPersonAttributeRentInterest[idx] *= amountMultiplier;
										atoPerPersonAttributeOtherIncome[idx] *= amountMultiplier;
										atoPerPersonAttributeTotalIncome[idx] *= amountMultiplier;
										atoPerPersonAttributeStudentLoan[idx] *= amountMultiplier;
									}

									/*
									 * Need to calibrate the right number of agents with interest, dividend,
									 * donation, rent, student loans, but want to do it randomly. Could make arrays
									 * of booleans populated with as many true values as there are adjusted people
									 * with that attribute, then use Collections.shuffle(List<T> list, Random rnd)
									 * to shuffle the list into a pseudo-random order so the combinations at each
									 * index are more heterogeneous.
									 */

									// create boolean lists for attributes and shuffle them
									int subListLength = 0;
									int flagListLength = atoCountEmployed[0] + atoCountUnemployed[0]
											+ atoCountPension[0] + atoCountSelfFundedRetiree[0]
											+ atoCountForeignIncome[0];
									List<Integer> employedFlags = Collections
											.nCopies(atoCountEmployed[0] > 0 ? atoCountEmployed[0] : 0, 0);
									List<Integer> unemployedFlags = Collections
											.nCopies(atoCountUnemployed[0] > 0 ? atoCountUnemployed[0] : 0, 1);
									List<Integer> pensionFlags = Collections
											.nCopies(atoCountPension[0] > 0 ? atoCountPension[0] : 0, 2);
									List<Integer> selfFundedRetireeFlags = Collections.nCopies(
											atoCountSelfFundedRetiree[0] > 0 ? atoCountSelfFundedRetiree[0] : 0, 3);
									List<Integer> foreignIncomeFlags = Collections
											.nCopies(atoCountForeignIncome[0] > 0 ? atoCountForeignIncome[0] : 0, 4);
									List<Integer> mainIncomeSourceFlags = new ArrayList<Integer>(flagListLength);
									mainIncomeSourceFlags.addAll(employedFlags);
									mainIncomeSourceFlags.addAll(unemployedFlags);
									mainIncomeSourceFlags.addAll(pensionFlags);
									mainIncomeSourceFlags.addAll(selfFundedRetireeFlags);
									mainIncomeSourceFlags.addAll(foreignIncomeFlags);
									Collections.shuffle(mainIncomeSourceFlags, this.random);

									subListLength = atoCountAttributeInterestIncome[0] > 0
											? atoCountAttributeInterestIncome[0]
											: 0;
									List<Boolean> trueFlags = Collections.nCopies(subListLength, true);
									List<Boolean> falseFlags = Collections.nCopies(flagListLength - subListLength,
											false);
									List<Boolean> flagAttributeInterestIncome = Stream
											.concat(trueFlags.stream(), falseFlags.stream())
											.collect(Collectors.toList());
									Collections.shuffle(flagAttributeInterestIncome, this.random);

									subListLength = atoCountAttributeDividendIncome[0] > 0
											? atoCountAttributeDividendIncome[0]
											: 0;
									trueFlags = Collections.nCopies(subListLength, true);
									falseFlags = Collections.nCopies(flagListLength - subListLength, false);
									List<Boolean> flagAttributeDividendIncome = Stream
											.concat(trueFlags.stream(), falseFlags.stream())
											.collect(Collectors.toList());
									Collections.shuffle(flagAttributeDividendIncome, this.random);

									subListLength = atoCountAttributeDonations[0] > 0 ? atoCountAttributeDonations[0]
											: 0;
									trueFlags = Collections.nCopies(subListLength, true);
									falseFlags = Collections.nCopies(flagListLength - subListLength, false);
									List<Boolean> flagAttributeDonations = Stream
											.concat(trueFlags.stream(), falseFlags.stream())
											.collect(Collectors.toList());
									Collections.shuffle(flagAttributeDonations, this.random);

									subListLength = atoCountAttributeOtherIncome[0] > 0
											? atoCountAttributeOtherIncome[0]
											: 0;
									trueFlags = Collections.nCopies(subListLength, true);
									falseFlags = Collections.nCopies(flagListLength - subListLength, false);
									List<Boolean> flagAttributeOtherIncome = Stream
											.concat(trueFlags.stream(), falseFlags.stream())
											.collect(Collectors.toList());
									Collections.shuffle(flagAttributeOtherIncome, this.random);

									subListLength = atoCountAttributeStudentLoan[0] > 0
											? atoCountAttributeStudentLoan[0]
											: 0;
									trueFlags = Collections.nCopies(subListLength, true);
									falseFlags = Collections.nCopies(flagListLength - subListLength, false);
									List<Boolean> flagAttributeStudentLoan = Stream
											.concat(trueFlags.stream(), falseFlags.stream())
											.collect(Collectors.toList());
									Collections.shuffle(flagAttributeStudentLoan, this.random);

									// use integer list for rent: 0=no rent, 1=rent with loan, 2=rent no loan
									subListLength = Math.max(
											Math.max(atoCountAttributeRentIncome[0], atoCountAttributeRentInterest[0]),
											0);
									List<Integer> rentIncomeFlags = Collections.nCopies(subListLength, 1);
									List<Integer> rentInterestFlags = new ArrayList<Integer>(
											atoCountAttributeRentInterest[0] > 0 ? atoCountAttributeRentInterest[0]
													: 0);
									rentInterestFlags.addAll(Collections.nCopies(atoCountAttributeRentInterest[0], 1));
									List<Integer> noRentalInterestFlags = Collections.nCopies(subListLength
											- (atoCountAttributeRentInterest[0] > 0 ? atoCountAttributeRentInterest[0]
													: 0),
											0);
									rentInterestFlags.addAll(noRentalInterestFlags);
									// assign rental interest randomly to rental income (theoretically necessary)
									Collections.shuffle(rentInterestFlags, this.random);
									List<Integer> rentFlags = new ArrayList<Integer>(rentIncomeFlags.size());
									for (int rentIdx = 0; rentIdx < rentIncomeFlags.size(); rentIdx++) {
										// sum each element so 1 = rent income only, 2 = rent income and interest
										rentFlags.add(rentIncomeFlags.get(rentIdx) + rentInterestFlags.get(rentIdx));
									}
									List<Integer> noRentFlags = Collections.nCopies(flagListLength - rentFlags.size(),
											0);
									List<Integer> flagAttributeRentIncomeInterest = Stream
											.concat(rentFlags.stream(), noRentFlags.stream())
											.collect(Collectors.toList());
									Collections.shuffle(flagAttributeRentIncomeInterest, this.random);

									// make flags for the age indices too (this solves the 1:n mappings)
									List<Integer> ageIndicesAgent = new ArrayList<Integer>(flagListLength);
									List<Integer> incomeIndicesAgent = new ArrayList<Integer>(flagListLength);
									for (int ageIdxAbs : ageIndicesAbs) {
										// doesn't need to exclude young children because it already filters on income
										// range
										for (int incomeIdxAbs : incomeIndicesAbs) {
											int numFlags = Math.max(0,
													censusMatrixPersonsAdjustedPOA[poaIdx][sexIdx][ageIdxAbs][divIdx][incomeIdxAbs]);
											List<Integer> tmpAgeFlags = Collections.nCopies(numFlags, ageIdxAbs);
											List<Integer> tmpIncomeFlags = Collections.nCopies(numFlags, ageIdxAbs);
											ageIndicesAgent.addAll(tmpAgeFlags);
											incomeIndicesAgent.addAll(tmpIncomeFlags);
										}
									}
									// ensure the index lists are fully populated
									if (ageIndicesAgent.size() < flagListLength) {
										int numFlags = flagListLength - ageIndicesAgent.size();
										List<Integer> tmpAgeFlags = Collections.nCopies(numFlags,
												ageIndicesAbs.get(ageIndicesAbs.size() - 1));
										ageIndicesAgent.addAll(tmpAgeFlags);
									}
									if (incomeIndicesAgent.size() < flagListLength) {
										int numFlags = flagListLength - incomeIndicesAgent.size();
										List<Integer> tmpIncomeFlags = Collections.nCopies(numFlags,
												incomeIndicesAbs.get(incomeIndicesAbs.size() - 1));
										incomeIndicesAgent.addAll(tmpIncomeFlags);
									}
									Collections.shuffle(ageIndicesAgent, this.random);
									Collections.shuffle(incomeIndicesAgent, this.random);

									// matrix Keys: postcode, sex, age, industry division, income (ABS categories)
									// this.individualMatrix = new
									// ArrayList<List<List<List<List<List<Individual>>>>>>(poaSetIntersection.size());

									// TODO: create Individual agents and store in a List matrix
									for (int agentIdx = 0; agentIdx < flagListLength; agentIdx++) {
										Individual individual = new Individual();
										individual.setAge(ageIndicesAgent.get(agentIdx));
										individual.setSex(SEX_ARRAY[sexIdx]);
										individual.setEmploymentIndustry(DIVISION_CODE_ARRAY[divIdx]);
										individual.setLocalGovernmentAreaCode(this.area.getLgaCodeFromPoa(poa));

										switch (mainIncomeSourceFlags.get(agentIdx)) {
										case 0:
											individual.setMainIncomeSource(0); // employed
											individual.setPnlWagesSalaries(atoPerPersonEmployed[0] / NUM_MONTHS);
											break;
										case 1:
											individual.setMainIncomeSource(1); // unemployed
											individual
													.setPnlUnemploymentBenefits(atoPerPersonUnemployed[0] / NUM_MONTHS);
											break;
										case 2:
											individual.setMainIncomeSource(2); // pension
											individual.setPnlOtherSocialSecurityIncome(
													atoPerPersonPension[0] / NUM_MONTHS);
											break;
										case 3:
											individual.setMainIncomeSource(3); // self-funded retiree
											individual.setPnlInvestmentIncome(
													atoPerPersonSelfFundedRetiree[0] / NUM_MONTHS);
											break;
										default:
											individual.setMainIncomeSource(4); // foreign income
											individual.setPnlForeignIncome(atoPerPersonForeignIncome[0] / NUM_MONTHS);
											break;
										}

										if (flagAttributeInterestIncome.get(agentIdx)) {
											individual.setPnlInterestIncome(
													atoPerPersonAttributeInterestIncome[0] / NUM_MONTHS);
										}
										if (flagAttributeDividendIncome.get(agentIdx)) {
											individual.setPnlInvestmentIncome(individual.getPnlInvestmentIncome()
													+ atoPerPersonAttributeDividendIncome[0] / NUM_MONTHS);
										}
										if (flagAttributeDonations.get(agentIdx)) {
											individual.setPnlDonations(atoPerPersonAttributeDonations[0] / NUM_MONTHS);
										}
										if (flagAttributeRentIncomeInterest.get(agentIdx) > 0) {
											individual
													.setPnlRentIncome(atoPerPersonAttributeRentIncome[0] / NUM_MONTHS);
										}
										if (flagAttributeRentIncomeInterest.get(agentIdx) > 1) {
											individual.setPnlRentInterestExpense(
													atoPerPersonAttributeRentInterest[0] / NUM_MONTHS);
										}
										if (flagAttributeOtherIncome.get(agentIdx)) {
											individual.setPnlOtherIncome(
													atoPerPersonAttributeOtherIncome[0] / NUM_MONTHS);
										}

										// adjust other income if the sum of the income lines is less than total income
										if (individual.getGrossIncome() < atoPerPersonAttributeTotalIncome[0]) {
											double newOtherIncome = individual.getPnlOtherIncome()
													+ (atoPerPersonAttributeTotalIncome[0]
															- individual.getGrossIncome());
											individual.setPnlOtherIncome(newOtherIncome);
										}

										// Bal Sht
										if (flagAttributeStudentLoan.get(agentIdx)) {
											individual.setBsStudentLoans(atoPerPersonAttributeStudentLoan[0]);
										}

										// add Individual to matrix (for household calibration) using ABS indices
										this.individualMatrix.get(poaIdx).get(sexIdx).get(ageIndicesAgent.get(agentIdx))
												.get(divIdx).get(incomeIndicesAgent.get(agentIdx)).add(individual);

										// add Individual to list (for payment clearing algorithm)
										this.individualAgents.add(individual);
									}

									// N.B. Need to deal with 1:n and n:1 index mapping here too - make sure we
									// calibrate those cells too.
									/*
									 * if (incomeAtoIdx == 0 || incomeAtoIdx == 4 || incomeAtoIdx == 7 ||
									 * incomeAtoIdx == 9 || incomeAtoIdx == 13) { // calculate for extra indices
									 * where it's a 2:1 mapping // 0-1 ==> 2, 3 ==> 5-4, 4-5 ==> 6, 7-8 ==> 8, 9-11
									 * ==> 9, 13-14 ==> 11
									 * 
									 * } // end if 2:1 mapping if (incomeAtoIdx == 9) { // calculate for extra
									 * indices where it's a 3:1 mapping // 9-11 ==> 9
									 * 
									 * } // end if 3:1 mapping
									 */
								} // end if !state.equals("Other")
							} // end for POA
//							} // end for state
						} // end for division
					} // end for sex
				} // end for age
			} // end if multi-index income range mapping
		} // end for ATO income range
		this.individualAgents.trimToSize();
		System.out.println(new Date(System.currentTimeMillis()) + ": Finished creating Individual agents");

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

		// FIXME: 3. create Individual agents

		// when reading from the ATO matrix, use modulo to ensure we don't read off the
		// end of the List. It's possible that due to rounding there are more people in
		// the ABS data than the ATO data in a given cell, so just loop back and keep
		// going from the beginning of the List. Will need to make a copy of the
		// Individual on subsequent iterations over the list so we don't end up with
		// multiple pointers to the same person.

		/*
		 * ------------------------------------------------------------------------<br>
		 * PART E: ADJUSTING FINANCIALS FOR HOUSEHOLDS
		 * ------------------------------------------------------------------------<br>
		 * 
		 * D1. RBA E2: Use the household debt-to-income and assets-to-income ratios to
		 * calculate total assets and total debt.<br>
		 * 
		 * N.B. Household, not individual.
		 */
		// RBA E2 Keys: Series Name, Date
		double debtToIncomeRatioRbaE2 = Double
				.valueOf(this.rbaE2.get(RBA_E2_SERIESID_DEBT_TO_INCOME).get(this.calibrationDateRba)) * PERCENT;
		double assetsToIncomeRatioRbaE2 = Double
				.valueOf(this.rbaE2.get(RBA_E2_SERIESID_ASSETS_TO_INCOME).get(this.calibrationDateRba)) * PERCENT;

		/*
		 * D2. RBA E1: Calculate the ratios between Bal Sht items. Use these, compared
		 * to assets and debt, to estimate the other balance sheet items.<br>
		 * 
		 * N.B. Household, not individual.
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
			indices = Arrays.asList(new Integer[] { 10, 9, 11 });
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
			indices = Arrays.asList(new Integer[] { 2, 0, 1 });
		} else if (atoIncomeIndex == 2) {
			// $10-18k ==> $8-16k
			indices = Arrays.asList(new Integer[] { 3 });
		} else if (atoIncomeIndex == 3) {
			// $18-25k ==> $16-20k, $20-26k
			indices = Arrays.asList(new Integer[] { 5, 4 });
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
	 * @param data the data to set
	 */
	@Autowired
	public void setCommonData(CalibrationData commonData) {
		this.commonData = commonData;
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

	/**
	 * @param properties the properties to set
	 */
	@Autowired
	public void setProperties(Properties properties) {
		this.properties = properties;
		this.random = this.properties.getRandom();
	}

}
