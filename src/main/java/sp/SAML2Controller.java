package sp;

import java.net.MalformedURLException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.net.URLBuilder;
import net.shibboleth.utilities.java.support.xml.SerializeSupport;

import org.joda.time.DateTime;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.encoder.MessageEncodingException;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.common.messaging.SAMLMessageSecuritySupport;
import org.opensaml.saml.common.messaging.context.SAMLEndpointContext;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.ext.saml2aslo.Asynchronous;
import org.opensaml.saml.saml2.binding.encoding.impl.HTTPPostEncoder;
import org.opensaml.saml.saml2.binding.encoding.impl.HTTPRedirectDeflateEncoder;
import org.opensaml.saml.saml2.core.Extensions;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.LogoutRequest;
import org.opensaml.saml.saml2.core.LogoutResponse;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.NameIDPolicy;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.metadata.Endpoint;
import org.opensaml.saml.saml2.metadata.SingleLogoutService;
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
public class SAML2Controller extends BaseSAMLController {
	
	private Logger log = LoggerFactory.getLogger(SAML2Controller.class);

	@RequestMapping(value="/InitSSO/Redirect", method=RequestMethod.GET)
	public void initSSORequestRedirect(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws Exception {
		AuthnRequest authnRequest = buildAuthnRequest(servletRequest);
		authnRequest.setDestination(getDestinationRedirect(servletRequest, "SSO"));
		MessageContext<SAMLObject> messageContext = buildOutboundMessageContext(authnRequest,
		        buildIdpSsoEndpoint(SAMLConstants.SAML2_REDIRECT_BINDING_URI, authnRequest.getDestination()));
		encodeOutboundMessageContextRedirect(messageContext, servletResponse);
	}

	@RequestMapping(value="/InitSSO/POST", method=RequestMethod.GET)
	public void initSsoRequestPost(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws Exception {
		AuthnRequest authnRequest = buildAuthnRequest(servletRequest);
		authnRequest.setDestination(getDestinationPost(servletRequest, "SSO"));
		MessageContext<SAMLObject> messageContext = buildOutboundMessageContext(authnRequest,
		        buildIdpSsoEndpoint(SAMLConstants.SAML2_POST_BINDING_URI, authnRequest.getDestination()));
		SAMLMessageSecuritySupport.signMessage(messageContext);
		encodeOutboundMessageContextPost(messageContext, servletResponse);
	}

    @RequestMapping(value="/InitSSO/Passive", method=RequestMethod.GET)
    public void initSSORequestPassive(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws Exception {
        AuthnRequest authnRequest = buildAuthnRequest(servletRequest);
        authnRequest.setDestination(getDestinationRedirect(servletRequest, "SSO"));
        authnRequest.setIsPassive(true);
        MessageContext<SAMLObject> messageContext = buildOutboundMessageContext(authnRequest,
                buildIdpSsoEndpoint(SAMLConstants.SAML2_REDIRECT_BINDING_URI, authnRequest.getDestination()));
        encodeOutboundMessageContextRedirect(messageContext, servletResponse);
    }

    @RequestMapping(value="/InitSSO/ForceAuthn", method=RequestMethod.GET)
    public void initSSORequestForceAuthn(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws Exception {
        AuthnRequest authnRequest = buildAuthnRequest(servletRequest);
        authnRequest.setDestination(getDestinationRedirect(servletRequest, "SSO"));
        authnRequest.setForceAuthn(true);
        MessageContext<SAMLObject> messageContext = buildOutboundMessageContext(authnRequest,
                buildIdpSsoEndpoint(SAMLConstants.SAML2_REDIRECT_BINDING_URI, authnRequest.getDestination()));
        encodeOutboundMessageContextRedirect(messageContext, servletResponse);
    }
    
    @RequestMapping(value="/InitSLO/Redirect", method=RequestMethod.GET)
    public void initSLORequestRedirect(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws Exception {
        LogoutRequest logoutRequest = buildLogoutRequest(servletRequest);
        logoutRequest.setDestination(getDestinationRedirect(servletRequest, "SLO"));
        MessageContext<SAMLObject> messageContext = buildOutboundMessageContext(logoutRequest,
                buildIdpSloEndpoint(SAMLConstants.SAML2_REDIRECT_BINDING_URI, logoutRequest.getDestination()));
        encodeOutboundMessageContextRedirect(messageContext, servletResponse);
    }

    @RequestMapping(value="/InitSLO/Async", method=RequestMethod.GET)
    public void initSLORequestAsync(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws Exception {
        LogoutRequest logoutRequest = buildLogoutRequest(servletRequest);
        logoutRequest.setDestination(getDestinationRedirect(servletRequest, "SLO"));
        
        final Extensions exts = (Extensions) builderFactory.getBuilder(Extensions.DEFAULT_ELEMENT_NAME)
                .buildObject(Extensions.DEFAULT_ELEMENT_NAME);
        logoutRequest.setExtensions(exts);
        exts.getUnknownXMLObjects().add(
                builderFactory.getBuilder(Asynchronous.DEFAULT_ELEMENT_NAME).buildObject(Asynchronous.DEFAULT_ELEMENT_NAME));
        
        MessageContext<SAMLObject> messageContext = buildOutboundMessageContext(logoutRequest,
                buildIdpSloEndpoint(SAMLConstants.SAML2_REDIRECT_BINDING_URI, logoutRequest.getDestination()));
        encodeOutboundMessageContextRedirect(messageContext, servletResponse);
    }
    
    @RequestMapping(value="/InitSLO/POST", method=RequestMethod.GET)
    public void initSLORequestPost(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws Exception {
        LogoutRequest logoutRequest = buildLogoutRequest(servletRequest);
        logoutRequest.setDestination(getDestinationPost(servletRequest, "SLO"));
        MessageContext<SAMLObject> messageContext = buildOutboundMessageContext(logoutRequest,
                buildIdpSloEndpoint(SAMLConstants.SAML2_POST_BINDING_URI, logoutRequest.getDestination()));
        SAMLMessageSecuritySupport.signMessage(messageContext);
        encodeOutboundMessageContextPost(messageContext, servletResponse);
    }

    @RequestMapping(value="/FinishSLO/Redirect", method=RequestMethod.GET)
    public void finishSLOResponseRedirect(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws Exception {
        LogoutResponse logoutResponse = buildLogoutResponse(servletRequest);
        logoutResponse.setDestination(getDestinationRedirect(servletRequest, "SLO"));
        MessageContext<SAMLObject> messageContext = buildOutboundMessageContext(logoutResponse,
                buildIdpSloEndpoint(SAMLConstants.SAML2_REDIRECT_BINDING_URI, logoutResponse.getDestination()));
        encodeOutboundMessageContextRedirect(messageContext, servletResponse);
    }

    @RequestMapping(value="/FinishSLO/POST", method=RequestMethod.GET)
    public void finishSLOResponsePost(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws Exception {
        LogoutResponse logoutResponse = buildLogoutResponse(servletRequest);
        logoutResponse.setDestination(getDestinationPost(servletRequest, "SLO"));
        MessageContext<SAMLObject> messageContext = buildOutboundMessageContext(logoutResponse,
                buildIdpSloEndpoint(SAMLConstants.SAML2_POST_BINDING_URI, logoutResponse.getDestination()));
        SAMLMessageSecuritySupport.signMessage(messageContext);
        encodeOutboundMessageContextPost(messageContext, servletResponse);
    }
    
	@RequestMapping(value="/POST/ACS", method=RequestMethod.POST)
	public ResponseEntity<String> handleSSOResponsePOST(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws Exception {
		final MessageContext<SAMLObject> messageContext = decodeInboundMessageContextPost(servletRequest);
		
		if (!(messageContext.getMessage() instanceof Response)) {
			log.error("Inbound message was not a SAML 2 Response");
			return new ResponseEntity<String>("Inbound message was not a SAML 2 Response", HttpStatus.BAD_REQUEST);
		}
		
		final Response response = (Response) messageContext.getMessage();
		final Element responseElement = response.getDOM();
		final String formattedMessage = SerializeSupport.prettyPrintXML(responseElement);
		
		//TODO instead of returning plain text via a ResponseEntity, add a JSP view that looks good
		
		final HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Type", "text/plain");
		
		return new ResponseEntity<String>(formattedMessage, headers, HttpStatus.OK);
	}

    @RequestMapping(value="/Redirect/SLO", method=RequestMethod.GET)
    public ResponseEntity<String> handleSLOResponseRedirect(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws Exception {
        final MessageContext<SAMLObject> messageContext = decodeInboundMessageContextRedirect(servletRequest);
        
        if (!(messageContext.getMessage() instanceof LogoutResponse)) {
            log.error("Inbound message was not a SAML 2 LogoutResponse");
            return new ResponseEntity<String>("Inbound message was not a SAML 2 LogoutResponse", HttpStatus.BAD_REQUEST);
        }
        
        final LogoutResponse response = (LogoutResponse) messageContext.getMessage();
        final Element responseElement = response.getDOM();
        final String formattedMessage = SerializeSupport.prettyPrintXML(responseElement);
        
        //TODO instead of returning plain text via a ResponseEntity, add a JSP view that looks good
        
        final HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "text/plain");
        
        return new ResponseEntity<String>(formattedMessage, headers, HttpStatus.OK);
    }
	
    @RequestMapping(value="/POST/SLO", method=RequestMethod.POST)
    public ResponseEntity<String> handleSLOResponsePOST(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws Exception {
        final MessageContext<SAMLObject> messageContext = decodeInboundMessageContextPost(servletRequest);
        
        if (!(messageContext.getMessage() instanceof LogoutResponse)) {
            log.error("Inbound message was not a SAML 2 LogoutResponse");
            return new ResponseEntity<String>("Inbound message was not a SAML 2 LogoutResponse", HttpStatus.BAD_REQUEST);
        }
        
        final LogoutResponse response = (LogoutResponse) messageContext.getMessage();
        final Element responseElement = response.getDOM();
        final String formattedMessage = SerializeSupport.prettyPrintXML(responseElement);
        
        //TODO instead of returning plain text via a ResponseEntity, add a JSP view that looks good
        
        final HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "text/plain");
        
        return new ResponseEntity<String>(formattedMessage, headers, HttpStatus.OK);
    }
    
	private MessageContext<SAMLObject> buildOutboundMessageContext(SAMLObject message, Endpoint endpoint) {
		MessageContext<SAMLObject> messageContext = new MessageContext<>();
		messageContext.setMessage(message);
		
		SAMLPeerEntityContext peerContext = messageContext.getSubcontext(SAMLPeerEntityContext.class, true);
		peerContext.setEntityId(getIdpEntityId());
		
		SAMLEndpointContext endpointContext = peerContext.getSubcontext(SAMLEndpointContext.class, true);
		endpointContext.setEndpoint(endpoint);
		
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
		final SingleSignOnService ssoEndpoint = (SingleSignOnService) builderFactory.getBuilder(
		        SingleSignOnService.DEFAULT_ELEMENT_NAME).buildObject(SingleSignOnService.DEFAULT_ELEMENT_NAME);
		ssoEndpoint.setBinding(binding);
		ssoEndpoint.setLocation(destination);
		return ssoEndpoint;
	}

    private SingleLogoutService buildIdpSloEndpoint(String binding, String destination) {
        final SingleLogoutService sloEndpoint = (SingleLogoutService) builderFactory.getBuilder(
                SingleLogoutService.DEFAULT_ELEMENT_NAME).buildObject(SingleLogoutService.DEFAULT_ELEMENT_NAME);
        sloEndpoint.setBinding(binding);
        sloEndpoint.setLocation(destination);
        return sloEndpoint;
    }
	
	private AuthnRequest buildAuthnRequest(HttpServletRequest servletRequest) {
		final AuthnRequest authnRequest = (AuthnRequest) builderFactory.getBuilder(
		        AuthnRequest.DEFAULT_ELEMENT_NAME).buildObject(AuthnRequest.DEFAULT_ELEMENT_NAME);
		
		authnRequest.setID(idGenerator.generateIdentifier());
		authnRequest.setIssueInstant(new DateTime());
		authnRequest.setAssertionConsumerServiceURL(getAcsUrl(servletRequest));
		authnRequest.setProtocolBinding(SAMLConstants.SAML2_POST_BINDING_URI);
		
		final Issuer issuer = (Issuer) builderFactory.getBuilder(Issuer.DEFAULT_ELEMENT_NAME).buildObject(Issuer.DEFAULT_ELEMENT_NAME);
		issuer.setValue(getSpEntityId());
		authnRequest.setIssuer(issuer);
		
		final NameIDPolicy nameIDPolicy = (NameIDPolicy) builderFactory.getBuilder(NameIDPolicy.DEFAULT_ELEMENT_NAME).buildObject(NameIDPolicy.DEFAULT_ELEMENT_NAME);
		nameIDPolicy.setAllowCreate(true);
		authnRequest.setNameIDPolicy(nameIDPolicy);
		
		return authnRequest;
	}

    private LogoutRequest buildLogoutRequest(HttpServletRequest servletRequest) {
        final LogoutRequest logoutRequest = (LogoutRequest) builderFactory.getBuilder(
                LogoutRequest.DEFAULT_ELEMENT_NAME).buildObject(LogoutRequest.DEFAULT_ELEMENT_NAME);
        
        logoutRequest.setID(idGenerator.generateIdentifier());
        logoutRequest.setIssueInstant(new DateTime());
        
        final Issuer issuer = (Issuer) builderFactory.getBuilder(Issuer.DEFAULT_ELEMENT_NAME).buildObject(Issuer.DEFAULT_ELEMENT_NAME);
        issuer.setValue(getSpEntityId());
        logoutRequest.setIssuer(issuer);
        
        final NameID nameID = (NameID) builderFactory.getBuilder(NameID.DEFAULT_ELEMENT_NAME).buildObject(NameID.DEFAULT_ELEMENT_NAME);
        nameID.setValue(servletRequest.getParameter("transientID"));
        nameID.setFormat(NameID.TRANSIENT);
        nameID.setSPNameQualifier(getSpEntityId());
        nameID.setNameQualifier(getIdpEntityId());
        logoutRequest.setNameID(nameID);
        
        return logoutRequest;
    }

    private LogoutResponse buildLogoutResponse(HttpServletRequest servletRequest) {
        final LogoutResponse logoutResponse = (LogoutResponse) builderFactory.getBuilder(
                LogoutResponse.DEFAULT_ELEMENT_NAME).buildObject(LogoutResponse.DEFAULT_ELEMENT_NAME);
        
        logoutResponse.setID(idGenerator.generateIdentifier());
        logoutResponse.setIssueInstant(new DateTime());
        
        final Issuer issuer = (Issuer) builderFactory.getBuilder(Issuer.DEFAULT_ELEMENT_NAME).buildObject(Issuer.DEFAULT_ELEMENT_NAME);
        issuer.setValue(getSpEntityId());
        logoutResponse.setIssuer(issuer);
        
        final Status status = (Status) builderFactory.getBuilder(Status.DEFAULT_ELEMENT_NAME).buildObject(Status.DEFAULT_ELEMENT_NAME);
        logoutResponse.setStatus(status);
        
        final StatusCode code = (StatusCode) builderFactory.getBuilder(StatusCode.DEFAULT_ELEMENT_NAME).buildObject(StatusCode.DEFAULT_ELEMENT_NAME);
        status.setStatusCode(code);
        final String param = servletRequest.getParameter("success");
        if (param != null && "1".equals(param)) {
            code.setValue(StatusCode.SUCCESS);
        } else {
            code.setValue(StatusCode.RESPONDER);
        }
        
        return logoutResponse;
    }
    
	private String getDestinationRedirect(HttpServletRequest servletRequest, String profile) {
		//TODO servlet context
		String destinationPath = "/idp/profile/SAML2/Redirect/" + profile;
		String baseUrl = getBaseUrl(servletRequest);
		try {
			URLBuilder urlBuilder = new URLBuilder(baseUrl);
			urlBuilder.setPath(destinationPath);
			return urlBuilder.buildURL();
		} catch (MalformedURLException e) {
			log.error("Couldn't parse base URL, reverting to internal default destination: {}", baseUrl);
			return "http://localhost:8080" + destinationPath;
		}
	}
	
	private String getDestinationPost(HttpServletRequest servletRequest, String profile) {
		//TODO servlet context
		String destinationPath = "/idp/profile/SAML2/POST/" + profile;
		String baseUrl = getBaseUrl(servletRequest);
		try {
			URLBuilder urlBuilder = new URLBuilder(baseUrl);
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
			URLBuilder urlBuilder = new URLBuilder(baseUrl);
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
			URLBuilder urlBuilder = new URLBuilder(requestUrl);
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
