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

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.core.xml.schema.XSString;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.opensaml.saml.saml2.core.Conditions;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.core.Subject;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.webflow.execution.FlowExecutionOutcome;
import org.springframework.webflow.executor.FlowExecutionResult;
import org.testng.Assert;

/**
 * Abstract SAML 2 flow test.
 */
@ContextConfiguration({"/system/conf/testbed-beans.xml", "file:src/main/webapp/WEB-INF/idp/testbed.xml"})
public class AbstractSAML2FlowTest extends AbstractFlowTest {

    /**
     * Top level validation of the {@link FlowExecutionResult}.
     * <p>
     * Calls validate methods :
     * <ul>
     * <li>{@link #validateConditions(Assertion)}</li>
     * <li>{@link #validateAuthenticationStatements(Assertion)}</li>
     * <li>{@link #validateAttributeStatements(Assertion)}</li>
     * </ul>
     * Calls assert methods :
     * <ul>
     * <li>{@link AbstractFlowTest#assertFlowExecutionResult(FlowExecutionResult, String)}</li>
     * <li>{@link AbstractFlowTest#assertFlowExecutionOutcome(FlowExecutionOutcome)}</li>
     * <li>{@link AbstractFlowTest#assertProfileRequestContext(ProfileRequestContext)}</li>
     * <li>{@link #assertOutboundMessageContextMessage(MessageContext)}</li>
     * <li>{@link #assertResponse(Response)}</li>
     * <li>{@link #assertStatus(Status)}</li>
     * <li>{@link #assertAssertions(List)}</li>
     * <li>{@link #assertAssertion(Assertion)}</li>
     * <li>{@link #assertSubject(Subject)}</li>
     * </ul>
     * 
     * @param result the flow execution result
     * @param flowId the flow ID
     */
    public void validateResult(@Nullable final FlowExecutionResult result, @Nonnull final String flowId) {

        assertFlowExecutionResult(result, flowId);

        final FlowExecutionOutcome outcome = result.getOutcome();
        assertFlowExecutionOutcome(outcome);

        final ProfileRequestContext prc = (ProfileRequestContext) outcome.getOutput().get(OUTPUT_ATTR_NAME);
        assertProfileRequestContext(prc);

        assertOutboundMessageContextMessage(prc.getOutboundMessageContext());

        final Response response = (Response) prc.getOutboundMessageContext().getMessage();
        assertResponse(response);

        assertStatus(response.getStatus());

        final List<Assertion> assertions = response.getAssertions();
        assertAssertions(assertions);

        final Assertion assertion = assertions.get(0);
        assertAssertion(assertion);

        assertSubject(assertion.getSubject());

        validateConditions(assertion);

        validateAuthenticationStatements(assertion);

        validateAttributeStatements(assertion);

        // TODO much more
    }

    /**
     * Validate the assertion conditions.
     * <p>
     * Calls assert methods :
     * <ul>
     * <li></li>
     * </ul>
     * 
     * @param assertion the assertion
     */
    public void validateConditions(@Nullable final Assertion assertion) {
        Assert.assertNotNull(assertion);

        final Conditions conditions = assertion.getConditions();
        assertConditions(conditions);
        // TODO implement
    }

    /**
     * Validate the assertion authentication statements.
     * <p>
     * Calls assert methods :
     * <ul>
     * <li></li>
     * </ul>
     * 
     * @param assertion the assertion
     */
    public void validateAuthenticationStatements(@Nullable final Assertion assertion) {
        Assert.assertNotNull(assertion);
        // TODO implement
    }

    /**
     * Validate the assertion attribute statements.
     * <p>
     * Calls assert methods :
     * <ul>
     * <li>{@link #assertAttributeStatements(List)}</li>
     * <li>{@link #assertAttributeStatement(AttributeStatement)}</li>
     * <li>{@link #assertAttributes(List)}</li>
     * </ul>
     * 
     * @param assertion the assertion
     */
    public void validateAttributeStatements(@Nullable final Assertion assertion) {
        Assert.assertNotNull(assertion);

        final List<AttributeStatement> attributeStatements = assertion.getAttributeStatements();
        assertAttributeStatements(attributeStatements);

        final AttributeStatement attributeStatement = attributeStatements.get(0);
        assertAttributeStatement(attributeStatement);

        final List<Attribute> attributes = attributeStatement.getAttributes();
        assertAttributes(attributes);

        // TODO
    }

    /**
     * Assert that the outbound message context message is a SAML 2 response.
     * 
     * @param outboundMessageContext the outbound message context
     */
    public void assertOutboundMessageContextMessage(@Nonnull final MessageContext outboundMessageContext) {
        Assert.assertTrue(outboundMessageContext.getMessage() instanceof Response);
    }

