package sp;

import java.net.MalformedURLException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.net.UrlBuilder;
import net.shibboleth.utilities.java.support.xml.SerializeSupport;

import org.joda.time.DateTime;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.encoder.MessageEncodingException;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.common.messaging.SAMLMessageSecuritySupport;
import org.opensaml.saml.common.messaging.context.SAMLEndpointContext;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.binding.encoding.impl.HTTPPostEncoder;
import org.opensaml.saml.saml2.binding.encoding.impl.HTTPRedirectDeflateEncoder;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.NameIDPolicy;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.metadata.SingleSignOnService;
import org.opensaml.xmlsec.SignatureSigningParameters;
import org.opensaml.xmlsec.context.SecurityParametersContext;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.w3c.dom.Element;

@Controller
@RequestMapping("/SAML2")
public class Saml2Controller extends BaseSAMLController {
	
	private Logger log = LoggerFactory.getLogger(Saml2Controller.class);

	@RequestMapping(value="/InitSSO/Redirect", method=RequestMethod.GET)
	public void initSSORequestRedirect(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws Exception {
		AuthnRequest authnRequest = buildAuthnRequest(servletRequest);
		authnRequest.setDestination(getDestinationRedirect(servletRequest));
		MessageContext<SAMLObject> messageContext = buildOutboundMessageContext(authnRequest, SAMLConstants.SAML2_REDIRECT_BINDING_URI);
		encodeOutboundMessageContextRedirect(messageContext, servletResponse);
	}

	@RequestMapping(value="/InitSSO/POST", method=RequestMethod.GET)
	public void initSsoRequestPost(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws Exception {
		AuthnRequest authnRequest = buildAuthnRequest(servletRequest);
		authnRequest.setDestination(getDestinationPost(servletRequest));
		MessageContext<SAMLObject> messageContext = buildOutboundMessageContext(authnRequest, SAMLConstants.SAML2_POST_BINDING_URI);
		SAMLMessageSecuritySupport.signMessage(messageContext);
		encodeOutboundMessageContextPost(messageContext, servletResponse);
	}

    @RequestMapping(value="/InitSSO/Passive", method=RequestMethod.GET)
    public void initSSORequestPassive(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws Exception {
        AuthnRequest authnRequest = buildAuthnRequest(servletRequest);
        authnRequest.setDestination(getDestinationRedirect(servletRequest));
        authnRequest.setIsPassive(true);
        MessageContext<SAMLObject> messageContext = buildOutboundMessageContext(authnRequest, SAMLConstants.SAML2_REDIRECT_BINDING_URI);
        encodeOutboundMessageContextRedirect(messageContext, servletResponse);
    }

    @RequestMapping(value="/InitSSO/ForceAuthn", method=RequestMethod.GET)
    public void initSSORequestForceAuthn(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws Exception {
        AuthnRequest authnRequest = buildAuthnRequest(servletRequest);
        authnRequest.setDestination(getDestinationRedirect(servletRequest));
        authnRequest.setForceAuthn(true);
        MessageContext<SAMLObject> messageContext = buildOutboundMessageContext(authnRequest, SAMLConstants.SAML2_REDIRECT_BINDING_URI);
        encodeOutboundMessageContextRedirect(messageContext, servletResponse);
    }
    
	@RequestMapping(value="/POST/ACS", method=RequestMethod.POST)
	public ResponseEntity<String> handleSSOResponsePOST(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws Exception {
		MessageContext<SAMLObject> messageContext = decodeInboundMessageContextPost(servletRequest);
		
		if (!(messageContext.getMessage() instanceof Response)) {
			log.error("Inbound message was not a SAML 2 Response");
			return new ResponseEntity<String>("Inbound message was not a SAML 2 Response", HttpStatus.BAD_REQUEST);
		}
		
		Response response = (Response) messageContext.getMessage();
		Element responseElement = response.getDOM();
		String formattedMessage = SerializeSupport.prettyPrintXML(responseElement);
		
		//TODO instead of returning plain text via a ResponseEntity, add a JSP view that looks good
		
		HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Type", "text/plain");
		
		return new ResponseEntity<String>(formattedMessage, headers, HttpStatus.OK);
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
		signingParameters.setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256);
		//signingParameters.setSignatureReferenceDigestMethod(SignatureConstants.ALGO_ID_DIGEST_SHA256);
		signingParameters.setSignatureCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
		SecurityParametersContext secParamsContext = messageContext.getSubcontext(SecurityParametersContext.class, true);
		secParamsContext.setSignatureSigningParameters(signingParameters);
		
		return messageContext;
	}
	
	private void encodeOutboundMessageContextRedirect(MessageContext<SAMLObject> messageContext, HttpServletResponse servletResponse) throws Exception {
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
	
	private void encodeOutboundMessageContextPost(MessageContext<SAMLObject> messageContext, HttpServletResponse servletResponse) throws Exception {
		HTTPPostEncoder encoder = new HTTPPostEncoder();
		try {
			encoder.setHttpServletResponse(servletResponse);
			encoder.setMessageContext(messageContext);
			encoder.setVelocityEngine(velocityEngine);
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
	
	private SingleSignOnService buildIdpSsoEndpoint(String binding, String destination) {
		SingleSignOnService ssoEndpoint = (SingleSignOnService) builderFactory.getBuilder(SingleSignOnService.DEFAULT_ELEMENT_NAME).buildObject(SingleSignOnService.DEFAULT_ELEMENT_NAME);
		ssoEndpoint.setBinding(binding);
		ssoEndpoint.setLocation(destination);
		return ssoEndpoint;
	}

	private AuthnRequest buildAuthnRequest(HttpServletRequest servletRequest) {
		AuthnRequest authnRequest = (AuthnRequest) builderFactory.getBuilder(AuthnRequest.DEFAULT_ELEMENT_NAME).buildObject(AuthnRequest.DEFAULT_ELEMENT_NAME);
		
		authnRequest.setID(idGenerator.generateIdentifier());
		authnRequest.setIssueInstant(new DateTime());
		authnRequest.setAssertionConsumerServiceURL(getAcsUrl(servletRequest));
		authnRequest.setProtocolBinding(SAMLConstants.SAML2_POST_BINDING_URI);
		
		Issuer issuer = (Issuer) builderFactory.getBuilder(Issuer.DEFAULT_ELEMENT_NAME).buildObject(Issuer.DEFAULT_ELEMENT_NAME);
		issuer.setValue(getSpEntityId());
		authnRequest.setIssuer(issuer);
		
		NameIDPolicy nameIDPolicy = (NameIDPolicy) builderFactory.getBuilder(NameIDPolicy.DEFAULT_ELEMENT_NAME).buildObject(NameIDPolicy.DEFAULT_ELEMENT_NAME);
		nameIDPolicy.setAllowCreate(true);
		authnRequest.setNameIDPolicy(nameIDPolicy);
		
		return authnRequest;
	}

	private String getDestinationRedirect(HttpServletRequest servletRequest) {
		//TODO servlet context
		String destinationPath = "/idp/profile/SAML2/Redirect/SSO";
		String baseUrl = getBaseUrl(servletRequest);
		try {
			UrlBuilder urlBuilder = new UrlBuilder(baseUrl);
			urlBuilder.setPath(destinationPath);
			return urlBuilder.buildURL();
		} catch (MalformedURLException e) {
			log.error("Couldn't parse base URL, reverting to internal default destination: {}", baseUrl);
			return "http://localhost:8080" + destinationPath;
		}
	}
	
	private String getDestinationPost(HttpServletRequest servletRequest) {
		//TODO servlet context
		String destinationPath = "/idp/profile/SAML2/POST/SSO";
		String baseUrl = getBaseUrl(servletRequest);
		try {
			UrlBuilder urlBuilder = new UrlBuilder(baseUrl);
			urlBuilder.setPath(destinationPath);
			return urlBuilder.buildURL();
		} catch (MalformedURLException e) {
			log.error("Couldn't parse base URL, reverting to internal default destination: {}", baseUrl);
			return "http://localhost:8080" + destinationPath;
		}
	}

	private String getAcsUrl(HttpServletRequest servletRequest) {
		//TODO servlet context
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
	
	private String getBaseUrl(HttpServletRequest servletRequest) {
		//TODO servlet context
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

	private String getSpEntityId() {
		//TODO get from config somewhere
		return "https://sp.example.org";
	}
	
	private String getIdpEntityId() {
		//TODO get from config somewhere
		return "https://idp.example.org";
	}

}
