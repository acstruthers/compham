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
	
	// Agent details
	protected String name;
	
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

		this.name = agent.name;
	}
	
	protected void init() {
		this.name = null;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the serialversionuid
	 */
	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	
}
