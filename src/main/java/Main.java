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

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.xml.XmlConfiguration;

import common.PathPropertySupport;

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

            PathPropertySupport.setupIdPHomeProperties();
            PathPropertySupport.setupIdPProperties();
            PathPropertySupport.setupTestbedHomeProperty();
            
            // Configure Jetty from jetty.xml.
            final Path pathToJettyXML =
                    Paths.get(System.getProperty(PathPropertySupport.IDP_HOME_RAW), "system", "conf", "jetty.xml");
            final Resource fileserver_xml = Resource.newResource(pathToJettyXML.toString());
            final XmlConfiguration configuration = new XmlConfiguration(fileserver_xml.getInputStream());
            final Server server = (Server) configuration.configure();

            final WebAppContext webapp = new WebAppContext();
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
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }
    
}