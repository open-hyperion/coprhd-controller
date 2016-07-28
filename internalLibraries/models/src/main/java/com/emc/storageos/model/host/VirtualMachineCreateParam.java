/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */
package com.emc.storageos.model.host;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonProperty;

import com.emc.storageos.model.valid.Endpoint;

/**
 * Request POST parameter for host creation.
 */
@XmlRootElement(name = "virtual_machine_create")
public class VirtualMachineCreateParam extends VirtualMachineParam {
    public VirtualMachineCreateParam() {
        setDiscoverable(true);
    }

    /**
     * The host type.
     * Valid values:
     * Windows
     * HPUX
     * Linux
     * Esx
     * SUNVCS
     * Other
     */
    @Override
    @XmlElement(required = true)
    public String getType() {
        return super.getType();
    }

    /**
     * The short or fully qualified host name or IP address of the host
     * management interface.
     * 
     */
    @Override
    @XmlElement(name = "host_name", required = true)
    @Endpoint(type = Endpoint.EndpointType.HOST)
    @JsonProperty("host_name")
    public String getHostName() {
        return super.getHostName();
    }

    /**
     * The user label for this host.
     * 
     */
    @Override
    @XmlElement(required = true)
    public String getName() {
        return super.getName();
    }
}