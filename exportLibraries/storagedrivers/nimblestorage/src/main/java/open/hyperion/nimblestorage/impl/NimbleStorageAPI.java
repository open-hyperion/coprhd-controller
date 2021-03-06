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

package open.hyperion.nimblestorage.impl;

import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.codehaus.jettison.json.JSONObject;

import com.google.gson.Gson;
import com.google.json.JsonSanitizer;
import com.sun.jersey.api.client.ClientResponse;

import open.hyperion.nimblestorage.connection.RESTClient;
import open.hyperion.nimblestorage.impl.NimbleStorageException;

import open.hyperion.nimblestorage.command.Privileges;
import open.hyperion.nimblestorage.command.UserRoleCommandResult;
import open.hyperion.nimblestorage.command.SystemCommandResult;


import static com.google.json.JsonSanitizer.*;

public class NimbleStorageAPI {

	private final URI _baseURL;
	private final RESTClient _client;
	private Logger _log = LoggerFactory.getLogger(NimbleStorageAPI.class);
	private String _authToken;
	private String _user;
	private String _password;

    // Authentication
	private static final URI URI_LOGIN   = URI.create("/api/1.8/auth/apitoken");
	private static final URI URI_SESSION = URI.create("/api/1.8/auth/session");

	private static final String URI_SYSTEM    = "/api/v1/system";
	private static final String URI_USER_ROLE = "/api/v1/users/{0}";


	public NimbleStorageAPI(URI baseURL, RESTClient client, String username, String password) {
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
		_log.info("NimbleStorage:getAuthToken enter");

		String authToken = null;
		ClientResponse clientResp = null;
		String body = "{\"data\":{\"username\":\"" + username + "\", \"password\":\"" + password + "\"}}";

		try {
			clientResp = _client.post_json(_baseURL.resolve(URI_LOGIN), body);
			if (clientResp == null) {
				_log.error("NimbleStorageDriver:There is no response from NimbleStorage");
				throw new NimbleStorageException("There is no response from NimbleStorage");
			} else if (clientResp.getStatus() != 201) {
				String errResp = getResponseDetails(clientResp);
				throw new NimbleStorageException(errResp);
			} else {
				JSONObject jObj = clientResp.getEntity(JSONObject.class);
				authToken = jObj.getString("api_token");
				_authToken = authToken;
				_user = username;
				_password = password;
				_log.info("NimbleStorageDriver:getAuthToken set");
			}
			return authToken;
		} catch (Exception e) {
			throw e;
		} finally {
			if (clientResp != null) {
				clientResp.close();
			}
			_log.info("NimbleStorageDriver:getAuthToken leave");
		} //end try/catch/finally
	}

