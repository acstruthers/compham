package xyz.struthers.rhul.ham.config;

import java.io.File;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

/**
 * @author Adam Struthers
 * @since 20-Jul-2019
 */
public class PropertiesXmlHandler {

	private PropertiesXmlHandler() {
		super();
	}

	public static PropertiesXml readPropertiesFromXmlFile(String inFilename) {
		JAXBContext jc = null;
		PropertiesXml propertiesXml = null;
		try {
			jc = JAXBContext.newInstance(PropertiesXml.class);
			Unmarshaller unmarshaller = jc.createUnmarshaller();
			File xml = new File(inFilename); // "src/forum13159089/input.xml"
			propertiesXml = (PropertiesXml) unmarshaller.unmarshal(xml);
		} catch (JAXBException e) {
			e.printStackTrace();
		}
		return propertiesXml;
	}

	public static void writePropertiesToXmlFile(PropertiesXml propertiesXml, String outFilename) {
		JAXBContext jc = null;
		try {
			jc = JAXBContext.newInstance(PropertiesXml.class);
			Marshaller marshaller = jc.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.marshal(propertiesXml, new File(outFilename)); // "c:/temp/employees.xml"
		} catch (JAXBException e) {
			e.printStackTrace();
		}
	}
}
