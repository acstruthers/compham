/**
 * 
 */
package xyz.struthers.rhul.ham.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @author Adam Struthers
 * @since 28-Jan-2019
 */
@Configuration
@ComponentScan(basePackages = { "xyz.struthers.rhul.ham" })
public class SpringConfiguration {
	// https://dzone.com/articles/spring-component-scan

}
