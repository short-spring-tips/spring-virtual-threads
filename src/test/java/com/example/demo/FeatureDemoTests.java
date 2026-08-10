package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FeatureDemoTests {

    @Autowired
    private Environment environment;

    @Test
    void enablesVirtualThreadsThroughConfiguration() {
        assertThat(environment.getProperty("spring.threads.virtual.enabled", Boolean.class))
                .isTrue();
    }

    @Test
    void limitsTomcatToOnePlatformWorkerThread() {
        assertThat(environment.getProperty("server.tomcat.threads.max", Integer.class))
                .isEqualTo(1);
        assertThat(environment.getProperty("server.tomcat.threads.min-spare", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void exposesOnlyTheBlockingBenchmarkEndpoint() throws Exception {
        int port = environment.getRequiredProperty("local.server.port", Integer.class);
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/benchmark"))
                .GET()
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"message\":\"Blocking work complete\"");
        assertThat(response.body()).contains("\"delayMillis\":1000");
        assertThat(response.body()).contains("\"virtual\":true");
        assertThat(response.body()).containsPattern("\\\"name\\\":\\\".+\\\"");

        HttpRequest removedEndpoint = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/thread"))
                .GET()
                .build();
        assertThat(HttpClient.newHttpClient()
                .send(removedEndpoint, HttpResponse.BodyHandlers.ofString())
                .statusCode()).isEqualTo(404);
    }

    @Test
    void virtualThreadsHandleConcurrentBlockingRequestsDespiteTheOneThreadLimit() {
        int port = environment.getRequiredProperty("local.server.port", Integer.class);
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/benchmark"))
                .GET()
                .build();
        HttpClient client = HttpClient.newHttpClient();

        long startedAt = System.nanoTime();
        List<CompletableFuture<HttpResponse<String>>> requests = List.of(
                client.sendAsync(request, HttpResponse.BodyHandlers.ofString()),
                client.sendAsync(request, HttpResponse.BodyHandlers.ofString()),
                client.sendAsync(request, HttpResponse.BodyHandlers.ofString()),
                client.sendAsync(request, HttpResponse.BodyHandlers.ofString()),
                client.sendAsync(request, HttpResponse.BodyHandlers.ofString()));
        CompletableFuture.allOf(requests.toArray(CompletableFuture[]::new)).join();
        Duration elapsed = Duration.ofNanos(System.nanoTime() - startedAt);

        assertThat(requests).allSatisfy(result -> assertThat(result.join().statusCode()).isEqualTo(200));
        assertThat(elapsed).isLessThan(Duration.ofSeconds(3));
    }
}
