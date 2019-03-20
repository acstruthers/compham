/**
 * 
 */
package xyz.struthers.rhul.ham.agent;

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
 * Each instance of this class stores 1 pointer, 18 doubles and 17 strings, so
 * will consume approximately 260 bytes of RAM. There are 114 instances of this
 * class in the model, so they will consume approximately 28.95 kB of RAM.
 * 
 * @author Adam Struthers
 * @since 27-Jan-2019
 */
public final class ForeignCountry extends Agent {

	private static final long serialVersionUID = 1L;
	
	// agent relationships
	protected int paymentClearingIndex;
	
	// field variables
	private Currency currency;
	private double totalExportsFromAustralia;
	private double totalImportsToAustralia;
	private Map<String, Double> exportsFromAustraliaByState; // 8 states
	private Map<String, Double> importsToAustraliaByState;

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

	public ForeignCountry(String countryName, Currency countryCurrency, double exportsFromAustralia,
			double importsToAustralia, Map<String, Double> exportsFromAusByState,
			Map<String, Double> importsToAusByState) {
		super();
		this.init();

		super.name = countryName;
		this.currency = countryCurrency;
		this.totalExportsFromAustralia = exportsFromAustralia;
		this.totalImportsToAustralia = importsToAustralia;
		this.exportsFromAustraliaByState = exportsFromAusByState;
		this.importsToAustraliaByState = importsToAusByState;
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
		// TODO Auto-generated method stub
		return null;
	}

	protected void init() {
		super.name = null;
		this.currency = null;
		this.totalExportsFromAustralia = 0d;
		this.totalImportsToAustralia = 0d;
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
	 * @param currency
	 *            the currency to set
	 */
	public void setCurrency(Currency currency) {
		this.currency = currency;
	}

	/**
	 * @return the totalExportsFromAustralia
	 */
	public double getTotalExportsFromAustralia() {
		return totalExportsFromAustralia;
	}

	/**
	 * @param totalExportsFromAustralia
	 *            the totalExportsFromAustralia to set
	 */
	public void setTotalExportsFromAustralia(double totalExportsFromAustralia) {
		this.totalExportsFromAustralia = totalExportsFromAustralia;
	}

	/**
	 * @return the totalImportsToAustralia
	 */
	public double getTotalImportsToAustralia() {
		return totalImportsToAustralia;
	}

	/**
	 * @param totalImportsToAustralia
	 *            the totalImportsToAustralia to set
	 */
	public void setTotalImportsToAustralia(double totalImportsToAustralia) {
		this.totalImportsToAustralia = totalImportsToAustralia;
	}

	/**
	 * @return the exportsFromAustraliaByState
	 */
	public Map<String, Double> getExportsFromAustraliaByState() {
		return exportsFromAustraliaByState;
	}

	/**
	 * @param exportsFromAustraliaByState
	 *            the exportsFromAustraliaByState to set
	 */
	public void setExportsFromAustraliaByState(Map<String, Double> exportsFromAustraliaByState) {
		this.exportsFromAustraliaByState = exportsFromAustraliaByState;
	}

	/**
	 * @return the importsToAustraliaByState
	 */
	public Map<String, Double> getImportsToAustraliaByState() {
		return importsToAustraliaByState;
	}

	/**
	 * @param importsToAustraliaByState
	 *            the importsToAustraliaByState to set
	 */
	public void setImportsToAustraliaByState(Map<String, Double> importsToAustraliaByState) {
		this.importsToAustraliaByState = importsToAustraliaByState;
	}

}
