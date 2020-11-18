/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.shared.health.impl;

import io.pravega.shared.health.ContributorRegistry;
import io.pravega.shared.health.HealthComponent;
import io.pravega.shared.health.HealthConfig;
import io.pravega.shared.health.StatusAggregator;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The {@link HealthConfig} class allows a client to define a (logical) tree-like structure of {@link HealthComponent}
 * to {@link HealthComponent} relations.
 */
@Slf4j
public class HealthConfigImpl implements HealthConfig {

    private final Map<String, HealthComponent> components;

    private final Set<HealthComponent> roots;

    private final Map<String, Set<String>> relations;

    private HealthConfigImpl(Map<String, HealthComponent> components, Set<HealthComponent> roots, Map<String, Set<String>> relations) {
        this.components = components;
        this.roots = roots;
        this.relations = relations;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final Map<String, HealthComponent> components = new HashMap<>();

        private final Map<String, Set<String>> relations = new HashMap<>();

        private final Set<HealthComponent> roots = new HashSet<>();

        public Builder define(String name, StatusAggregator aggregator) {
            if (components.containsKey(name)) {
                log.warn("Overwriting a pre-existing component definition -- aborting.");
                return this;
            }
            HealthComponent component = new HealthComponent(name, aggregator);
            roots.add(component);
            components.put(name, component);
            relations.put(name, new HashSet<>());
            return this;
        }

        public Builder relation(String child, String parent) {
            if (child.equals(parent)) {
                log.warn("Attempting to add a reference to itself -- aborting.");
                return this;
            }
            if (!components.containsKey(child) || !components.containsKey(parent)) {
                log.warn("At least one of the components in this relation has not been defined -- aborting.");
                return this;
            }
            roots.remove(components.get(child));
            relations.get(parent).add(child);
            return this;
        }

        /**
         * Performs an exhaustive graph traversal to ensure (DFS) that there are no cyclic relations.
         *
         * @return Whether or not the specification is cycle free.
         * @throws Exception
         */
        private boolean validate() {
            boolean cycle = false;
            for (val component : relations.entrySet()) {
                cycle |= validate(component.getKey(), new HashMap<>());
            }
            return !cycle;
        }

        private boolean validate(String name, Map<String, Boolean> visited) {
            if (visited.get(name)) {
                return true;
            }
            boolean cycle = false;
            visited.put(name, true);
            for (String child : relations.get(name)) {
                cycle |= validate(child, visited);
            }
            return cycle;
        }

        public HealthConfig empty() {
            HealthConfig config = null;
            try {
                config = build();
            } catch (Exception e) {
                log.error("Error building empty HealthConfig.");
            }
            return config;
        }

        public HealthConfig build() throws Exception {
            if (validate()) {
                return new HealthConfigImpl(components, roots, relations);
            } else {
                throw new RuntimeException("Invalid HealthComponentConfig definition -- cyclic references.");
            }
        }
    }

    public boolean isEmpty() {
        return components.isEmpty();
    }

    public void reconcile(ContributorRegistry registry) {
        for (val component : this.roots) {
            recurse(registry, component, null);
        }
    }

    // The validation should prevent any unintended StackOverflow errors.
    private void recurse(ContributorRegistry registry, HealthComponent component, HealthComponent parent) {
        registry.register(component, parent);
        // Register all dependencies.
        for (val name : relations.get(component.getName())) {
            recurse(registry, components.get(name), component);
        }
    }
}