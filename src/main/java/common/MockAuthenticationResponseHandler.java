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

package common;

import org.ldaptive.LdapAttribute;
import org.ldaptive.LdapEntry;
import org.ldaptive.LdapException;
import org.ldaptive.auth.AccountState;
import org.ldaptive.auth.AuthenticationResponse;
import org.ldaptive.auth.AuthenticationResponseHandler;

/**
 * {@link AuthenticationResponseHandler} used to inject arbitrary {@link AccountState} into an authentication response.
 */
public class MockAuthenticationResponseHandler implements AuthenticationResponseHandler {

    /** Account state to set on authentication response. */
    private final AccountState[] accountStates;

    /**
     * Creates a new mock authentication response handler.
     *
     * @param state to inject into the authentication response
     */
    public MockAuthenticationResponseHandler(final AccountState... state) {
        accountStates = state;
    }

    /** {@inheritDoc} */
    @Override
    public void handle(AuthenticationResponse response) throws LdapException
    {
        if (accountStates != null) {
            boolean setWarning = false;
            boolean setError = false;
            final LdapEntry entry = response.getLdapEntry();
            if (entry != null) {
                final LdapAttribute attr = entry.getAttribute("businessCategory");
                if (attr != null) {
                    setWarning = "accountStateWarning".equals(attr.getStringValue());
                    setError = "accountStateError".equals(attr.getStringValue());
                    
                }
            }
            for (AccountState state : accountStates) {
                if (response.getResult()) {
                    if (setWarning && state.getWarning() != null) {
                        response.setAccountState(state);            
                    }
                } else {
                    if (setError && state.getError() != null) {
                        response.setAccountState(state);            
                    }
                }
            }
        }
    }

}