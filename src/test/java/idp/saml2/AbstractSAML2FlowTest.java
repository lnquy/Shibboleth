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
import org.opensaml.saml.saml2.core.Audience;
import org.opensaml.saml.saml2.core.AudienceRestriction;
import org.opensaml.saml.saml2.core.AuthnContext;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.AuthnStatement;
import org.opensaml.saml.saml2.core.Conditions;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.core.Subject;
import org.opensaml.saml.saml2.core.SubjectConfirmation;
import org.opensaml.saml.saml2.core.SubjectConfirmationData;
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
     * <li>{@link #validateSubject(Subject)}</li>
     * <li>{@link #validateConditions(Assertion)}</li>
     * <li>{@link #validateAuthnStatements(Assertion)}</li>
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

        validateSubject(assertion.getSubject());

        validateConditions(assertion);

        validateAuthnStatements(assertion);

        validateAttributeStatements(assertion);
    }

    /**
     * Validate the subject.
     * <p>
     * Calls assert methods :
     * <ul>
     * <li>{@link #assertSubject(Subject)}</li>
     * <li>{@link #assertNameID(NameID)}</li>
     * <li>{@link #assertSubjectConfirmations(List)}</li>
     * <li>{@link #assertSubjectConfirmation(SubjectConfirmation)}</li>
     * <li>{@link #assertSubjectConfirmationMethod(SubjectConfirmation)}</li>
     * <li>{@link #assertSubjectConfirmationData(SubjectConfirmationData)}</li>
     * </ul>
     * 
     * @param subject the subject
     */
    public void validateSubject(@Nullable final Subject subject) {
        assertSubject(subject);
        assertNameID(subject.getNameID());
        assertSubjectConfirmations(subject.getSubjectConfirmations());
        final SubjectConfirmation subjectConfirmation = subject.getSubjectConfirmations().get(0);
        assertSubjectConfirmation(subjectConfirmation);
        assertSubjectConfirmationMethod(subjectConfirmation);
        assertSubjectConfirmationData(subjectConfirmation.getSubjectConfirmationData());
    }

    /**
     * Validate the assertion conditions.
     * <p>
     * Calls assert methods :
     * <ul>
     * <li>{@link #assertConditions(Conditions)}</li>
     * <li>{@link #assertAudienceRestrictions(List)}</li>
     * <li>{@link #assertAudienceRestriction(AudienceRestriction)}</li>
     * </ul>
     * 
     * @param assertion the assertion
     */
    public void validateConditions(@Nullable final Assertion assertion) {
        Assert.assertNotNull(assertion);

        final Conditions conditions = assertion.getConditions();
        assertConditions(conditions);

        final List<AudienceRestriction> audienceRestrictions = conditions.getAudienceRestrictions();
        assertAudienceRestrictions(audienceRestrictions);

        assertAudienceRestriction(audienceRestrictions.get(0));
    }

    /**
     * Validate the assertion authentication statements.
     * <p>
     * Calls assert methods :
     * <ul>
     * <li>{@link #assertAuthnStatements(List)}</li>
     * <li>{@link #assertAuthnStatement(AuthnStatement)}</li>
     * <li>{@link #assertAuthnContextClassRef(AuthnContextClassRef)}</li>
     * </ul>
     * 
     * @param assertion the assertion
     */
    public void validateAuthnStatements(@Nullable final Assertion assertion) {
        Assert.assertNotNull(assertion);

        final List<AuthnStatement> authnStatements = assertion.getAuthnStatements();
        assertAuthnStatements(authnStatements);

        final AuthnStatement authnStatement = authnStatements.get(0);
        assertAuthnStatement(authnStatement);
        assertAuthnContextClassRef(authnStatement.getAuthnContext().getAuthnContextClassRef());
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
     * Assert that :
     * <ul>
     * <li>the response ID is not null nor empty</li>
     * <li>the response issue instant is not null</li>
     * <li>the response version is {@link SAMLVersion#VERSION_20}</li>
     * <li>the response issuer is {@link AbstractFlowTest#IDP_ENTITY_ID}</li>
     * </ul>
     * 
     * @param response the response
     */
    public void assertResponse(@Nullable final Response response) {
        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getID());
        Assert.assertFalse(response.getID().isEmpty());
        Assert.assertNotNull(response.getIssueInstant());
        Assert.assertEquals(response.getVersion(), (SAMLVersion.VERSION_20));
        Assert.assertEquals(response.getIssuer().getValue(), IDP_ENTITY_ID);
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
     * Assert that :
     * <ul>
     * <li>the assertion ID is not null nor empty</li>
     * <li>the assertion issue instant is not null</li>
     * <li>the assertion version is {@link SAMLVersion#VERSION_20}</li>
     * <li>the issuer is {@link AbstractFlowTest#IDP_ENTITY_ID}</li>
     * </ul>
     * 
     * @param assertion the assertion
     */
    public void assertAssertion(@Nullable final Assertion assertion) {
        Assert.assertNotNull(assertion);
        Assert.assertNotNull(assertion.getID());
        Assert.assertFalse(assertion.getID().isEmpty());
        Assert.assertNotNull(assertion.getIssueInstant());
        Assert.assertEquals(assertion.getVersion(), SAMLVersion.VERSION_20);
        Assert.assertEquals(assertion.getIssuer().getValue(), IDP_ENTITY_ID);
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

    /**
     * Assert that a single subject confirmation is present.
     * 
     * @param subjectConfirmations the subject confirmations
     */
    public void assertSubjectConfirmations(@Nullable final List<SubjectConfirmation> subjectConfirmations) {
        Assert.assertNotNull(subjectConfirmations);
        Assert.assertEquals(subjectConfirmations.size(), 1);
    }

    /**
     * Assert that the subject confirmation has a confirmation method.
     * 
     * @param subjectConfirmation the subject confirmation
     */
    public void assertSubjectConfirmation(@Nullable final SubjectConfirmation subjectConfirmation) {
        Assert.assertNotNull(subjectConfirmation);
        Assert.assertNotNull(subjectConfirmation.getMethod());
    }

    /**
     * Assert that the subject confirmation method is {@link SubjectConfirmation#METHOD_BEARER}.
     * 
     * @param subjectConfirmation the subject confirmation
     */
    public void assertSubjectConfirmationMethod(@Nullable final SubjectConfirmation subjectConfirmation) {
        Assert.assertEquals(subjectConfirmation.getMethod(), SubjectConfirmation.METHOD_BEARER);
    }

    /**
     * Assert that :
     * <ul>
     * <li>the subject confirmation data address is "127.0.0.1"</li>
     * <li>the subject confirmation data NotOnOrAfter is not null</li>
     * <li>the subject confirmation data recipient is not null nor empty</li>
     * </ul>
     * 
     * @param subjectConfirmationData the subject confirmation data
     */
    public void assertSubjectConfirmationData(@Nullable final SubjectConfirmationData subjectConfirmationData) {
        Assert.assertEquals(subjectConfirmationData.getAddress(), "127.0.0.1");
        // TODO only in some cases ? Assert.assertNotNull(subjectConfirmationData.getNotBefore());
        Assert.assertNotNull(subjectConfirmationData.getNotOnOrAfter());
        Assert.assertNotNull(subjectConfirmationData.getRecipient());
        Assert.assertFalse(subjectConfirmationData.getRecipient().isEmpty());
    }

    /**
     * Assert that :
     * <ul>
     * <li>the NameID value is not null</li>
     * <li>the NameID format is {@link NameID#TRANSIENT}</li>
     * <li>the NameID name qualifier is {@link AbstractFlowTest#IDP_ENTITY_ID}</li>
     * <li>the NameID SP name qualifier is {@link AbstractFlowTest#SP_ENTITY_ID}</li>
     * <ul>
     * 
     * @param nameID the NameID
     */
    public void assertNameID(@Nullable final NameID nameID) {
        Assert.assertNotNull(nameID);
        Assert.assertNotNull(nameID.getValue());
        Assert.assertEquals(nameID.getFormat(), NameID.TRANSIENT);
        Assert.assertEquals(nameID.getNameQualifier(), AbstractFlowTest.IDP_ENTITY_ID);
        Assert.assertEquals(nameID.getSPNameQualifier(), AbstractFlowTest.SP_ENTITY_ID);
    }

    /**
     * Assert that the conditions has NotBefore and NotOnOrAfter attributes.
     * 
     * @param conditions the conditions
     */
    public void assertConditions(@Nullable final Conditions conditions) {
        Assert.assertNotNull(conditions);
        Assert.assertNotNull(conditions.getNotBefore());
        Assert.assertNotNull(conditions.getNotOnOrAfter());
        // TODO check time via some range ?
    }

    /**
     * Assert that a single audience restriction is present.
     * 
     * @param audienceRestrictions the audience restrictions
     */
    public void assertAudienceRestrictions(@Nullable final List<AudienceRestriction> audienceRestrictions) {
        Assert.assertNotNull(audienceRestrictions);
        Assert.assertEquals(audienceRestrictions.size(), 1);
    }

    /**
     * Assert that the audience restriction has a single audience whose URI is {@link AbstractFlowTest#SP_ENTITY_ID}.
     * 
     * @param audienceRestriction the audience restriction
     */
    public void assertAudienceRestriction(@Nullable final AudienceRestriction audienceRestriction) {
        Assert.assertNotNull(audienceRestriction);

        final List<Audience> audiences = audienceRestriction.getAudiences();
        Assert.assertEquals(audiences.size(), 1);

        final Audience audience = audiences.get(0);
        Assert.assertEquals(audience.getAudienceURI(), AbstractFlowTest.SP_ENTITY_ID);
    }

    /**
     * Assert that a single authn statement is present.
     * 
     * @param authnStatements the authn statements
     */
    public void assertAuthnStatements(@Nullable final List<AuthnStatement> authnStatements) {
        Assert.assertNotNull(authnStatements);
        Assert.assertEquals(authnStatements.size(), 1);
        Assert.assertNotNull(authnStatements.get(0));
    }

    /**
     * Assert that the authn statement has an authn instant and authn context class ref.
     * 
     * @param authnStatement the authn statement
     */
    public void assertAuthnStatement(@Nonnull final AuthnStatement authnStatement) {
        Assert.assertNotNull(authnStatement);
        Assert.assertNotNull(authnStatement.getAuthnInstant());
        // TODO check authn instant time ?
        Assert.assertNotNull(authnStatement.getAuthnContext());
        Assert.assertNotNull(authnStatement.getAuthnContext().getAuthnContextClassRef());
    }

    /**
     * Assert that the authn context class ref is {@link AuthnContext#IP_AUTHN_CTX}.
     * 
     * @param authnContextClassRef the authn context class ref
     */
    public void assertAuthnContextClassRef(@Nullable final AuthnContextClassRef authnContextClassRef) {
        Assert.assertEquals(authnContextClassRef.getAuthnContextClassRef(), AuthnContext.IP_AUTHN_CTX);
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
