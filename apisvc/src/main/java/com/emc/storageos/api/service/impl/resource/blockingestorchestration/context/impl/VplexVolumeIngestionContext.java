/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */
package com.emc.storageos.api.service.impl.resource.blockingestorchestration.context.impl;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.emc.storageos.api.service.impl.resource.blockingestorchestration.BlockIngestOrchestrator;
import com.emc.storageos.api.service.impl.resource.blockingestorchestration.context.IngestionRequestContext;
import com.emc.storageos.api.service.impl.resource.blockingestorchestration.context.VolumeIngestionContext;
import com.emc.storageos.api.service.impl.resource.blockingestorchestration.context.impl.BaseIngestionRequestContext.VolumeIngestionContextFactory;
import com.emc.storageos.api.service.impl.resource.utils.VolumeIngestionUtil;
import com.emc.storageos.db.client.DbClient;
import com.emc.storageos.db.client.URIUtil;
import com.emc.storageos.db.client.model.BlockConsistencyGroup;
import com.emc.storageos.db.client.model.BlockObject;
import com.emc.storageos.db.client.model.BlockSnapshot;
import com.emc.storageos.db.client.model.DataObject;
import com.emc.storageos.db.client.model.DataObject.Flag;
import com.emc.storageos.db.client.model.ExportGroup;
import com.emc.storageos.db.client.model.ExportMask;
import com.emc.storageos.db.client.model.Initiator;
import com.emc.storageos.db.client.model.NamedURI;
import com.emc.storageos.db.client.model.Project;
import com.emc.storageos.db.client.model.StorageSystem;
import com.emc.storageos.db.client.model.StringMap;
import com.emc.storageos.db.client.model.StringSet;
import com.emc.storageos.db.client.model.TenantOrg;
import com.emc.storageos.db.client.model.VirtualArray;
import com.emc.storageos.db.client.model.VirtualPool;
import com.emc.storageos.db.client.model.Volume;
import com.emc.storageos.db.client.model.VplexMirror;
import com.emc.storageos.db.client.model.UnManagedDiscoveredObjects.UnManagedConsistencyGroup;
import com.emc.storageos.db.client.model.UnManagedDiscoveredObjects.UnManagedVolume;
import com.emc.storageos.db.client.model.UnManagedDiscoveredObjects.UnManagedVolume.SupportedVolumeInformation;
import com.emc.storageos.db.client.util.NullColumnValueGetter;
import com.emc.storageos.util.ConnectivityUtil;
import com.emc.storageos.vplex.api.VPlexApiConstants;
import com.emc.storageos.vplexcontroller.VPlexControllerUtils;
import com.emc.storageos.vplexcontroller.VplexBackendIngestionContext;

/**
 * A combined implementation of VolumeIngestionContext and IngestionRequestContext
 * for VPLEX volumes.
 *
 * The VolumeIngestionContext implementation serves as context for ingestion of the
 * parent VPLEX virtual volume. The IngestionRequestContext implementation serves
 * as context for processing the VPLEX virtual volume's backend structure.
 *
 * This class extends VplexBackendIngestionContext which is the core of context
 * data for the backend volumes (and is also used separately
 * by the VPLEX discovery process).
 */
public class VplexVolumeIngestionContext extends VplexBackendIngestionContext implements VolumeIngestionContext, IngestionRequestContext {

    private Map<String, VolumeIngestionContext> _processedUnManagedVolumeMap;
    private Map<String, BlockObject> _blockObjectsToBeCreatedMap;
    private Map<String, Set<DataObject>> _dataObjectsToBeUpdatedMap;
    private Map<String, Set<DataObject>> _dataObjectsToBeCreatedMap;
    private List<UnManagedVolume> _unManagedVolumesToBeDeleted;

    private final IngestionRequestContext _parentRequestContext;
    private VolumeIngestionContext _currentBackendVolumeIngestionContext;
    private Iterator<UnManagedVolume> _backendVolumeUrisToProcessIterator;
    private List<VplexMirror> _createdVplexMirrors;
    private String _haClusterId;
    private String _virtualVolumeVplexClusterName;

    private List<String> _errorMessages;

    // export ingestion related items
    private boolean _exportGroupCreated = false;
    private ExportGroup _exportGroup;
    private List<Initiator> _deviceInitiators;
    private List<BlockObject> _objectsIngestedByExportProcessing;
    private Map<BlockObject, ExportGroup> _vplexBackendExportGroupMap;

    private Map<String, BlockConsistencyGroup> _cgsToCreateMap;
    private List<UnManagedConsistencyGroup> _umCGsToUpdate;

