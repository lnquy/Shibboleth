package common;

import javax.xml.namespace.QName;

import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.handler.AbstractMessageHandler;
import org.opensaml.messaging.handler.MessageHandlerException;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.opensaml.saml.common.messaging.context.SAMLProtocolContext;

public class MockPopulateContextData extends AbstractMessageHandler<SAMLObject> {
	
	private String samlProtocol;
	
	private QName peerSamlRole;

	/**
	 * @return the samlProtocol
	 */
	public String getSamlProtocol() {
		return samlProtocol;
	}

	/**
	 * @param samlProtocol the samlProtocol to set
	 */
	public void setSamlProtocol(String samlProtocol) {
		this.samlProtocol = samlProtocol;
	}

	/**
	 * @return the peerSamlRole
	 */
	public QName getPeerSamlRole() {
		return peerSamlRole;
	}

	/**
	 * @param peerSamlRole the peerSamlRole to set
	 */
	public void setPeerSamlRole(QName peerSamlRole) {
		this.peerSamlRole = peerSamlRole;
	}

	protected void doInitialize() throws ComponentInitializationException {
		super.doInitialize();
		Constraint.isNotNull(samlProtocol, "SAML protocol may not be null");
		Constraint.isNotNull(peerSamlRole, "SAML peer role may not be null");
	}
	
	protected void doInvoke(MessageContext<SAMLObject> messageContext) throws MessageHandlerException {
		messageContext.getSubcontext(SAMLProtocolContext.class, true).setProtocol(samlProtocol);
		messageContext.getSubcontext(SAMLPeerEntityContext.class, true).setRole(peerSamlRole);
	}
	
}
