/*
 * MIT License
 *
 * Copyright (c) 2016 Lavar Askew (open.hyperion@gmail.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package open.hyperion.purestorage.impl;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collector;

import com.emc.storageos.storagedriver.AbstractStorageDriver;
import com.emc.storageos.storagedriver.BlockStorageDriver;
import com.emc.storageos.storagedriver.DefaultStorageDriver;
import com.emc.storageos.storagedriver.DriverTask;
import com.emc.storageos.storagedriver.HostExportInfo;
import com.emc.storageos.storagedriver.RegistrationData;
import com.emc.storageos.storagedriver.model.Initiator;
import com.emc.storageos.storagedriver.model.StorageHostComponent;
import com.emc.storageos.storagedriver.model.StorageObject;
import com.emc.storageos.storagedriver.model.StorageObject.AccessStatus;
import com.emc.storageos.storagedriver.model.StoragePool;
import com.emc.storageos.storagedriver.model.StoragePool.PoolOperationalStatus;
import com.emc.storageos.storagedriver.model.StoragePool.PoolServiceType;
import com.emc.storageos.storagedriver.model.StoragePool.Protocols;
import com.emc.storageos.storagedriver.model.StoragePool.RaidLevels;
import com.emc.storageos.storagedriver.model.StoragePool.SupportedDriveTypes;
import com.emc.storageos.storagedriver.model.StoragePool.SupportedResourceType;
import com.emc.storageos.storagedriver.model.StoragePort;
import com.emc.storageos.storagedriver.model.StorageProvider;
import com.emc.storageos.storagedriver.model.StorageSystem;
import com.emc.storageos.storagedriver.model.StorageSystem.SupportedProvisioningType;
import com.emc.storageos.storagedriver.model.StorageVolume;
import com.emc.storageos.storagedriver.model.VolumeClone;
import com.emc.storageos.storagedriver.model.VolumeConsistencyGroup;
import com.emc.storageos.storagedriver.model.VolumeMirror;
import com.emc.storageos.storagedriver.model.VolumeSnapshot;

import com.emc.storageos.storagedriver.storagecapabilities.CapabilityInstance;
import com.emc.storageos.storagedriver.storagecapabilities.DeduplicationCapabilityDefinition;
import com.emc.storageos.storagedriver.storagecapabilities.StorageCapabilities;

import open.hyperion.purestorage.connection.PureStorageAPIFactory;
import open.hyperion.purestorage.impl.PureStorageAPI;
import open.hyperion.purestorage.utils.CompleteError;
import open.hyperion.purestorage.utils.PureStorageConstants;
import open.hyperion.purestorage.utils.PureStorageUtil;
import open.hyperion.purestorage.command.SystemCommandResult;
import open.hyperion.purestorage.command.array.ArrayCommandResult;
import open.hyperion.purestorage.command.array.ArrayControllerCommandResult;
import open.hyperion.purestorage.command.array.ArraySpaceCommandResult;
import open.hyperion.purestorage.command.port.StoragePortResult;
import open.hyperion.purestorage.command.host.HostCommandResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * 
 * Implements functions to discover the PureStorage storage array and provide provisioning
 * You can refer super class for method details
 *
 */
public class PureStorageStorageDriver extends DefaultStorageDriver implements BlockStorageDriver {

    private static final String PURESTORAGE_CONF_FILE = "purestorage-conf.xml";
	private static final Logger _log = LoggerFactory.getLogger(PureStorageStorageDriver.class);
	private ApplicationContext _parentApplicationContext;

	private PureStorageUtil       _pureStorageUtil;
	private PureStorageConstants  _pureStorageConstants;
	private PureStorageAPI        _pureStorageAPI; 
	private PureStorageAPIFactory _pureStorageAPIFactory;


	//private StoragePool    _storagePool = new StoragePool();
	//private Set<Protocols> _protocols   = new HashSet();

	public void init() {
		ApplicationContext context = new ClassPathXmlApplicationContext(new String[] {PURESTORAGE_CONF_FILE}, _parentApplicationContext);
		_pureStorageUtil = (PureStorageUtil) context.getBean("pureStorageUtil");
		_pureStorageAPIFactory = (PureStorageAPIFactory) context.getBean("pureStorageAPIFactory");
	}

	public void setApplicationContext(ApplicationContext parentApplicationContext) {
		_parentApplicationContext = parentApplicationContext;
	}

