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

package common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Helper methods for working with system properties whose values are paths.
 */
public class PathPropertySupport {

    /** The name of the system property defining the path to configuration files for the IdP. Defaults to 'idp.home'. */
    public final static String IDP_HOME = "idp.home";

    /**
     * The name of the system property defining the path to configuration files external to the IdP. Defaults to
     * 'app.home'.
     */
    public final static String APP_HOME = "app.home";

    /**
     * Set the 'idp.home' system property if not already set.
     * 
     * @return the 'idp.home' property
     */
    public static String setupIdPHomeProperty() {

        if (System.getProperty(IDP_HOME) != null) {
            return System.getProperty(IDP_HOME);
        }

        String idpHome =
                Paths.get(Paths.get("").toAbsolutePath().getParent().toAbsolutePath().toString(),
                        "java-identity-provider", "idp-conf", "src", "main", "resources").toString();

        return setupProperty(IDP_HOME, idpHome);
    }

    /**
     * Set the 'app.home' system property if not already set.
     * 
     * @return the 'app.home' system property
     */
    public static String setupAppHomeProperty() {

        if (System.getProperty(APP_HOME) != null) {
            return System.getProperty(APP_HOME);
        }

        String appHome = Paths.get("src", "main", "resources").toAbsolutePath().toString();
        return setupProperty(APP_HOME, appHome);
    }

    /**
     * Set system properties from 'idp.properties' file.F
     * 
     * @param idpHome 'idp.home' path
     * @return the properties
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static Properties setupIdPProperties(@Nonnull final String idpHome) throws FileNotFoundException,
            IOException {
        Path pathToIdPProperties = Paths.get(idpHome, "conf", "idp.properties");
        Properties idpProperties = new Properties();
        idpProperties.load(new FileInputStream(pathToIdPProperties.toFile()));
        for (String propertyName : idpProperties.stringPropertyNames()) {
            System.setProperty(propertyName, idpProperties.getProperty(propertyName));
        }
        return idpProperties;
    }

    /**
     * Normalize and set system property whose value is a path.
     * 
     * If running from the command line, the given path will be ignored, and instead will be the current path.
     * 
     * @param name system property name
     * @param path path to normalize and set
     * @return the normalized path
     */
    public static String setupProperty(@Nullable final String name, @Nullable final String path) {
        String value = path;

        if (System.getProperty(name) == null
                && PathPropertySupport.class.getProtectionDomain().getCodeSource().getLocation().toString()
                        .endsWith(".war")) {
            // Running from command line.
            value = Paths.get("").toAbsolutePath().toString();
        }

        final String normalizedValue;
        if (needsNormalized(value)) {
            normalizedValue = normalizePath(value);
        } else {
            normalizedValue = value;
        }

        System.setProperty(name, normalizedValue);

        return value;
    }

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
     * 
     * @param path the path to inspect
     * @return whether we need to normalize.
     */
    static private boolean needsNormalized(String path) {

        return path.length() >= 2 && path.charAt(0) != '/' && path.charAt(0) != File.pathSeparatorChar
                && path.charAt(1) == ':';

    }
}
