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

import net.shibboleth.idp.saml.impl.profile.BaseIdPInitiatedSSORequestMessageDecoder;

import org.opensaml.core.xml.schema.XSString;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.saml1.core.Assertion;
import org.opensaml.saml.saml1.core.Attribute;
import org.opensaml.saml.saml1.core.AttributeStatement;
import org.opensaml.saml.saml1.core.AuthenticationStatement;
import org.opensaml.saml.saml1.core.ConfirmationMethod;
import org.opensaml.saml.saml1.core.NameIdentifier;
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

    private String entityId = "http://sp.example.org";

    private String acsUrl = "https://sp.example.org/ACSURL";

    private String relayState = "myRelayState";

    @Test public void testFlow() {
        // TODO time request parameter ?
        request.addParameter(BaseIdPInitiatedSSORequestMessageDecoder.PROVIDER_ID_PARAM, entityId);
        request.addParameter(BaseIdPInitiatedSSORequestMessageDecoder.SHIRE_PARAM, acsUrl);
        request.addParameter(BaseIdPInitiatedSSORequestMessageDecoder.TARGET_PARAM, relayState);

        final FlowExecutionResult result = flowExecutor.launchExecution(FLOW_ID, null, externalContext);
        Assert.assertEquals(FLOW_ID, result.getFlowId());

        final FlowExecutionOutcome outcome = result.getOutcome();
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

        final Response response = (Response) prc.getOutboundMessageContext().getMessage();
        Assert.assertEquals(response.getVersion(), SAMLVersion.VERSION_11);
        Assert.assertEquals(response.getStatus().getStatusCode().getValue(), StatusCode.SUCCESS);
        Assert.assertEquals(response.getRecipient(), acsUrl);

        Assert.assertNotNull(response.getAssertions());
        Assert.assertFalse(response.getAssertions().isEmpty());
        Assert.assertEquals(response.getAssertions().size(), 1);
        Assert.assertNotNull(response.getAssertions().get(0));

        final Assertion assertion = response.getAssertions().get(0);
        Assert.assertEquals(assertion.getMajorVersion(), SAMLVersion.VERSION_11.getMajorVersion());
        Assert.assertEquals(assertion.getMinorVersion(), SAMLVersion.VERSION_11.getMinorVersion());
        Assert.assertEquals(assertion.getIssuer(), "https://idp.example.org");

        // TODO are these tests sufficient ?
        Assert.assertNotNull(assertion.getConditions());
        Assert.assertNotNull(assertion.getConditions().getNotBefore());
        Assert.assertNotNull(assertion.getConditions().getNotOnOrAfter());

        Assert.assertNotNull(assertion.getConditions().getAudienceRestrictionConditions());
        Assert.assertEquals(assertion.getConditions().getAudienceRestrictionConditions().size(), 1);

        Assert.assertNotNull(assertion.getAuthenticationStatements());
        Assert.assertFalse(assertion.getAuthenticationStatements().isEmpty());
        Assert.assertEquals(assertion.getAuthenticationStatements().size(), 1);
        Assert.assertNotNull(assertion.getAuthenticationStatements().get(0));

        AuthenticationStatement authnStmt = assertion.getAuthenticationStatements().get(0);
        // TODO authn method ?
        Assert.assertEquals(authnStmt.getAuthenticationMethod(), AuthenticationStatement.UNSPECIFIED_AUTHN_METHOD);
        // TODO subject locality, etc

        // TODO is this correct ?
        Assert.assertNotNull(authnStmt.getSubject());
        Assert.assertNotNull(authnStmt.getSubject().getNameIdentifier());
        Assert.assertEquals(authnStmt.getSubject().getNameIdentifier().getFormat(), NameIdentifier.EMAIL);
        Assert.assertEquals(authnStmt.getSubject().getNameIdentifier().getNameIdentifier(), "jdoe@shibboleth.net");
        Assert.assertEquals(authnStmt.getSubject().getNameIdentifier().getNameQualifier(), "https://idp.example.org");

        // TODO is this correct ?
        Assert.assertNotNull(authnStmt.getSubject().getSubjectConfirmation());
        Assert.assertNotNull(authnStmt.getSubject().getSubjectConfirmation().getConfirmationMethods());
        Assert.assertEquals(authnStmt.getSubject().getSubjectConfirmation().getConfirmationMethods().size(), 1);
        Assert.assertEquals(authnStmt.getSubject().getSubjectConfirmation().getConfirmationMethods().get(0)
                .getConfirmationMethod(), ConfirmationMethod.METHOD_BEARER);

        Assert.assertNotNull(assertion.getAttributeStatements());
        Assert.assertFalse(assertion.getAttributeStatements().isEmpty());
        Assert.assertEquals(assertion.getAttributeStatements().size(), 1);
        Assert.assertNotNull(assertion.getAttributeStatements().get(0));

        AttributeStatement attrStmt = assertion.getAttributeStatements().get(0);

        // TODO is this correct ?
        Assert.assertNotNull(attrStmt.getSubject());
        Assert.assertNotNull(attrStmt.getSubject().getNameIdentifier());
        Assert.assertEquals(attrStmt.getSubject().getNameIdentifier().getFormat(), NameIdentifier.EMAIL);
        Assert.assertEquals(attrStmt.getSubject().getNameIdentifier().getNameIdentifier(), "jdoe@shibboleth.net");
        Assert.assertEquals(attrStmt.getSubject().getNameIdentifier().getNameQualifier(), "https://idp.example.org");

        // TODO is this correct ?
        Assert.assertNotNull(attrStmt.getSubject().getSubjectConfirmation());
        Assert.assertNotNull(attrStmt.getSubject().getSubjectConfirmation().getConfirmationMethods());
        Assert.assertEquals(attrStmt.getSubject().getSubjectConfirmation().getConfirmationMethods().size(), 1);
        Assert.assertEquals(attrStmt.getSubject().getSubjectConfirmation().getConfirmationMethods().get(0)
                .getConfirmationMethod(), ConfirmationMethod.METHOD_BEARER);

        Assert.assertNotNull(attrStmt.getAttributes());
        Assert.assertFalse(attrStmt.getAttributes().isEmpty());
        Assert.assertEquals(attrStmt.getAttributes().size(), 2);

        // TODO attribute ordering ?
        Attribute epaAttr = attrStmt.getAttributes().get(0);
        Assert.assertEquals(epaAttr.getAttributeName(), "urn:mace:dir:attribute-def:eduPersonAffiliation");
        Assert.assertEquals(epaAttr.getAttributeNamespace(), "urn:mace:shibboleth:1.0:attributeNamespace:uri");
        Assert.assertEquals(epaAttr.getAttributeValues().size(), 1);
        Assert.assertTrue(epaAttr.getAttributeValues().get(0) instanceof XSString);
        Assert.assertEquals(((XSString) epaAttr.getAttributeValues().get(0)).getValue(), "member");

        Attribute mailAttr = attrStmt.getAttributes().get(1);
        Assert.assertEquals(mailAttr.getAttributeName(), "urn:mace:dir:attribute-def:mail");
        Assert.assertEquals(mailAttr.getAttributeNamespace(), "urn:mace:shibboleth:1.0:attributeNamespace:uri");
        Assert.assertEquals(mailAttr.getAttributeValues().size(), 1);
        Assert.assertTrue(mailAttr.getAttributeValues().get(0) instanceof XSString);
        Assert.assertEquals(((XSString) mailAttr.getAttributeValues().get(0)).getValue(), "jdoe@shibboleth.net");

        // TODO test SignAssertions

        // TODO test outbound message handling

        // TODO test message encoding
    }
}
