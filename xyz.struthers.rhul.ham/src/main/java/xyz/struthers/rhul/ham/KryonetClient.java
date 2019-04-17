/**
 * 
 */
package xyz.struthers.rhul.ham;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Date;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.nqzero.permit.Permit;

import xyz.struthers.rhul.ham.config.SpringConfiguration;
import xyz.struthers.rhul.ham.process.AustralianEconomy;
import xyz.struthers.rhul.ham.process.ClearingPaymentInputs;
import xyz.struthers.rhul.ham.process.ClearingPaymentOutputs;

/**
 * @author acstr
 *
 */
public class KryonetClient {

	static Thread t;
	static Client client = null;
	static ClearingPaymentInputs cpvInputs = null;
	static ClearingPaymentOutputs cpvOutputs = null;

	/**
	 * This KryonetClient constructor sends inputs to the CPV server to process, and
	 * listens for the result, then returns control to the main thread.
	 */
	public KryonetClient() {
		super();
		Permit.godMode(); // allows reflection in Java 11

		// create client and connect to server
		client = new Client();
		t = new Thread(client);
		t.start();
		// https://stackoverflow.com/questions/17011178/java-kryonet-servers-client-not-receiving-server-response
		try {
			client.connect(KryonetNetwork.NETWORK_TIMEOUT_MILLIS, KryonetNetwork.IPADDRESS_SERVER,
					KryonetNetwork.NETWORK_PORT_TCP, KryonetNetwork.NETWORK_PORT_UDP);
		} catch (IOException e) {
			e.printStackTrace();
		}
		KryonetNetwork.register(client);

		// create listener to process any messages received
		client.addListener(new Listener() {
			public void connected(Connection connection) {
				System.out.println("Kryonet CPV client connected.");
			}

			public void received(Connection connection, Object object) {
				if (object instanceof ClearingPaymentOutputs) {
					System.out.println("CPV outputs received at " + new Date(System.currentTimeMillis()) + ".");
					System.out.println("Updating agents in the economy now.");
					cpvOutputs = (ClearingPaymentOutputs) object;

					// return control to main thread to process CPV outputs
					client.removeListener(this);
					client.stop();
					return;
				}
			}

			public void disconnected(Connection connection) {
				System.out.println("Kryonet CPV client disconnected.");
				// System.exit(0);
			}
		});

		// send CPV inputs to server to process
		client.sendTCP(cpvInputs); // assumes cpvInputs has already been set
		System.out.println("CPV inputs sent at " + new Date(System.currentTimeMillis()) + ".");
	}

	/**
	 * Runs the simulation in a series of stages, releasing memory as it goes and
	 * sending CPV calculations to another computer (the Kryonet server) so it will
	 * run on two computers with 32GB of RAM.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		DecimalFormat formatter = new DecimalFormat("#,##0.00");
		long memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		float megabytesBefore = memoryBefore / 1024f / 1024f;
		System.out.println("################################################");
		System.out.println(new Date(System.currentTimeMillis()) + ": MEMORY USAGE IN MAIN AT BEGINNING: "
				+ formatter.format(megabytesBefore) + "MB");
		System.out.println("################################################");

		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(SpringConfiguration.class);
		InitialiseEconomy init = new InitialiseEconomy();
		cpvInputs = init.initialiseEconomy(ctx); // iteration 0
		AustralianEconomy economy = init.getEconomy();
		System.gc();

		long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		megabytesBefore = memoryBefore / 1024f / 1024f;
		float megabytesAfter = memoryAfter / 1024f / 1024f;
		System.out.println("################################################");
		System.out.println(new Date(System.currentTimeMillis()) + ": MEMORY USAGE IN MAIN AFTER MAKING CPV INPUTS: "
				+ formatter.format(megabytesAfter) + "MB");
		System.out.println("################################################");
		memoryBefore = memoryAfter;

		// send to Kryonet server to calculate CPV
		new KryonetClient();
		try {
			// wait for CPV listener to terminate before continuing with execution
			t.join();
		} catch (InterruptedException e) {
			// do nothing
		}
		cpvInputs.clear();
		cpvInputs = null;

		System.gc();
		memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		megabytesAfter = memoryAfter / 1024f / 1024f;
		System.out.println("################################################");
		System.out.println(new Date(System.currentTimeMillis()) + ": MEMORY USAGE IN MAIN AFTER CALCULATING CPV: "
				+ formatter.format(megabytesAfter) + "MB)");
		System.out.println("################################################");
		memoryBefore = memoryAfter;

		// update economy here
		// ideally loop through this logic a few times

		// TODO process CPV outputs
		// read agents back in from object file, then update financial statements
		// economy = (AustralianEconomy) readObjectFromFile(FILEPATH_AGENTS_INIT);
		// economy = readFstEconomyFromFile(FILEPATH_AGENTS_INIT_FST); // using FST
		// economy = readKryoObjectFromFile(FILEPATH_AGENTS_INIT_KRYO);

		economy.updateOneMonth(cpvOutputs);

		System.gc();
		memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		megabytesAfter = memoryAfter / 1024f / 1024f;
		System.out.println("################################################");
		System.out.println(new Date(System.currentTimeMillis())
				+ ": MEMORY USAGE IN MAIN AFTER UPDATING ECONOMY WITH CPV OUTPUTS: " + formatter.format(megabytesAfter)
				+ "MB)");
		System.out.println("################################################");
		memoryBefore = memoryAfter;

		// save summary to file
		int iteration = cpvOutputs.getIteration() + 1;
		economy.saveSummaryToFile(iteration);
		economy.saveDetailsToFile(iteration); // details after being updated with CPV output

		ctx.close();
	}

}
