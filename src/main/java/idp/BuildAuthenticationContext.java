package idp;

import net.shibboleth.idp.authn.context.AuthenticationContext;

import org.opensaml.profile.ProfileException;
import org.opensaml.profile.action.AbstractProfileAction;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.saml2.core.AuthnRequest;

public class BuildAuthenticationContext extends AbstractProfileAction<SAMLObject, SAMLObject> {
	
	protected void doExecute(ProfileRequestContext<SAMLObject, SAMLObject> profileRequestContext)
			throws ProfileException {
		
		AuthenticationContext ac = new AuthenticationContext();
		
		AuthnRequest request = (AuthnRequest) profileRequestContext.getInboundMessageContext().getMessage();
		ac.setForceAuthn(request.isForceAuthn());
		ac.setIsPassive(request.isPassive());
		
		profileRequestContext.addSubcontext(ac, true);
	}
	
}