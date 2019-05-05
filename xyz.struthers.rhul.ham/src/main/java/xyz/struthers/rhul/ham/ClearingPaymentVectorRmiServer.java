/**
 * 
 */
package xyz.struthers.rhul.ham;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import xyz.struthers.rhul.ham.config.Properties;
import xyz.struthers.rhul.ham.process.ClearingPaymentInputs;
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

	public ClearingPaymentVectorRmiServer() {
		super();
	}

	/**
	 * Using compressed objects for input and output to speed up the serialization /
	 * de-serialization and network transfer. All the third party libraries I've
	 * tried using have failed, so now I'm trying to make Java's default RMI work
	 * faster.
	 */
	public byte[] calculate(byte[] compressedBytes) throws RemoteException {
		System.out.println(new Date(System.currentTimeMillis()) + ": " + compressedBytes.length + " bytes received.");
		ClearingPaymentInputs cpvInputs = null;
		try {
			// uncompress CPV inputs
			ByteArrayInputStream bais = new ByteArrayInputStream(compressedBytes);
			GZIPInputStream gzipIn = new GZIPInputStream(bais);
			ObjectInputStream objectIn = new ObjectInputStream(gzipIn);
			cpvInputs = (ClearingPaymentInputs) objectIn.readObject();
			objectIn.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		System.out.println(new Date(System.currentTimeMillis()) + ": CPV inputs unmarshalled.");
		List<List<Float>> liabilitiesAmounts = cpvInputs.getLiabilitiesAmounts();
		List<List<Integer>> liabilitiesIndices = cpvInputs.getLiabilitiesIndices();
		List<Float> operatingCashFlow = cpvInputs.getOperatingCashFlow();
		List<Float> liquidAssets = cpvInputs.getLiquidAssets();
		int iteration = cpvInputs.getIteration();
		System.out.println(new Date(System.currentTimeMillis()) + ": CPV inputs split into separate lists.");

		// calculate CPV
		ClearingPaymentOutputs cpvOutputs = this.calculate(liabilitiesAmounts, liabilitiesIndices, operatingCashFlow,
				liquidAssets, iteration);

		System.out.println(new Date(System.currentTimeMillis()) + ": compressing CPV outputs.");
		ByteArrayOutputStream baos = null;
		try {
			// compress outputs
			baos = new ByteArrayOutputStream(Properties.NETWORK_BUFFER_BYTES);
			GZIPOutputStream gzipOut = new GZIPOutputStream(baos);
			ObjectOutputStream objectOut = new ObjectOutputStream(gzipOut);
			objectOut.writeObject(cpvOutputs);
			objectOut.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		byte[] bytes = baos.toByteArray();
		System.out
				.println(new Date(System.currentTimeMillis()) + ": sending CPV outputs (" + bytes.length + " bytes).");
		return bytes;
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
			System.setProperty("java.rmi.server.hostname", Properties.RMI_HOST);
			LocateRegistry.createRegistry(Properties.RMI_PORT);

			// register object
			ClearingPaymentVectorRmiServer obj = new ClearingPaymentVectorRmiServer();
			ClearingPaymentVectorInterface stub = (ClearingPaymentVectorInterface) UnicastRemoteObject.exportObject(obj,
					0);

			// Bind the remote object's stub in the registry
			Registry registry = LocateRegistry.getRegistry(Properties.RMI_HOST, Properties.RMI_PORT);
			registry.bind("ClearingPaymentVector", stub);

			System.err.println("ClearingPaymentVector server ready");
		} catch (Exception e) {
			System.err.println("ClearingPaymentVectorServer exception: " + e.toString());
			e.printStackTrace();
		}
	}

}
