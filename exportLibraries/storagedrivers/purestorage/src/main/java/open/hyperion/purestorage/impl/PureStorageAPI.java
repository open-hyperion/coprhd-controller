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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.codehaus.jettison.json.JSONObject;

import com.google.gson.Gson;
import com.google.json.JsonSanitizer;
import com.sun.jersey.api.client.ClientResponse;

import open.hyperion.purestorage.connection.RESTClient;

import open.hyperion.purestorage.impl.PureStorageException;

import open.hyperion.purestorage.utils.PureStorageUtil;

import open.hyperion.purestorage.command.Privileges;
import open.hyperion.purestorage.command.UserRoleCommandResult;
import open.hyperion.purestorage.command.SystemCommandResult;
import open.hyperion.purestorage.command.array.ArrayControllerCommandResult;
import open.hyperion.purestorage.command.array.ArrayCommandResult;
import open.hyperion.purestorage.command.array.ArraySpaceCommandResult;
import open.hyperion.purestorage.command.port.StoragePortResult;
import open.hyperion.purestorage.command.port.ArrayPortCommandResult;
import open.hyperion.purestorage.command.hardware.HardwareCommandResult;
import open.hyperion.purestorage.command.host.HostCommandResult;

import com.emc.storageos.storagedriver.model.StoragePort;
import com.emc.storageos.storagedriver.model.StoragePort.TransportType;
import com.emc.storageos.storagedriver.model.StoragePort.PortType;
import com.emc.storageos.storagedriver.model.StoragePort.OperationalStatus;

import static com.google.json.JsonSanitizer.*;

public class PureStorageAPI {

	private final URI _baseURL;
	private final RESTClient _client;
	private Logger _log = LoggerFactory.getLogger(PureStorageAPI.class);
	private String _authToken;
	private String _user;
	private String _password;

    // Authentication
	private static final URI URI_LOGIN   = URI.create("/api/1.6/auth/apitoken");
	private static final URI URI_SESSION = URI.create("/api/1.6/auth/session");
    private static List<NewCookie> _cookies;

	private static final String URI_SYSTEM    = "/api/1.6/array";
	private static final String URI_USER_ROLE = "/api/1.6/admin/{0}";

	private static final String URI_ARRAY_SPACE = "/api/1.6/array?space=true";
    private static final String URI_PORT        = "/api/1.6/port?initiators=true";
    private static final String URI_HARDWARE    = "/api/1.6/hardware";
    private static final String URI_HOST        = "/api/1.6/host?all=true";


	public PureStorageAPI(URI baseURL, RESTClient client, String username, String password) {
		_baseURL  = baseURL;
		_client   = client;
		_user     = username;
		_password = password;
	}

	public void close() {
		_client.close();
	}

    /**
     * Get authentication token from the storage
     * @param user user name 
     * @param password password
     * @return authentication token
     * @throws Exception
     */
	public String getAuthToken(String username, String password) throws Exception {
		_log.info("PureStorage:getAuthToken enter");

		String authToken = null;
		ClientResponse clientResp = null;
		String body = "{\"username\":\"" + username + "\", \"password\":\"" + password + "\"}";

		try {
			clientResp = _client.post_json(_baseURL.resolve(URI_LOGIN), body);
			_log.info(clientResp.toString());
			if (clientResp == null) {
				_log.error("PureStorageDriver:There is no response from PureStorage");
				throw new PureStorageException("There is no response from PureStorage");
			} else if (clientResp.getStatus() != 200) {
				String errResp = getResponseDetails(clientResp);
				throw new PureStorageException(errResp);
			} else {
				JSONObject jObj = clientResp.getEntity(JSONObject.class);
				authToken = jObj.getString("api_token");
				_authToken = authToken;
				_user = username;
				_password = password;
				_log.info("PureStorageDriver:getAuthToken set");
			}
			return authToken;
		} catch (Exception e) {
			throw e;
		} finally {
			if (clientResp != null) {
				clientResp.close();
			}
			_log.info("PureStorageDriver:getAuthToken leave");
		} //end try/catch/finally
	}

