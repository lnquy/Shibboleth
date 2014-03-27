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

package idp.saml2;

import idp.AbstractFlowTest;

import javax.annotation.Nonnull;

import org.opensaml.core.xml.schema.XSString;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.StatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.webflow.execution.FlowExecutionOutcome;
import org.springframework.webflow.executor.FlowExecutionResult;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test for SAML2 Unsolicited flow.
 */
@ContextConfiguration({"/system/conf/testbed-beans.xml", "file:src/main/webapp/WEB-INF/idp/testbed.xml"})
public class SAML2UnsolicitedFlowTest extends AbstractFlowTest {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(SAML2UnsolicitedFlowTest.class);

    /** Flow id. */
    @Nonnull public final static String FLOW_ID = "SAML2/Unsolicited/SSO";

    @Test public void testFlow() {
        request.addParameter("providerId", "https://sp.example.org");

        FlowExecutionResult result = flowExecutor.launchExecution(FLOW_ID, null, externalContext);
        Assert.assertEquals(result.getFlowId(), FLOW_ID);

        FlowExecutionOutcome outcome = result.getOutcome();
        log.debug("flow outcome {}", outcome);
        Assert.assertNotNull(outcome);
        Assert.assertEquals(outcome.getId(), "end");
        Assert.assertTrue(result.isEnded());

        // Get the response
        Assert.assertTrue(outcome.getOutput().contains(OUTPUT_ATTR_NAME));
        Assert.assertTrue(outcome.getOutput().get(OUTPUT_ATTR_NAME) instanceof ProfileRequestContext);
        ProfileRequestContext prc = (ProfileRequestContext) outcome.getOutput().get(OUTPUT_ATTR_NAME);
        Assert.assertNotNull(prc.getOutboundMessageContext());
        Assert.assertNotNull(prc.getOutboundMessageContext().getMessage());
        Assert.assertTrue(prc.getOutboundMessageContext().getMessage() instanceof Response);
        Response response = (Response) prc.getOutboundMessageContext().getMessage();

        Assert.assertEquals("https://idp.example.org", response.getIssuer().getValue());
        Assert.assertEquals(StatusCode.SUCCESS_URI, response.getStatus().getStatusCode().getValue());

        Assert.assertNotNull(response.getAssertions());
        Assert.assertFalse(response.getAssertions().isEmpty());
        Assert.assertEquals(response.getAssertions().size(), 1);
        Assert.assertNotNull(response.getAssertions().get(0));

        Assertion assertion = response.getAssertions().get(0);

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
        Assert.assertEquals(eduPersonAffiliation.getName(), "urn:oid:1.3.6.1.4.1.5923.1.1.1.1");
        Assert.assertEquals(eduPersonAffiliation.getNameFormat(), Attribute.URI_REFERENCE);
        Assert.assertEquals(eduPersonAffiliation.getFriendlyName(), "eduPersonAffiliation");
        Assert.assertEquals(eduPersonAffiliation.getAttributeValues().size(), 1);
        Assert.assertTrue(eduPersonAffiliation.getAttributeValues().get(0) instanceof XSString);
        Assert.assertEquals(((XSString) eduPersonAffiliation.getAttributeValues().get(0)).getValue(), "member");

        Attribute mail = attributeStatement.getAttributes().get(1);
        Assert.assertEquals(mail.getName(), "urn:oid:0.9.2342.19200300.100.1.3");
        Assert.assertEquals(mail.getNameFormat(), Attribute.URI_REFERENCE);
        Assert.assertEquals(mail.getFriendlyName(), "mail");
        Assert.assertEquals(mail.getAttributeValues().size(), 1);
        Assert.assertTrue(mail.getAttributeValues().get(0) instanceof XSString);
        Assert.assertEquals(((XSString) mail.getAttributeValues().get(0)).getValue(), "jdoe@shibboleth.net");    }
}
