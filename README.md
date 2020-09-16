# Distributed counter

Sample REST micro-service for counting events. Demonstrates usage of following technologies:
 - Java 11
 - Spring Boot 2 in reactive mode
 - Hazelcast in embedded mode
 - [Hazelcast cluster discovery using Kubernetes or multicast](distributedcounter-service/src/main/java/org/berk/distributedcounter/SpringConfig.java), depending the environment and selected Spring-Boot profile
 - [Docker](distributedcounter-service/Dockerfile), using multiple layers
 - Kubernetes and spring-cloud-kubernetes for linking ConfigMaps with Spring @ConfigurationProperties
 - Skaffold for local Kubernetes development environment
 
Counter implementation [`HazelcastCounter`](distributedcounter-service/src/main/java/org/berk/distributedcounter/counter/HazelcastCounter.java) utilizes Hazelcast executors for updating counter values, avoiding locks in cluster scope, for better performance: [`HazelcastIncrementer`](distributedcounter-service/src/main/java/org/berk/distributedcounter/counter/HazelcastIncrementer.java)

An alternative counter; [`LocalCachingHazelcastCounter`](distributedcounter-service/src/main/java/org/berk/distributedcounter/counter/LocalCachingHazelcastCounter.java) updates values using `AtomicLong` locally on each node, and syncs them with Hazelcast cluster periodically, without using locks. This demonstrates an idea of further improving the performance.

REST api is placed in a separate module "[distributedcounter-api](distributedcounter-api)"   

Integration tests are in a separate project: [distributedcounter-integrationtest](distributedcounter-integrationtest)

    
## Configuring and running

  * Build using `mvn clean install` _(use_ `mvnw` _if Maven 3 is not present)_. Since this ia a multi module project, `distributedcounter-api` module needs to be installed to be able to build `distributedcounter-service` module separately.
  
  #### Running on a Kubernetes managed cluster
  Default configuration will deploy a cluster of three pods, and a load balancer listening on 8080:   
  ```
  $ kubectl apply -f k8s/
  ```
  Update [ConfigMap](distributedcounter-service/k8s/configmap.yml) to change service parameters
  
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
## Notes
  Although service has been implemented using spring-webflux, this is primarily for demonstration purposes, since embedded Hazelcast won't be delaying the threads much, 
  and if it is running out of resources, the whole service is about to run out of resources as well. 

<br/>

_Author: Berk Köprücü [https://github.com/bkoprucu](https://github.com/bkoprucu)   -   [https://www.linkedin.com/in/koprucu](https://www.linkedin.com/in/koprucu/)_
