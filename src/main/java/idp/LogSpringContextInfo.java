package idp;

import javax.annotation.Nonnull;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.ActionSupport;

import org.opensaml.profile.ProfileException;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

public class LogSpringContextInfo extends AbstractProfileAction implements ApplicationContextAware {
	
	private Logger log = LoggerFactory.getLogger(LogSpringContextInfo.class);
	
	private ApplicationContext context;
	

	@Nonnull
	protected Event doExecute(RequestContext springRequestContext, ProfileRequestContext profileRequestContext) throws ProfileException {
		log.debug("***************************************************************************************************");
		
		ApplicationContext current = context;
		while (current != null) {
			log.debug("Context: {}", current.toString());
			log.debug("Context Name: {}", current.getApplicationName());
			log.debug("Context Parent: {}", current.getParent());
			log.debug("");
			log.debug("Bean Definition Count: {}", current.getBeanDefinitionCount());
			log.debug("");
			log.debug("Bean Details:");
			log.debug("");
			
			for (String beanName : current.getBeanDefinitionNames()) {
				log.debug(String.format("Bean id: %s, singleton?: %s, prototype?: %s, type: %s",
						beanName, current.isSingleton(beanName), current.isPrototype(beanName), 
						current.getType(beanName).getName()));
			}
			
			log.debug("***************************************************************************************************");
			current = current.getParent();
		}
		
		return ActionSupport.buildProceedEvent(this);
	}

	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		context = applicationContext;
	}

}