    /**
     * Get authentication token from the storage using previously saved username and password
     * @return authentication token
     * @throws Exception
     */
    public String getAuthToken() throws Exception {
        _log.info("PureStorageDriver:getAuthToken enter, after expiry");
        String authToken = null;
        ClientResponse clientResp = null;
        String body= "{\"username\":\"" + _user + "\", \"password\":\"" + _password + "\"}";

        try {
            clientResp = _client.post_json(_baseURL.resolve(URI_LOGIN), body);
            if (clientResp == null) {
                _log.error("PureStorageDriver:There is no response from PureStorage");
                throw new PureStorageException("There is no response from PureStorage");
            } else if (clientResp.getStatus() != 201) {
                String errResp = getResponseDetails(clientResp);
                throw new PureStorageException(errResp);
            } else {
                JSONObject jObj = clientResp.getEntity(JSONObject.class);
                authToken = jObj.getString("key");
            }
            _authToken = authToken;
            return authToken;
        } catch (Exception e) {
            throw e;
        } finally {
            if (clientResp != null) {
                clientResp.close();
            }
            _log.info("PureStorageDriver:getAuthToken leave, after expiry");
        } //end try/catch/finally
    }

    public String createSession() throws Exception {
		_log.info("PureStorage:createSession enter");

		String username = null;
		ClientResponse clientResp = null;
		String body = "{\"api_token\":\"" + _authToken + "\"}";
		try {
			clientResp = _client.post_json(_baseURL.resolve(URI_SESSION), body);
			_log.info(clientResp.toString());

			_cookies = clientResp.getCookies();
			if (clientResp == null) {
				_log.error("PureStorageDriver:createSession There is no response from PureStorage");
				throw new PureStorageException("There is no response from PureStorage");
			} else if (clientResp.getStatus() != 200) {
				String errResp = getResponseDetails(clientResp);
				throw new PureStorageException(errResp);
			} else {
				JSONObject jObj = clientResp.getEntity(JSONObject.class);
				username = jObj.getString("username");

				if (_user.equals(username)) {
					_log.info("PureStorageDriver:createSession session created");
				}
				else {
					_log.error("PureStorageDriver:createSession Session not created.  Names do not match.");
					throw new PureStorageException("PureStorageDriver:createSession Session not created.  Names do not match.  " + _user + " =/= " + username);	
				}
			}
			return username;
		} catch (Exception e) {
			throw e;
		} finally {
			if (clientResp != null) {
				clientResp.close();
			}
			_log.info("PureStorageDriver:createSession leave");
		} //end try/catch/finally
    }

    private String getResponseDetails(ClientResponse clientResp) {
        String detailedResponse = null, ref=null;
        try {
            JSONObject jObj = clientResp.getEntity(JSONObject.class);
            detailedResponse = String.format("PureStorage error code: %s, Description: %s",
                    jObj.getString("code"), jObj.getString("desc"));
            if (jObj.has("ref")) {
                ref = String.format(", refer:%s", jObj.getString("ref"));
                detailedResponse = detailedResponse + ref;
            }
            _log.error(String.format("PureStorageDriver:HTTP error code: %d, Complete PureStorage error response: %s", clientResp.getStatus(),
                    jObj.toString()));
        } catch (Exception e) {
            _log.error("PureStorageDriver:Unable to get PureStorage error details");
            detailedResponse = String.format("%1$s", (clientResp == null) ? "" : clientResp);
        }
        return detailedResponse;
    }

