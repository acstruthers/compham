package xyz.struthers.rhul.ham;

import java.util.ArrayList;
import java.util.List;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.EndPoint;

import xyz.struthers.rhul.ham.process.ClearingPaymentInputs;
import xyz.struthers.rhul.ham.process.ClearingPaymentOutputs;

//This class is a convenient place to keep things common to both the client and server.
public class KryonetNetwork {

	// public static final String IPADDRESS_SERVER = "192.168.1.74";
	public static final String IPADDRESS_SERVER = "169.254.58.222";
	public static final int NETWORK_PORT_TCP = 1099; // 54555 default value
	public static final int NETWORK_PORT_UDP = 54777;
	public static final int NETWORK_TIMEOUT_MILLIS = 5000;

	public KryonetNetwork() {
		super();
	}

	// This registers objects that are going to be sent over the network.
	static public void register(EndPoint endPoint) {
		Kryo kryo = endPoint.getKryo();
		kryo.register(ClearingPaymentInputs.class);
		kryo.register(ClearingPaymentOutputs.class);
		kryo.register(List.class);
		kryo.register(ArrayList.class);
		kryo.register(Float.class);
		kryo.register(Integer.class);
		kryo.register(int.class);
	}
}
