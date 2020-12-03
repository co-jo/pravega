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
import io.pravega.shared.health.Health;
import io.pravega.shared.health.HealthContributor;
import io.pravega.shared.health.Status;
import io.pravega.shared.health.StatusAggregator;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;

@Slf4j
public abstract class CompositeHealthContributor implements HealthContributor {

    /**
     * The {@link StatusAggregator} used to perform the aggregation of all the {@link HealthContributor} dependencies.
     */
    @Getter
    private final StatusAggregator aggregator;

    private ContributorRegistry registry = null;

    private Collection<HealthContributor> contributors = new HashSet<>();

    CompositeHealthContributor(StatusAggregator aggregator, ContributorRegistry registry) {
        this.aggregator = aggregator;
        this.registry = registry;
    }

    public Health health() {
        return health(false);
    }

    public Health health(boolean includeDetails) {
        Health.HealthBuilder builder = Health.builder().name(getName());
        // Fetch the Health Status of all dependencies.
        val children =  contributors().stream()
                .filter(contributor -> contributor != null)
                .map(contributor -> {
                    Health health = contributor.health(includeDetails);
                    if (health.getStatus() == Status.UNKNOWN) {
                        log.warn("{} has a Status of 'UNKNOWN'. This indicates `doHealthCheck` does not set a status" +
                                " or is an empty HealthComponent.", health.getName());
                    }
                    return health;
                })
                .collect(Collectors.toList());
        // Get the aggregate health status.
        Status status = aggregator.aggregate(children
                .stream()
                .map(contributor -> contributor.getStatus())
                .collect(Collectors.toList()));
        builder.status(status);
        // Even if includeDetails if false, iterating over the dependencies is necessary for Status aggregation.
        if (includeDetails) {
            builder.children(children);
        } else {
            // Creates a Health object with the minimal set of information (just the Name and Status).
            builder.children(children.stream()
                    .map(child -> Health.builder().name(child.getName()).status(child.getStatus()).build())
                    .collect(Collectors.toList()));
        }
        return builder.build();
    }

    /**
     * A method which supplies the {@link CompositeHealthContributor#health(boolean)} method with the collection of
     * {@link HealthContributor} objects to perform the aggregate health check on. This is helpful because it gives us
     * flexibility in defining where the contributors may be but also avoids the requirement of being bound to a
     * {@link ContributorRegistry} instance.
     *
     * @return The {@link Collection} of {@link HealthContributor}.
     */
    public Collection<HealthContributor> contributors() {
        if (registry != null) {
            return registry.dependencies(getName());
        }
        return this.contributors;
    }

    abstract public String getName();

}