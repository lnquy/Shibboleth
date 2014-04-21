/*
 * Licensed to the University Corporation for Advanced Internet Development, 
 * Inc. (UCAID) under one or more contributor license agreements.  See the 
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache 
 * License, Version 2.0 (the "License"); you may not use this file except in 
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package idp;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.net.HttpServletRequestResponseContext;
import net.shibboleth.utilities.java.support.xml.ParserPool;

import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.MarshallerFactory;
import org.opensaml.core.xml.io.UnmarshallerFactory;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.webflow.execution.FlowExecutionOutcome;
import org.springframework.webflow.executor.FlowExecutionResult;
import org.springframework.webflow.executor.FlowExecutor;
import org.springframework.webflow.test.MockExternalContext;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;

import com.unboundid.ldap.sdk.LDAPException;

import common.InMemoryDirectory;
import common.PathPropertySupport;

/**
 * Abstract flow test.
 */
@ActiveProfiles("dev")
@ContextConfiguration({"/system/conf/global-system.xml", "/conf/global-user.xml", "/system/conf/mvc-beans.xml",
        "/conf/webflow-config.xml"})
@WebAppConfiguration
public abstract class AbstractFlowTest extends AbstractTestNGSpringContextTests {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AbstractFlowTest.class);

    /** Path to LDIF file to be imported into directory server. */
    @Nonnull public final static String LDIF_FILE = "src/main/resources/test/ldap.ldif";

    /** Name of flow output attribute containing the profile request context. */
    @Nonnull public final static String OUTPUT_ATTR_NAME = "ProfileRequestContext";

    /** The IDP entity ID. */
    @Nonnull public final static String IDP_ENTITY_ID = "https://idp.example.org";
    
    /** The SP entity ID. */
    @Nonnull public final static String SP_ENTITY_ID = "https://sp.example.org";

    /** The SP ACS URL. */
    @Nonnull public final static String SP_ACS_URL = "https://sp.example.org/SAML1/POST/ACS";

    /** The SP relay state. */
    @Nonnull public final static String SP_RELAY_STATE = "myRelayState";

    /** In-memory directory server. */
    @NonnullAfterInit protected InMemoryDirectory directoryServer;

    /** Mock external context. */
    @Nonnull protected MockExternalContext externalContext;

    /** The web flow executor. */
    @Nonnull protected FlowExecutor flowExecutor;

    /** Mock request. */
    @Nonnull protected MockHttpServletRequest request;

    /** Mock response. */
    @Nonnull protected MockHttpServletResponse response;

    /** Parser pool */
    @NonnullAfterInit protected static ParserPool parserPool;

    /** XMLObject builder factory */
    @NonnullAfterInit protected static XMLObjectBuilderFactory builderFactory;

    /** XMLObject marshaller factory */
    @NonnullAfterInit protected static MarshallerFactory marshallerFactory;

    /** XMLObject unmarshaller factory */
    @NonnullAfterInit protected static UnmarshallerFactory unmarshallerFactory;

    static {
        PathPropertySupport.setupIdPHomeProperty();
        PathPropertySupport.setupAppHomeProperty();
    }

    /**
     * {@link HttpServletRequestResponseContext#clearCurrent()}
     */
    @AfterMethod public void clearThreadLocals() {
        HttpServletRequestResponseContext.clearCurrent();
    }

    /**
     * Initialize the web flow executor.
     */
    @BeforeMethod public void initializeFlowExecutor() {
        flowExecutor = applicationContext.getBean("flowExecutor", FlowExecutor.class);
        Assert.assertNotNull(flowExecutor);
    }

    /**
     * Initialize mock request, response, and external context.
     */
    @BeforeMethod public void initializeMocks() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        externalContext = new MockExternalContext();
        externalContext.setNativeRequest(request);
        externalContext.setNativeResponse(response);
    }

    /**
     * {@link HttpServletRequestResponseContext#loadCurrent(HttpServletRequest, HttpServletResponse)}
     */
    @BeforeMethod public void initializeThreadLocals() {
        HttpServletRequestResponseContext.loadCurrent((HttpServletRequest) request, (HttpServletResponse) response);
    }

    /**
     * Initialize XMLObject support classes.
     */
    @BeforeClass public void initializeXMLObjectSupport() {
        parserPool = XMLObjectProviderRegistrySupport.getParserPool();
        builderFactory = XMLObjectProviderRegistrySupport.getBuilderFactory();
        marshallerFactory = XMLObjectProviderRegistrySupport.getMarshallerFactory();
        unmarshallerFactory = XMLObjectProviderRegistrySupport.getUnmarshallerFactory();
    }

    /**
     * Creates an UnboundID in-memory directory server. Leverages LDIF found at {@value #LDIF_FILE}.
     * 
     * @throws LDAPException if the in-memory directory server cannot be created
     */
    @BeforeTest public void setupDirectoryServer() throws LDAPException {
        directoryServer = new InMemoryDirectory(LDIF_FILE);
        directoryServer.start();
    }

    /**
     * Shutdown the in-memory directory server.
     * 
     * Always run this method to avoid starting the server multiple times when tests fail.
     */
    @AfterTest(alwaysRun=true) public void teardownDirectoryServer() {
        if (directoryServer != null) {
            directoryServer.stop();
        }
    }

    /**
     * Assert that the flow execution result is not null, has ended, and its flow id equals the given flow id.
     * 
     * @param result the flow execution result
     * @param flowID the flow id
     */
    public void assertFlowExecutionResult(@Nullable final FlowExecutionResult result, @Nonnull String flowID) {
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getFlowId(), flowID);
        Assert.assertTrue(result.isEnded());
    }

    /**
     * Assert that the flow execution outcome is not null and its id equals 'end'. For testing purposes, the outcome's
     * attribute map must map {@value #OUTPUT_ATTR_NAME} to the {@link ProfileRequestContext}.
     * 
     * @param outcome the flow execution outcome
     */
    public void assertFlowExecutionOutcome(@Nullable final FlowExecutionOutcome outcome) {
        Assert.assertNotNull(outcome, "Flow ended with an error");
        Assert.assertEquals(outcome.getId(), "end");
        Assert.assertTrue(outcome.getOutput().contains(OUTPUT_ATTR_NAME));
        Assert.assertTrue(outcome.getOutput().get(OUTPUT_ATTR_NAME) instanceof ProfileRequestContext);
    }

}
