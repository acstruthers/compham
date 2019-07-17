/**
 * 
 */
package xyz.struthers.rhul.ham.agent;

import gnu.trove.map.hash.TObjectFloatHashMap;

/**
 * @author Adam Struthers
 * @since 26-Jan-2019
 */
public final class MajorBank extends AuthorisedDepositTakingInstitution {

	private static final long serialVersionUID = 1L;

	/**
	 * Default constructor
	 */
	public MajorBank() {
		super();
	}

	/**
	 * Initialisation constructor
	 * 
	 * @param adiAustralianBusinessNumber
	 * @param adiShortName
	 * @param adiLongName
	 * @param adiType
	 * @param financialStatementAmounts
	 */
	public MajorBank(String adiAustralianBusinessNumber, String adiShortName, String adiLongName, String adiType,
			TObjectFloatHashMap<String> financialStatementAmounts) {
		super(adiAustralianBusinessNumber, adiShortName, adiLongName, adiType, financialStatementAmounts);
	}

}
