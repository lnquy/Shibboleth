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

import java.net.MalformedURLException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import net.shibboleth.idp.cas.protocol.ProtocolParam;
import net.shibboleth.utilities.java.support.collection.Pair;
import net.shibboleth.utilities.java.support.net.HttpServletSupport;
import net.shibboleth.utilities.java.support.net.URLBuilder;

@Controller
@RequestMapping("/CAS")
public class CASController {

    private Logger log = LoggerFactory.getLogger(CASController.class);

    public String idpCASEndpointPath = "/idp/profile/cas";

    public String casSPServicePath = "/sp/CAS/Service";

    /**
     * Init SSO by redirecting to the CAS login service.
     * 
     * @param servletRequest
     * @param servletResponse
     * @throws Exception
     */
    @RequestMapping(value = "/InitSSO", method = RequestMethod.GET)
    public void initLogin(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws Exception {

        final String baseUrl = getBaseUrl(servletRequest);

        final String endpointURL = baseUrl + idpCASEndpointPath + "/login";

        final URLBuilder urlBuilder = new URLBuilder(endpointURL);

        final List<Pair<String, String>> queryParams = urlBuilder.getQueryParams();

        queryParams.add(new Pair<String, String>(ProtocolParam.Service.id(), baseUrl + casSPServicePath));

        final String redirectURL = urlBuilder.buildURL();

        HttpServletSupport.addNoCacheHeaders(servletResponse);
        HttpServletSupport.setUTF8Encoding(servletResponse);

        log.debug("Sending redirect to '{}'", redirectURL);
        servletResponse.sendRedirect(redirectURL);
    }

    /**
     * Produce a page displaying a link to the CAS validation endpoint.
     * 
     * @param servletRequest
     * @param servletResponse
     * @return HTML displaying a link to the CAS validation endpoint
     * @throws Exception
     */
    @RequestMapping(value = "/Service", method = RequestMethod.GET)
    public ResponseEntity<String> handleCASResponse(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws Exception {

        final String baseUrl = getBaseUrl(servletRequest);

        final String endpointURL = baseUrl + idpCASEndpointPath + "/serviceValidate";

        final URLBuilder urlBuilder = new URLBuilder(endpointURL);

        final String ticket = servletRequest.getParameter(ProtocolParam.Ticket.id());

        final List<Pair<String, String>> queryParams = urlBuilder.getQueryParams();
        queryParams.add(new Pair<String, String>(ProtocolParam.Service.id(), baseUrl + casSPServicePath));
        queryParams.add(new Pair<String, String>(ProtocolParam.Ticket.id(), ticket));

        final String redirectURL = urlBuilder.buildURL();

        final String html = "<html><body><a id=cas-service-validate href=\"" + redirectURL
                + "\">CAS Service Validate</a></body></html>";
        log.trace("Returning html '{}'", html);

        final HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "text/html");

        return new ResponseEntity<>(html, headers, HttpStatus.OK);
    }

    private String getBaseUrl(HttpServletRequest servletRequest) {
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
}
