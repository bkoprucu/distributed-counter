package org.bashar.distributedcounter.client;

public class CounterClientException extends RuntimeException {
    public CounterClientException(String message) {
        super(message);
    }

    public CounterClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public CounterClientException(Throwable cause) {
        super(cause);
    }
}
