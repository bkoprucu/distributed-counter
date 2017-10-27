package org.bashar.distributedcounter.client;

import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.bashar.distributedcounter.api.EventCount;
import org.bashar.distributedcounter.api.EventId;
import org.glassfish.jersey.apache.connector.ApacheClientProperties;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.JerseyClient;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.eclipse.jetty.util.StringUtil.isBlank;

//TODO fix bugs
public class DistributedCounterJerseyClient {
    private static final Logger log = LoggerFactory.getLogger(DistributedCounterJerseyClient.class);

    private static final String URI_FORMAT = "http://%s:%d/counter";
    private final JerseyClient client;
    private final URI uri;

    /** Default connect timeout in milliseconds */
    public static final long DEFAULT_CONNECT_TIMEOUT = 1000;

    /** Default request timeout in milliseconds */
    public static final long DEFAULT_READ_TIMEOUT = 10000;

    private static final GenericType<EventCount> EVENT_COUNT_STRING_TYPE = new GenericType<EventCount>(){};
    private static final GenericType<List<EventCount>> LIST_OF_EVENT_COUNT_STRING_TYPE = new GenericType<List<EventCount>>(){};



    public DistributedCounterJerseyClient(String host, int port, int threadPoolSize) {
        this(String.format(URI_FORMAT, host, port), DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT, threadPoolSize);
    }


    public DistributedCounterJerseyClient(String host, int port, long connectTimeoutMs, long readTimeoutMs, int threadPoolSize) {
        this(String.format(URI_FORMAT, host, port), connectTimeoutMs, readTimeoutMs, threadPoolSize);
    }


    public DistributedCounterJerseyClient(String uri, long connectTimeoutMs, long readTimeoutMs, int threadPoolSize)  {
        try {
            this.uri = new URI(uri);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
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
    public long getCount(String eventId) {
        if(isBlank(eventId)) {
            throw new IllegalArgumentException("eventId is mandatory");
        }
        return client.target(uri).path("/count")
                .queryParam("event_id", eventId)
                .request(APPLICATION_JSON_TYPE)
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
                .request(APPLICATION_JSON_TYPE)
                .put(Entity.json(new EventId(eventId)));
        return response.getStatusInfo();
    }


    /**
     * List all the event counters from index 'from' until index 'to'
     * @param from Optional. Considered first element if omitted
     * @param to Optional, if omittted all the items beginning from 'from' will be listed
     * @return List of #EventCount objects indicating the Event ids with their counts
     */
    public List<EventCount> list(Integer from,
                                 Integer to) {
        return client.target(uri).path("/counters")
                .queryParam("from", from)
                .queryParam("to", to)
                .request(APPLICATION_JSON_TYPE)
                .get(LIST_OF_EVENT_COUNT_STRING_TYPE);
    }

    /**
     * @return Total number of events in the counter
     */
    public int listSize() {
        return client.target(uri).path("/listsize")
                .request(APPLICATION_JSON_TYPE)
                .get(Integer.class);
    }



}