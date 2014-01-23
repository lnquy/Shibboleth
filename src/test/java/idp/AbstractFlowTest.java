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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.shibboleth.utilities.java.support.net.HttpServletRequestResponseContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.webflow.executor.FlowExecutor;
import org.springframework.webflow.test.MockExternalContext;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterTest;
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

    /** In-memory directory server. */
    @Nonnull protected InMemoryDirectory directoryServer;

    /** Mock external context. */
    @Nonnull protected MockExternalContext externalContext;

    /** The web flow executor. */
    @Nonnull protected FlowExecutor flowExecutor;

    /** Mock request. */
    @Nonnull protected MockHttpServletRequest request;

    /** Mock response. */
    @Nonnull protected MockHttpServletResponse response;

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
     */
    @AfterTest public void teardownDirectoryServer() {
        directoryServer.stop();
    }
}
