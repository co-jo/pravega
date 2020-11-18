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


import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;

/**
 * The {@link HealthIndicator} interface is the primary interface a client (some arbitrary class) uses to export health
 * information to the {@link HealthService}. A {@link HealthIndicator} is something that *contributes* health {@link Status}
 * information used to determine the well-being of one or more components.
 *
 * A {@link HealthIndicator} will implicitly register itself with the {@link ContributorRegistry}.
 */
@Slf4j
public abstract class HealthIndicator implements HealthContributor {

      final Details details;

      @Getter
      private final String name;

      @NonNull
      HealthIndicator(String name) {
            this(name, new Details());
      }

      @NonNull
      HealthIndicator(String name, Details details) {
            this.name = name;
            this.details = details;
      }

      public Health health(boolean includeDetails) {
            Health.HealthBuilder builder = new Health.HealthBuilder();
            try {
                  doHealthCheck(builder);
            } catch (Exception ex) {
                  log.warn(this.healthCheckFailedMessage());
                  builder.status(Status.DOWN);
            }
            if (includeDetails) {
                  builder.details(this.getDetails());
            }
            return builder.name(name).build();
      }

      String healthCheckFailedMessage() {
            return String.format("A Health Check on the {} has failed.", this.name);
      }

      Collection<Details.Result> getDetails() {
          return this.details.fetch();
      }

      @Override
      public String toString() {
            return String.format("HealthIndicator::%s", this.name);
      }

      /**
       * The {@link HealthIndicator#doHealthCheck(Health.HealthBuilder)} method is the primary interface used by some client
       * to define the logic which determines the health status of a component.
       *
       * This method *must* define logic to assign the {@link Status} that best reflects the current state of the component.
       * - It *should* also determine if the component is considered both {@link Health#alive()} and {@link Health#ready()}.
       *   If ready/alive logic is not defined, {@link Status#alive(Status)} defines the default logic for *both*.
       *
       * Optionally, {@link Details} may be provided to gain further insight to the status of the component. The end result
       * should be a key, value pair of type {@link String}. {@link Details} accepts a {@link java.util.function.Supplier}
       * that can return any arbitrary {@link Object}, but said object *must* have the necessary `toString` logic defined
       * (to be human readable).
       *
       * The {@link Details} object may be constructed ahead of time and provided during registration
       *
       * @param builder The {@link Health.HealthBuilder} object.
       * @throws Exception An exception to be thrown if the underlying health check fails.
       */
      public abstract void doHealthCheck(Health.HealthBuilder builder) throws Exception;
}
