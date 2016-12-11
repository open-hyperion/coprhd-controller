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

package open.hyperion.nimblestorage.connection;

import java.io.IOException;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.security.SecureRandom;
import java.security.cert.CertificateException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.net.ssl.X509TrustManager;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpMethodRetryHandler;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.log4j.BasicConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
import com.emc.storageos.hp3par.command.CPGCommandResult;
import com.emc.storageos.hp3par.command.HostCommandResult;
import com.emc.storageos.hp3par.command.HostMember;
import com.emc.storageos.hp3par.command.HostSetDetailsCommandResult;
import com.emc.storageos.hp3par.command.PortStatisticsCommandResult;
import com.emc.storageos.hp3par.command.SystemCommandResult;
import com.emc.storageos.hp3par.command.VirtualLunsList;
import com.emc.storageos.hp3par.command.VolumeDetailsCommandResult;
*/

import open.hyperion.nimblestorage.impl.NimbleStorageAPI;
import open.hyperion.nimblestorage.impl.NimbleStorageException;
import open.hyperion.nimblestorage.utils.CompleteError;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandler;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.client.apache.ApacheHttpClient;
import com.sun.jersey.client.apache.ApacheHttpClientHandler;
import com.sun.jersey.client.urlconnection.HTTPSProperties;
import com.sun.jersey.client.urlconnection.URLConnectionClientHandler;

/*
 * NimbleStorage API client factory
 */
public class NimbleStorageAPIFactory {
    private Logger _log = LoggerFactory.getLogger(NimbleStorageAPIFactory.class);
    private static final int DEFAULT_MAX_CONN = 300;
    private static final int DEFAULT_MAX_CONN_PER_HOST = 100;
    private static final int DEFAULT_CONN_TIMEOUT = 1000 * 30;
    private static final int DEFAULT_CONN_MGR_TIMEOUT = 1000 * 60;
    private static final int DEFAULT_SOCKET_CONN_TIMEOUT = 1000 * 60 * 60;

    private int _maxConn = DEFAULT_MAX_CONN;
    private int _maxConnPerHost = DEFAULT_MAX_CONN_PER_HOST;
    private int _connTimeout = DEFAULT_CONN_TIMEOUT;
    private int _socketConnTimeout = DEFAULT_SOCKET_CONN_TIMEOUT;
    private int _connManagerTimeout = DEFAULT_CONN_MGR_TIMEOUT;

    private ConcurrentMap<String, NimbleStorageAPI> _clientMap;
    private MultiThreadedHttpConnectionManager _connectionManager;

    /**
     * Maximum number of outstanding connections
     *
     * @param maxConn
     */
    public void setMaxConnections(int maxConn) {
        _maxConn = maxConn;
    }

    /**
     * Maximum number of outstanding connections per host
     *
     * @param maxConnPerHost
     */
    public void setMaxConnectionsPerHost(int maxConnPerHost) {
        _maxConnPerHost = maxConnPerHost;
    }

    /**
     * Connection timeout
     *
     * @param connectionTimeoutMs
     */
    public void setConnectionTimeoutMs(int connectionTimeoutMs) {
        _connTimeout = connectionTimeoutMs;
    }

    /**
     * Socket connection timeout
     *
     * @param connectionTimeoutMs
     */
    public void setSocketConnectionTimeoutMs(int connectionTimeoutMs) {
        _socketConnTimeout = connectionTimeoutMs;
    }

    /**
     * @param connManagerTimeout the connManagerTimeout to set
     */
    public void setConnManagerTimeout(int connManagerTimeout) {
        _connManagerTimeout = connManagerTimeout;
    }

    /**
     * Initialize HTTP client
     */
    public void init() {
        _log.info("NimbleStorageDriver:NimbleStorageAPIFactory init enter");
        _clientMap = new ConcurrentHashMap<String, NimbleStorageAPI>();

        HttpConnectionManagerParams params = new HttpConnectionManagerParams();
        params.setDefaultMaxConnectionsPerHost(_maxConnPerHost);
        params.setMaxTotalConnections(_maxConn);
        params.setTcpNoDelay(true);
        params.setConnectionTimeout(_connTimeout);
        params.setSoTimeout(_socketConnTimeout);

        _connectionManager = new MultiThreadedHttpConnectionManager();
        _connectionManager.setParams(params);
        _connectionManager.closeIdleConnections(0);  // close idle connections immediately

        HttpClient client = new HttpClient(_connectionManager);
        client.getParams().setConnectionManagerTimeout(_connManagerTimeout);
        client.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new HttpMethodRetryHandler() {
            @Override
            public boolean retryMethod(HttpMethod httpMethod, IOException e, int i) {
                return false;
            }
        });

