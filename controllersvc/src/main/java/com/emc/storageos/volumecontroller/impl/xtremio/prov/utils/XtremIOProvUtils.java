/*
 * Copyright (c) 2014 EMC Corporation
 * All Rights Reserved
 */
package com.emc.storageos.volumecontroller.impl.xtremio.prov.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.emc.storageos.db.client.DbClient;
import com.emc.storageos.db.client.model.StoragePool;
import com.emc.storageos.db.client.model.StorageSystem;
import com.emc.storageos.xtremio.restapi.XtremIOClient;
import com.emc.storageos.xtremio.restapi.XtremIOConstants;
import com.emc.storageos.xtremio.restapi.XtremIOConstants.XTREMIO_ENTITY_TYPE;
import com.emc.storageos.xtremio.restapi.model.response.XtremIOConsistencyGroup;
import com.emc.storageos.xtremio.restapi.model.response.XtremIOSystem;
import com.emc.storageos.xtremio.restapi.model.response.XtremIOTag;
import com.emc.storageos.xtremio.restapi.model.response.XtremIOVolume;
import com.google.common.base.Joiner;

public class XtremIOProvUtils {

    private static final Logger _log = LoggerFactory.getLogger(XtremIOProvUtils.class);

    public static void updateStoragePoolCapacity(XtremIOClient client, DbClient dbClient, StoragePool storagePool) {
        try {
            StorageSystem storageSystem = dbClient.queryObject(StorageSystem.class, storagePool.getStorageDevice());
            _log.info(String.format("Old storage pool capacity data for %n  pool %s/%s --- %n  free capacity: %s; subscribed capacity: %s",
                    storageSystem.getId(), storagePool.getId(),
                    storagePool.calculateFreeCapacityWithoutReservations(),
                    storagePool.getSubscribedCapacity()));
            List<XtremIOSystem> systems = client.getXtremIOSystemInfo();
            for (XtremIOSystem system : systems) {
                if (system.getSerialNumber().equalsIgnoreCase(storageSystem.getSerialNumber())) {
                    storagePool.setFreeCapacity(system.getTotalCapacity() - system.getUsedCapacity());
                    dbClient.persistObject(storagePool);
                    break;
                }
            }
            _log.info(String.format("New storage pool capacity data for %n  pool %s/%s --- %n  free capacity: %s; subscribed capacity: %s",
                    storageSystem.getId(), storagePool.getId(),
                    storagePool.calculateFreeCapacityWithoutReservations(),
                    storagePool.getSubscribedCapacity()));
        } catch (Exception e) {
            _log.warn("Problem when updating pool capacity for pool {}", storagePool.getNativeGuid());
        }
    }

    public static XtremIOVolume isVolumeAvailableInArray(XtremIOClient client, String label, String clusterName) {
        XtremIOVolume volume = null;
        try {
            volume = client.getVolumeDetails(label, clusterName);
        } catch (Exception e) {
            _log.info("Volume {} already deleted.", label);
        }
        return volume;
    }

    public static XtremIOVolume isSnapAvailableInArray(XtremIOClient client, String label, String clusterName) {
        XtremIOVolume volume = null;
        try {
            volume = client.getSnapShotDetails(label, clusterName);
        } catch (Exception e) {
            _log.info("Snapshot {} not available in Array.", label);
        }
        return volume;
    }
    
    public static XtremIOConsistencyGroup isCGAvailableInArray(XtremIOClient client, String label, String clusterName) {
    	XtremIOConsistencyGroup cg = null;
    	try {
    		cg = client.getConsistencyGroupDetails(label, clusterName);
    	} catch (Exception e) {
            _log.info("Consistency group {} not available in Array.", label);
        }
    	
    	return cg;
    }
    
