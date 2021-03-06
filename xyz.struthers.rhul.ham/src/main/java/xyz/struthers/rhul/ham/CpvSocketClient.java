/**
 * 
 */
package xyz.struthers.rhul.ham;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.Date;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import xyz.struthers.io.Serialization;
import xyz.struthers.io.Serialization.CompressionType;
import xyz.struthers.rhul.ham.config.PropertiesXml;
import xyz.struthers.rhul.ham.config.PropertiesXmlFactory;
import xyz.struthers.rhul.ham.config.SpringConfiguration;
import xyz.struthers.rhul.ham.process.AustralianEconomy;
import xyz.struthers.rhul.ham.process.ClearingPaymentInputs;
import xyz.struthers.rhul.ham.process.ClearingPaymentOutputs;

/**
 * KryonetClient.java contains an example of how to use threads successfully
 * 
 * @author Adam Struthers
 * @since 2019-05-15
 */
public class CpvSocketClient {

	// TODO: update name & iterations for each scenario (in properties file)
	// public final static String SCENARIO_NAME = "Baseline";
	// public final static boolean[] SAVE_ITERATION_SUMMARY = new boolean[] { true,
	// true, true, true, true, true, true,
	// true, true, true, true, true, true };
	// public final static int NUM_ITERATIONS = SAVE_ITERATION_SUMMARY.length; //
	// first iteration is zero

	static Thread t;
	// static Client client = null;
	// static ClearingPaymentInputs cpvInputs = null;
	// static ClearingPaymentOutputs cpvOutputs = null;

	/**
	 * This CpvSocketClient constructor sends inputs to the CPV server to process,
	 * and listens for the result, then returns control to the main thread.
	 */
	/*
	 * public CpvSocketClient() { super();
	 * 
	 * // TODO implement client logic here ClearingPaymentInputs cpvInputs = null;
	 * 
	 * }
	 */

	/*
	 * private void sendToServer(ClearingPaymentInputs cpvInputs) { Socket
	 * clientSocket = null; ObjectOutputStream out = null;
	 * 
	 * try { clientSocket = new Socket(Properties.CPV_SERVER_HOST,
	 * Properties.CPV_SERVER_PORT); out = new
	 * ObjectOutputStream(clientSocket.getOutputStream()); } catch
	 * (UnknownHostException e) { e.printStackTrace(); return; } catch (IOException
	 * e) { e.printStackTrace(); return; }
	 * 
	 * try { DataOutputStream dos = new
	 * DataOutputStream(clientSocket.getOutputStream());
	 * 
	 * byte[] bytes = null; byte compression = 1; switch (compression) { case 1: //
	 * GZIP compression bytes = Serialization.toBytesGZIP(cpvInputs,
	 * Properties.SOCKET_BUFFER_BYTES); break; case 2: // Deflater compression bytes
	 * = Serialization.toBytesDeflater(cpvInputs, Properties.SOCKET_BUFFER_BYTES);
	 * break; default: // no compression bytes = Serialization.toBytes(cpvInputs,
	 * Properties.SOCKET_BUFFER_BYTES); break; }
	 * 
	 * dos.writeInt(bytes.length); dos.writeByte(compression); dos.write(bytes);
	 * dos.flush(); // don't know if I really need this, but it probably can't hurt
	 * } catch (IOException e) { e.printStackTrace(); }
	 * 
	 * try { out.close(); clientSocket.close();
	 * System.out.println("Connection to client: " + clientSocket.getInetAddress() +
	 * " closed."); } catch (IOException e) { e.printStackTrace(); } }
	 */

