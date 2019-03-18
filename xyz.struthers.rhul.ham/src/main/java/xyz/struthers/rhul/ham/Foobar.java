package xyz.struthers.rhul.ham;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections.ListUtils;

/**
 * Tests how to use the resource stream
 * 
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

		Double a = 5d;
		Double b = a;
		b = 7d;
		System.out.println("a: " + a);
		System.out.println("b: " + b);
		List<Double> x = new ArrayList<Double>(3);
		x.add(1d);
		x.add(2d);
		x.add(Double.valueOf(3));
		List<Double> y = new ArrayList<Double>(x);
		y.set(2, Double.valueOf(9));
		System.out.println("x.get(2): " + x.get(2));
		System.out.println("y.get(2): " + y.get(2));
		List<Integer> changeless = new ArrayList<Integer>(Collections.nCopies(5, 1));
		System.out.println("changeless.get(3): " + changeless.get(3));
		changeless.set(3, 3);
		System.out.println("changeless.get(3): " + changeless.get(3));

		InputStream in = Foobar.class.getClassLoader()
				.getResourceAsStream("data/ABS/1270.0.55.001_AbsMeshblock/MB_2016_ACT.csv");
		System.out.println(in != null);
		in = Foobar.class.getResourceAsStream("/data/ABS/1270.0.55.001_AbsMeshblock/MB_2016_ACT.csv");
		System.out.println(in != null);
		System.out.println("done");

		int[] myArray = new int[] { 1, 2, 3 };
		myArray[1] *= 5;
		System.out.println("myArray[0]: " + myArray[0]);
		System.out.println("myArray[1]: " + myArray[1]);
		System.out.println("myArray[2]: " + myArray[2]);

		List<Integer> listOne = new ArrayList<Integer>(3);
		listOne.add(1);
		listOne.add(2);
		listOne.add(3);
		List<Integer> listTwo = new ArrayList<Integer>(3);
		listTwo.add(1);
		listTwo.add(2);
		listTwo.add(3);
		List<Integer> listThree = ListUtils.union(listOne, listTwo);
		System.out.println("listThree: " + listThree);
	}

}
