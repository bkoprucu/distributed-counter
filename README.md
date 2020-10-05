# Distributed counter

Sample REST micro-service for counting events. Demonstrates usage of following technologies:
 - Java 11
 - Spring Boot 2 & Spring Webflux (reactive)
 - Hazelcast in embedded mode, Hazelcast executors and async operations
 - [Hazelcast cluster discovery using Kubernetes or multicast](distributedcounter-service/src/main/java/org/berk/distributedcounter/SpringConfig.java), depending the environment and selected Spring-Boot profile
 - [Docker](distributedcounter-service/Dockerfile), using multiple layers
 - Kubernetes and spring-cloud-kubernetes for linking ConfigMaps with Spring @ConfigurationProperties
 - Skaffold for local Kubernetes development environment
 
Counter implementation [`HazelcastCounter`](distributedcounter-service/src/main/java/org/berk/distributedcounter/counter/HazelcastCounter.java) utilizes Hazelcast executors for updating counter values, avoiding locks in cluster scope, for better performance: [`HazelcastIncrementer`](distributedcounter-service/src/main/java/org/berk/distributedcounter/counter/HazelcastIncrementer.java)

An alternative counter; [`LocalCachingHazelcastCounter`](distributedcounter-service/src/main/java/org/berk/distributedcounter/counter/LocalCachingHazelcastCounter.java) updates values using `AtomicLong` locally on each node, and syncs them with Hazelcast cluster periodically, without using locks. This demonstrates an idea of further improving the performance.

REST api is placed in a separate module "[distributedcounter-api](distributedcounter-api)"   

Sample command line app, demonstrating usage of the client, with load/performance testing capabilities: [distributedcounter-testapp](distributedcounter-testapp)

    
## Configuring and running

  * Build using `mvn clean install` _(use_ `mvnw` _if Maven 3 is not present)_. Since this ia a multi module project, `distributedcounter-api` module needs to be installed to be able to build `distributedcounter-service` module separately.
  
  #### Running on a Kubernetes:
  Default configuration will deploy a cluster of two pods, and exposing the service on port 80:   
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
  
## API  
  #### Increment / add a counter named 'event1':
  ```
  $ curl -i -X PUT http://localhost/counter/count/event1
  HTTP/1.1 201 Created
  Host: distributedcounter-698954bf9d-7slj4
  Content-Length: 0
  $ _
  ```
  **"Host"** header shows the current host (or Kubernetes pod name or Docker container Id)
 
  Response code will be `HTTP 201` if the counter has been created, `HTTP 200` if an existing counter has been incremented
  
  The amount, by which the counter will be incremented can be passed as a query attribute:
  ```
  $ curl -X PUT http://localhost/counter/count/event1?amount=5
  ```

  #### Get the count of a counter:
  ```
  $ curl -w "\n" -i http://localhost/counter/count/event1
  HTTP/1.1 200 OK
  Content-Type: application/json
  4
  ```
  #### Reset / remove counter:
  ```
  $ curl -w "\n" -X DELETE http://localhos/counter/count/event1
  HTTP/1.1 200 OK
  Content-Type: application/json
  4
  ```
  Returns `HTTP 200` for successful removal, `HTTP 204` if the counter didn't exist
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

<br/>

Berk Köprücü [https://github.com/bkoprucu](https://github.com/bkoprucu)   -   [https://www.linkedin.com/in/koprucu](https://www.linkedin.com/in/koprucu/)_
