package xyz.struthers.rhul.ham;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections.ListUtils;

import xyz.struthers.rhul.ham.process.Tax;

/**
 * Tests how to use the resource stream
 * 
 * @author acstr
 *
 */
public class Foobar {

	public Foobar() {
		super();
	}

	public static void main(String[] args) {
		float income = 276.87f;
		float tax = Tax.calculateIndividualIncomeTax(income);
		System.out.println("Tax on " + income + " is " + tax);
		
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
		@SuppressWarnings("unchecked")
		List<Integer> listThree = ListUtils.union(listOne, listTwo);
		System.out.println("listThree: " + listThree);

		// float might be big enough for this model? ...and would halve memory footprint
		System.out.println("Double.MIN_VALUE: " + Double.MIN_VALUE);
		System.out.println("Double.MAX_VALUE: " + Double.MAX_VALUE);
		System.out.println("Double.SIZE: " + Double.SIZE);
		System.out.println("Float.MIN_VALUE: " + Float.MIN_VALUE);
		System.out.println("Float.MAX_VALUE: " + Float.MAX_VALUE);
		System.out.println("Float.SIZE: " + Float.SIZE);
		System.out.println("Integer.MIN_VALUE: " + Integer.MIN_VALUE);
		System.out.println("Integer.MAX_VALUE: " + Integer.MAX_VALUE);
		System.out.println("Integer.SIZE: " + Integer.SIZE);

		List<Integer> list = new ArrayList<Integer>(3);
		Integer int1 = 1;
		list.add(int1);
		list.add(Integer.valueOf(2));
		list.add(Integer.valueOf(3));
		Integer int2 = list.get(1);
		list.set(0, 7);
		list.clear();
		list = null;
		System.out.println("int1: " + int1);
		System.out.println("int2: " + int2);
	}
}
