/**
 * 
 */
package xyz.struthers.rhul.ham.agent;

import java.io.Serializable;

import xyz.struthers.rhul.ham.process.Clearable;

/**
 * A superclass for all agents in the model.
 * 
 * @author Adam Struthers
 * @since 02-Feb-2019
 */
public abstract class Agent implements Serializable, Clearable {

	private static final long serialVersionUID = 1L;

	/**
	 * Default constructor
	 */
	public Agent() {
		super();
		this.init();
	}

	/**
	 * Copy constructor
	 * 
	 * @param agent
	 */
	public Agent(Agent agent) {
		super();
		this.init();
	}

	protected void init() {
		// do nothing
	}

	/**
	 * @return the serialversionuid
	 */
	public static long getSerialversionuid() {
		return serialVersionUID;
	}

}
