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

import javax.annotation.Nonnull;

import net.shibboleth.idp.attribute.resolver.AttributeRecipientContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.idp.session.IdPSession;
import net.shibboleth.idp.session.context.SessionContext;

import org.opensaml.profile.ProfileException;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.RequestContext;

/**
 * Add {@link AttributeRecipientContext} to the {@link AttributeResolutionContext} with principal name from the
 * {@link IdPSession}.
 */
public class AddAttributeRecipientContext extends AbstractProfileAction {

    /** Class logger. */
    private Logger log = LoggerFactory.getLogger(AddAttributeRecipientContext.class);

    /** {@inheritDoc} */
    protected org.springframework.webflow.execution.Event doExecute(@Nonnull final RequestContext springRequestContext,
            @Nonnull final ProfileRequestContext profileRequestContext) throws ProfileException {

        SessionContext sessionCtx = profileRequestContext.getSubcontext(SessionContext.class);
        if (sessionCtx == null) {
            log.debug("Action {}: No session context available.", getId());
            return ActionSupport.buildEvent(this, EventIds.INVALID_PROFILE_CTX);
        }

        IdPSession idpSession = sessionCtx.getIdPSession();
        if (idpSession == null) {
            log.debug("Action {}: No IdP session available.", getId());
            return ActionSupport.buildEvent(this, EventIds.INVALID_PROFILE_CTX);
        }

        String principalName = idpSession.getPrincipalName();
        if (principalName == null) {
            log.debug("Action {}: No principal name available.", getId());
            return ActionSupport.buildEvent(this, EventIds.INVALID_PROFILE_CTX);
        }

        AttributeResolutionContext attributeResolutionContext =
                profileRequestContext.getSubcontext(AttributeResolutionContext.class, true);

        AttributeRecipientContext attributeRecipientContext = new AttributeRecipientContext();
        attributeRecipientContext.setPrincipal(principalName);
        attributeResolutionContext.addSubcontext(attributeRecipientContext);

        return ActionSupport.buildProceedEvent(this);
    }
}
