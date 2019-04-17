/**
 * 
 */
package xyz.struthers.rhul.ham;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Date;
import java.util.List;
import java.util.Map;

import xyz.struthers.rhul.ham.process.ClearingPaymentOutputs;

/**
 * ClearingPaymentVector server to enable RMI processing on a separate computer.
 * This helps to overcome the RAM constraints. This server is the part that
 * actually calculates the clearing payment vector. It is called by a client
 * that contains the details of all the agents in the economy.
 * 
 * @author Adam Struthers
 * @since 14-Apr-2019
 */
public class ClearingPaymentVectorRmiServer implements ClearingPaymentVectorInterface {

	public static final String RMI_HOST = "Adam-E590";
	public static final int RMI_PORT = 1099;

	public ClearingPaymentVectorRmiServer() {
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
	public ClearingPaymentOutputs calculate(List<List<Float>> liabilitiesAmounts,
			List<List<Integer>> liabilitiesIndices, List<Float> operatingCashFlow, List<Float> liquidAssets,
			int iteration) throws RemoteException {

		System.out.println(new Date(System.currentTimeMillis()) + ": CPV invoked via RMI.");

		RunSimulation sim = new RunSimulation();
		ClearingPaymentOutputs cpvOutputs = sim.calculateClearingPaymentVector(liabilitiesAmounts, liabilitiesIndices,
				operatingCashFlow, liquidAssets, iteration);

		System.out.println(
				new Date(System.currentTimeMillis()) + ": CPV outputs calculated and being returned to RMI client.");

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
			ClearingPaymentVectorRmiServer obj = new ClearingPaymentVectorRmiServer();
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