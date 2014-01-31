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

import java.net.MalformedURLException;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.shibboleth.utilities.java.support.collection.Pair;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.net.UrlBuilder;
import net.shibboleth.utilities.java.support.security.IdentifierGenerationStrategy;

import org.joda.time.DateTime;
import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.encoder.MessageEncodingException;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.common.messaging.context.SAMLEndpointContext;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.binding.encoding.HTTPRedirectDeflateEncoder;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.NameIDPolicy;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.metadata.SingleSignOnService;
import org.opensaml.security.credential.Credential;
import org.opensaml.xmlsec.SignatureSigningParameters;
import org.opensaml.xmlsec.messaging.SecurityParametersContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.webflow.execution.FlowExecutionOutcome;
import org.springframework.webflow.executor.FlowExecutionResult;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Work-in-progress test for SAML2 Unsolicited flow.
 */
@ContextConfiguration({"/system/conf/testbed-beans.xml", "file:src/main/webapp/WEB-INF/idp/testbed.xml",
        "file:src/main/webapp/WEB-INF/sp/testbed.xml"})
public class SAML2RedirectSSOFlowTest extends AbstractFlowTest {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(SAML2RedirectSSOFlowTest.class);

    /** Flow id. */
    @Nonnull public final static String FLOW_ID = "SAML2/Redirect/SSO";

    @Autowired private XMLObjectBuilderFactory builderFactory;

    @Autowired @Qualifier("testbed.IdGenerator") private IdentifierGenerationStrategy idGenerator;

    @Qualifier("sp.Credential") @Autowired private Credential spCredential;

    @Test public void testFlow() throws Exception {

        // setup, mostly wrong
        AuthnRequest authnRequest = buildAuthnRequest(request);

        authnRequest.setDestination(getDestinationRedirect(request));

        request.setRequestURI("/idp/SAML2/Redirect/SSO");

        MessageContext<SAMLObject> messageContext =
                buildOutboundMessageContext(authnRequest, SAMLConstants.SAML2_REDIRECT_BINDING_URI);

        encodeOutboundMessageContextRedirect(messageContext, response);

        UrlBuilder urlBuilder = new UrlBuilder(response.getRedirectedUrl());
        for (Pair<String, String> param : urlBuilder.getQueryParams()) {
            if (param.getFirst().equals("SAMLRequest")) {
                request.addParameter("SAMLRequest", param.getSecond());
            }
        }

        // execute the flow
        FlowExecutionResult result = flowExecutor.launchExecution(FLOW_ID, null, externalContext);

        // asserts
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
        Assert.assertEquals(eduPersonAffiliation.getNameFormat(), "urn:oasis:names:tc:SAML:2.0:attrname-format:uri");
        Assert.assertEquals(eduPersonAffiliation.getFriendlyName(), "eduPersonAffiliation");
        Assert.assertEquals(eduPersonAffiliation.getAttributeValues().size(), 1);
        Assert.assertTrue(eduPersonAffiliation.getAttributeValues().get(0) instanceof XSString);
        Assert.assertEquals(((XSString) eduPersonAffiliation.getAttributeValues().get(0)).getValue(), "member");

        Attribute mail = attributeStatement.getAttributes().get(1);
        Assert.assertEquals(mail.getName(), "urn:oid:0.9.2342.19200300.100.1.3");
        Assert.assertEquals(mail.getNameFormat(), "urn:oasis:names:tc:SAML:2.0:attrname-format:uri");
        Assert.assertEquals(mail.getFriendlyName(), "mail");
        Assert.assertEquals(mail.getAttributeValues().size(), 1);
        Assert.assertTrue(mail.getAttributeValues().get(0) instanceof XSString);
        Assert.assertEquals(((XSString) mail.getAttributeValues().get(0)).getValue(), "jdoe@shibboleth.net");

        // TODO meaningful asserts
    }

    private String getDestinationRedirect(HttpServletRequest servletRequest) {
        // TODO servlet context
        String destinationPath = "/idp/SAML2/Redirect/SSO";
        String baseUrl = getBaseUrl(servletRequest);
        try {
            UrlBuilder urlBuilder = new UrlBuilder(baseUrl);
            urlBuilder.setPath(destinationPath);
            log.debug("set des redi {}", urlBuilder.buildURL());
            return urlBuilder.buildURL();
        } catch (MalformedURLException e) {
            log.error("Couldn't parse base URL, reverting to internal default destination: {}", baseUrl);
            return "http://localhost:8080" + destinationPath;
        }
    }

