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

package open.hyperion.purestorage.utils;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import open.hyperion.purestorage.connection.PureStorageAPIFactory;
import open.hyperion.purestorage.impl.PureStorageAPI;
import open.hyperion.purestorage.impl.PureStorageException;

import com.emc.storageos.storagedriver.Registry;
import com.emc.storageos.storagedriver.model.StoragePort;
import com.emc.storageos.storagedriver.model.StorageSystem;
import com.emc.storageos.storagedriver.model.StoragePort.TransportType;

public class PureStorageUtil {

	private static final Logger _log = LoggerFactory.getLogger(PureStorageUtil.class);
	
	private PureStorageAPIFactory _pureStorageAPIFactory;
	
	public PureStorageAPI getPureStorageDevice(StorageSystem pureStorageSystem) throws PureStorageException {
		URI deviceURI;
		_log.info("PureStorageDriver:getPureStorageDevice input storage system");

		try {
			deviceURI = new URI("https", null, pureStorageSystem.getIpAddress(), pureStorageSystem.getPortNumber(), "/", null,
					null);
			return _pureStorageAPIFactory.getRESTClient(deviceURI, pureStorageSystem.getUsername(), pureStorageSystem.getPassword());
		} catch (Exception e) {
			e.printStackTrace();
			_log.error("PureStorageDriver:Error in getting PureStorage device, with StorageSystem");
			throw new PureStorageException("Error in getting PureStorage device");
		}
	}

	public PureStorageAPI getPureStorageDevice(String ip, String port, String user, String pass) throws PureStorageException {
		URI deviceURI;
		_log.info("PureStorageDriver:getPureStorageDevice input full details");

		try {
			deviceURI = new URI("https", null, ip, Integer.parseInt(port), "/", null, null);
			return _pureStorageAPIFactory.getRESTClient(deviceURI, user, pass);
		} catch (Exception e) {
			e.printStackTrace();
			_log.error("PureStorageDriver:Error in getting PureStorage device with details");
			throw new PureStorageException("Error in getting PureStorage device");
		}
	}
}