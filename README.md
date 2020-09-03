# Distributed counter

Sample REST micro-service for counting events, implemented using Spring Boot 2 & Hazelcast

Hazelcast has been used in embedded mode: [`HazelcastCounter`](distributedcounter-service/src/main/java/org/berk/distributedcounter/counter/HazelcastCounter.java)

It uses Hazelcast executors to change counts, without needing a lock for synchronization in the cluster: [`HazelcastIncrementer`](distributedcounter-service/src/main/java/org/berk/distributedcounter/counter/HazelcastIncrementer.java)

An (alternative) extended implementation is provided to improve performance by counting using `AtomicLong` locally on each node and syncing them with Hazelcast by intervals, 
handling synchronization without locking:[`PeriodicDistributingCounter`](distributedcounter-service/src/main/java/org/berk/distributedcounter/counter/HazelcastCounter.java), 
which can be configured in [application.yml](distributedcounter-service/src/main/resources/application.yml)  

[Dockerfile](Dockerfile) and Kubernetes deployment and service configuration [provided](k8s/Kubernetes_deployment.yml)

A client has been provided using Apache Http Client, in [distributedcounter-client](distributedcounter-client) module: [`CounterApacheClient`](distributedcounter-client/src/main/java/org/berk/distributedcounter/client/CounterApacheClient.java)

Module [distributedcounter-api](distributedcounter-api) can be used to implement and alternative client; it provides the interface as well: [`CounterClient`](distributedcounter-api/src/main/java/org/berk/distributedcounter/client/CounterClient.java)   

Project [distributedcounter-integrationtest](distributedcounter-integrationtest) Is a separate project, testing the service externally by using the client:  [`CounterClient`](distributedcounter-api/src/main/java/org/berk/distributedcounter/client/CounterClient.java)

## Prerequisites
  To build:
  * JDK 11
  
  To run containers:
  * Docker
  * Kubernetes
    
## Configuring and running

  * Configure which [`Counter`](distributedcounter-service/src/main/java/org/berk/distributedcounter/counter/Counter.java) implementation to use by editing [application.yml](distributedcounter-service/src/main/resources/application.yml)
  * Build using `mvn clean install` _(use_ `mvnw` _if Maven 3 is not present)_
  
  #### Running locally:
  ```
  $ java -jar distributedcounter-service/target/distributedcounter-service-0.1.1-SNAPSHOT.jar
  ```
  #### Running a cluster of (unmanaged) Docker containers  
  Create a Docker bridge network to enable container to form a cluster:
  ```
  $ docker network create distributedcounter
  ```
  Run container instances, forming a cluster:
  ```
  $ docker run --rm --network distributedcounter --name counter1 -p 8080:8080 bkoprucu/distributedcounter:0.1.1
  $ docker run --rm --network distributedcounter --name counter2 -p 8081:8080 bkoprucu/distributedcounter:0.1.1
  ...
  ```
  #### Running a cluster using Kubernetes
  Deploy a cluster of three pods, and a load balancer listening to port 8080:   
  ```
  $ kubectl apply -f k8s/
  ```
  
## Usage  
  #### Increment / add a counter named 'event1':
  ```
  $ curl -i -X PUT http://localhost:8080/counter/count/event1
  HTTP/1.1 200 OK
  Date: Sat, 19 Aug 2020 21:08:51 GMT
  Host: distributedcounter-698954bf9d-7slj4
  Content-Length: 0
  Server: Jetty(9.4.31.v20200723)
  $ _
  ```
  **"Host"** header shows the current host (or Kubernetes pod name or Docker container Id), which can be used to verify that load balancer is functional
  
  Increment by 5:
  ```
  $ curl -X PUT http://localhost:8080/counter/count/event1?amount=5
  ```

  #### Get the count of a counter:
  ```
  $ curl -w "\n" http://localhost:8080/counter/count/event1
  {"id":"event1","count":6}
  ```
  #### Reset / remove counter:
  ```
  $ curl -w "\n" -X DELETE http://localhost:8080/counter/count/event1
  ```
  #### List counters:
  ```
  $ curl -w "\n" http://localhost:8080/counter/list
  [{"id":"Event4","count":1},{"id":"Test Id","count":1},{"id":"Event3","count":1},{"id":"Event1","count":6}]
  ```
  With pagination arguments:
  ```
  $ curl -w "\n" http://localhost:8080/counter/list?from_index=50&item_count=50
  [{"id":"Event4","count":1},{"id":"Test Id","count":1},{"id":"Event3","count":1},{"id":"Event1","count":6}]
  ```
  
## Using the client
  
  Module **distributedcounter-client** provides a Java client implementation using Apache Http Client
  
  Sample usage of the client and some performance tests are in [`DistributedCounterClientTest`](distributedcounter-integrationtest/src/test/java/org/berk/distributedcounter/client/DistributedCounterClientTest.java)

<br/>

_Author: Berk Köprücü [https://github.com/bkoprucu](https://github.com/bkoprucu)   -   [https://www.linkedin.com/in/koprucu](https://www.linkedin.com/in/koprucu/)_