    private AuthnRequest buildAuthnRequest(HttpServletRequest servletRequest) {
        AuthnRequest authnRequest =
                (AuthnRequest) builderFactory.getBuilder(AuthnRequest.DEFAULT_ELEMENT_NAME).buildObject(
                        AuthnRequest.DEFAULT_ELEMENT_NAME);

        authnRequest.setID(idGenerator.generateIdentifier());
        authnRequest.setIssueInstant(new DateTime());
        authnRequest.setAssertionConsumerServiceURL(getAcsUrl(servletRequest));
        authnRequest.setProtocolBinding(SAMLConstants.SAML2_POST_BINDING_URI);

        Issuer issuer =
                (Issuer) builderFactory.getBuilder(Issuer.DEFAULT_ELEMENT_NAME)
                        .buildObject(Issuer.DEFAULT_ELEMENT_NAME);
        issuer.setValue(getSpEntityId());
        authnRequest.setIssuer(issuer);

        NameIDPolicy nameIDPolicy =
                (NameIDPolicy) builderFactory.getBuilder(NameIDPolicy.DEFAULT_ELEMENT_NAME).buildObject(
                        NameIDPolicy.DEFAULT_ELEMENT_NAME);
        nameIDPolicy.setAllowCreate(true);
        authnRequest.setNameIDPolicy(nameIDPolicy);

        return authnRequest;
    }

    private String getAcsUrl(HttpServletRequest servletRequest) {
        // TODO servlet context
        String acsPath = "/sp/SAML2/POST/ACS";
        String baseUrl = getBaseUrl(servletRequest);
        try {
            UrlBuilder urlBuilder = new UrlBuilder(baseUrl);
            urlBuilder.setPath(acsPath);
            return urlBuilder.buildURL();
        } catch (MalformedURLException e) {
            log.error("Couldn't parse base URL, reverting to internal default ACS: {}", baseUrl);
            return "http://localhost:8080" + acsPath;
        }
    }

    private SingleSignOnService buildIdpSsoEndpoint(String binding, String destination) {
        SingleSignOnService ssoEndpoint =
                (SingleSignOnService) builderFactory.getBuilder(SingleSignOnService.DEFAULT_ELEMENT_NAME).buildObject(
                        SingleSignOnService.DEFAULT_ELEMENT_NAME);
        ssoEndpoint.setBinding(binding);
        ssoEndpoint.setLocation(destination);
        return ssoEndpoint;
    }

    private String getBaseUrl(HttpServletRequest servletRequest) {
        // TODO servlet context
        String requestUrl = servletRequest.getRequestURL().toString();
        try {
            UrlBuilder urlBuilder = new UrlBuilder(requestUrl);
            urlBuilder.setUsername(null);
            urlBuilder.setPassword(null);
            urlBuilder.setPath(null);
            urlBuilder.getQueryParams().clear();
            urlBuilder.setFragment(null);
            return urlBuilder.buildURL();
        } catch (MalformedURLException e) {
            log.error("Couldn't parse request URL, reverting to internal default base URL: {}", requestUrl);
            return "http://localhost:8080";
        }

    }

    private MessageContext<SAMLObject> buildOutboundMessageContext(AuthnRequest authnRequest, String bindingUri) {
        MessageContext<SAMLObject> messageContext = new MessageContext<>();
        messageContext.setMessage(authnRequest);

        SAMLPeerEntityContext peerContext = messageContext.getSubcontext(SAMLPeerEntityContext.class, true);
        peerContext.setEntityId(getIdpEntityId());

        SAMLEndpointContext endpointContext = peerContext.getSubcontext(SAMLEndpointContext.class, true);
        endpointContext.setEndpoint(buildIdpSsoEndpoint(bindingUri, authnRequest.getDestination()));

        SignatureSigningParameters signingParameters = new SignatureSigningParameters();
        signingParameters.setSigningCredential(spCredential);
        SecurityParametersContext secParamsContext =
                messageContext.getSubcontext(SecurityParametersContext.class, true);
        secParamsContext.setSignatureSigningParameters(signingParameters);

        return messageContext;
    }

    private void encodeOutboundMessageContextRedirect(MessageContext<SAMLObject> messageContext,
            HttpServletResponse servletResponse) throws Exception {
        HTTPRedirectDeflateEncoder encoder = new HTTPRedirectDeflateEncoder();
        try {

            encoder.setHttpServletResponse(servletResponse);
            encoder.setMessageContext(messageContext);
            encoder.initialize();

            encoder.prepareContext();
            encoder.encode();
        } catch (ComponentInitializationException | MessageEncodingException e) {
            log.error("Error encoding the outbound message context", e);
            throw e;
        } finally {
            encoder.destroy();
        }
    }

    private String getSpEntityId() {
        // TODO get from config somewhere
        return "https://sp.example.org";
    }

    private String getIdpEntityId() {
        // TODO get from config somewhere
        return "https://idp.example.org";
    }

}
