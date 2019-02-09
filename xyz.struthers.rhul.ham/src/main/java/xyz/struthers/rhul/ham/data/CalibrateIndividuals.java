/**
 * 
 */
package xyz.struthers.rhul.ham.data;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

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
public class CalibrateIndividuals {

	// individuals should have an LGA
	private CalibrationData data;
	private AreaMapping area;
	private AustralianEconomy economy;
	private int peoplePerAgent;

	private List<Individual> individualAgents;

	// A single Bal Sht using national-level data
	private double bsAUBankDeposits;
	private double bsAUOtherFinancialAssets;
	private double bsAUResidentialLandAndDwellings;
	private double bsAUOtherNonFinancialAssets;

	private double bsAULoans;
	private double bsAUOtherLiabilities;

	/**
	 * 
	 */
	public CalibrateIndividuals() {
		super();
		this.init();
	}

	/**
	 * Calibrates individual financials, and works out how many of each to create,
	 * then adds them to the economy.
	 */
	public void createIndividualAgents() {
		// TODO: implement me

		this.addAgentsToEconomy();
	}

	private void addAgentsToEconomy() {
		this.economy.setIndividuals(this.individualAgents);
	}

	/**
	 * 
	 * @param date
	 *            - the date for which data should be used to calibrate the
	 *            individuals
	 * @param numberOfPeoplePerAgent
	 *            - the number of people represented by each agent in the model
	 */
	public void calibrateIndividualFinancials(Date date, int numberOfPeoplePerAgent) {
		// TODO: implement me
		this.peoplePerAgent = numberOfPeoplePerAgent;

		// calibrate individual P&Ls
		// 1. For each LGA
		// 2. Calculate number of people in each "economic segment", interpolating into
		// groups of size P
		// 3. For each segment, calibrate the P&L of a representative individual.
		// Calculate tax paid by the individual, then later aggregate it to households.
		this.calibrateAllIndividualsProfitAndLoss(date);

		// calibrate Bal Sht using national-level data, then pro-rata for each
		// individual using the ratios
		this.calibrateNationalBalSht();
		this.assignProRataBalShtToAllIndividuals();
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
			Map<String, List<Double>> lgaByIncp = this.segmentLgaByIncp(lgaCode, "2015-16", date); // from, to,
																									// meanIncome,
																									// adjustedPeople

			// TODO do other things here

			// TODO: shouldn't I be breaking this down into agents of size P here?
		}

	}

	private Map<String, List<Double>> segmentLgaByIncp(String lgaCode, String financialYear, Date date) {
		// local constants
		final int numBins = 14;

		// local variables
		// String[] title = { "$0", "$3900", "$11700", "$18200", "$23400", "$29900",
		// "$37700", "$46800", "$58500",
		// "$71500", "$84500", "$97500", "$130000", "$156000+" };
		Double[] from = new Double[] { 0d, 1d, 7800d, 15600d, 20800d, 26000d, 33800d, 41600d, 52000d, 65000d, 78000d,
				91000d, 104000d, 156000d };
		Double[] to = new Double[] { 0d, 7799d, 15599d, 20799d, 25999d, 33799d, 41599d, 51999d, 64999d, 77999d, 90999d,
				103999d, 155999d, 999999d }; // the last element is calculated in the code as a balancing item
		Double[] meanIncome = new Double[numBins];
		Double[] persons = new Double[numBins];

		// get number of people from census data LGA by INCP
		Map<String, Map<String, String>> census = this.data.getCensusLgaByINCP();
		persons[0] = Double.valueOf(census.get("Negative income").get(lgaCode));
		persons[0] += Double.valueOf(census.get("Nil income").get(lgaCode));
		// persons[0] += Double.valueOf(census.get("Not stated").get(lgaCode)); //
		// exclude these from the sample
		persons[0] += Double.valueOf(census.get("Not applicable").get(lgaCode));
		persons[1] += Double.valueOf(census.get("$1-$149 ($1-$7,799)").get(lgaCode));
		persons[2] += Double.valueOf(census.get("$150-$299 ($7,800-$15,599)").get(lgaCode));
		persons[3] += Double.valueOf(census.get("$300-$399 ($15,600-$20,799)").get(lgaCode));
		persons[4] += Double.valueOf(census.get("$400-$499 ($20,800-$25,999)").get(lgaCode));
		persons[5] += Double.valueOf(census.get("$500-$649 ($26,000-$33,799)").get(lgaCode));
		persons[6] += Double.valueOf(census.get("$650-$799 ($33,800-$41,599)").get(lgaCode));
		persons[7] += Double.valueOf(census.get("$800-$999 ($41,600-$51,999)").get(lgaCode));
		persons[8] += Double.valueOf(census.get("$1,000-$1,249 ($52,000-$64,999)").get(lgaCode));
		persons[9] += Double.valueOf(census.get("$1,250-$1,499 ($65,000-$77,999)").get(lgaCode));
		persons[10] += Double.valueOf(census.get("$1,500-$1,749 ($78,000-$90,999)").get(lgaCode));
		persons[11] += Double.valueOf(census.get("$1,750-$1,999 ($91,000-$103,999)").get(lgaCode));
		persons[12] += Double.valueOf(census.get("$2,000-$2,999 ($104,000-$155,999)").get(lgaCode));
		persons[13] += Double.valueOf(census.get("$3,000 or more ($156,000 or more)").get(lgaCode));
		double unadjustedTotalPersons = 0d;
		for (int i = 0; i < numBins; i++) {
			unadjustedTotalPersons += persons[i];
		}

		// get total employee income, and calculate mean income of highest bracket
		double totalIncome = Double
				.valueOf(data.getAbs6524_055_002EmployeeTable5().get(financialYear).get("Income").get(lgaCode));
		double highestBracketIncome = totalIncome;
		for (int i = 0; i < numBins - 1; i++) {
			meanIncome[i] = (from[i] + to[i]) / 2d;
			highestBracketIncome -= persons[i] * meanIncome[i];
		}
		meanIncome[numBins - 1] = highestBracketIncome / persons[numBins - 1];
		to[numBins - 1] = meanIncome[numBins - 1] + (meanIncome[numBins - 1] - from[numBins - 1]);

		// multiply by adjusted LGA population
		double factor = this.data.getAdjustedPeopleByLga(lgaCode, date) / unadjustedTotalPersons;
		Double[] personsAdjusted = new Double[numBins];
		for (int i = 0; i < numBins; i++) {
			personsAdjusted[i] = persons[i] * factor;
		}

		// populate result
		Map<String, List<Double>> result = new HashMap<String, List<Double>>();
		result.put("from", Arrays.asList(from));
		result.put("to", Arrays.asList(to));
		result.put("meanIncome", Arrays.asList(meanIncome));
		result.put("adjustedPeople", Arrays.asList(personsAdjusted));

		return result;
	}

	private Map<String, List<Double>> segmentLgaByMrerd(String lgaCode, String financialYear, Date date) {
		// local constants
		final int numBins = 19;

		// local variables
		Double[] from = new Double[] { 0d, 1d * 12d, 150d * 12d, 300d * 12d, 450d * 12d, 600d * 12d, 800d * 12d,
				1000d * 12d, 1200d * 12d, 1400d * 12d, 1600d * 12d, 1800d * 12d, 2000d * 12d, 2200d * 12d, 2400d * 12d,
				2600d * 12d, 3000d * 12d, 4000d * 12d, 5000d * 12d };
		Double[] to = new Double[] { 0d, 1d * 149d, 299d * 12d, 449d * 12d, 599d * 12d, 799d * 12d, 999d * 12d,
				1199d * 12d, 1399d * 12d, 1599d * 12d, 1799d * 12d, 1999d * 12d, 2999d * 12d, 3999d * 12d, 4999d * 12d,
				5999d * 12d }; // the last element is calculated in the code as a balancing item
		Double[] meanMortgageRepayments = new Double[numBins];
		Double[] dwellings = new Double[numBins];

		// get number of people from census data LGA by INCP
		Map<String, Map<String, String>> census = this.data.getCensusLgaByMRERD();
		dwellings[0] = Double.valueOf(census.get("Nil repayments").get(lgaCode));
		// persons[0] += Double.valueOf(census.get("Not stated").get(lgaCode)); //
		// exclude these from the sample
		dwellings[0] += Double.valueOf(census.get("Not applicable").get(lgaCode));
		dwellings[1] += Double.valueOf(census.get("$1-$149").get(lgaCode));
		dwellings[2] += Double.valueOf(census.get("$150-$299").get(lgaCode));
		dwellings[3] += Double.valueOf(census.get("$300-$449").get(lgaCode));
		dwellings[4] += Double.valueOf(census.get("$450-$599").get(lgaCode));
		dwellings[5] += Double.valueOf(census.get("$600-$799").get(lgaCode));
		dwellings[6] += Double.valueOf(census.get("$800-$999").get(lgaCode));
		dwellings[7] += Double.valueOf(census.get("$1,000-$1,199").get(lgaCode));
		dwellings[8] += Double.valueOf(census.get("$1,200-$1,399").get(lgaCode));
		dwellings[9] += Double.valueOf(census.get("$1,400-$1,599").get(lgaCode));
		dwellings[10] += Double.valueOf(census.get("$1,600-$1,799").get(lgaCode));
		dwellings[11] += Double.valueOf(census.get("$1,800-$1,999").get(lgaCode));
		dwellings[12] += Double.valueOf(census.get("$2,000-$2,199").get(lgaCode));
		dwellings[13] += Double.valueOf(census.get("$2,200-$2,399").get(lgaCode));
		dwellings[14] += Double.valueOf(census.get("$2,400-$2,599").get(lgaCode));
		dwellings[15] += Double.valueOf(census.get("$2,600-$2,999").get(lgaCode));
		dwellings[16] += Double.valueOf(census.get("$3,000-$3,999").get(lgaCode));
		dwellings[17] += Double.valueOf(census.get("$4,000-$4,999").get(lgaCode));
		dwellings[18] += Double.valueOf(census.get("$5000 and over").get(lgaCode));
		double unadjustedTotalDwellings = 0d;
		for (int i = 0; i < numBins; i++) {
			unadjustedTotalDwellings += dwellings[i];
			meanMortgageRepayments[i] = (from[i] + to[i]) / 2d;
		}

		// multiply by adjusted LGA population
		// TODO: use ABS 3236.0 to adjust this to the projected number of dwellings.
		double factor = this.data.getAdjustedPeopleByLga(lgaCode, date) / unadjustedTotalDwellings;
		Double[] dwellingsAdjusted = new Double[numBins];
		for (int i = 0; i < numBins; i++) {
			dwellingsAdjusted[i] = dwellings[i] * factor;
		}

		// populate result
		Map<String, List<Double>> result = new HashMap<String, List<Double>>();
		result.put("from", Arrays.asList(from));
		result.put("to", Arrays.asList(to));
		result.put("meanMortgageRepayments", Arrays.asList(meanMortgageRepayments));
		result.put("adjustedDwellings", Arrays.asList(dwellingsAdjusted));

		return result;
	}

	private void calibrateNationalBalSht() {
		// TODO: implement me
	}

	private void assignProRataBalShtToAllIndividuals() {
		// TODO: implement me
	}

	private void init() {
		this.peoplePerAgent = 0;
		this.area = null;
		this.data = null;

		this.bsAUBankDeposits = 0d;
		this.bsAUOtherFinancialAssets = 0d;
		this.bsAUResidentialLandAndDwellings = 0d;
		this.bsAUOtherNonFinancialAssets = 0d;

		this.bsAULoans = 0d;
		this.bsAUOtherLiabilities = 0d;
	}

	/**
	 * @param data
	 *            the data to set
	 */
	@Autowired
	public void setData(CalibrationData data) {
		this.data = data;
	}

	/**
	 * @param area
	 *            the area to set
	 */
	@Autowired
	public void setArea(AreaMapping area) {
		this.area = area;
	}

	/**
	 * @param economy
	 *            the economy to set
	 */
	@Autowired
	public void setEconomy(AustralianEconomy economy) {
		this.economy = economy;
	}

}
