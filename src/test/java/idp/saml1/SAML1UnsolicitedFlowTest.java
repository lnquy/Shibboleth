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

package idp.saml1;

import idp.AbstractSAML1FlowTest;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.saml.profile.impl.BaseIdPInitiatedSSORequestMessageDecoder;

import org.opensaml.saml.saml1.core.ConfirmationMethod;
import org.opensaml.saml.saml1.core.Response;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.webflow.executor.FlowExecutionResult;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test for SAML 1 unsolicited SSO flow.
 */
public class SAML1UnsolicitedFlowTest extends AbstractSAML1FlowTest {

    /** The flow id. */
    @Nonnull public final static String FLOW_ID = "Shibboleth/SSO";

    /**
     * Test the SAML1 unsolicited SSO flow.
     * 
     * @throws Exception if an error occurs
     */
    @Test public void testSAML1UnsolicitedSSOFlow() throws Exception {

        buildRequest();

        final FlowExecutionResult result = flowExecutor.launchExecution(FLOW_ID, null, externalContext);

        validateResult(result, FLOW_ID);
    }

    /**
     * Build the {@link MockHttpServletRequest}.
     * 
     * @throws Exception if an error occurs
     */
    public void buildRequest() {
        // TODO time request parameter ?
        request.addParameter(BaseIdPInitiatedSSORequestMessageDecoder.PROVIDER_ID_PARAM, SP_ENTITY_ID);
        request.addParameter(BaseIdPInitiatedSSORequestMessageDecoder.SHIRE_PARAM, SP_ACS_URL);
        request.addParameter(BaseIdPInitiatedSSORequestMessageDecoder.TARGET_PARAM, SP_RELAY_STATE);
    }

    /**
     * {@inheritDoc}
     * 
     * Assert that the response recipient equals the ACS URL.
     */
    @Override public void assertResponse(@Nullable final Response response) {
        super.assertResponse(response);
        Assert.assertEquals(response.getRecipient(), SP_ACS_URL);
    }

    /**
     * {@inheritDoc}
     * 
     * Assert that the confirmation method equals {@link org.opensaml.saml.saml1.core.ConfirmationMethod#METHOD_BEARER}.
     */
    @Override public void assertConfirmationMethod(@Nullable final ConfirmationMethod confirmationMethod) {
        Assert.assertNotNull(confirmationMethod);
        Assert.assertEquals(confirmationMethod.getConfirmationMethod(), ConfirmationMethod.METHOD_BEARER);
    }
}
