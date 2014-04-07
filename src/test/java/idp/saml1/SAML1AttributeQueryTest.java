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

import java.security.cert.X509Certificate;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.ext.spring.factory.X509CertificateFactoryBean;
import net.shibboleth.idp.saml.xml.SAMLConstants;
import net.shibboleth.utilities.java.support.xml.ParserPool;
import net.shibboleth.utilities.java.support.xml.SerializeSupport;

import org.joda.time.DateTime;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.MarshallerFactory;
import org.opensaml.core.xml.io.UnmarshallerFactory;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.saml1.core.Assertion;
import org.opensaml.saml.saml1.core.Attribute;
import org.opensaml.saml.saml1.core.AttributeStatement;
import org.opensaml.saml.saml1.core.Audience;
import org.opensaml.saml.saml1.core.AudienceRestrictionCondition;
import org.opensaml.saml.saml1.core.Conditions;
import org.opensaml.saml.saml1.core.ConfirmationMethod;
import org.opensaml.saml.saml1.core.NameIdentifier;
import org.opensaml.saml.saml1.core.Request;
import org.opensaml.saml.saml1.core.Response;
import org.opensaml.saml.saml1.core.StatusCode;
import org.opensaml.saml.saml1.core.Subject;
import org.opensaml.saml.saml1.core.SubjectConfirmation;
import org.opensaml.saml.saml1.profile.SAML1ActionTestingSupport;
import org.opensaml.security.messaging.ServletRequestX509CredentialAdapter;
import org.opensaml.soap.soap11.Body;
import org.opensaml.soap.soap11.Envelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.webflow.execution.FlowExecutionOutcome;
import org.springframework.webflow.executor.FlowExecutionResult;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Test for SAML 1 attribute query flow.
 */
@ContextConfiguration({"/system/conf/testbed-beans.xml", "file:src/main/webapp/WEB-INF/idp/testbed.xml"})
public class SAML1AttributeQueryTest extends AbstractFlowTest {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(SAML1AttributeQueryTest.class);

    /** Flow id. */
    @Nonnull public final static String FLOW_ID = "SAML1/AttributeQuery";

    // TODO Probably move XMLObject support classes to parent class.
    /** Parser pool */
    protected static ParserPool parserPool;

    /** XMLObject builder factory */
    protected static XMLObjectBuilderFactory builderFactory;

    /** XMLObject marshaller factory */
    protected static MarshallerFactory marshallerFactory;

    /** XMLObject unmarshaller factory */
    protected static UnmarshallerFactory unmarshallerFactory;

    // TODO Retrieve SP certificate from metadata
    @Autowired @Qualifier("testbed.sp.X509Certificate") private X509CertificateFactoryBean certFactoryBean;

    @BeforeClass public void initXMLObjectSupport() {
        parserPool = XMLObjectProviderRegistrySupport.getParserPool();
        builderFactory = XMLObjectProviderRegistrySupport.getBuilderFactory();
        marshallerFactory = XMLObjectProviderRegistrySupport.getMarshallerFactory();
        unmarshallerFactory = XMLObjectProviderRegistrySupport.getUnmarshallerFactory();
    }

    @Nonnull public Envelope buildSOAP11Envelope(@Nonnull final XMLObject payload) {
        final Envelope envelope =
                (Envelope) builderFactory.getBuilder(Envelope.DEFAULT_ELEMENT_NAME).buildObject(
                        Envelope.DEFAULT_ELEMENT_NAME);
        final Body body =
                (Body) builderFactory.getBuilder(Body.DEFAULT_ELEMENT_NAME).buildObject(Body.DEFAULT_ELEMENT_NAME);

        body.getUnknownXMLObjects().add(payload);
        envelope.setBody(body);

        return envelope;
    }

