package org.berk.distributedcounter.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.hc.core5.util.Timeout;
import org.berk.distributedcounter.api.EventCount;
import org.berk.distributedcounter.api.EventId;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;


public class DistributedCounterApacheClient implements Closeable {

    private boolean sharedHttpClient;
    private final CloseableHttpClient httpClient;

    private final URI uri;
    private final URI incrementUri; // re-using for performance

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
        PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
        connManager.setMaxTotal(threadPoolSize);
        connManager.setDefaultMaxPerRoute(threadPoolSize);
        return HttpClients.custom()
                .setConnectionManager(connManager)
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectTimeout(Timeout.ofMilliseconds(connectTimeout))
                        .setConnectionRequestTimeout(Timeout.ofMilliseconds(requestTimeout))
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

        HttpGet httpGet = new HttpGet(
                buildURI(List.of("count"), List.of(new BasicNameValuePair("event_id", eventId))));

        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            if (response.getCode() != HttpStatus.SC_OK) {
                throw new CounterClientException(response.getReasonPhrase());
            }
            try (InputStream contentStream = response.getEntity().getContent()) {
                EventCount eventCount = countReader.readValue(contentStream);
                return eventCount.getCount();
            }
        } catch (IOException ex) {
            throw new CounterClientException(ex);
        }
    }


    private URI buildURI(List<String> pathSegments, List<NameValuePair> nvps) {
        try {
            return new URIBuilder(uri).setPathSegments(pathSegments).setParameters(nvps).build();
        } catch (URISyntaxException e) {
            throw new CounterClientException(e);
        }

    }

    /**
     * Increment counter identified by eventId by one
     */
    public void increment(String eventId)  {
        if (isBlank(eventId)) {
            throw new IllegalArgumentException("eventId is mandatory");
        }
        HttpPut request = new HttpPut(incrementUri);
        request.setHeader(APPLICATION_JSON_CONTENT_TYPE_HEADER);
        CloseableHttpResponse response = null;
        try {
            request.setEntity(new ByteArrayEntity(eventIdWriter.writeValueAsBytes(new EventId(eventId)), ContentType.APPLICATION_JSON));
            response = httpClient.execute(request);
            if (response.getCode() != HttpStatus.SC_OK) {
                throw new CounterClientException(response.getReasonPhrase());
            }
        } catch (IOException ex) {
            throw new CounterClientException(ex);
        } finally {
            if(response != null) {
                try {
                    response.close();
                } catch (IOException ignore) {
                }
            }
        }
    }


    /**
     * List all the event counters from index 'from' until index 'to'
     * @param from Optional. Considered first element if omitted
     * @param to Optional, if omittted all the items beginning from 'from' will be listed
     * @return List of #EventCount objects indicating the Event ids with their counts
     */


    public List<EventCount> getCounters(Integer from, Integer to) {
        URI getcountersUri;
        try {
            URIBuilder uriBuilder = new URIBuilder().setPath(uri.getPath().concat("/counters"));
            if (from != null) {
                uriBuilder.addParameter("from", from.toString());
            }
            if (to != null) {
                uriBuilder.addParameter("to", to.toString());
            }
            getcountersUri = uriBuilder.build();
        } catch (URISyntaxException e) {
            throw  new CounterClientException(e);
        }
        try (CloseableHttpResponse response = httpClient.execute(new HttpGet(getcountersUri))) {
            if (response.getCode() != HttpStatus.SC_OK) {
                throw new CounterClientException(response.getReasonPhrase());
            }
            return listReader.readValue(response.getEntity().getContent());
        } catch (IOException ex) {
            throw new CounterClientException(ex);
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
        try (CloseableHttpResponse response = httpClient.execute(new HttpGet(buildURI(List.of("listsize"), Collections.emptyList())))) {
            if (response.getCode() != HttpStatus.SC_OK) {
                throw new CounterClientException(response.getReasonPhrase());
            }
            return mapper.readValue(response.getEntity().getContent(), Integer.class);
        } catch (IOException ex) {
            throw new CounterClientException(ex);
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
        if(!sharedHttpClient && httpClient != null) {
            httpClient.close();
        }
    }

}
