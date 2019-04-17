/**
 * 
 */
package xyz.struthers.rhul.ham;

import java.io.IOException;
import java.util.Date;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.nqzero.permit.Permit;

import xyz.struthers.kryonet.KryonetHelloRequest;
import xyz.struthers.rhul.ham.process.ClearingPaymentOutputs;

/**
 * @author acstr
 *
 */
public class KryonetClient {

	Client client;

	/**
	 * 
	 */
	public KryonetClient() {
		super();
		Permit.godMode(); // allows reflection in Java 11

		// create client
		client = new Client();
		new Thread(client).start();
		// https://stackoverflow.com/questions/17011178/java-kryonet-servers-client-not-receiving-server-response

		try {
			client.connect(KryonetNetwork.NETWORK_TIMEOUT_MILLIS, KryonetNetwork.IPADDRESS_SERVER,
					KryonetNetwork.NETWORK_PORT_TCP, KryonetNetwork.NETWORK_PORT_UDP);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// register classes
		KryonetNetwork.register(client);

		client.addListener(new Listener() {
			public void connected(Connection connection) {
				System.out.println("Kryonet CPV client connected.");
			}

			public void received(Connection connection, Object object) {
				if (object instanceof ClearingPaymentOutputs) {
					System.out.println("CPV outputs received at " + new Date(System.currentTimeMillis()) + ".");
					System.out.println("Updating agents in the economy now.");
					ClearingPaymentOutputs cpvOutputs = (ClearingPaymentOutputs) object;
					// FIXME: do something with Kryonet CPV outupts
					return;
				}
			}

			public void disconnected(Connection connection) {
				System.out.println("Kryonet CPV client disconnected.");
				// System.exit(0);
			}
		});

		KryonetHelloRequest request = new KryonetHelloRequest();
		request.setText("Hello");
		client.sendTCP(request);
		System.out.println("request sent");
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// build economy and CPV inputs here
		new KryonetClient(); // send to lappy to calc CPV
		// update economy here
		// ideally loop through this logic a few times
	}

}