    public void buildRequest() throws Exception {
        final Subject subject = SAML1ActionTestingSupport.buildSubject("jdoe");

        final Request attributeQuery = SAML1ActionTestingSupport.buildAttributeQueryRequest(subject);
        attributeQuery.setIssueInstant(new DateTime());
        attributeQuery.getAttributeQuery().setResource("https://sp.example.org");
        attributeQuery.setID("TESTID");

        final Envelope envelope = buildSOAP11Envelope(attributeQuery);

        final String requestContent =
                SerializeSupport.nodeToString(marshallerFactory.getMarshaller(envelope).marshall(envelope,
                        parserPool.newDocument()));

        request.setMethod("POST");
        request.setAttribute(ServletRequestX509CredentialAdapter.X509_CERT_REQUEST_ATTRIBUTE,
                new X509Certificate[] {certFactoryBean.getObject()});
        request.setContent(requestContent.getBytes("UTF-8"));
    }

    @Test public void testFlow() throws Exception {

        buildRequest();

        final FlowExecutionResult result = flowExecutor.launchExecution(FLOW_ID, null, externalContext);

        asserts(result);
    }

    public void asserts(@Nullable final FlowExecutionResult result) {

        // TODO Probably move asserts to parent class.

        assertFlowExecutionResult(result);

        final FlowExecutionOutcome outcome = result.getOutcome();
        assertFlowExecutionOutcome(outcome);

        final ProfileRequestContext prc = (ProfileRequestContext) outcome.getOutput().get(OUTPUT_ATTR_NAME);
        assertProfileRequestContext(prc);

        final Response response = (Response) prc.getOutboundMessageContext().getMessage();
        assertResponse(response);

        final Assertion assertion = response.getAssertions().get(0);
        assertAssertion(assertion);

        final Conditions conditions = assertion.getConditions();
        assertConditions(conditions);

        final List<AudienceRestrictionCondition> audienceRestrictionCondition =
                conditions.getAudienceRestrictionConditions();
        assertAudienceRestrictionCondition(audienceRestrictionCondition);

        final List<Audience> audiences = audienceRestrictionCondition.get(0).getAudiences();
        assertAudiences(audiences);

        final AttributeStatement attributeStatement = assertion.getAttributeStatements().get(0);
        assertAttributeStatement(attributeStatement);

        final Subject resultSubject = attributeStatement.getSubject();
        assertSubject(resultSubject);

        final NameIdentifier nameIdentifier = resultSubject.getNameIdentifier();
        assertNameIdentifier(nameIdentifier);

        final SubjectConfirmation subjectConfirmation = resultSubject.getSubjectConfirmation();
        assertSubjectConfirmation(subjectConfirmation);

        final List<Attribute> attributes = attributeStatement.getAttributes();
        assertAttributes(attributes);

        // TODO signing, etc.
    }

