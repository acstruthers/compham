/**
 * 
 */
package xyz.struthers.rhul.ham.agent;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import xyz.struthers.rhul.ham.data.Currency;
import xyz.struthers.rhul.ham.process.Clearable;
import xyz.struthers.rhul.ham.process.NodePayment;

/**
 * The model does not care about the financial impact on foreign countries -
 * only their impact on the Australian economy - so very little detail is
 * recorded against the countries themselves. Most of the details are recorded
 * against the individual businesses that are engaged in international trade
 * with these countries. Importantly, foreign ountries are given a receive a
 * large enough exogeneous income so that they never default in the Payment
 * Clearing Vector calculations.
 * 
 * Each instance of this class stores 1 pointer, 18 floats and 17 strings, so
 * will consume approximately 260 bytes of RAM. There are 114 instances of this
 * class in the model, so they will consume approximately 28.95 kB of RAM.
 * 
 * @author Adam Struthers
 * @since 27-Jan-2019
 */
public final class ForeignCountry extends Agent {

	private static final long serialVersionUID = 1L;

	private final int EXPORTERS_INIT_CAPACITY = 50000;
	private final int HOUSEHOLDS_INIT_CAPACITY = 50000;
	public static final String[] STATES = { "NSW", "VIC", "QLD", "SA", "WA", "TAS", "NT", "ACT" };

	// agent relationships
	private int paymentClearingIndex;
	private ArrayList<Business> exporters;
	private ArrayList<Household> households;
	private int defaultIteration;
	private int defaultOrder;

	// field variables
	private Currency currency;
	/**
	 * Lists the exchange rates at each iteration. Index 0 is the initially
	 * calibrated rate, index 1 is at month 1, etc.
	 * 
	 * Exchange rates are expressed as 1 AUD = xxx foreign currency<br>
	 * e.g. 1 AUD = 0.7391 USD
	 */
	private ArrayList<Float> exchangeRates;
	private float totalExportsFromAustralia;
	private float totalImportsToAustralia;
	// values based on ABS data, used to calibrate Agent links
	private Map<String, Float> absExportsFromAustraliaByState; // 8 states
	private Map<String, Float> absImportsToAustraliaByState;
	// values based on actual linked Business Agents
	private Map<String, Float> actualExportsFromAustraliaByState;
	private Map<String, Float> actualImportsToAustraliaByState;

	/**
	 * 
	 */
	public ForeignCountry() {
		super();
		this.init();
	}

	/**
	 * Initialisation constructor
	 * 
	 * @param countryName
	 * @param countryCurrency
	 */
	public ForeignCountry(String countryName, Currency countryCurrency) {
		super();
		this.init();

		super.name = countryName;
		this.currency = countryCurrency;
	}

	public ForeignCountry(String countryName, Currency countryCurrency, float initialExchangeRate,
			float exportsFromAustralia, float importsToAustralia, Map<String, Float> exportsFromAusByState,
			Map<String, Float> importsToAusByState) {
		super();
		this.init();

		super.name = countryName;
		this.currency = countryCurrency;
		this.exchangeRates = new ArrayList<Float>();
		this.exchangeRates.add(initialExchangeRate);
		this.totalExportsFromAustralia = exportsFromAustralia;
		this.totalImportsToAustralia = importsToAustralia;
		this.absExportsFromAustraliaByState = exportsFromAusByState;
		this.absImportsToAustraliaByState = importsToAusByState;
	}

	/**
	 * @return the exporters
	 */
	public ArrayList<Business> getExporters() {
		return exporters;
	}

	public void addExporter(Business exporter) {
		if (this.exporters == null) {
			this.exporters = new ArrayList<Business>(EXPORTERS_INIT_CAPACITY);
		}
		this.exporters.add(exporter);
	}

	public void addAllExporters(List<Business> exporters) {
		if (this.exporters == null) {
			this.exporters = new ArrayList<Business>(exporters.size());
		}
		this.exporters.addAll(exporters);
	}

	public void trimExportersListToSize() {
		if (this.exporters != null) {
			this.exporters.trimToSize();
		}
	}

	/**
	 * @return the households
	 */
	public ArrayList<Household> getHouseholds() {
		return households;
	}