    @Override
    public DriverTask discoverStorageProvider(StorageProvider storageProvider, List<StorageSystem> storageSystems) {
		DriverTask task = createDriverTask(PureStorageConstants.TASK_TYPE_DISCOVER_STORAGE_SYSTEM);

		_log.info("PureStorageDriver:discoverStorageProvider enter");
		try {
			storageProvider.setIsSupportedVersion(true);

			//systemType, serialNumber, systemName, nativeId


/*
			String curStorSysCountMsg = String.format("PureStorageDriver:discoverStorageProvider storage system count - %s.\n",storageSystems.size());
			_log.info(curStorSysCountMsg);
			for (StorageSystem s : storageSystems) {
				String curStorSysMsg = String.format("PureStorageDriver:discoverStorageProvider storage system found - %s.\n",s.getSystemName());
				_log.info(curStorSysMsg);
			}
*/

		} catch (Exception e) {
			String msg = String.format("PureStorageDriver:discoverStorageProvider Unable to gather storage provider information from the storage system %s; Error: %s.\n",
					"Pure Storage", e);
			_log.error(msg);
			_log.error(CompleteError.getStackTrace(e));
			task.setMessage(msg);
			task.setStatus(DriverTask.TaskStatus.FAILED);
			e.printStackTrace();

		}
		_log.info("PureStorageDriver:discoverStorageProvider exit");
//
        return task;
    }

	/**
	 * Get storage system information and capabilities
	 */
	@Override
	public DriverTask discoverStorageSystem(StorageSystem storageSystem) {
		DriverTask task = createDriverTask(PureStorageConstants.TASK_TYPE_DISCOVER_STORAGE_SYSTEM);

		try {
			_log.info("PureStorageDriver:discoverStorageSystem information for storage system {}, name {} - start",
					storageSystem.getIpAddress(), storageSystem.getSystemName());

			URI deviceURI = new URI("https", null, storageSystem.getIpAddress(), storageSystem.getPortNumber(), "/",
					null, null);

			// remove '/' as lock fails with this name
			String uniqueId = deviceURI.toString();
			uniqueId = uniqueId.replace("/", "");

			PureStorageAPI pureStorageAPI = _pureStorageUtil.getPureStorageDevice(storageSystem);
			String authToken = pureStorageAPI.getAuthToken(storageSystem.getUsername(), storageSystem.getPassword());
			if (authToken == null) {
				throw new PureStorageException("Could not get authentication token");
			} else { // start the session
				pureStorageAPI.createSession();
			}

			// get storage details
			ArrayCommandResult arrayRes = pureStorageAPI.getArrayDetails();
			storageSystem.setSerialNumber(arrayRes.getId());
			int[] versionNumbers = PureStorageUtil.getVersionNumbers(arrayRes.getVersion());
			
			if (versionNumbers != null && versionNumbers.length >= 1) {
				storageSystem.setMajorVersion("" + versionNumbers[0]);
			} else {
				storageSystem.setMajorVersion("UNKNOWN");
			}

			if (versionNumbers != null && versionNumbers.length >= 2) {
				storageSystem.setMinorVersion("" + versionNumbers[1]);
			} else {
				storageSystem.setMinorVersion("UNKNOWN");
			}
			storageSystem.setIsSupportedVersion(true);
			
			// protocols supported
			List<String> protocols = new ArrayList<String>();
			//protocols.add(Protocols.iSCSI.toString());
			protocols.add(Protocols.FC.toString());
			storageSystem.setProtocols(protocols);

			storageSystem.setFirmwareVersion(arrayRes.getRevision());

			ArrayControllerCommandResult[] arrConComResArray = pureStorageAPI.getArrayControllerDetails();
			for (ArrayControllerCommandResult arrConComRes : arrConComResArray) {
    			if (arrConComRes.getMode() != null &&
    				arrConComRes.getMode().trim().equalsIgnoreCase("primary")) {
    				storageSystem.setModel(arrConComRes.getModel().trim());
    				break;
    			}
  			}

			storageSystem.setProvisioningType(SupportedProvisioningType.THIN);
			Set<StorageSystem.SupportedReplication> supportedReplications = new HashSet<>();
            supportedReplications.add(StorageSystem.SupportedReplication.elementReplica);
            supportedReplications.add(StorageSystem.SupportedReplication.groupReplica);
			storageSystem.setSupportedReplications(supportedReplications);

			// Storage object properties
			storageSystem.setNativeId(uniqueId + ":" + arrayRes.getId());

			if (storageSystem.getDeviceLabel() == null) {
				if (storageSystem.getDisplayName() != null) {
					storageSystem.setDeviceLabel(storageSystem.getDisplayName());
				} else if (arrayRes.getArrayName() != null) {
					storageSystem.setDeviceLabel(arrayRes.getArrayName());
					storageSystem.setDisplayName(arrayRes.getArrayName());
				}
			}

			storageSystem.setAccessStatus(AccessStatus.READ_WRITE);
			setConnInfoToRegistry(storageSystem.getNativeId(), storageSystem.getIpAddress(),
					storageSystem.getPortNumber(), storageSystem.getUsername(), storageSystem.getPassword());

			task.setStatus(DriverTask.TaskStatus.READY);
			_log.info("PureStorageDriver:discoverStorageSystem Successfully discovered storage system {}, name {} - end",
					storageSystem.getIpAddress(), storageSystem.getSystemName());
		} catch (Exception e) {
			String msg = String.format("PureStorageDriver:discoverStorageSystem Unable to discover the storage system %s ip %s; Error: %s.\n",
					storageSystem.getSystemName(), storageSystem.getIpAddress(), e);
			_log.error(msg);
			_log.error(CompleteError.getStackTrace(e));
			task.setMessage(msg);
			task.setStatus(DriverTask.TaskStatus.FAILED);
			e.printStackTrace();
		}

		return task;
	}

