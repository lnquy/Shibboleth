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

package idp.c14n;

import idp.AbstractFlowTest;

import javax.annotation.Nonnull;

import org.junit.AfterClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.webflow.execution.FlowExecutionOutcome;
import org.springframework.webflow.executor.FlowExecutionResult;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Tests for C14N.
 */
@ContextConfiguration({"classpath:/c14n/test-webflow-config.xml", "classpath:/c14n/locate-legacy-resolver.xml"})
public class TestLegacyC14N extends AbstractFlowTest {
    
    @BeforeClass public void setPerClassProperties() {
        System.setProperty("idp.c14n.flows", "Simple|Legacy.*");
    }
    
    @AfterClass public void resetPerClassProperties() {
        System.setProperty("idp.c14n.flows", "Simple");
    }

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(TestLegacyC14N.class);


    @Test public void testTransientNameID() {

        FlowExecutionResult result = flowExecutor.launchExecution("transientNameID", null, externalContext);
        Assert.assertEquals("transientNameID", result.getFlowId());

        FlowExecutionOutcome outcome = result.getOutcome();
        log.debug("flow outcome {}", outcome);
        Assert.assertNotNull(outcome);
        Assert.assertEquals(outcome.getId(), "end");
        Assert.assertTrue(result.isEnded());

    }

    @Test public void testTransientNameIdentifier() {

        FlowExecutionResult result = flowExecutor.launchExecution("transientNameIdentifier", null, externalContext);
        Assert.assertEquals("transientNameIdentifier", result.getFlowId());

        FlowExecutionOutcome outcome = result.getOutcome();
        log.debug("flow outcome {}", outcome);
        Assert.assertNotNull(outcome);
        Assert.assertEquals(outcome.getId(), "end");
        Assert.assertTrue(result.isEnded());

    }
}
