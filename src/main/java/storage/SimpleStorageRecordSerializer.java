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

package storage;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;

import org.opensaml.storage.StorageRecord;
import org.opensaml.storage.StorageSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.utilities.java.support.component.AbstractInitializableComponent;

/**
 * A simple storage record serializer.
 * 
 * TODO implement deserialize
 */
public class SimpleStorageRecordSerializer extends AbstractInitializableComponent
        implements StorageSerializer<StorageRecord> {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(SimpleStorageRecordSerializer.class);

    /** JSON generator factory. */
    @Nonnull private final JsonGeneratorFactory generatorFactory;

    public SimpleStorageRecordSerializer() {
        final Map<String, String> generatorConfig = new HashMap<>();
        generatorConfig.put(JsonGenerator.PRETTY_PRINTING, "true");
        generatorFactory = Json.createGeneratorFactory(generatorConfig);
    }

    /** {@inheritDoc} */
    public String serialize(
            @Nonnull final StorageRecord instance) throws IOException {

        final StringWriter sink = new StringWriter(128);
        final JsonGenerator gen = generatorFactory.createGenerator(sink);

        gen.writeStartObject();
        gen.write("value", instance.getValue());
        gen.write("version", instance.getVersion());
        final Long expiration = instance.getExpiration();
        if (expiration == null) {
            gen.writeNull("expiration");
        } else {
            gen.write("expiration", expiration);
        }
        gen.writeEnd().close();

        final String serialized = sink.toString();
        log.debug("Serialized '{}' as '{}'", instance, serialized);
        return serialized;
    }

    /** {@inheritDoc} */
    public StorageRecord deserialize(
            long version,
            String context,
            String key,
            String value,
            Long expiration) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

}
