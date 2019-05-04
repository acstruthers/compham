package xyz.struthers.kryonet;

import java.io.IOException;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.rmi.ObjectSpace;
import com.nqzero.permit.Permit;

public class KryonetRmiClient {

	Client kryonetClient;
	KryonetRmiHelloInterface serverHelloObject;

	public KryonetRmiClient() {
		Permit.godMode();
		
		kryonetClient = new Client();
		kryonetClient.start();

		// Register the classes that will be sent over the network.
		KryonetRmiNetwork.register(kryonetClient);

		// Get the object on the other end of the connection.
		// This allows the client to call methods on the server.
		serverHelloObject = ObjectSpace.getRemoteObject(kryonetClient, KryonetRmiNetwork.HELLO,
				KryonetRmiHelloInterface.class);

		try {
			kryonetClient.connect(KryonetRmiNetwork.NETWORK_TIMEOUT_MILLIS, KryonetRmiNetwork.IPADDRESS_SERVER,
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
