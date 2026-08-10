# Spring Virtual Threads

![Spring Boot 4.1+](https://img.shields.io/badge/Spring%20Boot-4.1%2B-6DB33F?logo=springboot&logoColor=white)
![Java 25](https://img.shields.io/badge/Java-25-ED8B00?logo=openjdk&logoColor=white)

## The 30-Second Overview

This demo sends ten concurrent requests to a blocking Spring MVC endpoint. Tomcat is deliberately limited to one platform worker, so the batch takes roughly ten seconds without virtual threads. With one Spring Boot property, the same batch finishes in roughly one second because each waiting request can use a virtual thread.

## Quick Start

Prerequisites: Java 25 and [hey](https://github.com/rakyll/hey) (`brew install hey`).

1. Run the tests:

   ```bash
   ./mvnw test
   ```

2. Start the application:

   ```bash
   ./mvnw spring-boot:run
   ```

3. Send ten concurrent requests from another terminal:

   ```bash
   hey -n 10 -c 10 http://localhost:8080/benchmark | awk '/Total:/ {print $2 "s"}'
   ```

   With virtual threads enabled, the elapsed time should be about one second.

   ```text
   1.0069s
   ```

## Reproduce the Before-and-After

First remove or comment out this line in `application.properties`:

```properties
spring.threads.virtual.enabled=true
```

Restart the app and run the `hey` command. The single Tomcat platform worker processes ten one-second requests sequentially, so the elapsed time is about ten seconds.

Restore the property, restart, and run the identical command again. Spring Boot configures Tomcat with virtual threads, allowing all ten blocking requests to wait concurrently and complete in about one second.

```text
ONE PLATFORM THREAD  ████████████████████  10.05 s
VIRTUAL THREADS      ██                     1.01 s
```

Measured on Java 25 with Spring Boot 4.1, three runs per configuration.

## Code Highlight

```java
@GetMapping("/benchmark")
public BenchmarkResult benchmark() throws InterruptedException {
    Thread.sleep(1_000); // Simulates waiting on blocking I/O.

    Thread thread = Thread.currentThread();
    return new BenchmarkResult(
            "Blocking work complete",
            1_000,
            thread.getName(),
            thread.isVirtual());
}
```

The configuration deliberately constrains the platform-thread baseline:

```properties
server.tomcat.threads.max=1
server.tomcat.threads.min-spare=1
spring.threads.virtual.enabled=true
```

This is an educational blocking-I/O demonstration, not a general-purpose microbenchmark. Virtual threads improve concurrency for waiting work; they do not make CPU-bound work faster or remove downstream capacity limits.

## Video Reference

Watch the companion [YouTube Short](https://youtube.com/shorts/dPWbc1uO69c).
