package xyz.struthers.rhul.ham.config;

/**
 * @author Adam Struthers
 * @since 22-Jul-2019
 */
public class PropertiesXmlFactory {

	/**
	 * Store PropertiesXml filename in a static field variable so it can be set by
	 * the main executable and accessed by the factory.
	 */
	public static String propertiesXmlFilename;

	private static PropertiesXml props;

	private PropertiesXmlFactory() {
		super();
	}

	public static PropertiesXml getProperties() {
		if (props == null) {
			System.out.println("Loading properties file: " + propertiesXmlFilename);
			props = PropertiesXmlHandler.readPropertiesFromXmlFile(propertiesXmlFilename);
		}
		return props;
	}
}
