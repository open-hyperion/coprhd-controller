/*
 * Copyright 2016 Dell Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.emc.storageos.driver.dellsc.scapi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.emc.storageos.driver.dellsc.scapi.objects.ScControllerPort;
import com.emc.storageos.driver.dellsc.scapi.objects.ScControllerPortIscsiConfiguration;
import com.emc.storageos.driver.dellsc.scapi.objects.ScFaultDomain;
import com.emc.storageos.driver.dellsc.scapi.objects.ScServer;
import com.emc.storageos.driver.dellsc.scapi.objects.ScServerHba;
import com.emc.storageos.driver.dellsc.scapi.objects.ScStorageType;
import com.emc.storageos.driver.dellsc.scapi.objects.ScVolume;
import com.emc.storageos.driver.dellsc.scapi.objects.StorageCenter;
import com.emc.storageos.driver.dellsc.scapi.objects.StorageCenterStorageUsage;
import com.emc.storageos.driver.dellsc.scapi.rest.Parameters;
import com.emc.storageos.driver.dellsc.scapi.rest.PayloadFilter;
import com.emc.storageos.driver.dellsc.scapi.rest.RestClient;
import com.emc.storageos.driver.dellsc.scapi.rest.RestResult;
import com.google.gson.Gson;

/**
 * API client for managing Storage Center via DSM.
 */
