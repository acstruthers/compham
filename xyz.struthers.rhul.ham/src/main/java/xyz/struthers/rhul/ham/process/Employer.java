/**
 * 
 */
package xyz.struthers.rhul.ham.process;

import java.util.ArrayList;

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
	public ArrayList<Individual> getEmployees();

	/**
	 * Adds a single Individual as an employee of the organisation.
	 * 
	 * @param employee
	 */
	public void addEmployee(Individual employee);

	/**
	 * Gets the wages expense that was set during the Agent calibration process.
	 * 
	 * @return wages expense, per calibration data
	 */
	public float getInitialWagesExpense();

	/**
	 * Gets the actual wages expense according to the linked employees' salaries and
	 * wages incomes.
	 * 
	 * @return wages expense, per linked employees' incomes
	 */
	public float getActualWagesExpense();
	
	public char getIndustryDivisionCode();
	
	public void setIndustryDivisionCode(char industryDivisionCode);
}
