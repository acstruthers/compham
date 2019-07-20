package xyz.struthers.rhul.ham.config;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

/**
 * @author Adam Struthers
 * @since 20-Jul-2019
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class SaveIterationSummary implements Serializable {

	private static final long serialVersionUID = 1L;

	private boolean saveFlag;

	public SaveIterationSummary() {
		super();
		saveFlag = false;
	}

	public SaveIterationSummary(boolean flag) {
		super();
		saveFlag = flag;
	}

	/**
	 * @return the saveFlag
	 */
	public boolean isSaveFlag() {
		return saveFlag;
	}

	/**
	 * @param saveFlag the saveFlag to set
	 */
	public void setSaveFlag(boolean saveFlag) {
		this.saveFlag = saveFlag;
	}
}
