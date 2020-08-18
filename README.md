# Distributed counter

Sample REST micro-service for counting events.
Implementation uses Hazelcast in embedded mode to keep the counts in cluster scope: [`HazelcastCounter`](distributedcounter-service/src/main/java/org/berk/distributedcounter/counter/HazelcastCounter.java)

It uses Hazelcast executors to change counts, without needing a lock for synchronization in the cluster: [`HazelcastIncrementer`](distributedcounter-service/src/main/java/org/berk/distributedcounter/counter/HazelcastIncrementer.java)

An extended implementation is provided to improve performance by counting using `AtomicLong` locally on each node and syncing them with Hazelcast in intervals, handling synchronization without locking:[`PeriodicDistributingCounter`](distributedcounter-service/src/main/java/org/berk/distributedcounter/counter/HazelcastCounter.java) 

The [`Counter`](distributedcounter-service/src/main/java/org/berk/distributedcounter/counter/Counter.java) implementation can be chooses by defining it in [`Preferences`](distributedcounter-service/src/main/java/org/berk/distributedcounter/Preferences.java) 

A client has been provided using Apache Http Client, in [distributedcounter-client](distributedcounter-client) module: [`CounterApacheClient`](distributedcounter-client/src/main/java/org/berk/distributedcounter/client/CounterApacheClient.java)

Module [distributedcounter-api](distributedcounter-api) can be used to implement and alternative client; it provides the interface as well: [`CounterClient`](distributedcounter-api/src/main/java/org/berk/distributedcounter/client/CounterClient.java)   

Project [distributedcounter-integrationtest](distributedcounter-integrationtest) Is a separate project, testing the service externally by using the client:  [`CounterClient`](distributedcounter-api/src/main/java/org/berk/distributedcounter/client/CounterClient.java)

**I have implemented this using Jersey, to play with  it. From this version onward I will move the implementation to Spring Boot.**   

## Prerequisites

  * Maven 3
  * JDK 11
  
## Quick start

  * Externalized configuration and service discovery not implemented on this version. To configure nodes, edit [`Preferences`](distributedcounter-service/src/main/java/org/berk/distributedcounter/Preferences.java)
    
    If you skip this step, service will run using port 8080 for http, 950x for Hazelcast.
        
  * Build using `mvn package`

  * You may distribute fat jar `distributedcounter-service/target/distributedcounter-service-1.0-SNAPSHOT.jar` to other nodes    
  
  * Run the service
    ```
    $ java -jar service/target/service-1.0-SNAPSHOT.jar
    ```
      
  * Or create the a Docker image and run it in a container:
    ```
    $ docker build -t counter:jersey distributedcounter-service/
    $ docker run --name counter --rm -p 8080:8080 counter:jersey
    ```

## Test the service  
  #### Increment / add a counter named 'event1':
  ```
  $ curl -w "\n" -X PUT http://localhost:8080/counter/count/event1
  $ _
  ```
  #### Get the count of a counter:
  ```
  $ curl -w "\n" http://localhost:8080/counter/count/event1
  {"id":"event1","count":6}
  $ _
  ```
  #### Reset / remove counter:
  ```
  $ curl -w "\n" -X DELETE http://localhost:8080/counter/count/event1
  $ _
  ```
  #### List counters:
  ```
  $ curl -w "\n" http://localhost:8080/counter/list
  [{"id":"Event4","count":1},{"id":"Test Id","count":1},{"id":"Event3","count":1},{"id":"Event1","count":6}]
  $ _
  ```
  Or with pagination arguments:
  ```
  $ curl -w "\n" http://localhost:8080/counter/list?from_index=50&item_count=50
  [{"id":"Event4","count":1},{"id":"Test Id","count":1},{"id":"Event3","count":1},{"id":"Event1","count":6}]
  $ _
  ```
  
## Using the client
  
  Module **distributedcounter-client** provides a Java client implementation using Apache Http Client
  
  Sample usage of the client and some performance tests are in [`DistributedCounterClientTest`](distributedcounter-integrationtest/src/test/java/org/berk/distributedcounter/client/DistributedCounterClientTest.java)


## Author

* **Berk Köprücü** [bkoprucu](https://github.com/bkoprucu) - [https://www.linkedin.com/in/koprucu](https://www.linkedin.com/in/koprucu/)

