/**
 * 
 */
package xyz.struthers.rhul.ham.agent;

import java.util.Map;

/**
 * @author Adam Struthers
 * @since 26-Jan-2019
 */
public final class MutualBank extends AuthorisedDepositTakingInstitution {

	private static final long serialVersionUID = 1L;

	/**
	 * Default constructor
	 */
	public MutualBank() {
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
	public MutualBank(String adiAustralianBusinessNumber, String adiShortName, String adiLongName, String adiType,
			Map<String, Double> financialStatementAmounts) {
		super(adiAustralianBusinessNumber, adiShortName, adiLongName, adiType, financialStatementAmounts);
	}

}
