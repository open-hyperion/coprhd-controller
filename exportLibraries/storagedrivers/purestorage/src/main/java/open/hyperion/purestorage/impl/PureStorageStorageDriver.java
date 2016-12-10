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

import open.hyperion.purestorage.utils.PureStorageConstants;
import open.hyperion.purestorage.utils.PureStorageUtil;

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

	private PureStorageUtil _pureStorageUtil;
	private PureStorageConstants _pureStorageConstants;

	public void init() {
		ApplicationContext context = new ClassPathXmlApplicationContext(new String[] {PURESTORAGE_CONF_FILE}, _parentApplicationContext);
		_pureStorageUtil = (PureStorageUtil) context.getBean("pureStorageUtil");
	}

	public void setApplicationContext(ApplicationContext parentApplicationContext) {

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
			}

			// Verify user role
			pureStorageAPI.verifyUserRole(storageSystem.getUsername());

			// get storage details
			SystemCommandResult systemRes = pureStorageAPI.getSystemDetails();
			storageSystem.setSerialNumber(systemRes.getSerialNumber());
			storageSystem.setMajorVersion(systemRes.getSystemVersion());
			storageSystem.setMinorVersion("0"); // as there is no individual portion in PureStorage API
			
			// protocols supported
			List<String> protocols = new ArrayList<String>();
			protocols.add(Protocols.iSCSI.toString());
			protocols.add(Protocols.FC.toString());
			storageSystem.setProtocols(protocols);

			storageSystem.setFirmwareVersion(systemRes.getSystemVersion());
			if (systemRes.getSystemVersion().startsWith("3.1") || systemRes.getSystemVersion().startsWith("3.2.1") ) {
			    // SDK is taking care of unsupported message
			    storageSystem.setIsSupportedVersion(false);
			} else {
			    storageSystem.setIsSupportedVersion(true);
			}
			
			storageSystem.setModel(systemRes.getModel());
			storageSystem.setProvisioningType(SupportedProvisioningType.THIN_AND_THICK);
			Set<StorageSystem.SupportedReplication> supportedReplications = new HashSet<>();
            supportedReplications.add(StorageSystem.SupportedReplication.elementReplica);
            supportedReplications.add(StorageSystem.SupportedReplication.groupReplica);
			storageSystem.setSupportedReplications(supportedReplications);

			// Storage object properties
			storageSystem.setNativeId(uniqueId + ":" + systemRes.getSerialNumber());

			if (storageSystem.getDeviceLabel() == null) {
				if (storageSystem.getDisplayName() != null) {
					storageSystem.setDeviceLabel(storageSystem.getDisplayName());
				} else if (systemRes.getName() != null) {
					storageSystem.setDeviceLabel(systemRes.getName());
					storageSystem.setDisplayName(systemRes.getName());
				}
			}

			storageSystem.setAccessStatus(AccessStatus.READ_WRITE);
			setConnInfoToRegistry(storageSystem.getNativeId(), storageSystem.getIpAddress(),
					storageSystem.getPortNumber(), storageSystem.getUsername(), storageSystem.getPassword());

			task.setStatus(DriverTask.TaskStatus.READY);
			_log.info("PureStorageDriver: Successfull discovery storage system {}, name {} - end",
					storageSystem.getIpAddress(), storageSystem.getSystemName());
		} catch (Exception e) {
			String msg = String.format("PureStorageDriver: Unable to discover the storage system %s ip %s; Error: %s.\n",
					storageSystem.getSystemName(), storageSystem.getIpAddress(), e);
			_log.error(msg);
			_log.error(CompleteError.getStackTrace(e));
			task.setMessage(msg);
			task.setStatus(DriverTask.TaskStatus.FAILED);
			e.printStackTrace();
		}

		return task;
	}

	/**
	 * Create driver task for task type
	 *
	 * @param taskType
	 */
	private DriverTask createDriverTask(String taskType) {
		String taskID = String.format("%s+%s+%s", PureStorageConstants.DRIVER_NAME, taskType, UUID.randomUUID());
		DriverTask task = new HP3PARDriverTask(taskID);
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
		attributes.put(HP3PARConstants.IP_ADDRESS, listIP);
		listPort.add(Integer.toString(port));
		attributes.put(HP3PARConstants.PORT_NUMBER, listPort);
		listUserName.add(username);
		attributes.put(HP3PARConstants.USER_NAME, listUserName);
		listPwd.add(password);
		attributes.put(HP3PARConstants.PASSWORD, listPwd);
		this.driverRegistry.setDriverAttributesForKey(PureStorageConstants.DRIVER_NAME, systemNativeId, attributes);
		_log.info("PureStorageDriver:Saving connection info in registry leave");
	}

}