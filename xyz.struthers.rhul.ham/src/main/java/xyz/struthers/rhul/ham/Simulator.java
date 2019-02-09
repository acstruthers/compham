/**
 * 
 */
package xyz.struthers.rhul.ham;

import ec.util.MersenneTwisterFast;
import sim.engine.Schedule;
import sim.engine.SimState;

/**
 * @author Adam Struthers
 * @since 03-Feb-2019
 */
public class Simulator extends SimState {

	private static final long serialVersionUID = 1L;

	/**
	 * @param seed
	 */
	public Simulator(long seed) {
		super(seed);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param random
	 */
	public Simulator(MersenneTwisterFast random) {
		super(random);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param random
	 * @param schedule
	 */
	public Simulator(MersenneTwisterFast random, Schedule schedule) {
		super(random, schedule);
		// TODO Auto-generated constructor stub
	}

}