    /**
     * Get authentication token from the storage using previously saved username and password
     * @return authentication token
     * @throws Exception
     */
    public String getAuthToken() throws Exception {
        _log.info("NimbleStorageDriver:getAuthToken enter, after expiry");
        String authToken = null;
        ClientResponse clientResp = null;
        String body= "{\"username\":\"" + _user + "\", \"password\":\"" + _password + "\"}";

        try {
            clientResp = _client.post_json(_baseURL.resolve(URI_LOGIN), body);
            if (clientResp == null) {
                _log.error("NimbleStorageDriver:There is no response from NimbleStorage");
                throw new NimbleStorageException("There is no response from NimbleStorage");
            } else if (clientResp.getStatus() != 201) {
                String errResp = getResponseDetails(clientResp);
                throw new NimbleStorageException(errResp);
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
            _log.info("NimbleStorageDriver:getAuthToken leave, after expiry");
        } //end try/catch/finally
    }

    private String getResponseDetails(ClientResponse clientResp) {
        String detailedResponse = null, ref=null;
        try {
            JSONObject jObj = clientResp.getEntity(JSONObject.class);
            detailedResponse = String.format("NimbleStorage error code: %s, Description: %s",
                    jObj.getString("code"), jObj.getString("desc"));
            if (jObj.has("ref")) {
                ref = String.format(", refer:%s", jObj.getString("ref"));
                detailedResponse = detailedResponse + ref;
            }
            _log.error(String.format("NimbleStorageDriver:HTTP error code: %d, Complete NimbleStorage error response: %s", clientResp.getStatus(),
                    jObj.toString()));
        } catch (Exception e) {
            _log.error("NimbleStorageDriver:Unable to get NimbleStorage error details");
            detailedResponse = String.format("%1$s", (clientResp == null) ? "" : clientResp);
        }
        return detailedResponse;
    }

    public void verifyUserRole(String name) throws Exception {
        _log.info("NimbleStorageDriver:verifyUserRole enter");
        ClientResponse clientResp = null;
        final String path = MessageFormat.format(URI_USER_ROLE, name);
        _log.info("NimbleStorageDriver: verifyUserRole path is {}", path);

        try {
            clientResp = get(path);
            if (clientResp == null) {
                _log.error("NimbleStorageDriver:There is no response from 3PAR");
                throw new NimbleStorageException("There is no response from 3PAR");
            } else if (clientResp.getStatus() != 200) {
                String errResp = getResponseDetails(clientResp);
                throw new NimbleStorageException(errResp);
            } else {
                String responseString = clientResp.getEntity(String.class);
                _log.info("NimbleStorageDriver:getSystemDetails 3PAR response is {}", responseString);
                UserRoleCommandResult roleRes = new Gson().fromJson(sanitize(responseString),
                        UserRoleCommandResult.class);

                boolean superUser = false;
                for (Privileges currPriv:roleRes.getPrivileges()) {

                    if ( (currPriv.getDomain().compareToIgnoreCase("all") == 0) && 
                            (currPriv.getRole().compareToIgnoreCase("super") == 0)) {
                        superUser = true;
                    }
                }
                
                if (superUser == false) {
                    _log.error("NimbleStorageDriver:User does not have sufficient privilege to discover");
                    throw new NimbleStorageException("User does not have sufficient privilege");
                } else {
                    _log.info("NimbleStorageDriver:User is super user");
                }
                
            }
        } catch (Exception e) {
            throw e;
        } finally {
            if (clientResp != null) {
                clientResp.close();
            }
            _log.info("NimbleStorageDriver:verifyUserRole leave");
        } //end try/catch/finally
    }

    /**
     * Gets the storage array information
     * @return array details
     * @throws Exception
     */
    public SystemCommandResult getSystemDetails() throws Exception {
        _log.info("NimbleStorageDriver:getSystemDetails enter");
        ClientResponse clientResp = null;

        try {
            clientResp = get(URI_SYSTEM);
            if (clientResp == null) {
                _log.error("NimbleStorageDriver:There is no response from Nimble Storage");
                throw new NimbleStorageException("There is no response from Nimble Storage");
            } else if (clientResp.getStatus() != 200) {
                String errResp = getResponseDetails(clientResp);
                throw new NimbleStorageException(errResp);
            } else {
                String responseString = clientResp.getEntity(String.class);
                _log.info("NimbleStorageDriver:getSystemDetails Nimble Storage response is {}", responseString);
                SystemCommandResult systemRes = new Gson().fromJson(sanitize(responseString),
                        SystemCommandResult.class);
                return systemRes;
            }
        } catch (Exception e) {
            throw e;
        } finally {
            if (clientResp != null) {
                clientResp.close();
            }
            _log.info("NimbleStorageDriver:getSystemDetails leave");
        } //end try/catch/finally
    }

    private ClientResponse get(final String uri) throws Exception {
        ClientResponse clientResp = _client.get_json(_baseURL.resolve(uri), _authToken);
        if (clientResp.getStatus() == 403) {
            getAuthToken();
            clientResp = _client.get_json(_baseURL.resolve(uri), _authToken);
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
    
    private ClientResponse put(final String uri, String body) throws Exception {
        ClientResponse clientResp = _client.put_json(_baseURL.resolve(uri), _authToken, body);
        if (clientResp.getStatus() == 403) {
            getAuthToken();
            clientResp = _client.put_json(_baseURL.resolve(uri), _authToken, body);
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
}