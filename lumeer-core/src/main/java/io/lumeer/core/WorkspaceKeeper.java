/*
 * Lumeer: Modern Data Definition and Processing Platform
 *
 * Copyright (C) since 2017 Lumeer.io, s.r.o. and/or its affiliates.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.lumeer.core;

import io.lumeer.api.SelectedWorkspace;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.ServiceLimits;
import io.lumeer.core.cache.WorkspaceCache;

import java.util.Optional;
import java.util.Stack;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

@RequestScoped
public class WorkspaceKeeper implements SelectedWorkspace {

   private String organizationId;
   private String projectId;

   private Stack<Workspace> workspaceStack = new Stack<>();

   @Inject
   private WorkspaceCache workspaceCache;

   @Override
   public Optional<Organization> getOrganization() {
      if (organizationId == null) {
         return Optional.empty();
      }
      return Optional.of(workspaceCache.getOrganization(organizationId));
   }

   @Override
   public Optional<Project> getProject() {
      if (projectId == null) {
         return Optional.empty();
      }
      return Optional.of(workspaceCache.getProject(projectId));
   }

   public void push() {
      if (this.organizationId != null) {
         this.workspaceStack.push(new Workspace(this.organizationId, this.projectId));
      }
   }

   public void pop() {
      if (!this.workspaceStack.isEmpty()) {
         var workspace = this.workspaceStack.pop();
         this.setWorkspaceIds(workspace.getOrganizationId(), workspace.getProjectId());
      }

   }

   public String getOrganizationId() {
      return organizationId;
   }

   public void setOrganizationId(String organizationId) {
      this.organizationId = organizationId;
   }

   public void setOrganization(Organization organization) {
      this.organizationId = organization.getId();
      this.workspaceCache.updateOrganization(organization.getId(), organization);
   }

   public String getProjectId() {
      return projectId;
   }

   public void setProjectId(String projectId) {
      this.projectId = projectId;
   }

   public void setProject(Project project) {
      this.projectId = project.getId();
      this.workspaceCache.updateProject(project.getId(), project);
   }

   public void setWorkspaceIds(String organizationId, String projectId) {
      setOrganizationId(organizationId);
      setProjectId(projectId);
   }

   public void setWorkspace(Organization organization, Project project) {
      this.setOrganization(organization);
      this.setProject(project);
   }

   public void setServiceLimits(final Organization organization, final ServiceLimits serviceLimits) {
      if (organization != null) {
         workspaceCache.setServiceLimits(organization.getId(), serviceLimits);
      }
   }

   public ServiceLimits getServiceLimits(final Organization organization) {
      if (organization != null) {
         return workspaceCache.getServiceLimits(organization.getId());
      }

      return null;
   }

   public void clearServiceLimits(final Organization organization) {
      if (organization != null) {
         workspaceCache.removeServiceLimits(organization.getId());
      }
   }

   private class Workspace {
      private final String organizationId;
      private final String projectId;

      public Workspace(final String organizationId, final String projectId) {
         this.organizationId = organizationId;
         this.projectId = projectId;
      }

      public String getOrganizationId() {
         return organizationId;
      }

      public String getProjectId() {
         return projectId;
      }
   }
}
