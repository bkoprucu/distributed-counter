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
  To build:
  * Maven 3
  * JDK 11
  
  To run containers
  * Docker
  * Kubernetes
    
## Configuring and running

  * Externalized configuration and service discovery not implemented on this version. To configure nodes, edit [`Preferences`](distributedcounter-service/src/main/java/org/berk/distributedcounter/Preferences.java)
    
    If you skip this step, service will run using port 8080 for http, 950x for Hazelcast.
        
  * Build using `mvn clean install`
  
  ##### Running the service locally:
  ```
  $ java -jar distributedcounter-service/target/distributedcounter-service-1.0-SNAPSHOT.jar
  ```

  ##### Running a cluster of (unmanaged) Docker containers
    
  Create a Docker bridge network to enable container to form a cluster:
  ```
  $ docker network create distributedcounter
  ```
  Run container instances, forming a cluster:
  ```
  $ docker run --rm --network distributedcounter --name counter1 -p 8080:8080 bkoprucu/distributedcounter:jersey-1.0
  $ docker run --rm --network distributedcounter --name counter2 -p 8081:8080 bkoprucu/distributedcounter:jersey-1.0
  ...
  ```
  
  ##### Running a cluster using Kubernetes
  Following will deploy a cluster of three pods, and a load balancer listening to port 8080:   
  ```
  $ kubectl apply -f DistributedCounter_kubernetes_deployment.yml
  ```
  Deployment progress can be monitored with:
  ```
  $ kubectl get pods -l app=distributedcounter -o wide --watch
  ```
  
## Usage  
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

<br/>
<br/>

_Author: Berk Köprücü [https://github.com/bkoprucu](https://github.com/bkoprucu)   -   [https://www.linkedin.com/in/koprucu](https://www.linkedin.com/in/koprucu/)_

