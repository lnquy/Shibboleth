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

import java.io.FileNotFoundException;
import java.io.IOException;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.BeforeSuite;

import common.PathPropertySupport;

/**
 * Test the status servlet.
 */
@ContextConfiguration({"/idp-server.xml",})
public class StatusTest extends AbstractTestNGSpringContextTests {

    @BeforeSuite(enabled=false) public void setupProperties() throws FileNotFoundException, IOException {

        String idpHome = PathPropertySupport.setupIdPHomeProperty();

        PathPropertySupport.setupIdPProperties(idpHome);

        PathPropertySupport.setupAppHomeProperty();

        System.setProperty("idp.xml.securityManager", "org.apache.xerces.util.SecurityManager");
    }

    // TODO: test is failing because context is being initialized twice, and the LDAP server steps on itself
    public void testStatus() {

        String statusURL = "https://localhost:8443/idp/status";

        WebDriver driver = new HtmlUnitDriver();

        driver.get(statusURL);

        Assert.assertTrue(driver.getPageSource().startsWith("### Operating Environment Information"));
    }

}