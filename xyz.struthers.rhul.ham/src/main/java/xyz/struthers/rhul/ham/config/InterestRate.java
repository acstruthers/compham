package xyz.struthers.rhul.ham.config;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

/**
 * @author Adam Struthers
 * @since 20-Jul-2019
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class InterestRate implements Serializable {

	private static final long serialVersionUID = 1L;

	private float interestRate;

	public InterestRate() {
		super();
		interestRate = 0f;
	}

	public InterestRate(float rate) {
		super();
		interestRate = rate;
	}

	/**
	 * @return the interestRate
	 */
	public float getInterestRate() {
		return interestRate;
	}

	/**
	 * @param interestRate the interestRate to set
	 */
	public void setInterestRate(float interestRate) {
		this.interestRate = interestRate;
	}

}
