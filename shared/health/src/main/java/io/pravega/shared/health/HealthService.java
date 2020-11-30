/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.shared.health;

import java.util.Collection;

/**
 * The top level interface used to provide any and all health related information for a particular component
 * of Pravega. It holds the {@link ContributorRegistry} and provides the endpoint used to make health information
 * accessible to clients.
 *
 * A {@link HealthService} should provide four endpoints:
 *  * /health               A route providing the aggregate of the three routes listed below.
 *  * /health/readiness     Exposes the top level 'ready' status.
 *  * /health/liveness      Exposes the top level 'liveness' status.
 *  * /health/details       Exposes the aggregate {@link Health} details.
 */
public interface HealthService {

    /**
     * Returns a {@link Collection} of all the components the {@link HealthService} is responsible for and observes.
     * @return
     */
    Collection<String> components();

    /**
     * The {@link ContributorRegistry} acts as the means to organize and references to the various {@link HealthContributor}
     * objects we wish to track.
     *
     * @return The {@link ContributorRegistry} backing the {@link HealthService}.
     */
    ContributorRegistry registry();

    /**
     * The main interface between some client and the {@link HealthService}. The {@link HealthEndpoint} encapsulates
     * the various types of requests the {@link HealthService} will be able to fulfill.
     *
     * @return The {@link HealthEndpoint} instance.
     */
    HealthEndpoint endpoint();

    HealthDaemon daemon();

    /**
     * This method should remove all state from the {@link HealthService} object. Meaning the {@link HealthService} should
     * act as if it was just constructed.
     */
    void clear();
}
