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
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.json.Json;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonReaderFactory;
import javax.json.JsonStructure;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;

import org.opensaml.storage.MutableStorageRecord;
import org.opensaml.storage.StorageRecord;
import org.opensaml.storage.StorageSerializer;

import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.Positive;
import net.shibboleth.utilities.java.support.component.AbstractInitializableComponent;

/**
 * A simple {@link StorageRecord} serializer.
 */
public class SimpleStorageRecordSerializer extends AbstractInitializableComponent
        implements StorageSerializer<StorageRecord> {

    /** JSON generator factory. */
    @Nonnull private final JsonGeneratorFactory generatorFactory;

    /** JSON reader factory. */
    @Nonnull private JsonReaderFactory readerFactory;

    public SimpleStorageRecordSerializer() {
        final Map<String, String> generatorConfig = new HashMap<>();
        generatorConfig.put(JsonGenerator.PRETTY_PRINTING, "true");
        generatorFactory = Json.createGeneratorFactory(generatorConfig);
        readerFactory = Json.createReaderFactory(null);
    }

    /** {@inheritDoc} */
    public String serialize(@Nonnull final StorageRecord instance) throws IOException {

        final StringWriter sink = new StringWriter();
        final JsonGenerator gen = generatorFactory.createGenerator(sink);

        gen.writeStartObject();
        gen.write("value", instance.getValue());
        gen.write("version", instance.getVersion());
        final Long expiration = instance.getExpiration();
        if (expiration != null) {
            gen.write("expiration", expiration);
        }
        gen.writeEnd().close();

        return sink.toString();
    }

    /** {@inheritDoc} */
    @Nonnull public StorageRecord deserialize(final long version, @Nonnull @NotEmpty final String context,
            @Nonnull @NotEmpty final String key, @Nonnull @NotEmpty final String value, @Nullable Long expiration)
                    throws IOException {
        final VersionableStorageRecord record = new VersionableStorageRecord(value, expiration);
        record.setVersion(version);
        return record;
    }

    /**
     * Returns an object recovered from the context, key, and string produced through the {@link #serialize} method.
     * 
     * @param context storage record context
     * @param key storage record key
     * @param serialized serialized storage record
     * @return a deserialized object
     * @throws IOException if an error occurs
     */
    @Nonnull public StorageRecord deserialize(@Nonnull @NotEmpty final String context,
            @Nonnull @NotEmpty final String key, @Nonnull final String serialized) throws IOException {
        final JsonReader reader = readerFactory.createReader(new StringReader(serialized));
        final JsonStructure st = reader.read();
        if (!(st instanceof JsonObject)) {
            throw new IOException("Found invalid data structure");
        }
        final JsonObject obj = (JsonObject) st;
        final String value = obj.getString("value");
        final int version = obj.getInt("version");
        Long expiration = null;
        final JsonNumber jsonExpiration = obj.getJsonNumber("expiration");
        if (jsonExpiration != null) {
            expiration = Long.valueOf(jsonExpiration.longValueExact());
        }

        return deserialize(version, context, key, value, expiration);
    }

    /**
     * Exposes mutation of {@link StorageRecord} properties including version.
     */
    private class VersionableStorageRecord extends MutableStorageRecord {

        /**
         * Constructor.
         *
         * @param val value
         * @param exp expiration or null if none
         */
        public VersionableStorageRecord(@Nonnull @NotEmpty final String val, @Nullable final Long exp) {
            super(val, exp);
        }

        /**
         * Set the record version.
         * 
         * @param version record version; must be positive.
         */
        @Override protected void setVersion(@Positive final long version) {
            super.setVersion(version);
        }
    }

}
