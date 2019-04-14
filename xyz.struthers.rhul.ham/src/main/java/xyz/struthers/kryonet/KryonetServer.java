/**
 * 
 */
package xyz.struthers.kryonet;

import java.io.IOException;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

/**
 * @author acstr
 *
 */
public class KryonetServer {

	public static final String IPADDRESS_SERVER = "192.168.0.17";

	public KryonetServer() {
		super();
	}

	public static void kryonetServer(String[] args) {
		Server server = new Server();
		server.start();
		try {
			server.bind(54555, 54777);
		} catch (IOException e) {
			e.printStackTrace();
		}

		Kryo kryo = server.getKryo();
		kryo.register(KryonetRequest.class);
		kryo.register(KryonetResponse.class);

		server.addListener(new Listener() {
			public void received(Connection connection, Object object) {
				if (object instanceof KryonetRequest) {
					KryonetRequest request = (KryonetRequest) object;
					System.out.println(request.getText());

					KryonetResponse response = new KryonetResponse(request.getText());
					response.setText(request.getText() + ", Kryonet world!");
					System.out.println(response.getText());
					connection.sendTCP(response);
				}
			}
		});
	}

}
