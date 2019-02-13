/**
 * 
 */
package xyz.struthers.rhul.ham.data;

import java.util.ArrayList;
import java.util.Date;
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
	 * ----------------------------------------------------------------<br>
	 * PART A: INDUSTRY DETAIL WITH MODERATELY DETAILED P&L AND BAL SHT<br>
	 * ----------------------------------------------------------------<br>
	 * 1. ATO Company Table 4A: First calculate per-company figures, then calculate
	 * the ratios between the various P&L line items.<br>
	 * 2. ATO Company Table 4B: Calculate per-company figures, then multiply by the
	 * ratios from 4A to get a more detailed P&L. This gives per-company P&L for 574
	 * industries. Calculate the ratio of the number of companies in each of the
	 * relevant industry codes for each of the 18 industry divisions. This allows us
	 * to break the ABS data for 18 industries down into 574 industry codes.
	 * 
	 * ---------------------------------------------------<br>
	 * PART B: INDUSTRY FINANCIALS SPLIT BY SIZE AND STATE<br>
	 * ---------------------------------------------------<br>
	 * 3. ABS 8155.0 Table 6: Calculate ratios between states (for employment count,
	 * wages and sales) by industry.<br>
	 * 4. ABS 8155.0 Table 5: Use the ratios from Table 6 to split Size by State,
	 * assuming that all income is split in the same ratio as sales, and all
	 * expenses are split in the same ratio as wages. Calculate gross profit by
	 * subtracting total expenses from total income.
	 * 
	 * 574 industries x 8 states x 3 sizes = 13,776 distinct Business agents
	 * 
	 * -----------------------------------------------------<br>
	 * PART C: ASSIGN BUSINESSES TO LGA BY SIZE AND INDUSTRY<br>
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

		Set<String> fineIndustryKeySet = new HashSet<String>(
				this.atoCompanyTable4a.get("Other sales of goods and services\r\nno.").keySet());
		int numKeys = fineIndustryKeySet.size();
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
		double[] currentAssetsRatio = new double[numKeys];

		double[] totalLiabilitiesPerCompany = new double[numKeys];
		double[] tradeCreditorsRatio = new double[numKeys];
		double[] currentLiabilitiesRatio = new double[numKeys];
		double[] debtRatio = new double[numKeys];

		for (int i = 0; i < numKeys; i++) {
			int count = 0;
			double amount = 0d;
			double amountPerCompany = 0d;
			String key = industryClassCodes[i];

			// TOTAL INCOME
			count = Integer.valueOf(
					this.atoCompanyTable4a.get("Total Income3 no.").get(key).replace(",", ""));
			amount = Double.valueOf(
					this.atoCompanyTable4a.get("Total Income3 $").get(key).replace(",", ""));
			totalIncomePerCompany[i] = count == 0 ? 0d : amount / count;
			
			// Sales
			count = Integer.valueOf(
					this.atoCompanyTable4a.get("Other sales of goods and services no.").get(key).replace(",", ""));
			amount = Double.valueOf(
					this.atoCompanyTable4a.get("Other sales of goods and services $").get(key).replace(",", ""));
			amountPerCompany = count == 0 ? 0d : amount / count;
			salesRatio[i] = totalIncomePerCompany[i] == 0? 0d : amountPerCompany / totalIncomePerCompany[i];
			
			// Interest Income
			count = Integer.valueOf(
					this.atoCompanyTable4a.get("Gross Interest no.").get(key).replace(",", ""));
			amount = Double.valueOf(
					this.atoCompanyTable4a.get("Gross Interest $").get(key).replace(",", ""));
			amountPerCompany = count == 0 ? 0d : amount / count;
			interestIncomeRatio[i] = totalIncomePerCompany[i] == 0? 0d : amountPerCompany / totalIncomePerCompany[i];
			
			// Rent Income
			//FIXME: up to here
			
			// Government Income
			
			// Foreign Income
			
			// TOTAL EXPENSES
			
			// Cost of Sales
			
			// Rent and Lease Expense
			
			// Interest Expense
			
			// Foreign Interest Expense
			
			// Depreciation Expense
			
			// Salaries and Wages
			
			// TOTAL ASSETS
			
			// Current Assets
			
			// TOTAL LIABILITIES
			
			// Trade Creditors
			
			// Current Liabilities
			
			// Debt
			
		}
		/*
		 * Set<String> countryKeySet = new
		 * HashSet<String>(this.allCountryData.keySet()); for (String key :
		 * countryKeySet) { String country = key; String ccyCode =
		 * this.allCountryData.get(key).get("Currency Code"); Currency currency =
		 * this.currencies.getCurrency(ccyCode); ForeignCountry countryAgent = new
		 * ForeignCountry(country, currency); this.countryAgents.add(countryAgent); //
		 * Note: Populate the import/export volumes later when the exporter agents have
		 * been calibrated }
		 */

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
