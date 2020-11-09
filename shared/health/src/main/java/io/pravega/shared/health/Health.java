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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * The {@link Health} interface represents the data gathered by a {@link HealthIndicator} after performing a health check.
 */
@Builder
@AllArgsConstructor
@JsonInclude(Include.NON_EMPTY)
public class Health {

    /**
     * Any sort of identifying string that describes from which component this measurement
     * was taken from.
     */
    @Getter
    @JsonProperty("name")
    public final String name;

    @Getter
    @JsonProperty("status")
    private Status status = Status.UNKNOWN;

    @Getter
    @JsonProperty("details")
    private final Collection<Map.Entry<String, String>> details;

    @JsonProperty("ready")
    private final Optional<Boolean> ready;

    @JsonProperty("alive")
    private final Optional<Boolean> alive;

    /**
     * A {@link CompositeHealthContributor} may be composed of any number of child {@link HealthContributor}.
     */
    @Getter
    @JsonProperty("children")
    private final Collection<Health> children;

    Health(HealthBuilder builder) {
        this.status = builder.status;
        this.details = builder.details;
        this.name = builder.name;
        this.children = builder.children;
        this.ready = builder.ready;
        this.alive = builder.alive;
    }

    /**
     * Used to perform readiness checks. It determines if the {@link Health} object holds a {@link Status} that is considered 'ready'.
     * A component is considered 'ready' if it has completed it's initialization step(s) and is ready to execute.
     *
     * @return
     */
    public boolean ready() {
        if (this.ready.isPresent()) {
            return this.ready.get();
        } else {
            return Status.alive(this.status);
        }
    }

    /**
     * Used to perform liveness checks. It determines if the {@link Health} object holds a {@link Status} that is considered 'alive'.
     * A component is considered 'alive' if it is able to perform it's expected duties.
     *
     * The distinction is not as useful within a single process, but might mean that there exists some number of threads
     * backing the service. Otherwise, if the {@link Health} object describes the entire process, 'alive' describes whether
     * or not it is running.
     *
     * A component that is 'ready' implies that it is 'alive', but not vice versa.
     *
     * @return
     */
    public boolean alive() {
        if (this.alive.isPresent()) {
            return this.alive.get();
        } else {
            return Status.alive(this.status);
        }
    }
}
