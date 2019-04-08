/**
 * 
 */
package xyz.struthers.rhul.ham.data;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import xyz.struthers.rhul.ham.agent.Business;
import xyz.struthers.rhul.ham.config.Properties;
import xyz.struthers.rhul.ham.process.AustralianEconomy;

/**
 * 
 * @author Adam Struthers
 * @since 12-Feb-2019
 */
@Component
@Scope(value = "singleton")
public class CalibrateBusinesses {

	private static final boolean DEBUG = false;
	private static final boolean DEBUG_ZEROS = false;
	private static final boolean DEBUG_ZEROS_INFINITY = false;

	// constants
	public static final String RBA_E1_DATE_STRING = "Jun-2018";
	public static final String RBA_E1_BUSINESS_BANK_DEPOSITS = "BSPNSPNFAD";
	public static final String RBA_E1_BUSINESS_FOREIGN_EQUITIES = "BSPNSPNFAF";
	public static final String RBA_E1_BUSINESS_TOTAL_FINANCIAL_ASSETS = "BSPNSPNFAT";

	public static final String ABS8155_YEAR = "2016-17";
	public static final String ABS8155_TITLE_EMPLOYMENT = "Employment at end of June";
	public static final String ABS8155_TITLE_WAGES = "Wages and salaries";
	public static final String ABS8155_TITLE_SALES = "Sales and service income";
	public static final String ABS8155_TITLE_INCOME = "Total income";
	public static final String ABS8155_TITLE_EXPENSES = "Total expenses";

	public static final String ABS8165_TITLE_EMPLOYMENT_1 = "Non Employing";
	public static final String ABS8165_TITLE_EMPLOYMENT_2 = "1-19 Employees";
	public static final String ABS8165_TITLE_EMPLOYMENT_3 = "20-199 Employees";
	public static final String ABS8165_TITLE_EMPLOYMENT_4 = "200+ Employees";
	public static final int ABS8165_NUM_CLASS_CODES = 497;
	public static final int ABS8165_NUM_LGAS = 554;

	private static final float MILLION = 1000000f;
	private static final float THOUSAND = 1000f;
	private static final float EPSILON = 0.1f; // to round business counts so the integer sums match

	// beans
	private Properties properties;
	private CalibrationData data;
	private CalibrationDataBusiness businessData;
	private AreaMapping area;
	private AustralianEconomy economy;

	// field variables
	private List<Business> businessAgents;
	protected int[] businessTypeCount; // number of businesses of each type that were instantiated

	// data sets
	/**
	 * AU Bal Sht totals<br>
	 * Keys: Series, Date
	 */
	private Map<String, Map<Date, Float>> rbaE1;
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
	 * Business size by division<br>
	 * Keys: year, column title, size, industry
	 */
	private Map<String, Map<String, Map<String, Map<String, Float>>>> abs8155_0Table5;
	/**
	 * States by division<br>
	 * Keys: year, column title, state, industry
	 */
	private Map<String, Map<String, Map<String, Map<String, Float>>>> abs8155_0Table6;
	/**
	 * Count by state, industry & employment range<br>
	 * Keys: employment range, state, industry class code
	 */
	private Map<String, Map<String, Map<String, Float>>> abs8165_0StateEmployment;
	/**
	 * Count by LGA, industry & employment range<br>
	 * Keys: employment range, state acronym, LGA code, industry division code
	 */
	private Map<String, Map<String, Map<String, Map<String, Float>>>> abs8165_0LgaEmployment;
	/**
	 * ATO Fine Industry Detailed P&L and Bal Sht<br>
	 * Keys: column title, fine industry code
	 */
	private Map<String, Map<String, Float>> atoCompanyTable4a;
	/**
	 * Industry Code Total P&L<br>
	 * Keys: column title, business industry code
	 */
	private Map<String, Map<String, Float>> atoCompanyTable4b;

	/**
	 * 
	 */
	public CalibrateBusinesses() {
		super();
		this.init();
	}

	@PostConstruct
	private void init() {
		this.businessAgents = null;
		this.businessTypeCount = null;

		this.rbaE1 = null;
		this.abs1292_0_55_002ANZSIC = null;
		this.abs8155_0Table5 = null;
		this.abs8155_0Table6 = null;
		this.abs8165_0StateEmployment = null;
		this.abs8165_0LgaEmployment = null;
	}

	/**
	 * Deletes all the field variables, freeing up memory.
	 * 
	 * Does not do a deep delete because most objects are passed to other classes by
	 * reference, and the other classes will probably still need to refer to them.
	 */
	@PreDestroy
	public void close() {
		this.init(); // set all the pointers to null
	}

