package org.bashar.distributedcounter.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.bashar.distributedcounter.api.EventCount;
import org.bashar.distributedcounter.api.EventId;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;


public class DistributedCounterApacheClient implements Closeable {

    private boolean sharedHttpClient;
    private final CloseableHttpClient httpClient;

    private final URI uri;
    private final URI incrementUri; // re-useing for more performance

    private final ObjectMapper mapper;
    private final ObjectReader countReader;
    private final ObjectReader listReader;
    private final ObjectWriter eventIdWriter;

    private static final String URI_FORMAT = "http://%s:%d/counter";

    private static final Header APPLICATION_JSON_CONTENT_TYPE_HEADER = new BasicHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());

    private DistributedCounterApacheClient(String host, int port, CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
        try {
            this.uri = new URI(String.format(URI_FORMAT, host, port));
            this.incrementUri = new URIBuilder(uri).setPath(uri.getPath() + "/increment").build();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
        mapper = new ObjectMapper();
        countReader = mapper.readerFor(EventCount.class);
        listReader = mapper.readerFor(new TypeReference<List<EventCount>>() {}); // we avoid generating TypeRef and reader
        eventIdWriter = mapper.writerFor(EventId.class);
    }

    public static DistributedCounterApacheClient newClient(String host, int port, int connectTimeout, int requestTimeout, int threadPoolSize) {
        DistributedCounterApacheClient distributedCounterClient = newClient(host, port, httpClient(connectTimeout, requestTimeout, threadPoolSize));
        distributedCounterClient.sharedHttpClient = false;
        return distributedCounterClient;
    }

    public static DistributedCounterApacheClient newClient(String host, int port, CloseableHttpClient httpClient) {
        Objects.requireNonNull(httpClient);
        DistributedCounterApacheClient distributedCounterClient = new DistributedCounterApacheClient(host, port, httpClient);
        distributedCounterClient.sharedHttpClient = true;
        return distributedCounterClient;
    }

    private static CloseableHttpClient httpClient(int connectTimeout, int requestTimeout, int threadPoolSize)  {
        PoolingHttpClientConnectionManager connManager
                = new PoolingHttpClientConnectionManager();
        connManager.setMaxTotal(threadPoolSize);
        connManager.setDefaultMaxPerRoute(threadPoolSize);
        return HttpClients.custom()
                .setConnectionManager(connManager)
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectTimeout(connectTimeout)
                        .setConnectionRequestTimeout(requestTimeout)
                        .build())
                .disableRedirectHandling()
                .build();
    }


    /**
     * Get count of event identified with eventId
     */
    public long getCount(String eventId) {
        if(isBlank(eventId)) {
            throw new IllegalArgumentException("eventId is mandatory");
        }
        HttpGet request = null;
        try {
            request = new HttpGet(
                    new URIBuilder(uri).setPath(uri.getPath().concat("/count")).addParameter("event_id", eventId).build());
            CloseableHttpResponse response = httpClient.execute(request);
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
                throw new CounterClientException(statusLine.getReasonPhrase());
            }
            EventCount eventCount = countReader.readValue(response.getEntity().getContent());
            return eventCount.getCount();
        } catch (IOException | URISyntaxException ex) {
            throw new CounterClientException(ex);
        } finally {
            if(request != null) {
                request.releaseConnection();
            }
        }
    }




    /**
     * Increment counter identified by eventId by one
     */
    public void increment(String eventId)  {
        if (isBlank(eventId)) {
            throw new IllegalArgumentException("eventId is mandatory");
        }
        final HttpPut request = new HttpPut(incrementUri);
        try {
            request.setHeader(APPLICATION_JSON_CONTENT_TYPE_HEADER);
            request.setEntity(new ByteArrayEntity(eventIdWriter.writeValueAsBytes(new EventId(eventId))));
            final StatusLine statusLine = httpClient.execute(request).getStatusLine();
            if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
                throw new CounterClientException(statusLine.getReasonPhrase());

            }
        } catch (IOException ex) {
            throw new CounterClientException(ex);
        } finally {
            request.releaseConnection();
        }
    }


    /**
     * List all the event counters from index 'from' until index 'to'
     * @param from Optional. Considered first element if omitted
     * @param to Optional, if omittted all the items beginning from 'from' will be listed
     * @return List of #EventCount objects indicating the Event ids with their counts
     */


    public List<EventCount> getCounters(Integer from,
                                        Integer to) {
        HttpGet request = null;
        try {
            URIBuilder uriBuilder = new URIBuilder(uri).setPath(uri.getPath().concat("/counters"));
            if(from != null) {
                uriBuilder.addParameter("from", from.toString());
            }
            if(to != null) {
                uriBuilder.addParameter("to", to.toString());
            }
            request = new HttpGet(uriBuilder.build());
            CloseableHttpResponse response = httpClient.execute(request);
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
                throw new CounterClientException(statusLine.getReasonPhrase());
            }
            return listReader.readValue(response.getEntity().getContent());
        } catch (IOException | URISyntaxException ex) {
            throw new CounterClientException(ex);
        } finally {
            if(request != null)
                request.releaseConnection();
        }
    }

    /**
     * @return All counters
     * */
    public List<EventCount> getCounters() {
        return getCounters(null , null);
    }

    /**
     * Get number of counters
     */
    public int getListSize() {
        HttpGet request = null;
        try {
            request = new HttpGet(new URIBuilder(uri).setPath(uri.getPath().concat("/listsize")).build());
            CloseableHttpResponse response = httpClient.execute(request);
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
                throw new CounterClientException(statusLine.getReasonPhrase());
            }
            return mapper.readValue(response.getEntity().getContent(), Integer.class);
        } catch (IOException | URISyntaxException ex) {
            throw new CounterClientException(ex);
        } finally {
            if (request != null)
                request.releaseConnection();
        }
    }


    private static boolean isBlank(String input) {
        return input == null || input.length() == 0;
    }

    /**
     * Closes only if not initialized by already existing CloseableHttpClient
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        if(!sharedHttpClient && httpClient!=null) {
            httpClient.close();
        }
    }

}
