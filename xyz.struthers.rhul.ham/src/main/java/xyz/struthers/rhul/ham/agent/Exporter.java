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
	// protected float salesForeign; // already in Business super class

	// exporter fields
	private ArrayList<ForeignCountry> destinationCountries; // average 2.4 countries per exporter
	private ArrayList<Float> destinationCountryInitialRatios;

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

		// this.salesForeign = exporter.salesForeign;
		this.destinationCountries = new ArrayList<ForeignCountry>(exporter.destinationCountries);
		this.destinationCountryInitialRatios = new ArrayList<Float>(exporter.destinationCountryInitialRatios);
	}

	@Override
	public List<NodePayment> getAmountsPayable(int iteration) {
		List<NodePayment> liabilities = super.getAmountsPayable(iteration);

		// TODO: add import/export liabilities

		return liabilities;
	}

	protected void init() {
		super.init();

		this.destinationCountries = null;
		this.destinationCountryInitialRatios = null;
	}

	/**
	 * @return the destinationCountries
	 */
	public ArrayList<ForeignCountry> getDestinationCountries() {
		return destinationCountries;
	}

	/**
	 * @param destinationCountries the destinationCountries to set
	 */
	public void setDestinationCountries(ArrayList<ForeignCountry> destinationCountries) {
		this.destinationCountries = destinationCountries;
	}

	/**
	 * @return the destinationCountryInitialRatios
	 */
	public ArrayList<Float> getDestinationCountryInitialRatios() {
		return destinationCountryInitialRatios;
	}

	/**
	 * @param destinationCountryInitialRatios the destinationCountryInitialRatios to
	 *                                        set
	 */
	public void setDestinationCountryInitialRatios(ArrayList<Float> destinationCountryInitialRatios) {
		this.destinationCountryInitialRatios = destinationCountryInitialRatios;
	}

	/**
	 * @return the serialversionuid
	 */
	public static long getSerialversionuid() {
		return serialVersionUID;
	}

}
