/**
 * 
 */
package xyz.struthers.rmi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
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
public class GzipCpvHelloClient {

	public static final String HOST = "Adam-E590";
	public static final int PORT = 1099;
	public static final int NUM_AGENTS = 1000;
	public static final int NUM_LINKS_PER_AGENT = 100;

	public GzipCpvHelloClient() {
		super();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// String host = (args.length < 1) ? null : args[0];
		try {
			// create dummy CPV input
			List<List<Float>> liabilitiesAmounts = new ArrayList<List<Float>>(NUM_AGENTS);
			List<List<Integer>> liabilitiesIndices = new ArrayList<List<Integer>>(NUM_AGENTS);
			for (int i = 0; i < NUM_AGENTS; i++) {
				liabilitiesAmounts.add(new ArrayList<Float>(Collections.nCopies(NUM_LINKS_PER_AGENT, 0f)));
				liabilitiesIndices.add(new ArrayList<Integer>(Collections.nCopies(NUM_LINKS_PER_AGENT, 0)));
			}
			List<Float> operatingCashFlow = new ArrayList<Float>(Collections.nCopies(NUM_AGENTS, 0f));
			List<Float> liquidAssets = new ArrayList<Float>(Collections.nCopies(NUM_AGENTS, 0f));
			int iteration = 0;
			ClearingPaymentInputs cpvInput = new ClearingPaymentInputs(liabilitiesAmounts, liabilitiesIndices,
					operatingCashFlow, liquidAssets, iteration);

			// compress CPV input
			System.out.println(new Date(System.currentTimeMillis()) + ": compressing CPV inputs.");
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try {
				GZIPOutputStream gzipOut = new GZIPOutputStream(baos);
				ObjectOutputStream objectOut = new ObjectOutputStream(gzipOut);
				objectOut.writeObject(cpvInput);
				objectOut.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			byte[] cpvInputBytes = baos.toByteArray();

			// get RMI server object
			Registry registry = LocateRegistry.getRegistry(HOST, PORT);
			// IHello stub = (IHello) registry.lookup("Hello");
			GzipCpvInterface cpvStub = (GzipCpvInterface) registry.lookup("GzipCpv");

			// invoke remote method and receive results
			// String response = stub.sayHello("Hello");
			byte[] cpvOutputBytes = cpvStub.calculate(cpvInputBytes);

			System.out.println(new Date(System.currentTimeMillis()) + ": sending CPV inputs (" + cpvInputBytes.length
					+ " bytes).");
			ClearingPaymentOutputs cpvOutputs = null;
			ByteArrayInputStream bais = new ByteArrayInputStream(cpvOutputBytes);
			GZIPInputStream gzipIn;
			try {
				gzipIn = new GZIPInputStream(bais);
				ObjectInputStream objectIn = new ObjectInputStream(gzipIn);
				cpvOutputs = (ClearingPaymentOutputs) objectIn.readObject();
				objectIn.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			System.out.println(new Date(System.currentTimeMillis()) + ": CPV outputs unmarshalled.");
		} catch (Exception e) {
			System.err.println("Client exception: " + e.toString());
			e.printStackTrace();
		}
	}

}
