package idp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

public class LoggingBeanPostProcessor implements BeanPostProcessor {
	
	private Logger log = LoggerFactory.getLogger(LoggingBeanPostProcessor.class);

	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		log.debug("Before Init: Saw bean '{}' of type '{}'", beanName, bean.getClass().getName());
		return bean;
	}

	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		log.debug("After Init: Saw bean '{}' of type '{}'", beanName, bean.getClass().getName());
		return bean;
	}

}
