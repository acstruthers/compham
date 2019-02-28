/**
 * 
 */
package xyz.struthers.rhul.ham.data;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
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
	private static final double EPSILON = 0.1d; // to round business counts so the integer sums match

	// beans
	private AreaMapping area;
	private CalibrationData commonData;
	private CalibrationDataIndividual individualData;
	private AustralianEconomy economy;

	// field variables
	private List<Individual> individualAgents;
	private Date calibrationDate;
	private int totalPopulationAU;
	private Map<String, Integer> lgaPeopleCount;
	private Map<String, Integer> lgaDwellingsCount;

	// A single Bal Sht using national-level data
	// DEPRECATED ???
	private double bsAUBankDeposits;
	private double bsAUOtherFinancialAssets;
	private double bsAUResidentialLandAndDwellings;
	private double bsAUOtherNonFinancialAssets;

	private double bsAULoans;
	private double bsAUOtherLiabilities;

	// data sets
	/**
	 * Data by LGA: Economy<br>
	 * Contains property prices and counts per LGA<br>
	 * Keys: Year, LGA, data series
	 */
	private Map<String, Map<String, Map<String, String>>> abs1410_0Economy;
	/**
	 * ATO Individuals Table 2A<br>
	 * Contains P&L and people count by sex and 5-year age range.<br>
	 * Keys: Series Title, Age Range, Sex
	 */
	private Map<String, Map<String, String>> atoIndividualTable2a;
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
	private Map<String, Map<String, String>> atoIndividualTable6c;
	/**
	 * ATO Individuals Table 9<br>
	 * Contains P&L by industry code.<br>
	 * Keys: Series Title, Industry Code
	 */
	private Map<String, Map<String, String>> atoIndividualTable9;
	/**
	 * RBA E1 Household and Business Balance Sheets<br>
	 * Contains high-level balance sheet amounts at a national level.<br>
	 * Keys: Series Name, Date
	 */
	private Map<String, Map<Date, String>> rbaE1;
	/**
	 * RBA E2 Selected Ratios<br>
	 * Contains ratios that link P&L and Bal Sht.<br>
	 * Keys: Series Name, Date
	 */
	private Map<String, Map<Date, String>> rbaE2;
	/**
	 * ABS Census Table Builder data:<br>
	 * SEXP by LGA (UR) by AGE5P, INDP and INCP<br>
	 * Individual income by industry and demographic.
	 * 
	 * Keys: Age5, Industry Division, Personal Income, LGA, Sex<br>
	 * Values: Number of persons
	 */
	private Map<String, Map<String, Map<String, Map<String, Map<String, String>>>>> censusSEXP_LGA_AGE5P_INDP_INCP;
	/**
	 * ABS Census Table Builder data:<br>
	 * HCFMD and TEND by LGA by HIND and RNTRD<br>
	 * Rent by tenure, household income and composition.
	 * 
	 * Keys: Household Income, Rent Range, LGA, Household Composition Dwelling,
	 * Tenure<br>
	 * Values: Number of dwellings
	 */
	private Map<String, Map<String, Map<String, Map<String, Map<String, String>>>>> censusHCFMD_TEND_LGA_HIND_RNTRD;
	/**
	 * ABS Census Table Builder data:<br>
	 * HCFMD and TEND by LGA by HIND and MRERD<br>
	 * Mortgage payments by tenure, household income and composition.
	 * 
	 * Keys: Household Income, Rent Range, LGA, Household Composition Dwelling,
	 * Tenure<br>
	 * Values: Number of dwellings
	 */
	private Map<String, Map<String, Map<String, Map<String, Map<String, String>>>>> censusHCFMD_TEND_LGA_HIND_MRERD;
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
		this.calibrationDate = null;
		this.totalPopulationAU = 0;
		this.lgaPeopleCount = null;
		this.lgaDwellingsCount = null;

		this.bsAUBankDeposits = 0d;
		this.bsAUOtherFinancialAssets = 0d;
		this.bsAUResidentialLandAndDwellings = 0d;
		this.bsAUOtherNonFinancialAssets = 0d;

		this.bsAULoans = 0d;
		this.bsAUOtherLiabilities = 0d;

		// data sources
		this.abs1410_0Economy = null;
	}

	/**
	 * A destructor to free up the resources used by this class.
	 */
	public void close() {
		this.init();
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
	 * PART A: DETERMINING THE NUMBER AND TYPE OF INDIVIDUALS IN EACH LGA
	 * ------------------------------------------------------------------------<br>
	 * 
	 * 1. ABS 3222.0: Get the sum of the 2018 population projections. This will be
	 * the baseline we adjust the rest of the data against.
	 * 
	 * 2. ABS 2074.0: Sum the total number of dwellings and individuals for
	 * Australia. Divide the total by the population projections from ABS 3222.0 to
	 * get a multiplier that adjusts all LGA population and dwelling counts forward
	 * from 2016 to 2018. Determine the number of dwellings and individuals per LGA,
	 * and apply the population multiplier to each LGA to get the adjusted number of
	 * persons and dwellings.
	 * 
	 * Use this as the list of LGAs to iterate over.
	 * 
	 * 
	 * 
	 * ------------------------------------------------------------------------<br>
	 * PART B: DETERMINING THE NUMBER AND TYPE OF HOUSEHOLDS IN EACH LGA
	 * ------------------------------------------------------------------------<br>
	 * 
	 * - ATO 6C: income range ratios and multipliers (by POA, within each LGA)<br>
	 * N.B. I can probably omit ATO 6C and use ATO 2A instead.
	 * 
	 * TODO: Consider getting census data by POA, then just assigning to LGA later.
	 * This will make it easier to match with POA data from ATO. Probably don't need
	 * the TEND dimension - they're either renting, buying, or own outright.
	 * 
	 * START WITH COUNT, THEN ASSIGN AS WE GO BASED ON RATIOS. FOR EXAMPLE, WE
	 * SHOULD ASSIGN THE RIGHT NUMBER OF HELP DEBT - NOT JUST AN AVERAGE TO
	 * EVERYONE.<br>
	 * MAYBE PICK ONE CELL FROM THE CENSUS INDIVIDUAL DATA TABLE AND MOCK IT UP IN
	 * EXCEL TO HELP FIGURE OUT THE ALGORITHM?
	 * 
	 * ROUGH ALGORITHM FOR INDIVIDUAL COUNTS:<br>
	 * 1. Census age, industry, income & sex per LGA: Calculate ratios<br>
	 * 1.(a) For each age, sex & state, work out the ratio of people in each income
	 * range. We will use this to cross-multiply ATO 2A with ATO 3A.<br>
	 * - Multiply ratios by total people per LGA to give an adjusted count.<br>
	 * - Use ratios within categories to attribute the amounts from ATO data to make
	 * a more realistic matrix.
	 * 
	 * ROUGH ALGORITHM FOR AMOUNTS:<br>
	 * - ATO 9: P&L (by industry code). Only 1.2m of 13.5m taxpayers specified an
	 * industry, so just use the industry data to derive ratios of taxable income to
	 * multiply the the other data by. Make it a multiple of the industry-declaring
	 * national average so we can just multiply the other cells by this multiple
	 * rather than trying to split them. Don't use the total amounts or the ratios
	 * between line items as it's too small a sample. So, the algorithm becomes:
	 * Calculate mean taxable income per industry code. Calculate mean taxable
	 * income across all industry codes. Divide each industry code's mean by the
	 * overall mean to produce an industry multiplier.<br>
	 * 
	 * - ATO 2A: age/sex count, P&L, Help Debt (by State)???<br>
	 * - ATO 3A: age/sex count, P&L, Help Debt (by income range)???<br>
	 * - ATO 6B: POA count, P&L (by POA, within each LGA)???<br>
	 * 
	 * - RBA E2: ratios between P&L and Bal Sht. Use these to calculate total assets
	 * and total debt.<br>
	 * - RBA E1: ratios between Bal Sht items. Use these, compared to assets and
	 * debt, to estimate the other balance sheet items.<br>
	 * 
	 * N.B. Need to think about HELP debt and work out how to assign it by age &
	 * gender, not diminish it by including it in ratios too early.
	 * 
	 * This gives P&L and Bal Sht by age, gender, industry, POA
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
	 * ROUGH ALGORITHM TO ASSIGN PEOPLE TO HOUSEHOLDS:
	 * 
	 */
	public void createIndividualAgents() {
		DateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);
		try {
			this.calibrationDate = sdf.parse("01/06/2018");
		} catch (ParseException e) {
			e.printStackTrace();
		}
		this.abs1410_0Economy = this.individualData.getAbs1410_0Economy();
		this.atoIndividualTable6b = this.individualData.getAtoIndividualTable6b();
		this.atoIndividualTable6c = this.individualData.getAtoIndividualTable6c();
		this.atoIndividualTable9 = this.individualData.getAtoIndividualTable9();
		this.rbaE1 = this.commonData.getRbaE1();
		this.rbaE2 = this.individualData.getRbaE2();
		this.censusSEXP_LGA_AGE5P_INDP_INCP = this.individualData.getCensusSEXP_LGA_AGE5P_INDP_INCP();
		this.censusHCFMD_TEND_LGA_HIND_RNTRD = this.individualData.getCensusHCFMD_TEND_LGA_HIND_RNTRD();
		this.censusHCFMD_TEND_LGA_HIND_MRERD = this.individualData.getCensusHCFMD_TEND_LGA_HIND_MRERD();
		this.censusCDCF_LGA_FINF = this.individualData.getCensusCDCF_LGA_FINF();

		/*
		 * ------------------------------------------------------------------------<br>
		 * PART A: DETERMINING THE NUMBER AND TYPE OF INDIVIDUALS IN EACH LGA
		 * ------------------------------------------------------------------------<br>
		 * 
		 * 1. ABS 3222.0: Get the sum of the 2018 population projections. This will be
		 * the baseline we adjust the rest of the data against.
		 * 
		 * 2. ABS 2074.0: Sum the total number of dwellings and individuals for
		 * Australia. Divide the total by the population projections from ABS 3222.0 to
		 * get a multiplier that adjusts all LGA population and dwelling counts forward
		 * from 2016 to 2018. Determine the number of dwellings and individuals per LGA,
		 * and apply the population multiplier to each LGA to get the adjusted number of
		 * persons and dwellings. N.B. This is already done in the AreaMapping class.
		 */

		this.lgaPeopleCount = this.area.getAdjustedPeopleByLga(this.calibrationDate);
		this.lgaDwellingsCount = this.area.getAdjustedDwellingsByLga(this.calibrationDate);
		this.totalPopulationAU = this.area.getTotalPopulation(this.calibrationDate);
		Set<String> lgaCodes = this.lgaPeopleCount.keySet();

		// FIXME: implement me
		for (String lgaCode : lgaCodes) {

		}

		this.addAgentsToEconomy();
	}

	private void addAgentsToEconomy() {
		this.economy.setIndividuals(this.individualAgents);
	}

	/**
	 * 
	 * @param date                   - the date for which data should be used to
	 *                               calibrate the individuals
	 * @param numberOfPeoplePerAgent - the number of people represented by each
	 *                               agent in the model
	 */
	public void calibrateIndividualFinancials(Date date) {
		// TODO: implement me

		// calibrate individual P&Ls
		// 1. For each LGA
		// 2. Calculate number of people in each "economic segment", interpolating into
		// groups of size P
		// 3. For each segment, calibrate the P&L of a representative individual.
		// Calculate tax paid by the individual, then later aggregate it to households.
		this.calibrateAllIndividualsProfitAndLoss(date);

		// calibrate Bal Sht using national-level data, then pro-rata for each
		// individual using the ratios

	}

	/**
	 * 1. For each LGA<br>
	 * 2. Calculate number of people in each "economic segment", interpolating into
	 * groups of size P<br>
	 * 3. For each segment, calibrate the P&L of a representative individual.
	 * Calculate tax paid by the individual, then later aggregate it to households.
	 */
	private void calibrateAllIndividualsProfitAndLoss(Date date) {
		Map<String, Integer> peopleByLga = this.area.getCensusPeopleByLga();
		Set<String> lgaCodeSet = peopleByLga.keySet();
		for (String lgaCode : lgaCodeSet) {
			// Map<String, List<Double>> lgaByIncp = this.segmentLgaByIncp(lgaCode,
			// "2015-16", date); // from, to,
			// meanIncome,
			// adjustedPeople

		}

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
