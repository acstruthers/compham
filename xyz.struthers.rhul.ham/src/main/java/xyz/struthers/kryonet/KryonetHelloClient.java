/**
 * 
 */
package xyz.struthers.kryonet;

import java.io.IOException;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.nqzero.permit.Permit;

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
		client = new Client();
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
				}
			}

			public void disconnected(Connection connection) {
				System.out.println("client disconnected");
				// System.exit(0);
			}
		});

		KryonetHelloRequest request = new KryonetHelloRequest();
		request.setText(message);
		client.sendTCP(request);
		System.out.println("request sent");
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