    @Override
    public DriverTask discoverStoragePools(StorageSystem storageSystem, List<StoragePool> storagePools) {

		DriverTask task = createDriverTask(PureStorageConstants.TASK_TYPE_DISCOVER_STORAGE_POOLS);

		try {
			PureStorageAPI pureStorageAPI = _pureStorageUtil.getPureStorageDevice(storageSystem);

			// get storage pool details
			//ArraySpaceCommandResult arraySpcRes = pureStorageAPI.getSpaceDetails();

			ArraySpaceCommandResult[] arraySpcResArray = pureStorageAPI.getSpaceDetails();
			for (ArraySpaceCommandResult arraySpcRes : arraySpcResArray) {
    			if (arraySpcRes.getHostName() != null &&
    				!(arraySpcRes.getHostName().trim().equalsIgnoreCase(""))) {
					StoragePool     storagePool = new StoragePool();
					
					Set<Protocols>           protocols           = new HashSet();
					Set<RaidLevels>          raidLevels          = new HashSet();
					Set<SupportedDriveTypes> supportedDriveTypes = new HashSet();
        			
        			storagePool.setPoolName("Pure Storage single pool");

        			storagePool.setStorageSystemId(storageSystem.getNativeId());
        			protocols.add(Protocols.FC);
        			storagePool.setProtocols(protocols);
        			storagePool.setTotalCapacity(Long.valueOf(arraySpcRes.getCapacity()));
        			long freeCap = Long.valueOf(arraySpcRes.getCapacity()).longValue() - Long.valueOf(arraySpcRes.getTotal()).longValue();
        			storagePool.setFreeCapacity(freeCap);
        			storagePool.setMaximumThinVolumeSize(Long.getLong(arraySpcRes.getCapacity()));
        			storagePool.setMinimumThinVolumeSize(Long.getLong("0"));
        			storagePool.setSubscribedCapacity(Long.valueOf(arraySpcRes.getTotal()));
        			storagePool.setOperationalStatus(PoolOperationalStatus.READY);
        			storagePool.setSupportedResourceType(SupportedResourceType.THIN_ONLY);
        			storagePool.setPoolServiceType(PoolServiceType.block);
        			raidLevels.add(RaidLevels.RAID3);
        			storagePool.setSupportedRaidLevels(raidLevels);
        			DeduplicationCapabilityDefinition dedupCapabilityDefinition = new DeduplicationCapabilityDefinition();

        			Boolean dedupEnabled = true;

        			List<CapabilityInstance> capabilities = new ArrayList<>(); // SDK requires initialization
            		Map<String, List<String>> props = new HashMap<>();
            		props.put(DeduplicationCapabilityDefinition.PROPERTY_NAME.ENABLED.name(), Arrays.asList(dedupEnabled.toString()));

            		CapabilityInstance capabilityInstance = new CapabilityInstance(dedupCapabilityDefinition.getId(), dedupCapabilityDefinition.getId(), props);
            		capabilities.add(capabilityInstance);
            		storagePool.setCapabilities(capabilities);
            		supportedDriveTypes.add(SupportedDriveTypes.FC);
            		storagePool.setSupportedDriveTypes(supportedDriveTypes);
            		storagePool.setNativeId(arraySpcRes.getHostName() + " POOL");
            		//storagePools.clear();
					storagePools.add(storagePool);
            		task.setStatus(DriverTask.TaskStatus.READY);
    				break;
    			}
  			}

			_log.info("PureStorageDriver:discoverStoragePools information for storage system {}, nativeId {} - end",
					storageSystem.getIpAddress(), storageSystem.getNativeId());

		} catch (Exception e) {
			String msg = String.format("PureStorageDriver:discoverStoragePools Unable to gather Singleton pool information from the storage system %s ip %s; Error: %s.\n",
					storageSystem.getSystemName(), storageSystem.getIpAddress(), e);
			_log.error(msg);
			_log.error(CompleteError.getStackTrace(e));
			task.setMessage(msg);
			task.setStatus(DriverTask.TaskStatus.FAILED);
			e.printStackTrace();
		}

		task.setStatus(DriverTask.TaskStatus.READY);
        return task;

		//return null;
    }