	/**
	 * Creates all the Business agents in the model, and stores them in the
	 * AustralianEconomy. Does not link businesses with employees, banks, suppliers,
	 * etc.
	 * 
	 * =============<br>
	 * = ALGORITHM =<br>
	 * =============<br>
	 * 
	 * ------------------------------------------------------------------------<br>
	 * PART A: INDUSTRY DETAIL WITH MODERATELY DETAILED COMPANY P&L AND BAL SHT<br>
	 * ------------------------------------------------------------------------<br>
	 * 1. ATO Company Table 4A: First calculate per-company figures, then calculate
	 * the ratios between the various P&L line items.
	 * 
	 * Also calculate ratio of wages per company against average wages for that
	 * industry division. This allows us to modify the industry division total
	 * employment figures in ABS 8155.0 so they're calibrated to each company's
	 * financial situation. Assumes that within each industry division all companies
	 * pay the same rate, which is unrealistic but good enough for the sake of this
	 * model and its stated aims and degree of accuracy.
	 * 
	 * 2. ATO Company Table 4B: Calculate per-company figures, then multiply by the
	 * ratios from 4A to get a more detailed P&L. This gives per-company P&L for 574
	 * industries. Calculate the ratio of the number of companies in each of the
	 * relevant industry codes for each of the 18 industry divisions. This allows us
	 * to break the ABS data for 18 industries down into 574 industry codes.
	 * 
	 * N.B. Uses the ratio of assets and liabilities to income to calibrate the
	 * balance sheet based on the P&L statement.
	 * 
	 * We now have representative company P&L and Bal Shts for each of the 574
	 * industry codes.
	 * 
	 * ------------------------------------------------------------<br>
	 * PART B: INDUSTRY BUSINESS FINANCIALS SPLIT BY SIZE AND STATE<br>
	 * ------------------------------------------------------------<br>
	 * 3. ABS 8155.0 Table 6: Calculate ratios between states (for employment count,
	 * wages and sales) by industry.
	 * 
	 * 4. ABS 8155.0 Table 5: Use the ratios from Table 6 to split Size by State,
	 * assuming that all income is split in the same ratio as sales, and all
	 * expenses are split in the same ratio as wages. Calculate gross profit by
	 * subtracting total expenses from total income.
	 * 
	 * We now have total $ amounts by state, industry, and size.<br>
	 * 8 states x 18 industries x 3 sizes = 432 categories.
	 * 
	 * ----------------------------------------------------------<br>
	 * PART C: ADJUST INDUSTRY P&L AND BAL SHTS BY SIZE AND STATE<br>
	 * ----------------------------------------------------------<br>
	 * 
	 * 5. ABS 8165.0 LGA Employment Range: Get the number of businesses per state /
	 * industry / size then divide the dollar amounts from 8155.0 to determine the
	 * mean dollars per business. Divide the number of employees from 8155.0 by the
	 * number of businesses in 8165.0 to get the mean employees per business by
	 * state / industry / size. Also calculate a total national mean dollars and
	 * employees per business.
	 * 
	 * 6. Divide each state / industry / size's figures by the national average to
	 * produce a multiplier for each category combination.
	 * 
	 * 7. RBA E1: For the national total balance sheet, calculate the ratio of
	 * business bank deposits to total financial assets, and business foreign
	 * equities to total financial assets. Below we will assume that total financial
	 * assets and current assets are equivalent, and use these ratios to estimate
	 * the bank deposits and foreign equities held by businesses. We further assume
	 * that only large businesses hold foreign equities (i.e. multi-national
	 * corporations and large investment firms).
	 * 
	 * 8. For each state / industry / size, multiply the ATO industry figures by
	 * this multiplier to calibrate the ATO company P&L and Bal Shts so they're now
	 * representative of businesses per state / industry / size.
	 * 
	 * We now have employee count and detailed P&L and Bal Sht by state, industry
	 * code, and size.<br>
	 * 8 states x 574 industry codes x 3 sizes = 13,776 distinct Business agents
	 * 
	 * -----------------------------------------------------<br>
	 * PART D: ASSIGN BUSINESSES TO LGA BY SIZE AND INDUSTRY<br>
	 * -----------------------------------------------------<br>
	 * 
	 * 9. ABS 8165.0 State Employment Range: Load the data into a multi-dimensional
	 * array, and add Industry Division as an extra key to map by. For each
	 * combination of state and size, calculate the ratio of the number of
	 * businesses in each industry code within each industry division.
	 * 
	 * 10. ABS 8165.0 LGA Employment Range: For each LGA, size and division,
	 * multiply the number of businesses by the industry code ratios from ABS 8165.0
	 * State Employment Range. Round to integers, adding or subtracting epsilon and
	 * repeating until the difference in the total number is either equal to zero or
	 * grows larger again. This gives us the count of businesses by state, LGA, size
	 * and industry code.
	 * 
	 * Counts how many times each agent is instantiated when assigned to LGAs. This
	 * will allow me to do a frequency histogram to show the degree of heterogeneity
	 * among Business agents.
	 * 
	 * 554 LGAs x 3 sizes x 574 industry codes = 953,988 distinct Business agents
	 * 
	 * Deal with exporters later when linking the agents to create the network
	 * topology.
	 */
	public void createBusinessAgents() {
		// get just the relevant business data from the CalibrationData
		System.out.println("this.data: " + this.data);
		this.rbaE1 = this.data.getRbaE1();
		this.abs1292_0_55_002ANZSIC = this.data.getAbs1292_0_55_002ANZSIC();
		this.abs8155_0Table5 = this.businessData.getAbs8155_0Table5();
		this.abs8155_0Table6 = this.businessData.getAbs8155_0Table6();
		this.abs8165_0StateEmployment = this.businessData.getAbs8165_0StateEmployment();
		this.abs8165_0LgaEmployment = this.businessData.getAbs8165_0LgaEmployment();
		this.atoCompanyTable4a = this.businessData.getAtoCompanyTable4a();
		this.atoCompanyTable4b = this.businessData.getAtoCompanyTable4b();

		// create businesses
		this.businessAgents = new ArrayList<Business>();

		/*
		 * -------------------------------------------------------------------------
		 * PART A: INDUSTRY DETAIL WITH MODERATELY DETAILED COMPANY P&L AND BAL SHT
		 * -------------------------------------------------------------------------
		 * 
		 * 1. ATO Company Table 4A: First calculate per-company figures, then calculate
		 * the ratios between the various P&L line items.
		 * 
		 * Also calculate ratio of wages per company against average wages for that
		 * industry division. This allows us to modify the industry division total
		 * employment figures in ABS 8155.0 so they're calibrated to each company's
		 * financial situation. Assumes that within each industry division all companies
		 * pay the same rate, which is unrealistic but good enough for the sake of this
		 * model and its stated aims and degree of accuracy.
		 */
		// System.out.println("Step 1: " + new Date(System.currentTimeMillis()));

		Set<String> fineIndustryKeySet = new HashSet<String>(this.atoCompanyTable4a.get("Total Income3 no.").keySet());
		int numFineIndustryKeys = fineIndustryKeySet.size();
		Map<String, Integer> fineIndustryKeyIndex = new HashMap<String, Integer>(numFineIndustryKeys);
		String[] industryGroupCodes = fineIndustryKeySet.stream().toArray(String[]::new);

		// company wages multiplier
		Map<String, String> industryDivisionMap4A = new HashMap<String, String>(198); // maps ANZSIC Group Code to
																						// Division Code
		Set<String> industryDivisionsList4A = new HashSet<String>();
		for (String industryGroupCode : industryGroupCodes) {
			String div = this.abs1292_0_55_002ANZSIC.get("Group Code to Division Code").get(industryGroupCode);
			industryDivisionMap4A.put(industryGroupCode, div);
			industryDivisionsList4A.add(div);
		}
		String[] industryDivisions4A = industryDivisionsList4A.stream().toArray(String[]::new);
		Map<String, Float> wagesPerIndustryDivision = new HashMap<String, Float>(industryDivisions4A.length);
		Map<String, Integer> numberOfGroupsInDivision = new HashMap<String, Integer>(industryDivisions4A.length);
		for (String division : industryDivisions4A) {
			wagesPerIndustryDivision.put(division, 0f);
			numberOfGroupsInDivision.put(division, 0);
		}
		float[] wagesDivisionMultiplierPerGroup = new float[numFineIndustryKeys];

		// company financials
		float[] totalIncomePerCompany = new float[numFineIndustryKeys];
		float[] salesRatio = new float[numFineIndustryKeys];
		float[] interestIncomeRatio = new float[numFineIndustryKeys];
		float[] rentIncomeRatio = new float[numFineIndustryKeys];
		float[] governmentIncomeRatio = new float[numFineIndustryKeys];
		float[] foreignIncomeRatio = new float[numFineIndustryKeys];

		float[] totalExpensePerCompany = new float[numFineIndustryKeys];
		float[] costOfSalesRatio = new float[numFineIndustryKeys];
		float[] rentLeaseExpenseRatio = new float[numFineIndustryKeys];
		float[] interestExpenseRatio = new float[numFineIndustryKeys];
		float[] foreignInterestExpenseRatio = new float[numFineIndustryKeys];
		float[] depreciationExpenseRatio = new float[numFineIndustryKeys];
		float[] salaryWageRatio = new float[numFineIndustryKeys];
		float[] salaryWagePerGroup = new float[numFineIndustryKeys];

		float[] totalAssetsPerCompany = new float[numFineIndustryKeys];
		float[] totalAssetsToIncomeRatio = new float[numFineIndustryKeys];
		float[] currentAssetsRatio = new float[numFineIndustryKeys];

		float[] totalLiabilitiesPerCompany = new float[numFineIndustryKeys];
		float[] totalLiabilitiesToIncomeRatio = new float[numFineIndustryKeys];
		float[] tradeCreditorsRatio = new float[numFineIndustryKeys];
		float[] currentLiabilitiesRatio = new float[numFineIndustryKeys];
		float[] debtRatio = new float[numFineIndustryKeys];

		for (int i = 0; i < numFineIndustryKeys; i++) {
			int count = 0;
			float amount = 0f;
			float amountPerCompany = 0f;
			String key = industryGroupCodes[i];
			fineIndustryKeyIndex.put(key, i); // so we can look up the index cheaply in the next step of the algorithm

			// TOTAL INCOME
			count = Math.round(this.atoCompanyTable4a.get("Total Income3 no.").get(key));
			amount = this.atoCompanyTable4a.get("Total Income3 $").get(key);
			totalIncomePerCompany[i] = count == 0 ? 0f : amount / count;

			if (DEBUG_ZEROS) {
				System.out.println("totalIncomePerCompany[" + i + "]: " + totalIncomePerCompany[i]);
			}

			// Sales
			count = Math.round(this.atoCompanyTable4a.get("Other sales of goods and services no.").get(key));
			amount = this.atoCompanyTable4a.get("Other sales of goods and services $").get(key);
			amountPerCompany = count == 0 ? 0f : amount / count;
			salesRatio[i] = totalIncomePerCompany[i] == 0 ? 0f : amountPerCompany / totalIncomePerCompany[i];

			// Interest Income
			count = Math.round(this.atoCompanyTable4a.get("Gross Interest no.").get(key));
			amount = this.atoCompanyTable4a.get("Gross Interest $").get(key);
			amountPerCompany = count == 0 ? 0f : amount / count;
			interestIncomeRatio[i] = totalIncomePerCompany[i] == 0 ? 0f : amountPerCompany / totalIncomePerCompany[i];

			// Rent Income
			count = Math
					.round(this.atoCompanyTable4a.get("Gross rent and other leasing and hiring income no.").get(key));
			amount = this.atoCompanyTable4a.get("Gross rent and other leasing and hiring income $").get(key);
			amountPerCompany = count == 0 ? 0f : amount / count;
			rentIncomeRatio[i] = totalIncomePerCompany[i] == 0 ? 0f : amountPerCompany / totalIncomePerCompany[i];

			// Government Income
			count = Math.round(this.atoCompanyTable4a.get("Assessable government industry payments no.").get(key));
			amount = this.atoCompanyTable4a.get("Assessable government industry payments $").get(key);
			amountPerCompany = count == 0 ? 0f : amount / count;
			governmentIncomeRatio[i] = totalIncomePerCompany[i] == 0 ? 0f : amountPerCompany / totalIncomePerCompany[i];

			// Foreign Income
			count = Math.round(this.atoCompanyTable4a.get("Gross foreign income no.").get(key));
			amount = this.atoCompanyTable4a.get("Gross foreign income $").get(key);
			amountPerCompany = count == 0 ? 0f : amount / count;
			foreignIncomeRatio[i] = totalIncomePerCompany[i] == 0 ? 0f : amountPerCompany / totalIncomePerCompany[i];

			// TOTAL EXPENSES
			count = Math.round(this.atoCompanyTable4a.get("Total expenses no.").get(key));
			amount = this.atoCompanyTable4a.get("Total expenses $").get(key);
			totalExpensePerCompany[i] = count == 0 ? 0f : amount / count;

			// Cost of Sales
			count = Math.round(this.atoCompanyTable4a.get("Cost of sales no.").get(key));
			amount = this.atoCompanyTable4a.get("Cost of sales $").get(key);
			amountPerCompany = count == 0 ? 0f : amount / count;
			costOfSalesRatio[i] = totalIncomePerCompany[i] == 0 ? 0f : amountPerCompany / totalIncomePerCompany[i];

			// Rent and Lease Expense
			count = Math.round(this.atoCompanyTable4a.get("Lease expenses within Australia no.").get(key))
					+ Math.round(this.atoCompanyTable4a.get("Rent expenses no.").get(key));
			amount = this.atoCompanyTable4a.get("Lease expenses within Australia $").get(key)
					+ this.atoCompanyTable4a.get("Rent expenses $").get(key);
			amountPerCompany = count == 0 ? 0f : amount / count;
			rentLeaseExpenseRatio[i] = totalIncomePerCompany[i] == 0 ? 0f : amountPerCompany / totalIncomePerCompany[i];

			// Interest Expense
			count = Math.round(this.atoCompanyTable4a.get("Interest expenses within Australia no.").get(key));
			amount = this.atoCompanyTable4a.get("Interest expenses within Australia $").get(key);
			amountPerCompany = count == 0 ? 0f : amount / count;
			interestExpenseRatio[i] = totalIncomePerCompany[i] == 0 ? 0f : amountPerCompany / totalIncomePerCompany[i];

			// Foreign Interest Expense
			count = Math.round(this.atoCompanyTable4a.get("Interest expenses overseas no.").get(key));
			amount = this.atoCompanyTable4a.get("Interest expenses overseas $").get(key);
			amountPerCompany = count == 0 ? 0f : amount / count;
			foreignInterestExpenseRatio[i] = totalIncomePerCompany[i] == 0 ? 0f
					: amountPerCompany / totalIncomePerCompany[i];

			// Depreciation Expense
			count = Math.round(this.atoCompanyTable4a.get("Depreciation expenses no.").get(key));
			amount = this.atoCompanyTable4a.get("Depreciation expenses $").get(key);
			amountPerCompany = count == 0 ? 0f : amount / count;
			depreciationExpenseRatio[i] = totalIncomePerCompany[i] == 0 ? 0f
					: amountPerCompany / totalIncomePerCompany[i];

			// Salaries and Wages
			count = Math.round(this.atoCompanyTable4a.get("Total salary and wage expenses no.").get(key));
			amount = this.atoCompanyTable4a.get("Total salary and wage expenses $").get(key);
			amountPerCompany = count == 0 ? 0f : amount / count;
			salaryWagePerGroup[i] = amountPerCompany;
			salaryWageRatio[i] = totalIncomePerCompany[i] == 0 ? 0f : amountPerCompany / totalIncomePerCompany[i];

			// sum wages by Division so we can get the ratios later
			String thisDivision = industryDivisionMap4A.get(key);
			numberOfGroupsInDivision.put(thisDivision, numberOfGroupsInDivision.get(thisDivision) + 1);
			wagesPerIndustryDivision.put(thisDivision, wagesPerIndustryDivision.get(thisDivision) + amountPerCompany);

			// TOTAL ASSETS
			count = Math.round(this.atoCompanyTable4a.get("Total assets no.").get(key));
			amount = this.atoCompanyTable4a.get("Total assets $").get(key);
			totalAssetsPerCompany[i] = count == 0 ? 0f : amount / count;
			totalAssetsToIncomeRatio[i] = totalIncomePerCompany[i] == 0 ? 0f
					: totalAssetsPerCompany[i] / totalIncomePerCompany[i];

			// Current Assets
			count = Math.round(this.atoCompanyTable4a.get("All current assets no.").get(key));
			amount = this.atoCompanyTable4a.get("All current assets $").get(key);
			amountPerCompany = count == 0 ? 0f : amount / count;
			currentAssetsRatio[i] = totalIncomePerCompany[i] == 0 ? 0f : amountPerCompany / totalIncomePerCompany[i];

			// TOTAL LIABILITIES
			count = Math.round(this.atoCompanyTable4a.get("Total liabilities no.").get(key));
			amount = this.atoCompanyTable4a.get("Total liabilities $").get(key);
			totalLiabilitiesPerCompany[i] = count == 0 ? 0f : amount / count;
			totalLiabilitiesToIncomeRatio[i] = totalIncomePerCompany[i] == 0 ? 0f
					: totalLiabilitiesPerCompany[i] / totalIncomePerCompany[i];

			// Trade Creditors
			count = Math.round(this.atoCompanyTable4a.get("Trade creditors no.").get(key));
			amount = this.atoCompanyTable4a.get("Trade creditors $").get(key);
			amountPerCompany = count == 0 ? 0f : amount / count;
			tradeCreditorsRatio[i] = totalIncomePerCompany[i] == 0 ? 0f : amountPerCompany / totalIncomePerCompany[i];

			// Current Liabilities
			count = Math.round(this.atoCompanyTable4a.get("All current liabilities no.").get(key));
			amount = this.atoCompanyTable4a.get("All current liabilities $").get(key);
			amountPerCompany = count == 0 ? 0f : amount / count;
			currentLiabilitiesRatio[i] = totalIncomePerCompany[i] == 0 ? 0f
					: amountPerCompany / totalIncomePerCompany[i];

			// Debt
			count = Math.round(this.atoCompanyTable4a.get("Total debt no.").get(key));
			amount = this.atoCompanyTable4a.get("Total debt $").get(key);
			amountPerCompany = count == 0 ? 0f : amount / count;
			debtRatio[i] = totalIncomePerCompany[i] == 0 ? 0f : amountPerCompany / totalIncomePerCompany[i];

		}
		// calculate wages ratios
		for (int i = 0; i < numFineIndustryKeys; i++) {
			String key = industryGroupCodes[i];
			String thisDivision = industryDivisionMap4A.get(key);
			float meanWagesPerDivision = wagesPerIndustryDivision.get(thisDivision)
					/ numberOfGroupsInDivision.get(thisDivision);
			wagesDivisionMultiplierPerGroup[i] = salaryWagePerGroup[i] / meanWagesPerDivision;

			if (DEBUG_ZEROS) {
				System.out.println("wagesDivisionMultiplierPerGroup[" + i + "]: " + wagesDivisionMultiplierPerGroup[i]);
			}
		}

		/*
		 * 2. ATO Company Table 4B: Calculate per-company figures, then multiply by the
		 * ratios from 4A to get a more detailed P&L. This gives per-company P&L for 574
		 * industries. Calculate the ratio of the number of companies in each of the
		 * relevant industry codes for each of the 18 industry divisions. This allows us
		 * to break the ABS data for 18 industries down into 574 industry codes.
		 */
		// System.out.println("Step 2: " + new Date(System.currentTimeMillis()));

		Set<String> industryCodeKeySet = new HashSet<String>(
				this.atoCompanyTable4b.get("Number of companies").keySet());
		int numIndustryCodeKeys = industryCodeKeySet.size();
		String[] industryCodes = industryCodeKeySet.stream().toArray(String[]::new);

		int[] numberOfCompanies = new int[numIndustryCodeKeys];
		float[] wagesDivisionMultiplierPerIndustryCode = new float[numIndustryCodeKeys];

		// mean amounts per company
		float[] totalIncome = new float[numIndustryCodeKeys];
		float[] sales = new float[numIndustryCodeKeys];
		float[] interestIncome = new float[numIndustryCodeKeys];
		float[] rentIncome = new float[numIndustryCodeKeys];
		float[] governmentIncome = new float[numIndustryCodeKeys];
		float[] foreignIncome = new float[numIndustryCodeKeys];

		float[] totalExpense = new float[numIndustryCodeKeys];
		float[] costOfSales = new float[numIndustryCodeKeys];
		float[] rentLeaseExpense = new float[numIndustryCodeKeys];
		float[] interestExpense = new float[numIndustryCodeKeys];
		float[] foreignInterestExpense = new float[numIndustryCodeKeys];
		float[] depreciationExpense = new float[numIndustryCodeKeys];
		float[] salaryWage = new float[numIndustryCodeKeys];

		float[] totalAssets = new float[numIndustryCodeKeys];
		float[] currentAssets = new float[numIndustryCodeKeys];

		float[] totalLiabilities = new float[numIndustryCodeKeys];
		float[] tradeCreditors = new float[numIndustryCodeKeys];
		float[] currentLiabilities = new float[numIndustryCodeKeys];
		float[] debt = new float[numIndustryCodeKeys];

		for (int i = 0; i < numIndustryCodeKeys; i++) {
			int count = 0;
			float amount = 0f;
			float amountPerCompany = 0f;
			String key = industryCodes[i];
			int fineIndustryIndex = fineIndustryKeyIndex.get(key.substring(0, 3));

			// NUMBER OF COMPANIES
			// all "<10" values have been changed into 5
			numberOfCompanies[i] = Math.round(this.atoCompanyTable4b.get("Number of companies").get(key));

			// TOTAL INCOME
			count = Math.round(this.atoCompanyTable4b.get("Total income no.").get(key));
			amount = this.atoCompanyTable4b.get("Total income $").get(key);
			totalIncome[i] = count == 0 ? 0f : amount / count;

			// Calculate income line items
			sales[i] = salesRatio[fineIndustryIndex] * totalIncome[i];
			interestIncome[i] = interestIncomeRatio[fineIndustryIndex] * totalIncome[i];
			rentIncome[i] = rentIncomeRatio[fineIndustryIndex] * totalIncome[i];
			governmentIncome[i] = governmentIncomeRatio[fineIndustryIndex] * totalIncome[i];
			foreignIncome[i] = foreignIncomeRatio[fineIndustryIndex] * totalIncome[i];

			if (DEBUG_ZEROS) {
				if (i < 5) {
					System.out.println("totalIncome[" + i + "]: " + totalIncome[i]);
					System.out.println("sales[" + i + "]: " + sales[i]);
					System.out.println("interestIncome[" + i + "]: " + interestIncome[i]);
					System.out.println("rentIncome[" + i + "]: " + rentIncome[i]);
					System.out.println("governmentIncome[" + i + "]: " + governmentIncome[i]);
					System.out.println("foreignIncome[" + i + "]: " + foreignIncome[i]);
				}
			}

			// TOTAL EXPENSES
			count = Math.round(this.atoCompanyTable4b.get("Total expenses no.").get(key));
			amount = this.atoCompanyTable4b.get("Total expenses $").get(key);
			totalExpense[i] = count == 0 ? 0f : amount / count;

			// Calculate expense line items
			costOfSales[i] = costOfSalesRatio[fineIndustryIndex] * totalExpense[i];
			rentLeaseExpense[i] = rentLeaseExpenseRatio[fineIndustryIndex] * totalExpense[i];
			interestExpense[i] = interestExpenseRatio[fineIndustryIndex] * totalExpense[i];
			foreignInterestExpense[i] = foreignInterestExpenseRatio[fineIndustryIndex] * totalExpense[i];
			depreciationExpense[i] = depreciationExpenseRatio[fineIndustryIndex] * totalExpense[i];
			salaryWage[i] = salaryWageRatio[fineIndustryIndex] * totalExpense[i];
			wagesDivisionMultiplierPerIndustryCode[i] = wagesDivisionMultiplierPerGroup[fineIndustryIndex];

			// Calculate asset line items
			totalAssets[i] = totalAssetsToIncomeRatio[fineIndustryIndex] * totalIncome[i];
			currentAssets[i] = currentAssetsRatio[fineIndustryIndex] * totalAssets[i];

			// Calculate liability line items
			totalLiabilities[i] = totalLiabilitiesToIncomeRatio[fineIndustryIndex] * totalIncome[i];
			tradeCreditors[i] = tradeCreditorsRatio[fineIndustryIndex] * totalLiabilities[i];
			currentLiabilities[i] = currentLiabilitiesRatio[fineIndustryIndex] * totalLiabilities[i];
			debt[i] = debtRatio[fineIndustryIndex] * totalLiabilities[i];

		}

		/*
		 * -------------------------------------------------------------------------
		 * PART B: INDUSTRY BUSINESS FINANCIALS SPLIT BY SIZE AND STATE
		 * -------------------------------------------------------------------------
		 * 
		 * 3. ABS 8155.0 Table 6: Calculate ratios between states (for employment count,
		 * wages and sales) by industry.
		 * 
		 * Keys: year, column title, state, industry
		 */
		// System.out.println("Step 3: " + new Date(System.currentTimeMillis()));

		// String[] states = { "NSW", "VIC", "QLD", "SA", "WA", "TAS", "NT", "ACT",
		// "Other" };
		// exclude "Other" states because the data is inconsistent and it causes null
		// pointer errors
		String[] states = { "NSW", "VIC", "QLD", "SA", "WA", "TAS", "NT", "ACT" };
		Set<String> industriesSet8155 = this.abs8155_0Table6.get(ABS8155_YEAR).get(ABS8155_TITLE_EMPLOYMENT)
				.get(states[0]).keySet();
		industriesSet8155.remove("Other services");
		industriesSet8155.remove("Total selected industries");
		industriesSet8155.remove(null); // the first element in the array is null if I don't do this step
		String[] industries8155 = industriesSet8155.stream().toArray(String[]::new);
		float[] totalStateEmploymentByIndustry = new float[industries8155.length];
		float[] totalStateWagesByIndustry = new float[industries8155.length];
		float[] totalStateSalesByIndustry = new float[industries8155.length];
		float[][] stateRatioEmploymentCount = new float[industries8155.length][states.length];
		float[][] stateRatioWages = new float[industries8155.length][states.length];
		float[][] stateRatioSales = new float[industries8155.length][states.length];

		// calculate totals first so the ratios can be calculated
		for (String thisState : states) {
			for (int i = 0; i < industries8155.length; i++) {
				totalStateEmploymentByIndustry[i] += this.abs8155_0Table6.get(ABS8155_YEAR)
						.get(ABS8155_TITLE_EMPLOYMENT).get(thisState).get(industries8155[i]) * THOUSAND;
				totalStateWagesByIndustry[i] += this.abs8155_0Table6.get(ABS8155_YEAR).get(ABS8155_TITLE_WAGES)
						.get(thisState).get(industries8155[i]) * MILLION;
				totalStateSalesByIndustry[i] += this.abs8155_0Table6.get(ABS8155_YEAR).get(ABS8155_TITLE_SALES)
						.get(thisState).get(industries8155[i]) * MILLION;
			}
		}
		// calculate ratios between states for each industry
		for (int i = 0; i < industries8155.length; i++) {
			for (int j = 0; j < states.length; j++) {
				float cellValue = this.abs8155_0Table6.get(ABS8155_YEAR).get(ABS8155_TITLE_EMPLOYMENT).get(states[j])
						.get(industries8155[i]) * THOUSAND;
				stateRatioEmploymentCount[i][j] = totalStateEmploymentByIndustry[i] > 0f
						? cellValue / totalStateEmploymentByIndustry[i]
						: 0f;
				cellValue = this.abs8155_0Table6.get(ABS8155_YEAR).get(ABS8155_TITLE_WAGES).get(states[j])
						.get(industries8155[i]) * MILLION;
				stateRatioWages[i][j] = totalStateWagesByIndustry[i] > 0f ? cellValue / totalStateWagesByIndustry[i]
						: 0f;
				cellValue = this.abs8155_0Table6.get(ABS8155_YEAR).get(ABS8155_TITLE_SALES).get(states[j])
						.get(industries8155[i]) * MILLION;
				stateRatioSales[i][j] = totalStateSalesByIndustry[i] > 0f ? cellValue / totalStateSalesByIndustry[i]
						: 0f;

				if (DEBUG_ZEROS) {
					if (i < 5) {
						System.out.println("stateRatioSales[" + i + "][" + j + "]: " + stateRatioSales[i][j]);
					}
				}
			}
		}

		/*
		 * 4. ABS 8155.0 Table 5: Use the ratios from Table 6 to split Size by State,
		 * assuming that all income is split in the same ratio as sales, and all
		 * expenses are split in the same ratio as wages. Calculate gross profit by
		 * subtracting total expenses from total income.
		 * 
		 * Keys: year, column title, size, industry
		 */
		// System.out.println("Step 4: " + new Date(System.currentTimeMillis()));

		String[] sizes = { "S", "M", "L" };
		float[] totalSizeEmploymentByIndustry = new float[industries8155.length];
		float[] totalSizeWagesByIndustry = new float[industries8155.length];
		float[] totalSizeSalesByIndustry = new float[industries8155.length];
		float[] totalIncomeByIndustry = new float[industries8155.length];
		float[] totalExpensesByIndustry = new float[industries8155.length];
		float[][][] employmentCountByStateIndustrySize = new float[states.length][industries8155.length][sizes.length];
		float[][][] wagesByStateIndustrySize = new float[states.length][industries8155.length][sizes.length];
		float[][][] salesByStateIndustrySize = new float[states.length][industries8155.length][sizes.length];
		float[][][] totalIncomeByStateIndustrySize = new float[states.length][industries8155.length][sizes.length];
		float[][][] totalExpensesByStateIndustrySize = new float[states.length][industries8155.length][sizes.length];

		// calculate totals first so the ratios can be calculated
		for (String thisSize : sizes) {
			for (int i = 0; i < industries8155.length; i++) {
				totalSizeEmploymentByIndustry[i] += this.abs8155_0Table5.get(ABS8155_YEAR).get(ABS8155_TITLE_EMPLOYMENT)
						.get(thisSize).get(industries8155[i]) * THOUSAND;
				totalSizeWagesByIndustry[i] += this.abs8155_0Table5.get(ABS8155_YEAR).get(ABS8155_TITLE_WAGES)
						.get(thisSize).get(industries8155[i]) * MILLION;
				totalSizeSalesByIndustry[i] += this.abs8155_0Table5.get(ABS8155_YEAR).get(ABS8155_TITLE_SALES)
						.get(thisSize).get(industries8155[i]) * MILLION;
				totalIncomeByIndustry[i] += this.abs8155_0Table5.get(ABS8155_YEAR).get(ABS8155_TITLE_INCOME)
						.get(thisSize).get(industries8155[i]) * MILLION;
				totalExpensesByIndustry[i] += this.abs8155_0Table5.get(ABS8155_YEAR).get(ABS8155_TITLE_EXPENSES)
						.get(thisSize).get(industries8155[i]) * MILLION;
			}
		}
		// apply state ratios to sizes, joining on industry
		for (int idxIndustry = 0; idxIndustry < industries8155.length; idxIndustry++) {
			for (int idxSize = 0; idxSize < sizes.length; idxSize++) {
				float industrySizeEmployment = this.abs8155_0Table5.get(ABS8155_YEAR).get(ABS8155_TITLE_EMPLOYMENT)
						.get(sizes[idxSize]).get(industries8155[idxIndustry]) * THOUSAND;
				float industrySizeWages = this.abs8155_0Table5.get(ABS8155_YEAR).get(ABS8155_TITLE_WAGES)
						.get(sizes[idxSize]).get(industries8155[idxIndustry]) * MILLION;
				float industrySizeSales = this.abs8155_0Table5.get(ABS8155_YEAR).get(ABS8155_TITLE_SALES)
						.get(sizes[idxSize]).get(industries8155[idxIndustry]) * MILLION;
				float industrySizeTotalIncome = this.abs8155_0Table5.get(ABS8155_YEAR).get(ABS8155_TITLE_INCOME)
						.get(sizes[idxSize]).get(industries8155[idxIndustry]) * MILLION;
				float industrySizeTotalExpenses = this.abs8155_0Table5.get(ABS8155_YEAR).get(ABS8155_TITLE_EXPENSES)
						.get(sizes[idxSize]).get(industries8155[idxIndustry]) * MILLION;
				for (int idxState = 0; idxState < states.length; idxState++) {
					employmentCountByStateIndustrySize[idxState][idxIndustry][idxSize] = industrySizeEmployment
							* stateRatioEmploymentCount[idxIndustry][idxState];
					wagesByStateIndustrySize[idxState][idxIndustry][idxSize] = industrySizeWages
							* stateRatioWages[idxIndustry][idxState];
					salesByStateIndustrySize[idxState][idxIndustry][idxSize] = industrySizeSales
							* stateRatioSales[idxIndustry][idxState];
					totalIncomeByStateIndustrySize[idxState][idxIndustry][idxSize] = industrySizeTotalIncome
							* stateRatioSales[idxIndustry][idxState];
					totalExpensesByStateIndustrySize[idxState][idxIndustry][idxSize] = industrySizeTotalExpenses
							* stateRatioWages[idxIndustry][idxState];

					if (DEBUG_ZEROS) {
						if (idxIndustry < 5) {
							System.out.println("industrySizeEmployment: " + industrySizeEmployment);
							System.out.println("stateRatioEmploymentCount[" + idxIndustry + "][" + idxState + "]: "
									+ stateRatioEmploymentCount[idxIndustry][idxState]);
							System.out.println("employmentCountByStateIndustrySize[" + idxIndustry + "][" + idxSize
									+ "][" + idxState + "]: "
									+ employmentCountByStateIndustrySize[idxState][idxIndustry][idxSize]);
							System.out.println("industrySizeTotalIncome: " + industrySizeTotalIncome);
							System.out.println("stateRatioSales[" + idxIndustry + "][" + idxState + "]: "
									+ stateRatioSales[idxIndustry][idxState]);
							System.out.println(
									"totalIncomeByStateIndustrySize[" + idxIndustry + "][" + idxSize + "][" + idxState
											+ "]: " + totalIncomeByStateIndustrySize[idxState][idxIndustry][idxSize]);
						}
					}
				}
			}
		}
		// we now have total $ amounts by state, industry, and size.

		/*
		 * -------------------------------------------------------------------------
		 * PART C: ADJUST INDUSTRY P&L AND BAL SHTS BY SIZE AND STATE
		 * -------------------------------------------------------------------------
		 * 
		 * 5. ABS 8165.0 LGA Employment Range: Get the number of businesses per state /
		 * industry / size then divide the dollar amounts from 8155.0 to determine the
		 * mean dollars per business. Divide the number of employees from 8155.0 by the
		 * number of businesses in 8165.0 to get the mean employees per business by
		 * state / industry / size. Also calculate a total national mean dollars and
		 * employees per business.
		 */
		// System.out.println("Step 5: " + new Date(System.currentTimeMillis()));

		float businessCountAU = 0f;
		float employmentCountAU = 0f;
		float wagesAU = 0f;
		float salesAU = 0f;
		float totalIncomeAU = 0f;
		float totalExpensesAU = 0f;
		float[][][] businessCountByStateIndustrySize = new float[states.length][industries8155.length][sizes.length];
		float[][][] employmentCountPerBusiness = new float[states.length][industries8155.length][sizes.length];
		float[][][] wagesPerBusiness = new float[states.length][industries8155.length][sizes.length];
		float[][][] salesPerBusiness = new float[states.length][industries8155.length][sizes.length];
		float[][][] totalIncomePerBusiness = new float[states.length][industries8155.length][sizes.length];
		float[][][] totalExpensesPerBusiness = new float[states.length][industries8155.length][sizes.length];
		Set<String> lgaCodes8165 = this.abs8165_0LgaEmployment.get(ABS8165_TITLE_EMPLOYMENT_1).get(states[0]).keySet();
		lgaCodes8165.remove("Total"); // need to do this to avoid errors in the loop below
		for (int idxIndustry = 0; idxIndustry < industries8155.length; idxIndustry++) {
			for (int idxState = 0; idxState < states.length; idxState++) {
				// count number of businesses in each category
				float smallCount = 0f;
				float mediumCount = 0f;
				float largeCount = 0f;
				for (String lgaCode : lgaCodes8165) {
					// It loops over all LGA codes, but they don't all exist in every state. Adding
					// this if statement is inefficient but the fastest way to fix the bug for now.
					String lgaState = this.area.getStateFromLgaCode(lgaCode);
					if (DEBUG_ZEROS_INFINITY) {
						System.out.println("lgaCode: " + lgaCode);
						System.out.println("lgaState: " + lgaState);
						System.out.println("states[idxState]: " + states[idxState]);
					}

					if (lgaState != null && lgaState.equals(states[idxState])) {
						smallCount += this.abs8165_0LgaEmployment.get(ABS8165_TITLE_EMPLOYMENT_1).get(states[idxState])
								.get(lgaCode).get(industries8155[idxIndustry]);
						smallCount += this.abs8165_0LgaEmployment.get(ABS8165_TITLE_EMPLOYMENT_2).get(states[idxState])
								.get(lgaCode).get(industries8155[idxIndustry]);
						mediumCount += this.abs8165_0LgaEmployment.get(ABS8165_TITLE_EMPLOYMENT_3).get(states[idxState])
								.get(lgaCode).get(industries8155[idxIndustry]);
						largeCount += this.abs8165_0LgaEmployment.get(ABS8165_TITLE_EMPLOYMENT_4).get(states[idxState])
								.get(lgaCode).get(industries8155[idxIndustry]);

						if (DEBUG_ZEROS_INFINITY) {
							System.out.println("smallCount: " + smallCount);
							System.out.println("mediumCount: " + mediumCount);
							System.out.println("largeCount: " + largeCount);
							System.out.println("ABS8165_TITLE_EMPLOYMENT_1: " + ABS8165_TITLE_EMPLOYMENT_1);
							System.out.println("states[idxState]: " + states[idxState]);
							System.out.println("lgaCode: " + lgaCode);
							System.out.println("industries8155[idxIndustry]: " + industries8155[idxIndustry]);
							System.out.println(
									"this.abs8165_0LgaEmployment.get(ABS8165_TITLE_EMPLOYMENT_1).get(states[idxState]).get(lgaCode).get(industries8155[idxIndustry]): "
											+ this.abs8165_0LgaEmployment.get(ABS8165_TITLE_EMPLOYMENT_1)
													.get(states[idxState]).get(lgaCode)
													.get(industries8155[idxIndustry]));
						}
					}
				}
				businessCountAU += smallCount + mediumCount + largeCount;
				businessCountByStateIndustrySize[idxState][idxIndustry][0] = smallCount;
				businessCountByStateIndustrySize[idxState][idxIndustry][1] = mediumCount;
				businessCountByStateIndustrySize[idxState][idxIndustry][2] = largeCount;

				// calculate mean amounts per business
				employmentCountPerBusiness[idxState][idxIndustry][0] = smallCount == 0 ? 0
						: employmentCountByStateIndustrySize[idxState][idxIndustry][0] / smallCount;
				employmentCountPerBusiness[idxState][idxIndustry][1] = mediumCount == 0 ? 0
						: employmentCountByStateIndustrySize[idxState][idxIndustry][1] / mediumCount;
				employmentCountPerBusiness[idxState][idxIndustry][2] = largeCount == 0 ? 0
						: employmentCountByStateIndustrySize[idxState][idxIndustry][2] / largeCount;

				wagesPerBusiness[idxState][idxIndustry][0] = smallCount == 0 ? 0
						: wagesByStateIndustrySize[idxState][idxIndustry][0] / smallCount;
				wagesPerBusiness[idxState][idxIndustry][1] = mediumCount == 0 ? 0
						: wagesByStateIndustrySize[idxState][idxIndustry][1] / mediumCount;
				wagesPerBusiness[idxState][idxIndustry][2] = largeCount == 0 ? 0
						: wagesByStateIndustrySize[idxState][idxIndustry][2] / largeCount;

				salesPerBusiness[idxState][idxIndustry][0] = smallCount == 0 ? 0
						: salesByStateIndustrySize[idxState][idxIndustry][0] / smallCount;
				salesPerBusiness[idxState][idxIndustry][1] = mediumCount == 0 ? 0
						: salesByStateIndustrySize[idxState][idxIndustry][1] / mediumCount;
				salesPerBusiness[idxState][idxIndustry][2] = largeCount == 0 ? 0
						: salesByStateIndustrySize[idxState][idxIndustry][2] / largeCount;

				totalIncomePerBusiness[idxState][idxIndustry][0] = smallCount == 0 ? 0
						: totalIncomeByStateIndustrySize[idxState][idxIndustry][0] / smallCount;
				totalIncomePerBusiness[idxState][idxIndustry][1] = mediumCount == 0 ? 0
						: totalIncomeByStateIndustrySize[idxState][idxIndustry][1] / mediumCount;
				totalIncomePerBusiness[idxState][idxIndustry][2] = largeCount == 0 ? 0
						: totalIncomeByStateIndustrySize[idxState][idxIndustry][2] / largeCount;

				totalExpensesPerBusiness[idxState][idxIndustry][0] = smallCount == 0 ? 0
						: totalExpensesByStateIndustrySize[idxState][idxIndustry][0] / smallCount;
				totalExpensesPerBusiness[idxState][idxIndustry][1] = mediumCount == 0 ? 0
						: totalExpensesByStateIndustrySize[idxState][idxIndustry][1] / mediumCount;
				totalExpensesPerBusiness[idxState][idxIndustry][2] = largeCount == 0 ? 0
						: totalExpensesByStateIndustrySize[idxState][idxIndustry][2] / largeCount;

				// sum national totals per category
				for (int idxSize = 0; idxSize < sizes.length; idxSize++) {
					employmentCountAU += employmentCountByStateIndustrySize[idxState][idxIndustry][idxSize];
					wagesAU += wagesByStateIndustrySize[idxState][idxIndustry][idxSize];
					salesAU += salesByStateIndustrySize[idxState][idxIndustry][idxSize];
					totalIncomeAU += totalIncomeByStateIndustrySize[idxState][idxIndustry][idxSize];
					totalExpensesAU += totalExpensesByStateIndustrySize[idxState][idxIndustry][idxSize];
				}
			}
		}
		// calculate national means
		float employmentCountPerBusinessAU = employmentCountAU / businessCountAU;
		float wagesPerBusinessAU = wagesAU / businessCountAU;
		float salesPerBusinessAU = salesAU / businessCountAU;
		float totalIncomePerBusinessAU = totalIncomeAU / businessCountAU;
		float totalExpensesPerBusinessAU = totalExpensesAU / businessCountAU;

		if (DEBUG_ZEROS) {
			System.out.println("businessCountAU: " + businessCountAU);
			System.out.println("employmentCountPerBusinessAU: " + employmentCountPerBusinessAU);
			System.out.println("wagesPerBusinessAU: " + wagesPerBusinessAU);
			System.out.println("salesPerBusinessAU: " + salesPerBusinessAU);
			System.out.println("totalIncomePerBusinessAU: " + totalIncomePerBusinessAU);
			System.out.println("totalExpensesPerBusinessAU: " + totalExpensesPerBusinessAU);
		}

		/*
		 * 6. Divide each state / industry / size's figures by the national average to
		 * produce a multiplier for each category combination.
		 */
		// System.out.println("Step 6: " + new Date(System.currentTimeMillis()));

		float[][][] employmentCountMultiplier = new float[states.length][industries8155.length][sizes.length];
		float[][][] wagesMultiplier = new float[states.length][industries8155.length][sizes.length];
		float[][][] salesMultiplier = new float[states.length][industries8155.length][sizes.length];
		float[][][] totalIncomeMultiplier = new float[states.length][industries8155.length][sizes.length];
		float[][][] totalExpensesMultiplier = new float[states.length][industries8155.length][sizes.length];
		for (int idxState = 0; idxState < states.length; idxState++) {
			for (int idxIndustry = 0; idxIndustry < industries8155.length; idxIndustry++) {
				for (int idxSize = 0; idxSize < sizes.length; idxSize++) {
					employmentCountMultiplier[idxState][idxIndustry][idxSize] = employmentCountPerBusiness[idxState][idxIndustry][idxSize]
							/ employmentCountPerBusinessAU;
					wagesMultiplier[idxState][idxIndustry][idxSize] = wagesPerBusiness[idxState][idxIndustry][idxSize]
							/ wagesPerBusinessAU;
					salesMultiplier[idxState][idxIndustry][idxSize] = salesPerBusiness[idxState][idxIndustry][idxSize]
							/ salesPerBusinessAU;
					totalIncomeMultiplier[idxState][idxIndustry][idxSize] = totalIncomePerBusiness[idxState][idxIndustry][idxSize]
							/ totalIncomePerBusinessAU;
					totalExpensesMultiplier[idxState][idxIndustry][idxSize] = totalExpensesPerBusiness[idxState][idxIndustry][idxSize]
							/ totalExpensesPerBusinessAU;

					if (DEBUG_ZEROS) {
						if (idxState < 2 && idxIndustry < 2 && idxSize < 2) {
							System.out.println("employmentCountPerBusinessAU: " + employmentCountPerBusinessAU);
							System.out.println("employmentCountPerBusiness[" + idxState + "][" + idxIndustry + "]["
									+ idxSize + "]: " + employmentCountPerBusiness[idxState][idxIndustry][idxSize]);
							System.out.println("employmentCountMultiplier[" + idxState + "][" + idxIndustry + "]["
									+ idxSize + "]: " + employmentCountMultiplier[idxState][idxIndustry][idxSize]);
							System.out.println("totalIncomePerBusinessAU: " + totalIncomePerBusinessAU);
							System.out.println("totalIncomePerBusiness[" + idxState + "][" + idxIndustry + "]["
									+ idxSize + "]: " + totalIncomePerBusiness[idxState][idxIndustry][idxSize]);
							System.out.println("totalIncomeMultiplier[" + idxState + "][" + idxIndustry + "][" + idxSize
									+ "]: " + totalIncomeMultiplier[idxState][idxIndustry][idxSize]);
						}
					}
				}
			}
		}

		/*
		 * 7. RBA E1: For the national total balance sheet, calculate the ratio of
		 * business bank deposits to total financial assets, and business foreign
		 * equities to total financial assets. Below we will assume that total financial
		 * assets and current assets are equivalent, and use these ratios to estimate
		 * the bank deposits and foreign equities held by businesses. We further assume
		 * that only large businesses hold foreign equities (i.e. multi-national
		 * corporations and large investment firms).
		 */
		// System.out.println("Step 7: " + new Date(System.currentTimeMillis()));

		DateFormat df = new SimpleDateFormat("MMM-yyyy", Locale.ENGLISH);
		Date rbaE1Date = null;
		try {
			// convert String to Date
			rbaE1Date = df.parse(RBA_E1_DATE_STRING);
		} catch (ParseException e1) {
			// Auto-generated catch block
			e1.printStackTrace();
		}
		float bankDepositsE1 = Float.valueOf(this.rbaE1.get(RBA_E1_BUSINESS_BANK_DEPOSITS).get(rbaE1Date));
		float foreignEquitiesE1 = Float.valueOf(this.rbaE1.get(RBA_E1_BUSINESS_FOREIGN_EQUITIES).get(rbaE1Date));
		float totalFinancialAssetsE1 = Float
				.valueOf(this.rbaE1.get(RBA_E1_BUSINESS_TOTAL_FINANCIAL_ASSETS).get(rbaE1Date));
		float bankDepositRatioE1 = totalFinancialAssetsE1 > 0f ? bankDepositsE1 / totalFinancialAssetsE1 : 0f;
		float foreignEquitiesRatioE1 = totalFinancialAssetsE1 > 0f ? foreignEquitiesE1 / totalFinancialAssetsE1 : 0f;

		/*
		 * 8. For each state / industry / size, multiply the ATO industry figures by
		 * this multiplier to calibrate the ATO company P&L and Bal Shts so they're now
		 * representative of businesses per state / industry / size.
		 */
		// System.out.println("Step 8: " + new Date(System.currentTimeMillis()));

		// make a map so we can look up division indices cheaply
		Map<String, Integer> divisionCodeKeyIndex = new HashMap<String, Integer>(industries8155.length);
		for (int i = 0; i < industries8155.length; i++) {
			divisionCodeKeyIndex.put(industries8155[i], i);
		}
		int businessTypeId = 1;
		Business[][][] agentMatrix = new Business[states.length][numIndustryCodeKeys][sizes.length];
		Map<String, List<String>> classIndustryCodes = new HashMap<String, List<String>>();
		for (int idxIndustryCode = 0; idxIndustryCode < numIndustryCodeKeys; idxIndustryCode++) { // for all 574 ATO
																									// industry codes
			// get the industries8155 index (18 values) for this industry code (574 values)
			String divisionCode = this.abs1292_0_55_002ANZSIC.get("Industry Code to Division Code")
					.get(industryCodes[idxIndustryCode]);
			String classCode = this.abs1292_0_55_002ANZSIC.get("Industry Code to Class Code")
					.get(industryCodes[idxIndustryCode]);
			if (!classIndustryCodes.containsKey(classCode)) {
				classIndustryCodes.put(classCode, new ArrayList<String>(1));
			}
			classIndustryCodes.get(classCode).add(industryCodes[idxIndustryCode]);
			if (!divisionCode.equals("K")) { // deal with financial services separately
				int divisionCodeIndex8155 = divisionCodeKeyIndex.get(divisionCode);
				for (int idxState = 0; idxState < states.length; idxState++) {
					for (int idxSize = 0; idxSize < sizes.length; idxSize++) {
						float empMult = employmentCountMultiplier[idxState][divisionCodeIndex8155][idxSize];
						float wageMult = wagesMultiplier[idxState][divisionCodeIndex8155][idxSize];
						float saleMult = salesMultiplier[idxState][divisionCodeIndex8155][idxSize];
						float incMult = totalIncomeMultiplier[idxState][divisionCodeIndex8155][idxSize];
						float expMult = totalExpensesMultiplier[idxState][divisionCodeIndex8155][idxSize];

						// multiply P&L lines by the relevant multipliers
						float agentTotalIncome = totalIncome[idxIndustryCode] * incMult;
						float agentSales = sales[idxIndustryCode] * saleMult;
						float agentInterestIncome = interestIncome[idxIndustryCode] * incMult;
						float agentRentIncome = rentIncome[idxIndustryCode] * incMult;
						float agentGovernmentIncome = governmentIncome[idxIndustryCode] * incMult;
						float agentForeignIncome = foreignIncome[idxIndustryCode] * incMult;

						float agentTotalExpense = totalExpense[idxIndustryCode] * expMult;
						float agentCostOfSales = costOfSales[idxIndustryCode] * expMult;
						float agentRentLeaseExpense = rentLeaseExpense[idxIndustryCode] * expMult;
						float agentInterestExpense = interestExpense[idxIndustryCode] * expMult;
						float agentForeignInterestExpense = foreignInterestExpense[idxIndustryCode] * expMult;
						float agentDepreciationExpense = depreciationExpense[idxIndustryCode] * expMult;
						float agentSalaryWage = salaryWage[idxIndustryCode] * wageMult;

						// multiply Bal sht items by the income multiplier because that's the method
						// used in step 2 above
						float agentTotalAssets = totalAssets[idxIndustryCode] * incMult;
						float agentCurrentAssets = currentAssets[idxIndustryCode] * incMult;

						float agentTotalLiabilities = totalLiabilities[idxIndustryCode] * incMult;
						float agentTradeCreditors = tradeCreditors[idxIndustryCode] * incMult;
						float agentCurrentLiabilities = currentLiabilities[idxIndustryCode] * incMult;
						float agentDebt = debt[idxIndustryCode] * incMult;

						if (DEBUG_ZEROS) {
							if (idxState < 2 && idxIndustryCode < 2 && idxSize < 2) {
								System.out.println("incMult: " + incMult);
								System.out.println(
										"totalIncome[" + idxIndustryCode + "]: " + totalIncome[idxIndustryCode]);
								System.out.println("agentTotalIncome: " + agentTotalIncome);

								System.out.println("incMult: " + incMult);
								System.out
										.println("rentIncome[" + idxIndustryCode + "]: " + rentIncome[idxIndustryCode]);
								System.out.println("agentRentIncome: " + agentRentIncome);
							}
						}

						// create representative agent
						Business agent = new Business();
						agent.setName("Type " + businessTypeId + " business");
						agent.setBusinessTypeId(businessTypeId++);
						agent.setIndustryCode(industryCodes[idxIndustryCode]);
						agent.setIndustryDivisionCode(divisionCode.charAt(0));

						agent.setState(states[idxState]);
						agent.setSize(sizes[idxSize].charAt(0));
						agent.setExporter(false);

						// set employee count target
						agent.setEmployeeCountTarget(
								(int) Math.round(employmentCountPerBusiness[idxState][divisionCodeIndex8155][idxSize]
										* wagesDivisionMultiplierPerIndustryCode[idxIndustryCode]));
						// set employeeWages later when linking Individual agents in here

						agent.setTotalIncome(agentTotalIncome);
						agent.setSalesDomestic(agentSales);
						agent.setSalesGovernment(agentGovernmentIncome);
						agent.setSalesForeign(agentForeignIncome);
						agent.setInterestIncome(agentInterestIncome);
						agent.setRentIncome(agentRentIncome);
						// "other income" is a balancing item

						agent.setTotalExpenses(agentTotalExpense);
						agent.setWageExpenses(agentSalaryWage);
						agent.setSuperannuationExpense(agentSalaryWage * Properties.SUPERANNUATION_RATE);
						// can't calculate payroll tax until the state has been set
						agent.setForeignExpenses(agentForeignInterestExpense);
						agent.setInterestExpense(agentInterestExpense);
						agent.setRentExpense(agentRentLeaseExpense);
						agent.setDepreciationExpense(agentDepreciationExpense);
						// "other expenses" is a balancing item

						agent.setTotalAssets(agentTotalAssets);
						agent.setBankDeposits(agentCurrentAssets * bankDepositRatioE1);
						agent.setForeignEquities(agentCurrentAssets * foreignEquitiesRatioE1);
						agent.setOtherFinancialAssets(agentCurrentAssets - agentCurrentAssets * bankDepositRatioE1
								- agentCurrentAssets * foreignEquitiesRatioE1);
						// "other non-financial assets" is a balancing item

						agent.setTotalLiabilities(agentTotalLiabilities);
						agent.setTradeCreditors(agentTradeCreditors);
						agent.setLoans(agentDebt);
						agent.setOtherCurrentLiabilities(agentCurrentLiabilities - agentTradeCreditors);
						// "other non-current liabilities" is a balancing item

						agent.setTotalEquity(agentTotalAssets - agentTotalLiabilities);

						// store agent in a multi-dimensional array
						agentMatrix[idxState][idxIndustryCode][idxSize] = agent; // i == industry code index

						if (DEBUG_ZEROS) {
							if (idxState < 2 && idxIndustryCode < 2 && idxSize < 2) {
								System.out.println("agentMatrix[" + idxState + "][" + idxIndustryCode + "][" + idxSize
										+ "].getTotalIncome(): "
										+ agentMatrix[idxState][idxIndustryCode][idxSize].getTotalIncome());
								System.out.println("agentMatrix[" + idxState + "][" + idxIndustryCode + "][" + idxSize
										+ "].getRentIncome(): "
										+ agentMatrix[idxState][idxIndustryCode][idxSize].getRentIncome());
							}
						}
					}
				}
			}
		}

		/*
		 * -------------------------------------------------------------------------
		 * PART D: ASSIGN BUSINESSES TO LGA BY SIZE AND INDUSTRY
		 * -------------------------------------------------------------------------
		 * 
		 * 9. ABS 8165.0 State Employment Range: Load the data into a multi-dimensional
		 * array, and add Industry Division as an extra key to map by. For each
		 * combination of state and size, calculate the ratio of the number of
		 * businesses in each industry code within each industry division.
		 */
		// System.out.println("Step 9: " + new Date(System.currentTimeMillis()));

		// LGA emp Keys: emp range, state, LGA code, industry division code
		Set<String> divisionsKeySet8165 = abs8165_0LgaEmployment.get(ABS8165_TITLE_EMPLOYMENT_1).get(states[0])
				.get("10050").keySet();
		Set<String> divisionsToAddTo8165 = new HashSet<String>(divisionsKeySet8165);
		String[] divisions8165 = new String[Math.max(industries8155.length, divisionsKeySet8165.size())];
		for (int i = 0; i < industries8155.length; i++) {
			divisions8165[i] = industries8155[i];
			divisionsToAddTo8165.remove(industries8155[i]);
		}
		{// just to limit the scope of i
			int i = industries8155.length;
			for (String div : divisionsToAddTo8165) {
				divisions8165[i++] = div;
			}
		}
		Map<String, Integer> divisionIndexMap8165 = new HashMap<String, Integer>(divisions8165.length);
		for (int i = 0; i < divisions8165.length; i++) {
			divisionIndexMap8165.put(divisions8165[i], i);
		}

		// 8165.0 state employment Keys: employment range, state, industry class code
		Set<String> industryClassCodeSet8165 = this.abs8165_0StateEmployment.get(ABS8165_TITLE_EMPLOYMENT_1)
				.get(states[0]).keySet();
		String[] industryClassCodes8165 = new String[ABS8165_NUM_CLASS_CODES];
		industryClassCodes8165 = industryClassCodeSet8165.toArray(industryClassCodes8165);
		Map<String, Integer> industryClassCodes8165Index = new HashMap<String, Integer>(industryClassCodes8165.length);
		float[][][] industryDivisionTotals = new float[states.length][sizes.length][divisions8165.length];
		float[][][][] industryDivisionRatios = new float[states.length][sizes.length][divisions8165.length][industryClassCodes8165.length];
		for (int idxState = 0; idxState < states.length; idxState++) {
			// initialise the division totals to zero
			Arrays.fill(industryDivisionTotals[idxState][0], 0f);
			Arrays.fill(industryDivisionTotals[idxState][1], 0f);
			Arrays.fill(industryDivisionTotals[idxState][2], 0f);

			// set the division totals
			for (String industryClassCode8165 : industryClassCodeSet8165) {
				String div = this.abs1292_0_55_002ANZSIC.get("Class Code to Division Code").get(industryClassCode8165);
				if (div != null) {
					int idxDiv = divisionIndexMap8165.get(div);
					float smallValue = this.abs8165_0StateEmployment.get(ABS8165_TITLE_EMPLOYMENT_1)
							.get(states[idxState]).get(industryClassCode8165);
					smallValue += this.abs8165_0StateEmployment.get(ABS8165_TITLE_EMPLOYMENT_2).get(states[idxState])
							.get(industryClassCode8165);
					float mediumValue = this.abs8165_0StateEmployment.get(ABS8165_TITLE_EMPLOYMENT_3)
							.get(states[idxState]).get(industryClassCode8165);
					float largeValue = this.abs8165_0StateEmployment.get(ABS8165_TITLE_EMPLOYMENT_4)
							.get(states[idxState]).get(industryClassCode8165);
					industryDivisionTotals[idxState][0][idxDiv] += smallValue;
					industryDivisionTotals[idxState][1][idxDiv] += mediumValue;
					industryDivisionTotals[idxState][2][idxDiv] += largeValue;
				}
			}

			// calculate the ratios
			for (int idxClass = 0; idxClass < industryClassCodes8165.length; idxClass++) {
				industryClassCodes8165Index.put(industryClassCodes8165[idxClass], idxClass);
				String div = this.abs1292_0_55_002ANZSIC.get("Class Code to Division Code")
						.get(industryClassCodes8165[idxClass]);
				if (div != null) {
					int idxDiv = divisionIndexMap8165.get(div);
					float smallTotal = industryDivisionTotals[idxState][0][idxDiv];
					float mediumTotal = industryDivisionTotals[idxState][1][idxDiv];
					float largeTotal = industryDivisionTotals[idxState][2][idxDiv];
					float smallValue = this.abs8165_0StateEmployment.get(ABS8165_TITLE_EMPLOYMENT_1)
							.get(states[idxState]).get(industryClassCodes8165[idxClass]);
					smallValue += this.abs8165_0StateEmployment.get(ABS8165_TITLE_EMPLOYMENT_2).get(states[idxState])
							.get(industryClassCodes8165[idxClass]);
					float mediumValue = this.abs8165_0StateEmployment.get(ABS8165_TITLE_EMPLOYMENT_3)
							.get(states[idxState]).get(industryClassCodes8165[idxClass]);
					float largeValue = this.abs8165_0StateEmployment.get(ABS8165_TITLE_EMPLOYMENT_4)
							.get(states[idxState]).get(industryClassCodes8165[idxClass]);
					industryDivisionRatios[idxState][0][idxDiv][idxClass] = smallTotal > 0f ? smallValue / smallTotal
							: 0f;
					industryDivisionRatios[idxState][1][idxDiv][idxClass] = mediumTotal > 0f ? mediumValue / mediumTotal
							: 0f;
					industryDivisionRatios[idxState][2][idxDiv][idxClass] = largeTotal > 0f ? largeValue / largeTotal
							: 0f;
				}
			}
		}

		/*
		 * 10. ABS 8165.0 LGA Employment Range: For each LGA, size and division,
		 * multiply the number of businesses by the industry code ratios from ABS 8165.0
		 * State Employment Range. Round to integers, adding or subtracting epsilon and
		 * repeating until the difference in the total number is either equal to zero or
		 * grows larger again. This gives us the count of businesses by state, LGA, size
		 * and industry code.
		 * 
		 * Breaks the business count down from Class Code to Industry Code so we can use
		 * all the granularity from ATO Company Table 4B.
		 * 
		 * Counts how many times each agent is instantiated when assigned to LGAs. This
		 * will allow me to do a frequency histogram to show the degree of heterogeneity
		 * among Business agents.
		 * 
		 * 554 LGAs x 3 sizes x 574 industry codes = 953,988 distinct Business agents
		 * 
		 * Deal with exporters later when linking the agents to create the network
		 * topology.
		 */
		// System.out.println("Step 10: " + new Date(System.currentTimeMillis()));

		// calculate ratio of no. businesses in each Industry Code by Industry Class
		Map<String, Float> industryCodeClassRatio4B = new HashMap<String, Float>(industryCodes.length);
		Map<String, Integer> industryClassCompanyCount4B = new HashMap<String, Integer>(industryCodes.length);
		// calculate totals per Industry Class
		for (String industryCode : industryCodes) {
			String classCode = this.abs1292_0_55_002ANZSIC.get("Industry Code to Class Code").get(industryCode);
			if (!industryClassCompanyCount4B.containsKey(classCode)) {
				industryClassCompanyCount4B.put(classCode, 0);
			}
			int industryCodeCount = Math.round(this.atoCompanyTable4b.get("Number of companies").get(industryCode));
			industryClassCompanyCount4B.put(classCode, industryClassCompanyCount4B.get(classCode) + industryCodeCount);
		}
		// calculate ratios per Industry Code
		for (String industryCode : industryCodes) {
			String classCode = this.abs1292_0_55_002ANZSIC.get("Industry Code to Class Code").get(industryCode);
			float total = industryClassCompanyCount4B.get(classCode);
			float value = this.atoCompanyTable4b.get("Number of companies").get(industryCode);
			industryCodeClassRatio4B.put(industryCode, total > 0f ? value / total : 0f);
		}

		List<String> lgaCodes = new ArrayList<String>(ABS8165_NUM_LGAS);
		this.businessTypeCount = new int[businessTypeId];
		Arrays.fill(this.businessTypeCount, 0);
		for (int idxState = 0; idxState < states.length; idxState++) {
			Set<String> lgaCodeSet = this.abs8165_0LgaEmployment.get(ABS8165_TITLE_EMPLOYMENT_1).get(states[idxState])
					.keySet();
			lgaCodeSet.remove("Total"); // fixes a null pointer exception below
			for (String lgaCode : lgaCodeSet) {
				lgaCodes.add(lgaCode);
				for (int idxIndustryCode = 0; idxIndustryCode < industryCodes.length; idxIndustryCode++) {
					String industryCode = industryCodes[idxIndustryCode];
					String classCode = this.abs1292_0_55_002ANZSIC.get("Industry Code to Class Code").get(industryCode);
					if (industryClassCodeSet8165.contains(classCode)) {
						String divCode = this.abs1292_0_55_002ANZSIC.get("Industry Code to Division Code")
								.get(industryCode);
						int idxDiv = divisionIndexMap8165.get(divCode);
						int idxClass = industryClassCodes8165Index.get(classCode);
						float smallTotal = 0f;
						float mediumTotal = 0f;
						float largeTotal = 0f;
						// One of the values is a char, so need to make it 0f.
						try {
							smallTotal += this.abs8165_0LgaEmployment.get(ABS8165_TITLE_EMPLOYMENT_1)
									.get(states[idxState]).get(lgaCode).get(divCode);
						} catch (NumberFormatException e) {
							// do nothing - which is the same as making the invalid "number" 0f
						}
						try {
							smallTotal += this.abs8165_0LgaEmployment.get(ABS8165_TITLE_EMPLOYMENT_2)
									.get(states[idxState]).get(lgaCode).get(divCode);
						} catch (NumberFormatException e) {
							// do nothing - which is the same as making the invalid "number" 0f
						}
						try {
							mediumTotal += this.abs8165_0LgaEmployment.get(ABS8165_TITLE_EMPLOYMENT_3)
									.get(states[idxState]).get(lgaCode).get(divCode);
						} catch (NumberFormatException e) {
							// do nothing - which is the same as making the invalid "number" 0f
						}
						try {
							largeTotal += this.abs8165_0LgaEmployment.get(ABS8165_TITLE_EMPLOYMENT_4)
									.get(states[idxState]).get(lgaCode).get(divCode);
						} catch (NumberFormatException e) {
							// do nothing - which is the same as making the invalid "number" 0f
						}

						/*
						 * CHECKME: Loop over this section, using multiples of epsilon until the number
						 * of agents equals the number in the ABS8165 LGA data. Need to do this at a
						 * higher level though, not industry level. Would need to re-factor the loops,
						 * which is too hard for now. I can just manually tweak the EPSILON constant for
						 * now until the number of business agents created is about right.
						 */
						// calculate number of business agents to instantiate
						int numSmallBusinesses = (int) Math
								.round(smallTotal * industryDivisionRatios[idxState][0][idxDiv][idxClass]
										* industryCodeClassRatio4B.get(industryCode) + EPSILON);
						int numMediumBusinesses = (int) Math
								.round(mediumTotal * industryDivisionRatios[idxState][1][idxDiv][idxClass]
										* industryCodeClassRatio4B.get(industryCode) + EPSILON);
						int numLargeBusinesses = (int) Math
								.round(largeTotal * industryDivisionRatios[idxState][2][idxDiv][idxClass]
										* industryCodeClassRatio4B.get(industryCode) + EPSILON);

						// Create agents and add to list
						for (int i = 0; i < numSmallBusinesses; i++) {
							if (agentMatrix[idxState][idxIndustryCode][0] != null) {
								/*
								 * Sometimes the count data has cells populated that the amount data doesn't.
								 * This leads to a null pointer error, so we exclude businesses where there is a
								 * positive count but no corresponding financial information to calibrate the
								 * business with.
								 */
								Business businessAgent = new Business(agentMatrix[idxState][idxIndustryCode][0]);
								businessAgent.setLgaCode(lgaCode);

								// if a Business has no Other Expenses, assign 5% of Total Expenses
								if (businessAgent.getOtherExpenses() <= 0f) {
									float totalExp = businessAgent.getTotalExpenses();
									float otherExp = totalExp * 0.05f;
									businessAgent.setOtherExpenses(otherExp);
									businessAgent.setTotalExpenses(totalExp + otherExp);
								}

								this.businessAgents.add(businessAgent);
								int typeId = businessAgent.getBusinessTypeId();
								this.businessTypeCount[typeId]++;
							}
						}
						for (int i = 0; i < numMediumBusinesses; i++) {
							if (agentMatrix[idxState][idxIndustryCode][1] != null) {
								Business businessAgent = new Business(agentMatrix[idxState][idxIndustryCode][1]);
								businessAgent.setLgaCode(lgaCode);

								// if a Business has no Other Expenses, assign 5% of Total Expenses
								if (businessAgent.getOtherExpenses() <= 0f) {
									float totalExp = businessAgent.getTotalExpenses();
									float otherExp = totalExp * 0.05f;
									businessAgent.setOtherExpenses(otherExp);
									businessAgent.setTotalExpenses(totalExp + otherExp);
								}

								this.businessAgents.add(businessAgent);
								int typeId = businessAgent.getBusinessTypeId();
								this.businessTypeCount[typeId]++;
							}
						}
						for (int i = 0; i < numLargeBusinesses; i++) {
							if (agentMatrix[idxState][idxIndustryCode][2] != null) {
								Business businessAgent = new Business(agentMatrix[idxState][idxIndustryCode][2]);
								businessAgent.setLgaCode(lgaCode);

								// if a Business has no Other Expenses, assign 5% of Total Expenses
								if (businessAgent.getOtherExpenses() <= 0f) {
									float totalExp = businessAgent.getTotalExpenses();
									float otherExp = totalExp * 0.05f;
									businessAgent.setOtherExpenses(otherExp);
									businessAgent.setTotalExpenses(totalExp + otherExp);
								}

								this.businessAgents.add(businessAgent);
								int typeId = businessAgent.getBusinessTypeId();
								this.businessTypeCount[typeId]++;
							}
						}
					}
				}
			}
		}
		if (DEBUG) {
			int exporterCount = 0;
			int importerCount = 0;
			int rentExpenseCount = 0;
			int rentIncomeCount = 0;
			for (int i = 0; i < this.businessAgents.size(); i++) {
				if (this.businessAgents.get(i).getSalesForeign() > 0f) {
					exporterCount++;
				}
				if (this.businessAgents.get(i).getForeignExpenses() > 0f) {
					importerCount++;
				}
				if (this.businessAgents.get(i).getRentExpense() > 0f) {
					rentExpenseCount++;
				}
				if (this.businessAgents.get(i).getRentIncome() > 0f) {
					rentIncomeCount++;
				}
			}
			System.out.println("Created  " + exporterCount + " exporters.");
			System.out.println("Created  " + importerCount + " importers.");
			System.out.println("Created  " + rentExpenseCount + " renters.");
			System.out.println("Created  " + rentIncomeCount + " landlords.");
			Business tmp = this.businessAgents.get(1000000);
			System.out.println("FIRST BUSINESS CONFIG:");
			System.out.println("industryDivisionCode: " + tmp.getIndustryDivisionCode());
			System.out.println("state: " + tmp.getState());
			System.out.println("lgaCode: " + tmp.getLgaCode());
			System.out.println("size: " + tmp.getSize());
			System.out.println("isExporter: " + tmp.isExporter());
			System.out.println("totalIncome: " + tmp.getTotalIncome());
			System.out.println("salesDomestic: " + tmp.getSalesDomestic());
			System.out.println("salesGovernment: " + tmp.getSalesGovernment());
			System.out.println("salesForeign: " + tmp.getSalesForeign());
			System.out.println("interestIncome: " + tmp.getInterestIncome());
			System.out.println("rentIncome: " + tmp.getRentIncome());
			System.out.println("otherIncome: " + tmp.getOtherIncome());
			System.out.println("totalExpenses: " + tmp.getTotalExpenses());
			System.out.println("wageExpenses: " + tmp.getWageExpenses());
			System.out.println("superannuationExpense: " + tmp.getSuperannuationExpense());
			System.out.println("payrollTaxExpense: " + tmp.getPayrollTaxExpense());
			System.out.println("foreignExpenses: " + tmp.getForeignExpenses());
			System.out.println("interestExpense: " + tmp.getInterestExpense());
			System.out.println("rentExpense: " + tmp.getRentExpense());
			System.out.println("depreciationExpense: " + tmp.getDepreciationExpense());
			System.out.println("otherExpenses: " + tmp.getOtherExpenses());
			System.out.println("totalAssets: " + tmp.getTotalAssets());
			System.out.println("bankDeposits: " + tmp.getBankDeposits());
			System.out.println("foreignEquities: " + tmp.getForeignEquities());
			System.out.println("otherFinancialAssets: " + tmp.getOtherFinancialAssets());
			System.out.println("otherNonFinancialAssets: " + tmp.getOtherNonFinancialAssets());
			System.out.println("totalLiabilities: " + tmp.getTotalLiabilities());
			System.out.println("tradeCreditors: " + tmp.getTradeCreditors());
			System.out.println("loans: " + tmp.getLoans());
			System.out.println("otherCurrentLiabilities: " + tmp.getOtherCurrentLiabilities());
			System.out.println("otherNonCurrentLiabilities: " + tmp.getOtherNonCurrentLiabilities());
			System.out.println("totalEquity: " + tmp.getTotalEquity());

		}

		System.out.println("Adding business agents to economy: " + new Date(System.currentTimeMillis()));
		// Add agents to economy
		this.addAgentsToEconomy();
	}

