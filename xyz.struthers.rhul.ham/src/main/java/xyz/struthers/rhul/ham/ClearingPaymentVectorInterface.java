/**
 * 
 */
package xyz.struthers.rhul.ham;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

/**
 * ClearingPaymentVector interface to enable RMI processing on a separate
 * computer. This helps to overcome the RAM constraints.
 * 
 * @author Adam Struthers
 * @since 14-Apr-2019
 */
public interface ClearingPaymentVectorInterface extends Remote {

	public Map<String, Object> calculate(List<List<Float>> liabilitiesAmounts, List<List<Integer>> liabilitiesIndices,
			List<Float> operatingCashFlow, int iteration) throws RemoteException;

}
