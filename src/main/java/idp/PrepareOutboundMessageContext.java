package idp;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.ActionSupport;

import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.profile.ProfileException;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.common.binding.SAMLBindingSupport;
import org.opensaml.saml.common.messaging.context.SamlBindingContext;
import org.opensaml.saml.common.messaging.context.SamlEndpointContext;
import org.opensaml.saml.common.messaging.context.SamlPeerEntityContext;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.metadata.AssertionConsumerService;
import org.opensaml.saml.saml2.metadata.Endpoint;
import org.opensaml.security.credential.Credential;
import org.opensaml.xmlsec.SecurityConfiguration;
import org.opensaml.xmlsec.SignatureSigningParameters;
import org.opensaml.xmlsec.keyinfo.KeyInfoGenerator;
import org.opensaml.xmlsec.messaging.SecurityParametersContext;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

public class PrepareOutboundMessageContext extends AbstractProfileAction<SAMLObject, SAMLObject> {
	
	@Autowired
	private XMLObjectBuilderFactory builderFactory;
	
	@Autowired
	private Credential signingCredential;
	
	@Autowired
	private SecurityConfiguration globalSecurityConfiguration;

	
	protected Event doExecute(RequestContext springRequestContext, ProfileRequestContext<SAMLObject, SAMLObject> profileRequestContext) 
			throws ProfileException {
		
		addPeerAndEndpointInfo(springRequestContext, profileRequestContext);
		
		addBindingInfo(profileRequestContext);
		
		addSignatureParameters(profileRequestContext);
		
		return ActionSupport.buildProceedEvent(this);
	}
	
	private MessageContext<SAMLObject> addPeerAndEndpointInfo(RequestContext springRequestContext, 
			ProfileRequestContext<SAMLObject, SAMLObject> profileRequestContext) {
		
		// Just harcoding binding here and taking ACS URL direct from inbound message.
		// If for real, we'd use an endpoint selector based on metadata, etc.
		String bindingURI = SAMLConstants.SAML2_POST_BINDING_URI;
		AuthnRequest authnRequest = (AuthnRequest) profileRequestContext.getInboundMessageContext().getMessage();
		String acsURL = authnRequest.getAssertionConsumerServiceURL();
		
		// Set the binding URI in flow scope for testing the transition-based way of doing dynamic message encoder selection.
		springRequestContext.getFlowScope().put("bindingURI", bindingURI);
		
		MessageContext<SAMLObject> messageContext = profileRequestContext.getOutboundMessageContext();
		
		SamlPeerEntityContext peerContext = messageContext.getSubcontext(SamlPeerEntityContext.class, true);
		peerContext.setEntityId(getSpEntityId());
		
		SamlEndpointContext endpointContext = peerContext.getSubcontext(SamlEndpointContext.class, true);
		endpointContext.setEndpoint(buildSpAcsEndpoint(bindingURI, acsURL));
		
		return messageContext;
	}
	
	private AssertionConsumerService buildSpAcsEndpoint(String binding, String destination) {
		AssertionConsumerService acsEndpoint = (AssertionConsumerService) builderFactory.getBuilder(AssertionConsumerService.DEFAULT_ELEMENT_NAME)
				.buildObject(AssertionConsumerService.DEFAULT_ELEMENT_NAME);
		acsEndpoint.setBinding(binding);
		acsEndpoint.setLocation(destination);
		return acsEndpoint;
	}
	
	private void addBindingInfo(ProfileRequestContext<SAMLObject, SAMLObject> profileRequestContext) {
		MessageContext<SAMLObject> messageContext = profileRequestContext.getOutboundMessageContext();
		Endpoint endpoint = messageContext.getSubcontext(SamlPeerEntityContext.class).getSubcontext(SamlEndpointContext.class).getEndpoint();
		String bindingURI = endpoint.getBinding();
		
		SamlBindingContext bindingContext = messageContext.getSubcontext(SamlBindingContext.class, true);
		bindingContext.setBindingUri(bindingURI);
		bindingContext.setRelayState(SAMLBindingSupport.getRelayState(profileRequestContext.getInboundMessageContext()));
	}

	private void addSignatureParameters(ProfileRequestContext<SAMLObject, SAMLObject> profileRequestContext) {
		MessageContext<SAMLObject> messageContext = profileRequestContext.getOutboundMessageContext();
		
		KeyInfoGenerator kiGenerator = globalSecurityConfiguration.getKeyInfoGeneratorManager()
				.getDefaultManager().getFactory(signingCredential).newInstance();
		
        SignatureSigningParameters signingParameters = new SignatureSigningParameters();
        signingParameters.setSigningCredential(signingCredential);
        // We know it's an RSA key, so just hardcoding for now.
        signingParameters.setSignatureAlgorithmURI(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256);
        signingParameters.setSignatureReferenceDigestMethod(SignatureConstants.ALGO_ID_DIGEST_SHA256);
        signingParameters.setSignatureCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
        signingParameters.setKeyInfoGenerator(kiGenerator);
        
        messageContext.getSubcontext(SecurityParametersContext.class, true).setSignatureSigningParameters(signingParameters);
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
