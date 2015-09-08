/*
 * Copyright (c) 2012 EMC Corporation
 * All Rights Reserved
 */
package com.emc.storageos.volumecontroller.impl.smis.job;

import java.net.URI;

import javax.cim.CIMObjectPath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.emc.storageos.db.client.DbClient;
import com.emc.storageos.db.client.URIUtil;
import com.emc.storageos.db.client.model.BlockSnapshot;
import com.emc.storageos.db.client.model.OpStatusMap;
import com.emc.storageos.db.client.model.Volume;
import com.emc.storageos.volumecontroller.JobContext;
import com.emc.storageos.volumecontroller.TaskCompleter;
import com.emc.storageos.volumecontroller.impl.block.taskcompleter.BlockSnapshotSessionUnlinkTargetCompleter;

/**
 * ViPR Job created when an underlying CIM job is created to unlink
 * a target volume from an array snapshot.
 */
@SuppressWarnings("serial")
public class SmisBlockSnapshotSessionUnlinkTargetJob extends SmisJob {

    // The unique job name.
    private static final String JOB_NAME = "SmisBlockSnapshotSessionUnlinkTargetJob";

    // Reference to a logger.
    private static final Logger s_logger = LoggerFactory.getLogger(SmisBlockSnapshotSessionUnlinkTargetJob.class);

    /**
     * Constructor.
     * 
     * @param cimJob The CIM object path of the underlying CIM Job.
     * @param systemURI The URI of the storage system.
     * @param taskCompleter A reference to the task completer.
     */
    public SmisBlockSnapshotSessionUnlinkTargetJob(CIMObjectPath cimJob, URI systemURI, TaskCompleter taskCompleter) {
        super(cimJob, systemURI, taskCompleter, JOB_NAME);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateStatus(JobContext jobContext) throws Exception {
        DbClient dbClient = jobContext.getDbClient();
        JobStatus jobStatus = getJobStatus();
        BlockSnapshotSessionUnlinkTargetCompleter completer = (BlockSnapshotSessionUnlinkTargetCompleter) getTaskCompleter();
        boolean deleteTarget = completer.getDeleteTarget();
        try {
            if (jobStatus == JobStatus.IN_PROGRESS) {
                return;
            }
            if (jobStatus == JobStatus.SUCCESS) {
                // If we successfully unlinked the target from the array
                // snapshot, but we are not deleting the target, we need
                // to convert the BlockSnapshotInstance to a Volume instance
                // as the device is no longer a snapshot target and so should
                // not be represented by a BlockSnapshot instance in ViPR.
                if (!deleteTarget) {
                    BlockSnapshot snapshot = dbClient.queryObject(BlockSnapshot.class, completer.getId());
                    URI sourceObjURI = snapshot.getParent().getURI();
                    if (URIUtil.isType(sourceObjURI, Volume.class)) {
                        Volume sourceVolume = dbClient.queryObject(Volume.class, sourceObjURI);

                        // Create a new volume to represent the former snapshot target.
                        // We get what we can from the snapshot and for what is not
                        // available in the snapshot, we get from the source.
                        Volume volume = new Volume();
                        volume.setId(URIUtil.createId(Volume.class));
                        volume.setCreationTime(snapshot.getCreationTime());
                        volume.setWWN(snapshot.getWWN());
                        volume.setNativeGuid(snapshot.getNativeGuid());
                        volume.setNativeId(snapshot.getNativeId());
                        volume.setLabel(snapshot.getLabel());
                        volume.setDeviceLabel(snapshot.getDeviceLabel());
                        volume.setAlternateName(snapshot.getAlternateName());
                        volume.setSyncActive(true);
                        volume.setAccessState(sourceVolume.getAccessState());
                        volume.setCapacity(sourceVolume.getCapacity());
                        volume.setProvisionedCapacity(snapshot.getProvisionedCapacity());
                        volume.setAllocatedCapacity(snapshot.getAllocatedCapacity());
                        volume.setThinlyProvisioned(sourceVolume.getThinlyProvisioned());
                        volume.setVirtualPool(sourceVolume.getVirtualPool()); // TBD - This is not necessary true. What to do?
                        volume.setVirtualArray(snapshot.getVirtualArray());
                        volume.setProject(snapshot.getProject());
                        volume.setTenant(sourceVolume.getTenant());
                        volume.setStorageController(snapshot.getStorageController());
                        volume.setPool(sourceVolume.getPool()); // TBD - This is not necessary true. What to do?
                        volume.setProtocol(snapshot.getProtocol());
                        volume.setOpStatus(new OpStatusMap());
                        volume.setConsistencyGroup(snapshot.getConsistencyGroup());
                        dbClient.createObject(volume);
                    } else {
                        // TBD - Future handle block snapshot source.
                    }
                } else {
                    // TBD - Update capacity of storage pools when deleted?
                    // SmisUtils.updateStoragePoolCapacity(dbClient, client, poolURI);
                }

                s_logger.info("Post-processing successful for unlink snapshot session target for task ", getTaskCompleter().getOpId());
            } else if (jobStatus == JobStatus.FAILED || jobStatus == JobStatus.FATAL_ERROR) {
                s_logger.info("Failed to unlink snapshot session target for task ", getTaskCompleter().getOpId());
            }
        } catch (Exception e) {
            setPostProcessingErrorStatus("Encountered an internal error in unlink snapshot session target job status processing: "
                    + e.getMessage());
            s_logger.error("Encountered an internal error in unlink snapshot session target job status processing", e);
        } finally {
            if (!deleteTarget) {
                // We only want to invoke the completer if we are
                // not deleting the target after we unlink the target
                // from the array snapshot.
                super.updateStatus(jobContext);
            }
        }
    }
}
