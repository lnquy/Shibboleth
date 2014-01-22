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

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.context.web.WebAppConfiguration;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;

import com.unboundid.ldap.sdk.LDAPException;
import common.InMemoryDirectory;

/**
 * Abstract flow test.
 */
@ActiveProfiles("dev")
@WebAppConfiguration
@ContextConfiguration({"/system/conf/global-system.xml", "/conf/global-user.xml", "/system/conf/mvc-beans.xml",
        "/conf/webflow-config.xml"})
public abstract class AbstractFlowTest extends AbstractTestNGSpringContextTests {

    /** Path to LDIF file to be imported into directory server. */
    @Nonnull public final static String LDIF_FILE = "src/main/resources/test/ldap.ldif";

    /** In-memory directory server. */
    @Nonnull protected InMemoryDirectory directoryServer;

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
