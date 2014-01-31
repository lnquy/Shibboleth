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

import idp.AbstractFlowTest;

import javax.annotation.Nonnull;

import org.opensaml.core.xml.schema.XSString;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.saml1.core.Assertion;
import org.opensaml.saml.saml1.core.Attribute;
import org.opensaml.saml.saml1.core.AttributeStatement;
import org.opensaml.saml.saml1.core.AuthenticationStatement;
import org.opensaml.saml.saml1.core.Response;
import org.opensaml.saml.saml1.core.StatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.webflow.execution.FlowExecutionOutcome;
import org.springframework.webflow.executor.FlowExecutionResult;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test for SAML1 flow.
 */
// TODO Remove ContextConfiguration once credentials are figured out.
@ContextConfiguration({"/system/conf/testbed-beans.xml", "file:src/main/webapp/WEB-INF/idp/testbed.xml"})
public class SAML1UnsolicitedFlowTest extends AbstractFlowTest {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(SAML1UnsolicitedFlowTest.class);

    /** Flow id. */
    @Nonnull public final static String FLOW_ID = "Shibboleth/SSO";

    @Test public void testFlow() {
        // TODO more request parameters
        request.addParameter("providerId", "https://sp.example.org");

        FlowExecutionResult result = flowExecutor.launchExecution(FLOW_ID, null, externalContext);
        Assert.assertEquals(FLOW_ID, result.getFlowId());

        FlowExecutionOutcome outcome = result.getOutcome();
        log.debug("flow outcome {}", outcome);
        Assert.assertNotNull(outcome);
        Assert.assertEquals(outcome.getId(), "end");
        Assert.assertTrue(result.isEnded());

        Assert.assertTrue(outcome.getOutput().contains(OUTPUT_ATTR_NAME));
        Assert.assertTrue(outcome.getOutput().get(OUTPUT_ATTR_NAME) instanceof ProfileRequestContext);
        ProfileRequestContext prc = (ProfileRequestContext) outcome.getOutput().get(OUTPUT_ATTR_NAME);
        Assert.assertNotNull(prc.getOutboundMessageContext());
        Assert.assertNotNull(prc.getOutboundMessageContext().getMessage());
        Assert.assertTrue(prc.getOutboundMessageContext().getMessage() instanceof Response);

        Response response = (Response) prc.getOutboundMessageContext().getMessage();
        Assert.assertEquals(response.getVersion(), SAMLVersion.VERSION_11);
        Assert.assertEquals(response.getStatus().getStatusCode().getValue(), StatusCode.SUCCESS);

        Assert.assertNotNull(response.getAssertions());
        Assert.assertFalse(response.getAssertions().isEmpty());
        Assert.assertEquals(response.getAssertions().size(), 1);
        Assert.assertNotNull(response.getAssertions().get(0));

        Assertion assertion = response.getAssertions().get(0);
        Assert.assertEquals(assertion.getMajorVersion(), SAMLVersion.VERSION_11.getMajorVersion());
        Assert.assertEquals(assertion.getMinorVersion(), SAMLVersion.VERSION_11.getMinorVersion());
        Assert.assertEquals(assertion.getIssuer(), "https://idp.example.org");

        // TODO assertion conditions ?

        Assert.assertNotNull(assertion.getAuthenticationStatements());
        Assert.assertFalse(assertion.getAuthenticationStatements().isEmpty());
        Assert.assertEquals(assertion.getAuthenticationStatements().size(), 1);
        Assert.assertNotNull(assertion.getAuthenticationStatements().get(0));

        AuthenticationStatement authnStatement = assertion.getAuthenticationStatements().get(0);
        // TODO authn method ?
        Assert.assertEquals(authnStatement.getAuthenticationMethod(), AuthenticationStatement.UNSPECIFIED_AUTHN_METHOD);
        // TODO subject locality, etc

        Assert.assertNotNull(assertion.getAttributeStatements());
        Assert.assertFalse(assertion.getAttributeStatements().isEmpty());
        Assert.assertEquals(assertion.getAttributeStatements().size(), 1);
        Assert.assertNotNull(assertion.getAttributeStatements().get(0));

        AttributeStatement attributeStatement = assertion.getAttributeStatements().get(0);

        Assert.assertNotNull(attributeStatement.getAttributes());
        Assert.assertFalse(attributeStatement.getAttributes().isEmpty());
        Assert.assertEquals(attributeStatement.getAttributes().size(), 2);

        // TODO attribute ordering ?
        Attribute eduPersonAffiliation = attributeStatement.getAttributes().get(0);
        Assert.assertEquals(eduPersonAffiliation.getAttributeName(), "urn:mace:dir:attribute-def:eduPersonAffiliation");
        Assert.assertEquals(eduPersonAffiliation.getAttributeNamespace(),
                "urn:mace:shibboleth:1.0:attributeNamespace:uri");
        // Assert.assertEquals("eduPersonAffiliation", eduPersonAffiliation.);
        Assert.assertEquals(eduPersonAffiliation.getAttributeValues().size(), 1);
        Assert.assertTrue(eduPersonAffiliation.getAttributeValues().get(0) instanceof XSString);
        Assert.assertEquals(((XSString) eduPersonAffiliation.getAttributeValues().get(0)).getValue(), "member");

        Attribute mail = attributeStatement.getAttributes().get(1);
        Assert.assertEquals(mail.getAttributeName(), "urn:mace:dir:attribute-def:mail");
        Assert.assertEquals(mail.getAttributeNamespace(), "urn:mace:shibboleth:1.0:attributeNamespace:uri");
        // Assert.assertEquals("mail", mail.getFriendlyName());
        Assert.assertEquals(mail.getAttributeValues().size(), 1);
        Assert.assertTrue(mail.getAttributeValues().get(0) instanceof XSString);
        Assert.assertEquals(((XSString) mail.getAttributeValues().get(0)).getValue(), "jdoe@shibboleth.net");

        // TODO meaningful asserts
    }
}
