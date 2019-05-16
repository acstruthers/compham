package xyz.struthers.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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

	public static byte[] toBytes(Object object) {
		return toBytes(object, INITIAL_BUFFER_SIZE);
	}

	public static byte[] toBytes(Object object, int bufferSize) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream(bufferSize);
		try {
			// GZIPOutputStream zipOut = new GZIPOutputStream(baos);
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
}