    @Override
    public DriverTask discoverStoragePorts(StorageSystem storageSystem, List<StoragePort> storagePorts) {
		_log.info("PureStorageStorageDriver: discoverStoragePorts information for storage system {}, nativeId {} - start",
				storageSystem.getIpAddress(), storageSystem.getNativeId());
		DriverTask task = createDriverTask(PureStorageConstants.TASK_TYPE_DISCOVER_STORAGE_PORTS);

		try {
			PureStorageAPI pureStorageAPI = _pureStorageUtil.getPureStorageDevice(storageSystem);
			StoragePortResult[] portResArray = pureStorageAPI.getStoragePortDetails(storageSystem.getSystemName());
			List<StoragePort> storagePortList = Arrays.asList(portResArray);

			for (StoragePort sp : storagePortList) {
				_log.info("PureStorageStorageDriver: discoverStoragePorts information for storage port name {} - end",
					    sp.getPortName());
				storagePorts.add(sp);
			}
			task.setStatus(DriverTask.TaskStatus.READY);
			_log.info("PureStorageStorageDriver: discoverStoragePorts information for storage system {}, nativeId {} - end",
					storageSystem.getIpAddress(), storageSystem.getNativeId());
		} catch (Exception e) {
			String msg = String.format(
					"PureStorageStorageDriver: Unable to discover the storage port information for storage system %s native id %s; Error: %s.\n",
					storageSystem.getSystemName(), storageSystem.getNativeId(), e);
			_log.error(msg);
			_log.error(CompleteError.getStackTrace(e));
			task.setMessage(msg);
			task.setStatus(DriverTask.TaskStatus.FAILED);
			e.printStackTrace();
		}
		return task;
    }


