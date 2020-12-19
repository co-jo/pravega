/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.shared.health.rest.v1;

import io.pravega.shared.health.rest.generated.api.NotFoundException;
import io.pravega.shared.health.rest.generated.model.HealthDependencies;
import io.pravega.shared.health.rest.generated.model.HealthResult;
import io.pravega.shared.health.rest.generated.model.HealthDetails;
import io.pravega.shared.health.rest.generated.model.HealthStatus;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;

/**
 * Controller APIs exposed via REST.
 * Different interfaces will hold different groups of APIs.
 *
 * ##############################IMPORTANT NOTE###################################
 * Do not make any API changes here directly, you need to update swagger/Controller.yaml and generate
 * the server stubs as documented in swagger/README.md before updating this file.
 *
 */
public final class ApiV1 {

    @Path("/v1/health")
    public interface HealthApi {
        @GET
        @Path("/{id}")

        @Produces({ "application/json" })
        @io.swagger.annotations.ApiOperation(value = "", notes = "Return the Health of a health contributor with a given id.", response = HealthResult.class, tags = { "Health", })

        @io.swagger.annotations.ApiResponses(value = {
                @io.swagger.annotations.ApiResponse(code = 201, message = "The Health result of the Controller.", response = HealthResult.class),

                @io.swagger.annotations.ApiResponse(code = 405, message = "A health provider for the given id could not be found.", response = HealthResult.class),

                @io.swagger.annotations.ApiResponse(code = 501, message = "Internal server error while fetching the health for a given contributor.", response = HealthResult.class) })
        void getContributorHealth(@ApiParam(value = "The id of an existing health contributor.", required = true) @PathParam("id") String id, @QueryParam("details") Boolean details,
                                  @Context SecurityContext securityContext, @Suspended AsyncResponse asyncResponse)
                throws NotFoundException;

        @GET
        @Path("/liveness/{id}")

        @Produces({ "application/json" })
        @io.swagger.annotations.ApiOperation(value = "", notes = "Fetch the liveness state of the specified health contributor.", response = Boolean.class, tags = { "Health", })
        @io.swagger.annotations.ApiResponses(value = {
                @io.swagger.annotations.ApiResponse(code = 201, message = "The alive status for the specified health contributor.", response = Boolean.class),

                @io.swagger.annotations.ApiResponse(code = 405, message = "The liveness status for the contributor with given id was not found.", response = Boolean.class),

                @io.swagger.annotations.ApiResponse(code = 501, message = "Internal server error while fetching the liveness state for a given health contributor.", response = Boolean.class) })
        void getContributorLiveness(@ApiParam(value = "The id of an existing health contributor.", required = true) @PathParam("id") String id,
                                    @Context SecurityContext securityContext, @Suspended AsyncResponse asyncResponse)
                throws NotFoundException;

        @GET
        @Path("/components")

        @Produces({ "application/json" })
        @io.swagger.annotations.ApiOperation(value = "", notes = "Fetch the details of the Controller service.", response = HealthDependencies.class, tags = { "Health", })
        @io.swagger.annotations.ApiResponses(value = {
                @io.swagger.annotations.ApiResponse(code = 201, message = "The list of health dependencies of the Controller.", response = HealthDependencies.class),

                @io.swagger.annotations.ApiResponse(code = 501, message = "Internal server error while fetching the health dependencies of the Controller.", response = HealthDependencies.class) })
        void getDependencies(@Context SecurityContext securityContext, @Suspended AsyncResponse asyncResponse)
                throws NotFoundException;

        @GET
        @Path("/components/{id}")

        @Produces({ "application/json" })
        @io.swagger.annotations.ApiOperation(value = "", notes = "Fetch the health dependencies for a specific health contributor.", response = HealthDependencies.class, tags = { "Health", })
        @io.swagger.annotations.ApiResponses(value = {
                @io.swagger.annotations.ApiResponse(code = 201, message = "The list of health dependencies for a specific health contributor.", response = HealthDependencies.class),

                @io.swagger.annotations.ApiResponse(code = 405, message = "The health dependencies for the contributor with given id was not found.", response = HealthDependencies.class),

                @io.swagger.annotations.ApiResponse(code = 501, message = "Internal server error while fetching the health dependencies of a given health contributor.", response = HealthDependencies.class) })
        void getContributorDependencies(@ApiParam(value = "The id of an existing health contributor.", required = true) @PathParam("id") String id,
                                        @Context SecurityContext securityContext, @Suspended AsyncResponse asyncResponse)
                throws NotFoundException;

        @GET
        @Path("/details/{id}")

        @Produces({ "application/json" })
        @io.swagger.annotations.ApiOperation(value = "", notes = "Fetch the details of a specific health contributor.", response = HealthDetails.class, tags = { "Health", })
        @io.swagger.annotations.ApiResponses(value = {
                @io.swagger.annotations.ApiResponse(code = 201, message = "The list of details for the health contributor with a given id.", response = HealthDetails.class),

                @io.swagger.annotations.ApiResponse(code = 405, message = "The health details for the contributor with given id was not found.", response = HealthDetails.class),

                @io.swagger.annotations.ApiResponse(code = 501, message = "Internal server error while fetching the health details for a given health contributor.", response = HealthDetails.class) })
        void getContributorDetails(@ApiParam(value = "The id of an existing health contributor.", required = true) @PathParam("id") String id,
                                   @Context SecurityContext securityContext, @Suspended AsyncResponse asyncResponse)
                throws NotFoundException;

