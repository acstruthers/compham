/**
 * 
 */
package xyz.struthers.rhul.ham.data;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import xyz.struthers.rhul.ham.agent.ForeignCountry;
import xyz.struthers.rhul.ham.process.AustralianEconomy;

/**
 * Populate the import/export volumes later when the exporter agents have been
 * calibrated
 * 
 * @author Adam Struthers
 * @since 27-Jan-2019
 */
@Component
@Scope(value = "singleton")
public class CalibrateCountries {

	public static final String CALIBRATION_DATE_ABS = "01/06/2018";

	// beans
	private CalibrationData data;
	private Currencies currencies;
	private AustralianEconomy economy;

	// field variables
	private Map<String, Map<String, String>> allCountryData;
	private List<ForeignCountry> countryAgents;

	// values based on ABS data, used to calibrate Agent links
	private Map<String, Float> initialExportsFromAustraliaByState; // 8 states
	private Map<String, Float> initialImportsToAustraliaByState;

	/**
	 * 
	 */
	public CalibrateCountries() {
		super();
		this.init();
	}

	public void createCountryAgents() {
		this.allCountryData = this.data.getCountryData(); // just those with FX data
		this.countryAgents = new ArrayList<ForeignCountry>();

		Set<String> countryKeySet = new HashSet<String>(this.allCountryData.keySet());
		for (String key : countryKeySet) {
			String country = key;
			String ccyCode = this.allCountryData.get(key).get("Currency Code");
			Currency currency = this.currencies.getCurrency(ccyCode);
			ForeignCountry countryAgent = new ForeignCountry(country, currency);
			// populate the initial import/export estimates
			this.initialExportsFromAustraliaByState = this.calculateInitialExportsFromAustraliaByState();
			this.initialImportsToAustraliaByState = this.calculateInitialImportsToAustraliaByState();

			this.countryAgents.add(countryAgent);
			// N.B. Populate the actual import/export volumes later when the exporter agents
			// have been calibrated
		}

		this.addAgentsToEconomy();

		// release memory
		this.data.dropCountryData();
	}

	private void addAgentsToEconomy() {
		this.economy.setCountries(this.countryAgents);
	}

	/**
	 * FIXME: Converts the CSV data into a summarised map. This will be used in
	 * calibrating the links between countries and businesses.
	 * 
	 * @return a map of total exports from Australia by state
	 */
	private Map<String, Float> calculateInitialExportsFromAustraliaByState() {
		// set the calibration date
		DateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);
		Date absDate = null;
		try {
			absDate = sdf.parse(CALIBRATION_DATE_ABS);
		} catch (ParseException e) {
			e.printStackTrace();
		}

		// load raw CSV country data for each state
		Map<String, Map<String, Map<Date, Float>>> dataStates = new HashMap<String, Map<String, Map<Date, Float>>>(
				(int) Math.ceil(8f / 0.75f));
		dataStates.put("NSW", this.data.getAbs5368_0Table36a());
		dataStates.put("VIC", this.data.getAbs5368_0Table36b());
		dataStates.put("QLD", this.data.getAbs5368_0Table36c());
		dataStates.put("SA", this.data.getAbs5368_0Table36d());
		dataStates.put("WA", this.data.getAbs5368_0Table36e());
		dataStates.put("TAS", this.data.getAbs5368_0Table36f());
		dataStates.put("NT", this.data.getAbs5368_0Table36g());
		dataStates.put("ACT", this.data.getAbs5368_0Table36h());

		// sum data for each state
		Map<String, Float> exports = new HashMap<String, Float>((int) Math.ceil(ForeignCountry.STATES.length / 0.75f));
		Set<String> countryNames = this.allCountryData.keySet(); // just those with FX data
		for (int stateIdx = 0; stateIdx < ForeignCountry.STATES.length; stateIdx++) {
			String state = ForeignCountry.STATES[stateIdx];
			float stateSum = 0f;
			for (String country : dataStates.get(state).keySet()) {
				if (countryNames.contains(country)) {
					// one of the countries for which we have FX data
					stateSum += dataStates.get(state).get(country).get(absDate);
					
					//FIXME: add these to the country itself (ABS exports)
				}
			}
			exports.put(state, stateSum);
		}

		return exports;
	}

	/**
	 * CHECKME: Converts the CSV data into a summarised map. This will be used in
	 * calibrating the links between countries and businesses.
	 * 
	 * @return a map of total imports to Australia by state
	 */
	private Map<String, Float> calculateInitialImportsToAustraliaByState() {
		// set the calibration date
		DateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);
		Date absDate = null;
		try {
			absDate = sdf.parse(CALIBRATION_DATE_ABS);
		} catch (ParseException e) {
			e.printStackTrace();
		}

		// load raw CSV country data for each state
		Map<String, Map<String, Map<Date, Float>>> dataStates = new HashMap<String, Map<String, Map<Date, Float>>>(
				(int) Math.ceil(8f / 0.75f));
		dataStates.put("NSW", this.data.getAbs5368_0Table37a());
		dataStates.put("VIC", this.data.getAbs5368_0Table37b());
		dataStates.put("QLD", this.data.getAbs5368_0Table37c());
		dataStates.put("SA", this.data.getAbs5368_0Table37d());
		dataStates.put("WA", this.data.getAbs5368_0Table37e());
		dataStates.put("TAS", this.data.getAbs5368_0Table37f());
		dataStates.put("NT", this.data.getAbs5368_0Table37g());
		dataStates.put("ACT", this.data.getAbs5368_0Table37h());

		// sum data for each state
		Map<String, Float> imports = new HashMap<String, Float>((int) Math.ceil(ForeignCountry.STATES.length / 0.75f));
		Set<String> countryNames = this.allCountryData.keySet(); // just those with FX data
		for (int stateIdx = 0; stateIdx < ForeignCountry.STATES.length; stateIdx++) {
			String state = ForeignCountry.STATES[stateIdx];
			float stateSum = 0f;
			for (String country : dataStates.get(state).keySet()) {
				if (countryNames.contains(country)) {
					// one of the countries for which we have FX data
					stateSum += dataStates.get(state).get(country).get(absDate);
					
					//FIXME: add these to the country itself (ABS imports)
				}
			}
			imports.put(state, stateSum);
		}

		return imports;
	}

	@PostConstruct
	private void init() {
		this.allCountryData = null;
		this.countryAgents = null;
	}

	/**
	 * @return the allCountryData
	 */
	public Map<String, Map<String, String>> getAllCountryData() {
		return allCountryData;
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

	/**
	 * @return the initialExportsFromAustraliaByState
	 */
	public Map<String, Float> getInitialExportsFromAustraliaByState() {
		return initialExportsFromAustraliaByState;
	}

	/**
	 * @param initialExportsFromAustraliaByState the
	 *                                           initialExportsFromAustraliaByState
	 *                                           to set
	 */
	public void setInitialExportsFromAustraliaByState(Map<String, Float> initialExportsFromAustraliaByState) {
		this.initialExportsFromAustraliaByState = initialExportsFromAustraliaByState;
	}

	/**
	 * @return the initialImportsToAustraliaByState
	 */
	public Map<String, Float> getInitialImportsToAustraliaByState() {
		return initialImportsToAustraliaByState;
	}

	/**
	 * @param initialImportsToAustraliaByState the initialImportsToAustraliaByState
	 *                                         to set
	 */
	public void setInitialImportsToAustraliaByState(Map<String, Float> initialImportsToAustraliaByState) {
		this.initialImportsToAustraliaByState = initialImportsToAustraliaByState;
	}

	/**
	 * @param data the calibration data to set
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

}
