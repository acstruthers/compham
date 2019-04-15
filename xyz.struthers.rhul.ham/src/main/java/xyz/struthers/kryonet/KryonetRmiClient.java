package xyz.struthers.kryonet;

import java.io.IOException;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.rmi.ObjectSpace;

public class KryonetRmiClient {

	Client client;
	KryonetRmiHelloInterface serverHelloObject;

	public KryonetRmiClient() {
		client = new Client();
		client.start();

		// Register the classes that will be sent over the network.
		KryonetRmiNetwork.register(client);

		// Get the object on the other end of the connection.
		// This allows the client to call methods on the server.
		serverHelloObject = ObjectSpace.getRemoteObject(client, KryonetRmiNetwork.HELLO,
				KryonetRmiHelloInterface.class);

		try {
			client.connect(KryonetRmiNetwork.NETWORK_TIMEOUT_MILLIS, KryonetRmiNetwork.IPADDRESS_SERVER,
					KryonetRmiNetwork.NETWORK_PORT_TCP);
			// Server communication after connection can go here, or in
			// Listener#connected().
			serverHelloObject.sayHello("Hello");
		} catch (IOException ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}

	public static void main(String[] args) {
		new KryonetRmiClient();
	}
}
