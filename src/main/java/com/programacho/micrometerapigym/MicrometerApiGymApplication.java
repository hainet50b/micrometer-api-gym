package com.programacho.micrometerapigym;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.TimeGauge;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class MicrometerApiGymApplication {

    public static void main(String[] args) {
        // MeterRegistry - Timer
        timer();

        // MeterRegistry - Timer.Sample
        timerSample();

        // MeterRegistry - FunctionTrackingTimer
        functionTrackingTimer();

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

        // MeterRegistry - DistributionSummary
        distributionSummary();

        // Tag
        tag();

        // Another MeterRegistry in a thread.
        anotherMeterRegistry();
    }

    private static void timer() {
        System.out.println("MeterRegistry - Timer");

        MeterRegistry registry = new SimpleMeterRegistry();

        Timer timer = registry.timer("programacho.timer");
        timer.record(() -> sleep(100));
        timer.record(() -> sleep(200));
        timer.record(() -> sleep(300));

        System.out.println("Count: " + registry.find("programacho.timer").timer().count());
        System.out.println("Max: " + registry.find("programacho.timer").timer().max(TimeUnit.SECONDS));
        System.out.println("Average: " + registry.find("programacho.timer").timer().mean(TimeUnit.SECONDS));
        System.out.println("Sum: " + registry.find("programacho.timer").timer().totalTime(TimeUnit.SECONDS));
    }

    private static void timerSample() {
        System.out.println("MeterRegistry - Timer.Sample");

        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        Timer.Sample sample = Timer.start(registry);
        sleep(1_000);
        sample.stop(registry.timer("programacho.timer"));

        System.out.println(registry.find("programacho.timer").timer().totalTime(TimeUnit.SECONDS));
    }

    private static void functionTrackingTimer() {
        System.out.println("MeterRegistry - FunctionTrackingTimer");

        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        Cache<String, Object> cache = Caffeine.newBuilder().recordStats().build();
        registry.more().timer(
                "programacho.timer",
                Collections.emptyList(),
                cache,
                c -> c.stats().loadCount(),
                c -> c.stats().totalLoadTime(),
                TimeUnit.NANOSECONDS
        );

        for (int i = 1; i <= 100; i++) {
            cache.get(UUID.randomUUID().toString(), k -> k);
        }

        System.out.println(registry.find("programacho.timer").functionTimer().count());
        System.out.println(registry.find("programacho.timer").functionTimer().totalTime(TimeUnit.SECONDS));
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

        Cache<String, Object> cache = Caffeine.newBuilder().maximumSize(1).recordStats().build();
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

    private static void distributionSummary() {
        System.out.println("MeterRegistry - DistributionSummary");

        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        DistributionSummary summary = registry.summary("programacho.summary");
        summary.record(1);
        summary.record(2);
        summary.record(3);

        System.out.println("Count: " + registry.find("programacho.summary").summary().count());
        System.out.println("Max: " + registry.find("programacho.summary").summary().max());
        System.out.println("Average: " + registry.find("programacho.summary").summary().mean());
        System.out.println("Sum: " + registry.find("programacho.summary").summary().totalAmount());
    }

    private static void tag() {
        System.out.println("Tag");

        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        Counter foo = registry.counter("programacho.counter", "user", "foo");
        foo.increment(10);

        Counter bar = registry.counter("programacho.counter", "user", "bar");
        bar.increment(20);

        Counter baz = registry.counter("programacho.counter", "user", "baz");
        baz.increment(30);

        System.out.println("No tag: " + registry.find("programacho.counter").counter().count());
        System.out.println("foo: " + registry.find("programacho.counter").tag("user", "foo").counter().count());
        System.out.println("bar: " + registry.find("programacho.counter").tag("user", "bar").counter().count());
        System.out.println("baz: " + registry.find("programacho.counter").tag("user", "baz").counter().count());
    }

    private static void wrap() {
        // TODO
    }

    private static void anotherMeterRegistry() {
        System.out.println("Another MeterRegistry in a thread.");

        MeterRegistry registry1 = new SimpleMeterRegistry();
        registry1.counter("programacho.counter").increment(10);
        System.out.println("MeterRegistry1: " + Counter.builder("programacho.counter").register(registry1).count());

        MeterRegistry registry2 = new SimpleMeterRegistry();
        registry2.counter("programacho.counter").increment(20);
        System.out.println("MeterRegistry2: " + Counter.builder("programacho.counter").register(registry2).count());
    }

    private static void sleep(int timeout) {
        try {
            TimeUnit.MILLISECONDS.sleep(timeout);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
