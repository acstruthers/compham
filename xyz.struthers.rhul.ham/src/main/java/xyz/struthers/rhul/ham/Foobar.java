package xyz.struthers.rhul.ham;

import java.io.InputStream;

/**
 * Tests how to use the resource stream
 * @author acstr
 *
 */
public class Foobar {

	public Foobar() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String foo = "Total Australia";
		System.out.println("==>" + foo.substring(6) + ".");

		InputStream in = Foobar.class.getClassLoader()
				.getResourceAsStream("data/ABS/1270.0.55.001_AbsMeshblock/MB_2016_ACT.csv");
		System.out.println(in != null);
		in = Foobar.class
				.getResourceAsStream("/data/ABS/1270.0.55.001_AbsMeshblock/MB_2016_ACT.csv");
		System.out.println(in != null);
		System.out.println("done");
	}

}
