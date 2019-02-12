/**
 * 
 */
package xyz.struthers.rhul.ham.data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

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
	private Map<String, Map<Date, String>> rbaE1; // AU Bal Sht totals
	private Map<String, Map<Date, String>> rbaE2; // AU Bal Sht ratios
	private Map<String, Map<String, String>> abs1292_0_55_002ANZSIC; // ANZSIC industry code mapping
	private Map<String, Map<String, Map<String, String>>> abs1410_0Economy; // Data by LGA: Economy (keys: year, LGA,
																			// series)
	private Map<String, Map<String, Map<String, Map<String, String>>>> abs5368_0Exporters; // formatted export data
	// (keys: industry, state,
	// country, value range)
	private Map<String, Map<Date, String>> abs5676_0Table7; // Business Indicators: Sales by State
	private Map<String, Map<Date, String>> abs5676_0Table19; // Business Indicators: Wages by State
	private Map<String, Map<Date, String>> abs5676_0Table21; // Business Indicators: Sales vs Wages Ratio
	private Map<String, Map<Date, String>> abs5676_0Table22; // Business Indicators: Profits vs Sales Ratio
	private Map<String, Map<String, Map<String, String>>> abs8155_0Table2; // labour costs by division (keys: year,
	// column title, industry)
	private Map<String, Map<String, Map<String, String>>> abs8155_0Table4; // industry performance by division (keys:
	// year, column title, industry)
	private Map<String, Map<String, Map<String, Map<String, String>>>> abs8155_0Table5; // business size by division
	// (keys: year, column title,
	// size, industry)
	private Map<String, Map<String, Map<String, Map<String, String>>>> abs8155_0Table6; // states by division (keys:
	// year, column title, state,
	// industry)
	/*
	 * Count by state, industry & employment range. Keys are: employment range,
	 * state, industry class code.
	 */
	private Map<String, Map<String, Map<String, String>>> abs8165_0StateEmployment;
	/*
	 * Count by state, industry & turnover range. Keys are: turnover range, state,
	 * industry class code.
	 */
	private Map<String, Map<String, Map<String, String>>> abs8165_0StateTurnover;
	/*
	 * Count by LGA, industry & employment range. Keys are: employment range, state,
	 * LGA code, industry division code.
	 */
	private Map<String, Map<String, Map<String, Map<String, String>>>> abs8165_0LgaEmployment;
	/*
	 * Count by LGA, industry & turnover range. Keys are: turnover range, state, LGA
	 * code, industry division code.
	 */
	private Map<String, Map<String, Map<String, Map<String, String>>>> abs8165_0LgaTurnover;
	private Map<String, Map<String, String>> abs8165_0Table4; // businesses by main state
	private Map<String, Map<String, String>> abs8165_0Table13; // businesses by employment size range
	private Map<String, Map<String, String>> abs8165_0Table17; // businesses by annual turnover range
	private Map<String, Map<String, String>> abs8167_0Table3; // main source of income
	private Map<String, Map<String, String>> abs8167_0Table6; // main supplier

	/**
	 * 
	 */
	public CalibrateBusinesses() {
		super();
		this.init();
	}

	/**
	 * FIXME: implement me (to create alll the Business agents)
	 * 
	 * Creates all the Business agents in teh model, and stores them in the
	 * AustralianEconomy. Does not link businesses with employees, banks, suppliers,
	 * etc.
	 */
	public void createBusinessAgents() {
		// get just the relevant business data from the CalibrationData
		this.rbaE1 = this.data.getRbaE1();
		this.rbaE2 = this.data.getRbaE2();
		this.abs1292_0_55_002ANZSIC = this.data.getAbs1292_0_55_002ANZSIC();
		this.abs1410_0Economy = this.data.getAbs1410_0Economy();
		this.abs5368_0Exporters = this.data.getAbs5368_0Exporters();
		this.abs5676_0Table7 = this.data.getAbs5676_0Table7();
		this.abs5676_0Table19 = this.data.getAbs5676_0Table19();
		this.abs5676_0Table21 = this.data.getAbs5676_0Table21();
		this.abs5676_0Table22 = this.data.getAbs5676_0Table22();
		this.abs8155_0Table2 = this.data.getAbs8155_0Table2();
		this.abs8155_0Table4 = this.data.getAbs8155_0Table4();
		this.abs8155_0Table5 = this.data.getAbs8155_0Table5();
		this.abs8155_0Table6 = this.data.getAbs8155_0Table6();
		this.abs8165_0StateEmployment = this.data.getAbs8165_0StateEmployment();
		this.abs8165_0StateTurnover = this.data.getAbs8165_0StateTurnover();
		this.abs8165_0LgaEmployment = this.data.getAbs8165_0LgaEmployment();
		this.abs8165_0LgaTurnover = this.data.getAbs8165_0LgaTurnover();
		this.abs8165_0Table4 = this.data.getAbs8165_0Table4();
		this.abs8165_0Table13 = this.data.getAbs8165_0Table13();
		this.abs8165_0Table17 = this.data.getAbs8165_0Table17();
		this.abs8167_0Table3 = this.data.getAbs8167_0Table3();
		this.abs8167_0Table6 = this.data.getAbs8167_0Table6();

		// create businesses
		this.businessAgents = new ArrayList<Business>();

		/* =============
		 * = ALGORITHM =
		 * =============
		 * 
		 * 
		 */
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
