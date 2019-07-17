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

import gnu.trove.map.hash.TObjectFloatHashMap;
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
	private static final boolean DEBUG = false;

	private static final float MILLION = 1000000f;

	// beans
	private CalibrationData data;
	private Currencies currencies;
	private AustralianEconomy economy;

	// field variables
	private Map<String, Map<String, String>> allCountryData;
	private ArrayList<ForeignCountry> countryAgents;
	private Map<String, ForeignCountry> countryNameMap;

	// values based on ABS data, used to calibrate Agent links
	private TObjectFloatHashMap<String> initialExportsFromAustraliaByState; // 8 states
	private TObjectFloatHashMap<String> initialImportsToAustraliaByState;

	/**
	 * 
	 */
	public CalibrateCountries() {
		super();
		this.init();
	}

	public void createCountryAgents() {
		this.allCountryData = this.data.getCountryData(); // just those with FX data

		if (DEBUG) {
			System.out.println(new Date(System.currentTimeMillis()) + ": CREATING COUNTRY AGENTS");
		}

		Set<String> countryKeySet = new HashSet<String>(this.allCountryData.keySet());
		this.countryAgents = new ArrayList<ForeignCountry>(countryKeySet.size());
		this.countryNameMap = new HashMap<String, ForeignCountry>((int) Math.ceil(countryKeySet.size() / 0.75f));
		for (String key : countryKeySet) {
			String country = key;
			String ccyCode = this.allCountryData.get(key).get("Currency Code");
			Currency currency = this.currencies.getCurrency(ccyCode);
			ForeignCountry countryAgent = new ForeignCountry(country, currency);

			// CHECKME: add ABS initial import/export volumes to country agent (below loop)

			this.countryAgents.add(countryAgent);
			// N.B. Populate the actual import/export volumes later when the exporter agents
			// have been calibrated

			// add the countries to a map so we can look them up by name
			this.countryNameMap.put(countryAgent.getName(), countryAgent); // CHECKME: does the name exist here?
		}
		// populate the initial import/export estimates
		this.initialExportsFromAustraliaByState = this.calculateInitialExportsFromAustraliaByState();
		this.initialImportsToAustraliaByState = this.calculateInitialImportsToAustraliaByState();
		// add to economy
		this.countryAgents.trimToSize();
		this.addAgentsToEconomy();

		if (DEBUG) {
			System.out.println(new Date(System.currentTimeMillis()) + ": CREATED COUNTRY AGENTS");
		}

		// release memory
		// this.data.dropCountryData();
	}

	private void addAgentsToEconomy() {
		this.economy.setCountries(this.countryAgents);
	}

	/**
	 * CHECKME: Converts the CSV data into a summarised map. This will be used in
	 * calibrating the links between countries and businesses.
	 * 
	 * @return a map of total exports from Australia by state
	 */
	private TObjectFloatHashMap<String> calculateInitialExportsFromAustraliaByState() {
		// set the calibration date
		DateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);
		Date absDate = null;
		try {
			absDate = sdf.parse(CALIBRATION_DATE_ABS);
		} catch (ParseException e) {
			e.printStackTrace();
		}

		// load raw CSV country data for each state
		Map<String, Map<String, TObjectFloatHashMap<Date>>> dataStates = new HashMap<String, Map<String, TObjectFloatHashMap<Date>>>(
				(int) Math.ceil(8f / 0.75f));
		dataStates.put("NSW", this.data.getAbs5368_0Table36a());
		dataStates.put("VIC", this.data.getAbs5368_0Table36b());
		dataStates.put("QLD", this.data.getAbs5368_0Table36c());
		dataStates.put("SA", this.data.getAbs5368_0Table36d());
		dataStates.put("WA", this.data.getAbs5368_0Table36e());
		dataStates.put("TAS", this.data.getAbs5368_0Table36f());
		dataStates.put("NT", this.data.getAbs5368_0Table36g());
		dataStates.put("ACT", this.data.getAbs5368_0Table36h());

		if (DEBUG) {
			System.out.println(new Date(System.currentTimeMillis())
					+ ": Setting initial Exports from Australia using ABS 5368.0 Table 36");
		}

		// sum data for each state
		TObjectFloatHashMap<String> exports = new TObjectFloatHashMap<String>(
				(int) Math.ceil(ForeignCountry.STATES.length / 0.75f));
		Set<String> countryNames = this.allCountryData.keySet(); // just those with FX data
		for (int stateIdx = 0; stateIdx < ForeignCountry.STATES.length; stateIdx++) {
			String state = ForeignCountry.STATES[stateIdx];
			if (DEBUG) {
				System.out.println("--------------------------------------------------");
				System.out.println("stateIdx: " + stateIdx + ", state: " + state);
				System.out.println("dataStates.get(state).keySet(): " + dataStates.get(state).keySet());
				System.out.println("countryNames: " + countryNames);
			}
			float stateSum = 0f;
			for (String country : dataStates.get(state).keySet()) {
				if (DEBUG) {
					System.out.println("country: " + country + ", state: " + state);
				}
				if (countryNames.contains(country)) {
					// one of the countries for which we have FX data
					float exportAmount = dataStates.get(state).get(country).get(absDate) * MILLION;
					stateSum += exportAmount;

					// add these to the country itself (ABS exports)
					this.countryNameMap.get(country).putAbsExportsFromAustraliaByState(state, exportAmount);
					if (DEBUG) {
						System.out.println(country + ", " + state + ", " + exportAmount);
					}
				}
			}
			exports.put(state, stateSum);
		}
		if (DEBUG) {
			System.out.println("--------------------------------------------------");
		}

		return exports;
	}

	/**
	 * CHECKME: Converts the CSV data into a summarised map. This will be used in
	 * calibrating the links between countries and businesses.
	 * 
	 * @return a map of total imports to Australia by state
	 */
	private TObjectFloatHashMap<String> calculateInitialImportsToAustraliaByState() {
		// set the calibration date
		DateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);
		Date absDate = null;
		try {
			absDate = sdf.parse(CALIBRATION_DATE_ABS);
		} catch (ParseException e) {
			e.printStackTrace();
		}

		// load raw CSV country data for each state
		Map<String, Map<String, TObjectFloatHashMap<Date>>> dataStates = new HashMap<String, Map<String, TObjectFloatHashMap<Date>>>(
				(int) Math.ceil(8f / 0.75f));
		dataStates.put("NSW", this.data.getAbs5368_0Table37a());
		dataStates.put("VIC", this.data.getAbs5368_0Table37b());
		dataStates.put("QLD", this.data.getAbs5368_0Table37c());
		dataStates.put("SA", this.data.getAbs5368_0Table37d());
		dataStates.put("WA", this.data.getAbs5368_0Table37e());
		dataStates.put("TAS", this.data.getAbs5368_0Table37f());
		dataStates.put("NT", this.data.getAbs5368_0Table37g());
		dataStates.put("ACT", this.data.getAbs5368_0Table37h());

		if (DEBUG) {
			System.out.println(new Date(System.currentTimeMillis())
					+ ": Setting initial Imports to Australia using ABS 5368.0 Table 37");
		}

		// sum data for each state
		TObjectFloatHashMap<String> imports = new TObjectFloatHashMap<String>(
				(int) Math.ceil(ForeignCountry.STATES.length / 0.75f));
		Set<String> countryNames = this.allCountryData.keySet(); // just those with FX data
		for (int stateIdx = 0; stateIdx < ForeignCountry.STATES.length; stateIdx++) {
			String state = ForeignCountry.STATES[stateIdx];
			float stateSum = 0f;
			for (String country : dataStates.get(state).keySet()) {
				if (countryNames.contains(country)) {
					// one of the countries for which we have FX data
					float importAmount = dataStates.get(state).get(country).get(absDate) * MILLION;
					stateSum += importAmount;

					// add these to the country itself (ABS imports)
					this.countryNameMap.get(country).putAbsImportsToAustraliaByState(state, importAmount);
					if (DEBUG) {
						System.out.println(country + ", " + state + ", " + importAmount);
					}
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
	public TObjectFloatHashMap<String> getInitialExportsFromAustraliaByState() {
		return initialExportsFromAustraliaByState;
	}

	/**
	 * @param initialExportsFromAustraliaByState the
	 *                                           initialExportsFromAustraliaByState
	 *                                           to set
	 */
	public void setInitialExportsFromAustraliaByState(TObjectFloatHashMap<String> initialExportsFromAustraliaByState) {
		this.initialExportsFromAustraliaByState = initialExportsFromAustraliaByState;
	}

	/**
	 * @return the initialImportsToAustraliaByState
	 */
	public TObjectFloatHashMap<String> getInitialImportsToAustraliaByState() {
		return initialImportsToAustraliaByState;
	}

	/**
	 * @param initialImportsToAustraliaByState the initialImportsToAustraliaByState
	 *                                         to set
	 */
	public void setInitialImportsToAustraliaByState(TObjectFloatHashMap<String> initialImportsToAustraliaByState) {
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
