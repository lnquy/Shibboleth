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

package sp;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.shibboleth.utilities.java.support.xml.SerializeSupport;

import org.opensaml.messaging.context.MessageContext;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.saml1.core.Response;
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
@RequestMapping("/SAML1")
public class SAML1Controller extends BaseSAMLController {
    
    private final Logger log = LoggerFactory.getLogger(SAML1Controller.class);
    
    @RequestMapping(value="/POST/ACS", method=RequestMethod.POST)
    public ResponseEntity<String> handleSSOResponsePOST(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws Exception {
        
        MessageContext<SAMLObject> messageContext = decodeInboundMessageContextPost(servletRequest);
        
        if (!(messageContext.getMessage() instanceof Response)) {
            log.error("Inbound message was not a SAML 1 Response");
            return new ResponseEntity<>("Inbound message was not a SAML 1 Response", HttpStatus.BAD_REQUEST);
        }
        
        Response response = (Response) messageContext.getMessage();
        Element responseElement = response.getDOM();
        String formattedMessage = SerializeSupport.prettyPrintXML(responseElement);
        log.trace("Returning response" + System.lineSeparator() + "{}", formattedMessage);
        
        //TODO instead of returning plain text via a ResponseEntity, add a JSP view that looks good
        
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "text/plain");
        
        return new ResponseEntity<>(formattedMessage, headers, HttpStatus.OK);
    }

}
