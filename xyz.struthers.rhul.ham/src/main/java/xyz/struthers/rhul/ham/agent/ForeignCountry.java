/**
 * 
 */
package xyz.struthers.rhul.ham.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import xyz.struthers.rhul.ham.data.Currency;
import xyz.struthers.rhul.ham.process.NodePayment;

/**
 * The model does not care about the financial impact on foreign countries -
 * only their impact on the Australian economy - so very little detail is
 * recorded against the countries themselves. Most of the details are recorded
 * against the individual businesses that are engaged in international trade
 * with these countries.
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

	// agent relationships
	private int paymentClearingIndex;
	private ArrayList<Exporter> exporters;

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
	private Map<String, Float> exportsFromAustraliaByState; // 8 states
	private Map<String, Float> importsToAustraliaByState;

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
		this.exportsFromAustraliaByState = exportsFromAusByState;
		this.importsToAustraliaByState = importsToAusByState;
	}

	public void addExporter(Exporter exporter) {
		if (this.exporters == null) {
			this.exporters = new ArrayList<Exporter>(EXPORTERS_INIT_CAPACITY);
		}
		this.exporters.add(exporter);
	}

	public void addAllExporters(List<Exporter> exporters) {
		if (this.exporters == null) {
			this.exporters = new ArrayList<Exporter>(EXPORTERS_INIT_CAPACITY);
		}
		this.exporters.addAll(exporters);
	}

	public void trimExportersListToSize() {
		if (this.exporters != null) {
			this.exporters.trimToSize();
		}
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
		for (int exporterIdx = 0; exporterIdx < this.exporters.size(); exporterIdx++) {
			int index = this.exporters.get(exporterIdx).getPaymentClearingIndex();
			float audAmount = this.exporters.get(exporterIdx).getSalesForeign() * exchRateAdjustment;
			liabilities.add(new NodePayment(index, audAmount));
		}

		liabilities.trimToSize();
		return liabilities;
	}

	protected void init() {
		super.name = null;

		this.paymentClearingIndex = 0;
		this.exporters = null;

		this.currency = null;
		this.totalExportsFromAustralia = 0f;
		this.totalImportsToAustralia = 0f;
		this.exportsFromAustraliaByState = null;
		this.importsToAustraliaByState = null;
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
	public Map<String, Float> getExportsFromAustraliaByState() {
		return exportsFromAustraliaByState;
	}

	/**
	 * @param exportsFromAustraliaByState the exportsFromAustraliaByState to set
	 */
	public void setExportsFromAustraliaByState(Map<String, Float> exportsFromAustraliaByState) {
		this.exportsFromAustraliaByState = exportsFromAustraliaByState;
	}

	/**
	 * @return the importsToAustraliaByState
	 */
	public Map<String, Float> getImportsToAustraliaByState() {
		return importsToAustraliaByState;
	}

	/**
	 * @param importsToAustraliaByState the importsToAustraliaByState to set
	 */
	public void setImportsToAustraliaByState(Map<String, Float> importsToAustraliaByState) {
		this.importsToAustraliaByState = importsToAustraliaByState;
	}

}
