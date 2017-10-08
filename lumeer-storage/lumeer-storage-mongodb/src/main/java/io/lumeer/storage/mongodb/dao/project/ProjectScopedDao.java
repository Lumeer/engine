/*
 * Lumeer: Modern Data Definition and Processing Platform
 *
 * Copyright (C) since 2017 Answer Institute, s.r.o. and/or its affiliates.
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
package io.lumeer.storage.mongodb.dao.project;

import io.lumeer.api.SelectedWorkspace;
import io.lumeer.api.model.Project;
import io.lumeer.storage.mongodb.dao.organization.OrganizationScopedDao;

import java.util.Optional;
import javax.annotation.PostConstruct;
import javax.inject.Inject;

public abstract class ProjectScopedDao extends OrganizationScopedDao {

   @Inject
   private SelectedWorkspace selectedWorkspace;

   private Project project;

   @PostConstruct
   public void init() {
      super.init();

      if (selectedWorkspace.getProject().isPresent()) {
         this.project = selectedWorkspace.getProject().get();
      }
   }

   public Optional<Project> getProject() {
      return Optional.ofNullable(project);
   }

   public void setProject(final Project project) {
      this.project = project;
   }
}