	public void addHousehold(Household household) {
		if (this.households == null) {
			this.households = new ArrayList<Household>(HOUSEHOLDS_INIT_CAPACITY);
		}
		this.households.add(household);
	}

	public void addAllHouseholds(List<Household> households) {
		if (this.households == null) {
			this.households = new ArrayList<Household>(households.size());
		}
		this.households.addAll(households);
	}

	public void trimHouseholdsListToSize() {
		if (this.households != null) {
			this.households.trimToSize();
		}
	}

	/**
	 * Updates the state totals based on the actual Business agents that are
	 * exporters.
	 */
	public void updateActualExporters() {
		if (this.exporters != null) {
			if (this.actualExportsFromAustraliaByState != null) {
				// clear out any existing data
				for (String state : this.actualExportsFromAustraliaByState.keySet()) {
					this.actualExportsFromAustraliaByState.put(state, 0f);
				}
				this.actualExportsFromAustraliaByState.clear();
				this.actualExportsFromAustraliaByState = null;
			}
		}
		this.actualExportsFromAustraliaByState = new HashMap<String, Float>((int) Math.ceil(STATES.length / 0.75f));
		for (String state : STATES) {
			float stateTotal = (float) this.exporters.stream().filter(o -> o.getState().equals(state))
					.mapToDouble(o -> o.getSalesForeign()).sum();
			this.actualExportsFromAustraliaByState.put(state, stateTotal);
		}
	}

	/**
	 * Updates the list of exchange rates from the currency's current data.
	 */
	public void updateExchangeRates() {
		this.exchangeRates = this.currency.getExchangeRates();
	}

	/**
	 * Gets the column headings, to write to CSV file.
	 * 
	 * @param separator
	 * @return a CSV list of the column headings
	 */
	public String toCsvStringHeaders(String separator) {
		StringBuilder sb = new StringBuilder();

		sb.append("Name" + separator);
		sb.append("PaymentClearingIndex" + separator);
		sb.append("ExporterCount" + separator);
		sb.append("Currency" + separator);
		sb.append("ExchangeRate" + separator);
		sb.append("TotalExportsFromAustralia" + separator);
		sb.append("TotalImportsToAustralia" + separator);
		sb.append("ExportsFromAustraliaByStateNSW" + separator);
		sb.append("ExportsFromAustraliaByStateVIC" + separator);
		sb.append("ExportsFromAustraliaByStateQLD" + separator);
		sb.append("ExportsFromAustraliaByStateSA" + separator);
		sb.append("ExportsFromAustraliaByStateWA" + separator);
		sb.append("ExportsFromAustraliaByStateTAS" + separator);
		sb.append("ExportsFromAustraliaByStateNT" + separator);
		sb.append("ExportsFromAustraliaByStateACT" + separator);
		sb.append("ImportsToAustraliaByStateNSW" + separator);
		sb.append("ImportsToAustraliaByStateVIC" + separator);
		sb.append("ImportsToAustraliaByStateQLD" + separator);
		sb.append("ImportsToAustraliaByStateSA" + separator);
		sb.append("ImportsToAustraliaByStateWA" + separator);
		sb.append("ImportsToAustraliaByStateTAS" + separator);
		sb.append("ImportsToAustraliaByStateNT" + separator);
		sb.append("ImportsToAustraliaByStateACT");

		return sb.toString();
	}

