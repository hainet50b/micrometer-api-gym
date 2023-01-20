package com.programacho.micrometerapigym;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.TimeGauge;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class MicrometerApiGymApplication {

    public static void main(String[] args) {
        // MeterRegistry - Timer
        timer();

        // MeterRegistry - Counter
        counter();

        // MeterRegistry - Gauge
        gauge();

        // MeterRegistry - TimeGauge
        timeGauge();

        // Another MeterRegistry in a thread.
        anotherMeterRegistry();
    }

    private static void timer() {
        System.out.println("MeterRegistry - Timer");

        MeterRegistry registry = new SimpleMeterRegistry();

        registry.timer("programacho.timer").record(() -> {
            sleep(1);
        });

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
