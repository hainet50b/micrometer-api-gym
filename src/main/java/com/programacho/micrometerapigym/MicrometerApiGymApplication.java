package com.programacho.micrometerapigym;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.TimeGauge;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class MicrometerApiGymApplication {

    public static void main(String[] args) {
        // MeterRegistry - Timer
        timer();

        // MeterRegistry - Counter
        counter();

        // MeterRegistry - FunctionTrackingCounter
        functionTrackingCounter();

        // MeterRegistry - Gauge
        gauge();

        // MeterRegistry - TimeGauge
        timeGauge();

        // MeterRegistry - MultiGauge
        multiGauge();

        // Another MeterRegistry in a thread.
        anotherMeterRegistry();
    }

    private static void timer() {
        System.out.println("MeterRegistry - Timer");

        MeterRegistry registry = new SimpleMeterRegistry();

        registry.timer("programacho.timer").record(() -> sleep(1));

        System.out.println(registry.find("programacho.timer").timer().totalTime(TimeUnit.SECONDS));
    }

    private static void counter() {
        System.out.println("MeterRegistry - Counter");

        MeterRegistry registry = new SimpleMeterRegistry();

        Counter counter = registry.counter("programacho.counter");
        counter.increment();
        counter.increment(5);

        System.out.println(registry.find("programacho.counter").counter().count());
    }

    private static void functionTrackingCounter() {
        System.out.println("MeterRegistry - FunctionTrackingCounter");

        MeterRegistry registry = new SimpleMeterRegistry();

        Cache<Object, Object> cache = Caffeine.newBuilder().maximumSize(1).recordStats().build();
        registry.more().counter(
                "programacho.function-tracking-counter",
                Collections.emptyList(),
                cache,
                c -> c.stats().requestCount()
        );

        for (int i = 1; i <= 100; i++) {
            cache.getIfPresent("key");
        }

        System.out.println(registry.find("programacho.function-tracking-counter").functionCounter().count());
    }

    private static void gauge() {
        System.out.println("MeterRegistry - Gauge");

        MeterRegistry registry = new SimpleMeterRegistry();

        AtomicInteger speed = new AtomicInteger(0);
        registry.gauge("programacho.gauge", speed);

        speed.set(40);
        System.out.println(registry.find("programacho.gauge").gauge().value());

        speed.set(100);
        System.out.println(registry.find("programacho.gauge").gauge().value());
    }

    private static void timeGauge() {
        System.out.println("MeterRegistry - TimeGauge");

        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        AtomicInteger duration = new AtomicInteger(300);
        TimeGauge.builder("programacho.time-gauge", () -> duration, TimeUnit.SECONDS).register(registry);
        TimeGauge.builder("programacho.other.time-gauge", () -> duration, TimeUnit.MILLISECONDS).register(registry);

        System.out.println(registry.find("programacho.time-gauge").timeGauge().value());
        System.out.println(registry.find("programacho.other.time-gauge").timeGauge().value());
    }

    private static void multiGauge() {
        System.out.println("MeterRegistry - MultiGauge");

        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        List<Map<String, Object>> resultSet = List.of(
                Map.of("user", "foo", "value", 1),
                Map.of("user", "bar", "value", 2),
                Map.of("user", "baz", "value", 3)
        );

        MultiGauge values = MultiGauge.builder("programacho.multi-gauge").register(registry);
        values.register(resultSet.stream()
                .map(it -> MultiGauge.Row.of(Tags.of("user", (String) it.get("user")), (Integer) it.get("value")))
                .collect(Collectors.toList())
        );

        System.out.println("Size: " + registry.find("programacho.multi-gauge").gauges().size());
        System.out.println("foo: " + registry.find("programacho.multi-gauge").tag("user", "foo").gauge().value());
        System.out.println("bar: " + registry.find("programacho.multi-gauge").tag("user", "bar").gauge().value());
        System.out.println("baz: " + registry.find("programacho.multi-gauge").tag("user", "baz").gauge().value());
    }

    private static void tag() {
        // TODO
    }

    private static void anotherMeterRegistry() {
        System.out.println("Another MeterRegistry in a thread.");

        MeterRegistry registry = new SimpleMeterRegistry();

        System.out.println(Timer.builder("programacho.timer").register(registry).totalTime(TimeUnit.SECONDS));
        System.out.println(Counter.builder("programacho.counter").register(registry).count());
    }

    private static void multiThread() {
        // TODO
    }

    private static void sleep(int timeout) {
        try {
            TimeUnit.SECONDS.sleep(timeout);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
