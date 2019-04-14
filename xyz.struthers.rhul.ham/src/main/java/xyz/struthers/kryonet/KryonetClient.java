/**
 * 
 */
package xyz.struthers.kryonet;

import java.io.IOException;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

/**
 * @author acstr
 *
 */
public class KryonetClient {

	public static final String IPADDRESS_SERVER = "192.168.0.17";

	public KryonetClient() {
		super();
	}

	public static void kryonetClient(String[] args) {
		Client client = new Client();
		client.start();
		try {
			client.connect(5000, IPADDRESS_SERVER, 54555, 54777);
		} catch (IOException e) {
			e.printStackTrace();
		}

		Kryo kryoClient = client.getKryo();
		kryoClient.register(KryonetRequest.class);
		kryoClient.register(KryonetResponse.class);

		KryonetRequest request = new KryonetRequest();
		client.sendTCP(request);

		client.addListener(new Listener() {
			public void received(Connection connection, Object object) {
				if (object instanceof KryonetResponse) {
					KryonetResponse response = (KryonetResponse) object;
					System.out.println("Kryonet server response: " + response.getText());
				}
			}
		});

	}

}
