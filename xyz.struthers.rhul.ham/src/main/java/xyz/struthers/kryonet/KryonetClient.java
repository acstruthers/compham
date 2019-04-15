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

	public KryonetClient() {
		super();
		Permit.godMode();
		
		// create client
		Client client = new Client();
		client.start();

		// register classes
		KryonetNetwork.register(client);

		client.addListener(new Listener() {
			public void connected(Connection connection) {
				System.out.println("client connected");
			}

			public void received(Connection connection, Object object) {
				if (object instanceof KryonetResponse) {
					KryonetResponse response = (KryonetResponse) object;
					System.out.println("Kryonet server response: " + response.getText());
					client.sendTCP("Hello to you too.");
				}
			}

			public void disconnected(Connection connection) {
				System.out.println("client disconnected");
				System.exit(0);
			}
		});

		try {
			client.connect(KryonetNetwork.NETWORK_TIMEOUT_MILLIS, KryonetNetwork.IPADDRESS_SERVER,
					KryonetNetwork.NETWORK_PORT_TCP, KryonetNetwork.NETWORK_PORT_UDP);
		} catch (IOException e) {
			e.printStackTrace();
		}

		KryonetRequest request = new KryonetRequest();
		request.setText("Hello");
		client.sendTCP(request);

	}

	public static void main(String[] args) {
		new KryonetClient();
	}

}