    @Override
    public DriverTask discoverStorageHostComponents(StorageSystem storageSystem, List<StorageHostComponent> embeddedStorageHostComponents) {
		_log.info("PureStorageStorageDriver: discoverStorageHostComponents information for storage system {}, nativeId {} - start",
				storageSystem.getIpAddress(), storageSystem.getNativeId());
		DriverTask task = createDriverTask(PureStorageConstants.TASK_TYPE_DISCOVER_STORAGE_HOSTS);
        try {
			PureStorageAPI pureStorageAPI = _pureStorageUtil.getPureStorageDevice(storageSystem);
			HostCommandResult[] hostResArray = pureStorageAPI.getHostsDetails();
			for (HostCommandResult h : hostResArray) {
				StorageHostComponent shc = new StorageHostComponent();
				shc.setHostName(h.getName());
				//shc.setArrays.stream(h.getWwn()).collect(Collectors.toSet());
				embeddedStorageHostComponents.add(shc);
			}
			task.setStatus(DriverTask.TaskStatus.READY);
			_log.info("PureStorageStorageDriver: discoverStoragePorts information for storage system {}, nativeId {} - end",
					storageSystem.getIpAddress(), storageSystem.getNativeId());
        } catch (Exception e) {
			String msg = String.format(
					"PureStorageStorageDriver: Unable to discover the storage hosts information for storage system %s native id %s; Error: %s.\n",
					storageSystem.getSystemName(), storageSystem.getNativeId(), e);
			_log.error(msg);
			_log.error(CompleteError.getStackTrace(e));
			task.setMessage(msg);
			task.setStatus(DriverTask.TaskStatus.FAILED);
			e.printStackTrace();

        }
        return task;
    }
/*
    @Override
    public DriverTask stopManagement(StorageSystem driverStorageSystem){
    	_log.info("Stopping management for StorageSystem {}", driverStorageSystem.getNativeId());
    	String driverName = this.getClass().getSimpleName();
        String taskId = String.format("%s+%s+%s", driverName, "stopManagement", UUID.randomUUID().toString());
        DriverTask task = new DefaultDriverTask(taskId);
        task.setStatus(DriverTask.TaskStatus.FAILED);
        
        String msg = String.format("%s: %s --- operation is not supported.", driverName, "stopManagement");
        _log.warn(msg);
        task.setMessage(msg);
        return null;
    }

    @Override
    public DriverTask createVolumes(List<StorageVolume> volumes, StorageCapabilities capabilities) {
        String taskType = "create-storage-volumes";
        String driverName = this.getClass().getSimpleName();
        String taskId = String.format("%s+%s+%s", driverName, taskType, UUID.randomUUID().toString());
        DriverTask task = new DefaultDriverTask(taskId);
        task.setStatus(DriverTask.TaskStatus.FAILED);

        String msg = String.format("%s: %s --- operation is not supported.", driverName, "createVolumes");
        _log.warn(msg);
        task.setMessage(msg);
        return null;
    }
*/

/*
    @Override
    public DriverTask getStorageVolumes(StorageSystem storageSystem, List<StorageVolume> storageVolumes, MutableInt token) {
        String taskType = "get-storage-volumes";
        String driverName = this.getClass().getSimpleName();
        String taskId = String.format("%s+%s+%s", driverName, taskType, UUID.randomUUID().toString());
        DriverTask task = new DefaultDriverTask(taskId);
        task.setStatus(DriverTask.TaskStatus.FAILED);

        String msg = String.format("%s: %s --- operation is not supported.", driverName, "getStorageVolumes");
        _log.warn(msg);
        task.setMessage(msg);
        return null;
    }

    @Override
    public List<VolumeSnapshot> getVolumeSnapshots(StorageVolume volume) {
        String driverName = this.getClass().getSimpleName();
        String msg = String.format("%s: %s --- operation is not supported.", driverName, "getVolumeSnapshots");
        _log.warn(msg);
        throw new UnsupportedOperationException(msg);
    }

    @Override
    public List<VolumeClone> getVolumeClones(StorageVolume volume) {
        String driverName = this.getClass().getSimpleName();
        String msg = String.format("%s: %s --- operation is not supported.", driverName, "getVolumeClones");
        _log.warn(msg);
        throw new UnsupportedOperationException(msg);
    }

    @Override
    public List<VolumeMirror> getVolumeMirrors(StorageVolume volume) {
        String driverName = this.getClass().getSimpleName();
        String msg = String.format("%s: %s --- operation is not supported.", driverName, "getVolumeMirrors");
        _log.warn(msg);
        throw new UnsupportedOperationException(msg);
    }

    @Override
    public DriverTask expandVolume(StorageVolume volume, long newCapacity) {
        String taskType = "expand-volume";
        String driverName = this.getClass().getSimpleName();
        String taskId = String.format("%s+%s+%s", driverName, taskType, UUID.randomUUID().toString());
        DriverTask task = new DefaultDriverTask(taskId);
        task.setStatus(DriverTask.TaskStatus.FAILED);

        String msg = String.format("%s: %s --- operation is not supported.", driverName, "expandVolume");
        _log.warn(msg);
        task.setMessage(msg);
        return null;
    }

    @Override
    public DriverTask deleteVolume(StorageVolume volume) {
        String taskType = "delete-storage-volume";
        String driverName = this.getClass().getSimpleName();
        String taskId = String.format("%s+%s+%s", driverName, taskType, UUID.randomUUID().toString());
        DriverTask task = new DefaultDriverTask(taskId);
        task.setStatus(DriverTask.TaskStatus.FAILED);

        String msg = String.format("%s: %s --- operation is not supported.", driverName, "deleteVolume");
        _log.warn(msg);
        task.setMessage(msg);
        return null;
    }

    @Override
    public DriverTask createVolumeSnapshot(List<VolumeSnapshot> snapshots, StorageCapabilities capabilities) {
        String taskType = "create-volume-snapshot";
        String driverName = this.getClass().getSimpleName();
        String taskId = String.format("%s+%s+%s", driverName, taskType, UUID.randomUUID().toString());
        DriverTask task = new DefaultDriverTask(taskId);
        task.setStatus(DriverTask.TaskStatus.FAILED);

        String msg = String.format("%s: %s --- operation is not supported.", driverName, "createVolumeSnapshot");
        _log.warn(msg);
        task.setMessage(msg);
        return null;
    }

    @Override
    public DriverTask restoreSnapshot(List<VolumeSnapshot> snapshots) {
        String driverName = this.getClass().getSimpleName();
        String taskId = String.format("%s+%s+%s", driverName, "restoreSnapshot", UUID.randomUUID().toString());
        DriverTask task = new DefaultDriverTask(taskId);
        task.setStatus(DriverTask.TaskStatus.FAILED);

        String msg = String.format("%s: %s --- operation is not supported.", driverName, "restoreSnapshot");
        _log.warn(msg);
        task.setMessage(msg);
        return null;
    }
    
    @Override
    public DriverTask deleteVolumeSnapshot(VolumeSnapshot snapshot) {
        String driverName = this.getClass().getSimpleName();
        String taskId = String.format("%s+%s+%s", driverName, "deleteVolumeSnapshot", UUID.randomUUID().toString());
        DriverTask task = new DefaultDriverTask(taskId);
        task.setStatus(DriverTask.TaskStatus.FAILED);

        String msg = String.format("%s: %s --- operation is not supported.", driverName, "deleteVolumeSnapshot");
        _log.warn(msg);
        task.setMessage(msg);
        return null;
    }

    @Override
    public DriverTask createVolumeClone(List<VolumeClone> clones, StorageCapabilities capabilities) {
        String driverName = this.getClass().getSimpleName();
        String taskId = String.format("%s+%s+%s", driverName, "createVolumeClone", UUID.randomUUID().toString());
        DriverTask task = new DefaultDriverTask(taskId);
        task.setStatus(DriverTask.TaskStatus.FAILED);

        String msg = String.format("%s: %s --- operation is not supported.", driverName, "createVolumeClone");
        _log.warn(msg);
        task.setMessage(msg);
        return null;
    }

    @Override
    public DriverTask detachVolumeClone(List<VolumeClone> clones) {
        String driverName = this.getClass().getSimpleName();
        String taskId = String.format("%s+%s+%s", driverName, "detachVolumeClone", UUID.randomUUID().toString());
        DriverTask task = new DefaultDriverTask(taskId);
        task.setStatus(DriverTask.TaskStatus.FAILED);

        String msg = String.format("%s: %s --- operation is not supported.", driverName, "detachVolumeClone");
        _log.warn(msg);
        task.setMessage(msg);
        return null;
    }

    @Override
    public DriverTask restoreFromClone(List<VolumeClone> clones) {
        String driverName = this.getClass().getSimpleName();
        String taskId = String.format("%s+%s+%s", driverName, "restoreFromClone", UUID.randomUUID().toString());
        DriverTask task = new DefaultDriverTask(taskId);
        task.setStatus(DriverTask.TaskStatus.FAILED);

        String msg = String.format("%s: %s --- operation is not supported.", driverName, "restoreFromClone");
        _log.warn(msg);
        task.setMessage(msg);
        return null;
    }

    @Override
    public DriverTask deleteVolumeClone(VolumeClone clone) {
        String driverName = this.getClass().getSimpleName();
        String taskId = String.format("%s+%s+%s", driverName, "deleteVolumeClone", UUID.randomUUID().toString());
        DriverTask task = new DefaultDriverTask(taskId);
        task.setStatus(DriverTask.TaskStatus.FAILED);

        String msg = String.format("%s: %s --- operation is not supported.", driverName, "deleteVolumeClone");
        _log.warn(msg);
        task.setMessage(msg);
        return null;
    }

    @Override
    public DriverTask createVolumeMirror(List<VolumeMirror> mirrors, StorageCapabilities capabilities) {
        String driverName = this.getClass().getSimpleName();
        String taskId = String.format("%s+%s+%s", driverName, "createVolumeMirror", UUID.randomUUID().toString());
        DriverTask task = new DefaultDriverTask(taskId);
        task.setStatus(DriverTask.TaskStatus.FAILED);

        String msg = String.format("%s: %s --- operation is not supported.", driverName, "createVolumeMirror");
        _log.warn(msg);
        task.setMessage(msg);
        return null;
    }

    @Override
    public DriverTask deleteVolumeMirror(VolumeMirror mirror) {
        String driverName = this.getClass().getSimpleName();
        String taskId = String.format("%s+%s+%s", driverName, "deleteVolumeMirror", UUID.randomUUID().toString());
        DriverTask task = new DefaultDriverTask(taskId);
        task.setStatus(DriverTask.TaskStatus.FAILED);

        String msg = String.format("%s: %s --- operation is not supported.", driverName, "deleteVolumeMirror");
        _log.warn(msg);
        task.setMessage(msg);
        return null;
    }

    @Override
    public DriverTask createConsistencyGroupMirror(VolumeConsistencyGroup consistencyGroup, List<VolumeMirror> mirrors, List<CapabilityInstance> capabilities) {
        String driverName = this.getClass().getSimpleName();
        String taskId = String.format("%s+%s+%s", driverName, "createConsistencyGroupMirror", UUID.randomUUID().toString());
        DriverTask task = new DefaultDriverTask(taskId);
        task.setStatus(DriverTask.TaskStatus.FAILED);

        String msg = String.format("%s: %s --- operation is not supported.", driverName, "createConsistencyGroupMirror");
        _log.warn(msg);
        task.setMessage(msg);
        return null;
    }

    @Override
    public DriverTask deleteConsistencyGroupMirror(List<VolumeMirror> mirrors) {
        String driverName = this.getClass().getSimpleName();
        String taskId = String.format("%s+%s+%s", driverName, "deleteConsistencyGroupMirror", UUID.randomUUID().toString());
        DriverTask task = new DefaultDriverTask(taskId);
        task.setStatus(DriverTask.TaskStatus.FAILED);

        String msg = String.format("%s: %s --- operation is not supported.", driverName, "deleteConsistencyGroupMirror");
        _log.warn(msg);
        task.setMessage(msg);
        return null;
    }
    
    @Override
    public DriverTask addVolumesToConsistencyGroup (List<StorageVolume> volumes, StorageCapabilities capabilities){
    	_log.info("addVolumesToConsistencyGroup : unsupported operation.");
    	String driverName = this.getClass().getSimpleName();
        String taskType = "add-volumes-to-consistency-groupd";
        String taskId = String.format("%s+%s+%s", driverName, taskType, UUID.randomUUID().toString());
        DriverTask task = new DefaultDriverTask(taskId);
        task.setStatus(DriverTask.TaskStatus.FAILED);
        
        String msg = String.format("addVolumesToConsistencyGroup: unsupported operation");
        _log.info(msg);
        task.setMessage(msg);
        
        return null;
    }
    
    @Override
    public DriverTask removeVolumesFromConsistencyGroup(List<StorageVolume> volumes,  StorageCapabilities capabilities){
    	_log.info("removeVolumesFromConsistencyGroup : unsupported operation.");
        String taskType = "remove-volumes-to-consistency-groupd";
        String driverName = this.getClass().getSimpleName();
        String taskId = String.format("%s+%s+%s", driverName, taskType, UUID.randomUUID().toString());
        DriverTask task = new DefaultDriverTask(taskId);
        task.setStatus(DriverTask.TaskStatus.FAILED);
        
        String msg = String.format("removeVolumesFromConsistencyGroup: unsupported operation");
        _log.info(msg);
        task.setMessage(msg);
        
        return null;
    }

    @Override
    public DriverTask splitVolumeMirror(List<VolumeMirror> mirrors) {
        String driverName = this.getClass().getSimpleName();
        String taskId = String.format("%s+%s+%s", driverName, "splitVolumeMirror", UUID.randomUUID().toString());
        DriverTask task = new DefaultDriverTask(taskId);
        task.setStatus(DriverTask.TaskStatus.FAILED);

        String msg = String.format("%s: %s --- operation is not supported.", driverName, "splitVolumeMirror");
        _log.warn(msg);
        task.setMessage(msg);
        return null;
    }

    @Override
    public DriverTask resumeVolumeMirror(List<VolumeMirror> mirrors) {
        String driverName = this.getClass().getSimpleName();
        String taskId = String.format("%s+%s+%s", driverName, "resumeVolumeMirror", UUID.randomUUID().toString());
        DriverTask task = new DefaultDriverTask(taskId);
        task.setStatus(DriverTask.TaskStatus.FAILED);

        String msg = String.format("%s: %s --- operation is not supported.", driverName, "resumeVolumeMirror");
        _log.warn(msg);
        task.setMessage(msg);
        return null;
    }

    @Override
    public DriverTask restoreVolumeMirror(List<VolumeMirror> mirrors) {
        String driverName = this.getClass().getSimpleName();
        String taskId = String.format("%s+%s+%s", driverName, "restoreVolumeMirror", UUID.randomUUID().toString());
        DriverTask task = new DefaultDriverTask(taskId);
        task.setStatus(DriverTask.TaskStatus.FAILED);

        String msg = String.format("%s: %s --- operation is not supported.", driverName, "restoreVolumeMirror");
        _log.warn(msg);
        task.setMessage(msg);
        return null;
    }

    @Override
    public Map<String, HostExportInfo> getVolumeExportInfoForHosts(StorageVolume volume) {
        String driverName = this.getClass().getSimpleName();
        String msg = String.format("%s: %s --- operation is not supported.", driverName, "getVolumeExportInfoForHosts");
        _log.warn(msg);
        throw new UnsupportedOperationException(msg);
    }

    @Override
    public Map<String, HostExportInfo> getSnapshotExportInfoForHosts(VolumeSnapshot snapshot) {
        String driverName = this.getClass().getSimpleName();
        String msg = String.format("%s: %s --- operation is not supported.", driverName, "getSnapshotExportInfoForHosts");
        _log.warn(msg);
        throw new UnsupportedOperationException(msg);
    }

    @Override
    public Map<String, HostExportInfo> getCloneExportInfoForHosts(VolumeClone clone) {
        String driverName = this.getClass().getSimpleName();
        String msg = String.format("%s: %s --- operation is not supported.", driverName, "getCloneExportInfoForHosts");
        _log.warn(msg);
        throw new UnsupportedOperationException(msg);
    }

    @Override
    public Map<String, HostExportInfo> getMirrorExportInfoForHosts(VolumeMirror mirror) {
        String driverName = this.getClass().getSimpleName();
        String msg = String.format("%s: %s --- operation is not supported.", driverName, "getMirrorExportInfoForHosts");
        _log.warn(msg);
        throw new UnsupportedOperationException(msg);
    }

    @Override
    public DriverTask exportVolumesToInitiators(List<Initiator> initiators, List<StorageVolume> volumes, Map<String, String> volumeToHLUMap, List<StoragePort> recommendedPorts, List<StoragePort> availablePorts, StorageCapabilities capabilities, MutableBoolean usedRecommendedPorts, List<StoragePort> selectedPorts) {
        String driverName = this.getClass().getSimpleName();
        String taskId = String.format("%s+%s+%s", driverName, "exportVolumesToInitiators", UUID.randomUUID().toString());
        DriverTask task = new DefaultDriverTask(taskId);
        task.setStatus(DriverTask.TaskStatus.FAILED);

        String msg = String.format("%s: %s --- operation is not supported.", driverName, "exportVolumesToInitiators");
        _log.warn(msg);
        task.setMessage(msg);
        return null;
    }

    @Override
    public DriverTask unexportVolumesFromInitiators(List<Initiator> initiators, List<StorageVolume> volumes) {
        String driverName = this.getClass().getSimpleName();
        String taskId = String.format("%s+%s+%s", driverName, "unexportVolumesFromInitiators", UUID.randomUUID().toString());
        DriverTask task = new DefaultDriverTask(taskId);
        task.setStatus(DriverTask.TaskStatus.FAILED);

        String msg = String.format("%s: %s --- operation is not supported.", driverName, "unexportVolumesFromInitiators");
        _log.warn(msg);
        task.setMessage(msg);
        return null;
    }

    @Override
    public DriverTask createConsistencyGroup(VolumeConsistencyGroup consistencyGroup) {
        String driverName = this.getClass().getSimpleName();
        String taskId = String.format("%s+%s+%s", driverName, "createConsistencyGroup", UUID.randomUUID().toString());
        DriverTask task = new DefaultDriverTask(taskId);
        task.setStatus(DriverTask.TaskStatus.FAILED);

        String msg = String.format("%s: %s --- operation is not supported.", driverName, "createConsistencyGroup");
        _log.warn(msg);
        task.setMessage(msg);
        return null;
    }

    @Override
    public DriverTask deleteConsistencyGroup(VolumeConsistencyGroup consistencyGroup) {
        String driverName = this.getClass().getSimpleName();
        String taskId = String.format("%s+%s+%s", driverName, "deleteConsistencyGroup", UUID.randomUUID().toString());
        DriverTask task = new DefaultDriverTask(taskId);
        task.setStatus(DriverTask.TaskStatus.FAILED);

        String msg = String.format("%s: %s --- operation is not supported.", driverName, "deleteConsistencyGroup");
        _log.warn(msg);
        task.setMessage(msg);
        return null;
    }

    @Override
    public DriverTask createConsistencyGroupSnapshot(VolumeConsistencyGroup consistencyGroup, List<VolumeSnapshot> snapshots, List<CapabilityInstance> capabilities) {
        String driverName = this.getClass().getSimpleName();
        String taskId = String.format("%s+%s+%s", driverName, "createConsistencyGroupSnapshot", UUID.randomUUID().toString());
        DriverTask task = new DefaultDriverTask(taskId);
        task.setStatus(DriverTask.TaskStatus.FAILED);

        String msg = String.format("%s: %s --- operation is not supported.", driverName, "createConsistencyGroupSnapshot");
        _log.warn(msg);
        task.setMessage(msg);
        return null;
    }

    @Override
    public DriverTask deleteConsistencyGroupSnapshot(List<VolumeSnapshot> snapshots) {
        String driverName = this.getClass().getSimpleName();
        String taskId = String.format("%s+%s+%s", driverName, "deleteConsistencyGroupSnapshot", UUID.randomUUID().toString());
        DriverTask task = new DefaultDriverTask(taskId);
        task.setStatus(DriverTask.TaskStatus.FAILED);

        String msg = String.format("%s: %s --- operation is not supported.", driverName, "deleteConsistencyGroupSnapshot");
        _log.warn(msg);
        task.setMessage(msg);
        return null;
    }

    @Override
    public DriverTask createConsistencyGroupClone(VolumeConsistencyGroup consistencyGroup, List<VolumeClone> clones, List<CapabilityInstance> capabilities) {
        String driverName = this.getClass().getSimpleName();
        String taskId = String.format("%s+%s+%s", driverName, "createConsistencyGroupClone", UUID.randomUUID().toString());
        DriverTask task = new DefaultDriverTask(taskId);
        task.setStatus(DriverTask.TaskStatus.FAILED);

        String msg = String.format("%s: %s --- operation is not supported.", driverName, "createConsistencyGroupClone");
        _log.warn(msg);
        task.setMessage(msg);
        return null;
    }

    @Override
    public RegistrationData getRegistrationData() {
        String driverName = this.getClass().getSimpleName();
        String msg = String.format("%s: %s --- operation is not supported.", driverName, "getRegistrationData");
        _log.warn(msg);
        throw new UnsupportedOperationException(msg);
    }

    @Override
    public DriverTask getTask(String taskId) {
        String driverName = this.getClass().getSimpleName();
        String msg = String.format("%s: %s --- operation is not supported.", driverName, "getTask");
        _log.warn(msg);
        throw new UnsupportedOperationException(msg);
    }

    @Override
    public <T extends StorageObject> T getStorageObject(String storageSystemId, String objectId, Class<T> type) {
        String driverName = this.getClass().getSimpleName();
        String msg = String.format("%s: %s --- operation is not supported.", driverName, "getStorageObject");
        _log.warn(msg);
        throw new UnsupportedOperationException(msg);
    }

    @Override
    public boolean validateStorageProviderConnection(StorageProvider storageProvider) {
        String driverName = this.getClass().getSimpleName();
        String msg = String.format("%s: %s --- operation is not supported.", driverName, "validateStorageProviderConnection");
        _log.warn(msg);
        throw new UnsupportedOperationException(msg);
    }

	/**
	 * Create driver task for task type
	 *
	 * @param taskType
	 */
	
