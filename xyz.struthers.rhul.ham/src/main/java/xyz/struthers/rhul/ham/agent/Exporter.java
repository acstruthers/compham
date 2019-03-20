/**
 * 
 */
package xyz.struthers.rhul.ham.agent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import xyz.struthers.rhul.ham.data.Currency;
import xyz.struthers.rhul.ham.process.NodePayment;

/**
 * There are approximately 50,000 exporters in the model. Each one uses rouhgly
 * an incremental 90 bytes, so they will consume approximately 4.3 MB of RAM.
 * Using a sub-class saved approximately 290 MB of RAM.
 * 
 * @author Adam Struthers
 * @since 02-Feb-2019
 */
public final class Exporter extends Business {

	private static final long serialVersionUID = 1L;

	// P&L
	protected double salesForeign;

	// exporter fields
	private Map<ForeignCountry, Double> destinationCountryAmounts; // average 2.4 countries per exporter
	private List<Currency> currencies;

	/**
	 * Default constructor
	 */
	public Exporter() {
		super();
		this.init();
	}

	/**
	 * Copy constructor
	 * 
	 * @param exporter
	 */
	public Exporter(Exporter exporter) {
		super(exporter);

		this.destinationCountryAmounts = new HashMap<ForeignCountry, Double>(exporter.destinationCountryAmounts.size());
		for (ForeignCountry country : exporter.destinationCountryAmounts.keySet()) {
			this.destinationCountryAmounts.put(country, exporter.getDestinationCountryAmounts().get(country));
		}

		this.currencies = new ArrayList<Currency>(exporter.currencies.size());
		for (Currency ccy : exporter.currencies) {
			this.currencies.add(ccy);
		}
	}

	@Override
	public List<NodePayment> getAmountsPayable(int iteration) {
		List<NodePayment> liabilities = super.getAmountsPayable(iteration);
		
		// TODO: add import/export liabilities
		
		return liabilities;
	}

	protected void init() {
		super.init();

		this.destinationCountryAmounts = null;
		this.currencies = null;
	}

	/**
	 * @return the destinationCountries
	 */
	public Map<ForeignCountry, Double> getDestinationCountryAmounts() {
		return destinationCountryAmounts;
	}

	/**
	 * @param destinationCountries the destinationCountries to set
	 */
	public void setDestinationCountryAmounts(Map<ForeignCountry, Double> destinationCountryAmounts) {
		this.destinationCountryAmounts = destinationCountryAmounts;
	}

	/**
	 * @return the currencies
	 */
	public List<Currency> getCurrencies() {
		return currencies;
	}

	/**
	 * @param currencies the currencies to set
	 */
	public void setCurrencies(List<Currency> currencies) {
		this.currencies = currencies;
	}

	/**
	 * @return the serialversionuid
	 */
	public static long getSerialversionuid() {
		return serialVersionUID;
	}

}
