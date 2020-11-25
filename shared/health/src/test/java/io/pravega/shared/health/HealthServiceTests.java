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

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.junit.rules.Timeout;

import io.pravega.shared.health.TestHealthIndicators.SampleHealthyIndicator;

@Slf4j
// Hosts much of the same tests that would go into a HealthEndpointTests class, making HealthEndpointTests redundant.
public class HealthServiceTests {

    @Rule
    public final Timeout timeout = new Timeout(600, TimeUnit.SECONDS);

    HealthService service;

    public void start() throws IOException {
        HealthServiceFactory factory = new HealthServiceFactory();
        service = factory.createHealthService(true);
    }

    public void stop() {
        service.clear();
        Assert.assertEquals("The HealthService should not maintain any references to HealthContributors.",
                1,
                service.components().size());
        Assert.assertEquals("The ContributorRegistry should not maintain any references to HealthContributors",
                1,
                service.registry().contributors().size());
    }

    @Before
    @SneakyThrows
    public void before() {
        start();
    }

    @After
    public void after() {
        stop();
    }

    @Test
    public void lifecycle() throws IOException {
        // Perform a start-up and shutdown sequence (implicit start() with @Before).
        stop();
        // Verify that it is repeatable.
        start();
        stop();
    }

    @Test
    public void health() throws IOException {
        SampleHealthyIndicator indicator = new SampleHealthyIndicator();
        service.registry().register(indicator);
        try {
            Health health = service.endpoint().health(indicator.getName());
            Assert.assertTrue("Status of the default/root component is expected to be 'UP'", health.getStatus() == Status.UP);

            health = service.endpoint().health(true);
            Assert.assertEquals("There should be exactly one child (SimpleIndicator)", health.getChildren().size(), 1);
        } catch (ContributorNotFoundException e) {
            Assert.fail("HealthContributor not found.");
        }
    }

    @Test
    public void components() {
        SampleHealthyIndicator indicator = new SampleHealthyIndicator();
        service.registry().register(indicator);

        Collection<String> components = service.components();
        // Only the 'ROOT' HealthComponent should be registered.
        Assert.assertEquals("No non-root components have been defined.", service.components().size(), 1);
        // Assert that it is indeed the 'ROOT'
        Assert.assertEquals("Expected the name of the returned component to match the ROOT's given name.",
                ContributorRegistry.DEFAULT_CONTRIBUTOR_NAME,
                components.stream().findFirst().get());
    }

    @Test
    public void details() {
        SampleHealthyIndicator indicator = new SampleHealthyIndicator();
        service.registry().register(indicator);

        Health health = service.endpoint().health(true);
        Assert.assertTrue("There should be at least one child (SimpleIndicator)", health.getChildren().size() >= 1);

        Health sample = health.getChildren().stream().findFirst().get();
        Assert.assertEquals("There should be one details entry provided by the SimpleIndicator.", 1, sample.getDetails().size());
        Assert.assertEquals(String.format("Key should equal \"%s\"", SampleHealthyIndicator.DETAILS_KEY),
                SampleHealthyIndicator.DETAILS_KEY,
                sample.getDetails().keySet().stream().findFirst().get());
        Assert.assertEquals(String.format("Value should equal \"%s\"", SampleHealthyIndicator.DETAILS_VAL),
                SampleHealthyIndicator.DETAILS_VAL,
                sample.getDetails().entrySet().stream().findFirst().get().getValue());
    }

    @Test
    public void liveness() {
        SampleHealthyIndicator indicator = new SampleHealthyIndicator();
        service.registry().register(indicator);

        boolean alive = service.endpoint().liveness();
        Assert.assertEquals("The SampleIndicator should produce an 'alive' result.", true, alive);
    }

    @Test
    public void readiness() {
        SampleHealthyIndicator indicator = new SampleHealthyIndicator();
        service.registry().register(indicator);

        boolean ready = service.endpoint().readiness();
        Assert.assertEquals("The SampleIndicator should produce a 'ready' result.", true, ready);
    }
}
