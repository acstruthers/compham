package xyz.struthers.kryonet;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.EndPoint;

//This class is a convenient place to keep things common to both the client and server.
public class KryonetHelloNetwork {

	//public static final String IPADDRESS_SERVER = "192.168.1.74";
	public static final String IPADDRESS_SERVER = "169.254.58.222";
	public static final int NETWORK_PORT_TCP = 1099; // 54555 default value
	public static final int NETWORK_PORT_UDP = 54777;
	public static final int NETWORK_TIMEOUT_MILLIS = 5000;

	public KryonetHelloNetwork() {
		super();
	}

	// This registers objects that are going to be sent over the network.
	static public void register(EndPoint endPoint) {
		Kryo kryo = endPoint.getKryo();
		kryo.register(KryonetHelloResponse.class);
		kryo.register(KryonetHelloRequest.class);
	}

}
