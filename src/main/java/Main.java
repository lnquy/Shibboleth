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

import java.io.FileInputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.ProtectionDomain;
import java.util.Properties;

import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.server.Server;
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
            ProtectionDomain protectionDomain = Main.class.getProtectionDomain();
            URL location = protectionDomain.getCodeSource().getLocation();

            // Set idp.home system property if it has not been set as a command line option.
            String idpHome = System.getProperty("idp.home");
            if (idpHome == null) {
                if (protectionDomain.getCodeSource().getLocation().toString().endsWith(".war")) {
                    // Running from command line.
                    idpHome = Paths.get("").toAbsolutePath().toString();
                } else {
                    // Running from Eclipse.
                    idpHome =
                            Paths.get(Paths.get("").toAbsolutePath().getParent().toAbsolutePath().toString(),
                                    "java-identity-provider", "idp-conf", "src", "main", "resources").toString();
                }
                System.setProperty("idp.home", idpHome);
            }

            // Set app.home system property if it has not been set as a command line option.
            String appHome = System.getProperty("app.home");
            if (appHome == null) {
                if (protectionDomain.getCodeSource().getLocation().toString().endsWith(".war")) {
                    // Running from command line.
                    appHome = Paths.get("").toAbsolutePath().toString();
                } else {
                    // Running from Eclipse.
                    appHome = Paths.get("src", "main", "resources").toAbsolutePath().toString();
                }
                System.setProperty("app.home", appHome);
            }

            // Add system properties from idp.properties.
            Path pathToIdPProperties = Paths.get(idpHome, "conf", "idp.properties");
            Properties idpProperties = new Properties();
            idpProperties.load(new FileInputStream(pathToIdPProperties.toFile()));
            for (String propertyName : idpProperties.stringPropertyNames()) {
                System.setProperty(propertyName, idpProperties.getProperty(propertyName));
            }

            // Configure Jetty from jetty.xml.
            Path pathToJettyXML = Paths.get(idpHome, "system", "conf", "jetty.xml");
            Resource fileserver_xml = Resource.newResource(pathToJettyXML.toString());
            XmlConfiguration configuration = new XmlConfiguration(fileserver_xml.getInputStream());
            Server server = (Server) configuration.configure();

            Path pathToRealm = Paths.get(idpHome, "test", "jetty-realm.properties");
            HashLoginService loginService = new HashLoginService();
            loginService.setName("Shib Testbed Web Authentication");
            loginService.setConfig(pathToRealm.toString());
            server.addBean(loginService);

            WebAppContext webapp = new WebAppContext();
            webapp.setContextPath("/");
            server.setHandler(webapp);
            if (protectionDomain.getCodeSource().getLocation().toString().endsWith(".war")) {
                // Running from command line.
                webapp.setWar(location.toExternalForm());
            } else {
                // Running from Eclipse.
                webapp.setWar("src/main/webapp");
            }

            server.start();
            server.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