    public void assertFlowExecutionResult(@Nonnull final FlowExecutionResult result) {
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getFlowId(), FLOW_ID);
        Assert.assertTrue(result.isEnded());
    }

    public void assertFlowExecutionOutcome(@Nonnull final FlowExecutionOutcome outcome) {
        Assert.assertNotNull(outcome, "Flow ended with an error");
        Assert.assertEquals(outcome.getId(), "end");
        Assert.assertTrue(outcome.getOutput().contains(OUTPUT_ATTR_NAME));
        Assert.assertTrue(outcome.getOutput().get(OUTPUT_ATTR_NAME) instanceof ProfileRequestContext);
    }

    public void assertProfileRequestContext(@Nonnull final ProfileRequestContext prc) {
        Assert.assertNotNull(prc);
        Assert.assertNotNull(prc.getOutboundMessageContext());
        Assert.assertNotNull(prc.getOutboundMessageContext().getMessage());
        Assert.assertTrue(prc.getOutboundMessageContext().getMessage() instanceof Response);
    }

    public void assertResponse(@Nonnull final Response response) {
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getInResponseTo(), "TESTID");
        Assert.assertEquals(response.getVersion(), SAMLVersion.VERSION_11);
        Assert.assertEquals(response.getStatus().getStatusCode().getValue(), StatusCode.SUCCESS);
        Assert.assertNotNull(response.getAssertions());
        Assert.assertFalse(response.getAssertions().isEmpty());
        Assert.assertEquals(response.getAssertions().size(), 1);
        Assert.assertNotNull(response.getAssertions().get(0));
        // TODO response.getIssueInstant()
    }

    public void assertAssertion(@Nonnull final Assertion assertion) {
        Assert.assertNotNull(assertion);
        Assert.assertEquals(assertion.getMajorVersion(), SAMLVersion.VERSION_11.getMajorVersion());
        Assert.assertEquals(assertion.getMinorVersion(), SAMLVersion.VERSION_11.getMinorVersion());
        Assert.assertEquals(assertion.getIssuer(), "https://idp.example.org");
        // TODO assertion.getIssueInstant()

        Assert.assertTrue(assertion.getAuthenticationStatements().isEmpty());

        Assert.assertNotNull(assertion.getAttributeStatements());
        Assert.assertFalse(assertion.getAttributeStatements().isEmpty());
        Assert.assertEquals(assertion.getAttributeStatements().size(), 1);
        Assert.assertNotNull(assertion.getAttributeStatements().get(0));
    }

    public void assertAudienceRestrictionCondition(
            @Nullable final List<AudienceRestrictionCondition> audienceRestrictionConditions) {
        Assert.assertNotNull(audienceRestrictionConditions);
        Assert.assertEquals(audienceRestrictionConditions.size(), 1);
    }

    public void assertAudiences(@Nullable final List<Audience> audiences) {
        Assert.assertNotNull(audiences);
        Assert.assertEquals(audiences.size(), 1);
        Assert.assertEquals(audiences.get(0).getUri(), "https://sp.example.org");
    }

    public void assertConditions(@Nonnull final Conditions conditions) {
        Assert.assertNotNull(conditions);
        Assert.assertNotNull(conditions.getNotBefore());
        Assert.assertNotNull(conditions.getNotOnOrAfter());
        // TODO check time via some range ?
        Assert.assertNotNull(conditions.getAudienceRestrictionConditions());
        Assert.assertEquals(conditions.getAudienceRestrictionConditions().size(), 1);
    }

    public void assertAttributeStatement(@Nonnull final AttributeStatement attrStmt) {
        Assert.assertNotNull(attrStmt);
        Assert.assertNotNull(attrStmt.getSubject());
        Assert.assertNotNull(attrStmt.getAttributes());
    }

    public void assertSubject(@Nonnull final Subject subject) {
        Assert.assertNotNull(subject);
        Assert.assertNotNull(subject.getNameIdentifier());
        Assert.assertNotNull(subject.getSubjectConfirmation());
    }

    public void assertNameIdentifier(@Nonnull final NameIdentifier nameIdentifier) {
        Assert.assertNotNull(nameIdentifier);
        Assert.assertEquals(nameIdentifier.getNameIdentifier(), "jdoe");
        // TODO format, qualifier
    }

    public void assertSubjectConfirmation(@Nonnull final SubjectConfirmation subjectConfirmation) {
        Assert.assertNotNull(subjectConfirmation);
        Assert.assertEquals(subjectConfirmation.getConfirmationMethods().size(), 1);
        Assert.assertEquals(subjectConfirmation.getConfirmationMethods().get(0).getConfirmationMethod(),
                ConfirmationMethod.METHOD_SENDER_VOUCHES);
    }

    public void assertAttributes(@Nonnull final List<Attribute> attributes) {
        Assert.assertNotNull(attributes);
        Assert.assertFalse(attributes.isEmpty());
        Assert.assertEquals(attributes.size(), 2);
        assertAttribute(attributes.get(0), "urn:mace:dir:attribute-def:eduPersonAffiliation", "member");
        assertAttribute(attributes.get(1), "urn:mace:dir:attribute-def:mail", "jdoe@shibboleth.net");
    }

    public void assertAttribute(@Nonnull final Attribute attribute, @Nonnull final String attributeName,
            @Nonnull final String attributeValue) {
        Assert.assertNotNull(attribute);
        Assert.assertEquals(attribute.getAttributeName(), attributeName);
        Assert.assertEquals(attribute.getAttributeNamespace(), SAMLConstants.SAML1_ATTR_NAMESPACE_URI);
        Assert.assertEquals(attribute.getAttributeValues().size(), 1);
        Assert.assertTrue(attribute.getAttributeValues().get(0) instanceof XSString);
        Assert.assertEquals(((XSString) attribute.getAttributeValues().get(0)).getValue(), attributeValue);
    }
}
