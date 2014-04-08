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

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.saml.xml.SAMLConstants;

import org.opensaml.core.xml.schema.XSString;
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

    public void validateResult(@Nullable final FlowExecutionResult result, String flowId) {

        assertFlowExecutionResult(result, flowId);

        final FlowExecutionOutcome outcome = result.getOutcome();
        assertFlowExecutionOutcome(outcome);

        final ProfileRequestContext prc = (ProfileRequestContext) outcome.getOutput().get(OUTPUT_ATTR_NAME);
        assertProfileRequestContext(prc);

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
        this.assertConfirmationMethods(confirmationMethods);

        final List<Attribute> attributes = attributeStatement.getAttributes();
        assertAttributes(attributes);
    }

    /**
     * Assert that the profile request context has an outbound message context whose message is a SAML 1 response.
     * 
     * @param profileRequestContext the profile request context
     */
    public void assertProfileRequestContext(@Nullable final ProfileRequestContext profileRequestContext) {
        Assert.assertNotNull(profileRequestContext);
        Assert.assertNotNull(profileRequestContext.getOutboundMessageContext());
        Assert.assertNotNull(profileRequestContext.getOutboundMessageContext().getMessage());
        Assert.assertTrue(profileRequestContext.getOutboundMessageContext().getMessage() instanceof Response);
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
     * Assert that the assertion has the correct major and minor version and the issuer is correct.
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

    public void assertAuthenticationStatements(@Nullable List<AuthenticationStatement> authenticationStatements) {
        Assert.assertNotNull(authenticationStatements);
        Assert.assertFalse(authenticationStatements.isEmpty());
        Assert.assertEquals(authenticationStatements.size(), 1);
        Assert.assertNotNull(authenticationStatements.get(0));
    }

    public void assertAuthenticationStatement(@Nullable AuthenticationStatement authenticationStatement) {
        Assert.assertNotNull(authenticationStatement);
        Assert.assertNotNull(authenticationStatement.getSubject());
        // TODO issueInstant
    }

    public void assertAuthenticationMethod(@Nullable String authenticationMethod) {
        Assert.assertNotNull(authenticationMethod);
        Assert.assertEquals(authenticationMethod, AuthenticationStatement.UNSPECIFIED_AUTHN_METHOD);
    }

    public void assertAudienceRestrictionConditions(
            @Nullable final List<AudienceRestrictionCondition> audienceRestrictionConditions) {
        Assert.assertNotNull(audienceRestrictionConditions);
        Assert.assertEquals(audienceRestrictionConditions.size(), 1);
    }

    public void assertAudiences(@Nullable final List<Audience> audiences) {
        Assert.assertNotNull(audiences);
        Assert.assertEquals(audiences.size(), 1);
        Assert.assertEquals(audiences.get(0).getUri(), "https://sp.example.org");
    }

    public void assertConditions(@Nullable final Conditions conditions) {
        Assert.assertNotNull(conditions);
        Assert.assertNotNull(conditions.getNotBefore());
        Assert.assertNotNull(conditions.getNotOnOrAfter());
        // TODO check time via some range ?
        Assert.assertNotNull(conditions.getAudienceRestrictionConditions());
        Assert.assertEquals(conditions.getAudienceRestrictionConditions().size(), 1);
    }

    public void assertAttributeStatements(@Nullable final List<AttributeStatement> attributeStatements) {
        Assert.assertNotNull(attributeStatements);
        Assert.assertFalse(attributeStatements.isEmpty());
        Assert.assertEquals(attributeStatements.size(), 1);
        Assert.assertNotNull(attributeStatements.get(0));
    }

    public void assertAttributeStatement(@Nullable final AttributeStatement attributeStatement) {
        Assert.assertNotNull(attributeStatement);
        Assert.assertNotNull(attributeStatement.getSubject());
        Assert.assertNotNull(attributeStatement.getAttributes());
    }

    public void assertSubject(@Nullable final Subject subject) {
        Assert.assertNotNull(subject);
        Assert.assertNotNull(subject.getNameIdentifier());
        Assert.assertNotNull(subject.getSubjectConfirmation());
    }

    public void assertNameIdentifier(@Nullable final NameIdentifier nameIdentifier) {
        Assert.assertNotNull(nameIdentifier);
        Assert.assertNotNull(nameIdentifier.getNameIdentifier());
        Assert.assertEquals(nameIdentifier.getFormat(), SAMLConstants.SAML1_NAMEID_TRANSIENT);
        Assert.assertEquals(nameIdentifier.getNameQualifier(), "https://idp.example.org");
    }

    public void assertSubjectConfirmation(@Nullable final SubjectConfirmation subjectConfirmation) {
        Assert.assertNotNull(subjectConfirmation);
        Assert.assertEquals(subjectConfirmation.getConfirmationMethods().size(), 1);
    }

    public void assertConfirmationMethods(@Nullable final List<ConfirmationMethod> confirmationMethods) {
        Assert.assertNotNull(confirmationMethods);
        Assert.assertFalse(confirmationMethods.isEmpty());
        Assert.assertEquals(confirmationMethods.size(), 1);
        Assert.assertNotNull(confirmationMethods.get(0));

        assertConfirmationMethod(confirmationMethods.get(0));
    }

    public void assertConfirmationMethod(@Nullable final ConfirmationMethod confirmationMethod) {
        Assert.assertNotNull(confirmationMethod);
    }

    public void assertAttributes(@Nullable final List<Attribute> attributes) {
        Assert.assertNotNull(attributes);
        Assert.assertFalse(attributes.isEmpty());
        Assert.assertEquals(attributes.size(), 2);

        assertAttribute(attributes.get(0), "urn:mace:dir:attribute-def:eduPersonAffiliation", "member");
        assertAttribute(attributes.get(1), "urn:mace:dir:attribute-def:mail", "jdoe@shibboleth.net");
    }

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
