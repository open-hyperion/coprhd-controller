/*
 * Copyright (c) 2012 EMC Corporation
 * All Rights Reserved
 */
package com.emc.storageos.api.service.impl.resource.snapshot;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.SecurityContext;

import com.emc.storageos.api.service.authorization.PermissionsHelper;
import com.emc.storageos.api.service.impl.resource.fullcopy.BlockFullCopyManager;
import com.emc.storageos.blockorchestrationcontroller.BlockOrchestrationController;
import com.emc.storageos.blockorchestrationcontroller.VolumeDescriptor;
import com.emc.storageos.coordinator.client.service.CoordinatorClient;
import com.emc.storageos.db.client.DbClient;
import com.emc.storageos.db.client.constraint.ContainmentConstraint;
import com.emc.storageos.db.client.model.BlockObject;
import com.emc.storageos.db.client.model.BlockSnapshotSession;
import com.emc.storageos.db.client.model.Operation;
import com.emc.storageos.db.client.model.Project;
import com.emc.storageos.db.client.model.StorageSystem;
import com.emc.storageos.db.client.model.StringSet;
import com.emc.storageos.db.client.util.CustomQueryUtility;
import com.emc.storageos.model.block.VolumeDeleteTypeEnum;
import com.emc.storageos.services.OperationTypeEnum;
import com.emc.storageos.svcs.errorhandling.resources.APIException;
import com.emc.storageos.volumecontroller.BlockController;
import com.emc.storageos.volumecontroller.impl.utils.VirtualPoolCapabilityValuesWrapper;

/**
 * Block snapshot session implementation for volumes a VMAX3 systems.
 */
public class VMAX3BlockSnapshotSessionApiImpl extends DefaultBlockSnapshotSessionApiImpl {

    private static final int MAX_LINKED_TARGETS_PER_SOURCE = 1024;
    private static final int MAX_SNAPSHOTS_PER_SOURCE = 256;

    /**
     * Private default constructor should not be called outside class.
     */
    @SuppressWarnings("unused")
    private VMAX3BlockSnapshotSessionApiImpl() {
        super();
    }