    public static Map<String, String> createFoldersForVolumeAndSnaps(XtremIOClient client, String rootVolumeFolderName)
            throws Exception {
        
        List<String> folderNames = client.getVolumeFolderNames();
        _log.info("Volume folder Names found on Array : {}", Joiner.on("; ").join(folderNames));
        Map<String, String> folderNamesMap = new HashMap<String, String>();
        String rootFolderName = XtremIOConstants.V1_ROOT_FOLDER.concat(rootVolumeFolderName);
        _log.info("rootVolumeFolderName: {}", rootFolderName);
        String volumesFolderName = rootFolderName.concat(XtremIOConstants.VOLUMES_SUBFOLDER);
        String snapshotsFolderName = rootFolderName.concat(XtremIOConstants.SNAPSHOTS_SUBFOLDER);
        folderNamesMap.put(XtremIOConstants.VOLUME_KEY, volumesFolderName);
        folderNamesMap.put(XtremIOConstants.SNAPSHOT_KEY, snapshotsFolderName);
        if (!folderNames.contains(rootFolderName)) {
            _log.info("Sending create root folder request {}", rootFolderName);
            client.createTag(rootVolumeFolderName, "/", XtremIOConstants.XTREMIO_ENTITY_TYPE.Volume.name(), null);
        } else {
            _log.info("Found {} folder on the Array.", rootFolderName);
        }

        if (!folderNames.contains(volumesFolderName)) {
            _log.info("Sending create volume folder request {}", volumesFolderName);
            client.createTag("volumes", rootFolderName, XtremIOConstants.XTREMIO_ENTITY_TYPE.Volume.name(), null);
        } else {
            _log.info("Found {} folder on the Array.", volumesFolderName);
        }

        if (!folderNames.contains(snapshotsFolderName)) {
            _log.info("Sending create snapshot folder request {}", snapshotsFolderName);
            client.createTag("snapshots", rootFolderName, XtremIOConstants.XTREMIO_ENTITY_TYPE.Volume.name(), null);
        } else {
            _log.info("Found {} folder on the Array.", snapshotsFolderName);
        }

        return folderNamesMap;
    }
    
    public static Map<String, String> createTagsForVolumeAndSnaps(XtremIOClient client, String rootTagName, String clusterName)
            throws Exception {
        List<String> tagNames = client.getTagNames(clusterName);
        _log.info("Tag Names found on Array : {}", Joiner.on("; ").join(tagNames));
        Map<String, String> tagNamesMap = new HashMap<String, String>();
        String volumesTagName = XtremIOConstants.V2_VOLUME_ROOT_FOLDER.concat(rootTagName);
        String snapshotsTagName = XtremIOConstants.V2_SNAPSHOT_ROOT_FOLDER.concat(rootTagName);
        tagNamesMap.put(XtremIOConstants.VOLUME_KEY, volumesTagName);
        tagNamesMap.put(XtremIOConstants.SNAPSHOT_KEY, snapshotsTagName);
        
        if (!tagNames.contains(XtremIOConstants.V2_VOLUME_ROOT_FOLDER.concat(volumesTagName))) {
            _log.info("Sending create volume tag request {}", volumesTagName);
            client.createTag(volumesTagName, null, XtremIOConstants.XTREMIO_ENTITY_TYPE.Volume.name(), clusterName);
        } else {
            _log.info("Found {} tag on the Array.", volumesTagName);
        }
        
        if (!tagNames.contains(XtremIOConstants.V2_SNAPSHOT_ROOT_FOLDER.concat(snapshotsTagName))) {
            _log.info("Sending create snapshot tag request {}", snapshotsTagName);
            client.createTag(snapshotsTagName, null, XtremIOConstants.XTREMIO_ENTITY_TYPE.SnapshotSet.name(), clusterName);
        } else {
            _log.info("Found {} tag on the Array.", snapshotsTagName);
        }
        
        return tagNamesMap;
        
    }
    
    public static void cleanupVolumeFoldersIfNeeded(XtremIOClient client, String xioClusterName, String volumeFolderName,
            StorageSystem storageSystem) throws Exception {
        try {
            boolean isVersion2 = client.isVersion2();
            // Find the # volumes in folder, if the Volume folder is empty,
            // then delete the folder too
            XtremIOTag tag = client.getTagDetails(volumeFolderName, XTREMIO_ENTITY_TYPE.Volume.name(), xioClusterName);
            int numberOfVolumes = Integer.parseInt(tag.getNumberOfVolumes());
            if(numberOfVolumes == 0) {
                if(isVersion2) {
                    client.deleteTag(volumeFolderName, XtremIOConstants.XTREMIO_ENTITY_TYPE.Volume.name(), xioClusterName);
                } else {
                    String volumesFolderName = volumeFolderName.concat(XtremIOConstants.VOLUMES_SUBFOLDER);
                    String snapshotsFolderName = volumeFolderName.concat(XtremIOConstants.SNAPSHOTS_SUBFOLDER);
                    _log.info("Deleting Volumes Folder ...");
                    client.deleteTag(volumesFolderName, XtremIOConstants.XTREMIO_ENTITY_TYPE.Volume.name(), xioClusterName);
                    _log.info("Deleting Snapshots Folder ...");
                    client.deleteTag(snapshotsFolderName, XtremIOConstants.XTREMIO_ENTITY_TYPE.Volume.name(), xioClusterName);
                    _log.info("Deleting Root Folder ...");
                    client.deleteTag(volumeFolderName, XtremIOConstants.XTREMIO_ENTITY_TYPE.Volume.name(), xioClusterName);
                }
            }
        } catch (Exception e) {
            _log.warn("Deleting root folder {} failed", volumeFolderName, e);
        }
    }
}
