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

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "primitives")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class PrimitiveList {

    private List<PrimitiveRestRep> primitives;
    
    public PrimitiveList() {
        
    }
    
    public PrimitiveList(List<PrimitiveRestRep> primitives) {
        this.primitives = primitives;
    }
    
    @XmlElement(name = "primitive")
    public List<PrimitiveRestRep> getPrimitives() {
        return primitives;
    }

    public void setPrimitives(List<PrimitiveRestRep> primitives) {
        this.primitives = primitives;
    }
}