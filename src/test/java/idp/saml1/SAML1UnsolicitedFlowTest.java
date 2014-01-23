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

package idp.saml1;

import idp.AbstractFlowTest;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.FlowExecutionOutcome;
import org.springframework.webflow.executor.FlowExecutionResult;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test for SAML1 flow.
 */
public class SAML1UnsolicitedFlowTest extends AbstractFlowTest {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(SAML1UnsolicitedFlowTest.class);

    /** Flow id. */
    @Nonnull public final static String FLOW_ID = "Shibboleth/SSO";

    @Test public void testFlow() {
        // TODO more request parameters
        request.addParameter("providerId", "https://sp.example.org");

        FlowExecutionResult result = flowExecutor.launchExecution(FLOW_ID, null, externalContext);
        Assert.assertEquals(result.getFlowId(), FLOW_ID);

        FlowExecutionOutcome outcome = result.getOutcome();
        log.debug("flow outcome {}", outcome);
        Assert.assertNotNull(outcome);
        Assert.assertEquals(outcome.getId(), "end");

        // TODO meaningful asserts

        Assert.assertTrue(result.isEnded());
    }
}