	/**
	 * Gets the data, to write to CSV file.
	 * 
	 * @param separator
	 * @return a CSV list of the data
	 */
	public String toCsvString(String separator, int iteration) {
		StringBuilder sb = new StringBuilder();

		DecimalFormat decimal = new DecimalFormat("###0.00");
		DecimalFormat rate = new DecimalFormat("###0.000000");
		DecimalFormat wholeNumber = new DecimalFormat("###0");
		// DecimalFormat percent = new DecimalFormat("###0.0000");

		sb.append(this.name.replace(",", " ") + separator);
		sb.append(wholeNumber.format(this.paymentClearingIndex) + separator);
		sb.append(wholeNumber.format(this.exporters != null ? this.exporters.size() : 0) + separator);
		sb.append((this.currency != null ? this.currency.getIso4217code() : "NA") + separator);
		sb.append(rate.format(this.exchangeRates != null ? this.exchangeRates.get(iteration) : 0) + separator);
		sb.append(decimal.format(this.totalExportsFromAustralia) + separator);
		sb.append(decimal.format(this.totalImportsToAustralia) + separator);
		sb.append(decimal.format(
				this.absExportsFromAustraliaByState != null && this.absExportsFromAustraliaByState.containsKey("NSW")
						? this.absExportsFromAustraliaByState.get("NSW")
						: 0)
				+ separator);
		sb.append(decimal.format(
				this.absExportsFromAustraliaByState != null && this.absExportsFromAustraliaByState.containsKey("VIC")
						? this.absExportsFromAustraliaByState.get("VIC")
						: 0)
				+ separator);
		sb.append(decimal.format(
				this.absExportsFromAustraliaByState != null && this.absExportsFromAustraliaByState.containsKey("QLD")
						? this.absExportsFromAustraliaByState.get("QLD")
						: 0)
				+ separator);
		sb.append(decimal.format(
				this.absExportsFromAustraliaByState != null && this.absExportsFromAustraliaByState.containsKey("SA")
						? this.absExportsFromAustraliaByState.get("SA")
						: 0)
				+ separator);
		sb.append(decimal.format(
				this.absExportsFromAustraliaByState != null && this.absExportsFromAustraliaByState.containsKey("WA")
						? this.absExportsFromAustraliaByState.get("WA")
						: 0)
				+ separator);
		sb.append(decimal.format(
				this.absExportsFromAustraliaByState != null && this.absExportsFromAustraliaByState.containsKey("TAS")
						? this.absExportsFromAustraliaByState.get("TAS")
						: 0)
				+ separator);
		sb.append(decimal.format(
				this.absExportsFromAustraliaByState != null && this.absExportsFromAustraliaByState.containsKey("NT")
						? this.absExportsFromAustraliaByState.get("NT")
						: 0)
				+ separator);
		sb.append(decimal.format(
				this.absExportsFromAustraliaByState != null && this.absExportsFromAustraliaByState.containsKey("ACT")
						? this.absExportsFromAustraliaByState.get("ACT")
						: 0)
				+ separator);
		sb.append(decimal.format(
				this.absImportsToAustraliaByState != null && this.absImportsToAustraliaByState.containsKey("NSW")
						? this.absImportsToAustraliaByState.get("NSW")
						: 0)
				+ separator);
		sb.append(decimal.format(
				this.absImportsToAustraliaByState != null && this.absImportsToAustraliaByState.containsKey("VIC")
						? this.absImportsToAustraliaByState.get("VIC")
						: 0)
				+ separator);
		sb.append(decimal.format(
				this.absImportsToAustraliaByState != null && this.absImportsToAustraliaByState.containsKey("QLD")
						? this.absImportsToAustraliaByState.get("QLD")
						: 0)
				+ separator);
		sb.append(decimal
				.format(this.absImportsToAustraliaByState != null && this.absImportsToAustraliaByState.containsKey("SA")
						? this.absImportsToAustraliaByState.get("SA")
						: 0)
				+ separator);
		sb.append(decimal
				.format(this.absImportsToAustraliaByState != null && this.absImportsToAustraliaByState.containsKey("WA")
						? this.absImportsToAustraliaByState.get("WA")
						: 0)
				+ separator);
		sb.append(decimal.format(
				this.absImportsToAustraliaByState != null && this.absImportsToAustraliaByState.containsKey("TAS")
						? this.absImportsToAustraliaByState.get("TAS")
						: 0)
				+ separator);
		sb.append(decimal
				.format(this.absImportsToAustraliaByState != null && this.absImportsToAustraliaByState.containsKey("NT")
						? this.absImportsToAustraliaByState.get("NT")
						: 0)
				+ separator);
		sb.append(decimal.format(
				this.absImportsToAustraliaByState != null && this.absImportsToAustraliaByState.containsKey("ACT")
						? this.absImportsToAustraliaByState.get("ACT")
						: 0));

		return sb.toString();
	}

