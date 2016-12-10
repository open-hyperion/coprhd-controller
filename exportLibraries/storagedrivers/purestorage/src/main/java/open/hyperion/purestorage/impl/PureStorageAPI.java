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

import javax.ws.rs.core.MultivaluedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.codehaus.jettison.json.JSONObject;

import com.google.gson.Gson;
import com.google.json.JsonSanitizer;
import com.sun.jersey.api.client.ClientResponse;

import static com.google.json.JsonSanitizer.*;

public class PureStorageAPI {

	private final URI _baseURL;
	private final RESTClient _client;
	private Logger _log = LoggerFactory.getLogger(PureStorageAPI.class);
	private String _authToken;
	private String _user;
	private String _password;

    // Authentication
	private static final URI URI_LOGIN   = URI.create("/api/1.8/auth/apitoken");
	private static final URI URI_SESSION = URI.create("/api/1.8/auth/session");

	public PureStorageAPI(URI endpoint, RESTClient client, String username, String password) {
		_baseURL  = endpoint;
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
			if (clientResp == null) {
				_log.error("PureStorageDriver:There is no response from PureStorage");
				throw new PureStorageException("There is no response from PureStorage");
			} else if (clientResp.getStatus() != 201) {
				String errResp = getResponseDetails(clientResp);
				throw PureStorageException(errResp);
			} else {
				JSONObject jObj = clientResp.getEntity(JSONObject.class);
				authToken = jObj.getString("api_token");
				this._authToken = authToken;
				this._user = user;
				this._password = password;
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
            clientResp = _client.post_json(_baseUrl.resolve(URI_LOGIN), body);
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

}