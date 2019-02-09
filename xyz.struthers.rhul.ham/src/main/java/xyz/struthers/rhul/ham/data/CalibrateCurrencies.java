/**
 * 
 */
package xyz.struthers.rhul.ham.data;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;

import xyz.struthers.rhul.ham.process.AustralianEconomy;

/**
 * @author Adam Struthers
 * @since 27-Jan-2019
 */
public class CalibrateCurrencies {

	private CalibrationData data;
	private Map<String, Map<String, String>> allCurrencyData;
	private Currencies currencies;
	private AustralianEconomy economy;

	/**
	 * 
	 */
	public CalibrateCurrencies() {
		super();
		this.init();
	}

	/**
	 * Creates the exchange rate objects for the model, based on the calibration
	 * data.
	 */
	public void createExchangeRates() {
		this.allCurrencyData = this.data.getCurrencyData();
		
		Set<String> ccyKeySet = new HashSet<String>(this.allCurrencyData.keySet());
		for (String key : ccyKeySet) {
			String isoCode = key;
			String currencyName = this.allCurrencyData.get(key).get("Currency Name");
			double fxRate = Double.valueOf(this.allCurrencyData.get(key).get("Jun-18"));
			double average1yr = Double.valueOf(this.allCurrencyData.get(key).get("1-Year Avg"));
			double standardDeviation1yr = Double.valueOf(this.allCurrencyData.get(key).get("1-Year Std Dev"));
			double standardDeviation5yr = Double.valueOf(this.allCurrencyData.get(key).get("5-Year Std Dev"));
			Currency ccy = new Currency(isoCode, currencyName, fxRate, average1yr, standardDeviation1yr,
					standardDeviation5yr);
			this.currencies.setCurrency(ccy);
		}
		
		this.addAgentsToEconomy();
	}

	private void addAgentsToEconomy() {
		this.economy.setCurrencies(this.currencies);
	}
	
	@PostConstruct
	private void init() {
		this.allCurrencyData = null;
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
	 * @return the exchangeRates
	 */
	@Autowired
	public Currencies getExchangeRates() {
		if (currencies == null) {
			this.createExchangeRates();
		}
		return currencies;
	}

	/**
	 * @param economy the economy to set
	 */
	@Autowired
	public void setEconomy(AustralianEconomy economy) {
		this.economy = economy;
	}
	
}