    /**
     * Constructor.
     * 
     * @param dbClient A reference to a data base client.
     * @param coordinator A reference to the coordinator client.
     * @param permissionsHelper A reference to a permission helper.
     * @param securityContext A reference to the security context.
     * @param blockSnapshotSessionMgr A reference to the snapshot session manager.
     */
    public VMAX3BlockSnapshotSessionApiImpl(DbClient dbClient, CoordinatorClient coordinator, PermissionsHelper permissionsHelper,
            SecurityContext securityContext, BlockSnapshotSessionManager blockSnapshotSessionMgr) {
        super(dbClient, coordinator, permissionsHelper, securityContext, blockSnapshotSessionMgr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateSnapshotSessionCreateRequest(BlockObject requestedSourceObj, List<BlockObject> sourceObjList, Project project,
            String name, int newTargetsCount, String newTargetsName, String newTargetCopyMode, boolean skipInternalCheck,
            BlockFullCopyManager fcManager) {
        // Do the super class validation.
        super.validateSnapshotSessionCreateRequest(requestedSourceObj, sourceObjList, project, name, newTargetsCount,
                newTargetsName, newTargetCopyMode, skipInternalCheck, fcManager);

        // Verify new target copy mode is a valid value.
        verifyNewTargetCopyMode(newTargetCopyMode);
    }

    /**
     * Verifies the copy mode specified is valid.
     * 
     * @param copyMode The copy mode specified in the request.
     */
    private void verifyNewTargetCopyMode(String copyMode) {
        if ((!BlockSnapshotSession.CopyMode.copy.name().equals(copyMode))
                && (!BlockSnapshotSession.CopyMode.nocopy.name().equals(copyMode))) {
            throw APIException.badRequests.invalidCopyModeForLinkedTarget(copyMode);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void verifyNewTargetCount(BlockObject sourceObj, int newTargetsCount, boolean zeroIsValid) {
        // Call super first.
        super.verifyNewTargetCount(sourceObj, newTargetsCount, zeroIsValid);

        // Now make sure max is not exceeded.
        if (newTargetsCount > 0) {
            // The total number of linked targets for all sessions for a given
            // source can't exceed 1024. So, get all sessions for the source
            // and add all linked targets for these sessions. That value plus
            // the number of new targets cannot exceed the max.
            int totalLinkedTargets = newTargetsCount;
            List<BlockSnapshotSession> snapSessions = CustomQueryUtility.queryActiveResourcesByConstraint(_dbClient,
                    BlockSnapshotSession.class, ContainmentConstraint.Factory.getParentSnapshotSessionConstraint(sourceObj.getId()));
            for (BlockSnapshotSession snapSession : snapSessions) {
                StringSet linkedTargetIds = snapSession.getLinkedTargets();
                if (linkedTargetIds != null) {
                    totalLinkedTargets += linkedTargetIds.size();
                }
            }

            if (totalLinkedTargets > MAX_LINKED_TARGETS_PER_SOURCE) {
                throw APIException.badRequests.invalidNewLinkedTargetsCount(newTargetsCount, sourceObj.getLabel(),
                        MAX_LINKED_TARGETS_PER_SOURCE - totalLinkedTargets);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getMaxSnapshotsForSource() {
        return MAX_SNAPSHOTS_PER_SOURCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createSnapshotSession(BlockObject sourceObj, URI snapSessionURI,
            List<List<URI>> snapSessionSnapshotURIs, String copyMode, String taskId) {
        // Invoke the BlockDeviceController to create the array snapshot session and create and link
        // target volumes as necessary.
        StorageSystem storageSystem = _dbClient.queryObject(StorageSystem.class, sourceObj.getStorageController());
        
        VirtualPoolCapabilityValuesWrapper capabilities = new VirtualPoolCapabilityValuesWrapper();
        capabilities.put(VirtualPoolCapabilityValuesWrapper.SNAPSHOT_SESSION_COPY_MODE, copyMode);
        VolumeDescriptor sessionDescriptor = new VolumeDescriptor(VolumeDescriptor.Type.BLOCK_SNAPSHOT_SESSION,
                storageSystem.getId(), snapSessionURI, null, sourceObj.getConsistencyGroup(), capabilities,
                snapSessionSnapshotURIs);
        
        List<VolumeDescriptor> volumeDescriptors = new ArrayList<VolumeDescriptor>();
        volumeDescriptors.add(sessionDescriptor);
        
        BlockOrchestrationController controller = getController(BlockOrchestrationController.class,
                BlockOrchestrationController.BLOCK_ORCHESTRATION_DEVICE);
        controller.createSnapshotSession(volumeDescriptors, taskId);
        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateLinkNewTargetsRequest(BlockObject snapSessionSourceObj, Project project, int newTargetsCount,
            String newTargetsName, String newTargetCopyMode) {
        // Do the super class validation.
        super.validateLinkNewTargetsRequest(snapSessionSourceObj, project, newTargetsCount, newTargetsName, newTargetCopyMode);

        // Verify new target copy mode is a valid value.
        verifyNewTargetCopyMode(newTargetCopyMode);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void linkNewTargetVolumesToSnapshotSession(BlockObject snapSessionSourceObj, BlockSnapshotSession snapSession,
                                                      List<List<URI>> snapshotURIs, String copyMode, String taskId) {
        // Invoke the BlockDeviceController to create and link new target
        // volumes to the passed snapshot session.
        StorageSystem storageSystem = _dbClient.queryObject(StorageSystem.class, snapSessionSourceObj.getStorageController());
        BlockController controller = getController(BlockController.class, storageSystem.getSystemType());
        controller.linkNewTargetVolumesToSnapshotSession(storageSystem.getId(), snapSession.getId(), snapshotURIs, copyMode, taskId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void relinkTargetVolumesToSnapshotSession(BlockObject snapSessionSourceObj, BlockSnapshotSession tgtSnapSession,
            List<URI> snapshotURIs, String taskId) {
        // Invoke the BlockDeviceController to relink the targets
        // to the passed target snapshot session.
        StorageSystem storageSystem = _dbClient.queryObject(StorageSystem.class, snapSessionSourceObj.getStorageController());
        BlockController controller = getController(BlockController.class, storageSystem.getSystemType());
        controller.relinkTargetsToSnapshotSession(storageSystem.getId(), tgtSnapSession.getId(), snapshotURIs, Boolean.TRUE, taskId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unlinkTargetVolumesFromSnapshotSession(BlockObject snapSessionSourceObj, BlockSnapshotSession snapSession,
            Map<URI, Boolean> snapshotDeletionMap, OperationTypeEnum opType, String taskId) {
        // Invoke the BlockDeviceController to unlink the targets
        // from the snapshot session.
        StorageSystem storageSystem = _dbClient.queryObject(StorageSystem.class, snapSessionSourceObj.getStorageController());
        BlockController controller = getController(BlockController.class, storageSystem.getSystemType());
        controller.unlinkTargetsFromSnapshotSession(storageSystem.getId(), snapSession.getId(), snapshotDeletionMap, opType, taskId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void restoreSnapshotSession(BlockSnapshotSession snapSession, BlockObject snapSessionSourceObj, String taskId) {
        // Invoke the BlockDeviceController to restore the snapshot session source.
        StorageSystem storageSystem = _dbClient.queryObject(StorageSystem.class, snapSessionSourceObj.getStorageController());
        BlockController controller = getController(BlockController.class, storageSystem.getSystemType());
        controller.restoreSnapshotSession(storageSystem.getId(), snapSession.getId(), Boolean.TRUE, taskId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteSnapshotSession(BlockSnapshotSession snapSession, BlockObject snapSessionSourceObj, String taskId, String deleteType) {
        if (VolumeDeleteTypeEnum.VIPR_ONLY.name().equals(deleteType)) {
            // Update the task status for the session.
            // Note that we must get the session form the database to get the latest status map.
            BlockSnapshotSession updatedSession = _dbClient.queryObject(BlockSnapshotSession.class, snapSession.getId());
            Operation op = updatedSession.getOpStatus().get(taskId);
            op.ready("Snapshot session succesfully deleted from ViPR");
            updatedSession.getOpStatus().updateTaskStatus(taskId, op);
            _dbClient.updateObject(updatedSession);

            // Mark the snapshot session for deletion.
            _dbClient.markForDeletion(updatedSession);
        } else {
            // Invoke the BlockDeviceController to delete the snapshot session.
            StorageSystem storageSystem = _dbClient.queryObject(StorageSystem.class, snapSessionSourceObj.getStorageController());
            BlockController controller = getController(BlockController.class, storageSystem.getSystemType());
            controller.deleteSnapshotSession(storageSystem.getId(), snapSession.getId(), taskId);
        }
    }
}
