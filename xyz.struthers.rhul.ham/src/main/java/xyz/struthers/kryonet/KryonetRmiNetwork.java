package xyz.struthers.kryonet;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.EndPoint;
import com.esotericsoftware.kryonet.rmi.ObjectSpace;

public class KryonetRmiNetwork {

	// Kryonet networking parameters
	public static final String IPADDRESS_SERVER = "192.168.1.74";
	public static final int NETWORK_PORT_TCP = 1099; // 54555 default value
	public static final int NETWORK_PORT_UDP = 54777;
	public static final int NETWORK_TIMEOUT_MILLIS = 5000;
	public static final int NETWORK_BUFFER_BYTES = 2000000000; // approx 2GB

	// These IDs are used to register objects in ObjectSpaces.
	static public final short HELLO = 1;
	
	public KryonetRmiNetwork() {
		super();
	}

	// This registers objects that are going to be sent over the network.
	static public void register(EndPoint endPoint) {
		Kryo kryo = endPoint.getKryo();

		// This must be called in order to use ObjectSpaces.
		ObjectSpace.registerClasses(kryo);

		// The interfaces that will be used as remote objects must be registered.
		kryo.register(KryonetRmiHelloInterface.class);

		// The classes of all method parameters and return values
		// for remote objects must also be registered.
		kryo.register(String.class);
	}

}
