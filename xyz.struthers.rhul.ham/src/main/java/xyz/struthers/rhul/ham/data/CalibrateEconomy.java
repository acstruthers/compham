/**
 * 
 */
package xyz.struthers.rhul.ham.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import xyz.struthers.rhul.ham.agent.AustralianGovernment;
import xyz.struthers.rhul.ham.agent.AuthorisedDepositTakingInstitution;
import xyz.struthers.rhul.ham.agent.Business;
import xyz.struthers.rhul.ham.agent.ForeignCountry;
import xyz.struthers.rhul.ham.agent.Household;
import xyz.struthers.rhul.ham.agent.Individual;
import xyz.struthers.rhul.ham.agent.ReserveBankOfAustralia;
import xyz.struthers.rhul.ham.config.Properties;
import xyz.struthers.rhul.ham.process.AustralianEconomy;
import xyz.struthers.rhul.ham.process.Employer;

/**
 * Creates the links between the agents, and finishes initialising the fields
 * that can't be initialised until the links have been created.
 * 
 * @author Adam Struthers
 * @since 2019-03-18
 */
@Component
@Scope(value = "singleton")
public class CalibrateEconomy {

	private static final boolean DEBUG = true;
	private static final boolean DEBUG_HH = false;

	/*
	 * SOURCE: ABS 6530.0 Household Expenditure Survey, Australia: Summary of
	 * Results, 2015–16; Table 1.1 HOUSEHOLD EXPENDITURE, 1984 to 2015–16(a)
	 */
	public static final String[] ABS_6530_0_SPEND_CATEGORY = { "Domestic fuel and power",
			"Food and non-alcoholic beverages", "Alcoholic beverages", "Tobacco products", "Clothing and footwear",
			"Household furnishings and equipment", "Medical care and health expenses", "Transport", "Communication",
			"Recreation", "Education", "Personal care", "Miscellaneous goods and services" };
	public static final float[] ABS_6530_0_SPEND_AMT = { 40.92f, 236.97f, 31.95f, 12.84f, 43.75f, 57.87f, 44.90f,
			82.38f, 206.69f, 46.62f, 171.85f, 43.86f, 28.64f, 97.08f };
	public static final String[] ABS_6530_0_SPEND_DIV_CODE = { "D", "G", "H", "G", "G", "C", "P", "Q", "I", "J", "R",
			"P", "S", "S" };
	public static final String[] BUSINESS_SUPPLIER_DIV_CODE = { "A", "B", "C", "D", "E", "F", "I", "J", "K", "L", "M",
			"N", "O" };
	public static final String[] ADI_SUPPLIER_DIV_CODE = { "C", "D", "E", "F", "I", "J", "K", "L", "M", "N", "O", "P" };
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
	private AustralianGovernment govt;
	private ReserveBankOfAustralia rba;

	// field variables
	private Random random;
	private boolean indicesAssigned;

	/**
	 * 
	 */
	public CalibrateEconomy() {
		super();
		this.init();
	}

	private void init() {
		// agents
		this.households = null;
		this.individuals = null;
		this.businesses = null;
		this.adis = null;
		this.countries = null;
		this.currencies = null;
		this.govt = null;
		this.rba = null;

		// field variables
		this.random = null;
		this.indicesAssigned = false;
	}

