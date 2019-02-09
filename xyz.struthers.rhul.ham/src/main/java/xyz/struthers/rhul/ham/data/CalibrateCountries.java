/**
 * 
 */
package xyz.struthers.rhul.ham.data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;

import xyz.struthers.rhul.ham.agent.ForeignCountry;
import xyz.struthers.rhul.ham.process.AustralianEconomy;

/**
 * Populate the import/export volumes later when the exporter agents have been calibrated
 * 
 * @author Adam Struthers
 * @since 27-Jan-2019
 */
public class CalibrateCountries {

	private CalibrationData data;
	private Currencies currencies;
	private Map<String, Map<String, String>> allCountryData;
	private List<ForeignCountry> countryAgents;
	private AustralianEconomy economy;
	
	/**
	 * 
	 */
	public CalibrateCountries() {
		super();
		this.init();
	}

	public void createCountryAgents() {
		this.allCountryData = this.data.getCountryData();
		this.countryAgents = new ArrayList<ForeignCountry>();

		Set<String> countryKeySet = new HashSet<String>(this.allCountryData.keySet());
		for (String key : countryKeySet) {
			String country = key;
			String ccyCode = this.allCountryData.get(key).get("Currency Code");
			Currency currency = this.currencies.getCurrency(ccyCode);
			ForeignCountry countryAgent = new ForeignCountry(country, currency);
			this.countryAgents.add(countryAgent);
			// Note: Populate the import/export volumes later when the exporter agents have been calibrated
		}
		
		this.addAgentsToEconomy();
	}
	
	private void addAgentsToEconomy() {
		this.economy.setCountries(this.countryAgents);
	}

	@PostConstruct
	private void init() {
		this.allCountryData = null;
		this.countryAgents = null;
	}

	/**
	 * @param data
	 *            the calibration data to set
	 */
	@Autowired
	public void setData(CalibrationData data) {
		this.data = data;
	}

	/**
	 * @return the currencies
	 */
	public Currencies getCurrencies() {
		return currencies;
	}

	/**
	 * @param currencies the currencies to set
	 */
	@Autowired
	public void setCurrencies(Currencies currencies) {
		this.currencies = currencies;
	}

	/**
	 * @param economy the economy to set
	 */
	@Autowired
	public void setEconomy(AustralianEconomy economy) {
		this.economy = economy;
	}

	/**
	 * @return the countryAgents
	 */
	public List<ForeignCountry> getCountryAgents() {
		if (countryAgents == null) {
			this.createCountryAgents();
		}
		return countryAgents;
	}
}