public class StorageCenterAPI implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(StorageCenterAPI.class);
    private RestClient restClient;
    private Gson gson;

    private StorageCenterAPI(String host, int port, String user, String password)
            throws StorageCenterAPIException {
        LOG.debug("{} {} {}", host, port, user);
        restClient = new RestClient(host, port, user, password);
        gson = new Gson();

        Parameters params = new Parameters();
        params.add("Application", "CoprHD Driver");
        params.add("ApplicationVersion", "2.0");

        RestResult result = restClient.post("/ApiConnection/Login", params.toJson());
        if (result.getResponseCode() != 200) {
            throw new StorageCenterAPIException(result.getErrorMsg(), result.getResponseCode());
        }
        LOG.debug("Successful login to {} for user {}", host, user);
    }

    /**
     * Get a Storage Center API connection.
     *
     * @param host The DSM host name or IP address.
     * @param port The DSM port.
     * @param user The user to connect as.
     * @param password The password.
     * @return The Storage Center API connection.
     * @throws StorageCenterAPIException the storage center API exception
     */
    public static StorageCenterAPI openConnection(String host, int port, String user, String password)
            throws StorageCenterAPIException {
        return new StorageCenterAPI(host, port, user, password);
    }

    /**
     * Close API connection.
     */
    public void closeConnection() {
        // Log out of the API
        restClient.post("/ApiConnection/Logout", "{}");
        restClient = null;
    }

    @Override
    public void close() {
        this.closeConnection();
    }

    @SuppressWarnings("unchecked")
    private boolean checkResults(RestResult result) {
        if (result.getResponseCode() >= 200 &&
                result.getResponseCode() < 300) {
            return true;
        }

        // Some versions return the reason text, some return a JSON structure
        String text = result.getResult();
        if (text.startsWith("{\"")) {
            Map<String, String> resultJson = new HashMap<>(0);
            resultJson = gson.fromJson(text, resultJson.getClass());
            text = resultJson.get("result");
        }

        LOG.warn("REST call result:\n\tURL:         {}\n\tStatus Code: {}\n\tReason:      {}\n\tText:        {}",
                result.getUrl(), result.getResponseCode(), result.getErrorMsg(), text);
        return false;
    }

    /**
     * Get all Storage Centers managed by this DSM instance.
     *
     * @return Collection of Storage Centers.
     */
    public StorageCenter[] getStorageCenterInfo() {
        RestResult result = restClient.get("/StorageCenter/StorageCenter");

        try {
            return gson.fromJson(result.getResult(), StorageCenter[].class);
        } catch (Exception e) {
            LOG.warn("Error getting Storage Center info results: {}", e);
        }
        return new StorageCenter[0];
    }

    /**
     * Find a specific Storage Center.
     *
     * @param ssn The Storage Center serial number.
     * @return The Storage Center.
     * @throws StorageCenterAPIException if the Storage Center is not found.
     */
    public StorageCenter findStorageCenter(long ssn) throws StorageCenterAPIException {
        StorageCenter[] scs = getStorageCenterInfo();
        for (StorageCenter sc : scs) {
            if (sc.serialNumber == ssn) {
                return sc;
            }
        }

        throw new StorageCenterAPIException(String.format("Unable to locate Storage Center %s", ssn));
    }

    /**
     * Creates a volume on the Storage Center.
     *
     * @param ssn The Storage Center SN on which to create the volume.
     * @param name The volume name.
     * @param storageType The storage type to use.
     * @param sizeInGB The size in GB
     * @return The Storage Center volume.
     * @throws StorageCenterAPIException
     */
    public ScVolume createVolume(String ssn, String name, String storageType, int sizeInGB) throws StorageCenterAPIException {
        LOG.debug("Creating {}GB volume: '{}'", sizeInGB, name);
        String errorMessage = "";

        Parameters params = new Parameters();
        params.add("Name", name);
        params.add("Notes", "Created by CoprHD driver.");
        params.add("Size", String.format("%d GB", sizeInGB));
        params.add("StorageCenter", ssn);

        ScStorageType[] storageTypes = getStorageTypes(ssn);
        for (ScStorageType storType : storageTypes) {
            if (storType.name.equals(storageType)) {
                params.add("StorageType", storType.instanceId);
                break;
            }
        }

        try {
            RestResult result = restClient.post("StorageCenter/ScVolume", params.toJson());
            if (checkResults(result)) {
                return gson.fromJson(result.getResult(), ScVolume.class);
            }
        } catch (Exception e) {
            errorMessage = String.format("Error creating volume: %s", e);
            LOG.warn(errorMessage);
        }

        if (errorMessage.length() == 0) {
            errorMessage = String.format("Unable to create volume %s on SC %s", name, ssn);
        }
        throw new StorageCenterAPIException(errorMessage);
    }

    /**
     * Gets a volume.
     *
     * @param instanceId the instance id of the volume.
     * @return the volume or null if not found
     */
    public ScVolume getVolume(String instanceId) {
        ScVolume result = null;

        if (instanceId != null) {
            RestResult rr = restClient.get(String.format("StorageCenter/ScVolume/%s", instanceId));
            if (checkResults(rr)) {
                result = gson.fromJson(rr.getResult(), ScVolume.class);
            }
        }

        return result;
    }

    /**
     * Delete a volume.
     *
     * If the volume can't be found we return success assuming the volume has been deleted
     * by some other means.
     *
     * @param instanceId the instance id of the volume.
     * @throws StorageCenterAPIException if error encountered deleting the volume.
     */
    public void deleteVolume(String instanceId) throws StorageCenterAPIException {
        ScVolume vol = getVolume(instanceId);

        if (vol == null) {
            LOG.warn("Volume delete request for {}, volume not found. Assuming deleted.", instanceId);
            return;
        }

        RestResult rr = restClient.delete(String.format("StorageCenter/ScVolume/%s", instanceId));
        if (!checkResults(rr)) {
            String msg = String.format("Error deleting volume %s", instanceId);
            LOG.error(msg);
            throw new StorageCenterAPIException(msg);
        }
    }

    /**
     * Find an initiator WWN or iSCSI IQN.
     * 
     * @param ssn The Storage Center SN on which to check.
     * @param iqnOrWwn The FC WWN or iSCSI IQN.
     * @return The HBA object.
     */
    private ScServerHba findServerHba(String ssn, String iqnOrWwn) {
        ScServerHba result = null;

        PayloadFilter filter = new PayloadFilter();
        filter.append("scSerialNumber", ssn);
        filter.append("instanceName", iqnOrWwn);

        RestResult rr = restClient.post("StorageCenter/ScServerHba/GetList", filter.toJson());
        if (checkResults(rr)) {
            ScServerHba[] hbas = gson.fromJson(rr.getResult(), ScServerHba[].class);
            for (ScServerHba hba : hbas) {
                // Should only return one if found, but just grab first from list
                result = hba;
                break;
            }
        }

        return result;
    }

    /**
     * Find a server definition.
     * 
     * @param ssn The Storage Center SN on which to check.
     * @param iqnOrWwn The WWN or IQN.
     * @return The server definition.
     */
    public ScServer findServer(String ssn, String iqnOrWwn) {
        ScServer result = null;
        ScServerHba hba = findServerHba(ssn, iqnOrWwn);

        if (hba != null) {
            RestResult rr = restClient.get(String.format("StorageCenter/ScServer/%s", hba.server.instanceId));
            if (checkResults(rr)) {
                result = gson.fromJson(rr.getResult(), ScServer.class);
            }
        }

        return result;
    }

    /**
     * Gets all server definions on the array.
     * 
     * @param ssn The Storage Center system serial number.
     * @return The server definitions.
     */
    public ScServer[] getServerDefinitions(String ssn) {
        PayloadFilter filter = new PayloadFilter();
        filter.append("scSerialNumber", ssn);

        RestResult rr = restClient.post("StorageCenter/ScServer/GetList", filter.toJson());
        if (checkResults(rr)) {
            return gson.fromJson(rr.getResult(), ScServer[].class);
        }

        return new ScServer[0];
    }

    /**
     * Get all storage types from the system.
     * 
     * @param ssn The Storage Center system serial number.
     * @return The storage types.
     */
    public ScStorageType[] getStorageTypes(String ssn) {

        PayloadFilter filter = new PayloadFilter();
        if (ssn != null) {
            filter.append("scSerialNumber", ssn);
        }

        RestResult rr = restClient.post("StorageCenter/ScStorageType/GetList", filter.toJson());
        if (checkResults(rr)) {
            return gson.fromJson(rr.getResult(), ScStorageType[].class);
        }

        return new ScStorageType[0];
    }

    /**
     * Gets the Storage Center usage data.
     * 
     * @param ssn The Storage Center system serial number.
     * @return The storage usage.
     */
    public StorageCenterStorageUsage getStorageUsage(String ssn) {
        RestResult rr = restClient.get(String.format("StorageCenter/StorageCenter/%s/StorageUsage", ssn));
        if (checkResults(rr)) {
            return gson.fromJson(rr.getResult(), StorageCenterStorageUsage.class);
        }

        return new StorageCenterStorageUsage();
    }

    /**
     * Gets all controller target ports.
     * 
     * @param ssn The Storage Center serial number.
     * @param type The type of port to get or Null for all types.
     * @return The controller ports.
     */
    public ScControllerPort[] getTargetPorts(String ssn, String type) {
        PayloadFilter filter = new PayloadFilter();
        filter.append("scSerialNumber", ssn);
        if (type != null && type.length() > 0) {
            filter.append("transportType", type);
        }

        RestResult rr = restClient.post("StorageCenter/ScControllerPort/GetList", filter.toJson());
        if (checkResults(rr)) {
            List<ScControllerPort> ports = new ArrayList<>();
            ScControllerPort[] scPorts = gson.fromJson(rr.getResult(), ScControllerPort[].class);
            for (ScControllerPort port : scPorts) {
                if (port.purpose.startsWith("FrontEnd") && !"FrontEndReserved".equals(port.purpose)) {
                    ports.add(port);
                }
            }

            return ports.toArray(new ScControllerPort[0]);
        }

        return new ScControllerPort[0];
    }

    /**
     * Gets all iSCSI target ports.
     * 
     * @param ssn The Storage Center serial number.
     * @return The iSCSI controller ports.
     */
    public ScControllerPort[] getIscsiTargetPorts(String ssn) {
        return getTargetPorts(ssn, "Iscsi");
    }

    /**
     * Gets all fibre channel target ports.
     * 
     * @param ssn The Storage Center serial number.
     * @return The FC controller ports.
     */
    public ScControllerPort[] getFcTargetPorts(String ssn) {
        return getTargetPorts(ssn, "FibreChannel");
    }

    /**
     * Gets all defined HBAs for a server definition.
     * 
     * @param ssn The Storage Center serial number.
     * @param serverInstanceId The server definition.
     * @return The HBAs.
     */
    public ScServerHba[] getServerHbas(String ssn, String serverInstanceId) {
        PayloadFilter filter = new PayloadFilter();
        filter.append("scSerialNumber", ssn);
        filter.append("Server", serverInstanceId);

        RestResult rr = restClient.post("StorageCenter/ScServerHba/GetList", filter.toJson());
        if (checkResults(rr)) {
            return gson.fromJson(rr.getResult(), ScServerHba[].class);
        }

        return new ScServerHba[0];
    }

    /**
     * Get the fault domains configured for a port.
     * 
     * @param instanceId The port instance ID.
     * @return The fault domains.
     */
    public ScFaultDomain[] getControllerPortFaultDomains(String instanceId) {
        RestResult rr = restClient.get(
                String.format("/StorageCenter/ScControllerPort/%s/FaultDomainList", instanceId));
        if (checkResults(rr)) {
            return gson.fromJson(rr.getResult(), ScFaultDomain[].class);
        }

        return new ScFaultDomain[0];
    }

    /**
     * Gets configuration about an iSCSI controller port.
     * 
     * @param instanceId The port instance ID.
     * @return The port configuration.
     */
    public ScControllerPortIscsiConfiguration getControllerPortIscsiConfig(String instanceId) {
        RestResult rr = restClient.get(
                String.format("/StorageCenter/ScControllerPort/%s/ControllerPortConfiguration", instanceId));
        if (checkResults(rr)) {
            return gson.fromJson(rr.getResult(), ScControllerPortIscsiConfiguration.class);
        }

        return new ScControllerPortIscsiConfiguration();
    }

    /**
     * Expand a volume to a larger size.
     * 
     * @param instanceId The volume instance ID.
     * @param newSize The new size.
     * @return The ScVolume object.
     * @throws StorageCenterAPIException
     */
    public ScVolume expandVolume(String instanceId, int newSize) throws StorageCenterAPIException {
        LOG.debug("Expanding volume '{}' to {}GB", instanceId, newSize);

        Parameters params = new Parameters();
        params.add("NewSize", String.format("%d GB", newSize));

        try {
            RestResult result = restClient.post(
                    String.format("StorageCenter/ScVolume/%s/ExpandToSize", instanceId),
                    params.toJson());
            if (checkResults(result)) {
                return gson.fromJson(result.getResult(), ScVolume.class);
            }

            throw new StorageCenterAPIException(
                    String.format("Failed to expande volume: %s", result.getErrorMsg()));
        } catch (Exception e) {
            LOG.warn(String.format("Error expanding volume: %s", e));
            throw new StorageCenterAPIException("Error expanding volume", e);
        }
    }
}