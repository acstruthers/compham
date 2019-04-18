/**
 * 
 */
package xyz.struthers.rhul.ham;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import xyz.struthers.rhul.ham.process.ClearingPaymentOutputs;

/**
 * ClearingPaymentVector interface to enable RMI processing on a separate
 * computer. This helps to overcome the RAM constraints.
 * 
 * @author Adam Struthers
 * @since 14-Apr-2019
 */
public interface ClearingPaymentVectorInterface extends Remote {

	public ClearingPaymentOutputs calculate(List<List<Float>> liabilitiesAmounts,
			List<List<Integer>> liabilitiesIndices, List<Float> operatingCashFlow, List<Float> liquidAssets,
			int iteration) throws RemoteException;

	// https://stackoverflow.com/questions/5934495/implementing-in-memory-compression-for-objects-in-java
	public byte[] calculate(byte[] compressedBytes) throws RemoteException;

}
