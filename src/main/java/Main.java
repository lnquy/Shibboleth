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

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.ProtectionDomain;
import java.util.Properties;

import net.shibboleth.idp.test.InMemoryDirectory;
import net.shibboleth.idp.test.flows.AbstractFlowTest;

import org.eclipse.jetty.jaas.JAASLoginService;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.xml.XmlConfiguration;
import org.springframework.core.io.ClassPathResource;

import common.PathPropertySupport;

/** Start Jetty */
public class Main {

    /**
     * @param args
     */
    public static void main(String[] args) {
        try {
            // Start in-memory directory server.
            final InMemoryDirectory directoryServer =
                    new InMemoryDirectory(new ClassPathResource(AbstractFlowTest.LDIF_FILE));
            directoryServer.start();

            // Hack. If protection domain location ends with ".war", then assume we are running from a CLI, otherwise
            // assume we are running from within Eclipse.
            final ProtectionDomain protectionDomain = Main.class.getProtectionDomain();
            final URL location = protectionDomain.getCodeSource().getLocation();

            PathPropertySupport.setupIdPHomeProperties();

            // Determine path to jetty-base in the idp-distribution module.
            final Path pathToJettyBase =
                    Paths.get(Paths.get("").toAbsolutePath().getParent().toAbsolutePath().toString(),
                            "java-identity-provider", "idp-distribution", "src", "main", "resources", "jetty-base");

            // Create Jetty configuration from jetty-base/etc/jetty.xml in the idp-distribution module.
            final Path pathToJettyXML = pathToJettyBase.resolve(Paths.get("etc", "jetty.xml"));
            final Resource jettyXML = Resource.newResource(pathToJettyXML.toString());
            final XmlConfiguration configuration = new XmlConfiguration(jettyXML.getInputStream());

            // Add properties to the Jetty configuration from jetty-base/start.d/idp.ini in the idp-distribution module.
            final Path pathToIdPIni = pathToJettyBase.resolve(Paths.get("start.d", "idp.ini"));
            final Properties properties = new Properties();
            properties.load(Resource.newResource(pathToIdPIni.toFile().getAbsolutePath()).getInputStream());
            for (String key : properties.stringPropertyNames()) {
                configuration.getProperties().put(key, properties.getProperty(key));
            }

            // The keystore path defined in jetty-base/start.d/idp.ini is relative to jetty.base, which is not correct
            // for the testbed. So replace "../" with "${idp.home}/".
            final String idpIniJettyKeystorePath = configuration.getProperties().get("jetty.keystore.path");
            final String testbedJettyKeystorePath =
                    idpIniJettyKeystorePath.replace("../", System.getProperty(PathPropertySupport.IDP_HOME) + "/");
            configuration.getProperties().put("jetty.keystore.path", testbedJettyKeystorePath);

            // Configure the Jetty server (with both the XML and properties file configurations).
            final Server server = (Server) configuration.configure();

            // The SP and test webapps
            final WebAppContext testbedWebapp = new WebAppContext();
            testbedWebapp.setContextPath("/");
            if (protectionDomain.getCodeSource().getLocation().toString().endsWith(".war")) {
                // Running from command line.
                testbedWebapp.setWar(location.toExternalForm());
            } else {
                // Running from Eclipse.
                testbedWebapp.setWar("src/main/webapp");
            }

            // The IdP web app
            // TODO support running from command line
            final Path idpWebappPath =
                    Paths.get(Paths.get("").toAbsolutePath().getParent().toAbsolutePath().toString(),
                            "java-identity-provider", "idp-war", "src", "main", "webapp");
            final WebAppContext idpWebapp = new WebAppContext();
            idpWebapp.setContextPath("/idp");
            idpWebapp.setWar(idpWebappPath.toString());
            
            System.setProperty("java.security.auth.login.config",
                    System.getProperty(PathPropertySupport.IDP_HOME) + "/" + properties.getProperty("jetty.jaas.path"));
            final JAASLoginService jaasLogin = new JAASLoginService();
            jaasLogin.setName("Web Login Service");
            jaasLogin.setLoginModuleName("ShibUserPassAuth");
            final ConstraintSecurityHandler securityHandler = new ConstraintSecurityHandler();
            securityHandler.setLoginService(jaasLogin);
            idpWebapp.setSecurityHandler(securityHandler);

            final ContextHandlerCollection contexts = new ContextHandlerCollection();
            contexts.addHandler(testbedWebapp);
            contexts.addHandler(idpWebapp);
            server.setHandler(contexts);

            server.start();
            server.join();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

}