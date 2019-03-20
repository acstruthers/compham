/**
 * 
 */
package xyz.struthers.rhul.ham.data;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import xyz.struthers.rhul.ham.process.AustralianEconomy;

/**
 * @author Adam Struthers
 * @since 27-Jan-2019
 */
@Component
@Scope(value = "singleton")
public class CalibrateCurrencies {

	// beans
	private CalibrationData data;
	private AustralianEconomy economy;
	private Currencies currencies;

	private Map<String, Map<String, String>> allCurrencyData;

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
			float fxRate = Float.valueOf(this.allCurrencyData.get(key).get("Jun-18"));
			float average1yr = Float.valueOf(this.allCurrencyData.get(key).get("1-Year Avg"));
			float standardDeviation1yr = Float.valueOf(this.allCurrencyData.get(key).get("1-Year Std Dev"));
			float standardDeviation5yr = Float.valueOf(this.allCurrencyData.get(key).get("5-Year Std Dev"));
			Currency ccy = new Currency(isoCode, currencyName, fxRate, average1yr, standardDeviation1yr,
					standardDeviation5yr);
			this.currencies.setCurrency(ccy);
		}

		this.addAgentsToEconomy();

		// release memory
		this.data.dropCurrencyData();
	}

	private void addAgentsToEconomy() {
		this.economy.setCurrencies(this.currencies);
	}

	@PostConstruct
	private void init() {
		this.allCurrencyData = null;
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
	 * @param currencies the currencies to set
	 */
	@Autowired
	public void setCurrencies(Currencies currencies) {
		this.currencies = currencies;
	}

	/**
	 * @return the currencies
	 */
	public Currencies getCurrencies() {
		if (currencies == null) {
			this.createExchangeRates();
		}
		return currencies;
	}

}