        @GET
        @Path("/readiness/{id}")

        @Produces({ "application/json" })
        @io.swagger.annotations.ApiOperation(value = "", notes = "Fetch the ready state of the health contributor.", response = Boolean.class, tags = { "Health", })
        @io.swagger.annotations.ApiResponses(value = {
                @io.swagger.annotations.ApiResponse(code = 201, message = "The readiness status for the health contributor with given id.", response = Boolean.class),

                @io.swagger.annotations.ApiResponse(code = 405, message = "The readiness status for the contributor with given id was not found.", response = Boolean.class),

                @io.swagger.annotations.ApiResponse(code = 501, message = "Internal server error while fetching the ready state for a given health contributor.", response = Boolean.class) })
        void getContributorReadiness(@ApiParam(value = "The id of an existing health contributor.", required = true) @PathParam("id") String id,
                                     @Context SecurityContext securityContext, @Suspended AsyncResponse asyncResponse)
                throws NotFoundException;

        @GET
        @Path("/status/{id}")

        @Produces({ "application/json" })
        @io.swagger.annotations.ApiOperation(value = "", notes = "Fetch the status of a specific health contributor.", response = HealthStatus.class, tags = { "Health", })
        @io.swagger.annotations.ApiResponses(value = {
                @io.swagger.annotations.ApiResponse(code = 201, message = "The health status of the Controller.", response = HealthStatus.class),

                @io.swagger.annotations.ApiResponse(code = 405, message = "The health status for the contributor with given id was not found.", response = HealthStatus.class),

                @io.swagger.annotations.ApiResponse(code = 501, message = "Internal server error while fetching the health status of a given health contributor.", response = HealthStatus.class) })
        void getContributorStatus(@ApiParam(value = "The id of an existing health contributor.", required = true) @PathParam("id") String id,
                                  @Context SecurityContext securityContext, @Suspended AsyncResponse asyncResponse)
                throws NotFoundException;

        @GET
        @Path("/details")

        @Produces({ "application/json" })
        @io.swagger.annotations.ApiOperation(value = "", notes = "Fetch the details of the Controller service.", response = HealthDetails.class, tags = { "Health", })
        @io.swagger.annotations.ApiResponses(value = {
                @io.swagger.annotations.ApiResponse(code = 201, message = "The list of details.", response = HealthDetails.class),

                @io.swagger.annotations.ApiResponse(code = 501, message = "Internal server error while fetching the health details of the Controller.", response = HealthDetails.class) })
        void getDetails(@Context SecurityContext securityContext, @Suspended AsyncResponse asyncResponse)
                throws NotFoundException;

        @GET


        @Produces({ "application/json" })
        @io.swagger.annotations.ApiOperation(value = "", notes = "Return the Health of the Controller service.", response = HealthResult.class, tags = { "Health", })
        @io.swagger.annotations.ApiResponses(value = {
                @io.swagger.annotations.ApiResponse(code = 201, message = "The Health result of the Controller.", response = HealthResult.class),

                @io.swagger.annotations.ApiResponse(code = 501, message = "Internal server error while fetching the Health.", response = HealthResult.class) })
        void getHealth(@ApiParam(value = "Whether or not to provide the details in the health response.", defaultValue = "false") @QueryParam("details") Boolean details,
                       @Context SecurityContext securityContext, @Suspended AsyncResponse asyncResponse)
                throws NotFoundException;

        @GET
        @Path("/liveness")

        @Produces({ "application/json" })
        @io.swagger.annotations.ApiOperation(value = "", notes = "Fetch the liveness state of the Controller service.", response = Boolean.class, tags = { "Health", })
        @io.swagger.annotations.ApiResponses(value = {
                @io.swagger.annotations.ApiResponse(code = 201, message = "The alive status.", response = Boolean.class),

                @io.swagger.annotations.ApiResponse(code = 501, message = "Internal server error while fetching the liveness state of the Controller.", response = Boolean.class) })
        void getLiveness(@Context SecurityContext securityContext, @Suspended AsyncResponse asyncResponse)
                throws NotFoundException;

        @GET
        @Path("/readiness")

        @Produces({ "application/json" })
        @io.swagger.annotations.ApiOperation(value = "", notes = "Fetch the ready state of the Controller service.", response = Boolean.class, tags = { "Health", })
        @io.swagger.annotations.ApiResponses(value = {
                @io.swagger.annotations.ApiResponse(code = 201, message = "The ready status.", response = Boolean.class),

                @io.swagger.annotations.ApiResponse(code = 501, message = "Internal server error while fetching the ready state of the Controller.", response = Boolean.class) })
        void getReadiness(@Context SecurityContext securityContext, @Suspended AsyncResponse asyncResponse)
                throws NotFoundException;

        @GET
        @Path("/status")

        @Produces({ "application/json" })
        @io.swagger.annotations.ApiOperation(value = "", notes = "Fetch the status of the Controller service.", response = HealthStatus.class, tags = { "Health", })
        @io.swagger.annotations.ApiResponses(value = {
                @io.swagger.annotations.ApiResponse(code = 201, message = "The health status of the Controller.", response = HealthStatus.class),

                @io.swagger.annotations.ApiResponse(code = 501, message = "Internal server error while fetching the health status of the Controller.", response = HealthStatus.class) })
        void getStatus(@Context SecurityContext securityContext, @Suspended  AsyncResponse asyncResponse)
                throws NotFoundException;
    }
}
