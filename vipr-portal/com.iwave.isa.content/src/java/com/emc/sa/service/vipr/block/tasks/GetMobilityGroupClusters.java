/*
 * Copyright (c) 2015 EMC
 * All Rights Reserved
 */
package com.emc.sa.service.vipr.block.tasks;

import java.net.URI;
import java.util.List;

import com.emc.sa.service.vipr.tasks.ViPRExecutionTask;
import com.emc.storageos.model.NamedRelatedResourceRep;

public class GetMobilityGroupClusters extends ViPRExecutionTask<List<NamedRelatedResourceRep>> {
    private final URI mobilityGroup;

    public GetMobilityGroupClusters(URI mobilityGroup) {
        this.mobilityGroup = mobilityGroup;
        provideDetailArgs(mobilityGroup);
    }

    @Override
    public List<NamedRelatedResourceRep> executeTask() throws Exception {
        return getClient().application().getClusters(mobilityGroup);
    }
}
