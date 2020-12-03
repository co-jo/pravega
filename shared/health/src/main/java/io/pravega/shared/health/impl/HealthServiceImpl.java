/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.shared.health.impl;

import io.pravega.shared.health.ContributorRegistry;
import io.pravega.shared.health.HealthConfig;
import io.pravega.shared.health.HealthDaemon;
import io.pravega.shared.health.HealthEndpoint;
import io.pravega.shared.health.HealthService;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;

@Slf4j
public class HealthServiceImpl implements HealthService {

    private final ContributorRegistry registry;

    /**
     * The {@link HealthConfig} object used to setup the {@link HealthComponent} hierarchy.
     */
    private HealthConfig config;

    /**
     * The {@link HealthDaemon} which provides passive health updates.
     */
    private final HealthDaemon daemon;

    private final HealthEndpoint endpoint;

    public HealthServiceImpl(HealthConfig config) {
        this.config = config;
        this.registry = new ContributorRegistryImpl();
        this.endpoint = new HealthEndpointImpl(this.registry);
        // Initializes the ContributorRegistry into the expected starting state.
        this.config.reconcile(this.registry);
        this.daemon = new HealthDaemonImpl(this);
    }

    @Override
    public Collection<String> components() {
        return registry().components();
    }

    @Override
    public ContributorRegistry registry() {
        return this.registry;
    }

    @Override
    public HealthEndpoint endpoint() {
        return this.endpoint;
    }

    @Override
    public HealthDaemon daemon() {
        return daemon;
    }

    /**
     * Reverts the state of the {@link HealthService} had it just been initialized and applied the provided
     * {@link  HealthConfig}.
     */
    @Override
    public void clear() {
        this.registry.reset();
        // The ContributorRegistry clears all it's internal state, so the reconcile process must be repeated.
        this.config.reconcile(this.registry);
    }
}
