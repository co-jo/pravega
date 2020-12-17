package io.pravega.segmentstore.server.rest.generated.api;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

public abstract class HealthApiService {
    public abstract Response getContributorDependencies(String id,SecurityContext securityContext) throws NotFoundException;
    public abstract Response getContributorDetails(String id,SecurityContext securityContext) throws NotFoundException;
    public abstract Response getContributorHealth(String id, Boolean details,SecurityContext securityContext) throws NotFoundException;
    public abstract Response getContributorLiveness(String id,SecurityContext securityContext) throws NotFoundException;
    public abstract Response getContributorReadiness(String id,SecurityContext securityContext) throws NotFoundException;
    public abstract Response getContributorStatus(String id,SecurityContext securityContext) throws NotFoundException;
    public abstract Response getDependencies(SecurityContext securityContext) throws NotFoundException;
    public abstract Response getDetails(SecurityContext securityContext) throws NotFoundException;
    public abstract Response getHealth( Boolean details,SecurityContext securityContext) throws NotFoundException;
    public abstract Response getLiveness(SecurityContext securityContext) throws NotFoundException;
    public abstract Response getReadiness(SecurityContext securityContext) throws NotFoundException;
    public abstract Response getStatus(SecurityContext securityContext) throws NotFoundException;
}
