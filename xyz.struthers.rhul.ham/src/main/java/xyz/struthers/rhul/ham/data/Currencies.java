/**
 * 
 */
package xyz.struthers.rhul.ham.data;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author Adam Struthers
 * @since 27-Jan-2019
 */
@Component
@Scope(value = "singleton")
public class Currencies {

	Map<String, Currency> currencies; // key is ISO-4217 Code (e.g. USD)

	/**
	 * 
	 */
	public Currencies() {
		super();
	}

	public void setCurrency(Currency currency) {
		// ensure valid state
		if (this.currencies == null) {
			this.currencies = new HashMap<String, Currency>();
		} else if (this.currencies.containsKey(currency.getIso4217code())) {
			this.currencies.remove(currency.getIso4217code());
		}

		// store data
		this.currencies.put(currency.getIso4217code(), currency);
	}
	
	public void setCurrency(String isoCode, String currencyName, double fxRate, double average1yr,
			double standardDeviation1yr, double standardDeviation5yr) {
		// ensure valid state
		if (this.currencies == null) {
			this.currencies = new HashMap<String, Currency>();
		} else if (this.currencies.containsKey(isoCode)) {
			this.currencies.remove(isoCode);
		}

		// store data
		Currency ccy = new Currency(isoCode, currencyName, fxRate, average1yr, standardDeviation1yr,
				standardDeviation5yr);
		this.currencies.put(isoCode, ccy);
	}

	public Map<String, Currency> getAllCurrencies() {
		return this.currencies;
	}
	
	public Currency getCurrency(String isoCode) {
		return this.currencies.get(isoCode);
	}
	
	public String getName(String isoCode) {
		String result = null;
		if (this.currencies != null) {
			result = this.currencies.get(isoCode).getName();
		}
		return result;
	}

	public double getExchangeRate(String isoCode) {
		double result = 0d;
		if (this.currencies != null) {
			result = this.currencies.get(isoCode).getExchangeRate();
		}
		return result;
	}

	public double get1yrAvgExchRate(String isoCode) {
		double result = 0d;
		if (this.currencies != null) {
			result = this.currencies.get(isoCode).getAvg1yr();
		}
		return result;
	}

	public double get1yrStdDevExchRate(String isoCode) {
		double result = 0d;
		if (this.currencies != null) {
			result = this.currencies.get(isoCode).getStdev1yr();
		}
		return result;
	}

	public double get5yrStdDevExchRate(String isoCode) {
		double result = 0d;
		if (this.currencies != null) {
			result = this.currencies.get(isoCode).getStdev5yr();
		}
		return result;
	}

}