    /**
     * Assert that the response issuer is {@link AbstractFlowTest#IDP_ENTITY_ID}, the status code is
     * {@link StatusCode#SUCCESS_URI}.
     * 
     * @param response
     */
    public void assertResponse(@Nullable final Response response) {
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getIssuer().getValue(), IDP_ENTITY_ID);
        // TODO
    }

    /**
     * Assert that the status has a status code of {@link StatusCode#SUCCESS}.
     * 
     * @param status the status
     */
    public void assertStatus(@Nullable final Status status) {
        Assert.assertNotNull(status);
        Assert.assertNotNull(status.getStatusCode());
        Assert.assertEquals(status.getStatusCode().getValue(), StatusCode.SUCCESS_URI);
    }

    /**
     * Assert that a single assertion is present.
     * 
     * @param assertions the assertions
     */
    public void assertAssertions(@Nullable List<Assertion> assertions) {
        Assert.assertNotNull(assertions);
        Assert.assertFalse(assertions.isEmpty());
        Assert.assertEquals(assertions.size(), 1);
        Assert.assertNotNull(assertions.get(0));
    }

    /**
     * Assert that the assertion version is {@link SAMLVersion#VERSION_20} and that the issuer is
     * {@link AbstractFlowTest#IDP_ENTITY_ID}.
     * 
     * @param assertion the assertion
     */
    public void assertAssertion(@Nullable final Assertion assertion) {
        Assert.assertNotNull(assertion);
        Assert.assertEquals(assertion.getVersion().getMajorVersion(), SAMLVersion.VERSION_20.getMajorVersion());
        Assert.assertEquals(assertion.getVersion().getMinorVersion(), SAMLVersion.VERSION_20.getMinorVersion());
        Assert.assertEquals(assertion.getIssuer().getValue(), IDP_ENTITY_ID);
        // TODO probably need an assertIssuer() method
        // TODO assertion.getIssueInstant()
    }

    /**
     * Assert that the subject has a nameID and subject confirmations.
     * 
     * @param subject the subject
     */
    public void assertSubject(@Nullable final Subject subject) {
        Assert.assertNotNull(subject);
        Assert.assertNotNull(subject.getNameID());
        Assert.assertNotNull(subject.getSubjectConfirmations());
    }

    public void assertNameID(@Nullable final NameID nameID) {
        Assert.assertNotNull(nameID);
        // TODO nameID.getNameQualifier();
        // TODO nameID.getValue()
    }

    public void assertConditions(@Nullable final Conditions conditions) {
        Assert.assertNotNull(conditions);
        // TODO implement
    }

    /**
     * Assert that a single attribute statement is present.
     * 
     * @param attributeStatements the attribute statements
     */
    public void assertAttributeStatements(@Nullable final List<AttributeStatement> attributeStatements) {
        Assert.assertNotNull(attributeStatements);
        Assert.assertFalse(attributeStatements.isEmpty());
        Assert.assertEquals(attributeStatements.size(), 1);
        Assert.assertNotNull(attributeStatements.get(0));
    }

    /**
     * Assert that the attribute statement has attributes.
     * 
     * @param attributeStatement the attribute statement
     */
    public void assertAttributeStatement(@Nullable final AttributeStatement attributeStatement) {
        Assert.assertNotNull(attributeStatement);
        Assert.assertNotNull(attributeStatement.getAttributes());
    }

    /**
     * Assert that two attributes are present.
     * <p>
     * The first attribute is
     * <ul>
     * <li>name : urn:oid:1.3.6.1.4.1.5923.1.1.1.1</li>
     * <li>name format : {@link Attribute#URI_REFERENCE}</li>
     * <li>friendly name : eduPersonAffiliation</li>
     * <li>value : member</li>
     * </ul>
     * <p>
     * The second attribute is
     * <ul>
     * <li>name : urn:oid:0.9.2342.19200300.100.1.3</li>
     * <li>name format : {@link Attribute#URI_REFERENCE}</li>
     * <li>friendly name : mail</li>
     * <li>value : jdoe@shibboleth.net</li>
     * </ul>
     * 
     * Calls assert methods :
     * <ul>
     * <li>{@link #assertAttributeName(Attribute, String, String, String)}</li>
     * <li>{@link #assertAttributeValue(Attribute, String)}</li>
     * </ul>
     * 
     * @param attributes the attributes
     */
    public void assertAttributes(@Nullable final List<Attribute> attributes) {
        Assert.assertNotNull(attributes);
        Assert.assertFalse(attributes.isEmpty());
        Assert.assertEquals(attributes.size(), 2);

        Attribute eduPersonAffiliation = attributes.get(0);
        assertAttributeName(eduPersonAffiliation, "urn:oid:1.3.6.1.4.1.5923.1.1.1.1", Attribute.URI_REFERENCE,
                "eduPersonAffiliation");
        assertAttributeValue(eduPersonAffiliation, "member");

        Attribute mail = attributes.get(1);
        assertAttributeName(mail, "urn:oid:0.9.2342.19200300.100.1.3", Attribute.URI_REFERENCE, "mail");
        assertAttributeValue(mail, "jdoe@shibboleth.net");
    }

    /**
     * Assert that the attribute name, name format, and friendly name are the supplied names.
     * 
     * @param attribute the attribute
     * @param name the attribute name
     * @param nameFormat the attribute name format
     * @param friendlyName the attribute friendly name
     */
    public void assertAttributeName(@Nullable final Attribute attribute, @Nonnull final String name,
            @Nonnull final String nameFormat, @Nonnull final String friendlyName) {
        Assert.assertNotNull(attribute);
        Assert.assertEquals(attribute.getName(), name);
        Assert.assertEquals(attribute.getNameFormat(), nameFormat);
        Assert.assertEquals(attribute.getFriendlyName(), friendlyName);
    }

    /**
     * Assert that the attribute value is the supplied String value.
     * 
     * @param attribute the attribute
     * @param attributeValue the attribute value
     */
    public void assertAttributeValue(@Nullable final Attribute attribute, @Nonnull final String attributeValue) {
        Assert.assertEquals(attribute.getAttributeValues().size(), 1);
        Assert.assertTrue(attribute.getAttributeValues().get(0) instanceof XSString);
        Assert.assertEquals(((XSString) attribute.getAttributeValues().get(0)).getValue(), attributeValue);
    }

}
