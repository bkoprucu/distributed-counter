package org.bashar.distributedcounter.client;

import com.sun.istack.internal.Nullable;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.bashar.distributedcounter.api.EventCount;
import org.eclipse.jetty.util.StringUtil;
import org.glassfish.jersey.apache.connector.ApacheClientProperties;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.JerseyClient;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static org.eclipse.jetty.util.StringUtil.isBlank;

public class DistributedCounterClient {

    private static final Logger log = LoggerFactory.getLogger(DistributedCounterClient.class);

    private static final String URI_FORMAT = "http://%s:%d/counter";

    private final JerseyClient client;

    private final URI uri;


    //TODO error handling, refactoring

    private static final GenericType<EventCount<String>> EVENT_COUNT_STRING_TYPE = new GenericType<EventCount<String>>(){};
    private static final GenericType<List<EventCount<String>>> LIST_OF_EVENT_COUNT_STRING_TYPE = new GenericType<List<EventCount<String>>>(){};


    public DistributedCounterClient(String host, int port, long connectTimeoutMs, long readTimeoutMs, int threadPoolSize) throws URISyntaxException {
        this(String.format(URI_FORMAT, host, port), connectTimeoutMs, readTimeoutMs, threadPoolSize);
    }


    public DistributedCounterClient(String uri, long connectTimeoutMs, long readTimeoutMs, int threadPoolSize) throws URISyntaxException {
        this.uri = new URI(uri);
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.property(ClientProperties.READ_TIMEOUT, readTimeoutMs);
        clientConfig.property(ClientProperties.CONNECT_TIMEOUT, connectTimeoutMs);
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(threadPoolSize);
        connectionManager.setDefaultMaxPerRoute(threadPoolSize);
        clientConfig.property(ApacheClientProperties.CONNECTION_MANAGER, connectionManager);
        clientConfig.connectorProvider(new ApacheConnectorProvider());
        this.client = JerseyClientBuilder.createClient(clientConfig);
    }

    /**
     * Gets count of event identified with eventId
     */
    public long getEventCount(String eventId) {
        if(isBlank(eventId)) {
            throw new IllegalArgumentException("eventId is mandatory");
        }
        return client.target(uri).path("/getcount")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(EVENT_COUNT_STRING_TYPE).getCount();
    }


    /**
     * Increment count of event evendId
     * @return Response.StatusType.getStatusCode == 200 if successfull
     *         Otherwise details about the failure
     *         TODO simplify this
     */
    public Response.StatusType increment(String eventId) {
        if(isBlank(eventId)) {
            throw new IllegalArgumentException("eventId is mandatory");
        }
        Response response = client.target(uri).path("/increment")
                .queryParam("event_id", eventId)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get();
        return response.getStatusInfo();
    }


    /**
     * List all the event counters from index 'from' until index 'to'
     * @param from Optional. Considered first element if omitted
     * @param to Optional, if omittted all the items beginning from 'from' will be listed
     * @return List of #EventCount objects indicating the Event ids with their counts
     */
    public List<EventCount<String>> list(@Nullable Integer from,
                                         @Nullable Integer to) {
        return client.target(uri).path("/list")
                .queryParam("from", from)
                .queryParam("to", to)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(LIST_OF_EVENT_COUNT_STRING_TYPE);
    }

    /**
     * @return Total number of events in the counter
     */
    public int listSize() {
        return client.target(uri).path("/listsize")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(Integer.class);
    }



}