/**
 * 
 */
package xyz.struthers.rhul.ham.process;

import java.util.List;

import xyz.struthers.rhul.ham.agent.Individual;

/**
 * Any agent that employs people will implement this interface.
 * 
 * @author Adam Struthers
 * @since 2019-03-19
 */
public interface Employer {

	/**
	 * Gets a list of the employer's employees.
	 * 
	 * @return a List<Individual> of the employees.
	 */
	public List<Individual> getEmployees();

	/**
	 * Adds a single Individual as an employee of the organisation.
	 * 
	 * @param employee
	 */
	public void addEmployee(Individual employee);
}
