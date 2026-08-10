package com.example.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FeatureDemo {

    private static final long DELAY_MILLIS = 1_000;

    @GetMapping("/benchmark")
    public BenchmarkResult benchmark() throws InterruptedException {
        Thread.sleep(DELAY_MILLIS); // Simulates waiting on blocking I/O.

        Thread currentThread = Thread.currentThread();
        return new BenchmarkResult(
                "Blocking work complete",
                DELAY_MILLIS,
                currentThread.getName(),
                currentThread.isVirtual());
    }

    public record BenchmarkResult(
            String message,
            long delayMillis,
            String name,
            boolean virtual) {
    }
}
