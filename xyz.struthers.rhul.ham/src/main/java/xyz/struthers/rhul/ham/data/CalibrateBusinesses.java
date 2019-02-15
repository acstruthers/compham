/**
 * 
 */
package xyz.struthers.rhul.ham.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;

import xyz.struthers.rhul.ham.agent.Business;
import xyz.struthers.rhul.ham.process.AustralianEconomy;

/**
 * @author Adam Struthers
 * @since 12-Feb-2019
 */
public class CalibrateBusinesses {

	// constants
	public static final String ABS8155_YEAR = "2016-17";
	public static final String ABS8155_TITLE_EMPLOYMENT = "Employment at end of June";
	public static final String ABS8155_TITLE_WAGES = "Wages and salaries";
	public static final String ABS8155_TITLE_SALES = "Sales and service income";
	public static final String ABS8155_TITLE_INCOME = "Total income";
	public static final String ABS8155_TITLE_EXPENSES = "Total expenses";

	// beans
	private CalibrationData data;
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
	 * Keys: employment range, state, LGA code, industry division code
	 */
	private Map<String, Map<String, Map<String, Map<String, String>>>> abs8165_0LgaEmployment;
	/**
	 * Count by LGA, industry & turnover range<br>
	 * Keys: turnover range, state, LGA code, industry division code
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
	 * the ratios between the various P&L line items.<br>
	 * 2. ATO Company Table 4B: Calculate per-company figures, then multiply by the
	 * ratios from 4A to get a more detailed P&L. This gives per-company P&L for 574
	 * industries. Calculate the ratio of the number of companies in each of the
	 * relevant industry codes for each of the 18 industry divisions. This allows us
	 * to break the ABS data for 18 industries down into 574 industry codes.
	 * 
	 * N.B. Uses the ratio of assets and liabilities to income to calibrate the
	 * balance sheet based on the P&L statement.
	 * 
	 * We now have representative business P&L and Bal Shts for each industry.
	 * 
	 * ------------------------------------------------------------<br>
	 * PART B: INDUSTRY BUSINESS FINANCIALS SPLIT BY SIZE AND STATE<br>
	 * ------------------------------------------------------------<br>
	 * 3. ABS 8155.0 Table 6: Calculate ratios between states (for employment count,
	 * wages and sales) by industry.<br>
	 * 4. ABS 8155.0 Table 5: Use the ratios from Table 6 to split Size by State,
	 * assuming that all income is split in the same ratio as sales, and all
	 * expenses are split in the same ratio as wages. Calculate gross profit by
	 * subtracting total expenses from total income.
	 * 
	 * We now have total $ amounts by state, industry, and size. 8 states x 574
	 * industries x 3 sizes = 13,776 distinct Business agents
	 * 
	 * ----------------------------------------------------------<br>
	 * PART C: ADJUST INDUSTRY P&L AND BAL SHTS BY SIZE AND STATE<br>
	 * ----------------------------------------------------------<br>
	 * 
	 * rough algorithm: use number of businesses per state / industry / size from
	 * 8165.0 to determine the mean dollars and employees per business from 8155.0.
	 * Also calculate a total national mean dollars and employees per business.
	 * Divide each state / industry / size's figures by the national average to
	 * produce a multiplier. Multiply the ATO industry figures by this multiplier to
	 * calibrate the ATO company P&L and Bal Shts so they're now representative of
	 * businesses per state / industry / size.
	 * 
	 * -----------------------------------------------------<br>
	 * PART D: ASSIGN BUSINESSES TO LGA BY SIZE AND INDUSTRY<br>
	 * -----------------------------------------------------<br>
	 * 
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
		 */
		Set<String> fineIndustryKeySet = new HashSet<String>(this.atoCompanyTable4a.get("Total Income3 no.").keySet());
		int numKeys = fineIndustryKeySet.size();
		Map<String, Integer> fineIndustryKeyIndex = new HashMap<String, Integer>(numKeys);
		String[] industryClassCodes = new String[numKeys];
		industryClassCodes = fineIndustryKeySet.toArray(industryClassCodes);
		double[] totalIncomePerCompany = new double[numKeys];
		double[] salesRatio = new double[numKeys];
		double[] interestIncomeRatio = new double[numKeys];
		double[] rentIncomeRatio = new double[numKeys];
		double[] governmentIncomeRatio = new double[numKeys];
		double[] foreignIncomeRatio = new double[numKeys];