	private void addAgentsToEconomy() {
		this.economy.setBusinesses(this.businessAgents);
		this.economy.setBusinessTypeCount(this.businessTypeCount);

		/*
		 * Release memory by deleting the data maps used to create these agents. Doesn't
		 * guarantee the garbage collector will run, but should make the memory
		 * available to collect when it does run.
		 */
		this.businessData.close();
		this.businessData = null;
		System.gc();
	}

	/**
	 * @param properties the properties to set
	 */
	@Autowired
	public void setProperties(Properties properties) {
		this.properties = properties;
	}

	/**
	 * @param data the calibration data to set
	 */
	@Autowired
	public void setCalibrationData(CalibrationData data) {
		this.data = data;
	}

	/**
	 * @param businessData the businessData to set
	 */
	@Autowired
	public void setBusinessData(CalibrationDataBusiness businessData) {
		this.businessData = businessData;
	}

	/**
	 * @param area the area to set
	 */
	@Autowired
	public void setAreaMapping(AreaMapping area) {
		this.area = area;
	}

	/**
	 * @param economy the economy to set
	 */
	@Autowired
	public void setAustralianEconomy(AustralianEconomy economy) {
		this.economy = economy;
	}

	/**
	 * @return the businessAgents
	 */
	public List<Business> getBusinessAgents() {
		if (businessAgents == null) {
			this.createBusinessAgents();
		}
		return businessAgents;
	}
}
