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

    /** The name of the system property defining the normalized path to testbed-only configuration. */
    public final static String TESTBED_HOME = "testbed.home";
    
    /**
     * Set the 'idp.home' system property if not already set, and preserve the unnormalized
     * form in idp.home.raw
     */
    public static void setupIdPHomeProperties() {

        String raw = System.getProperty(IDP_HOME);
        if (raw != null) {
            normalizeProperty(IDP_HOME, raw);
        } else {
            raw = Paths.get(Paths.get("").toAbsolutePath().getParent().toAbsolutePath().toString(),
                    "java-identity-provider", "idp-conf", "src", "main", "resources").toString();
            normalizeProperty(IDP_HOME, raw);
        }
    }

    /**
     * Set the 'testbed.home' system property if not already set.
     */
    public static void setupTestbedHomeProperty() {
        
        String raw = System.getProperty(TESTBED_HOME);
        if (raw != null) {
            normalizeProperty(TESTBED_HOME, raw);
        } else {
            raw = Paths.get("src", "main", "resources").toAbsolutePath().toString();
            normalizeProperty(TESTBED_HOME, raw);
        }
    }
    
    /**
     * Set system properties sourced from 'idp.properties' file.
     * 
     * @return the properties
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static Properties setupIdPProperties() throws FileNotFoundException,
            IOException {
        final Path pathToIdPProperties = Paths.get(System.getProperty(IDP_HOME), "conf", "idp.properties");
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
     */
    public static void normalizeProperty(@Nullable final String name, @Nullable final String path) {
        String value = path;

        if (System.getProperty(name) == null
                && PathPropertySupport.class.getProtectionDomain().getCodeSource().getLocation().toString()
                        .endsWith(".war")) {
            // Running from command line.
            value = Paths.get("").toAbsolutePath().toString();
        }

        System.setProperty(name, normalizePath(value));
    }

    /**
     * Normalize a path with multiple leading slashes and convert backslashes to forward slashes.
     * 
     * <p>This normalization presupposes that any Windows paths that start with a drive letter are
     * usable, with the caveat that use of the path in a URI will be corrected for in-situ by adding a slash
     * when needed.</p>
     * 
     * @param path the input path
     * @return the normalized path.
     */
    static private String normalizePath(String path) {

        String normalized = path.replace("\\", "/");
        while (normalized.startsWith("//")) {
            // Skip the first slash
            normalized = normalized.substring(1);
        }
        
        return normalized;
    }

}