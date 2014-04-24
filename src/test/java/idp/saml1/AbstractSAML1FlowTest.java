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

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.saml.xml.SAMLConstants;

import org.opensaml.core.xml.schema.XSString;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.saml1.core.Assertion;
import org.opensaml.saml.saml1.core.Attribute;
import org.opensaml.saml.saml1.core.AttributeStatement;
import org.opensaml.saml.saml1.core.Audience;
import org.opensaml.saml.saml1.core.AudienceRestrictionCondition;
import org.opensaml.saml.saml1.core.AuthenticationStatement;
import org.opensaml.saml.saml1.core.Conditions;
import org.opensaml.saml.saml1.core.ConfirmationMethod;
import org.opensaml.saml.saml1.core.NameIdentifier;
import org.opensaml.saml.saml1.core.Response;
import org.opensaml.saml.saml1.core.StatusCode;
import org.opensaml.saml.saml1.core.Subject;
import org.opensaml.saml.saml1.core.SubjectConfirmation;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.webflow.execution.FlowExecutionOutcome;
import org.springframework.webflow.executor.FlowExecutionResult;
import org.testng.Assert;

/**
 * Abstract SAML 1 flow test.
 */
// TODO Remove ContextConfiguration once credentials are figured out.
@ContextConfiguration({"/system/conf/testbed-beans.xml", "file:src/main/webapp/WEB-INF/idp/testbed.xml"})
public abstract class AbstractSAML1FlowTest extends AbstractFlowTest {

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
     * <li>{@link #assertAssertions(List)}</li>
     * <li>{@link #assertAssertion(Assertion)}</li>
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

        final List<Assertion> assertions = response.getAssertions();
        assertAssertions(assertions);

        final Assertion assertion = assertions.get(0);
        assertAssertion(assertion);

        validateConditions(assertion);

        validateAuthenticationStatements(assertion);

        validateAttributeStatements(assertion);

        // TODO signing, encoding
    }

    /**
     * Validate the assertion conditions.
     * <p>
     * Calls assert methods :
     * <ul>
     * <li>{@link #assertConditions(Conditions)}</li>
     * <li>{@link #assertAudienceRestrictionConditions(List)}</li>
     * <li>{@link #assertAudiences(List)}</li>
     * </ul>
     * 
     * @param assertion the assertion
     */
    public void validateConditions(@Nullable final Assertion assertion) {
        Assert.assertNotNull(assertion);

        final Conditions conditions = assertion.getConditions();
        assertConditions(conditions);

        final List<AudienceRestrictionCondition> audienceRestrictionConditions =
                conditions.getAudienceRestrictionConditions();
        assertAudienceRestrictionConditions(audienceRestrictionConditions);

        final List<Audience> audiences = audienceRestrictionConditions.get(0).getAudiences();
        assertAudiences(audiences);
    }

    /**
     * Validate the assertion authentication statements.
     * <p>
     * Calls assert methods :
     * <ul>
     * <li>{@link #assertAuthenticationStatement(AuthenticationStatement)}</li>
     * <li>{@link #assertAttributeStatement(AttributeStatement)}</li>
     * <li>{@link #assertSubject(Subject)}</li>
     * <li>{@link #assertAuthenticationMethod(String)}</li>
     * </ul>
     * 
     * @param assertion the assertion
     */
    public void validateAuthenticationStatements(@Nullable final Assertion assertion) {
        Assert.assertNotNull(assertion);

        final List<AuthenticationStatement> authenticationStatements = assertion.getAuthenticationStatements();
        assertAuthenticationStatements(authenticationStatements);

        final AuthenticationStatement authenticationStatement = authenticationStatements.get(0);
        assertAuthenticationStatement(authenticationStatement);

        final Subject authenticationStatementSubject = authenticationStatement.getSubject();
        assertSubject(authenticationStatementSubject);

        assertAuthenticationMethod(authenticationStatement.getAuthenticationMethod());
    }

    /**
     * Validate the assertion attribute statements.
     * <p>
     * Calls assert methods :
     * <ul>
     * <li>{@link #assertAuthenticationStatements(List)}</li>
     * <li>{@link #assertAttributeStatement(AttributeStatement)}</li>
     * <li>{@link #assertSubject(Subject)}</li>
     * <li>{@link #assertNameIdentifier(NameIdentifier)}</li>
     * <li>{@link #assertSubjectConfirmation(SubjectConfirmation)}</li>
     * <li>{@link #assertConfirmationMethods(List)</li>
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

        final Subject attributeStatementSubject = attributeStatement.getSubject();
        assertSubject(attributeStatementSubject);

        final NameIdentifier nameIdentifier = attributeStatementSubject.getNameIdentifier();
        assertNameIdentifier(nameIdentifier);

        final SubjectConfirmation subjectConfirmation = attributeStatementSubject.getSubjectConfirmation();
        assertSubjectConfirmation(subjectConfirmation);

        final List<ConfirmationMethod> confirmationMethods = subjectConfirmation.getConfirmationMethods();
        assertConfirmationMethods(confirmationMethods);

        final List<Attribute> attributes = attributeStatement.getAttributes();
        assertAttributes(attributes);
    }
    
    /**
     * Assert that the outbound message context message is a SAML 1 response.
     * 
     * @param outboundMessageContext the outbound message context
     */
    public void assertOutboundMessageContextMessage(@Nonnull final MessageContext outboundMessageContext) {
        Assert.assertTrue(outboundMessageContext.getMessage() instanceof Response);
    }

    /**
     * Assert that the SAML 1 response is a success, the response version is correct, and the response contains a single
     * assertion.
     * 
     * @param response the SAML 1 response
     */
    public void assertResponse(@Nullable final Response response) {
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getVersion(), SAMLVersion.VERSION_11);
        Assert.assertEquals(response.getStatus().getStatusCode().getValue(), StatusCode.SUCCESS);
        Assert.assertNotNull(response.getAssertions());
        Assert.assertFalse(response.getAssertions().isEmpty());
        Assert.assertEquals(response.getAssertions().size(), 1);
        Assert.assertNotNull(response.getAssertions().get(0));
        // TODO response.getIssueInstant()
        // TODO response.getRecipient()
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
     * Assert that the assertion version is {@link SAMLVersion#VERSION_11}.
     * 
     * @param assertion the assertion
     */
    public void assertAssertion(@Nullable final Assertion assertion) {
        Assert.assertNotNull(assertion);
        Assert.assertEquals(assertion.getMajorVersion(), SAMLVersion.VERSION_11.getMajorVersion());
        Assert.assertEquals(assertion.getMinorVersion(), SAMLVersion.VERSION_11.getMinorVersion());
        Assert.assertEquals(assertion.getIssuer(), IDP_ENTITY_ID);
        // TODO assertion.getIssueInstant()
    }

    /**
     * Assert that a single authentication statement is present.
     * 
     * @param authenticationStatements the authentication statements
     */
    public void assertAuthenticationStatements(@Nullable List<AuthenticationStatement> authenticationStatements) {
        Assert.assertNotNull(authenticationStatements);
        Assert.assertFalse(authenticationStatements.isEmpty());
        Assert.assertEquals(authenticationStatements.size(), 1);
        Assert.assertNotNull(authenticationStatements.get(0));
    }

    /**
     * Assert that the authentication statement has a subject.
     * 
     * @param authenticationStatement the authentication statement
     */
    public void assertAuthenticationStatement(@Nullable AuthenticationStatement authenticationStatement) {
        Assert.assertNotNull(authenticationStatement);
        Assert.assertNotNull(authenticationStatement.getSubject());
        // TODO issueInstant
    }

    /**
     * Assert that the authentication method is {@link AuthenticationStatement#UNSPECIFIED_AUTHN_METHOD}.
     * 
     * @param authenticationMethod the authentication method
     */
    public void assertAuthenticationMethod(@Nullable String authenticationMethod) {
        Assert.assertNotNull(authenticationMethod);
        Assert.assertEquals(authenticationMethod, AuthenticationStatement.UNSPECIFIED_AUTHN_METHOD);
    }

    /**
     * Assert that a single audience restriction condition is present.
     * 
     * @param audienceRestrictionConditions the audience restriction conditions
     */
    public void assertAudienceRestrictionConditions(
            @Nullable final List<AudienceRestrictionCondition> audienceRestrictionConditions) {
        Assert.assertNotNull(audienceRestrictionConditions);
        Assert.assertEquals(audienceRestrictionConditions.size(), 1);
    }

    /**
     * Assert that a single audience is present whose URI is {@link AbstractFlowTest#SP_ENTITY_ID}.
     * 
     * @param audiences the audiences
     */
    public void assertAudiences(@Nullable final List<Audience> audiences) {
        Assert.assertNotNull(audiences);
        Assert.assertEquals(audiences.size(), 1);
        Assert.assertEquals(audiences.get(0).getUri(), SP_ENTITY_ID);
    }

    /**
     * Assert that the conditions has a NotBefore and NotOnOrAfter attribute, and that a single audience restriction
     * conditions is present.
     * 
     * @param conditions the conditions
     */
    public void assertConditions(@Nullable final Conditions conditions) {
        Assert.assertNotNull(conditions);
        Assert.assertNotNull(conditions.getNotBefore());
        Assert.assertNotNull(conditions.getNotOnOrAfter());
        // TODO check time via some range ?
        Assert.assertNotNull(conditions.getAudienceRestrictionConditions());
        Assert.assertEquals(conditions.getAudienceRestrictionConditions().size(), 1);
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
     * Assert that the attribute statement has a subject and attributes.
     * 
     * @param attributeStatement the attribute statement
     */
    public void assertAttributeStatement(@Nullable final AttributeStatement attributeStatement) {
        Assert.assertNotNull(attributeStatement);
        Assert.assertNotNull(attributeStatement.getSubject());
        Assert.assertNotNull(attributeStatement.getAttributes());
    }

    /**
     * Assert that the subject has a name identifier and subject confirmation.
     * 
     * @param subject the subject
     */
    public void assertSubject(@Nullable final Subject subject) {
        Assert.assertNotNull(subject);
        Assert.assertNotNull(subject.getNameIdentifier());
        Assert.assertNotNull(subject.getSubjectConfirmation());
    }

    /**
     * Assert that the name identifier format is {@link SAMLConstants#SAML1_NAMEID_TRANSIENT} and the name qualifier is
     * {@link AbstractFlowTest#IDP_ENTITY_ID}.
     * 
     * @param nameIdentifier the name identifier
     */
    public void assertNameIdentifier(@Nullable final NameIdentifier nameIdentifier) {
        Assert.assertNotNull(nameIdentifier);
        Assert.assertNotNull(nameIdentifier.getNameIdentifier());
        Assert.assertEquals(nameIdentifier.getFormat(), SAMLConstants.SAML1_NAMEID_TRANSIENT);
        Assert.assertEquals(nameIdentifier.getNameQualifier(), IDP_ENTITY_ID);
    }

    /**
     * Assert that the subject confirmation has a single confirmation method.
     * 
     * @param subjectConfirmation the subject confirmation
     */
    public void assertSubjectConfirmation(@Nullable final SubjectConfirmation subjectConfirmation) {
        Assert.assertNotNull(subjectConfirmation);
        Assert.assertEquals(subjectConfirmation.getConfirmationMethods().size(), 1);
    }

    /**
     * Assert that a single confirmation method is present.
     * <p>
     * Calls {@link #assertConfirmationMethod(ConfirmationMethod)}.
     * 
     * @param confirmationMethods the confirmation methods
     */
    public void assertConfirmationMethods(@Nullable final List<ConfirmationMethod> confirmationMethods) {
        Assert.assertNotNull(confirmationMethods);
        Assert.assertFalse(confirmationMethods.isEmpty());
        Assert.assertEquals(confirmationMethods.size(), 1);
        Assert.assertNotNull(confirmationMethods.get(0));

        assertConfirmationMethod(confirmationMethods.get(0));
    }

    /**
     * Assert the confirmation method, probably {@link ConfirmationMethod#METHOD_BEARER} or
     * {@link ConfirmationMethod#METHOD_ARTIFACT}.
     * 
     * @param confirmationMethod the confirmation method
     */
    public abstract void assertConfirmationMethod(@Nullable final ConfirmationMethod confirmationMethod);

    /**
     * Assert that two attributes are present.
     * <p>
     * The first attribute is
     * <ul>
     * <li>name : urn:mace:dir:attribute-def:eduPersonAffiliation</li>
     * <li>namespace : {@link SAMLConstants#SAML1_ATTR_NAMESPACE_URI}</li>
     * <li>value : member</li>
     * </ul>
     * <p>
     * The second attribute is
     * <ul>
     * <li>name : urn:oid:0.9.2342.19200300.100.1.3</li>
     * <li>namespace : {@link SAMLConstants#SAML1_ATTR_NAMESPACE_URI}</li>
     * <li>value : jdoe@shibboleth.net</li>
     * </ul>
     * 
     * Calls assert methods :
     * <ul>
     * <li>{@link #assertAttribute(Attribute, String, String)}</li>
     * </ul>
     * 
     * @param attributes the attributes
     */
    public void assertAttributes(@Nullable final List<Attribute> attributes) {
        Assert.assertNotNull(attributes);
        Assert.assertFalse(attributes.isEmpty());
        Assert.assertEquals(attributes.size(), 2);

        assertAttribute(attributes.get(0), "urn:mace:dir:attribute-def:eduPersonAffiliation", "member");
        assertAttribute(attributes.get(1), "urn:mace:dir:attribute-def:mail", "jdoe@shibboleth.net");
    }

    /**
     * Assert that the attribute namespace is {@link SAMLConstants#SAML1_ATTR_NAMESPACE_URI}, the attribute name is the
     * supplied name, and the attribute value is the single supplied String value.
     * 
     * @param attribute the attribute
     * @param attributeName the attribute name
     * @param attributeValue the attribute value
     */
    public void assertAttribute(@Nullable final Attribute attribute, @Nonnull final String attributeName,
            @Nonnull final String attributeValue) {
        Assert.assertNotNull(attribute);
        Assert.assertEquals(attribute.getAttributeName(), attributeName);
        Assert.assertEquals(attribute.getAttributeNamespace(), SAMLConstants.SAML1_ATTR_NAMESPACE_URI);
        Assert.assertEquals(attribute.getAttributeValues().size(), 1);
        Assert.assertTrue(attribute.getAttributeValues().get(0) instanceof XSString);
        Assert.assertEquals(((XSString) attribute.getAttributeValues().get(0)).getValue(), attributeValue);
    }
}
