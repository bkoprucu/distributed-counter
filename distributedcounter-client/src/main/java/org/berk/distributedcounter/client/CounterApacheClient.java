package org.berk.distributedcounter.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.hc.core5.util.Timeout;
import org.berk.distributedcounter.api.Count;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.hc.core5.util.TextUtils.isBlank;


public class CounterApacheClient implements CounterClient, Closeable {

    private boolean sharedHttpClient;
    private final CloseableHttpClient httpClient;

    private final String baseUriStr;
    private final String countUriStr;

    private final ObjectMapper mapper;
    private final ObjectReader countReader;
    private final ObjectReader listReader;


    private static final Logger log = LoggerFactory.getLogger(CounterApacheClient.class);

    private CounterApacheClient(String host, int port, CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
        this.baseUriStr = String.format("http://%s:%d/counter/", host, port);
        this.countUriStr = baseUriStr + "count/";
        mapper = new ObjectMapper();
        countReader = mapper.readerFor(Count.class);
        listReader = mapper.readerFor(new TypeReference<List<Count<?>>>() {}); // we avoid generating TypeRef and reader
        log.info("Logger initialized for baseUri:{}", baseUriStr);
    }


    /** Create a client with pool and connection parameters. This will create the pool and ClosableHttpClient instance  */
    public static CounterApacheClient newClient(String host, int port, int connectTimeout, int requestTimeout, int threadPoolSize) {
        CounterApacheClient distributedCounterClient = newClient(host, port, httpClient(connectTimeout, requestTimeout, threadPoolSize));
        distributedCounterClient.sharedHttpClient = false;
        return distributedCounterClient;
    }

    /** Create a client using a already existing HttpClient instance */
    public static CounterApacheClient newClient(String host, int port, CloseableHttpClient httpClient) {
        Objects.requireNonNull(httpClient);
        CounterApacheClient distributedCounterClient = new CounterApacheClient(host, port, httpClient);
        distributedCounterClient.sharedHttpClient = true;
        return distributedCounterClient;
    }


    private static CloseableHttpClient httpClient(int connectTimeout, int requestTimeout, int threadPoolSize)  {
        log.info("Creating HttpClient and thread pool");
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


    private URI buildCountURI(String counterId, Long amount) {
        if(isBlank(counterId)) {
            throw new IllegalArgumentException("counterId is mandatory");
        }
        try {
            URIBuilder builder = new URIBuilder(countUriStr +  URLEncoder.encode(counterId, StandardCharsets.UTF_8));
            if(amount != null) {
                builder.setParameter("amount", amount.toString());
            }
            return builder.build();
        } catch (URISyntaxException e) {
            throw new CounterClientException(e);
        }
    }


    private URI buildURI(String path, Map<String, ?> queryParams) {
        try {
            URIBuilder builder = new URIBuilder(baseUriStr + URLEncoder.encode(path, StandardCharsets.UTF_8));
            if(queryParams != null && !queryParams.isEmpty()) {
                builder.setParameters(queryParams.entrySet().stream()
                        .map(entry -> new BasicNameValuePair(entry.getKey(), entry.getValue().toString())).collect(Collectors.toList()));
            }
            return builder.build();
        } catch (URISyntaxException e) {
            throw new CounterClientException(e);
        }
    }


    @Override
    public long getCount(String counterId) {
        try (CloseableHttpResponse response = httpClient.execute(new HttpGet(buildCountURI(counterId, null)))) {
            if (response.getCode() != HttpStatus.SC_OK) {
                throw new CounterClientException(response.getReasonPhrase());
            }
            try (InputStream contentStream = response.getEntity().getContent()) {
                Count<?> count = countReader.readValue(contentStream);
                return count.getCountVal();
            }
        } catch (IOException ex) {
            throw new CounterClientException(ex);
        }
    }


    @Override
    public void increment(String counterId)  {
        increment(counterId, null);
    }


    @Override
    public void increment(String counterId, Long amount)  {
        try (CloseableHttpResponse response = httpClient.execute(new HttpPut(buildCountURI(counterId, amount)))) {
            if (response.getCode() != HttpStatus.SC_OK) {
                throw new CounterClientException(response.getReasonPhrase());
            }
        } catch (IOException ex) {
            throw new CounterClientException(ex);
        }
    }


    void remove(String counterId) {
        try (CloseableHttpResponse response = httpClient.execute(new HttpDelete(buildCountURI(counterId, null)))) {
            if (response.getCode() != HttpStatus.SC_OK) {
                throw new CounterClientException(response.getReasonPhrase());
            }
        } catch (IOException ex) {
            throw new CounterClientException(ex);
        }
    }


    @Override
    public List<Count<String>> getCounters(Integer fromIndex, Integer itemCount) {
        Map<String, Integer> queryParams = new HashMap<>();
        if(fromIndex != null) {
            queryParams.put("from_index", fromIndex);
        }
        if(itemCount != null) {
            queryParams.put("item_count", itemCount);
        }
        try (CloseableHttpResponse response = httpClient.execute(new HttpGet(buildURI("list", queryParams)))) {
            if (response.getCode() != HttpStatus.SC_OK) {
                throw new CounterClientException(response.getReasonPhrase());
            }
            return listReader.readValue(response.getEntity().getContent());
        } catch (IOException ex) {
            throw new CounterClientException(ex);
        }
    }


    public List<Count<String>> getCounters() {
        return getCounters(null , null);
    }


    @Override
    public int getListSize() {
        try (CloseableHttpResponse response = httpClient.execute(new HttpGet(buildURI("listsize", null)))) {
            if (response.getCode() != HttpStatus.SC_OK) {
                throw new CounterClientException(response.getReasonPhrase());
            }
            return mapper.readValue(response.getEntity().getContent(), Integer.class);
        } catch (IOException ex) {
            throw new CounterClientException(ex);
        }
    }


    /**
     * Closes only if not initialized by already existing CloseableHttpClient
     */
    @Override
    public void close() throws IOException {
        if(!sharedHttpClient && httpClient != null) {
            httpClient.close();
        }
    }

}