	/**
	 * Deletes all the field variables, freeing up memory.
	 * 
	 * Does not do a deep delete because most objects are passed to other classes by
	 * reference, and the other classes will probably still need to refer to them.
	 */
	public void close() {
		// beans
		this.commonData = null;
		this.area = null;
		this.economy = null;
		this.properties = null;

		// agents
		this.households = null;
		this.individuals = null;
		this.businesses = null;
		this.adis = null;
		this.countries = null;
		this.currencies = null;
		this.govt = null;
		this.rba = null;

		// field variables
		this.random = null;
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
	 * Links are stored in the agent who has the liability. I would like to store
	 * pointers in both agents involved in the link(to make reporting easier), but
	 * it would consume a lot of unnecessary memory.
	 */
	public void linkAllAgents() {
		// get agents
		this.govt = this.economy.getGovernment();
		this.rba = this.economy.getRba();
		this.households = this.economy.getHouseholds();
		this.individuals = this.economy.getIndividuals();
		this.businesses = this.economy.getBusinesses();
		this.adis = this.economy.getAdis();
		this.countries = this.economy.getCountries();
		this.currencies = this.economy.getCurrencies();

		/*
		 * To ensure that every agent has other agents linked to it, use the shuffled
		 * index approach rather than just sampling from a PDF to assign links. Could
		 * even populate the indices with relative counts per the PDF so that they get
		 * the right weighting during the sampling process. It would be much harder to
		 * weight by balance too, so I would probably just use "head count" to assign
		 * links.
		 * 
		 * An alternative but similar approach would be to re-calculate the PDF between
		 * each assignment so it's more akin to sampling without replacement. That would
		 * be better than the pure PDF approach, but still not as robust as the shuffled
		 * index approach. The biggest issue I've had in the calibration process has
		 * been mis-matches where cells have data in some sources but not others, so the
		 * cross-product results in lots more empty cells and ultimately a loss of data.
		 */
		this.assignPaymentClearingIndices();
		this.random = this.properties.getRandom();
		this.linkHouseholds();
		this.linkEmployees();
		this.linkBusinesses();
		this.linkAdis();
		this.linkForeignCountries();
		this.linkGovernment();
		this.linkRba();

	}

	/**
	 * Assign unique Payment Clearing Indices to every participating agent.
	 * 
	 * N.B. This is the order they MUST be in the Payment Clearing Vector arguments
	 */
	private void assignPaymentClearingIndices() {
		if (!this.indicesAssigned) {
			int paymentClearingIdx = 0;
			this.govt.setPaymentClearingIndex(paymentClearingIdx++);
			this.rba.setPaymentClearingIndex(paymentClearingIdx++);
			for (int i = 0; i < this.households.length; i++) {
				this.households[i].setPaymentClearingIndex(paymentClearingIdx++);
			}
			for (int i = 0; i < this.businesses.length; i++) {
				this.businesses[i].setPaymentClearingIndex(paymentClearingIdx++);
			}
			for (int i = 0; i < this.adis.length; i++) {
				this.adis[i].setPaymentClearingIndex(paymentClearingIdx++);
			}
			for (int i = 0; i < this.countries.length; i++) {
				this.countries[i].setPaymentClearingIndex(paymentClearingIdx++);
			}
			this.indicesAssigned = true;
		}
	}

	/**
	 * Links the Households to their banks, employers and suppliers.
	 * 
	 * This includes linking Individuals to their employers, though they will only
	 * be included in the payment clearing vector at a Household level.
	 */
	private void linkHouseholds() {
		if (!this.indicesAssigned) {
			this.assignPaymentClearingIndices();
		}

		// link Household spending to businesses (domestic suppliers)
		Set<String> spendingDivisions = new HashSet<String>(Arrays.asList(ABS_6530_0_SPEND_DIV_CODE));
		Map<String, Float> industryTotals = new HashMap<String, Float>(
				(int) Math.ceil(ABS_6530_0_SPEND_DIV_CODE.length / 0.75f));
		// calculate total domestic sales per industry
		for (int i = 0; i < this.businesses.length; i++) {
			String div = String.valueOf(this.businesses[i].getIndustryDivisionCode());
			if (spendingDivisions.contains(div)) {
				if (!industryTotals.containsKey(div)) {
					industryTotals.put(div, 0f);
				}
				industryTotals.put(div, industryTotals.get(div) + this.businesses[i].getSalesDomestic());
			}
		}
		// make a list of businesses in each industry, with relative weights
		Map<String, ArrayList<Integer>> businessIndices = new HashMap<String, ArrayList<Integer>>(
				(int) Math.ceil(spendingDivisions.size() / 0.75f));
		for (String div : spendingDivisions) {
			businessIndices.put(div, new ArrayList<Integer>(this.households.length));
		}
		for (int i = 0; i < this.businesses.length; i++) {
			String div = String.valueOf(this.businesses[i].getIndustryDivisionCode());
			if (spendingDivisions.contains(div)) {
				float domesticSales = this.businesses[i].getSalesDomestic();
				int businessCount = (int) Math.ceil(domesticSales / industryTotals.get(div) * this.households.length);
				businessIndices.get(div).addAll(Collections.nCopies(businessCount, i));
			}
		}
		// shuffle indices and assign household spending per ABS 6530.0 ratios
		Map<String, Integer> nextBusinessIndex = new HashMap<String, Integer>(
				(int) Math.ceil(spendingDivisions.size() / 0.75f));
		ArrayList<Float> abs6530SpendRatios = this.calcAbs6530SpendRatios();
		for (String div : spendingDivisions) {
			businessIndices.get(div).trimToSize();
			Collections.shuffle(businessIndices.get(div), this.random);
			nextBusinessIndex.put(div, 0);
		}
		for (int i = 0; i < this.households.length; i++) {
			ArrayList<Business> suppliers = new ArrayList<Business>(ABS_6530_0_SPEND_DIV_CODE.length);
			for (int j = 0; j < ABS_6530_0_SPEND_DIV_CODE.length; j++) {
				// String div = String.valueOf(this.businesses[i].getIndustryDivisionCode());
				String div = ABS_6530_0_SPEND_DIV_CODE[j];
				// assign next random business for that Division
				int nextShuffledIndex = nextBusinessIndex.get(div);
				suppliers.add(this.businesses[businessIndices.get(div).get(nextShuffledIndex)]);
				// increment the index, but stay within the array bounds
				nextBusinessIndex.put(div, (nextShuffledIndex + 1) % businessIndices.get(div).size());
			}
			this.households[i].setSuppliers(suppliers);
			this.households[i].setSupplierRatios(abs6530SpendRatios);
		}
		// release memory
		for (String div : spendingDivisions) {
			businessIndices.get(div).clear();
			businessIndices.put(div, null);
		}
		businessIndices.clear();
		businessIndices = null;
		nextBusinessIndex.clear();
		nextBusinessIndex = null;
		industryTotals.clear();
		industryTotals = null;
		spendingDivisions.clear();
		spendingDivisions = null;
		abs6530SpendRatios = null; // don't clear it because the Households still point to it

		// Landlord
		float totalRent = (float) Arrays.asList(this.households).stream().mapToDouble(o -> o.getPnlRentIncome()).sum();
		// populate indices with relative amounts of each landlord
		ArrayList<Integer> shuffledIndices = new ArrayList<Integer>(this.households.length);
		for (int i = 0; i < this.households.length; i++) {
			// calculate ratio of landlord to total
			float landlordRent = this.households[i].getPnlRentIncome();
			if (landlordRent > 0f) {
				// convert to indices, rounding up so we have at least enough
				int landlordTenantCount = (int) Math.ceil(landlordRent / totalRent * this.households.length);
				shuffledIndices.addAll(Collections.nCopies(landlordTenantCount, i));
				if (DEBUG_HH) {
					System.out.println("landlordRent (" + i + "): " + landlordRent);
				}
			}
		}
		// shuffle indices, and assign landlords to renting Households
		shuffledIndices.trimToSize();
		Collections.shuffle(shuffledIndices, this.random);
		int nextShuffledIdx = 0;
		for (int i = 0; i < this.households.length; i++) {
			if (this.households[i].getPnlRentExpense() > 0f) {
				// assign landlord to Household
				if (DEBUG_HH) {
					System.out.println("totalRent: " + totalRent);
					System.out.println("i: " + i);
					System.out.println("households.length: " + households.length);
					System.out.println("shuffledIndices.size(): " + shuffledIndices.size());
					System.out.println("nextShuffledIdx: " + nextShuffledIdx);
					System.out
							.println("shuffledIndices.get(nextShuffledIdx++): " + shuffledIndices.get(nextShuffledIdx));
					System.out.println("this.households[shuffledIndices.get(nextShuffledIdx++)]: "
							+ this.households[shuffledIndices.get(nextShuffledIdx)]);
				}
				this.households[i].setLandlord(this.households[shuffledIndices.get(nextShuffledIdx++)]);
			}
		}
		// release memory
		shuffledIndices.clear();
		shuffledIndices = null;

		// Loan ADI
		// get total loans for entire ADI industry
		float totalLoanBal = (float) Arrays.asList(this.adis).stream().mapToDouble(o -> o.getBsLoansHome()).sum();
		totalLoanBal += (float) Arrays.asList(this.adis).stream().mapToDouble(o -> o.getBsLoansPersonal()).sum();
		// populate indices with relative amounts of each ADI
		shuffledIndices = new ArrayList<Integer>(this.households.length);
		for (int i = 0; i < this.adis.length; i++) {
			// calculate ratio of ADI to total
			float adiLoanBal = this.adis[i].getBsLoansHome() + this.adis[i].getBsLoansPersonal();
			if (adiLoanBal > 0f) {
				// convert to indices, rounding up so we have at least enough
				int adiCustomerCount = (int) Math.ceil(adiLoanBal / totalLoanBal * this.households.length);
				shuffledIndices.addAll(Collections.nCopies(adiCustomerCount, i));
			}
		}
		// shuffle indices, and assign ADIs to Households
		shuffledIndices.trimToSize();
		Collections.shuffle(shuffledIndices, this.random);
		nextShuffledIdx = 0;
		for (int i = 0; i < this.households.length; i++) {
			if (this.households[i].getBsLoans() > 0f) {
				// assign loan ADI to Household
				this.households[i].setLoanAdi(this.adis[shuffledIndices.get(nextShuffledIdx++)]);
			}
		}
		// release memory
		shuffledIndices.clear();
		shuffledIndices = null;

		// Australian Government
		for (int i = 0; i < this.households.length; i++) {
			this.households[i].setGovt(this.govt);
		}
	}

	/**
	 * Assigns employees to employers. Recognises that employers can be either
	 * Business, ADIs, the RBA or the Australian Government.
	 * 
	 * FIXME: Individuals are already assigned to Industry Divisions, so should
	 * drill through the Household to get the relevant Individuals and link the
	 * employer to the Household based on the Individual's Division and wages or
	 * salary.
	 */
	private void linkEmployees() {
		if (!this.indicesAssigned) {
			this.assignPaymentClearingIndices();
		}

		// link to employees
		// get total wages expense by industry division
		Map<String, Float> totalWagesExpenseByDiv = new HashMap<String, Float>(
				(int) Math.ceil(CalibrateIndividuals.DIVISION_CODE_ARRAY.length / 0.75f));
		for (String div : CalibrateIndividuals.DIVISION_CODE_ARRAY) {
			float totalWagesExpense = (float) Arrays.asList(this.businesses).stream()
					.filter(o -> String.valueOf(o.getIndustryDivisionCode()).equals(div))
					.mapToDouble(o -> o.getInitialWagesExpense()).sum();
			totalWagesExpense += (float) Arrays.asList(this.adis).stream()
					.filter(o -> String.valueOf(o.getIndustryDivisionCode()).equals(div))
					.mapToDouble(o -> o.getInitialWagesExpense()).sum();
			totalWagesExpense += (float) Arrays.asList(this.rba).stream()
					.filter(o -> String.valueOf(o.getIndustryDivisionCode()).equals(div))
					.mapToDouble(o -> o.getInitialWagesExpense()).sum();
			totalWagesExpense += (float) Arrays.asList(this.govt).stream()
					.filter(o -> String.valueOf(o.getIndustryDivisionCode()).equals(div))
					.mapToDouble(o -> o.getInitialWagesExpense()).sum();
			totalWagesExpenseByDiv.put(div, totalWagesExpense);
		}
		// get list of all Employers
		List<Business> businessEmployers = Arrays.asList(this.businesses).stream()
				.filter(o -> o.getInitialWagesExpense() > 0f).collect(Collectors.toList());
		ArrayList<Employer> employers = new ArrayList<Employer>(businessEmployers);
		employers.addAll(Arrays.asList(this.adis)); // all ADIs have employees
		employers.add(this.rba); // RBA has employees
		employers.add(this.govt); // government has employees
		employers.trimToSize();
		// get list of all Households with employees, by industry division
		List<Household> employeeHouseholds = Arrays.asList(this.households).stream()
				.filter(o -> o.getPnlWagesSalaries() > 0f).collect(Collectors.toList());
		Map<String, ArrayList<Household>> employeeHouseholdsByDiv = new HashMap<String, ArrayList<Household>>(
				(int) Math.ceil(DIVISION_CODE_INDICES.length / 0.75f));
		for (Household house : employeeHouseholds) {
			Individual[] members = house.getIndividuals();
			for (int individualIdx = 0; individualIdx < members.length; individualIdx++) {
				if (members[individualIdx].getPnlWagesSalaries() > 0f) {
					// Individual is an employee
					String div = members[individualIdx].getEmploymentIndustry();
					if (!employeeHouseholdsByDiv.containsKey(div)) {
						employeeHouseholdsByDiv.put(div, new ArrayList<Household>(
								(int) Math.ceil(this.households.length / DIVISION_CODE_INDICES.length * 2f / 0.75f)));
					}
					employeeHouseholdsByDiv.get(div).add(house);
				}
			}
		}
		for (String div : employeeHouseholdsByDiv.keySet()) {
			employeeHouseholdsByDiv.get(div).trimToSize();
		}
		// get list of all Individual employees, by industry division
		List<Individual> employees = Arrays.asList(this.individuals).stream().filter(o -> o.getPnlWagesSalaries() > 0f)
				.collect(Collectors.toList());
		Map<String, ArrayList<Individual>> employeesByDiv = new HashMap<String, ArrayList<Individual>>(
				(int) Math.ceil(DIVISION_CODE_INDICES.length / 0.75f));
		for (Individual employee : employees) {
			String div = employee.getEmploymentIndustry();
			if (!employeesByDiv.containsKey(div)) {
				employeesByDiv.put(div, new ArrayList<Individual>(
						(int) Math.ceil(employees.size() / DIVISION_CODE_INDICES.length * 2f / 0.75f)));
			}
			employeesByDiv.get(div).add(employee);
		}
		for (String div : employeesByDiv.keySet()) {
			employeesByDiv.get(div).trimToSize();
		}
		// for each division, calculate employer ratio and multiply by employee count
		Map<String, ArrayList<Integer>> shuffledEmployerIndicesByDiv = new HashMap<String, ArrayList<Integer>>(
				(int) Math.ceil(totalWagesExpenseByDiv.size() / 0.75f));
		for (int i = 0; i < employers.size(); i++) {
			// Employer employer : employers
			String div = String.valueOf(employers.get(i).getIndustryDivisionCode());
			float divTotalWages = totalWagesExpenseByDiv.get(div);
			float employerWages = employers.get(i).getInitialWagesExpense();
			float employeesPerDiv = employeesByDiv.get(div).size();
			int employeeCount = (int) Math.ceil(employerWages / divTotalWages * employeesPerDiv);
			if (!shuffledEmployerIndicesByDiv.containsKey(div)) {
				shuffledEmployerIndicesByDiv.put(div, new ArrayList<Integer>(
						(int) Math.ceil(employees.size() / DIVISION_CODE_INDICES.length * 2f / 0.75f)));
			}
			shuffledEmployerIndicesByDiv.get(div).addAll(Collections.nCopies(employeeCount, i));
		}
		// shuffle indices and initialise next index map
		Map<String, Integer> nextDivIdx = new HashMap<String, Integer>(
				(int) Math.ceil(shuffledEmployerIndicesByDiv.size() / 0.75f));
		for (String div : shuffledEmployerIndicesByDiv.keySet()) {
			shuffledEmployerIndicesByDiv.get(div).trimToSize();
			Collections.shuffle(shuffledEmployerIndicesByDiv.get(div), this.random);
			nextDivIdx.put(div, 0);
		}
		// assign employees to employers
		for (int employeeIdx = 0; employeeIdx < employees.size(); employeeIdx++) {
			Individual employee = employees.get(employeeIdx);
			String div = employee.getEmploymentIndustry();
			int nextIdx = nextDivIdx.get(div);
			Employer employer = employers.get(shuffledEmployerIndicesByDiv.get(div).get(nextIdx));
			employer.addEmployee(employee);
			nextIdx = (nextIdx + 1) % shuffledEmployerIndicesByDiv.get(div).size();
			nextDivIdx.put(div, nextIdx);
		}
		// release memory
		totalWagesExpenseByDiv.clear();
		totalWagesExpenseByDiv = null;
		businessEmployers.clear();
		businessEmployers = null;
		employers.clear();
		employers = null;
		employeeHouseholds.clear();
		employeeHouseholds = null;
		for (String div : employeeHouseholdsByDiv.keySet()) {
			employeeHouseholdsByDiv.get(div).clear();
			employeeHouseholdsByDiv.put(div, null);
		}
		employeeHouseholdsByDiv.clear();
		employeeHouseholdsByDiv = null;
		employees.clear();
		employees = null;
		for (String div : employeesByDiv.keySet()) {
			employeesByDiv.get(div).clear();
			employeesByDiv.put(div, null);
		}
		employeesByDiv.clear();
		employeesByDiv = null;
		for (String div : shuffledEmployerIndicesByDiv.keySet()) {
			shuffledEmployerIndicesByDiv.get(div).clear();
			shuffledEmployerIndicesByDiv.put(div, null);
		}
		shuffledEmployerIndicesByDiv.clear();
		shuffledEmployerIndicesByDiv = null;
		for (String div : nextDivIdx.keySet()) {
			nextDivIdx.put(div, null);
		}
		nextDivIdx.clear();
		nextDivIdx = null;
	}

	/**
	 * Links the Businesses to their banks and foreign trading parties.
	 * 
	 * N.B. Must run linkBusiness() before linkAdis() for the business links to be
	 * populated.
	 * 
	 * ------------------------------------------------------------------
	 * 
	 * Make a PDF of businesses by their wage expense: one for each industry
	 * division.
	 * 
	 * Choose business in the same industry as the individual, based on PDF<br>
	 * IF (existing assigned wages + current wage) < (business calibrated wage +
	 * small tolerance)<br>
	 * THEN assign individual to business
	 * 
	 * FIXME: Randomly select 50k businesses and make a PDF of these businesses'
	 * relative revenue (foreign revenue?), by state. Assign foreign countries to
	 * the businesses based on the given probabilities.
	 */
	void linkBusinesses() {
		if (!this.indicesAssigned) {
			this.assignPaymentClearingIndices();
		}

		// N.B. link to employees performed in linkEmployees()

		// link to domestic suppliers
		// get total domestic revenue by industry division
		Map<String, Float> totalDomesticRevenueByDiv = new HashMap<String, Float>(
				(int) Math.ceil(BUSINESS_SUPPLIER_DIV_CODE.length / 0.75f));
		for (String div : BUSINESS_SUPPLIER_DIV_CODE) {
			float divDomesticRevenue = (float) Arrays.asList(this.businesses).stream()
					.filter(o -> String.valueOf(o.getIndustryDivisionCode()).equals(div))
					.mapToDouble(o -> o.getSalesDomestic()).sum();
			totalDomesticRevenueByDiv.put(div, divDomesticRevenue);
		}
		// assume all businesses have some domestic expenses
		// get businesses with domestic revenue, by division (for specified divisions)
		Set<String> domesticSupplierDivs = new HashSet<String>(Arrays.asList(BUSINESS_SUPPLIER_DIV_CODE));
		// make a list of suppliers in each industry, with relative weights
		Map<String, ArrayList<Integer>> shuffledSupplierIndicesByDiv = new HashMap<String, ArrayList<Integer>>(
				(int) Math.ceil(BUSINESS_SUPPLIER_DIV_CODE.length / 0.75f));
		for (String div : BUSINESS_SUPPLIER_DIV_CODE) {
			shuffledSupplierIndicesByDiv.put(div, new ArrayList<Integer>(
					(int) Math.ceil(this.businesses.length / BUSINESS_SUPPLIER_DIV_CODE.length * 2f / 0.75f)));
		}
		for (int i = 0; i < this.businesses.length; i++) {
			String div = this.businesses[i].getIndustryCode();
			if (domesticSupplierDivs.contains(div)) {
				float domesticSales = this.businesses[i].getSalesDomestic();
				int businessCount = (int) Math
						.ceil(domesticSales / totalDomesticRevenueByDiv.get(div) * this.businesses.length);
				shuffledSupplierIndicesByDiv.get(div).addAll(Collections.nCopies(businessCount, i));
			}
		}
		// shuffle indices and initialise next index map
		Map<String, Integer> nextDivIdx = new HashMap<String, Integer>(
				(int) Math.ceil(shuffledSupplierIndicesByDiv.size() / 0.75f));
		for (String div : BUSINESS_SUPPLIER_DIV_CODE) {
			shuffledSupplierIndicesByDiv.get(div).trimToSize();
			Collections.shuffle(shuffledSupplierIndicesByDiv.get(div), this.random);
			nextDivIdx.put(div, 0);
		}
		// assign suppliers to customers
		for (int customerIdx = 0; customerIdx < this.businesses.length; customerIdx++) {
			Business customer = this.businesses[customerIdx];
			for (int supDivIdx = 0; supDivIdx < BUSINESS_SUPPLIER_DIV_CODE.length; supDivIdx++) {
				String div = BUSINESS_SUPPLIER_DIV_CODE[supDivIdx];
				int nextIdx = nextDivIdx.get(div);
				Business supplier = this.businesses[shuffledSupplierIndicesByDiv.get(div).get(nextIdx)];
				customer.addDomesticSupplier(supplier);
				nextIdx = (nextIdx + 1) % shuffledSupplierIndicesByDiv.get(div).size();
				nextDivIdx.put(div, nextIdx);
			}
			customer.trimDomesticSuppliersList();
		}
		// release memory
		for (String div : totalDomesticRevenueByDiv.keySet()) {
			totalDomesticRevenueByDiv.put(div, null);
		}
		totalDomesticRevenueByDiv.clear();
		totalDomesticRevenueByDiv = null;
		domesticSupplierDivs.clear();
		domesticSupplierDivs = null;
		for (String div : shuffledSupplierIndicesByDiv.keySet()) {
			shuffledSupplierIndicesByDiv.put(div, null);
		}
		shuffledSupplierIndicesByDiv.clear();
		shuffledSupplierIndicesByDiv = null;
		for (String div : nextDivIdx.keySet()) {
			nextDivIdx.put(div, null);
		}
		nextDivIdx.clear();
		nextDivIdx = null;

		// FIXME: up to here implementing linkBusinesses()
		// TODO link to foreign suppliers (i.e. foreign countries)

		// link to landlord (rent)
		// get list of businesses with rental income
		float totalRentIncome = (float) Arrays.asList(this.businesses).stream().filter(o -> o.getRentIncome() > 0f)
				.mapToDouble(o -> o.getRentIncome()).sum();
		List<Business> landlords = Arrays.asList(this.businesses).stream().filter(o -> o.getRentIncome() > 0f)
				.collect(Collectors.toList());
		List<Business> renters = Arrays.asList(this.businesses).stream().filter(o -> o.getRentExpense() > 0f)
				.collect(Collectors.toList());
		ArrayList<Integer> shuffledIndices = new ArrayList<Integer>(renters.size());
		for (int i = 0; i < landlords.size(); i++) {
			// calculate ratio of landlord to total rent
			float landlordRentIncome = landlords.get(i).getRentIncome();
			int landlordRenterCount = (int) Math.ceil(landlordRentIncome / totalRentIncome * renters.size());
			shuffledIndices.addAll(Collections.nCopies(landlordRenterCount, i));
		}
		// shuffle indices, and assign landlords to renters
		Collections.shuffle(shuffledIndices, this.random);
		int nextShuffledIdx = 0;
		for (int i = 0; i < renters.size(); i++) {
			renters.get(i).setLandlord(landlords.get(shuffledIndices.get(nextShuffledIdx++)));
		}
		// release memory
		shuffledIndices.clear();
		shuffledIndices = null;

		// link to ADI (loans & deposits with same ADI)
		// get total business loans for entire ADI industry
		float totalLoanBal = (float) Arrays.asList(this.adis).stream().mapToDouble(o -> o.getBsLoansBusiness()).sum();
		// populate indices with relative amounts of each ADI
		shuffledIndices = new ArrayList<Integer>(this.businesses.length);
		for (int i = 0; i < this.adis.length; i++) {
			// calculate ratio of ADI to total
			float adiLoanBal = this.adis[i].getBsLoansBusiness();
			if (adiLoanBal > 0f) {
				// convert to indices, rounding up so we have at least enough
				int adiCustomerCount = (int) Math.ceil(adiLoanBal / totalLoanBal * this.businesses.length);
				shuffledIndices.addAll(Collections.nCopies(adiCustomerCount, i));
			}
		}
		// shuffle indices, and assign ADIs to Businesses
		Collections.shuffle(shuffledIndices, this.random);
		nextShuffledIdx = 0;
		for (int i = 0; i < this.businesses.length; i++) {
			// Assign loan ADI to Business. It doesn't matter if the Business doesn't have a
			// loan. We assume businesses only bank with ADIs who have business loans.
			this.businesses[i].setAdi(this.adis[shuffledIndices.get(nextShuffledIdx++)]);
		}
		// release memory
		shuffledIndices.clear();
		shuffledIndices = null;

		// link to Australian Government (payroll & income tax)
		for (int i = 0; i < this.businesses.length; i++) {
			this.businesses[i].setGovt(this.govt);
		}
	}

	/**
	 * After the Households and Businesses have been linked to the ADIs, this
	 * re-calibrates the ADI's financials so that they're proportional to the actual
	 * amount of business that has been assigned to them.
	 * 
	 * N.B. Must run linkBusiness() before linkAdis() for the business links to be
	 * populated.
	 */
	void linkAdis() {
		if (!this.indicesAssigned) {
			this.assignPaymentClearingIndices();
		}

		// N.B. link to employees performed in linkEmployees()

		// link to domestic suppliers
		// TODO: copy supplier link logic from businesses

		// TODO link to retail depositors (Households)
		// get total deposits for entire ADI industry
		float totalDepositBal = (float) Arrays.asList(this.adis).stream().mapToDouble(o -> o.getBsDepositsAtCall())
				.sum();
		totalDepositBal += (float) Arrays.asList(this.adis).stream().mapToDouble(o -> o.getBsDepositsTerm()).sum();
		// populate indices with relative amounts of each ADI
		ArrayList<Integer> shuffledIndices = new ArrayList<Integer>(this.households.length);
		for (int i = 0; i < this.adis.length; i++) {
			// calculate ratio of ADI to total
			float adiDepositBal = this.adis[i].getBsDepositsAtCall() + this.adis[i].getBsDepositsTerm();
			if (adiDepositBal > 0f) {
				// convert to indices, rounding up so we have at least enough
				int adiCustomerCount = (int) Math.ceil(adiDepositBal / totalDepositBal * this.households.length);
				shuffledIndices.addAll(Collections.nCopies(adiCustomerCount, i));
			}
		}
		// shuffle indices, and assign ADIs to Households
		shuffledIndices.trimToSize();
		Collections.shuffle(shuffledIndices, this.random);
		int nextShuffledIdx = 0;
		for (int i = 0; i < this.households.length; i++) {
			if (this.households[i].getBsLoans() > 0f) {
				// assign depositor Household to ADI
				this.adis[shuffledIndices.get(nextShuffledIdx++)].addRetailDepositor(this.households[i]);
				nextShuffledIdx = (nextShuffledIdx + 1) % shuffledIndices.size();
			}
		}
		// release memory
		shuffledIndices.clear();
		shuffledIndices = null;

		// link to commercial depositors (Businesses)
		for (Business business : this.businesses) {
			// already assigned
			AuthorisedDepositTakingInstitution adi = business.getAdi();
			adi.addCommercialDepositor(business);
		}
		for (AuthorisedDepositTakingInstitution adi : this.adis) {
			adi.trimCommercialDepositorList();
		}

		// TODO link to ADI investors (can't be more than 50% of capital)

		// link to Australian Government (payroll & income tax)
		for (int i = 0; i < this.adis.length; i++) {
			this.adis[i].setGovt(this.govt);
		}
	}

	/**
	 * Links the ForeignCountries to their Exporter trading partners.
	 * 
	 * The Business calibration stage didn't create any exporters, so we need to do
	 * that at this stage while linking them to countries.
	 */
	void linkForeignCountries() {
		if (!this.indicesAssigned) {
			this.assignPaymentClearingIndices();
		}

		// link to exporters

	}

	/**
	 * Links the AustralianGovernment to their welfare recipients and bond
	 * investors.
	 */
	private void linkGovernment() {
		if (!this.indicesAssigned) {
			this.assignPaymentClearingIndices();
		}

		// N.B. link to employees performed in linkEmployees()

		// link to welfare recipients
		ArrayList<Household> welfareRecipients = new ArrayList<Household>(this.households.length);
		for (int i = 0; i < this.households.length; i++) {
			if (this.households[i].getPnlUnemploymentBenefits() > 0f
					|| this.households[i].getPnlOtherSocialSecurityIncome() > 0f) {
				welfareRecipients.add(this.households[i]);
			}
		}
		welfareRecipients.trimToSize();
		this.govt.setWelfareRecipients(welfareRecipients);

		// link to bond investors (ADIs)
		ArrayList<AuthorisedDepositTakingInstitution> bondInvestors = new ArrayList<AuthorisedDepositTakingInstitution>(
				this.adis.length);
		for (int i = 0; i < this.adis.length; i++) {
			if (this.adis[i].getBsLoansGovernment() > 0f) {
				// ADI is a government investor
				bondInvestors.add(adis[i]);
			}
		}
		bondInvestors.trimToSize();
		this.govt.setBondInvestors(bondInvestors);

		// FIXME: link to businesses with government sales revenue

	}

	/**
	 * Links the RBA to the banks with their cash balances, and the government to
	 * pay its annual dividends to.
	 */
	private void linkRba() {
		if (!this.indicesAssigned) {
			this.assignPaymentClearingIndices();
		}

		// N.B. link to employees performed in linkEmployees()

		// link to major and regional banks
		ArrayList<AuthorisedDepositTakingInstitution> adiDepositors = new ArrayList<AuthorisedDepositTakingInstitution>(
				this.adis.length);
		for (int i = 0; i < this.adis.length; i++) {
			if (this.adis[i].getAdiCategory().equalsIgnoreCase("Major Bank")
					|| this.adis[i].getAdiCategory().equalsIgnoreCase("Other Domestic Bank")
					|| this.adis[i].getAdiCategory().equalsIgnoreCase("Foreign Bank")) {
				// ADI holds their cash in an ESA account with the RBA
				adiDepositors.add(this.adis[i]);
			}
		}
		adiDepositors.trimToSize();
		this.rba.setAdiDepositors(adiDepositors);

		// link to Australian Government (annual dividends)
		this.rba.setGovt(this.govt);
	}

	/**
	 * Calculates the ratios between the line items in the ABS 6530.0 household
	 * expenditure data.
	 * 
	 * @return
	 */
	private ArrayList<Float> calcAbs6530SpendRatios() {
		ArrayList<Float> ratios = new ArrayList<Float>(ABS_6530_0_SPEND_AMT.length);
		float total = 0f;
		for (int i = 0; i < ABS_6530_0_SPEND_AMT.length; i++) {
			total += ABS_6530_0_SPEND_AMT[i];
		}
		for (int i = 0; i < ABS_6530_0_SPEND_AMT.length; i++) {
			ratios.add(ABS_6530_0_SPEND_AMT[i] / total);
		}
		ratios.trimToSize();
		return ratios;
	}

	/**
	 * Calculates the ratio of the home and personal loan balances by ADI. This will
	 * be used as a PDF to assign Households' loans to ADIs.
	 * 
	 * @return
	 */
	private float[] calcAdiIndividualLoanRatios() {
		float[] ratios = new float[this.adis.length];
		float total = 0f;
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
	private float[] calcAdiDepositRatios() {
		float[] ratios = new float[this.adis.length];
		float total = 0f;
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
	private float[] calcAdiBusinessLoanRatios() {
		float[] ratios = new float[this.adis.length];
		float total = 0f;
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
	private List<List<Float>> calcBusinessDivisionWageRatios() {
		// initialise division totals
		Float[] divTotal = Collections.nCopies(CalibrateIndividuals.DIVISION_CODE_ARRAY.length, 0f)
				.toArray(Float[]::new);
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
		List<List<Float>> ratios = new ArrayList<List<Float>>(CalibrateIndividuals.DIVISION_CODE_ARRAY.length);
		for (int ratioIdx = 0; ratioIdx < CalibrateIndividuals.DIVISION_CODE_ARRAY.length; ratioIdx++) {
			ratios.add(new ArrayList<Float>(divisionBusinessCount[ratioIdx]));
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
