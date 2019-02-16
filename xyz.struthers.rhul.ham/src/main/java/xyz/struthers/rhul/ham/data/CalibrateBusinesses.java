/**
 * 
 */
package xyz.struthers.rhul.ham.data;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;

import xyz.struthers.rhul.ham.agent.Business;
import xyz.struthers.rhul.ham.config.Properties;
import xyz.struthers.rhul.ham.process.AustralianEconomy;

/**
 * @author Adam Struthers
 * @since 12-Feb-2019
 */
public class CalibrateBusinesses {

	// constants
	public static final String RBA_E1_DATE = "Jun-2018";

	public static final String ABS8155_YEAR = "2016-17";
	public static final String ABS8155_TITLE_EMPLOYMENT = "Employment at end of June";
	public static final String ABS8155_TITLE_WAGES = "Wages and salaries";
	public static final String ABS8155_TITLE_SALES = "Sales and service income";
	public static final String ABS8155_TITLE_INCOME = "Total income";
	public static final String ABS8155_TITLE_EXPENSES = "Total expenses";

	public static final String ABS8165_TITLE_EMPLOYMENT_1 = "Non employing";
	public static final String ABS8165_TITLE_EMPLOYMENT_2 = "1-19 Employees";
	public static final String ABS8165_TITLE_EMPLOYMENT_3 = "20-199 Employees";
	public static final String ABS8165_TITLE_EMPLOYMENT_4 = "200+ Employees";

	private static final double MILLION = 1000000d;
	private static final double THOUSAND = 1000d;

	// beans
	private Properties properties;
	private CalibrationData data;
	private AreaMapping area;
	private AustralianEconomy economy;
	private List<Business> businessAgents;

	// data sets
	/**
	 * AU Bal Sht totals<br>
	 * Keys: Series, Date
	 */
	private Map<String, Map<Date, String>> rbaE1;
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
	 * formatted export data<br>
	 * Keys: industry, state, country, value range
	 */
	private Map<String, Map<String, Map<String, Map<String, String>>>> abs5368_0Exporters;
	/**
	 * Business size by division<br>
	 * Keys: year, column title, size, industry
	 */
	private Map<String, Map<String, Map<String, Map<String, String>>>> abs8155_0Table5;
	/**
	 * States by division<br>
	 * Keys: year, column title, state, industry
	 */
	private Map<String, Map<String, Map<String, Map<String, String>>>> abs8155_0Table6;
	/**
	 * Count by state, industry & employment range<br>
	 * Keys: employment range, state, industry class code
	 */
	private Map<String, Map<String, Map<String, String>>> abs8165_0StateEmployment;
	/**
	 * Count by state, industry & turnover range<br>
	 * Keys: turnover range, state, industry class code
	 */
	private Map<String, Map<String, Map<String, String>>> abs8165_0StateTurnover;
	/**
	 * Count by LGA, industry & employment range<br>
	 * Keys: employment range, state acronym, LGA code, industry division code
	 */
	private Map<String, Map<String, Map<String, Map<String, String>>>> abs8165_0LgaEmployment;
	/**
	 * Count by LGA, industry & turnover range<br>
	 * Keys: turnover range, state acronym, LGA code, industry division code
	 */
	private Map<String, Map<String, Map<String, Map<String, String>>>> abs8165_0LgaTurnover;
	/**
	 * ATO Fine Industry Detailed P&L and Bal Sht<br>
	 * Keys: column title, fine industry code
	 */
	private Map<String, Map<String, String>> atoCompanyTable4a;
	/**
	 * Industry Code Total P&L<br>
	 * Keys: column title, business industry code
	 */
	private Map<String, Map<String, String>> atoCompanyTable4b;

	/**
	 * 
	 */
	public CalibrateBusinesses() {
		super();
		this.init();
	}