        Protocol.registerProtocol("https", new Protocol("https", new NonValidatingSocketFactory(), 8080));
    }

    /**
     * shutdown http connection manager.
     */
    protected void shutdown() {
        _connectionManager.shutdown();
    }
    
    
    public ClientConfig configureClient() throws NoSuchAlgorithmException,
    KeyManagementException {

    	TrustManager[] certs = new TrustManager[] { new X509TrustManager() {
    		@Override
    		public X509Certificate[] getAcceptedIssuers() {
    			return null;
    		}

    		@Override
    		public void checkServerTrusted(X509Certificate[] chain,
    				String authType) throws CertificateException {
    		}

    		@Override
    		public void checkClientTrusted(X509Certificate[] chain,
    				String authType) throws CertificateException {
    		}
    	} };
    	SSLContext ctx = null;
    	try {
    		ctx = SSLContext.getInstance("TLS");
    		ctx.init(null, certs, new SecureRandom());
    	} catch (java.security.GeneralSecurityException ex) {
    	}
    	HttpsURLConnection.setDefaultSSLSocketFactory(ctx.getSocketFactory());

    	ClientConfig config = new DefaultClientConfig();
    	try {
    		config.getProperties().put(
    				HTTPSProperties.PROPERTY_HTTPS_PROPERTIES,
    				new HTTPSProperties(new HostnameVerifier() {
    					@Override
    					public boolean verify(String hostname,
    							SSLSession session) {
    						return true;
    					}
    				}, ctx));
    	} catch (Exception e) {
    	}
    	config.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
    	return config;
    }

    /**
     * Create NimbleStorage API client
     * 
     * @param endpoint
     * @param username
     * @param password
     * @return api client
     * @throws NimbleStorageException 
     */
    public NimbleStorageAPI getRESTClient(URI endpoint, String username, String password) throws NimbleStorageException {
        try {
            _log.info("NimbleStorageDriver:getRESTClient");
            // key=uri+user+pass to make unique, value=NimbleStorageAPI object
            NimbleStorageAPI nimbleStorageAPI = _clientMap.get(endpoint.toString() + ":" + username + ":" + password);
            if (nimbleStorageAPI == null) {
                _log.info("NimbleStorageDriver:getRESTClient1 nimbleStorageAPI null");
               
                ClientHandler handler = new URLConnectionClientHandler();
                Client connClient = new Client(handler,configureClient());
                RESTClient restClient = new RESTClient(connClient);
                nimbleStorageAPI = new NimbleStorageAPI(endpoint, restClient, username, password);
                _clientMap.putIfAbsent(endpoint.toString() + ":" + username + ":" + password, nimbleStorageAPI);
            }
            return nimbleStorageAPI;
        } catch (Exception e) {
            _log.error("NimbleStorageDriver:getRESTClient Error in getting RESTclient");
            e.printStackTrace();
            throw new NimbleStorageException(e.toString());
        }
    }

	// Sample direct program
    public static void main(String[] args) {
        System.out.println("starting NimbleStorage main");
        try {
        	URI uri = URI.create(String.format("https://<ip_address>:<port>/api/1.8/auth/apitoken"));
        	NimbleStorageAPIFactory factory = new NimbleStorageAPIFactory();
        	factory.setConnectionTimeoutMs(30000*4);
        	factory.setConnManagerTimeout(60000*4);
        	factory.setSocketConnectionTimeoutMs(7200000*4);
        	BasicConfigurator.configure();
        	factory.init();
        	NimbleStorageAPI nimbleStorageAPI = factory.getRESTClient(uri, "<username>", "<password>");

        	String authToken = nimbleStorageAPI.getAuthToken();
        	System.out.println(authToken);
    	} catch (Exception e) {
            System.out.println("EROR");
            System.out.println(e);
            System.out.println(CompleteError.getStackTrace(e));
            e.printStackTrace();
        }
	} //end main
}