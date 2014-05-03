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

import javax.annotation.Nullable;

/**
 * Helper methods for working with system properties whose values are paths.
 */
public class PathPropertySupport {

    /** The name of the system property defining the normalized path to configuration files for the IdP. */
    public final static String IDP_HOME = "idp.home";

    /** The name of the system property defining the raw path to configuration files for the IdP. */
    public final static String IDP_HOME_RAW = "idp.home.raw";

    /** The name of the system property defining the normalized path to testbed-only configuration. */
    public final static String TESTBED_HOME = "testbed.home";
    
    /**
     * Set the 'idp.home' system property if not already set, and preserve the unnormalized
     * form in idp.home.raw
     * 
     * @return the 'idp.home' property
     */
    public static void setupIdPHomeProperties() {

        String raw = System.getProperty(IDP_HOME);

        if (raw != null) {
            normalizeProperty(IDP_HOME, raw);
            System.setProperty(IDP_HOME_RAW, raw);
        } else {
            raw = Paths.get(Paths.get("").toAbsolutePath().getParent().toAbsolutePath().toString(),
                    "java-identity-provider", "idp-conf", "src", "main", "resources").toString();
            normalizeProperty(IDP_HOME, raw);
            System.setProperty(IDP_HOME_RAW, raw);
        }
    }

    /**
     * Set the 'testbed.home' system property if not already set.
     * 
     * @return the 'testbed.home' system property
     */
    public static void setupTestbedHomeProperty() {
        
        String raw = System.getProperty(TESTBED_HOME);

        if (raw != null) {
            normalizeProperty(TESTBED_HOME, raw);
        }

        raw = Paths.get("src", "main", "resources").toAbsolutePath().toString();
        normalizeProperty(TESTBED_HOME, raw);
    }
    
    /**
     * Set system properties sourced from 'idp.properties' file.
     * 
     * @param idpHome 'idp.home' path
     * @return the properties
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static Properties setupIdPProperties() throws FileNotFoundException,
            IOException {
        final Path pathToIdPProperties = Paths.get(System.getProperty(IDP_HOME_RAW), "conf", "idp.properties");
        final Properties idpProperties = new Properties();
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
    public static void normalizeProperty(@Nullable final String name, @Nullable final String path) {
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
