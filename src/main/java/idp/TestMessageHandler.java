package idp;

import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.handler.AbstractMessageHandler;
import org.opensaml.messaging.handler.MessageHandlerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestMessageHandler extends AbstractMessageHandler {
	
	private Logger log = LoggerFactory.getLogger(TestMessageHandler.class);

	protected void doInitialize() throws ComponentInitializationException {
		super.doInitialize();
		log.debug("MessageHandler instance initialized: {}", this.toString());
	}

	protected void doInvoke(MessageContext messageContext) throws MessageHandlerException {
		log.debug("MessageHandler instance invoked: {}", this.toString());
		log.debug("Saw message of type: {}", messageContext.getMessage().getClass().getName());
	}

}
