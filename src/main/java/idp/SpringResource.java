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

import java.io.IOException;
import java.io.InputStream;

import net.shibboleth.utilities.java.support.resource.AbstractResource;
import net.shibboleth.utilities.java.support.resource.Resource;
import net.shibboleth.utilities.java.support.resource.ResourceException;

/** A {@link Resource} which wraps a {@link org.springframework.core.io.Resource}. */
public class SpringResource extends AbstractResource {

    /** The wrapped Spring resource. */
    private org.springframework.core.io.Resource springResource;

    /**
     * 
     * Constructor.
     * 
     * @param resource the Spring resource
     */
    public SpringResource(org.springframework.core.io.Resource resource) {
        springResource = resource;
    }

    /** {@inheritDoc} */
    protected boolean doExists() throws ResourceException {
        return springResource.exists();
    }

    /** {@inheritDoc} */
    protected InputStream doGetInputStream() throws ResourceException {
        try {
            return springResource.getInputStream();
        } catch (IOException e) {
            throw new ResourceException(e);
        }
    }

    /** {@inheritDoc} */
    protected long doGetLastModifiedTime() throws ResourceException {
        // TODO Auto-generated method stub
        return 0;
    }
}
