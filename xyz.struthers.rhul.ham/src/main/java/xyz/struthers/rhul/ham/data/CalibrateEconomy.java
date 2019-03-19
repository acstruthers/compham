/**
 * 
 */
package xyz.struthers.rhul.ham.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;

import xyz.struthers.rhul.ham.agent.AuthorisedDepositTakingInstitution;
import xyz.struthers.rhul.ham.agent.Business;
import xyz.struthers.rhul.ham.agent.ForeignCountry;
import xyz.struthers.rhul.ham.agent.Household;
import xyz.struthers.rhul.ham.agent.Individual;
import xyz.struthers.rhul.ham.config.Properties;
import xyz.struthers.rhul.ham.process.AustralianEconomy;

/**
 * Creates the links between the agents, and finishes initialising the fields
 * that can't be initialised until the links have been created.
 * 
 * @author Adam Struthers
 * @since 2019-03-18
 */
public class CalibrateEconomy {

	/*
	 * SOURCE: ABS 6530.0 Household Expenditure Survey, Australia: Summary of
	 * Results, 2015–16; Table 1.1 HOUSEHOLD EXPENDITURE, 1984 to 2015–16(a)
	 */
	public static final String[] ABS_6530_0_SPEND_CATEGORY = { "Domestic fuel and power",
			"Food and non-alcoholic beverages", "Alcoholic beverages", "Tobacco products", "Clothing and footwear",
			"Household furnishings and equipment", "Medical care and health expenses", "Transport", "Communication",
			"Recreation", "Education", "Personal care", "Miscellaneous goods and services" };
	public static final double[] ABS_6530_0_SPEND_AMT = { 40.92d, 236.97d, 31.95d, 12.84d, 43.75d, 57.87d, 44.90d,
			82.38d, 206.69d, 46.62d, 171.85d, 43.86d, 28.64d, 97.08d };
	public static final String[] ABS_6530_0_SPEND_DIV_CODE = { "D", "G", "H", "G", "G", "C", "P", "Q", "I", "J", "R",
			"P", "S", "S" };

