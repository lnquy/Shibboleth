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

import java.io.File;
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
     * Normalize a path for windows.<br/>
     * 
     * On operating systems which present filesystems not rooted in '\', we need to be careful about the construction of
     * URLS. According to RFC 1738 a url of the form file://foo/bar is for <em>host</em>foo and <em>path</em> bar. Hence
     * if we take a path of the form c:\foo\bar and (or normalized to c:/foo/bar) and prepend 'file://' the URL code
     * will, quite correctly take this as host 'C:'. This will at best end up with a timeout because 'C:' cannot be
     * found and at worst with getting the completely the wrong info. Contrast the case of passing in /opt/idp/config,
     * this yields file:///opt/idp/config which the URL code interprets as '/opt/idp/config' on the null host.
     * 
     * 
     * <br/>
     * The canonical solution on windows is to prepend a '/'. We can deal with other operatring systems as the need
     * arises.
     * 
     * @param path the input path
     * @return the normalized path.
     */
    static private String normalizePath(String path) {

        if (needsNormalized(path)) {
            return '/' + path;
        } 
        return path;
    }
    
    /**
     * Does the path need to be normalized? <br/>
     * Yes if it doesn't start with '/' (or '\') and the second character is ':'
     * @param path the path to inspect
     * @return whether we need to normalize.
     */
    static private boolean needsNormalized(String path) {
        
        return path.length() >= 2 && path.charAt(0) != '/' && path.charAt(0) != File.pathSeparatorChar && path.charAt(1) == ':';
        
    }

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
                    idpHome = Paths.get(Paths.get("").toAbsolutePath().getParent().toAbsolutePath().toString(),
                                    "java-identity-provider", "idp-conf", "src", "main", "resources").toString();
                }
                System.setProperty("idp.home", normalizePath(idpHome));
            } else if (needsNormalized(idpHome)) {
                System.setProperty("idp.home", normalizePath(idpHome));
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
                System.setProperty("app.home", normalizePath(appHome));
            } else if (needsNormalized(appHome)) {
                System.setProperty("app.home", normalizePath(appHome));
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
