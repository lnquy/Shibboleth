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

import org.eclipse.jetty.jaas.JAASLoginService;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.xml.XmlConfiguration;

/** Start Jetty */
public class Main {

    /**
     * @param args
     */
    public static void main(String[] args) {
        try {
            // Hack. If protection domain location ends with ".war", then assume we are running from a CLI, otherwise
            // assume we are running from within Eclipse.
            final ProtectionDomain protectionDomain = Main.class.getProtectionDomain();
            final URL location = protectionDomain.getCodeSource().getLocation();

            // Set idp.home to "classpath:" so test files in idp-conf/src/test/resources can be found.
            System.setProperty("idp.home", "classpath:");

            // Set idp.webflows to "classpath*:/flows" so user flows in multiple locations can be found.
            System.setProperty("idp.webflows", "classpath*:/flows");

            // Determine path to jetty-base in the idp-distribution module.
            final Path pathToJettyBase =
                    Paths.get(Paths.get("").toAbsolutePath().getParent().toAbsolutePath().toString(),
                            "java-identity-provider", "idp-distribution", "src", "main", "resources", "embedded", "jetty-base");

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

            // The keystore paths defined in jetty-base/start.d/idp.ini is relative to jetty.base, which is not correct
            // for the testbed. So replace "../" with path to idp-conf/src/test/resources
            final Path pathToIdPConfTestResources =
                    Paths.get(Paths.get("").toAbsolutePath().getParent().toAbsolutePath().toString(),
                            "java-identity-provider", "idp-conf", "src", "test", "resources");
            final String idpIniJettyBackchannelKeystorePath = configuration.getProperties().get("jetty.backchannel.keystore.path");
            final String testbedJettyBackchannelKeystorePath =
                    idpIniJettyBackchannelKeystorePath
                            .replace("../", pathToIdPConfTestResources.toAbsolutePath().toString() + "/");
            configuration.getProperties().put("jetty.backchannel.keystore.path", testbedJettyBackchannelKeystorePath);
            final String idpIniJettyBrowserKeystorePath = configuration.getProperties().get("jetty.browser.keystore.path");
            final String testbedJettyBrowserKeystorePath =
                    idpIniJettyBrowserKeystorePath
                            .replace("../", pathToIdPConfTestResources.toAbsolutePath().toString() + "/");
            configuration.getProperties().put("jetty.browser.keystore.path", testbedJettyBrowserKeystorePath);

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
            
            // Uncomment along with request log handler to enable TeeFilter.
            // final Path override = Paths.get("src", "main", "resources", "system", "conf", "web-override.xml");
            // idpWebapp.setOverrideDescriptor(override.toString());
            
            final String idpJaasConfigPath = configuration.getProperties().get("jetty.jaas.path");
            final String testbedJaasConfigPath = pathToIdPConfTestResources.toAbsolutePath().toString()
                    + "/" + idpJaasConfigPath;
            System.setProperty("java.security.auth.login.config", testbedJaasConfigPath);
            
            final JAASLoginService jaasLogin = new JAASLoginService();
            jaasLogin.setName("Web Login Service");
            jaasLogin.setLoginModuleName("ShibUserPassAuth");
            final ConstraintSecurityHandler securityHandler = new ConstraintSecurityHandler();
            securityHandler.setLoginService(jaasLogin);
            idpWebapp.setSecurityHandler(securityHandler);

            final HandlerCollection handlers = new HandlerCollection();
            final ContextHandlerCollection contexts = new ContextHandlerCollection();
            handlers.setHandlers(new Handler[] {contexts, new DefaultHandler()});

            contexts.addHandler(testbedWebapp);
            contexts.addHandler(idpWebapp);

            // Uncomment to emable TeeFilter.
            // final RequestLogImpl requestLog = new RequestLogImpl();
            // requestLog.setResource("/system/conf/logback-access.xml");
            // final RequestLogHandler requestLogHandler = new RequestLogHandler();
            // requestLogHandler.setRequestLog(requestLog);
            // handlers.addHandler(requestLogHandler);

            server.setHandler(handlers);
            
            server.start();
            server.join();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

}