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

import xyz.struthers.lang.CustomMath;
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

	private static final int MAX_IMPORT_COUNTRIES = 3;
	private static final int MAX_EXPORT_COUNTRIES = 3;

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
	public static final String[] BUSINESS_SUPPLIER_DIV_CODE = { "A", "B", "C", "D", "E", "F", "I", "J", "L", "M", "N",
			"O" };
	public static final String[] ADI_SUPPLIER_DIV_CODE = { "C", "D", "E", "F", "I", "J", "L", "M", "N", "O", "P" };
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
	private boolean agentsLinked;

	/**
	 * 
	 */
	public CalibrateEconomy() {
		super();
		this.init();
	}

	private void init() {
		System.out.println("################## initialising economy ##################");

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
		this.agentsLinked = false;
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
		if (!this.agentsLinked) {
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
			// N.B. Must link Businesses before ADIs for the business links to be populated.
			this.linkAdis();
			this.linkForeignCountries();
			this.linkGovernment();
			this.linkRba();

			this.agentsLinked = true;
		}
	}

	/**
	 * Assign unique Payment Clearing Indices to every participating agent.
	 * 
	 * N.B. This is the order they MUST be in the Payment Clearing Vector arguments
	 * 
	 * By following this order exactly, it makes it far more efficient to deal with
	 * the output of the CPV, being able to determine which agent each index relates
	 * to simply by their relative position and hte number of each type of agent.
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
			}
		}
		// shuffle indices, and assign landlords to renting Households
		shuffledIndices.trimToSize();
		Collections.shuffle(shuffledIndices, this.random);
		int nextShuffledIdx = 0;
		for (int i = 0; i < this.households.length; i++) {
			if (this.households[i].getPnlRentExpense() > 0f) {
				// assign landlord to Household
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
	 * Individuals are already assigned to Industry Divisions, so should drill
	 * through the Household to get the relevant Individuals and link the employer
	 * to the Household based on the Individual's Division and wages or salary. It
	 * links the Employer to the Individual, then it's the responsibility of the
	 * Payment Clearing Vector algorithm's driver to consolidate these at a
	 * Household level.
	 * 
	 * MAYBE: refactor later to assign employees to employers who are based closer
	 * to the employee's LGA.
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
	 */
	private void linkBusinesses() {
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
			// String div = this.businesses[i].getIndustryCode();
			String div = String.valueOf(this.businesses[i].getIndustryDivisionCode());
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
			ArrayList<Float> supplierRatios = new ArrayList<Float>(BUSINESS_SUPPLIER_DIV_CODE.length);
			float sum = 0f;
			for (int supDivIdx = 0; supDivIdx < BUSINESS_SUPPLIER_DIV_CODE.length; supDivIdx++) {
				// assign suppliers
				String div = BUSINESS_SUPPLIER_DIV_CODE[supDivIdx];
				int nextIdx = nextDivIdx.get(div);
				Business supplier = this.businesses[shuffledSupplierIndicesByDiv.get(div).get(nextIdx)];
				customer.addDomesticSupplier(supplier);
				nextIdx = (nextIdx + 1) % shuffledSupplierIndicesByDiv.get(div).size();
				nextDivIdx.put(div, nextIdx);

				// calculate random numbers (not ratios yet)
				float rand = this.random.nextFloat();
				supplierRatios.add(rand);
				sum += rand;
			}
			// calculate & assign supplier ratios
			for (int supDivIdx = 0; supDivIdx < BUSINESS_SUPPLIER_DIV_CODE.length; supDivIdx++) {
				// normalise ratios so they sum to 100%
				supplierRatios.set(supDivIdx, supplierRatios.get(supDivIdx) / sum);
			}
			customer.trimDomesticSuppliersList();
			supplierRatios.trimToSize();
			customer.setSupplierRatios(supplierRatios);
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

		// link to foreign suppliers (i.e. foreign countries)
		/**
		 * Create PDF of each country's import volumes to Australia, by state. Loop
		 * through the Businesses, assigning each to a country based on their state.
		 */
		Map<String, List<ForeignCountry>> stateCountries = new HashMap<String, List<ForeignCountry>>(
				(int) Math.ceil(ForeignCountry.STATES.length / 0.75f));
		Map<String, List<Float>> stateCountryImportRatios = new HashMap<String, List<Float>>(
				(int) Math.ceil(ForeignCountry.STATES.length / 0.75f));
		Map<String, Float> stateImportTotal = new HashMap<String, Float>(
				(int) Math.ceil(ForeignCountry.STATES.length / 0.75));
		for (int stateIdx = 0; stateIdx < ForeignCountry.STATES.length; stateIdx++) {
			// get relevant foreign countries
			String state = ForeignCountry.STATES[stateIdx];
			stateCountries.put(state, Arrays.asList(this.countries).stream()
					.filter(o -> o.getAbsImportsToAustraliaForState(state) > 0f).collect(Collectors.toList()));

			// calculate state totals
			stateImportTotal.put(state, (float) stateCountries.get(state).stream()
					.mapToDouble(o -> o.getAbsImportsToAustraliaForState(state)).sum());
		}
		for (int stateIdx = 0; stateIdx < ForeignCountry.STATES.length; stateIdx++) {
			// calculate PDF from country ratios
			String state = ForeignCountry.STATES[stateIdx];
			float stateTotal = stateImportTotal.get(state);
			ArrayList<Float> ratios = new ArrayList<Float>(stateCountries.get(state).size());
			for (ForeignCountry country : stateCountries.get(state)) {
				float countryAmt = country.getAbsImportsToAustraliaForState(state);
				ratios.add(countryAmt / stateTotal);
			}
			ratios.trimToSize();
			stateCountryImportRatios.put(state, ratios);
		}
		for (int importerIdx = 0; importerIdx < this.businesses.length; importerIdx++) {
			// calculate country ratios within each state
			String state = this.businesses[importerIdx].getState();
			if (this.businesses[importerIdx].getForeignExpenses() > 0f) {
				int numCountries = this.random.nextInt(MAX_IMPORT_COUNTRIES);
				ArrayList<ForeignCountry> foreignSuppliers = new ArrayList<ForeignCountry>(numCountries);
				ArrayList<Float> foreignSupplierRatios = new ArrayList<Float>(numCountries);
				for (int i = 0; i < numCountries; i++) {
					if (stateCountryImportRatios.get(state) != null) {
						int countryIdx = CustomMath.sample(stateCountryImportRatios.get(state), this.random);
						foreignSuppliers.add(stateCountries.get(state).get(countryIdx));
						foreignSupplierRatios.add(1f / numCountries);
					}
				}
				foreignSuppliers.trimToSize();
				foreignSupplierRatios.trimToSize();
				this.businesses[importerIdx].setForeignSuppliers(foreignSuppliers);
				this.businesses[importerIdx].setForeignSupplierRatios(foreignSupplierRatios);
			}
		}
		// release memory
		for (String state : stateCountries.keySet()) {
			stateCountries.get(state).clear();
		}
		stateCountries.clear();
		stateCountries = null;
		for (String state : stateCountryImportRatios.keySet()) {
			stateCountryImportRatios.get(state).clear();
		}
		stateCountryImportRatios.clear();
		stateCountryImportRatios = null;
		stateImportTotal.clear();
		stateImportTotal = null;

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
	private void linkAdis() {
		if (!this.indicesAssigned) {
			this.assignPaymentClearingIndices();
		}

		// N.B. link to employees performed in linkEmployees()

		// link to domestic suppliers
		// get total domestic revenue by industry division
		Map<String, Float> totalDomesticRevenueByDiv = new HashMap<String, Float>(
				(int) Math.ceil(ADI_SUPPLIER_DIV_CODE.length / 0.75f));
		for (String div : ADI_SUPPLIER_DIV_CODE) {
			float divDomesticRevenue = (float) Arrays.asList(this.businesses).stream()
					.filter(o -> String.valueOf(o.getIndustryDivisionCode()).equals(div))
					.mapToDouble(o -> o.getSalesDomestic()).sum();
			totalDomesticRevenueByDiv.put(div, divDomesticRevenue);
		}
		// assume all ADIs have some domestic expenses
		// get businesses with domestic revenue, by division (for specified divisions)
		Set<String> domesticSupplierDivs = new HashSet<String>(Arrays.asList(ADI_SUPPLIER_DIV_CODE));
		// make a PDF of suppliers in each industry, with relative weights
		Map<String, ArrayList<Integer>> pdfSupplierIndicesByDiv = new HashMap<String, ArrayList<Integer>>(
				(int) Math.ceil(ADI_SUPPLIER_DIV_CODE.length / 0.75f));
		Map<String, ArrayList<Float>> pdfSupplierRatiosByDiv = new HashMap<String, ArrayList<Float>>(
				(int) Math.ceil(ADI_SUPPLIER_DIV_CODE.length / 0.75f));
		for (String div : ADI_SUPPLIER_DIV_CODE) {
			pdfSupplierIndicesByDiv.put(div, new ArrayList<Integer>(
					(int) Math.ceil(this.businesses.length / ADI_SUPPLIER_DIV_CODE.length * 2f / 0.75f)));
			pdfSupplierRatiosByDiv.put(div, new ArrayList<Float>(
					(int) Math.ceil(this.businesses.length / ADI_SUPPLIER_DIV_CODE.length * 2f / 0.75f)));
		}
		for (int i = 0; i < this.businesses.length; i++) {
			String div = String.valueOf(this.businesses[i].getIndustryDivisionCode());
			if (domesticSupplierDivs.contains(div)) {
				float domesticSales = this.businesses[i].getSalesDomestic();
				// can't just use indices here because only 86 ADIs. Use PDFs.
				float businessRatio = domesticSales / totalDomesticRevenueByDiv.get(div);
				pdfSupplierIndicesByDiv.get(div).add(i);
				pdfSupplierRatiosByDiv.get(div).add(businessRatio);
			}
		}
		// randomly assign suppliers to ADIs
		for (int customerIdx = 0; customerIdx < this.adis.length; customerIdx++) {
			AuthorisedDepositTakingInstitution customer = this.adis[customerIdx];
			ArrayList<Float> supplierRatios = new ArrayList<Float>(ADI_SUPPLIER_DIV_CODE.length);
			float sum = 0f;
			for (int supDivIdx = 0; supDivIdx < ADI_SUPPLIER_DIV_CODE.length; supDivIdx++) {
				String div = ADI_SUPPLIER_DIV_CODE[supDivIdx];
				// get index within Division from PDF
				int nextIdx = CustomMath.sample(pdfSupplierRatiosByDiv.get(div), this.random);
				// convert to the corresponding business index
				Business supplier = this.businesses[pdfSupplierIndicesByDiv.get(div).get(nextIdx)];
				customer.addDomesticSupplier(supplier);

				// calculate random numbers
				float rand = this.random.nextFloat();
				supplierRatios.add(rand);
				sum += rand;
			}
			// calculate & assign supplier ratios
			for (int i = 0; i < supplierRatios.size(); i++) {
				// normalise ratios so they sum to 100%
				supplierRatios.set(i, supplierRatios.get(i) / sum);
			}
			customer.trimDomesticSuppliersList();
			supplierRatios.trimToSize();
			customer.setDomesticSupplierRatios(supplierRatios);
		}
		// release memory
		for (String div : totalDomesticRevenueByDiv.keySet()) {
			totalDomesticRevenueByDiv.put(div, null);
		}
		totalDomesticRevenueByDiv.clear();
		totalDomesticRevenueByDiv = null;
		domesticSupplierDivs.clear();
		domesticSupplierDivs = null;
		for (String div : pdfSupplierIndicesByDiv.keySet()) {
			pdfSupplierIndicesByDiv.put(div, null);
		}
		pdfSupplierIndicesByDiv.clear();
		pdfSupplierIndicesByDiv = null;
		for (String div : pdfSupplierRatiosByDiv.keySet()) {
			pdfSupplierRatiosByDiv.put(div, null);
		}
		pdfSupplierRatiosByDiv.clear();
		pdfSupplierRatiosByDiv = null;

		// link to retail depositors (Households)
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

		// link to ADI investors (can't be more than 50% of capital)
		/**
		 * ADIs aren't allowed to invest more than 50% of their capital with a single
		 * counterparty. The largest ratio of investments to total regulatory capital is
		 * 754% by Pulse Credit Union. Limiting the ratios so that no more than 10% of
		 * investment balances can be with a single counterparty is a reasonable proxy
		 * for ADIs complying with investment concentration restrictions.
		 * 
		 * ALGORITHM THINKING:<br>
		 * $335bn in ADI Repo Deposits (liability)<br>
		 * $97bn in ADI loans (asset)<br>
		 * $238bn remaining to be allocated<br>
		 * $51bn investments by non-major-bank ADIs. Cross-multiply matrix approach.<br>
		 * $187bn investments remaining must be by major banks<br>
		 * Allocate the $187bn per the ratio of the major banks' investment balances<br>
		 * 
		 * So to turn this into code:<br>
		 * 1. Calculate the ratio of the major banks' investment balances
		 * (majorBankInvestmentRatio).<br>
		 * 2. Sum the $335bn in ADI Repo Deposits (totalAdiRepoEligibleDeposits).<br>
		 * 3. Subtract from the ADI repo deposits the $97bn in ADI loans and $51bn
		 * non-major-bank investments.<br>
		 * 4. Split the remaining $187bn to the major banks per their investment balance
		 * ratio.<br>
		 * 5. For each ADI, sum the ADI loans and investments (per steps 3-4).<br>
		 * 6. For each ADI, calculate the ratio of the investment balances of the other
		 * ADIs, excluding the major banks. Assign the ADI's ADI Repo Deposits to the
		 * other ADIs per these calculated ratios (store the amounts - not the ratios).
		 * Store pointers to the ADIs at the same time.<br>
		 * 7. Take the Government Loans, add in the remaining investment amounts for the
		 * major banks, and assign these as Bond Investors in the AustralianGovernment
		 * agent.
		 */
		// 1. Calculate the ratio of the major banks' investment balances
		float majorBankInvestmentBalTotal = (float) Arrays.asList(this.adis).stream()
				.filter(o -> o.getAdiCategory().equals("Major Bank")).mapToDouble(o -> o.getBsInvestments()).sum();
		ArrayList<Float> majorBankInvestmentRatio = new ArrayList<Float>(Collections.nCopies(this.adis.length, 0f));
		for (int adiIdx = 0; adiIdx < this.adis.length; adiIdx++) {
			if (this.adis[adiIdx].getAdiCategory().equals("Major Bank")) {
				majorBankInvestmentRatio.set(adiIdx,
						this.adis[adiIdx].getBsInvestments() / majorBankInvestmentBalTotal);
			}
		}
		// 2. Sum the $335bn in ADI Repo Deposits
		float totalAdiRepoEligibleDeposits = (float) Arrays.asList(this.adis).stream()
				.mapToDouble(o -> o.getBsDepositsAdiRepoEligible()).sum();
		// 3. Subtract from the ADI repo deposits the $97bn in ADI loans and $51bn
		// non-major-bank investments.
		float totalAdiLoans = (float) Arrays.asList(this.adis).stream().mapToDouble(o -> o.getBsLoansADI()).sum();
		float totalInvestmentsNonMajorBank = (float) Arrays.asList(this.adis).stream()
				.filter(o -> !o.getAdiCategory().equals("Major Bank")).mapToDouble(o -> o.getBsInvestments()).sum();
		// 4. Split the remaining $187bn to the major banks per their investment balance
		// ratio.
		float remainingMajorBankInvestments = totalAdiRepoEligibleDeposits - totalAdiLoans
				- totalInvestmentsNonMajorBank;
		ArrayList<Float> interAdiInvestments = new ArrayList<Float>(Collections.nCopies(this.adis.length, 0f));
		for (int adiIdx = 0; adiIdx < this.adis.length; adiIdx++) {
			if (this.adis[adiIdx].getAdiCategory().equals("Major Bank")) {
				interAdiInvestments.set(adiIdx, remainingMajorBankInvestments * majorBankInvestmentRatio.get(adiIdx));
			}
		}
		ArrayList<Float> majorBankInvestments = new ArrayList<Float>(interAdiInvestments);
		// 5. For each ADI, sum the ADI loans and investments (per steps 3-4).
		for (int adiIdx = 0; adiIdx < this.adis.length; adiIdx++) {
			float adiBal = this.adis[adiIdx].getBsLoansADI();
			if (this.adis[adiIdx].getAdiCategory().equals("Major Bank")) {
				adiBal += interAdiInvestments.get(adiIdx);
			} else {
				adiBal += this.adis[adiIdx].getBsInvestments();
			}
			interAdiInvestments.set(adiIdx, adiBal);
		}
		// 6. For each ADI, calculate the ratio of the investment balances of the other
		// ADIs, excluding the major banks. Assign the ADI's ADI Repo Deposits to the
		// other ADIs per these calculated ratios (store the amounts - not the ratios).
		// Store pointers to the ADIs at the same time.
		for (int adiIdx = 0; adiIdx < this.adis.length; adiIdx++) {
			ArrayList<Float> otherAdiRatios = new ArrayList<Float>(Collections.nCopies(this.adis.length, 0f));
			// calculate total
			float otherAdiTotal = 0f;
			for (int otherAdiIdx = 0; otherAdiIdx < this.adis.length; otherAdiIdx++) {
				if (adiIdx != otherAdiIdx) {
					otherAdiTotal += interAdiInvestments.get(otherAdiIdx);
				}
			}
			// create links between ADIs
			ArrayList<AuthorisedDepositTakingInstitution> adiInvestors = new ArrayList<AuthorisedDepositTakingInstitution>(
					this.adis.length - 1);
			ArrayList<Float> adiInvestorAmounts = new ArrayList<Float>(this.adis.length - 1);
			for (int otherAdiIdx = 0; otherAdiIdx < this.adis.length; otherAdiIdx++) {
				if (adiIdx != otherAdiIdx) {
					// calculate ratios and amounts
					float liability = this.adis[adiIdx].getBsDepositsAdiRepoEligible()
							* interAdiInvestments.get(otherAdiIdx) / otherAdiTotal;
					adiInvestorAmounts.add(liability);
					adiInvestors.add(this.adis[otherAdiIdx]);
				}
			}
			adiInvestors.trimToSize();
			adiInvestorAmounts.trimToSize();
			this.adis[adiIdx].setAdiInvestors(adiInvestors);
			this.adis[adiIdx].setAdiInvestorAmounts(adiInvestorAmounts);
		}
		// 7. Take the Government Loans, add in the remaining investment amounts for the
		// major banks, and assign these as Bond Investors in the AustralianGovernment
		// agent.
		ArrayList<AuthorisedDepositTakingInstitution> bondInvestors = new ArrayList<AuthorisedDepositTakingInstitution>();
		ArrayList<Float> bondInvestorAmounts = new ArrayList<Float>();
		for (int adiIdx = 0; adiIdx < this.adis.length; adiIdx++) {
			float bondAmount = majorBankInvestments.get(adiIdx);
			bondAmount += this.adis[adiIdx].getBsLoansGovernment();
			if (bondAmount > 0f) {
				// add bond investor amount & link to govt agent
				bondInvestors.add(this.adis[adiIdx]);
				bondInvestorAmounts.add(bondAmount);
			}
		}
		bondInvestors.trimToSize();
		bondInvestorAmounts.trimToSize();
		this.govt.setBondInvestors(bondInvestors);
		this.govt.setBondInvestorAmounts(bondInvestorAmounts);

		// link major banks to the RBA for Committed Liquidity Facility fees
		float clfTotal = this.rba.getPnlCommittedLiquidityFacilityFees();
		float majorBankDepositsTotal = (float) Arrays.asList(this.adis).stream()
				.filter(o -> o.getAdiCategory().equals("Major Bank")).mapToDouble(o -> o.getBsDepositsAtCall()).sum();
		majorBankDepositsTotal += (float) Arrays.asList(this.adis).stream()
				.filter(o -> o.getAdiCategory().equals("Major Bank")).mapToDouble(o -> o.getBsDepositsTerm()).sum();
		majorBankDepositsTotal += (float) Arrays.asList(this.adis).stream()
				.filter(o -> o.getAdiCategory().equals("Major Bank")).mapToDouble(o -> o.getBsDepositsAdiRepoEligible())
				.sum();
		List<AuthorisedDepositTakingInstitution> majorBanks = Arrays.asList(this.adis).stream()
				.filter(o -> o.getAdiCategory().equals("Major Bank")).collect(Collectors.toList());
		for (AuthorisedDepositTakingInstitution majorBank : majorBanks) {
			float bankDeposits = majorBank.getBsDepositsAtCall() + majorBank.getBsDepositsTerm()
					+ majorBank.getBsDepositsAdiRepoEligible();
			float ratio = bankDeposits / majorBankDepositsTotal;
			float clfExpense = ratio * clfTotal;
			majorBank.setPnlCommittedLiquidityFacilityFees(clfExpense);
			// take CLF fees from the Other Expenses category
			float otherExpense = majorBank.getPnlOtherExpenses();
			majorBank.setPnlOtherExpenses(Math.max(otherExpense - clfExpense, 0f));
		}

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
	private void linkForeignCountries() {
		if (!this.indicesAssigned) {
			this.assignPaymentClearingIndices();
		}

		// link to exporters
		/**
		 * Create PDF of each country's export volumes from Australia, by state. Loop
		 * through the Businesses, assigning each to a country based on their state.
		 */
		Map<String, List<ForeignCountry>> stateCountries = new HashMap<String, List<ForeignCountry>>(
				(int) Math.ceil(ForeignCountry.STATES.length / 0.75f));
		Map<String, List<Float>> stateCountryExportRatios = new HashMap<String, List<Float>>(
				(int) Math.ceil(ForeignCountry.STATES.length / 0.75f));
		Map<String, Float> stateExportTotal = new HashMap<String, Float>(
				(int) Math.ceil(ForeignCountry.STATES.length / 0.75));
		for (int stateIdx = 0; stateIdx < ForeignCountry.STATES.length; stateIdx++) {
			// get relevant foreign countries
			String state = ForeignCountry.STATES[stateIdx];
			stateCountries.put(state, Arrays.asList(this.countries).stream()
					.filter(o -> o.getAbsExportsFromAustraliaForState(state) > 0f).collect(Collectors.toList()));

			// calculate state totals
			stateExportTotal.put(state, (float) stateCountries.get(state).stream()
					.mapToDouble(o -> o.getAbsExportsFromAustraliaForState(state)).sum());
		}
		for (int stateIdx = 0; stateIdx < ForeignCountry.STATES.length; stateIdx++) {
			// calculate PDF from country ratios
			String state = ForeignCountry.STATES[stateIdx];
			float stateTotal = stateExportTotal.get(state);
			ArrayList<Float> ratios = new ArrayList<Float>(stateCountries.get(state).size());
			for (ForeignCountry country : stateCountries.get(state)) {
				float countryAmt = country.getAbsExportsFromAustraliaForState(state);
				ratios.add(countryAmt / stateTotal);
			}
			ratios.trimToSize();
			stateCountryExportRatios.put(state, ratios);
		}
		for (int exporterIdx = 0; exporterIdx < this.businesses.length; exporterIdx++) {
			// calculate country ratios within each state
			String state = this.businesses[exporterIdx].getState();
			if (this.businesses[exporterIdx].getSalesForeign() > 0f) {
				int numCountries = this.random.nextInt(MAX_EXPORT_COUNTRIES);
				ArrayList<ForeignCountry> foreignCustomers = new ArrayList<ForeignCountry>(numCountries);
				ArrayList<Float> foreignCustomerRatios = new ArrayList<Float>(numCountries);
				for (int i = 0; i < numCountries; i++) {
					int countryIdx = CustomMath.sample(stateCountryExportRatios.get(state), this.random);
					ForeignCountry country = stateCountries.get(state).get(countryIdx);
					foreignCustomers.add(country);
					foreignCustomerRatios.add(1f / numCountries);

					// add exporter to the ForeignCountry too
					country.addExporter(this.businesses[exporterIdx]);
				}
				foreignCustomers.trimToSize();
				foreignCustomerRatios.trimToSize();
				this.businesses[exporterIdx].setForeignSuppliers(foreignCustomers);
				this.businesses[exporterIdx].setForeignSupplierRatios(foreignCustomerRatios);
			}
		}
		// release memory
		stateExportTotal.clear();
		stateExportTotal = null;

		// link to households with foreign income
		/**
		 * Use the PDF of each country's export volumes from Australia, by state created
		 * above. Loop through the Households with foreign income, assigning each to a
		 * country based on their state.
		 */
		for (int householdIdx = 0; householdIdx < this.households.length; householdIdx++) {
			if (this.households[householdIdx].getPnlForeignIncome() > 0f) {
				// use country ratios within each state
				String state = this.households[householdIdx].getState();

				// assume foreign income is from a single country (e.g. a job or a pension)
				int countryIdx = CustomMath.sample(stateCountryExportRatios.get(state), this.random);
				ForeignCountry foreignIncomeSource = stateCountries.get(state).get(countryIdx);

				// link household and country
				foreignIncomeSource.addHousehold(this.households[householdIdx]);
			}
		}
		for (ForeignCountry country : this.countries) {
			country.trimHouseholdsListToSize();
		}
		// release memory
		for (String state : stateCountries.keySet()) {
			stateCountries.get(state).clear();
		}
		stateCountries.clear();
		stateCountries = null;
		for (String state : stateCountryExportRatios.keySet()) {
			stateCountryExportRatios.get(state).clear();
		}
		stateCountryExportRatios.clear();
		stateCountryExportRatios = null;
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

		// N.B. link to bond investors (ADIs) is done in the linkAdis() method
		/*
		 * ArrayList<AuthorisedDepositTakingInstitution> bondInvestors = new
		 * ArrayList<AuthorisedDepositTakingInstitution>( this.adis.length); for (int i
		 * = 0; i < this.adis.length; i++) { if (this.adis[i].getBsLoansGovernment() >
		 * 0f) { // ADI is a government investor bondInvestors.add(adis[i]); } }
		 * bondInvestors.trimToSize(); this.govt.setBondInvestors(bondInvestors);
		 */

		// link to businesses with government sales revenue
		List<Business> govtSuppliers = Arrays.asList(this.businesses).stream().filter(o -> o.getSalesGovernment() > 0f)
				.collect(Collectors.toList());
		ArrayList<Business> governmentSuppliers = new ArrayList<Business>(govtSuppliers);
		this.govt.setGovernmentSuppliers(governmentSuppliers);
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
