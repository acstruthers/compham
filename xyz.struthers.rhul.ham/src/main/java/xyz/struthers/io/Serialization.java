package xyz.struthers.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.zip.DeflaterInputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * A utility class to make it easier to serialize and deserialize objects.
 * 
 * @author Adam Struthers
 * @since 2019-05-15
 */
public abstract class Serialization {

	public static final int INITIAL_BUFFER_SIZE = 32; // bytes (will grow as necessary)
	public static final int MSG_TYPE_HDR = 0;
	public static final int MSG_TYPE_BODY = 1;
	public static final int MSG_TYPE_END = -1;

	public static final int COMPRESSION_NONE = 0;
	public static final int COMPRESSION_GZIP = 1;
	public static final int COMPRESSION_DEFLATER = 2;

	public static byte[] toBytes(Object object) {
		return toBytes(object, INITIAL_BUFFER_SIZE);
	}

	public static byte[] toBytes(Object object, int bufferSize) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream(bufferSize);
		try {
			ObjectOutputStream objectOut = new ObjectOutputStream(baos);
			objectOut.writeObject(object);
			objectOut.flush();
			objectOut.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return baos.toByteArray();
	}

	public static Object toObject(byte[] bytes) {
		Object object = null;
		try {
			ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
			ObjectInputStream objectIn = new ObjectInputStream(bais);
			object = objectIn.readObject();
			objectIn.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return object;
	}

	public static byte[] toBytesGZIP(Object object) {
		return toBytesGZIP(object, INITIAL_BUFFER_SIZE);
	}

	public static byte[] toBytesGZIP(Object object, int bufferSize) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream(bufferSize);
		try {
			GZIPOutputStream zipOut = new GZIPOutputStream(baos);
			ObjectOutputStream objectOut = new ObjectOutputStream(zipOut);
			objectOut.writeObject(object);
			objectOut.flush();
			objectOut.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return baos.toByteArray();
	}

	public static Object toObjectFromGZIP(byte[] bytes) {
		Object object = null;
		try {
			ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
			GZIPInputStream zipIn = new GZIPInputStream(bais);
			ObjectInputStream objectIn = new ObjectInputStream(zipIn);
			object = objectIn.readObject();
			objectIn.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return object;
	}

	public static byte[] toBytesDeflater(Object object) {
		return toBytesDeflater(object, INITIAL_BUFFER_SIZE);
	}

	public static byte[] toBytesDeflater(Object object, int bufferSize) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream(bufferSize);
		try {
			DeflaterOutputStream zipOut = new DeflaterOutputStream(baos);
			ObjectOutputStream objectOut = new ObjectOutputStream(zipOut);
			objectOut.writeObject(object);
			objectOut.flush();
			objectOut.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return baos.toByteArray();
	}

	public static Object toObjectFromDeflater(byte[] bytes) {
		Object object = null;
		try {
			ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
			DeflaterInputStream zipIn = new DeflaterInputStream(bais);
			ObjectInputStream objectIn = new ObjectInputStream(zipIn);
			object = objectIn.readObject();
			objectIn.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return object;
	}

	/**
	 * Writes a byte array to a data stream in a series of smaller messages.
	 * 
	 * @param dos
	 * @param bytes
	 * @param packetSize
	 * @throws IOException
	 */
	public static void writeToDataStream(DataOutputStream dos, byte[] bytes, int messageSize) throws IOException {
		// send header details first
		dos.writeInt(MSG_TYPE_HDR);
		dos.writeInt(bytes.length);
		dos.flush(); // don't know if I need this, but probably can't hurt

		// send message body
		if (messageSize >= bytes.length) {
			dos.writeInt(MSG_TYPE_BODY);
			dos.writeInt(bytes.length); // length of this message
			dos.write(bytes);
			dos.flush();
		} else {
			// loop over array, sending body as a series of messages
			int from = 0;
			int to = Math.min(bytes.length, messageSize);
			while (to - from > 0) {
				// send data
				byte[] msg = Arrays.copyOfRange(bytes, from, to);
				dos.writeInt(MSG_TYPE_BODY);
				dos.writeInt(to - from); // length of this message
				dos.write(msg);
				dos.flush();

				// set indices for next message
				from = to;
				to = Math.min(bytes.length, from + messageSize);
			}
		}

		// send end message last
		dos.writeInt(MSG_TYPE_END);
		dos.flush();
	}

	/**
	 * Sends an object over a data output stream using a given compression method,
	 * buffer size, and message size.
	 * 
	 * @param dos
	 * @param object
	 * @param bufferSize
	 * @param messageSize
	 * @throws IOException
	 */
	public static void writeToDataStream(DataOutputStream dos, Object object, int bufferSize, int messageSize,
			CompressionType compressionType) throws IOException {

		ByteArrayOutputStream baos = new ByteArrayOutputStream(bufferSize);
		try {
			ObjectOutputStream objectOut = null;
			switch (compressionType) {
			case GZIP:
				GZIPOutputStream zipOut = new GZIPOutputStream(baos);
				objectOut = new ObjectOutputStream(zipOut);
				break;
			case DEFLATER:
				DeflaterOutputStream deflaterOut = new DeflaterOutputStream(baos);
				objectOut = new ObjectOutputStream(deflaterOut);
				break;
			default:
				objectOut = new ObjectOutputStream(baos);
			}
			objectOut.writeObject(object);
			objectOut.flush();
			objectOut.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		byte[] bytes = baos.toByteArray();

		// send header details first
		dos.writeInt(MSG_TYPE_HDR);
		dos.writeInt(bytes.length);
		dos.writeInt(compressionType.intValue());
		dos.flush(); // don't know if I need this, but probably can't hurt

		// send message body
		if (messageSize >= bytes.length) {
			dos.writeInt(MSG_TYPE_BODY);
			dos.writeInt(bytes.length); // length of this message
			dos.write(bytes);
			dos.flush();
		} else {
			// loop over array, sending body as a series of messages
			int from = 0;
			int to = Math.min(bytes.length, messageSize);
			while (to - from > 0) {
				// send data
				byte[] msg = Arrays.copyOfRange(bytes, from, to);
				dos.writeInt(MSG_TYPE_BODY);
				dos.writeInt(to - from); // length of this message
				dos.write(msg);
				dos.flush();

				// set indices for next message
				from = to;
				to = Math.min(bytes.length, from + messageSize);
			}
		}

		// send end message last
		dos.writeInt(MSG_TYPE_END);
		dos.flush();
	}

	public static byte[] readDataFromStream(DataInputStream dis) throws IOException {
		// read header details first
		dis.readInt(); // reads the MSG_TYPE_HDR integer (and discards it)
		int totalLength = dis.readInt();
		byte[] bytes = new byte[totalLength];
		int i = 0;

		// read message body
		while (dis.readInt() != -1) {
			int msgLength = dis.readInt();
			byte[] msgBytes = new byte[msgLength];
			dis.readFully(msgBytes);

			// append msg to total byte array
			for (int j = 0; j < msgLength; j++) {
				bytes[i++] = msgBytes[j];
			}
		}
		// end message received

		return bytes;
	}

	public static Object readObjectFromStream(DataInputStream dis) throws IOException {
		// read header details first
		dis.readInt(); // reads the MSG_TYPE_HDR integer (and discards it)
		int totalLength = dis.readInt();
		int compTypeInt = dis.readInt();
		CompressionType compressionType = null;
		switch (compTypeInt) {
		case 1:
			compressionType = CompressionType.GZIP;
			break;
		case 2:
			compressionType = CompressionType.DEFLATER;
			break;
		default:
			compressionType = CompressionType.NO_COMPRESSION;
			break;
		}
		byte[] bytes = new byte[totalLength];
		int i = 0;

		// read message body
		while (dis.readInt() != -1) {
			int msgLength = dis.readInt();
			byte[] msgBytes = new byte[msgLength];
			dis.readFully(msgBytes);

			// append msg to total byte array
			for (int j = 0; j < msgLength; j++) {
				bytes[i++] = msgBytes[j];
			}
		}

		// end message received
		Object object = null;
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		try {
			ObjectInputStream objectIn = null;
			switch (compressionType) {
			case GZIP:
				GZIPInputStream zipIn = new GZIPInputStream(bais);
				objectIn = new ObjectInputStream(zipIn);
				break;
			case DEFLATER:
				DeflaterInputStream deflaterIn = new DeflaterInputStream(bais);
				objectIn = new ObjectInputStream(deflaterIn);
				break;
			default:
				objectIn = new ObjectInputStream(bais);
				break;
			}
			object = objectIn.readObject();
			objectIn.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return object;
	}

	public enum CompressionType {
		NO_COMPRESSION, GZIP, DEFLATER;

		public int intValue() {
			int value = 0;
			switch (this) {
			case GZIP:
				value = 1;
				break;
			case DEFLATER:
				value = 2;
				break;
			default:
				// no compression
				value = 0;
				break;
			}
			return value;
		}
	}
}