	/**
	 * Sends inputs to the CPV server to process, and listens for the result, then
	 * returns control to the main thread.
	 * 
	 * @param cpvInputs
	 * @return cpvOutputs
	 */
	private static ClearingPaymentOutputs runCpvOnServer(ClearingPaymentInputs cpvInputs, PropertiesXml properties) {
		// getting CPV server's IP address
		InetAddress ip = null;
		try {
			ip = InetAddress.getByName(properties.getCpvServerHost());
		} catch (UnknownHostException e) {
			System.err.println("Unknown host: " + properties.getCpvServerHost());
			e.printStackTrace();
		}

		// establish the connection with server on teh specified port
		Socket s = null;
		DataInputStream dis = null;
		DataOutputStream dos = null;
		ClearingPaymentOutputs cpvOutputs = null;
		try {
			s = new Socket(ip, properties.getCpvServerPort());

			// obtaining input and out streams
			dis = new DataInputStream(s.getInputStream());
			dos = new DataOutputStream(s.getOutputStream());

			// OUTBOUND
			// send CPV inputs to server for processing
			System.out.println(new Date(System.currentTimeMillis()) + ": serializing CPV inputs.");
			//Serialization.writeToDataStream(dos, cpvInputs, properties.getSocketBufferBytes(),
			//		properties.getSocketMessageBytes(), CompressionType.NO_COMPRESSION);
			
			// The above ran into memory/size issues, so split it into its components
			Serialization.writeToDataStream(dos, cpvInputs.getLiabilitiesAmounts(), properties.getSocketBufferBytes(),
					properties.getSocketMessageBytes(), CompressionType.NO_COMPRESSION);
			Serialization.writeToDataStream(dos, cpvInputs.getLiabilitiesIndices(), properties.getSocketBufferBytes(),
					properties.getSocketMessageBytes(), CompressionType.NO_COMPRESSION);
			Serialization.writeToDataStream(dos, cpvInputs.getOperatingCashFlow(), properties.getSocketBufferBytes(),
					properties.getSocketMessageBytes(), CompressionType.NO_COMPRESSION);
			Serialization.writeToDataStream(dos, cpvInputs.getLiquidAssets(), properties.getSocketBufferBytes(),
					properties.getSocketMessageBytes(), CompressionType.NO_COMPRESSION);
			Serialization.writeToDataStream(dos, cpvInputs.getIteration(), properties.getSocketBufferBytes(),
					properties.getSocketMessageBytes(), CompressionType.NO_COMPRESSION);
			
			/*
			 * byte[] bytes = null; byte compression = 1; switch (compression) { case 1: //
			 * GZIP compression bytes = Serialization.toBytesGZIP(cpvInputs,
			 * Properties.SOCKET_BUFFER_BYTES); break; case 2: // Deflater compression bytes
			 * = Serialization.toBytesDeflater(cpvInputs, Properties.SOCKET_BUFFER_BYTES);
			 * break; default: // no compression bytes = Serialization.toBytes(cpvInputs,
			 * Properties.SOCKET_BUFFER_BYTES); break; }
			 * 
			 * System.out.println(new Date(System.currentTimeMillis()) +
			 * ": sending CPV inputs to server."); dos.writeInt(bytes.length);
			 * dos.writeByte(compression); dos.write(bytes); dos.flush(); // don't know if I
			 * really need this, but it probably can't hurt
			 */
			System.out.println(new Date(System.currentTimeMillis()) + ": CPV inputs sent to server.");

			// INBOUND
			// receive CPV outputs from server
			System.out.println(new Date(System.currentTimeMillis()) + ": receiving CPV outputs from server.");
			cpvOutputs = (ClearingPaymentOutputs) Serialization.readObjectFromStream(dis);
			/*
			 * int size = dis.readInt(); compression = dis.readByte(); bytes = new
			 * byte[size]; dis.readFully(bytes);
			 * 
			 * // deserialize bytes to CPV input System.out.println(new
			 * Date(System.currentTimeMillis()) + ": deserializing CPV outputs."); switch
			 * (compression) { case 1: // GZIP compression cpvOutputs =
			 * (ClearingPaymentOutputs) Serialization.toObjectFromGZIP(bytes); break; case
			 * 2: // Deflater compression cpvOutputs = (ClearingPaymentOutputs)
			 * Serialization.toObjectFromDeflater(bytes); break; default: // no compression
			 * cpvOutputs = (ClearingPaymentOutputs) Serialization.toObject(bytes); break; }
			 */
		} catch (IOException e) {
			System.err.println("Error connecting to : " + properties.getCpvServerHost() + " on port: "
					+ properties.getCpvServerPort());
			e.printStackTrace();
		}

		// close resources
		try {
			System.out.println(new Date(System.currentTimeMillis()) + ": closing data input and output streams.");
			dis.close();
			dos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return cpvOutputs;
	}

	/**
	 * Runs the simulation in a series of stages, releasing memory as it goes and
	 * sending CPV calculations to another computer (the Kryonet server) so it will
	 * run on two computers with 32GB of RAM.
	 * 
	 * @param args - the fully qualified filename of the XML file that contains the
	 *             properties to use for this simulation.
	 */
	public static void main(String[] args) {

		DecimalFormat formatter = new DecimalFormat("#,##0.00");
		long memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		float megabytesBefore = memoryBefore / 1024f / 1024f;
		System.out.println("################################################");
		System.out.println(new Date(System.currentTimeMillis()) + ": MEMORY USAGE IN MAIN AT BEGINNING: "
				+ formatter.format(megabytesBefore) + "MB");
		System.out.println("################################################");

		// set properties filename
		for (int i = 0; i < args.length; i++) {
			System.out.println("args[" + i + "] = " + args[i]);
		}
		PropertiesXmlFactory.propertiesXmlFilename = args[0];
		PropertiesXml props = PropertiesXmlFactory.getProperties();

		// load Spring context
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(SpringConfiguration.class);
		InitialiseEconomy init = new InitialiseEconomy();
		init.initialiseEconomy(ctx); // iteration 0
		AustralianEconomy economy = init.getEconomy();
		init.close();
		init = null;
		System.gc();

		long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		megabytesBefore = memoryBefore / 1024f / 1024f;
		float megabytesAfter = memoryAfter / 1024f / 1024f;
		System.out.println("################################################");
		System.out.println(new Date(System.currentTimeMillis()) + ": MEMORY USAGE IN MAIN AFTER MAKING CPV INPUTS: "
				+ formatter.format(megabytesAfter) + "MB");
		System.out.println("################################################");
		memoryBefore = memoryAfter;

		int iteration = 0;
		for (iteration = 0; iteration < props.getNumberOfIterations(); iteration++) {
			// prepare CPV inputs inside loop
			ClearingPaymentInputs cpvInputs = economy.prepareOneMonth(iteration, props.getScenarioName());

			// send to CpvSocketServer to calculate CPV
			// new CpvSocketClient();
			ClearingPaymentOutputs cpvOutputs = runCpvOnServer(cpvInputs, props);
			/*
			 * try { // wait for CPV listener to terminate before continuing with execution
			 * t.join(); } catch (InterruptedException e) { // do nothing }
			 */

			System.gc();
			memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			megabytesAfter = memoryAfter / 1024f / 1024f;
			System.out.println("################################################");
			System.out.println(new Date(System.currentTimeMillis()) + ": MEMORY USAGE IN MAIN AFTER CALCULATING CPV: "
					+ formatter.format(megabytesAfter) + "MB)");
			System.out.println("################################################");
			memoryBefore = memoryAfter;

			// update financial statements
			economy.updateOneMonth(cpvOutputs);
			cpvInputs.clear();
			cpvInputs = null;
			cpvOutputs.close();
			cpvOutputs = null;

			System.gc();
			memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			megabytesAfter = memoryAfter / 1024f / 1024f;
			System.out.println("################################################");
			System.out.println(new Date(System.currentTimeMillis())
					+ ": MEMORY USAGE IN MAIN AFTER UPDATING ECONOMY WITH CPV OUTPUTS: "
					+ formatter.format(megabytesAfter) + "MB)");
			System.out.println("################################################");
			memoryBefore = memoryAfter;

			// save summary to file
			if (props.getSaveIterationSummary(iteration)) {
				economy.saveSummaryToFile(iteration, props.getScenarioName());
			}
			// details after being updated with CPV output
			// 6.32GB of CSV files (takes 4 minutes to write to disk)
			// economy.saveDetailsToFile(iteration);
		}

		ctx.close();
	}

}