    /**
     * Gets the list of array controllers and their respective details
     * @return an array of controller details.
     * @throws Exception
     */
    public ArrayControllerCommandResult[] getArrayControllerDetails () throws Exception {
    	_log.info("PureStorageDriver:getArrayControllerDetails enter");
    	ClientResponse clientResp = null;

    	try {
    		clientResp = getUsingSession(URI_SYSTEM + "?controllers=true");
            if (clientResp == null) {
                _log.error("PureStorageDriver:getArrayControllerDetails There is no response from Pure Storage");
                throw new PureStorageException("There is no response from Pure Storage");
            } else if (clientResp.getStatus() != 200) {
                String errResp = getResponseDetails(clientResp);
                throw new PureStorageException(errResp);
            } else {
                String responseString = clientResp.getEntity(String.class);
                _log.info("PureStorageDriver:getArrayControllerDetails Pure Storage response is {}", responseString);
                ArrayControllerCommandResult[] arrConResArray = new Gson().fromJson(sanitize(responseString),
                    ArrayControllerCommandResult[].class);
                return arrConResArray;
            }
    	} catch (Exception e) {
    		throw e;
    	} finally {
    		if (clientResp != null) {
    			clientResp.close();
    		}
    		_log.info("PureStorageDriver:getArrayControllerDetails leave");
    	} //end try/catch/finally
    }

    /**
     * Gets the storage array information
     * @return array details
     * @throws Exception
     */
    public ArrayCommandResult getArrayDetails() throws Exception {
        _log.info("PureStorageDriver:getArrayDetails enter");
        ClientResponse clientResp = null;

        try {
            clientResp = getUsingSession(URI_SYSTEM);
            if (clientResp == null) {
                _log.error("PureStorageDriver:getArrayDetails There is no response from Pure Storage");
                throw new PureStorageException("There is no response from Pure Storage");
            } else if (clientResp.getStatus() != 200) {
                String errResp = getResponseDetails(clientResp);
                throw new PureStorageException(errResp);
            } else {
                String responseString = clientResp.getEntity(String.class);
                _log.info("PureStorageDriver:getArrayDetails Pure Storage response is {}", responseString);
                ArrayCommandResult arrayRes = new Gson().fromJson(sanitize(responseString),
                    ArrayCommandResult.class);
                return arrayRes;
            }
        } catch (Exception e) {
            throw e;
        } finally {
            if (clientResp != null) {
                clientResp.close();
            }
            _log.info("PureStorageDriver:getArrayDetails leave");
        } //end try/catch/finally
    }

    /**
     * Gets the storage array information
     * @return array space details
     * @throws Exception
     */
    public ArraySpaceCommandResult[] getSpaceDetails() throws Exception {
        _log.info("PureStorageDriver:getSpaceDetails enter");
        ClientResponse clientResp = null;

    	try {
    		clientResp = getUsingSession(URI_ARRAY_SPACE);
    		if (clientResp == null) {
                _log.error("PureStorageDriver:getSpaceDetails There is no response from Pure Storage");
                throw new PureStorageException("There is no response from Pure Storage");
    		} else if (clientResp.getStatus() != 200) {
                String errResp = getResponseDetails(clientResp);
                throw new PureStorageException(errResp);
    		} else {
                String responseString = clientResp.getEntity(String.class);
                _log.info("PureStorageDriver:getSpaceDetails Pure Storage response is {}", responseString);
                ArraySpaceCommandResult[] arraySpRes = new Gson().fromJson(sanitize(responseString),
                    ArraySpaceCommandResult[].class);
                return arraySpRes;
    		}
    	} catch (Exception e) {
    		throw e;
    	} finally {
            if (clientResp != null) {
                clientResp.close();
            }
            _log.info("PureStorageDriver:getSpaceDetails leave");
    	}  //end try/catch/finally
    }