    /**
     * Constructor.
     *
     * @param unManagedVolume the parent UnManagedVolume for this context
     * @param dbClient a reference to the database client
     * @param parentRequestContext the parent IngestionRequestContext
     */
    public VplexVolumeIngestionContext(UnManagedVolume unManagedVolume, DbClient dbClient, IngestionRequestContext parentRequestContext) {
        super(unManagedVolume, dbClient);
        _parentRequestContext = parentRequestContext;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.emc.storageos.api.service.impl.resource.blockingestorchestration.context.VolumeIngestionContext#getUnManagedVolume()
     */
    @Override
    public UnManagedVolume getUnmanagedVolume() {
        return super.getUnmanagedVirtualVolume();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.emc.storageos.api.service.impl.resource.blockingestorchestration.context.VolumeIngestionContext#isVolumeExported()
     */
    @Override
    public boolean isVolumeExported() {
        return VolumeIngestionUtil.checkUnManagedResourceAlreadyExported(getUnmanagedVolume());
    }

    /*
     * (non-Javadoc)
     *
     * @see com.emc.storageos.api.service.impl.resource.blockingestorchestration.context.VolumeIngestionContext#commit()
     */
    @Override
    public void commit() {

        _logger.info("persisting VPLEX backend for virtual volume " + getUnmanagedVolume().forDisplay());
        setFlagsAndUpdateExportMasks();
        createVplexMirrorObjects();
        _dbClient.createObject(getCreatedVplexMirrors());
        _dbClient.createObject(getCreatedSnapshotMap().values());

        // commit the basic IngestionRequestContext collections
        for (BlockObject bo : getObjectsIngestedByExportProcessing()) {
            _logger.info("Creating BlockObject {} (hash {})", bo.forDisplay(), bo.hashCode());
            _dbClient.createObject(bo);
        }
        for (BlockObject bo : getBlockObjectsToBeCreatedMap().values()) {
            _logger.info("Creating BlockObject {} (hash {})", bo.forDisplay(), bo.hashCode());
            _dbClient.createObject(bo);
        }

        for (Set<DataObject> createdObjects : getDataObjectsToBeCreatedMap().values()) {
            if (createdObjects != null && !createdObjects.isEmpty()) {
                for (DataObject dob : createdObjects) {
                    _logger.info("Creating DataObject {} (hash {})", dob.forDisplay(), dob.hashCode());
                    _dbClient.createObject(dob);
                }
            }
        }

        for (Set<DataObject> updatedObjects : getDataObjectsToBeUpdatedMap().values()) {
            if (updatedObjects != null && !updatedObjects.isEmpty()) {
                for (DataObject dob : updatedObjects) {
                    if (dob.getInactive()) {
                        _logger.info("Deleting DataObject {} (hash {})", dob.forDisplay(), dob.hashCode());
                    } else {
                        _logger.info("Updating DataObject {} (hash {})", dob.forDisplay(), dob.hashCode());
                    }
                    _dbClient.updateObject(dob);
                }
            }
        }

        for (UnManagedVolume umv : getUnManagedVolumesToBeDeleted()) {
            _logger.info("Deleting UnManagedVolume {} (hash {})", umv.forDisplay(), umv.hashCode());
            _dbClient.updateObject(umv);
        }

        for (Entry<BlockObject, ExportGroup> entry : getVplexBackendExportGroupMap().entrySet()) {
            BlockObject volume = entry.getKey();
            ExportGroup exportGroup = entry.getValue();
            ExportGroup egInDb = _dbClient.queryObject(ExportGroup.class, exportGroup.getId());
            exportGroup.addVolume(volume.getId(), ExportGroup.LUN_UNASSIGNED);
            if (null == egInDb) {
                _logger.info("Creating VPLEX backend ExportGroup {} for Volume {}", exportGroup.forDisplay(), volume.forDisplay());
                _dbClient.createObject(exportGroup);
            } else {
                _logger.info("Updating VPLEX backend ExportGroup {} for Volume {}", exportGroup.forDisplay(), volume.forDisplay());
                _dbClient.updateObject(exportGroup);
            }
        }

        for (UnManagedConsistencyGroup umcg : getUmCGObjectsToUpdate()) {
            if (umcg.getInactive()) {
                _logger.info("Deleting UnManagedConsistencyGroup {} (hash {})", umcg.forDisplay(), umcg.hashCode());
            } else {
                _logger.info("Updating UnManagedConsistencyGroup {} (hash {})", umcg.forDisplay(), umcg.hashCode());
            }
            _dbClient.updateObject(umcg);
        }

        for (BlockConsistencyGroup bcg : getCGObjectsToCreateMap().values()) {
            _logger.info("Creating BlockConsistencyGroup {} (hash {})", bcg.forDisplay(), bcg.hashCode());
            _dbClient.createObject(bcg);
        }
    }

    /**
     * Gets a Map of backend Volume objects to the ExportGroup to 
     * which they are tied.  This is necessary because a VPLEX distributed
     * volume could have two different backend ExportGroups.
     * 
     * @return a Map of backend Volume objects to its ExportGroup
     */
    public Map<BlockObject, ExportGroup> getVplexBackendExportGroupMap() {
        if (null == _vplexBackendExportGroupMap) {
            _vplexBackendExportGroupMap = new HashMap<BlockObject, ExportGroup>();
        }
        
        return _vplexBackendExportGroupMap;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.emc.storageos.api.service.impl.resource.blockingestorchestration.context.VolumeIngestionContext#rollback()
     */
    @Override
    public void rollback() {
        _logger.warn("performing rollback for " + getUnmanagedVirtualVolume().forDisplay());
        getObjectsIngestedByExportProcessing().clear();
        getBlockObjectsToBeCreatedMap().clear();
        getCreatedSnapshotMap().clear();
        getDataObjectsToBeCreatedMap().clear();
        getDataObjectsToBeUpdatedMap().clear();
        getUnManagedVolumesToBeDeleted().clear();
        getCreatedVplexMirrors().clear();
        getUmCGObjectsToUpdate().clear();
        getCGObjectsToCreateMap().clear();
        getVplexBackendExportGroupMap().clear();
        Set<DataObject> objectsToUpdateForUnManagedVolume = 
                getRootIngestionRequestContext().getDataObjectsToBeUpdatedMap().get(getUnmanagedVirtualVolume().getNativeGuid());
        if (null != objectsToUpdateForUnManagedVolume) {
            _logger.warn("clearing objects to update on rollback for {}: {}", 
                    getUnmanagedVirtualVolume().getNativeGuid(), objectsToUpdateForUnManagedVolume);
            objectsToUpdateForUnManagedVolume.clear();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.emc.storageos.api.service.impl.resource.blockingestorchestration.context.VolumeIngestionContext#getErrorMessages()
     */
    @Override
    public List<String> getErrorMessages() {
        if (null == _errorMessages) {
            _errorMessages = new ArrayList<String>();
        }

        return _errorMessages;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.Iterator#hasNext()
     */
    @Override
    public boolean hasNext() {
        iteratorIniterator();
        return _backendVolumeUrisToProcessIterator.hasNext();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.Iterator#next()
     */
    @Override
    public UnManagedVolume next() {
        iteratorIniterator();

        UnManagedVolume currentUnmanagedVolume = _backendVolumeUrisToProcessIterator.next();
        setCurrentUnmanagedVolume(currentUnmanagedVolume);
        return currentUnmanagedVolume;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.Iterator#remove()
     */
    @Override
    public void remove() {
        iteratorIniterator();
        _backendVolumeUrisToProcessIterator.remove();
    }

    /**
     * Initializes the internal backend volume URI interator.
     */
    private void iteratorIniterator() {
        if (null == _backendVolumeUrisToProcessIterator) {
            _backendVolumeUrisToProcessIterator = this.getUnmanagedVolumesToIngest().iterator();
        }
    }

    /**
     * Private setter for the current backend UnManagedVolume, used by this class' implementation
     * of Iterator<UnManagedVolume>. Will also set the current VolumeIngestionContext.
     *
     * @param unManagedVolume the UnManagedVolume to set
     */
    private void setCurrentUnmanagedVolume(UnManagedVolume unManagedVolume) {
        _currentBackendVolumeIngestionContext = VolumeIngestionContextFactory.getVolumeIngestionContext(unManagedVolume, _dbClient, this);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.emc.storageos.api.service.impl.resource.blockingestorchestration.context.IngestionRequestContext#getCurrentUnmanagedVolume()
     */
    @Override
    public UnManagedVolume getCurrentUnmanagedVolume() {
        if (_currentBackendVolumeIngestionContext == null) {
            return null;
        }

        return _currentBackendVolumeIngestionContext.getUnmanagedVolume();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.emc.storageos.api.service.impl.resource.blockingestorchestration.context.IngestionRequestContext#getCurrentUnManagedVolumeUri()
     */
    @Override
    public URI getCurrentUnManagedVolumeUri() {
        return getCurrentUnmanagedVolume().getId();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.emc.storageos.api.service.impl.resource.blockingestorchestration.context.IngestionRequestContext#getVolumeContext()
     */
    @Override
    public VolumeIngestionContext getVolumeContext() {
        return _currentBackendVolumeIngestionContext;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.emc.storageos.api.service.impl.resource.blockingestorchestration.context.IngestionRequestContext#getVolumeContext(java.lang.
     * String
     * )
     */
    @Override
    public VolumeIngestionContext getVolumeContext(String unmanagedVolumeGuid) {
        if (getProcessedUnManagedVolumeMap().get(unmanagedVolumeGuid) != null) {
            return getProcessedUnManagedVolumeMap().get(unmanagedVolumeGuid);
        }
        return getVolumeContext();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.emc.storageos.api.service.impl.resource.blockingestorchestration.context.IngestionRequestContext#getStorageSystem()
     */
    @Override
    public StorageSystem getStorageSystem() {
        URI storageSystemUri = getCurrentUnmanagedVolume().getStorageSystemUri();
        StorageSystem storageSystem = getStorageSystemCache().get(storageSystemUri.toString());
        if (null == storageSystem) {
            storageSystem = _dbClient.queryObject(StorageSystem.class, storageSystemUri);
            getStorageSystemCache().put(storageSystemUri.toString(), storageSystem);
        }

        return storageSystem;
    }

    /**
     * Returns the high availability VirtualPool for this VolumeIngestionContext's
     * UnManagedVolume virtual volume.
     *
     * @return the high availability VirtualPool
     */
    public VirtualPool getHaVpool(UnManagedVolume unmanagedVolume) {

        VirtualPool haVpool = null;
        StringMap haVarrayVpoolMap = _parentRequestContext.getVpool(unmanagedVolume).getHaVarrayVpoolMap();

        if (haVarrayVpoolMap != null && !haVarrayVpoolMap.isEmpty()) {
            String haVarrayStr = haVarrayVpoolMap.keySet().iterator().next();
            String haVpoolStr = haVarrayVpoolMap.get(haVarrayStr);
            if (haVpoolStr != null && !(haVpoolStr.equals(NullColumnValueGetter.getNullURI().toString()))) {
                haVpool = _dbClient.queryObject(VirtualPool.class, URI.create(haVpoolStr));
            }
        }

        return haVpool;
    }

    /**
     * Returns the high availability VirtualArray for this VolumeIngestionContext's
     * UnManagedVolume virtual volume.
     *
     * @return the high availability VirtualArray
     */
    public VirtualArray getHaVarray(UnManagedVolume unmanagedVolume) {

        VirtualArray haVarray = null;
        StringMap haVarrayVpoolMap = _parentRequestContext.getVpool(unmanagedVolume).getHaVarrayVpoolMap();
        if (haVarrayVpoolMap != null && !haVarrayVpoolMap.isEmpty()) {
            String haVarrayStr = haVarrayVpoolMap.keySet().iterator().next();
            if (haVarrayStr != null && !(haVarrayStr.equals(NullColumnValueGetter.getNullURI().toString()))) {
                haVarray = _dbClient.queryObject(VirtualArray.class, URI.create(haVarrayStr));
            }
        }

        return haVarray;
    }

    /**
     * Sets the VPLEX cluster ID for the high availability cluster.
     *
     * @param haClusterId the high availability cluster ID to set
     */
    public void setHaClusterId(String haClusterId) {
        this._haClusterId = haClusterId;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.emc.storageos.api.service.impl.resource.blockingestorchestration.context.IngestionRequestContext#getVpool()
     */
    @Override
    public VirtualPool getVpool(UnManagedVolume unmanagedVolume) {

        VirtualPool vpoolForThisVolume = _parentRequestContext.getVpool(unmanagedVolume);

        // get the backend volume cluster id
        String backendClusterId = VplexBackendIngestionContext.extractValueFromStringSet(
                SupportedVolumeInformation.VPLEX_BACKEND_CLUSTER_ID.toString(),
                unmanagedVolume.getVolumeInformation());
        if (null != backendClusterId && null != _haClusterId
                && backendClusterId.equals(_haClusterId)) {
            if (null != getHaVpool(unmanagedVolume)) {
                _logger.info("using high availability vpool " + getHaVpool(unmanagedVolume).getLabel());
                vpoolForThisVolume = getHaVpool(unmanagedVolume);
            }
        }

        // finally, double check for a separate mirror / continuous copies vpool
        // TODO: verify separate mirror vpool
        if (getUnmanagedVplexMirrors().keySet().contains(unmanagedVolume)
                && vpoolForThisVolume.getMirrorVirtualPool() != null) {
            _logger.info("this associated volume is a mirror and separate mirror vpool is defined");
            VirtualPool mirrorVpool = _dbClient.queryObject(
                    VirtualPool.class, URI.create(vpoolForThisVolume.getMirrorVirtualPool()));
            _logger.info("using mirror vpool " + mirrorVpool.getLabel());
            vpoolForThisVolume = mirrorVpool;
        }

        return vpoolForThisVolume;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.emc.storageos.api.service.impl.resource.blockingestorchestration.context.IngestionRequestContext#getVarray()
     */
    @Override
    public VirtualArray getVarray(UnManagedVolume unmanagedVolume) {

        VirtualArray varrayForThisVolume = _parentRequestContext.getVarray(unmanagedVolume);

        // get the backend volume cluster id
        String backendClusterId = VplexBackendIngestionContext.extractValueFromStringSet(
                SupportedVolumeInformation.VPLEX_BACKEND_CLUSTER_ID.toString(),
                unmanagedVolume.getVolumeInformation());
        if (null != backendClusterId && null != _haClusterId
                && backendClusterId.equals(_haClusterId)) {
            _logger.info("using high availability varray " + getHaVarray(unmanagedVolume).getLabel());
            varrayForThisVolume = getHaVarray(unmanagedVolume);
        }

        return varrayForThisVolume;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.emc.storageos.api.service.impl.resource.blockingestorchestration.context.IngestionRequestContext#getProject()
     */
    @Override
    public Project getProject() {
        // determine the correct project to use with this volume:
        // the backend volumes have the vplex backend Project, but
        // the rest have the same Project as the virtual volume.
        Project project = getUnmanagedBackendVolumes().contains(getCurrentUnmanagedVolume()) ? getBackendProject() : getFrontendProject();

        return project;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.emc.storageos.api.service.impl.resource.blockingestorchestration.context.IngestionRequestContext#getTenant()
     */
    @Override
    public TenantOrg getTenant() {
        return _parentRequestContext.getTenant();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.emc.storageos.api.service.impl.resource.blockingestorchestration.context.IngestionRequestContext#getVplexIngestionMethod()
     */
    @Override
    public String getVplexIngestionMethod() {
        return _parentRequestContext.getVplexIngestionMethod();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.emc.storageos.api.service.impl.resource.blockingestorchestration.context.IngestionRequestContext#getStorageSystemCache()
     */
    @Override
    public Map<String, StorageSystem> getStorageSystemCache() {
        return _parentRequestContext.getStorageSystemCache();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.emc.storageos.api.service.impl.resource.blockingestorchestration.context.IngestionRequestContext#getExhaustedStorageSystems()
     */
    @Override
    public List<URI> getExhaustedStorageSystems() {
        return _parentRequestContext.getExhaustedStorageSystems();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.emc.storageos.api.service.impl.resource.blockingestorchestration.context.IngestionRequestContext#getExhaustedPools()
     */
    @Override
    public List<URI> getExhaustedPools() {
        return _parentRequestContext.getExhaustedPools();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.emc.storageos.api.service.impl.resource.blockingestorchestration.context.IngestionRequestContext#getUnManagedVolumesToBeDeleted()
     */
    @Override
    public List<UnManagedVolume> getUnManagedVolumesToBeDeleted() {
        if (null == _unManagedVolumesToBeDeleted) {
            _unManagedVolumesToBeDeleted = new ArrayList<UnManagedVolume>();
        }

        return _unManagedVolumesToBeDeleted;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.emc.storageos.api.service.impl.resource.blockingestorchestration.context.IngestionRequestContext#getTaskStatusMap()
     */
    @Override
    public Map<String, StringBuffer> getTaskStatusMap() {
        return _parentRequestContext.getTaskStatusMap();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.emc.storageos.api.service.impl.resource.blockingestorchestration.context.IIngestionRequestContext#getProcessedUnManagedVolumeMap
     * ()
     */
    @Override
    public Map<String, VolumeIngestionContext> getProcessedUnManagedVolumeMap() {
        if (null == _processedUnManagedVolumeMap) {
            _processedUnManagedVolumeMap = new HashMap<String, VolumeIngestionContext>();
        }

        return _processedUnManagedVolumeMap;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.emc.storageos.api.service.impl.resource.blockingestorchestration.context.IIngestionRequestContext#getProcessedUnManagedVolume
     * (java.lang.String)
     */
    @Override
    public UnManagedVolume getProcessedUnManagedVolume(String nativeGuid) {
        VolumeIngestionContext volumeContext = getProcessedUnManagedVolumeMap().get(nativeGuid);
        if (null != volumeContext) {
            return volumeContext.getUnmanagedVolume();
        }

        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.emc.storageos.api.service.impl.resource.blockingestorchestration.context.IngestionRequestContext#getProcessedBlockObject(java
     * .lang.String)
     */
    @Override
    public BlockObject getProcessedBlockObject(String unmanagedVolumeGuid) {
        String objectGUID = unmanagedVolumeGuid.replace(VolumeIngestionUtil.UNMANAGEDVOLUME, VolumeIngestionUtil.VOLUME);
        return getBlockObjectsToBeCreatedMap().get(objectGUID);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.emc.storageos.api.service.impl.resource.blockingestorchestration.context.IIngestionRequestContext#getProcessedVolumeContext(java
     * .lang.String)
     */
    @Override
    public VolumeIngestionContext getProcessedVolumeContext(String nativeGuid) {
        return getProcessedUnManagedVolumeMap().get(nativeGuid);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.emc.storageos.api.service.impl.resource.blockingestorchestration.context.IngestionRequestContext#getErrorMessagesForVolume(java
     * .lang.String)
     */
    @Override
    public List<String> getErrorMessagesForVolume(String nativeGuid) {
        // for VPLEX, we want to return the error messages List for the
        // main UnManagedVolume, whose status would be returned to the user
        return getErrorMessages();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.emc.storageos.api.service.impl.resource.blockingestorchestration.context.IIngestionRequestContext#
     * getObjectsIngestedByExportProcessing()
     */
    @Override
    public List<BlockObject> getObjectsIngestedByExportProcessing() {
        if (null == _objectsIngestedByExportProcessing) {
            _objectsIngestedByExportProcessing = new ArrayList<BlockObject>();
        }

        return _objectsIngestedByExportProcessing;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.emc.storageos.api.service.impl.resource.blockingestorchestration.context.IngestionRequestContext#isExportGroupCreated()
     */
    @Override
    public boolean isExportGroupCreated() {
        return _exportGroupCreated;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.emc.storageos.api.service.impl.resource.blockingestorchestration.context.IngestionRequestContext#setExportGroupCreated(boolean)
     */
    @Override
    public void setExportGroupCreated(boolean exportGroupCreated) {
        this._exportGroupCreated = exportGroupCreated;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.emc.storageos.api.service.impl.resource.blockingestorchestration.context.IngestionRequestContext#getExportGroup()
     */
    @Override
    public ExportGroup getExportGroup() {
        return _exportGroup;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.emc.storageos.api.service.impl.resource.blockingestorchestration.context.IngestionRequestContext#setExportGroup(com.emc.storageos
     * .db.client.model.ExportGroup)
     */
    @Override
    public void setExportGroup(ExportGroup exportGroup) {
        this._exportGroup = exportGroup;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.emc.storageos.api.service.impl.resource.blockingestorchestration.context.IngestionRequestContext#getHost()
     */
    @Override
    public URI getHost() {
        // VPLEX backend volumes would never be exported directly to a Host
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.emc.storageos.api.service.impl.resource.blockingestorchestration.context.IngestionRequestContext#setHost(java.net.URI)
     */
    @Override
    public void setHost(URI host) {
        // no-op; vplex ingestion only uses device initiators for export
    }

    /*
     * (non-Javadoc)
     *
     * @see com.emc.storageos.api.service.impl.resource.blockingestorchestration.context.IngestionRequestContext#getCluster()
     */
    @Override
    public URI getCluster() {
        // VPLEX backend volumes would never be exported directly to a Cluster
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.emc.storageos.api.service.impl.resource.blockingestorchestration.context.IngestionRequestContext#setCluster(java.net.URI)
     */
    @Override
    public void setCluster(URI cluster) {
        // no-op; vplex ingestion only uses device initiators for export
    }

    /*
     * (non-Javadoc)
     *
     * @see com.emc.storageos.api.service.impl.resource.blockingestorchestration.context.IngestionRequestContext#getDeviceInitiators()
     */
    @Override
    public List<Initiator> getDeviceInitiators() {
        return _deviceInitiators;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.emc.storageos.api.service.impl.resource.blockingestorchestration.context.IngestionRequestContext#setDeviceInitiators(java.util
     * .List)
     */
    @Override
    public void setDeviceInitiators(List<Initiator> deviceInitiators) {
        this._deviceInitiators = deviceInitiators;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.emc.storageos.api.service.impl.resource.blockingestorchestration.context.IngestionRequestContext#getObjectsToBeCreatedMap()
     */
    @Override
    public Map<String, BlockObject> getBlockObjectsToBeCreatedMap() {
        if (null == _blockObjectsToBeCreatedMap) {
            _blockObjectsToBeCreatedMap = new HashMap<String, BlockObject>();
        }

        return _blockObjectsToBeCreatedMap;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.emc.storageos.api.service.impl.resource.blockingestorchestration.context.IngestionRequestContext#getDataObjectsToBeCreatedMap()
     */
    @Override
    public Map<String, Set<DataObject>> getDataObjectsToBeCreatedMap() {
        if (null == _dataObjectsToBeCreatedMap) {
            _dataObjectsToBeCreatedMap = new HashMap<String, Set<DataObject>>();
        }

        return _dataObjectsToBeCreatedMap;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.emc.storageos.api.service.impl.resource.blockingestorchestration.context.IngestionRequestContext#getObjectsToBeUpdatedMap()
     */
    @Override
    public Map<String, Set<DataObject>> getDataObjectsToBeUpdatedMap() {
        if (null == _dataObjectsToBeUpdatedMap) {
            _dataObjectsToBeUpdatedMap = new HashMap<String, Set<DataObject>>();
        }

        return _dataObjectsToBeUpdatedMap;
    }

    /**
     * Updates any internal flags on the ingested backend resources.
     *
     * @param context the VplexBackendIngestionContext
     */
    private void setFlagsAndUpdateExportMasks() {
        // assemble a map of backend Volume native guid(s) to new backend ExportMask.
        // if a new backend ExportMask isn't being created for a backend volume, then there will be no entry.
        Map<String, ExportMask> backendVolumeGuidToNewExportMaskMap = new HashMap<String, ExportMask>();
        for (Entry<String, Set<DataObject>> entry : getDataObjectsToBeCreatedMap().entrySet()) {
            Set<DataObject> values = entry.getValue();
            for (DataObject dataObject : values) {
                if (dataObject instanceof ExportMask) {
                    backendVolumeGuidToNewExportMaskMap.put(
                            entry.getKey().replace(VolumeIngestionUtil.UNMANAGEDVOLUME, VolumeIngestionUtil.VOLUME), 
                            (ExportMask) dataObject);
                }
            }
        }

        // set internal object flag on any backend volumes
        for (BlockObject o : getBlockObjectsToBeCreatedMap().values()) {
            if (getBackendVolumeGuids().contains(o.getNativeGuid())) {
                o.clearInternalFlags(BlockIngestOrchestrator.INTERNAL_VOLUME_FLAGS);
                _logger.info("setting INTERNAL_OBJECT flag on " + o.getLabel());
                o.addInternalFlags(Flag.INTERNAL_OBJECT);

                // check if any new backend ExportMasks need to be updated
                ExportMask exportMask = backendVolumeGuidToNewExportMaskMap.get(o.getNativeGuid());
                if (null != exportMask) {
                    _logger.info(
                            "Removing block object {} from existing volumes and adding to user created volumes of export mask {}",
                            o.getNativeGuid(), exportMask.getMaskName());
                    exportMask.removeFromExistingVolumes(o);
                    exportMask.addToUserCreatedVolumes(o);
                }
            }
        }

        // Look to see if the backend ingestion resulted in the creation of a
        // BlockSnapshot instance, which would occur if the backend volume is
        // also a snapshot target volume. It is possible that the snapshot is
        // still marked internal if the VPLEX volume built on the snapshot
        // is ingested after the VPLEX volume whose backend volume is the
        // snapshot source volume. If the snapshot source is set, then snapshot
        // and source are fully ingested and we need to make sure the snapshot
        // is public.
        for (BlockSnapshot snapshot : getCreatedSnapshotMap().values()) {
            if (!NullColumnValueGetter.isNullValue(snapshot.getSourceNativeId())) {
                snapshot.clearInternalFlags(BlockIngestOrchestrator.INTERNAL_VOLUME_FLAGS);
            }
        }
    }

    /**
     * Returns a List of created VplexMirror Objects.
     *
     * @return a List of created VplexMirror Objects
     */
    public List<VplexMirror> getCreatedVplexMirrors() {
        if (null == _createdVplexMirrors) {
            _createdVplexMirrors = new ArrayList<VplexMirror>();
        }

        return _createdVplexMirrors;
    }

    /**
     * Create a VplexMirror database object if a VPLEX native mirror is present.
     * This should be called after the parent virtual volume has already been ingested.
     *
     * @param context the VplexBackendIngestionContext
     * @param virtualVolume the ingested virtual volume's Volume object.
     */
    private void createVplexMirrorObjects() {
        if (!getUnmanagedVplexMirrors().isEmpty()) {
            Volume virtualVolume = (Volume) _parentRequestContext.getProcessedBlockObject(
                    getUnmanagedVirtualVolume().getNativeGuid());
            _logger.info("creating VplexMirror object for virtual volume " + virtualVolume.getLabel());
            for (Entry<UnManagedVolume, String> entry : getUnmanagedVplexMirrors().entrySet()) {
                // find mirror and create a VplexMirror object
                BlockObject mirror = getBlockObjectsToBeCreatedMap().get(entry.getKey().getNativeGuid()
                        .replace(VolumeIngestionUtil.UNMANAGEDVOLUME,
                                VolumeIngestionUtil.VOLUME));
                if (null != mirror) {
                    _logger.info("processing mirror " + mirror.getLabel());
                    if (mirror instanceof Volume) {
                        Volume mirrorVolume = (Volume) mirror;

                        // create VplexMirror set all the basic properties
                        VplexMirror vplexMirror = new VplexMirror();
                        vplexMirror.setId(URIUtil.createId(VplexMirror.class));
                        vplexMirror.setCapacity(mirrorVolume.getCapacity());
                        vplexMirror.setLabel(mirrorVolume.getLabel());
                        vplexMirror.setNativeId(entry.getValue());
                        // For Vplex virtual volumes set allocated capacity to 0 (cop-18608)
                        vplexMirror.setAllocatedCapacity(0L);
                        vplexMirror.setProvisionedCapacity(mirrorVolume.getProvisionedCapacity());
                        vplexMirror.setSource(new NamedURI(virtualVolume.getId(), virtualVolume.getLabel()));
                        vplexMirror.setStorageController(virtualVolume.getStorageController());
                        vplexMirror.setTenant(mirrorVolume.getTenant());
                        vplexMirror.setThinPreAllocationSize(mirrorVolume.getThinVolumePreAllocationSize());
                        vplexMirror.setThinlyProvisioned(mirrorVolume.getThinlyProvisioned());
                        vplexMirror.setVirtualArray(mirrorVolume.getVirtualArray());
                        vplexMirror.setVirtualPool(mirrorVolume.getVirtualPool());

                        // set the associated volume for this VplexMirror
                        StringSet associatedVolumes = new StringSet();
                        associatedVolumes.add(mirrorVolume.getId().toString());
                        vplexMirror.setAssociatedVolumes(associatedVolumes);

                        // VplexMirror will have the same project
                        // as the virtual volume (i.e., the front-end project)
                        // but the mirror backend will have the backend project
                        vplexMirror.setProject(new NamedURI(
                                getFrontendProject().getId(), mirrorVolume.getLabel()));
                        mirrorVolume.setProject(new NamedURI(
                                getBackendProject().getId(), mirrorVolume.getLabel()));

                        // update flags on mirror volume
                        Set<DataObject> updatedObjects = getDataObjectsToBeUpdatedMap().get(mirrorVolume.getNativeGuid());
                        if (updatedObjects == null) {
                            updatedObjects = new HashSet<DataObject>();
                            getDataObjectsToBeUpdatedMap().put(mirrorVolume.getNativeGuid(), updatedObjects);
                        }
                        VolumeIngestionUtil.clearInternalFlags(this, mirrorVolume, updatedObjects, _dbClient);
                        // VPLEX backend volumes should still have the INTERNAL_OBJECT flag
                        mirrorVolume.addInternalFlags(Flag.INTERNAL_OBJECT);

                        // deviceLabel will be the very last part of the native guid
                        String[] devicePathParts = entry.getValue().split("/");
                        String deviceName = devicePathParts[devicePathParts.length - 1];
                        vplexMirror.setDeviceLabel(deviceName);

                        // save the new VplexMirror & persist backend & updated objects
                        getCreatedVplexMirrors().add(vplexMirror);

                        // set mirrors property on the parent virtual volume
                        StringSet mirrors = virtualVolume.getMirrors();
                        if (mirrors == null) {
                            mirrors = new StringSet();
                        }
                        mirrors.add(vplexMirror.getId().toString());
                        virtualVolume.setMirrors(mirrors);
                    }
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.emc.storageos.api.service.impl.resource.blockingestorchestration.context.IngestionRequestContext#findCreatedBlockObject(java.
     * lang.
     * String)
     */
    @Override
    public BlockObject findCreatedBlockObject(String nativeGuid) {
        BlockObject blockObject = getBlockObjectsToBeCreatedMap().get(nativeGuid);
        return blockObject;
    }

    /**
     * Returns a detailed report on the state of everything in this context,
     * useful for debugging.
     *
     * @return a detailed report on the context
     */
    public String toStringDebug() {
        StringBuilder s = new StringBuilder("\n\nVplexBackendIngestionContext \n\t ");
        s.append("unmanaged virtual volume: ").append(getUnmanagedVirtualVolume()).append(" \n\t ");
        s.append("unmanaged backend volume(s): ").append(this.getUnmanagedBackendVolumes()).append(" \n\t ");
        s.append("unmanaged snapshots: ").append(this.getUnmanagedSnapshots()).append(" \n\t ");
        s.append("unmanaged full clones: ").append(this.getUnmanagedVplexClones()).append(" \n\t ");
        s.append("unmanaged backend only clones: ").append(this.getUnmanagedBackendOnlyClones()).append(" \n\t ");
        s.append("unmanaged mirrors: ").append(this.getUnmanagedVplexMirrors()).append(" \n\t ");
        s.append("ingested objects: ").append(this.getObjectsIngestedByExportProcessing()).append(" \n\t ");
        s.append("created objects map: ").append(this.getBlockObjectsToBeCreatedMap()).append(" \n\t ");
        s.append("updated objects map: ");
        for (Entry<String, Set<DataObject>> e : this.getDataObjectsToBeUpdatedMap().entrySet()) {
            s.append(e.getKey()).append(": ");
            for (DataObject o : e.getValue()) {
                s.append(o.getLabel()).append("; ");
            }
        }
        s.append(" \n\t ");
        s.append("processed unmanaged volumes: ").append(this.getProcessedUnManagedVolumeMap()).append("\n\n");
        return s.toString();
    }

    @Override
    public String toString() {
        if (_logger.isDebugEnabled()) {
            return toStringDebug();
        }

        return super.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.emc.storageos.api.service.impl.resource.blockingestorchestration.context.IngestionRequestContext#
     * findAllUnManagedVolumesToBeDeleted()
     */
    @Override
    public List<UnManagedVolume> findAllUnManagedVolumesToBeDeleted() {
        return _parentRequestContext.findAllUnManagedVolumesToBeDeleted();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.emc.storageos.api.service.impl.resource.blockingestorchestration.context.IngestionRequestContext#findInUpdatedObjects(java.net.
     * URI)
     */
    @Override
    public DataObject findInUpdatedObjects(URI uri) {

        if (!URIUtil.isValid(uri)) {
            _logger.warn("URI ({}) for findCreatedBlockObject is null or invalid", uri);
            return null;
        }

        for (Set<DataObject> objectsToBeUpdated : this.getDataObjectsToBeUpdatedMap().values()) {
            for (DataObject o : objectsToBeUpdated) {
                if (o.getId().equals(uri)) {
                    _logger.info("\tfound data object in vplex request context: " + o.forDisplay());
                    return o;
                }
            }
        }

        return _parentRequestContext.findInUpdatedObjects(uri);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.emc.storageos.api.service.impl.resource.blockingestorchestration.context.IngestionRequestContext#findCreatedBlockObject(java.net.
     * URI)
     */
    @Override
    public BlockObject findCreatedBlockObject(URI uri) {

        if (!URIUtil.isValid(uri)) {
            _logger.warn("URI ({}) for findCreatedBlockObject is null or invalid", uri);
            return null;
        }

        for (BlockObject bo : getBlockObjectsToBeCreatedMap().values()) {
            if (bo.getId() != null && uri.toString().equals(bo.getId().toString())) {
                _logger.info("\tfound block object in vplex request context: " + bo.forDisplay());
                return bo;
            }
        }

        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.emc.storageos.api.service.impl.resource.blockingestorchestration.context.IngestionRequestContext#getCGObjectsToCreateMap()
     */
    @Override
    public Map<String, BlockConsistencyGroup> getCGObjectsToCreateMap() {
        if (null == _cgsToCreateMap) {
            _cgsToCreateMap = new HashMap<String, BlockConsistencyGroup>();
        }

        return _cgsToCreateMap;
    }

    @Override
    public List<UnManagedConsistencyGroup> getUmCGObjectsToUpdate() {
        if (null == _umCGsToUpdate) {
            _umCGsToUpdate = new ArrayList<UnManagedConsistencyGroup>();
        }

        return _umCGsToUpdate;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.emc.storageos.api.service.impl.resource.blockingestorchestration.context.IngestionRequestContext#findUnManagedConsistencyGroup(
     * com.emc.storageos.db.client.model.BlockConsistencyGroup)
     */
    @Override
    public UnManagedConsistencyGroup findUnManagedConsistencyGroup(String cgName) {
        return _parentRequestContext.findUnManagedConsistencyGroup(cgName);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.emc.storageos.api.service.impl.resource.blockingestorchestration.context.IngestionRequestContext#addObjectToCreate(com.emc.
     * storageos.db.client.model.BlockObject)
     */
    @Override
    public void addBlockObjectToCreate(BlockObject blockObject) {
        getBlockObjectsToBeCreatedMap().put(blockObject.getNativeGuid(), blockObject);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.emc.storageos.api.service.impl.resource.blockingestorchestration.context.IngestionRequestContext#addObjectToUpdate(com.emc.
     * storageos.db.client.model.DataObject)
     */
    @Override
    public void addDataObjectToUpdate(DataObject dataObject, UnManagedVolume unManagedVolume) {
        if (null == getDataObjectsToBeUpdatedMap().get(unManagedVolume.getNativeGuid())) {
            getDataObjectsToBeUpdatedMap().put(unManagedVolume.getNativeGuid(), new HashSet<DataObject>());
        }
        getDataObjectsToBeUpdatedMap().get(unManagedVolume.getNativeGuid()).add(dataObject);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.emc.storageos.api.service.impl.resource.blockingestorchestration.context.IngestionRequestContext#addDataObjectToCreate(com.emc.
     * storageos.db.client.model.DataObject)
     */
    @Override
    public void addDataObjectToCreate(DataObject dataObject, UnManagedVolume unManagedVolume) {
        if (null == getDataObjectsToBeCreatedMap().get(unManagedVolume.getNativeGuid())) {
            getDataObjectsToBeCreatedMap().put(unManagedVolume.getNativeGuid(), new HashSet<DataObject>());
        }
        getDataObjectsToBeCreatedMap().get(unManagedVolume.getNativeGuid()).add(dataObject);
    }

    /* (non-Javadoc)
     * @see com.emc.storageos.api.service.impl.resource.blockingestorchestration.context.IngestionRequestContext#findExportGroup(java.lang.String)
     */
    @Override
    public ExportGroup findExportGroup(String exportGroupLabel, URI project, URI varray, URI computeResource, String resourceType) {
        if (exportGroupLabel != null) {

            ExportGroup localExportGroup = getExportGroup();
            if (null != localExportGroup && exportGroupLabel.equals(localExportGroup.getLabel())) {
                if (VolumeIngestionUtil.verifyExportGroupMatches(localExportGroup, 
                        exportGroupLabel, project, varray, computeResource, resourceType)) {
                    _logger.info("Found existing local ExportGroup {} in VPLEX ingestion request context", 
                            localExportGroup.forDisplay());
                    return localExportGroup;
                }
            }

            for (ExportGroup backendExportGroup : getVplexBackendExportGroupMap().values()) {
                if (VolumeIngestionUtil.verifyExportGroupMatches(backendExportGroup, 
                        exportGroupLabel, project, varray, computeResource, resourceType)) {
                    _logger.info("Found existing nested ExportGroup {} in VPLEX backend ingestion request context", 
                            backendExportGroup.forDisplay());
                    return backendExportGroup;
                }
            }
        }

        _logger.info("Could not find existing export group for label " + exportGroupLabel);
        return null;
    }

    /* (non-Javadoc)
     * @see com.emc.storageos.api.service.impl.resource.blockingestorchestration.context.IngestionRequestContext#findAllNewExportMasks()
     */
    @Override
    public List<ExportMask> findAllNewExportMasks() {
        List<ExportMask> newExportMasks = new ArrayList<ExportMask>();
        
        for (Set<DataObject> createdObjects : this.getDataObjectsToBeCreatedMap().values()) {
            for (DataObject createdObject : createdObjects) {
                if (createdObject instanceof ExportMask) {
                    newExportMasks.add((ExportMask) createdObject);
                }
            }
        }

        return newExportMasks;
    }

    /* (non-Javadoc)
     * @see com.emc.storageos.api.service.impl.resource.blockingestorchestration.context.IngestionRequestContext#getRootIngestionRequestContext()
     */
    @Override
    public IngestionRequestContext getRootIngestionRequestContext() {
        return _parentRequestContext;
    }

    /* (non-Javadoc)
     * @see com.emc.storageos.api.service.impl.resource.blockingestorchestration.context.IngestionRequestContext#findDataObjectByType(java.lang.Class, java.net.URI)
     */
    @Override
    public <T extends DataObject> T findDataObjectByType(Class<T> clazz, URI id, boolean fallbackToDatabase) {
        return getRootIngestionRequestContext().findDataObjectByType(clazz, id, fallbackToDatabase);
    }

    /**
     * Get the name of the VPLEX cluster on which this virtual volume resides
     * according to the Virtual Array that is currently being ingested.
     * 
     * @return the virtualVolumeVplexClusterName the VPLEX cluster name for this virtual volume
     */
    public String getVirtualVolumeVplexClusterName() {
        if (_virtualVolumeVplexClusterName == null) {
            // this should be set by the BlockVplexVolumeIngestOrchestrator to use the cluster
            // name cache, but in the case of re-ingestion, it may not be set, so call from here
            URI varrayUri = getRootIngestionRequestContext().getVarray(getUnmanagedVolume()).getId();
            URI vplexUri = getRootIngestionRequestContext().getStorageSystem().getId();
            String varrayClusterId = ConnectivityUtil.getVplexClusterForVarray(varrayUri, vplexUri, _dbClient);
            _virtualVolumeVplexClusterName = VPlexControllerUtils.getClusterNameForId(varrayClusterId, vplexUri, _dbClient);
        }

        return _virtualVolumeVplexClusterName;
    }

    /**
     * Sets the name of the VPLEX cluster on which this virtual volume resides
     * according to the Virtual Array that is currently being ingested.
     * 
     * @param virtualVolumeVplexClusterName the VPLEX cluster name to set
     */
    public void setVirtualVolumeVplexClusterName(String virtualVolumeVplexClusterName) {
        _logger.info("setting virtual volume VPLEX cluster name to " + virtualVolumeVplexClusterName);
        this._virtualVolumeVplexClusterName = virtualVolumeVplexClusterName;
    }

    /**
     * Returns a StringSet of associated volume URIs for this context's VPLEX virtual volume.
     * If the volume already has associated backend volume URIs, then that Set is returned.
     * Otherwise, the block objects to be created map will be checked for the existence of 
     * ingested backend volumes that haven't been associated yet.
     * 
     * An incomplete or empty Set may be returned depending on the state of ingestion, but a warning
     * will be logged if the count of volume URIs to be returned is not the expected number for the VPLEX
     * virtual volume type (2 for distributed type, 1 for local type).
     * 
     * @param volume the VPLEX virtual volume object to check
     * @return a StringSet of associated volume URIs for this context's VPLEX virtual volume
     */
    public StringSet getAssociatedVolumeIds(Volume volume) {
        if (volume != null && volume.getAssociatedVolumes() != null && !volume.getAssociatedVolumes().isEmpty()) {
            _logger.info("getAssociatedVolumes: volumes are already associated with the virtual volume, returning {}", 
                   volume.getAssociatedVolumes());
            return volume.getAssociatedVolumes();
        }

        StringSet associatedVolumes = new StringSet();
        for (String backendVolumeNativeGuid : getBackendVolumeGuids()) {
            BlockObject backendVolume = getBlockObjectsToBeCreatedMap().get(backendVolumeNativeGuid);
            associatedVolumes.add(backendVolume.getId().toString());
        }

        if (this.isDistributed() && associatedVolumes.size() != VPlexApiConstants.DISTRIBUTED_BACKEND_VOLUME_COUNT) {
            _logger.warn("getAssociatedVolumes: virtual volume is distributed, but {} backend volumes were found (expected 2)",
                    associatedVolumes.size());
        } else if (this.isLocal() && associatedVolumes.size() != VPlexApiConstants.LOCAL_BACKEND_VOLUME_COUNT) {
            _logger.warn("getAssociatedVolumes: virtual volume is local, but {} backend volumes were found (expected 1)",
                    associatedVolumes.size());
        }

        _logger.info("getAssociatedVolumes: backend volumes assembled, returning {}", associatedVolumes);
        return associatedVolumes;
    }
}
