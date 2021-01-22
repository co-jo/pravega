/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.shared.metrics;

import com.google.common.base.Preconditions;
import io.micrometer.core.instrument.Meter;
import io.pravega.common.Exceptions;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Base class for a Metric Proxy.
 *
 * @param <T> Type of Metric.
 */
@Slf4j
abstract class MetricProxy<T extends Metric> implements AutoCloseable {
    private final AtomicReference<T> instance = new AtomicReference<>();
    private final AtomicBoolean closed = new AtomicBoolean();

    @Getter
    private final String proxyName;
    private final Consumer<String> closeCallback;

    /**
     * Creates a new instance of the MetricProxy class.
     *
     * @param instance      The initial Metric Instance.
     * @param proxyName     The name of the MetricProxy. This may be different from the name of the Metric's instance.
     * @param closeCallback A Consumer that will be invoked when this Proxy is closed.
     */
    MetricProxy(T instance, String proxyName, Consumer<String> closeCallback) {
        this.closeCallback = Preconditions.checkNotNull(closeCallback, "closeCallback");
        this.proxyName = Exceptions.checkNotNullOrEmpty(proxyName, "name");
        updateInstance(instance);
    }

    @Override
    public void close() {
        if (!closed.getAndSet(true)) {
            T i = this.instance.get();
            if (i != null) {
                log.info("Closing Metric: {} Instance: {}", proxyName, instance);
                i.close();
                this.closeCallback.accept(this.proxyName);
            }
        } else {
            log.info("Already Closed (close()) Metric: {} Instance: {}", proxyName, instance);
        }
    }

    /**
     * Gets the id of the underlying metric.
     *
     * @return The id of the underlying metric.
     */
    public Meter.Id getId() {
        return getInstance().getId();
    }

    /**
     * Updates the underlying Metric instance with the given one, and closes out the previous one.
     *
     * @param instance The instance to update to.
     */
    void updateInstance(T instance) {
        T oldInstance = this.instance.getAndSet(Preconditions.checkNotNull(instance, "instance"));
        if (oldInstance != null && oldInstance != instance) {
            oldInstance.close();
        }
    }

    protected T getInstance() {
        if (closed.get()) {
            log.warn("This MetricProxy ({}) has already been closed. Further updates to this Metric will not be seen by a MeterRegistry.", instance);
        }
        return this.instance.get();
    }
}
