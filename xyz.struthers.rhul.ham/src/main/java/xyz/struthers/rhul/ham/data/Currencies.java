/**
 * 
 */
package xyz.struthers.rhul.ham.data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import gnu.trove.list.array.TFloatArrayList;

/**
 * @author Adam Struthers
 * @since 27-Jan-2019
 */
@Component
@Scope(value = "singleton")
public class Currencies implements Serializable {

	private static final long serialVersionUID = 1L;

	// FX rate change strategies
	public static final int SAME = 0;
	public static final int RANDOM_1YR = 1;
	public static final int RANDOM_1YR_UP = 2;
	public static final int RANDOM_1YR_DOWN = 3;
	public static final int RANDOM_5YR = 4;
	public static final int RANDOM_5YR_UP = 5;
	public static final int RANDOM_5YR_DOWN = 6;
	public static final int CUSTOM_PATH = 7;

	Map<String, Currency> currencies; // key is ISO-4217 Code (e.g. USD)

	/**
	 * 
	 */
	public Currencies() {
		super();
	}

	public void prepareFxRatesSame(int iteration) {
		for (String ccyCode : this.currencies.keySet()) {
			this.currencies.get(ccyCode).setExchangeRateSame(iteration);
		}
	}

	public void prepareFxRatesRandom1yr(int iteration, Random random) {
		for (String ccyCode : this.currencies.keySet()) {
			this.currencies.get(ccyCode).setExchangeRateRandom1yr(iteration, random);
		}
	}

	public void prepareFxRatesRandom1yrUp(int iteration, Random random) {
		for (String ccyCode : this.currencies.keySet()) {
			this.currencies.get(ccyCode).setExchangeRateRandom1yrUp(iteration, random);
		}
	}

	public void prepareFxRatesRandom1yrDown(int iteration, Random random) {
		for (String ccyCode : this.currencies.keySet()) {
			this.currencies.get(ccyCode).setExchangeRateRandom1yrDown(iteration, random);
		}
	}

	public void prepareFxRatesRandom5yr(int iteration, Random random) {
		for (String ccyCode : this.currencies.keySet()) {
			this.currencies.get(ccyCode).setExchangeRateRandom5yr(iteration, random);
		}
	}

	public void prepareFxRatesRandom5yrUp(int iteration, Random random) {
		for (String ccyCode : this.currencies.keySet()) {
			this.currencies.get(ccyCode).setExchangeRateRandom5yrUp(iteration, random);
		}
	}

	public void prepareFxRatesRandom5yrDown(int iteration, Random random) {
		for (String ccyCode : this.currencies.keySet()) {
			this.currencies.get(ccyCode).setExchangeRateRandom5yrDown(iteration, random);
		}
	}

	public void prepareFxRatesCustomPath(int iteration, Map<String, TFloatArrayList> rates) {
		for (String ccyCode : this.currencies.keySet()) {
			if (rates.containsKey(ccyCode) && rates.get(ccyCode).size() > iteration) {
				// set the custom rate
				this.currencies.get(ccyCode).setExchangeRate(iteration, rates.get(ccyCode).get(iteration));
			} else {
				// no custom rate data, so just use the previous rate
				this.currencies.get(ccyCode).setExchangeRateSame(iteration);
			}
		}
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

	public void setCurrency(String isoCode, String currencyName, float fxRate, float average1yr,
			float standardDeviation1yr, float standardDeviation5yr) {
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

	public float getExchangeRate(String isoCode, int iteration) {
		float result = 0f;
		if (this.currencies != null) {
			result = this.currencies.get(isoCode).getExchangeRate(iteration);
		}
		return result;
	}

	public float get1yrAvgExchRate(String isoCode) {
		float result = 0f;
		if (this.currencies != null) {
			result = this.currencies.get(isoCode).getAvg1yr();
		}
		return result;
	}

	public float get1yrStdDevExchRate(String isoCode) {
		float result = 0f;
		if (this.currencies != null) {
			result = this.currencies.get(isoCode).getStdev1yr();
		}
		return result;
	}

	public float get5yrStdDevExchRate(String isoCode) {
		float result = 0f;
		if (this.currencies != null) {
			result = this.currencies.get(isoCode).getStdev5yr();
		}
		return result;
	}

}
