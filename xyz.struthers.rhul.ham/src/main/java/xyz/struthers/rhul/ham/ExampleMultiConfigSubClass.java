package xyz.struthers.rhul.ham;

import xyz.struthers.rhul.ham.config.PropertiesXml;
import xyz.struthers.rhul.ham.config.PropertiesXmlFactory;

public class ExampleMultiConfigSubClass {

	public ExampleMultiConfigSubClass() {
		super();
	}

	public static void main(String[] args) {
		PropertiesXmlFactory.propertiesXmlFilename = args[0];
		PropertiesXml props = PropertiesXmlFactory.getProperties();
		System.out.println(props.getScenarioName());
	}
	
}