	/**
	 * FIXME: implement me (to create all the Business agents)
	 * 
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
	 * rough algorithm:
	 * 
	 * TODO: count how many times each agent is instantiated when assigned to LGAs.
	 * This will allow me to do a frequency histogram to show the degree of
	 * heterogeneity among Business agents.
	 * 
	 */
	public void createBusinessAgents() {
		// get just the relevant business data from the CalibrationData
		this.rbaE1 = this.data.getRbaE1();
		this.abs1292_0_55_002ANZSIC = this.data.getAbs1292_0_55_002ANZSIC();
		this.abs5368_0Exporters = this.data.getAbs5368_0Exporters();
		this.abs8155_0Table5 = this.data.getAbs8155_0Table5();
		this.abs8155_0Table6 = this.data.getAbs8155_0Table6();
		this.abs8165_0StateEmployment = this.data.getAbs8165_0StateEmployment();
		this.abs8165_0StateTurnover = this.data.getAbs8165_0StateTurnover();
		this.abs8165_0LgaEmployment = this.data.getAbs8165_0LgaEmployment();
		this.abs8165_0LgaTurnover = this.data.getAbs8165_0LgaTurnover();
		this.atoCompanyTable4a = this.data.getAtoCompanyTable4a();
		this.atoCompanyTable4b = this.data.getAtoCompanyTable4b();

		// create businesses
		this.businessAgents = new ArrayList<Business>();

		/*
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
		Set<String> fineIndustryKeySet = new HashSet<String>(this.atoCompanyTable4a.get("Total Income3 no.").keySet());
		int numFineIndustryKeys = fineIndustryKeySet.size();
		Map<String, Integer> fineIndustryKeyIndex = new HashMap<String, Integer>(numFineIndustryKeys);
		String[] industryGroupCodes = fineIndustryKeySet.stream().toArray(String[]::new);

		// company wages multiplier
		Map<String, String> industryDivisionMap4A = new HashMap<String, String>(198); // maps ANZSIC Group Code to
																						// Division Code
		for (String industryGroupCode : industryGroupCodes) {
			industryDivisionMap4A.put(industryGroupCode,
					this.abs1292_0_55_002ANZSIC.get("Group Code to Division Code").get(industryGroupCode));
		}
		String[] industryDivisions4A = industryDivisionMap4A.keySet().stream().toArray(String[]::new);
		Map<String, Double> wagesPerIndustryDivision = new HashMap<String, Double>(industryDivisions4A.length);
		Map<String, Integer> numberOfGroupsInDivision = new HashMap<String, Integer>(industryDivisions4A.length);
		for (String division : industryDivisions4A) {
			wagesPerIndustryDivision.put(division, 0d);
			numberOfGroupsInDivision.put(division, 0);
		}
		double[] wagesDivisionMultiplierPerGroup = new double[numFineIndustryKeys];

		// company financials
		double[] totalIncomePerCompany = new double[numFineIndustryKeys];
		double[] salesRatio = new double[numFineIndustryKeys];
		double[] interestIncomeRatio = new double[numFineIndustryKeys];
		double[] rentIncomeRatio = new double[numFineIndustryKeys];
		double[] governmentIncomeRatio = new double[numFineIndustryKeys];
		double[] foreignIncomeRatio = new double[numFineIndustryKeys];

		double[] totalExpensePerCompany = new double[numFineIndustryKeys];
		double[] costOfSalesRatio = new double[numFineIndustryKeys];
		double[] rentLeaseExpenseRatio = new double[numFineIndustryKeys];
		double[] interestExpenseRatio = new double[numFineIndustryKeys];
		double[] foreignInterestExpenseRatio = new double[numFineIndustryKeys];
		double[] depreciationExpenseRatio = new double[numFineIndustryKeys];
		double[] salaryWageRatio = new double[numFineIndustryKeys];
		double[] salaryWagePerGroup = new double[numFineIndustryKeys];

		double[] totalAssetsPerCompany = new double[numFineIndustryKeys];
		double[] totalAssetsToIncomeRatio = new double[numFineIndustryKeys];
		double[] currentAssetsRatio = new double[numFineIndustryKeys];

		double[] totalLiabilitiesPerCompany = new double[numFineIndustryKeys];
		double[] totalLiabilitiesToIncomeRatio = new double[numFineIndustryKeys];
		double[] tradeCreditorsRatio = new double[numFineIndustryKeys];
		double[] currentLiabilitiesRatio = new double[numFineIndustryKeys];
		double[] debtRatio = new double[numFineIndustryKeys];

		for (int i = 0; i < numFineIndustryKeys; i++) {
			int count = 0;
			double amount = 0d;
			double amountPerCompany = 0d;
			String key = industryGroupCodes[i];
			fineIndustryKeyIndex.put(key, i); // so we can look up the index cheaply in the next step of the algorithm

			// TOTAL INCOME
			count = Integer.valueOf(this.atoCompanyTable4a.get("Total Income3 no.").get(key).replace(",", ""));
			amount = Double.valueOf(this.atoCompanyTable4a.get("Total Income3 $").get(key).replace(",", ""));
			totalIncomePerCompany[i] = count == 0 ? 0d : amount / count;

			// Sales
			count = Integer.valueOf(
					this.atoCompanyTable4a.get("Other sales of goods and services no.").get(key).replace(",", ""));
			amount = Double.valueOf(
					this.atoCompanyTable4a.get("Other sales of goods and services $").get(key).replace(",", ""));
			amountPerCompany = count == 0 ? 0d : amount / count;
			salesRatio[i] = totalIncomePerCompany[i] == 0 ? 0d : amountPerCompany / totalIncomePerCompany[i];

			// Interest Income
			count = Integer.valueOf(this.atoCompanyTable4a.get("Gross Interest no.").get(key).replace(",", ""));
			amount = Double.valueOf(this.atoCompanyTable4a.get("Gross Interest $").get(key).replace(",", ""));
			amountPerCompany = count == 0 ? 0d : amount / count;
			interestIncomeRatio[i] = totalIncomePerCompany[i] == 0 ? 0d : amountPerCompany / totalIncomePerCompany[i];

			// Rent Income
			count = Integer.valueOf(this.atoCompanyTable4a.get("Gross rent and other leasing and hiring income no.")
					.get(key).replace(",", ""));
			amount = Double.valueOf(this.atoCompanyTable4a.get("Gross rent and other leasing and hiring income $")
					.get(key).replace(",", ""));
			amountPerCompany = count == 0 ? 0d : amount / count;
			rentIncomeRatio[i] = totalIncomePerCompany[i] == 0 ? 0d : amountPerCompany / totalIncomePerCompany[i];

			// Government Income
			count = Integer.valueOf(this.atoCompanyTable4a.get("Assessable government industry payments no.").get(key)
					.replace(",", ""));
			amount = Double.valueOf(
					this.atoCompanyTable4a.get("Assessable government industry payments $").get(key).replace(",", ""));
			amountPerCompany = count == 0 ? 0d : amount / count;
			governmentIncomeRatio[i] = totalIncomePerCompany[i] == 0 ? 0d : amountPerCompany / totalIncomePerCompany[i];

			// Foreign Income
			count = Integer.valueOf(this.atoCompanyTable4a.get("Gross foreign income no.").get(key).replace(",", ""));
			amount = Double.valueOf(this.atoCompanyTable4a.get("Gross foreign income $").get(key).replace(",", ""));
			amountPerCompany = count == 0 ? 0d : amount / count;
			foreignIncomeRatio[i] = totalIncomePerCompany[i] == 0 ? 0d : amountPerCompany / totalIncomePerCompany[i];

			// TOTAL EXPENSES
			count = Integer.valueOf(this.atoCompanyTable4a.get("Total expenses no.").get(key).replace(",", ""));
			amount = Double.valueOf(this.atoCompanyTable4a.get("Total expenses $").get(key).replace(",", ""));
			totalExpensePerCompany[i] = count == 0 ? 0d : amount / count;

			// Cost of Sales
			count = Integer.valueOf(this.atoCompanyTable4a.get("Cost of sales no.").get(key).replace(",", ""));
			amount = Double.valueOf(this.atoCompanyTable4a.get("Cost of sales $").get(key).replace(",", ""));
			amountPerCompany = count == 0 ? 0d : amount / count;
			costOfSalesRatio[i] = totalIncomePerCompany[i] == 0 ? 0d : amountPerCompany / totalIncomePerCompany[i];

			// Rent and Lease Expense
			count = Integer.valueOf(
					this.atoCompanyTable4a.get("Lease expenses within Australia no.").get(key).replace(",", ""))
					+ Integer.valueOf(this.atoCompanyTable4a.get("Rent expenses no.").get(key).replace(",", ""));
			amount = Double
					.valueOf(this.atoCompanyTable4a.get("Lease expenses within Australia $").get(key).replace(",", ""))
					+ Double.valueOf(this.atoCompanyTable4a.get("Rent expenses $").get(key).replace(",", ""));
			amountPerCompany = count == 0 ? 0d : amount / count;
			rentLeaseExpenseRatio[i] = totalIncomePerCompany[i] == 0 ? 0d : amountPerCompany / totalIncomePerCompany[i];

			// Interest Expense
			count = Integer.valueOf(
					this.atoCompanyTable4a.get("Interest expenses within Australia no.").get(key).replace(",", ""));
			amount = Double.valueOf(
					this.atoCompanyTable4a.get("Interest expenses within Australia $").get(key).replace(",", ""));
			amountPerCompany = count == 0 ? 0d : amount / count;
			interestExpenseRatio[i] = totalIncomePerCompany[i] == 0 ? 0d : amountPerCompany / totalIncomePerCompany[i];

			// Foreign Interest Expense
			count = Integer
					.valueOf(this.atoCompanyTable4a.get("Interest expenses overseas no.").get(key).replace(",", ""));
			amount = Double
					.valueOf(this.atoCompanyTable4a.get("Interest expenses overseas $").get(key).replace(",", ""));
			amountPerCompany = count == 0 ? 0d : amount / count;
			foreignInterestExpenseRatio[i] = totalIncomePerCompany[i] == 0 ? 0d
					: amountPerCompany / totalIncomePerCompany[i];

			// Depreciation Expense
			count = Integer.valueOf(this.atoCompanyTable4a.get("Depreciation expenses no.").get(key).replace(",", ""));
			amount = Double.valueOf(this.atoCompanyTable4a.get("Depreciation expenses $").get(key).replace(",", ""));
			amountPerCompany = count == 0 ? 0d : amount / count;
			depreciationExpenseRatio[i] = totalIncomePerCompany[i] == 0 ? 0d
					: amountPerCompany / totalIncomePerCompany[i];

			// Salaries and Wages
			count = Integer.valueOf(
					this.atoCompanyTable4a.get("Total salary and wage expenses no.").get(key).replace(",", ""));
			amount = Double
					.valueOf(this.atoCompanyTable4a.get("Total salary and wage expenses $").get(key).replace(",", ""));
			amountPerCompany = count == 0 ? 0d : amount / count;
			salaryWagePerGroup[i] = amountPerCompany;
			salaryWageRatio[i] = totalIncomePerCompany[i] == 0 ? 0d : amountPerCompany / totalIncomePerCompany[i];

			// sum wages by Division so we can get the ratios later
			String thisDivision = industryDivisionMap4A.get(key);
			numberOfGroupsInDivision.put(thisDivision, numberOfGroupsInDivision.get(thisDivision) + 1);
			wagesPerIndustryDivision.put(thisDivision, wagesPerIndustryDivision.get(thisDivision) + amountPerCompany);

			// TOTAL ASSETS
			count = Integer.valueOf(this.atoCompanyTable4a.get("Total assets no.").get(key).replace(",", ""));
			amount = Double.valueOf(this.atoCompanyTable4a.get("Total assets $").get(key).replace(",", ""));
			totalAssetsPerCompany[i] = count == 0 ? 0d : amount / count;
			totalAssetsToIncomeRatio[i] = totalIncomePerCompany[i] == 0 ? 0d
					: totalAssetsPerCompany[i] / totalIncomePerCompany[i];

			// Current Assets
			count = Integer.valueOf(this.atoCompanyTable4a.get("All current assets no.").get(key).replace(",", ""));
			amount = Double.valueOf(this.atoCompanyTable4a.get("All current assets $").get(key).replace(",", ""));
			amountPerCompany = count == 0 ? 0d : amount / count;
			currentAssetsRatio[i] = totalIncomePerCompany[i] == 0 ? 0d : amountPerCompany / totalIncomePerCompany[i];

			// TOTAL LIABILITIES
			count = Integer.valueOf(this.atoCompanyTable4a.get("Total liabilities no.").get(key).replace(",", ""));
			amount = Double.valueOf(this.atoCompanyTable4a.get("Total liabilities $").get(key).replace(",", ""));
			totalLiabilitiesPerCompany[i] = count == 0 ? 0d : amount / count;
			totalLiabilitiesToIncomeRatio[i] = totalIncomePerCompany[i] == 0 ? 0d
					: totalLiabilitiesPerCompany[i] / totalIncomePerCompany[i];

			// Trade Creditors
			count = Integer.valueOf(this.atoCompanyTable4a.get("Trade creditors no.").get(key).replace(",", ""));
			amount = Double.valueOf(this.atoCompanyTable4a.get("Trade creditors $").get(key).replace(",", ""));
			amountPerCompany = count == 0 ? 0d : amount / count;
			tradeCreditorsRatio[i] = totalIncomePerCompany[i] == 0 ? 0d : amountPerCompany / totalIncomePerCompany[i];

			// Current Liabilities
			count = Integer
					.valueOf(this.atoCompanyTable4a.get("All current liabilities no.").get(key).replace(",", ""));
			amount = Double.valueOf(this.atoCompanyTable4a.get("All current liabilities $").get(key).replace(",", ""));
			amountPerCompany = count == 0 ? 0d : amount / count;
			currentLiabilitiesRatio[i] = totalIncomePerCompany[i] == 0 ? 0d
					: amountPerCompany / totalIncomePerCompany[i];

			// Debt
			count = Integer.valueOf(this.atoCompanyTable4a.get("Total debt no.").get(key).replace(",", ""));
			amount = Double.valueOf(this.atoCompanyTable4a.get("Total debt $").get(key).replace(",", ""));
			amountPerCompany = count == 0 ? 0d : amount / count;
			debtRatio[i] = totalIncomePerCompany[i] == 0 ? 0d : amountPerCompany / totalIncomePerCompany[i];

		}
		// calculate wages ratios
		for (int i = 0; i < numFineIndustryKeys; i++) {
			String key = industryGroupCodes[i];
			String thisDivision = industryDivisionMap4A.get(key);
			double meanWagesPerDivision = wagesPerIndustryDivision.get(thisDivision)
					/ numberOfGroupsInDivision.get(thisDivision);
			wagesDivisionMultiplierPerGroup[i] = salaryWagePerGroup[i] / meanWagesPerDivision;
		}

		/*
		 * 2. ATO Company Table 4B: Calculate per-company figures, then multiply by the
		 * ratios from 4A to get a more detailed P&L. This gives per-company P&L for 574
		 * industries. Calculate the ratio of the number of companies in each of the
		 * relevant industry codes for each of the 18 industry divisions. This allows us
		 * to break the ABS data for 18 industries down into 574 industry codes.
		 */
		Set<String> industryCodeKeySet = new HashSet<String>(
				this.atoCompanyTable4b.get("Number of companies").keySet());
		int numIndustryCodeKeys = industryCodeKeySet.size();
		String[] industryCodes = industryCodeKeySet.stream().toArray(String[]::new);

		int[] numberOfCompanies = new int[numIndustryCodeKeys];
		double[] wagesDivisionMultiplierPerIndustryCode = new double[numIndustryCodeKeys];

		// mean amounts per company
		double[] totalIncome = new double[numIndustryCodeKeys];
		double[] sales = new double[numIndustryCodeKeys];
		double[] interestIncome = new double[numIndustryCodeKeys];
		double[] rentIncome = new double[numIndustryCodeKeys];
		double[] governmentIncome = new double[numIndustryCodeKeys];
		double[] foreignIncome = new double[numIndustryCodeKeys];

		double[] totalExpense = new double[numIndustryCodeKeys];
		double[] costOfSales = new double[numIndustryCodeKeys];
		double[] rentLeaseExpense = new double[numIndustryCodeKeys];
		double[] interestExpense = new double[numIndustryCodeKeys];
		double[] foreignInterestExpense = new double[numIndustryCodeKeys];
		double[] depreciationExpense = new double[numIndustryCodeKeys];
		double[] salaryWage = new double[numIndustryCodeKeys];

		double[] totalAssets = new double[numIndustryCodeKeys];
		double[] currentAssets = new double[numIndustryCodeKeys];

		double[] totalLiabilities = new double[numIndustryCodeKeys];
		double[] tradeCreditors = new double[numIndustryCodeKeys];
		double[] currentLiabilities = new double[numIndustryCodeKeys];
		double[] debt = new double[numIndustryCodeKeys];

		for (int i = 0; i < numIndustryCodeKeys; i++) {
			int count = 0;
			double amount = 0d;
			double amountPerCompany = 0d;
			String key = industryCodes[i];
			int fineIndustryIndex = fineIndustryKeyIndex.get(key.substring(0, 3));

			// NUMBER OF COMPANIES
			String tmp = this.atoCompanyTable4b.get("Number of companies").get(key).replace(",", "");
			// change all "<10" values into 5b
			numberOfCompanies[i] = Integer.valueOf(tmp.substring(0, 1).equals("<") ? "5" : tmp);

			// TOTAL INCOME
			count = Integer.valueOf(this.atoCompanyTable4b.get("Total income no.").get(key).replace(",", ""));
			amount = Double.valueOf(this.atoCompanyTable4b.get("Total income $").get(key).replace(",", ""));
			totalIncome[i] = count == 0 ? 0d : amount / count;

			// Calculate income line items
			sales[i] = salesRatio[fineIndustryIndex] * totalIncome[i];
			interestIncome[i] = interestIncomeRatio[fineIndustryIndex] * totalIncome[i];
			rentIncome[i] = rentIncomeRatio[fineIndustryIndex] * totalIncome[i];
			governmentIncome[i] = governmentIncomeRatio[fineIndustryIndex] * totalIncome[i];
			foreignIncome[i] = foreignIncomeRatio[fineIndustryIndex] * totalIncome[i];

			// TOTAL EXPENSES
			count = Integer.valueOf(this.atoCompanyTable4b.get("Total expenses no.").get(key).replace(",", ""));
			amount = Double.valueOf(this.atoCompanyTable4b.get("Total expenses $").get(key).replace(",", ""));
			totalExpense[i] = count == 0 ? 0d : amount / count;

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
		 * 3. ABS 8155.0 Table 6: Calculate ratios between states (for employment count,
		 * wages and sales) by industry.
		 * 
		 * Keys: year, column title, state, industry
		 */

		String[] states = { "NSW", "VIC", "QLD", "SA", "WA", "TAS", "NT", "ACT", "Other" };
		Set<String> industriesSet8155 = this.abs8155_0Table6.get(ABS8155_YEAR).get(ABS8155_TITLE_EMPLOYMENT)
				.get(states[0]).keySet();
		industriesSet8155.remove("Other services");
		industriesSet8155.remove("Total selected industries");
		String[] industries8155 = industriesSet8155.stream().toArray(String[]::new);
		double[] totalStateEmploymentByIndustry = new double[industries8155.length];
		double[] totalStateWagesByIndustry = new double[industries8155.length];
		double[] totalStateSalesByIndustry = new double[industries8155.length];
		double[][] stateRatioEmploymentCount = new double[industries8155.length][states.length];
		double[][] stateRatioWages = new double[industries8155.length][states.length];
		double[][] stateRatioSales = new double[industries8155.length][states.length];

		// calculate totals first so the ratios can be calculated
		for (String thisState : states) {
			for (int i = 0; i < industries8155.length; i++) {
				totalStateEmploymentByIndustry[i] += Double.valueOf(this.abs8155_0Table6.get(ABS8155_YEAR)
						.get(ABS8155_TITLE_EMPLOYMENT).get(thisState).get(industries8155[i]).replace(",", ""))
						* THOUSAND;
				totalStateWagesByIndustry[i] += Double.valueOf(this.abs8155_0Table6.get(ABS8155_YEAR)
						.get(ABS8155_TITLE_WAGES).get(thisState).get(industries8155[i]).replace(",", "")) * MILLION;
				totalStateSalesByIndustry[i] += Double.valueOf(this.abs8155_0Table6.get(ABS8155_YEAR)
						.get(ABS8155_TITLE_SALES).get(thisState).get(industries8155[i]).replace(",", "")) * MILLION;
			}
		}
		// calculate ratios between states for each industry
		for (int i = 0; i < industries8155.length; i++) {
			for (int j = 0; j < states.length; j++) {
				double cellValue = Double.valueOf(this.abs8155_0Table6.get(ABS8155_YEAR).get(ABS8155_TITLE_EMPLOYMENT)
						.get(states[j]).get(industries8155[i]).replace(",", "")) * THOUSAND;
				stateRatioEmploymentCount[i][j] = totalStateEmploymentByIndustry[i] > 0d
						? cellValue / totalStateEmploymentByIndustry[i]
						: 0d;
				cellValue = Double.valueOf(this.abs8155_0Table6.get(ABS8155_YEAR).get(ABS8155_TITLE_WAGES)
						.get(states[j]).get(industries8155[i]).replace(",", "")) * MILLION;
				stateRatioWages[i][j] = totalStateWagesByIndustry[i] > 0d ? cellValue / totalStateWagesByIndustry[i]
						: 0d;
				cellValue = Double.valueOf(this.abs8155_0Table6.get(ABS8155_YEAR).get(ABS8155_TITLE_SALES)
						.get(states[j]).get(industries8155[i]).replace(",", "")) * MILLION;
				stateRatioSales[i][j] = totalStateSalesByIndustry[i] > 0d ? cellValue / totalStateSalesByIndustry[i]
						: 0d;

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

		String[] sizes = { "S", "M", "L" };
		double[] totalSizeEmploymentByIndustry = new double[industries8155.length];
		double[] totalSizeWagesByIndustry = new double[industries8155.length];
		double[] totalSizeSalesByIndustry = new double[industries8155.length];
		double[] totalIncomeByIndustry = new double[industries8155.length];
		double[] totalExpensesByIndustry = new double[industries8155.length];
		double[][][] employmentCountByStateIndustrySize = new double[states.length][industries8155.length][sizes.length];
		double[][][] wagesByStateIndustrySize = new double[states.length][industries8155.length][sizes.length];
		double[][][] salesByStateIndustrySize = new double[states.length][industries8155.length][sizes.length];
		double[][][] totalIncomeByStateIndustrySize = new double[states.length][industries8155.length][sizes.length];
		double[][][] totalExpensesByStateIndustrySize = new double[states.length][industries8155.length][sizes.length];

		// calculate totals first so the ratios can be calculated
		for (String thisSize : sizes) {
			for (int i = 0; i < industries8155.length; i++) {
				totalSizeEmploymentByIndustry[i] += Double.valueOf(this.abs8155_0Table5.get(ABS8155_YEAR)
						.get(ABS8155_TITLE_EMPLOYMENT).get(thisSize).get(industries8155[i]).replace(",", ""))
						* THOUSAND;
				totalSizeWagesByIndustry[i] += Double.valueOf(this.abs8155_0Table5.get(ABS8155_YEAR)
						.get(ABS8155_TITLE_WAGES).get(thisSize).get(industries8155[i]).replace(",", "")) * MILLION;
				totalSizeSalesByIndustry[i] += Double.valueOf(this.abs8155_0Table5.get(ABS8155_YEAR)
						.get(ABS8155_TITLE_SALES).get(thisSize).get(industries8155[i]).replace(",", "")) * MILLION;
				totalIncomeByIndustry[i] += Double.valueOf(this.abs8155_0Table5.get(ABS8155_YEAR)
						.get(ABS8155_TITLE_INCOME).get(thisSize).get(industries8155[i]).replace(",", "")) * MILLION;
				totalExpensesByIndustry[i] += Double.valueOf(this.abs8155_0Table5.get(ABS8155_YEAR)
						.get(ABS8155_TITLE_EXPENSES).get(thisSize).get(industries8155[i]).replace(",", "")) * MILLION;
			}
		}
		// apply state ratios to sizes, joining on industry
		for (int idxIndustry = 0; idxIndustry < industries8155.length; idxIndustry++) {
			for (int idxSize = 0; idxSize < sizes.length; idxSize++) {
				double industrySizeEmployment = Double
						.valueOf(this.abs8155_0Table5.get(ABS8155_YEAR).get(ABS8155_TITLE_EMPLOYMENT)
								.get(sizes[idxSize]).get(industries8155[idxIndustry]).replace(",", ""))
						* THOUSAND;
				double industrySizeWages = Double.valueOf(this.abs8155_0Table5.get(ABS8155_YEAR)
						.get(ABS8155_TITLE_WAGES).get(sizes[idxSize]).get(industries8155[idxIndustry]).replace(",", ""))
						* MILLION;
				double industrySizeSales = Double.valueOf(this.abs8155_0Table5.get(ABS8155_YEAR)
						.get(ABS8155_TITLE_SALES).get(sizes[idxSize]).get(industries8155[idxIndustry]).replace(",", ""))
						* MILLION;
				double industrySizeTotalIncome = Double
						.valueOf(this.abs8155_0Table5.get(ABS8155_YEAR).get(ABS8155_TITLE_INCOME).get(sizes[idxSize])
								.get(industries8155[idxIndustry]).replace(",", ""))
						* MILLION;
				double industrySizeTotalExpenses = Double
						.valueOf(this.abs8155_0Table5.get(ABS8155_YEAR).get(ABS8155_TITLE_EXPENSES).get(sizes[idxSize])
								.get(industries8155[idxIndustry]).replace(",", ""))
						* MILLION;
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
				}
			}
		}
		// we now have total $ amounts by state, industry, and size.

		/**
		 * 5. ABS 8165.0 LGA Employment Range: Get the number of businesses per state /
		 * industry / size then divide the dollar amounts from 8155.0 to determine the
		 * mean dollars per business. Divide the number of employees from 8155.0 by the
		 * number of businesses in 8165.0 to get the mean employees per business by
		 * state / industry / size. Also calculate a total national mean dollars and
		 * employees per business.
		 */
		double businessCountAU = 0d;
		double employmentCountAU = 0d;
		double wagesAU = 0d;
		double salesAU = 0d;
		double totalIncomeAU = 0d;
		double totalExpensesAU = 0d;
		double[][][] businessCountByStateIndustrySize = new double[states.length][industries8155.length][sizes.length];
		double[][][] employmentCountPerBusiness = new double[states.length][industries8155.length][sizes.length];
		double[][][] wagesPerBusiness = new double[states.length][industries8155.length][sizes.length];
		double[][][] salesPerBusiness = new double[states.length][industries8155.length][sizes.length];
		double[][][] totalIncomePerBusiness = new double[states.length][industries8155.length][sizes.length];
		double[][][] totalExpensesPerBusiness = new double[states.length][industries8155.length][sizes.length];
		Set<String> lgaCodes8165 = this.abs8165_0LgaEmployment.get(ABS8165_TITLE_EMPLOYMENT_1).get(states[0]).keySet();
		for (int idxIndustry = 0; idxIndustry < industries8155.length; idxIndustry++) {
			for (int idxState = 0; idxState < states.length; idxState++) {
				// count number of businesses in each category
				double smallCount = 0d;
				double mediumCount = 0d;
				double largeCount = 0d;
				for (String lgaCode : lgaCodes8165) {
					smallCount += Double.valueOf(this.abs8165_0LgaEmployment.get(ABS8165_TITLE_EMPLOYMENT_1)
							.get(states[idxState]).get(lgaCode).get(industries8155[idxIndustry]).replace(",", ""));
					smallCount += Double.valueOf(this.abs8165_0LgaEmployment.get(ABS8165_TITLE_EMPLOYMENT_2)
							.get(states[idxState]).get(lgaCode).get(industries8155[idxIndustry]).replace(",", ""));
					mediumCount += Double.valueOf(this.abs8165_0LgaEmployment.get(ABS8165_TITLE_EMPLOYMENT_3)
							.get(states[idxState]).get(lgaCode).get(industries8155[idxIndustry]).replace(",", ""));
					largeCount += Double.valueOf(this.abs8165_0LgaEmployment.get(ABS8165_TITLE_EMPLOYMENT_4)
							.get(states[idxState]).get(lgaCode).get(industries8155[idxIndustry]).replace(",", ""));
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
		double employmentCountPerBusinessAU = employmentCountAU / businessCountAU;
		double wagesPerBusinessAU = wagesAU / businessCountAU;
		double salesPerBusinessAU = salesAU / businessCountAU;
		double totalIncomePerBusinessAU = totalIncomeAU / businessCountAU;
		double totalExpensesPerBusinessAU = totalExpensesAU / businessCountAU;

		/**
		 * 6. Divide each state / industry / size's figures by the national average to
		 * produce a multiplier for each category combination.
		 */
		double[][][] employmentCountMultiplier = new double[states.length][industries8155.length][sizes.length];
		double[][][] wagesMultiplier = new double[states.length][industries8155.length][sizes.length];
		double[][][] salesMultiplier = new double[states.length][industries8155.length][sizes.length];
		double[][][] totalIncomeMultiplier = new double[states.length][industries8155.length][sizes.length];
		double[][][] totalExpensesMultiplier = new double[states.length][industries8155.length][sizes.length];
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
				}
			}
		}

		/**
		 * 7. RBA E1: For the national total balance sheet, calculate the ratio of
		 * business bank deposits to total financial assets, and business foreign
		 * equities to total financial assets. Below we will assume that total financial
		 * assets and current assets are equivalent, and use these ratios to estimate
		 * the bank deposits and foreign equities held by businesses. We further assume
		 * that only large businesses hold foreign equities (i.e. multi-national
		 * corporations and large investment firms).
		 */

		double bankDepositsE1 = Double.valueOf(this.rbaE1.get("Business bank deposits").get(RBA_E1_DATE));
		double foreignEquitiesE1 = Double.valueOf(this.rbaE1.get("Business foreign equities").get(RBA_E1_DATE));
		double totalFinancialAssetsE1 = Double
				.valueOf(this.rbaE1.get("Business total financial assets").get(RBA_E1_DATE));
		double bankDepositRatioE1 = totalFinancialAssetsE1 > 0d ? bankDepositsE1 / totalFinancialAssetsE1 : 0d;
		double foreignEquitiesRatioE1 = totalFinancialAssetsE1 > 0d ? foreignEquitiesE1 / totalFinancialAssetsE1 : 0d;

		/**
		 * 8. For each state / industry / size, multiply the ATO industry figures by
		 * this multiplier to calibrate the ATO company P&L and Bal Shts so they're now
		 * representative of businesses per state / industry / size.
		 */
		// make a map so we can look up division indices cheaply
		Map<String, Integer> divisionCodeKeyIndex = new HashMap<String, Integer>(industries8155.length);
		for (int i = 0; i < industries8155.length; i++) {
			divisionCodeKeyIndex.put(industries8155[i], i);
		}
		int businessTypeId = 1;
		Business[][][] agentMatrix = new Business[states.length][numIndustryCodeKeys][sizes.length];
		for (int i = 0; i < numIndustryCodeKeys; i++) { // for all 574 ATO industry codes
			// get the industries8155 index (18 values) for this industry code (574 values)
			String divisionCode = this.abs1292_0_55_002ANZSIC.get("Industry Code to Division Code")
					.get(industryCodes[i]);
			if (!divisionCode.equals("K")) { // deal with financial services separately
				int divisionCodeIndex8155 = divisionCodeKeyIndex.get(divisionCode);
				for (int idxState = 0; idxState < states.length; idxState++) {
					for (int idxSize = 0; idxSize < sizes.length; idxSize++) {
						double empMult = employmentCountMultiplier[idxState][divisionCodeIndex8155][idxSize];
						double wageMult = wagesMultiplier[idxState][divisionCodeIndex8155][idxSize];
						double saleMult = salesMultiplier[idxState][divisionCodeIndex8155][idxSize];
						double incMult = totalIncomeMultiplier[idxState][divisionCodeIndex8155][idxSize];
						double expMult = totalExpensesMultiplier[idxState][divisionCodeIndex8155][idxSize];

						// TODO: multiply ABS 8155 employee counts by an ATO Table 4B multiplier based
						// on the ratio of company wages to AU average wages.

						// multiply P&L lines by the relevant multipliers
						double agentTotalIncome = totalIncome[i] * incMult;
						double agentSales = sales[i] * saleMult;
						double agentInterestIncome = interestIncome[i] * incMult;
						double agentRentIncome = rentIncome[i] * incMult;
						double agentGovernmentIncome = governmentIncome[i] * incMult;
						double agentForeignIncome = foreignIncome[i] * incMult;

						double agentTotalExpense = totalExpense[i] * expMult;
						double agentCostOfSales = costOfSales[i] * expMult;
						double agentRentLeaseExpense = rentLeaseExpense[i] * expMult;
						double agentInterestExpense = interestExpense[i] * expMult;
						double agentForeignInterestExpense = foreignInterestExpense[i] * expMult;
						double agentDepreciationExpense = depreciationExpense[i] * expMult;
						double agentSalaryWage = salaryWage[i] * wageMult;

						// multiply Bal sht items by the income multiplier because that's the method
						// used in step 2 above
						double agentTotalAssets = totalAssets[i] * incMult;
						double agentCurrentAssets = currentAssets[i] * incMult;

						double agentTotalLiabilities = totalLiabilities[i] * incMult;
						double agentTradeCreditors = tradeCreditors[i] * incMult;
						double agentCurrentLiabilities = currentLiabilities[i] * incMult;
						double agentDebt = debt[i] * incMult;

						// create representative agent
						Business agent = new Business();
						agent.setBusinessTypeId(businessTypeId++);
						agent.setIndustryCode(industryCodes[i]);
						agent.setIndustryDivisionCode(divisionCode.charAt(0));

						agent.setSize(sizes[idxSize].charAt(0));
						agent.setExporter(false);

						// set employee count target
						agent.setEmployeeCountTarget(
								(int) Math.round(employmentCountPerBusiness[idxState][divisionCodeIndex8155][idxSize]
										* wagesDivisionMultiplierPerIndustryCode[i]));
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
						agentMatrix[idxState][i][idxSize] = agent;
					}
				}
			}
		}

		/**
		 * -----------------------------------------------------<br>
		 * PART D: ASSIGN BUSINESSES TO LGA BY SIZE AND INDUSTRY<br>
		 * -----------------------------------------------------<br>
		 */
		// FIXME: <<< UP TO HERE >>>

		// Create agents and add to economy
		Business businessAgent = new Business();
		this.businessAgents.add(businessAgent);

		this.addAgentsToEconomy();
	}

	private void addAgentsToEconomy() {
		this.economy.setBusinesses(this.businessAgents);
	}

	@PostConstruct
	private void init() {
		this.businessAgents = null;

		this.rbaE1 = null;
		this.abs1292_0_55_002ANZSIC = null;
		this.abs5368_0Exporters = null;
		this.abs8155_0Table5 = null;
		this.abs8155_0Table6 = null;
		this.abs8165_0StateEmployment = null;
		this.abs8165_0StateTurnover = null;
		this.abs8165_0LgaEmployment = null;
		this.abs8165_0LgaTurnover = null;
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
	public void setData(CalibrationData data) {
		this.data = data;
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
	 * @return the businessAgents
	 */
	public List<Business> getBusinessAgents() {
		if (businessAgents == null) {
			this.createBusinessAgents();
		}
		return businessAgents;
	}
}
