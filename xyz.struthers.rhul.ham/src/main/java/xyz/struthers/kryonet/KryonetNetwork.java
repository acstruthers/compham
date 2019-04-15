package xyz.struthers.kryonet;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.EndPoint;

import xyz.struthers.kryonet.Network.AddCharacter;
import xyz.struthers.kryonet.Network.MoveCharacter;
import xyz.struthers.kryonet.Network.Register;
import xyz.struthers.kryonet.Network.RemoveCharacter;
import xyz.struthers.kryonet.Network.UpdateCharacter;

//This class is a convenient place to keep things common to both the client and server.
public class KryonetNetwork {

	public static final String IPADDRESS_SERVER = "192.168.0.17";
	public static final int NETWORK_PORT_TCP = 54555;
	public static final int NETWORK_PORT_UDP = 54777;
	public static final int NETWORK_TIMEOUT_MILLIS = 5000;

	public KryonetNetwork() {
		super();
	}

	// This registers objects that are going to be sent over the network.
	static public void register(EndPoint endPoint) {
		Kryo kryo = endPoint.getKryo();
		kryo.register(KryonetResponse.class);
		kryo.register(KryonetRequest.class);
		kryo.register(Register.class);
		kryo.register(AddCharacter.class);
		kryo.register(UpdateCharacter.class);
		kryo.register(RemoveCharacter.class);
		kryo.register(Character.class);
		kryo.register(MoveCharacter.class);
	}

}