		double[] totalExpensePerCompany = new double[numKeys];
		double[] costOfSalesRatio = new double[numKeys];
		double[] rentLeaseExpenseRatio = new double[numKeys];
		double[] interestExpenseRatio = new double[numKeys];
		double[] foreignInterestExpenseRatio = new double[numKeys];
		double[] depreciationExpenseRatio = new double[numKeys];
		double[] salaryWageRatio = new double[numKeys];

		double[] totalAssetsPerCompany = new double[numKeys];
		double[] totalAssetsToIncomeRatio = new double[numKeys];
		double[] currentAssetsRatio = new double[numKeys];

		double[] totalLiabilitiesPerCompany = new double[numKeys];
		double[] totalLiabilitiesToIncomeRatio = new double[numKeys];
		double[] tradeCreditorsRatio = new double[numKeys];
		double[] currentLiabilitiesRatio = new double[numKeys];
		double[] debtRatio = new double[numKeys];

		for (int i = 0; i < numKeys; i++) {
			int count = 0;
			double amount = 0d;
			double amountPerCompany = 0d;
			String key = industryClassCodes[i];
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
			salaryWageRatio[i] = totalIncomePerCompany[i] == 0 ? 0d : amountPerCompany / totalIncomePerCompany[i];

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

		/*
		 * 2. ATO Company Table 4B: Calculate per-company figures, then multiply by the
		 * ratios from 4A to get a more detailed P&L. This gives per-company P&L for 574
		 * industries. Calculate the ratio of the number of companies in each of the
		 * relevant industry codes for each of the 18 industry divisions. This allows us
		 * to break the ABS data for 18 industries down into 574 industry codes.
		 * 
		 */
		Set<String> industryCodeKeySet = new HashSet<String>(
				this.atoCompanyTable4b.get("Number of companies").keySet());
		numKeys = industryCodeKeySet.size();
		String[] industryCodes = new String[numKeys];
		industryClassCodes = fineIndustryKeySet.toArray(industryClassCodes);

		int[] numberOfCompanies = new int[numKeys];

		double[] totalIncome = new double[numKeys];
		double[] sales = new double[numKeys];
		double[] interestIncome = new double[numKeys];
		double[] rentIncome = new double[numKeys];
		double[] governmentIncome = new double[numKeys];
		double[] foreignIncome = new double[numKeys];

		double[] totalExpense = new double[numKeys];
		double[] costOfSales = new double[numKeys];
		double[] rentLeaseExpense = new double[numKeys];
		double[] interestExpense = new double[numKeys];
		double[] foreignInterestExpense = new double[numKeys];
		double[] depreciationExpense = new double[numKeys];
		double[] salaryWage = new double[numKeys];

		double[] totalAssets = new double[numKeys];
		double[] currentAssets = new double[numKeys];

		double[] totalLiabilities = new double[numKeys];
		double[] tradeCreditors = new double[numKeys];
		double[] currentLiabilities = new double[numKeys];
		double[] debt = new double[numKeys];

		for (int i = 0; i < numKeys; i++) {
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
		Set<String> industriesSet8155 = new HashSet<String>(18);
		industriesSet8155 = this.abs8155_0Table6.get(ABS8155_YEAR).get(ABS8155_TITLE_EMPLOYMENT).get(states[0])
				.keySet();
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
						.get(ABS8155_TITLE_EMPLOYMENT).get(thisState).get(industries8155[i]).replace(",", ""));
				totalStateWagesByIndustry[i] += Double.valueOf(this.abs8155_0Table6.get(ABS8155_YEAR)
						.get(ABS8155_TITLE_WAGES).get(thisState).get(industries8155[i]).replace(",", ""));
				totalStateSalesByIndustry[i] += Double.valueOf(this.abs8155_0Table6.get(ABS8155_YEAR)
						.get(ABS8155_TITLE_SALES).get(thisState).get(industries8155[i]).replace(",", ""));
			}
		}
		// calculate ratios between states for each industry
		for (int i = 0; i < industries8155.length; i++) {
			for (int j = 0; j < states.length; j++) {
				double cellValue = Double.valueOf(this.abs8155_0Table6.get(ABS8155_YEAR).get(ABS8155_TITLE_EMPLOYMENT)
						.get(states[j]).get(industries8155[i]).replace(",", ""));
				stateRatioEmploymentCount[i][j] = totalStateEmploymentByIndustry[i] > 0d
						? cellValue / totalStateEmploymentByIndustry[i]
						: 0d;
				cellValue = Double.valueOf(this.abs8155_0Table6.get(ABS8155_YEAR).get(ABS8155_TITLE_WAGES)
						.get(states[j]).get(industries8155[i]).replace(",", ""));
				stateRatioWages[i][j] = totalStateWagesByIndustry[i] > 0d ? cellValue / totalStateWagesByIndustry[i]
						: 0d;
				cellValue = Double.valueOf(this.abs8155_0Table6.get(ABS8155_YEAR).get(ABS8155_TITLE_SALES)
						.get(states[j]).get(industries8155[i]).replace(",", ""));
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
						.get(ABS8155_TITLE_EMPLOYMENT).get(thisSize).get(industries8155[i]).replace(",", ""));
				totalSizeWagesByIndustry[i] += Double.valueOf(this.abs8155_0Table5.get(ABS8155_YEAR)
						.get(ABS8155_TITLE_WAGES).get(thisSize).get(industries8155[i]).replace(",", ""));
				totalSizeSalesByIndustry[i] += Double.valueOf(this.abs8155_0Table5.get(ABS8155_YEAR)
						.get(ABS8155_TITLE_SALES).get(thisSize).get(industries8155[i]).replace(",", ""));
				totalIncomeByIndustry[i] += Double.valueOf(this.abs8155_0Table5.get(ABS8155_YEAR)
						.get(ABS8155_TITLE_INCOME).get(thisSize).get(industries8155[i]).replace(",", ""));
				totalExpensesByIndustry[i] += Double.valueOf(this.abs8155_0Table5.get(ABS8155_YEAR)
						.get(ABS8155_TITLE_EXPENSES).get(thisSize).get(industries8155[i]).replace(",", ""));
			}
		}
		// apply state ratios to sizes, joining on industry
		for (int idxIndustry = 0; idxIndustry < industries8155.length; idxIndustry++) {
			for (int idxSize = 0; idxSize < sizes.length; idxSize++) {
				double industrySizeEmployment = Double
						.valueOf(this.abs8155_0Table5.get(ABS8155_YEAR).get(ABS8155_TITLE_EMPLOYMENT)
								.get(sizes[idxSize]).get(industries8155[idxIndustry]).replace(",", ""));
				double industrySizeWages = Double
						.valueOf(this.abs8155_0Table5.get(ABS8155_YEAR).get(ABS8155_TITLE_WAGES).get(sizes[idxSize])
								.get(industries8155[idxIndustry]).replace(",", ""));
				double industrySizeSales = Double
						.valueOf(this.abs8155_0Table5.get(ABS8155_YEAR).get(ABS8155_TITLE_SALES).get(sizes[idxSize])
								.get(industries8155[idxIndustry]).replace(",", ""));
				double industrySizeTotalIncome = Double
						.valueOf(this.abs8155_0Table5.get(ABS8155_YEAR).get(ABS8155_TITLE_INCOME).get(sizes[idxSize])
								.get(industries8155[idxIndustry]).replace(",", ""));
				double industrySizeTotalExpenses = Double
						.valueOf(this.abs8155_0Table5.get(ABS8155_YEAR).get(ABS8155_TITLE_EXPENSES).get(sizes[idxSize])
								.get(industries8155[idxIndustry]).replace(",", ""));
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

		// FIXME: up to here on train

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
	 * @param data the calibration data to set
	 */
	@Autowired
	public void setData(CalibrationData data) {
		this.data = data;
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
