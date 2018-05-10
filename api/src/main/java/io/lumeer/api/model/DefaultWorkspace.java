package io.lumeer.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class DefaultWorkspace {

   public static final String ORGANIZATION_ID = "organizationId";
   public static final String PROJECT_ID = "projectId";

   private String organizationId;
   private String organizationCode;
   private String projectId;
   private String projectCode;

   @JsonCreator
   public DefaultWorkspace(@JsonProperty(ORGANIZATION_ID) final String organizationId,
         @JsonProperty(PROJECT_ID) final String projectId) {
      this.organizationId = organizationId;
      this.projectId = projectId;
   }

   public String getOrganizationId() {
      return organizationId;
   }

   public void setOrganizationId(final String organizationId) {
      this.organizationId = organizationId;
   }

   public String getProjectId() {
      return projectId;
   }

   public void setProjectId(final String projectId) {
      this.projectId = projectId;
   }

   public String getOrganizationCode() {
      return organizationCode;
   }

   public void setOrganizationCode(final String organizationCode) {
      this.organizationCode = organizationCode;
   }

   public String getProjectCode() {
      return projectCode;
   }

   public void setProjectCode(final String projectCode) {
      this.projectCode = projectCode;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (!(o instanceof DefaultWorkspace)) {
         return false;
      }
      final DefaultWorkspace that = (DefaultWorkspace) o;
      return Objects.equals(getOrganizationId(), that.getOrganizationId()) &&
            Objects.equals(getProjectId(), that.getProjectId());
   }

   @Override
   public int hashCode() {
      return Objects.hash(getOrganizationId(), getProjectId());
   }

   @Override
   public String toString() {
      return "DefaultWorkspace{" +
            "organizationId='" + organizationId + '\'' +
            ", projectId='" + projectId + '\'' +
            '}';
   }
}
