package idp;

import javax.annotation.Nonnull;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.utilities.java.support.security.IdentifierGenerationStrategy;

import org.joda.time.DateTime;
import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.profile.ProfileException;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

public class BuildMockSaml2Response extends AbstractProfileAction<SAMLObject, SAMLObject> {
	
	private Logger log = LoggerFactory.getLogger(BuildMockSaml2Response.class);
	
	@Autowired
	private XMLObjectBuilderFactory builderFactory;
	
	@Autowired
	private IdentifierGenerationStrategy idGenerator;


	@Nonnull
	protected Event doExecute(RequestContext springRequestContext, ProfileRequestContext<SAMLObject, SAMLObject> profileRequestContext)
			throws ProfileException {
		
		log.debug("Building mock SAML 2 Response");
		
		Response response = buildResponse(profileRequestContext);
		
		MessageContext<SAMLObject> outboundMessageContext = buildOutboundMessageContext(response);
		
		profileRequestContext.setOutboundMessageContext(outboundMessageContext);
		
		return ActionSupport.buildProceedEvent(this);
	}

	private MessageContext<SAMLObject> buildOutboundMessageContext(Response response) {
		MessageContext<SAMLObject> messageContext = new MessageContext<>();
		messageContext.setMessage(response);
		
		return messageContext;
	}

	private Response buildResponse(ProfileRequestContext<SAMLObject, SAMLObject> profileRequestContext) {
		Response response = (Response) builderFactory.getBuilder(Response.DEFAULT_ELEMENT_NAME).buildObject(Response.DEFAULT_ELEMENT_NAME);
		
		response.setID(idGenerator.generateIdentifier());
		response.setIssueInstant(new DateTime());
		
		Issuer issuer = (Issuer) builderFactory.getBuilder(Issuer.DEFAULT_ELEMENT_NAME).buildObject(Issuer.DEFAULT_ELEMENT_NAME);
		issuer.setValue(getIdpEntityId());
		response.setIssuer(issuer);
		
		Status status = (Status) builderFactory.getBuilder(Status.DEFAULT_ELEMENT_NAME).buildObject(Status.DEFAULT_ELEMENT_NAME);
		StatusCode statusCode = (StatusCode) builderFactory.getBuilder(StatusCode.DEFAULT_ELEMENT_NAME).buildObject(StatusCode.DEFAULT_ELEMENT_NAME);
		statusCode.setValue(StatusCode.SUCCESS_URI);
		status.setStatusCode(statusCode);
		response.setStatus(status);
		
		return response;
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
