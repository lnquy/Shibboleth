package idp;

import javax.annotation.Nonnull;

import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.utilities.java.support.security.IdentifierGenerationStrategy;

import org.joda.time.DateTime;
import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.profile.ProfileException;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.core.StatusMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

public class BuildMockSaml2Response extends AbstractProfileAction<SAMLObject, SAMLObject> {
	
	private Logger log = LoggerFactory.getLogger(BuildMockSaml2Response.class);
	
	@Autowired
	private XMLObjectBuilderFactory builderFactory;
	
	@Autowired
	@Qualifier("testbed.IdGenerator")
	private IdentifierGenerationStrategy idGenerator;


	@Nonnull
	protected Event doExecute(RequestContext springRequestContext, ProfileRequestContext<SAMLObject, SAMLObject> profileRequestContext)
			throws ProfileException {
		
		log.debug("Building mock SAML 2 Response");
		
		Response response = buildResponse(springRequestContext, profileRequestContext);
		
		profileRequestContext.getOutboundMessageContext().setMessage(response);
		
		return ActionSupport.buildProceedEvent(this);
	}

	private Response buildResponse(RequestContext springRequestContext, ProfileRequestContext<SAMLObject, SAMLObject> profileRequestContext) {
		Response response = (Response) builderFactory.getBuilder(Response.DEFAULT_ELEMENT_NAME).buildObject(Response.DEFAULT_ELEMENT_NAME);
		
		response.setID(idGenerator.generateIdentifier());
		response.setIssueInstant(new DateTime());
		
		Issuer issuer = (Issuer) builderFactory.getBuilder(Issuer.DEFAULT_ELEMENT_NAME).buildObject(Issuer.DEFAULT_ELEMENT_NAME);
		issuer.setValue(getIdpEntityId());
		response.setIssuer(issuer);
		
		Status status = (Status) builderFactory.getBuilder(Status.DEFAULT_ELEMENT_NAME).buildObject(Status.DEFAULT_ELEMENT_NAME);
		StatusCode statusCode = (StatusCode) builderFactory.getBuilder(StatusCode.DEFAULT_ELEMENT_NAME).buildObject(StatusCode.DEFAULT_ELEMENT_NAME);
        status.setStatusCode(statusCode);
		
		if (springRequestContext.getCurrentEvent().getId().equals(EventIds.PROCEED_EVENT_ID)) {
		    statusCode.setValue(StatusCode.SUCCESS_URI);
		} else if (profileRequestContext.getSubcontext(AuthenticationContext.class, true).isPassive()) {
            statusCode.setValue(StatusCode.REQUESTER_URI);
            StatusCode subCode = (StatusCode) builderFactory.getBuilder(StatusCode.DEFAULT_ELEMENT_NAME).buildObject(StatusCode.DEFAULT_ELEMENT_NAME);
            subCode.setValue(StatusCode.NO_PASSIVE_URI);
            statusCode.setStatusCode(subCode);
		} else {
		    statusCode.setValue(StatusCode.RESPONDER_URI);
		    StatusMessage msg = (StatusMessage) builderFactory.getBuilder(StatusMessage.DEFAULT_ELEMENT_NAME).buildObject(StatusMessage.DEFAULT_ELEMENT_NAME);
		    msg.setMessage(getMessage(springRequestContext.getCurrentEvent().getId(), null,
		            springRequestContext.getCurrentEvent().getId(), springRequestContext.getExternalContext().getLocale()));
		    status.setStatusMessage(msg);
		}
		
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
