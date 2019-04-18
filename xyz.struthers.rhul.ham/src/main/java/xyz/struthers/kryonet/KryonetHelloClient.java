/**
 * 
 */
package xyz.struthers.kryonet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.nqzero.permit.Permit;

import xyz.struthers.rhul.ham.config.Properties;
import xyz.struthers.rhul.ham.process.ClearingPaymentInputs;
import xyz.struthers.rhul.ham.process.ClearingPaymentOutputs;

/**
 * @author acstr
 *
 */
public class KryonetHelloClient {

	static Client client;
	static Thread t;
	static String msg = "";

	public KryonetHelloClient(String message) {
		super();
		Permit.godMode(); // allows reflection in Java 11

		// create client
		client = new Client(Properties.NETWORK_BUFFER_BYTES, Properties.NETWORK_BUFFER_BYTES);
		// client.start();
		t = new Thread(client);
		t.start();
		// https://stackoverflow.com/questions/17011178/java-kryonet-servers-client-not-receiving-server-response

		try {
			client.connect(KryonetHelloNetwork.NETWORK_TIMEOUT_MILLIS, KryonetHelloNetwork.IPADDRESS_SERVER,
					KryonetHelloNetwork.NETWORK_PORT_TCP, KryonetHelloNetwork.NETWORK_PORT_UDP);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// register classes
		KryonetHelloNetwork.register(client);

		client.addListener(new Listener() {
			public void connected(Connection connection) {
				System.out.println("client connected");
				client.sendTCP("client connected");
			}

			public void received(Connection connection, Object object) {
				System.out.println("Kryonet server responded.");
				if (object instanceof KryonetHelloResponse) {
					KryonetHelloResponse response = (KryonetHelloResponse) object;
					System.out.println("Kryonet server response: " + response.getText());
					client.sendTCP("Hello to you too.");
					msg = "I was set inside the listener";

					// return control to the main thread
					client.removeListener(this);
					client.stop();
					return;
				} else if (object instanceof KryonetHelloResponse) {
					System.out.println("CPV outputs received at " + new Date(System.currentTimeMillis()) + ".");
					ClearingPaymentOutputs cpvOutputs = (ClearingPaymentOutputs) object;
					System.out.println("CPV outputs unmarshalled at " + new Date(System.currentTimeMillis()) + ".");

					// return control to the main thread
					client.removeListener(this);
					client.stop();
					return;
				}
			}

			public void disconnected(Connection connection) {
				System.out.println("client disconnected");
				// System.exit(0);
			}
		});

		/*
		 * KryonetHelloRequest request = new KryonetHelloRequest();
		 * request.setText(message); client.sendTCP(request);
		 * System.out.println("request sent");
		 */

		ClearingPaymentInputs cpvInputs = new ClearingPaymentInputs();
		final int numAgents = 12500000;
		final int numLinks = 20;
		List<List<Float>> liabilitiesAmounts = new ArrayList<List<Float>>(numAgents);
		List<List<Integer>> liabilitiesIndices = new ArrayList<List<Integer>>(numAgents);
		List<Float> operatingCashFlow = new ArrayList<Float>(numAgents);
		List<Float> liquidAssets = new ArrayList<Float>(numAgents);
		for (int i = 0; i < numAgents; i++) {
			liabilitiesAmounts.add(new ArrayList<Float>(numLinks));
			liabilitiesIndices.add(new ArrayList<Integer>(numLinks));
			for (int j = 0; j < numLinks; j++) {
				liabilitiesAmounts.get(i).add(0f);
				liabilitiesIndices.get(i).add(i);
			}
			operatingCashFlow.add(0f);
			liquidAssets.add(0f);
		}
		int iteration = 0;
		System.out.println(new Date(System.currentTimeMillis())
				+ ": CPV inputs calculated and being sent to Kryonet server.");
		client.sendTCP(cpvInputs);
	}

	public static void main(String[] args) {
		// https://www.codejava.net/java-core/concurrency/how-to-use-threads-in-java-create-start-pause-interrupt-and-join

		System.out.println("* * * MAIN START * * *");
		new KryonetHelloClient("Hello");
		try {
			// wait for previous thread to terminate before continuing with execution
			t.join();
		} catch (InterruptedException e) {
			// do nothing
		}
		System.out.println(msg);
		System.out.println("MAIN before second client creation.");
		new KryonetHelloClient("Goodbye");
		try {
			// wait for previous thread to terminate before continuing with execution
			t.join();
		} catch (InterruptedException e) {
			// do nothing
		}
		System.out.println("* * * MAIN FINISH * * *");
	}

}
