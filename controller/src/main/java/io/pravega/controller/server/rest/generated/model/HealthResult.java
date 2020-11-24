/*
 * Pravega Controller APIs
 * List of admin REST APIs for the pravega controller service.
 *
 * OpenAPI spec version: 1.0.1
 * 
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */


package io.pravega.controller.server.rest.generated.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.pravega.controller.server.rest.generated.model.HealthDetails;
import io.pravega.controller.server.rest.generated.model.HealthResult;
import io.pravega.controller.server.rest.generated.model.HealthStatus;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.*;

/**
 * HealthResult
 */

public class HealthResult   {
  @JsonProperty("name")
  private String name = null;

  @JsonProperty("status")
  private HealthStatus status = null;

  @JsonProperty("readiness")
  private Boolean readiness = null;

  @JsonProperty("liveness")
  private Boolean liveness = null;

  @JsonProperty("details")
  private HealthDetails details = null;

  @JsonProperty("children")
  private List<HealthResult> children = null;

  public HealthResult name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Get name
   * @return name
   **/
  @JsonProperty("name")
  @ApiModelProperty(value = "")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public HealthResult status(HealthStatus status) {
    this.status = status;
    return this;
  }

  /**
   * Get status
   * @return status
   **/
  @JsonProperty("status")
  @ApiModelProperty(value = "")
  public HealthStatus getStatus() {
    return status;
  }

  public void setStatus(HealthStatus status) {
    this.status = status;
  }

  public HealthResult readiness(Boolean readiness) {
    this.readiness = readiness;
    return this;
  }

  /**
   * Get readiness
   * @return readiness
   **/
  @JsonProperty("readiness")
  @ApiModelProperty(value = "")
  public Boolean getReadiness() {
    return readiness;
  }

  public void setReadiness(Boolean readiness) {
    this.readiness = readiness;
  }

  public HealthResult liveness(Boolean liveness) {
    this.liveness = liveness;
    return this;
  }

  /**
   * Get liveness
   * @return liveness
   **/
  @JsonProperty("liveness")
  @ApiModelProperty(value = "")
  public Boolean getLiveness() {
    return liveness;
  }

  public void setLiveness(Boolean liveness) {
    this.liveness = liveness;
  }

  public HealthResult details(HealthDetails details) {
    this.details = details;
    return this;
  }

  /**
   * Get details
   * @return details
   **/
  @JsonProperty("details")
  @ApiModelProperty(value = "")
  public HealthDetails getDetails() {
    return details;
  }

  public void setDetails(HealthDetails details) {
    this.details = details;
  }

  public HealthResult children(List<HealthResult> children) {
    this.children = children;
    return this;
  }

  public HealthResult addChildrenItem(HealthResult childrenItem) {
    if (this.children == null) {
      this.children = new ArrayList<HealthResult>();
    }
    this.children.add(childrenItem);
    return this;
  }

  /**
   * Get children
   * @return children
   **/
  @JsonProperty("children")
  @ApiModelProperty(value = "")
  public List<HealthResult> getChildren() {
    return children;
  }

  public void setChildren(List<HealthResult> children) {
    this.children = children;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    HealthResult healthResult = (HealthResult) o;
    return Objects.equals(this.name, healthResult.name) &&
        Objects.equals(this.status, healthResult.status) &&
        Objects.equals(this.readiness, healthResult.readiness) &&
        Objects.equals(this.liveness, healthResult.liveness) &&
        Objects.equals(this.details, healthResult.details) &&
        Objects.equals(this.children, healthResult.children);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, status, readiness, liveness, details, children);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class HealthResult {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    readiness: ").append(toIndentedString(readiness)).append("\n");
    sb.append("    liveness: ").append(toIndentedString(liveness)).append("\n");
    sb.append("    details: ").append(toIndentedString(details)).append("\n");
    sb.append("    children: ").append(toIndentedString(children)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

