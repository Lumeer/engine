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
import io.lumeer.core.facade.SystemDatabaseConfigurationFacade;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;
import io.lumeer.core.task.AbstractContextualTask;
import io.lumeer.core.task.ContextualTaskFactory;
import io.lumeer.engine.annotation.SystemDataStorage;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.StorageConnection;
import io.lumeer.storage.api.DataStorageFactory;
import io.lumeer.storage.api.dao.context.DaoContextSnapshot;

import java.util.List;
import java.util.Optional;
import javax.inject.Inject;

public abstract class WorkspaceContext {

   @SystemDataStorage
   @Inject
   private DataStorage systemDataStorage;

   @Inject
   private SystemDatabaseConfigurationFacade systemDatabaseConfigurationFacade;

   @Inject
   private DataStorageFactory dataStorageFactory;

   @Inject
   protected DefaultConfigurationProducer configurationProducer;

   protected DataStorage getDataStorage(final String organizationId) {
      final List<StorageConnection> connections = systemDatabaseConfigurationFacade.getDataStorage(organizationId);
      final String database = systemDatabaseConfigurationFacade.getDataStorageDatabase(organizationId);
      final Boolean useSsl = systemDatabaseConfigurationFacade.getDataStorageUseSsl(organizationId);
      return dataStorageFactory.getStorage(connections, database, useSsl);
   }

   protected DaoContextSnapshot getDaoContextSnapshot(final DataStorage userDataStorage, final SelectedWorkspace selectedWorkspace) {
      return dataStorageFactory.getDaoContextSnapshot(systemDataStorage, userDataStorage, selectedWorkspace);
   }

   protected ContextualTaskFactory getTaskFactory(final DaoContextSnapshot contextSnapshot) {
      return new AbstractContextualTask.SyntheticContextualTaskFactory(configurationProducer, contextSnapshot);
   }

   public static class Workspace implements SelectedWorkspace {
      private final Organization organization;
      private final Project project;

      public Workspace(final Organization organization, final Project project) {
         this.organization = organization;
         this.project = project;
      }

      @Override
      public Optional<Organization> getOrganization() {
         return Optional.of(organization);
      }

      @Override
      public Optional<Project> getProject() {
         return Optional.ofNullable(project);
      }
   }

}
