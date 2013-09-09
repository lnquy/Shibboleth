package common;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.core.config.ConfigurationService;
import org.opensaml.core.config.InitializationException;
import org.opensaml.core.config.InitializationService;
import org.opensaml.core.xml.config.XMLObjectProviderRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.utilities.java.support.component.AbstractInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.xml.ParserPool;

/**
 * A simple bean that may be used with Spring to initialize the OpenSAML library.
 */
public class OpensamlConfigBean extends AbstractInitializableComponent {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(OpensamlConfigBean.class);
    
    /** Optional ParserPool to configure. */
    private ParserPool parserPool;
    
    /**
     * Get the global ParserPool to configure.
     * 
     * @return Returns the parserPool.
     */
    @Nullable public ParserPool getParserPool() {
        return parserPool;
    }

    /**
     * Set the global ParserPool to configure.
     * 
     * @param newParserPool The parserPool to set.
     */
    public void setParserPool(@Nonnull final ParserPool newParserPool) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        parserPool = Constraint.isNotNull(newParserPool, "ParserPool cannot be null");
    }
    
    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {

        // Initialize OpenSAML.
        try {
            InitializationService.initialize();
        } catch (InitializationException e) {
            throw new ComponentInitializationException("Exception initializing OpenSAML", e);
        }
        
        XMLObjectProviderRegistry registry = null;
        synchronized(ConfigurationService.class) {
            registry = ConfigurationService.get(XMLObjectProviderRegistry.class);
            if (registry == null) {
                log.debug("XMLObjectProviderRegistry did not exist in ConfigurationService, will be created");
                registry = new XMLObjectProviderRegistry();
                ConfigurationService.register(XMLObjectProviderRegistry.class, registry);
            }
        }
        
        registry.setParserPool(parserPool);
    }
}