	private DriverTask createDriverTask(String taskType) {
		String taskID = String.format("%s+%s+%s", PureStorageConstants.DRIVER_NAME, taskType, UUID.randomUUID());
		DriverTask task = new PureStorageDriverTask(taskID);
		return task;
	}
	
	private void setConnInfoToRegistry(String systemNativeId, String ipAddress, int port, String username,
			String password) {
		_log.info("PureStorageDriver:Saving connection info in registry enter");
		Map<String, List<String>> attributes = new HashMap<>();
		List<String> listIP = new ArrayList<>();
		List<String> listPort = new ArrayList<>();
		List<String> listUserName = new ArrayList<>();
		List<String> listPwd = new ArrayList<>();

		listIP.add(ipAddress);
		attributes.put(PureStorageConstants.IP_ADDRESS, listIP);
		listPort.add(Integer.toString(port));
		attributes.put(PureStorageConstants.PORT_NUMBER, listPort);
		listUserName.add(username);
		attributes.put(PureStorageConstants.USER_NAME, listUserName);
		listPwd.add(password);
		attributes.put(PureStorageConstants.PASSWORD, listPwd);
		this.driverRegistry.setDriverAttributesForKey(PureStorageConstants.DRIVER_NAME, systemNativeId, attributes);
		_log.info("PureStorageDriver:Saving connection info in registry leave");
	}
}