    /**
     * Gets the port information
     * @return array and initiator ports
     * @throws Exception
     */
    public ArrayPortCommandResult[] getPortDetails() throws Exception {
        _log.info("PureStorageDriver:getPortDetails enter");
        ClientResponse clientResp = null;
        try {
            clientResp = getUsingSession(URI_PORT);
            if (clientResp == null) {
                _log.error("PureStorageDriver:getPortDetails There is no response from Pure Storage");
                throw new PureStorageException("There is no response from Pure Storage");
            } else if (clientResp.getStatus() != 200) {
                String errResp = getResponseDetails(clientResp);
                throw new PureStorageException(errResp);
            } else {
                String responseString = clientResp.getEntity(String.class);
                _log.info("PureStorageDriver:getPortDetails Pure Storage response is {}", responseString);
                ArrayPortCommandResult[] portRes = new Gson().fromJson(sanitize(responseString),
                    ArrayPortCommandResult[].class);
                return portRes;
            }
        } catch (Exception e) {
            throw e;
        } finally {
            if (clientResp != null) {
                clientResp.close();
            }
            _log.info("PureStorageDriver:getPortDetails leave");
        }  //end try/catch/finally
    }

    /**
     * Gets the hardware information
     * @return hardware details
     * @throws Exception
     */
    public HardwareCommandResult[] getHardwareDetails() throws Exception {
        _log.info("PureStorageDriver:getHardwareDetails enter");
        ClientResponse clientResp = null;
        try {
            clientResp = getUsingSession(URI_HARDWARE);
            if (clientResp == null) {
                _log.error("PureStorageDriver:getHardwareDetails There is no response from Pure Storage");
                throw new PureStorageException("There is no response from Pure Storage");
            } else if (clientResp.getStatus() != 200) {
                String errResp = getResponseDetails(clientResp);
                throw new PureStorageException(errResp);
            } else {
                String responseString = clientResp.getEntity(String.class);
                _log.info("PureStorageDriver:getHardwareDetails Pure Storage response is {}", responseString);
                HardwareCommandResult[] hwRes = new Gson().fromJson(sanitize(responseString),
                    HardwareCommandResult[].class);
                return hwRes;
            }
        } catch (Exception e) {
            throw e;
        } finally {
            if (clientResp != null) {
                clientResp.close();
            }
            _log.info("PureStorageDriver:getHardwareDetails leave");
        }  //end try/catch/finally
    }

    public StoragePortResult[] getStoragePortDetails (String storageSystemId) throws Exception {
        _log.info("PureStorageDriver:getStoragePortDetails enter");
        Map<String, StoragePortResult> storagePorts = new HashMap<>();
        try {
            HardwareCommandResult[]  hcr  = getHardwareDetails();
            ArrayPortCommandResult[] apcr = getPortDetails();
            for (ArrayPortCommandResult ap : apcr) {
                StoragePortResult spr = new StoragePortResult();
                spr.setStorageSystemId(storageSystemId);
                spr.setPortName(PureStorageUtil.formatMacAddress(ap.getTarget()));
                spr.setPortNetworkId(PureStorageUtil.formatMacAddress(ap.getTargetWwn()));
                spr.setTransportType(TransportType.FC);
                spr.setNativeId(ap.getTarget());
                storagePorts.put(ap.getTarget(), spr);
            }
            for (HardwareCommandResult h : hcr) {
                StoragePortResult s = storagePorts.get(h.getName());
                if (s != null) {
                    if (h.getStatus().equalsIgnoreCase("ok")) {
                        s.setOperationalStatus(OperationalStatus.OK);
                    } else {
                        s.setOperationalStatus(OperationalStatus.NOT_OK);
                    }
                    s.setPortSpeed(Long.valueOf(h.getSpeed()));
                }
            }
            Object[] a = storagePorts.values().toArray();
            StoragePortResult[] c = new StoragePortResult[a.length];
            _log.info("Number of storage ports: " + a.length);
            for(int i = 0; i < a.length; i++)
            {
                c[i] = (StoragePortResult) a[i];
                _log.info("port index: " + i + " -" + c[i].getPortName() + " port wwn: " + c[i].getPortNetworkId());
            }
            return c;
        } catch (Exception e) {
            throw e;
        } finally {
            _log.info("PureStorageDriver:getStoragePortDetails leave");
        }
    }

