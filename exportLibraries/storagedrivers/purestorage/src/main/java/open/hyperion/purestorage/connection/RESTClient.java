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

package open.hyperion.purestorage.connection;

import java.net.URI;
import com.sun.jersey.api.client.*;
import javax.ws.rs.core.MediaType;

/*
 * REST communication with 3PAR storage device 
 */
public class RESTClient {
    private Client _client;

    /**
     * Constructor
     *
     * @param client Jersey client to use
     */
    public RESTClient(Client client) {
        _client = client;
    }

    public ClientResponse post_json(URI url, String body) {
        WebResource r = _client.resource(url);
        return r.header("Content-Type", "application/json")
                .post(ClientResponse.class, body);
    }

    public ClientResponse post_json(URI url, String authToken, String body) {
        WebResource r = _client.resource(url);
        return r.header("Content-Type", "application/json")
                .header("User-Agent","None")
                .post(ClientResponse.class, body);
    }
    
    public ClientResponse get_json(URI url, String authToken) {
        WebResource r = _client.resource(url);
        return r.header("Content-Type", "application/json")
                .header("User-Agent","None")
                .get(ClientResponse.class);
    }

    public ClientResponse put_json(URI url, String authToken, String body) {
        WebResource r = _client.resource(url);
        return r.header("Content-Type", "application/json")
                .header("User-Agent","None")
                .put(ClientResponse.class, body);
    }

    public ClientResponse delete_json(URI url, String authToken) {
        WebResource r = _client.resource(url);
        return r.header("Content-Type", "application/json")
                .header("User-Agent","None")
                .delete(ClientResponse.class);
    }

    /**
     * Close the client
     */
    public void close() {
        _client.destroy();
    }
}
