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
package io.lumeer.api.model;

import java.util.List;
import java.util.Map;

public class WorkspacesData {

   private final List<Organization> organizations;
   private final Map<String, List<Project>> projects;
   private final Map<String, ServiceLimits> limits;
   private final Map<String, List<Group>> groups;

   public WorkspacesData(final List<Organization> organizations, final Map<String, List<Project>> projects,  final Map<String, ServiceLimits> limits, final Map<String, List<Group>> groups) {
      this.organizations = organizations;
      this.projects = projects;
      this.limits = limits;
      this.groups = groups;
   }

   public List<Organization> getOrganizations() {
      return organizations;
   }

   public Map<String, List<Project>> getProjects() {
      return projects;
   }

   public Map<String, ServiceLimits> getLimits() {
      return limits;
   }

   public Map<String, List<Group>> getGroups() {
      return groups;
   }
}
