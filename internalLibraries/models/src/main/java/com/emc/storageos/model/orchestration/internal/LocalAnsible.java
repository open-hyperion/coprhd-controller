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
package com.emc.storageos.model.orchestration.internal;

import javax.xml.bind.annotation.XmlRootElement;

import com.emc.storageos.model.orchestration.internal.BasicInputParameter.StringParameter;
import com.emc.storageos.model.orchestration.internal.BasicOutputParameter.NameValueListOutputParameter;

/**
 * Class that represents primitive meta data for an ansible script 
 * that can be executed locally
 */
@XmlRootElement(name = "primitive")
public class LocalAnsible extends Primitive {

    private final static String FRIENDLY_NAME = "Ansible playbook";
    private final static String DESCRIPTION = "Locally executed ansible playbook";
    private final static String SUCCESS_CRITERIA = "code = 0";
    
    private final static StringParameter CONTENT = new StringParameter("content", true, null); 
    private final static InputParameter INPUT[] = {CONTENT}; 
    
    private final static NameValueListOutputParameter OUTPUT = new NameValueListOutputParameter("output");
    private final static BasicOutputParameter OUTPUT_LIST[]  = {OUTPUT};

    public LocalAnsible() {
        super(LocalAnsible.class.getName(), FRIENDLY_NAME, DESCRIPTION, SUCCESS_CRITERIA, INPUT, OUTPUT_LIST);
    }

}