	/**
	 * Gets the column summary headings, to write to CSV file.
	 * 
	 * @param separator
	 * @return a CSV list of the column headings
	 */
	public String toCsvSummaryStringHeaders(String separator) {
		return this.toCsvStringHeaders(separator);
	}

	/**
	 * Gets the summary data, to write to CSV file.
	 * 
	 * @param separator
	 * @return a CSV list of the data
	 */
	public String toCsvSummaryString(String separator, int iteration) {
		return this.toCsvString(separator, iteration);
	}

	@Override
	public int getPaymentClearingIndex() {
		return this.paymentClearingIndex;
	}

	@Override
	public void setPaymentClearingIndex(int index) {
		this.paymentClearingIndex = index;
	}

	@Override
	public List<NodePayment> getAmountsPayable(int iteration) {
		int numberOfCreditors = 1;
		if (this.exporters != null) {
			numberOfCreditors = this.exporters.size();
		}
		if (this.households != null) {
			numberOfCreditors += this.households.size();
		}

		ArrayList<NodePayment> liabilities = new ArrayList<NodePayment>(numberOfCreditors);

		/*
		 * Assume that Australian businesses export in AUD, but import in the foreign
		 * country's currency. However, if the value of the AUD increases this will
		 * decrease foreign demand and result in a drop in sales.
		 */
		float currentExchangeRate = this.exchangeRates.get(0);
		if (this.exchangeRates.size() >= iteration && this.exchangeRates.get(iteration) != null) {
			currentExchangeRate = this.exchangeRates.get(iteration);
		}
		float exchRateAdjustment = this.exchangeRates.get(0) / currentExchangeRate;
		if (this.exporters != null) {
			for (int exporterIdx = 0; exporterIdx < this.exporters.size(); exporterIdx++) {
				int index = this.exporters.get(exporterIdx).getPaymentClearingIndex();
				float audAmount = this.exporters.get(exporterIdx).getSalesForeign() * exchRateAdjustment;
				liabilities.add(new NodePayment(index, audAmount));
			}
		}

		// some households have foreign income
		if (this.households != null) {
			for (int householdIdx = 0; householdIdx < this.households.size(); householdIdx++) {
				int index = this.households.get(householdIdx).getPaymentClearingIndex();
				float audAmount = this.households.get(householdIdx).getPnlForeignIncome() * exchRateAdjustment;
				liabilities.add(new NodePayment(index, audAmount));
			}
		}

		liabilities.trimToSize();
		return liabilities;
	}

	@Override
	public void setDefaultedIteration(int iteration, int order) {
		this.defaultIteration = iteration;
		this.defaultOrder = order;
	}

	@Override
	public int getDefaultIteration() {
		return this.defaultIteration;
	}

	@Override
	public int getDefaultOrder() {
		return this.defaultOrder;
	}

	@Override
	public int processClearingPaymentVectorOutput(float nodeEquity, int iteration, int defaultOrder) {
		// update default details
		if (defaultOrder > 0) {
			// update default details unless it defaulted in a previous iteration
			if (this.defaultIteration == 0) {
				// hasn't defaulted in a previous iteration
				this.defaultIteration = iteration;
				this.defaultOrder = defaultOrder;
			}
		}

		// foreign countries are exogeneous to the model and so don't have any
		// financials to update at the end of each round of the CPV algorithm

		return Clearable.OK; // assume foreign countries never default
	}

	protected void init() {
		super.name = null;

		this.paymentClearingIndex = 0;
		this.exporters = null;
		this.households = null;
		this.defaultIteration = 0;
		this.defaultOrder = 0;

		this.currency = null;
		this.exchangeRates = null;

		this.totalExportsFromAustralia = 0f;
		this.totalImportsToAustralia = 0f;
		this.absExportsFromAustraliaByState = null;
		this.absImportsToAustraliaByState = null;
		this.actualExportsFromAustraliaByState = null;
		this.actualImportsToAustraliaByState = null;
	}

	/**
	 * @return the currency
	 */
	public Currency getCurrency() {
		return currency;
	}

	/**
	 * @param currency the currency to set
	 */
	public void setCurrency(Currency currency) {
		this.currency = currency;
	}