	public static final int[] DIVISION_CODE_INDICES = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17,
			18 };

	// beans
	private CalibrationData commonData;
	private AreaMapping area;
	private AustralianEconomy economy;
	private Properties properties;

	// agents
	private Household[] households;
	private Individual[] individuals;
	private Business[] businesses;
	private AuthorisedDepositTakingInstitution[] adis;
	private ForeignCountry[] countries;
	private Currencies currencies;

	// field variables
	private Random random;

	/**
	 * 
	 */
	public CalibrateEconomy() {
		super();
		this.init();
	}

	/**
	 * Joins all the agents in the economy, and finishes calibrating the figures
	 * that can't be calibrated until they're linked (e.g. loan balances and
	 * interest rates).
	 * 
	 * Although Individuals are employed by Businesses, it is Household units who
	 * participate in the economy so only Households will be included in the
	 * Payments Clearing Vector calculations. This saves RAM, time, and recognises
	 * that families support each other.
	 * 
	 * ROUGH ALGORITHM:
	 * 
	 * --------------------------------------------- - PART A: Assigning employees
	 * to businesses - ---------------------------------------------
	 * 
	 * Make a PDF of businesses by their wage expense: one for each industry
	 * division.
	 * 
	 * Choose business in the same industry as the individual, based on PDF<br>
	 * IF (existing assigned wages + current wage) < (business calibrated wage +
	 * small tolerance)<br>
	 * THEN assign individual to business
	 * 
	 * FIXME: Randomly select 50k businesses and make a PDF of these businesses' relative
	 * revenue (foreign revenue?), by state. Assign foreign countries to the
	 * businesses based on the given probabilities.
	 */
	public void linkAllAgents() {
		// get agents

		this.random = this.properties.getRandom();

		// FIXME: implement me
		double[] abs6530SpendRatios = this.calcAbs6530SpendRatios();

	}

	private void init() {
		// agents
		this.households = null;
		this.individuals = null;
		this.businesses = null;
		this.adis = null;
		this.countries = null;
		this.currencies = null;

		// field variables
		this.random = null;
	}

	/**
	 * Calculates the ratios between the line items in the ABS 6530.0 household
	 * expenditure data.
	 * 
	 * @return
	 */
	private double[] calcAbs6530SpendRatios() {
		double[] ratios = new double[ABS_6530_0_SPEND_AMT.length];
		double total = 0d;
		for (int i = 0; i < ABS_6530_0_SPEND_AMT.length; i++) {
			total += ABS_6530_0_SPEND_AMT[i];
		}
		for (int i = 0; i < ABS_6530_0_SPEND_AMT.length; i++) {
			ratios[i] = ABS_6530_0_SPEND_AMT[i] / total;
		}
		return ratios;
	}

	/**
	 * Calculates the ratio of the home and personal loan balances by ADI. This will
	 * be used as a PDF to assign Households' loans to ADIs.
	 * 
	 * @return
	 */
	private double[] calcAdiIndividualLoanRatios() {
		double[] ratios = new double[this.adis.length];
		double total = 0d;
		for (int i = 0; i < this.adis.length; i++) {
			total += this.adis[i].getBsLoansHome() + this.adis[i].getBsLoansPersonal();
		}
		for (int i = 0; i < this.adis.length; i++) {
			ratios[i] = (this.adis[i].getBsLoansHome() + this.adis[i].getBsLoansPersonal()) / total;
		}
		return ratios;
	}

	/**
	 * Calculates the ratio of the deposit balances by ADI. This will be used as a
	 * PDF to assign Households' deposits to ADIs.
	 * 
	 * @return
	 */
	private double[] calcAdiDepositRatios() {
		double[] ratios = new double[this.adis.length];
		double total = 0d;
		for (int i = 0; i < this.adis.length; i++) {
			total += this.adis[i].getBsDepositsAtCall() + this.adis[i].getBsDepositsTerm();
		}
		for (int i = 0; i < this.adis.length; i++) {
			ratios[i] = (this.adis[i].getBsDepositsAtCall() + this.adis[i].getBsDepositsTerm()) / total;
		}
		return ratios;
	}

	/**
	 * Calculates the ratio of the business loan balances by ADI. This will be used
	 * as a PDF to assign Business loans and deposits to ADIs. Businesses are
	 * assumed to bank with a single ADI, so both loans and deposits will be
	 * assigned to the same ADI.
	 * 
	 * @return
	 */
	private double[] calcAdiBusinessLoanRatios() {
		double[] ratios = new double[this.adis.length];
		double total = 0d;
		for (int i = 0; i < this.adis.length; i++) {
			total += this.adis[i].getBsLoansBusiness();
		}
		for (int i = 0; i < this.adis.length; i++) {
			ratios[i] = this.adis[i].getBsLoansBusiness() / total;
		}
		return ratios;
	}

	/**
	 * Calculates the ratio of business's wage expenses by Division. This will be
	 * used as a PDF to assign Business loans and deposits to ADIs. Businesses are
	 * assumed to bank with a single ADI, so both loans and deposits will be
	 * assigned to the same ADI.
	 * 
	 * @return
	 */
	private List<List<Double>> calcBusinessDivisionWageRatios() {
		// initialise division totals
		Double[] divTotal = Collections.nCopies(CalibrateIndividuals.DIVISION_CODE_ARRAY.length, 0d)
				.toArray(Double[]::new);
		Integer[] divisionBusinessCount = Collections.nCopies(CalibrateIndividuals.DIVISION_CODE_ARRAY.length, 0)
				.toArray(Integer[]::new);

		// calculate division totals
		for (int busIdx = 0; busIdx < this.businesses.length; busIdx++) {
			char divCode = this.businesses[busIdx].getIndustryDivisionCode();
			int divIdx = this.getIndustryDivisionIndex(divCode);
			divTotal[divIdx] += this.businesses[busIdx].getWageExpenses();
			divisionBusinessCount[divIdx]++;
		}

		// initialise ratios
		List<List<Double>> ratios = new ArrayList<List<Double>>(CalibrateIndividuals.DIVISION_CODE_ARRAY.length);
		for (int ratioIdx = 0; ratioIdx < CalibrateIndividuals.DIVISION_CODE_ARRAY.length; ratioIdx++) {
			ratios.add(new ArrayList<Double>(divisionBusinessCount[ratioIdx]));
		}

		// calculate ratios
		for (int busIdx = 0; busIdx < this.businesses.length; busIdx++) {
			char divCode = this.businesses[busIdx].getIndustryDivisionCode();
			int divIdx = this.getIndustryDivisionIndex(divCode);
			ratios.get(divIdx).add(this.businesses[busIdx].getWageExpenses() / divTotal[divIdx]);
		}
		return ratios;
	}

	private int getIndustryDivisionIndex(char divisionCode) {
		int index = 0;
		switch (divisionCode) {
		case 'A':
			index = 0;
			break;
		case 'B':
			index = 1;
			break;
		case 'C':
			index = 2;
			break;
		case 'D':
			index = 3;
			break;
		case 'E':
			index = 4;
			break;
		case 'F':
			index = 5;
			break;
		case 'G':
			index = 6;
			break;
		case 'H':
			index = 7;
			break;
		case 'I':
			index = 8;
			break;
		case 'J':
			index = 9;
			break;
		case 'K':
			index = 10;
			break;
		case 'L':
			index = 11;
			break;
		case 'M':
			index = 12;
			break;
		case 'N':
			index = 13;
			break;
		case 'O':
			index = 14;
			break;
		case 'P':
			index = 15;
			break;
		case 'Q':
			index = 16;
			break;
		case 'R':
			index = 17;
			break;
		default: // case 'S':
			index = 18;
			break;
		}
		return index;
	}

	/**
	 * @param data the data to set
	 */
	@Autowired
	public void setCommonData(CalibrationData data) {
		this.commonData = data;
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
