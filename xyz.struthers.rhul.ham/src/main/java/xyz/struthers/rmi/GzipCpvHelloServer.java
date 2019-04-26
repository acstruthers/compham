/**
 * 
 */
package xyz.struthers.rmi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import xyz.struthers.rhul.ham.process.ClearingPaymentInputs;
import xyz.struthers.rhul.ham.process.ClearingPaymentOutputs;

/**
 * @author acstr
 *
 */
public class GzipCpvHelloServer implements IHello, GzipCpvInterface {

	public static final String HOST = "Adam-E590";
	public static final int PORT = 1099;
	public static final int NUM_AGENTS = GzipCpvHelloClient.NUM_AGENTS;

	public GzipCpvHelloServer() {
		super();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see xyz.struthers.rmi.IHello#sayHello(java.lang.String)
	 */
	@Override
	public String sayHello(String input) throws RemoteException {
		return input + ", world!";
	}

	/**
	 * 
	 * @param cpvInputBytes - cpvInputs zipped using GZip
	 * @return covOutputs zipped using GZip
	 * @throws RemoteException
	 */
	@Override
	public byte[] calculate(byte[] cpvInputBytes) throws RemoteException {
		// Maybe store the bytes first, then unmarshall them all at once later?
		// Could send a different object just to signal that transfer is complete?

		// decompress CPV input
		System.out.println(new Date(System.currentTimeMillis()) + ": " + cpvInputBytes.length + " bytes received.");
		ClearingPaymentInputs cpvInputs = null;
		ByteArrayInputStream bais = new ByteArrayInputStream(cpvInputBytes);
		GZIPInputStream gzipIn;
		try {
			gzipIn = new GZIPInputStream(bais);
			ObjectInputStream objectIn = new ObjectInputStream(gzipIn);
			cpvInputs = (ClearingPaymentInputs) objectIn.readObject();
			objectIn.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		System.out.println(new Date(System.currentTimeMillis()) + ": CPV inputs unmarshalled.");

		// create dummy CPV output
		ClearingPaymentOutputs cpvOutput = new ClearingPaymentOutputs();
		List<Float> equityOfNode = new ArrayList<Float>(Collections.nCopies(NUM_AGENTS, 1f));
		List<Integer> defaultOrderOfNode = new ArrayList<Integer>(Collections.nCopies(NUM_AGENTS, 1));
		int iteration = 1;
		cpvOutput.setEquityOfNode(equityOfNode);
		cpvOutput.setDefaultOrderOfNode(defaultOrderOfNode);
		cpvOutput.setIteration(iteration);

		// compress CPV output
		System.out.println(new Date(System.currentTimeMillis()) + ": compressing CPV outputs.");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			GZIPOutputStream gzipOut = new GZIPOutputStream(baos);
			ObjectOutputStream objectOut = new ObjectOutputStream(gzipOut);
			objectOut.writeObject(cpvOutput);
			objectOut.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		byte[] cpvOutputBytes = baos.toByteArray();
		System.out.println(new Date(System.currentTimeMillis()) + ": sending CPV outputs (" + cpvOutputBytes.length + " bytes).");
		return cpvOutputBytes;
	}

	public static void main(String args[]) {
		try {
			// Create RMI Registry
			System.setProperty("java.rmi.server.hostname", HOST);
			LocateRegistry.createRegistry(PORT);

			// register object
			GzipCpvHelloServer obj = new GzipCpvHelloServer();
			//IHello stub = (IHello) UnicastRemoteObject.exportObject(obj, 0);
			GzipCpvInterface cpvStub = (GzipCpvInterface) UnicastRemoteObject.exportObject(obj, 0);

			// Bind the remote object's stub in the registry
			Registry registry = LocateRegistry.getRegistry(HOST, PORT);
			//registry.bind("Hello", stub);
			registry.bind("GzipCpv", cpvStub);

			System.err.println("Server ready");
		} catch (Exception e) {
			System.err.println("GzipCpvHelloServer exception: " + e.toString());
			e.printStackTrace();
		}
	}
}
