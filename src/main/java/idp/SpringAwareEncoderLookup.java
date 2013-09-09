package idp;

import java.util.Map;

import org.opensaml.messaging.encoder.MessageEncoder;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class SpringAwareEncoderLookup implements ApplicationContextAware {

	private Map<String, String> lookupMap;
	
	private String beanNamePrefix;

	private ApplicationContext appContext;

	// Used with an explicit mapping strategy.
	public SpringAwareEncoderLookup(Map<String, String> map) {
		lookupMap = map;
	}
	
	// Used with an implicit mapping strategy, bean Name based on binding URI + a prefix.
	public SpringAwareEncoderLookup(String prefix) {
		beanNamePrefix = prefix;
	}

	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		appContext = applicationContext;
	}

	public MessageEncoder<?> get(String bindingURI) {
		if (lookupMap != null) {
			String beanName = lookupMap.get(bindingURI);
			if (beanName != null) {
				return appContext.getBean(beanName, MessageEncoder.class);
			} else {
				return null;
			}
		} else if (beanNamePrefix != null) {
			String beanName = beanNamePrefix + "." + bindingURI;
			return appContext.getBean(beanName, MessageEncoder.class);
		} else {
			return null;
		}
	}

}
