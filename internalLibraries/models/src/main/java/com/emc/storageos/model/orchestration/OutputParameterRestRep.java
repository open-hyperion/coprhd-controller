/*
 * Copyright 2016 Dell Inc. or its subsidiaries.
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
package com.emc.storageos.model.orchestration;

import javax.xml.bind.annotation.XmlElement;

import com.emc.storageos.model.orchestration.internal.Parameter.ParameterType;

/**
 * Rest representation class for an orchestration output parameter
 */
public class OutputParameterRestRep {

    private String name;
    private ParameterType type;
    private String table;
    
    @XmlElement(name = "name")
    public String getName() {
        return name;
    }
    
    public void setName(final String name) {
        this.name = name;
    }
    
    @XmlElement(name = "type")
    public ParameterType getType() {
        return type;
    }

    public void setType(ParameterType type) {
        this.type = type;
    }

    @XmlElement(name = "table")
    public String getTable() {
        return table;
    }
    public void setTable(final String table) {
        this.table = table;
    }
}