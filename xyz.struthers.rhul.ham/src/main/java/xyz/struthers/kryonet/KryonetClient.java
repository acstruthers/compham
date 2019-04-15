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
public class KryonetClient {

	Client client;

	public KryonetClient() {
		super();
		Permit.godMode();

		// create client
		client = new Client();
		client.start();

		try {
			client.connect(KryonetNetwork.NETWORK_TIMEOUT_MILLIS, KryonetNetwork.IPADDRESS_SERVER,
					KryonetNetwork.NETWORK_PORT_TCP, KryonetNetwork.NETWORK_PORT_UDP);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// register classes
		KryonetNetwork.register(client);

		KryonetRequest request = new KryonetRequest();
		request.setText("Hello");
		client.sendTCP(request);
		
		client.addListener(new Listener() {
			public void connected(Connection connection) {
				System.out.println("client connected");
				client.sendTCP("client connected");
			}

			public void received(Connection connection, Object object) {
				System.out.println("Kryonet server responded.");
				if (object instanceof KryonetResponse) {
					KryonetResponse response = (KryonetResponse) object;
					System.out.println("Kryonet server response: " + response.getText());
					client.sendTCP("Hello to you too.");
					return;
				}
			}

			public void disconnected(Connection connection) {
				System.out.println("client disconnected");
				// System.exit(0);
			}
		});

		

		// We'll do the connect on a new thread so the ChatFrame can show a progress
		// bar.
		// Connecting to localhost is usually so fast you won't see the progress bar.
		/*
		 * new Thread("Connect") { public void run() { try {
		 * client.connect(KryonetNetwork.NETWORK_TIMEOUT_MILLIS,
		 * KryonetNetwork.IPADDRESS_SERVER, KryonetNetwork.NETWORK_PORT_TCP); // Server
		 * communication after connection can go here, or in // Listener#connected(). }
		 * catch (IOException ex) { ex.printStackTrace(); System.exit(1); } } }.start();
		 */
	}

	public static void main(String[] args) {
		new KryonetClient();
	}

}