    public HostCommandResult[] getHostsDetails () throws Exception {
        _log.info("PureStorageDriver:getHosts enter");
        ClientResponse clientResp = null;
        try {
            clientResp = getUsingSession(URI_HOST);
            if (clientResp == null) {
                _log.error("PureStorageDriver:getHosts There is no response from Pure Storage");
                throw new PureStorageException("There is no response from Pure Storage");
            } else if (clientResp.getStatus() != 200) {
                String errResp = getResponseDetails(clientResp);
                throw new PureStorageException(errResp);
            } else {
                String responseString = clientResp.getEntity(String.class);
                _log.info("PureStorageDriver:getHosts Pure Storage response is {}", responseString);
                HostCommandResult[] hRes = new Gson().fromJson(sanitize(responseString),
                    HostCommandResult[].class);
                return hRes;
            }
        } catch (Exception e) {
            throw e;
        } finally {
            _log.info("PureStorageDriver:getHosts leave");
        }

    }
    
    private ClientResponse get(final String uri) throws Exception {
        ClientResponse clientResp = _client.get_json(_baseURL.resolve(uri), _authToken);
        if (clientResp.getStatus() == 403) {
            getAuthToken();
            clientResp = _client.get_json(_baseURL.resolve(uri), _authToken);
        }
        return clientResp;
    }

    private ClientResponse getUsingSession(final String uri) throws Exception {
        
        ClientResponse clientResp = _client.get_json(_baseURL.resolve(uri), _cookies);
        _log.info("Response status from  - " + _baseURL.resolve(uri).toString() + ": " + clientResp.getStatus());
        if (clientResp.getStatus() == 403) {
            getAuthToken();
            createSession();
            clientResp = _client.get_json(_baseURL.resolve(uri), _cookies);
        }
        return clientResp;
    }
    
    private ClientResponse post(final String uri, String body) throws Exception {
        ClientResponse clientResp = _client.post_json(_baseURL.resolve(uri), _authToken, body);
        if (clientResp.getStatus() == 403) {
            getAuthToken();
            clientResp = _client.post_json(_baseURL.resolve(uri), _authToken, body);
        }
        return clientResp;
    }

    private ClientResponse postUsingSession(final String uri, String body) throws Exception {
        ClientResponse clientResp = _client.post_json(_baseURL.resolve(uri), _cookies, body);
        if (clientResp.getStatus() == 403) {
            getAuthToken();
            createSession();
            clientResp = _client.post_json(_baseURL.resolve(uri), _cookies, body);
        }
        return clientResp;
    }
    
    private ClientResponse put(final String uri, String body) throws Exception {
        ClientResponse clientResp = _client.put_json(_baseURL.resolve(uri), _authToken, body);
        if (clientResp.getStatus() == 403) {
            getAuthToken();
            clientResp = _client.put_json(_baseURL.resolve(uri), _authToken, body);
        }
        return clientResp;
    }

    private ClientResponse putUsingSession(final String uri, String body) throws Exception {
        ClientResponse clientResp = _client.put_json(_baseURL.resolve(uri), _cookies, body);
        if (clientResp.getStatus() == 403) {
            getAuthToken();
            createSession();
            clientResp = _client.put_json(_baseURL.resolve(uri), _cookies, body);
        }
        return clientResp;
    }
    
    private ClientResponse delete(final String uri) throws Exception {
        ClientResponse clientResp = _client.delete_json(_baseURL.resolve(uri), _authToken);
        if (clientResp.getStatus() == 403) {
            getAuthToken();
            clientResp = _client.delete_json(_baseURL.resolve(uri), _authToken);
        }
        return clientResp;
    }        

    private ClientResponse deleteUsingSession(final String uri) throws Exception {
        ClientResponse clientResp = _client.delete_json(_baseURL.resolve(uri), _cookies);
        if (clientResp.getStatus() == 403) {
            getAuthToken();
            createSession();
            clientResp = _client.delete_json(_baseURL.resolve(uri), _cookies);
        }
        return clientResp;
    }        
}