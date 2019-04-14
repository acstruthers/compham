/**
 * 
 */
package xyz.struthers.rhul.ham;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Map;

/**
 * ClearingPaymentVector server to enable RMI processing on a separate computer.
 * This helps to overcome the RAM constraints. This server is the part that
 * actually calculates the clearing payment vector. It is called by a client
 * that contains the details of all the agents in the economy.
 * 
 * @author Adam Struthers
 * @since 14-Apr-2019
 */
public class ClearingPaymentVectorServer implements ClearingPaymentVectorInterface {

	public static final String RMI_HOST = "Adam-E590";
	public static final int RMI_PORT = 1099;

	public ClearingPaymentVectorServer() {
		super();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * xyz.struthers.rhul.ham.ClearingPaymentVectorInterface#calculate(java.util.
	 * List, java.util.List, java.util.List)
	 */
	@Override
	public Map<String, Object> calculate(List<List<Float>> liabilitiesAmounts, List<List<Integer>> liabilitiesIndices,
			List<Float> operatingCashFlow, int iteration) throws RemoteException {

		RunSimulation sim = new RunSimulation();
		Map<String, Object> cpvOutputs = sim.calculateClearingPaymentVector(liabilitiesAmounts, liabilitiesIndices,
				operatingCashFlow, iteration);

		return cpvOutputs;
	}

	/**
	 * This starts the server and registers the ClearingPaymentVectorInterface so
	 * it's ready to be invoked.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			// Create RMI Registry
			System.setProperty("java.rmi.server.hostname", RMI_HOST);
			LocateRegistry.createRegistry(RMI_PORT);

			// register object
			ClearingPaymentVectorServer obj = new ClearingPaymentVectorServer();
			ClearingPaymentVectorInterface stub = (ClearingPaymentVectorInterface) UnicastRemoteObject.exportObject(obj,
					0);

			// Bind the remote object's stub in the registry
			Registry registry = LocateRegistry.getRegistry(RMI_HOST, RMI_PORT);
			registry.bind("ClearingPaymentVector", stub);

			System.err.println("ClearingPaymentVector server ready");
		} catch (Exception e) {
			System.err.println("ClearingPaymentVectorServer exception: " + e.toString());
			e.printStackTrace();
		}
	}

}
