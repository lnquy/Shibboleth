/*
. * Licensed to the University Corporation for Advanced Internet Development, 
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
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;

import org.opensaml.storage.StorageRecord;
import org.opensaml.storage.StorageSerializer;
import org.opensaml.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Interact with storage services via HTTP.
 * 
 * Only the 'read' and 'create' storage operations are supported.
 * 
 * Examples follow.
 * 
 * To create :
 * <p>
 * curl -X POST -dvalue=value 'http://localhost:8080/idp/storage/shibboleth.StorageService/create/context/key'
 * <p>
 * 
 * To read :
 * <p>
 * curl 'http://localhost:8080/idp/storage/shibboleth.StorageService/read/context/key'
 * <p>
 * 
 * To list storage services :
 * <p>
 * curl 'http://localhost:8080/idp/storage/'
 * <p>
 */
@Controller
public class StorageServiceWrapperController {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(StorageServiceWrapperController.class);

    /** JSON generator factory. */
    @Nonnull private final JsonGeneratorFactory generatorFactory;

    /** Storage record serializer. */
    @Nonnull private final StorageSerializer<StorageRecord> serializer;

    /** The (IdP) application context. */
    @Autowired private ApplicationContext appContext;

    public StorageServiceWrapperController() {
        log.debug("StorageServiceWrapperController construtor");

        final Map<String, String> generatorConfig = new HashMap<>();
        generatorConfig.put(JsonGenerator.PRETTY_PRINTING, "true");
        generatorFactory = Json.createGeneratorFactory(generatorConfig);

        serializer = new SimpleStorageRecordSerializer();
    }

    @Nullable
    protected StorageService getStorageService(
            @Nonnull final String storageServiceId) {
        try {
            log.debug("Get storage service with id '{}'", storageServiceId);
            final StorageService storageService = appContext.getBean(storageServiceId, StorageService.class);
            log.debug("Get storage service with id '{}' returned '{}'", storageServiceId, storageService);
            return storageService;
        } catch (BeansException e) {
            log.debug("Unable to get storage service", e);
        }

        return null;
    }

    @ResponseBody
    @RequestMapping(
            value = "/",
            method = RequestMethod.GET)
    public String listStorageServices() {

        final Map<String, StorageService> storageServices = new HashMap<>();
        storageServices.putAll(appContext.getBeansOfType(StorageService.class));
        storageServices.putAll(appContext.getParent().getBeansOfType(StorageService.class));

        if (log.isDebugEnabled()) {
            for (final Map.Entry<String, StorageService> entry : storageServices.entrySet()) {
                log.debug("Storage service '{}' : '{}'", entry.getKey(), entry.getValue());
            }
        }

        return storageServices.toString();
    }

    protected ResponseEntity<String> create(
            @Nonnull final String storageServiceId,
            @Nonnull final String context,
            @Nonnull final String key,
            @Nonnull final String value) {
        try {
            final StorageService storageService = getStorageService(storageServiceId);

            log.debug("Creating in '{}' with context '{}' and key '{}'", storageServiceId, context, key);
            boolean success = storageService.create(context, key, value, null);
            log.debug("Create '{}' in '{}' with context '{}' and key '{}'", success, storageServiceId, context, key);
            if (success) {
                return new ResponseEntity<String>(HttpStatus.CREATED);
            } else {
                return new ResponseEntity<String>(HttpStatus.CONFLICT);
            }

        } catch (IOException e) {
            return new ResponseEntity<String>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(
            value = "/{storageServiceId}/create/{context}/{key}",
            method = RequestMethod.POST)
    public ResponseEntity<String> createFromPathVariables(
            @PathVariable final String storageServiceId,
            @PathVariable final String context,
            @PathVariable final String key,
            @RequestParam("value") String value) throws Exception {
        return create(storageServiceId, context, key, value);
    }

    @RequestMapping(
            value = "/{storageServiceId}/create",
            method = RequestMethod.POST)
    public ResponseEntity<String> createFromRequestParams(
            @PathVariable final String storageServiceId,
            @RequestParam final String context,
            @RequestParam final String key,
            @RequestParam("value") String value) throws Exception {
        return create(storageServiceId, context, key, value);
    }

    protected ResponseEntity<String> read(
            @Nonnull final String storageServiceId,
            @Nonnull final String context,
            @Nonnull final String key) {
        try {
            final StorageService storageService = getStorageService(storageServiceId);

            log.debug("Reading from '{}' with context '{}' and key '{}'", storageServiceId, context, key);
            final StorageRecord record = storageService.read(context, key);
            log.debug("Read '{}' from '{}' with context '{}' and key '{}'", record, storageServiceId, context, key);

            if (record == null) {
                return new ResponseEntity<String>(HttpStatus.NOT_FOUND);
            } else {
                final HttpHeaders httpHeaders = new HttpHeaders();
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                final String serializedStorageRecord = serializer.serialize(record);
                return new ResponseEntity<String>(serializedStorageRecord, httpHeaders, HttpStatus.OK);
            }
        } catch (IOException e) {
            return new ResponseEntity<String>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(
            value = "/{storageServiceId}/read/{context}/{key}",
            method = RequestMethod.GET,
            produces = {"application/json"})
    public ResponseEntity<String> readFromPathVariables(
            @PathVariable @Nonnull final String storageServiceId,
            @PathVariable @Nonnull final String context,
            @PathVariable @Nonnull final String key) throws IOException {
        return read(storageServiceId, context, key);
    }

    @RequestMapping(
            value = "/{storageServiceId}/read",
            method = RequestMethod.GET,
            produces = {"application/json"})
    public ResponseEntity<String> readFromRequestParams(
            @PathVariable @Nonnull final String storageServiceId,
            @RequestParam("context") @Nonnull final String context,
            @RequestParam("key") @Nonnull final String key) throws IOException {
        return read(storageServiceId, context, key);
    }

}
