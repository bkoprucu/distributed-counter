package org.bashar.distributedcounter.client;

import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.glassfish.jersey.apache.connector.ApacheClientProperties;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.JerseyClient;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;

public class DistributedCounterClient {

    private static final Logger log = LoggerFactory.getLogger(DistributedCounterClient.class);

    private static final String URI_FORMAT = "http://%s:%d/counter";

    private final JerseyClient client;

    private final URI uri;


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

    //TODO implement





}