	/**
	 * @return the exchangeRates
	 */
	public ArrayList<Float> getExchangeRates() {
		return exchangeRates;
	}

	/**
	 * @param exchangeRates the exchangeRates to set
	 */
	public void setExchangeRates(ArrayList<Float> exchangeRates) {
		this.exchangeRates = exchangeRates;
	}

	/**
	 * @return the totalExportsFromAustralia
	 */
	public float getTotalExportsFromAustralia() {
		return totalExportsFromAustralia;
	}

	/**
	 * @param totalExportsFromAustralia the totalExportsFromAustralia to set
	 */
	public void setTotalExportsFromAustralia(float totalExportsFromAustralia) {
		this.totalExportsFromAustralia = totalExportsFromAustralia;
	}

	/**
	 * @return the totalImportsToAustralia
	 */
	public float getTotalImportsToAustralia() {
		return totalImportsToAustralia;
	}

	/**
	 * @param totalImportsToAustralia the totalImportsToAustralia to set
	 */
	public void setTotalImportsToAustralia(float totalImportsToAustralia) {
		this.totalImportsToAustralia = totalImportsToAustralia;
	}

	/**
	 * @return the exportsFromAustraliaByState
	 */
	public Map<String, Float> getAbsExportsFromAustraliaByState() {
		return absExportsFromAustraliaByState;
	}

	/**
	 * @return the exportsFromAustraliaByState for the specified state
	 */
	public float getAbsExportsFromAustraliaForState(String state) {
		float exports = 0f;
		if (absExportsFromAustraliaByState.containsKey(state)) {
			exports = absExportsFromAustraliaByState.get(state);
		}
		return exports;
	}

	/**
	 * @param exportsFromAustraliaByState the exportsFromAustraliaByState to set
	 */
	public void setAbsExportsFromAustraliaByState(Map<String, Float> exportsFromAustraliaByState) {
		this.absExportsFromAustraliaByState = exportsFromAustraliaByState;
	}

	/**
	 * Adds a single state's exports to the country
	 * 
	 * @param state
	 * @param exportsFromAustralia
	 */
	public void putAbsExportsFromAustraliaByState(String state, Float exportsFromAustralia) {
		if (this.absExportsFromAustraliaByState == null) {
			this.absExportsFromAustraliaByState = new HashMap<String, Float>((int) Math.ceil(STATES.length / 0.75f));
		}
		this.absExportsFromAustraliaByState.put(state, exportsFromAustralia);
	}

	/**
	 * @return the importsToAustraliaByState
	 */
	public Map<String, Float> getAbsImportsToAustraliaByState() {
		return absImportsToAustraliaByState;
	}

	/**
	 * @return the importsToAustraliaByState for the specified state
	 */
	public float getAbsImportsToAustraliaForState(String state) {
		float imports = 0f;
		if (absImportsToAustraliaByState.containsKey(state)) {
			imports = absImportsToAustraliaByState.get(state);
		}
		return imports;
	}

	/**
	 * @param absImportsToAustraliaByState the absImportsToAustraliaByState to set
	 */
	public void setAbsImportsToAustraliaByState(Map<String, Float> absImportsToAustraliaByState) {
		this.absImportsToAustraliaByState = absImportsToAustraliaByState;
	}

	/**
	 * Adds a single state's imports to the country
	 * 
	 * @param state
	 * @param absImportsToAustralia
	 */
	public void putAbsImportsToAustraliaByState(String state, Float absImportsToAustralia) {
		if (this.absImportsToAustraliaByState == null) {
			this.absImportsToAustraliaByState = new HashMap<String, Float>((int) Math.ceil(STATES.length / 0.75f));
		}
		this.absImportsToAustraliaByState.put(state, absImportsToAustralia);
	}

	/**
	 * @return the actualExportsFromAustraliaByState
	 */
	public Map<String, Float> getActualExportsFromAustraliaByState() {
		return actualExportsFromAustraliaByState;
	}

	/**
	 * @return the actualImportsToAustraliaByState
	 */
	public Map<String, Float> getActualImportsToAustraliaByState() {
		return actualImportsToAustraliaByState;
	}

}
