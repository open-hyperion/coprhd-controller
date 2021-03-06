/*
 * Copyright 2015 EMC Corporation
 * All Rights Reserved
 */

package com.emc.storageos.volumecontroller;

import java.net.URI;

import com.emc.storageos.db.client.model.Bucket;
import com.emc.storageos.db.client.model.ObjectUserSecretKey;
import com.emc.storageos.db.client.model.StorageSystem;
import com.emc.storageos.model.object.BucketACLUpdateParams;
import com.emc.storageos.svcs.errorhandling.resources.InternalException;
import com.emc.storageos.volumecontroller.impl.BiosCommandResult;

public interface ObjectStorageDevice {

    /**
     * 
     * @param storageObj storage system
     * @param bucket Bucket instance
     * @param ob Device specific paras
     * @param taskId Task ID
     * @return Result of command
     * @throws ControllerException if create fails
     */

    BiosCommandResult doCreateBucket(StorageSystem storageObj, Bucket bucket, ObjectDeviceInputOutput ob, String taskId)
            throws ControllerException;

    /**
     * Update Bucket information to the Object Storage (Storage System)
     * 
     * @param storageObj Storage system instance
     * @param bucket Bucket instance
     * @param softQuota Soft Quota for a bucket
     * @param hardQuota Hard Quota for a bucket
     * @param retention Retention period on a bucket
     * @param taskId Task ID
     * @return Result of operation
     * @throws ControllerException if Update fails
     */
    BiosCommandResult doUpdateBucket(StorageSystem storageObj, Bucket bucket, Long softQuota, Long hardQuota,
            Integer retention, String taskId) throws ControllerException;

    /**
     * 
     * @param storageObj Storage system instance
     * @param bucket Bucket instance
     * @param taskId Task ID
     * @return Result of operation
     * @throws ControllerException if Delete fails
     */
    BiosCommandResult doDeleteBucket(StorageSystem storageObj, Bucket bucket, String deleteType, String taskId) throws ControllerException;
    
    /**
     * @param storageObj
     * @param objectArgs
     * @return
     * @throws ControllerException
     */
    BiosCommandResult doUpdateBucketACL(StorageSystem storageObj, Bucket bucket, ObjectDeviceInputOutput objectArgs, BucketACLUpdateParams param, String taskId) throws ControllerException;
    
    /**
     * @param storageObj
     * @param objectArgs
     * @return
     * @throws ControllerException
     */
    BiosCommandResult doDeleteBucketACL(StorageSystem storageObj, Bucket bucket, ObjectDeviceInputOutput objectArgs, String taskId) throws ControllerException;
    

    /**
     * Sync bucket ACL with the Object storage
     * @param storageObj
     * @param bucket
     * @param objectArgs
     * @param taskId
     * @return
     * @throws ControllerException
     */
    BiosCommandResult doSyncBucketACL(StorageSystem storageObj, Bucket bucket, ObjectDeviceInputOutput objectArgs, String taskId) throws ControllerException;

    /**
     * Add user secret keys
     * @param storageObj
     * @param userId
     * @param secretKey
     * @return
     * @throws InternalException
     */
    ObjectUserSecretKey doAddUserSecretKey(StorageSystem storageObj, String userId, String secretKey) throws InternalException;
   

}
